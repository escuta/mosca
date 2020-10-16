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

/*
	// These methods relate to control of synths when SW Input delected

	// Set by user. Registerred functions called by Automation's play
	setTriggerFunc { | function | triggerFunc = function; }

	// Companion stop method
	setStopFunc { |function | stopFunc = function; }

	clearTriggerFunc { triggerFunc = nil; }

	clearStopFunc { stopFunc = nil; }

	embedSynth { | numChans = 1, triggerFunc, stopFunc, register |

		external.value_(true);
		nChan.value_(numChans);
		this.setTriggerFunc(triggerFunc);
		this.setStopFunc(stopFunc);
		this.registerSynth(register);
	}

	registerSynth { | synth | synthRegistry.add(synth); }

	deregisterSynth { | synth |
		if (synthRegistry.notNil) { synthRegistry.remove(synth); };
	}

	getSynthRegistry { ^synthRegistry; }*/
}
