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
//                    VBAP                   //
//-------------------------------------------//

VBAPDef : SpatDef {
	classvar <format, <key; // class access

	// instance access
	key { ^key; }
	format { ^format; }

	*initClass
	{
		defList = defList.add(this.asClass);
		key = "VBAP";
		format = \NONAMBI;
	}

	getFunc
	{ | maxOrder, renderer, nChanns |

		if (nChanns == 1)
		{
			^{ | lrevRef, p, rad, radRoot, azimuth, elevation, contract, vbapBuffer |
				var sig = distFilter.value(p, rad),
				azi = azimuth * MoscaUtils.rad2deg, // convert to degrees
				elev = elevation * MoscaUtils.rad2deg, // convert to degrees
				elevexcess = Select.kr(elev < renderer.lowestElevation, [0, elev.abs]);
				elevexcess = Select.kr(elev > renderer.highestElevation, [0, elev]);
				// get elevation overshoot
				elev = elev.clip(renderer.lowestElevation, renderer.highestElevation);
				// restrict between min & max
				lrevRef.value = VBAP.ar(renderer.numOutputs,
					lrevRef.value + (sig * atenuator.value(radRoot)),
					vbapBuffer.bufnum, CircleRamp.kr(azi, 0.1, -180, 180), Lag.kr(elev),
					((1 - contract) + (elevexcess / 90)) * 100);
			};
		} {
			^{ | lrevRef, p, rad, radRoot, azimuth, elevation, contract, angle, vbapBuffer |
				var sig = distFilter.value(p, rad),
				azi = azimuth * MoscaUtils.rad2deg, // convert to degrees
				elev = elevation * MoscaUtils.rad2deg, // convert to degrees
				contr = ((1 - contract) + (elevexcess / 90)) * 100,
				elevexcess = Select.kr(elev < renderer.lowestElevation, [0, elev.abs]);
				elevexcess = Select.kr(elev > renderer.highestElevation, [0, elev]);
				azi = CircleRamp.kr(azi, 0.1, -180, 180);
				// get elevation overshoot
				elev = elev.clip(renderer.lowestElevation, renderer.highestElevation);
				// restrict between min & max
				elev = Lag.kr(elev);
				lrevRef.value = VBAP.ar(renderer.numOutputs,
					lrevRef.value + (sig * atenuator.value(radRoot)),
					vbapBuffer.bufnum, azi, elev, contr);
			};
		}
	}
}
