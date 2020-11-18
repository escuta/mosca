/*
 * Mosca: SuperCollider class by Iain Mott, 2016. Licensed under a
 * Creative Commons Attribution-NonCommercial 4.0 International License
 * http://creativecommons.org/licenses/by-nc/4.0/
 * The class makes extensive use of the Ambisonic Toolkit (http://www.ambisonictoolkit.net/)
 * by Joseph Anderson and the Automation quark
 * (https://github.com/neeels/Automation) by Neels Hofmeyr.
 * Required Quarks : Automation, Ctk, XML and  MathLib
 * Required classes:
 * SC Plugins: https://github.com/supercollider/sc3-plugins
 * User must set up a project directory with subdirectoties "rir" and "auto"
 * RIRs should have the first 100 or 120ms silenced to act as "tail" reverberators
 * and must be placed in the "rir" directory.
 * Run help on the "Mosca" class in SuperCollider for detailed information
 * and code examples. Further information and sample RIRs and B-format recordings
 * may be downloaded here: http://escuta.org/mosca
*/

AutomationBase
{
	var auto, docking_name;

	dockTo
	{ | automation, name |

		auto = automation;
		docking_name = name;
		automation.dock(this, name);
	}

	free
	{
		if (auto.notNil)
		{
			var index = auto.clients.detectIndex(
				{ | client | client.name == docking_name }
			);

			auto.clients.removeAt(index).free;
			postln(docking_name ++ "automation client removed");
		};

		super.free;
	}
}

AutomationProxy : AutomationBase
{
	var <>value, <>action;

	absoluteBounds { ^Rect(0,0,0,0) } // to keep Automation happy!

	*new { | val | ^super.new.ctr(val) }

	ctr { | val | value = val }

	doAction { action.value(value) }

	valueAction_ { | val | this.value_(val).doAction }
}

OssiaAutomationProxy : AutomationBase
{
	// embed an OSSIA_Parameter in a View to be used with Automation
	// single value version
	var <node;

	absoluteBounds { ^Rect(0,0,0,0) } // to keep Automation happy!

	*new
	{ | parent_node, name, type, domain, default_value, bounding_mode = 'free',
		critical = false, repetition_filter = true |

		^super.new.ctr(parent_node, name, type, domain, default_value, bounding_mode, critical, repetition_filter);
	}

	ctr
	{ | parent_node, name, type, domain, default_value, bounding_mode, critical, repetition_filter |

		node = OSSIA_Parameter(parent_node, name, type, domain, default_value,
			bounding_mode, critical, repetition_filter);
	}

	value { ^node.v }

	value_ { | value | node.v_(value) }

	action { ^node.callback }

	action_ { | function | node.callback_(function) }
}


	//-------------------------------------------//
	//             COORDINATE SYSTEM             //
	//-------------------------------------------//


OssiaAutomationCenter
{
	// defines the listenig point position and orientation
	var <ossiaOrient, <ossiaOrigine;
	var oX, oY, oZ;
	var <heading, <pitch, <roll;
	var <origine, <scale;

	*new { | parent_node, allCritical | ^super.new.ctr(parent_node, allCritical) }

	ctr
	{ | parent_node, allCritical, automation |

		ossiaOrient = OSSIA_Parameter(parent_node, "Orientation", OSSIA_vec3f,
			domain:[[-pi, -pi, -pi], [pi, pi, pi]], default_value:[0, 0, 0],
			bounding_mode:'wrap', critical:allCritical, repetition_filter:true);

		ossiaOrient.unit_(OSSIA_orientation.euler);

		heading = AutomationProxy(0.0);
		pitch = AutomationProxy(0.0);
		roll = AutomationProxy(0.0);

		origine = Cartesian();

		ossiaOrigine = OSSIA_Parameter(parent_node, "Origine", OSSIA_vec3f,
			domain:[[-10, -10, -10], [10, 10, 10]], default_value:[0, 0, 0],
			critical:allCritical, repetition_filter:true);

		ossiaOrigine.unit_(OSSIA_position.cart3D);

		oX = AutomationProxy(0.0);
		oY = AutomationProxy(0.0);
		oZ = AutomationProxy(0.0);

		scale = OssiaAutomationProxy(parent_node, "Scale_factor", Float,
			[0.01, 10],	1, 'clip', critical:allCritical);
	}

	setAction
	{ | sources |

		var halfPi = MoscaUtils.halfPi();

		ossiaOrient.callback_({ | num |

			sources.get.do({ | item |
				var euler = (item.coordinates.cartVal - origine)
				.rotate(num.value[0].neg)
				.tilt(num.value[1].neg)
				.tumble(num.value[2].neg)
				/ scale.value;

				item.coordinates.cartBack_(false);

				item.coordinates.azElDist.v_([(euler.theta - halfPi).wrap(-pi, pi).raddeg,
					euler.phi.raddeg, euler.rho]);

				item.coordinates.cartBack_(true);
			});

			if (heading.value != num[0].value) { heading.valueAction_(num[0].value) };

			if (pitch.value != num[1].value) { pitch.valueAction_(num[1].value) };

			if (roll.value != num[2].value) { roll.valueAction_(num[2].value) };
		});

		heading.action_({ | num | ossiaOrient.v_([num.value, pitch.value, roll.value]) });

		pitch.action_({ | num | ossiaOrient.v_([heading.value, num.value, roll.value]) });

		roll.action_({ | num | ossiaOrient.v_([heading.value, pitch.value, num.value]) });

		ossiaOrigine.callback_({ | num |

			origine.set(num[0].value, num[1].value, num[2].value);

			sources.get.do({ | item |

				var cart = (item.coordinates.cartVal - origine)
				.rotate(heading.value.neg)
				.tilt(pitch.value.neg)
				.tumble(roll.value.neg)
				/ scale.value;

				item.coordinates.cartBack_(false);

				item.coordinates.azElDist.v_([(cart.theta - halfPi).wrap(-pi, pi).raddeg,
					cart.phi.raddeg, cart.rho]);

				item.coordinates.cartBack_(true);
			});

			if (oX.value != num[0].value) { oX.valueAction = num[0].value };

			if (oY.value != num[1].value) { oY.valueAction = num[1].value };

			if (oZ.value != num[2].value) { oZ.valueAction = num[2].value };
		});

		oX.action_({ | num | ossiaOrigine.v_([num.value, oY.value, oZ.value]) });

		oY.action_({ | num | ossiaOrigine.v_([oX.value, num.value, oZ.value]) });

		oZ.action_({ | num | ossiaOrigine.v_([oX.value, oY.value, num.value]) });

		scale.node.callback_({ | v |

			sources.get.do({ | item |

			})
		})
	}

	dockTo
	{ | automation |

		oX.dockTo(automation, "oxProxy");
		oY.dockTo(automation, "oyProxy");
		oZ.dockTo(automation, "ozProxy");

		heading.dockTo(automation, "headingProxy");
		pitch.dockTo(automation, "pitchProxy");
		roll.dockTo(automation, "rollProxy");

		scale.dockTo(automation, "scaleProxy");
	}

	free
	{
		oX.free;
		oY.free;
		oZ.free;

		heading.free;
		pitch.free;
		roll.free;

		ossiaOrient.free;
		ossiaOrigine.free;

		scale.free;

		super.free;
	}
}

