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
//                 ATK BASE                  //
//-------------------------------------------//

ATKBaseDef : SpatDef
{
	var <format, encoder; // specific variables

	getFunc
	{ | maxOrder, renderer, nChanns |

		switch (nChanns,
			1,
			{
				^{ | lrevRef, p, rad, radRoot, azimuth, elevation, contract, encoder |
					var sig = distFilter.value(p.value, rad);
					sig = lrevRef.value + (sig * atenuator.value(radRoot));
					sig = FoaEncode.ar(sig, encoder);
					sig.removeAt(0);
					sig = FoaTransform.ar(sig, 'push', MoscaUtils.halfPi * contract,
						CircleRamp.kr(azimuth, 0.1, -pi, pi), elevation);
					sig = HPF.ar(sig, 20); // stops bass frequency blow outs by proximity
					lrevRef.value = FoaTransform.ar(sig, 'proximity', rad * renderer.longestRadius);
				};
			},
			2,
			{
				^{ | lrevRef, p, rad, radRoot, azimuth, elevation, contract, angle, encoder |
					var l, r, sig = distFilter.value(p.value, rad),
					az = CircleRamp.kr(azimuth, 0.1, -pi, pi),
					contr = MoscaUtils.halfPi * contract;
					sig = lrevRef.value + (sig * atenuator.value(radRoot));
					l = FoaEncode.ar(sig[0], encoder);
					l.removeAt(0);
					r = FoaEncode.ar(sig[1], encoder);
					r.removeAt(0);
					l = FoaTransform.ar(l, 'push', contr, az + (angle * (1 - rad)), elevation);
					r = FoaTransform.ar(r, 'push', contr, az - (angle * (1 - rad)), elevation);
					sig = HPF.ar(l + r, 20); // stops bass frequency blow outs by proximity
					lrevRef.value = FoaTransform.ar(sig, 'proximity', rad * renderer.longestRadius);
				};
			},
			4,
			{ // assume FuMa input
				^{ | lrevRef, p, rad, radRoot, azimuth, elevation, contract, rotAngle, directang |
					var sig, pushang = 2 - (contract * 2);
					pushang = rad.linlin(pushang - 1, pushang, 0, MoscaUtils.halfPi);
					sig = FoaDirectO.ar(p.value * ((1/radRoot) - 1), directang);
					// directivity
					sig = FoaTransform.ar(sig, 'rotate', rotAngle);
					p.value = FoaTransform.ar(sig, 'push', pushang, azimuth, elevation);
				};
			},
			{ // assume N3D input
				var ord = (nChanns.sqrt) - 1;
				^{ | lrevRef, p, rad, radRoot, azimuth, elevation, contract, rotAngle, directang |
					var sig, pushang = 2 - (contract * 2);
					pushang = rad.linlin(pushang - 1, pushang, 0, MoscaUtils.halfPi);
					sig = FoaEncode.ar(p.value * ((1/radRoot) - 1), MoscaUtils.n2f);
					sig = FoaDirectO.ar(sig, directang);
					// directivity
					sig = FoaTransform.ar(sig, 'rotate', rotAngle);
					p.value = FoaTransform.ar(sig, 'push', pushang, azimuth, elevation);
				};
			}
		)
	}

	getArgs{ ^[\encoder, encoder] }
}

//-------------------------------------------//
//                   ATK                     //
//-------------------------------------------//

ATKDef : ATKBaseDef
{
	// class access
	const <channels = #[ 1, 2, 4, 16, 25, 36 ]; // possible number of channels
	classvar <key;

	// instance access
	key { ^key; }
	format { ^format; }
	channels { ^channels; }

	*initClass
	{
		defList = defList.add(this.asClass);
		key = "ATK";
	}

	prSetVars
	{ | maxOrder, renderer, server |
		format = \FUMA;
		encoder = FoaEncoderMatrix.newOmni();
	}
}

//-------------------------------------------//
//                ATK SPREAD                 //
//-------------------------------------------//

ATKDfDef : ATKBaseDef
{
	// class acces
	const <channels = #[ 1, 2 ]; // possible number of channels
	classvar <key;

	// instance access
	key { ^key; }
	format { ^format; }
	channels { ^channels; }

	*initClass
	{
		defList = defList.add(this.asClass);
		key = "ATK_sp";
	}

	prSetVars
	{ | maxOrder, renderer, server |
		format = \FUMA;
		encoder = FoaEncoderKernel.newSpread(subjectID: 6, kernelSize: 2048,
			server:server, sampleRate:server.sampleRate.asInteger);
	}
}

//-------------------------------------------//
//                 ATK DIFFUSE               //
//-------------------------------------------//

ATKSpDef : ATKBaseDef
{
	// class acces
	const <channels = #[ 1, 2 ]; // possible number of channels
	classvar <key;

	// instance access
	key { ^key; }
	format { ^format; }
	channels { ^channels; }

	*initClass
	{
		defList = defList.add(this.asClass);
		key = "ATK_df";
	}

	prSetVars
	{ | maxOrder, renderer, server |
		format = \FUMA;
		encoder = FoaEncoderKernel.newDiffuse(subjectID: 3, kernelSize: 2048,
			server:server, sampleRate:server.sampleRate.asInteger);
	}
}
