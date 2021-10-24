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
//                  ALLPASS                  //
//-------------------------------------------//

AllPassDef : EffectDef
{
	classvar <key; // class access
	key { ^key } // instance access

	*initClass { key = "AllPass"; }

	getFunc
	{ | nChanns |

		switch (nChanns,
			1,
			{
				^{ | lrevRef, p, rad, llev, room, damp |
					var temp = p.value;
					16.do({ temp = AllpassC.ar(temp, 0.08, room * { Rand(0, 0.08) } +
						{ Rand(0, 0.001) },
						damp * 2)});
					lrevRef.value = temp * (rad * llev);
				};
			},
			2,
			{
				^{ | lrevRef, p, rad, llev, room, damp |
					var temp1 = p.value[0], temp2 = p.value[1];
					8.do({ temp1 = AllpassC.ar(temp1, 0.08, room * { Rand(0, 0.08) } +
						{ Rand(0, 0.001) },
						damp * 2)});
					8.do({ temp2 = AllpassC.ar(temp2, 0.08, room * { Rand(0, 0.08) } +
						{ Rand(0, 0.001) },
						damp * 2)});
					lrevRef.value = [temp1, temp2] * (rad * llev);
				};
			},
			{
				^{ | lrevRef, p, rad, llev, room, damp |
					var temp = p.value[0];
					16.do({ temp = AllpassC.ar(temp, 0.08, room * { Rand(0, 0.08) } +
						{ Rand(0, 0.001) },
						damp * 2)});
					lrevRef.value = temp * (rad * llev);
				};
		})
	}

	prFourChanGlobal
	{
		globalFunc = { | sig, room, damp |
			16.do({ sig = AllpassC.ar(sig, 0.08, room * { Rand(0, 0.08) }.dup(4) +
				{ Rand(0, 0.001) }, damp * 2)});
			sig;
		};
	}

	prTwelveChanGlobal
	{
		globalFunc = { | sig, room, damp |
			16.do({ sig = AllpassC.ar(sig, 0.08, room * { Rand(0, 0.08) }.dup(12) +
				{ Rand(0, 0.001) }, damp * 2)});
			sig;
		};
	}
}
