# Mosca
Mosca is a SuperCollider class for GUI-assisted production of ambisonic sound fields with simulated moving or stationary sound sources. Sound fields may be decoded using a variety of built in 1st order ambisonic SuperCollider decoders (including binaural) or with external 2nd order decoders such as Ambdec in Linux. The class makes extensive use of the Ambisonic Toolkit (ATK, see: http://www.ambisonictoolkit.net/) by Joseph Anderson and the Automation quark (https://github.com/neeels/Automation) by Neels Hofmeyr.

Input sources may be any combination of mono, stereo or B-format material and the signals may originate from file, from hardware inputs (physical or from other applications such a DAW via Jack) or from SuperCollider's own synths. In the case of synth input, synths are associated by the user with a particular source in the GUI and registered in a synth registry. In this way, they are spatialised by the GUI and also receive all of the data from the GUI pertaining to the source (eg. x, y and z coordinates or auxiliary fader data). Mosca has its own transport provided by the Automation quark for recording and playback of source data. This may be used independently or may be synchronised to a DAW using Midi Machine Control (MMC) messages. This function has been tested to work with Ardour and Jack.

Mono and stereo sources are encoded as second order ambisonic signals whereas B-format signals remain as 1st order and are angled in space using "push" transformations. Source signals are attenuated proportionally to the inverse of the square root of proximity or in a linear relationship with distance, selectable on a per-source basis via the GUI. All sources are subject to high-frequency attenuation with distance and if decoding is performed by one of the ATK's 1st order decoders, a proximity effect is generated adding a bass boost to proximal sources among other phase effects to simulate wave curvature (see: http://doc.sccode.org/Classes/FoaProximity.html).

Reverberation is performed either using a B-format tail room impulse response (RIR) - the preferred method - or using simple built-in allpass filters, options selectable on creation of a Mosca instance. With both options, two reverberation level controls are included in the GUI to set proximal and distant levels. A further two reverb types are selectable in the GUI on a per-source basis for both RIR and allpass reverberation modes. The default reverb type uses John Chowning's technique of applying "local" and "global" reverberation to sources (CHOWNING). Proximal reverberation in this case is "global" and is audible by the listener from all directions when the source is close where as distant reverb is "local" in scope and is encoded as a 2nd order ambisonic signal along with the dry signal. This predominates as the source becomes more distant. The second type of reverberation may be described as a "2nd order diffuse reverberation". This technique produces reverberation weighted in the direction of sound events encoded in the dry ambisonic signal and involves conversion to and from A-format in order to apply the effect (ANDERSON). The encoded 2nd order ambisonic signal is converted to a 12-channel A-format signal and then either a) convolved with a B-format RIR which has been "upsampled" to 2nd order and converted to A-format impulse spectrum, or, as in the case of the allpass option, b) passed through a 12-channel bank of allpass filters before being converted back to a 2nd order B-format diffuse signal. Please note that the 2nd order diffuse reverberation may require the user to set a larger audio output buffer and thus increase the latency of the system. The "Chowning" type reverberation is more efficient and the "allpass" option, more still. 

Mosca also has other features including a scalable Doppler Effect on moving sources, looping of sources loaded from file, adjustment of virtual loudspeaker angle of stereo sources and in the case of B-format sources: a rotation control, adjustment of "directivity" (see ATK documentation) and a control of "contraction", whereby the B-format signal may be crossfaded with its W component and which is spatialised as a 2nd order ambisonic signal.


USING MOSCA

The user must set up a project directory with subdirectories "rir" and "auto". RIRs should have the first 100 or 120ms silenced to act as "tail" reverberator and must be placed in the "rir" directory. For convenience, please download the "moscaproject.zip" file on the following page which contains the file structure, example RIRs and B-format recordings as well as other information and demos. Note that the example RIR is recorded at 48kHz:

http://escuta.org/mosca

Please then see the methods and code examples below.

Once you have successfully opened the GUI, read this:

NOTES ON GUI COMPONENTS

a) Source pull down menu. Select a source

b) Doppler. The user must also use the "Doppler Amount" slider to adjust the effect

c) Loop. Loop sounds loaded from file

d) A-format reverb. By default the system uses the more efficient Chowning-style reverb described above. This toggle applies a second order diffuse 2nd order reverberation to mono and stereo sources as well as "contracted" B-format material (see below)

e) HW-in. Toggle this to read audio from hardware inputs. The user must specify the number of channels and the staring bus (starting with zero) in the two fields beneath the toggle. Note this will override any loaded sound file. It is up to the user to manage the start busses for the various source. If for example source 1 is a 4 channel signal and starts on bus zero, a second stereo source myst use a starting bus of 4 or higher.

f) SC-in. Get audio in from a SuperCollider synth. The user needs to specify the number of channels in the GUI but does not need to specify the starting bus. See code examples below for more information. Like HW-in, selecting SC-in for a particular source will disable any sound file that has been loaded.

