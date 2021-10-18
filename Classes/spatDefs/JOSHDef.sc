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
//                   JOSH                    //
//-------------------------------------------//

JOSHDef : SpatDef
{
	// class access
	const <channels = #[ 1, 2 ]; // possible number of channels
	classvar <format, <key;

	// instance access
	key { ^key; }
	format { ^format; }
	channels { ^channels; }

	*initClass
	{
		if (\HOALibEnc3D1.asClass.notNil) { defList = defList.add(this.asClass)};
		key = "HoaLib";
		format = \N3D;
	}

	getFunc { | maxOrder, renderer, nChanns |

		if (nChanns == 1)
		{
			var enc;

			if (maxOrder > 1) { enc = FMHEncode1 } { enc = BFEncode1 };

			^{ | lrevRef, p, rad, radRoot, azimuth, elevation, contract |
				var sig = distFilter.value(p.value, rad);
				sig = enc.ar(lrevRef.value + sig, azimuth, elevation,
					aten2distance.value(radRoot)); // invert to represnet distance
				lrevRef.value = (sig * contract) +
				Silent.ar(renderer.bFormNumChan - 1).addFirst(Mix(sig) * (1 - contract));
			};
		} {
			if (maxOrder > 1)
			{
				^{ | lrevRef, p, rad, radRoot, azimuth, elevation, contract, angle |
					var sig = distFilter.value(p.value, rad);
					sig = FMHEncode1.ar(lrevRef.value + sig, azimuth + (angle * (1 - rad)), elevation,
						aten2distance.value(radRoot))
					+ FMHEncode1.ar(lrevRef.value + sig, azimuth - (angle * (1 - rad)), elevation,
						aten2distance.value(radRoot)); // invert to represnet distance
					lrevRef.value = (sig * contract) +
					Silent.ar(renderer.bFormNumChan - 1).addFirst(Mix(sig) * (1 - contract));
				}
			} {
				^{ | lrevRef, p, rad, radRoot, azimuth, elevation, contract, angle |
					var sig = distFilter.value(p.value, rad) + lrevRef.value;
					sig = BFEncodeSter.ar(sig[0], sig[1], azimuth,
						angle * MoscaUtils.deg2rad,
						elevation, aten2distance.value(radRoot)); // invert to represnet distance
					lrevRef.value = (sig * contract) +
					Silent.ar(renderer.bFormNumChan - 1).addFirst(Mix(sig) * (1 - contract));
				}
			}
		};
	}
}