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
}