g) Linear Intensity. Select this to apply linear attenuation of itensity with distance. By default, intensity is adjusted in proportion to the invesrse square root of proximity.

h) load audio. Load a sound file for a given a source.

i) show data. Open and close a data window for all sources showing all parameters.

j) show nodes. Show SuperCollider node tree.

k) show aux. Open and close an auxiliary controller window for a source. These sliders do not affect spatialisation of the source, however the data produced is sent to any "registered" SuperCollider synth is recorded and reproduced by the GUI's transport. See the code examples for more information.

l) audition. Use this button to audition a given source. Note that the transport also plays and cues sounds, "audition" should only be used to test sounds with the interface.

m) Level. Adjust playback level of source.

n) Doppler amount. See b) above.

o) Proximal Reverb. Adjust level of reverberation for proximal sources.

p) Distant Reverb. Adjust level of reverberation for distant sources.

q) Angle. Adjust angle of virtual speaker pair for stereo sources. The default is 1.05 radians or 60 degrees.

r) Rotate. Rotate a B-format signal on the horizontal plane.

s) Directivity. Adjust the directivity of B-format signal (see ATK documentation)

t) Contraction. Cross-fade between B-format signal an its W component. Note that the "contracted" signal is spatialised with 2nd order ambisonics.

u) Z-axis. Manipulate the Z-axis of current source.

v) Automation transport. Includes a "play/stop" button, a return to start button, a record button and a "snapshot" button of current values button. The Automation transport also contains a slider to move the "play head". Loaded sounds which are not looped will start at the beginning of the file at "0" on the transport and the transport fader may be used to advance through these sounds as well as advance through the recorded fader settings.

w) save auto / load auto. Save/load to/from a chosen directory.

x) Slave to MMC. Slave the transport to incoming Midi Machine Control data. This has been tested with Ardour and Jack on Linux.

ACKNOWLEDGEMENTS

Many thanks to Joseph Anderson, Neels Hofmeyr and members of the SuperCollider list for their assistance and valuable suggestions.

REFERENCES

ANDERSON, Joseph. Authoring complex Ambisonic soundfields: An artists tips & tricks. . In: DIGITAL HYBRIDITY AND SOUNDS IN SPACE JOINT SYMPOSIUM. University of Derby, UK: 2011.

CHOWNING, John M. The Simulation of Moving Sound Sources. Computer Music Journal, v. 1, n. 3, p. 4852, 1977. 

CLASSMETHODS::

METHOD:: new
Create Mosca instance, prepare RIR spectrum data and synth internals.

ARGUMENT:: projDir
Path to project directory. The project directory must have 2 subdirectories named "rir" and "auto". The first contains B-format RIRs to be used for reverberation and the second may be initially empty.

ARGUMENT:: nsources
The number of sources to be spatialised.

ARGUMENT:: width
Pixel width and height of GUI window. Minimum of 550 pixels.

ARGUMENT:: dur
Duration in seconds of automation transport.

ARGUMENT:: rir
B-Format ambisonic room impulse response (contained in projDir/rir). Ensure that it has the same sample rate as your system.

ARGUMENT:: server
Server used. Default is Server.default.

ARGUMENT:: decoder
Name of 1st order ambisonic decoder to be used. See the following ATK links:
http://doc.sccode.org/Classes/FoaDecode.html http://doc.sccode.org/Classes/FoaDecoderMatrix.html http://doc.sccode.org/Classes/FoaDecoderKernel.html

Note. If decoder is left blank, Mosca will send raw 1st order and 2nd order signals to the outputs for external decoding with, for example, AmbDec: http://kokkinizita.linuxaudio.org/linuxaudio/

ARGUMENT:: rawbus
Starting bus for raw ambisonic output. Useful for separating decoded and raw signals in patching set up.

returns:: New instance.


METHOD:: printSynthParams
Print GUI Parameters usable in SynthDefs.


returns:: A string.

INSTANCEMETHODS::

METHOD:: gui
Creates GUI window

METHOD:: registerSynth
Registers synth instance for a given source so that it may be spatialised and receive control data from GUI.

ARGUMENT:: source
The number of the source (starting with 1).

ARGUMENT:: synth
Synth instance.

METHOD:: deregisterSynth
Deregisters synth instance for a given source.

ARGUMENT:: source
The number of the source (starting with 1).

ARGUMENT:: synth
Synth instance.

METHOD:: getSCBus
Return the bus number associated with synth registrant.

ARGUMENT:: source
The number of the source (starting with 1). See code example below.

METHOD:: setTriggerFunc
Designate a function to be triggered when selecting play in the transport or auditioning a sound in SC-in mode. See code examples below. 

ARGUMENT:: source
The number of the source (starting with 1). See code example below.

ARGUMENT:: function
Function name. See code example below.

METHOD:: setStopFunc
Companion method to setTriggerFunc. Designate the name of a function to be called when a particular source stops auditioning or playing. See code examples below.

