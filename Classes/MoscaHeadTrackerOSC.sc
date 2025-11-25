MoscaHeadTrackerOSC
{
	var moscaCenter, oscPort, headingOffset, volpot, masterLevel, ossiaParent;
	var headingResponder, levelResponder;
	var updateCounter = 0, updateSkip = 4;  // Only update every 5th packet (50Hz -> 10Hz)
	
	*new
	{ | center, ossiaTrack, oscPort = 57120, offsetHeading = 0, volPot = false, ossiaParent |
		^super.new.init(center, ossiaTrack, oscPort, offsetHeading, volPot, ossiaParent);
	}
	
	init
	{ | center, ossiaTrack, port, offsetHead, volPot, parent |
		
		moscaCenter = center;
		oscPort = port;
		headingOffset = offsetHead;
		volpot = volPot;
		ossiaParent = parent;
		
		// Defer initialization to avoid blocking Mosca init
		{
			this.delayedInit();
		}.defer(1.0);
	}
	
	delayedInit
	{
		"MoscaHeadTrackerOSC: Starting delayed initialization...".postln;
		
		if (ossiaParent.notNil) {
			masterLevel = ossiaParent.find("Master_level");
		} {
			masterLevel = nil;
		};
		
		this.setupOSCResponders();
		
		("MoscaHeadTrackerOSC initialized on port " ++ oscPort).postln;
		("  Heading offset: " ++ headingOffset ++ " degrees").postln;
		("  Volume pot enabled: " ++ volpot).postln;
	}
	
	setupOSCResponders
	{
		// Orientation responder - receives all three values at once
		headingResponder = OSCdef(\orientationReceiver, { |msg|
			var h, r, p;
			
			// msg format: ['/headtracker/orientation', heading, roll, pitch] in radians
			h = msg[1];  // heading in radians
			r = msg[2];  // roll in radians
			p = msg[3];  // pitch in radians
			
			this.procGyro(h, r, p);
		}, '/headtracker/orientation', recvPort: oscPort);
		
		// Volume potentiometer responder (optional)
		if (volpot) {
			levelResponder = OSCdef(\volumeReceiver, { |msg|
				var vol = msg[1];
				this.procLev(vol);
			}, '/headtracker/volume', recvPort: oscPort);
		};
		
		"OSC responders set up for /headtracker/orientation".postln;
		if (volpot) { "OSC responder set up for /headtracker/volume".postln; };
	}
	
	procGyro
	{ | h, r, p |
		// Throttle updates - only process every 5th packet (10Hz)
		updateCounter = updateCounter + 1;
		if (updateCounter < updateSkip) {
			^this;  // Skip this update
		};
		updateCounter = 0;  // Reset counter
		
		// Safety check - don't update if moscaCenter isn't ready
		if (moscaCenter.isNil or: { moscaCenter.ossiaOrient.isNil }) {
			^this;
		};
		
		// Incoming h, r, p are already in radians (from Python script)
		// Apply heading offset and wrap
		h = h + headingOffset;
		h = h.wrap(0, 2pi) - pi;
		
		// Roll and pitch already offset by +pi in Arduino, convert to -pi..pi
		r = r - pi;
		p = p - pi;
		
		// Send to Mosca via ossiaOrient
		try {
			moscaCenter.ossiaOrient.v_([h.neg, p.neg, r]);
		} { |error|
			// Silently ignore errors
		};
	}
	
	procLev
	{ | vol |
		if (masterLevel.notNil) {
			masterLevel.v_(vol / 255.0);
		};
	}
	
	free
	{
		// Clean up OSC responders
		if (headingResponder.notNil) { headingResponder.free; };
		if (levelResponder.notNil) { levelResponder.free; };
		
		"MoscaHeadTrackerOSC freed and OSC responders removed".postln;
	}
}
