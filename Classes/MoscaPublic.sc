+ Mosca {

	saveData { | directory |

		if (directory.notNil) {
			switch (directory.pathExists,
				{ false }, {
					("mkdir -p" + directory).systemCmd;
					control.presetDir = directory;
				},
				{ \file }, { ^Error(directory + "is a FILE, not a DIRECTORY").throw; });
		};

		control.save(control.presetDir);
	}

	loadData { | directory |

		if (directory.notNil) {
			switch (directory.pathExists,
				{ false }, { ^Error(directory + "does not exist").throw; },
				{ \file }, { ^Error(directory + "is a FILE, not a DIRECTORY").throw; },
				{ control.presetDir = directory; });
		};

		control.load(control.presetDir);

		// seek at curent time to apply the values loaded
		control.seek(control.now);
	}

	// These methods relate to control of synths when SW Input delected
	// Set by user. Registerred functions called by "play"

	embedSynth { | sourceNum = 1, numChans = 1, triggerFunc, stopFunc, register |

		var src = this.getSource(sourceNum);

		src.nChan.value_(numChans);
		src.scSynths.value_(true);
		src.triggerFunc = triggerFunc;
		src.stopFunc = stopFunc;
		src.synthRegistry.clear;
		src.synthRegistry.add(register);
	}

	getSCBus { | sourceNum = 1, numChans = 1 |

		var src = this.getSource(sourceNum);

		src.nChan.value_(numChans);
		src.scSynths.value_(true);
		^src.scInBus.index;
	}

	getSource { | sourceNum = 1 |

		if ((sourceNum < 1) || sourceNum >= sources.size) {
			^Error("Wrong source index").throw;
		} {
			^sources[sourceNum -1];
		}
	}
}
