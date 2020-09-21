/*
* Mosca: SuperCollider class by Iain Mott, 2016. Licensed under a
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

FreeVerbDef : RevDefBase {
	classvar <localMonoReverbFunc, <localStereoReverbFunc, <defName;

	*initClass {

		defList = defList.add(this.asClass);

		defName = "FreeVerb";

		localMonoReverbFunc = { | lrevRef, p, rirWspectrum, locallev, room, damp |
			lrevRef.value = FreeVerb.ar(p, mix: 1, room: room, damp: damp, mul: locallev);
		};

		localStereoReverbFunc = { | lrev1Ref, lrev2Ref, p1, p2, rirZspectrum, locallev
			room = 0.5, damp = 0.5 |
			var temp;
			temp = FreeVerb2.ar(p1, p2, mix: 1, room: room, damp: damp, mul: locallev);
			lrev1Ref.value = temp[0];
			lrev2Ref.value = temp[1];
		};
	}

	prFourChanGlobal {

		globalRevFunc = { | sig, room, damp |
			^[
				FreeVerb.ar(sig[0], mix: 1, room: room, damp: damp),
				FreeVerb.ar(sig[1], mix: 1, room: room, damp: damp),
				FreeVerb.ar(sig[2], mix: 1, room: room, damp: damp),
				FreeVerb.ar(sig[3], mix: 1, room: room, damp: damp)
			].flat;
		};
	}

	prTwelveChanGlobal {

		globalRevFunc = { | sig, room, damp |
			^[
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

//-------------------------------------------//
//                  ALLPASS                  //
//-------------------------------------------//

PassRevDef : RevDefBase {
	classvar <localMonoReverbFunc, <localStereoReverbFunc, <defName;

	*initClass {

		defList = defList.add(this.asClass);

		defName = "AllPass";

		localMonoReverbFunc = { | lrevRef, p, rirWspectrum, locallev, room, damp |
			var temp = p;
			16.do({ temp = AllpassC.ar(temp, 0.08, room * { Rand(0, 0.08) } +
				{ Rand(0, 0.001) },
			damp * 2)});
			lrevRef.value = temp * locallev;
		};

		localStereoReverbFunc = { | lrev1Ref, lrev2Ref, p1, p2, rirZspectrum,
			locallev, room, damp |
			var temp1 = p1, temp2 = p2;
			8.do({ temp1 = AllpassC.ar(temp1, 0.08, room * { Rand(0, 0.08) } +
				{ Rand(0, 0.001) },
			damp * 2)});
			8.do({ temp2 = AllpassC.ar(temp2, 0.08, room * { Rand(0, 0.08) } +
				{ Rand(0, 0.001) },
			damp * 2)});
			lrev1Ref.value = temp1 * locallev;
			lrev2Ref.value = temp2 * locallev;
		};
	}

	prFourChanGlobal {

		globalRevFunc = { | sig, room, damp |
			16.do({ sig = AllpassC.ar(sig, 0.08, room * { Rand(0, 0.08) }.dup(4) +
				{ Rand(0, 0.001) },
			damp * 2)});
			^sig;
		};
	}

	prTwelveChanGlobal {

		globalRevFunc = { | sig, room, damp |
			16.do({ sig = AllpassC.ar(sig, 0.08, room * { Rand(0, 0.08) }.dup(12) +
				{ Rand(0, 0.001) },
			damp * 2)});
			^sig;
		};
	}
}

//-------------------------------------------//
//               CONVOLUTION                 //
//-------------------------------------------//

ConVerbDef : RevDefBase {
	classvar <localMonoReverbFunc, <localStereoReverbFunc, <defName;

	*initClass {

		defName = "Convolv";

		localMonoReverbFunc = { | lrevRef, p, rirWspectrum, locallev, room, damp |
			lrevRef.value = PartConv.ar(p, MoscaUtils.fftSize(), rirWspectrum, locallev);
		};

		localStereoReverbFunc = { | lrev1Ref, lrev2Ref, p1, p2, rirZspectrum, locallev
			room = 0.5, damp = 0.5 |
			var temp1 = p1, temp2 = p2;
			temp1 = PartConv.ar(p1, MoscaUtils.fftSize(), rirZspectrum, locallev);
			temp2 = PartConv.ar(p2, MoscaUtils.fftSize(), rirZspectrum, locallev);
			lrev1Ref.value = temp1 * locallev;
			lrev2Ref.value = temp2 * locallev;
		};
	}

	prFourChanGlobal {

		globalRevFunc = { | sig, room, damp, fluir, frdir, bldir, bruir |
			^[
				PartConv.ar(sig[0], MoscaUtils.fftSize(), fluir),
				PartConv.ar(sig[1], MoscaUtils.fftSize(), frdir),
				PartConv.ar(sig[2], MoscaUtils.fftSize(), bldir),
				PartConv.ar(sig[3], MoscaUtils.fftSize(), bruir)
			];
		};
	}

	prTwelveChanGlobal {

		globalRevFunc = { | sig, room, damp, a0ir, a1ir, a2ir, a3ir,
			a4ir, a5ir, a6ir, a7ir, a8ir, a9ir, a10ir, a11ir|
			^[
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

//-------------------------------------------//
//                 NOREVERB                  //
//-------------------------------------------//

NoRevDef : RevDefBase {
	classvar <localMonoReverbFunc, <localStereoReverbFunc, <defName;

	*initClass {

		defName = "no reverb";

		localMonoReverbFunc = { | lrevRef, p, rirWspectrum, locallev, room, damp| };

		localStereoReverbFunc = { | lrev1Ref, lrev2Ref, p1, p2, rirZspectrum,
			locallev, room, damp |
		};
	}
}

//-------------------------------------------//
//               Base Classe                 //
//-------------------------------------------//

RevDefBase {
	classvar <defList;
	var	<convolution = false, <multyThread;
	var <globalRevFunc;

	*initClass {
		Class.initClassTree(MoscaUtils);
	}

	*new { | maxOrder |

		^super.new.ctr(maxOrder);
	}

	ctr { | maxOrder |

		multyThread = Server.program.asString.endsWith("supernova");

		if (maxOrder == 1 ) {
			this.prFourChanMulty();
		}{
			this.prTwelveChanMulty();
		};
	}

	prFourChanMulty {}
	prTwelveChanMulty {}

}