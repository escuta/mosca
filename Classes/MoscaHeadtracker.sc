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
	troutine, kroutine,
	trackarr, trackarr2, tracki, trackPort,
	lastXStep = 0, xStepIncrement, curXStep = 0, interpXStep = true,
	lastYStep = 0, yStepIncrement, curYStep = 0, interpYStep = true,
	lagFactor = 0;

	*new
	{ | center, flag, serialPort, ofsetHeading |

		^super.new.headTrackerCtr(center, flag, serialPort, ofsetHeading);
	}

	headTrackerCtr
	{ | center, flag, serialPort, ofsetHeading |

		moscaCenter = center;
		switch = flag;
		serport = serialPort;
		headingOffset = ofsetHeading;

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
				{ this.matchByte(trackPort.read) }
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
						troutine.play; // start tracker routine again
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
		trackarr = [251, 252, 253, 254, nil, nil, nil, nil, nil, nil, 255];
	}

	procGyro
	{ | heading, roll, pitch |

		var h, r, p;

		h = (heading / 100) - pi;
		h = h - headingOffset;

		r = (roll / 100) - pi;
		p = (pitch / 100) - pi;

		moscaCenter.ossiaOrient.v_([h.neg, p.neg, r.neg]);
	}

	matchByte
	{ | byte |  // match incoming headtracker data

		if (trackarr[tracki].isNil || ( trackarr[tracki] == byte ))
		{
			trackarr2[tracki] = byte;
			tracki = tracki + 1;

			if (tracki >= trackarr.size)
			{
				this.procGyro(
					(trackarr2[5]<<8) + trackarr2[4],
					(trackarr2[7]<<8) + trackarr2[6],
					(trackarr2[9]<<8) + trackarr2[8]
				);

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
			center = [latLongAlt[0], latLongAlt[1]];
			if (latLongAlt.size == 4) {
				lagFactor = latLongAlt[2] / latLongAlt[3];
			};
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
					("Latitude: " + coordinates[0]
						+ "Longitude: " + coordinates[1]).postln;
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
	var procRTK, rtkroutine, lon, lat;
	*new
	{ | center, flag, serialPort, ofsetHeading, setup, amosca |
		("Setup is: " + setup + "aMosca is" + amosca).postln;
		if (setup.isNil)
		{
			^super.newCopyArgs().headTrackerCtr(center, flag, serialPort, ofsetHeading);
		} {
			^super.newCopyArgs().headTrackerCtr(center, flag, serialPort, ofsetHeading)
			.setCenter(setup, amosca).rtkCtr(amosca);
		};

	}
	//rtkCtr
	
	rtkCtr
	{ | amosca | 
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

		trackPort.doneAction_({
			"Serial port down".postln;
			rtkroutine.stop;
			rtkroutine.reset;
		});

	}

	
	prSetFunc
	{ | amosca |
		procRTK = { | amosca | 
			var dLat, dLong, yStep, xStep, res;

			if ( lat.notNil && lon.notNil ) { 

				dLat = lat - center[0];
				dLong = lon - center[1];

				yStep = (dLat * latDeg2meters);
				xStep = (dLong * longDeg2meters) * cos(lat.degrad);
				// lag in xStep
				//	("lagFactor = " + lagFactor).postln;
				if (lagFactor != 0)
				{
					if (xStep != lastXStep) {
						var diff = xStep - curXStep;
						xStepIncrement = diff / lagFactor;
						lastXStep = xStep;
						interpXStep = true;
						("Latitude: " + lat
							+ "Longitude: " + lon +
							"aMosca: " + amosca).postln;
						
						amosca.gnssLat = lat;
						amosca.gnssLon = lon;

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
				if(amosca.gnssScaleLatAr[0].isNil ||
					amosca.gnssScaleLatAr[2].isNil) {		
				moscaCenter.ossiaOrigin.v_([curXStep, curYStep, 0]
					/ areaInMeters);
					} {
						"Decided on something different".postln;
					}
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

	matchByte
	{
		| byte | // match incoming headtracker data
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
				lat = latAr.asAscii.asFloat;	
				lon = lonAr.asAscii.asFloat;
				//				coords = [lat, lon];
				//( "Coords are: " + coords ).postln;
				//("Lat is " + lat + "Lon is "
				//		+ lon).postln;
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
	