OssiaAutomationCoordinates
{
	// 3D value version for absolute and relative coordinatesiantes
	var <x, <y, <z, <cartesian, <azElDist;
	var <cartVal, <spheVal;
	var <>cartBack = true, <spheBack = true;

	*new
	{ | parent_node, allCritical, center, spatializer, synth |

		^super.new.ctr(parent_node, allCritical, center, spatializer, synth);
	}

	ctr
	{ | parent_node, allCritical, center, spatializer, synth |

		var halfPi = MoscaUtils.halfPi();

		cartesian = OSSIA_Parameter(parent_node, "Cartesian", OSSIA_vec3f,
			domain:[[-10, -10, -10], [10, 10, 10]], default_value:[0, 10, 0],
		critical:allCritical, repetition_filter:true);

		cartesian.unit_(OSSIA_position.cart3D);

		cartVal = Cartesian(0, 20, 0);
		spheVal = cartVal.asSpherical;

		x = AutomationProxy(0.0);
		y = AutomationProxy(20.0);
		z = AutomationProxy(0.0);

		azElDist = OSSIA_Parameter(parent_node, "AzElDist", OSSIA_vec3f,
			domain:[[-180, -90, 0], [180, 90, 20]], default_value:[0, 0, 10],
			critical:allCritical, repetition_filter:true);

		// azElDist.unit_(OSSIA_position.AzElDist);
	}

	setAction
	{ | center, spatializer, synth |

		var halfPi = MoscaUtils.halfPi();

		cartesian.callback_({ | num |
			var sphe, sphediff;
			cartVal.set(num.value[0], num.value[1], num.value[2]);
			sphe = (cartVal - center.origine)
			.rotate(center.heading.value.neg)
			.tilt(center.pitch.value.neg)
			.tumble(center.roll.value.neg);

			sphediff = [(sphe.theta - halfPi).wrap(-pi, pi).raddeg, sphe.phi.raddeg, sphe.rho];

			cartBack = false;

			if (spheBack) { azElDist.v_(sphediff) };

			if (x.value != num[0].value) { x.valueAction_(num[0].value) };

			if (y.value != num[1].value) { y.valueAction_(num[1].value) };

			if (z.value != num[2].value) { z.valueAction_(num[2].value) };

			cartBack = true;
		});

		x.action_({ | num |
			if (cartBack) { cartesian.v_([num.value, y.value, z.value]) };
		});

		y.action_({ | num |
			if (cartBack) { cartesian.v_([x.value, num.value, z.value]) };
		});

		z.action_({ | num |
			if (cartBack) { cartesian.v_([x.value, y.value, num.value]) };
		});

		azElDist.callback_({ | num |
			spheVal.rho_(num.value[2] * center.scale.value);
			spheVal.theta_((num.value[0].degrad.wrap(-pi, pi)) + halfPi);
			spheVal.phi_(num.value[1].degrad.clip(halfPi.neg, halfPi));
			spheBack = false;
			if (cartBack)
			{
				cartesian.v_(
					((spheVal.tumble(center.roll.value)
						.tilt(center.pitch.value)
						.rotate(center.heading.value)
						.asCartesian) + center.origine).asArray);
			};

			if(spatializer.get.notNil)
			{
				spatializer.get.set(\radAzimElev, [spheVal.rho, spheVal.theta, spheVal.phi]);
			};

			if (synth.get.notNil)
			{
				synth.get.do({ _.set(\radAzimElev, [spheVal.rho, spheVal.theta, spheVal.phi]); });
			};

			spheBack = true;
		});
	}

	dockTo
	{ | automation, index |

		x.dockTo(automation, "x_axisProxy_" ++ index);
		y.dockTo(automation, "y_axisProxy_" ++ index);
		z.dockTo(automation, "z_axisProxy_" ++ index);
	}

	free
	{
		x.free;
		y.free;
		z.free;

		cartesian.free;
		azElDist.free;

		super.free
	}
}
