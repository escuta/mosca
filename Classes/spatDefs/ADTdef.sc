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
//                    ADT                    //
//-------------------------------------------//

ADTDef : N3DDef
{
	// class access
	const <channels = #[ 1, 2 ];
	classvar <key;

	// instance access
	key { ^key; }
	channels { ^channels; }

	*initClass
	{
		if (\HOAmbiPanner1.asClass.notNil) { defList = defList.add(this.asClass); };
		key = "ADT";
	}

	getFunc
	{ | maxOrder, renderer, nChanns |

		if (nChanns == 1)
		{
			^{ | lrevRef, p, rad, radRoot, azimuth, elevation, contract |
				var sig = distFilter.value(p.value, rad);
				// attenuate high freq with distance
				sig = HOAmbiPanner.ar(maxOrder,
					lrevRef.value + (sig * atenuator.value(radRoot)),
					CircleRamp.kr(azimuth, 0.1, -pi, pi), Lag.kr(elevation));
				lrevRef.value = (sig * contract) +
				Silent.ar(renderer.bFormNumChan - 1).addFirst(Mix(sig) * (1 - contract));
			};
		} {
			^{ | lrevRef, p, rad, radRoot, azimuth, elevation, contract, angle |
				var az = CircleRamp.kr(azimuth, 0.1, -pi, pi),
				el = Lag.kr(elevation),
				sig = distFilter.value(p.value, rad);
				// attenuate high freq with distance
				sig = lrevRef.value + (sig * atenuator.value(radRoot));
				sig = HOAmbiPanner.ar(maxOrder,
					sig[0],
					az + (angle * (1 - rad)),
					el)
				+ HOAmbiPanner.ar(maxOrder,
					sig[1],
					az - (angle * (1 - rad)),
					el);
				lrevRef.value = (sig * contract) +
				Silent.ar(renderer.bFormNumChan - 1).addFirst(Mix(sig) * (1 - contract));
			};
		}
	}
}