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
//                 FREEVERB                  //
//-------------------------------------------//

FreeVerbDef : EffectDef
{
	classvar <key; // class access
	key { ^key } // instance access

	*initClass { key = "FreeVerb"; }

	getFunc
	{ | nChanns |

		switch(nChanns,
			1,
			{
				^{ | lrevRef, p, locallev, room, damp |
					lrevRef.value = FreeVerb.ar(p.value, mix: 1, room: room, damp: damp, mul: locallev);
				};
			},
			2,
			{
				^{ | lrevRef, p, locallev, room, damp |
					lrevRef.value = FreeVerb2.ar(p.value[0], p.value[1], mix: 1, room: room, damp: damp, mul: locallev);
				};
			},
			{
				^{ | lrevRef, p, locallev, room, damp |
					lrevRef.value = FreeVerb.ar(p.value[0], mix: 1, room: room, damp: damp, mul: locallev);
				};
		})
	}

	prFourChanGlobal
	{
		globalFunc = { | sig, room, damp |
			[
				FreeVerb.ar(sig[0], mix: 1, room: room, damp: damp),
				FreeVerb.ar(sig[1], mix: 1, room: room, damp: damp),
				FreeVerb.ar(sig[2], mix: 1, room: room, damp: damp),
				FreeVerb.ar(sig[3], mix: 1, room: room, damp: damp)
			].flat;
		};
	}

	prTwelveChanGlobal
	{
		globalFunc = { | sig, room, damp |
			[
				FreeVerb2.ar(sig[0], sig[1], mix: 1, room: room, damp: damp),
				FreeVerb2.ar(sig[2], sig[3], mix: 1, room: room, damp: damp),
				FreeVerb2.ar(sig[4], sig[5], mix: 1, room: room, damp: damp),
				FreeVerb2.ar(sig[6], sig[7], mix: 1, room: room, damp: damp),
				FreeVerb2.ar(sig[8], sig[9], mix: 1, room: room, damp: damp),
				FreeVerb2.ar(sig[10], sig[11], mix: 1, room: room, damp: damp)
			].flat;
		};
	}
}
