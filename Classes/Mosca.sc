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

/*
* TODO
* defName and Synth args for sources
* get clear on conversion (general + 2effects)
* conveterNeeded methode
* API
* Multythread evrything
* GUI
* DBAP https://github.com/woolgathering/dbap
* NHHall
*/

Mosca {
	var dur, autoLoop, server, <ossiaParent; // initial rguments
	var renderer, effects, center, sources, srcGrp, gui, tracker;
	var ossiaMasterPlay, ossiaMasterLib, dependant, convertor, virtualAmbi;
	var <control, watcher, ossiaAutomation, isPlay, // automation control
	ossiaPlay, ossiaLoop, ossiaTransport, ossiaRec, ossiaSeekBack;

	*new { | projDir, nsources = 10, dur = 180, irBank, server, parentOssiaNode,
		allCritical = false, decoder, maxorder = 1, speaker_array, outbus = 0,
		suboutbus, rawformat = \FUMA, rawoutbus, autoloop = false |

		^super.newCopyArgs(dur, autoloop, server).ctr(projDir, nsources, irBank,
			parentOssiaNode, allCritical, decoder, maxorder, speaker_array,
			outbus, suboutbus, rawformat, rawoutbus);
	}

	ctr { | projDir, nsources, irBank, parentOssiaNode, allCritical, decoder, maxOrder,
		speaker_array, outBus, subOutBus, rawFormat, rawOutBus |

		var spat, multyThread = false; // Server.program.asString.endsWith("supernova");

		if (server.isNil) { server = Server.local; };

		renderer = MoscaRenderer(maxOrder);

		spat = MoscaSpatializer(server);

		effects = MoscaEffects();

		// start asynchronious processes
		server.doWhenBooted({

			srcGrp = ParGroup.tail(server.defaultGroup);

			renderer.setup(server, speaker_array, maxOrder, decoder,
				outBus, subOutBus, rawOutBus, rawFormat);

			spat.initSpat(maxOrder, renderer, server);

			effects.setup(server, srcGrp, multyThread, maxOrder, renderer, irBank);

			spat.makeSpatialisers(server, maxOrder, renderer, effects);

			effects.sendFx(multyThread, server);

			renderer.launchRenderer(server, server.defaultGroup);
		});

		// setup ossia parameter tree
		if (parentOssiaNode.isNil) {
			ossiaParent = OSSIA_Device("Mosca");
		} {
			ossiaParent = OSSIA_Node(parentOssiaNode, "Mosca");
		};

		this.prSetParam(spat.spatList, allCritical); // global control

		center = OssiaAutomationCenter(ossiaParent, allCritical);

		sources = Array.fill(nsources,
			{ | i | MoscaSource(i, server, srcGrp, ossiaParent, allCritical,
				spat.spatList, effects.ossiaGlobal.node.domain.values(), center); });

		control = Automation(dur, showLoadSave: false, showSnapshot: true,
			minTimeStep: 0.001);

		isPlay = `false;

		if (projDir.isNil) {
			control.presetDir = "HOME".getenv ++ "/auto/";
		} {
			control.presetDir = projDir;
			control.load(control.presetDir);
		};

		this.prSetAction(spat.defs);

		// setup and run underlying synth routine
		watcher = Routine.new({
			var plim = MoscaUtils.plim();

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
						if((isPlay.get() || item.play.value) && item.spatializer.isNil
							&& item.firstTime) {
							// could set the start point for file
							item.launchSynth(true);
							item.firstTime = false;
						};
					};
				});

				if (isPlay.get) {
					ossiaSeekBack = false;
					ossiaTransport.v_(control.now);
					ossiaSeekBack = true;
				};
			});
		});

		watcher.play;
	}

	prSetParam { | spatList, allCritical |

		ossiaMasterPlay = OSSIA_Parameter(ossiaParent, "Audition_all", Boolean,
			critical:true);

		ossiaMasterLib = OSSIA_Parameter(ossiaParent, "Library_all", String,
			[nil, nil, spatList], "Ambitools", critical:true, repetition_filter:true);

		ossiaMasterLib.description_(spatList.asString);

		renderer.setParam(ossiaParent, allCritical);

		effects.setParam(ossiaParent, allCritical);

		ossiaAutomation = OSSIA_Node(ossiaParent, "Automation");

		ossiaPlay = OSSIA_Parameter(ossiaAutomation, "Play", Boolean,
			critical:true, repetition_filter:true);

		ossiaLoop = OSSIA_Parameter(ossiaAutomation, "Loop", Boolean,
			critical:true, repetition_filter:true);

		ossiaTransport = OSSIA_Parameter(ossiaAutomation, "Transport", Float,
			[0, dur], 0, 'wrap', critical:true, repetition_filter:true);

		ossiaTransport.unit_(OSSIA_time.second);

		ossiaRec = OSSIA_Parameter(ossiaAutomation, "Record", Boolean,
			critical:true, repetition_filter:true);
	}

	prSetAction { | spatDefs |

		center.setAction(sources);

		renderer.setAction();

		effects.setAction();

		dependant = { | obj ... loadArgs | this.prCheck4Convert(loadArgs); };

		sources.do({ | item |
			item.setAction(effects.effectList, spatDefs, center, isPlay);
			item.addDependant(dependant);
		});

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

			isPlay.set(true);

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

				isPlay.set(false);
				ossiaLoop.v_(false);
			} {
				("Did not stop. dur = " ++ dur ++ " now = " ++ control.now).postln;
				ossiaLoop.v_(true);
				control.play;
			};

			ossiaPlay.value_(false);
		});

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

		control.onEnd_({ control.seek(); });

		ossiaPlay.callback_({ | bool |
			if (bool) {
				if (isPlay.get.not) {
					control.play; };
			} {
				if (isPlay.get) {
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
	}

	prDockTo {

		center.dockTo(control);
		effects.dockTo(control);
		renderer.dockTo(control);

		sources.do({ | item | item.dockTo(control); });
	}

	prCheck4Convert { | loadArgs |
		var newSynth, spatType;

		#newSynth, spatType = loadArgs;

		loadArgs.postln;

		if (newSynth) { // evaluate before launching a new spatializer synth

			if (spatType == \NONAMBI) {

				if (renderer.virtualSetup) {

					if (virtualAmbi.isNil) { // launch virtualAmbi if needed

						virtualAmbi = Synth(\virtualAmbi,
							target:effects.transformGrp).onFree({
							virtualAmbi = nil;
						});
					};
				};
			} {
				if (spatType != renderer.format) {

					if (convertor.isNil) { // launch converter if needed

						convertor = Synth(\virtualAmbi,
							target:effects.transformGrp).onFree({
							convertor = nil;
						});
					};
				};
			};
		} {

		};
	}

	nonAmbi2BfNeeded { | i = 0 |

		if (i == sources.size) {
			^false.asBoolean;
		} {
			if (sources[i].toAmbi && sources[i].spatializer.notNil) {
				^true.asBoolean;
			} {
				^this.nonAmbi2BfNeeded(i + 1);
			};
		};
	}

	converterNeeded { | i = 0 |

		if (i == sources.size) {
			^false.asBoolean;
		} {
			if (sources[i].toConvert && sources[i].spatializer.notNil) {
				^true.asBoolean;
			} {
				^this.converterNeeded(i + 1);
			};
		};
	}
}
