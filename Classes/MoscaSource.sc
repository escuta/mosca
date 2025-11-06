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
* User must set up a project directory with subdirectoties "rir" and "auto"
* RIRs should have the first 100 or 120ms silenced to act as "tail" reverberators
* and must be placed in the "rir" directory.
* Run help on the "Mosca" class in SuperCollider for detailed information
* and code examples. Further information and sample RIRs and B-format recordings
* may be downloaded here: http://escuta.org/mosca
*/

MoscaSource[]
{
	var <index, server, srcGrp, spatInstances, effectInstances, defName;
	var <chanNum = 1, spatType, curentSpat;
	var <spatializer, synths, buffer; // communicatin with the audio server
	var embeddedSynthRef;
	var <scInBus, <>triggerFunc, <>firstTime; // sc synth specific
	// common automation and ossia parameters
	var input, <file, <stream, <scSynths, <external, <nChan, sRate, <busInd, >tpos = 0; // inputs types
	var <src, <coordinates, <library, <localEffect, <localAmount, <localDelay, <localDecay;
	var <play, <loop, <level, <contraction ,<doppler, <globalAmount, <ox;
	var <reach;
	var <angle, <rotation, <extraParams; // input specific parameters
	var <josh, <rate, <window, <random; // joshGrain specific parameters
	var <auxiliary, <aux, <check, orientation;

	*new
	{ | index, server, sourceGroup, ossiaParent, allCritical, spat, effects |

		^super.newCopyArgs(index, server, sourceGroup,
			spat.spatInstances, effects.effectInstances).ctr(
			ossiaParent, allCritical, spat.spatList, effects.effectList);
	}

	ctr
	{ | ossiaParent, allCritical, spatList, effectList |

		src = OSSIA_Node(ossiaParent, "Source_" ++ (index + 1));

		input = OSSIA_Node(src, "Input");

		file = OssiaAutomationProxy(input, "File_path", String, critical: true, repetition_filter: false);

		file.node.description_("For loading or streaming sound files");

		stream = OssiaAutomationProxy(input, "Stream", Boolean, critical: true);

		stream.node.description_("Prefer loading smaler files and streaming when they exceed 6 minutes");

		loop = OssiaAutomationProxy(input, "Loop", Boolean, critical:true);


		external = OssiaAutomationProxy(input, "External", Boolean, critical: true);

		external.node.description_("External input, harware or software");

		scSynths = OssiaAutomationProxy(input, "SCSynths", Boolean, critical: true);

		scSynths.node.description_("Launch SC Synths");

		nChan = OssiaAutomationProxy(input, "Chanels", Integer,
			[ nil, nil, MoscaUtils.channels() ], 1, critical: true, repetition_filter: false);

		nChan.node.description_("number of channels for SC or External inputs");

		busInd = OssiaAutomationProxy(input, "Bus_index", Integer,
			[ 1, (server.options.numInputBusChannels) ], index + 1, 'clip', true, false);
		// start at 1 (instead of 0) to fit jack's port indexes

		busInd.node.description_("Index of the external input bus");

		localEffect = OssiaAutomationProxy(src, "Local_effect", String,
			[nil, nil, effectList], "Clear", critical:true);

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

		coordinates = OssiaAutomationCoordinates(src, allCritical);
		play = OSSIA_Parameter(src, "Play", Boolean, critical:true, repetition_filter:true);

		firstTime = true;

		level = OssiaAutomationProxy(src, "Level", Float, [-96, 12],
			0, 'clip', critical:allCritical);

		level.node.unit_(OSSIA_gain.decibel);

		reach = OssiaAutomationProxy(src, "Reach", Float,
			[0, 1], 1.0, 'clip', critical:allCritical);

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

		extraParams = [];

		spatInstances.get.do({ | item |
			extraParams = extraParams ++ item.setParams(src, allCritical);
		});

		auxiliary = OSSIA_Node(src, "Auxiliary");

		aux = [];

		check = [];

		5.do({ | i |
			aux = aux.add(OssiaAutomationProxy(auxiliary, "Aux_" ++ (i + 1), Float,
				[0, 1], 0, 'clip', critical:allCritical));

			check = check.add(OssiaAutomationProxy(auxiliary, "Ckeck_" ++ (i + 1),
				Boolean, critical:allCritical));
		});

		orientation = ossiaParent.find("Orientation").value;
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

		busInd.action_({

			this.prReloadIfNeeded();
			this.changed(\ctl);
		});

		spatializer = Ref();
		synths = Ref();
		embeddedSynthRef = Ref();
		
		coordinates.setAction(center, spatializer, synths, embeddedSynthRef);

		localEffect.action_({ | val | this.prSetDefName() });

		localAmount.action_({ | val | this.setSynths(\llev, val.value) });

		localDelay.action_({ | val | this.setSynths(\room, val.value) });

		localDecay.action_({ | val | this.setSynths(\damp, val.value) });

		library.action_({ | val |

			spatType = spatInstances.get.at(val.asSymbol).format;

			this.prSetDefName();
		});

		play.callback_({ | val | this.prCheck4Synth(val) }); // not an automation

		loop.action_({ | val |

			this.setSynths(\lp, val.value.asInteger);
			if (val.value) { this.setSynths(\tpos, 0) };
		});

		level.action_({ | val | this.setSynths(\amp, val.value.dbamp) });

		reach.action_({ | val | this.setSynths(\reach, val.value) });
		contraction.action_({ | val | this.setSynths(\contract, val.value) });

		doppler.action_({ | val | this.setSynths(\dopamnt, val.value) });

		globalAmount.action_({ | val | this.setSynths(\glev, val.value) });

		angle.action_({ | val | this.setSynths(\angle, val.value.degrad) });

		//	coordinates.action_({ | val | this.setSynths(\angle, val.value.degrad) });

		rotation.action_({ | val |

			if (chanNum > 2)
			{
				var rotated = orientation;
				this.setSynths(\orientation, orientation[0] + val.value.degrad);
			}
		});

		spatInstances.get.do({ | item |
			item.setAction(src, this);
		});

		aux.do({ | item |
			item.action_({
				this.setSynths(\aux, [aux[0].value, aux[1].value,
					aux[2].value, aux[3].value, aux[4].value]);
			});
		});

		check.do({ | item |
			item.action_({
				this.setSynths(\check, [check[0].value, check[1].value,
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
		reach.dockTo(automation, "reachProxy_" ++ index);
		doppler.dockTo(automation, "dopamtProxy_" ++ index);
		globalAmount.dockTo(automation, "globaamtlProxy_" ++ index);

		localEffect.dockTo(automation, "localProxy_" ++ index);
		localDelay.dockTo(automation, "localDelayProxy_" ++ index);
		localDecay.dockTo(automation, "localDecayProxy_" ++ index);
		localAmount.dockTo(automation, "localAmountProxy_" ++ index);
		angle.dockTo(automation, "angleProxy_" ++ index);
		rotation.dockTo(automation, "rotationProxy_" ++ index);
		contraction.dockTo(automation, "contractionProxy_" ++ index);

		extraParams.do({ | item | item.dockTo(automation,
			item.node.name ++ "Proxy_" ++ index)});

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

			embeddedSynthRef.set(
				triggerFunc.value;
			);
				"RUNNING TRIGGER".postln;
		};
	}

	runStop
	{
		if (triggerFunc.notNil)
		{
			if (embeddedSynthRef.get.notNil) {
				var synth = embeddedSynthRef.get;
				embeddedSynthRef.set(nil);  // Clear reference immediately
				
				// Mute before freeing
				synth.set(\amp, 0, \llev, 0, \glev, 0);
				synth.free;
			};
			
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
		reach.free;
		doppler.free;
		globalAmount.free;
		localEffect.free;
		localDelay.free;
		localDecay.free;
		localAmount.free;

		angle.free;
		rotation.free;

		extraParams.do({ | item | item.free });

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

			if (chanNum == 2)
			{
				args = args ++ [\angle, angle.value.degrad];
			};

			if (chanNum > 2)
			{
				args = args ++ [\orientation, orientation[0] + rotation.value.degrad]
			};

			args = args ++ effectInstances.get.at(localEffect.value.asSymbol)
				.getArgs(localEffect.node, chanNum);

			if (scSynths.value)
			{
				args = args ++ [\busini, scInBus];
				this.runTrigger();
			};

			if (external.value) { args = args ++ [\busini, busInd.value] };

			curentSpat = spatType;

			if ((file.value != "") && (scSynths.value || external.value).not)
			{
				var startFrame = 0;

				this.changed(\tpos); // fetch mosca's time for syncing files

				startFrame = sRate * tpos;


if (stream.value)
{
var bufferValid = false;
    this.prFreeBuffer();
    
    if(File.exists(file.value), {
        buffer = Buffer.cueSoundFile(
            server, file.value, startFrame, chanNum, 131072,
            { | buf |
                if(buf.notNil and: { buf.numFrames > 0 }, {
                    bufferValid = true;
                    "✓ Created streaming buffer for source %".format(index + 1).postln;
                }, {
                    "ERROR: Failed to create streaming buffer for source %".format(index + 1).postln;
                    bufferValid = false;
                });
            }
        );
    }, {
        "ERROR: Cannot stream - file does not exist: %".format(file.value).postln;
        buffer = nil;
        bufferValid = false;
    });
};
				args = args ++ [
					\bufnum, buffer.bufnum,
					\lp, loop.value.asInteger,
					\tpos, startFrame
				]
				// WARNING is evrything syncked ?
			};

			args = args ++ spatInstances.get.at(library.value.asSymbol)
				.getArgs(src, chanNum);

			this.changed(\audio, true, curentSpat); // triggers Mosca's prCheckConversion method
			spatializer.set(Synth(defName, // launch spatializer synth
				[
					\outBus, spatInstances.get.at(library.value.asSymbol).busses,
					\radAzimElev,
					[
						coordinates.spheVal.rho,
						coordinates.spheVal.theta,
						coordinates.spheVal.phi
					],
					\contract, contraction.value,
					\dopamnt, doppler.value,
					\glev, globalAmount.value,
					\amp, level.value.dbamp,
					\reach, reach.value
				] ++ args,
				srcGrp.get()
				).onFree({
					this.changed(\audio, false, curentSpat);
					spatializer.set(nil);
				});
			);
		};
	}

	setSynths
	{ | param, value |

		if (spatializer.get.notNil) { spatializer.get.set(param, value) };

		if (synths.get.notNil) { synths.get.do({ _.set(param, value) }) };

		if (scSynths.value && embeddedSynthRef.get.notNil) { embeddedSynthRef.get.set(param, value)};   
	}

	orientation_
	{ | newOrient |

		orientation = newOrient;

		if (chanNum > 2)
		{
			this.setSynths(\orientation, [ (orientation[0] + rotation.value.degrad),
				orientation[1].neg, orientation[2].neg ]);
		};
		if (embeddedSynthRef.get.notNil) {
			embeddedSynthRef.get.set(\orientation, [ (orientation[0] + rotation.value.degrad),
				orientation[1].neg, orientation[2].neg ]);
		}
	}

	/*
	//test
	cartesian_
	{ | newCartesian |

		cartesian = newCartesian;

		if (embeddedSynth.notNil) {
			embeddedSynth.set(\cartesian, [ cartesian[0],
				cartesian[1].neg, cartesian[2].neg ]);
		}
		}
	*/

	getLibParams
	{
		^spatInstances.get.at(library.value.asSymbol)
		.getParams(src, chanNum);
	}

	//-------------------------------------------//
	//              private methods              //
	//-------------------------------------------//

	prSetDefName
	{
		var playType;

		if ((file.value != "") && (scSynths.value || external.value).not)
		{
			var sf = SoundFile.openRead(file.value);

			if (sf.isNil)
			{
				// if the fie isn't found, look in the project parent directory
				var preset = PathName(file.auto.presetDir).parentPath;

				SoundFile.openRead(preset ++ Pathname(file.value).fileName);

				if (sf.isNil) { ^Error("incorrect file path").throw }
			};

			chanNum = sf.numChannels;
			sRate = sf.sampleRate;
			sf.close;

			if (stream.value)
			{
				playType = "Stream";
			} {
				if(File.exists(file.value), {
					buffer = Buffer.read(server, file.value, action: { | buf |
						if(buf.numFrames == 0, {
							"ERROR: File loaded but contains no audio data: %".format(file.value).postln;
						}, {
							"✓ Loaded: % (% ch, %s)".format(
								PathName(file.value).fileName,
								buf.numChannels, 
								buf.duration.round(0.01)
							).postln;
						});
					});
					playType = "File";
				}, {
					"ERROR: File does not exist: %".format(file.value).postln;
					buffer = nil;
					playType = "File";
				});
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
			var effect = effectInstances.get.at(localEffect.value.asSymbol);
			
			if (effect.isNil) {
				("ERROR: Effect not found: " ++ localEffect.value).postln;
				defName = nil;
			} {
				defName = library.value ++ playType ++ chanNum ++ effect.key;
				this.prReloadIfNeeded();
			};
		};
		this.changed(\ctl);

		defName.postln;
	}

prReloadIfNeeded
{// if the synth is playing, stop and relaunch it
	if (spatializer.get.notNil && play.v)
	{
		var synth = spatializer.get;
		spatializer.set(nil);
		// SMOOTH FADEOUT instead of instant mute
		synth.set(\gate, 0); // Use gate instead of forcing to 0
		firstTime = true;
	};
	if (embeddedSynthRef.get.notNil && play.v)
	{
		var synth = embeddedSynthRef.get;
		embeddedSynthRef.set(nil);
		"Switching off!".postln;
		// SMOOTH FADEOUT
		synth.set(\gate, 0);
	};
}
	
prCheck4Synth
{ | bool |

	if (bool)
	{
		var playlim = MoscaUtils.plim() * reach.value;
		if (spatializer.get.isNil && (coordinates.spheVal.rho < playlim))
		{
			this.launchSynth();
			firstTime = false;
		};
	} {
		if (spatializer.get.notNil)
		{
			var synth = spatializer.get;
			spatializer.set(nil);
			// SMOOTH FADEOUT instead of instant mute
			synth.set(\gate, 0); // Let envelope handle the fadeout
			this.runStop();
			firstTime = true;
			("Source " + (index + 1) + " stopping with fadeout!").postln;
		};
	};
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
