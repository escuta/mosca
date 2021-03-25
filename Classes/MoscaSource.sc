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

MoscaSource[]
{
	var <index, server, srcGrp, defName, effect, <chanNum = 1, spatType, curentSpat;
	var <spatializer, synths, buffer; // communicatin with the audio server
	var <scInBus, <>triggerFunc, <>stopFunc, <synthRegistry, <>firstTime; // sc synth specific
	// common automation and ossia parameters
	var input, <file, <stream, <scSynths, <external, <nChan, sRate, <busInd, >tpos = 0; // inputs types
	var <src, <coordinates, <library, <localEffect, <localAmount, <localDelay, <localDecay;
	var <play, <loop, <level, <contraction ,<doppler, <globalAmount;
	var <angle, <rotation, <directivity; // input specific parameters
	var <atk, <spread, <diffuse; // atk specific parameters
	var <josh, <rate, <window, <random; // joshGrain specific parameters
	var <auxiliary, <aux, <check;

	*new
	{ | index, server, sourceGroup, ossiaParent, allCritical, spatList, effectList |

		^super.newCopyArgs(index, server, sourceGroup).ctr(
			ossiaParent, allCritical, spatList, effectList);
	}

	ctr
	{ | ossiaParent, allCritical, spatList, effectList |

		src = OSSIA_Node(ossiaParent, "Source_" ++ (index + 1));

		input = OSSIA_Node(src, "Input");

		file = OssiaAutomationProxy(input, "File_path", String, critical: true, repetition_filter: false);

		file.node.description_("For loading or streaming sound files");

		stream = OssiaAutomationProxy(input, "Stream", Boolean, critical: true);

		stream.node.description_("Prefer loading smaler files and streaming when they excid 6 minutes");

		loop = OssiaAutomationProxy(input, "Loop", Boolean, critical:true);

		external = OssiaAutomationProxy(input, "External", Boolean, critical: true);

		external.node.description_("External input, harware or software");

		scSynths = OssiaAutomationProxy(input, "SCSynths", Boolean, critical: true);

		scSynths.node.description_("Launch SC Synths");

		synthRegistry = List[];

		nChan = OssiaAutomationProxy(input, "Chanels", Integer,
			[ nil, nil, MoscaUtils.channels() ], 1, critical: true, repetition_filter: false);

		nChan.node.description_("number of channels for SC or External inputs");

		busInd = OssiaAutomationProxy(input, "Bus_index", Integer,
			[ 1, (server.options.numInputBusChannels) ], 1, 'clip', true, false); // start at 1 (instead of 0) to fit jack's port indexes

		busInd.node.description_("Index of the external input bus");

		coordinates = OssiaAutomationCoordinates(src, allCritical);

		localEffect = OssiaAutomationProxy(src, "Local_effect", String,
			[nil, nil, effectList], "Clear", critical:true);

		effect = "Clear";

		localEffect.node.description_(effectList.asString);

		localAmount = OssiaAutomationProxy(localEffect.node, "Local_amount", Float,
			[0, 1], 0, 'clip', critical:allCritical);

		localAmount.node.unit_(OSSIA_gain.linear);

		localDelay = OssiaAutomationProxy(localEffect.node, "Room_delay", Float,
			[0, 1], 0.5, 'clip', critical:allCritical);

		localDecay = OssiaAutomationProxy(localEffect.node, "Damp_decay", Float,
			[0, 1], 0.5, 'clip', critical:allCritical);

		library = OssiaAutomationProxy(src, "Library", String, [nil, nil, spatList],
			"Ambitools", critical:true);

		library.node.description_(spatList.asString);

		spatType = \N3D;

		play = OSSIA_Parameter(src, "Play", Boolean, critical:true, repetition_filter:true);

		firstTime = true;

		level = OssiaAutomationProxy(src, "Level", Float, [-96, 12],
			0, 'clip', critical:allCritical);

		level.node.unit_(OSSIA_gain.decibel);

		contraction = OssiaAutomationProxy(src, "Contraction", Float,
			[0, 1], 1.0, 'clip', critical:allCritical);

		doppler = OssiaAutomationProxy(src, "Doppler_amount", Float,
			[0, 1], 0, 'clip', critical:allCritical);

		globalAmount = OssiaAutomationProxy(src, "Global_amount", Float,
			[0, 1], 0, 'clip', critical:allCritical);

		globalAmount.node.unit_(OSSIA_gain.linear);

		angle = OssiaAutomationProxy(src, "Stereo_angle", Float,
			[0, 180], 60, 'clip', critical:allCritical);

		angle.node.unit_(OSSIA_angle.degree).description_("Stereo_only");

		rotation = OssiaAutomationProxy(src, "B-Fmt_rotation", Float,
			[-180, 180], 0, 'wrap', critical:allCritical);

		rotation.node.unit_(OSSIA_angle.degree).description_("B-Format only");

		atk = OSSIA_Node(src, "Atk");

		directivity = OssiaAutomationProxy(atk, "Directivity", Float,
			[0, 90], 0, 'clip', critical:allCritical);

		directivity.node.unit_(OSSIA_angle.degree).description_("ATK B-Format only");

		spread = OssiaAutomationProxy(atk, "Spread", Boolean, critical:true);

		spread.node.description_("ATK only");

		diffuse = OssiaAutomationProxy(atk, "Diffuse", Boolean, critical:true);

		diffuse.node.description_("ATK only");

		josh = OSSIA_Node(src, "Josh");

		rate = OssiaAutomationProxy(josh, "Grain_rate", Float,
			[1, 60], 10, 'clip', critical:allCritical);

		rate.node.unit_(OSSIA_time.frequency).description_("JoshGrain only");

		window = OssiaAutomationProxy(josh, "Window_size", Float,
			[0, 0.2], 0.1, 'clip', critical:allCritical);

		window.node.unit_(OSSIA_time.second).description_("JoshGrain only");

		random = OssiaAutomationProxy(josh, "Random_size", Float,
			[0, 1], 0, 'clip', critical:allCritical);

		random.node.description_("JoshGrain only");

		auxiliary = OSSIA_Node(src, "Auxiliary");

		aux = [];

		check = [];

		5.do({ | i |

			aux = aux.add(OssiaAutomationProxy(auxiliary, "Aux_" ++ (i + 1), Float,
				[0, 1], 0, 'clip', critical:allCritical));

			check = check.add(OssiaAutomationProxy(auxiliary, "Ckeck_" ++ (i + 1),
				Boolean, critical:allCritical));
		});
	}

	setAction
	{ | effectList, spatDefs, center |

		file.action_({ | path |

			this.prFreeBus();
			this.prFreeBuffer();
			this.prSetDefName();
		});

		stream.action_({ file.valueAction_(file.value); });

		external.action_({ | val |

			if (val.value)
			{
				if (scSynths.value)
				{
					scSynths.valueAction_(false);
				} {
					this.prSetDefName();
				};

				this.prFreeBus();
				this.prFreeBuffer();
			} {
				this.prSetDefName();
			};
		});

		scSynths.action_({ | val |

			if (val.value)
			{
				if (external.value)
				{
					external.valueAction_(false);
				} {
					this.prSetDefName();
				};

				this.prFreeBuffer();
			} {
				this.prFreeBus();
				this.prSetDefName();
			};
		});

		nChan.action_({ | val | this.prSetDefName() });

		spatializer = Ref();
		synths = Ref();

		coordinates.setAction(center, spatializer, synths);

		localEffect.action_({ | val |

			var i = localEffect.node.domain.values().detectIndex(
				{ | item | item == val.value });

			effect = effectList[i];

			this.prSetDefName();
		});

		localAmount.action_({ | val | this.prSetSynths(\llev, val.value) });

		localDelay.action_({ | val | this.prSetSynths(\room, val.value) });

		localDecay.action_({ | val | this.prSetSynths(\damp, val.value) });

		library.action_({ | val |
			var i = spatDefs.detectIndex({ | item | item.key == val.value });

			spatType = spatDefs[i].format;

			this.prSetDefName();
		});

		play.callback_({ | val | this.prCheck4Synth(val) }); // not an automation

		loop.action_({ | val | this.prSetSynths(\lp, val.value.asInteger) });

		level.action_({ | val | this.prSetSynths(\amp, val.value.dbamp) });

		contraction.action_({ | val | this.prSetSynths(\contr, val.value) });

		doppler.action_({ | val | this.prSetSynths(\dopamnt, val.value) });

		globalAmount.action_({ | val | this.prSetSynths(\glev, val.value) });

		angle.action_({ | val | this.prSetSynths(\angle, val.value.degrad) });

		rotation.action_({ | val |
			this.prSetSynths(\rotAngle, val.value.degrad  + center.heading.value);
		});

		directivity.action_({ | val | this.prSetSynths(\directang, val.value.degrad) });

		spread.action_({ | val |
			this.prSetSynths(\sp, val.value.asInteger);

			if (val.value) { diffuse.valueAction_(false) };
		});

		diffuse.action_({ | val |
			this.prSetSynths(\df, val.value.asInteger);

			if (val.value) { spread.valueAction_(false) };
		});

		rate.action_({ | val | this.prSetSynths(\grainrate, val.value) });

		window.action_({ | val | this.prSetSynths(\winsize, val.value) });

		random.action_({ | val | this.prSetSynths(\winrand, val.value) });

		aux.do({ | item |
			item.action_({
				this.prSetSynths(\aux, [aux[0].value, aux[1].value,
					aux[2].value, aux[3].value, aux[4].value]);
			});
		});

		check.do({ | item |
			item.action_({
				this.prSetSynths(\check, [check[0].value, check[1].value,
					check[2].value, check[3].value, check[4].value]);
			});
		});
	}

	dockTo
	{ | automation |

		file.dockTo(automation, "fileProxy_" ++ index);
		stream.dockTo(automation, "streamProxy_" ++ index);
		scSynths.dockTo(automation, "scSynthsProxy_" ++ index);
		external.dockTo(automation, "externalProxy_" ++ index);
		nChan.dockTo(automation, "nChanProxy_" ++ index);
		busInd.dockTo(automation, "busIndProxy_" ++ index);
		loop.dockTo(automation, "loopProxy_" ++ index);

		coordinates.dockTo(automation, index);

		library.dockTo(automation, "libraryProxy_" ++ index);
		level.dockTo(automation, "levelProxy_" ++ index);
		doppler.dockTo(automation, "dopamtProxy_" ++ index);
		globalAmount.dockTo(automation, "globaamtlProxy_" ++ index);
		localEffect.dockTo(automation, "localProxy_" ++ index);
		localDelay.dockTo(automation, "localDelayProxy_" ++ index);
		localDecay.dockTo(automation, "localDecayProxy_" ++ index);

		angle.dockTo(automation, "angleProxy_" ++ index);
		rotation.dockTo(automation, "rotationProxy_" ++ index);
		directivity.dockTo(automation, "directivityProxy_" ++ index);
		contraction.dockTo(automation, "contractionProxy_" ++ index);
		rate.dockTo(automation, "grainrateProxy_" ++ index);
		window.dockTo(automation, "windowsizeProxy_" ++ index);
		random.dockTo(automation, "randomwindowProxy_" ++ index);

		aux.do({ | item, i | item.dockTo(automation,
			"aux" ++ (i + 1) ++ "Proxy_" ++ index)
		});

		check.do({ | item, i | item.dockTo(automation,
			"check" ++ (i + 1) ++ "Proxy_" ++ index)
		});
	}

	getSCBus { ^scInBus.index }

	runTrigger
	{
		if (triggerFunc.notNil)
		{
			triggerFunc.value;
			"RUNNING TRIGGER".postln;
		};
	}

	runStop
	{
		if (stopFunc.notNil)
		{
			stopFunc.value;
			synths.set(nil);
			"RUNNING STOP".postln;
		};
	}

	setSCBus
	{
		if (scInBus.notNil)
		{
			if (scInBus.numChannels != chanNum)
			{
				scInBus.free;
				scInBus = Bus.audio(server, chanNum);
			};
		} {
			scInBus = Bus.audio(server, chanNum);
		};
	}

	free
	{
		this.prFreeBuffer();
		this.prFreeBus();

		file.free;
		stream.free;
		scSynths.free;
		external.free;
		nChan.free;
		busInd.free;
		loop.free;

		coordinates.free;

		library.free;
		level.free;
		doppler.free;
		globalAmount.free;
		localEffect.free;
		localDelay.free;
		localDecay.free;

		angle.free;
		rotation.free;
		directivity.free;
		contraction.free;
		rate.free;
		window.free;
		random.free;

		aux.do({ | item, i | item.free });

		check.do({ | item, i | item.free });

		src.free;
		super.free;
	}

	launchSynth
	{
		if (defName.notNil)
		{
			var args = []; // prepare synth Arguments

			switch (nChan.value,
				1,
				{
					if (effect != "Clear")
					{
						args = args ++ [\llev, localAmount.value];

						if (effect.class != String) { args = args ++ effect.wSpecPar }
						{ args = args ++ [\room, localDelay.value, \damp, localDecay.value] }
					}
				},
				2,
				{
					if (effect != "Clear")
					{
						args = args ++ [\llev, localAmount.value];

						if (effect.class != String) { args = args ++ effect.zSpecPar }
						{ args = args ++ [\room, localDelay.value, \damp, localDecay.value] }
					};

					args = args ++ [\angle, angle.value.degrad];
				},
				{
					if (effect != "Clear")
					{
						args = args ++ [\llev, localAmount.value];

						if (effect.class != String) { args = args ++ effect.wSpecPar }
						{ args = args ++ [\room, localDelay.value, \damp, localDecay.value] }
					};

					// no acces to center here
					// args = args ++ [\rotAngle, rotation.value + center.heading.value];

					if (library.value == "ATK") { args = args ++ [\directang, directivity.value] };
				};
			);

			if (scSynths.value)
			{
				args = args ++ [\busini, scInBus];
				this.runTrigger();
			};

			if (external.value) { args = args ++ [\busini, busInd.value] };

			switch (library.value,
				"ATK",
				{ args = args ++ [\sp, spread.value.asInteger, \df, diffuse.value.asInteger] },
				"Josh",
				{
					args = args ++ [\grainrate, rate.value, \winsize, window.value,
						\winrand, random.value];
				}
			);

			if ((file.value != "") && (scSynths.value || external.value).not)
			{
				var startFrame = 0;

				this.changed(\tpos); // fetch mosca's time for syncing files

				tpos.postln;

				startFrame = sRate * tpos;

				if (stream.value)
				{
					this.prFreeBuffer();

					buffer = Buffer.cueSoundFile(
					server, file.value, startFrame, chanNum, 131072,
					{("Creating buffer for source " + (index + 1)).postln; });
				};

				args = args ++ [
					\bufnum, buffer.bufnum,
					\lp, loop.value.asInteger,
					\tpos, startFrame
				]
				// WARNING is evrything syncked ?
			};

			curentSpat = spatType;

			this.changed(\audio, true, curentSpat); // triggers Mosca's prCheckConversion method

			spatializer.set(Synth(defName, // launch spatializer synth
				[
					\radAzimElev,
					[
						coordinates.spheVal.rho,
						coordinates.spheVal.theta,
						coordinates.spheVal.phi
					],
					\contr, contraction.value,
					\dopamnt, doppler.value,
					\glev, globalAmount.value,
					\amp, level.value.dbamp
				] ++ args,
				srcGrp.get()
			).onFree({
				this.changed(\audio, false, curentSpat);
				spatializer.set(nil);
			});
			);
		};
	}


	//-------------------------------------------//
	//              private methods              //
	//-------------------------------------------//

	prSetDefName
	{
		var fxType, playType;

		if ((file.value != "") && (scSynths.value || external.value).not)
		{
			var sf = SoundFile.openRead(file.value);

			if (sf.isNil)
			{
				// if the fie isn't found, look in the project directory
				var preset = PathName(file.auto.presetDir).parentPath;

				SoundFile.openRead(preset ++ Pathname(file.value).fileName);

				if (sf.isNil) { ^Error("incorrect file path").throw }
			};

			chanNum = sf.numChannels;
			sRate = sf.sampleRate;
			sf.close;

			if (stream.value)
			{
				// leave streaming buffer allocation to launher
				playType = "Stream";
			} {
				buffer = Buffer.read(server, file.value, action: { | buf |
					"Loaded file".postln;
				});

				playType = "File";
			};
		} {
			if (external.value)
			{
				playType = "EXBus";
				chanNum = nChan.value;
			};

			if (scSynths.value)
			{
				playType = "SCBus";
				chanNum = nChan.value;
				this.setSCBus();
			};
		};

		if (playType.isNil)
		{
			defName = nil;
		} {

			if (effect.class == String)
			{
				fxType = effect;
			} {
				fxType = "Conv";
			};

			defName = library.value ++ playType ++ chanNum ++ fxType;

			// if the synth is playing, stop and relaunch it
			if (spatializer.get.notNil && play.v)
			{
				spatializer.get.free;
				firstTime = true;
			}
		};

		this.changed(\ctl);

		defName.postln;
	}

	prCheck4Synth
	{ | bool |

		if (bool)
		{
			if (spatializer.get.isNil && (coordinates.spheVal.rho < MoscaUtils.plim()))
			{
				this.launchSynth();
				firstTime = false;
			};
		} {
			if (spatializer.get.notNil)
			{
				spatializer.get.free;
				this.runStop();
				firstTime = true;
				("Source " + (index + 1) + " stopping!").postln;
			};
		};
	}

	prSetSynths
	{ | param, value |

		if (spatializer.get.notNil) { spatializer.get.set(param, value) };

		if (synths.get.notNil) { synths.get.do({ _.set(param, value) }) };
	}

	prFreeBuffer // Always free the buffer before changing configuration
	{
		if (buffer.notNil)
		{
			buffer.freeMsg({ "Buffer freed".postln; });
			buffer = nil;
		};
	}

	prFreeBus // free Synth bus before changing configuration
	{
		if (scInBus.notNil)
		{
			scInBus.free;
			"SC input bus freed".postln;
			scInBus = nil;
		};
	}
}