ARGUMENT:: source
The number of the source (starting with 1). See code example below.

ARGUMENT:: function
Function name. See code example below.


EXAMPLES::

code::



/*
Please also read "USING MOSCA" above

Make sure the server is stopped before running this block example code
*/

(
s = Server.local;
s.quit;
o = s.options;
o.numAudioBusChannels = 1024;
o.numInputBusChannels = 32;
o.numOutputBusChannels = 11; // 2 for stereo & 9 for 2nd order ambisonics
o.memSize = 64 * 8192;
o.numAudioBusChannels = 512;
o.numWireBufs = 512;
s.waitForBoot {

// Add a SC internal decoder (1st order only)
// See the ATK docs for more info: http://doc.sccode.org/Classes/FoaDecode.html
//~decoder = FoaDecoderKernel.newUHJ;
~decoder = FoaDecoderKernel.newCIPIC(21); // Binaural
s.sync;
MIDIIn.connect;
s.sync;

/*Create a project directory and in it create to more directories "rir" and "auto". Place your ambisonic rirs in the rir folder. For some demo rirs and ambisonic recordings see the zip archive here: http://escuta.org/mosca 

Create Mosca instance with arguments (projDir, nsources: 1, width: 800, dur: 180, rir: "allpass", server, decoder). If <decoder> is left blank, Mosca will send 2nd order and 1st order signals out of SC's outputs for decoding with an external decoder. */

//~testMosca = Mosca.new(projDir: "/home/iain/projects/ambisonics/moscaproject", nsources: 12,
//	   width: 950, dur: 180, rir: "QL14Tail2Sec.amb", decoder: ~decoder);

// Use allpass filter reverberation
~testMosca = Mosca.new(projDir: "/home/iain/projects/ambisonics/moscaproject", nsources: 12,
	   width: 995, dur: 200, rir: "allpass", decoder: ~decoder);

// Use RIR and send raw 2nd order ambi sginal to outputs (channels 2-10)
//using no decoder and rawbus value of 2.
//~testMosca = Mosca.new(projDir: "/home/iain/projects/ambisonics/moscaproject", nsources: 12,
//	   width: 995, dur: 200, rir: "QL14Tail2Sec.amb", rawbus: 2);


s.sync;

~testMosca.gui;

};

)

// If you close the Mosca window, tidy up

(
MIDIIn.disconnect; 
~decoder.free;
)

/*
Using SuperCollider synths as input

Must have SC-in selected on GUI for particular source and the number of channels entered (either 1, 2 or 4).

In this example, two sources are dedicated in the GUI. Source 1 and 2 each with 1 channel (in "No. of Channels) and select "SC-in" for both sources.

Write a synthdef. Note that you must specify an out bus in the arguments as well as any GUI data that you wish to use (in this example mx and my). For a full list of GUI data available run: */

Mosca.printSynthParams

(
SynthDef("test-out", { arg outbus=0, freq=440, mx, my;
	var source, point, adjust, delEnv;
	point = Point.new;
	point.set(mx, my);
	adjust = point.rho * 200;
	adjust = Lag.kr(adjust, 0.1);
	delEnv = Env.dadsr(0.2, attackTime: 0.01, decayTime: 200,
	sustainLevel: 0.1); // envelope with an onset delay equal to lag buffer
	source = Pulse.ar(freq + adjust, mul: EnvGen.kr(delEnv, doneAction: 2));
	Out.ar(outbus, source);

}).send(s);
)

/* 
Set up first source

The user must create a "Trigger function" that contains synths. This function will be called when "audition" a particular source or when the transport's play button is selected in the GUI. The user must use the getSCBus method to set the bus number for the particular source (sources numbered from 1 on) as well as "onFree" of the Synth class to deregister the synth (with the method deregisterSynth). The method registerSynth is used to register it.
*/

(
~source1PlayFunc = {
~mySynth = Synth.new("test-out", [\freq, 50, \outbus, ~testMosca.getSCBus(1)]).onFree({"done".postln; ~testMosca.deregisterSynth(1, ~mySynth)});

~testMosca.registerSynth(1, ~mySynth);
};
~source1StopFunc = {~mySynth.free; };
~testMosca.setTriggerFunc(1, ~source1PlayFunc); 
~testMosca.setStopFunc(1, ~source1StopFunc);
)

// second source
(
~source2PlayFunc = {
~myOtherSynth = Synth.new("test-out", [\freq, 50, \outbus, ~testMosca.getSCBus(2)]).onFree({"done".postln; ~testMosca.deregisterSynth(2, ~myOtherSynth)});

~testMosca.registerSynth(2, ~myOtherSynth);
};
~source2StopFunc = {~myOtherSynth.free; };

~testMosca.setTriggerFunc(2, ~source2PlayFunc); 
~testMosca.setStopFunc(2, ~source2StopFunc);
)



::