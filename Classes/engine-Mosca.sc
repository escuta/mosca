/*
Mosca: SuperCollider class by Iain Mott, 2016. Licensed under a
Creative Commons Attribution-NonCommercial 4.0 International License
http://creativecommons.org/licenses/by-nc/4.0/
The class makes extensive use of the Ambisonic Toolkit (http://www.ambisonictoolkit.net/)
by Joseph Anderson and the Automation quark
(https://github.com/neeels/Automation) by Neels Hofmeyr.
Required Quarks : Automation, Ctk, XML and  MathLib
Required classes:
SC Plugins: https://github.com/supercollider/sc3-plugins
User must set up a project directory with subdirectoties "rir" and "auto"
RIRs should have the first 100 or 120ms silenced to act as "tail" reverberators
and must be placed in the "rir" directory.
Run help on the "Mosca" class in SuperCollider for detailed information
and code examples. Further information and sample RIRs and B-format recordings
may be downloaded here: http://escuta.org/mosca
*/


+ Mosca {

	*printSynthParams {
		var string =
		"

GUI Parameters usable in SynthDefs

\\level | level [0, 2]
\\dopamnt | Doppler ammount [0, 1]
\\angle | Stereo angle | default 1.05 (60 degrees) [0, pi]
\\glev | Global/Close reverb level [0, 1]
\\llev | Local/Distant reverb level [0, 1]
\\azim | azimuth coord [-pi, pi]
\\elev | elevation coord [-pi, pi]
\\radius | spherical radius [0, 20]
\\rotAngle | B-format rotation angle [-pi, pi]
\\directang | B-format directivity [0, pi/2]
\\contr | Contraction: fade between WXYZ & W [0, 1]
\\aux1 | Auxiliary slider 1 value [0, 1]
\\aux2 | Auxiliary slider 2 value [0, 1]
\\aux3 | Auxiliary slider 3 value [0, 1]
\\aux4 | Auxiliary slider 4 value [0, 1]
\\aux5 | Auxiliary slider 5 value [0, 1]
\\a1check | Auxiliary checkbox/button [0 or 1]
\\a2check | Auxiliary checkbox/button [0 or 1]
\\a3check | Auxiliary checkbox/button [0 or 1]
\\a4check | Auxiliary checkbox/button [0 or 1]
\\a5check | Auxiliary checkbox/button [0 or 1]

";
		^string;

	}

	auditionFunc { |source, bool|
		if(isPlay.not) {
			if(bool) {
				firstTime[source] = true;
				audit[source] = true;
				this.newtocar(source, 0, force: true);
			} {
				runStop.value(source);
				audit[source] = false;
				espacializador[source].free;
				"stopping!".postln;
			};

			if (ossiaaud[source].value != bool) {
				ossiaaud[source].value = bool;
			};

			if (guiflag) {
				{ novoplot.value; }.defer;
				if (source == currentsource) {
					{ baudi.value = bool.asInteger; }.defer;
				};
			};
		};
	}

	remoteCtl { |bool|
		if (bool) {
			hdtrk = true;

			ossiaorient.access_mode_(\bi);
			ossiaorigine.access_mode_(\bi);
		} {
			hdtrk = false;
			headingnumboxProxy.valueAction = 0;
			pitchnumboxProxy.valueAction = 0;
			rollnumboxProxy.valueAction = 0;
			oxnumboxProxy.valueAction = 0;
			oynumboxProxy.valueAction = 0;
			oznumboxProxy.valueAction = 0;

			ossiaorient.access_mode_(\set);
			ossiaorigine.access_mode_(\set);
		};

		if (ossiaremotectl.value != bool) {
			ossiaremotectl.value = bool;
		};

		if (guiflag) {
			if (hdtrkcheck.value != bool.asInteger) {
				{ hdtrkcheck.value = bool.asInteger; }.defer;
			};
		};
	}

	blips {
		Routine.new({
			4.do{
				Synth(\blip);
				1.wait;
			};
			yieldAndReset(true);
		}).play;
	}

	registerSynth { // selection of Mosca arguments for use in synths
		| source, synth |
		synthRegistry[source-1].add(synth);
	}

	deregisterSynth { // selection of Mosca arguments for use in synths
		| source, synth |
		if(synthRegistry[source-1].notNil){
			synthRegistry[source-1].remove(synth);

		};
	}

	getSynthRegistry { // selection of Mosca arguments for use in synths
		| source |
		^synthRegistry[source-1];
	}

	getSCBus {
		|source = 1, numChans = 1 |
			scInBus[source - 1] = Bus.audio(server, numChans);
			^scInBus[source - 1].index;
	}

	setSynths {
		|source, param, value|

		synthRegistry[source].do({
			| item, i |

			if(item.notNil) {
				item.set(param, value);
			}
		});
	}

	getInsertIn {
		|source |
		if (source > 0) {
			var bus = insertBus[0,source-1];
			insertFlag[source-1]=1;
			espacializador[source-1].set(\insertFlag, 1);
			synt[source-1].set(\insertFlag, 1);
			^bus
		}
	}

	getInsertOut {
		|source |
		if (source > 0) {
			var bus = insertBus[1,source-1];
			insertFlag[source-1]=1;
			espacializador[source-1].set(\insertFlag, 1);
			synt[source-1].set(\insertFlag, 1);
			^bus
		}
	}

	releaseInsert {
		|source |
		if (source > 0) {
			insertFlag[source-1]=0;
			espacializador[source-1].set(\insertFlag, 0);
		}
	}

	// These methods relate to control of synths when SW Input delected
	// for source in GUI

	// Set by user. Registerred functions called by Automation's play
	setTriggerFunc {
		|source, function|
		if (source > 0) {
			triggerFunc[source-1] = function;
		}
	}

	// Companion stop method
	setStopFunc {
		|source, function|
		if (source > 0) {
			stopFunc[source-1] = function;
		}
	}

	clearTriggerFunc {
		|source|
		if (source > 0) {
			triggerFunc[source-1] = nil;
		}
	}

	clearStopFunc {
		|source|
		if (source > 0) {
			stopFunc[source-1] = nil;
		}
	}

	playAutomation {
		control.play;
	}

	// no longer necessary as added a autoloop creation argument
	playAutomationLooped {
		//autoloopval = true;
		autoloop.valueAction = true;
		control.play;
	}

	nonAmbi2FuMaNeeded { |i|
		if (i == nfontes) {
			^false.asBoolean;
		} {
			if ( (lib[i].value > lastFUMA) // pass ambisonic libs
				&& espacializador[i].notNil ) {
				^true.asBoolean;
			} {
				^this.nonAmbi2FuMaNeeded(i + 1);
			};
		};
	}

	converterNeeded { |i|
		if (i == nfontes) {
			^false.asBoolean;
		} {
			if (convert_fuma && (clsrv != 0)) {
				^true.asBoolean;
			} {
				if ( convert[i] && espacializador[i].notNil ) {
					^true.asBoolean;
				} {
					^this.converterNeeded(i + 1);
				};
			};
		};
	}

	loadNonAutomationData { | path |
		var libf, loopedf, aformatrevf, hwinf, scinf,
		spreadf, diffusef, ncanf, businif, stcheckf, filenames;
		//("THE PATH IS " ++ path ++ "/filenames.txt").postln;
		filenames = File((path ++ "/filenames.txt").standardizePath,"r");

		libf = File((path ++ "/lib.txt").standardizePath,"r");
		loopedf = File((path ++ "/looped.txt").standardizePath,"r");
		aformatrevf = File((path ++ "/aformatrev.txt").standardizePath,"r");
		hwinf = File((path ++ "/hwin.txt").standardizePath,"r");
		scinf = File((path ++ "/scin.txt").standardizePath,"r");
		spreadf = File((path ++ "/spread.txt").standardizePath,"r");
		diffusef = File((path ++ "/diffuse.txt").standardizePath,"r");
		ncanf = File((path ++ "/ncan.txt").standardizePath,"r");
		businif = File((path ++ "/busini.txt").standardizePath,"r");
		stcheckf = File((path ++ "/stcheck.txt").standardizePath,"r");


		//{	("BEFORE ACTION - stream = " ++ stcheckProxy[0].value).postln;}.defer;


		nfontes.do { | i |
			var flag = { |string| if (string == "true") { true } { false } },
			line = stcheckf.getLine(1024);

			stcheckProxy[i].valueAction = flag.value(line);

			line = filenames.getLine(1024);
			if (line != "NULL") {
				tfieldProxy[i].valueAction = line;
			} {
				tfieldProxy[i].valueAction = "";
			};

			line = libf.getLine(1024);
			libboxProxy[i].valueAction = line.asInteger;

			line = loopedf.getLine(1024);
			lpcheckProxy[i].valueAction = flag.value(line);

			line = aformatrevf.getLine(1024);
			dstrvboxProxy[i].valueAction = line.asInteger;

			line = spreadf.getLine(1024);
			spcheckProxy[i].valueAction = flag.value(line);

			line = diffusef.getLine(1024);
			dfcheckProxy[i].valueAction = flag.value(line);

			line = ncanf.getLine(1024);
			ncanboxProxy[i].valueAction = line.asInteger;

			line = businif.getLine(1024);
			businiboxProxy[i].valueAction = line.asInteger;

			line = hwinf.getLine(1024);
			hwncheckProxy[i].valueAction = flag.value(line);

			line = scinf.getLine(1024);
			scncheckProxy[i].value = flag.value(line);
		};

		filenames.close;

		libf.close;
		loopedf.close;
		aformatrevf.close;
		hwinf.close;
		scinf.close;
		spreadf.close;
		diffusef.close;
		ncanf.close;
		businif.close;
		stcheckf.close;

		//"RARARARARAR".postln;

		// delay necessary here because streamdisks take some time to register
		// after control.load
		//Routine {

		//1.wait;
		/*nfontes.do { arg i;

		var newpath = tfieldProxy[i].value;
		if (streamdisk[i].not && (tfieldProxy[i].value != "")) {
		i.postln;
		newpath.postln;

		sombuf[i] = Buffer.read(server, newpath, action: {arg buf;
		"Loaded file".postln;
		});
		};

		};*/
		//	}.play;
		//	watcher.play;

	}

	// Automation call-back doesn' seem to work with no GUI, so these duplicate
	// control.onPlay, etc.
	blindControlPlay {
		var startTime;
		nfontes.do { | i |
			firstTime[i]=true;
		};

		if(control.now < 0)
		{
			startTime = 0
		} {
			startTime = control.now
		};
		isPlay = true;
		control.play;
		//runTriggers.value;
	}

	blindControlStop {
		control.stop;
		runStops.value;
		nfontes.do { | i |
			// if sound is currently being "tested", don't switch off on stop
			// leave that for user
			if (audit[i] == false) {
				synt[i].free; // error check
			};
			//	espacializador[i].free;
		};
		isPlay = false;
	}

}