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

HeadTracker
{
	const baudRate = 115200;

	var moscaCenter, switch, serport, <headingOffset,
	volpot, ossiaParent, masterLevel,
	troutine, kroutine, rtkroutine,
	trackarr, trackarr2, tracki, trackPort,
	lastXStep = 0, xStepIncrement, curXStep = 0, interpXStep = true,
	lastYStep = 0, yStepIncrement, curYStep = 0, interpYStep = true,
	lagFactor = 0;

	*new
	{ | center, flag, serialPort, ofsetHeading, volPot, ossiaParent |

		^super.new.headTrackerCtr(center, flag, serialPort, ofsetHeading, volPot, ossiaParent);
	}

	headTrackerCtr
	{ | center, flag, serialPort, ofsetHeading, volPot, ossiaParent |
		moscaCenter = center;
		switch = flag;
		serport = serialPort;
		headingOffset = ofsetHeading;
		volpot = volPot;
		if (ossiaParent.notNil) {
			masterLevel = ossiaParent.find("Master_level");
		} {
			masterLevel = 0
		};
		("volPot = " + volpot).postln;
		SerialPort.devicePattern = serport;
		// needed in serKeepItUp routine - see below
		trackPort = SerialPort(serport, baudRate, crtscts: true);
		trackPort.doneAction_({
			"Serial port down".postln;
			troutine.stop;
			troutine.reset;
		});
		this.setTracker();
		trackarr2 = trackarr.copy;
		tracki = 0;
		
		troutine = Routine.new({
			inf.do({
				if (switch.v)
				{ this.matchByte(trackPort.read) }  // Back to original - no wait
				{ 1.wait };
			});
		});
		
		kroutine = Routine.new({
			inf.do({
				if (trackPort.isOpen.not) // if serial port is closed
				{
					"Trying to reopen serial port!".postln;
					if (SerialPort.devices.includesEqual(serport))
					// and if device is actually connected
					{
						"Device connected! Opening port!".postln;
						troutine.stop;
						troutine.reset;
						trackPort = SerialPort(serport, baudRate,
							crtscts: true);
						troutine.play;
					}
				};
				100.wait;
			});
		});
		
		troutine.play;
		kroutine.play;
	}
	
	setTracker // protocol
	{
		if (volpot) {
			trackarr = [251, 252, 253, 254, nil, nil, nil, nil, nil, nil, nil, nil, 255];
		} {
			trackarr = [251, 252, 253, 254, nil, nil, nil, nil, nil, nil, 255];
		};
	}

	procGyro
	{ | heading, roll, pitch |

		var h, r, p, hmod;

		h = (heading / 100);
		h = h + headingOffset;
		h = h.wrap(0, 2pi) - pi;
		r = (roll / 100) - pi;
		p = (pitch / 100) - pi;

		moscaCenter.ossiaOrient.v_([h.neg, p.neg, r]);
	}

	procLev
	{ | lev |
		masterLevel.v = lev.linexp(0, 255, -96, -0.0001);
	}

	matchByte
	{ | byte |  // match incoming headtracker data

		if (trackarr[tracki].isNil || ( trackarr[tracki] == byte ))
		{
			trackarr2[tracki] = byte;
			tracki = tracki + 1;

			if (tracki >= trackarr.size)
			{
				var teste;
				this.procGyro(
					(trackarr2[5]<<8) + trackarr2[4],
					(trackarr2[7]<<8) + trackarr2[6],
					(trackarr2[9]<<8) + trackarr2[8]
				);
				if(volpot) {
					this.procLev( (trackarr2[11]<<8) + trackarr2[10] );
				};

				

				tracki = 0;
			};
		} {
			tracki = 0;
		};
	}

	// give offset to reset North
	offsetHeading { | angle | headingOffset = angle }

	free
	{
		troutine.stop;
		kroutine.stop;
		trackPort.close;
		^super.free;
	}
}

