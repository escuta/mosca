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

+ Moscav2 {

	newtocar {
		| i, tpos, force = false |
		var path = tfieldProxy[i].value;

		if (streamdisk[i]) {
			var sframe, srate;
			// sframe = tpos * srate;
			// stdur = sf.numFrames / srate; // needed?
			streambuf[i] = Buffer.cueSoundFile(server, path, 0,
				ncan[i], 131072);
			//		streambuf[i] = srate; //??
			("Creating buffer for source: " ++ i).postln;
		};

		// ncan is number of channels of the source
		// busini is the initial bus used for a particular stream
		// If we have ncan = 4 and busini = 7, the stream will enter
		// in buses 7, 8, 9 and 10.

		/// STREAM FROM DISK

		if ((path != "") && hwncheckProxy[i].value.not
			&& scncheckProxy[i].value.not
			&& streamdisk[i]) {
			"Content Streamed from disk".postln;
			if (audit[i].not || force) { // if source is testing don't relaunch synths

				case
				{ ncan[i] == 1} {
					"1 channel".postln;

					// set lib, convert and dstrv variables when stynths are lauched
					// for the tracking functions to stay relevant

					lib[i] = libboxProxy[i].value;

					case
					{ libboxProxy[i].value <= lastN3D }
					{ convert[i] = convert_n3d; }
					{ (libboxProxy[i].value >= (lastN3D + 1)) &&
						(libboxProxy[i].value <= lastFUMA) }
					{ convert[i] = convert_fuma; }
					{ libboxProxy[i].value >= (lastFUMA + 1) }
					{ convert[i] = convert_direct; };

					dstrv[i] = dstrvboxProxy[i].value;

					if (convert[i]) {

						if (convertor.notNil) {
							convertor.set(\gate, 1);
						} {
							convertor = Synth(\ambiConverter, [\gate, 1],
								target:glbRevDecGrp, addAction:\addAfter).onFree({
								convertor = nil;
							});
						};
					};

					if (azimuths.isNil) {

						if ((libboxProxy[i].value > lastFUMA)
							&& nonAmbi2FuMa.isNil) {
							nonAmbi2FuMa = Synth(\nonAmbi2FuMa,
								target:glbRevDecGrp).onFree({
								nonAmbi2FuMa = nil;
							});
						};
					};

					espacializador[i] = Synth(ossialib[i].v++"Stream"
						++dstrvtypes[i],
						[\bufnum, streambuf[i].bufnum,
							\lp, lpcheckProxy[i].value,
							\azim, spheval[i].theta,
							\elev, spheval[i].phi,
							\radius, spheval[i].rho,
							\dopamnt, dpboxProxy[i].value,
							\glev, gboxProxy[i].value,
							\llev, gboxProxy[i].value,
							\insertFlag, insertFlag[i],
							\insertOut, insertBus[0,i],
							\insertBack, insertBus[1,i],
							\contr, cboxProxy[i].value,
							\room, rmboxProxy[i].value,
							\damp, dmboxProxy[i].value,
							\amp, vboxProxy[i].value.dbamp,
							\grainrate, rateboxProxy[i].value,
							\winsize, winboxProxy[i].value,
							\winrand, randboxProxy[i].value] ++
						wSpecPar.value(max(dstrvboxProxy[i].value - 3, 0)),
						playEspacGrp).onFree({
						espacializador[i] = nil;
						streambuf[i].free;
						if (azimuths.isNil) {
							if (this.nonAmbi2FuMaNeeded(0).not
									&& nonAmbi2FuMa.notNil) {
								nonAmbi2FuMa.free;
							};
						};
						if (convertor.notNil) {
							if (convert[i]) {
								if (this.converterNeeded(0).not) {
									convertor.set(\gate, 0);
								};
							};
						};
					});

					//atualizarvariaveis.value;
					//updatesourcevariables.value(i);

				}
				{ ncan[i] == 2} {
					"2 channel".postln;

					// set lib, convert and dstrv variables when stynths are lauched
					// for the tracking functions to stay relevant

					lib[i] = libboxProxy[i].value;

					case
					{ libboxProxy[i].value <= lastN3D }
					{ convert[i] = convert_n3d; }
					{ (libboxProxy[i].value >= (lastN3D + 1))
						&& (libboxProxy[i].value <= lastFUMA) }
					{ convert[i] = convert_fuma; }
					{ libboxProxy[i].value >= (lastFUMA + 1) }
					{ convert[i] = convert_direct; };

					dstrv[i] = dstrvboxProxy[i].value;

					if (convert[i]) {

						if (convertor.notNil) {
							convertor.set(\gate, 1);
						} {
							convertor = Synth(\ambiConverter, [\gate, 1],
								target:glbRevDecGrp, addAction:\addAfter).onFree({
								convertor = nil;
							});
						};
					};

					if (azimuths.isNil) {

						if ((libboxProxy[i].value > lastFUMA)
							&& nonAmbi2FuMa.isNil) {
							nonAmbi2FuMa = Synth(\nonAmbi2FuMa,
								target:glbRevDecGrp).onFree({
								nonAmbi2FuMa = nil;
							});
						};
					};

					espacializador[i] = Synth(ossialib[i].v++"StereoStream"++dstrvtypes[i],
						[\bufnum, streambuf[i].bufnum,
							\angle, aboxProxy[i].value,
							\lp, lpcheckProxy[i].value,
							\azim, spheval[i].theta,
							\elev, spheval[i].phi,
							\radius, spheval[i].rho,
							\dopamnt, dpboxProxy[i].value,
							\glev, gboxProxy[i].value,
							\llev, gboxProxy[i].value,
							\insertFlag, insertFlag[i],
							\insertOut, insertBus[0,i],
							\insertBack, insertBus[1,i],
							\contr, cboxProxy[i].value,
							\room, rmboxProxy[i].value,
							\damp, dmboxProxy[i].value,
							\amp, vboxProxy[i].value.dbamp,
							\grainrate, rateboxProxy[i].value,
							\winsize, winboxProxy[i].value,
							\winrand, randboxProxy[i].value] ++
						zSpecPar.value(max(dstrvboxProxy[i].value - 3, 0)),
						playEspacGrp).onFree({
						espacializador[i] = nil;
						streambuf[i].free;
						if (azimuths.isNil) {
							if (this.nonAmbi2FuMaNeeded(0).not
								&& nonAmbi2FuMa.notNil) {
								nonAmbi2FuMa.free;
							};
						};
						if (convertor.notNil) {
							if (convert[i]) {
								if (this.converterNeeded(0).not) {
									convertor.set(\gate, 0);
								};
							};
						};
					});

					//updatesourcevariables.value(i);

				}
				{ ncan[i] >= 4} {
					playingBF[i] = true;
					("contains "++ncan[i]++" channels").postln;

					if ((libboxProxy[i].value >= (lastN3D + 1)) ||
						(dstrvboxProxy[i].value == 3)) {
						libboxProxy[i].valueAction = lastN3D + 1;
						lib[i] = lastN3D + 1;
						convert[i] = convert_fuma;
					} {
						libboxProxy[i].valueAction = 0;
						lib[i] = 0;
						convert[i] = convert_n3d;
						dstrv[i] = dstrvboxProxy[i].value;
					};

					// set lib, convert and dstrv variables when stynths are lauched
					// for the tracking functions to stay relevant

					if (convert[i]) {
						if (convertor.notNil) {
							convertor.set(\gate, 1);
						} {
							convertor = Synth(\ambiConverter, [\gate, 1],
								target:glbRevDecGrp, addAction:\addAfter).onFree({
								convertor = nil;
							});
						};
					};

					espacializador[i] = Synth(ossialib[i].v++"BFormatStream"++
						(ncan[i])++dstrvtypes[i],
						[\bufnum, streambuf[i].bufnum,
							\rotAngle, rboxProxy[i].value,
							\directang, dboxProxy[i].value,
							\azim, spheval[i].theta,
							\elev, spheval[i].phi,
							\radius, spheval[i].rho,
							\dopamnt, dpboxProxy[i].value,
							\glev, gboxProxy[i].value,
							\llev, gboxProxy[i].value,
							\insertFlag, insertFlag[i],
							\insertOut, insertBus[0,i],
							\insertBack, insertBus[1,i],
							\contr, cboxProxy[i].value,
							\room, rmboxProxy[i].value,
							\damp, dmboxProxy[i].value,
							\amp, vboxProxy[i].value.dbamp,
							\grainrate, rateboxProxy[i].value,
							\winsize, winboxProxy[i].value,
							\winrand, randboxProxy[i].value] ++
						zSpecPar.value(max(dstrvboxProxy[i].value - 3, 0)),
						playEspacGrp).onFree({
						espacializador[i] = nil;
						playingBF[i] = false;
						streambuf[i].free;
						if (convertor.notNil) {
							if (convert[i]) {
								if (this.converterNeeded(0).not) {
									convertor.set(\gate, 0);
								};
							};
						};
					});

				};

				//updatesourcevariables.value(i);

			};
		};

		/// END STREAM FROM DISK

		// check this logic - what should override what?
		if ((path != "") && (hwncheckProxy[i].value.not
			|| scncheckProxy[i].value.not)
		&& streamdisk[i].not) {

			//{
			case
			{ ncan[i] == 1 } { // arquivo mono

				// set lib, convert and dstrv variables when stynths are lauched
				// for the tracking functions to stay relevant

				lib[i] = libboxProxy[i].value;

				case
				{ libboxProxy[i].value <= lastN3D }
				{ convert[i] = convert_n3d; }
				{ (libboxProxy[i].value >= (lastN3D + 1))
					&& (libboxProxy[i].value <= lastFUMA) }
				{ convert[i] = convert_fuma; }
				{ libboxProxy[i].value >= (lastFUMA + 1) }
				{ convert[i] = convert_direct; };

				dstrv[i] = dstrvboxProxy[i].value;

				if (convert[i]) {

					if (convertor.notNil) {
						convertor.set(\gate, 1);
					} {
						convertor = Synth(\ambiConverter, [\gate, 1],
							target:glbRevDecGrp, addAction:\addAfter).onFree({
							convertor = nil;
						});
					};
				};

				if (azimuths.isNil) {

					if ((libboxProxy[i].value > lastFUMA)
						&& nonAmbi2FuMa.isNil) {
						nonAmbi2FuMa = Synth(\nonAmbi2FuMa,
							target:glbRevDecGrp).onFree({
							nonAmbi2FuMa = nil;
						});
					};
				};

				espacializador[i] = Synth(ossialib[i].v++"File"++dstrvtypes[i],
					[\bufnum, sombuf[i].bufnum,
						\lp, lpcheckProxy[i].value,
						\azim, spheval[i].theta,
						\elev, spheval[i].phi,
						\radius, spheval[i].rho,
						\dopamnt, dpboxProxy[i].value,
						\glev, gboxProxy[i].value,
						\llev, gboxProxy[i].value,
						\insertFlag, insertFlag[i],
						\insertOut, insertBus[0,i],
						\insertBack, insertBus[1,i],
						\contr, cboxProxy[i].value,
						\room, rmboxProxy[i].value,
						\damp, dmboxProxy[i].value,
						\amp, vboxProxy[i].value.dbamp,
						\grainrate, rateboxProxy[i].value,
						\winsize, winboxProxy[i].value,
						\winrand, randboxProxy[i].value] ++
					wSpecPar.value(max(dstrvboxProxy[i].value - 3, 0)),
					playEspacGrp).onFree({
					espacializador[i] = nil;
					if (azimuths.isNil) {
						if (this.nonAmbi2FuMaNeeded(0).not
							&& nonAmbi2FuMa.notNil) {
							nonAmbi2FuMa.free;
						};
					};
					if (convertor.notNil) {
						if (convert[i]) {
							if (this.converterNeeded(0).not) {
								convertor.set(\gate, 0);
							};
						};
					};
				});

				//updatesourcevariables.value(i);

			}
			{ ncan[i] == 2 } {

				// set lib, convert and dstrv variables when stynths are lauched
				// for the tracking functions to stay relevant

				lib[i] = libboxProxy[i].value;

				case
				{ libboxProxy[i].value <= lastN3D }
				{ convert[i] = convert_n3d; }
				{ (libboxProxy[i].value >= (lastN3D + 1))
					&& (libboxProxy[i].value <= lastFUMA) }
				{ convert[i] = convert_fuma; }
				{ libboxProxy[i].value >= (lastFUMA + 1) }
				{ convert[i] = convert_direct; };

				dstrv[i] = dstrvboxProxy[i].value;

				if (convert[i]) {

					if (convertor.notNil) {
						convertor.set(\gate, 1);
					} {
						convertor = Synth(\ambiConverter, [\gate, 1],
							target:glbRevDecGrp, addAction:\addAfter).onFree({
							convertor = nil;
						});
					};
				};

				if (azimuths.isNil) {

					if ((libboxProxy[i].value > lastFUMA) && nonAmbi2FuMa.isNil) {
						nonAmbi2FuMa = Synth(\nonAmbi2FuMa,
							target:glbRevDecGrp).onFree({
							nonAmbi2FuMa = nil;
						});
					};
				};

				espacializador[i] = Synth(ossialib[i].v++"StereoFile"++dstrvtypes[i],
					[\bufnum, sombuf[i].bufnum,
						\angle, aboxProxy[i].value,
						\lp, lpcheckProxy[i].value,
						\azim, spheval[i].theta,
						\elev, spheval[i].phi,
						\radius, spheval[i].rho,
						\dopamnt, dpboxProxy[i].value,
						\glev, gboxProxy[i].value,
						\llev, gboxProxy[i].value,
						\insertFlag, insertFlag[i],
						\insertOut, insertBus[0,i],
						\insertBack, insertBus[1,i],
						\contr, cboxProxy[i].value,
						\room, rmboxProxy[i].value,
						\damp, dmboxProxy[i].value,
						\amp, vboxProxy[i].value.dbamp,
						\grainrate, rateboxProxy[i].value,
						\winsize, winboxProxy[i].value,
						\winrand, randboxProxy[i].value] ++
					zSpecPar.value(max(dstrvboxProxy[i].value - 3, 0)),
					playEspacGrp).onFree({
					espacializador[i] = nil;
					if (azimuths.isNil) {
						if (this.nonAmbi2FuMaNeeded(0).not
							&& nonAmbi2FuMa.notNil) {
							nonAmbi2FuMa.free;
						};
					};
					if (convertor.notNil) {
						if (convert[i]) {
							if (this.converterNeeded(0).not) {
								convertor.set(\gate, 0);
							};
						};
					};
				});

				//atualizarvariaveis.value;
				//updatesourcevariables.value(i);

			}
			{ ncan[i] >= 4 } {
				playingBF[i] = true;

				if ((libboxProxy[i].value >= (lastN3D + 1)) ||
					(dstrvboxProxy[i].value == 3)) {
					libboxProxy[i].valueAction = lastN3D + 1;
					lib[i] = lastN3D + 1;
					convert[i] = convert_fuma;
				} {
					libboxProxy[i].valueAction = 0;
					lib[i] = 0;
					convert[i] = convert_n3d;
					dstrv[i] = dstrvboxProxy[i].value;
				};

				// set lib, convert and dstrv variables when stynths are lauched
				// for the tracking functions to stay relevant

				if (convert[i]) {
					if (convertor.notNil) {
						convertor.set(\gate, 1);
					} {
						convertor = Synth(\ambiConverter, [\gate, 1],
							target:glbRevDecGrp, addAction:\addAfter).onFree({
							convertor = nil;
						});
					};
				};

				espacializador[i] = Synth(ossialib[i].v++"BFormatFile"++
					(ncan[i])++dstrvtypes[i],
					[\bufnum, sombuf[i].bufnum,
						\rotAngle, rboxProxy[i].value,
						\directang, dboxProxy[i].value,
						\azim, spheval[i].theta,
						\elev, spheval[i].phi,
						\radius, spheval[i].rho,
						\dopamnt, dpboxProxy[i].value,
						\glev, gboxProxy[i].value,
						\llev, gboxProxy[i].value,
						\insertFlag, insertFlag[i],
						\insertOut, insertBus[0,i],
						\insertBack, insertBus[1,i],
						\contr, cboxProxy[i].value,
						\room, rmboxProxy[i].value,
						\damp, dmboxProxy[i].value,
						\amp, vboxProxy[i].value.dbamp,
						\grainrate, rateboxProxy[i].value,
						\winsize, winboxProxy[i].value,
						\winrand, randboxProxy[i].value] ++
					wSpecPar.value(max(dstrvboxProxy[i].value - 3, 0)),
					playEspacGrp).onFree({espacializador[i].free;
					espacializador[i] = nil;
					playingBF[i] = false;
					if (convertor.notNil) {
						if (convert[i]) {
							if (this.converterNeeded(0).not) {
								convertor.set(\gate, 0);
							};
						};
					};
				});

			};

			//updatesourcevariables.value(i);

		} {
			if ((scncheckProxy[i].value) || (hwncheckProxy[i])) {

				var witch, bus;

				if (hwncheckProxy[i].value) {

					witch = "HWBus";
					bus = busini[i];
				} {
					witch = "SWBus";
					bus = scInBus[i];
				};

				case
				{ ncan[i] == 1 } {

					// set lib, convert and dstrv variables when stynths are lauched
					// for the tracking functions to stay relevant

					lib[i] = libboxProxy[i].value;

					case
					{ libboxProxy[i].value <= lastN3D }
					{ convert[i] = convert_n3d; }
					{ (libboxProxy[i].value >= (lastN3D + 1))
						&& (libboxProxy[i].value <= lastFUMA) }
					{ convert[i] = convert_fuma; }
					{ libboxProxy[i].value >= (lastFUMA + 1) }
					{ convert[i] = convert_direct; };

					dstrv[i] = dstrvboxProxy[i].value;

					if (convert[i]) {

						if (convertor.notNil) {
							convertor.set(\gate, 1);
						} {
							convertor = Synth(\ambiConverter, [\gate, 1],
								target:glbRevDecGrp, addAction:\addAfter).onFree({
								convertor = nil;
							});
						};
					};

					if (azimuths.isNil) {

						if ((libboxProxy[i].value > lastFUMA)
							&& nonAmbi2FuMa.isNil) {
							nonAmbi2FuMa = Synth(\nonAmbi2FuMa,
								target:glbRevDecGrp).onFree({
								nonAmbi2FuMa = nil;
							});
						};
					};

					this.runTrigger(i);
					if (synthRegistry[i].isEmpty.not) {
						synt[i] = synthRegistry[i];
						synt[i].do({ _.set(
							\azim, spheval[i].theta,
							\elev, spheval[i].phi,
							\radius, spheval[i].rho,
							\dopamnt, dpboxProxy[i].value,
							\glev, gboxProxy[i].value,
							\llev, gboxProxy[i].value,
							\contr, cboxProxy[i].value,
							\room, rmboxProxy[i].value,
							\damp, dmboxProxy[i].value,
							\amp, vboxProxy[i].value.dbamp,
							\grainrate, rateboxProxy[i].value,
							\winsize, winboxProxy[i].value);
						});
					};

					espacializador[i] = Synth(ossialib[i].v++witch
						++dstrvtypes[i],
						[\busini, bus,
							\azim, spheval[i].theta,
							\elev, spheval[i].phi,
							\radius, spheval[i].rho,
							\dopamnt, dpboxProxy[i].value,
							\glev, gboxProxy[i].value,
							\llev, gboxProxy[i].value,
							\insertFlag, insertFlag[i],
							\insertOut, insertBus[0,i],
							\insertBack, insertBus[1,i],
							\contr, cboxProxy[i].value,
							\room, rmboxProxy[i].value,
							\damp, dmboxProxy[i].value,
							\amp, vboxProxy[i].value.dbamp,
							\grainrate, rateboxProxy[i].value,
							\winsize, winboxProxy[i].value,
							\winrand, randboxProxy[i].value] ++
						wSpecPar.value(max(dstrvboxProxy[i].value - 3, 0)),
						playEspacGrp).onFree({
						espacializador[i] = nil;
						if (azimuths.isNil) {
							if (this.nonAmbi2FuMaNeeded(0).not
								&& nonAmbi2FuMa.notNil) {
								nonAmbi2FuMa.free;
							};
						};
						if (convertor.notNil) {
							if (convert[i]) {
								if (this.converterNeeded(0).not) {
									convertor.set(\gate, 0);
								};
							};
						};
					});

					//atualizarvariaveis.value;
					//updatesourcevariables.value(i);

				}
				{ ncan[i] == 2 } {

					// set lib, convert and dstrv variables when stynths are lauched
					// for the tracking functions to stay relevant

					lib[i] = libboxProxy[i].value;

					case
					{ libboxProxy[i].value <= lastN3D }
					{ convert[i] = convert_n3d; }
					{ (libboxProxy[i].value >= (lastN3D + 1))
						&& (libboxProxy[i].value <= lastFUMA) }
					{ convert[i] = convert_fuma; }
					{ libboxProxy[i].value >= (lastFUMA + 1) }
					{ convert[i] = convert_direct; };

					dstrv[i] = dstrvboxProxy[i].value;

					if (convert[i]) {

						if (convertor.notNil) {
							convertor.set(\gate, 1);
						} {
							convertor = Synth(\ambiConverter, [\gate, 1],
								target:glbRevDecGrp, addAction:\addAfter).onFree({
								convertor = nil;
							});
						};
					};

					if (azimuths.isNil) {

						if ((libboxProxy[i].value > lastFUMA)
							&& nonAmbi2FuMa.isNil) {
							nonAmbi2FuMa = Synth(\nonAmbi2FuMa,
								target:glbRevDecGrp).onFree({
								nonAmbi2FuMa = nil;
							});
						};
					};

					this.runTrigger(i);
					if (synthRegistry[i].isEmpty.not) {
						synt[i] = synthRegistry[i];
						synt[i].do({ _.set(
							\azim, spheval[i].theta,
							\elev, spheval[i].phi,
							\radius, spheval[i].rho,
							\dopamnt, dpboxProxy[i].value,
							\glev, gboxProxy[i].value,
							\llev, gboxProxy[i].value,
							\contr, cboxProxy[i].value,
							\room, rmboxProxy[i].value,
							\damp, dmboxProxy[i].value,
							\amp, vboxProxy[i].value.dbamp,
							\grainrate, rateboxProxy[i].value,
							\winsize, winboxProxy[i].value);
						});
					};

					espacializador[i] = Synth(ossialib[i].v++"Stereo"++witch
						++dstrvtypes[i],
						[\busini, bus,
							\angle, aboxProxy[i].value,
							\azim, spheval[i].theta,
							\elev, spheval[i].phi,
							\radius, spheval[i].rho,
							\dopamnt, dpboxProxy[i].value,
							\glev, gboxProxy[i].value,
							\llev, gboxProxy[i].value,
							\insertFlag, insertFlag[i],
							\insertOut, insertBus[0,i],
							\insertBack, insertBus[1,i],
							\contr, cboxProxy[i].value,
							\room, rmboxProxy[i].value,
							\damp, dmboxProxy[i].value,
							\amp, vboxProxy[i].value.dbamp,
							\grainrate, rateboxProxy[i].value,
							\winsize, winboxProxy[i].value,
							\winrand, randboxProxy[i].value] ++
						zSpecPar.value(max(dstrvboxProxy[i].value - 3, 0)),
						playEspacGrp).onFree({
						espacializador[i] = nil;
						if (azimuths.isNil) {
							if (this.nonAmbi2FuMaNeeded(0).not
								&& nonAmbi2FuMa.notNil) {
								nonAmbi2FuMa.free;
							};
						};
						if (convertor.notNil) {
							if (convert[i]) {
								if (this.converterNeeded(0).not) {
									convertor.set(\gate, 0);
								};
							};
						};
					});

					//atualizadstrvariaveis.value;
					//updatesourcevariables.value(i);

				}
				{ ncan[i] >= 4 } {

					if ((libboxProxy[i].value >= (lastN3D + 1)) ||
						(dstrvboxProxy[i].value == 3)) {
						libboxProxy[i].valueAction = lastN3D + 1;
						lib[i] = lastN3D + 1;
						convert[i] = convert_fuma;
					} {
						libboxProxy[i].valueAction = 0;
						lib[i] = 0;
						convert[i] = convert_n3d;
						dstrv[i] = dstrvboxProxy[i].value;
					};

					if (convert[i]) {
						if (convertor.notNil) {
							convertor.set(\gate, 1);
						} {
							convertor = Synth(\ambiConverter, [\gate, 1],
								target:glbRevDecGrp, addAction:\addAfter).onFree({
								convertor = nil;
							});
						};
					};

					this.runTrigger(i);
					if (synthRegistry[i].isEmpty.not) {
						synt[i] = synthRegistry[i];
						synt[i].do({ _.set(
							\azim, spheval[i].theta,
							\elev, spheval[i].phi,
							\radius, spheval[i].rho,
							\dopamnt, dpboxProxy[i].value,
							\glev, gboxProxy[i].value,
							\llev, gboxProxy[i].value,
							\contr, cboxProxy[i].value,
							\room, rmboxProxy[i].value,
							\damp, dmboxProxy[i].value,
							\amp, vboxProxy[i].value.dbamp,
							\grainrate, rateboxProxy[i].value,
							\winsize, winboxProxy[i].value);
						});
					};

					espacializador[i] = Synth(ossialib[i].v++"BFormat"++witch++
						(ncan[i])++dstrvtypes[i],
						[\busini, bus,
							\rotAngle, rboxProxy[i].value,
							\directang, dboxProxy[i].value,
							\azim, spheval[i].theta,
							\elev, spheval[i].phi,
							\radius, spheval[i].rho,
							\dopamnt, dpboxProxy[i].value,
							\glev, gboxProxy[i].value,
							\llev, gboxProxy[i].value,
							\insertFlag, insertFlag[i],
							\insertOut, insertBus[0,i],
							\insertBack, insertBus[1,i],
							\contr, cboxProxy[i].value,
							\room, rmboxProxy[i].value,
							\damp, dmboxProxy[i].value,
							\amp, vboxProxy[i].value.dbamp,
							\grainrate, rateboxProxy[i].value,
							\winsize, winboxProxy[i].value,
							\winrand, randboxProxy[i].value] ++
						wSpecPar.value(max(dstrvboxProxy[i].value - 3, 0)),
						playEspacGrp).onFree({
						espacializador[i] = nil;
						if (convert[i]) {
							if (convertor.notNil) {
								if (this.converterNeeded(0).not) {
									convertor.set(\gate, 0);
								};
							};
						};
					});

					//atualizarvariaveis.value;
					//updatesourcevariables.value(i);

				};
			};
		};
	}

}