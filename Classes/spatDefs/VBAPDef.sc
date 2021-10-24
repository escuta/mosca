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

VBAPDef : SpatDef
{
	var vbapBuffer; // specific variables

	// class access
	const <channels = #[ 1, 2 ];
	classvar <format, <key;

	// instance access
	key { ^key; }
	format { ^format; }
	channels { ^channels; }

	*initClass
	{
		defList = defList.add(this.asClass);
		key = "VBAP";
		format = \NONAMBI;
	}

	prSetVars
	{ | maxOrder, renderer, server |
		vbapBuffer = renderer.vbapBuffer;
	}

	getArgs{ ^[\vbapBuffer, vbapBuffer.bufnum] }

	getFunc
	{ | maxOrder, renderer, nChanns |

		if (nChanns == 1)
		{
			^{ | lrevRef, p, rad, radRoot, azimuth, elevation, contract, vbapBuffer |
				var sig = distFilter.value(p.value, rad),
				azi = azimuth * MoscaUtils.rad2deg, // convert to degrees
				elev = elevation * MoscaUtils.rad2deg, // convert to degrees
				elevexcess = Select.kr(elev < renderer.lowestElevation, [0, elev.abs]);
				elevexcess = Select.kr(elev > renderer.highestElevation, [0, elev]);
				// get elevation overshoot
				elev = elev.clip(renderer.lowestElevation, renderer.highestElevation);
				// restrict between min & max
				lrevRef.value = VBAP.ar(renderer.numOutputs,
					lrevRef.value + (sig * atenuator.value(radRoot)),
					vbapBuffer, CircleRamp.kr(azi, 0.1, -180, 180), Lag.kr(elev),
					((1 - contract) + (elevexcess / 90)) * 100);
			};
		} {
			^{ | lrevRef, p, rad, radRoot, azimuth, elevation, contract, angle, vbapBuffer |
				var contr, sig = distFilter.value(p.value, rad),
				// convert to degrees
				azi = azimuth * MoscaUtils.rad2deg,
				elev = elevation * MoscaUtils.rad2deg,
				spread = angle * MoscaUtils.rad2deg,
				elevexcess = Select.kr(elev < renderer.lowestElevation, [0, elev.abs]);
				elevexcess = Select.kr(elev > renderer.highestElevation, [0, elev]);
				contr = ((1 - contract) + (elevexcess / 90)) * 100;
				azi = CircleRamp.kr(azi, 0.1, -180, 180);
				// get elevation overshoot
				elev = elev.clip(renderer.lowestElevation, renderer.highestElevation);
				// restrict between min & max
				elev = Lag.kr(elev);
				sig = lrevRef.value + (sig * atenuator.value(radRoot));
				lrevRef.value = VBAP.ar(renderer.numOutputs, sig[0], vbapBuffer,
					azi + (spread * (1 - rad)),
					elev, contr) +
				VBAP.ar(renderer.numOutputs, sig[1], vbapBuffer,
					azi - (spread * (1 - rad)),
					elev, contr);
			};
		}
	}
}
