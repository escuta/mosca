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
//               CONVOLUTION                 //
//-------------------------------------------//

ConvolutionDef : EffectDef
{
	classvar <key; // class access
	key { ^key } // instance access

	*initClass { key = "Conv"; }

	getFunc
	{ | nChanns |

		switch (nChanns,
			1,
			{
				^{ | lrevRef, p, rad, llev, wir |
					lrevRef.value = PartConv.ar(p.value, MoscaUtils.fftSize(), wir,
						rad * llev);
				};
			},
			2,
			{
				^{ | lrevRef, p, rad, llev, zir |
					var temp1 = p.value[0], temp2 = p.value[1];
					temp1 = PartConv.ar(temp1, MoscaUtils.fftSize(), zir);
					temp2 = PartConv.ar(temp2, MoscaUtils.fftSize(), zir);
					lrevRef.value = [temp1, temp2] * (rad * llev);
				}
			},
			{
				^{ | lrevRef, p, rad, llev, wir |
					lrevRef.value = PartConv.ar(p.value[0], MoscaUtils.fftSize(), wir,
						rad * llev);
				};
			}
		)
	}

	prFourChanGlobal
	{
		globalFunc = { | sig, a0ir, a1ir, a2ir, a3ir |
			[
				PartConv.ar(sig[0], MoscaUtils.fftSize(), a0ir),
				PartConv.ar(sig[1], MoscaUtils.fftSize(), a1ir),
				PartConv.ar(sig[2], MoscaUtils.fftSize(), a2ir),
				PartConv.ar(sig[3], MoscaUtils.fftSize(), a3ir)
			];
		};
	}

	prTwelveChanGlobal
	{
		globalFunc = { | sig, a0ir, a1ir, a2ir, a3ir,
		a4ir, a5ir, a6ir, a7ir, a8ir, a9ir, a10ir, a11ir |
			[
				PartConv.ar(sig[0], MoscaUtils.fftSize(), a0ir),
				PartConv.ar(sig[1], MoscaUtils.fftSize(), a1ir),
				PartConv.ar(sig[2], MoscaUtils.fftSize(), a2ir),
				PartConv.ar(sig[3], MoscaUtils.fftSize(), a3ir),
				PartConv.ar(sig[4], MoscaUtils.fftSize(), a4ir),
				PartConv.ar(sig[5], MoscaUtils.fftSize(), a5ir),
				PartConv.ar(sig[6], MoscaUtils.fftSize(), a6ir),
				PartConv.ar(sig[7], MoscaUtils.fftSize(), a7ir),
				PartConv.ar(sig[8], MoscaUtils.fftSize(), a8ir),
				PartConv.ar(sig[9], MoscaUtils.fftSize(), a9ir),
				PartConv.ar(sig[10], MoscaUtils.fftSize(), a10ir),
				PartConv.ar(sig[11], MoscaUtils.fftSize(), a11ir)
			];
		};
	}
}
