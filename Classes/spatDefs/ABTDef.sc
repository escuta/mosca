/*
* Mosca: SuperCollider class by Iain Mott, 2016 and Thibaud Keller, 2018. Licensed under a
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

//-------------------------------------------//
//                 AMBITOOLS                 //
//-------------------------------------------//

ABTDef : SpatDef {
	classvar <format, <key; // class access
	const maxRadius = 50, // maximum radius accepeted by ambitools
	<channels = #[ 1, 2, 4, 16, 25, 36 ]; // possible number of channels
	var lim, converge; // specific variables

	// instance access
	key { ^key; }
	format { ^format; }

	*initClass
	{
		defList = defList.add(this.asClass);
		key = "Ambitools";
		format = \N3D;
	}

	prSetVars
	{ | maxOrder, renderer, server |

		lim = maxRadius / (maxRadius + renderer.longestRadius); // radRoot value at the maxRadius
		converge = { | radRoot | atenuator.value(radRoot.linlin(lim, 1, 0.5, 1)) };
	}

	getFunc
	{ | maxOrder, renderer, nChanns |

		switch (nChanns,
			1,
			{
				^{ | lrevRef, p, radRoot, azimuth, elevation, contract |
					var distance = aten2distance.value(radRoot.min(lim)),
					sig = (lrevRef.value * distance) // local reverb make up gain
						+ (p * converge.value(radRoot));
					sig = HOAEncoder.ar(maxOrder,
						sig,
						CircleRamp.kr(azimuth, 0.1, -pi, pi),
						Lag.kr(elevation),
						0, // gain
						1, // spherical
						distance * renderer.longestRadius,
						renderer.longestRadius);
					lrevRef.value = (sig * contract) +
					Silent.ar(renderer.bFormNumChan - 1).addFirst(Mix(sig) * (1 - contract));
				};
			},
			2,
			{
				^{ | lrevRef, p, rad, radRoot, azimuth, elevation, contract, angle |
					var distance = aten2distance.value(radRoot.min(lim)),
					az = CircleRamp.kr(azimuth, 0.1, -pi, pi),
					el = Lag.kr(elevation),
					r = distance * renderer.longestRadius,
					sr = renderer.longestRadius,
					sig = (lrevRef.value * distance) // local reverb make up gain
						+ (p * converge.value(radRoot));
					sig = HOAEncoder.ar(maxOrder,
						sig[0],
						az + (angle * (1 - rad)),
						el,
						0, // gain
						1, // spherical
						r, sr)
					+ HOAEncoder.ar(maxOrder,
						sig[1],
						az - (angle * (1 - rad)),
						el,
						0, // gain
						1, // spherical
						r, sr);
					lrevRef.value = (sig * contract) +
					Silent.ar(renderer.bFormNumChan - 1).addFirst(Mix(sig) * (1 - contract));
				};
			},
			4,
			{ // assume FuMa input
				^{ | lrevRef, p, rad, radRoot, azimuth, elevation, contract, rotAngle |
					var pushang = 2 - (contract * 2);
					pushang = rad.linlin(pushang - 1, pushang, 0, MoscaUtils.halfPi);
					p = FoaDecode.ar(p * ((1/radRoot) - 1), MoscaUtils.f2n);
					p = HOATransRotateAz.ar(1, p, rotAngle);
					p = HOABeamDirac2Hoa.ar(1, p, azimuth, elevation, timer_manual:1, focus:pushang);
				};
			},
			{ // assume N3D input
				var ord = (nChanns.sqrt) - 1;
				^{ | lrevRef, p, rad, radRoot, azimuth, elevation, contract, rotAngle |
					var pushang = 2 - (contract * 2);
					pushang = rad.linlin(pushang - 1, pushang, 0, MoscaUtils.halfPi);
					p = HOATransRotateAz.ar(ord, lrevRef.value +
						(p * ((1/radRoot) - 1)), rotAngle);
					p = HOABeamDirac2Hoa.ar(ord, p, azimuth, elevation, timer_manual:1, focus:pushang);
				};
			}
		)
	}
}