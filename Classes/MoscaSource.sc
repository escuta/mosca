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

MoscaSource {
	var index, server, src, <defName, <playType, <nChan, <dstrvTypes;
	var spatializer, synths, buffer; // communicatin with the audio server
	var scInBus, triggerFunc, stopFunc, synthRegistry; // sc synth specific
	// common automation and ossia parameters
	var <coordinates, <audition, <loop, <library, <level, <contraction ,<doppler;
	// reveb parameters
	var file, stream, scSynths, external; // inputs types
	var <globalAmount, <localReverb, <localAmount, <localRoom, <localDamp;
	var <angle, <rotation, <directivity; // input specific parameters
	var <spread, <diffuse; // atk specific parameters
	var <rate, <window, <random; // joshGrain specific parameters

	*new { | index, server, ossiaParent, allCritical, spatList, center |
		^super.newCopyArgs(index, server).ctr(ossiaParent, allCritical, center);
	}

	ctr { | ossiaParent, allCritical, spatList, rirList, center |

		var input, atk, josh;

		src = OSSIA_Node(ossiaParent, "Source_" ++ (index + 1));

		input = OSSIA_Node(ossiaParent, "Input");

		file = OssiaAutomationProxy(input, "File_path", String, critical: true);

		file.param.description_("For loading or streaming sound files");

		stream = OssiaAutomationProxy(input, "Stream", Boolean, critical: true);

		stream.param.description_("Prefer loading smaler files and streaming and streaming when thy excid 6 minutes");

		external = OssiaAutomationProxy(input, "External", Boolean, critical: true);

		external.param.description_("External input, harware or software");

		scSynths = OssiaAutomationProxy(input, "SCSynths", Boolean, critical: true);

		scSynths.param.description_("Launch SC Synths");

		nChan = OssiaAutomationProxy(input, "Chanels", Integer,
			[nil, nil, [1, 2, 4, 9, 16, 25]], critical: true, repetition_filter: false);

		nChan.param.description_("number of channels for SC or external inputs");

		coordinates = OssiaAutomatCoordinates(ossiaParent, allCritical, center, spatializer, synths);

		library = OssiaAutomationProxy(ossiaParent, "Library", String, [nil, nil, spatList],
		spatList.first, critical:true, repetition_filter:true);

		library.param.description_(spatList.asString);

		audition = OssiaAutomationProxy(ossiaParent, "Audition", Boolean,
		critical:true, repetition_filter:true);

		loop = OssiaAutomationProxy(ossiaParent, "Loop", Boolean,
		critical:true, repetition_filter:true);

		level = OssiaAutomationProxy(ossiaParent, "Level", Float, [-96, 12],
		0, 'clip', critical:allCritical, repetition_filter:true);

		level.param.unit_(OSSIA_gain.decibel);

		contraction = OssiaAutomationProxy(ossiaParent, "Contraction", Float,
			[0, 1], 1.0, 'clip', critical:allCritical, repetition_filter:true);

		doppler = OssiaAutomationProxy(ossiaParent, "Doppler_amount", Float,
		[0, 1], 0, 'clip', critical:allCritical, repetition_filter:true);

		globalAmount = OssiaAutomationProxy(ossiaParent, "Global_amount", Float,
			[0, 1], 0, 'clip', critical:allCritical, repetition_filter:true);

		globalAmount.param.unit_(OSSIA_gain.linear);

		localReverb = OssiaAutomationProxy(ossiaParent, "Local_reverb", String,
			[nil, nil, ["no-reverb","freeverb","allpass", "A-format"] ++ rirList],
			"no-reverb", critical:true, repetition_filter:true);

		localReverb.param.description_((["no-reverb","freeverb","allpass", "A-format"]
			++ rirList).asString);

		localAmount = OssiaAutomationProxy(localReverb, "Local_amount", Float,
			[0, 1], 0, 'clip', critical:allCritical, repetition_filter:true);

		localAmount.param.unit_(OSSIA_gain.linear);

		localRoom = OssiaAutomationProxy(localReverb, "Distant_room_delay", Float,
			[0, 1], 0.5, 'clip', critical:allCritical, repetition_filter:true);

		localDamp = OssiaAutomationProxy(localReverb, "Distant_damp_decay", Float,
			[0, 1], 0.5, 'clip', critical:allCritical, repetition_filter:true);

		angle = OssiaAutomationProxy(ossiaParent, "Stereo_angle", Float,
			[0, pi], 1.05, 'clip', critical:allCritical, repetition_filter:true);

		angle.param.unit_(OSSIA_angle.radian).description_("Stereo_only");

		rotation = OssiaAutomationProxy(ossiaParent, "B-Format_rotation", Float,
			[-pi, pi], 0, 'wrap', critical:allCritical, repetition_filter:true);

		rotation.param.unit_(OSSIA_angle.radian).description_("B-Format only");

		atk = OSSIA_Node(ossiaParent, "Atk");

		directivity = OssiaAutomationProxy(atk, "Directivity", Float,
			[0, pi * 0.5], 0, 'clip', critical:allCritical, repetition_filter:true);

		directivity.pram.description_("ATK B-Format only");

		spread = OssiaAutomationProxy(atk, "Spread", Boolean,
			critical:true, repetition_filter:true);

		spread.param.description_("ATK only");

		diffuse = OssiaAutomationProxy(atk, "Diffuse", Boolean,
			critical:true, repetition_filter:true);

		diffuse.description_("ATK only");

		josh = OSSIA_Node(ossiaParent, "Josh");

		rate = OssiaAutomationProxy(josh, "Grain_rate", Float,
			[1, 60], 10, 'clip', critical:allCritical, repetition_filter:true);

		rate.param.unit_(OSSIA_time.frequency).description_("JoshGrain only");

		window = OssiaAutomationProxy(josh, "Window_size", Float,
			[0, 0.2], 0.1, 'clip', critical:allCritical, repetition_filter:true);

		window.param.unit_(OSSIA_time.second).description_("JoshGrain only");

		random = OssiaAutomationProxy(josh, "Randomize_window", Float,
			[0, 1], 0, 'clip', critical:allCritical, repetition_filter:true);

		random.description_("JoshGrain only");

		this.setAction(spatList, rirList, center);
	}

	setAction { | spatList, rirList, center |

		file.action_({ | path |

			if (path != "") {
				var sf = SoundFile.new;
				sf.openRead(path);
				nChan.valueAction_(sf.numChannels);
				sf.close;

				if (stream.value.not) {
					if (buffer.notNil) {
						buffer.freeMsg({
							"Buffer freed".postln;
						});
					};

					buffer = Buffer.read(server, path.value, action: { | buf |
						"Loaded file".postln;
					});
				} {
					"To stream file".postln;
				};
			} {
				if (buffer.notNil) {
					buffer.freeMsg({
						buffer = nil;
						"Buffer freed".postln;
					});
				};
			};
		});

		external.action = { | val |
			if (val.value == true) {
				nChan.valueAction_(nChan.value);
				scSynths.valueAction_(false);
			};
		};

		scSynths.action_({ | val |
			if (val.value == true) {
				nChan.valueAction_(nChan.value);
				external.valueAction_(false);
			}{
				if (scInBus.notNil) {
					scInBus.free;
					scInBus = nil;
				};
				triggerFunc = nil;
				stopFunc = nil;
				synthRegistry.clear;
			};
		});

		library.action_({ | val |
			var index = spatList.detectIndex({ | item | item == val.value });

			defName = val ++ nChan ++ playType ++ localReverb.value;
		});

		audition.action({ | val |
			this.auditionFunc(val.value);
		});

		loop.action_({ | val |
			this.setSynths(\lp, val.value.asInt);
		});

		level.action_({ | val |
			this.setSynths(\amp, val.value.dbamp);
		});

		contraction.action_({ | val |
			this.setSynths(\contr, val.value);
		});

		doppler.action_({ | val |
			this.setSynths(\dopamnt, val.value);
		});

		globalAmount.action_({ | val |
			this.setSynths(\glev, val.value);
		});

		localReverb.action_({ | val |
			var index = (["no-reverb","freeverb","allpass"] ++ rirList).detectIndex({ | item | item == val.value });

			defName = library.value ++ nChan ++ playType ++ val.value;
		});

		localAmount.action_({ | val |
			this.setSynths(\llev, val.value);
		});

		localRoom.action_({ | val |
			this.setSynths(\room, val.value);
		});

		localDamp.action_({ | val |
			this.setSynths(\damp, val.value);
		});

		angle.action_({ | val |
			this.setSynths(\angle, val.value);
		});

		rotation.action_({ | val |
			this.setSynths(\rotAngle, val.value  + center.heading.value);
		});

		directivity.action_({ | val |
			this.setSynths(\directang, val.value);
		});

		spread.action_({ | val |
			this.setSynths(\sp, val.value.asInt);

			if (val.value) { diffuse.value_(false) };
		});

		diffuse.action_({ | val |
			this.setSynths(\df, val.value.asInt);

			if (val.value) { spread.value_(false) };
		});

		rate.action_({ | val |
			this.setSynths(\grainrate, val.value);
		});

		window.action_({ | val |
			this.setSynths(\winsize, val.value);
		});

		random.action_({ | val |
			this.setSynths(\winrand, val.value);
		});
	}
}