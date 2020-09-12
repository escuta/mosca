/*
Mosca: SuperCollider class by Iain Mott, 2016. Licensed under a
Creative Commons Attribution-NonCommercial 4.0 International License
http://creativecommons.org/licenses/by-nc/4.0/
The class makes extensive use of the Ambisonic Toolkit (http://www.ambisonictoolkit.net/)
by Joseph Anderson and the Automation quark
(https://github.com/neeels/Automation) by Neels Hofmeyr.
Required Quarks : Automation, Ctk, XML and  MathLib
Required classes:
SC Plugins: https://github.com/supercollider/sc3-plugins
User must set up a project directory with subdirectoties "rir" and "auto"
RIRs should have the first 100 or 120ms silenced to act as "tail" reverberators
and must be placed in the "rir" directory.
Run help on the "Mosca" class in SuperCollider for detailed information
and code examples. Further information and sample RIRs and B-format recordings
may be downloaded here: http://escuta.org/mosca
*/

MoscaRevDefs {
	const fftSize = 2048;

	classvar <wSpecPar, <zSpecPar, <wxyzSpecPar, <irSpecPar;
	classvar <rirList, <localReverbFunc;

	var <gBus, <gBfBus, <gBixBus;

	*new { | server, maxorder, renderer, rirBank |

		^super.ctr(server, maxorder, renderer, rirBank);
	}

	ctr { | server, maxorder, renderer, rirBank |

		gBus = Bus.audio(server, 1); // global reverb bus
		gBfBus = Bus.audio(server, MoscaUtils.fourOrNine); // global b-format bus
		gBixBus = Bus.audio(server, MoscaUtils.fourOrNine); // global n3d b-format bus

		// if (Server.program.asString.endsWith("supernova")) {
		//
		// 	if (maxorder == 1) {
		//
		// 		SynthDef("revGlobalAmb_in", { | gate = 1, room = 0.5, damp = 0.5 |
		// 			var env, temp, convsig, sig, sigx, sigf = In.ar(gBfBus, 4);
		// 			sigx = In.ar(gBixBus, 4);
		// 			sig = In.ar(gBus, 1);
		// 			sigx = FoaEncode.ar(sigx, MoscaUtils.n2f);
		// 			env = EnvGen.kr(Env.asr(1), gate, doneAction:2);
		// 			sig = sig + sigf + sigx;
		// 			sig = FoaDecode.ar(sig, MoscaUtils.b2a);
		// 			16.do({ sig = AllpassC.ar(sig, 0.08, room * { Rand(0, 0.08) }.dup(4) +
		// 				{ Rand(0, 0.001) },
		// 			damp * 2)});
		// 			sig = FoaEncode.ar(sig, MoscaUtils.a2b);
		// 			sig = sig * env;
		// 			Out.ar(renderer.fumaBus, sig);
		// 		}).send(server);
		//
		//
		// 		SynthDef("parallelVerb_pass", { | gate = 1, room = 0.5, damp = 0.5 |
		// 			var env, temp, convsig, sig, sigx, sigf = In.ar(gBfBus, 4);
		// 			sigx = In.ar(gBixBus, 4);
		// 			sig = In.ar(gBus, 1);
		// 			sigx = FoaEncode.ar(sigx, MoscaUtils.n2f);
		// 			env = EnvGen.kr(Env.asr(1), gate, doneAction:2);
		// 			sig = sig + sigf + sigx;
		// 			sig = FoaDecode.ar(sig, MoscaUtils.b2a);
		// 			16.do({ sig = AllpassC.ar(sig, 0.08, room * { Rand(0, 0.08) }.dup(4) +
		// 				{ Rand(0, 0.001) },
		// 			damp * 2)});
		// 			sig = FoaEncode.ar(sig, MoscaUtils.a2b);
		// 			sig = sig * env;
		// 			Out.ar(renderer.fumaBus, sig);
		// 		}).send(server);
		//
		// 		SynthDef("revGlobalAmb_free",  { | gate = 1, room = 0.5, damp = 0.5 |
		// 			var env, temp, convsig, sig, sigx, sigf = In.ar(gBfBus, 4);
		// 			sigx = In.ar(gBixBus, 4);
		// 			sig = In.ar(gBus, 1);
		// 			sigx = FoaEncode.ar(sigx, MoscaUtils.n2f);
		// 			env = EnvGen.kr(Env.asr(1), gate, doneAction:2);
		// 			sig = sig + sigf + sigx;
		// 			sigf = FoaDecode.ar(sigf, MoscaUtils.b2a);
		// 			convsig = [
		// 				FreeVerb2.ar(sig[0], sig[1], mix: 1, room: room, damp: damp),
		// 			FreeVerb2.ar(sig[2], sig[3], mix: 1, room: room, damp: damp)];
		// 			convsig = FoaEncode.ar(convsig.flat, MoscaUtils.a2b);
		// 			convsig = convsig * env;
		// 			Out.ar(renderer.fumaBus, convsig);
		// 		}).send(server);
		//
		// 	} {
		//
		// 		SynthDef("revGlobalAmb_pass", { | gate = 1, room = 0.5, damp = 0.5 |
		// 			var env, w, x, y, z, r, s, t, u, v,
		// 			soaSig, tmpsig, sig, sigx, sigf = In.ar(gBfBus, 9);
		// 			sigx = In.ar(gBixBus, 9);
		// 			sigx = HOAConvert.ar(2, sigx, \FuMa, \ACN_N3D);
		// 			sig = In.ar(gBus, 1);
		// 			env = EnvGen.kr(Env.asr(1), gate, doneAction:2);
		// 			sig = sig + sigf + sigx;
		// 			sig = AtkMatrixMix.ar(sig, MoscaUtils.soa_a12_decoder_matrix);
		// 			16.do({ sig = AllpassC.ar(sig, 0.08, room * { Rand(0, 0.08) }.dup(12) +
		// 				{ Rand(0, 0.001) },
		// 			damp * 2)});
		// 			#w, x, y, z, r, s, t, u, v = AtkMatrixMix.ar(sig, MoscaUtils.soa_a12_encoder_matrix)
		// 			* env;
		// 			soaSig = [w, x, y, z, r, s, t, u, v];
		// 			Out.ar(renderer.fumaBus, soaSig);
		// 		}).load(server);
		//
		// 	};
		//
		// } {

		if (maxorder == 1) {

			SynthDef("revGlobalAmb_pass", { | gate = 1, room = 0.5, damp = 0.5 |
				var env, temp, convsig, sig, sigx, sigf = In.ar(gBfBus, 4);
				sigx = In.ar(gBixBus, 4);
				sig = In.ar(gBus, 1);
				sigx = FoaEncode.ar(sigx, MoscaUtils.n2f);
				env = EnvGen.kr(Env.asr(1), gate, doneAction:2);
				sig = sig + sigf + sigx;
				sig = FoaDecode.ar(sig, MoscaUtils.b2a);
				16.do({ sig = AllpassC.ar(sig, 0.08, room * { Rand(0, 0.08) }.dup(4) +
					{ Rand(0, 0.001) },
					damp * 2)});
				sig = FoaEncode.ar(sig, MoscaUtils.a2b);
				sig = sig * env;
				Out.ar(renderer.fumaBus, sig);
			}).send(server);

			SynthDef("revGlobalAmb_free",  { | gate = 1, room = 0.5, damp = 0.5 |
				var env, temp, convsig, sig, sigx, sigf = In.ar(gBfBus, 4);
				sigx = In.ar(gBixBus, 4);
				sig = In.ar(gBus, 1);
				sigx = FoaEncode.ar(sigx, MoscaUtils.n2f);
				env = EnvGen.kr(Env.asr(1), gate, doneAction:2);
				sig = sig + sigf + sigx;
				sig = FoaDecode.ar(sig, MoscaUtils.b2a);
				convsig = [
					FreeVerb.ar(sig[0], mix: 1, room: room, damp: damp),
					FreeVerb.ar(sig[1], mix: 1, room: room, damp: damp),
					FreeVerb.ar(sig[2], mix: 1, room: room, damp: damp),
					FreeVerb.ar(sig[3], mix: 1, room: room, damp: damp)];
				convsig = FoaEncode.ar(convsig.flat, MoscaUtils.a2b);
				convsig = convsig * env;
				Out.ar(renderer.fumaBus, convsig);
			}).send(server);

		} {

			SynthDef("revGlobalAmb_pass", { | gate = 1, room = 0.5, damp = 0.5 |
				var env, w, x, y, z, r, s, t, u, v,
				soaSig, tmpsig, sig, sigx, sigf = In.ar(gBfBus, 9);
				sigx = In.ar(gBixBus, 9);
				sigx = HOAConvert.ar(2, sigx, \FuMa, \ACN_N3D);
				sig = In.ar(gBus, 1);
				env = EnvGen.kr(Env.asr(1), gate, doneAction:2);
				sig = sig + sigf + sigx;
				sig = AtkMatrixMix.ar(sig, MoscaUtils.soa_a12_decoder_matrix);
				16.do({ sig = AllpassC.ar(sig, 0.08, room * { Rand(0, 0.08) }.dup(12) +
					{ Rand(0, 0.001) },
					damp * 2)});
				#w, x, y, z, r, s, t, u, v = AtkMatrixMix.ar(sig, MoscaUtils.soa_a12_encoder_matrix)
				* env;
				soaSig = [w, x, y, z, r, s, t, u, v];
				Out.ar(renderer.fumaBus, soaSig);
			}).load(server);

			SynthDef("revGlobalAmb_free",  { | gate = 1, room = 0.5, damp = 0.5 |
				var env, w, x, y, z, r, s, t, u, v,
				soaSig, tmpsig, sig, sigx, sigf = In.ar(gBfBus, 9);
				sigx = In.ar(gBixBus, 9);
				sigx = HOAConvert.ar(2, sigx, \FuMa, \ACN_N3D);
				sig = In.ar(gBus, 1);
				env = EnvGen.kr(Env.asr(1), gate, doneAction:2);
				sig = sig + sigf + sigx;
				sig = AtkMatrixMix.ar(sig, MoscaUtils.soa_a12_decoder_matrix);
				tmpsig = [
					FreeVerb2.ar(sig[0], sig[1], mix: 1, room: room, damp: damp),
					FreeVerb2.ar(sig[2], sig[3], mix: 1, room: room, damp: damp),
					FreeVerb2.ar(sig[4], sig[5], mix: 1, room: room, damp: damp),
					FreeVerb2.ar(sig[6], sig[7], mix: 1, room: room, damp: damp),
					FreeVerb2.ar(sig[8], sig[9], mix: 1, room: room, damp: damp),
					FreeVerb2.ar(sig[10], sig[11], mix: 1, room: room, damp: damp)];
				tmpsig = tmpsig.flat * env;
				#w, x, y, z, r, s, t, u, v = AtkMatrixMix.ar(tmpsig,
					MoscaUtils.soa_a12_encoder_matrix);
				soaSig = [w, x, y, z, r, s, t, u, v];
				Out.ar(renderer.fumaBus, soaSig);
			}).send(server);

		};
		//		};

		//run the makeSpatialisers methode for each types of local reverbs

		localReverbFunc = Array2D(4, 3);

		localReverbFunc[0, 0] = "_pass";

		localReverbFunc[0, 1] = { | lrevRef, p, rirWspectrum, locallev, room, damp |
			var temp = p;
			16.do({ temp = AllpassC.ar(temp, 0.08, room * { Rand(0, 0.08) } +
				{ Rand(0, 0.001) },
				damp * 2)});
			lrevRef.value = temp * locallev;
		};

		localReverbFunc[0, 2] = { | lrev1Ref, lrev2Ref, p1, p2, rirZspectrum,
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
		//
		// localReverbFunc[0, 3] = { | lrev1Ref, lrev2Ref, p1, p2, rirZspectrum,
		// 	locallev, room, damp |
		// 	var temp1 = p1, temp2 = p2;
		// 	8.do({ temp1 = AllpassC.ar(temp1, 0.08, room * { Rand(0, 0.08) } +
		// 		{ Rand(0, 0.001) },
		// 	damp * 2)});
		// 	8.do({ temp2 = AllpassC.ar(temp2, 0.08, room * { Rand(0, 0.08) } +
		// 		{ Rand(0, 0.001) },
		// 	damp * 2)});
		// 	lrev1Ref.value = temp1 * locallev;
		// 	lrev2Ref.value = temp2 * locallev;
		// };


		// freeverb defs

		//run the makeSpatialisers methode for each types of local reverbs

		localReverbFunc[1, 0] = "_free";

		localReverbFunc[1, 1] = { | lrevRef, p, rirWspectrum, locallev, room = 0.5, damp = 0.5 |
			lrevRef.value = FreeVerb.ar(p, mix: 1, room: room, damp: damp, mul: locallev);
		};

		localReverbFunc[1, 2] = { | lrev1Ref, lrev2Ref, p1, p2, rirZspectrum, locallev,
			room = 0.5, damp = 0.5|
			var temp;
			temp = FreeVerb2.ar(p1, p2, mix: 1, room: room, damp: damp, mul: locallev);
			lrev1Ref.value = temp[0];
			lrev2Ref.value = temp[1];
		};

		// localReverbFunc[1, 3] = { | lrevRef, p, a0ir, a1ir, a2ir, a3ir,
		// 	a4ir, a5ir, a6ir, a7ir, a8ir, a9ir, a10ir, a11ir, locallev,
		// 	room = 0.5, damp = 0.5|
		// 	var temp, sig;
		//
		// 	if (maxorder == 1) {
		// 		sig = FoaDecode.ar(p, MoscaUtils.b2a);
		// 		temp = [
		// 			FreeVerb2.ar(sig[0], sig[1], mix: 1, room: room, damp: damp),
		// 		FreeVerb2.ar(sig[2], sig[3], mix: 1, room: room, damp: damp)];
		// 		lrevRef.value = FoaEncode.ar(temp.flat, MoscaUtils.a2b);
		// 	} {
		// 		sig = AtkMatrixMix.ar(p, MoscaUtils.soa_a12_decoder_matrix);
		// 		temp = [
		// 			FreeVerb2.ar(sig[0], sig[1], mix: 1, room: room, damp: damp),
		// 			FreeVerb2.ar(sig[2], sig[3], mix: 1, room: room, damp: damp),
		// 			FreeVerb2.ar(sig[4], sig[5], mix: 1, room: room, damp: damp),
		// 			FreeVerb2.ar(sig[6], sig[7], mix: 1, room: room, damp: damp),
		// 			FreeVerb2.ar(sig[8], sig[9], mix: 1, room: room, damp: damp),
		// 		FreeVerb2.ar(sig[10], sig[11], mix: 1, room: room, damp: damp)];
		// 		lrevRef.value = AtkMatrixMix.ar(temp.flat,
		// 		MoscaUtils.soa_a12_encoder_matrix);
		// 	}
		// };


		// function for no-reverb option

		localReverbFunc[2, 0] = "";

		localReverbFunc[2, 1] = { | lrevRef, p, rirWspectrum, locallev, room, damp|
		};

		localReverbFunc[2, 2] = { | lrev1Ref, lrev2Ref, p1, p2, rirZspectrum,
			locallev, room, damp |
		};

		// localReverbFunc[2, 3] = { | lrevRef, p, a0ir, a1ir, a2ir, a3ir,
		// 	a4ir, a5ir, a6ir, a7ir, a8ir, a9ir, a10ir, a11ir, locallev,
		// 	room, damp |
		// };

		rirList = Array.newClear();

		if (rirBank.notNil) {

			var rirWspectrum, rirxspectrum, rirYspectrum, rirZspectrum;
			var rirFLUspectrum, rirFRDspectrum, rirBLDspectrum, rirBRUspectrum;
			var rirXspectrum, rirA12Spectrum;

			/////////// START loading rirBank /////////////////////

			var rirName, rirW, rirX, rirY, rirZ, bufWXYZ, rirFLU, rirFRD, rirBLD, rirBRU,
			bufAformat, bufAformat_soa_a12, rirA12, bufsize,
			// prepare list of impulse responses for close and distant reverb
			// selection menue

			rirPath = PathName(rirBank),
			rirNum = 0; // initialize

			rirW = [];
			rirX = [];
			rirY = [];
			rirZ = [];
			bufWXYZ = [];

			rirPath.entries.do({ |item, count|

				if (item.extension == "amb") {
					rirNum = rirNum + 1;

					rirName = item.fileNameWithoutExtension;
					rirList = rirList ++ [rirName];

					rirW = rirW ++ [ Buffer.readChannel(server, item.fullPath,
						channels: [0]) ];
					rirX = rirX ++ [ Buffer.readChannel(server, item.fullPath,
						channels: [1]) ];
					rirY = rirY ++ [ Buffer.readChannel(server, item.fullPath,
						channels: [2]) ];
					rirZ = rirZ ++ [ Buffer.readChannel(server, item.fullPath,
						channels: [3]) ];

					bufWXYZ = bufWXYZ ++ [ Buffer.read(server, item.fullPath) ];
				};

			});

			bufAformat = Array.newClear(rirNum);
			bufAformat_soa_a12 = Array.newClear(rirNum);
			rirFLU = Array.newClear(rirNum);
			rirFRD = Array.newClear(rirNum);
			rirBLD = Array.newClear(rirNum);
			rirBRU = Array.newClear(rirNum);
			bufsize = Array.newClear(rirNum);

			rirWspectrum = Array.newClear(rirNum);
			rirXspectrum = Array.newClear(rirNum);
			rirYspectrum = Array.newClear(rirNum);
			rirZspectrum = Array.newClear(rirNum);

			rirFLUspectrum = Array.newClear(rirNum);
			rirFRDspectrum = Array.newClear(rirNum);
			rirBLDspectrum = Array.newClear(rirNum);
			rirBRUspectrum = Array.newClear(rirNum);

			server.sync;

			rirList.do({ |item, count|

				bufsize[count] = PartConv.calcBufSize(fftSize, rirW[count]);

				bufAformat[count] = Buffer.alloc(server, bufWXYZ[count].numFrames,
					bufWXYZ[count].numChannels);
				bufAformat_soa_a12[count] = Buffer.alloc(server,
					bufWXYZ[count].numFrames, 12);
				// for second order conv

				if (File.exists(rirBank ++ "/" ++ item ++ "_Flu.wav").not) {

					("writing " ++ item ++ "_Flu.wav file in" ++ rirBank).postln;

					{BufWr.ar(FoaDecode.ar(PlayBuf.ar(4, bufWXYZ[count],
						loop: 0, doneAction: 2), MoscaUtils.b2a),
					bufAformat[count], Phasor.ar(0,
						BufRateScale.kr(bufAformat[count]),
						0, BufFrames.kr(bufAformat[count])));
					Out.ar(0, Silent.ar);
					}.play;

					(bufAformat[count].numFrames / server.sampleRate).wait;

					bufAformat[count].write(rirBank ++ "/" ++ item ++ "_Flu.wav",
						headerFormat: "wav", sampleFormat: "int24");

					"done".postln;

				};


				if (File.exists(rirBank ++ "/" ++ item ++ "_SoaA12.wav").not) {

					("writing " ++ item ++ "_SoaA12.wav file in " ++ rirBank).postln;

					{BufWr.ar(AtkMatrixMix.ar(PlayBuf.ar(4, bufWXYZ[count],
						loop: 0, doneAction: 2),
					MoscaUtils.foa_a12_decoder_matrix),
					bufAformat_soa_a12[count],
					Phasor.ar(0, BufRateScale.kr(bufAformat[count]), 0,
						BufFrames.kr(bufAformat[count])));
					Out.ar(0, Silent.ar);
					}.play;

					(bufAformat[count].numFrames / server.sampleRate).wait;

					bufAformat_soa_a12[count].write(
						rirBank ++ "/" ++ item ++ "_SoaA12.wav",
						headerFormat: "wav", sampleFormat: "int24");

					"done".postln;

				};

			});

			"Loading rir bank".postln;

			rirList.do({ |item, count|

				server.sync;

				bufAformat[count].free;
				bufWXYZ[count].free;

				rirFLU[count] = Buffer.readChannel(server,
					rirBank ++ "/" ++ item ++ "_Flu.wav",
					channels: [0]);
				rirFRD[count] = Buffer.readChannel(server,
					rirBank ++ "/" ++ item ++ "_Flu.wav",
					channels: [1]);
				rirBLD[count] = Buffer.readChannel(server,
					rirBank ++ "/" ++ item ++ "_Flu.wav",
					channels: [2]);
				rirBRU[count] = Buffer.readChannel(server,
					rirBank ++ "/" ++ item ++ "_Flu.wav",
					channels: [3]);

				rirWspectrum[count] = Buffer.alloc(server, bufsize[count], 1);
				rirXspectrum[count] = Buffer.alloc(server, bufsize[count], 1);
				rirYspectrum[count] = Buffer.alloc(server, bufsize[count], 1);
				rirZspectrum[count] = Buffer.alloc(server, bufsize[count], 1);
				rirWspectrum[count].preparePartConv(rirW[count], fftSize);
				rirW[count].free;
				// don't need time domain data anymore, just needed spectral version
				rirXspectrum[count].preparePartConv(rirX[count], fftSize);
				rirX[count].free;
				rirYspectrum[count].preparePartConv(rirY[count], fftSize);
				rirY[count].free;
				rirZspectrum[count].preparePartConv(rirZ[count], fftSize);
				rirZ[count].free;

				rirFLUspectrum[count] = Buffer.alloc(server, bufsize[count], 1);
				rirFRDspectrum[count] = Buffer.alloc(server, bufsize[count], 1);
				rirBLDspectrum[count] = Buffer.alloc(server, bufsize[count], 1);
				rirBRUspectrum[count] = Buffer.alloc(server, bufsize[count], 1);
				rirFLUspectrum[count].preparePartConv(rirFLU[count], fftSize);
				rirFLU[count].free;
				rirFRDspectrum[count].preparePartConv(rirFRD[count], fftSize);
				rirFRD[count].free;
				rirBLDspectrum[count].preparePartConv(rirBLD[count], fftSize);
				rirBLD[count].free;
				rirBRUspectrum[count].preparePartConv(rirBRU[count], fftSize);
				rirBRU[count].free;

				/////////// END loading rirBank /////////////////////
			});

			if (maxorder == 1) {

				SynthDef("revGlobalAmb_conv",  { | gate = 1,
					fluir, frdir, bldir, bruir |
					var env, temp, convsig, sig, sigx, sigf = In.ar(gBfBus, 4);
					sigx = In.ar(gBixBus, 4);
					sig = In.ar(gBus, 1);
					sigx = FoaEncode.ar(sigx, MoscaUtils.n2f);
					env = EnvGen.kr(Env.asr(1), gate, doneAction:2);
					sig = sig + sigf + sigx;
					sig = FoaDecode.ar(sig, MoscaUtils.b2a);
					convsig = [
						PartConv.ar(sig[0], fftSize, fluir),
						PartConv.ar(sig[1], fftSize, frdir),
						PartConv.ar(sig[2], fftSize, bldir),
						PartConv.ar(sig[3], fftSize, bruir)];
					convsig = FoaEncode.ar(convsig, MoscaUtils.a2b);
					convsig = convsig * env;
					Out.ar(renderer.fumaBus, convsig);
				}).send(server);

			} {

				rirA12 = Array.newClear(12);
				rirA12Spectrum = Array2D(rirNum, 12);

				rirList.do({ |item, count|

					12.do { | i |
						rirA12[i] = Buffer.readChannel(server,
							rirBank ++ "/" ++ item ++ "_SoaA12.wav",
							channels: [i]);
						server.sync;
						rirA12Spectrum[count, i] = Buffer.alloc(server,
							bufsize[count], 1);
						server.sync;
						rirA12Spectrum[count, i].preparePartConv(rirA12[i], fftSize);
						server.sync;
						rirA12[i].free;
					};

				});

				SynthDef("revGlobalAmb_conv",  { | gate = 1, a0ir, a1ir, a2ir, a3ir,
					a4ir, a5ir, a6ir, a7ir, a8ir, a9ir, a10ir, a11ir |
					var env, w, x, y, z, r, s, t, u, v,
					soaSig, tmpsig, sig, sigx, sigf = In.ar(gBfBus, 9);
					sigx = In.ar(gBixBus, 9);
					sigx = HOAConvert.ar(2, sigx, \ACN_N3D, \FuMa);
					sig = In.ar(gBus, 1);
					env = EnvGen.kr(Env.asr(1), gate, doneAction:2);
					sig = sig + sigf + sigx;
					sig = AtkMatrixMix.ar(sig, MoscaUtils.soa_a12_decoder_matrix);
					tmpsig = [
						PartConv.ar(sig[0], fftSize, a0ir),
						PartConv.ar(sig[1], fftSize, a1ir),
						PartConv.ar(sig[2], fftSize, a2ir),
						PartConv.ar(sig[3], fftSize, a3ir),
						PartConv.ar(sig[4], fftSize, a4ir),
						PartConv.ar(sig[5], fftSize, a5ir),
						PartConv.ar(sig[6], fftSize, a6ir),
						PartConv.ar(sig[7], fftSize, a7ir),
						PartConv.ar(sig[8], fftSize, a8ir),
						PartConv.ar(sig[9], fftSize, a9ir),
						PartConv.ar(sig[10], fftSize, a10ir),
						PartConv.ar(sig[11], fftSize, a11ir)];
					tmpsig = tmpsig * env;
					#w, x, y, z, r, s, t, u, v = AtkMatrixMix.ar(tmpsig,
						MoscaUtils.soa_a12_encoder_matrix);
					soaSig = [w, x, y, z, r, s, t, u, v];
					Out.ar(renderer.fumaBus, soaSig);
				}).send(server);

			};

			//run the makeSpatialisers methode for each types of local reverbs

			localReverbFunc[3, 0] = "_conv";

			localReverbFunc[3, 1] = { | lrevRef, p, wir, locallev, room, damp |
				lrevRef.value = PartConv.ar(p, fftSize, wir, locallev);
			};

			localReverbFunc[3, 2] = { | lrev1Ref, lrev2Ref, p1, p2, zir, locallev,
				room, damp |
				var temp1 = p1, temp2 = p2;
				temp1 = PartConv.ar(p1, fftSize, zir, locallev);
				temp2 = PartConv.ar(p2, fftSize, zir, locallev);
				lrev1Ref.value = temp1 * locallev;
				lrev2Ref.value = temp2 * locallev;
			};

			// create fonctions to pass rri busses as Synth arguments

			wSpecPar = {|i|
				[\wir, rirWspectrum[i]]
			};

			zSpecPar = {|i|
				[\zir, rirZspectrum[i]]
			};

			wxyzSpecPar = {|i|
				[\wir, rirWspectrum[i],
					\xir, rirXspectrum[i],
					\yir, rirYspectrum[i],
					\zir, rirZspectrum[i]]
			};

			if (maxorder == 1) {

				irSpecPar = { |i|
					[\fluir, rirFLUspectrum[i],
						\frdir, rirFRDspectrum[i],
						\bldir, rirBLDspectrum[i],
						\bruir, rirBRUspectrum[i]]
				};
			} {
				irSpecPar = { |i|
					[\a0ir, rirA12Spectrum[i, 0],
						\a1ir, rirA12Spectrum[i, 1],
						\a2ir, rirA12Spectrum[i, 2],
						\a3ir, rirA12Spectrum[i, 3],
						\a4ir, rirA12Spectrum[i, 4],
						\a5ir, rirA12Spectrum[i, 5],
						\a6ir, rirA12Spectrum[i, 6],
						\a7ir, rirA12Spectrum[i, 7],
						\a8ir, rirA12Spectrum[i, 8],
						\a9ir, rirA12Spectrum[i, 9],
						\a10ir, rirA12Spectrum[i, 10],
						\a11ir, rirA12Spectrum[i, 11]]
				};
			};

		};
	}
}