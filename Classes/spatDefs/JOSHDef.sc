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

JOSHDef : FumaDef
{
	// class access
	const <channels = #[ 1, 2 ]; // possible number of channels
	classvar <key;

	// instance access
	key { ^key; }
	channels { ^channels; }

	*initClass
	{
		defList = defList.add(this.asClass);
		key = "Josh";
	}

	getFunc { | maxOrder, renderer, nChanns |

		if (nChanns == 1)
		{
			^{ | lrevRef, p, rad, radRoot, azimuth, elevation, contract, winsize, grainrate, winrand |
				var azelwinrand = 1 - contract,
				sig = distFilter.value(p.value, rad);
				lrevRef.value = MonoGrainBF.ar(lrevRef.value + sig, winsize, grainrate, winrand,
					azimuth, azelwinrand, elevation, azelwinrand,
					rho: aten2distance.value(radRoot), // invert to represent distance
					mul: ((0.5 - winsize) + (1 - (grainrate / 40))).clip(0, 1) * 0.5);
			}
		} {
			^{ | lrevRef, p, rad, radRoot, azimuth, elevation, contract, angle winsize, grainrate, winrand |
				var azelwinrand = 1 - contract,
				rho = aten2distance.value(radRoot), // invert to represent distance
				mul = ((0.5 - winsize) + (1 - (grainrate / 40))).clip(0, 1) * 0.5,
				sig = distFilter.value(p.value, rad);
				sig = lrevRef.value + sig;
				lrevRef.value = MonoGrainBF.ar(sig[0], winsize, grainrate, winrand,
					azimuth + (angle * (1 - rad)), azelwinrand, elevation, azelwinrand,
					rho: rho, mul: mul)
				+ MonoGrainBF.ar(sig[1], winsize, grainrate, winrand,
					azimuth + (angle * (1 - rad)), azelwinrand, elevation, azelwinrand,
					rho: rho, mul: mul);
			}
		};
	}
}