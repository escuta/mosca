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


AutomationGuiProxy : QView
{
	var <>val, <>function, <>action;

	*new { | val | ^super.new.initAutomationProxy(val) }

	initAutomationProxy
	{ | ival |

		this.val = ival;
		this.bounds(Rect(0,0,0,0)); // set fake bounds to keep Automation happy!
	}

	value { ^this.val; }

	value_ { | value | this.val = value; }

	mapToGlobal
	{ | point |
		_QWidget_MapToGlobal
		^this.primitiveFailed;
	}

	absoluteBounds { ^this.bounds.moveToPoint(this.mapToGlobal( 0@0 )) }

	bounds { ^this.getProperty(\geometry) }

	bounds_ { | rect | this.setProperty(\geometry, rect.asRect) }

	doAction { this.action.value(this.val) }

	valueAction_ { | val | this.value_(val).doAction }
}

AutomationView : QView {
	// holds basic GUI specifics to keep Automation happy!
	*new { ^super.new.ctr() }

	ctr { this.bounds(Rect(0,0,0,0)) } // set fake bounds

	absoluteBounds { ^this.bounds.moveToPoint( this.mapToGlobal( 0@0 )) }

	mapToGlobal
	{ | point |
		_QWidget_MapToGlobal
		^this.primitiveFailed;
	}

	bounds { ^this.getProperty(\geometry) }

	bounds_ { | rect | this.setProperty(\geometry, rect.asRect);}
}

AutomationProxy //: AutomationView
// is the QView inheritence needed ?
{
	var val, >action;

	*new { | val | ^super.newCopyArgs(val) }

	value { ^val }

	value_ { | value | val = value }

	doAction { action.value(val) }

	valueAction_ { | val | this.value_(val).doAction }
}

OssiaAutomationProxy //: AutomationView
// is the QView inheritence needed ?
{
	// embed an OSSIA_Parameter in a View to be used with Automation
	// single value version
	var <node;

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
	var <ossiaOrigine, <ossiaOrient;
	var oX, oY, oZ;
	var <heading, <pitch, <roll;
	var <origine, <scale;

	*new { | parent_node, allCritical | ^super.new.ctr(parent_node, allCritical) }

	ctr
	{ | parent_node, allCritical, automation |

		origine = Cartesian();

		ossiaOrigine = OSSIA_Parameter(parent_node, "Origine", OSSIA_vec3f,
			domain:[[-20, -20, -20], [20, 20, 20]], default_value:[0, 0, 0],
			critical:allCritical, repetition_filter:true);

		ossiaOrigine.unit_(OSSIA_position.cart3D);

		oX = AutomationProxy(0.0);
		oY = AutomationProxy(0.0);
		oZ = AutomationProxy(0.0);

		ossiaOrient = OSSIA_Parameter(parent_node, "Orientation", OSSIA_vec3f,
			domain:[[-pi, -pi, -pi], [pi, pi, pi]], default_value:[0, 0, 0],
			bounding_mode:'wrap', critical:allCritical, repetition_filter:true);

		ossiaOrient.unit_(OSSIA_orientation.euler);

		heading = AutomationProxy(0.0);
		pitch = AutomationProxy(0.0);
		roll = AutomationProxy(0.0);

		scale = OssiaAutomationProxy(parent_node, "Scale_factor", Float,
			[0.01, 10],	1, 'clip', critical:allCritical);
	}

	setAction
	{ | sources |

		var halfPi = MoscaUtils.halfPi();

		ossiaOrigine.callback_({ | num |

			origine.set(num[0].value, num[1].value, num[2].value);

			sources.do({ | item |
				var cart = (item.coordinates.cartVal - origine)
				.rotate(heading.value.neg)
				.tilt(pitch.value.neg)
				.tumble(roll.value.neg)
				/ scale.value;

				item.coordinates.cartBack_(false);

				item.coordinates.azElDist.v_([cart.rho,
				(cart.theta - halfPi).wrap(-pi, pi), cart.phi]);

				item.coordinates.cartBack_(true);
			});

			if (oX.value != num[0].value) { oX.valueAction = num[0].value };

			if (oY.value != num[1].value) { oY.valueAction = num[1].value };

			if (oZ.value != num[2].value) { oZ.valueAction = num[2].value };
		});

		ossiaOrient.callback_({ | num |

			sources.do({ | item |
				var euler = (item.coordinates.cartVal - origine)
				.rotate(num.value[0].neg)
				.tilt(num.value[1].neg)
				.tumble(num.value[2].neg)
				/ scale.value;

				item.coordinates.cartBack_(false);

				item.coordinates.azElDist.v_([euler.rho,
					(euler.theta - halfPi).wrap(-pi, pi), euler.phi]);

				item.coordinates.cartBack_(true);
			});

			if (heading.value != num[0].value) { heading.valueAction_(num[0].value) };

			if (pitch.value != num[1].value) { pitch.valueAction_(num[1].value) };

			if (roll.value != num[2].value) { roll.valueAction_(num[2].value) };
		});

		oX.action_({ | num | ossiaOrigine.v_([num.value, oY.value, oZ.value]) });

		oY.action_({ | num | ossiaOrigine.v_([oX.value, num.value, oZ.value]) });

		oZ.action_({ | num | ossiaOrigine.v_([oX.value, oY.value, num.value]) });

		heading.action_({ | num | ossiaOrient.v_([num.value, pitch.value, roll.value]) });

		pitch.action_({ | num | ossiaOrient.v_([heading.value, num.value, roll.value]) });

		roll.action_({ | num | ossiaOrient.v_([heading.value, pitch.value, num.value]) });
	}

	dockTo
	{ | automation |

		automation.dock(oX, "oxProxy");
		automation.dock(oY, "oyProxy");
		automation.dock(oZ, "ozProxy");

		automation.dock(heading, "headingProxy");
		automation.dock(pitch, "pitchProxy");
		automation.dock(roll, "rollProxy");

		automation.dock(scale, "scaleProxy");
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
			domain:[[-20, -20, -20], [20, 20, 20]], default_value:[0, 20, 0],
		critical:allCritical, repetition_filter:true);

		cartesian.unit_(OSSIA_position.cart3D);

		cartVal = Cartesian(0, 20, 0);
		spheVal = cartVal.asSpherical;

		x = AutomationProxy(0.0);
		y = AutomationProxy(20.0);
		z = AutomationProxy(0.0);

		azElDist = OSSIA_Parameter(parent_node, "AzElDist", OSSIA_vec3f,
			domain:[[-180, -90, 0], [180, 90, 20]],default_value:[0, 0, 20],
			critical:allCritical, repetition_filter:true);

		// azElDist.unit_(OSSIA_position.AzElDist);

		this.setAction(center, spatializer, synth);
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

			if(spatializer.notNil)
			{
				spatializer.set(\radAzimElev, [spheVal.rho, spheVal.theta, spheVal.phi]);
			};

			if (synth.notNil)
			{
				synth.do({ _.set(\radAzimElev, [spheVal.rho, spheVal.theta, spheVal.phi]); });
			};

			spheBack = true;
		});
	}

	dockTo
	{ | automation, index |

		automation.dock(x, "x_axisProxy_" ++ index);
		automation.dock(y, "y_axisProxy_" ++ index);
		automation.dock(z, "z_axisProxy_" ++ index);
	}
}
