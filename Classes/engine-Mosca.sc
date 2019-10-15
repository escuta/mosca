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
\\radius | spherical radius [0, 200]
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
				this.newtocar(source, 0, force: true);
				firstTime[source] = true;
				//runTrigger.value(currentsource); - watcher does this now
				//tocar.value(currentsource, 0); // needed only by SC input
				//- and probably by HW - causes duplicates with file
				// as file playback is handled by the "watcher" routine
				audit[source] = true;
			} {
				runStop.value(source);
				espacializador[source].free;
				audit[source] = false;
				"stopping!".postln;
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

	//	procTracker  {|heading, roll, pitch, lat, lon|
	procTracker  {|heading, roll, pitch|
		var h, r, p;
		//lattemp, lontemp, newOX, newOY;
		h = (heading / 100) - pi;
		h = h + headingOffset;
		if (h < -pi) {
			h = pi + (pi + h);
		};
		if (h > pi) {
			h = -pi - (pi - h);
		};

		r = (roll / 100) - pi;
		p = (pitch / 100) - pi;

		// pitchnumboxProxy.valueAction = p;
		// rollnumboxProxy.valueAction = r;
		// headingnumboxProxy.valueAction = h;
	}

	matchTByte { |byte|  // match incoming headtracker data

		if(trackarr[tracki].isNil or:{trackarr[tracki]==byte}, {
			trackarr2[tracki]= byte;
			tracki= tracki+1;
			if(tracki>=trackarr.size, {
				//				this.procTracker(trackarr2[4]<<8+trackarr2[5],
				//				trackarr2[6]<<8+trackarr2[7],
				//              trackarr2[8]<<8+trackarr2[9],
				if(hdtrk){
					this.procTracker(
						(trackarr2[5]<<8)+trackarr2[4],
						(trackarr2[7]<<8)+trackarr2[6],
						(trackarr2[9]<<8)+trackarr2[8]
						//,
						//(trackarr2[13]<<24) + (trackarr2[12]<<16) +
						//(trackarr2[11]<<8)
						//+ trackarr2[10],
						//(trackarr2[17]<<24) + (trackarr2[16]<<16) +
						//(trackarr2[15]<<8)
						//+ trackarr2[14]
					);
				};
				tracki= 0;
			});
		}, {
			tracki= 0;
		});
	}

	trackerRoutine { Routine.new
		( {
			inf.do{
				//trackPort.read.postln;
				this.matchTByte(trackPort.read);
			};
		})
	}

	serialKeepItUp {Routine.new({
		inf.do{
			if (trackPort.isOpen.not) // if serial port is closed
			{
				"Trying to reopen serial port!".postln;
				if (SerialPort.devices.includesEqual(serport))
				// and if device is actually connected
				{
					"Device connected! Opening port!".postln;
					trackPort = SerialPort(serport, 115200, crtscts: true);
					this.trackerRoutine; // start tracker routine again
				}
			};
			1.wait;
		};
	})}

	offsetHeading { // give offset to reset North
		| angle |
		headingOffset = angle;
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
		|source |
		if (source > 0) {
			var bus = scInBus[source - 1].index;
			^bus
		}
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
			var line = filenames.getLine(1024);
			if(line!="NULL") {
				tfieldProxy[i].valueAction = line;
			} {
				tfieldProxy[i].valueAction = "";
			};
		};

		nfontes.do { | i |
			var line = libf.getLine(1024);
			libboxProxy[i].valueAction = line.asInteger;
		};

		nfontes.do { | i |
			var line = loopedf.getLine(1024);
			var flag;
			if (line == "true") {flag = true;} {flag = false;};
			lpcheckProxy[i].valueAction = flag;
			//lp[i] 0 or 1
		};

		nfontes.do { | i |
			var line = aformatrevf.getLine(1024);
			dstrvboxProxy[i].valueAction = line.asInteger;
			//dstrv[i] 0 or 1
		};

		nfontes.do { | i |
			var line = spreadf.getLine(1024);
			var flag;
			if (line == "true") {flag = true;} {flag = false;};
			spcheckProxy[i].valueAction = flag;
		};

		nfontes.do { | i |
			var line = diffusef.getLine(1024);
			var flag;
			if (line == "true") {flag = true;} {flag = false;};
			dfcheckProxy[i].valueAction = flag;
		};
		nfontes.do { | i |
			var line = ncanf.getLine(1024);
			ncanboxProxy[i].valueAction = line.asInteger;
		};

		nfontes.do { | i |
			var line = businif.getLine(1024);
			businiboxProxy[i].valueAction = line.asInteger;
		};

		nfontes.do { | i |
			var line = stcheckf.getLine(1024);
			var flag;
			if (line == "true") {flag = true;} {flag = false;};
			stcheckProxy[i].valueAction = flag;
		};

		//		nfontes.do { arg i;
		//	var line = hwinf.getLine(1024);
		//	hwncheckProxy[i].valueAction = line;
		// };
		nfontes.do { | i |
			var line = hwinf.getLine(1024);
			var flag;
			if (line == "true") {flag = true;} {flag = false;};

			//("line = " ++ line.asString).postln;

			//hwncheckProxy[i].valueAction = line.booleanValue;
			// why, why, why is this asBoolean necessary!
			hwncheckProxy[i].valueAction = flag;
		};

		nfontes.do { | i |
			var line = scinf.getLine(1024);
			var flag;
			if (line == "true") {flag = true;} {flag = false;};
			scncheckProxy[i].value = flag;
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
		//	server.sync;
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