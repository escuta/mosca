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

Mosca {
	var projDir, dur, autoLoop, server, <ossiaParent; // initial rguments
	var renderer, effects, center, sources, gui, tracker;
	var ossiaMasterPlay, ossiaMasterLib;
	var <control, watcher, isPlay, ossiaAutomation, // automation control
	ossiaPlay, ossiaLoop, ossiaTransport, ossiaRec, ossiaSeekBack;

	*new { | projDir, nsources = 10, dur = 180, irBank, server, parentOssiaNode,
		allCritical = false, decoder, maxorder = 1, speaker_array, outbus = 0,
		suboutbus, rawformat = \FUMA, rawoutbus, autoloop = false |

		^super.newCopyArgs(projDir, dur, autoloop, server).ctr(nsources, irBank,
			parentOssiaNode, allCritical, decoder, maxorder, speaker_array,
			outbus, suboutbus, rawformat, rawoutbus);
	}

	ctr { | nsources, irBank, parentOssiaNode, allCritical, decoder, maxOrder,
		speaker_array, outBus, subOutBus, rawFormat, rawOutBus |

		var multyThread = false;
		// Server.program.asString.endsWith("supernova");

		if (server.isNil) { server = Server.local; };

		server.doWhenBooted({
			var spat = MoscaSpatializer; // The instance will only be needed in the constructor

			if (parentOssiaNode.isNil) {
				ossiaParent = OSSIA_Device("Mosca");
			} {
				ossiaParent = OSSIA_Node(parentOssiaNode, "Mosca");
			};

			ossiaMasterPlay = OSSIA_Parameter(ossiaParent, "Audition_all", Boolean,
				critical:true);

			ossiaMasterLib = OSSIA_Parameter(ossiaParent, "Library_all", String,
				[nil, nil, spat.spatList], "Ambitools", critical:true, repetition_filter:true);

			ossiaMasterLib.description_(spat.spatList.asString);

			control = Automation(dur, showLoadSave: false, showSnapshot: true,
				minTimeStep: 0.001);

			ossiaAutomation = OSSIA_Node(ossiaParent, "Automation");

			ossiaPlay = OSSIA_Parameter(ossiaAutomation, "Play", Boolean,
				critical:true, repetition_filter:true);

			isPlay = false;

			ossiaLoop = OSSIA_Parameter(ossiaAutomation, "Loop", Boolean,
				critical:true, repetition_filter:true);

			ossiaTransport = OSSIA_Parameter(ossiaAutomation, "Transport", Float,
				[0, dur], 0, 'wrap', critical:true, repetition_filter:true);

			ossiaTransport.unit_(OSSIA_time.second);

			ossiaRec = OSSIA_Parameter(ossiaAutomation, "Record", Boolean,
				critical:true, repetition_filter:true);

			renderer = MoscaRenderer(server, speaker_array, maxOrder, decoder, outBus,
				subOutBus, rawOutBus, rawFormat, ossiaParent, allCritical, control);

			effects = MoscaEffects(server, maxOrder, multyThread, renderer, irBank,
				ossiaParent, allCritical, control);

			spat.new(server, maxOrder, renderer, effects);

			effects.sendFx(multyThread, server);

			renderer.launchRenderer(server, server.defaultGroup);

			center = OssiaAutomationCenter(ossiaParent, allCritical);

			sources = Array.fill(nsources,
				{ | i | MoscaSource(i, server, ossiaParent, allCritical,
					spat.spatList, effects, center); });

			control.onPlay_({
				var startTime;

				"NOW PLAYING".postln;

				if (ossiaLoop.v) {

					sources.do_({ | item | item.firstTime = true; });
					ossiaLoop.v_(false);
					"Was looping".postln;
				};

				if(control.now < 0) {
					startTime = 0
				} {
					startTime = control.now
				};

				isPlay = true;

				ossiaPlay.v_(true);
			});

			control.onStop_({

				if (autoLoop.not || (control.now.round != dur)) {

					("I HAVE STOPPED. dur = " ++ dur ++ " now = " ++ control.now).postln;

					sources.do({ | item |
						// don't switch off sources playing individally
						// leave that for user
						if (item.play.value == false) {
							item.runStop(); // to kill SC input synths
							item.spatializer.free;
						};

						item.firstTime = true;
					});

					isPlay = false;
					ossiaLoop.v_(false);
				} {
					("Did not stop. dur = " ++ dur ++ " now = " ++ control.now).postln;
					ossiaLoop.v_(true);
					control.play;
				};

				ossiaPlay.value_(false);
			});

			control.onEnd_({ control.seek(); });

			if (projDir.notNil) {
				control.presetDir = projDir ++ "/auto";
				control.load(control.presetDir);
			} {
				control.presetDir = "/auto";
			};

			ossiaPlay.callback_({ | bool |
				if (bool) {
					if (isPlay.not) {
						control.play; };
				} {
					if (isPlay) {
						control.stop; };
				};
			});

			ossiaLoop.callback_({ | val |
				if (autoLoop.value != val.value) {
					autoLoop.valueAction = val.value;
				};
			});

			ossiaTransport.callback_({ | num |
				if (ossiaSeekBack) {
					control.seek(num.value);
				};
			});

			ossiaRec.callback_({ | bool |
				if (bool.value) {
					control.enableRecording;
				} {
					control.stopRecording;
				};
			});

			center.setActions(sources);

			watcher = Routine.new({
				var plim = MoscaUtils.plim();
				"WATCHER!!!".postln;
				inf.do({
					0.1.wait;

					sources.do({ | item |

						if (item.coordinates.spheVal.rho >= plim) {
							if(item.spatializer.notNil) {
								item.runStop(); // to kill SC input synths
								item.spatializer.free;
							};

							item.firstTime = true;
						} {
							if((isPlay || item.play.value) && item.spatializer.isNil && item.firstTime) {
								// could set the start point for file
								item.launchSynth(true);
								item.firstTime = false;
							};
						};
					});

					if (isPlay) {
						ossiaSeekBack = false;
						ossiaTransport.v_(control.now);
						ossiaSeekBack = true;
					};
				});
			});

			watcher.play;

		}, { ^Error("server not booted").throw; });
	}
}

//
// if (gui.isNil) {
// 	// when there is no gui, Automation callback does not work,
// 	// so here we monitor when the transport reaches end
//
// 	if (control.now > dur) {
// 		if (autoloopval) {
// 			control.seek; // note, onSeek not called
// 		} {
// 			this.blindControlStop; // stop everything
// 		};
// 	};
// };
