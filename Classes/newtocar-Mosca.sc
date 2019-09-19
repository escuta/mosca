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

+ Mosca {

		newtocar {
		| i, tpos, force = false |
		var path = tfieldProxy[i].value;

		if (streamdisk[i]) {
			var sframe, srate;
			// sframe = tpos * srate;
			// stdur = sf.numFrames / srate; // needed?
			streambuf[i] = Buffer.cueSoundFile(server, path, 0,
				ncanais[i], 131072);
			//		streambuf[i] = srate; //??
			("Creating buffer for source: " ++ i).postln;
		};


		// Note: ncanais refers to number of channels in the context of
		// files on disk
		// ncan is number of channels for hardware or supercollider input
		// busini is the initial bus used for a particular stream
		// If we have ncan = 4 and busini = 7, the stream will enter
		// in buses 7, 8, 9 and 10.


		/// STREAM FROM DISK


		if ((path != "") && hwncheckProxy[i].value.not
			&& scncheckProxy[i].value.not
			&& streamdisk[i]) {
			"Content Streamed from disk".postln;
			if (audit[i].not || force) { // if source is testing don't relaunch synths

				case
				{ ncanais[i] == 1} {
					"1 channel".postln;

/*					synt[i] = Synth(\playMonoStream, [\outbus, mbus[i],
						\bufnum, streambuf[i].bufnum, \rate, 1, \tpos, tpos, \lp, lp[i],
						\level, level[i]],
					playEspacGrp).onFree({espacializador[i].free;
						espacializador[i] = nil; synt[i] = nil;
						streambuf[i].free;
					});*/

					// set lib, convert and dstrv variables when stynths are lauched
					// for the tracking functions to stay relevant

					lib[i] = libboxProxy[i].value;

					case
					{ libboxProxy[i].value <= lastN3D }
					{ convert[i] = convert_n3d; }
					{ (libboxProxy[i].value >= (lastN3D + 1)) &&
						(libboxProxy[i].value <= lastFUMA) }
					{ convert[i] = convert_fuma; }
					{ libboxProxy[i].value >= (lastFUMA + 1) }
					{ convert[i] = convert_direct; };

					dstrv[i] = dstrvboxProxy[i].value;

					if (convert[i]) {

						if (convertor.notNil) {
							convertor.set(\gate, 1);
						} {
							convertor = Synth(\ambiConverter, [\gate, 1],
								target:glbRevDecGrp).onFree({
								convertor = nil;
							});
						};
					};

					if (azimuths.isNil) {

						if ((libboxProxy[i].value > lastFUMA)
							&& nonAmbi2FuMa.isNil) {
							nonAmbi2FuMa = Synth(\nonAmbi2FuMa,
								target:glbRevDecGrp).onFree({
								nonAmbi2FuMa = nil;
							});
						};
					};

					espacializador[i] = Synth(libName[i]++"Stream"
						++dstrvtypes[i],
						[\bufnum, streambuf[i].bufnum, \rate, 1, \tpos, tpos, \lp, lp[i],
							\insertFlag, insertFlag[i],
							\insertIn, insertBus[0,i],
							\insertOut, insertBus[1,i],
							\contr, clev[i], \room, rm[i], \damp, dm[i],
							\grainrate, grainrate[i], \winsize, winsize[i],
							\winrand, winrand[i]] ++
						wSpecPar.value(max(dstrvboxProxy[i].value - 3, 0)),
						playEspacGrp).onFree({
						espacializador[i] = nil;
						streambuf[i].free;
						if (azimuths.isNil) {
							if (this.nonAmbi2FuMaNeeded(0).not
								&& nonAmbi2FuMa.notNil) {
								nonAmbi2FuMa.free;
							};
						};
						if (convertor.notNil) {
							if (convert[i]) {
								if (this.converterNeeded(0).not) {
									convertor.set(\gate, 0);
								};
							};
						};
					});

					//atualizarvariaveis.value;
					updatesourcevariables.value(i);

				}
				{ ncanais[i] == 2} {
					"2 channel".postln;

/*					synt[i] = Synth(\playStereoStream, [\outbus, sbus[i],
						\bufnum, streambuf[i].bufnum, \rate, 1, \tpos, tpos, \lp, lp[i],
						\level, level[i]],
					playEspacGrp).onFree({espacializador[i].free;
						espacializador[i] = nil; synt[i] = nil;
						streambuf[i].free;
					});*/

					// set lib, convert and dstrv variables when stynths are lauched
					// for the tracking functions to stay relevant

					lib[i] = libboxProxy[i].value;

					case
					{ libboxProxy[i].value <= lastN3D }
					{ convert[i] = convert_n3d; }
					{ (libboxProxy[i].value >= (lastN3D + 1))
						&& (libboxProxy[i].value <= lastFUMA) }
					{ convert[i] = convert_fuma; }
					{ libboxProxy[i].value >= (lastFUMA + 1) }
					{ convert[i] = convert_direct; };

					dstrv[i] = dstrvboxProxy[i].value;

					if (convert[i]) {

						if (convertor.notNil) {
							convertor.set(\gate, 1);
						} {
							convertor = Synth(\ambiConverter, [\gate, 1],
								target:glbRevDecGrp).onFree({
								convertor = nil;
							});
						};
					};

					if (azimuths.isNil) {

						if ((libboxProxy[i].value > lastFUMA)
							&& nonAmbi2FuMa.isNil) {
							nonAmbi2FuMa = Synth(\nonAmbi2FuMa,
								target:glbRevDecGrp).onFree({
								nonAmbi2FuMa = nil;
							});
						};
					};

					espacializador[i] = Synth(libName[i]++"StereoStream"++dstrvtypes[i],
						[\bufnum, streambuf[i].bufnum, \rate, 1, \tpos, tpos, \lp, lp[i],
							\angle, angle[i],
							\insertFlag, insertFlag[i],
							\insertIn, insertBus[0,i],
							\insertOut, insertBus[1,i],
							\room, rm[i], \damp, dm[i], \contr, clev[i],
							\grainrate, grainrate[i], \winsize, winsize[i],
							\winrand, winrand[i]] ++
						zSpecPar.value(max(dstrvboxProxy[i].value - 3, 0)),
						playEspacGrp).onFree({
						espacializador[i] = nil;
						streambuf[i].free;
						if (azimuths.isNil) {
							if (this.nonAmbi2FuMaNeeded(0).not
								&& nonAmbi2FuMa.notNil) {
								nonAmbi2FuMa.free;
							};
						};
						if (convertor.notNil) {
							if (convert[i]) {
								if (this.converterNeeded(0).not) {
									convertor.set(\gate, 0);
								};
							};
						};
					});

					updatesourcevariables.value(i);

				}
				{ ncanais[i] >= 4} {
					playingBF[i] = true;
					("contains "++ncanais[i]++" channels").postln;

					if ((libboxProxy[i].value >= (lastN3D + 1)) ||
						(dstrvboxProxy[i].value == 3)) {
						libboxProxy[i].valueAction = lastN3D + 1;
						lib[i] = lastN3D + 1;
						convert[i] = convert_fuma;
					} {
						libboxProxy[i].valueAction = 0;
						lib[i] = 0;
						convert[i] = true;
						dstrv[i] = dstrvboxProxy[i].value;
					};

					// set lib, convert and dstrv variables when stynths are lauched
					// for the tracking functions to stay relevant

					if (convert[i]) {
						if (convertor.notNil) {
							convertor.set(\gate, 1);
						} {
							convertor = Synth(\ambiConverter, [\gate, 1],
								target:glbRevDecGrp).onFree({
								convertor = nil;
							});
						};
					};

					espacializador[i] = Synth(libName[i]++"BFormatStream"++ncanais[i],
						[\bufnum, streambuf[i].bufnum, \contr, clev[i],
							\rate, 1, \tpos, tpos, \lp, lp[i], \level, level[i],
							\insertFlag, insertFlag[i],
							\insertIn, insertBus[0,i],
							\insertOut, insertBus[1,i],
							\grainrate, grainrate[i], \winsize, winsize[i], \winrand, winrand[i]] ++
						zSpecPar.value(max(dstrvboxProxy[i].value - 3, 0)),
						playEspacGrp).onFree({
						espacializador[i] = nil;
						playingBF[i] = false;
						streambuf[i].free;
						if (convertor.notNil) {
							if (convert[i]) {
								if (this.converterNeeded(0).not) {
									convertor.set(\gate, 0);
								};
							};
						};
					});

				};

				updatesourcevariables.value(i);

			};
		};

		/// END STREAM FROM DISK

		// check this logic - what should override what?
		if ((path != "") && (hwncheckProxy[i].value.not
			|| scncheckProxy[i].value.not)
		&& streamdisk[i].not) {

			//{
			case
			{ ncanais[i] == 1} { // arquivo mono

/*				synt[i] = Synth(\playMonoFile, [\outbus, mbus[i],
					\bufnum, sombuf[i].bufnum, \rate, 1, \tpos, tpos, \lp, lp[i],
					\level, level[i]],
				playEspacGrp).onFree({espacializador[i].free;
					espacializador[i] = nil;
					synt[i] = nil});*/

				// set lib, convert and dstrv variables when stynths are lauched
				// for the tracking functions to stay relevant

				lib[i] = libboxProxy[i].value;

				case
				{ libboxProxy[i].value <= lastN3D }
				{ convert[i] = convert_n3d; }
				{ (libboxProxy[i].value >= (lastN3D + 1))
					&& (libboxProxy[i].value <= lastFUMA) }
				{ convert[i] = convert_fuma; }
				{ libboxProxy[i].value >= (lastFUMA + 1) }
				{ convert[i] = convert_direct; };

				dstrv[i] = dstrvboxProxy[i].value;

				if (convert[i]) {

					if (convertor.notNil) {
						convertor.set(\gate, 1);
					} {
						convertor = Synth(\ambiConverter, [\gate, 1],
							target:glbRevDecGrp).onFree({
								convertor = nil;
						});
					};
				};

				if (azimuths.isNil) {

					if ((libboxProxy[i].value > lastFUMA)
						&& nonAmbi2FuMa.isNil) {
						nonAmbi2FuMa = Synth(\nonAmbi2FuMa,
							target:glbRevDecGrp).onFree({
							nonAmbi2FuMa = nil;
						});
					};
				};

				espacializador[i] = Synth(libName[i]++"File"++dstrvtypes[i],
					[\bufnum, sombuf[i].bufnum, \rate, 1, \tpos, tpos, \lp, lp[i],
						\insertFlag, insertFlag[i],
						\insertIn, insertBus[0,i],
						\insertOut, insertBus[1,i],
						\room, rm[i], \damp, dm[i], \contr, clev[i],
						\grainrate, grainrate[i], \winsize, winsize[i],
						\winrand, winrand[i]]  ++
					wSpecPar.value(max(dstrvboxProxy[i].value - 3, 0)),
					playEspacGrp).onFree({
					espacializador[i] = nil;
					if (azimuths.isNil) {
						if (this.nonAmbi2FuMaNeeded(0).not
							&& nonAmbi2FuMa.notNil) {
							nonAmbi2FuMa.free;
						};
					};
						if (convertor.notNil) {
						if (convert[i]) {
							if (this.converterNeeded(0).not) {
								convertor.set(\gate, 0);
							};
						};
					};
				});

				updatesourcevariables.value(i);

			}
			{ ncanais[i] == 2 } {

				/*synt[i] = Synth(\playStereoFile, [\outbus, sbus[i],
					\bufnum, sombuf[i].bufnum, \rate, 1, \tpos, tpos, \lp, lp[i],
					\level, level[i]],
				playEspacGrp).onFree({espacializador[i].free;
					espacializador[i] = nil;
					synt[i] = nil});*/

				// set lib, convert and dstrv variables when stynths are lauched
				// for the tracking functions to stay relevant

				lib[i] = libboxProxy[i].value;

				case
				{ libboxProxy[i].value <= lastN3D }
				{ convert[i] = convert_n3d; }
				{ (libboxProxy[i].value >= (lastN3D + 1))
					&& (libboxProxy[i].value <= lastFUMA) }
				{ convert[i] = convert_fuma; }
				{ libboxProxy[i].value >= (lastFUMA + 1) }
				{ convert[i] = convert_direct; };

				dstrv[i] = dstrvboxProxy[i].value;

				if (convert[i]) {

					if (convertor.notNil) {
						convertor.set(\gate, 1);
					} {
						convertor = Synth(\ambiConverter, [\gate, 1],
							target:glbRevDecGrp).onFree({
							convertor = nil;
						});
					};
				};

				if (azimuths.isNil) {

					if ((libboxProxy[i].value > lastFUMA) && nonAmbi2FuMa.isNil) {
						nonAmbi2FuMa = Synth(\nonAmbi2FuMa,
							target:glbRevDecGrp).onFree({
							nonAmbi2FuMa = nil;
						});
					};
				};

				espacializador[i] = Synth(libName[i]++"StereoFile"++dstrvtypes[i],
					[\bufnum, sombuf[i].bufnum, \rate, 1, \tpos, tpos, \lp, lp[i],
						\angle, angle[i],
						\insertFlag, insertFlag[i],
						\insertIn, insertBus[0,i],
						\insertOut, insertBus[1,i],
						\room, rm[i], \damp, dm[i], \contr, clev[i],
						\grainrate, grainrate[i], \winsize, winsize[i],
						\winrand, winrand[i]] ++
					zSpecPar.value(max(dstrvboxProxy[i].value - 3, 0)),
					playEspacGrp).onFree({
					espacializador[i] = nil;
					if (azimuths.isNil) {
						if (this.nonAmbi2FuMaNeeded(0).not
							&& nonAmbi2FuMa.notNil) {
							nonAmbi2FuMa.free;
						};
					};
					if (convertor.notNil) {
						if (convert[i]) {
							if (this.converterNeeded(0).not) {
								convertor.set(\gate, 0);
							};
						};
					};
				});

				//atualizarvariaveis.value;
				updatesourcevariables.value(i);

			}
			{ ncanais[i] >= 4 } {
				playingBF[i] = true;

				if ((libboxProxy[i].value >= (lastN3D + 1)) ||
					(dstrvboxProxy[i].value == 3)) {
					libboxProxy[i].valueAction = lastN3D + 1;
					lib[i] = lastN3D + 1;
					convert[i] = convert_fuma;
				} {
					libboxProxy[i].valueAction = 0;
					lib[i] = 0;
					convert[i] = true;
					dstrv[i] = dstrvboxProxy[i].value;
				};

				// set lib, convert and dstrv variables when stynths are lauched
				// for the tracking functions to stay relevant

				if (convert[i]) {
					if (convertor.notNil) {
						convertor.set(\gate, 1);
					} {
						convertor = Synth(\ambiConverter, [\gate, 1],
							target:glbRevDecGrp).onFree({
							convertor = nil;
						});
					};
				};

				espacializador[i] = Synth(libName[i]++"BFormatFile"++ncanais[i]++dstrvtypes[i],
					[\bufnum, sombuf[i].bufnum, \rate, 1, \tpos, tpos, \lp, lp[i],
						\contr, clev[i], \level, level[i],
						\insertFlag, insertFlag[i],
						\insertIn, insertBus[0,i],
						\insertOut, insertBus[1,i],
						\grainrate, grainrate[i], \winsize, winsize[i], \winrand, winrand[i]] ++
					wSpecPar.value(max(dstrvboxProxy[i].value - 3, 0)),
					playEspacGrp).onFree({espacializador[i].free;
					espacializador[i] = nil;
					playingBF[i] = false;
					if (convertor.notNil) {
						if (convert[i]) {
							if (this.converterNeeded(0).not) {
								convertor.set(\gate, 0);
							};
						};
					};
				});

/*				espacializador[i] = Synth(\ATK2Chowning++dstrvtypes[i],
					[\inbus, mbus[i], \insertFlag, insertFlag[i],
						\insertIn, insertBus[0,i],
						\insertOut, insertBus[1,i],
						\contr, clev[i], \room, rm[i], \damp, dm[i],
						\grainrate, grainrate[i], \winsize, winsize[i],
						\winrand, winrand[i]] ++
					wSpecPar.value(max(dstrvboxProxy[i].value - 3, 0)),
					synt[i], addAction: \addAfter).onFree({
					if (convertor.notNil) {
						if (convert[i]) {
							if (this.converterNeeded(0).not) {
								convertor.set(\gate, 0);
							};
						};
					};
				});*/

			};

			updatesourcevariables.value(i);

		} {
			if ((scncheckProxy[i].value) || (hwncheckProxy[i])) {

				var witch;

				if (hwncheckProxy[i].value) {

					witch = "HWBus";

/*					synt[i] = Synth(\playMonoHWBus, [\outbus, mbus[i],
						\busini, busini[i],\level, level[i]],
					playEspacGrp).onFree({espacializador[i].free;
						espacializador[i] = nil;
						synt[i] = nil});*/
				} {
					witch = "SWBus";

/*					synt[i] = Synth(\playMonoSWBus, [\outbus, mbus[i],
						\busini, scInBus[i], // use "index" method?
						\level, level[i]],
					playEspacGrp).onFree({espacializador[i].free;
						espacializador[i] = nil;
						synt[i] = nil});*/
				};

				case
				{ ncan[i] == 1 } {

					// set lib, convert and dstrv variables when stynths are lauched
					// for the tracking functions to stay relevant

					lib[i] = libboxProxy[i].value;

					case
					{ libboxProxy[i].value <= lastN3D }
					{ convert[i] = convert_n3d; }
					{ (libboxProxy[i].value >= (lastN3D + 1))
						&& (libboxProxy[i].value <= lastFUMA) }
					{ convert[i] = convert_fuma; }
					{ libboxProxy[i].value >= (lastFUMA + 1) }
					{ convert[i] = convert_direct; };

					dstrv[i] = dstrvboxProxy[i].value;

					if (convert[i]) {

						if (convertor.notNil) {
							convertor.set(\gate, 1);
						} {
							convertor = Synth(\ambiConverter, [\gate, 1],
								target:glbRevDecGrp).onFree({
								convertor = nil;
							});
						};
					};

					if (azimuths.isNil) {

						if ((libboxProxy[i].value > lastFUMA)
							&& nonAmbi2FuMa.isNil) {
							nonAmbi2FuMa = Synth(\nonAmbi2FuMa,
								target:glbRevDecGrp).onFree({
								nonAmbi2FuMa = nil;
							});
						};
					};

					espacializador[i] = Synth(libName[i]++witch
						++dstrvtypes[i],
						[\inbus, mbus[i], \insertFlag, insertFlag[i],
							\insertIn, insertBus[0,i],
							\insertOut, insertBus[1,i],
							\room, rm[i], \damp, dm[i], \contr, clev[i],
							\grainrate, grainrate[i], \winsize, winsize[i],
							\winrand, winrand[i]] ++
						wSpecPar.value(max(dstrvboxProxy[i].value - 3, 0)),
						playEspacGrp).onFree({
						espacializador[i] = nil;
						if (azimuths.isNil) {
							if (this.nonAmbi2FuMaNeeded(0).not
								&& nonAmbi2FuMa.notNil) {
								nonAmbi2FuMa.free;
							};
						};
						if (convertor.notNil) {
							if (convert[i]) {
								if (this.converterNeeded(0).not) {
									convertor.set(\gate, 0);
								};
							};
						};
					});

					//atualizarvariaveis.value;
					updatesourcevariables.value(i);

				}
				{ ncan[i] == 2 } {

/*					if (hwncheckProxy[i].value) {

						synt[i] = Synth(\playStereoHWBus,
							[\outbus, sbus[i], \busini,
							busini[i],
							\level, level[i]], playEspacGrp).onFree({
							espacializador[i].free;
							espacializador[i] = nil;
							synt[i] = nil});
					} {
						synt[i] = Synth(\playStereoSWBus, [\outbus, sbus[i],
							\busini, scInBus[i],
							\level, level[i]], playEspacGrp).onFree({
							espacializador[i].free;
							espacializador[i] = nil;
							synt[i] = nil});
					};*/

					// set lib, convert and dstrv variables when stynths are lauched
					// for the tracking functions to stay relevant

					lib[i] = libboxProxy[i].value;

					case
					{ libboxProxy[i].value <= lastN3D }
					{ convert[i] = convert_n3d; }
					{ (libboxProxy[i].value >= (lastN3D + 1))
						&& (libboxProxy[i].value <= lastFUMA) }
					{ convert[i] = convert_fuma; }
					{ libboxProxy[i].value >= (lastFUMA + 1) }
					{ convert[i] = convert_direct; };

					dstrv[i] = dstrvboxProxy[i].value;

					if (convert[i]) {

						if (convertor.notNil) {
							convertor.set(\gate, 1);
						} {
							convertor = Synth(\ambiConverter, [\gate, 1],
								target:glbRevDecGrp).onFree({
								convertor = nil;
							});
						};
					};

					if (azimuths.isNil) {

						if ((libboxProxy[i].value > lastFUMA)
							&& nonAmbi2FuMa.isNil) {
							nonAmbi2FuMa = Synth(\nonAmbi2FuMa,
								target:glbRevDecGrp).onFree({
								nonAmbi2FuMa = nil;
							});
						};
					};

					espacializador[i] = Synth(libName[i]++"Stereo"++witch
						++dstrvtypes[i],
						[\inbus, sbus[i], \angle, angle[i],
							\insertFlag, insertFlag[i],
							\insertIn, insertBus[0,i],
							\insertOut, insertBus[1,i],
							\contr, clev[i], \room, rm[i], \damp, dm[i],
							\grainrate, grainrate[i], \winsize, winsize[i],
							\winrand, winrand[i]] ++
						zSpecPar.value(max(dstrvboxProxy[i].value - 3, 0)),
						playEspacGrp).onFree({
						espacializador[i] = nil;
						if (azimuths.isNil) {
							if (this.nonAmbi2FuMaNeeded(0).not
								&& nonAmbi2FuMa.notNil) {
								nonAmbi2FuMa.free;
							};
						};
						if (convertor.notNil) {
							if (convert[i]) {
								if (this.converterNeeded(0).not) {
									convertor.set(\gate, 0);
								};
							};
						};
					});

					//atualizadstrvariaveis.value;
					updatesourcevariables.value(i);

				}
				{ ncan[i] >= 4 } {

					if ((libboxProxy[i].value >= (lastN3D + 1)) ||
						(dstrvboxProxy[i].value == 3)) {
						libboxProxy[i].valueAction = lastN3D + 1;
						lib[i] = lastN3D + 1;
						convert[i] = convert_fuma;
					} {
						libboxProxy[i].valueAction = 0;
						lib[i] = 0;
						convert[i] = true;
						dstrv[i] = dstrvboxProxy[i].value;
					};

					if (convert[i]) {
						if (convertor.notNil) {
							convertor.set(\gate, 1);
						} {
							convertor = Synth(\ambiConverter, [\gate, 1],
								target:glbRevDecGrp).onFree({
								convertor = nil;
							});
						};
					};
/*
					if (hwncheckProxy[i].value) {

						synt[i] = Synth(\playBFormat++libName[i]++"HWBus_"
							++ncan[i],
							[\gbfbus, gbfbus, \gbixfbus, gbixfbus, \outbus, mbus[i],
								\contr, clev[i], \rate, 1, \tpos, tpos, \level, level[i],
								\insertFlag, insertFlag[i],
								\insertIn, insertBus[0,i],
								\insertOut, insertBus[1,i],
								\busini, busini[i]],playEspacGrp).onFree({
							espacializador[i].free;
							espacializador[i] = nil;
							synt[i] = nil;
						});

					} {

						synt[i] = Synth(\playBFormat++libName[i]++"SWBus_"
							++ncan[i],
							[\gbfbus, gbfbus, \gbixfbus, gbixfbus, \outbus, mbus[i],
								\contr, clev[i], \rate, 1, \tpos, tpos,
								\level, level[i],
								\insertFlag, insertFlag[i],
								\insertIn, insertBus[0,i],
								\insertOut, insertBus[1,i],
								\busini, scInBus[i] ],playEspacGrp).onFree({
							espacializador[i].free;
							espacializador[i] = nil;
							synt[i] = nil;
						});
					};*/

					espacializador[i] = Synth(libName[i]++"BFormat"++witch++
						ncan[i]++dstrvtypes[i],
						[\inbus, mbus[i], \insertFlag, insertFlag[i],
							\insertIn, insertBus[0,i],
							\insertOut, insertBus[1,i],
							\contr, clev[i], \room, rm[i], \damp, dm[i],
							\grainrate, grainrate[i], \winsize, winsize[i],
							\winrand, winrand[i]] ++
						wSpecPar.value(max(dstrvboxProxy[i].value - 3, 0)),
						playEspacGrp).onFree({
						espacializador[i].free;
						if (convertor.notNil) {
							if (convert[i]) {
								if (this.converterNeeded(0).not) {
									convertor.set(\gate, 0);
								};
							};
						};
					});

					//atualizarvariaveis.value;
					updatesourcevariables.value(i);

				};
			};
		};
	}

}