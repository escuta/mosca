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

MoscaRenderer {
	var <nonAmbiBus, <fumaBus, <n3dBus; // buses
	var <convertFuma, <convertN3D; // conversion
	var numOutputs, bFormNumChan; // usefull number of chanels
	var <longestRadius, quarterRadius, twoAndaHalfRadius, <lowestElevation, <highestElevation; // utils
	var <vbapBuffer;
	var <renderer; // synth

	*new { | server, speaker_array, maxOrder, decoder, outBus, subOutBus, rawOutBus, rawformat |

		^super.ctr(server, speaker_array, maxOrder, decoder, outBus, subOutBus, rawOutBus, rawformat);
	}

	ctr { | server, speaker_array, maxOrder, decoder, outBus, subOutBus, rawOutBus, rawformat |

		var radiusses, azimuths, elevations, subOutFunc, perfectSphereFunc;

		fumaBus = Bus.audio(server, MoscaUtils.fourOrNine(maxOrder)); // global b-format FUMA bus
		n3dBus = Bus.audio(server, bFormNumChan); // global b-format ACN-N3D bus

		// setup Vbap
		if (speaker_array.notNil) {
			var max_func, min_func, dimention, vbap_setup, adjust;

			numOutputs = speaker_array.size;

			nonAmbiBus = Bus.audio(server, numOutputs);

			max_func = { |x| // extract the highest value from an array
				var rep = 0;
				x.do{ |item|
					if(item > rep,
						{ rep = item };
					)
				};
				rep };

			case
			{ speaker_array[0].size < 2 || speaker_array[0].size > 3 }
			{ Error("bad speaker array").throw; }
			{ speaker_array[0].size == 2 }
			{
				dimention = 2;

				radiusses = Array.newFrom(speaker_array).collect({ |val| val[1]; });
				longestRadius = max_func.value(radiusses);

				adjust = Array.fill(numOutputs, { |i|
					[(longestRadius - radiusses[i]) / 334, longestRadius/radiusses[i]];
				});

				lowestElevation = 0;
				highestElevation = 0;

				speaker_array.collect({ |val| val.pop; });

				azimuths = speaker_array.flat;

				vbap_setup = VBAPSpeakerArray(dimention, azimuths);
			}
			{ speaker_array[0].size == 3 }
			{
				dimention = 3;

				radiusses = Array.newFrom(speaker_array).collect({ |val| val[2] });
				longestRadius = max_func.value(radiusses);

				adjust = Array.fill(numOutputs, { |i|
					[(longestRadius - radiusses[i]) / 334, longestRadius/radiusses[i]];
				});

				min_func = { |x| // extract the lowest value from an array
					var rep = 0;
					x.do{ |item|
						if (item < rep,
							{ rep = item };
					) };
					rep };

				elevations = Array.newFrom(speaker_array).collect({ |val| val[1] });
				lowestElevation = min_func.value(elevations);
				highestElevation = max_func.value(elevations);

				speaker_array.collect({ |val| val.pop });

				vbap_setup = VBAPSpeakerArray(dimention, speaker_array);

				speaker_array.collect({ |val| val.pop });

				azimuths = speaker_array.flat;

			};

			vbapBuffer = Buffer.loadCollection(server, vbap_setup.getSetsAndMatrices);

			perfectSphereFunc = { |sig|
				sig = Array.fill(numOutputs, { |i| DelayN.ar(sig[i],
					delaytime:adjust[i][0], mul:adjust[i][1]) });
			};

			server.sync;
		} {

			var emulate_array, vbap_setup;

			numOutputs = 26;

			emulate_array = [ [ 0, 90 ], [ 0, 45 ], [ 90, 45 ], [ 180, 45 ], [ -90, 45 ],
				[ 45, 35 ], [ 135, 35 ], [ -135, 35 ], [ -45, 35 ], [ 0, 0 ], [ 45, 0 ],
				[ 90, 0 ], [ 135, 0 ], [ 180, 0 ], [ -135, 0 ], [ -90, 0 ], [ -45, 0 ],
				[ 45, -35 ], [ 135, -35 ], [ -135, -35 ], [ -45, -35 ], [ 0, -45 ],
				[ 90, -45 ], [ 180, -45 ], [ -90, -45 ], [ 0, -90 ] ];

			vbap_setup = VBAPSpeakerArray(3, emulate_array);
			// emulate 26-point Lebedev grid

			vbapBuffer = Buffer.loadCollection(server, vbap_setup.getSetsAndMatrices);

			longestRadius = 1;
			lowestElevation = -90;
			highestElevation = 90;

			perfectSphereFunc = { | sig |
				sig;
			};

			nonAmbiBus = Bus.audio(server, numOutputs);

			server.sync;

			SynthDef("nonAmbi2FuMa", {
				var sig = In.ar(nonAmbiBus, numOutputs);
				sig = FoaEncode.ar(sig,
					FoaEncoderMatrix.newDirections(emulate_array.degrad));
				Out.ar(fumaBus, sig);
			}).send(server);

		};

		quarterRadius = longestRadius / 4;
		twoAndaHalfRadius = longestRadius * 2.5;

		if (subOutBus.notNil) {
			subOutFunc = { | signal, sublevel = 1 |
				var subOut = Mix(signal) * sublevel * 0.5;
				Out.ar(subOutBus, subOut);
			};
		} {
			subOutFunc = { | signal, sublevel | };
		};

		if (decoder.isNil) {

			case
			{rawformat == \FuMa}
			{
				convertFuma = false;
				convertN3D = true;

				SynthDef("ambiConverter", { | gate = 1 |
					var n3dsig, env;
					env = EnvGen.kr(Env.asr(curve:\hold), gate, doneAction:2);
					n3dsig = In.ar(n3dBus, bFormNumChan);
					n3dsig = HOAConvert.ar(maxOrder, n3dsig, \ACN_N3D, \FuMa) * env;
					Out.ar(fumaBus, n3dsig);
				}).send(server);

				SynthDef("MoscaRenderSynth",  { | sub = 1, level = 1 |
					var sig, nonambi;
					sig = In.ar(fumaBus, bFormNumChan);
					nonambi = In.ar(nonAmbiBus, numOutputs);
					perfectSphereFunc.value(nonambi);
					sig = (sig + nonambi) * level;
					subOutFunc.value(sig, sub);
					Out.ar(rawOutBus, sig);
					Out.ar(outBus, nonambi);
				}).send(server);
			}
			{rawformat == \N3D}
			{
				convertFuma = true;
				convertN3D = false;

				SynthDef("ambiConverter", { | gate = 1 |
					var sig, env;
					env = EnvGen.kr(Env.asr(curve:\hold), gate, doneAction:2);
					sig = In.ar(fumaBus, MoscaUtils.fourOrNine(maxOrder));
					sig = HOAConvert.ar(maxOrder, sig, \FuMa, \ACN_N3D) * env;
					Out.ar(n3dBus, sig);
				}).send(server);

				SynthDef("MoscaRenderSynth", {
					| lf_hf=0, xover=400, sub = 1, level = 1 |
					var sig, nonambi;
					sig = In.ar(n3dBus, bFormNumChan) * level;
					nonambi = In.ar(nonAmbiBus, numOutputs) * level;
					perfectSphereFunc.value(nonambi);
					subOutFunc.value(sig + nonambi, sub);
					Out.ar(rawOutBus, sig);
					Out.ar(outBus, nonambi);
				}).send(server);
			};

		} {

			case
			{ maxOrder == 1 }
			{
				convertFuma = false;
				convertN3D = true;

				SynthDef("ambiConverter", { | gate = 1 |
					var n3dsig, env;
					env = EnvGen.kr(Env.asr(curve:\hold), gate, doneAction:2);
					n3dsig = In.ar(n3dBus, 4);
					n3dsig = FoaEncode.ar(n3dsig, MoscaUtils.n2f) * env;
					Out.ar(fumaBus, n3dsig);
				}).send(server);

				if (decoder == "internal") {

					if (elevations.isNil) {
						elevations = Array.fill(numOutputs, { 0 });
					};

					SynthDef("MoscaRenderSynth",  { | sub = 1, level = 1 |
						var sig, nonambi;
						sig = In.ar(fumaBus, 4);
						sig = BFDecode1.ar1(sig[0], sig[1], sig[2], sig[3],
							azimuths.collect(_.degrad), elevations.collect(_.degrad),
							longestRadius, radiusses, mul: 0.5);
						nonambi = In.ar(nonAmbiBus, numOutputs);
						perfectSphereFunc.value(nonambi);
						sig = (sig + nonambi) * level;
						subOutFunc.value(sig, sub);
						Out.ar(outBus, sig);
					}).send(server);

				} {

					if (speaker_array.notNil) {
						SynthDef("MoscaRenderSynth",  { | sub = 1, level = 1 |
							var sig, nonambi;
							sig = In.ar(fumaBus, 4);
							sig = FoaDecode.ar(sig, decoder);
							nonambi = In.ar(nonAmbiBus, numOutputs);
							perfectSphereFunc.value(nonambi);
							sig = (sig + nonambi) * level;
							subOutFunc.value(sig, sub);
							Out.ar(outBus, sig);
						}).send(server);

					} {
						SynthDef("MoscaRenderSynth",  { | sub = 1, level = 1 |
							var sig, nonambi;
							sig = In.ar(fumaBus, 4);
							sig = FoaDecode.ar(sig, decoder);
							sig = sig * level;
							subOutFunc.value(sig, sub);
							Out.ar(outBus, sig);
						}).send(server);
					}
				}
			}

			{ maxOrder == 2 }
			{
				if (decoder == "internal") {

					convertFuma = false;
					convertN3D = true;

					if (elevations.isNil) {
						elevations = Array.fill(numOutputs, { 0 });
					};

					SynthDef("ambiConverter", { | gate = 1 |
						var n3dsig, env;
						env = EnvGen.kr(Env.asr(curve:\hold), gate, doneAction:2);
						n3dsig = In.ar(n3dBus, 9);
						n3dsig = HOAConvert.ar(2, n3dsig, \ACN_N3D, \FuMa) * env;
						Out.ar(fumaBus, n3dsig);
					}).send(server);

					SynthDef("MoscaRenderSynth",  { | sub = 1, level = 1 |
						var sig, nonambi;
						sig = In.ar(fumaBus, 9);
						sig = FMHDecode1.ar1(sig[0], sig[1], sig[2], sig[3], sig[4],
							sig[5], sig[6], sig[7], sig[8],
							azimuths.collect(_.degrad), elevations.collect(_.degrad),
							longestRadius, radiusses, 0.5);
						nonambi = In.ar(nonAmbiBus, numOutputs);
						perfectSphereFunc.value(nonambi);
						sig = (sig + nonambi) * level;
						subOutFunc.value(sig, sub);
						Out.ar(outBus, sig);
					}).send(server);

				} { // assume ADT Decoder
					convertFuma = true;
					convertN3D = false;

					SynthDef("ambiConverter", { | gate = 1 |
						var sig, env;
						env = EnvGen.kr(Env.asr(curve:\hold), gate, doneAction:2);
						sig = In.ar(fumaBus, MoscaUtils.fourOrNine(maxOrder));
						sig = HOAConvert.ar(maxOrder, sig, \FuMa, \ACN_N3D) * env;
						Out.ar(n3dBus, sig);
					}).send(server);

					SynthDef("MoscaRenderSynth", {
						| lf_hf=0, xover=400, sub = 1, level = 1 |
						var sig, nonambi;
						sig = In.ar(n3dBus, bFormNumChan);
						sig = decoder.ar(sig[0], sig[1], sig[2], sig[3], sig[4],
							sig[5], sig[6], sig[7], sig[8], 0, lf_hf, xover:xover);
						nonambi = In.ar(nonAmbiBus, numOutputs);
						perfectSphereFunc.value(nonambi);
						sig = (sig + nonambi) * level;
						subOutFunc.value(sig, sub);
						Out.ar(outBus, sig);
					}).send(server);
				};
			}

			{ maxOrder == 3 } // assume ADT Decoder
			{
				convertFuma = true;
				convertN3D = false;

				SynthDef("ambiConverter", { | gate = 1 |
					var sig, env;
					env = EnvGen.kr(Env.asr(curve:\hold), gate, doneAction:2);
					sig = In.ar(fumaBus, 9);
					sig = HOAConvert.ar(2, sig, \FuMa, \ACN_N3D) * env;
					Out.ar(n3dBus, sig);
				}).send(server);

				SynthDef("MoscaRenderSynth", {
					| lf_hf=0, xover=400, sub = 1, level = 1 |
					var sig, nonambi;
					sig = In.ar(n3dBus, bFormNumChan);
					sig = decoder.ar(sig[0], sig[1], sig[2], sig[3], sig[4],
						sig[5], sig[6], sig[7], sig[8], sig[9], sig[10], sig[11],
						sig[12], sig[13], sig[14], sig[15], 0, lf_hf, xover:xover);
					nonambi = In.ar(nonAmbiBus, numOutputs);
					perfectSphereFunc.value(nonambi);
					sig = (sig + nonambi) * level;
					subOutFunc.value(sig, sub);
					Out.ar(outBus, sig);
				}).send(server);
			}

			{ maxOrder == 4 } // assume ADT Decoder
			{
				convertFuma = true;
				convertN3D = false;

				SynthDef("ambiConverter", { | gate = 1 |
					var sig, env;
					env = EnvGen.kr(Env.asr(curve:\hold), gate, doneAction:2);
					sig = In.ar(fumaBus, 9);
					sig = HOAConvert.ar(2, sig, \FuMa, \ACN_N3D) * env;
					Out.ar(n3dBus, sig);
				}).send(server);

				SynthDef("MoscaRenderSynth", {
					| lf_hf=0, xover=400, sub = 1, level = 1 |
					var sig, nonambi;
					sig = In.ar(n3dBus, bFormNumChan);
					sig = decoder.ar(sig[0], sig[1], sig[2], sig[3], sig[4],
						sig[5], sig[6], sig[7], sig[8], sig[9], sig[10], sig[11],
						sig[12], sig[13], sig[14],sig[15], sig[16], sig[17], sig[18],
						sig[19], sig[20], sig[21], sig[22], sig[23], sig[24],
						0, lf_hf, xover:xover);
					nonambi = In.ar(nonAmbiBus, numOutputs);
					perfectSphereFunc.value(nonambi);
					sig = (sig + nonambi) * level;
					subOutFunc.value(sig, sub);
					Out.ar(outBus, sig);
				}).send(server);
			}

			{ maxOrder == 5 } // assume ADT Decoder
			{
				convertFuma = true;
				convertN3D = false;

				SynthDef("ambiConverter", { | gate = 1 |
					var sig, env;
					env = EnvGen.kr(Env.asr(curve:\hold), gate, doneAction:2);
					sig = In.ar(fumaBus, 9);
					sig = HOAConvert.ar(2, sig, \FuMa, \ACN_N3D) * env;
					Out.ar(n3dBus, sig);
				}).send(server);

				SynthDef("MoscaRenderSynth", {
					| lf_hf=0, xover=400, sub = 1, level = 1 |
					var sig, nonambi;
					sig = In.ar(n3dBus, bFormNumChan);
					sig = decoder.ar(sig[0], sig[1], sig[2], sig[3], sig[4],
						sig[5], sig[6], sig[7], sig[8], sig[9], sig[10], sig[11],
						sig[12], sig[13], sig[14], sig[15], sig[16], sig[17],
						sig[18], sig[19], sig[20], sig[21], sig[22], sig[23],
						sig[24], sig[15], sig[16], sig[17], sig[18], sig[19],
						sig[20], sig[21], sig[22], sig[23], sig[24], sig[25],
						sig[26], sig[27], sig[28], sig[29], sig[30], sig[31],
						sig[32], sig[33], sig[34], sig[35],
						0, lf_hf, xover:xover);
					nonambi = In.ar(nonAmbiBus, numOutputs);
					perfectSphereFunc.value(nonambi);
					sig = (sig + nonambi) * level;
					subOutFunc.value(sig, sub);
					Out.ar(outBus, sig);
				}).send(server);
			};
		};
	}

	launchRenderer	{ | target |
		renderer = Synth(\MoscaRenderSynth, target: target, addAction: \addAfter);
	}
}