HeadTrackerGPS : HeadTracker
{
	const gpsCoeficient = 0.0000001, latDeg2meters = 111317.099692,
	longDeg2meters = 111319.488, areaInMeters = 10;
	var center, procGPS;

	*new
	{ | center, flag, serialPort, ofsetHeading, setup |

		if (setup.isNil)
		{
			^super.newCopyArgs().headTrackerCtr(center, flag, serialPort, ofsetHeading);
		} {
			^super.newCopyArgs().headTrackerCtr(center, flag, serialPort, ofsetHeading)
			.setCenter(setup);
		};
	}

	setCenter
	{ | latLongAlt |

		if (latLongAlt.isArray)
		{
			//		if(latLongAlt.size < 8) {  // doesn't include GPS scaling params
				center = [latLongAlt[0], latLongAlt[1]];
				if (latLongAlt.size == 4) {
					lagFactor = latLongAlt[2] / latLongAlt[3];
				};
			//	} {
				
			//};
			this.prSetFunc();
		}
		{ Error.throw("coordinates must be an array") }
	}

	prSetFunc
	{
		procGPS = { | coordinates |
			var dLat, dLong, yStep, xStep, res;
			
			dLat = coordinates[0] - center[0];
			dLong = coordinates[1] - center[1];

			yStep = (dLat * latDeg2meters);
			xStep = (dLong * longDeg2meters) * cos(coordinates[0].degrad);
			// lag in xStep
			//	("lagFactor = " + lagFactor).postln;
			if (lagFactor != 0)
			{
				if (xStep != lastXStep) {
					var diff = xStep - curXStep;
					xStepIncrement = diff / lagFactor;
					lastXStep = xStep;
					interpXStep = true;
					//	("Latitude: " + coordinates[0]
					//	+ "Longitude: " + coordinates[1]).postln;
				};
				if (interpXStep == true) {
					curXStep = curXStep + xStepIncrement;
					if (xStepIncrement < 0) {
						if (curXStep < lastXStep) {
							interpXStep = false;
						}
					};
					if (xStepIncrement > 0) 
					{
						if (curXStep > lastXStep) {
							interpXStep = false;
					}
					};
				};
				// Lag in yStep
				if (yStep != lastYStep) {
					var diff = yStep - curYStep;
					yStepIncrement = diff / lagFactor;
					lastYStep = yStep;
					interpYStep = true;
				};
				if (interpYStep == true) {
					curYStep = curYStep + yStepIncrement;
					if (yStepIncrement < 0) {
						if (curYStep < lastYStep) {
							interpYStep = false;
						}
					};
					if (yStepIncrement > 0)
					{
						if (curYStep > lastYStep) {
							interpYStep = false;
						}
					};
				};
			} {
				curXStep = xStep;
				curYStep = yStep;
			};
			
			moscaCenter.ossiaOrigin.v_([curXStep, curYStep, 0] / areaInMeters);
			//postln("x " + xStep + "curX " + curXStep + "xStepIncrement " + xStepIncrement);
			//postln("y " + yStep + "curY " + curYStep + "yStepIncrement " + yStepIncrement);
		}
	}

	setTracker //protocol
	{
		trackarr = [251, 252, 253, 254, nil, nil, nil, nil, nil, nil,
			nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, 255];

		procGPS = { | coordinates |
			postln("latitude " + coordinates[0]);
			postln("longitude " + coordinates[1]);
			// postln("altitude " + coordinates[2]);
		}; // initialize in case no coordinates are set;
	}

	matchByte
	{
		| byte | // match incoming headtracker data

		if (trackarr[tracki].isNil || (trackarr[tracki] == byte ))
		{
			trackarr2[tracki] = byte;
			tracki= tracki + 1;

			if (tracki >= trackarr.size)
			{
				this.procGyro(
					(trackarr2[5]<<8) + trackarr2[4],
					(trackarr2[7]<<8) + trackarr2[6],
					(trackarr2[9]<<8) + trackarr2[8]
				);

				procGPS.value([(trackarr2[13]<<24) + (trackarr2[12]<<16) +
					(trackarr2[11]<<8) + trackarr2[10],
					(trackarr2[17]<<24) + (trackarr2[16]<<16) +
					(trackarr2[15]<<8) + trackarr2[14]
					//,(trackarr2[21]<<24) + (trackarr2[20]<<16) +
					//(trackarr2[19]<<8) + trackarr2[18]
				] * gpsCoeficient);

				tracki = 0;
			};
		} {
			tracki = 0;
		};
	}
}

