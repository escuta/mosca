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
//                     ATK                   //
//-------------------------------------------//

ATKDef : SpatDef {
	classvar <format, <key; // class access
	const <channels = #[ 1, 2, 4, 16, 25, 36 ]; // possible number of channels

	// instance access
	key { ^key; }
	format { ^format; }

	*initClass
	{
		defList = defList.add(this.asClass);
		key = "ATK";
		format = \FUMA;
	}

	getFunc
	{ | maxOrder, renderer, nChanns |

		switch (nChanns,
			1,
			{
				^{ | lrevRef, p, rad, radRoot, azimuth, elevation, contract |
					var sig = distFilter.value(p, rad);
					sig = lrevRef.value + (sig * atenuator.value(radRoot));
					sig = FoaEncode.ar(sig, FoaEncoderMatrix.newOmni());
					sig = FoaTransform.ar(sig, 'push', MoscaUtils.halfPi * contract,
						CircleRamp.kr(azimuth, 0.1, -pi, pi), elevation);
					sig = HPF.ar(sig, 20); // stops bass frequency blow outs by proximity
					lrevRef.value = FoaTransform.ar(sig, 'proximity', rad * renderer.longestRadius);
				};
			},
			2,
			{
				^{ | lrevRef, p, rad, radRoot, azimuth, elevation, contract, angle |
					var sig = distFilter.value(p, rad);
					sig = lrevRef.value + (sig * atenuator.value(radRoot));
					sig = FoaEncode.ar(sig, FoaEncoderMatrix.newStereo(angle * MoscaUtils.deg2rad));
					sig = FoaTransform.ar(sig, 'push', MoscaUtils.halfPi * contract,
						CircleRamp.kr(azimuth, 0.1, -pi, pi), elevation);
					sig = HPF.ar(sig, 20); // stops bass frequency blow outs by proximity
					lrevRef.value = FoaTransform.ar(sig, 'proximity', rad * renderer.longestRadius);
				};
			},
			4,
			{ // assume FuMa input
				^{ | lrevRef, p, rad, radRoot, azimuth, elevation, contract, rotAngle, directang |
					var pushang = 2 - (contract * 2);
					pushang = rad.linlin(pushang - 1, pushang, 0, MoscaUtils.halfPi);
					p = FoaDirectO.ar(p * ((1/radRoot) - 1), directang);
					// directivity
					p = FoaTransform.ar(p, 'rotate', rotAngle);
					p = FoaTransform.ar(p, 'push', pushang, azimuth, elevation);
				};
			},
			{ // assume N3D input
				var ord = (nChanns.sqrt) - 1;
				^{ | lrevRef, p, rad, radRoot, azimuth, elevation, contract, rotAngle, directang |
					var pushang = 2 - (contract * 2);
					pushang = rad.linlin(pushang - 1, pushang, 0, MoscaUtils.halfPi);
					p = FoaEncode.ar(p * ((1/radRoot) - 1), MoscaUtils.n2f);
					p = FoaDirectO.ar(p, directang);
					// directivity
					p = FoaTransform.ar(p, 'rotate', rotAngle);
					p = FoaTransform.ar(p, 'push', pushang, azimuth, elevation);
				};
			}
		)
	}
}