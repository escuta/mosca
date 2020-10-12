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

MoscaSource[] {
	var index, server, srcGrp, defName, synthArgs, effect, chanNum, playType, spatType;
	var <spatializer, synths, buffer; // communicatin with the audio server
	var scInBus, triggerFunc, stopFunc, synthRegistry, <>firstTime; // sc synth specific
	// common automation and ossia parameters
	var src, <coordinates, <play, <loop, <library, <level, <contraction ,<doppler;
	var input, <file, <stream, <scSynths, <external, <nChan; // inputs types
	var <globalAmount, <localEffect, <localAmount, <localDelay, <localDecay;
	var <angle, <rotation, <directivity; // input specific parameters
	var atk, <spread, <diffuse; // atk specific parameters
	var josh, <rate, <window, <random; // joshGrain specific parameters
	var auxiliary, <aux, <check;

	*new { | index, server, sourceGroup, ossiaParent, allCritical, spatList, effectList, center |
		^super.newCopyArgs(index, server, sourceGroup).ctr(
			ossiaParent, allCritical, spatList, effectList, center);
	}

	ctr { | ossiaParent, allCritical, spatList, effectList, center |

		src = OSSIA_Node(ossiaParent, "Source_" ++ (index + 1));

		input = OSSIA_Node(src, "Input");

		file = OssiaAutomationProxy(input, "File_path", String, critical: true);

		file.node.description_("For loading or streaming sound files");

		stream = OssiaAutomationProxy(input, "Stream", Boolean, critical: true);

		stream.node.description_("Prefer loading smaler files and streaming when they excid 6 minutes");

		external = OssiaAutomationProxy(input, "External", Boolean, critical: true);

		external.node.description_("External input, harware or software");

		scSynths = OssiaAutomationProxy(input, "SCSynths", Boolean, critical: true);

		scSynths.node.description_("Launch SC Synths");

		nChan = OssiaAutomationProxy(input, "Chanels", Integer,
			[nil, nil, [1, 2, 4, 9, 16, 25]], 1, critical: true, repetition_filter: false);

		nChan.node.description_("number of channels for SC or External inputs");

		coordinates = OssiaAutomationCoordinates(src, allCritical, center, spatializer, synths);

		library = OssiaAutomationProxy(src, "Library", String, [nil, nil, spatList],
			"Ambitools", critical:true, repetition_filter:true);

		library.node.description_(spatList.asString);

		spatType = \N3D;

		play = OssiaAutomationProxy(src, "play", Boolean,
			critical:true, repetition_filter:true);

		firstTime = true;

		loop = OssiaAutomationProxy(src, "Loop", Boolean,
			critical:true, repetition_filter:true);

		level = OssiaAutomationProxy(src, "Level", Float, [-96, 12],
			0, 'clip', critical:allCritical, repetition_filter:true);

		level.node.unit_(OSSIA_gain.decibel);

		contraction = OssiaAutomationProxy(src, "Contraction", Float,
			[0, 1], 1.0, 'clip', critical:allCritical, repetition_filter:true);

		doppler = OssiaAutomationProxy(src, "Doppler_amount", Float,
			[0, 1], 0, 'clip', critical:allCritical, repetition_filter:true);

		globalAmount = OssiaAutomationProxy(src, "Global_amount", Float,
			[0, 1], 0, 'clip', critical:allCritical, repetition_filter:true);

		globalAmount.node.unit_(OSSIA_gain.linear);

		localEffect = OssiaAutomationProxy(src, "Local_effect", String,
			[nil, nil, effectList], "Clear", critical:true, repetition_filter:true);

		effect = "Clear";

		localEffect.node.description_(effectList.asString);

		localAmount = OssiaAutomationProxy(localEffect.node, "Local_amount", Float,
			[0, 1], 0, 'clip', critical:allCritical, repetition_filter:true);

		localAmount.node.unit_(OSSIA_gain.linear);

		localDelay = OssiaAutomationProxy(localEffect.node, "Room_delay", Float,
			[0, 1], 0.5, 'clip', critical:allCritical, repetition_filter:true);

		localDecay = OssiaAutomationProxy(localEffect.node, "Damp_decay", Float,
			[0, 1], 0.5, 'clip', critical:allCritical, repetition_filter:true);

		angle = OssiaAutomationProxy(src, "Stereo_angle", Float,
			[0, pi], 1.05, 'clip', critical:allCritical, repetition_filter:true);

		angle.node.unit_(OSSIA_angle.radian).description_("Stereo_only");

		rotation = OssiaAutomationProxy(src, "B-Format_rotation", Float,
			[-pi, pi], 0, 'wrap', critical:allCritical, repetition_filter:true);

		rotation.node.unit_(OSSIA_angle.radian).description_("B-Format only");

		atk = OSSIA_Node(src, "Atk");

		directivity = OssiaAutomationProxy(atk, "Directivity", Float,
			[0, pi * 0.5], 0, 'clip', critical:allCritical, repetition_filter:true);

		directivity.node.description_("ATK B-Format only");

		spread = OssiaAutomationProxy(atk, "Spread", Boolean,
			critical:true, repetition_filter:true);

		spread.node.description_("ATK only");

		diffuse = OssiaAutomationProxy(atk, "Diffuse", Boolean,
			critical:true, repetition_filter:true);

		diffuse.node.description_("ATK only");

		josh = OSSIA_Node(src, "Josh");

		rate = OssiaAutomationProxy(josh, "Grain_rate", Float,
			[1, 60], 10, 'clip', critical:allCritical, repetition_filter:true);

		rate.node.unit_(OSSIA_time.frequency).description_("JoshGrain only");

		window = OssiaAutomationProxy(josh, "Window_size", Float,
			[0, 0.2], 0.1, 'clip', critical:allCritical, repetition_filter:true);

		window.node.unit_(OSSIA_time.second).description_("JoshGrain only");

		random = OssiaAutomationProxy(josh, "Randomize_window", Float,
			[0, 1], 0, 'clip', critical:allCritical, repetition_filter:true);

		random.node.description_("JoshGrain only");

		auxiliary = OSSIA_Node(src, "Auxiliary");

		aux = Array.fill(5,
			{ | i | OssiaAutomationProxy(auxiliary, "Aux_" ++ (i + 1), Float,
			[0, 1], 0, 'clip', critical:allCritical, repetition_filter:true);
		});

		check = Array.fill(5,
			{ | i | OssiaAutomationProxy(auxiliary, "Ckeck_" ++ (i + 1),
				Boolean, critical:allCritical, repetition_filter:true);
		});
	}

	setAction { | effectList, spatDefs, center, playing |

		file.action_({ | path |

			if (buffer.notNil) {
				buffer.freeMsg({
					"Buffer freed".postln;
					if (path == "") { buffer = nil; };
				});
			};

			if (path != "") {
				var sf = SoundFile.new;

				sf.openRead(path);

				if (stream.value.not) {

					buffer = Buffer.read(server, path.value, action: { | buf |
						"Loaded file".postln;
					});

					playType = "File";
				} {
					buffer = Buffer.cueSoundFile(
						server, path.value, 0, nChan.value, 131072,
						{("Creating buffer for source: " + (index + 1)).postln; });

					playType = "Stream";
				};

				chanNum = sf.numChannels;
				sf.close;
				scSynths.value_(false);
				external.value_(false);
			};

			this.prSetDefName();
		});

		external.action_({ | val |

			if (val.value) {
				playType = "EXBus";

				scSynths.value_(false);
				nChan.value_(nChan.value);
			} {
				this.prSetDefName();
			}
		});

		scSynths.action_({ | val |

			if (val.value) {
				external.value_(false);
				playType = "SWBus";
				nChan.value_(nChan.value);
				scInBus = Bus.audio(server, chanNum);
			} {
				if (scInBus.notNil) {
					scInBus.free;
					scInBus = nil;
				};

				triggerFunc = nil;
				stopFunc = nil;
				synthRegistry.clear;

				this.prSetDefName();
			};
		});

		nChan.action_({ | val |

			if (scSynths.value || external.value) { chanNum = val.value; };

			if (chanNum.value < 4) {
				contraction.value_(1);
			} {
				contraction.value_(0.5);
			};

			this.prSetDefName();
		});

		library.action_({ | val |
			var i = spatDefs.detectIndex({ | item | item.key == val.value });

			spatType = spatDefs[i].format;

			this.prSetDefName();
			this.prSetSynthArgs();
		});

		play.action_({ | val | this.prCheck4Synth(val.value, playing); });

		loop.action_({ | val | this.setSynths(\lp, val.value.asInt); });

		level.action_({ | val | this.setSynths(\amp, val.value.dbamp); });

		contraction.action_({ | val | this.setSynths(\contr, val.value); });

		doppler.action_({ | val | this.setSynths(\dopamnt, val.value); });

		globalAmount.action_({ | val | this.setSynths(\glev, val.value); });

		localEffect.action_({ | val |

			if (val.value != "Clear") {
				var i = localEffect.node.domain.values().detectIndex(
					{ | item | item == val.value; });

				if (effectList[i].class == String) {
					effect = val.value;
					this.prSetSynthArgs();
				} {
					effect = "Conv";
					this.prSetSynthArgs(effectList[i]);
				};
			} {
				effect = "Clear";
				this.prSetSynthArgs();
			};

			this.prSetDefName();
		});

		localAmount.action_({ | val | this.setSynths(\llev, val.value); });

		localDelay.action_({ | val | this.setSynths(\room, val.value); });

		localDecay.action_({ | val | this.setSynths(\damp, val.value); });

		angle.action_({ | val | this.setSynths(\angle, val.value); });

		rotation.action_({ | val |
			this.setSynths(\rotAngle, val.value  + center.heading.value);
		});

		directivity.action_({ | val | this.setSynths(\directang, val.value); });

		spread.action_({ | val |
			this.setSynths(\sp, val.value.asInteger);

			if (val.value) { diffuse.value_(false) };
		});

		diffuse.action_({ | val |
			this.setSynths(\df, val.value.asInteger);

			if (val.value) { spread.value_(false) };
		});

		rate.action_({ | val | this.setSynths(\grainrate, val.value); });

		window.action_({ | val | this.setSynths(\winsize, val.value); });

		random.action_({ | val | this.setSynths(\winrand, val.value); });

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

	prSetDefName {

		if ((file.value == "") && (scSynths.value || external.value).not) {

			defName = nil;
		} {
			defName = library.value ++ playType ++ chanNum ++ effect;
		};

		defName.postln;
	}

	prSetSynthArgs { | effect |
		var args = [];

		if ((file.value != "") && (scSynths.value || external.value).not) {

			args ++ [\bufnum, buffer.bufnum, \lp, loop.value.asInteger];
		};

		switch (library.value,
			"ATK", {
				args ++ [\sp, spread.value.asInteger, \df, diffuse.value.asInteger];
			}, "Josh", {
				args ++ [\grainrate, rate.value, \winsize, window.value,
					\winrand, random.value];
		});

		if (nChan.value >= 4) {

			args ++ [\rotAngle, rotation.value];

			if (library.value == "ATK") { args ++ [\directang, directivity.value]; };
		};

		synthArgs = args;

		synthArgs.postln;
	}

	prCheck4Synth { | bool, playing |

		if(bool) {
			if (playing.get.not && spatializer.isNil && (coordinates.spheVal.rho < MoscaUtils.plim())) {
				this.launchSynth(true);
				firstTime = false;
			};
		} {
			if (playing.get.not && spatializer.notNil) {
				spatializer.free;
				this.runStop();
				firstTime = true;
				("Source_" + index + " stopping!").postln;
			};
		};
	}

	launchSynth { | force |

		if (defName.notNil) {

			this.changed(true, spatType);

			srcGrp.get().postln;

			spatializer = Synth(\truc,
			//spatializer = Synth(defName,
				[
					\radAzimElev, [
						coordinates.spheVal.rho,
						coordinates.spheVal.theta,
						coordinates.spheVal.phi
					],
					\dopamnt, doppler.value,
					\glev, globalAmount.value,
					\amp, level.value.dbamp
				] ++ synthArgs,
				srcGrp.get()).onFree(
				{
					this.changed(false, spatType);
					spatializer = nil;
				}
			);
		};
	}

	setSynths { | param, value |

		if (spatializer.notNil) { spatializer.set(param, value); };

		if (synths.notNil) {
			synths.do({ _.set(param, value); });
		};

	}

	dockTo { | automation |

		coordinates.dockTo(automation, index);

		automation.dock(level, "levelProxy_" ++ index);
		automation.dock(doppler, "dopamtProxy_" ++ index);
		automation.dock(globalAmount, "globaamtlProxy_" ++ index);
		automation.dock(localEffect, "localProxy_" ++ index);
		automation.dock(localDelay, "localDelayProxy_" ++ index);
		automation.dock(localDecay, "localDecayProxy_" ++ index);
		automation.dock(angle, "angleProxy_" ++ index);
		automation.dock(rotation, "rotationProxy_" ++ index);
		automation.dock(directivity, "directivityProxy_" ++ index);
		automation.dock(contraction, "contractionProxy_" ++ index);
		automation.dock(rate, "grainrateProxy_" ++ index);
		automation.dock(window, "windowsizeProxy_" ++ index);
		automation.dock(random, "randomwindowProxy_" ++ index);

		aux.do({ | item, i | automation.dock(item,
			"aux" ++ (i + 1) ++ "Proxy_" ++ index)
		});

		check.do({ | item, i | automation.dock(item,
			"check" ++ (i + 1) ++ "Proxy_" ++ index)
		});
	}

	runStop {
	}
}