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

MoscaRevDefs {
	var <wSpecPar, <zSpecPar, wxyzSpecPar, irSpecPar;
	var <gBus, <gBfBus, <gBixBus, glbRevGrp;
	var multyThread;
	var defs;

	*new { | server, maxOrder, renderer, rirBank |

		^super.new.ctr(server, maxOrder, renderer, rirBank);
	}

	ctr { | server, maxOrder, renderer, rirBank |
		var busChans = MoscaUtils.fourOrNine(maxOrder);

		gBus = Bus.audio(server, 1); // global reverb bus
		gBfBus = Bus.audio(server, busChans); // global b-format bus
		gBixBus = Bus.audio(server, busChans); // global n3d b-format bus
		glbRevGrp = ParGroup.tail(server.defaultGroup);

		defs = Array.newFrom(RevDefBase.defList);

		multyThread = Server.program.asString.endsWith("supernova");

		if (rirBank.notNil) {
			this.prLoadRir(server, rirBank);
			defs = defs.add(ConVerbDef);
		};
	}

	prLoadRir { | server, maxOrder, renderer, rirBank |
		var rirWspectrum, rirxspectrum, rirYspectrum, rirZspectrum;
		var rirFLUspectrum, rirFRDspectrum, rirBLDspectrum, rirBRUspectrum;
		var rirXspectrum, rirA12Spectrum;
		var rirName, rirW, rirX, rirY, rirZ, bufWXYZ, rirFLU, rirFRD, rirBLD, rirBRU,
		bufAformat, bufAformat_soa_a12, rirA12, bufsize,
		// prepare list of impulse responses for close and distant reverb

		rirList = Array.newClear(),
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

			bufsize[count] = PartConv.calcBufSize(MoscaUtils.fftSize(), rirW[count]);

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
			rirWspectrum[count].preparePartConv(rirW[count], MoscaUtils.fftSize());
			rirW[count].free;
			// don't need time domain data anymore, just needed spectral version
			rirXspectrum[count].preparePartConv(rirX[count], MoscaUtils.fftSize());
			rirX[count].free;
			rirYspectrum[count].preparePartConv(rirY[count], MoscaUtils.fftSize());
			rirY[count].free;
			rirZspectrum[count].preparePartConv(rirZ[count], MoscaUtils.fftSize());
			rirZ[count].free;

			rirFLUspectrum[count] = Buffer.alloc(server, bufsize[count], 1);
			rirFRDspectrum[count] = Buffer.alloc(server, bufsize[count], 1);
			rirBLDspectrum[count] = Buffer.alloc(server, bufsize[count], 1);
			rirBRUspectrum[count] = Buffer.alloc(server, bufsize[count], 1);
			rirFLUspectrum[count].preparePartConv(rirFLU[count], MoscaUtils.fftSize());
			rirFLU[count].free;
			rirFRDspectrum[count].preparePartConv(rirFRD[count], MoscaUtils.fftSize());
			rirFRD[count].free;
			rirBLDspectrum[count].preparePartConv(rirBLD[count], MoscaUtils.fftSize());
			rirBLD[count].free;
			rirBRUspectrum[count].preparePartConv(rirBRU[count], MoscaUtils.fftSize());
			rirBRU[count].free;

			/////////// END loading rirBank /////////////////////
		});

		if (maxOrder == 1) {

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
					PartConv.ar(sig[0], MoscaUtils.fftSize(), fluir),
					PartConv.ar(sig[1], MoscaUtils.fftSize(), frdir),
					PartConv.ar(sig[2], MoscaUtils.fftSize(), bldir),
					PartConv.ar(sig[3], MoscaUtils.fftSize(), bruir)];
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
					rirA12Spectrum[count, i].preparePartConv(rirA12[i], MoscaUtils.fftSize());
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
					PartConv.ar(sig[11], MoscaUtils.fftSize(), a11ir)];
				tmpsig = tmpsig * env;
				#w, x, y, z, r, s, t, u, v = AtkMatrixMix.ar(tmpsig,
					MoscaUtils.soa_a12_encoder_matrix);
				soaSig = [w, x, y, z, r, s, t, u, v];
				Out.ar(renderer.fumaBus, soaSig);
			}).send(server);

		};

		//run the makeSpatialisers methode for each types of local reverbs

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

		if (maxOrder == 1) {

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
	}

	revDefs { | maxOrder, server, renderer, rirBank |

		if (maxOrder == 1) {

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
	}

	multyRevDefs { | maxOrder, server, renderer, rirBank |

		if (maxOrder == 1) {

			SynthDef("parallelVerb_pass", { | gate = 1, room = 0.5, damp = 0.5 |
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

		};

		if (rirBank.notNil) {

			var rirWspectrum, rirxspectrum, rirYspectrum, rirZspectrum;
			var rirFLUspectrum, rirFRDspectrum, rirBLDspectrum, rirBRUspectrum;
			var rirXspectrum, rirA12Spectrum;

			/////////// START loading rirBank /////////////////////

			var rirName, rirW, rirX, rirY, rirZ, bufWXYZ, rirFLU, rirFRD, rirBLD, rirBRU,
			bufAformat, bufAformat_soa_a12, rirA12, bufsize,
			// prepare list of impulse responses for close and distant reverb
			// selection menue

			rirList = Array.newClear(),
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

				bufsize[count] = PartConv.calcBufSize(MoscaUtils.fftSize(), rirW[count]);

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
				rirWspectrum[count].preparePartConv(rirW[count], MoscaUtils.fftSize());
				rirW[count].free;
				// don't need time domain data anymore, just needed spectral version
				rirXspectrum[count].preparePartConv(rirX[count], MoscaUtils.fftSize());
				rirX[count].free;
				rirYspectrum[count].preparePartConv(rirY[count], MoscaUtils.fftSize());
				rirY[count].free;
				rirZspectrum[count].preparePartConv(rirZ[count], MoscaUtils.fftSize());
				rirZ[count].free;

				rirFLUspectrum[count] = Buffer.alloc(server, bufsize[count], 1);
				rirFRDspectrum[count] = Buffer.alloc(server, bufsize[count], 1);
				rirBLDspectrum[count] = Buffer.alloc(server, bufsize[count], 1);
				rirBRUspectrum[count] = Buffer.alloc(server, bufsize[count], 1);
				rirFLUspectrum[count].preparePartConv(rirFLU[count], MoscaUtils.fftSize());
				rirFLU[count].free;
				rirFRDspectrum[count].preparePartConv(rirFRD[count], MoscaUtils.fftSize());
				rirFRD[count].free;
				rirBLDspectrum[count].preparePartConv(rirBLD[count], MoscaUtils.fftSize());
				rirBLD[count].free;
				rirBRUspectrum[count].preparePartConv(rirBRU[count], MoscaUtils.fftSize());
				rirBRU[count].free;

				/////////// END loading rirBank /////////////////////
			});

			if (maxOrder == 1) {

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
						PartConv.ar(sig[0], MoscaUtils.fftSize(), fluir),
						PartConv.ar(sig[1], MoscaUtils.fftSize(), frdir),
						PartConv.ar(sig[2], MoscaUtils.fftSize(), bldir),
						PartConv.ar(sig[3], MoscaUtils.fftSize(), bruir)];
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
						rirA12Spectrum[count, i].preparePartConv(rirA12[i], MoscaUtils.fftSize());
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
						PartConv.ar(sig[11], MoscaUtils.fftSize(), a11ir)];
					tmpsig = tmpsig * env;
					#w, x, y, z, r, s, t, u, v = AtkMatrixMix.ar(tmpsig,
						MoscaUtils.soa_a12_encoder_matrix);
					soaSig = [w, x, y, z, r, s, t, u, v];
					Out.ar(renderer.fumaBus, soaSig);
				}).send(server);

			};

			//run the makeSpatialisers methode for each types of local reverbs

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

			if (maxOrder == 1) {

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

	makeGlobalReverbs { | server |
	}
}