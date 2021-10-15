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
//                 ATK SPREAD                //
//-------------------------------------------//

ATKSpDef : ATKDef {
	const <channels = #[ 1, 2 ]; // possible number of channels
	var foaEncoder; // specific variables

	*initClass
	{
		defList = defList.add(this.asClass);
		key = "ATK_sp";
		format = \FUMA;
	}

	prSetVars{ | maxOrder, renderer, server |
		foaEncoder = FoaEncoderKernel.newSpread(subjectID: 6, kernelSize: 2048,
			server:server, sampleRate:server.sampleRate.asInteger);
	}

	getFunc
	{ | maxOrder, renderer, nChanns |

		if (nChanns == 1)
		{
			^{ | lrevRef, p, rad, radRoot, azimuth, elevation, contract, foaEncoder |
				var sig = distFilter.value(p, rad);
				sig = distFilter.value(p, rad);
				sig = lrevRef.value + (sig * atenuator.value(radRoot));
				sig = FoaEncode.ar(sig, foaEncoder);
				sig = FoaTransform.ar(sig, 'push', MoscaUtils.halfPi * contract,
					CircleRamp.kr(azimuth, 0.1, -pi, pi), elevation);
				sig = HPF.ar(sig, 20); // stops bass frequency blow outs by proximity
				lrevRef.value = FoaTransform.ar(sig, 'proximity', rad * renderer.longestRadius);
			};
		} {
			^{ | lrevRef, p, rad, radRoot, azimuth, elevation, contract, angle, foaEncoder |
				var l, r, sig = distFilter.value(p, rad),
				az = CircleRamp.kr(azimuth, 0.1, -pi, pi),
				contr = MoscaUtils.halfPi * contract;
				sig = distFilter.value(p, rad);
				sig = lrevRef.value + (sig * atenuator.value(radRoot));
				l = FoaEncode.ar(sig[0], foaEncoder);
				r = FoaEncode.ar(sig[1], foaEncoder);
				l = FoaTransform.ar(l, 'push', contr, az + (angle * (1 - rad)), elevation);
				r = FoaTransform.ar(r, 'push', contr, az - (angle * (1 - rad)), elevation);
				sig = HPF.ar(l + r, 20); // stops bass frequency blow outs by proximity
				lrevRef.value = FoaTransform.ar(sig, 'proximity', rad * renderer.longestRadius);
			};
		}
	}
}

//-------------------------------------------//
//                 ATK DIFFUSE               //
//-------------------------------------------//

ATKDfDef : ATKSpDef {

	*initClass
	{
		defList = defList.add(this.asClass);
		key = "ATK_df";
		format = \FUMA;
	}

	prSetVars{ | maxOrder, renderer, server |
		foaEncoder = FoaEncoderKernel.newDiffuse(subjectID: 3, kernelSize: 2048,
			server:server, sampleRate:server.sampleRate.asInteger);
	}
}