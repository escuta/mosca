/*
Mosca: SuperCollider class by Iain Mott, 2016. Licensed under a
Creative Commons Attribution-NonCommercial 4.0 International License
http://creativecommons.org/licenses/by-nc/4.0/
The class makes extensive use of the Ambisonic Toolkit (http://www.ambisonictoolkit.net/)
by Joseph Anderson and the Automation quark
(https://github.com/neeels/Automation) by Neels Hofmeyr.
Required Quarks : Automation, Ctk, XML and  MathLib
Required classes:
SC Plugins: https://github.com/supercollider/sc3-plugins
User must set up a project directory with subdirectoties "rir" and "auto"
RIRs should have the first 100 or 120ms silenced to act as "tail" reverberators
and must be placed in the "rir" directory.
Run help on the "Mosca" class in SuperCollider for detailed information
and code examples. Further information and sample RIRs and B-format recordings
may be downloaded here: http://escuta.org/mosca
*/


AutomationGuiProxy : QView {
	var <>val, <>function, <>action;

	*new { | val |
		^super.new.initAutomationProxy(val);
	}

	initAutomationProxy { | ival |
		this.val = ival;
		this.bounds(Rect(0,0,0,0)); // set fake bounds to keep Automation happy!
	}

	value { ^this.val; }

	value_ { | value | this.val = value; }

	mapToGlobal { | point |
		_QWidget_MapToGlobal
		^this.primitiveFailed;
	}

	absoluteBounds {
		^this.bounds.moveToPoint( this.mapToGlobal( 0@0 ); );
	}

	bounds { ^this.getProperty(\geometry); }

	bounds_ { | rect | this.setProperty(\geometry, rect.asRect); }

	doAction { this.action.value(this.val); }

	valueAction_ { |val| this.value_(val).doAction; }
}

AutomationView : QView {
	// holds basic GUI specifics to keep Automation happy!
	*new {
		^super.new.ctr();
	}

	ctr {
		this.bounds(Rect(0,0,0,0)); // set fake bounds
	}

	absoluteBounds {
		^this.bounds.moveToPoint( this.mapToGlobal( 0@0 ); );
	}

	mapToGlobal { | point |
		_QWidget_MapToGlobal
		^this.primitiveFailed;
	}

	bounds { ^this.getProperty(\geometry); }

	bounds_ { | rect | this.setProperty(\geometry, rect.asRect); }
}

AutomationProxy : AutomationView {
	var val, action;

	*new { | val |
		^super.newCopyArgs(val);
	}

	value { ^val; }

	value_ { | value | val = value; }

	doAction { action.value(val); }

	valueAction_ { |val| this.value_(val).doAction; }
}

OssiaAutomationProxy : AutomationView {
	// embed an OSSIA_Parameter in a View to be used with Automation
	// single value version
	var <>param;

	*new { | parent_node, name, type, domain, default_value, bounding_mode = 'free', critical = false, repetition_filter = true |
		^super.new.ctr(parent_node, name, type, domain, default_value, bounding_mode, critical, repetition_filter);
	}

	ctr { | parent_node, name, type, domain, default_value, bounding_mode, critical, repetition_filter |
		param = OSSIA_Parameter(parent_node, name, type, domain, default_value, bounding_mode, critical, repetition_filter);
	}

	value { ^param.v; }

	value_ { | value | param.v_(value); }

	action { ^param.callback; }

	action_ { | function | param.callback_(function); }
}

OssiaAutomatCenter {
	// embed an OSSIA_Parameter in a View to be used with Automation
	// 3D value version for absolute and relative coordiantes
	var <ossiaOrigine, <ossiaOrient;
	var oX, oY, oZ;
	var heading, pitch, roll;
	var origine, scale;

	*new { | parent_node, allCrtitical |
		^super.new.ctr(parent_node, allCrtitical);
	}

	ctr { | parent_node, allCrtitical |

		ossiaOrigine = OSSIA_Parameter(parent_node, "Origine", OSSIA_vec3f,
			domain:[[-20, -20, -20], [20, 20, 20]], default_value:[0, 0, 0],
			critical:allCrtitical, repetition_filter:true);

		ossiaOrigine.unit_(OSSIA_position.cart3D);

		oX = AutomationProxy(0.0);
		oY = AutomationProxy(0.0);
		oZ = AutomationProxy(0.0);

		ossiaOrient = OSSIA_Parameter(parent_node, "Orientation", OSSIA_vec3f,
			domain:[[-pi, -pi, -pi], [pi, pi, pi]], default_value:[0, 0, 0],
			bounding_mode:'wrap', critical:allCrtitical, repetition_filter:true);

		ossiaOrient.unit_(OSSIA_orientation.euler);

		this.setAction();
	}

	setActions {
		var halfPi = MoscaUtils.halfPi();
	}
}

