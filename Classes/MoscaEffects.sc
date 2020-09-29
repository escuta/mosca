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
* User must set up a project directory with subdirectoties "ir" and "auto"
* irs should have the first 100 or 120ms silenced to act as "tail" reverberators
* and must be placed in the "ir" directory.
* Run help on the "Mosca" class in SuperCollider for detailed information
* and code examples. Further information and sample irs and B-format recordings
* may be downloaded here: http://escuta.org/mosca
*/

MoscaEffects {
	var multyThread, <defs, <irList, <fxList;
	var <wSpecPar, <zSpecPar, wxyzSpecPar, irSpecPar;
	var <gBfBus, <gBxBus, glbFxGrp, globalFx;
	var encodeFunc, decodeFunc;
	var ossiaGlobal, ossiaDel, ossiaDec;

	*new { | server, maxOrder, multyThread, renderer, irBank |

		^super.newCopyArgs(multyThread).ctr(server, maxOrder, renderer, irBank);
	}

	ctr { | server, maxOrder, renderer, irBank |
		var busChans = MoscaUtils.fourOrNine(maxOrder);

		gBfBus = Bus.audio(server, busChans); // global b-format bus
		gBxBus = Bus.audio(server, busChans); // global n3d b-format bus
		glbFxGrp = ParGroup.tail(server.defaultGroup);

		defs = Array.newFrom(EffectDef.defList);

		if (irBank.notNil) {
			this.prLoadir(server, maxOrder, irBank);
			defs = defs.add(ConVerbDef);
		};

		if (multyThread) {
		} {
			if (maxOrder == 1) {

				decodeFunc = {
					var sigf = In.ar(gBfBus, 4);
					var sigx = In.ar(gBxBus, 4);
					sigx = FoaEncode.ar(sigx, MoscaUtils.n2f());
					sigf = sigf + sigx;
					^sigf = FoaDecode.ar(sigf, MoscaUtils.b2a());
				};

				if (renderer.convertFuma) {
					var enc = MoscaUtils.foa_n3d_encoder();

					encodeFunc = { | sig, gate |
						var convsig = sig * enc;
						convsig = convsig * EnvGen.kr(Env.asr(1), gate, doneAction:2);
						Out.ar(renderer.n3dBus, convsig);
					};
				} {
					encodeFunc = { | sig, gate |
						var convsig = FoaEncode.ar(sig, MoscaUtils.a2b());
						convsig = convsig * EnvGen.kr(Env.asr(1), gate, doneAction:2);
						Out.ar(renderer.fumaBus, convsig);
					};
				};
			} {
				decodeFunc = {
					var sigf = In.ar(gBfBus, 9);
					var sigx = In.ar(gBxBus, 9);
					sigx = HOAConvert.ar(2, sigx, \FuMa, \ACN_N3D);
					sigf = sigf + sigx;
					^sigf = AtkMatrixMix.ar(sigf, MoscaUtils.soa_a12_decoder_matrix());
				};

				if (renderer.convertFuma) {
					var enc = MoscaUtils.soa_n3d_encoder();

					encodeFunc = { | sig, gate |
						var convsig = sig * enc;
						convsig = convsig * EnvGen.kr(Env.asr(1), gate, doneAction:2);
						Out.ar(renderer.n3dBus, convsig);
					};
				} {
					encodeFunc = { | sig, gate |
						var convsig = AtkMatrixMix.ar(sig,
							MoscaUtils.soa_a12_encoder_matrix());
						convsig = convsig * EnvGen.kr(Env.asr(1), gate, doneAction:2);
						Out.ar(renderer.fumaBus, convsig);
					};
				}
			};
		};
	}

	setGlobalControl { | ossiaParent, allCritical, automation, fxList |

		ossiaGlobal = OssiaAutomationProxy(ossiaParent, "Global_effect", String,
			[nil, nil, fxList], "Clear", critical:true, repetition_filter:true);

		ossiaGlobal.param.description_(fxList.asString);

		ossiaGlobal.action_({ | num |

			if (globalFx.isPlaying) { globalFx.set(\gate, 0) };

			if (num.value != "Clear") {

				globalFx = Synth(\globalFx_ ++ num.value,
					[\gate, 1, \room, ossiaDel.value, \damp, ossiaDec.value],
					glbFxGrp).register;
			};
		});

		ossiaDel = OssiaAutomationProxy(ossiaGlobal, "Room_delay", Float,
			[0, 1], 0.5, 'clip', critical:allCritical, repetition_filter:true);

		ossiaDel.action_({ | num | globalFx.set(\room, num.value); });

		ossiaDec = OssiaAutomationProxy(ossiaGlobal, "Damp_decay", Float,
			[0, 1], 0.5, 'clip', critical:allCritical, repetition_filter:true);

		ossiaDec.action_({ | num |	globalFx.set(\damp, num.value); });
	}

	prLoadIr { | server, maxOrder, irBank |
		var irWspectrum, irxspectrum, irYspectrum, irZspectrum;
		var irFLUspectrum, irFRDspectrum, irBLDspectrum, irBRUspectrum;
		var irXspectrum, irA12Spectrum;
		var irW, irX, irY, irZ, bufWXYZ, irFLU, irFRD, irBLD, irBRU,
		bufAformat, bufAformat_soa_a12, irA12, bufsize,
		// prepare list of impulse responses for close and distant reverb

		irPath = PathName(irBank),
		irNum = 0; // initialize

		irW = [];
		irX = [];
		irY = [];
		irZ = [];
		bufWXYZ = [];

		irPath.entries.do({ |item, count|

			if (item.extension == "amb") {
				var irName = item.fileNameWithoutExtension;

				irNum = irNum + 1;

				irList = irList ++ [irName];

				irW = irW ++ [ Buffer.readChannel(server, item.fullPath,
					channels: [0]) ];
				irX = irX ++ [ Buffer.readChannel(server, item.fullPath,
					channels: [1]) ];
				irY = irY ++ [ Buffer.readChannel(server, item.fullPath,
					channels: [2]) ];
				irZ = irZ ++ [ Buffer.readChannel(server, item.fullPath,
					channels: [3]) ];

				bufWXYZ = bufWXYZ ++ [ Buffer.read(server, item.fullPath) ];
			};

		});

		bufAformat = Array.newClear(irNum);
		bufAformat_soa_a12 = Array.newClear(irNum);
		irFLU = Array.newClear(irNum);
		irFRD = Array.newClear(irNum);
		irBLD = Array.newClear(irNum);
		irBRU = Array.newClear(irNum);
		bufsize = Array.newClear(irNum);

		irWspectrum = Array.newClear(irNum);
		irXspectrum = Array.newClear(irNum);
		irYspectrum = Array.newClear(irNum);
		irZspectrum = Array.newClear(irNum);

		irFLUspectrum = Array.newClear(irNum);
		irFRDspectrum = Array.newClear(irNum);
		irBLDspectrum = Array.newClear(irNum);
		irBRUspectrum = Array.newClear(irNum);

		server.sync;

		irList.do({ |item, count|

			bufsize[count] = PartConv.calcBufSize(MoscaUtils.fftSize(), irW[count]);

			bufAformat[count] = Buffer.alloc(server, bufWXYZ[count].numFrames,
				bufWXYZ[count].numChannels);
			bufAformat_soa_a12[count] = Buffer.alloc(server,
				bufWXYZ[count].numFrames, 12);
			// for second order conv

			if (File.exists(irBank ++ "/" ++ item ++ "_Flu.wav").not) {

				("writing " ++ item ++ "_Flu.wav file in" ++ irBank).postln;

				{BufWr.ar(FoaDecode.ar(PlayBuf.ar(4, bufWXYZ[count],
					loop: 0, doneAction: 2), MoscaUtils.b2a),
				bufAformat[count], Phasor.ar(0,
					BufRateScale.kr(bufAformat[count]),
					0, BufFrames.kr(bufAformat[count])));
				Out.ar(0, Silent.ar);
				}.play;

				(bufAformat[count].numFrames / server.sampleRate).wait;

				bufAformat[count].write(irBank ++ "/" ++ item ++ "_Flu.wav",
					headerFormat: "wav", sampleFormat: "int24");

				"done".postln;
			};


			if (File.exists(irBank ++ "/" ++ item ++ "_SoaA12.wav").not) {

				("writing " ++ item ++ "_SoaA12.wav file in " ++ irBank).postln;

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
					irBank ++ "/" ++ item ++ "_SoaA12.wav",
					headerFormat: "wav", sampleFormat: "int24");

				"done".postln;
			};
		});

		"Loading ir bank".postln;

		irList.do({ |item, count|

			server.sync;

			bufAformat[count].free;
			bufWXYZ[count].free;

			irFLU[count] = Buffer.readChannel(server,
				irBank ++ "/" ++ item ++ "_Flu.wav",
				channels: [0]);
			irFRD[count] = Buffer.readChannel(server,
				irBank ++ "/" ++ item ++ "_Flu.wav",
				channels: [1]);
			irBLD[count] = Buffer.readChannel(server,
				irBank ++ "/" ++ item ++ "_Flu.wav",
				channels: [2]);
			irBRU[count] = Buffer.readChannel(server,
				irBank ++ "/" ++ item ++ "_Flu.wav",
				channels: [3]);

			irWspectrum[count] = Buffer.alloc(server, bufsize[count], 1);
			irXspectrum[count] = Buffer.alloc(server, bufsize[count], 1);
			irYspectrum[count] = Buffer.alloc(server, bufsize[count], 1);
			irZspectrum[count] = Buffer.alloc(server, bufsize[count], 1);
			irWspectrum[count].preparePartConv(irW[count], MoscaUtils.fftSize());
			irW[count].free;
			// don't need time domain data anymore, just needed spectral version
			irXspectrum[count].preparePartConv(irX[count], MoscaUtils.fftSize());
			irX[count].free;
			irYspectrum[count].preparePartConv(irY[count], MoscaUtils.fftSize());
			irY[count].free;
			irZspectrum[count].preparePartConv(irZ[count], MoscaUtils.fftSize());
			irZ[count].free;

			irFLUspectrum[count] = Buffer.alloc(server, bufsize[count], 1);
			irFRDspectrum[count] = Buffer.alloc(server, bufsize[count], 1);
			irBLDspectrum[count] = Buffer.alloc(server, bufsize[count], 1);
			irBRUspectrum[count] = Buffer.alloc(server, bufsize[count], 1);
			irFLUspectrum[count].preparePartConv(irFLU[count], MoscaUtils.fftSize());
			irFLU[count].free;
			irFRDspectrum[count].preparePartConv(irFRD[count], MoscaUtils.fftSize());
			irFRD[count].free;
			irBLDspectrum[count].preparePartConv(irBLD[count], MoscaUtils.fftSize());
			irBLD[count].free;
			irBRUspectrum[count].preparePartConv(irBRU[count], MoscaUtils.fftSize());
			irBRU[count].free;

			/////////// END loading irBank /////////////////////
		});

		if (maxOrder > 1) {

			irA12 = Array.newClear(12);
			irA12Spectrum = Array2D(irNum, 12);

			irList.do({ |item, count|

				12.do({ | i |
					irA12[i] = Buffer.readChannel(server,
						irBank ++ "/" ++ item ++ "_SoaA12.wav",
						channels: [i]);
					server.sync;
					irA12Spectrum[count, i] = Buffer.alloc(server,
						bufsize[count], 1);
					server.sync;
					irA12Spectrum[count, i].preparePartConv(irA12[i], MoscaUtils.fftSize());
					server.sync;
					irA12[i].free;
				});

			});
		};

		// create fonctions to pass ir as Synth arguments

		wSpecPar = {|i|
			^[\wir, irWspectrum[i]]
		};

		zSpecPar = {|i|
			^[\zir, irZspectrum[i]]
		};

		wxyzSpecPar = {|i|
			^[\wir, irWspectrum[i],
				\xir, irXspectrum[i],
				\yir, irYspectrum[i],
				\zir, irZspectrum[i]]
		};

		if (maxOrder == 1) {

			irSpecPar = { |i|
				^[\a0ir, irFLUspectrum[i],
					\a1ir, irFRDspectrum[i],
					\a2ir, irBLDspectrum[i],
					\a3ir, irBRUspectrum[i]]
			};
		} {
			irSpecPar = { |i|
				^[\a0ir, irA12Spectrum[i, 0],
					\a1ir, irA12Spectrum[i, 1],
					\a2ir, irA12Spectrum[i, 2],
					\a3ir, irA12Spectrum[i, 3],
					\a4ir, irA12Spectrum[i, 4],
					\a5ir, irA12Spectrum[i, 5],
					\a6ir, irA12Spectrum[i, 6],
					\a7ir, irA12Spectrum[i, 7],
					\a8ir, irA12Spectrum[i, 8],
					\a9ir, irA12Spectrum[i, 9],
					\a10ir, irA12Spectrum[i, 10],
					\a11ir, irA12Spectrum[i, 11]]
			};
		};
	}

	sendReverbs { | multyThread, server |

		if (multyThread) {
		} {
			defs.do({ | item |

				SynthDef(\globalFx_ ++ item.defName, { | gate = 1, room = 0.5, damp = 0.5,
					a0ir, a1ir, a2ir, a3ir, a4ir, a5ir, a6ir, a7ir, a8ir, a9ir, a10ir, a11ir |
					var sig = decodeFunc.vaue();
					sig = _.globalRevFunc.value(sig, room, damp,a0ir, a1ir, a2ir, a3ir, a4ir,
						a5ir, a6ir, a7ir, a8ir, a9ir, a10ir, a11ir);
					encodeFunc.value(sig, gate);
				}).send(server);
			});
		};
	}
}