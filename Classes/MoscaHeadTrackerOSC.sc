/*
MoscaHeadTrackerOSC - Receives head tracking data via OSC from external Python bridge
This is an alternative to the built-in HeadTracker class for high-performance scenarios.

Usage in Mosca/Botanica:
    .headTrackerOSC(57120, volPot: true, offsetheading: -0.36)

The Python bridge must be running:
    python3 headtracker_osc.py /dev/ttyUSB0 127.0.0.1 57120

Author: Claude (for Botanica project)
*/

MoscaHeadTrackerOSC
{
	var moscaCenter, <headingOffset, volpot, masterLevel;
	var orientOSC, volumeOSC;
	
	*new
	{ | center, oscPort = 57120, ofsetHeading = 0, volPot = false, ossiaParent |
		^super.new.init(center, oscPort, ofsetHeading, volPot, ossiaParent);
	}
	
	init
	{ | center, oscPort, ofsetHeading, volPot, ossiaParent |
		moscaCenter = center;
		headingOffset = ofsetHeading;
		volpot = volPot;
		
		if (ossiaParent.notNil) {
			masterLevel = ossiaParent.find("Master_level");
		} {
			masterLevel = 0
		};
		
		("HeadTrackerOSC: Listening on port " ++ oscPort).postln;
		("volPot = " ++ volpot).postln;
		
		// OSC responder for orientation data
		orientOSC = OSCFunc({ | msg, time, addr, recvPort |
			var h, r, p;
			
			// msg format: ['/headtracker/orientation', heading, roll, pitch]
			h = msg[1];  // heading in radians
			r = msg[2];  // roll in radians
			p = msg[3];  // pitch in radians
			
			this.procGyro(h, r, p);
		}, '/headtracker/orientation', recvPort: oscPort);
		
		// OSC responder for volume pot (if enabled)
		if (volpot) {
			volumeOSC = OSCFunc({ | msg, time, addr, recvPort |
				var vol = msg[1];
				this.procLev(vol);
			}, '/headtracker/volume', recvPort: oscPort);
		};
	}
	
	procGyro
	{ | h, r, p |
		// Incoming h, r, p are already in radians (not *100 like serial version)
		// Apply heading offset and wrap
		h = h + headingOffset;
		h = h.wrap(0, 2pi) - pi;
		
		// Roll and pitch already offset by +pi in Arduino, convert to -pi..pi
		r = r - pi;
		p = p - pi;
		
		// Send to Mosca
		moscaCenter.ossiaOrient.v_([h.neg, p.neg, r]);
	}
	
	procLev
	{ | vol |
		if (masterLevel != 0) {
			masterLevel.v_(vol / 255.0);
		};
	}
	
	offsetHeading { | angle | headingOffset = angle }
	
	free
	{
		orientOSC.free;
		if (volumeOSC.notNil) { volumeOSC.free };
		^super.free;
	}
}
