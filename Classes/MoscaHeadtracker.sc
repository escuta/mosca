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
	trackarr, trackarr2, tracki, trackPort;

	*new
	{ | center, flag, serialPort, offsetheading |

		^super.newCopyArgs(center, flag, serialPort, offsetheading).headTrackerCtr();
	}

	headTrackerCtr
	{
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
				{ this.matchByte(trackPort.read) };
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

				1.wait;
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

		if (h < -pi) {
			h = pi + (pi + h);
		};

		if (h > pi) {
			h = -pi - (pi - h);
		};

		r = (roll / 100) - pi;
		p = (pitch / 100) - pi;

		moscaCenter.ossiaOrient.v_([(-1 * h), p, r]);
	}

	matchByte
	{ | byte |  // match incoming headtracker data

		if (trackarr[tracki].isNil or:{ trackarr[tracki] == byte })
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

	offsetHeading { | angle | // give offset to reset North

		headingOffset = angle;
	}

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
	var previousLat, previusLon, previusAlt;

	setTracker //protocol
	{
		trackarr = [251, 252, 253, 254, nil, nil, nil, nil, nil, nil,
			nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, 255];
	}

	procGps
	{ | lat, lon, alt |

		postln( "latitude " + lat);
		postln( "longitude " + lon);
		postln( "altitude " + alt);
	}

	matchByte
	{
		| byte | // match incoming headtracker data

		if (trackarr[tracki].isNil or:{ trackarr[tracki] == byte })
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

				this.procGps(
					(trackarr2[13]<<24) + (trackarr2[12]<<16) +
					(trackarr2[11]<<8) + trackarr2[10],
					(trackarr2[17]<<24) + (trackarr2[16]<<16) +
					(trackarr2[15]<<8) + trackarr2[14],
					(trackarr2[21]<<24) + (trackarr2[20]<<16) +
					(trackarr2[19]<<8) + trackarr2[18]
				);

				tracki = 0;
			};
		} {
			tracki = 0;
		};
	}
}

PozyxOSC
{
	var func;

	*new
	{ | center, flag, port = 8888 |

		^super.new.ctr(center, flag, port);
	}

	ctr
	{ | center, flag, osc_port |

		func = OSCFunc(
			{ | msg |

				if (flag.v)
				{
					var angles = [msg[1], msg[2], msg[3]] / 16;

					center.ossiaOrient.v_(
						[
							(-1 * angles[1]).degrad.wrap(-pi, pi),
							angles[2].degrad,
							angles[3].degrad,
						]
					);

					center.ossiaOrigine.v_(
						[msg[4], msg[5], msg[6]] / 1000
					);
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
	