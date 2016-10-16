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


Mosca {
	//	classvar fftsize = 2048; 
   	var <>myTestVar;
	var  <>kernelSize, <>scale, <>rirW, <>rirX, <>rirY, <>rirZ,
	<>rirWspectrum, <>rirXspectrum, <>rirYspectrum, <>rirZspectrum,
	//	<>rirWXYZspectrum,  rirWXYZ, // experimental!
	rirFLUspectrum, rirFRDspectrum, rirBLDspectrum, rirBRUspectrum,

	<>irbuffer, <>bufsize, <>win, <>wdados, <>waux, <>sprite, <>nfontes,
	<>controle, <>revGlobal, <>revGlobalSoa, <>revGlobalBF, <>m, <>offset, <>textbuf, <>controle,
	<>sysex, <>mmcslave,
	<>synthRegistry, <>busini, <>ncan, <>swinbus,
	<>aux1, <>aux2, <>aux3, <>aux4, <>aux5, 
	<>dec,
	<>triggerFunc, <>stopFunc,
	<>scInBus,
	<>width, <>halfwidth, <>scale,
	<>dur,
	<>rawbus,
    <>delaytime, <>decaytime; // for allpass 

	classvar server, rirW, rirX, rirY, rirZ,
	rirFLU, rirFRD, rirBLD, rirBRU,
	rirA12, // 2nd order a-format array of RIRs
	rirA12Spectrum,
	bufsize, irbuffer,
	b2a, a2b,

	soa_a12_decoder_matrix, soa_a12_encoder_matrix,
	cart, spher, foa_a12_decoder_matrix,

	o, //debugging
	prjDr;
	classvar fftsize = 2048,
	//	classvar fftsize = 1024,
	//classvar fftsize = 4096,
	server;

	*new { arg projDir, nsources = 1, width = 800, dur = 180, rir = "allpass", server = Server.default, decoder = nil, rawbus = 0;
		^super.new.initMosca(projDir, nsources, width, dur, rir, server, decoder, rawbus);
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
";
		^string;
		
	}

	initMosca { arg projDir, nsources, iwidth, idur, rir, iserver, decoder, irawbus;
		var makeSynthDefPlayers, makeSpatialisers, revGlobTxt,
		espacAmbOutFunc, espacAmbEstereoOutFunc, revGlobalAmbFunc,
		playBFormatOutFunc, playMonoInFunc, playStereoInFunc, playBFormatInFunc,
		revGlobalSoaOutFunc,
		prepareSoaSigFunc,
		localReverbFunc, localReverbStereoFunc,
		bufAformat, bufAformat_soa_a12, bufWXYZ;
		//synthRegistry = List[],
		
		//	testit; // remove at some point with other debugging stuff
		b2a = FoaDecoderMatrix.newBtoA;
		a2b = FoaEncoderMatrix.newAtoB;
		if (iwidth < 550) {
			this.width = 550;
		} {
			this.width = iwidth;
		};
		this.halfwidth = this.width / 2;
		this.scale = this.halfwidth; // for the moment at least
		this.dur = idur;
		this.rawbus = irawbus;
		
		this.nfontes = nsources;
		playMonoInFunc = Array.newClear(3); // one for File, Stereo & BFormat;
		playStereoInFunc = Array.newClear(3);
		playBFormatInFunc = Array.newClear(3);

		this.synthRegistry = Array.newClear(this.nfontes);
		this.nfontes.do { arg x;
			this.synthRegistry[x] = List[];
		};

		server = iserver;

		o = OSCresponderNode(server.addr, '/tr', { |time, resp, msg| msg.postln }).add;  // debugging

		// Note. this will replace swinbus 
		this.scInBus = Array.newClear(this.nfontes);
		this.nfontes.do { arg x;
			this.scInBus[x] = Bus.audio(server, 4);
		};
		
		// array of functions, 1 for each source (if defined), that will be launched on Automation's "play"
		this.triggerFunc = Array.newClear(this.nfontes);
		//companion to above. Launched by "Stop"
		this.stopFunc = Array.newClear(this.nfontes);


		
		///////////// Functions to substitute blocks of code in SynthDefs //////////////
		if (decoder.isNil) {
			espacAmbOutFunc = { |ambsinal, ambsinal1O, dec|
				Out.ar( this.rawbus, ambsinal); };
			espacAmbEstereoOutFunc = { |ambsinal1plus2, ambsinal1plus2_1O, dec|
				Out.ar( this.rawbus, ambsinal1plus2); };
			revGlobalAmbFunc = { |ambsinal, dec|
				Out.ar( this.rawbus, ambsinal); };
			revGlobalSoaOutFunc = { |soaSig, foaSig, dec|
				Out.ar( this.rawbus, soaSig); };
			playBFormatOutFunc = { |player, dec|
				Out.ar( this.rawbus, player); };
			
		} {
			espacAmbOutFunc = { |ambsinal, ambsinal1O, dec|
				Out.ar( 0, FoaDecode.ar(ambsinal1O, dec)); };
			espacAmbEstereoOutFunc = { |ambsinal1plus2, ambsinal1plus2_1O, dec|
				Out.ar( 0, FoaDecode.ar(ambsinal1plus2_1O, dec)); };
			revGlobalAmbFunc = { |ambsinal, dec|
				Out.ar( 0, FoaDecode.ar(ambsinal, dec)); };
			revGlobalSoaOutFunc = { |soaSig, foaSig, dec|
				Out.ar( 0, FoaDecode.ar(foaSig, dec)); };
			playBFormatOutFunc = { |player, dec|
				Out.ar( 0, FoaDecode.ar(player, dec)); };
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
		dec = decoder;

		if (rir != "allpass") {
			//testit = OSCresponderNode(server.addr, '/tr', { |time, resp, msg| msg.postln }).add;  // debugging
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
			//this.delaytime = rir / 20;
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
				//sig = DelayN.ar(sig, 0.048,0.048);

				//sig=Mix.fill(8,{CombL.ar(sig,0.1,rrand(0.01, 0.1),5)});
				
				//				6.do({ sig = AllpassN.ar(sig, 0.051, [rrand(0.01, 0.05),rrand(0.01, 0.05)], 1) });
				//12.do({ sig = AllpassN.ar(sig, 0.051, rrand(0.01, 0.05), 3) });
				16.do({ sig = AllpassC.ar(sig, this.delaytime, { Rand(0.01,this.delaytime) }.dup(4),
					this.decaytime)});
				
				
				//sig = AllpassN.ar(sig, 0.05, Array.fill(4, {0.05}).rand, 1.0);

				//sig = AllpassL.ar(sig, 1.0, Array.fill( 4, {1.0}).rand, 2.0);
				
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
				//lrevRef.value = AllpassN.ar(p, delaytime, delaytime.rand, decaytime);
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
				glev = 0, llev = 0;
				//var w, x, y, z, r, s, t, u, v,
				var p, ambsinal, ambsinal1O,
				junto, rd, dopplershift, azim, dis, xatras, yatras,  
				globallev, locallev, gsig, fonte,
				intens,
				soa_a12_sig;
				var lrev;
				var grevganho = 0.04; // needs less gain
				var soaSigRef = Ref(0);
				mx = Lag.kr(mx, 0.1);
				my = Lag.kr(my, 0.1);
				mz = Lag.kr(mz, 0.1);
				fonte = Cartesian.new;
				fonte.set(mx, my, mz);
				//				dis = (1 - (fonte.rho - this.scale)) / this.scale;
				dis = 1 - fonte.rho;

				//SendTrig.kr(Impulse.kr(1),0,  dis); // debugging
				
				azim = fonte.theta;
				el = fonte.phi;
				dis = Select.kr(dis < 0, [dis, 0]); 
				dis = Select.kr(dis > 1, [dis, 1]); 
				//SendTrig.kr(Impulse.kr(1),0,  azim); // debugging			
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
				//				ambsinal = [w, x, y, z, r, s, t, u, v];
				prepareSoaSigFunc.value(soaSigRef, junto, azim, el, intens: intens, dis: dis);

				ambsinal1O = [soaSigRef[0].value, soaSigRef[1].value, soaSigRef[2].value, soaSigRef[3].value];

				ambsinal = [soaSigRef[0].value, soaSigRef[1].value, soaSigRef[2].value, soaSigRef[3].value,
					soaSigRef[4].value, soaSigRef[5].value, soaSigRef[6].value, soaSigRef[7].value,
					soaSigRef[8].value];

				//				Out.ar(soaBus, (ambsinal*globallev) + (ambsinal*locallev));
				Out.ar(soaBus, (ambsinal*globallev) + (ambsinal*locallev));
				//				ambsinal1O = [w, x, y, z];

				dis = (1 - dis) * 5.0;
				dis = Select.kr(dis < 0.001, [dis, 0.001]);
				ambsinal1O = HPF.ar(ambsinal1O, 20); // stops bass frequency blow outs by proximity
				ambsinal1O = FoaTransform.ar(ambsinal1O, 'proximity', dis);

				espacAmbOutFunc.value(ambsinal, ambsinal1O, dec);			
			}).add;

			SynthDef.new("espacAmbChowning"++linear,  {
				arg el = 0, inbus, gbus, soaBus, mx = -5000, my = -5000, mz = 0,
				dopon = 0, dopamnt = 0,
				glev = 0, llev = 0;
				var wRef, xRef, yRef, zRef, rRef, sRef, tRef, uRef, vRef, pRef,
				ambsinal, ambsinal1O,
				junto, rd, dopplershift, azim, dis, xatras, yatras,  
				//		globallev = 0.0001, locallev, gsig, fonte;
				globallev, locallev, gsig, fonte,
				intens,
				soa_a12_sig;
				var lrev, p;
				var grevganho = 0.04; // needs less gain
				var w, x, y, z, r, s, t, u, v;
				var soaSigRef = Ref(0);
				var lrevRef = Ref(0);
				mx = Lag.kr(mx, 0.1);
				my = Lag.kr(my, 0.1);
				mz = Lag.kr(mz, 0.1);
				fonte = Cartesian.new;
				fonte.set(mx, my, mz);
				dis = 1 - fonte.rho;
				//SendTrig.kr(Impulse.kr(1), 0, dis); // debug
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
				globallev = Select.kr(globallev > 1, [globallev, 1]); 
				globallev = Select.kr(globallev < 0, [globallev, 0]);
				globallev = globallev * Lag.kr(glev, 0.1);
				gsig = p * globallev;
				Out.ar(gbus, gsig); //send part of direct signal global reverb synth
				// Local reverberation
				locallev = 1 - dis; 
				locallev = locallev  * Lag.kr(llev, 0.1);

				//lrevRef.value = PartConv.ar(p, fftsize, rirWspectrum.bufnum, locallev);
				localReverbFunc.value(lrevRef, p, fftsize, rirWspectrum, locallev);

				junto = p + lrevRef.value;
				//			#w, x, y, z, r, s, t, u, v = FMHEncode0.ar(junto, azim, el, dis);

				//				#w, x, y, z, r, s, t, u, v = FMHEncode0.ar(junto, azim, el, intens);
				//				ambsinal = [w, x, y, z, r, s, t, u, v];
				//soaSigRef.value = FMHEncode0.ar(junto, azim, el, intens);

				prepareSoaSigFunc.value(soaSigRef, junto, azim, el, intens: intens, dis: dis);

				ambsinal1O = [soaSigRef[0].value, soaSigRef[1].value, soaSigRef[2].value, soaSigRef[3].value];

				dis = (1 - dis) * 5.0;
				dis = Select.kr(dis < 0.001, [dis, 0.001]);
				ambsinal1O = HPF.ar(ambsinal1O, 20); // stops bass frequency blow outs by proximity
				ambsinal1O = FoaTransform.ar(ambsinal1O, 'proximity', dis);

				
				ambsinal = [soaSigRef[0].value, soaSigRef[1].value, soaSigRef[2].value, soaSigRef[3].value,
					soaSigRef[4].value, soaSigRef[5].value, soaSigRef[6].value, soaSigRef[7].value,
					soaSigRef[8].value];

				espacAmbOutFunc.value(ambsinal, ambsinal1O, dec);			
			}).add;

			


			// This second version of espacAmb is used with contracted B-format sources

			
			SynthDef.new("espacAmb2Chowning"++linear,  { 
				arg el = 0, inbus, gbus, mx = -5000, my = -5000, mz = 0, dopon = 0,
				glev = 0, llev = 0.2;
				var w, x, y, z, r, s, t, u, v, p, ambsinal, ambsinal1O,
				junto, rd, dopplershift, azim, dis, xatras, yatras,  
				globallev = 0.0004, locallev, gsig, fonte;
				var lrev,
				intens;
				var soaSigRef = Ref(0);
				var lrevRef = Ref(0);
				var grevganho = 0.20;
				mx = Lag.kr(mx, 0.1);
				my = Lag.kr(my, 0.1);
				mz = Lag.kr(mz, 0.1);
				fonte = Cartesian.new;
				fonte.set(mx, my, mz);
				//dis = (1 - (fonte.rho - this.scale)) / this.scale;
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
				
				
				//lrev = PartConv.ar(p, fftsize, rirWspectrum.bufnum, locallev);

				localReverbFunc.value(lrevRef, p, fftsize, rirWspectrum, locallev);
				
				//SendTrig.kr(Impulse.kr(1),0,  lrev); // debugging
				junto = p + lrevRef.value;
				
				//				#w, x, y, z, r, s, t, u, v = FMHEncode0.ar(junto, azim, el, intens);
				//				ambsinal = [w, x, y, z, r, s, t, u, v]; 
				
				//	ambsinal1O = [w, x, y, z];
				prepareSoaSigFunc.value(soaSigRef, junto, azim, el, intens: intens, dis: dis);
				ambsinal1O = [soaSigRef[0].value, soaSigRef[1].value, soaSigRef[2].value, soaSigRef[3].value];
				ambsinal = [soaSigRef[0].value, soaSigRef[1].value, soaSigRef[2].value, soaSigRef[3].value,
					soaSigRef[4].value, soaSigRef[5].value, soaSigRef[6].value, soaSigRef[7].value,
					soaSigRef[8].value];

				dis = (1 - dis) * 5.0;
				dis = Select.kr(dis < 0.001, [dis, 0.001]);
				ambsinal1O = HPF.ar(ambsinal1O, 20); // stops bass frequency blow outs by proximity
				ambsinal1O = FoaTransform.ar(ambsinal1O, 'proximity', dis);

				espacAmbOutFunc.value(ambsinal, ambsinal1O, dec);
				
			}).add;

			SynthDef.new("espacAmb2AFormat"++linear,  { 
				arg el = 0, inbus, gbus, mx = -5000, my = -5000, mz = 0, dopon = 0,
				glev = 0, llev = 0.2, soaBus;
				var w, x, y, z, r, s, t, u, v, p, ambsinal, ambsinal1O,
				junto, rd, dopplershift, azim, dis, xatras, yatras,  
				globallev = 0.0004, locallev, gsig, fonte;
				var lrev, intens;
				var grevganho = 0.20;
				var soaSigRef = Ref(0);
				mx = Lag.kr(mx, 0.1);
				my = Lag.kr(my, 0.1);
				mz = Lag.kr(mz, 0.1);
				fonte = Cartesian.new;
				fonte.set(mx, my, mz);
				//dis = (1 - (fonte.rho - this.scale)) / this.scale;
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
				// DISABLE
				//Out.ar(gbus, gsig); //send part of direct signal global reverb synth
				
				// Reverberação local
				locallev = 1 - dis; 
				//		SendTrig.kr(Impulse.kr(1),0,  locallev); // debugging
				locallev = locallev * Lag.kr(llev, 0.1);
				
				
				//				lrev = PartConv.ar(p, fftsize, rirZspectrum.bufnum, locallev);
				//SendTrig.kr(Impulse.kr(1),0,  lrev); // debugging
				junto = p ;
				
				//				#w, x, y, z, r, s, t, u, v = FMHEncode0.ar(junto, azim, el, intens);
				//				ambsinal = [w, x, y, z, r, s, t, u, v];

				prepareSoaSigFunc.value(soaSigRef, junto, azim, el, intens: intens, dis: dis);
				ambsinal1O = [soaSigRef[0].value, soaSigRef[1].value, soaSigRef[2].value, soaSigRef[3].value];
				ambsinal = [soaSigRef[0].value, soaSigRef[1].value, soaSigRef[2].value, soaSigRef[3].value,
					soaSigRef[4].value, soaSigRef[5].value, soaSigRef[6].value, soaSigRef[7].value,
					soaSigRef[8].value];


				Out.ar(soaBus, (ambsinal*globallev) + (ambsinal*locallev));
				//				ambsinal1O = [w, x, y, z];

				dis = (1 - dis) * 5.0;
				dis = Select.kr(dis < 0.001, [dis, 0.001]);
				ambsinal1O = HPF.ar(ambsinal1O, 20); // stops bass frequency blow outs by proximity
				ambsinal1O = FoaTransform.ar(ambsinal1O, 'proximity', dis);
				
				espacAmbOutFunc.value(ambsinal, ambsinal1O, dec);
				
			}).add;


			


			SynthDef.new("espacAmbEstereoAFormat"++linear,  {
				arg el = 0, inbus, gbus, soaBus, mx = -5000, my = -5000, mz = 0, angle = 1.05,
				dopon = 0, dopamnt = 0, 
				glev = 0, llev = 0;
				var w, x, y, z, r, s, t, u, v, p, ambsinal,
				w1, x1, y1, z1, r1, s1, t1, u1, v1, p1, ambsinal1,
				w2, x2, y2, z2, r2, s2, t2, u2, v2, p2, ambsinal2, ambsinal1plus2, ambsinal1plus2_1O,
				junto, rd, dopplershift, azim, dis, 
				junto1, azim1, 
				junto2, azim2,
				intens,
				globallev = 0.0001, locallev, gsig, fonte;
				var lrev;
				var grevganho = 0.20;
				var soaSigLRef = Ref(0);
				var soaSigRRef = Ref(0);
				mx = Lag.kr(mx, 0.1);
				my = Lag.kr(my, 0.1);
				mz = Lag.kr(mz, 0.1);
				fonte = Cartesian.new;
				fonte.set(mx, my);
				
				azim1 = fonte.rotate(angle / -2).theta;
				azim2 = fonte.rotate(angle / 2).theta;
				
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
				globallev = Select.kr(globallev > 1, [globallev, 1]); // verifica se o "sinal" está mais do que 1
				globallev = Select.kr(globallev < 0, [globallev, 0]); 
				
				globallev = globallev * Lag.kr(glev, 0.1);
				
				//			gsig = Mix.new(p) / 2 * grevganho * globallev;
				//			Out.ar(gbus, gsig); //send part of direct signal global reverb synth
				
				p1 = p[0];
				p2 = p[1];
				// Reverberação local
				locallev = 1 - dis; 
				
				locallev = locallev  * Lag.kr(llev, 0.1);
				
				
				//			junto1 = p1 + PartConv.ar(p1, fftsize, rirZspectrum.bufnum, 1.0 * locallev);
				//			junto2 = p2 + PartConv.ar(p2, fftsize, rirZspectrum.bufnum, 1.0 * locallev);
				junto1 = p1;
				junto2 = p2;
				
				
				//				#w1, x1, y1, z1, r1, s1, t1, u1, v1 = FMHEncode0.ar(junto1, azim1, el, intens);
				//				#w2, x2, y2, z2, r2, s2, t2, u2, v2 = FMHEncode0.ar(junto2, azim2, el, intens);

				prepareSoaSigFunc.value(soaSigLRef, junto1, azim1, el, intens: intens, dis: dis);
				ambsinal1 = [soaSigLRef[0].value, soaSigLRef[1].value, soaSigLRef[2].value, soaSigLRef[3].value,
					soaSigLRef[4].value, soaSigLRef[5].value, soaSigLRef[6].value, soaSigLRef[7].value,
					soaSigLRef[8].value];

				prepareSoaSigFunc.value(soaSigRRef, junto2, azim2, el, intens: intens, dis: dis);
				ambsinal2 = [soaSigRRef[0].value, soaSigRRef[1].value, soaSigRRef[2].value, soaSigRRef[3].value,
					soaSigRRef[4].value, soaSigRRef[5].value, soaSigRRef[6].value, soaSigRRef[7].value,
					soaSigRRef[8].value];
				
				
				//ambsinal1 = [w1, x1, y1, z1, r1, s1, t1, u1, v1]; 
				//ambsinal2 = [w2, x2, y2, z2, r2, s2, t2, u2, v2];
				
				ambsinal1plus2 = ambsinal1 + ambsinal2;
				ambsinal1plus2_1O = [soaSigLRef[0].value, soaSigLRef[1].value, soaSigLRef[2].value,
					soaSigLRef[3].value] + [soaSigRRef[0].value, soaSigRRef[1].value,
						soaSigRRef[2].value, soaSigRRef[3].value];

				Out.ar(soaBus, (ambsinal1plus2_1O*globallev) + (ambsinal1plus2_1O*locallev));

				dis = (1 - dis) * 5.0;
				dis = Select.kr(dis < 0.001, [dis, 0.001]);
				ambsinal1plus2_1O = HPF.ar(ambsinal1plus2_1O, 20); // stops bass frequency blow outs by proximity
				ambsinal1plus2_1O = FoaTransform.ar(ambsinal1plus2_1O, 'proximity', dis);

				espacAmbEstereoOutFunc.value(ambsinal1plus2, ambsinal1plus2_1O, dec);
				
			}).add;

			SynthDef.new("espacAmbEstereoChowning"++linear,  {
				arg el = 0, inbus, gbus, soaBus, mx = -5000, my = -5000, mz = 0, angle = 1.05,
				dopon = 0, dopamnt = 0, 
				glev = 0, llev = 0;
				var w, x, y, z, r, s, t, u, v, p, ambsinal,
				w1, x1, y1, z1, r1, s1, t1, u1, v1, p1, ambsinal1,
				w2, x2, y2, z2, r2, s2, t2, u2, v2, p2, ambsinal2, ambsinal1plus2, ambsinal1plus2_1O,
				junto, rd, dopplershift, azim, dis, 
				junto1, azim1, 
				junto2, azim2, 
				globallev = 0.0001, locallev, gsig, fonte;
				var lrev,
				intens;
				var grevganho = 0.20;
				var soaSigLRef = Ref(0);
				var soaSigRRef = Ref(0);
				//var junto1Ref =  Ref(0);
				//	var junto2Ref =  Ref(0);
				var lrev1Ref =  Ref(0);
				var lrev2Ref =  Ref(0);

				mx = Lag.kr(mx, 0.1);
				my = Lag.kr(my, 0.1);
				mz = Lag.kr(mz, 0.1);
				fonte = Cartesian.new;
				fonte.set(mx, my);
				
				azim1 = fonte.rotate(angle / -2).theta;
				azim2 = fonte.rotate(angle / 2).theta;
				
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
				
				
				//				junto1 = p1 + PartConv.ar(p1, fftsize, rirZspectrum.bufnum, 1.0 * locallev);
				//				junto2 = p2 + PartConv.ar(p2, fftsize, rirZspectrum.bufnum, 1.0 * locallev);

				localReverbStereoFunc.value(lrev1Ref, lrev2Ref, p1, p2, fftsize, rirZspectrum, locallev);
				junto1 = p1 + lrev1Ref.value;
				junto2 = p2 + lrev2Ref.value;

				prepareSoaSigFunc.value(soaSigLRef, junto1, azim1, el, intens: intens, dis: dis);
				ambsinal1 = [soaSigLRef[0].value, soaSigLRef[1].value, soaSigLRef[2].value, soaSigLRef[3].value,
					soaSigLRef[4].value, soaSigLRef[5].value, soaSigLRef[6].value, soaSigLRef[7].value,
					soaSigLRef[8].value];

				prepareSoaSigFunc.value(soaSigRRef, junto2, azim2, el, intens: intens, dis: dis);
				ambsinal2 = [soaSigRRef[0].value, soaSigRRef[1].value, soaSigRRef[2].value, soaSigRRef[3].value,
					soaSigRRef[4].value, soaSigRRef[5].value, soaSigRRef[6].value, soaSigRRef[7].value,
					soaSigRRef[8].value];
				
				
				ambsinal1plus2 = ambsinal1 + ambsinal2;
				//				ambsinal1plus2_1O = [w1, x1, y1, z1] + [w2, x2, y2, z2];
				ambsinal1plus2_1O = [soaSigLRef[0].value, soaSigLRef[1].value, soaSigLRef[2].value,
					soaSigLRef[3].value] + [soaSigRRef[0].value, soaSigRRef[1].value,
						soaSigRRef[2].value, soaSigRRef[3].value];
				
				dis = (1 - dis) * 5.0;
				dis = Select.kr(dis < 0.001, [dis, 0.001]);
				ambsinal1plus2_1O = HPF.ar(ambsinal1plus2_1O, 20); // stops bass frequency blow outs by proximity
				ambsinal1plus2_1O = FoaTransform.ar(ambsinal1plus2_1O, 'proximity', dis);

				espacAmbEstereoOutFunc.value(ambsinal1plus2, ambsinal1plus2_1O, dec);
				
			}).add;

		}; //end makeSpatialisers

		prepareSoaSigFunc = { |soaSigRef, junto, azim, el, intens, dis|
			soaSigRef.value = FMHEncode0.ar(junto, azim, el, intens);

		};
		makeSpatialisers.value(linear: false);

		prepareSoaSigFunc = { |soaSigRef, junto, azim, el, intens, dis|
			soaSigRef.value = FMHEncode0.ar(junto, azim, el, dis);

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
				//		var sig;
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
					busini;
					var scaledRate, playerRef, wsinal, spos, pushang = 0,
					azim, dis = 1, fonte, globallev, locallev, 
					gsig, lsig, rd, dopplershift,
					intens;
					var grevganho = 0.20;			
					mx = Lag.kr(mx, 0.1);
					my = Lag.kr(my, 0.1);
					mz = Lag.kr(mz, 0.1);
					fonte = Cartesian.new;
					fonte.set(mx, my, mz);
					//					dis = (1 - (fonte.rho - this.scale)) / this.scale;
					dis = 1 - fonte.rho;
					pushang = (1 - dis) * pi / 2; // degree of sound field displacement
					//  0 = centred. pi/2 = 100% displaced
					azim = fonte.theta; // ângulo (azimuth) de deslocamento
					dis = Select.kr(dis < 0, [dis, 0]); 
					dis = Select.kr(dis > 1, [dis, 1]); 
					//SendTrig.kr(Impulse.kr(1), 0, mx); // debug
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
					

					//					playerRef.value = FoaTransform.ar(playerRef.value, 'rotate', rotAngle,
					//						level * intens * (1 - contr));
					prepareRotateFunc.value(dis, intens, playerRef, contr, rotAngle, Lag.kr(level, 0.1));

					playerRef.value = FoaTransform.ar(playerRef.value, 'push', pushang, azim);
					
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
					
					//
					//				Out.ar(gbus, gsig + lsig); //send part of direct signal global reverb synth
					
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

		// Make SWBus In SynthDefs

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

	// for testing

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


	openGui {

		//arg dur = 120;
		var fonte, dist, espacializador, mbus, sbus, soaBus, ncanais, synt, fatual = 0, 
		itensdemenu, gbus, gbfbus, azimuth, event, brec, bplay, bload, bnodes, sombuf, funcs, 
		dopcheque,
		lastAutomation = nil,
		loopcheck, lpcheck, lp,
		revcheck, rvcheck, rv,
		lincheck, lncheck, ln,
		hwInCheck, hwncheck, hwn, scInCheck, scncheck, scn,
		dopcheque2, doppler, angle, level, glev, 
		llev, angnumbox, volnumbox,
		ncannumbox, busininumbox, // for streams. ncan = number of channels (1, 2 or 4)
		// busini = initial bus number in range starting with "0"
		ncanbox, businibox,
		
		//ncan,
		//busini,
		novoplot,
		runTriggers, runStops, runTrigger, runStop,
		playingBF,
		
		dopnumbox, volslider, dirnumbox, dirslider, connumbox, conslider, cbox,
		a1box, a2box, a3box, a4box, a5box, 
		angslider, bsalvar, bcarregar, bdados, baux, xbox, ybox, abox, vbox, gbox, lbox, dbox, dpbox, dcheck,
		gslider, gnumbox, lslider, lnumbox, tfield, dopflag = 0, btestar, tocar, isPlay = false, isRec,
		atualizarvariaveis, updateSynthInArgs,
		auxslider1, auxslider2, auxslider3, auxslider4, auxslider5, 
		testado,
		rnumbox, rslider, rbox, 
		znumbox, zslider, zbox, zlev, // z-axis
		xval, yval, zval,
		rlev, dlev, clev, cslider, dplev, dpslider, cnumbox,
		aux1numbox, aux2numbox, aux3numbox, aux4numbox, aux5numbox, 
		zSliderHeight = this.width * 2 / 3;
		espacializador = Array.newClear(this.nfontes);
		doppler = Array.newClear(this.nfontes); 
		lp = Array.newClear(this.nfontes); 
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
		sombuf = Array.newClear(this.nfontes); 
		xval = Array.newClear(this.nfontes); 
		yval = Array.newClear(this.nfontes); 
		zval = Array.newClear(this.nfontes); 
		synt = Array.newClear(this.nfontes);
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

		ncanbox = Array.newClear(this.nfontes); 
		businibox = Array.newClear(this.nfontes); 
		playingBF = Array.newClear(this.nfontes); 
		
		
		xbox = Array.newClear(this.nfontes); 
		zbox = Array.newClear(this.nfontes); 
		ybox = Array.newClear(this.nfontes); 
		abox = Array.newClear(this.nfontes); // ângulo
		vbox = Array.newClear(this.nfontes);  // level
		dcheck = Array.newClear(this.nfontes);  // Doppler check
		gbox = Array.newClear(this.nfontes); // reverberação global
		lbox = Array.newClear(this.nfontes); // reverberação local
		rbox = Array.newClear(this.nfontes); // rotação de b-format
		dbox = Array.newClear(this.nfontes); // diretividade de b-format
		cbox = Array.newClear(this.nfontes); // contrair b-format
		dpbox = Array.newClear(this.nfontes); // dop amount
		lpcheck = Array.newClear(this.nfontes); // loop
		rvcheck = Array.newClear(this.nfontes); // diffuse reverb
		lncheck = Array.newClear(this.nfontes); // linear intensity
		hwncheck = Array.newClear(this.nfontes); // hardware-in check
		scncheck = Array.newClear(this.nfontes); // SuperCollider-in check
		a1box = Array.newClear(this.nfontes); // aux 
		a2box = Array.newClear(this.nfontes); // aux 
		a3box = Array.newClear(this.nfontes); // aux 
		a4box = Array.newClear(this.nfontes); // aux 
		a5box = Array.newClear(this.nfontes); // aux 

		tfield = Array.newClear(this.nfontes);
		
		testado = Array.newClear(this.nfontes);
		
		
		this.nfontes.do { arg i;
			doppler[i] = 0;
			angle[i] = 0;
			level[i] = 0;
			glev[i] = 0;
			llev[i] = 0;
			lp[i] = 0;
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

			this.ncan[i] = 0;
			this.busini[i] = 0;
			sprite[i, 0] = -20;
			sprite[i, 1] = -20;
			testado[i] = false;
			playingBF[i] = false;
		};
		
		
		
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
		wdados = Window.new("Data", Rect(this.width, 900, 955, (this.nfontes*20)+60 ));
		wdados.userCanClose = false;
		
		
		bdados = Button(win, Rect(280, 50, 90, 20))
		.states_([
			["show data", Color.black, Color.white],
			["hide data", Color.white, Color.blue]
		])
		.action_({ arg but;
			//	but.value.postln;
			if(but.value == 1)
			{wdados.front;}
			{wdados.visible = false;};
		});

		waux = Window.new("Auxiliary Controllers", Rect(this.width, 400, 260, 240 ));
		waux.userCanClose = false;
		
		
		baux = Button(win, Rect(280, 70, 90, 20))
		.states_([
			["show aux", Color.black, Color.white],
			["hide aux", Color.white, Color.blue]
		])
		.action_({ arg but;
			//	but.value.postln;
			if(but.value == 1)
			{waux.front;}
			{waux.visible = false;};
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
				this.setSynths(source, \mx, xval[source]);
				this.setSynths(source, \my, yval[source]);
				this.setSynths(source, \mz, zval[source]);
				
				this.setSynths(source, \rotAngle, rlev[source]);
				this.setSynths(source, \directang, dlev[source]);
				this.setSynths(source, \contr, clev[source]);

				this.setSynths(source, \aux1, aux1[source]);
				this.setSynths(source, \aux2, aux2[source]);
				this.setSynths(source, \aux3, aux3[source]);
				this.setSynths(source, \aux4, aux4[source]);
				this.setSynths(source, \aux5, aux5[source]);

				("Updating source" ++ (source+1)).postln;
			}.fork;
		};
		
		atualizarvariaveis = {
			"atualizando!".postln;
			
			this.nfontes.do { arg i;
				//	updateSynthInArgs.value(i);
				
				if(espacializador[i] != nil) {
					("atualizando espacializador # " ++ i).postln;
					espacializador[i].set(
						//	\mx, num.value  ???
						\dopon, doppler[i], // not needed...
						\angle, angle[i],
						\level, level[i], // ? or in player?
						\dopamnt, dplev[i],
						\glev, glev[i],
						\llev, llev[i],
						\mx, xbox[i].value,
						\my, ybox[i].value,
						\mz, zbox[i].value
					);
				};
				
				if(synt[i] != nil) {
					
					synt[i].set(
						\level, level[i],
						\rotAngle, rlev[i],
						\directang, dlev[i],
						\contr, clev[i],
						\dopamnt, dplev[i],
						\glev, glev[i],
						\llev, llev[i],
						\mx, xbox[i].value,
						\my, ybox[i].value,
						\mz, zbox[i].value
					);
					
					("x value = " ++ xbox[i].value).postln;
					
				};

				
			};
			
			
			
		};
		

		
		
		tocar = {
			arg i, tpos;
			var path = tfield[i].value;

			

			// Note: ncanais refers to number of channels in the context of
			// files on disk
			// ncan is number of channels for streamed input!
			// busini is the initial bus used for a particular stream
			// If we have ncan = 4 and busini = 7, the stream will enter
			// in buses 7, 8, 9 and 10.

			if(revGlobalBF == nil){
				revGlobalBF = Synth.new(\revGlobalBFormatAmb, [\gbfbus, gbfbus],
					addAction:\addToTail);
			};
			if(revGlobal == nil){
				revGlobal = Synth.new(\revGlobalAmb, [\gbus, gbus], addAction:\addToTail);
			};
			
			("tpos = " ++ tpos).postln;
			if ((path != "") && (hwncheck[i].value == false)) {
				{	
					
					if (sombuf[i].numChannels == 1)  // arquivo mono
					{ncanais[i] = 1;
						angle[i] = 0;
						{angnumbox.value = 0;}.defer;
						{angslider.value = 0;}.defer;
						
						if(rv[i] == 1) {
							if(revGlobalSoa == nil) {
								revGlobalSoa = Synth.new(\revGlobalSoaA12, [\soaBus, soaBus],
									revGlobalBF, addAction:\addBefore);
							};
							if (testado[i] == false) { // if source is testing don't relaunch synths
								synt[i] = Synth.new(\playMonoFile, [\outbus, mbus[i], 
									\bufnum, sombuf[i].bufnum, \rate, 1, \tpos, tpos, \lp, lp[i],
									\level, level[i]], 
									revGlobalSoa, addAction: \addBefore).onFree({espacializador[i].free;
										espacializador[i] = nil; synt[i] = nil});
								
								espacializador[i] = Synth.new(\espacAmbAFormatVerb++ln[i], [\inbus, mbus[i], 
									\soaBus, soaBus, \dopon, doppler[i]], 
									synt[i], addAction: \addAfter);
							};
						} { 
							if (testado[i].not) { // if source is testing don't relaunch synths
								synt[i] = Synth.new(\playMonoFile, [\outbus, mbus[i], 
									\bufnum, sombuf[i].bufnum, \rate, 1, \tpos, tpos, \lp, lp[i],
									\level, level[i]], revGlobalBF,
									addAction: \addBefore).onFree({espacializador[i].free;
										espacializador[i] = nil; synt[i] = nil});
								
								espacializador[i] = Synth.new(\espacAmbChowning++ln[i], [\inbus, mbus[i], 
									\gbus, gbus, \dopon, doppler[i]], 
									synt[i], addAction: \addAfter);
							};

						};
						atualizarvariaveis.value;
						
						
					}
					{if (sombuf[i].numChannels == 2) {ncanais[i] = 2; // arquivo estéreo
						angle[i] = pi/2;
						//						{angnumbox.value = pi/2;}.defer; 
						{angnumbox.value = 1.05;}.defer; // 60 degrees
						//						{angslider.value = 0.5;}.defer;
						{angslider.value = 0.33;}.defer;
						/*		
							if(revGlobalSoa == nil) {
							revGlobalSoa = Synth.new(\revGlobalSoaA12, [\soaBus, soaBus], addAction:\addToTail);
							};
						*/
						if(rv[i] == 1) {
							if(revGlobalSoa.isNil) {
								revGlobalSoa = Synth.new(\revGlobalSoaA12, [\soaBus, soaBus],
									revGlobalBF, addAction:\addBefore);
							};
							
							if (testado[i].not) {
								synt[i] = Synth.new(\playStereoFile, [\outbus, sbus[i], 
									\bufnum, sombuf[i].bufnum, \rate, 1, \tpos, tpos, \lp, lp[i], \level, level[i]], 
									revGlobalSoa, addAction: \addBefore).onFree({espacializador[i].free;
										espacializador[i] = nil; synt[i] = nil});
								
								espacializador[i] = Synth.new(\espacAmbEstereoAFormat++ln[i], [\inbus, sbus[i],
									\gbus, gbus, \soaBus, soaBus, \dopon, doppler[i]], 
									synt[i], addAction: \addAfter);
							};


						} {
							if (testado[i].not) {
								synt[i] = Synth.new(\playStereoFile, [\outbus, sbus[i], 
									\bufnum, sombuf[i].bufnum, \rate, 1, \tpos, tpos, \lp, lp[i],
									\level, level[i]], 
									revGlobalBF, addAction: \addBefore).onFree({espacializador[i].free;
										espacializador[i] = nil; synt[i] = nil});
								
								espacializador[i] = Synth.new(\espacAmbEstereoChowning++ln[i], [\inbus, sbus[i],
									\gbus, gbus, \dopon, doppler[i]], 
									synt[i], addAction: \addAfter);
							};
							
						};
						atualizarvariaveis.value;
						
						//	~revGlobal = Synth.new(\revGlobalAmb, [\gbus, gbus], addAction:\addToTail);



						
					} {
						if (sombuf[i].numChannels == 4) {
							"B-format".postln;
							playingBF[i] = true;
							ncanais[i] = 4;
							angle[i] = 0;
							{angnumbox.value = 0;}.defer;
							{angslider.value = 0;}.defer;
							// reverb for non-contracted (full b-format) component

							// reverb for contracted (mono) component - and for rest too
							if(rv[i] == 1) {

								if(revGlobalSoa == nil) {
									revGlobalSoa = Synth.new(\revGlobalSoaA12, [\soaBus, soaBus],
										revGlobalBF, addAction:\addBefore);
								};
								

								if (testado[i] == false) {

									
									synt[i] = Synth.new(\playBFormatFile++ln[i], [\gbus, gbus, \gbfbus, gbfbus, \outbus,
										mbus[i], \bufnum, sombuf[i].bufnum, \contr, clev[i],
										\rate, 1, \tpos, tpos, \lp,
										lp[i], \level, level[i], \dopon, doppler[i]], 
										//					~revGlobal, addAction: \addBefore);
										revGlobalSoa, addAction: \addBefore).onFree({espacializador[i].free;
											espacializador[i] = nil; synt[i] = nil;
											playingBF[i] = false});
									
									espacializador[i] = Synth.new(\espacAmb2AFormat++ln[i], [\inbus, mbus[i], 
										\gbus, gbus, \soaBus, soaBus, \dopon, doppler[i]], 
										synt[i], addAction: \addAfter);
								};
							} {
								if (testado[i] == false) {

									
									synt[i] = Synth.new(\playBFormatFile++ln[i], [\gbus, gbus, \gbfbus, gbfbus, \outbus,
										mbus[i], \bufnum, sombuf[i].bufnum, \contr, clev[i],
										\rate, 1, \tpos, tpos, \lp,
										lp[i], \level, level[i], \dopon, doppler[i]], 
										//					~revGlobal, addAction: \addBefore);
										revGlobalBF, addAction: \addBefore).onFree({espacializador[i].free;
											espacializador[i] = nil; synt[i] = nil;
											playingBF[i] = false});
									
									espacializador[i] = Synth.new(\espacAmb2Chowning++ln[i], [\inbus, mbus[i], \gbus, gbus, 
										\dopon, doppler[i]], 
										synt[i], addAction: \addAfter);
								};


							};
							atualizarvariaveis.value;
							

							
							
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
				("HELLO scncheck[i].value = " ++ scncheck[i].value).postln;
				if ((scncheck[i].value) || (hwncheck[i])) {
					var x;
					("Streaming! ncan = " ++ this.ncan[i].value
						++ " & this.busini = " ++ this.busini[i].value).postln;
					x = case
					{ this.ncan[i] == 1 } {
						"Mono!".postln;
						


						if(rv[i] == 1) {
							if(revGlobalSoa == nil) {
								revGlobalSoa = Synth.new(\revGlobalSoaA12, [\soaBus, soaBus],
									revGlobalBF, addAction:\addBefore);
							};
							
							if (testado[i].not) {
								if (hwncheck[i].value) {
									synt[i] = Synth.new(\playMonoHWBus, [\outbus, mbus[i], \busini, this.busini[i],
										\level, level[i]], revGlobalSoa,
										addAction: \addBefore).onFree({espacializador[i].free;
											espacializador[i] = nil; synt[i] = nil});
								} {
									synt[i] = Synth.new(\playMonoSWBus, [\outbus, mbus[i],
										\busini, this.scInBus[i], // use "index" method?
										\level, level[i]], revGlobalSoa,
										addAction: \addBefore).onFree({espacializador[i].free;
											espacializador[i] = nil; synt[i] = nil});
								};
								
								
								espacializador[i] = Synth.new(\espacAmbAFormatVerb++ln[i], [\inbus, mbus[i], 
									\soaBus, soaBus, \dopon, doppler[i]], 
									synt[i], addAction: \addAfter);
							};
							
						} {
							if (testado[i].not) {
								if (hwncheck[i].value) {
									synt[i] = Synth.new(\playMonoHWBus, [\outbus, mbus[i], \busini, this.busini[i],
										\level, level[i]], revGlobalBF,
										addAction: \addBefore).onFree({espacializador[i].free;
											espacializador[i] = nil; synt[i] = nil});
								} {
									"But especially here".postln;
									synt[i] = Synth.new(\playMonoSWBus, [\outbus, mbus[i],
										\busini, this.scInBus[i], // use "index" method?
										\level, level[i]], revGlobalBF,
										addAction: \addBefore).onFree({espacializador[i].free;
											espacializador[i] = nil; synt[i] = nil});
								};
								
								
								espacializador[i] = Synth.new(\espacAmbChowning++ln[i], [\inbus, mbus[i], 
									\gbus, gbus, \dopon, doppler[i]], 
									synt[i], addAction: \addAfter);
							};

						};
						
						atualizarvariaveis.value;
						
						



						
					}
					{ this.ncan[i] == 2 } {
						"Stereo!".postln;
						ncanais[i] = 0; // just in case!
						angle[i] = pi/2;
						{angnumbox.value = pi/2;}.defer;
						{angslider.value = 0.5;}.defer;
						
						


						if(rv[i] == 1) {	


							if (testado[i] == false) {

								if(revGlobalSoa.isNil) {
									revGlobalSoa = Synth.new(\revGlobalSoaA12, [\soaBus, soaBus],
										revGlobalBF, addAction:\addBefore);
								};
								if (hwncheck[i].value) {
									synt[i] = Synth.new(\playStereoHWBus, [\outbus, sbus[i], \busini, this.busini[i],
										\level, level[i]], revGlobalSoa,
										addAction: \addBefore).onFree({espacializador[i].free;
											espacializador[i] = nil; synt[i] = nil});
								} {
									synt[i] = Synth.new(\playStereoSWBus, [\outbus, sbus[i],
										\busini, this.scInBus[i],
										\level, level[i]], revGlobalSoa,
										addAction: \addBefore).onFree({espacializador[i].free;
											espacializador[i] = nil; synt[i] = nil});
								};
								
								espacializador[i] = Synth.new(\espacAmbEstereoAFormat++ln[i], [\inbus, sbus[i], \gbus, gbus,
									\soaBus, soaBus, \dopon, doppler[i]], 
									synt[i], addAction: \addAfter);
							};


						} {
							if (hwncheck[i].value) {
								synt[i] = Synth.new(\playStereoHWBus, [\outbus, sbus[i], \busini, this.busini[i],
									\level, level[i]], revGlobalBF,
									addAction: \addBefore).onFree({espacializador[i].free;
										espacializador[i] = nil; synt[i] = nil});
							} {
								synt[i] = Synth.new(\playStereoSWBus, [\outbus, sbus[i],
									\busini, this.scInBus[i],
									\level, level[i]], revGlobalBF,
									addAction: \addBefore).onFree({espacializador[i].free;
										espacializador[i] = nil; synt[i] = nil});
							};
							
							espacializador[i] = Synth.new(\espacAmbEstereoChowning++ln[i], [\inbus, sbus[i],
								\gbus, gbus, \dopon, doppler[i]], 
								synt[i], addAction: \addAfter);


						};
						atualizarvariaveis.value;
						
						



						
					}
					{ this.ncan[i] == 4 } {
						//"B-format!".postln;
						
						"B-format ambisonic!!!".postln;
						//	~revGlobal = Synth.new(\revGlobalAmb, [\gbus, gbus], addAction:\addToTail);
						// reverb for non-contracted (full b-format) component

						
						if(rv[i] == 1) {

							if(revGlobalSoa == nil) {
								revGlobalSoa = Synth.new(\revGlobalSoaA12, [\soaBus, soaBus],
									revGlobalBF, addAction:\addBefore);
							};
							
							if (testado[i] == false) {
								if (hwncheck[i].value) {
									synt[i] = Synth.new(\playBFormatHWBus++ln[i], [\gbfbus, gbfbus, \outbus, mbus[i],
										\contr, clev[i], \rate, 1, \tpos, tpos, \level, level[i], \dopon, doppler[i],
										\busini, this.busini[i]], 
										revGlobalSoa, addAction: \addBefore).onFree({espacializador[i].free;
											espacializador[i] = nil; synt[i] = nil;});
								} {
									synt[i] = Synth.new(\playBFormatSWBus++ln[i], [\gbfbus, gbfbus, \outbus,
										mbus[i], \contr, clev[i], \rate, 1, \tpos, tpos, \level,
										level[i], \dopon, doppler[i],
										\busini, this.scInBus[i] ], 
										revGlobalSoa, addAction: \addBefore).onFree({espacializador[i].free;
											espacializador[i] = nil; synt[i] = nil;});
								};
								
								espacializador[i] = Synth.new(\espacAmb2AFormat++ln[i], [\inbus, mbus[i],
									\gbus, gbus, \soaBus, soaBus, \dopon, doppler[i]], 
									synt[i], addAction: \addAfter);
							};

						} {

							if (testado[i] == false) {
								if (hwncheck[i].value) {
									synt[i] = Synth.new(\playBFormatHWBus++ln[i], [\gbfbus, gbfbus, \outbus,
										mbus[i], \contr, clev[i], \rate, 1, \tpos, tpos, \level,
										level[i], \dopon, doppler[i],
										\busini, this.busini[i]], 
										revGlobalBF, addAction: \addBefore).onFree({espacializador[i].free;
											espacializador[i] = nil; synt[i] = nil;
											playingBF[i] = false});
								} {
									synt[i] = Synth.new(\playBFormatSWBus++ln[i], [\gbfbus, gbfbus, \outbus,
										mbus[i], \contr, clev[i], \rate, 1, \tpos, tpos, \level,
										level[i], \dopon, doppler[i],
										\busini, this.scInBus[i] ], 
										revGlobalBF, addAction: \addBefore).onFree({espacializador[i].free;
											espacializador[i] = nil; synt[i] = nil;});
								};
								
								espacializador[i] = Synth.new(\espacAmb2Chowning++ln[i], [\inbus, mbus[i],
									\gbus, gbus, \dopon, doppler[i]], 
									synt[i], addAction: \addAfter);
							};
							
						};
						

						

						
						atualizarvariaveis.value;
						
						




						
					};
					
					
				};
				
				
				
			};
			
		};
		
		
		
		btestar = Button(win, Rect(280, 90, 90, 20))
		.states_([
			["audition", Color.black, Color.white],
			["stop", Color.white, Color.red]
		])
		.action_({ arg but;
			//	var testado = fatual;
			//	but.value.postln;
			{ if(isPlay.not) {
				if(but.value == 1)
				{
					
					runTrigger.value(fatual);
					//	("FATUAL + " ++ fatual).postln;

					//atualizarvariaveis.value;
					tocar.value(fatual, 0);
					//		~testado = fatual;
					testado[fatual] = true;
					
					//win.drawFunc.value;	
				}
				{
					
					runStop.value(fatual);
					synt[fatual].free;
					synt[fatual] = nil;
					testado[fatual] = false;
					
					
				};
			} {
				but.value = 0;
			}
			}.defer;
			
		});
		
		~win = win;
		
// save automation - adapted from chooseDirectoryDialog in AutomationGui.sc
		
		bsalvar = Button(win, Rect(10, this.width - 40, 90, 20))
		.states_([
			["save auto", Color.black, Color.white],
			
		])
		.action_({
			//arg but;

			var title="Save: select automation dir", onSuccess, onFailure=nil,
			preset=nil, bounds,  dwin, textField, success=false;
			bounds = Rect(100,300,300,30);
			if(prjDr.isNil && lastAutomation.isNil) {
				preset = "HOME".getenv ++ "/auto/"; } {
					if (lastAutomation.isNil) {
						preset = prjDr ++ "/auto/";
					} {
						preset = lastAutomation;
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
				
				controle.seek; // rewind to zero
				controle.snapshot; // and take snapshot
				("FILE IS " ++ textField.value).postln;
				("mkdir -p" + textField.value).systemCmd;
				controle.save(textField.value);
				lastAutomation = textField.value;

            };
            dwin.front;

			//			controle.seek;
			//			controle.snapshot; // rewind to zero
			//			controle.save(controle.presetDir);
			
			
		});
		
		
		// load automation - adapted from chooseDirectoryDialog in AutomationGui.sc
		
		bcarregar = Button(win, Rect(100, this.width - 40, 90, 20))
		.states_([
			["load auto", Color.black, Color.white],
		])
		.action_({
			//arg but;
			var title="Select Automation directory", onSuccess, onFailure=nil,
			preset=nil, bounds,  dwin, textField, success=false;
			bounds = Rect(100,300,300,30);
			if(prjDr.isNil && lastAutomation.isNil) {
				preset = "HOME".getenv ++ "/auto/"; } {
					if(lastAutomation.isNil) {
						preset = prjDr ++ "/auto/";
					} {
						preset = lastAutomation;
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
				controle.seek;
				lastAutomation = textField.value;
            };
            dwin.front;
			
			//			controle.load(controle.presetDir);
		});
		
		
		
		
		
		
		win.view.background = Color(0.7,0.8,0.8);
		
		
		win.drawFunc = {
			//paint origin
			Pen.fillColor = Color(0.6,0.8,0.8);
			Pen.addArc(this.halfwidth@this.halfwidth, this.halfwidth, 0, 2pi);
			Pen.fill;
			//Pen.width = 10;

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
			rlev[fatual].postln;
			dirslider.value = dlev[fatual] / (pi/2);
			dirnumbox.value = dlev[fatual];
			cslider.value = clev[fatual];
			zslider.value = (zlev[fatual] + this.halfwidth) / this.width;
			("Z-lev = " ++  zlev[fatual]).postln;
			
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
				if (synt[fatual] == nil){
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
			("Doppler is " ++ butt.value).postln;
			{dcheck[fatual].valueAction = butt.value;}.defer;
		});
		dopcheque.value = false;
		
		loopcheck = CheckBox( win, Rect(184, 10, 80, 20), "Loop").action_({ arg butt;
			("Loop is " ++ butt.value).postln;
			{lpcheck[fatual].valueAction = butt.value;}.defer;
		});
		loopcheck.value = false;
		
		revcheck = CheckBox( win, Rect(250, 10, 180, 20), "2nd order diffuse reverb").action_({ arg butt;
			("A-format rev is " ++ butt.value).postln;
			{rvcheck[fatual].valueAction = butt.value;}.defer;
		});
		revcheck.value = false;

		lincheck = CheckBox( win, Rect(184, 30, 180, 20), "Linear intensity").action_({ arg butt;
			("Linear intensity is " ++ butt.value).postln;
			{lncheck[fatual].valueAction = butt.value;}.defer;
		});
		lincheck.value = false;

		
		hwInCheck = CheckBox( win, Rect(10, 30, 100, 20), "HW-in").action_({ arg butt;
			{hwncheck[fatual].valueAction = butt.value;}.defer;
			if (hwInCheck.value && scInCheck.value) {
				//scInCheck.value = false;
			};
		});

		scInCheck = CheckBox( win, Rect(104, 30, 60, 20), "SC-in").action_({ arg butt;
			{scncheck[fatual].valueAction = butt.value;}.defer;
			if (scInCheck.value && hwInCheck.value) {
				//hwInCheck.value = false;
			};
		});



		
		dopcheque.value = false;
		
		
		
		//	~ncantexto = StaticText(~win, Rect(55, -10 + ~offset, 200, 20));
		//	~ncantexto.string = "No. of chans. (HW & SC-in)";
		textbuf = StaticText(win, Rect(55, -10 + offset, 200, 20));
		textbuf.string = "No. of chans. (HW & SC-in)";
		ncannumbox = NumberBox(win, Rect(10, -10 + offset, 40, 20));
		ncannumbox.value = 0;
		ncannumbox.clipHi = 4;
		ncannumbox.clipLo = 0;
		//angnumbox.step_(0.1); 
		//angnumbox.scroll_step=0.1;
		ncannumbox.align = \center;
		ncannumbox.action = {arg num;
			
			
			{ncanbox[fatual].valueAction = num.value;}.defer;
			this.ncan[fatual] = num.value;
			
		};
		
		
		
		textbuf = StaticText(win, Rect(55, 10 + offset, 240, 20));
		textbuf.string = "Start Bus (HW-in)";
		busininumbox = NumberBox(win, Rect(10, 10 + offset, 40, 20));
		busininumbox.value = 0;
		//		busininumbox.clipHi = 31;
		busininumbox.clipLo = 0;
		//angnumbox.step_(0.1); 
		//angnumbox.scroll_step=0.1;
		busininumbox.align = \center;
		busininumbox.action = {arg num; 
			{businibox[fatual].valueAction = num.value;}.defer;
			this.busini[fatual] = num.value;
		};
		
		
		
		
		textbuf = StaticText(win, Rect(163, 110 + offset, 90, 20));
		textbuf.string = "Angle (Stereo)";
		angnumbox = NumberBox(win, Rect(10, 110 + offset, 40, 20));
		angnumbox.value = 0;
		angnumbox.clipHi = pi;
		angnumbox.clipLo = 0;
		angnumbox.step_(0.1); 
		angnumbox.scroll_step=0.1;
		angnumbox.align = \center;
		angnumbox.action = {arg num; 
			{abox[fatual].valueAction = num.value;}.defer;
			if((ncanais[fatual]==2) || (this.ncan[fatual]==2)){
				espacializador[fatual].set(\angle, num.value);
				this.setSynths(fatual, \angle, num.value);
				angle[fatual] = num.value;
				("ângulo = " ++ num.value).postln; 
			}
			{angnumbox.value = 0;};
		};
		
		angslider = Slider.new(win, Rect(50, 110 + offset, 110, 20));
		//	b = ControlSpec(0.0, 3.14, \linear, 0.01); // min, max, mapping, step
		
		angslider.action = {arg num;
			{abox[fatual].valueAction = num.value * pi;}.defer;
			if((ncanais[fatual]==2) || (this.ncan[fatual]==2)) {
				{angnumbox.value = num.value * pi;}.defer;
				//			espacializador[fatual].set(\angle, b.map(num.value));
				espacializador[fatual].set(\angle, num.value * pi);
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
			if(ncanais[fatual]==2){
				espacializador[fatual].set(\elev, num.value);
				this.setSynths(fatual, \elev, num.value);
				zlev[fatual] = num.value;
				("Z-Axis = " ++ num.value).postln; 
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
			
			//		synt[fatual].set(\level, num.value);
			//		level[fatual] = num.value;
			//		("level = " ++ num.value).postln; 
		};
		// stepsize?
		volslider = Slider.new(win, Rect(50, 30 + offset, 110, 20));
		volslider.value = 0;
		volslider.action = {arg num;
			{vbox[fatual].valueAction = num.value;}.defer;
			
			//		volnumbox.value = num.value;
			//		synt[fatual].set(\level, num.value);
			//		level[fatual] = num.value;
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
		textbuf.string = "Proximal Reverb";
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
		
		

		textbuf = StaticText(win, Rect(163, 130 + offset, 150, 20));
		textbuf.string = "Rotation (B-Format)";
		rnumbox = NumberBox(win, Rect(10, 130 + offset, 40, 20));
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
		rslider = Slider.new(win, Rect(50, 130 + offset, 110, 20));
		rslider.value = 0.5;
		rslider.action = {arg num;
			{rbox[fatual].valueAction = num.value * 6.28 - pi;}.defer;
			{rnumbox.value = num.value * 2pi - pi;}.defer;
			
		};
		
		

		textbuf = StaticText(win, Rect(163, 150 + offset, 150, 20));
		textbuf.string = "Directivity (B-Format)";
		dirnumbox = NumberBox(win, Rect(10, 150 + offset, 40, 20));
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
		dirslider = Slider.new(win, Rect(50, 150 + offset, 110, 20));
		dirslider.value = 0;
		dirslider.action = {arg num;
			{dbox[fatual].valueAction = num.value * pi/2;}.defer;
			{dirnumbox.value = num.value * pi/2;}.defer;
		};

		

		textbuf = StaticText(win, Rect(163, 170 + offset, 150, 20));
		textbuf.string = "Contraction (B-Format)";
		connumbox = NumberBox(win, Rect(10, 170 + offset, 40, 20));
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
		cslider = Slider.new(win, Rect(50, 170 + offset, 110, 20));
		cslider.value = 0;
		cslider.action = {arg num;
			{cbox[fatual].valueAction = num.value;}.defer;
			{connumbox.value = num.value;}.defer;
		};
		
		

		
		
		bload = Button(win, Rect(220, 50, 60, 20))
		.states_([
			["load", Color.black, Color.white],
		])
		.action_({ arg but;
			//var filebuf, filebuf1, filebuf2;
			but.value.postln;
			synt[fatual].free; // error check
			espacializador[fatual].free;
			dopcheque.value = false; // coloque toggle no padrão
			
			

			Dialog.openPanel({ 
				arg path;

				{tfield[fatual].valueAction = path;}.defer;
				controle.seek;  // need to take an Automation snapshot at zero or we'll lose
				controle.snapshot; // this filename on next rewind of transport
				// perhaps we should resume prior transport position afterwards
				

			}, 
				{
					"cancelado".postln;
					{tfield[fatual].value = "";}.defer;
					
				}
			);	
		});

		bnodes = Button(win, Rect(220, 70, 60, 20))
		.states_([
			["nodes", Color.black, Color.white],
		])
		.action_({
			server.plotTree;
		});

		textbuf = StaticText(wdados, Rect(10, 20, 50, 20));
		textbuf.font = Font(Font.defaultSansFace, 9);
		textbuf.string = "Dp";
		textbuf = StaticText(wdados, Rect(25, 20, 50, 20));
		textbuf.font = Font(Font.defaultSansFace, 9);
		textbuf.string = "Lp";
		textbuf = StaticText(wdados, Rect(40, 20, 50, 20));
		textbuf.font = Font(Font.defaultSansFace, 9);
		textbuf.string = "Rv";
		textbuf = StaticText(wdados, Rect(55, 20, 50, 20));
		textbuf.font = Font(Font.defaultSansFace, 9);
		textbuf.string = "Hw";
		textbuf = StaticText(wdados, Rect(70, 20, 50, 20));
		textbuf.font = Font(Font.defaultSansFace, 9);
		textbuf.string = "Sc";
		textbuf = StaticText(wdados, Rect(85, 20, 50, 20));
		textbuf.font = Font(Font.defaultSansFace, 9);
		textbuf.string = "Ln";

		textbuf = StaticText(wdados, Rect(100, 20, 50, 20));
		textbuf.font = Font(Font.defaultSansFace, 9);
		textbuf.string = "NCan";
		textbuf = StaticText(wdados, Rect(125, 20, 50, 20));
		textbuf.font = Font(Font.defaultSansFace, 9);
		textbuf.string = "SBus";

		textbuf = StaticText(wdados, Rect(150, 20, 50, 20));
		textbuf.font = Font(Font.defaultSansFace, 9);
		textbuf.string = "X";
		textbuf = StaticText(wdados, Rect(190, 20, 50, 20));
		textbuf.font = Font(Font.defaultSansFace, 9);
		textbuf.string = "Y";

		textbuf = StaticText(wdados, Rect(230, 20, 50, 20));
		textbuf.font = Font(Font.defaultSansFace, 9);
		textbuf.string = "Z";

		
		textbuf = StaticText(wdados, Rect(270, 20, 50, 20));
		textbuf.font = Font(Font.defaultSansFace, 9);
		textbuf.string = "Lev";
		textbuf = StaticText(wdados, Rect(295, 20, 50, 20));
		textbuf.font = Font(Font.defaultSansFace, 9);
		textbuf.string = "DAmt";
		textbuf = StaticText(wdados, Rect(320, 20, 50, 20));
		textbuf.font = Font(Font.defaultSansFace, 9);
		textbuf.string = "Prox";
		textbuf = StaticText(wdados, Rect(345, 20, 50, 20));
		textbuf.font = Font(Font.defaultSansFace, 9);
		textbuf.string = "Dist";
		textbuf = StaticText(wdados, Rect(370, 20, 50, 20));
		textbuf.font = Font(Font.defaultSansFace, 9);
		textbuf.string = "Ang";
		textbuf = StaticText(wdados, Rect(395, 20, 50, 20));
		textbuf.font = Font(Font.defaultSansFace, 9);
		textbuf.string = "Rot";
		textbuf = StaticText(wdados, Rect(420, 20, 50, 20));
		textbuf.font = Font(Font.defaultSansFace, 9);
		textbuf.string = "Dir";
		textbuf = StaticText(wdados, Rect(445, 20, 50, 20));
		textbuf.font = Font(Font.defaultSansFace, 9);
		textbuf.string = "Cont";

		textbuf = StaticText(wdados, Rect(470, 20, 50, 20));
		textbuf.font = Font(Font.defaultSansFace, 9);
		textbuf.string = "A1";
		textbuf = StaticText(wdados, Rect(495, 20, 50, 20));
		textbuf.font = Font(Font.defaultSansFace, 9);
		textbuf.string = "A2";
		textbuf = StaticText(wdados, Rect(520, 20, 50, 20));
		textbuf.font = Font(Font.defaultSansFace, 9);
		textbuf.string = "A3";
		textbuf = StaticText(wdados, Rect(545, 20, 50, 20));
		textbuf.font = Font(Font.defaultSansFace, 9);
		textbuf.string = "A4";
		textbuf = StaticText(wdados, Rect(570, 20, 50, 20));
		textbuf.font = Font(Font.defaultSansFace, 9);
		textbuf.string = "A5";


		textbuf = StaticText(wdados, Rect(595, 20, 50, 20));
		textbuf.font = Font(Font.defaultSansFace, 9);
		textbuf.string = "File";

		
		this.nfontes.do { arg i;	
			dcheck[i] = CheckBox.new( wdados, Rect(10, 40 + (i*20), 40, 20))
			.action_({ arg but;
				if(i==fatual){dopcheque.value = but.value;};
				if (but.value == true) {
					//	"Aqui!!!".postln;
					doppler[i] = 1;
					espacializador[i].set(\dopon, 1);
					this.setSynths(i, \dopon, 1);
				}{
					doppler[i] = 0;
					espacializador[i].set(\dopon, 0);
					this.setSynths(i, \dopon, 0);
				};
			});

			lpcheck[i] = CheckBox.new( wdados, Rect(25, 40 + (i*20), 40, 20))
			.action_({ arg but;
				if(i==fatual){loopcheck.value = but.value;};
				if (but.value == true) {
					//	"Aqui!!!".postln;
					lp[i] = 1;
					synt[i].set(\lp, 1);
					this.setSynths(i, \lp, 1);
				}{
					lp[i] = 0;
					synt[i].set(\lp, 0);
					this.setSynths(i, \lp, 0);
				};
			});

			rvcheck[i] = CheckBox.new( wdados, Rect(40, 40 + (i*20), 40, 20))
			.action_({ arg but;
				if(i==fatual){revcheck.value = but.value;};
				if (but.value == true) {
					//	"Aqui!!!".postln;
					rv[i] = 1;
					//synt[i].set(\lp, 1);
					this.setSynths(i, \rv, 1);
				}{
					rv[i] = 0;
					//synt[i].set(\lp, 0);
					this.setSynths(i, \rv, 0);
				};
			});


			hwncheck[i] = CheckBox.new( wdados, Rect(55, 40 + (i*20), 40, 20))
			.action_({ arg but;
				if(i==fatual){hwInCheck.value = but.value;};
				if (but.value == true) {
					scncheck[i].value = false;
					if(i==fatual){scInCheck.value = false;};
					hwn[i] = 1;
					scn[i] = 0;
					synt[i].set(\hwn, 1);
				}{
					hwn[i] = 0;
					synt[i].set(\hwn, 0);
				};
			});
			
			scncheck[i] = CheckBox.new( wdados, Rect(70, 40 + (i*20), 40, 20))
			.action_({ arg but;
				if(i==fatual){scInCheck.value = but.value;};
				if (but.value == true) {
					hwncheck[i].value = false;
					if(i==fatual){hwInCheck.value = false;};
					scn[i] = 1;
					hwn[i] = 0;
					synt[i].set(\scn, 1);
				}{
					scn[i] = 0;
					synt[i].set(\scn, 0);
				};
			});

						
			lncheck[i] = CheckBox.new( wdados, Rect(85, 40 + (i*20), 40, 20))
			.action_({ arg but;
				if(i==fatual){lincheck.value = but.value;};
				if (but.value == true) {
					//	"Aqui!!!".postln;
					ln[i] = "_linear";
					//synt[i].set(\lp, 1);
					this.setSynths(i, \ln, 1);
				}{
					ln[i] = "";
					//synt[i].set(\lp, 0);
					this.setSynths(i, \ln, 0);
				};
			});

			
			ncanbox[i] = NumberBox(wdados, Rect(100, 40 + (i*20), 25, 20));
			businibox[i] = NumberBox(wdados, Rect(125, 40 + (i*20), 25, 20));

			xbox[i] = NumberBox(wdados, Rect(150, 40 + (i*20), 40, 20));
			ybox[i] = NumberBox(wdados, Rect(190, 40+ (i*20), 40, 20));
			zbox[i] = NumberBox(wdados, Rect(230, 40+ (i*20), 40, 20));

			vbox[i] = NumberBox(wdados, Rect(270, 40 + (i*20), 25, 20));
			dpbox[i] = NumberBox(wdados, Rect(295, 40+ (i*20), 25, 20));
			gbox[i] = NumberBox(wdados, Rect(320, 40+ (i*20), 25, 20));
			lbox[i] = NumberBox(wdados, Rect(345, 40+ (i*20), 25, 20));
			abox[i] = NumberBox(wdados, Rect(370, 40+ (i*20), 25, 20));
			rbox[i] = NumberBox(wdados, Rect(395, 40+ (i*20), 25, 20));
			dbox[i] = NumberBox(wdados, Rect(420, 40+ (i*20), 25, 20));
			cbox[i] = NumberBox(wdados, Rect(445, 40+ (i*20), 25, 20));

			a1box[i] = NumberBox(wdados, Rect(470, 40+ (i*20), 25, 20));
			a2box[i] = NumberBox(wdados, Rect(495, 40+ (i*20), 25, 20));
			a3box[i] = NumberBox(wdados, Rect(520, 40+ (i*20), 25, 20));
			a4box[i] = NumberBox(wdados, Rect(545, 40+ (i*20), 25, 20));
			a5box[i] = NumberBox(wdados, Rect(570, 40+ (i*20), 25, 20));


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

			tfield[i] = TextField(wdados, Rect(595, 40+ (i*20), 350, 20));


			
			ncanbox[i].font = Font(Font.defaultSansFace, 9);
			businibox[i].font = Font(Font.defaultSansFace, 9);
			xbox[i].font = Font(Font.defaultSansFace, 9);
			ybox[i].font = Font(Font.defaultSansFace, 9);
			zbox[i].font = Font(Font.defaultSansFace, 9);
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

			tfield[i].font = Font(Font.defaultSansFace, 9);
			
			xbox[i].decimals = 4;
			ybox[i].decimals = 4;
			zbox[i].decimals = 4;


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

			
			tfield[i].action = {arg path;
				if (path != "") {
					
					sombuf[i] = Buffer.read(server, path.value, action: {arg buf; 
						((buf.numFrames ) / buf.sampleRate).postln;
						//				(buf.sampleRate).postln;
					});
				}
				
			};
			
			xbox[i].action = {arg num;
				sprite[i, 1] = this.halfwidth + (num.value * -1 * this.halfwidth);
				novoplot.value(num.value, ybox[i], i, this.nfontes);
				//xval[i] = num.value;
				xval[i] = num.value;
				if (xval[i] > 1) {xval[i] = 1};
				if (xval[i] < -1) {xval[i] = -1};
				if(espacializador[i].notNil || playingBF[i]){
					espacializador[i].set(\mx, xval[i]);
					this.setSynths(i, \mx, xval[i]);
					synt[i].set(\mx, xval[i]);
					//xval[i].postln;
				};
				
				
			};
			ybox[i].action = {arg num; 
				sprite[i, 0] = ((num.value * this.halfwidth * -1) + this.halfwidth);
				yval[i] = num.value;
				yval[i] = num.value;
				if (yval[i] > 1) {yval[i] = 1};
				if (yval[i] < -1) {yval[i] = -1};

				if(espacializador[i].notNil || playingBF[i]){
					espacializador[i].set(\my, yval[i]);
					this.setSynths(i, \my, yval[i]);
					synt[i].set(\my, yval[i]);
				};		
				
			};

			zbox[i].action = {arg num;
				espacializador[i].set(\mz, num.value);
				//	zval[i] = num.value;
				zval[i] = num.value / this.halfwidth;
				if (zval[i] > 1) {zval[i] = 1};
				if (zval[i] < -1) {zval[i] = -1};
				
				this.setSynths(i, \mz, zval[i]);
				//"fooooob".postln;
				synt[i].set(\mz, zval[i]);
				//synt[i].set(\elev, num.value);
				zlev[i] = zval[i];
				if(i == fatual) 
				{
					//var val = (this.halfwidth - (num.value * width)) * -1;
					//					zslider.value = (num.value + this.halfwidth) / this.width;
					zslider.value = (num.value + 1) / 2;
					znumbox.value = num.value;
				};
			};
			
			
			dcheck[i].value = 0;
			
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
				//	("Ângulo = " ++ num.value ++ " radianos").postln;
				angle[i] = num.value;
				if((ncanais[i]==2) || (this.ncan[i]==2)){
					espacializador[i].set(\angle, num.value);
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
				synt[i].set(\level, num.value);
				this.setSynths(i, \level, num.value);
				level[i] = num.value;
				//	("level = " ++ num.value).postln; 
				if(i == fatual) 
				{
					volslider.value = num.value;
					volnumbox.value = num.value;
				};
			}; 





			gbox[i].value = 0;
			lbox[i].value = 0;
			
			gbox[i].action = {arg num;
				espacializador[i].set(\glev, num.value);
				this.setSynths(i, \glev, num.value);

				synt[i].set(\glev, num.value);
				glev[i] = num.value;
				if(i == fatual) 
				{
					gslider.value = num.value;
					gnumbox.value = num.value;
				};
			}; 
			
			
			lbox[i].action = {arg num;
				espacializador[i].set(\llev, num.value);
				this.setSynths(i, \llev, num.value);
				synt[i].set(\llev, num.value);
				llev[i] = num.value;
				if(i == fatual) 
				{
					lslider.value = num.value;
					lnumbox.value = num.value;
				};
			}; 


			rbox[i].action = {arg num; 
				
				synt[i].set(\rotAngle, num.value);
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
				//	num.value.postln;
				synt[i].set(\directang, num.value);
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
				//	num.value.postln;
				//	"cbox!".postln;
				synt[i].set(\contr, num.value);
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
				synt[i].set(\dopamnt, num.value);
				this.setSynths(i, \dopamnt, num.value);
				// used for the others
				espacializador[i].set(\dopamnt, num.value);
				dplev[i] = num.value;
				if(i == fatual) 
				{
					dpslider.value = num.value;
					dopnumbox.value = num.value;
				};
			};


			// CHECK THESE NEXT 2
			ncanbox[i].action = {arg num;
				espacializador[i].set(\mz, num.value);
				this.setSynths(i, \mz, num.value);
				synt[i].set(\mz, num.value);
				this.ncan[i] = num.value;
				if(i == fatual )
				{
					//var val = (this.halfwidth - (num.value * width)) * -1;
					//	ncanslider.value = num.value;
					ncannumbox.value = num.value;
				};
			}; 
			businibox[i].action = {arg num;
				espacializador[i].set(\mz, num.value);
				this.setSynths(i, \mz, num.value);
				synt[i].set(\mz, num.value);
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
		~autotest = controle = Automation(this.dur, showLoadSave: false, minTimeStep: 0.001).front(win,
			Rect(10, this.width - 80, 400, 22));
		controle.presetDir = prjDr ++ "/auto";
		//controle.setMinTimeStep(2.0);
		controle.onEnd = {
			controle.stop;
			"controle is stopped".postln;
			controle.seek;
			this.nfontes.do { arg i;	
				synt[i].free; // error check
				
			};
		};
		
		controle.onPlay = {
			var startTime;
			//	runTriggers.value;
			if(controle.now < 0)
			{
				startTime = 0
			}
			{ 
				startTime = controle.now
			};
			this.nfontes.do { arg i;	
				var loaded, dur, looped;
				if(testado[i].not) {
					{runTrigger.value(i);}.defer;
				};
				{loaded = tfield[i].value;}.defer;
				looped = lp[i];
				if(lp[i] != 1){
					{tocar.value(i, startTime);}.defer;
				}			
				{
					if(sombuf[i].notNil){
						var dur = sombuf[i].numFrames / sombuf[i].sampleRate;
						{tocar.value(i, dur.rand);}.defer;
					}
				};
				//runTrigger.value(i);
				//updateSynthInArgs.value(i);
			};
			
			isPlay = true;
			//runTriggers.value;
		};
		
		
		controle.onSeek = {
			//	("onSeek = " ++ ~controle.now).postln;
			if(isPlay == true) {
				this.nfontes.do { arg i;	
					synt[i].free; // error check
				};
			};
		};

		controle.onStop = {
			runStops.value;
			this.nfontes.do { arg i;
				// if sound is currently being "tested", don't switch off on stop
				// leave that for user
				if (testado[i] == false) {
					synt[i].free; // error check
				};
				//	espacializador[i].free;
			};
			isPlay = false;
			/*			if(revGlobal.notNil){
				revGlobal.free;
				revGlobal = nil;
			};
			if(revGlobalBF.notNil){
				revGlobalBF.free;
				revGlobalBF = nil;
			};
			if(revGlobalSoa.notNil){
				revGlobalSoa.free;
				revGlobalSoa = nil;
			};
			*/
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
			
			controle.dock(tfield[i], "filename_" ++ i);
			controle.dock(dcheck[i], "doppler_" ++ i);			
			controle.dock(lpcheck[i], "loop_" ++ i);
			controle.dock(hwncheck[i], "hwin_" ++ i);
			controle.dock(ncanbox[i], "numchannels_" ++ i);
			controle.dock(businibox[i], "busini_" ++ i);
			controle.dock(scncheck[i], "scin_" ++ i);
			controle.dock(rvcheck[i], "rev_" ++ i);
			controle.dock(lncheck[i], "linear_" ++ i);

			controle.dock(a1box[i], "aux1_" ++ i);
			controle.dock(a2box[i], "aux2_" ++ i);
			controle.dock(a3box[i], "aux3_" ++ i);
			controle.dock(a4box[i], "aux4_" ++ i);
			controle.dock(a5box[i], "aux5_" ++ i);

			
			
		};

		
		
		win.view.mouseMoveAction = {|view, x, y, modifiers | [x, y];

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
			this.nfontes.do { arg x;
				espacializador[x].free;
				mbus[x].free;
				sbus[x].free;
				//	bfbus.[x].free;
				sombuf[x].free;
				synt[x].free;
				MIDIIn.removeFuncFrom(\sysex, sysex);
				this.scInBus[x].free;
				//		kespac[x].stop;
			};

			if(revGlobal.notNil){
				revGlobal.free;
			};
			if(revGlobalBF.notNil){
				revGlobalBF.free;
			};
			if(revGlobalSoa.notNil){
				revGlobalSoa.free;
			};
			
			
			wdados.close;
			waux.close;
			gbus.free;
			gbfbus.free;
			rirWspectrum.free;
			rirXspectrum.free;
			rirYspectrum.free;
			rirZspectrum.free;
			rirFLUspectrum.free;
			rirFRDspectrum.free;
			rirBLDspectrum.free;
			rirBRUspectrum.free;
			soaBus.free;
			12.do { arg i;
				rirA12Spectrum[i].free;
			};
			
			
		});

		mmcslave = CheckBox( win, Rect(195, this.width - 40, 140, 20), "Slave to MMC").action_({ arg butt;
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


		sysex  = { arg src, sysex;
			//	("Sysex is: " ++ sysex ++ " e src = " ++ src).postln;
			//~lastsysex = sysex;
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
		}
	}


}