RTKGPS : HeadTrackerGPS
{
	/*
	const gpsCoeficient = 0.0000001, latDeg2meters = 111317.099692,
	longDeg2meters = 111319.488, areaInMeters = 10;
		var center, procGPS;
	*/
	//	var lastLat, lastLon;
	var procRTK, rtkroutine;
	var trackRTKPort;
	//lon, lat, latOffset = 0, lonOffset = 0;
	*new
	{ | center, flag, serialPort, ofsetHeading, setup, amosca |
		("Setup is: " + setup + "aMosca is" + amosca).postln;
		if (setup.isNil)
		{
			^super.newCopyArgs().headTrackerCtr(center, flag, serialPort, ofsetHeading);
		} {
			("setup.size: " + setup.size).postln;
			
			^super.newCopyArgs().headTrackerCtr(center, flag, serialPort, ofsetHeading)
			.setCenter(setup, amosca).rtkCtr(amosca, serialPort);
		};

	}
	//rtkCtr
	setCenter
	{ | latLongAlt, amosca |
		if (latLongAlt.isArray)
		{
			if(latLongAlt.size < 8) {  // doesn't include GPS scaling params
				center = [latLongAlt[0], latLongAlt[1]];
				if (latLongAlt.size == 4) {
					lagFactor = latLongAlt[2] / latLongAlt[3];
				};
			} {
				var del = 300; // delay time take reference measurement
				               // default is 5 min (300sec)
				if (latLongAlt[12].notNil) {
					del = latLongAlt[12];
				};
				
				SystemClock.sched(del, { ("LAT is: " + amosca.lat +
					"LON is: " + amosca.lon ).postln;
					if (latLongAlt[10].isNil) {
						"No reference value".postln;
					} {
						amosca.latOffset = (amosca.lat - latLongAlt[10]);
						amosca.lonOffset = (amosca.lon - latLongAlt[11]);
						("latOffset = " + amosca.latOffset).postln;
						("lonOffset = " + amosca.lonOffset).postln;
					};
					
					
				};
					

				);
				
				("amosca = " + amosca).postln;
				("amosca.mark1 is an array? " + amosca.mark1.isArray).postln;
				("latLongAlt: " + latLongAlt).postln;
				if (amosca.mark1.isArray) {
					center = [latLongAlt[0], latLongAlt[1]];
					amosca.mark1[0] = latLongAlt[0];
					amosca.mark1[1] = latLongAlt[1];
					amosca.mark1[2] = latLongAlt[2];
					amosca.mark1[3] = latLongAlt[3];
					amosca.mark2[0] = latLongAlt[4];
					amosca.mark2[1] = latLongAlt[5];
					amosca.mark2[2] = latLongAlt[6];
					amosca.mark2[3] = latLongAlt[7];
				};
				if (latLongAlt.size >= 10) {
					lagFactor = latLongAlt[8] / latLongAlt[9];
				};
				//("latLongAlt.size is " + latLongAlt.size).println;
			};
			this.prSetFunc();
		}
		{ Error.throw("coordinates must be an array") }
	}
	

	rtkCtr
	{ | amosca, serialPort | 
		rtkroutine = Routine.new({
			inf.do({
				if (switch.v)
				{
					//"Doing something".postln;
					procRTK.value(amosca);
					0.1.wait;
				}
				{ 1.wait };
			});
		});
		rtkroutine.play;

		trackRTKPort = SerialPort(serialPort, baudRate, crtscts: true);
		/*
			trackPort.doneAction_({
			"Serial port down".postln;
			rtkroutine.stop;
			rtkroutine.reset;
		}); */

			rtkroutine = Routine.new({
			inf.do({
				
				if (switch.v)
				{ this.rtkMatchByte(trackRTKPort.read, amosca) }
				{ 1.wait };
			});
		});
			rtkroutine.play;
		
	
	}

	
	prSetFunc
	{ | amosca |
		procRTK = { | amosca | 
			var dLat, dLong, yStep, xStep, res;

			if ( amosca.lat.notNil && amosca.lon.notNil ) { 
				if(amosca.mark1[0].isNil ||
					amosca.mark2[0].isNil) {    //no marks, use centre param value
						dLat = amosca.lat - center[0];
						dLong = amosca.lon - center[1];
						("lat: " + amosca.lat + "center[0]: " + center[0]).postln;
						yStep = (dLat * latDeg2meters);
						xStep = (dLong * longDeg2meters) * cos(amosca.lat.degrad);
					} {
						yStep = (amosca.mark1[1]+(((amosca.mark2[1]-amosca.mark1[1])
							/ (amosca.mark2[2]-amosca.mark1[2]))
							* (amosca.lat - amosca.latOffset - amosca.mark1[2])));
						
						xStep = (amosca.mark1[0]+(((amosca.mark2[0]-amosca.mark1[0])
							/ (amosca.mark2[3]-amosca.mark1[3]))
							* (amosca.lon - amosca.lonOffset - amosca.mark1[3])));
						//	("xStep: " + xStep + "yStep: " + yStep + "lat: " + amosca.lat + "lon: " + amosca.lon).postln;
					};
				
				// lag in xStep
				//("lagFactor = " + lagFactor).postln;
				if (lagFactor != 0)
				{
					if (xStep != lastXStep) {
						var diff = xStep - curXStep;
												xStepIncrement = diff / lagFactor;
						lastXStep = xStep;
						interpXStep = true;
						//("Latitude: " + lat
						//	+ "Longitude: " + lon +
						//	"aMosca: " + amosca).postln;
						
						amosca.gnssLat = amosca.lat;
						amosca.gnssLon = amosca.lon;
						//("Am I different? amosca.gnssLat = " + amosca.gnssLat).postln;

					};
					if (interpXStep == true) {
						curXStep = curXStep + xStepIncrement;
						if (xStepIncrement < 0) {
							if (curXStep < lastXStep) {
								interpXStep = false;
							}
						};
						if (xStepIncrement > 0) 
						{
							if (curXStep > lastXStep) {
								interpXStep = false;
							}
						};
					};
					// Lag in yStep
					if (yStep != lastYStep) {
						var diff = yStep - curYStep;
						yStepIncrement = diff / lagFactor;
						lastYStep = yStep;
						interpYStep = true;
					};
					if (interpYStep == true) {
						curYStep = curYStep + yStepIncrement;
						if (yStepIncrement < 0) {
							if (curYStep < lastYStep) {
								interpYStep = false;
							}
						};
						if (yStepIncrement > 0)
						{
							if (curYStep > lastYStep) {
								interpYStep = false;
							}
						};
					};
				} {
					curXStep = xStep;
					curYStep = yStep;
				};
				if(amosca.mark1[0].isNil ||
					amosca.mark2[0].isNil) {    //no marks, use centre param value		
						moscaCenter.ossiaOrigin.v_([curXStep, curYStep, 0]
							/ areaInMeters);
					} {
						moscaCenter.ossiaOrigin.v_([curXStep, curYStep, 0])
					};
			}
		}
	}

	setTracker //protocol
	{
		trackarr = [251, 252, 253, 254,
			nil, nil, nil, nil, nil, nil, nil, nil, nil, nil,
			nil, nil, nil, nil, nil, nil, nil, nil, nil, nil,
			nil, nil, nil, nil,nil, nil,
			255];
	}

	rtkMatchByte
	{
		| byte, amosca | // match incoming headtracker data
		//	"called here".postln;
		//var dLat, dLong, yStep, xStep,
		var res;
		//		byte.postln;
		
		if (trackarr[tracki].isNil || (trackarr[tracki] == byte ))
		{
			trackarr2[tracki] = byte;
			tracki= tracki + 1;

			if (tracki >= trackarr.size)
			{
				var latAr = [0,0,0,0,0,0,0,0,0,0,0,0,0],
				lonAr = [0,0,0,0,0,0,0,0,0,0,0,0,0];
				//trackarr2.postln;
				
				for (0, 12,
					{ arg i;
						latAr[i] = trackarr2[i + 4];
						lonAr[i] = trackarr2[i + 17];
					});
				amosca.lat = latAr.asAscii.asFloat;	
				amosca.lon = lonAr.asAscii.asFloat;
				//				coords = [lat, lon];
				//( "Coords are: " + coords ).postln;
				//	("Lat is " + amosca.lat + "Lon is "
				//		+ amosca.lon).postln; 
				//procRTK.value();
				tracki = 0;
			};
		} {
			tracki = 0;
		};
	}
}

PozyxOSC
{
	const avreageBy = 10;
	var func;

	*new
	{ | center, flag, port = 8888, setup |

		^super.new.ctr(center, flag, port, setup);
	}

	ctr
	{ | center, flag, osc_port, setup |

		func = OSCFunc(
			{ | msg |

				if (flag.v)
				{
					var angles = (-1 * [msg[1], msg[2], msg[3]]) / 16;

					center.ossiaOrient.v_(
						[
							(angles[0]).degrad.wrap(-pi, pi),
							(angles[1]).degrad,
							(angles[2]).degrad,
						]
					);

					center.ossiaOrigin.v_(2 * ([msg[4], msg[5], msg[6]] / setup) - 1);
				}
			},
			"/position", recvPort: osc_port);
	}

	free
	{
		func.free;
		^super.free;
	}
}
	