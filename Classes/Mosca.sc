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
* User must set up a project directory with subdirectoties "ir" and "auto"
* irs should have the first 100 or 120ms silenced to act as "tail" reverberators
* and must be placed in the "ir" directory.
* Run help on the "Mosca" class in SuperCollider for detailed information
* and code examples. Further information and sample irs and B-format recordings
* may be downloaded here: http://escuta.org/mosca
*/

/*
* TODO
* Write SynthDef Once
* Multythread evrything
* Higher order binaural
* show Map
*/

Mosca : MoscaBase
{
	*new
	{ | projDir, nsources = 10, dur = 180, irBank, server, parentOssiaNode,
		allCritical = false, decoder, maxorder = 1, speaker_array, outbus = 0,
		suboutbus, rawformat = \FUMA, rawoutbus = 0,
		graphicPath, scaleFactor, fontSize, maxUndo |

		^super.newCopyArgs(dur, server).ctr(projDir, nsources, irBank,
			parentOssiaNode, allCritical, decoder, maxorder, speaker_array,
			outbus, suboutbus, rawformat, rawoutbus, scaleFactor, fontSize, maxUndo);
	}
	ctr
	{ | projDir, nsources, irBank, parentOssiaNode, allCritical, decoder, maxOrder,
		speaker_array, outBus, subOutBus, rawFormat, rawOutBus, scaleFactor, fontSize, maxUndo |

		var multyThread;

		if (server.isNil) { server = Server.local };


		//~osc = OSCFunc({ |msg| msg.postln }, '/tr', server.addr); // debugging

		// TODO
		// Server.program.asString.endsWith("supernova");
		// multyThread = server.options.threads.notNil;
		multyThread = false;

		renderer = MoscaRenderer(maxOrder);

		effects = MoscaEffects(irBank);

		spat = MoscaSpatializer(server);

		srcGrp = Ref();

		// start asynchronious processes
		server.doWhenBooted({

			srcGrp.set(ParGroup.tail(server.defaultGroup));

			renderer.setup(server, speaker_array, maxOrder, decoder,
				outBus, subOutBus, rawOutBus, rawFormat);

			// recording blip synth for synchronisation
			SynthDef(\blip, {
				var env = Env([0, 0.8, 1, 0], [0, 0.1, 0]);
				var blip = SinOsc.ar(1000) * EnvGen.kr(env, doneAction: 2);
				Out.ar(renderer.fumaBus, blip);
			}).send(server);

			effects.setup(server, srcGrp.get(), multyThread, maxOrder, renderer);

			spat.makeSpatialisers(server, maxOrder, effects, renderer, speaker_array);

			effects.finalize(multyThread, server, maxOrder, irBank);

			renderer.launchRenderer(server, server.defaultGroup);

			postln("Ready !");
		});

		// setup ossia parameter tree
		if (parentOssiaNode.isNil)
		{
			ossiaParent = OSSIA_Device("Mosca");
		} {
			ossiaParent = OSSIA_Node(parentOssiaNode, "Mosca");
		};

		this.prSetParam(spat.spatList, allCritical); // global control

		center = OssiaAutomationCenter(ossiaParent, allCritical);
		if (scaleFactor.notNil) { 	center.scale.value_(scaleFactor); };

		sources = Ref(
			Array.fill(nsources,
				{ | i |
					MoscaSource(i, server, srcGrp, ossiaParent, allCritical,
						spat, effects);
				}
			)
		);

		control = Ref(Automation(dur, showLoadSave: false, showSnapshot: true,
			minTimeStep: 0.001));

		this.prSetAction(spat.spatInstances);
		this.prDockTo();
		this.prSetSysex();

		// setup and run underlying synth routine
		watcher = Routine.new({
			var plim = MoscaUtils.plim();

			inf.do({
				0.1.wait;

				sources.get.do({ | item |

					if (item.coordinates.spheVal.rho >= plim)
					{
						if(item.spatializer.get.notNil)
						{
							item.runStop(); // to kill SC input synths
							item.spatializer.get.free;
						};

						item.firstTime = true;
					} {
						if (item.play.value && item.spatializer.get.isNil
							&& item.firstTime)
						{
							item.launchSynth(); // could set the start point for file
							item.firstTime = false;
						};
					};
				});

				if (ossiaPlay.value)
				{
					ossiaSeekBack = false;
					ossiaTransport.v_(control.get.now);
					ossiaSeekBack = true;
				};
			});
		});

		watcher.play;

		if (projDir.isNil)
		{
			control.get.presetDir = "HOME".getenv ++ "/auto/";
		} {
			this.loadAutomation(projDir);
		};
	}

	prSetParam
	{ | spatList, allCritical |

		ossiaMasterPlay = OSSIA_Parameter(ossiaParent, "Play_all", Boolean,
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

		ossiaSync = OSSIA_Parameter(ossiaAutomation, "Sync_files", Boolean,
			critical:true, repetition_filter:true);

		ossiaTrack = OSSIA_Parameter(ossiaParent, "Track_Centre", Boolean, default_value:true);
	}

	prSetAction
	{ | spatDefs |

		ossiaMasterPlay.callback_({ | v |

			sources.get.do({ | item |
				item.play.v_(v); // not an automation
			})
		});

		ossiaMasterLib.callback_({ | v |

			sources.get.do({ | item |
				item.library.valueAction_(v);
			})
		});

		center.setAction(sources);

		renderer.setAction();

		effects.setAction();

		dependant = { | obj ... loadArgs |

			if ((loadArgs[0] == \tpos) && ossiaSync.v)
			{
				obj.tpos_((control.get.now).max(0));
			};

			if (loadArgs.removeAt(0) == \audio)
			{
				this.prCheckConversion(loadArgs);
			};
		};

		sources.get.do({ | item |
			item.setAction(effects.effectList, spatDefs, center);
			item.addDependant(dependant);
		});

		control.get.onPlay_({
			var startTime;

			ossiaMasterPlay.v_(true);

			"NOW PLAYING".postln;

			if (ossiaLoop.v)
			{
				sources.get.do({ | item | item.firstTime = true });
				"Was looping".postln;
			};

			if(control.get.now < 0)
			{
				startTime = 0
			} {
				startTime = control.get.now
			};

			ossiaPlay.set_(true);
		});

		control.get.onStop_({

			ossiaMasterPlay.v_(false);

			if (ossiaLoop.v.not || (control.get.now.round != dur))
			{
				("I HAVE STOPPED. dur = " ++ dur ++ " now = " ++ control.get.now).postln;
			} {
				("Did not stop. dur = " ++ dur ++ " now = " ++ control.get.now).postln;
				control.get.play;
			};

			ossiaPlay.set_(false);
		});

		if (gui.isNil)
		{ // when there is no gui, Automation callback does not work,
			// so here we monitor when the transport reaches end

			if (control.get.now > dur)
			{
				if (ossiaLoop)
				{
					control.get.seek; // note, onSeek not called
				} {
					control.get.stop; // stop everything
				};
			};
		};

		control.get.onEnd_({ control.get.seek(); });

		ossiaPlay.callback_({ | bool |
			if (bool)
			{
				control.get.play;
			} {
				control.get.stop;
			};
		});

		ossiaTransport.callback_({ | num |
			if (ossiaSeekBack)
			{ control.get.seek(num.value) };
		});

		ossiaRec.callback_({ | bool |
			if (bool.value)
			{
				control.get.enableRecording;
			} {
				control.get.stopRecording;
			};
		});

		ossiaTrack.callback_({ | v |
			var orientation = center.ossiaOrient;
			var origin = center.ossiaOrigin;

			if (v)
			{
				orientation.access_mode_(\bi);
				origin.access_mode_(\bi);
			} {
				orientation.access_mode_(\set);
				origin.access_mode_(\set);

			};
		});
	}

	prDockTo
	{
		center.dockTo(control);
		effects.dockTo(control);

		sources.get.do({ | item | item.dockTo(control); });

		control.get.snapshot; // necessary to call at least once before saving automation
		// otherwise will get not understood errors on load
	}

	prCheckConversion
	{ | loadArgs |

		var newSynth, curentSpat;

		#newSynth, curentSpat = loadArgs;

		if (newSynth)
		{ // evaluate before launching a new spatializer synth
			if (curentSpat == \NONAMBI)
			{
				if (renderer.virtualSetup)
				{
					needVirtualAmbi = needVirtualAmbi + 1;

					if (virtualAmbi.isNil)
					{ // launch virtualAmbi if needed
						virtualAmbi = Synth(\virtualAmbi,
							target:effects.transformGrp).onFree({
							virtualAmbi = nil;
						});
					};
				};
			} {
				if (curentSpat != renderer.format)
				{
					needConvert = needConvert + 1;

					if (convertor.isNil)
					{ // launch converter if needed
						convertor = Synth(\ambiConverter,
							target:effects.transformGrp).onFree({
							convertor = nil;
						});
					};
				};
			};
		} {
			if (curentSpat == \NONAMBI)
			{
				if (renderer.virtualSetup)
				{
					needVirtualAmbi = needVirtualAmbi - 1;

					if (virtualAmbi.notNil && (needVirtualAmbi == 0))
					{ virtualAmbi.free }; // free virtualAmbi if no longer needed
				};
			} {
				if (curentSpat != renderer.format)
				{
					needConvert = needConvert - 1;

					if (convertor.notNil && (needConvert == 0))
					{ convertor.free }; // free converter if no longer needed
				};
			};
		};
	}

	prSetSysex
	{
		sysex  = { | src, sysex |
			// This should be more elaborate - other things might trigger it...fix this!
			if (sysex[3] == 6)
			{
				("We have : " ++ sysex[4] ++ " type action").postln;

				switch (sysex[4],
					1,
					{
						"Stop".postln;
						control.get.stop;
					},
					2,
					{
						"Play".postln;
						control.get.play;
					},
					3,
					{
						"Deffered Play".postln;
						control.get.play;
					},
					68,
					{
						var goto;

						("Go to event: " ++ sysex[7] ++ "hr " ++ sysex[8] ++ "min "
							++ sysex[9] ++ "sec and " ++ sysex[10] ++ "frames").postln;

						goto =  (sysex[7] * 3600) + (sysex[8] * 60) + sysex[9] +
						(sysex[10] / 30);

						control.get.seek(goto);
				});
			};
		};
	}

	prBlips
	{
		Routine.new({

			4.do({
				Synth(\blip);
				1.wait;
			});

			yieldAndReset(true);

		}).play;
	}
}
