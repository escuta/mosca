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
	my be downloaded here: http://escuta.org/mosca
*/

AutomationGuiProxy {
	var <>val, <>function;
	*new { arg val;
		^super.new.initAutomationProxy(val);
	}
	initAutomationProxy { arg ival;
		this.val = ival;
	}
	value {
		^this.val;
	}
	value_ { arg value;
		this.val = value;
	}
	valueAction_ { arg value;
		this.val = value;
		this.function.value(value);
	}
	action_ { arg func;
		this.function = func;
	}
	action {
		this.function.value(this.val);
	}
}

AutomationGuiProxy2 {
	var <>val, <>function, <>action;
	*new { arg val, action;
		^super.new.initAutomationProxy(val, action);
	}
	initAutomationProxy { arg ival, iaction;
		this.val = ival;
		action = iaction;
	}
	value {
		^this.val;
	}
	value_ { arg value;
		this.val = value;
	}

	doAction { action.value(this.val)
	}
	valueAction_ { |val| this.value_(val).doAction
	}

	/*	action_ { arg func;
		this.function = func;
	}
	action {
		this.function.value(this.val); 
	
		} */
}




Mosca {
	var <>myTestVar;
	var  <>kernelSize, <>scale, <>rirW, <>rirX, <>rirY, <>rirZ,
	<>rirWspectrum, <>rirXspectrum, <>rirYspectrum, <>rirZspectrum,
	rirFLUspectrum, rirFRDspectrum, rirBLDspectrum, rirBRUspectrum,

	<>irbuffer, <>bufsize, <>win, <>wdados, <>waux, <>sprite, <>nfontes,
	<>revGlobal, <>revGlobalSoa, <>revGlobalBF, <>m, <>offset, <>textbuf, <>controle,
	<>globFOATransform,
	<>sysex, <>mmcslave,
	<>synthRegistry, <>busini, <>ncan, <>swinbus,
	<>aux1, <>aux2, <>aux3, <>aux4, <>aux5,  // aux slider values 
	<>dec,
	<>triggerFunc, <>stopFunc,
	<>scInBus,
	<>globTBus,
	<>width, <>halfwidth, <>scale,
	<>insertFlag,
	<>aFormatBusFoa, <>aFormatBusSoa, 
	<>dur,
	<>firstTime,
	<>playingBF,
	<>rawbusfoa, <>rawbussoa, <>raworder,
	<>decoder,
	<>serport,
	<>offsetheading,
	<>dcheck, <>lpcheck, <>rvcheck, <>hwncheck, <>scncheck, <>lncheck, <>spcheck, <>dfcheck, <>ncanbox, <>businibox,
	<>espacializador, <>synt,
	<>lastAutomation = nil,
	<>tfield, 
	<>autoloopval=false,
	<>autoloop,
	<>streamdisk,
	<>streambuf, <>streamrate,
    <>delaytime, <>decaytime, // for allpass;
	// head tracking
	<>trackarr, <>trackarr2, <>tracki, <>trackPort,
	//<>track2arr, <>track2arr2, <>track2i,
	<>headingnumbox, <>rollnumbox, <>pitchnumbox,
	<>headingOffset,
	<>troutine, <>kroutine, <>watcher,
	<>binMasterBus,
	<>xval, <>yval, <>zval,
	<>recchans, <>recbus,
	<>mark1, <>mark2;	// 4 number arrays for marker data
	classvar server, rirW, rirX, rirY, rirZ,
	rirFLU, rirFRD, rirBLD, rirBRU,
	rirA12, // 2nd order a-format array of RIRs
	rirA12Spectrum,
	bufsize, irbuffer,
	b2a, a2b,
	blips,

	soa_a12_decoder_matrix, soa_a12_encoder_matrix,
	cart, spher, foa_a12_decoder_matrix,

	o, //debugging
	prjDr;
	classvar fftsize = 2048,
	offsetLag = 2.0,  // lag in seconds for incoming GPS data
	server;
	classvar foaEncoderOmni, foaEncoderSpread, foaEncoderDiffuse; 
	*new { arg projDir, nsources = 1, width = 800, dur = 180, rir = "allpass",
		server = Server.default, decoder = nil, rawbusfoa = 0, rawbussoa = 0, raworder = 2,
		serport = nil, offsetheading = 0, recchans = 2, recbus = 0;
		^super.new.initMosca(projDir, nsources, width, dur, rir, server, decoder,
			rawbusfoa, rawbussoa, raworder, serport, offsetheading, recchans, recbus);
	}

	

	*printSynthParams {
		var string =
		"

GUI Parameters usable in SynthDefs

\\level | level | 0 - 1 |
\\dopon | Doppler effect on/off | 0 or 1
\\dopamnt | Doppler ammount | 0 - 1 |
\\angle | Stereo angle | default 1.05 (60 degrees) | 0 - 3.14 |
\\glev | Global/Close reverb level | 0 - 1 |
\\llev | Local/Distant reverb level | 0 - 1 |
\\mx | X coord | -1 - 1 |
\\my | Y coord | -1 - 1 |
\\mz | Z coord | -1 - 1 |
\\sp | Spread 1st order encoding | 0 or 1 |
\\df | Diffuse 1st order encoding | 0 or 1 |
\\rotAngle | B-format rotation angle | -3.14 - 3.14 |
\\directang | B-format directivity | 0 - 1.57 |
\\contr | Contraction: fade between WXYZ & W | 0 - 1 |
\\rv | Diffuse 2nd order A-format reverb check | 0 or 1 |
\\ln | Linear intensity check | 0 or 1 |
\\aux1 | Auxiliary slider 1 value | 0 - 1 |
\\aux2 | Auxiliary slider 2 value | 0 - 1 |
\\aux3 | Auxiliary slider 3 value | 0 - 1 |
\\aux4 | Auxiliary slider 4 value | 0 - 1 |
\\aux5 | Auxiliary slider 5 value | 0 - 1 |
\\a1check | Auxiliary checkbox/button | 0 or 1 |
\\a2check | Auxiliary checkbox/button | 0 or 1 |
\\a3check | Auxiliary checkbox/button | 0 or 1 |
\\a4check | Auxiliary checkbox/button | 0 or 1 |
\\a5check | Auxiliary checkbox/button | 0 or 1 |

";
		^string;
		
	}

	initMosca { arg projDir, nsources, iwidth, idur, rir, iserver, idecoder,
		irawbusfoa, irawbussoa, iraworder, iserport, ioffsetheading,
		irecchans, irecbus;
		var makeSynthDefPlayers, makeSpatialisers, revGlobTxt,
		espacAmbOutFunc, espacAmbEstereoOutFunc, revGlobalAmbFunc,
		playBFormatOutFunc, playMonoInFunc, playStereoInFunc, playBFormatInFunc,
		revGlobalSoaOutFunc,
		prepareAmbSigFunc,
		localReverbFunc, localReverbStereoFunc,
		reverbOutFunc,
		bufAformat, bufAformat_soa_a12, bufWXYZ;
		server = iserver;
		b2a = FoaDecoderMatrix.newBtoA;
		a2b = FoaEncoderMatrix.newAtoB;
		foaEncoderOmni = FoaEncoderMatrix.newOmni;
		server.sync;
		foaEncoderSpread = FoaEncoderKernel.newSpread (subjectID: 6, kernelSize: 2048);
		server.sync;
		foaEncoderDiffuse = FoaEncoderKernel.newDiffuse (subjectID: 3, kernelSize: 2048);
		server.sync;
		this.globTBus = Bus.audio(server, 4);
		server.sync;
		//this.lock = ilock;
		
		
		if (iwidth < 600) {
			this.width = 600;
		} {
			this.width = iwidth;
		};
		this.halfwidth = this.width / 2;
		this.scale = this.halfwidth; // for the moment at least
		this.dur = idur;
		this.rawbusfoa = irawbusfoa;
		this.rawbussoa = irawbussoa;
		this.raworder = iraworder;
		this.decoder = idecoder;
		this.serport = iserport;
		this.offsetheading = ioffsetheading;
		this.recchans = irecchans;
		this.recbus = irecbus;



		if (this.serport.notNil) {

			SerialPort.devicePattern = this.serport; // needed in serKeepItUp routine - see below
			this.trackPort = SerialPort(this.serport, 115200, crtscts: true);
			//this.trackarr= [251, 252, 253, 254, nil, nil, nil, nil, nil, nil,
			//	nil, nil, nil, nil, nil, nil, nil, nil, 255];  //protocol 
			this.trackarr= [251, 252, 253, 254, nil, nil, nil, nil, nil, nil, 255];  //protocol 
			this.trackarr2= this.trackarr.copy; 
			this.tracki= 0;
			//this.track2arr= [247, 248, 249, 250, nil, nil, nil, nil, nil, nil, nil, nil, 255];  //protocol 
			//this.track2arr2= trackarr.copy; 
			//this.track2i= 0;


			this.trackPort.doneAction = {
				"Serial port down".postln;
				this.troutine.stop;
				this.troutine.reset;
			};
		};


		this.troutine = Routine.new({
			inf.do{ 
				this.matchTByte(this.trackPort.read); 
			}; 
		});

		this.kroutine = Routine.new({
			inf.do{
				if (this.trackPort.isOpen.not) // if serial port is closed
				{
					"Trying to reopen serial port!".postln;
					if (SerialPort.devices.includesEqual(this.serport)) // and if device is actually connected
					{
						"Device connected! Opening port!".postln;
						this.troutine.stop;
						this.troutine.reset;
						this.trackPort = SerialPort(this.serport, 115200, crtscts: true);
						this.troutine.play; // start tracker routine again
					}
					
				};
				1.wait; 
			}; 
		});
		
		this.headingOffset = this.offsetheading;

		this.mark1 = Array.newClear(4);
		this.mark2 = Array.newClear(4);
		
		this.nfontes = nsources;
		playMonoInFunc = Array.newClear(4); // one for File, Stereo, BFormat, Stream - streamed file;
		playStereoInFunc = Array.newClear(4);
		playBFormatInFunc = Array.newClear(4);
		this.synthRegistry = Array.newClear(this.nfontes);
		this.nfontes.do { arg x;
			this.synthRegistry[x] = List[];
		};

		this.streambuf = Array.newClear(this.nfontes);
		this.streamrate = Array.newClear(this.nfontes);
	

		o = OSCresponderNode(server.addr, '/tr', { |time, resp, msg| msg.postln }).add;  // debugging

		this.scInBus = Array.newClear(this.nfontes);
		this.nfontes.do { arg x;
			this.scInBus[x] = Bus.audio(server, 4);
		};
		
		this.insertFlag = Array.newClear(this.nfontes);
		this.aFormatBusFoa = Array2D.new(2, this.nfontes);
		this.aFormatBusSoa = Array2D.new(2, this.nfontes);
		
		this.nfontes.do { arg x;
			this.aFormatBusFoa[0, x] =  Bus.audio(server, 4);
			server.sync;
		};
		this.nfontes.do { arg x;
			this.aFormatBusFoa[1, x] =  Bus.audio(server, 4);
			server.sync;
		};
		this.nfontes.do { arg x;
			this.aFormatBusSoa[0, x] =  Bus.audio(server, 12);
			server.sync;
		};
		this.nfontes.do { arg x;
			this.aFormatBusSoa[1, x] =  Bus.audio(server, 12);
			server.sync;
		};
		this.nfontes.do { arg x;
			this.insertFlag[x] = 0;
		};

		
		// array of functions, 1 for each source (if defined), that will be launched on Automation's "play"
		this.triggerFunc = Array.newClear(this.nfontes);
		//companion to above. Launched by "Stop"
		this.stopFunc = Array.newClear(this.nfontes);


		// can place headtracker rotations in these functions - don't forget that the synthdefs
		// need to have there values for heading, roll and pitch "set" by serial routine
		// NO - DON'T PUT THIS HERE - Make a global synth with a common input bus
		
		///////////// Functions to substitute blocks of code in SynthDefs //////////////
		if (this.decoder.notNil) {
			espacAmbOutFunc = { |ambsinal, ambsinal1O, dec|
				Out.ar(this.globTBus, ambsinal1O);
			};
			espacAmbEstereoOutFunc = { |ambsinal1plus2, ambsinal1plus2_1O, dec|
				//				Out.ar( 0, FoaDecode.ar(ambsinal1plus2_1O, dec));
				Out.ar(this.globTBus, ambsinal1plus2_1O);
			};
			revGlobalAmbFunc = { |ambsinal, dec|
				//				Out.ar( 0, FoaDecode.ar(ambsinal, dec));
				Out.ar(this.globTBus, ambsinal);
			};
			revGlobalSoaOutFunc = { |soaSig, foaSig, dec|
				//				Out.ar( 0, FoaDecode.ar(foaSig, dec));
				Out.ar(this.globTBus, foaSig);
			};
			playBFormatOutFunc = { |player, dec|
				//				Out.ar( 0, FoaDecode.ar(player, dec));
				Out.ar(this.globTBus, player);
			};
			reverbOutFunc = { |soaBus, gbfbus, ambsinal, ambsinal1O, globallev, locallev |
				Out.ar(gbfbus, (ambsinal1O*globallev) + (ambsinal1O*locallev));};


		} {
			if(raworder == 1) {
				espacAmbOutFunc = { |ambsinal, ambsinal1O, dec|
					Out.ar( this.rawbusfoa, ambsinal1O); };
				espacAmbEstereoOutFunc = { |ambsinal1plus2, ambsinal1plus2_1O, dec|
					Out.ar( this.rawbusfoa, ambsinal1plus2_1O); };
				revGlobalAmbFunc = { |ambsinal, dec|
					Out.ar( this.rawbusfoa, ambsinal); };
				
				revGlobalSoaOutFunc = { |soaSig, foaSig, dec|
					Out.ar( this.rawbusfoa, foaSig); };
				playBFormatOutFunc = { |player, dec|
					Out.ar( this.rawbusfoa, player); };
				reverbOutFunc = { |soaBus, gbfbus, ambsinal, ambsinal1O, globallev, locallev |
					Out.ar(gbfbus, (ambsinal1O*globallev) + (ambsinal1O*locallev));	};
			} {
				espacAmbOutFunc = { |ambsinal, ambsinal1O, dec|
					Out.ar( this.rawbussoa, ambsinal); };
				espacAmbEstereoOutFunc = { |ambsinal1plus2, ambsinal1plus2_1O, dec|
					Out.ar( this.rawbussoa, ambsinal1plus2); };
				revGlobalAmbFunc = { |ambsinal, dec|
					Out.ar( this.rawbusfoa, ambsinal); };
				revGlobalSoaOutFunc = { |soaSig, foaSig, dec|
					Out.ar( this.rawbussoa, soaSig); };
				playBFormatOutFunc = { |player, dec|
					Out.ar( this.rawbusfoa, player); };
				reverbOutFunc = { |soaBus, gbfbus, ambsinal, ambsinal1O, globallev, locallev |
					Out.ar(soaBus, (ambsinal*globallev) + (ambsinal*locallev));	};
			}

		};


		////////////////// END Functions to substitute blocs of code /////////////
		

		/////////// START code for 2nd order matrices /////////////////////
		/*
			2nd-order FuMa-MaxN A-format decoder & encoder
			Author: Joseph Anderson 
			http://www.ambisonictoolkit.net
			Taken from: https://gist.github.com/joslloand/c70745ef0106afded73e1ea07ff69afc
		*/

		// a-12 decoder matrix
		soa_a12_decoder_matrix = Matrix.with([
			[ 0.11785113, 0.212662702, 0, -0.131432778, -0.0355875819, -0.279508497, 0, 0.226127124, 0 ],
			[ 0.11785113, 0.131432778, -0.212662702, 0, -0.208333333, 0, 0, -0.139754249, -0.279508497 ],
			[ 0.11785113, 0, -0.131432778, 0.212662702, 0.243920915, 0, -0.279508497, -0.0863728757, 0 ],
			[ 0.11785113, 0.212662702, 0, 0.131432778, -0.0355875819, 0.279508497, 0, 0.226127124, 0 ],
			[ 0.11785113, -0.131432778, -0.212662702, 0, -0.208333333, 0, 0, -0.139754249, 0.279508497 ],
			[ 0.11785113, 0, 0.131432778, -0.212662702, 0.243920915, 0, -0.279508497, -0.0863728757, 0 ],
			[ 0.11785113, -0.212662702, 0, -0.131432778, -0.0355875819, 0.279508497, 0, 0.226127124, 0 ],
			[ 0.11785113, -0.131432778, 0.212662702, 0, -0.208333333, 0, 0, -0.139754249, -0.279508497 ],
			[ 0.11785113, 0, 0.131432778, 0.212662702, 0.243920915, 0, 0.279508497, -0.0863728757, 0 ],
			[ 0.11785113, -0.212662702, 0, 0.131432778, -0.0355875819, -0.279508497, 0, 0.226127124, 0 ],
			[ 0.11785113, 0.131432778, 0.212662702, 0, -0.208333333, 0, 0, -0.139754249, 0.279508497 ],
			[ 0.11785113, 0, -0.131432778, -0.212662702, 0.243920915, 0, 0.279508497, -0.0863728757, 0 ],
		]);
		
		// a-12 encoder matrix
		soa_a12_encoder_matrix = Matrix.with([
			[ 0.707106781, 0.707106781, 0.707106781, 0.707106781, 0.707106781, 0.707106781, 0.707106781,
				0.707106781, 0.707106781, 0.707106781, 0.707106781, 0.707106781 ],
			[ 0.850650808, 0.525731112, 0, 0.850650808, -0.525731112, 0, -0.850650808, -0.525731112, 0,
				-0.850650808, 0.525731112, 0 ],
			[ 0, -0.850650808, -0.525731112, 0, -0.850650808, 0.525731112, 0, 0.850650808, 0.525731112,
				0, 0.850650808, -0.525731112 ],
			[ -0.525731112, 0, 0.850650808, 0.525731112, 0, -0.850650808, -0.525731112, 0, 0.850650808,
				0.525731112, 0, -0.850650808 ],
			[ -0.0854101966, -0.5, 0.585410197, -0.0854101966, -0.5, 0.585410197, -0.0854101966, -0.5,
				0.585410197, -0.0854101966, -0.5, 0.585410197 ],
			[ -0.894427191, 0, 0, 0.894427191, 0, 0, 0.894427191, 0, 0, -0.894427191, 0, 0 ],
			[ 0, 0, -0.894427191, 0, 0, -0.894427191, 0, 0, 0.894427191, 0, 0, 0.894427191 ],
			[ 0.723606798, -0.447213596, -0.276393202, 0.723606798, -0.447213596, -0.276393202,
				0.723606798, -0.447213596, -0.276393202, 0.723606798, -0.447213596, -0.276393202 ],
			[ 0, -0.894427191, 0, 0, 0.894427191, 0, 0, -0.894427191, 0, 0, 0.894427191, 0 ],
		]);
		
		/*
			1st-order FuMa-MaxN A-format decoder
		*/

		cart = [
			0.850650808352E+00,
			0,
			-0.525731112119E+00,
			0.525731112119E+00,
			-0.850650808352E+00,
			0.000000000000E+00,
			0,
			-0.525731112119E+00,
			0.850650808352E+00,
			0.850650808352E+00,
			0,
			0.525731112119E+00,
			-0.525731112119E+00,
			-0.850650808352E+00,
			0,
			0,
			0.525731112119E+00,
			-0.850650808352E+00,
			-0.850650808352E+00,
			0,
			-0.525731112119E+00,
			-0.525731112119E+00,
			0.850650808352E+00,
			0,
			0,
			0.525731112119E+00,
			0.850650808352E+00,
			-0.850650808352E+00,
			0,
			0.525731112119E+00,
			0.525731112119E+00,
			0.850650808352E+00,
			0,
			0,
			-0.525731112119E+00,
			-0.850650808352E+00
		];

		// convert to angles -- use these directions
		spher = cart.clump(3).collect({ arg cart, i;
			cart.asCartesian.asSpherical.angles;
		});	

		foa_a12_decoder_matrix = FoaEncoderMatrix.newDirections(spher).matrix.pseudoInverse;
		~teste2 = foa_a12_decoder_matrix;

		/////////// END code for 2nd order matrices /////////////////////


		

		prjDr = projDir;
		dec = this.decoder;

		if (rir != "allpass") {
			rirW = Buffer.readChannel(server, prjDr ++ "/rir/" ++ rir, channels: [0]);
			rirX = Buffer.readChannel(server, prjDr ++ "/rir/" ++ rir, channels: [1]);
			rirY = Buffer.readChannel(server, prjDr ++ "/rir/" ++ rir, channels: [2]);
			rirZ = Buffer.readChannel(server, prjDr ++ "/rir/" ++ rir, channels: [3]);
			
			bufWXYZ = Buffer.read(server, prjDr ++ "/rir/" ++ rir);
			server.sync;
			bufAformat = Buffer.alloc(server, bufWXYZ.numFrames, bufWXYZ.numChannels);
			bufAformat_soa_a12 = Buffer.alloc(server, bufWXYZ.numFrames, 12); // for second order conv
			server.sync;
			
			
			{BufWr.ar(FoaDecode.ar(PlayBuf.ar(4, bufWXYZ, loop: 0, doneAction: 2), b2a),
				bufAformat, Phasor.ar(0, BufRateScale.kr(bufAformat), 0, BufFrames.kr(bufAformat)));
				Out.ar(0, Silent.ar);
			}.play;

			
			(bufAformat.numFrames / server.sampleRate).wait;

			
			bufAformat.write(prjDr ++ "/rir/rirFlu.wav", headerFormat: "wav", sampleFormat: "int24");
			
			
			server.sync;
			
			
			{BufWr.ar(AtkMatrixMix.ar(PlayBuf.ar(4, bufWXYZ, loop: 0, doneAction: 2), foa_a12_decoder_matrix),
				bufAformat_soa_a12, 
				Phasor.ar(0, BufRateScale.kr(bufAformat), 0, BufFrames.kr(bufAformat)));
				Out.ar(0, Silent.ar);
			}.play;
			

			(bufAformat.numFrames / server.sampleRate).wait;

			bufAformat_soa_a12.write(prjDr ++ "/rir/rirSoaA12.wav", headerFormat: "wav", sampleFormat: "int24");
			
			
			
			server.sync;
			rirFLU = Buffer.readChannel(server, prjDr ++ "/rir/rirFlu.wav", channels: [0]);
			rirFRD = Buffer.readChannel(server, prjDr ++ "/rir/rirFlu.wav", channels: [1]);
			rirBLD = Buffer.readChannel(server, prjDr ++ "/rir/rirFlu.wav", channels: [2]);
			rirBRU = Buffer.readChannel(server, prjDr ++ "/rir/rirFlu.wav", channels: [3]);
			
			server.sync;
			
			
			bufsize = PartConv.calcBufSize(fftsize, rirW);

			~bufsize1=bufsize;

			rirWspectrum= Buffer.alloc(server, bufsize, 1);
			rirXspectrum= Buffer.alloc(server, bufsize, 1);
			rirYspectrum= Buffer.alloc(server, bufsize, 1);
			rirZspectrum= Buffer.alloc(server, bufsize, 1);
			server.sync;
			rirWspectrum.preparePartConv(rirW, fftsize);
			server.sync;
			rirXspectrum.preparePartConv(rirX, fftsize);
			server.sync;
			rirYspectrum.preparePartConv(rirY, fftsize);
			server.sync;
			rirZspectrum.preparePartConv(rirZ, fftsize);

			
			server.sync;
			
			rirFLUspectrum= Buffer.alloc(server, bufsize, 1);
			rirFRDspectrum= Buffer.alloc(server, bufsize, 1);
			rirBLDspectrum= Buffer.alloc(server, bufsize, 1);
			rirBRUspectrum= Buffer.alloc(server, bufsize, 1);
			server.sync;
			rirFLUspectrum.preparePartConv(rirFLU, fftsize);
			server.sync;
			rirFRDspectrum.preparePartConv(rirFRD, fftsize);
			server.sync;
			rirBLDspectrum.preparePartConv(rirBLD, fftsize);
			server.sync;
			rirBRUspectrum.preparePartConv(rirBRU, fftsize);
			server.sync;

			rirA12 = Array.newClear(12);
			rirA12Spectrum = Array.newClear(12);
			12.do { arg i;
				rirA12[i] = Buffer.readChannel(server, prjDr ++ "/rir/rirSoaA12.wav", channels: [i]);
				server.sync;
				rirA12Spectrum[i] = Buffer.alloc(server, bufsize, 1);
				server.sync;
				rirA12Spectrum[i].preparePartConv(rirA12[i], fftsize);
				server.sync;
			};
			server.sync;



			~rirSpecTest =  rirA12Spectrum;
			~rirFLUspectrum = rirFLUspectrum;

			
			rirW.free; // don't need time domain data anymore, just needed spectral version
			rirX.free;
			rirY.free;
			rirZ.free;
			rirFLU.free; 
			rirFRD.free;
			rirBLD.free;
			rirBRU.free;
			bufAformat.free;
			bufWXYZ.free;
			12.do { arg i;
				rirA12[i].free;
			};
			
			server.sync;



			/// START SYNTH DEFS ///////


			SynthDef(\blip, {
				var env = Env([0, 0.8, 1, 0], [0, 0.1, 0]);
				var blip = SinOsc.ar(1000) * EnvGen.kr(env, doneAction: 2);
				Out.ar(0, [blip, blip]
					
				)
			}).add;

			if (this.serport.notNil) {
				SynthDef.new("globFOATransformSynth",  { arg globtbus=0, heading=0, roll=0, pitch=0;
					var sig = In.ar(globtbus, 4);
					sig = FoaTransform.ar(sig, 'rtt',  Lag.kr(heading, 0.01),  Lag.kr(roll, 0.01),
						Lag.kr(pitch, 0.01));
					Out.ar( 0, FoaDecode.ar(sig, this.decoder));
				}).add;
			};
			

			SynthDef.new("revGlobalAmb",  { arg gbus;
				var sig, convsig;
				sig = In.ar(gbus, 1);
				convsig = [
					PartConv.ar(sig, fftsize, rirWspectrum), 
					PartConv.ar(sig, fftsize, rirXspectrum), 
					PartConv.ar(sig, fftsize, rirYspectrum),
					PartConv.ar(sig, fftsize, rirZspectrum)
				];
				revGlobalAmbFunc.value(convsig, dec);
			}).add;
			
			
			SynthDef.new("revGlobalBFormatAmb",  { arg gbfbus;
				var convsig, sig = In.ar(gbfbus, 4);
				sig = FoaDecode.ar(sig, b2a);
				convsig = [
					PartConv.ar(sig[0], fftsize, rirFLUspectrum), 
					PartConv.ar(sig[1], fftsize, rirFRDspectrum), 
					PartConv.ar(sig[2], fftsize, rirBLDspectrum),
					PartConv.ar(sig[3], fftsize, rirBRUspectrum)
				];
				convsig = FoaEncode.ar(convsig, a2b);
				revGlobalAmbFunc.value(convsig, dec);
			}).add;

			SynthDef.new("revGlobalSoaA12",  { arg soaBus;
				var w, x, y, z, r, s, t, u, v,
				foaSig, soaSig, tmpsig;
				var sig = In.ar(soaBus, 9);


				sig = AtkMatrixMix.ar(sig, soa_a12_decoder_matrix);
				//SendTrig.kr(Impulse.kr(1), 0, sig[0]); // debug
				tmpsig = [
					PartConv.ar(sig[0], fftsize, rirA12Spectrum[0]), 
					PartConv.ar(sig[1], fftsize, rirA12Spectrum[1]), 
					PartConv.ar(sig[2], fftsize, rirA12Spectrum[2]), 
					PartConv.ar(sig[3], fftsize, rirA12Spectrum[3]), 
					PartConv.ar(sig[4], fftsize, rirA12Spectrum[4]), 
					PartConv.ar(sig[5], fftsize, rirA12Spectrum[5]), 
					PartConv.ar(sig[6], fftsize, rirA12Spectrum[6]), 
					PartConv.ar(sig[7], fftsize, rirA12Spectrum[7]), 
					PartConv.ar(sig[8], fftsize, rirA12Spectrum[8]), 
					PartConv.ar(sig[9], fftsize, rirA12Spectrum[9]), 
					PartConv.ar(sig[10], fftsize, rirA12Spectrum[10]), 
					PartConv.ar(sig[11], fftsize, rirA12Spectrum[11]), 
				];

				tmpsig = tmpsig*4;
				#w, x, y, z, r, s, t, u, v = AtkMatrixMix.ar(tmpsig, soa_a12_encoder_matrix);
				foaSig = [w, x, y, z];
				soaSig = [w, x, y, z, r, s, t, u, v];
				revGlobalSoaOutFunc.value(soaSig, foaSig, dec);
			}).add;

			localReverbFunc = { | lrevRef, p, fftsize, rirWspectrum, locallev |
				lrevRef.value = PartConv.ar(p, fftsize, rirWspectrum.bufnum, locallev);
			};

			localReverbStereoFunc = { | lrev1Ref, lrev2Ref, p1, p2, fftsize, rirZspectrum, locallev |
				var temp1 = p1, temp2 = p2;
				temp1 = PartConv.ar(p1, fftsize, rirZspectrum.bufnum, 1.0 * locallev);
				temp2 = PartConv.ar(p2, fftsize, rirZspectrum.bufnum, 1.0 * locallev);
				lrev1Ref.value = temp1 * locallev; 
				lrev2Ref.value = temp2 * locallev; 

			};


		} 
		{  // else use allpass filters

			this.decaytime = 1.0;
			this.delaytime = 0.04;
			SynthDef.new("revGlobalAmb",  { arg gbus;
				var sig = In.ar(gbus, 1);
				//	sig = [sig, sig, sig, sig];
				16.do({ sig = AllpassC.ar(sig, this.delaytime, { Rand(0.01,this.delaytime) }.dup(4),
					this.decaytime)});
				sig = sig / 4; // running too hot, so attenuate
				sig = FoaEncode.ar(sig, a2b);
				revGlobalAmbFunc.value(sig, dec);
			}).add;

			SynthDef.new("revGlobalBFormatAmb",  { arg gbfbus;
				var temp, sig = In.ar(gbfbus, 4);

				sig = FoaDecode.ar(sig, b2a);
				16.do({ sig = AllpassC.ar(sig, this.delaytime, { Rand(0.01,this.delaytime) }.dup(4),
					this.decaytime)});
				
				
				
				sig = FoaEncode.ar(sig, a2b);
				revGlobalAmbFunc.value(sig, dec);
			}).add;

			SynthDef.new("revGlobalSoaA12",  { arg soaBus;
				var w, x, y, z, r, s, t, u, v,
				foaSig, soaSig, tmpsig;
				var sig = In.ar(soaBus, 9);
				sig = AtkMatrixMix.ar(sig, soa_a12_decoder_matrix);
				16.do({ sig = AllpassC.ar(sig, this.delaytime, { Rand(0.001,this.delaytime) }.dup(12),
					this.decaytime)});
				#w, x, y, z, r, s, t, u, v = AtkMatrixMix.ar(sig, soa_a12_encoder_matrix);
				foaSig = [w, x, y, z];
				soaSig = [w, x, y, z, r, s, t, u, v];
				revGlobalSoaOutFunc.value(soaSig, foaSig, dec);
			}).add;

			localReverbFunc = { | lrevRef, p, fftsize, rirWspectrum, locallev |
				var temp;
				temp = p;
				16.do({ temp = AllpassC.ar(temp, this.delaytime, { Rand(0.001,this.delaytime) }, this.decaytime)});
				lrevRef.value = temp * locallev; 
			};
			localReverbStereoFunc = { | lrev1Ref, lrev2Ref, p1, p2, fftsize, rirZspectrum, locallev |
				var temp1 = p1, temp2 = p2;
				16.do({ temp1 = AllpassC.ar(temp1, this.delaytime, { Rand(0.001,this.delaytime) },
					this.decaytime)});
				16.do({ temp2 = AllpassC.ar(temp2, this.delaytime, { Rand(0.001,this.delaytime) },
					this.decaytime)});
				lrev1Ref.value = temp1 * locallev; 
				lrev2Ref.value = temp2 * locallev; 


			};
		};
		
		makeSpatialisers = { arg linear = false;
			if(linear) {
				linear = "_linear";
			} {
				linear = "";
			};

			SynthDef.new("espacAmbAFormatVerb"++linear,  {
				arg el = 0, inbus, gbus, soaBus, mx = 0, my = 0, mz = 0,
				dopon = 0, dopamnt = 0,
				glev = 0, llev = 0, contr = 1,
				gbfbus,
				sp = 0, df = 0,
				//heading = 0, roll = 0, pitch = 0,
				
				insertFlag = 0, aFormatBusOutFoa, aFormatBusInFoa,
				aFormatBusOutSoa, aFormatBusInSoa,
				aFormatFoa, aFormatSoa, ambSigFoaProcessed, ambSigSoaProcessed;

				//var w, x, y, z, r, s, t, u, v,
				var p, ambSigSoa, ambSigFoa,
				junto, rd, dopplershift, azim, dis, xatras, yatras,  
				globallev, locallev, gsig, fonte,
				intens,
				omni, spread, diffuse,
				soa_a12_sig;
				

				var lrev;
				var grevganho = 0.04; // needs less gain
				var ambSigRef = Ref(0);
				mx = Lag.kr(mx, 0.1);
				my = Lag.kr(my, 0.1);
				mz = Lag.kr(mz, 0.1);
				contr = Lag.kr(contr, 0.1);
				fonte = Cartesian.new;
				fonte.set(mx, my, mz);
				dis = 1 - fonte.rho;

				//SendTrig.kr(Impulse.kr(1),0,  dis); // debugging
				
				azim = fonte.theta;
				el = fonte.phi;
				dis = Select.kr(dis < 0, [dis, 0]); 
				dis = Select.kr(dis > 1, [dis, 1]); 
				
				// high freq attenuation
				p = In.ar(inbus, 1);
				p = LPF.ar(p, (dis) * 18000 + 2000); // attenuate high freq with distance
				// Doppler
				rd = (1 - dis) * 340;
				rd = Lag.kr(rd, 1.0);
				dopplershift= DelayC.ar(p, 0.2, rd/1640.0 * dopon * dopamnt);
				p = dopplershift;			
				// Global reverberation & intensity
				globallev = 1 / (1 - dis).sqrt;
				intens = globallev - 1;
				intens = Select.kr(intens > 4, [intens, 4]); 
				intens = Select.kr(intens < 0, [intens, 0]);
				intens = intens / 4;

				globallev = globallev - 1.0; // lower tail of curve to zero
				globallev = Select.kr(globallev > 1, [globallev, 1]); 
				globallev = Select.kr(globallev < 0, [globallev, 0]);
				globallev = globallev * Lag.kr(glev, 0.1);			
				gsig = p * globallev;
				// Local reverberation
				locallev = 1 - dis; 			
				locallev = locallev  * Lag.kr(llev, 0.1);			
				junto = p;
				//				#w, x, y, z, r, s, t, u, v = FMHEncode0.ar(junto, azim, el, intens);
				//				ambSigSoa = [w, x, y, z, r, s, t, u, v];
				//ambSigRef.value = [0,0,0,0];				
				prepareAmbSigFunc.value(ambSigRef, junto, azim, el, intens: intens, dis: dis);

				//				junto = FoaEncode.ar(junto, foaEncoderOmni);
				omni = FoaEncode.ar(junto, foaEncoderOmni);
				spread = FoaEncode.ar(junto, foaEncoderSpread);
				diffuse = FoaEncode.ar(junto, foaEncoderDiffuse);
				junto = Select.ar(df, [omni, diffuse]);
				junto = Select.ar(sp, [junto, spread]);
				
				
				ambSigFoa	 = FoaTransform.ar(junto, 'push', pi/2*contr, azim, el, intens);


				ambSigSoa = [ambSigRef[0].value, ambSigRef[1].value, ambSigRef[2].value, ambSigRef[3].value,
					ambSigRef[4].value, ambSigRef[5].value, ambSigRef[6].value, ambSigRef[7].value,
					ambSigRef[8].value];

				dis = (1 - dis) * 5.0;
				dis = Select.kr(dis < 0.001, [dis, 0.001]);
				ambSigFoa = HPF.ar(ambSigFoa, 20); // stops bass frequency blow outs by proximity
				ambSigFoa = FoaTransform.ar(ambSigFoa, 'proximity', dis);


				// convert to A-format and send to a-format out busses
				aFormatFoa = FoaDecode.ar(ambSigFoa, b2a);
				//SendTrig.kr(Impulse.kr(1), 0, aFormatBusOutFoa); // debug
				Out.ar(aFormatBusOutFoa, aFormatFoa);
				aFormatSoa = AtkMatrixMix.ar(ambSigSoa, soa_a12_decoder_matrix);
				Out.ar(aFormatBusOutSoa, aFormatSoa);

				// flag switchable selector of a-format signal (from insert or not) 
				aFormatFoa = Select.ar(insertFlag, [aFormatFoa, InFeedback.ar(aFormatBusInFoa, 4)]);
				aFormatSoa = Select.ar(insertFlag, [aFormatSoa, InFeedback.ar(aFormatBusInSoa, 12)]);

				// convert back to b-format
				ambSigFoaProcessed  = FoaEncode.ar(aFormatFoa, a2b);
				ambSigSoaProcessed = AtkMatrixMix.ar(aFormatSoa, soa_a12_encoder_matrix);
				
				//SendTrig.kr(Impulse.kr(0.5), 0, ambSigFoaProcessed); // debug
				// not sure if the b2a/a2b process degrades signal. Just in case it does:
				ambSigFoa = Select.ar(insertFlag, [ambSigFoa, ambSigFoaProcessed]);
				ambSigSoa = Select.ar(insertFlag, [ambSigSoa, ambSigSoaProcessed]);


				reverbOutFunc.value(soaBus, gbfbus, ambSigSoa, ambSigFoa, globallev, locallev);
				espacAmbOutFunc.value(ambSigSoa, ambSigFoa, dec);			
			}).add;

			SynthDef.new("espacAmbChowning"++linear,  {
				arg el = 0, inbus, gbus, soaBus, mx = -5000, my = -5000, mz = 0,
				//xoffset = 0, yoffset = 0,
				dopon = 0, dopamnt = 0, sp, df,
				glev = 0, llev = 0, contr=1,
				//heading = 0, roll = 0, pitch = 0,
				insertFlag = 0, aFormatBusOutFoa, aFormatBusInFoa,
				aFormatBusOutSoa, aFormatBusInSoa,
				aFormatFoa, aFormatSoa, ambSigFoaProcessed, ambSigSoaProcessed;
				
				var wRef, xRef, yRef, zRef, rRef, sRef, tRef, uRef, vRef, pRef,
				ambSigSoa, ambSigFoa,
				junto, rd, dopplershift, azim, dis, xatras, yatras,
				//w, x, y, z, r, s, t, u, v,
				//		globallev = 0.0001, locallev, gsig, fonte;
				globallev, locallev, gsig, fonte,
				intens,
				spread, diffuse, omni,
				soa_a12_sig;
				var lrev, p;
				var grevganho = 0.04; // needs less gain
				var w, x, y, z, r, s, t, u, v;
				var ambSigRef = Ref(0);
				var lrevRef = Ref(0);
				mx = Lag.kr(mx, 0.1);
				my = Lag.kr(my, 0.1);
				mz = Lag.kr(mz, 0.1);
				contr = Lag.kr(contr, 0.1);
				//SendTrig.kr(Impulse.kr(1), 0, contr); // debug
				fonte = Cartesian.new;
				fonte.set(mx, my, mz);
				dis = 1 - fonte.rho;



				azim = fonte.theta;
				el = fonte.phi;
				dis = Select.kr(dis < 0, [dis, 0]); 
				dis = Select.kr(dis > 1, [dis, 1]); 
				// high freq attenuation
				p = In.ar(inbus, 1);
				p = LPF.ar(p, (dis) * 18000 + 2000); // attenuate high freq with distance
				// Doppler
				rd = (1 - dis) * 340;
				rd = Lag.kr(rd, 1.0);
				dopplershift= DelayC.ar(p, 0.2, rd/1640.0 * dopon * dopamnt);
				p = dopplershift;
				// Global reverberation & intensity
				globallev = 1 / (1 - dis).sqrt;
				intens = globallev - 1;
				intens = Select.kr(intens > 4, [intens, 4]); 
				intens = Select.kr(intens < 0, [intens, 0]);
				intens = intens / 4;

				//SendTrig.kr(Impulse.kr(1), 0, intens); // debug
				globallev = globallev - 1.0; // lower tail of curve to zero
				//				SendTrig.kr(Impulse.kr(1), 0, globallev); // debug
				globallev = globallev / 3; // scale it so that it values 1 close to origin
				globallev = Select.kr(globallev > 1, [globallev, 1]); 
				globallev = Select.kr(globallev < 0, [globallev, 0]);

				

				globallev = globallev * Lag.kr(glev, 0.1);
				gsig = p * globallev;

				
				
				Out.ar(gbus, gsig); //send part of direct signal global reverb synth
				// Local reverberation
				locallev = 1 - dis; 
				locallev = locallev  * Lag.kr(llev, 0.1);

				localReverbFunc.value(lrevRef, p, fftsize, rirWspectrum, locallev);

				junto = p + lrevRef.value;


				// do decond order encoding
				prepareAmbSigFunc.value(ambSigRef, junto, azim, el, intens: intens, dis: dis);

				//				junto = FoaEncode.ar(junto, foaEncoderOmni);

				omni = FoaEncode.ar(junto, foaEncoderOmni);
				spread = FoaEncode.ar(junto, foaEncoderSpread);
				diffuse = FoaEncode.ar(junto, foaEncoderDiffuse);
				junto = Select.ar(df, [omni, diffuse]);
				junto = Select.ar(sp, [junto, spread]);
				//junto = diffuse;
				//SendTrig.kr(Impulse.kr(1), 0, df); // debug
				ambSigFoa	 = FoaTransform.ar(junto, 'push', pi/2*contr, azim, el, intens);
				

				dis = (1 - dis) * 5.0;
				dis = Select.kr(dis < 0.001, [dis, 0.001]);
				ambSigFoa = HPF.ar(ambSigFoa, 20); // stops bass frequency blow outs by proximity
				ambSigFoa = FoaTransform.ar(ambSigFoa, 'proximity', dis);

				
				
				ambSigSoa = [ambSigRef[0].value, ambSigRef[1].value, ambSigRef[2].value, ambSigRef[3].value,
					ambSigRef[4].value, ambSigRef[5].value, ambSigRef[6].value, ambSigRef[7].value,
					ambSigRef[8].value];
				
				// convert to A-format and send to a-format out busses
				aFormatFoa = FoaDecode.ar(ambSigFoa, b2a);
				//SendTrig.kr(Impulse.kr(1), 0, aFormatBusOutFoa); // debug
				Out.ar(aFormatBusOutFoa, aFormatFoa);
				aFormatSoa = AtkMatrixMix.ar(ambSigSoa, soa_a12_decoder_matrix);
				Out.ar(aFormatBusOutSoa, aFormatSoa);

				// flag switchable selector of a-format signal (from insert or not) 
				aFormatFoa = Select.ar(insertFlag, [aFormatFoa, InFeedback.ar(aFormatBusInFoa, 4)]);
				aFormatSoa = Select.ar(insertFlag, [aFormatSoa, InFeedback.ar(aFormatBusInSoa, 12)]);

				// convert back to b-format
				ambSigFoaProcessed  = FoaEncode.ar(aFormatFoa, a2b);
				ambSigSoaProcessed = AtkMatrixMix.ar(aFormatSoa, soa_a12_encoder_matrix);
				
				//SendTrig.kr(Impulse.kr(0.5), 0, ambSigFoaProcessed); // debug
				// not sure if the b2a/a2b process degrades signal. Just in case it does:
				ambSigFoa = Select.ar(insertFlag, [ambSigFoa, ambSigFoaProcessed]);
				ambSigSoa = Select.ar(insertFlag, [ambSigSoa, ambSigSoaProcessed]);
				
				espacAmbOutFunc.value(ambSigSoa, ambSigFoa, dec);			
			}).add;

			


			// This second version of espacAmb is used with contracted B-format sources

			
			SynthDef.new("espacAmb2Chowning"++linear,  { 
				arg el = 0, inbus, gbus, mx = -5000, my = -5000, mz = 0, dopon = 0,
				glev = 0, llev = 0.2,
				insertFlag = 0, aFormatBusOutFoa, aFormatBusInFoa,
				aFormatBusOutSoa, aFormatBusInSoa;
				var w, x, y, z, r, s, t, u, v, p, ambSigSoa, ambSigFoa,
				junto, rd, dopplershift, azim, dis, xatras, yatras,  
				globallev = 0.0004, locallev, gsig, fonte,
				aFormatFoa, aFormatSoa, ambSigFoaProcessed, ambSigSoaProcessed;
				var lrev,
				intens;
				var ambSigRef = Ref(0);
				var lrevRef = Ref(0);
				var grevganho = 0.20;
				mx = Lag.kr(mx, 0.1);
				my = Lag.kr(my, 0.1);
				mz = Lag.kr(mz, 0.1);
				fonte = Cartesian.new;
				fonte.set(mx, my, mz);
				dis = 1 - fonte.rho;
				azim = fonte.theta;
				el = fonte.phi;
				dis = Select.kr(dis < 0, [dis, 0]); 
				dis = Select.kr(dis > 1, [dis, 1]); 
				//SendTrig.kr(Impulse.kr(1),0,  azim); // debugging
				
				// high freq attenuation
				p = In.ar(inbus, 1);
				p = LPF.ar(p, (dis) * 18000 + 2000); // attenuate high freq with distance
				
				// Reverberação global
				globallev = 1 / (1 - dis).sqrt;
				intens = globallev - 1;
				intens = Select.kr(intens > 4, [intens, 4]); 
				intens = Select.kr(intens < 0, [intens, 0]);
				intens = intens / 4;

				globallev = globallev - 1.0; // lower tail of curve to zero
				globallev = Select.kr(globallev > 1, [globallev, 1]); 
				globallev = Select.kr(globallev < 0, [globallev, 0]);
				
				globallev = globallev * Lag.kr(glev, 0.1);
				
				
				gsig = p * globallev;
				Out.ar(gbus, gsig); //send part of direct signal global reverb synth
				
				// Reverberação local
				locallev = 1 - dis; 
				//		SendTrig.kr(Impulse.kr(1),0,  locallev); // debugging
				locallev = locallev * Lag.kr(llev, 0.1);
				
				
				
				localReverbFunc.value(lrevRef, p, fftsize, rirWspectrum, locallev);
				
				//SendTrig.kr(Impulse.kr(1),0,  lrev); // debugging
				junto = p + lrevRef.value;
				
				//				#w, x, y, z, r, s, t, u, v = FMHEncode0.ar(junto, azim, el, intens);
				//				ambSigSoa = [w, x, y, z, r, s, t, u, v]; 
				
				//	ambSigFoa = [w, x, y, z];
				prepareAmbSigFunc.value(ambSigRef, junto, azim, el, intens: intens, dis: dis);
				ambSigFoa = [ambSigRef[0].value, ambSigRef[1].value, ambSigRef[2].value, ambSigRef[3].value];
				ambSigSoa = [ambSigRef[0].value, ambSigRef[1].value, ambSigRef[2].value, ambSigRef[3].value,
					ambSigRef[4].value, ambSigRef[5].value, ambSigRef[6].value, ambSigRef[7].value,
					ambSigRef[8].value];

				dis = (1 - dis) * 5.0;
				dis = Select.kr(dis < 0.001, [dis, 0.001]);
				ambSigFoa = HPF.ar(ambSigFoa, 20); // stops bass frequency blow outs by proximity
				ambSigFoa = FoaTransform.ar(ambSigFoa, 'proximity', dis);


				// convert to A-format and send to a-format out busses
				aFormatFoa = FoaDecode.ar(ambSigFoa, b2a);
				//SendTrig.kr(Impulse.kr(1), 0, aFormatBusOutFoa); // debug
				Out.ar(aFormatBusOutFoa, aFormatFoa);
				aFormatSoa = AtkMatrixMix.ar(ambSigSoa, soa_a12_decoder_matrix);
				Out.ar(aFormatBusOutSoa, aFormatSoa);

				// flag switchable selector of a-format signal (from insert or not) 
				aFormatFoa = Select.ar(insertFlag, [aFormatFoa, InFeedback.ar(aFormatBusInFoa, 4)]);
				aFormatSoa = Select.ar(insertFlag, [aFormatSoa, InFeedback.ar(aFormatBusInSoa, 12)]);

				// convert back to b-format
				ambSigFoaProcessed  = FoaEncode.ar(aFormatFoa, a2b);
				ambSigSoaProcessed = AtkMatrixMix.ar(aFormatSoa, soa_a12_encoder_matrix);
				
				//SendTrig.kr(Impulse.kr(0.5), 0, ambSigFoaProcessed); // debug
				// not sure if the b2a/a2b process degrades signal. Just in case it does:
				ambSigFoa = Select.ar(insertFlag, [ambSigFoa, ambSigFoaProcessed]);
				ambSigSoa = Select.ar(insertFlag, [ambSigSoa, ambSigSoaProcessed]);


				
				espacAmbOutFunc.value(ambSigSoa, ambSigFoa, dec);
				
			}).add;

			SynthDef.new("espacAmb2AFormat"++linear,  { 
				arg el = 0, inbus, gbus, mx = -5000, my = -5000, mz = 0, dopon = 0,
				glev = 0, llev = 0.2, soaBus,
				//heading = 0, roll = 0, pitch = 0,
				insertFlag = 0, aFormatBusOutFoa, aFormatBusInFoa,
				aFormatBusOutSoa, aFormatBusInSoa;
				var w, x, y, z, r, s, t, u, v, p, ambSigSoa, ambSigFoa,
				aFormatFoa, aFormatSoa, ambSigFoaProcessed, ambSigSoaProcessed,
				junto, rd, dopplershift, azim, dis, xatras, yatras,  
				globallev = 0.0004, locallev, gsig, fonte;
				var lrev, intens;
				var grevganho = 0.20;
				var ambSigRef = Ref(0);
				mx = Lag.kr(mx, 0.1);
				my = Lag.kr(my, 0.1);
				mz = Lag.kr(mz, 0.1);
				fonte = Cartesian.new;
				fonte.set(mx, my, mz);
				dis = 1 - fonte.rho;
				azim = fonte.theta;
				el = fonte.phi;
				dis = Select.kr(dis < 0, [dis, 0]); 
				dis = Select.kr(dis > 1, [dis, 1]); 
				
				// high freq attenuation
				p = In.ar(inbus, 1);
				p = LPF.ar(p, (dis) * 18000 + 2000); // attenuate high freq with distance
				
				// Reverberação global
				globallev = 1 / (1 - dis).sqrt;
				intens = globallev - 1;
				intens = Select.kr(intens > 4, [intens, 4]); 
				intens = Select.kr(intens < 0, [intens, 0]);
				intens = intens / 4;

				globallev = globallev - 1.0; // lower tail of curve to zero
				globallev = Select.kr(globallev > 1, [globallev, 1]); 
				globallev = Select.kr(globallev < 0, [globallev, 0]);
				
				globallev = globallev * Lag.kr(glev, 0.1);
				
				
				gsig = p * globallev;
				
				// Reverberação local
				locallev = 1 - dis; 
				//		SendTrig.kr(Impulse.kr(1),0,  locallev); // debugging
				locallev = locallev * Lag.kr(llev, 0.1);
				
				
				junto = p ;
				
				//				#w, x, y, z, r, s, t, u, v = FMHEncode0.ar(junto, azim, el, intens);
				//				ambSigSoa = [w, x, y, z, r, s, t, u, v];

				prepareAmbSigFunc.value(ambSigRef, junto, azim, el, intens: intens, dis: dis);
				ambSigFoa = [ambSigRef[0].value, ambSigRef[1].value, ambSigRef[2].value, ambSigRef[3].value];
				ambSigSoa = [ambSigRef[0].value, ambSigRef[1].value, ambSigRef[2].value, ambSigRef[3].value,
					ambSigRef[4].value, ambSigRef[5].value, ambSigRef[6].value, ambSigRef[7].value,
					ambSigRef[8].value];


				Out.ar(soaBus, (ambSigSoa*globallev) + (ambSigSoa*locallev));

				dis = (1 - dis) * 5.0;
				dis = Select.kr(dis < 0.001, [dis, 0.001]);
				ambSigFoa = HPF.ar(ambSigFoa, 20); // stops bass frequency blow outs by proximity
				ambSigFoa = FoaTransform.ar(ambSigFoa, 'proximity', dis);

				// convert to A-format and send to a-format out busses
				aFormatFoa = FoaDecode.ar(ambSigFoa, b2a);
				//SendTrig.kr(Impulse.kr(1), 0, aFormatBusOutFoa); // debug
				Out.ar(aFormatBusOutFoa, aFormatFoa);
				aFormatSoa = AtkMatrixMix.ar(ambSigSoa, soa_a12_decoder_matrix);
				Out.ar(aFormatBusOutSoa, aFormatSoa);

				// flag switchable selector of a-format signal (from insert or not) 
				aFormatFoa = Select.ar(insertFlag, [aFormatFoa, InFeedback.ar(aFormatBusInFoa, 4)]);
				aFormatSoa = Select.ar(insertFlag, [aFormatSoa, InFeedback.ar(aFormatBusInSoa, 12)]);

				// convert back to b-format
				ambSigFoaProcessed  = FoaEncode.ar(aFormatFoa, a2b);
				ambSigSoaProcessed = AtkMatrixMix.ar(aFormatSoa, soa_a12_encoder_matrix);
				
				//SendTrig.kr(Impulse.kr(0.5), 0, ambSigFoaProcessed); // debug
				// not sure if the b2a/a2b process degrades signal. Just in case it does:
				ambSigFoa = Select.ar(insertFlag, [ambSigFoa, ambSigFoaProcessed]);
				ambSigSoa = Select.ar(insertFlag, [ambSigSoa, ambSigSoaProcessed]);
				
				
				espacAmbOutFunc.value(ambSigSoa, ambSigFoa, dec);
				
			}).add;


			


			SynthDef.new("espacAmbEstereoAFormat"++linear,  {
				arg el = 0, inbus, gbus, soaBus, gbfbus, mx = -5000, my = -5000, mz = 0, angle = 1.05,
				//xoffset = 0, yoffset = 0,
				dopon = 0, dopamnt = 0,
				sp, df,
				glev = 0, llev = 0, contr=1,
				insertFlag = 0, aFormatBusOutFoa, aFormatBusInFoa,
				aFormatBusOutSoa, aFormatBusInSoa,
				aFormatFoa, aFormatSoa, ambSigFoaProcessed, ambSigSoaProcessed;				
				var w, x, y, z, r, s, t, u, v, p, ambSigSoa,
				w1, x1, y1, z1, r1, s1, t1, u1, v1, p1, ambSigSoa1,
				w2, x2, y2, z2, r2, s2, t2, u2, v2, p2, ambSigSoa2, ambSigSoa1plus2, ambSigFoa1plus2,
				junto, rd, dopplershift, azim, dis, 
				junto1, azim1, 
				junto2, azim2,
				omni1, spread1, diffuse1,
				omni2, spread2, diffuse2,
				intens,
				globallev = 0.0001, locallev, gsig, fonte;
				var lrev;
				var grevganho = 0.20;
				var soaSigLRef = Ref(0);
				var soaSigRRef = Ref(0);
				mx = Lag.kr(mx, 0.1);
				my = Lag.kr(my, 0.1);
				mz = Lag.kr(mz, 0.1);
				contr = Lag.kr(contr, 0.1);
				fonte = Cartesian.new;
				fonte.set(mx, my);
				
				azim1 = fonte.rotate(angle / -2).theta;
				azim2 = fonte.rotate(angle / 2).theta;
				
				fonte.set(mx, my, mz);
				el = fonte.phi;
				
				dis = 1 - fonte.rho;
				dis = Select.kr(dis < 0, [dis, 0]); 
				dis = Select.kr(dis > 1, [dis, 1]); 

				p = In.ar(inbus, 2);
				p = LPF.ar(p, (dis) * 18000 + 2000); // attenuate high freq with distance
				
				// Doppler
				rd = (1 - dis) * 340; 
				rd = Lag.kr(rd, 1.0);
				dopplershift= DelayC.ar(p, 0.2, rd/1640.0 * dopon * dopamnt);
				p = dopplershift;
				
				// Reverberação global
				globallev = 1 / (1 - dis).sqrt;
				intens = globallev - 1;
				intens = Select.kr(intens > 4, [intens, 4]); 
				intens = Select.kr(intens < 0, [intens, 0]);
				intens = intens / 4;

				globallev = globallev - 1.0; // lower tail of curve to zero
				globallev = Select.kr(globallev > 1, [globallev, 1]); // verifica se o "sinal" está mais do que 1
				globallev = Select.kr(globallev < 0, [globallev, 0]); 
				
				globallev = globallev * Lag.kr(glev, 0.1);
				
				
				p1 = p[0];
				p2 = p[1];
				// Reverberação local
				locallev = 1 - dis; 
				
				locallev = locallev  * Lag.kr(llev, 0.1);
				
				
				junto1 = p1;
				junto2 = p2;
				
				
				//				#w1, x1, y1, z1, r1, s1, t1, u1, v1 = FMHEncode0.ar(junto1, azim1, el, intens);
				//				#w2, x2, y2, z2, r2, s2, t2, u2, v2 = FMHEncode0.ar(junto2, azim2, el, intens);

				prepareAmbSigFunc.value(soaSigLRef, junto1, azim1, el, intens: intens, dis: dis);
				ambSigSoa1 = [soaSigLRef[0].value, soaSigLRef[1].value, soaSigLRef[2].value, soaSigLRef[3].value,
					soaSigLRef[4].value, soaSigLRef[5].value, soaSigLRef[6].value, soaSigLRef[7].value,
					soaSigLRef[8].value];

				prepareAmbSigFunc.value(soaSigRRef, junto2, azim2, el, intens: intens, dis: dis);
				ambSigSoa2 = [soaSigRRef[0].value, soaSigRRef[1].value, soaSigRRef[2].value, soaSigRRef[3].value,
					soaSigRRef[4].value, soaSigRRef[5].value, soaSigRRef[6].value, soaSigRRef[7].value,
					soaSigRRef[8].value];
				
				
				//ambSigSoa1 = [w1, x1, y1, z1, r1, s1, t1, u1, v1]; 
				//ambSigSoa2 = [w2, x2, y2, z2, r2, s2, t2, u2, v2];

				//				junto1 = FoaEncode.ar(junto1, foaEncoderOmni);
				omni1 = FoaEncode.ar(junto1, foaEncoderOmni);
				spread1 = FoaEncode.ar(junto1, foaEncoderSpread);
				diffuse1 = FoaEncode.ar(junto1, foaEncoderDiffuse);
				junto1 = Select.ar(df, [omni1, diffuse1]);
				junto1 = Select.ar(sp, [junto1, spread1]);

				//				junto2 = FoaEncode.ar(junto2, foaEncoderOmni);
				omni2 = FoaEncode.ar(junto2, foaEncoderOmni);
				spread2 = FoaEncode.ar(junto2, foaEncoderSpread);
				diffuse2 = FoaEncode.ar(junto2, foaEncoderDiffuse);
				junto2 = Select.ar(df, [omni2, diffuse2]);
				junto2 = Select.ar(sp, [junto2, spread2]);

				
				ambSigFoa1plus2 = FoaTransform.ar(junto1, 'push', pi/2*contr, azim1, el, intens) +
				FoaTransform.ar(junto2, 'push', pi/2*contr, azim2, el, intens);

				
				ambSigSoa1plus2 = ambSigSoa1 + ambSigSoa2;


				//Out.ar(soaBus, (ambSigFoa1plus2*globallev) + (ambSigFoa1plus2*locallev));

				
				dis = (1 - dis) * 5.0;
				dis = Select.kr(dis < 0.001, [dis, 0.001]);
				ambSigFoa1plus2 = HPF.ar(ambSigFoa1plus2, 20); // stops bass frequency blow outs by proximity
				ambSigFoa1plus2 = FoaTransform.ar(ambSigFoa1plus2, 'proximity', dis);



				// convert to A-format and send to a-format out busses
				aFormatFoa = FoaDecode.ar(ambSigFoa1plus2, b2a);
				//SendTrig.kr(Impulse.kr(1), 0, aFormatBusOutFoa); // debug
				Out.ar(aFormatBusOutFoa, aFormatFoa);
				aFormatSoa = AtkMatrixMix.ar(ambSigSoa1plus2, soa_a12_decoder_matrix);
				Out.ar(aFormatBusOutSoa, aFormatSoa);

				// flag switchable selector of a-format signal (from insert or not) 
				aFormatFoa = Select.ar(insertFlag, [aFormatFoa, InFeedback.ar(aFormatBusInFoa, 4)]);
				aFormatSoa = Select.ar(insertFlag, [aFormatSoa, InFeedback.ar(aFormatBusInSoa, 12)]);

				// convert back to b-format
				ambSigFoaProcessed  = FoaEncode.ar(aFormatFoa, a2b);
				ambSigSoaProcessed = AtkMatrixMix.ar(aFormatSoa, soa_a12_encoder_matrix);
				
				//SendTrig.kr(Impulse.kr(0.5), 0, ambSigFoaProcessed); // debug
				// not sure if the b2a/a2b process degrades signal. Just in case it does:
				ambSigFoa1plus2 = Select.ar(insertFlag, [ambSigFoa1plus2, ambSigFoaProcessed]);
				ambSigSoa1plus2 = Select.ar(insertFlag, [ambSigSoa1plus2, ambSigSoaProcessed]);


				
				reverbOutFunc.value(soaBus, gbfbus, ambSigSoa1plus2, ambSigFoa1plus2, globallev, locallev);

				espacAmbEstereoOutFunc.value(ambSigSoa1plus2, ambSigFoa1plus2, dec);
				
			}).add;

			SynthDef.new("espacAmbEstereoChowning"++linear,  {
				arg el = 0, inbus, gbus, soaBus, mx = -5000, my = -5000, mz = 0, angle = 1.05,
				//xoffset = 0, yoffset = 0,
				dopon = 0, dopamnt = 0, 
				glev = 0, llev = 0, contr=1,
				sp, df,
				insertFlag = 0, aFormatBusOutFoa, aFormatBusInFoa,
				aFormatBusOutSoa, aFormatBusInSoa;			
				var w, x, y, z, r, s, t, u, v, p, ambSigSoa,
				w1, x1, y1, z1, r1, s1, t1, u1, v1, p1, ambSigSoa1,
				w2, x2, y2, z2, r2, s2, t2, u2, v2, p2, ambSigSoa2, ambSigSoa1plus2, ambSigFoa1plus2,
				junto, rd, dopplershift, azim, dis, 
				junto1, azim1, 
				junto2, azim2,
				omni1, spread1, diffuse1,
				omni2, spread2, diffuse2,
				globallev = 0.0001, locallev, gsig, fonte,
				aFormatFoa, aFormatSoa, ambSigFoaProcessed, ambSigSoaProcessed;	
				var lrev,
				intens;
				var grevganho = 0.20;
				var soaSigLRef = Ref(0);
				var soaSigRRef = Ref(0);
				var lrev1Ref =  Ref(0);
				var lrev2Ref =  Ref(0);

				mx = Lag.kr(mx, 0.1);
				my = Lag.kr(my, 0.1);
				mz = Lag.kr(mz, 0.1);
				contr = Lag.kr(contr, 0.1);
				fonte = Cartesian.new;
				fonte.set(mx, my);
				
				azim1 = fonte.rotate(angle / -2).theta;
				azim2 = fonte.rotate(angle / 2).theta;
				
				//				fonte.set(mx+xoffset, my+yoffset, mz);
				fonte.set(mx, my, mz);
				el = fonte.phi;
				
				//				dis = (1 - (fonte.rho - this.scale)) / this.scale;
				dis = 1 - fonte.rho;
				
				dis = Select.kr(dis < 0, [dis, 0]); 
				dis = Select.kr(dis > 1, [dis, 1]); 

				p = In.ar(inbus, 2);
				//p = p[0];
				p = LPF.ar(p, (dis) * 18000 + 2000); // attenuate high freq with distance
				
				// Doppler
				rd = (1 - dis) * 340; 
				rd = Lag.kr(rd, 1.0);
				dopplershift= DelayC.ar(p, 0.2, rd/1640.0 * dopon * dopamnt);
				p = dopplershift;
				
				// Reverberação global
				globallev = 1 / (1 - dis).sqrt;
				intens = globallev - 1;
				intens = Select.kr(intens > 4, [intens, 4]); 
				intens = Select.kr(intens < 0, [intens, 0]);
				intens = intens / 4;
				
				globallev = globallev - 1.0; // lower tail of curve to zero
				globallev = globallev / 3; //scale so it values 1 close to origin
				
				globallev = Select.kr(globallev > 1, [globallev, 1]); // verifica se o "sinal" está mais do que 1
				globallev = Select.kr(globallev < 0, [globallev, 0]); 
				
				globallev = globallev * Lag.kr(glev, 0.1);
				
				gsig = Mix.new(p) / 2 * globallev;
				Out.ar(gbus, gsig); //send part of direct signal global reverb synth
				
				p1 = p[0];
				p2 = p[1];
				// Reverberação local
				locallev = 1 - dis; 
				
				locallev = locallev  * Lag.kr(llev, 0.1);
				

				localReverbStereoFunc.value(lrev1Ref, lrev2Ref, p1, p2, fftsize, rirZspectrum, locallev);
				junto1 = p1 + lrev1Ref.value;
				junto2 = p2 + lrev2Ref.value;

				prepareAmbSigFunc.value(soaSigLRef, junto1, azim1, el, intens: intens, dis: dis);

				ambSigSoa1 = [soaSigLRef[0].value, soaSigLRef[1].value, soaSigLRef[2].value, soaSigLRef[3].value,
					soaSigLRef[4].value, soaSigLRef[5].value, soaSigLRef[6].value, soaSigLRef[7].value,
					soaSigLRef[8].value];

				prepareAmbSigFunc.value(soaSigRRef, junto2, azim2, el, intens: intens, dis: dis);
				ambSigSoa2 = [soaSigRRef[0].value, soaSigRRef[1].value, soaSigRRef[2].value, soaSigRRef[3].value,
					soaSigRRef[4].value, soaSigRRef[5].value, soaSigRRef[6].value, soaSigRRef[7].value,
					soaSigRRef[8].value];
				
				ambSigSoa1plus2 = ambSigSoa1 + ambSigSoa2;


				//				junto1 = FoaEncode.ar(junto1, foaEncoderOmni);
				omni1 = FoaEncode.ar(junto1, foaEncoderOmni);
				spread1 = FoaEncode.ar(junto1, foaEncoderSpread);
				diffuse1 = FoaEncode.ar(junto1, foaEncoderDiffuse);
				junto1 = Select.ar(df, [omni1, diffuse1]);
				junto1 = Select.ar(sp, [junto1, spread1]);

				
				//				junto2 = FoaEncode.ar(junto2, foaEncoderOmni);
				omni2 = FoaEncode.ar(junto2, foaEncoderOmni);
				spread2 = FoaEncode.ar(junto2, foaEncoderSpread);
				diffuse2 = FoaEncode.ar(junto2, foaEncoderDiffuse);
				junto2 = Select.ar(df, [omni2, diffuse2]);
				junto2 = Select.ar(sp, [junto2, spread2]);

				ambSigFoa1plus2 = FoaTransform.ar(junto1, 'push', pi/2*contr, azim1, el, intens) +
				FoaTransform.ar(junto2, 'push', pi/2*contr, azim2, el, intens);

				
				dis = (1 - dis) * 5.0;
				dis = Select.kr(dis < 0.001, [dis, 0.001]);
				ambSigFoa1plus2 = HPF.ar(ambSigFoa1plus2, 20); // stops bass frequency blow outs by proximity
				ambSigFoa1plus2 = FoaTransform.ar(ambSigFoa1plus2, 'proximity', dis);

				// convert to A-format and send to a-format out busses
				aFormatFoa = FoaDecode.ar(ambSigFoa1plus2, b2a);
				//SendTrig.kr(Impulse.kr(1), 0, aFormatBusOutFoa); // debug
				Out.ar(aFormatBusOutFoa, aFormatFoa);
				aFormatSoa = AtkMatrixMix.ar(ambSigSoa1plus2, soa_a12_decoder_matrix);
				Out.ar(aFormatBusOutSoa, aFormatSoa);

				// flag switchable selector of a-format signal (from insert or not) 
				aFormatFoa = Select.ar(insertFlag, [aFormatFoa, InFeedback.ar(aFormatBusInFoa, 4)]);
				aFormatSoa = Select.ar(insertFlag, [aFormatSoa, InFeedback.ar(aFormatBusInSoa, 12)]);

				// convert back to b-format
				ambSigFoaProcessed  = FoaEncode.ar(aFormatFoa, a2b);
				ambSigSoaProcessed = AtkMatrixMix.ar(aFormatSoa, soa_a12_encoder_matrix);
				
				//SendTrig.kr(Impulse.kr(0.5), 0, ambSigFoaProcessed); // debug
				// not sure if the b2a/a2b process degrades signal. Just in case it does:
				ambSigFoa1plus2 = Select.ar(insertFlag, [ambSigFoa1plus2, ambSigFoaProcessed]);
				ambSigSoa1plus2 = Select.ar(insertFlag, [ambSigSoa1plus2, ambSigSoaProcessed]);

				

				espacAmbEstereoOutFunc.value(ambSigSoa1plus2, ambSigFoa1plus2, dec);
				
			}).add;

		}; //end makeSpatialisers


		
		prepareAmbSigFunc = { |ambSigRef, junto, azim, el, intens, dis|
			ambSigRef.value = FMHEncode0.ar(junto, azim, el, intens);				
		};
		makeSpatialisers.value(linear: false);
		
		prepareAmbSigFunc = { |ambSigRef, junto, azim, el, intens, dis|
			ambSigRef.value = FMHEncode0.ar(junto, azim, el, dis);
		};
		makeSpatialisers.value(linear: true);

		
		makeSynthDefPlayers = { arg type, i = 0;    // 3 types : File, HWBus and SWBus - i duplicates with 0, 1 & 2

			SynthDef.new("playMono"++type, { arg outbus, bufnum = 0, rate = 1, 
				level = 0, tpos = 0, lp = 0, busini;
				var scaledRate, spos, playerRef;
				playerRef = Ref(0);
				playMonoInFunc[i].value(playerRef, busini, bufnum, scaledRate, tpos, spos, lp, rate);
				//SendTrig.kr(Impulse.kr(1),0,  funcString); // debugging
				Out.ar(outbus, playerRef.value * Lag.kr(level, 0.1));
			}).add;

			SynthDef.new("playStereo"++type, { arg outbus, bufnum = 0, rate = 1, 
				level = 0, tpos = 0, lp = 0, busini;
				var scaledRate, spos, playerRef;
				playerRef = Ref(0);
				playStereoInFunc[i].value(playerRef, busini, bufnum, scaledRate, tpos, spos, lp, rate);
				Out.ar(outbus, playerRef.value * Lag.kr(level, 0.1));
			}).add;

			2.do {   // make linear and non-linear versions
				arg x;
				var prepareRotateFunc, linear = "";
				if (x == 1) {
					linear = "_linear";
					prepareRotateFunc = {|dis, intens, playerRef, contr, rotAngle, level|
						playerRef.value = FoaTransform.ar(playerRef.value, 'rotate', rotAngle,
							Lag.kr(level, 0.1) * dis * (1 - contr));
					};
				} {
					prepareRotateFunc = {|dis, intens, playerRef, contr, rotAngle, level|
						playerRef.value = FoaTransform.ar(playerRef.value, 'rotate', rotAngle,
							Lag.kr(level, 0.1) * intens * (1 - contr));
					};
				};
				
				
				SynthDef.new("playBFormat"++type++linear, { arg outbus, bufnum = 0, rate = 1, 
					level = 0, tpos = 0, lp = 0, rotAngle = 0, tilAngle = 0, tumAngle = 0,
					mx = 0, my = 0, mz = 0, gbus, gbfbus, glev, llev, directang = 0, contr, dopon, dopamnt,
					//xoffset = 0, yoffset = 0,
					busini,
					insertFlag = 0, aFormatBusOutFoa, aFormatBusInFoa,
					aFormatBusOutSoa, aFormatBusInSoa;

					var scaledRate, playerRef, wsinal, spos, pushang = 0,
					aFormatFoa, aFormatSoa, ambSigFoaProcessed, ambSigSoaProcessed,
					
					azim, dis = 1, fonte, globallev, locallev, 
					gsig, lsig, rd, dopplershift,
					intens;
					var grevganho = 0.20;
					//SendTrig.kr(Impulse.kr(1), 0, mz); // debug
					mx = Lag.kr(mx, 0.1);
					my = Lag.kr(my, 0.1);
					mz = Lag.kr(mz, 0.1);
					
					
					fonte = Cartesian.new;
					fonte.set(mx, my, mz);
					dis = 1 - fonte.rho;
					pushang = (1 - dis) * pi / 2; // degree of sound field displacement
					azim = fonte.theta; // ângulo (azimuth) de deslocamento
					dis = Select.kr(dis < 0, [dis, 0]); 
					dis = Select.kr(dis > 1, [dis, 1]); 
					playerRef = Ref(0);
					playBFormatInFunc[i].value(playerRef, busini, bufnum, scaledRate, tpos, spos, lp, rate);
					
					rd = (1 - dis) * 340;
					rd = Lag.kr(rd, 1.0);
					dopplershift= DelayC.ar(playerRef.value, 0.2, rd/1640.0 * dopon * dopamnt);
					playerRef.value = dopplershift;
					
					wsinal = playerRef.value[0] * contr * Lag.kr(level, 0.1) * dis * 2.0;
					
					Out.ar(outbus, wsinal);
					
					// global reverb
					globallev = 1 / (1 - dis).sqrt;
					intens = globallev - 1;
					intens = Select.kr(intens > 4, [intens, 4]); 
					intens = Select.kr(intens < 0, [intens, 0]);
					intens = intens / 4;
					//SendTrig.kr(Impulse.kr(1), 0, dis); // debug
					playerRef.value = FoaDirectO.ar(playerRef.value, directang); // diretividade ("tamanho")
					

					prepareRotateFunc.value(dis, intens, playerRef, contr, rotAngle, Lag.kr(level, 0.1));

					playerRef.value = FoaTransform.ar(playerRef.value, 'push', pushang, azim);


					// convert to A-format and send to a-format out busses
					aFormatFoa = FoaDecode.ar(playerRef.value, b2a);
					//SendTrig.kr(Impulse.kr(1), 0, aFormatBusOutFoa); // debug
					Out.ar(aFormatBusOutFoa, aFormatFoa);
					//	aFormatSoa = AtkMatrixMix.ar(ambSigSoa, soa_a12_decoder_matrix);
					//Out.ar(aFormatBusOutSoa, aFormatSoa);

					// flag switchable selector of a-format signal (from insert or not) 
					aFormatFoa = Select.ar(insertFlag, [aFormatFoa, InFeedback.ar(aFormatBusInFoa, 4)]);
					//aFormatSoa = Select.ar(insertFlag, [aFormatSoa, InFeedback.ar(aFormatBusInSoa, 12)]);

					// convert back to b-format
					ambSigFoaProcessed  = FoaEncode.ar(aFormatFoa, a2b);
					//ambSigSoaProcessed = AtkMatrixMix.ar(aFormatSoa, soa_a12_encoder_matrix);
					
					//SendTrig.kr(Impulse.kr(0.5), 0, ambSigFoaProcessed); // debug
					// not sure if the b2a/a2b process degrades signal. Just in case it does:
					playerRef.value = Select.ar(insertFlag, [playerRef.value, ambSigFoaProcessed]);
					//ambSigSoa = Select.ar(insertFlag, [ambSigSoa, ambSigSoaProcessed]);

					
					
					//	Out.ar(2, player);
					playBFormatOutFunc.value(playerRef.value, dec);
					
					
					globallev = globallev - 1.0; // lower tail of curve to zero
					globallev = Select.kr(globallev > 1, [globallev, 1]); 
					globallev = Select.kr(globallev < 0, [globallev, 0]); 
					globallev = globallev * Lag.kr(glev, 0.1) * 6;
					
					gsig = playerRef.value[0] * globallev;
					
					locallev = 1 - dis; 
					
					//				locallev = locallev  * (llev*10) * grevganho;
					locallev = locallev  * Lag.kr(llev, 0.1) * 5;
					lsig = playerRef.value[0] * locallev;
					
					
					// trying again ... testing
					
					gsig = (playerRef.value * globallev) + (playerRef.value * locallev); // b-format
					Out.ar(gbfbus, gsig); 
					
					
				}).add;
			};
			
		}; //end makeSynthDefPlayers

		// Make File-in SynthDefs
		
		playMonoInFunc[0] = {
			arg playerRef, busini, bufnum, scaledRate, tpos, spos, lp = 0, rate; // Note it needs all the variables
			
			spos = tpos * BufSampleRate.kr(bufnum);
			scaledRate = rate * BufRateScale.kr(bufnum);
			playerRef.value = PlayBuf.ar(1, bufnum, scaledRate, startPos: spos, loop: lp, doneAction:2);			
		};
		
		playStereoInFunc[0] = {
			arg playerRef, busini, bufnum, scaledRate, tpos, spos, lp = 0, rate;
			spos = tpos * BufSampleRate.kr(bufnum);
			scaledRate = rate * BufRateScale.kr(bufnum);
			playerRef.value = PlayBuf.ar(2, bufnum, scaledRate, startPos: spos, loop: lp, doneAction:2);			
		};

		playBFormatInFunc[0] = {
			arg playerRef, busini, bufnum, scaledRate, tpos, spos, lp = 0, rate;
			spos = tpos * BufSampleRate.kr(bufnum);
			scaledRate = rate * BufRateScale.kr(bufnum); 
			playerRef.value = PlayBuf.ar(4, bufnum, scaledRate, startPos: spos, loop: lp, doneAction:2);
		};
		
		makeSynthDefPlayers.("File", 0);


		playMonoInFunc[3] = {
			arg playerRef, busini, bufnum, scaledRate, tpos, spos, lp = 0, rate; // Note it needs all the variables
			var trig;
			playerRef.value = DiskIn.ar(1, bufnum, lp);
			trig = Done.kr(playerRef.value);
			FreeSelf.kr(trig);
		};
		
		playStereoInFunc[3] = {
			arg playerRef, busini, bufnum, scaledRate, tpos, spos, lp = 0, rate;
			var trig;
			playerRef.value = DiskIn.ar(2, bufnum, lp);
			trig = Done.kr(playerRef.value);
			FreeSelf.kr(trig);
		};

		playBFormatInFunc[3] = {
			arg playerRef, busini, bufnum, scaledRate, tpos, spos, lp = 0, rate;
			var trig;
			playerRef.value = DiskIn.ar(4, bufnum, lp);
			trig = Done.kr(playerRef.value);
			FreeSelf.kr(trig);	
		};
		
		makeSynthDefPlayers.("Stream", 3);

		// Make HWBus-in SynthDefs

		
		playMonoInFunc[1] = {
			arg playerRef, busini, bufnum, scaledRate, tpos, spos, lp = 0, rate;
			playerRef.value =  SoundIn.ar(busini, 1);
		};
		
		playStereoInFunc[1] = {
			arg playerRef, busini, bufnum, scaledRate, tpos, spos, lp = 0, rate;
			playerRef.value =  [SoundIn.ar(busini), SoundIn.ar(busini + 1)];
		};
		

		playBFormatInFunc[1] = {
			arg playerRef, busini = 0, bufnum, scaledRate, tpos, spos, lp = 0, rate;
			playerRef.value =  [SoundIn.ar(busini), SoundIn.ar(busini + 1),
				SoundIn.ar(busini + 2), SoundIn.ar(busini + 3)];

		};
		
		
		makeSynthDefPlayers.("HWBus", 1);
		//("Bus is " ++ this.swinbus[0]).postln;

		// Make SCBus In SynthDefs

		playMonoInFunc[2] = {
			arg playerRef, busini, bufnum, scaledRate, tpos, spos, lp = 0, rate;
			playerRef.value =  In.ar(busini, 1);
		};
		
		playStereoInFunc[2] = {
			arg playerRef, busini, bufnum, scaledRate, tpos, spos, lp = 0, rate;
			playerRef.value =  [In.ar(busini, 1), In.ar(busini + 1, 1)];
		};
		

		playBFormatInFunc[2] = {
			arg playerRef, busini = 0, bufnum, scaledRate, tpos, spos, lp = 0, rate;
			playerRef.value =  [In.ar(busini, 1), In.ar(busini + 1, 1),
				In.ar(busini + 2, 1), In.ar(busini + 3, 1)];

		};
		

		makeSynthDefPlayers.("SWBus", 2);

		
		//////// END SYNTHDEFS ///////////////

		

	} // end initMosca

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
		h = h + this.headingOffset;
		if (h < -pi) {
			h = pi + (pi + h);
		};
		if (h > pi) {
			h = -pi - (pi - h);
		};
		
		r = (roll / 100) - pi;
		p = (pitch / 100) - pi;
		{this.headingnumbox.valueAction = h}.defer;
		{this.rollnumbox.valueAction = r}.defer;
		{this.pitchnumbox.valueAction = p}.defer;
		this.nfontes.do { arg i;
			
			
			sprite[i, 1] = ((xval[i] * this.halfwidth * -1) + this.halfwidth);
			sprite[i, 0] = ((yval[i] * this.halfwidth * -1) + this.halfwidth);

			if(this.espacializador[i].notNil) {
				
				this.espacializador[i].set(\mx, this.xval[i], \my, this.yval[i]);
				this.setSynths(i, \mx, this.xval[i], \my, this.yval[i]);
				this.synt[i].set(\mx, this.xval[i], \my, this.yval[i]);
			};
			
		};


		
		
	}

	matchTByte {|byte|  // match incoming headtracker data
		
        if(this.trackarr[this.tracki].isNil or:{this.trackarr[this.tracki]==byte}, { 
			this.trackarr2[this.tracki]= byte; 
			this.tracki= this.tracki+1; 
			if(this.tracki>=this.trackarr.size, { 
				//				this.procTracker(this.trackarr2[4]<<8+this.trackarr2[5],
				//				this.trackarr2[6]<<8+this.trackarr2[7], this.trackarr2[8]<<8+this.trackarr2[9],
				this.procTracker(
					(this.trackarr2[5]<<8)+this.trackarr2[4],
					(this.trackarr2[7]<<8)+this.trackarr2[6], (this.trackarr2[9]<<8)+this.trackarr2[8]
					//,
					//(this.trackarr2[13]<<24) + (this.trackarr2[12]<<16) + (this.trackarr2[11]<<8) + this.trackarr2[10],
					//(this.trackarr2[17]<<24) + (this.trackarr2[16]<<16) + (this.trackarr2[15]<<8) + this.trackarr2[14]
				); 
				this.tracki= 0;
			}); 
        }, { 
			this.tracki= 0; 
        });
	}

	

	trackerRoutine {Routine.new({
		inf.do{
			//this.trackPort.read.postln;
			this.matchTByte(this.trackPort.read); 
		}; 
	})}

	serialKeepItUp {Routine.new({
		inf.do{
			if (this.trackPort.isOpen.not) // if serial port is closed
			{
				"Trying to reopen serial port!".postln;
				if (SerialPort.devices.includesEqual(this.serport)) // and if device is actually connected
				{
					"Device connected! Opening port!".postln;
					this.trackPort = SerialPort(this.serport, 115200, crtscts: true);
					this.trackerRoutine; // start tracker routine again
				}

			};
			1.wait; 
		}; 
	})}

	offsetHeading { // give offset to reset North
		| angle |
		this.headingOffset = angle;
	}
	
	
	registerSynth { // selection of Mosca arguments for use in synths
		| source, synth |
		this.synthRegistry[source-1].add(synth);
	}
	deregisterSynth { // selection of Mosca arguments for use in synths
		| source, synth |
		if(this.synthRegistry[source-1].notNil){
			this.synthRegistry[source-1].remove(synth);
			
		};
	}


	getSynthRegistry { // selection of Mosca arguments for use in synths
		| source |
		^this.synthRegistry[source-1];
	}

	getSCBus {
		|source |
		if (source > 0) {
			var bus = this.scInBus[source - 1].index;
			^bus
		}
	}


	setSynths {
		|source, param, value|
		
		this.synthRegistry[source].do({
			arg item, i;
			
			//	if(item.isPlaying) {
			item.set(param, value);
			//	}
		});
		
	}

	getFoaInsertIn {
		|source |
		if (source > 0) {
			var bus = this.aFormatBusFoa[0,source-1];
			this.insertFlag[source-1]=1;
			this.espacializador[source-1].set(\insertFlag, 1);
			this.synt[source-1].set(\insertFlag, 1);
			^bus
		}
	}
	getFoaInsertOut {
		|source |
		if (source > 0) {
			var bus = this.aFormatBusFoa[1,source-1];
			this.insertFlag[source-1]=1;
			this.espacializador[source-1].set(\insertFlag, 1);
			this.synt[source-1].set(\insertFlag, 1);
			^bus
		}
	}
	getSoaInsertIn {
		|source |
		if (source > 0) {
			var bus = this.aFormatBusSoa[0,source-1];
			this.insertFlag[source-1]=1;
			this.espacializador[source-1].set(\insertFlag, 1);
			^bus
		}
	}
	getSoaInsertOut {
		|source |
		if (source > 0) {
			var bus = this.aFormatBusSoa[1,source-1];
			this.insertFlag[source-1]=1;
			this.espacializador[source-1].set(\insertFlag, 1);
			^bus
		}
	}
	releaseInsert {
		|source |
		if (source > 0) {
			this.insertFlag[source-1]=0;
			this.espacializador[source-1].set(\insertFlag, 0);
		}
	}
	
	// These methods relate to control of synths when SW Input delected
	// for source in GUI
	
	// Set by user. Registerred functions called by Automation's play
	setTriggerFunc {
		|source, function|
		if (source > 0) {
			this.triggerFunc[source-1] = function;
		}
	}
	// Companion stop method
	setStopFunc {
		|source, function|
		if (source > 0) {
			this.stopFunc[source-1] = function;
		}
	}
	clearTriggerFunc {
		|source|
		if (source > 0) {
			this.triggerFunc[source-1] = nil;
		}
	}
	clearStopFunc {
		|source|
		if (source > 0) {
			this.stopFunc[source-1] = nil;
		}
	}
	// load automation data. Can be called after creation of Mosca instance
	loadAutomation {
		|path|
		var filenames;
		var dopplerf, loopedf, aformatrevf, hwinf, scinf, linearf, spreadf, diffusef, ncanf, businif;
		this.controle.load(path);
		//this.controle.seek; 
		this.lastAutomation = path;
		filenames = File((path ++ "/filenames.txt").standardizePath,"r");

		dopplerf = File((path ++ "/doppler.txt").standardizePath,"r");
		loopedf = File((path ++ "/looped.txt").standardizePath,"r");
		aformatrevf = File((path ++ "/aformatrev.txt").standardizePath,"r");
		hwinf = File((path ++ "/hwin.txt").standardizePath,"r");
		scinf = File((path ++ "/scin.txt").standardizePath,"r");
		linearf = File((path ++ "/linear.txt").standardizePath,"r");
		spreadf = File((path ++ "/spread.txt").standardizePath,"r");
		diffusef = File((path ++ "/diffuse.txt").standardizePath,"r");
		ncanf = File((path ++ "/ncan.txt").standardizePath,"r");
		businif = File((path ++ "/busini.txt").standardizePath,"r");
		
		this.nfontes.do { arg i;
			var line = filenames.getLine(1024);
			if(line!="NULL"){this.tfield[i].valueAction = line};
		};
				nfontes.do { arg i;
					var line = dopplerf.getLine(1024);
					this.dcheck[i].valueAction = line;
				};

				nfontes.do { arg i;
					var line = loopedf.getLine(1024);
					this.lpcheck[i].valueAction = line;
				};
				nfontes.do { arg i;
					var line = aformatrevf.getLine(1024);
					this.rvcheck[i].valueAction = line;
				};
				nfontes.do { arg i;
					var line = hwinf.getLine(1024);
					this.hwncheck[i].valueAction = line;
				};
				nfontes.do { arg i;
					var line = scinf.getLine(1024);
					this.scncheck[i].valueAction = line;
				};
				nfontes.do { arg i;
					var line = linearf.getLine(1024);
					this.lncheck[i].valueAction = line;
				};
				nfontes.do { arg i;
					var line = spreadf.getLine(1024);
					this.spcheck[i].valueAction = line;
				};
				nfontes.do { arg i;
					var line = diffusef.getLine(1024);
					this.dfcheck[i].valueAction = line;
				};
				nfontes.do { arg i;
					var line = ncanf.getLine(1024);
					this.ncanbox[i].valueAction = line.asFloat;
				};
				nfontes.do { arg i;
					var line = businif.getLine(1024);
					this.businibox[i].valueAction = line.asFloat;
				};

		
		filenames.close;

		dopplerf.close;
		loopedf.close;
		aformatrevf.close;
		hwinf.close;
		scinf.close;
		linearf.close;
		spreadf.close;
		diffusef.close;
		ncanf.close;
		businif.close;
		
	}


	

	playAutomation {
		this.controle.play;
	}
	playAutomationLooped {
		//this.autoloopval = true;
		this.autoloop.valueAction = true;
		this.controle.play;
	}


	gui {

		//arg dur = 120;
		var fonte, dist, mbus, sbus, soaBus, ncanais, fatual = 0, 
		itensdemenu, gbus, gbfbus, azimuth, event, brec, bplay, bload, bstream, bnodes, sombuf, funcs, 
		dopcheque,
		//lastAutomation = nil,
		loopcheck,
		//lpcheck,
		lp,
		spreadcheck,
		//spcheck,
		sp,
		diffusecheck,
		//dfcheck,
		df,
		revcheck,
		//rvcheck,
		rv,
		lincheck,
		//lncheck,
		ln,
		hwInCheck,
		//hwncheck,
		hwn, scInCheck,
		//scncheck,
		scn,
		dopcheque2, doppler, angle, level, glev, 
		llev, angnumbox, volnumbox,
		ncannumbox, busininumbox, // for streams. ncan = number of channels (1, 2 or 4)
		// busini = initial bus number in range starting with "0"
		//ncanbox, businibox,
		mouseButton, dragStartScreen,
		novoplot,
		runTriggers, runStops, runTrigger, runStop,
		
		dopnumbox, volslider, dirnumbox, dirslider, connumbox, conslider, cbox,
		a1box, a2box, a3box, a4box, a5box, 
		a1but, a2but, a3but, a4but, a5but, // variable
		a1check, a2check, a3check, a4check, a5check, // data windows representation of a1but etc (ie. as checkbox)
		stcheck, // check box for streamed from disk audio
		angslider, bsalvar, bcarregar, bsnap, bdados, baux, xbox, ybox, abox, vbox, gbox, lbox, dbox, dpbox,
		//dcheck,
		oxbox, oybox, ozbox,
		lastx, lasty, lastz,
		gslider, gnumbox, lslider, lnumbox, dopflag = 0, btestar, brecaudio,
		blipcheck,
		tocar, isPlay = false, isRec,
		atualizarvariaveis, updateSynthInArgs,
		updatesourcevariables,
		auxslider1, auxslider2, auxslider3, auxslider4, auxslider5, // aux sliders in control window
		auxbutton1, auxbutton2, auxbutton3, auxbutton4, auxbutton5, // aux sliders in control window
		testado,
		rnumbox, rslider, rbox, 
		znumbox, zslider, zbox, zlev, // z-axis
		bmark1, bmark2,
		
		rlev, dlev, clev, cslider, dplev, dpslider, cnumbox,
		aux1numbox, aux2numbox, aux3numbox, aux4numbox, aux5numbox, 
		zSliderHeight = this.width * 2 / 3;
		dragStartScreen = Point.new;
		//dragStartMap = Point.new;
		this.espacializador = Array.newClear(this.nfontes);
		doppler = Array.newClear(this.nfontes); 
		lp = Array.newClear(this.nfontes); 
		sp = Array.newClear(this.nfontes); 
		df = Array.newClear(this.nfontes); 
		rv = Array.newClear(this.nfontes); 
		ln = Array.newClear(this.nfontes); 
		hwn = Array.newClear(this.nfontes); 
		scn = Array.newClear(this.nfontes); 
		mbus = Array.newClear(this.nfontes); 
		sbus = Array.newClear(this.nfontes); 
		//soaBus = Array.newClear(this.nfontes); 
		ncanais = Array.newClear(this.nfontes);  // 0 = não, nem estéreo. 1 = mono. 2 = estéreo.
		this.ncan = Array.newClear(this.nfontes);  // 0 = não, nem estéreo. 1 = mono. 2 = estéreo.
		// note that ncan refers to # of channels in streamed sources.
		// ncanais is related to sources read from file
		this.busini = Array.newClear(this.nfontes); // initial bus # in streamed audio grouping (ie. mono, stereo or b-format)
		this.aux1 = Array.newClear(this.nfontes);
		this.aux2 = Array.newClear(this.nfontes);
		this.aux3 = Array.newClear(this.nfontes);
		this.aux4 = Array.newClear(this.nfontes);
		this.aux5 = Array.newClear(this.nfontes);

		a1but = Array.newClear(this.nfontes);
		a2but = Array.newClear(this.nfontes);
		a3but = Array.newClear(this.nfontes);
		a4but = Array.newClear(this.nfontes);
		a5but = Array.newClear(this.nfontes);

		sombuf = Array.newClear(this.nfontes); 
		xval = Array.fill(this.nfontes, 100000); 
		yval = Array.fill(this.nfontes, 100000); 
		zval = Array.fill(this.nfontes, 0); 
		//		xoffset = Array.fill(this.nfontes, 0); 
		//		yoffset = Array.fill(this.nfontes, 0);
		this.synt = Array.newClear(this.nfontes);
		sprite = Array2D.new(this.nfontes, 2);
		funcs = Array.newClear(this.nfontes);
		angle = Array.newClear(this.nfontes); // ângulo dos canais estereofônicos
		zlev = Array.newClear(this.nfontes); 
		level = Array.newClear(this.nfontes); 
		//	doplev = Array.newClear(this.nfontes); 
		glev = Array.newClear(this.nfontes); 
		llev = Array.newClear(this.nfontes); 
		rlev = Array.newClear(this.nfontes); 
		dlev = Array.newClear(this.nfontes); 
		dplev = Array.newClear(this.nfontes); 
		clev = Array.newClear(this.nfontes); 

		this.ncanbox = Array.newClear(this.nfontes); 
		this.businibox = Array.newClear(this.nfontes); 
		this.playingBF = Array.newClear(this.nfontes); 
		
		
		oxbox = Array.newClear(this.nfontes); 
		oybox = Array.newClear(this.nfontes); 
		ozbox = Array.newClear(this.nfontes); 
		xbox = Array.newClear(this.nfontes); 
		zbox = Array.newClear(this.nfontes); 
		ybox = Array.newClear(this.nfontes); 
		lastx = Array.newClear(this.nfontes); 
		lasty = Array.newClear(this.nfontes); 
		lastz = Array.newClear(this.nfontes); 
		abox = Array.newClear(this.nfontes); // ângulo
		vbox = Array.newClear(this.nfontes);  // level
		this.dcheck = Array.newClear(this.nfontes);  // Doppler check
		gbox = Array.newClear(this.nfontes); // reverberação global
		lbox = Array.newClear(this.nfontes); // reverberação local
		rbox = Array.newClear(this.nfontes); // rotação de b-format
		dbox = Array.newClear(this.nfontes); // diretividade de b-format
		cbox = Array.newClear(this.nfontes); // contrair b-format
		dpbox = Array.newClear(this.nfontes); // dop amount
		this.lpcheck = Array.newClear(this.nfontes); // loop
		this.spcheck = Array.newClear(this.nfontes); // spread
		this.dfcheck = Array.newClear(this.nfontes); // diffuse
		this.rvcheck = Array.newClear(this.nfontes); // diffuse reverb
		this.lncheck = Array.newClear(this.nfontes); // linear intensity
		this.hwncheck = Array.newClear(this.nfontes); // hardware-in check
		this.scncheck = Array.newClear(this.nfontes); // SuperCollider-in check
		a1box = Array.newClear(this.nfontes); // aux - array of num boxes in data window
		a2box = Array.newClear(this.nfontes); // aux - array of num boxes in data window
		a3box = Array.newClear(this.nfontes); // aux - array of num boxes in data window
		a4box = Array.newClear(this.nfontes); // aux - array of num boxes in data window
		a5box = Array.newClear(this.nfontes); // aux - array of num boxes in data window

		a1but = Array.newClear(this.nfontes); // aux - array of buttons in data window
		a2but = Array.newClear(this.nfontes); // aux - array of buttons in data window
		a3but = Array.newClear(this.nfontes); // aux - array of buttons in data window
		a4but = Array.newClear(this.nfontes); // aux - array of buttons in data window
		a5but = Array.newClear(this.nfontes); // aux - array of buttons in data window

		a1check = Array.newClear(this.nfontes); // aux - array of buttons in data window
		a2check = Array.newClear(this.nfontes); // aux - array of buttons in data window
		a3check = Array.newClear(this.nfontes); // aux - array of buttons in data window
		a4check = Array.newClear(this.nfontes); // aux - array of buttons in data window
		a5check = Array.newClear(this.nfontes); // aux - array of buttons in data window

		stcheck = Array.newClear(this.nfontes); // aux - array of buttons in data window

		this.firstTime = Array.newClear(this.nfontes);


		this.tfield = Array.newClear(this.nfontes);	
		this.streamdisk = Array.newClear(this.nfontes);	
		
		testado = Array.newClear(this.nfontes);

		//this.offsetHeading(this.offsetheading);
		//("headingoffset variable = " ++ this.offsetheading).postln;


		
		// this regulates file playing synths
		this.watcher = Routine.new({
			inf.do({
				0.1.wait;
				
				this.nfontes.do({
					arg i;
					{
						//("scn = " ++ scn[i]).postln;
						if ((this.tfield[i].value != "") || ((scn[i] > 0) && (this.ncan[i]>0))
							|| (this.hwncheck[i].value && (this.ncan[i]>0)) ) {
							var source = Point.new;  // should use cartesian but it's giving problems
							//source.set(this.xval[i] + this.xoffset[i], this.yval[i] + this.yoffset[i]);
							source.set(this.xval[i], this.yval[i]);
							//("testado = " ++ testado[i]).postln;
							//("distance " ++ i ++ " = " ++ source.rho).postln;
								if (source.rho > 1.2) {
									this.firstTime[i] = true;
									if(this.synt[i].isPlaying) {
										//this.synthRegistry[i].free;
										runStop.value(i); // to kill SC input synths
										this.espacializador[i].free; // just in case...
										this.synt[i].free;
										this.synt[i] = nil;
										this.espacializador[i] = nil;
									};
								} {
									if(this.synt[i].isPlaying.not && (isPlay || testado[i])
										&& (this.firstTime[i] || (this.tfield[i].value == ""))) {
											("HELLO _ Loop is: " ++ lp[i]).postln;
											//this.triggerFunc[i].value; // play SC input synth
											this.firstTime[i] = false;
											runTrigger.value(i);
											
											if(lp[i] == 0) {
												//tocar.value(i, controle.now, force: true);
												tocar.value(i, 1, force: true);
											} {   // could remake this a random start point in future
												tocar.value(i, 1, force: true);
											};
										};
									
								};
							};
					}.defer;
				});
			});
		});
		
		
		this.nfontes.do { arg i;
			doppler[i] = 0;
			angle[i] = 0;
			level[i] = 0;
			glev[i] = 0;
			llev[i] = 0;
			lp[i] = 0;
			sp[i] = 0;
			df[i] = 0;
			rv[i] = 0;
			ln[i] = "";
			hwn[i] = 0;
			scn[i] = 0;
			rlev[i] = 0;
			dlev[i] = 0;
			clev[i] = 0;
			zlev[i] = 0;
			dplev[i] = 0;

			aux1[i] = 0;
			aux2[i] = 0;
			aux3[i] = 0;
			aux4[i] = 0;
			aux5[i] = 0;
			this.streamdisk[i] = false;
			this.ncan[i] = 0;
			this.busini[i] = 0;
			sprite[i, 0] = -20;
			sprite[i, 1] = -20;
			testado[i] = false;
			this.playingBF[i] = false;
			this.firstTime[i] = true;
		};
		
		
		// Note there is an extreme amount repetition occurring here. See the calling function. fix
		novoplot = {
			arg mx, my, i, nfnts; 
			var btest;
			{
				win.drawFunc = {


					Pen.fillColor = Color(0.6,0.8,0.8);
					Pen.addArc(this.halfwidth@this.halfwidth, this.halfwidth, 0, 2pi);
					Pen.fill;
					nfnts.do { arg ind;
						Pen.fillColor = Color(0.8,0.2,0.9);
						Pen.addArc(sprite[ind, 0]@sprite[ind, 1], 20, 0, 2pi);
						Pen.fill;
						(ind + 1).asString.drawCenteredIn(Rect(sprite[ind, 0] - 10, sprite[ind, 1] - 10, 20, 20), 
							Font.default, Color.white);
					};
					Pen.fillColor = Color.gray(0, 0.5);
					Pen.addArc(this.halfwidth@this.halfwidth, 20, 0, 2pi);
					Pen.fill;

					
				}
			}.defer;
			{ win.refresh; }.defer;
			
		};
		
		
		
		gbus = Bus.audio(server, 1); // global reverb bus
		gbfbus = Bus.audio(server, 4); // global b-format bus
		soaBus = Bus.audio(server, 9);
		~t1 = gbus;
		~t2 = gbfbus;
		~t3 = soaBus;
		fonte = Point.new;
		win = Window.new("Mosca", Rect(0, this.width, this.width, this.width)).front;
		wdados = Window.new("Data", Rect(this.width, 0, 955, (this.nfontes*20)+60 ), scroll: true);
		wdados.userCanClose = false;
		wdados.alwaysOnTop = true;
		
		
		bdados = Button(win, Rect(this.width - 100, 10, 90, 20))
		.states_([
			["show data", Color.black, Color.white],
			["hide data", Color.white, Color.blue]
		])
		.action_({ arg but;
			if(but.value == 1)
			{wdados.front;}
			{wdados.visible = false;};
		});

		waux = Window.new("Auxiliary Controllers", Rect(this.width, (this.nfontes*20)+114, 260, 250 ));
		waux.userCanClose = false;
		waux.alwaysOnTop = true;
		
		
		baux = Button(win, Rect(this.width - 100, 30, 90, 20))
		.states_([
			["show aux", Color.black, Color.white],
			["hide aux", Color.white, Color.blue]
		])
		.action_({ arg but;
			if(but.value == 1)
			{waux.front;}
			{waux.visible = false;};
		});

		auxbutton1 = Button(waux, Rect(40, 210, 20, 20))
		.states_([
			["1", Color.black, Color.gray]
		])
		.mouseDownAction_({
			//a = {EnvGen.kr(Env.adsr, doneAction:2) * SinOsc.ar(440, 0, 0.4)}.play;
			a1check[fatual].valueAction = 1;
		})
		.action_({ arg butt, mod;
			//a.release(0.3);
			a1check[fatual].valueAction = 0;
		});

		auxbutton2 = Button(waux, Rect(80, 210, 20, 20))
		.states_([
			["2", Color.black, Color.gray]
		])
		.mouseDownAction_({
			//a = {EnvGen.kr(Env.adsr, doneAction:2) * SinOsc.ar(440, 0, 0.4)}.play;
			a2check[fatual].valueAction = 1;
		})
		.action_({ arg butt, mod;
			//a.release(0.3);
			a2check[fatual].valueAction = 0;
		});
		auxbutton3 = Button(waux, Rect(120, 210, 20, 20))
		.states_([
			["3", Color.black, Color.gray]
		])
		.mouseDownAction_({
			//a = {EnvGen.kr(Env.adsr, doneAction:2) * SinOsc.ar(440, 0, 0.4)}.play;
			a3check[fatual].valueAction = 1;
		})
		.action_({ arg butt, mod;
			//a.release(0.3);
			a3check[fatual].valueAction = 0;
		});
		auxbutton4 = Button(waux, Rect(160, 210, 20, 20))
		.states_([
			["4", Color.black, Color.gray]
		])
		.mouseDownAction_({
			//a = {EnvGen.kr(Env.adsr, doneAction:2) * SinOsc.ar(440, 0, 0.4)}.play;
			a4check[fatual].valueAction = 1;
		})
		.action_({ arg butt, mod;
			//a.release(0.3);
			a4check[fatual].valueAction = 0;
		});
		auxbutton5 = Button(waux, Rect(200, 210, 20, 20))
		.states_([
			["5", Color.black, Color.gray]
		])
		.mouseDownAction_({
			//a = {EnvGen.kr(Env.adsr, doneAction:2) * SinOsc.ar(440, 0, 0.4)}.play;
			a5check[fatual].valueAction = 1;
		})
		.action_({ arg butt, mod;
			//a.release(0.3);
			a5check[fatual].valueAction = 0;
		});


		
		auxslider1 = Slider.new(waux, Rect(40, 20, 20, 160));
		auxslider2 = Slider.new(waux, Rect(80, 20, 20, 160));
		auxslider3 = Slider.new(waux, Rect(120, 20, 20, 160));
		auxslider4 = Slider.new(waux, Rect(160, 20, 20, 160));
		auxslider5 = Slider.new(waux, Rect(200, 20, 20, 160));

		aux1numbox = NumberBox(waux, Rect(30, 185, 40, 20));
		aux2numbox = NumberBox(waux, Rect(70, 185, 40, 20));
		aux3numbox = NumberBox(waux, Rect(110, 185, 40, 20));
		aux4numbox = NumberBox(waux, Rect(150, 185, 40, 20));
		aux5numbox = NumberBox(waux, Rect(190, 185, 40, 20));

		aux1numbox.clipHi = 1;
		aux1numbox.clipLo = 0;
		aux2numbox.clipHi = 1;
		aux2numbox.clipLo = 0;
		aux3numbox.clipHi = 1;
		aux3numbox.clipLo = 0;
		aux4numbox.clipHi = 1;
		aux4numbox.clipLo = 0;
		aux5numbox.clipHi = 1;
		aux5numbox.clipLo = 0;


		aux1numbox.action = {arg num;
			a1box[fatual].valueAction = num.value;
			//this.aux1[fatual] = num.value;
			auxslider1.value = num.value;
		};

		auxslider1.action = {arg num;
			a1box[fatual].valueAction = num.value;
			aux1numbox.value = num.value;
		};
		auxslider2.action = {arg num;
			a2box[fatual].valueAction = num.value;
			aux2numbox.value = num.value;
		};
		auxslider3.action = {arg num;
			a3box[fatual].valueAction = num.value;
			aux3numbox.value = num.value;
		};
		auxslider4.action = {arg num;
			a4box[fatual].valueAction = num.value;
			aux4numbox.value = num.value;
		};
		auxslider5.action = {arg num;
			a5box[fatual].valueAction = num.value;
			aux5numbox.value = num.value;
		};


		
		updateSynthInArgs = { arg source;
			{
				server.sync;
				this.setSynths(source, \dopon, doppler[source]);
				this.setSynths(source, \angle, angle[source]);
				this.setSynths(source, \level, level[source]);
				this.setSynths(source, \dopamnt, dplev[source]);
				this.setSynths(source, \glev, glev[source]);
				this.setSynths(source, \llev, llev[source]);
				this.setSynths(source, \mx, this.xval[source]);
				this.setSynths(source, \my, this.yval[source]);
				this.setSynths(source, \mz, this.zval[source]);
				

				this.setSynths(source, \sp, sp[source]);
				this.setSynths(source, \df, df[source]);

				this.setSynths(source, \rotAngle, rlev[source]);
				this.setSynths(source, \directang, dlev[source]);
				this.setSynths(source, \contr, clev[source]);

				this.setSynths(source, \aux1, aux1[source]);
				this.setSynths(source, \aux2, aux2[source]);
				this.setSynths(source, \aux3, aux3[source]);
				this.setSynths(source, \aux4, aux4[source]);
				this.setSynths(source, \aux5, aux5[source]);

				this.setSynths(source, \a1check, a1but[source]);
				this.setSynths(source, \a2check, a2but[source]);
				this.setSynths(source, \a3check, a3but[source]);
				this.setSynths(source, \a4check, a4but[source]);
				this.setSynths(source, \a5check, a5but[source]);

				
			}.fork;
		};
		
		atualizarvariaveis = {
			
			
			this.nfontes.do { arg i;
				//	updateSynthInArgs.value(i);
				
				if(this.espacializador[i] != nil) {
					this.espacializador[i].set(
						//	\mx, num.value  ???
						\dopon, doppler[i], // not needed...
						\angle, angle[i],
						\level, level[i], // ? or in player?
						\dopamnt, dplev[i],
						\glev, glev[i],
						\llev, llev[i],
						//\mx, xbox[i].value,
						//\my, ybox[i].value,
						//\mz, zbox[i].value,
						\mx, this.xval[i].value,
						\my, this.yval[i].value,
						\mz, this.zval[i].value,
						//\xoffset, this.xoffset[i],
						//\yoffset, this.yoffset[i],
						\sp, sp[i].value,
						\df, df[i].value
					);
				};
				
				if(this.synt[i] != nil) {
					
					this.synt[i].set(
						\level, level[i],
						\rotAngle, rlev[i],
						\directang, dlev[i],
						\contr, clev[i],
						\dopamnt, dplev[i],
						\glev, glev[i],
						\llev, llev[i],
						//\mx, xbox[i].value,
						//\my, ybox[i].value,
						\mz, zbox[i].value,
						\mx, this.xval[i].value,
						\my, this.yval[i].value,
						//\xoffset, this.xoffset[i],
						//\yoffset, this.yoffset[i],
						//\mz, this.zval[i].value,
						\sp, sp[i].value,
						\df, df[i].value
					);
					
					
				};

				
			};
			
			
			
		};


		//source only version (perhaps phase put other

		updatesourcevariables = {
			arg source;				
			if(this.espacializador[source] != nil) {
				this.espacializador[source].set(
					//	\mx, num.value  ???
					\dopon, doppler[source], // not needed...
					\angle, angle[source],
					\level, level[source], // ? or in player?
					\dopamnt, dplev[source],
					\glev, glev[source],
					\llev, llev[source],
					\mx, this.xval[source].value,
					\my, this.yval[source].value,
					\mz, this.zval[source].value,
					\sp, sp[source].value,
					\df, df[source].value
				);
			};
			if(this.synt[source] != nil) {		
				this.synt[source].set(
					\level, level[source],
					\rotAngle, rlev[source],
					\directang, dlev[source],
					\contr, clev[source],
					\dopamnt, dplev[source],
					\glev, glev[source],
					\llev, llev[source],
					\mx, this.xval[source].value,
					\my, this.yval[source].value,
					\mz, this.zval[source].value,
					\sp, sp[source].value,
					\df, df[source].value
				);
			};
		};
		

		
		
		tocar = {
			arg i, tpos, force = false;
			var path = this.tfield[i].value, stdur;

			"TOCAR".postln;
			

			
			if (this.streamdisk[i]) {
				var sf = SoundFile.new;
				var nchan, sframe, srate;
				sf.openRead(path);
				nchan = sf.numChannels;
				srate = sf.sampleRate;
				sframe = tpos * srate;
				stdur = sf.numFrames / srate; // needed?
				sf.close;
				this.streambuf[i] = Buffer.cueSoundFile(server, path, sframe, nchan, 131072);
				//		this.streambuf[i] = srate; //??
				("Creating buffer for source: " ++ i).postln;
			};

			

			

			// Note: ncanais refers to number of channels in the context of
			// files on disk
			// ncan is number of channels for hardware or supercollider input
			// busini is the initial bus used for a particular stream
			// If we have ncan = 4 and busini = 7, the stream will enter
			// in buses 7, 8, 9 and 10.

			if(revGlobalBF.isNil){
				revGlobalBF = Synth.new(\revGlobalBFormatAmb, [\gbfbus, gbfbus],
					addAction:\addToTail);
			};
			if(revGlobal.isNil){
				revGlobal = Synth.new(\revGlobalAmb, [\gbus, gbus], addAction:\addToTail);
			};

			if (this.serport.notNil) {
				if(globFOATransform.isNil){
					this.globFOATransform = Synth.new(\globFOATransformSynth, [\globtbus, this.globTBus,
						\heading, 0, \roll, 0, \pitch, 0], addAction:\addToTail);
				};
			};
			
			/// STREAM FROM DISK

			if ((path != "") && this.hwncheck[i].value.not && this.scncheck[i].value.not && this.streamdisk[i]) {
				var x;
				"Content Streamed from disk".postln;
				if (testado[i].not || force) { // if source is testing don't relaunch synths
					x = case
					{ this.streambuf[i].numChannels == 1} {
						"1 channel".postln;
						{angnumbox.value = 0;}.defer;
						{angslider.value = 0;}.defer;
						cbox[i].value = 1;
						clev[i] = 1;
						if(i == fatual) {
							cslider.value = 1;
							connumbox.value = 1;
						};
						if(rv[i] == 1) {
							if(revGlobalSoa.isNil && this.decoder.isNil && (this.raworder == 2)) {
								revGlobalSoa = Synth.new(\revGlobalSoaA12, [\soaBus, soaBus],
									revGlobalBF, addAction:\addBefore);
							};
							
							if(this.decoder.isNil && (this.raworder == 2)) {
								
								this.synt[i] = Synth.new(\playMonoStream, [\outbus, mbus[i], 
									\bufnum, streambuf[i].bufnum, \rate, 1, \tpos, tpos, \lp, lp[i],
									\level, level[i]], 
									revGlobalSoa, addAction: \addBefore).onFree({this.espacializador[i].free;
										this.espacializador[i] = nil; this.synt[i] = nil;
										this.streambuf[i].free;
									});
								
								
							} {
								this.synt[i] = Synth.new(\playMonoStream, [\outbus, mbus[i], 
									\bufnum, streambuf[i].bufnum, \rate, 1, \tpos, tpos, \lp, lp[i],
									\level, level[i]], 
									revGlobalBF, addAction: \addBefore).onFree({this.espacializador[i].free;
										this.espacializador[i] = nil; this.synt[i] = nil;
										this.streambuf[i].free;
									});															
								
							};
							this.espacializador[i] = Synth.new(\espacAmbAFormatVerb++ln[i], [\inbus, mbus[i], 
								\soaBus, soaBus, \gbfbus, gbfbus,
								\insertFlag, this.insertFlag[i],
								\aFormatBusInFoa, this.aFormatBusFoa[0,i].index,
								\aFormatBusOutFoa, this.aFormatBusFoa[1,i].index,
								\aFormatBusInSoa, this.aFormatBusSoa[0,i].index,
								\aFormatBusOutSoa, this.aFormatBusSoa[1,i].index,
								\dopon, doppler[i]], 
								this.synt[i], addAction: \addAfter);
							
						} { 
							
							this.synt[i] = Synth.new(\playMonoStream, [\outbus, mbus[i], 
								\bufnum, streambuf[i].bufnum, \rate, 1, \tpos, tpos, \lp, lp[i],
								\level, level[i]], revGlobalBF,
								addAction: \addBefore).onFree({this.espacializador[i].free;
									this.espacializador[i] = nil; this.synt[i] = nil;
									this.streambuf[i].free;
								});

							("HERE!!!!!!!!!!!!!!! xval = " ++ this.xval[i] ++ "yval = " ++ this.yval[i]).postln;
							
							this.espacializador[i] = Synth.new(\espacAmbChowning++ln[i], [\inbus, mbus[i], 
								\gbus, gbus, 
								
								\insertFlag, this.insertFlag[i],
								\aFormatBusInFoa, this.aFormatBusFoa[0,i].index,
								\aFormatBusOutFoa, this.aFormatBusFoa[1,i].index,
								\aFormatBusInSoa, this.aFormatBusSoa[0,i].index,
								\aFormatBusOutSoa, this.aFormatBusSoa[1,i].index,
								
								\dopon, doppler[i]], 
								this.synt[i], addAction: \addAfter);
							
							
						};
						//atualizarvariaveis.value;
						updatesourcevariables.value(i);	
					}
					{ this.streambuf[i].numChannels == 2} {
						"2 channel".postln;
						angle[i] = pi/2;
						cbox[i].value = 1;
						clev[i] = 1;
						if(i == fatual) {
							cslider.value = 1;
							connumbox.value = 1;
						};						
						{angnumbox.value = 1.05;}.defer; // 60 degrees
						{angslider.value = 0.33;}.defer;
						if(rv[i] == 1) {
							if(revGlobalSoa.isNil && this.decoder.isNil && (this.raworder == 2)) {
								
								revGlobalSoa = Synth.new(\revGlobalSoaA12, [\soaBus, soaBus],
									revGlobalBF, addAction:\addBefore);
							};
							
							

							if(this.decoder.isNil && (this.raworder == 2)) {
								this.synt[i] = Synth.new(\playStereoStream, [\outbus, sbus[i], 
									\bufnum, streambuf[i].bufnum, \rate, 1, \tpos, tpos, \lp, lp[i],
									\level, level[i]],
									revGlobalSoa, addAction: \addBefore).onFree({this.espacializador[i].free;
										this.espacializador[i] = nil; this.synt[i] = nil;
										this.streambuf[i].free;
									});
							} {
								this.synt[i] = Synth.new(\playStereoStream, [\outbus, sbus[i], 
									\bufnum, streambuf[i].bufnum, \rate, 1, \tpos, tpos, \lp, lp[i],
									\level, level[i]],
									revGlobalBF, addAction: \addBefore).onFree({this.espacializador[i].free;
										this.espacializador[i] = nil; this.synt[i] = nil;
										this.streambuf[i].free;
									});
							};
							
							this.espacializador[i] = Synth.new(\espacAmbEstereoAFormat++ln[i], [\inbus, sbus[i],
								\gbus, gbus, \soaBus, soaBus, \gbfbus, gbfbus,
								\insertFlag, this.insertFlag[i],
								\aFormatBusInFoa, this.aFormatBusFoa[0,i].index,
								\aFormatBusOutFoa, this.aFormatBusFoa[1,i].index,
								\aFormatBusInSoa, this.aFormatBusSoa[0,i].index,
								\aFormatBusOutSoa, this.aFormatBusSoa[1,i].index,
								\dopon, doppler[i]], 
								this.synt[i], addAction: \addAfter);
							


						} {
							if (testado[i].not || force) {
								this.synt[i] = Synth.new(\playStereoStream, [\outbus, sbus[i], 
									\bufnum, streambuf[i].bufnum, \rate, 1, \tpos, tpos, \lp, lp[i],
									\level, level[i]], 
									revGlobalBF, addAction: \addBefore).onFree({this.espacializador[i].free;
										this.espacializador[i] = nil; this.synt[i] = nil;
										this.streambuf[i].free;
									});
								
								this.espacializador[i] = Synth.new(\espacAmbEstereoChowning++ln[i], [\inbus, sbus[i],
									\gbus, gbus,
									\insertFlag, this.insertFlag[i],
									\aFormatBusInFoa, this.aFormatBusFoa[0,i].index,
									\aFormatBusOutFoa, this.aFormatBusFoa[1,i].index,
									\aFormatBusInSoa, this.aFormatBusSoa[0,i].index,
									\aFormatBusOutSoa, this.aFormatBusSoa[1,i].index,
									\dopon, doppler[i]], 
									this.synt[i], addAction: \addAfter);
							};
							
						};
						updatesourcevariables.value(i);	
						
						
						
					}
					{ this.streambuf[i].numChannels == 4} {
						"4 channel".postln;
						this.playingBF[i] = true;
						ncanais[i] = 4;
						angle[i] = 0;
						{angnumbox.value = 0;}.defer;
						cbox[i].value = 0;
						clev[i] = 0;
						if(i == fatual) {
							cslider.value = 0;
							connumbox.value = 0;
						};
						
						{angslider.value = 0;}.defer;

						if(rv[i] == 1) {

							if(revGlobalSoa.isNil && this.decoder.isNil && (this.raworder == 2)) {
								revGlobalSoa = Synth.new(\revGlobalSoaA12, [\soaBus, soaBus],
									revGlobalBF, addAction:\addBefore);
							};
							

							if(this.decoder.isNil && (this.raworder == 2)) {
								this.synt[i] = Synth.new(\playBFormatStream++ln[i], [\gbus, gbus, \gbfbus,
									gbfbus, \outbus,
									mbus[i], \bufnum, streambuf[i].bufnum, \contr, clev[i],
									\rate, 1, \tpos, tpos, \lp,
									lp[i], \level, level[i],
									\insertFlag, this.insertFlag[i],
									\aFormatBusInFoa, this.aFormatBusFoa[0,i].index,
									\aFormatBusOutFoa, this.aFormatBusFoa[1,i].index,
									\aFormatBusInSoa, this.aFormatBusSoa[0,i].index,
									\aFormatBusOutSoa, this.aFormatBusSoa[1,i].index,
									\dopon, doppler[i]], 
									revGlobalSoa, addAction: \addBefore).onFree({this.espacializador[i].free;
										this.espacializador[i] = nil; this.synt[i] = nil;
										this.playingBF[i] = false;
										this.streambuf[i].free;
									});
							} {
								this.synt[i] = Synth.new(\playBFormatStream++ln[i], [\gbus, gbus, \gbfbus,
									gbfbus, \outbus,
									mbus[i], \bufnum, streambuf[i].bufnum, \contr, clev[i],
									\rate, 1, \tpos, tpos, \lp,
									lp[i], \level, level[i],
									\insertFlag, this.insertFlag[i],
									\aFormatBusInFoa, this.aFormatBusFoa[0,i].index,
									\aFormatBusOutFoa, this.aFormatBusFoa[1,i].index,
									\aFormatBusInSoa, this.aFormatBusSoa[0,i].index,
									\aFormatBusOutSoa, this.aFormatBusSoa[1,i].index,
									\dopon, doppler[i]], 
									revGlobalBF, addAction: \addBefore).onFree({this.espacializador[i].free;
										this.espacializador[i] = nil; this.synt[i] = nil;
										this.playingBF[i] = false;
										this.streambuf[i].free;
									});					
							};
							
							this.espacializador[i] = Synth.new(\espacAmb2AFormat++ln[i], [\inbus, mbus[i], 
								\gbus, gbus, \soaBus, soaBus,
								\insertFlag, this.insertFlag[i],
								\aFormatBusInFoa, this.aFormatBusFoa[0,i].index,
								\aFormatBusOutFoa, this.aFormatBusFoa[1,i].index,
								\aFormatBusInSoa, this.aFormatBusSoa[0,i].index,
								\aFormatBusOutSoa, this.aFormatBusSoa[1,i].index,
								\dopon, doppler[i]], 
								this.synt[i], addAction: \addAfter);
							
						} {
							
							
							this.synt[i] = Synth.new(\playBFormatStream++ln[i], [\gbus, gbus, \gbfbus, gbfbus, \outbus,
								mbus[i], \bufnum, streambuf[i].bufnum, \contr, clev[i],
								\rate, 1, \tpos, tpos, \lp,
								lp[i], \level, level[i],
								\insertFlag, this.insertFlag[i],
								\aFormatBusInFoa, this.aFormatBusFoa[0,i].index,
								\aFormatBusOutFoa, this.aFormatBusFoa[1,i].index,
								\aFormatBusInSoa, this.aFormatBusSoa[0,i].index,
								\aFormatBusOutSoa, this.aFormatBusSoa[1,i].index,
								\dopon, doppler[i]], 
								//					~revGlobal, addAction: \addBefore);
								revGlobalBF, addAction: \addBefore).onFree({this.espacializador[i].free;
									this.espacializador[i] = nil; this.synt[i] = nil;
									this.playingBF[i] = false;
									this.streambuf[i].free;
								});
							
							this.espacializador[i] = Synth.new(\espacAmb2Chowning++ln[i],
								[\inbus, mbus[i], \gbus, gbus, 
									\insertFlag, this.insertFlag[i],
									\aFormatBusInFoa, this.aFormatBusFoa[0,i].index,
									\aFormatBusOutFoa, this.aFormatBusFoa[1,i].index,
									\aFormatBusInSoa, this.aFormatBusSoa[0,i].index,
									\aFormatBusOutSoa, this.aFormatBusSoa[1,i].index,
									\dopon, doppler[i]], 
								this.synt[i], addAction: \addAfter);
							
							
							
						};
						updatesourcevariables.value(i);	
					};
					

				};
			};
			/// END STREAM FROM DISK

			// check this logic - what should override what?
			if ((path != "") && (this.hwncheck[i].value.not || this.scncheck[i].value.not) && this.streamdisk[i].not) {
				{	
					
					if (sombuf[i].numChannels == 1)  // arquivo mono
					{ncanais[i] = 1;
						angle[i] = 0;
						{angnumbox.value = 0;}.defer;
						{angslider.value = 0;}.defer;
						cbox[i].value = 1;
						clev[i] = 1;
						if(i == fatual) {
							cslider.value = 1;
							connumbox.value = 1;
						};
						
						if(rv[i] == 1) {
							if(revGlobalSoa.isNil && this.decoder.isNil && (this.raworder == 2)) {
								revGlobalSoa = Synth.new(\revGlobalSoaA12, [\soaBus, soaBus],
									revGlobalBF, addAction:\addBefore);
							};
							if (testado[i].not || force) { // if source is testing don't relaunch synths

								if(this.decoder.isNil && (this.raworder == 2)) {

									this.synt[i] = Synth.new(\playMonoFile, [\outbus, mbus[i], 
										\bufnum, sombuf[i].bufnum, \rate, 1, \tpos, tpos, \lp, lp[i],
										\level, level[i]], 
										revGlobalSoa, addAction: \addBefore).onFree({this.espacializador[i].free;
											this.espacializador[i] = nil; this.synt[i] = nil});
									
									
								} {
									this.synt[i] = Synth.new(\playMonoFile, [\outbus, mbus[i], 
										\bufnum, sombuf[i].bufnum, \rate, 1, \tpos, tpos, \lp, lp[i],
										\level, level[i]], 
										revGlobalBF, addAction: \addBefore).onFree({this.espacializador[i].free;
											this.espacializador[i] = nil; this.synt[i] = nil});															

								};
								this.espacializador[i] = Synth.new(\espacAmbAFormatVerb++ln[i], [\inbus, mbus[i], 
									\soaBus, soaBus, \gbfbus, gbfbus,
									\insertFlag, this.insertFlag[i],
									\aFormatBusInFoa, this.aFormatBusFoa[0,i].index,
									\aFormatBusOutFoa, this.aFormatBusFoa[1,i].index,
									\aFormatBusInSoa, this.aFormatBusSoa[0,i].index,
									\aFormatBusOutSoa, this.aFormatBusSoa[1,i].index,
									\dopon, doppler[i]], 
									this.synt[i], addAction: \addAfter);
							};
						} { 
							if (testado[i].not || force) { // if source is testing don't relaunch synths
								this.synt[i] = Synth.new(\playMonoFile, [\outbus, mbus[i], 
									\bufnum, sombuf[i].bufnum, \rate, 1, \tpos, tpos, \lp, lp[i],
									\level, level[i]], revGlobalBF,
									addAction: \addBefore).onFree({this.espacializador[i].free;
										this.espacializador[i] = nil; this.synt[i] = nil});
								
								this.espacializador[i] = Synth.new(\espacAmbChowning++ln[i], [\inbus, mbus[i], 
									\gbus, gbus, 
									
									\insertFlag, this.insertFlag[i],
									\aFormatBusInFoa, this.aFormatBusFoa[0,i].index,
									\aFormatBusOutFoa, this.aFormatBusFoa[1,i].index,
									\aFormatBusInSoa, this.aFormatBusSoa[0,i].index,
									\aFormatBusOutSoa, this.aFormatBusSoa[1,i].index,
									
									\dopon, doppler[i]], 
									this.synt[i], addAction: \addAfter);
							};

						};
						updatesourcevariables.value(i);	
						
						
					}
					{if (sombuf[i].numChannels == 2) {ncanais[i] = 2; // arquivo estéreo
						angle[i] = pi/2;
						cbox[i].value = 1;
						clev[i] = 1;
						if(i == fatual) {
							cslider.value = 1;
							connumbox.value = 1;
						};

						//						{angnumbox.value = pi/2;}.defer; 
						{angnumbox.value = 1.05;}.defer; // 60 degrees
						//						{angslider.value = 0.5;}.defer;
						{angslider.value = 0.33;}.defer;
						if(rv[i] == 1) {
							if(revGlobalSoa.isNil && this.decoder.isNil && (this.raworder == 2)) {
								
								revGlobalSoa = Synth.new(\revGlobalSoaA12, [\soaBus, soaBus],
									revGlobalBF, addAction:\addBefore);
							};
							
							if (testado[i].not || force) {

								if(this.decoder.isNil && (this.raworder == 2)) {
									this.synt[i] = Synth.new(\playStereoFile, [\outbus, sbus[i], 
										\bufnum, sombuf[i].bufnum, \rate, 1, \tpos, tpos, \lp, lp[i],
										\level, level[i]],
										revGlobalSoa, addAction: \addBefore).onFree({this.espacializador[i].free;
											this.espacializador[i] = nil; this.synt[i] = nil});
								} {
									this.synt[i] = Synth.new(\playStereoFile, [\outbus, sbus[i], 
										\bufnum, sombuf[i].bufnum, \rate, 1, \tpos, tpos, \lp, lp[i],
										\level, level[i]],
										revGlobalBF, addAction: \addBefore).onFree({this.espacializador[i].free;
											this.espacializador[i] = nil; this.synt[i] = nil});
								};
								
								this.espacializador[i] = Synth.new(\espacAmbEstereoAFormat++ln[i], [\inbus, sbus[i],
									\gbus, gbus, \soaBus, soaBus, \gbfbus, gbfbus,
									\insertFlag, this.insertFlag[i],
									\aFormatBusInFoa, this.aFormatBusFoa[0,i].index,
									\aFormatBusOutFoa, this.aFormatBusFoa[1,i].index,
									\aFormatBusInSoa, this.aFormatBusSoa[0,i].index,
									\aFormatBusOutSoa, this.aFormatBusSoa[1,i].index,
									\dopon, doppler[i]], 
									this.synt[i], addAction: \addAfter);
							};


						} {
							if (testado[i].not || force) {
								this.synt[i] = Synth.new(\playStereoFile, [\outbus, sbus[i], 
									\bufnum, sombuf[i].bufnum, \rate, 1, \tpos, tpos, \lp, lp[i],
									\level, level[i]], 
									revGlobalBF, addAction: \addBefore).onFree({this.espacializador[i].free;
										this.espacializador[i] = nil; this.synt[i] = nil});
								
								this.espacializador[i] = Synth.new(\espacAmbEstereoChowning++ln[i], [\inbus, sbus[i],
									\gbus, gbus,
									\insertFlag, this.insertFlag[i],
									\aFormatBusInFoa, this.aFormatBusFoa[0,i].index,
									\aFormatBusOutFoa, this.aFormatBusFoa[1,i].index,
									\aFormatBusInSoa, this.aFormatBusSoa[0,i].index,
									\aFormatBusOutSoa, this.aFormatBusSoa[1,i].index,
									\dopon, doppler[i]], 
									this.synt[i], addAction: \addAfter);
							};
							
						};
						//atualizarvariaveis.value;
						updatesourcevariables.value(i);	
						
						//	~revGlobal = Synth.new(\revGlobalAmb, [\gbus, gbus], addAction:\addToTail);



						
					} {
						if (sombuf[i].numChannels == 4) {
							this.playingBF[i] = true;
							ncanais[i] = 4;
							angle[i] = 0;
							{angnumbox.value = 0;}.defer;
							cbox[i].value = 0;
							clev[i] = 0;
							if(i == fatual) {
								cslider.value = 0;
								connumbox.value = 0;
							};

							{angslider.value = 0;}.defer;
							
							// reverb for non-contracted (full b-format) component

							// reverb for contracted (mono) component - and for rest too
							if(rv[i] == 1) {

								if(revGlobalSoa.isNil && this.decoder.isNil && (this.raworder == 2)) {
									revGlobalSoa = Synth.new(\revGlobalSoaA12, [\soaBus, soaBus],
										revGlobalBF, addAction:\addBefore);
								};
								

								if (testado[i].not || force) {

									if(this.decoder.isNil && (this.raworder == 2)) {
										this.synt[i] = Synth.new(\playBFormatFile++ln[i], [\gbus, gbus, \gbfbus,
											gbfbus, \outbus,
											mbus[i], \bufnum, sombuf[i].bufnum, \contr, clev[i],
											\rate, 1, \tpos, tpos, \lp,
											lp[i], \level, level[i],
											\insertFlag, this.insertFlag[i],
											\aFormatBusInFoa, this.aFormatBusFoa[0,i].index,
											\aFormatBusOutFoa, this.aFormatBusFoa[1,i].index,
											\aFormatBusInSoa, this.aFormatBusSoa[0,i].index,
											\aFormatBusOutSoa, this.aFormatBusSoa[1,i].index,
											\dopon, doppler[i]], 
											revGlobalSoa, addAction: \addBefore).onFree({this.espacializador[i].free;
												this.espacializador[i] = nil; this.synt[i] = nil;
												this.playingBF[i] = false});
									} {
										this.synt[i] = Synth.new(\playBFormatFile++ln[i], [\gbus, gbus, \gbfbus,
											gbfbus, \outbus,
											mbus[i], \bufnum, sombuf[i].bufnum, \contr, clev[i],
											\rate, 1, \tpos, tpos, \lp,
											lp[i], \level, level[i],
											\insertFlag, this.insertFlag[i],
											\aFormatBusInFoa, this.aFormatBusFoa[0,i].index,
											\aFormatBusOutFoa, this.aFormatBusFoa[1,i].index,
											\aFormatBusInSoa, this.aFormatBusSoa[0,i].index,
											\aFormatBusOutSoa, this.aFormatBusSoa[1,i].index,
											\dopon, doppler[i]], 
											revGlobalBF, addAction: \addBefore).onFree({this.espacializador[i].free;
												this.espacializador[i] = nil; this.synt[i] = nil;
												this.playingBF[i] = false});					
									};
									
									this.espacializador[i] = Synth.new(\espacAmb2AFormat++ln[i], [\inbus, mbus[i], 
										\gbus, gbus, \soaBus, soaBus,
										\insertFlag, this.insertFlag[i],
										\aFormatBusInFoa, this.aFormatBusFoa[0,i].index,
										\aFormatBusOutFoa, this.aFormatBusFoa[1,i].index,
										\aFormatBusInSoa, this.aFormatBusSoa[0,i].index,
										\aFormatBusOutSoa, this.aFormatBusSoa[1,i].index,
										\dopon, doppler[i]], 
										this.synt[i], addAction: \addAfter);
								};
							} {
								if (testado[i].not || force) {

									
									this.synt[i] = Synth.new(\playBFormatFile++ln[i], [\gbus, gbus, \gbfbus, gbfbus, \outbus,
										mbus[i], \bufnum, sombuf[i].bufnum, \contr, clev[i],
										\rate, 1, \tpos, tpos, \lp,
										lp[i], \level, level[i],
										\insertFlag, this.insertFlag[i],
										\aFormatBusInFoa, this.aFormatBusFoa[0,i].index,
										\aFormatBusOutFoa, this.aFormatBusFoa[1,i].index,
										\aFormatBusInSoa, this.aFormatBusSoa[0,i].index,
										\aFormatBusOutSoa, this.aFormatBusSoa[1,i].index,
										\dopon, doppler[i]], 
										//					~revGlobal, addAction: \addBefore);
										revGlobalBF, addAction: \addBefore).onFree({this.espacializador[i].free;
											this.espacializador[i] = nil; this.synt[i] = nil;
											this.playingBF[i] = false});
									
									this.espacializador[i] = Synth.new(\espacAmb2Chowning++ln[i],
										[\inbus, mbus[i], \gbus, gbus, 
											\insertFlag, this.insertFlag[i],
											\aFormatBusInFoa, this.aFormatBusFoa[0,i].index,
											\aFormatBusOutFoa, this.aFormatBusFoa[1,i].index,
											\aFormatBusInSoa, this.aFormatBusSoa[0,i].index,
											\aFormatBusOutSoa, this.aFormatBusSoa[1,i].index,
											\dopon, doppler[i]], 
										this.synt[i], addAction: \addAfter);
								};
								

							};
							updatesourcevariables.value(i);	
							

							
							
						}
						{ncanais[i] = 0; // outro tipo de arquivo, faz nada.
						};
					};  }; 
					if(controle.doRecord == false){
						{	xbox[i].valueAction = xbox[i].value;
							ybox[i].valueAction = ybox[i].value;
						}.defer;
					};
					
					
					//	}); 
				}.defer;	
			} {
				
				if ((this.scncheck[i].value) || (this.hwncheck[i])) {
					var x;
					x = case
					{ this.ncan[i] == 1 } {
						
						cbox[i].value = 1;
						clev[i] = 1;
						if(i == fatual) {
							cslider.value = 1;
							connumbox.value = 1;
						};

						if(rv[i] == 1) {
							if(revGlobalSoa.isNil && this.decoder.isNil && (this.raworder == 2)) {
								revGlobalSoa = Synth.new(\revGlobalSoaA12, [\soaBus, soaBus],
									revGlobalBF, addAction:\addBefore);
							};
							
							if (testado[i].not || force) {
								if (this.hwncheck[i].value) {
									if(this.decoder.isNil && (this.raworder == 2)) {
										this.synt[i] = Synth.new(\playMonoHWBus, [\outbus, mbus[i], \busini,
											this.busini[i],
											\level, level[i]], revGlobalSoa,
											addAction: \addBefore).onFree({this.espacializador[i].free;
												this.espacializador[i] = nil; this.synt[i] = nil});
									} {
										this.synt[i] = Synth.new(\playMonoHWBus, [\outbus, mbus[i], \busini,
											this.busini[i],
											\level, level[i]], revGlobalBF,
											addAction: \addBefore).onFree({this.espacializador[i].free;
												this.espacializador[i] = nil; this.synt[i] = nil});
									};
								} {
									if(this.decoder.isNil && (this.raworder == 2)) {
										this.synt[i] = Synth.new(\playMonoSWBus, [\outbus, mbus[i],
											\busini, this.scInBus[i], // use "index" method?
											\level, level[i]], revGlobalSoa,
											addAction: \addBefore).onFree({this.espacializador[i].free;
												this.espacializador[i] = nil; this.synt[i] = nil});
									} {
										this.synt[i] = Synth.new(\playMonoSWBus, [\outbus, mbus[i],
											\busini, this.scInBus[i], // use "index" method?
											\level, level[i]], revGlobalBF,
											addAction: \addBefore).onFree({this.espacializador[i].free;
												this.espacializador[i] = nil; this.synt[i] = nil});
									};
								};
								
								
								this.espacializador[i] = Synth.new(\espacAmbAFormatVerb++ln[i], [\inbus, mbus[i], 
									\soaBus, soaBus, \gbfbus, gbfbus,
									\insertFlag, this.insertFlag[i],
									\aFormatBusInFoa, this.aFormatBusFoa[0,i].index,
									\aFormatBusOutFoa, this.aFormatBusFoa[1,i].index,
									\aFormatBusInSoa, this.aFormatBusSoa[0,i].index,
									\aFormatBusOutSoa, this.aFormatBusSoa[1,i].index,
									\dopon, doppler[i]], 
									this.synt[i], addAction: \addAfter);
							};
							
						} {
							if (testado[i].not || force) {
								if (this.hwncheck[i].value) {
									this.synt[i] = Synth.new(\playMonoHWBus, [\outbus, mbus[i], \busini, this.busini[i],
										\level, level[i]], revGlobalBF,
										addAction: \addBefore).onFree({this.espacializador[i].free;
											this.espacializador[i] = nil; this.synt[i] = nil});
								} {
									this.synt[i] = Synth.new(\playMonoSWBus, [\outbus, mbus[i],
										\busini, this.scInBus[i], // use "index" method?
										\level, level[i]], revGlobalBF,
										addAction: \addBefore).onFree({this.espacializador[i].free;
											this.espacializador[i] = nil; this.synt[i] = nil});
								};
								
								
								this.espacializador[i] = Synth.new(\espacAmbChowning++ln[i], [\inbus, mbus[i], 
									\gbus, gbus,
									\insertFlag, this.insertFlag[i],
									\aFormatBusInFoa, this.aFormatBusFoa[0,i].index,
									\aFormatBusOutFoa, this.aFormatBusFoa[1,i].index,
									\aFormatBusInSoa, this.aFormatBusSoa[0,i].index,
									\aFormatBusOutSoa, this.aFormatBusSoa[1,i].index,
									\dopon, doppler[i]], 
									this.synt[i], addAction: \addAfter);
							};

						};
						
						//atualizarvariaveis.value;
						updatesourcevariables.value(i);	
						
						



						
					}
					{ this.ncan[i] == 2 } {
						ncanais[i] = 0; // just in case!
						angle[i] = pi/2;
						{angnumbox.value = pi/2;}.defer;
						{angslider.value = 0.5;}.defer;
						
						cbox[i].value = 1;
						clev[i] = 1;
						if(i == fatual) {
							cslider.value = 1;
							connumbox.value = 1;
						};

						if(rv[i] == 1) {	


							if (testado[i].not || force) {

								if(revGlobalSoa.isNil && this.decoder.isNil && (this.raworder == 2)) {
									revGlobalSoa = Synth.new(\revGlobalSoaA12, [\soaBus, soaBus],
										revGlobalBF, addAction:\addBefore);
								};
								if (this.hwncheck[i].value) {

									if(this.decoder.isNil && (this.raworder == 2)){
										this.synt[i] = Synth.new(\playStereoHWBus, [\outbus, sbus[i], \busini,
											this.busini[i],
											\level, level[i]], revGlobalSoa,
											addAction: \addBefore).onFree({this.espacializador[i].free;
												this.espacializador[i] = nil; this.synt[i] = nil});
									} {
										synt[i] = Synth.new(\playStereoHWBus, [\outbus, sbus[i], \busini,
											this.busini[i],
											\level, level[i]], revGlobalBF,
											addAction: \addBefore).onFree({this.espacializador[i].free;
												this.espacializador[i] = nil; this.synt[i] = nil});							
									};
								} {
									if(this.decoder.isNil && (this.raworder == 2)){
										this.synt[i] = Synth.new(\playStereoSWBus, [\outbus, sbus[i],
											\busini, this.scInBus[i],
											\level, level[i]], revGlobalSoa,
											addAction: \addBefore).onFree({this.espacializador[i].free;
												this.espacializador[i] = nil; this.synt[i] = nil});
									} {
										synt[i] = Synth.new(\playStereoSWBus, [\outbus, sbus[i],
											\busini, this.scInBus[i],
											\level, level[i]], revGlobalBF,
											addAction: \addBefore).onFree({this.espacializador[i].free;
												this.espacializador[i] = nil; this.synt[i] = nil});										
									};
								};
								
								this.espacializador[i] = Synth.new(\espacAmbEstereoAFormat++ln[i], [\inbus, sbus[i], \gbus, gbus,
									\soaBus, soaBus, \gbfbus, gbfbus,
									\insertFlag, this.insertFlag[i],
									\aFormatBusInFoa, this.aFormatBusFoa[0,i].index,
									\aFormatBusOutFoa, this.aFormatBusFoa[1,i].index,
									\aFormatBusInSoa, this.aFormatBusSoa[0,i].index,
									\aFormatBusOutSoa, this.aFormatBusSoa[1,i].index,
									\dopon, doppler[i]], 
									this.synt[i], addAction: \addAfter);
							};


						} {
							if (this.hwncheck[i].value) {
								this.synt[i] = Synth.new(\playStereoHWBus, [\outbus, sbus[i], \busini, this.busini[i],
									\level, level[i]], revGlobalBF,
									addAction: \addBefore).onFree({this.espacializador[i].free;
										this.espacializador[i] = nil; this.synt[i] = nil});
							} {
								this.synt[i] = Synth.new(\playStereoSWBus, [\outbus, sbus[i],
									\busini, this.scInBus[i],
									\level, level[i]], revGlobalBF,
									addAction: \addBefore).onFree({this.espacializador[i].free;
										this.espacializador[i] = nil; this.synt[i] = nil});
							};
							
							this.espacializador[i] = Synth.new(\espacAmbEstereoChowning++ln[i], [\inbus, sbus[i],
								\gbus, gbus,
								\insertFlag, this.insertFlag[i],
								\aFormatBusInFoa, this.aFormatBusFoa[0,i].index,
								\aFormatBusOutFoa, this.aFormatBusFoa[1,i].index,
								\aFormatBusInSoa, this.aFormatBusSoa[0,i].index,
								\aFormatBusOutSoa, this.aFormatBusSoa[1,i].index,
								\dopon, doppler[i]], 
								this.synt[i], addAction: \addAfter);


						};
						//atualizarvariaveis.value;
						updatesourcevariables.value(i);	
						
						



						
					}
					{ this.ncan[i] == 4 } {
						
						cbox[i].value = 0;
						clev[i] = 0;
						if(i == fatual) {
							cslider.value = 0;
							connumbox.value = 0;
						};

						if(rv[i] == 1) {

							if(revGlobalSoa == nil && this.decoder.isNil && (this.raworder == 2)) {
								revGlobalSoa = Synth.new(\revGlobalSoaA12, [\soaBus, soaBus],
									revGlobalBF, addAction:\addBefore);
							};
							
							if (testado[i].not || force) {
								if(this.decoder.isNil && (this.raworder == 2)) {
									if (this.hwncheck[i].value) {
										this.synt[i] = Synth.new(\playBFormatHWBus++ln[i], [\gbfbus, gbfbus,
											\outbus, mbus[i],
											\contr, clev[i], \rate, 1, \tpos, tpos, \level, level[i],
											\dopon, doppler[i],
											\insertFlag, this.insertFlag[i],
											\aFormatBusInFoa, this.aFormatBusFoa[0,i].index,
											\aFormatBusOutFoa, this.aFormatBusFoa[1,i].index,
											\aFormatBusInSoa, this.aFormatBusSoa[0,i].index,
											\aFormatBusOutSoa, this.aFormatBusSoa[1,i].index,
											\busini, this.busini[i]], 
											revGlobalSoa, addAction: \addBefore).onFree({this.espacializador[i].free;
												this.espacializador[i] = nil; this.synt[i] = nil;});
									} {
										this.synt[i] = Synth.new(\playBFormatSWBus++ln[i], [\gbfbus, gbfbus, \outbus,
											mbus[i], \contr, clev[i], \rate, 1, \tpos, tpos, \level,
											level[i],
											\insertFlag, this.insertFlag[i],
											\aFormatBusInFoa, this.aFormatBusFoa[0,i].index,
											\aFormatBusOutFoa, this.aFormatBusFoa[1,i].index,
											\aFormatBusInSoa, this.aFormatBusSoa[0,i].index,
											\aFormatBusOutSoa, this.aFormatBusSoa[1,i].index,
											\dopon, doppler[i],
											\busini, this.scInBus[i] ], 
											revGlobalSoa, addAction: \addBefore).onFree({this.espacializador[i].free;
												this.espacializador[i] = nil; this.synt[i] = nil;});
									};
								} {
									if (this.hwncheck[i].value) {
										this.synt[i] = Synth.new(\playBFormatHWBus++ln[i], [\gbfbus, gbfbus,
											\outbus, mbus[i],
											\contr, clev[i], \rate, 1, \tpos, tpos, \level, level[i],
											\insertFlag, this.insertFlag[i],
											\aFormatBusInFoa, this.aFormatBusFoa[0,i].index,
											\aFormatBusOutFoa, this.aFormatBusFoa[1,i].index,
											\aFormatBusInSoa, this.aFormatBusSoa[0,i].index,
											\aFormatBusOutSoa, this.aFormatBusSoa[1,i].index,
											\dopon, doppler[i],
											\busini, this.busini[i]], 
											revGlobalBF, addAction: \addBefore).onFree({this.espacializador[i].free;
												this.espacializador[i] = nil; this.synt[i] = nil;});
									} {
										this.synt[i] = Synth.new(\playBFormatSWBus++ln[i], [\gbfbus, gbfbus, \outbus,
											mbus[i], \contr, clev[i], \rate, 1, \tpos, tpos, \level,
											level[i], \dopon, doppler[i],
											\busini, this.scInBus[i] ], 
											revGlobalBF, addAction: \addBefore).onFree({this.espacializador[i].free;
												this.espacializador[i] = nil; this.synt[i] = nil;});
									};
									
								};
								this.espacializador[i] = Synth.new(\espacAmb2AFormat++ln[i], [\inbus, mbus[i],
									\gbus, gbus, \soaBus, soaBus,
									\insertFlag, this.insertFlag[i],
									\aFormatBusInFoa, this.aFormatBusFoa[0,i].index,
									\aFormatBusOutFoa, this.aFormatBusFoa[1,i].index,
									\aFormatBusInSoa, this.aFormatBusSoa[0,i].index,
									\aFormatBusOutSoa, this.aFormatBusSoa[1,i].index,
									\dopon, doppler[i]], 
									this.synt[i], addAction: \addAfter);
							};

						} {

							if (testado[i].not || force) {
								if (this.hwncheck[i].value) {
									this.synt[i] = Synth.new(\playBFormatHWBus++ln[i], [\gbfbus, gbfbus, \outbus,
										mbus[i], \contr, clev[i], \rate, 1, \tpos, tpos, \level,
										level[i], \dopon, doppler[i],
										\insertFlag, this.insertFlag[i],
										\aFormatBusInFoa, this.aFormatBusFoa[0,i].index,
										\aFormatBusOutFoa, this.aFormatBusFoa[1,i].index,
										\aFormatBusInSoa, this.aFormatBusSoa[0,i].index,
										\aFormatBusOutSoa, this.aFormatBusSoa[1,i].index,
										\busini, this.busini[i]], 
										revGlobalBF, addAction: \addBefore).onFree({this.espacializador[i].free;
											this.espacializador[i] = nil; this.synt[i] = nil;
											this.playingBF[i] = false});
								} {
									this.synt[i] = Synth.new(\playBFormatSWBus++ln[i], [\gbfbus, gbfbus, \outbus,
										mbus[i], \contr, clev[i], \rate, 1, \tpos, tpos, \level,
										level[i], \dopon, doppler[i],
										\insertFlag, this.insertFlag[i],
										\aFormatBusInFoa, this.aFormatBusFoa[0,i].index,
										\aFormatBusOutFoa, this.aFormatBusFoa[1,i].index,
										\aFormatBusInSoa, this.aFormatBusSoa[0,i].index,
										\aFormatBusOutSoa, this.aFormatBusSoa[1,i].index,
										\busini, this.scInBus[i] ], 
										revGlobalBF, addAction: \addBefore).onFree({this.espacializador[i].free;
											this.espacializador[i] = nil; this.synt[i] = nil;});
								};
								
								this.espacializador[i] = Synth.new(\espacAmb2Chowning++ln[i], [\inbus, mbus[i],
									\gbus, gbus,
									\insertFlag, this.insertFlag[i],
									\aFormatBusInFoa, this.aFormatBusFoa[0,i].index,
									\aFormatBusOutFoa, this.aFormatBusFoa[1,i].index,
									\aFormatBusInSoa, this.aFormatBusSoa[0,i].index,
									\aFormatBusOutSoa, this.aFormatBusSoa[1,i].index,
									\dopon, doppler[i]], 
									this.synt[i], addAction: \addAfter);
							};
							
						};
						

						

						
						//atualizarvariaveis.value;
						updatesourcevariables.value(i);	
						
						




						
					};
					
					
				};
				
				
				
			};
			
		};
		
		
		
		btestar = Button(win, Rect(this.width - 100, 50, 90, 20))
		.states_([
			["audition", Color.black, Color.white],
			["stop", Color.white, Color.red]
		])
		.action_({ arg but;
			{ if(isPlay.not) {
				if(but.value == 1)
				{
					this.firstTime[fatual] = true;
					                  //testado[fatual] = true;
					//runTrigger.value(fatual); - watcher does this now
					//tocar.value(fatual, 0); // needed only by SC input
					//- and probably by HW - causes duplicates with file
					// as file playback is handled by the "watcher" routine
                                      "really curious!".postln;
					testado[fatual] = true;
					
				}
				{
					
					//testado[fatual] = false;
					runStop.value(fatual);
					this.synt[fatual].free;
					this.synt[fatual] = nil;
					testado[fatual] = false;
					"stopping!".postln;
					
				};
			} {
				but.value = 0;
			}
			}.defer;
			
		});


		//brecaudio = Button(win, Rect(this.width - 190, 70, 90, 20))
		Button(win, Rect(this.width - 190, 70, 90, 20))
		.states_([
			["record audio", Color.red, Color.white],
			["stop", Color.white, Color.red]
		])
		.action_({ arg but;
			if(but.value == 1)
			{
				
				//("Recording stereo. chans = " ++ chans ++ " bus = " ++ bus).postln;
				prjDr.postln;
				if(blipcheck.value)
				{
					this.blips;
				};
				server.recChannels = this.recchans;
				// note the 2nd bus argument only works in SC 3.9
				server.record((prjDr ++ "/out.wav").standardizePath, this.recbus);
				
			}
			{
				server.stopRecording;
				"Recording stopped".postln;
			};		
			
		});
		blipcheck = CheckBox( win, Rect(this.width - 95, 63, 60, 40), "blips").action_({ arg butt;
			if(butt.value) {
				//"Looping transport".postln;
				//this.autoloopval = true;
			} {
				//		this.autoloopval = false;			
			};
			
		});

		~win = win;
		
		// save automation - adapted from chooseDirectoryDialog in AutomationGui.sc
		
		bsalvar = Button(win, Rect(10, this.width - 40, 80, 20))
		.states_([
			["save auto", Color.black, Color.white],
			
		])
		.action_({
			//arg but;
			var filenames;
			//			var arquivo = File((prjDr ++ "/auto/arquivos.txt").standardizePath,"w");
			var title="Save: select automation dir", onSuccess, onFailure=nil,
			preset=nil, bounds,  dwin, textField, success=false;
			

			////// Changes

			/*	var dopplerf = File((prjDr ++ "/auto/doppler.txt").standardizePath,"w");
			var loopedf = File((prjDr ++ "/auto/looped.txt").standardizePath,"w");
			var aformatrevf = File((prjDr ++ "/auto/aformatrev.txt").standardizePath,"w");
			var hwinf = File((prjDr ++ "/auto/hwin.txt").standardizePath,"w");
			var scinf = File((prjDr ++ "/auto/scin.txt").standardizePath,"w");
			var linearf = File((prjDr ++ "/auto/linear.txt").standardizePath,"w");
			var spreadf = File((prjDr ++ "/auto/spread.txt").standardizePath,"w");
			var diffusef = File((prjDr ++ "/auto/diffuse.txt").standardizePath,"w");
			*/
			var dopplerf, loopedf, aformatrevf, hwinf, scinf, linearf, spreadf, diffusef, ncanf, businif;

			//////////////


			bounds = Rect(100,300,300,30);
			
			if(prjDr.isNil && this.lastAutomation.isNil) {
				preset = "HOME".getenv ++ "/auto/"; } {
					if (this.lastAutomation.isNil) {
						preset = prjDr ++ "/auto/";
					} {
						preset = this.lastAutomation;
					};
				};
			dwin = GUI.window.new(title, bounds);
            dwin.onClose = {
                if (success.not){
                    onFailure.value(textField.value);
					"Aborted save".postln;
                };
            };
            textField = GUI.textField.new(dwin, Rect(0,0,bounds.width,bounds.height));
            textField.value = preset;
            textField.action = {
                success = true;
                onSuccess.value(textField.value);
                dwin.close;
				
				("FILE IS " ++ textField.value ++ "/filenames.txt").postln;
				("mkdir -p" + textField.value).systemCmd;
				filenames = File((textField.value ++ "/filenames.txt").standardizePath,"w");

				dopplerf = File((textField.value ++ "/doppler.txt").standardizePath,"w");
				loopedf = File((textField.value ++ "/looped.txt").standardizePath,"w");
				aformatrevf = File((textField.value ++ "/aformatrev.txt").standardizePath,"w");
				hwinf = File((textField.value ++ "/hwin.txt").standardizePath,"w");
				scinf = File((textField.value ++ "/scin.txt").standardizePath,"w");
				linearf = File((textField.value ++ "/linear.txt").standardizePath,"w");
				spreadf = File((textField.value ++ "/spread.txt").standardizePath,"w");
				diffusef = File((textField.value ++ "/diffuse.txt").standardizePath,"w");
				ncanf = File((textField.value ++ "/ncan.txt").standardizePath,"w");
				businif = File((textField.value ++ "/busini.txt").standardizePath,"w");


				nfontes.do { arg i;
					if(this.tfield[i].value != "") {
						filenames.write(this.tfield[i].value ++ "\n")
					} {
						filenames.write("NULL\n")
					};


					dopplerf.write(this.dcheck[i].value.asString ++ "\n");
					loopedf.write(this.lpcheck[i].value.asString ++ "\n");
					aformatrevf.write(this.rvcheck[i].value.asString ++ "\n");
					hwinf.write(this.hwncheck[i].value.asString ++ "\n");
					scinf.write(this.scncheck[i].value.asString ++ "\n");
					linearf.write(this.lncheck[i].value.asString ++ "\n");
					spreadf.write(this.spcheck[i].value.asString ++ "\n");
					diffusef.write(this.dfcheck[i].value.asString ++ "\n");
					ncanf.write(this.ncanbox[i].value.asString ++ "\n");
					businif.write(this.businibox[i].value.asString ++ "\n");
					
					// REMEMBER AUX? 
					
				};
				filenames.close;

				////////

				dopplerf.close;
				loopedf.close;
				aformatrevf.close;
				hwinf.close;
				scinf.close;
				linearf.close;
				spreadf.close;
				diffusef.close;
				ncanf.close;
				businif.close;
				
				///////

				
				controle.save(textField.value);
				this.lastAutomation = textField.value;

            };
            dwin.front;

			
			
		});
		
		
		// load automation - adapted from chooseDirectoryDialog in AutomationGui.sc
		
		bcarregar = Button(win, Rect(90, this.width - 40, 80, 20))
		.states_([
			["load auto", Color.black, Color.white],
		])
		.action_({
			var filenames;
			var title="Select Automation directory", onSuccess, onFailure=nil,
			preset=nil, bounds,  dwin, textField, success=false;
			var dopplerf, loopedf, aformatrevf, hwinf, scinf, linearf, spreadf, diffusef, ncanf, businif;

			bounds = Rect(100,300,300,30);
			if(prjDr.isNil && this.lastAutomation.isNil) {
				preset = "HOME".getenv ++ "/auto/"; } {
					if(this.lastAutomation.isNil) {
						preset = prjDr ++ "/auto/";
					} {
						preset = this.lastAutomation;
					};
				};
			dwin = GUI.window.new(title, bounds);
            dwin.onClose = {
                if (success.not){
                    onFailure.value(textField.value);
					"Aborted load".postln;
                };
            };
            textField = GUI.textField.new(dwin, Rect(0,0,bounds.width,bounds.height));
            textField.value = preset;
            textField.action = {
                success = true;
                onSuccess.value(textField.value);
                dwin.close;
				controle.load(textField.value);
				//controle.seek;
				this.lastAutomation = textField.value;
				filenames = File((textField.value ++ "/filenames.txt").standardizePath,"r");

				dopplerf = File((textField.value ++ "/doppler.txt").standardizePath,"r");
				loopedf = File((textField.value ++ "/looped.txt").standardizePath,"r");
				aformatrevf = File((textField.value ++ "/aformatrev.txt").standardizePath,"r");
				hwinf = File((textField.value ++ "/hwin.txt").standardizePath,"r");
				scinf = File((textField.value ++ "/scin.txt").standardizePath,"r");
				linearf = File((textField.value ++ "/linear.txt").standardizePath,"r");
				spreadf = File((textField.value ++ "/spread.txt").standardizePath,"r");
				diffusef = File((textField.value ++ "/diffuse.txt").standardizePath,"r");
				ncanf = File((textField.value ++ "/ncan.txt").standardizePath,"r");
				businif = File((textField.value ++ "/busini.txt").standardizePath,"r");

				
				{	("BEFORE ACTION - stream = " ++ stcheck[0].value).postln;}.defer;
				
				
				nfontes.do { arg i;
					var line = filenames.getLine(1024);
					if(line!="NULL"){this.tfield[i].value = line};
				};
				nfontes.do { arg i;
					var line = dopplerf.getLine(1024);
					this.dcheck[i].valueAction = line;
				};

				nfontes.do { arg i;
					var line = loopedf.getLine(1024);
					this.lpcheck[i].valueAction = line;
				};
				nfontes.do { arg i;
					var line = aformatrevf.getLine(1024);
					this.rvcheck[i].valueAction = line;
				};
				nfontes.do { arg i;
					var line = hwinf.getLine(1024);
					this.hwncheck[i].valueAction = line;
				};
				nfontes.do { arg i;
					var line = scinf.getLine(1024);
					this.scncheck[i].valueAction = line;
				};
				nfontes.do { arg i;
					var line = linearf.getLine(1024);
					this.lncheck[i].valueAction = line;
				};
				nfontes.do { arg i;
					var line = spreadf.getLine(1024);
					this.spcheck[i].valueAction = line;
				};
				nfontes.do { arg i;
					var line = diffusef.getLine(1024);
					this.dfcheck[i].valueAction = line;
				};
				nfontes.do { arg i;
					var line = diffusef.getLine(1024);
					this.dfcheck[i].valueAction = line;
				};
				nfontes.do { arg i;
					var line = ncanf.getLine(1024);
					this.ncanbox[i].valueAction = line.asFloat;
				};
				nfontes.do { arg i;
					var line = businif.getLine(1024);
					this.businibox[i].valueAction = line.asFloat;
				};


				
				filenames.close;

				dopplerf.close;
				loopedf.close;
				aformatrevf.close;
				hwinf.close;
				scinf.close;
				linearf.close;
				spreadf.close;
				diffusef.close;
				ncanf.close;
				businif.close;

				
				
				// delay necessary here because streamdisks take some time to register after controle.load
				Routine {
					{
					}.defer;
					1.wait;
					nfontes.do { arg i;
						{
							var path = this.tfield[i].value;
							if (this.streamdisk[i].not && (this.tfield[i].value != "")) {
								i.postln;
								path.postln;
								sombuf[i] = Buffer.read(server, path, action: {arg buf; 
									"Loaded file".postln;
								});
							};
						}.defer;
					};
				}.play;
};
dwin.front;




});



bsnap = Button(win, Rect(170, this.width - 40, 25, 20))
.states_([
	["[ô]", Color.black, Color.white],
])
.action_({ arg but;
	if(controle.now>0) {
       controle.seek; // go to 0.0
       "Snapshot: Transport must be at zero seconds. Please try again.".postln;
    } {
        controle.snapshot;  // only take snapshot at 0.0
        "Snapshot taken".postln;
    }
	
});



this.win.drawFunc = {
	//paint origin
	Pen.fillColor = Color(0.6,0.8,0.8);
	Pen.addArc(this.halfwidth@this.halfwidth, this.halfwidth, 0, 2pi);
	Pen.fill;


	Pen.fillColor = Color.gray(0, 0.5);
	Pen.addArc(this.halfwidth@this.halfwidth, 20, 0, 2pi);
	Pen.fill;

	//	Pen.width = 10;
};

// seleção de fontes
itensdemenu = Array.newClear(this.nfontes);
this.nfontes.do { arg i;
	itensdemenu[i] = "Source " ++ (i + 1).asString;
};

m = PopUpMenu(win,Rect(10,10,90,20));
m.items = itensdemenu; 
m.action = { arg menu;
	fatual = menu.value;
	
	if(doppler[fatual] == 1){dopcheque.value = true}{dopcheque.value = false};
if(lp[fatual] == 1){loopcheck.value = true}{loopcheck.value = false};

if(sp[fatual] == 1){spreadcheck.value = true}{spreadcheck.value = false};
if(df[fatual] == 1){diffusecheck.value = true}{diffusecheck.value = false};

if(rv[fatual] == 1){revcheck.value = true}{revcheck.value = false};
if(ln[fatual] == "_linear"){lincheck.value = true}{lincheck.value = false};

if(hwn[fatual] == 1){hwInCheck.value = true}{hwInCheck.value = false};
if(scn[fatual] == 1){scInCheck.value = true}{scInCheck.value = false};

angnumbox.value = angle[fatual];
angslider.value = angle[fatual] / pi;
volnumbox.value = level[fatual];
dopnumbox.value = dplev[fatual];
volslider.value = level[fatual];
gnumbox.value = glev[fatual];
gslider.value = glev[fatual];
lnumbox.value = llev[fatual];
lslider.value = llev[fatual];
rslider.value = (rlev[fatual] + pi) / 2pi;
rnumbox.value = rlev[fatual];
dirslider.value = dlev[fatual] / (pi/2);
dirnumbox.value = dlev[fatual];
cslider.value = clev[fatual];
zslider.value = (zlev[fatual] + this.halfwidth) / this.width;

dpslider.value = dplev[fatual];
connumbox.value = clev[fatual];

ncannumbox.value = this.ncan[fatual];
busininumbox.value = this.busini[fatual];

auxslider1.value = this.aux1[fatual];
aux1numbox.value = this.aux1[fatual];
auxslider2.value = this.aux2[fatual];
aux2numbox.value = this.aux2[fatual];
auxslider3.value = this.aux3[fatual];
aux3numbox.value = this.aux3[fatual];
auxslider4.value = this.aux4[fatual];
aux4numbox.value = this.aux4[fatual];
auxslider5.value = this.aux5[fatual];
aux5numbox.value = this.aux5[fatual];

if(testado[fatual]) {  // don't change button if we are playing via automation
	// only if it is being played/streamed manually
	if (this.synt[fatual] == nil){
btestar.value = 0;
} {
btestar.value = 1;
};
} {
	btestar.value = 0;
};
};



offset = 60;


dopcheque = CheckBox( win, Rect(104, 10, 80, 20), "Doppler").action_({ arg butt;
	{this.dcheck[fatual].valueAction = butt.value;}.defer;
});
dopcheque.value = false;

loopcheck = CheckBox( win, Rect(184, 10, 80, 20), "Loop").action_({ arg butt;
	{this.lpcheck[fatual].valueAction = butt.value;}.defer;
});
loopcheck.value = false;

spreadcheck = CheckBox( win, Rect(244, 170, 80, 20), "Spread").action_({ arg butt;
	{this.spcheck[fatual].valueAction = butt.value;}.defer;
});
spreadcheck.value = false;
diffusecheck = CheckBox( win, Rect(314, 170, 80, 20), "Diffuse").action_({ arg butt;
	{this.dfcheck[fatual].valueAction = butt.value;}.defer;
});
diffusecheck.value = false;


revcheck = CheckBox( win, Rect(250, 10, 180, 20), "A-format reverb").action_({ arg butt;
	{this.rvcheck[fatual].valueAction = butt.value;}.defer;
});
revcheck.value = false;

lincheck = CheckBox( win, Rect(184, 30, 180, 20), "Linear intensity").action_({ arg butt;
	{this.lncheck[fatual].valueAction = butt.value;}.defer;
});
lincheck.value = false;


hwInCheck = CheckBox( win, Rect(10, 30, 100, 20), "HW-in").action_({ arg butt;
	{this.hwncheck[fatual].valueAction = butt.value;}.defer;
if (hwInCheck.value && scInCheck.value) {
};
});

scInCheck = CheckBox( win, Rect(104, 30, 60, 20), "SC-in").action_({ arg butt;
	{this.scncheck[fatual].valueAction = butt.value;}.defer;
if (scInCheck.value && hwInCheck.value) {
};
});




dopcheque.value = false;



textbuf = StaticText(win, Rect(55, -10 + offset, 200, 20));
textbuf.string = "No. of chans. (HW & SC-in)";
ncannumbox = NumberBox(win, Rect(10, -10 + offset, 40, 20));
ncannumbox.value = 0;
ncannumbox.clipHi = 4;
ncannumbox.clipLo = 0;
ncannumbox.align = \center;
ncannumbox.action = {arg num;
	
	
	{this.ncanbox[fatual].valueAction = num.value;}.defer;
this.ncan[fatual] = num.value;

};



textbuf = StaticText(win, Rect(55, 10 + offset, 240, 20));
textbuf.string = "Start Bus (HW-in)";
busininumbox = NumberBox(win, Rect(10, 10 + offset, 40, 20));
busininumbox.value = 0;
busininumbox.clipLo = 0;
busininumbox.align = \center;
busininumbox.action = {arg num; 
	{this.businibox[fatual].valueAction = num.value;}.defer;
this.busini[fatual] = num.value;
};




textbuf = StaticText(win, Rect(163, 130 + offset, 90, 20));
textbuf.string = "Angle (Stereo)";
angnumbox = NumberBox(win, Rect(10, 130 + offset, 40, 20));
angnumbox.value = 0;
angnumbox.clipHi = pi;
angnumbox.clipLo = 0;
angnumbox.step_(0.1); 
angnumbox.scroll_step=0.1;
angnumbox.align = \center;
angnumbox.action = {arg num; 
	{abox[fatual].valueAction = num.value;}.defer;
if((ncanais[fatual]==2) || (this.ncan[fatual]==2)){
	this.espacializador[fatual].set(\angle, num.value);
this.setSynths(fatual, \angle, num.value);
angle[fatual] = num.value;
}
{angnumbox.value = 0;};
};

angslider = Slider.new(win, Rect(50, 130 + offset, 110, 20));
//	b = ControlSpec(0.0, 3.14, \linear, 0.01); // min, max, mapping, step

angslider.action = {arg num;
	{abox[fatual].valueAction = num.value * pi;}.defer;
if((ncanais[fatual]==2) || (this.ncan[fatual]==2)) {
	{angnumbox.value = num.value * pi;}.defer;
//			this.espacializador[fatual].set(\angle, b.map(num.value));
this.espacializador[fatual].set(\angle, num.value * pi);
this.setSynths(fatual, \angle, num.value * pi);
//			angle[fatual] = b.map(num.value);
angle[fatual] = num.value * pi;
}{{angnumbox.value = num.value * pi;}.defer;};
};



/////////////////////////////////////////////////////////


textbuf = StaticText(win, Rect(this.width - 90, this.halfwidth - 10, 90, 20));
textbuf.string = "Z-Axis";
znumbox = NumberBox(win, Rect(this.width - 65, ((this.width - zSliderHeight) / 2) + zSliderHeight, 60, 20));
znumbox.value = 0;
znumbox.clipHi = 1;
znumbox.clipLo = -1;
znumbox.step_(0.1); 
znumbox.scroll_step=0.1;
znumbox.align = \center;
znumbox.action = {arg num; 
	{zbox[fatual].valueAction = num.value;}.defer;
if(ncanais[fatual]==2)   {
	this.espacializador[fatual].set(\elev, num.value);
this.setSynths(fatual, \elev, num.value);
zlev[fatual] = num.value;
}
{{znumbox.value = 0;}.defer;};
};


zslider = Slider.new(win, Rect(this.width - 45, ((this.width - zSliderHeight) / 2), 20, zSliderHeight));
zslider.value = 0.5;
zslider.action = {arg num;
	{znumbox.value = (0.5 - num.value) * -2;}.defer;
{zbox[fatual].valueAction = znumbox.value;}.defer;
{zlev[fatual] = znumbox.value;}.defer;


};


////////////////////////////// Orientation //////////////

if (this.serport.notNil) {
	
this.headingnumbox = NumberBox(win, Rect(this.width - 45, this.width - 65, 40, 20));
this.rollnumbox = NumberBox(win, Rect(this.width - 45, this.width - 45, 40, 20));
this.pitchnumbox = NumberBox(win, Rect(this.width - 45, this.width - 25, 40, 20));
this.headingnumbox.action = {arg num;
	this.globFOATransform.set(\heading, num.value);
};
this.rollnumbox.action = {arg num;
	this.globFOATransform.set(\roll, num.value);
};
this.pitchnumbox.action = {arg num;
	this.globFOATransform.set(\pitch, num.value);
};
textbuf = StaticText(win, Rect(this.width - 60, this.width - 65, 12, 20));
textbuf.string = "H:";
textbuf = StaticText(win, Rect(this.width - 60, this.width - 45, 10, 22));
textbuf.string = "R:";
textbuf = StaticText(win, Rect(this.width - 60, this.width - 25, 10, 22));
textbuf.string = "P:";


textbuf = StaticText(win, Rect(this.width - 45, this.width - 85, 90, 20));
textbuf.string = "Orient.";

};


////////////////////////////////////////////////////////////


textbuf = StaticText(win, Rect(163, 30 + offset, 50, 20));
textbuf.string = "Level";
volnumbox = NumberBox(win, Rect(10, 30 + offset, 40, 20));
volnumbox.value = 0;
volnumbox.clipHi = pi;
volnumbox.clipLo = 0;
volnumbox.step_(0.1); 
volnumbox.scroll_step=0.1;
volnumbox.align = \center;
volnumbox.action = {arg num; 
	{vbox[fatual].valueAction = num.value;}.defer;

};
volslider = Slider.new(win, Rect(50, 30 + offset, 110, 20));
volslider.value = 0;
volslider.action = {arg num;
	{vbox[fatual].valueAction = num.value;}.defer;		
};


///////////////////////////////////////////////////////////////


textbuf= StaticText(win, Rect(163, 50 + offset, 120, 20));
textbuf.string = "Doppler amount";
// was called contraction, hence "connumbox".
dopnumbox = NumberBox(win, Rect(10, 50 + offset, 40, 20));
dopnumbox.value = 0;
dopnumbox.clipHi = pi;
dopnumbox.clipLo = -pi;
dopnumbox.step_(0.1); 
dopnumbox.scroll_step=0.1;
dopnumbox.align = \center;
dopnumbox.action = {arg num; 
	{dpbox[fatual].valueAction = num.value;}.defer;

};
// stepsize?
dpslider = Slider.new(win, Rect(50, 50 + offset, 110, 20));
dpslider.value = 0;
dpslider.action = {arg num;
	{dpbox[fatual].valueAction = num.value;}.defer;
{dopnumbox.value = num.value;}.defer;
};

/////////////////////////////////////////////////////////////////////////



textbuf = StaticText(win, Rect(163, 70 + offset, 150, 20));
textbuf.string = "Close Reverb";
gnumbox = NumberBox(win, Rect(10, 70 + offset, 40, 20));
gnumbox.value = 1;
gnumbox.clipHi = pi;
gnumbox.clipLo = 0;
gnumbox.step_(0.1); 
gnumbox.scroll_step=0.1;
gnumbox.align = \center;
gnumbox.action = {arg num; 
	{gbox[fatual].valueAction = num.value;}.defer;

};
// stepsize?
gslider = Slider.new(win, Rect(50, 70 + offset, 110, 20));
gslider.value = 0;
gslider.action = {arg num;
	{gbox[fatual].valueAction = num.value;}.defer;
};



textbuf = StaticText(win, Rect(163, 90 + offset, 150, 20));
textbuf.string = "Distant Reverb";
lnumbox = NumberBox(win, Rect(10, 90 + offset, 40, 20));
lnumbox.value = 1;
lnumbox.clipHi = pi;
lnumbox.clipLo = 0;
lnumbox.step_(0.1); 
lnumbox.scroll_step=0.1;
lnumbox.align = \center;
lnumbox.action = {arg num; 
	{lbox[fatual].valueAction = num.value;}.defer;

};
// stepsize?
lslider = Slider.new(win, Rect(50, 90 + offset, 110, 20));
lslider.value = 0;
lslider.action = {arg num;
	{lbox[fatual].valueAction = num.value;}.defer;
};



textbuf = StaticText(win, Rect(163, 150 + offset, 150, 20));
textbuf.string = "Rotation (B-Format)";
rnumbox = NumberBox(win, Rect(10, 150 + offset, 40, 20));
rnumbox.value = 0;
rnumbox.clipHi = pi;
rnumbox.clipLo = -pi;
rnumbox.step_(0.1); 
rnumbox.scroll_step=0.1;
rnumbox.align = \center;
rnumbox.action = {arg num; 
	{rbox[fatual].valueAction = num.value;}.defer;

};
// stepsize?
rslider = Slider.new(win, Rect(50, 150 + offset, 110, 20));
rslider.value = 0.5;
rslider.action = {arg num;
	{rbox[fatual].valueAction = num.value * 6.28 - pi;}.defer;
{rnumbox.value = num.value * 2pi - pi;}.defer;

};



textbuf = StaticText(win, Rect(163, 170 + offset, 150, 20));
textbuf.string = "Directivity (B-Format)";
dirnumbox = NumberBox(win, Rect(10, 170 + offset, 40, 20));
dirnumbox.value = 0;
dirnumbox.clipHi = pi;
dirnumbox.clipLo = -pi;
dirnumbox.step_(0.1); 
dirnumbox.scroll_step=0.1;
dirnumbox.align = \center;
dirnumbox.action = {arg num; 
	{dbox[fatual].valueAction = num.value;}.defer;
};
// stepsize?
dirslider = Slider.new(win, Rect(50, 170 + offset, 110, 20));
dirslider.value = 0;
dirslider.action = {arg num;
	{dbox[fatual].valueAction = num.value * pi/2;}.defer;
{dirnumbox.value = num.value * pi/2;}.defer;
};



textbuf = StaticText(win, Rect(163, 110 + offset, 80, 20));
textbuf.string = "Contraction";
connumbox = NumberBox(win, Rect(10, 110 + offset, 40, 20));
connumbox.value = 0;
connumbox.clipHi = pi;
connumbox.clipLo = -pi;
connumbox.step_(0.1); 
connumbox.scroll_step=0.1;
connumbox.align = \center;
connumbox.action = {arg num; 
	{cbox[fatual].valueAction = num.value;}.defer;

};
// stepsize?
cslider = Slider.new(win, Rect(50, 110 + offset, 110, 20));
cslider.value = 0;
cslider.action = {arg num;
	{cbox[fatual].valueAction = num.value;}.defer;
{connumbox.value = num.value;}.defer;
};





bload = Button(win, Rect(this.width - 190, 10, 90, 20))
.states_([
	["load audio", Color.black, Color.white],
])
.action_({ arg but;
	this.synt[fatual].free; // error check
this.espacializador[fatual].free;
dopcheque.value = false; // coloque toggle no padrão



Dialog.openPanel(
controle.stopRecording;
controle.stop;
//controle.seek;


{ 
	arg path;
	
	{
		this.streamdisk[fatual] = false;
		this.tfield[fatual].valueAction = path;}.defer;
	stcheck[fatual].valueAction = false;

	

	
}, 
{
	this.streamdisk[fatual] = false;
	"cancelled".postln;
	{this.tfield[fatual].value = "";}.defer;
	stcheck[fatual].valueAction = false;
}				
);

});

bstream = Button(win, Rect(this.width - 190, 30, 90, 20))
.states_([
	["stream audio", Color.black, Color.white],
])
.action_({ arg but;
	this.synt[fatual].free; // error check
this.espacializador[fatual].free;
dopcheque.value = false; // set to the default


Dialog.openPanel(
controle.stopRecording;
controle.stop;
controle.seek;
{ 
	arg path;
	
	{
		this.streamdisk[fatual] = true;
		this.tfield[fatual].valueAction = path;}.defer;
	stcheck[fatual].valueAction = true;
	
}, 
{
	"cancelled".postln;
	this.streamdisk[fatual] = false;
	{this.tfield[fatual].value = "";}.defer;
	stcheck[fatual].valueAction = false;
}

);

});


bnodes = Button(win, Rect(this.width - 190, 50, 90, 20))
.states_([
	["show nodes", Color.black, Color.white],
])
.action_({
	server.plotTree;
});

textbuf = StaticText(wdados, Rect(30, 20, 50, 20));
textbuf.font = Font(Font.defaultSansFace, 9);
textbuf.string = "Dp";
textbuf = StaticText(wdados, Rect(45, 20, 50, 20));
textbuf.font = Font(Font.defaultSansFace, 9);
textbuf.string = "Lp";
textbuf = StaticText(wdados, Rect(60, 20, 50, 20));
textbuf.font = Font(Font.defaultSansFace, 9);
textbuf.string = "Rv";
textbuf = StaticText(wdados, Rect(75, 20, 50, 20));
textbuf.font = Font(Font.defaultSansFace, 9);
textbuf.string = "Hw";
textbuf = StaticText(wdados, Rect(90, 20, 50, 20));
textbuf.font = Font(Font.defaultSansFace, 9);
textbuf.string = "Sc";
textbuf = StaticText(wdados, Rect(105, 20, 50, 20));
textbuf.font = Font(Font.defaultSansFace, 9);
textbuf.string = "Ln";

textbuf = StaticText(wdados, Rect(120, 20, 50, 20));
textbuf.font = Font(Font.defaultSansFace, 9);
textbuf.string = "Sp";
textbuf = StaticText(wdados, Rect(135, 20, 50, 20));
textbuf.font = Font(Font.defaultSansFace, 9);
textbuf.string = "Df";

textbuf = StaticText(wdados, Rect(150, 20, 50, 20));
textbuf.font = Font(Font.defaultSansFace, 9);
textbuf.string = "NCan";
textbuf = StaticText(wdados, Rect(175, 20, 50, 20));
textbuf.font = Font(Font.defaultSansFace, 9);
textbuf.string = "SBus";

textbuf = StaticText(wdados, Rect(200, 20, 50, 20));
textbuf.font = Font(Font.defaultSansFace, 9);
textbuf.string = "X";
textbuf = StaticText(wdados, Rect(233, 20, 50, 20));
textbuf.font = Font(Font.defaultSansFace, 9);
textbuf.string = "Y";

textbuf = StaticText(wdados, Rect(266, 20, 50, 20));
textbuf.font = Font(Font.defaultSansFace, 9);
textbuf.string = "Z";

textbuf = StaticText(wdados, Rect(299, 20, 50, 20));
textbuf.font = Font(Font.defaultSansFace, 9);
textbuf.string = "O.x";
textbuf = StaticText(wdados, Rect(333, 20, 50, 20));
textbuf.font = Font(Font.defaultSansFace, 9);
textbuf.string = "O.y";
textbuf = StaticText(wdados, Rect(366, 20, 50, 20));
textbuf.font = Font(Font.defaultSansFace, 9);
textbuf.string = "O.z";


textbuf = StaticText(wdados, Rect(400, 20, 50, 20));
textbuf.font = Font(Font.defaultSansFace, 9);
textbuf.string = "Lev";
textbuf = StaticText(wdados, Rect(425, 20, 50, 20));
textbuf.font = Font(Font.defaultSansFace, 9);
textbuf.string = "DAmt";
textbuf = StaticText(wdados, Rect(450, 20, 50, 20));
textbuf.font = Font(Font.defaultSansFace, 9);
textbuf.string = "Prox";
textbuf = StaticText(wdados, Rect(475, 20, 50, 20));
textbuf.font = Font(Font.defaultSansFace, 9);
textbuf.string = "Dist";
textbuf = StaticText(wdados, Rect(500, 20, 50, 20));
textbuf.font = Font(Font.defaultSansFace, 9);
textbuf.string = "Ang";
textbuf = StaticText(wdados, Rect(525, 20, 50, 20));
textbuf.font = Font(Font.defaultSansFace, 9);
textbuf.string = "Rot";
textbuf = StaticText(wdados, Rect(550, 20, 50, 20));
textbuf.font = Font(Font.defaultSansFace, 9);
textbuf.string = "Dir";
textbuf = StaticText(wdados, Rect(575, 20, 50, 20));
textbuf.font = Font(Font.defaultSansFace, 9);
textbuf.string = "Cont";

textbuf = StaticText(wdados, Rect(600, 20, 50, 20));
textbuf.font = Font(Font.defaultSansFace, 9);
textbuf.string = "A1";
textbuf = StaticText(wdados, Rect(625, 20, 50, 20));
textbuf.font = Font(Font.defaultSansFace, 9);
textbuf.string = "A2";
textbuf = StaticText(wdados, Rect(650, 20, 50, 20));
textbuf.font = Font(Font.defaultSansFace, 9);
textbuf.string = "A3";
textbuf = StaticText(wdados, Rect(675, 20, 50, 20));
textbuf.font = Font(Font.defaultSansFace, 9);
textbuf.string = "A4";
textbuf = StaticText(wdados, Rect(700, 20, 50, 20));
textbuf.font = Font(Font.defaultSansFace, 9);
textbuf.string = "A5";

textbuf = StaticText(wdados, Rect(725, 20, 50, 20));
textbuf.font = Font(Font.defaultSansFace, 9);
textbuf.string = "a1";
textbuf = StaticText(wdados, Rect(740, 20, 50, 20));
textbuf.font = Font(Font.defaultSansFace, 9);
textbuf.string = "a2";
textbuf = StaticText(wdados, Rect(755, 20, 50, 20));
textbuf.font = Font(Font.defaultSansFace, 9);
textbuf.string = "a3";
textbuf = StaticText(wdados, Rect(770, 20, 50, 20));
textbuf.font = Font(Font.defaultSansFace, 9);
textbuf.string = "a4";
textbuf = StaticText(wdados, Rect(785, 20, 50, 20));
textbuf.font = Font(Font.defaultSansFace, 9);
textbuf.string = "a5";


textbuf = StaticText(wdados, Rect(800, 20, 50, 20));
textbuf.font = Font(Font.defaultSansFace, 9);
textbuf.string = "File";

textbuf = StaticText(wdados, Rect(925, 20, 50, 20));
textbuf.font = Font(Font.defaultSansFace, 9);
textbuf.string = "St";


this.nfontes.do { arg i;

	textbuf = StaticText(wdados, Rect(10, 40 + (i*20), 50, 20));
textbuf.font = Font(Font.defaultSansFace, 9);
textbuf.string = (i+1).asString;

this.dcheck[i] = CheckBox.new( wdados, Rect(30, 40 + (i*20), 40, 20))
.action_({ arg but;
if(i==fatual){dopcheque.value = but.value;};
if (but.value == true) {
	doppler[i] = 1;
	this.espacializador[i].set(\dopon, 1);
	this.synt[i].set(\dopon, 1);
	this.setSynths(i, \dopon, 1);
}{
	doppler[i] = 0;
	this.espacializador[i].set(\dopon, 0);
	this.synt[i].set(\dopon, 0);
	this.setSynths(i, \dopon, 0);
};
});

this.lpcheck[i] = CheckBox.new(wdados, Rect(45, 40 + (i*20), 40, 20))
.action_({ arg but;
if(i==fatual){loopcheck.value = but.value;};
if (but.value == true) {
	lp[i] = 1;
	this.synt[i].set(\lp, 1);
	this.setSynths(i, \lp, 1);
}{
	lp[i] = 0;
	this.synt[i].set(\lp, 0);
	this.setSynths(i, \lp, 0);
};
});



this.rvcheck[i] = CheckBox.new(wdados, Rect(60, 40 + (i*20), 40, 20))
.action_({ arg but;
if(i==fatual){revcheck.value = but.value;};
if (but.value == true) {
	rv[i] = 1;
	//this.synt[i].set(\lp, 1);
	this.setSynths(i, \rv, 1);
}{
	rv[i] = 0;
	//this.synt[i].set(\lp, 0);
	this.setSynths(i, \rv, 0);
};
});


this.hwncheck[i] = CheckBox.new( wdados, Rect(75, 40 + (i*20), 40, 20))
.action_({ arg but;
if(i==fatual){hwInCheck.value = but.value;};
if (but.value == true) {
	this.scncheck[i].value = false;
	if(i==fatual){scInCheck.value = false;};
	hwn[i] = 1;
	scn[i] = 0;
	this.synt[i].set(\hwn, 1);
}{
	hwn[i] = 0;
	this.synt[i].set(\hwn, 0);
};
});

this.scncheck[i] = CheckBox.new( wdados, Rect(90, 40 + (i*20), 40, 20))
.action_({ arg but;
if(i==fatual){scInCheck.value = but.value;};
if (but.value == true) {
	this.hwncheck[i].value = false;
	if(i==fatual){hwInCheck.value = false;};
	scn[i] = 1;
	hwn[i] = 0;
	this.synt[i].set(\scn, 1);
}{
	scn[i] = 0;
	this.synt[i].set(\scn, 0);
};
});


this.lncheck[i] = CheckBox.new( wdados, Rect(105, 40 + (i*20), 40, 20))
.action_({ arg but;
if(i==fatual){lincheck.value = but.value;};
if (but.value == true) {
	ln[i] = "_linear";
	this.setSynths(i, \ln, 1);
}{
	ln[i] = "";
	this.setSynths(i, \ln, 0);
};
});

this.spcheck[i] = CheckBox.new(wdados, Rect(120, 40 + (i*20), 40, 20))
.action_({ arg but;
if(i==fatual){spreadcheck.value = but.value;};
if (but.value == true) {
	this.dfcheck[i].value = false;
	if(i==fatual){diffusecheck.value = false;};
	sp[i] = 1;
	df[i] = 0;
	this.espacializador[i].set(\sp, 1);
	this.espacializador[i].set(\df, 0);
	this.synt[i].set(\sp, 1);
	this.setSynths(i, \ls, 1);
}{
	sp[i] = 0;
	this.espacializador[i].set(\sp, 0);
	this.synt[i].set(\sp, 0);
	this.setSynths(i, \sp, 0);
};
});
this.dfcheck[i] = CheckBox.new(wdados, Rect(135, 40 + (i*20), 40, 20))
.action_({ arg but;
if(i==fatual){diffusecheck.value = but.value;};
if (but.value == true) {
	this.spcheck[i].value = false;
	if(i==fatual){spreadcheck.value = false;};
	df[i] = 1;
	sp[i] = 0;
	this.espacializador[i].set(\df, 1);
	this.espacializador[i].set(\sp, 0);
	this.synt[i].set(\df, 1);
	this.setSynths(i, \df, 1);
}{
	df[i] = 0;
	this.espacializador[i].set(\df, 0);
	this.synt[i].set(\df, 0);
	this.setSynths(i, \df, 0);
};
});



this.ncanbox[i] = NumberBox(wdados, Rect(150, 40 + (i*20), 25, 20));
this.businibox[i] = NumberBox(wdados, Rect(175, 40 + (i*20), 25, 20));

xbox[i] = NumberBox(wdados, Rect(200, 40 + (i*20), 33, 20));
ybox[i] = NumberBox(wdados, Rect(233, 40+ (i*20), 33, 20));
zbox[i] = NumberBox(wdados, Rect(266, 40+ (i*20), 33, 20));

oxbox[i] = NumberBox(wdados, Rect(300, 40 + (i*20), 33, 20));
oybox[i] = NumberBox(wdados, Rect(333, 40+ (i*20), 33, 20));
ozbox[i] = NumberBox(wdados, Rect(366, 40+ (i*20), 33, 20));

vbox[i] = NumberBox(wdados, Rect(400, 40 + (i*20), 25, 20));
dpbox[i] = NumberBox(wdados, Rect(425, 40+ (i*20), 25, 20));
gbox[i] = NumberBox(wdados, Rect(450, 40+ (i*20), 25, 20));
lbox[i] = NumberBox(wdados, Rect(475, 40+ (i*20), 25, 20));
abox[i] = NumberBox(wdados, Rect(500, 40+ (i*20), 25, 20));
rbox[i] = NumberBox(wdados, Rect(525, 40+ (i*20), 25, 20));
dbox[i] = NumberBox(wdados, Rect(550, 40+ (i*20), 25, 20));
cbox[i] = NumberBox(wdados, Rect(575, 40+ (i*20), 25, 20));

a1box[i] = NumberBox(wdados, Rect(600, 40+ (i*20), 25, 20));
a2box[i] = NumberBox(wdados, Rect(625, 40+ (i*20), 25, 20));
a3box[i] = NumberBox(wdados, Rect(650, 40+ (i*20), 25, 20));
a4box[i] = NumberBox(wdados, Rect(675, 40+ (i*20), 25, 20));
a5box[i] = NumberBox(wdados, Rect(700, 40+ (i*20), 25, 20));


a1box[i].clipHi = 1;
a1box[i].clipLo = 0;
a2box[i].clipHi = 1;
a2box[i].clipLo = 0;
a3box[i].clipHi = 1;
a3box[i].clipLo = 0;
a4box[i].clipHi = 1;
a4box[i].clipLo = 0;
a5box[i].clipHi = 1;
a5box[i].clipLo = 0;

a1check[i] = CheckBox.new( wdados, Rect(725, 40 + (i*20), 40, 20))
.action_({ arg but;

if (but.value == true) {
	a1but[i] = 1;
	this.setSynths(i, \a1check, 1);
}{
	a1but[i] = 0;
	this.setSynths(i, \a1check, 0);
};
});
a2check[i] = CheckBox.new( wdados, Rect(740, 40 + (i*20), 40, 20))
.action_({ arg but;

if (but.value == true) {
	a2but[i] = 1;
	this.setSynths(i, \a2check, 1);
}{
	a2but[i] = 0;
	this.setSynths(i, \a2check, 0);
};
});
a3check[i] = CheckBox.new( wdados, Rect(755, 40 + (i*20), 40, 20))
.action_({ arg but;

if (but.value == true) {
	a3but[i] = 1;
	this.setSynths(i, \a3check, 1);
}{
	a3but[i] = 0;	
	this.setSynths(i, \a3check, 0);
};
});
a4check[i] = CheckBox.new( wdados, Rect(770, 40 + (i*20), 40, 20))
.action_({ arg but;

if (but.value == true) {
	a4but[i] = 1;
	this.setSynths(i, \a4check, 1);
}{
	a4but[i] = 0;
	this.setSynths(i, \a4check, 0);
};
});
a5check[i] = CheckBox.new( wdados, Rect(785, 40 + (i*20), 40, 20))
.action_({ arg but;

if (but.value == true) {
	a5but[i] = 1;
	this.setSynths(i, \a5check, 1);
}{
	a5but[i] = 0;
	this.setSynths(i, \a5check, 0);
};
});


this.tfield[i] = TextField(wdados, Rect(800, 40+ (i*20), 125, 20));
//			this.tfield[i] = TextField(wdados, Rect(720, 40+ (i*20), 220, 20));

stcheck[i] = CheckBox.new( wdados, Rect(925, 40 + (i*20), 40, 20))
.action_({ arg but;

if (but.value == true) {
	this.streamdisk[i] = true;
	//	this.setSynths(i, \a5check, 1);
}{
	this.streamdisk[i] = false;
	//	a5but[i] = 0;
	//	this.setSynths(i, \a5check, 0);
};
});


this.ncanbox[i].font = Font(Font.defaultSansFace, 9);
this.businibox[i].font = Font(Font.defaultSansFace, 9);
xbox[i].font = Font(Font.defaultSansFace, 9);
ybox[i].font = Font(Font.defaultSansFace, 9);
zbox[i].font = Font(Font.defaultSansFace, 9);
oxbox[i].font = Font(Font.defaultSansFace, 9);
oybox[i].font = Font(Font.defaultSansFace, 9);
ozbox[i].font = Font(Font.defaultSansFace, 9);


abox[i].font = Font(Font.defaultSansFace, 9);
vbox[i].font = Font(Font.defaultSansFace, 9);
gbox[i].font = Font(Font.defaultSansFace, 9);
lbox[i].font = Font(Font.defaultSansFace, 9);
rbox[i].font = Font(Font.defaultSansFace, 9);
dbox[i].font = Font(Font.defaultSansFace, 9);
cbox[i].font = Font(Font.defaultSansFace, 9);
dpbox[i].font = Font(Font.defaultSansFace, 9);
a1box[i].font = Font(Font.defaultSansFace, 9);
a2box[i].font = Font(Font.defaultSansFace, 9);
a3box[i].font = Font(Font.defaultSansFace, 9);
a4box[i].font = Font(Font.defaultSansFace, 9);
a5box[i].font = Font(Font.defaultSansFace, 9);

this.tfield[i].font = Font(Font.defaultSansFace, 9);

xbox[i].decimals = 3;
ybox[i].decimals = 3;
zbox[i].decimals = 3;
oxbox[i].decimals = 0;
oybox[i].decimals = 0;
ozbox[i].decimals = 0;


a1box[i].action = {arg num;
	this.setSynths(i, \aux1, num.value);
aux1[i] = num.value;
if(i == fatual) 
{
auxslider1.value = num.value;
aux1numbox.value = num.value;
};
}; 
a2box[i].action = {arg num;
	this.setSynths(i, \aux2, num.value);
aux2[i] = num.value;
if(i == fatual) 
{
auxslider2.value = num.value;
aux2numbox.value = num.value;
};
}; 
a3box[i].action = {arg num;
	this.setSynths(i, \aux3, num.value);
aux3[i] = num.value;
if(i == fatual) 
{
auxslider3.value = num.value;
aux3numbox.value = num.value;
};
}; 
a4box[i].action = {arg num;
	this.setSynths(i, \aux4, num.value);
aux4[i] = num.value;
if(i == fatual) 
{
auxslider4.value = num.value;
aux4numbox.value = num.value;
};
}; 
a5box[i].action = {arg num;
	this.setSynths(i, \aux5, num.value);
aux5[i] = num.value;
if(i == fatual) 
{
auxslider5.value = num.value;
aux5numbox.value = num.value;
};
}; 


this.tfield[i].action = {arg path;
	if ( (path.notNil || (path != "")) && this.streamdisk[i].not ) {
//{stcheck[i].value.postln}.defer;
sombuf[i] = Buffer.read(server, path.value, action: {arg buf; 
	"Loaded file".postln;
});
} {
"To stream file".postln;
};

};
//// EDIT HERE

xbox[i].action = {arg num;
	//	lastx[i] = num.value;
	this.xval[i] = num.value;
sprite[i, 1] =  this.halfwidth + (num.value * -1 * this.halfwidth);
novoplot.value(num.value, ybox[i], i, this.nfontes);
if(this.espacializador[i].notNil || this.playingBF[i]) {
this.espacializador[i].set(\mx, this.xval[i]);
this.setSynths(i, \mx, this.xval[i]);
this.synt[i].set(\mx, this.xval[i]);
};

};
ybox[i].action = {arg num;
	this.yval[i] = num.value;
sprite[i, 0] = ((num.value * this.halfwidth * -1) + this.halfwidth);

if(this.espacializador[i].notNil || this.playingBF[i]){

this.espacializador[i].set(\my, this.yval[i]);
this.setSynths(i, \my, this.yval[i]);
this.synt[i].set(\my, this.yval[i]);
};		
//{oybox[i].valueAction = this.origin.y;}.defer;
};

zbox[i].action = {arg num;
	lastz[i] = num.value;
this.espacializador[i].set(\mz, num.value);
//(num.value).postln;
// this wrong
//this.zval[i] = num.value / this.halfwidth;
// should be
this.zval[i] = num.value;
if (this.zval[i] > 1) {this.zval[i] = 1};
if (this.zval[i] < -1) {this.zval[i] = -1};

this.setSynths(i, \mz, this.zval[i]);
this.synt[i].set(\mz, this.zval[i]);
zlev[i] = this.zval[i];
if(i == fatual) 
{
zslider.value = (num.value + 1) / 2;
znumbox.value = num.value;
};
};




this.dcheck[i].value = 0;

abox[i].clipHi = pi;
abox[i].clipLo = 0;
vbox[i].clipHi = 1.0;
vbox[i].clipLo = 0;
gbox[i].clipHi = 1.0;
gbox[i].clipLo = 0;
lbox[i].clipHi = 1.0;
lbox[i].clipLo = 0;

vbox[i].scroll_step = 0.01;
abox[i].scroll_step = 0.01;
vbox[i].step = 0.01;
abox[i].step = 0.01;
gbox[i].scroll_step = 0.01;
lbox[i].scroll_step = 0.01;
gbox[i].step = 0.01;
lbox[i].step = 0.01;


abox[i].action = {arg num;
	angle[i] = num.value;
if((ncanais[i]==2) || (this.ncan[i]==2)){
this.espacializador[i].set(\angle, num.value);
this.setSynths(i, \angle, num.value);
angle[i] = num.value;
};
if(i == fatual) 
{
angnumbox.value = num.value;
angslider.value = num.value / pi;
};

}; 
vbox[i].action = {arg num;
	this.synt[i].set(\level, num.value);
this.setSynths(i, \level, num.value);
level[i] = num.value;
if(i == fatual) 
{
volslider.value = num.value;
volnumbox.value = num.value;
};
}; 





gbox[i].value = 0;
lbox[i].value = 0;

gbox[i].action = {arg num;
	this.espacializador[i].set(\glev, num.value);
this.setSynths(i, \glev, num.value);

this.synt[i].set(\glev, num.value);
glev[i] = num.value;
if(i == fatual) 
{
gslider.value = num.value;
gnumbox.value = num.value;
};
}; 


lbox[i].action = {arg num;
	this.espacializador[i].set(\llev, num.value);
this.setSynths(i, \llev, num.value);
this.synt[i].set(\llev, num.value);
llev[i] = num.value;
if(i == fatual) 
{
lslider.value = num.value;
lnumbox.value = num.value;
};
}; 


rbox[i].action = {arg num; 

	this.synt[i].set(\rotAngle, num.value);
this.setSynths(i, \rotAngle, num.value);
rlev[i] = num.value;
if(i == fatual) 
{
//num.value * 6.28 - pi;
rslider.value = (num.value + pi) / 2pi;
rnumbox.value = num.value;
};
};

dbox[i].action = {arg num; 
	this.synt[i].set(\directang, num.value);
this.setSynths(i, \directang, num.value);
dlev[i] = num.value;
if(i == fatual) 
{
//num.value * pi/2;
dirslider.value = num.value / (pi/2);
dirnumbox.value = num.value;
};
};

cbox[i].action = {arg num; 
	this.synt[i].set(\contr, num.value);

// TESTING
this.espacializador[i].set(\contr, num.value);


this.setSynths(i, \contr, num.value);
clev[i] = num.value;
if(i == fatual) 
{
cslider.value = num.value;
connumbox.value = num.value;
};
};

dpbox[i].action = {arg num;
	// used for b-format amb/bin only
	this.synt[i].set(\dopamnt, num.value);
this.setSynths(i, \dopamnt, num.value);
// used for the others
this.espacializador[i].set(\dopamnt, num.value);
dplev[i] = num.value;
if(i == fatual) 
{
dpslider.value = num.value;
dopnumbox.value = num.value;
};
};


// CHECK THESE NEXT 2
this.ncanbox[i].action = {arg num;
	this.espacializador[i].set(\mz, num.value);
this.setSynths(i, \mz, num.value);
this.synt[i].set(\mz, num.value);
this.ncan[i] = num.value;
if(i == fatual )
{
//var val = (this.halfwidth - (num.value * width)) * -1;
//	ncanslider.value = num.value;
ncannumbox.value = num.value;
};
}; 
this.businibox[i].action = {arg num;
	this.espacializador[i].set(\mz, num.value);
this.setSynths(i, \mz, num.value);
this.synt[i].set(\mz, num.value);
this.busini[i] = num.value;
if(i == fatual) 
{
//var val = (this.halfwidth - (num.value * width)) * -1;
//	ncanslider.value = num.value;
busininumbox.value = num.value;
};
}; 


};


runTriggers = {
	this.nfontes.do({
arg i;
if(testado[i].not) {
	if(this.triggerFunc[i].notNil) {
		this.triggerFunc[i].value;
		//updateSynthInArgs.value(i);
	}
}
})
};

runTrigger = {
	arg source;
	//	if(scncheck[i]) {
	if(this.triggerFunc[source].notNil) {
		this.triggerFunc[source].value;
updateSynthInArgs.value(source);
}
};

runStops = {
	this.nfontes.do({
    arg i;
    if(testado[i].not) {
		if(this.stopFunc[i].notNil) {
			this.stopFunc[i].value;
		}
	} 
})
};

runStop = { 
	arg source;
	if(this.stopFunc[source].notNil) {
		this.stopFunc[source].value;
}
};


//controle = Automation(dur).front(win, Rect(this.halfwidth, 10, 400, 25));
~autotest = controle = Automation(this.dur, showLoadSave: false, showSnapshot: false,
	minTimeStep: 0.001).front(win,
		Rect(10, this.width - 80, 400, 22));
controle.presetDir = prjDr ++ "/auto";
//controle.setMinTimeStep(2.0);
controle.onEnd = {
	//	controle.stop;
	controle.seek;
	if(this.autoloopval) {
		controle.play;	
	};
	this.nfontes.do { arg i;
		if(this.synt[i].notNil) {
this.synt[i].free;
};
};
};

controle.onPlay = {

	var startTime;
	this.nfontes.do { arg i;
		this.firstTime[i]=true;
//("HERE = " ++ this.firstTime[i]).postln;
};

	//	runTriggers.value;
	if(controle.now < 0)
	{
		startTime = 0
	}
	{ 
		startTime = controle.now
	};
	isPlay = true;



	//runTriggers.value;
};


controle.onSeek = {
	var wasplaying = isPlay;
	//("isPlay = " ++ isPlay).postln;
	//runStops.value; // necessary? doesn't seem to help prob of SC input
	
	//runStops.value;
	if(isPlay == true) {
		this.nfontes.do { arg i;	
        this.synt[i].free; // error check
        };
    controle.stop;
  };
    
     if(wasplaying) {
		 //isPlay = true;
		 "we are here".postln;
		 {controle.play}.defer(0.5); //delay necessary. may need more?
	 };
};

controle.onStop = {
	runStops.value;
	this.nfontes.do { arg i;
		// if sound is currently being "tested", don't switch off on stop
		// leave that for user
		if (testado[i] == false) {
           this.synt[i].free; // error check
        };
//	this.espacializador[i].free;
};
isPlay = false;

};



this.nfontes.do { arg i;
	controle.dock(xbox[i], "x_axis_" ++ i);
controle.dock(ybox[i], "y_axis_" ++ i);
controle.dock(zbox[i], "z_axis_" ++ i);



controle.dock(vbox[i], "level_" ++ i);
controle.dock(dpbox[i], "dopamt_" ++ i);

controle.dock(abox[i], "angle_" ++ i);
controle.dock(gbox[i], "revglobal_" ++ i);
controle.dock(lbox[i], "revlocal_" ++ i);
controle.dock(rbox[i], "rotation_" ++ i);
controle.dock(dbox[i], "diretividade_" ++ i);
controle.dock(cbox[i], "contraction_" ++ i);

// automation change
/*
controle.dock(dcheck[i], "doppler_" ++ i);			
controle.dock(lpcheck[i], "loop_" ++ i);
controle.dock(hwncheck[i], "hwin_" ++ i);
controle.dock(ncanbox[i], "numchannels_" ++ i);
controle.dock(businibox[i], "busini_" ++ i);
controle.dock(scncheck[i], "scin_" ++ i);
controle.dock(rvcheck[i], "rev_" ++ i);
controle.dock(lncheck[i], "linear_" ++ i);
controle.dock(spcheck[i], "spread_" ++ i);
controle.dock(dfcheck[i], "diffuse_" ++ i);
*/
controle.dock(a1box[i], "aux1_" ++ i);
controle.dock(a2box[i], "aux2_" ++ i);
controle.dock(a3box[i], "aux3_" ++ i);
controle.dock(a4box[i], "aux4_" ++ i);
controle.dock(a5box[i], "aux5_" ++ i);

controle.dock(a1check[i], "aux1check_" ++ i);
controle.dock(a2check[i], "aux2check_" ++ i);
controle.dock(a3check[i], "aux3check_" ++ i);
controle.dock(a4check[i], "aux4check_" ++ i);
controle.dock(a5check[i], "aux5check_" ++ i);

controle.dock(stcheck[i], "stcheck_" ++ i);



};

/*
	~myview = UserView.new(win,win.view.bounds);

	~myview.mouseDownAction = { |x, y, modifiers, buttonNumber, clickCount|
	buttonNumber.postln;
	};
*/
win.view.mouseDownAction={ arg view, x, y, modifiers, buttonNumber, clickCount;
	mouseButton = buttonNumber; // 0 = left, 2 = middle, 1 = right
	if((mouseButton == 1) || (mouseButton == 2)) {
		dragStartScreen.x = y - this.halfwidth;
		dragStartScreen.y = x - this.halfwidth;
		this.nfontes.do { arg i;
if(this.espacializador[i].notNil) {
	
	this.espacializador[i].set(\mx, this.xval[i], \my, this.yval[i]
	);
	//,
	//\xoffset, this.xoffset[i], \yoffset, this.yoffset[i]);
	this.setSynths(i, \mx, this.xval[i], \my, this.yval[i]
	);
	//,
	//\xoffset, this.xoffset[i], \yoffset, this.yoffset[i]);
	this.synt[i].set(\mx, this.xval[i], \my, this.yval[i]
	);
	//,
	//\xoffset, this.xoffset[i], \yoffset, this.yoffset[i]);
};
};
};
};

win.view.mouseMoveAction = {|view, x, y, modifiers| [x, y];
	if(mouseButton == 0) { // left button
		xbox[fatual].valueAction = (this.halfwidth - y) / this.halfwidth;
ybox[fatual].valueAction = ((x - this.halfwidth) * -1) / this.halfwidth;

win.drawFunc = {
// big circle
Pen.fillColor = Color(0.6,0.8,0.8);
Pen.addArc(this.halfwidth@this.halfwidth, this.halfwidth, 0, 2pi);
Pen.fill;
//Pen.width = 10;

this.nfontes.do { arg i;	
	Pen.fillColor = Color(0.8,0.2,0.9);
	Pen.addArc(sprite[i, 0]@sprite[i, 1], 20, 0, 2pi);
	Pen.fill;
	(i + 1).asString.drawCenteredIn(Rect(sprite[i, 0] - 10, sprite[i, 1] - 10, 20, 20), 
		Font.default, Color.white);
};



// círculo central
Pen.fillColor = Color.gray(0, 0.5);
Pen.addArc(this.halfwidth@this.halfwidth, 20, 0, 2pi);
Pen.fill;


};
} {

};

win.refresh;

};

// busses to send audio from player to spatialiser synths
this.nfontes.do { arg x;
	mbus[x] = Bus.audio(server, 1); 
	sbus[x] = Bus.audio(server, 2); 
	//	bfbus[x] = Bus.audio(s, 4); 
	if (dopflag == 0, {

};, {
});
};



win.onClose_({ 
	controle.quit;
	if (this.serport.notNil) {
this.trackPort.close;
//				this.trackerRoutine.stop;
//				this.serialKeepItUp.stop;
};
this.troutine.stop;
this.kroutine.stop;
this.watcher.stop;

this.globTBus.free;
this.nfontes.do { arg x;
this.espacializador[x].free;
this.aFormatBusFoa[0,x].free;
this.aFormatBusFoa[1,x].free;
this.aFormatBusSoa[0,x].free;
this.aFormatBusSoa[1,x].free;
mbus[x].free;
sbus[x].free;
//	bfbus.[x].free;
sombuf[x].free;
this.streambuf[x].free;
this.synt[x].free;
this.scInBus[x].free;
//		kespac[x].stop;
};
MIDIIn.removeFuncFrom(\sysex, sysex);
if(revGlobal.notNil){
revGlobal.free;
};
if(revGlobalBF.notNil){
revGlobalBF.free;
};
if(revGlobalSoa.notNil){
revGlobalSoa.free;
};

if (this.serport.notNil) {
if(this.globFOATransform.notNil){
this.globFOATransform.free
};
};

wdados.close;
waux.close;
gbus.free;
gbfbus.free;
if(rirWspectrum.notNil){
rirWspectrum.free; };
if(rirXspectrum.notNil){
rirXspectrum.free;};
if(rirYspectrum.notNil){
rirYspectrum.free;};
if(rirZspectrum.notNil){
rirZspectrum.free;};
if(rirFLUspectrum.notNil){
rirFLUspectrum.free;};
if(rirFRDspectrum.notNil){
rirFRDspectrum.free;};
if(rirBLDspectrum.notNil){
rirBLDspectrum.free;};
if(rirBRUspectrum.notNil){
rirBRUspectrum.free;};
soaBus.free;
12.do { arg i;
if(rirA12Spectrum[i].notNil){
	rirA12Spectrum[i].free;};
};
foaEncoderOmni.free;
foaEncoderSpread.free;
foaEncoderDiffuse.free;
b2a.free;
a2b.free;
});

mmcslave = CheckBox( win, Rect(197, this.width - 40, 140, 20), "Slave to MMC").action_({ arg butt;
	//("Doppler is " ++ butt.value).postln;
	if(butt.value) {
"Slaving transport to MMC".postln;
MIDIIn.addFuncTo(\sysex, sysex);
} {
"MIDI input closed".postln;
MIDIIn.removeFuncFrom(\sysex, sysex);
};

//	dcheck[fatual].valueAction = butt.value;
});

this.autoloop = CheckBox( win, Rect(305, this.width - 40, 140, 20), "Loop").action_({ arg butt;
	//("Doppler is " ++ butt.value).postln;
	if(butt.value) {
"Looping transport".postln;
this.autoloopval = true;
} {
this.autoloopval = false;			
};

});


sysex  = { arg src, sysex;
	// This should be more elaborate - other things might trigger it...fix this!
	if(sysex[3] == 6){ var x;
		("We have : " ++ sysex[4] ++ " type action").postln;

x = case
{ sysex[4] == 1 } {

"Stop".postln;
controle.stop;
}
{ sysex[4] == 2 } {
"Play".postln;
controle.play;

}
{ sysex[4] == 3 } {
"Deffered Play".postln;
controle.play;

}
{ sysex[4] == 68 } { var goto; 
("Go to event: " ++ sysex[7] ++ "hr " ++ sysex[8] ++ "min "
++ sysex[9] ++ "sec and " ++ sysex[10] ++ "frames").postln;
goto =  (sysex[7] * 3600) + (sysex[8] * 60) + sysex[9] + (sysex[10] / 30);
controle.seek(goto);

};
};
};
if (this.serport.notNil) {
	//this.troutine = this.trackerRoutine; // start parsing of serial head tracker data
	//	this.kroutine = this.serialKeepItUp;
	this.troutine.play;
	this.kroutine.play;
};
this.watcher.play;
controle.snapshot; // necessary to call at least once before saving automation
// otherwise will get not understood errors
}


}