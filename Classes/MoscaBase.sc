MoscaBase // acts as the public interface
{
	var dur, <autoLoop, server, <ossiaParent, gui, tracker; // initial rguments
	var renderer, effects, center, sources, srcGrp;
	var convertor, virtualAmbi, needConvert = 0, needVirtualAmbi = 0;
	var ossiaMasterPlay, ossiaMasterLib, ossiaTrack, dependant;
	var <control, sysex, <slaved = false, ossiaAutomation, ossiaPlay, // automation control
	ossiaLoop, ossiaTransport, ossiaRec, ossiaSeekBack, watcher;

	saveData
	{ | directory |

		if (directory.notNil)
		{
			switch (directory.pathExists,
				false,
				{
					("mkdir -p" + directory).systemCmd;
					control.presetDir = directory;
				},
				\file,
				{ ^Error(directory + "is a FILE, not a DIRECTORY").throw; });
		};

		control.save(control.presetDir);
	}

	loadData
	{ | directory |

		if (directory.notNil)
		{
			switch (directory.pathExists,
				false,
				{ ^Error(directory + "does not exist").throw; },
				\file,
				{ ^Error(directory + "is a FILE, not a DIRECTORY").throw; },
				{ control.presetDir = directory; });
		};

		control.load(control.presetDir);

		// seek at curent time to apply the values loaded
		control.seek(control.now);
	}

	slaveToMMC
	{ | enable = false |

		if (enable)
		{
			if (slaved.not)
			{
				"Slaving transport to MMC".postln;
				MIDIIn.addFuncTo(\sysex, sysex);
			};

			slaved = true;
		} {
			if (slaved)
			{
				"MIDI input closed".postln;
				MIDIIn.removeFuncFrom(\sysex, sysex);
			};

			slaved = false;
		};
	}

	inputFile
	{ | sourceNum = 1, path = "", stream = false |

		var src = this.prGetSource(sourceNum);

		src.file.value_(path);
		src.stream.value_(stream);
	}

	inputExternal
	{ | sourceNum = 1, numChanels = 1, boolean = false |

		var src = this.prGetSource(sourceNum);

		src.nChan.value_(numChanels);
		src.external.value_(boolean);
	}

	inputScSynths
	{ | sourceNum = 1, numChanels = 1, boolean = false |

		var src = this.prGetSource(sourceNum);

		src.nChan.value_(numChanels);
		src.scSynths.value_(boolean);
	}

	setCartesian
	{ | sourceNum = 1, x = 0, y = 20, z = 0 |

		var src = this.prGetSource(sourceNum);

		src.coordinates.cartesian.value_([x, y, z]);
	}

	setAzElDist
	{ | sourceNum = 1, azimuth = 0, elevation = 0, distance = 20 |

		var src = this.prGetSource(sourceNum);

		src.coordinates.azElDist.value_([azimuth, elevation, distance]);
	}

	setLibrary
	{ | sourceNum = 1, library = "Ambitools" |

		var src = this.prGetSource(sourceNum);

		src.library.value_(library);
	}

	play
	{ | sourceNum = 1, boolean = false |

		var src = this.prGetSource(sourceNum);

		src.play.value_(boolean);
	}

	setLoop
	{ | sourceNum = 1, boolean = false |

		var src = this.prGetSource(sourceNum);

		src.loop.value_(boolean);
	}

	setLevel
	{ | sourceNum = 1, db = 0 |

		var src = this.prGetSource(sourceNum);

		src.level.value_(db);
	}

	setContraction
	{ | sourceNum = 1, contraction = 1 |

		var src = this.prGetSource(sourceNum);

		src.contraction.value_(contraction);
	}

	setDoppler
	{ | sourceNum = 1, amount = 0 |

		var src = this.prGetSource(sourceNum);

		src.doppler.value_(amount);
	}

	setGlobalAmount
	{ | sourceNum = 1, amount = 0 |

		var src = this.prGetSource(sourceNum);

		src.globalAmount.value_(amount);
	}

	setLocalEffect
	{ | sourceNum = 1, effect = "Clear" |

		var src = this.prGetSource(sourceNum);

		src.localEffect.value_(effect);
	}

	setLocalAmount
	{ | sourceNum = 1, amount = 0 |

		var src = this.prGetSource(sourceNum);

		src.localAmount.value_(amount);
	}

	setLocalDelay
	{ | sourceNum = 1, delay = 0.5 |

		var src = this.prGetSource(sourceNum);

		src.localDelay.value_(delay);
	}

	setLocalDecay
	{ | sourceNum = 1, decay = 0.5 |

		var src = this.prGetSource(sourceNum);

		src.localDecay.value_(decay);
	}

	setStereoAngle
	{ | sourceNum = 1, angle = 60 |

		var src = this.prGetSource(sourceNum);

		src.angle.value_(angle);
	}

	setBFormatRotation
	{ | sourceNum = 1, rotation = 0 |

		var src = this.prGetSource(sourceNum);

		src.rotation.value_(rotation);
	}

	setBFormatDirectivity
	{ | sourceNum = 1, directivity = 0 |

		var src = this.prGetSource(sourceNum);

		src.directivity.value_(directivity);
	}

	setAtkSpread
	{ | sourceNum = 1, boolean = false |

		var src = this.prGetSource(sourceNum);

		src.spread.value_(false);
	}

	setAtkDiffuse
	{ | sourceNum = 1, boolean = false |

		var src = this.prGetSource(sourceNum);

		src.diffuse.value_(false);
	}

	setJoshRate
	{ | sourceNum = 1, rate = 10 |

		var src = this.prGetSource(sourceNum);

		src.rate.value_(rate);
	}

	setJoshWindow
	{ | sourceNum = 1, size = 0.1 |

		var src = this.prGetSource(sourceNum);

		src.window.value_(size);
	}

	setJoshRAndom
	{ | sourceNum = 1, randomize = 0 |

		var src = this.prGetSource(sourceNum);

		src.random.value_(randomize);
	}

	setAuxilary
	{ | sourceNum = 1, auxNum = 1, value = 0 |

		var i, src = this.prGetSource(sourceNum);

		i = prGetAuxIndex(auxNum);

		src.aux[i].value_(value);
	}

	setAuxCheck
	{ | sourceNum = 1, auxNum = 1, boolean = false |

		var i, src = this.prGetSource(sourceNum);

		i = prGetAuxIndex(auxNum);

		src.check[i].value_(boolean);
	}

	prGetSource
	{ | sourceNum |

		if (sourceNum.isInteger)
		{
			^Error("index must be an Integer").throw;
		} {
			^sources[(sourceNum.clip(1, sources.size)) - 1];
		};
	}

	prGetAuxIndex
	{ | auxNum |

		if (auxNum.isInteger)
		{
			^Error("index must be an Integer").throw;
		} {
			^(auxNum.clip(1, 5) - 1);
		};
	}

	// These methods relate to control of synths when SW Input delected
	// Set by user. Registerred functions called by "play"

	embedSynth
	{ | sourceNum = 1, numChans = 1, triggerFunc, stopFunc, register |

		var src = this.prGetSource(sourceNum);

		src.nChan.value_(numChans);
		src.scSynths.value_(true);
		src.triggerFunc = triggerFunc;
		src.stopFunc = stopFunc;
		src.synthRegistry.clear;
		src.synthRegistry.add(register);
	}

	clearEmbededSynth
	{ | sourceNum = 1 |

		var src = this.prGetSource(sourceNum);

		src.triggerFunc = nil;
		src.stopFunc = nil;
		src.synthRegistry.clear;
	}

	getSCBus
	{ | sourceNum = 1, numChans = 1 |

		var src = this.prGetSource(sourceNum);

		src.nChan.value_(numChans);
		src.scSynths.value_(true);
		^src.scInBus.index;
	}

	gui
	{ | size = 800, lag = 0.05, palette = \ossia |

		if (gui.isNil)
		{
			gui = MoscaGUI(this, sources, effects, size, palette, lag);

			gui.win.onClose({
				gui.free;
				gui = nil;
			});
		}
	}

	headTracker
	{ | port = "/dev/ttyUSB0", offsetheading = 0, type = \orient, extraArgs |

		if (tracker.isNil)
		{
			switch (type,
				\orient,
				{ tracker = HeadTrackerGPS(center, ossiaTrack, port, offsetheading) },
				\gps,
				{ tracker = HeadTracker(center, ossiaTrack, port, offsetheading) },
				\pozyxOSC,
				{ tracker = PozyxOSC(center, ossiaTrack, port, extraArgs) }
			);
		}
	}

	freeHeadTracker
	{
		if (tracker.notNil)
		{
			tracker.free();
			tracker = nil;
		};
	}

	track
	{ | enable = true |

		if (ossiaTrack.value != enable) { ossiaTrack.value = enable };
	}
}