OssiaAutomatCoordinates {
	// embed an OSSIA_Parameter in a View to be used with Automation
	// 3D value version for absolute and relative coordiantes
	var espacializador, synth;
	var <x, <y, <z, <cart, <sphe;
	var cartval, cartBack, spheval, spheBack;

	*new { | espacializador, synth, parent_node, allCrtitical, origine, scale, heading, pitch, roll |
		^super.newCopyrgs(espacializador, synth).ctr(parent_node, allCrtitical, origine, heading, pitch, roll);
	}

	ctr { | parent_node, allCrtitical, origine, scale, heading, pitch, roll |
		var halfPi = MoscaUtils.halfPi();

		cart = OSSIA_Parameter(parent_node, "Cartesian", OSSIA_vec3f,
			domain:[[-20, -20, -20], [20, 20, 20]], default_value:[0, 20, 0],
		critical:allCrtitical, repetition_filter:true);

		cart.unit_(OSSIA_position.cart3D);

		cartval = Cartesian(0, 20, 0);
		spheval = cartval.asSpherical;

		x = AutomationProxy(0.0);
		y = AutomationProxy(20.0);
		z = AutomationProxy(0.0);

		sphe = OSSIA_Parameter(parent_node, "Spherical", OSSIA_vec3f,
			domain:[[0, -pi, halfPi.neg], [20, pi, halfPi]],
		default_value:[20, 0, 0], critical:allCrtitical, repetition_filter:true);

		sphe.unit_(OSSIA_position.spherical);

		this.setAction(origine, scale, heading, pitch, roll);
	}

	setActions { | origine, scale, heading, pitch, roll |
		var halfPi = MoscaUtils.halfPi();

		cart.callback_({arg num;
			var sphe, sphediff;
			cartval.set(num.value[0], num.value[1], num.value[2]);
			sphe = (cartval - origine)
			.rotate(heading.value.neg)
			.tilt(pitch.value.neg)
			.tumble(roll.value.neg);

			sphediff = [sphe.rho, (sphe.theta - halfPi).wrap(-pi, pi), sphe.phi];

			cartBack = false;

			if (spheBack && (sphe.v != sphediff)) {
				sphe.v_(sphediff);
			};

			if (x.value != num[0].value) {
				x.valueAction_(num[0].value);
			};
			if (y.value != num[1].value) {
				y.valueAction_(num[1].value);
			};
			if (z.value != num[2].value) {
				z.valueAction_(num[2].value);
			};

			cartBack = true;
		});

		x.action_({ | num |
			if (cartBack && (cart.v[0] != num.value)) {
				cart.v_([num.value, y.value,
					z.value]);
			};
		});

		y.action_({ | num |
			if (cartBack && (cart.v[1] != num.value)) {
				cart.v_([x.value, num.value,
					z.value]);
			};
		});

		z.action_({ | num |
			if (cartBack && (cart.v[2] != num.value)) {
				cart.v_([x.value, y.value,
					num.value]);
			};
		});

		sphe.callback_({arg num;
			spheval.rho_(num.value[0] * scale.v);
			spheval.theta_(num.value[1].wrap(-pi, pi) + halfPi);
			spheval.phi_(num.value[2].fold(halfPi.neg, halfPi));
			spheBack = false;
			if (cartBack) {
				cart.v_(
					((spheval.tumble(roll.value)
						.tilt(pitch.value)
						.rotate(heading.value)
						.asCartesian) + origine).asArray);
			};

			if(espacializador.notNil) {
				espacializador.set(\radius, spheval.rho, \azim, spheval.theta, \elev, spheval.phi);
			};

			if (synth.notNil) {
				synth.do({ _.set(\radius, spheval.rho, \azim, spheval.theta, \elev, spheval.phi); });
			};

			spheBack = true;
		});
	}
}
