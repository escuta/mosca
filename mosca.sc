// SuperCollider class by Iain Mott, 2016. Licensed under a 
// Creative Commons Attribution-NonCommercial 4.0 International License
// http://creativecommons.org/licenses/by-nc/4.0/
// The class makes extensive use of the Ambisonic Toolkit (http://www.ambisonictoolkit.net/)
// and the Automation quark (https://github.com/supercollider-quarks/Automation).

// Required Quarks : Automation, Ctk, XML, MathLib
// Required classes: 
// SC Plugins: https://github.com/supercollider/sc3-plugins
// RIRs (first 100ms silenced) used in help files:
// http://www.openairlib.net/auralizationdb/content/central-hall-university-york
// http://www.openairlib.net/auralizationdb/content/koli-national-park-summer


Mosca {
	//	classvar fftsize = 2048; 
   	var <>myTestVar;
	var  <>kernelSize, <>scale, <>rirW, <>rirX, <>rirY, <>rirZ,
	<>rirWspectrum, <>rirXspectrum, <>rirYspectrum, <>rirZspectrum,
	//	<>rirWXYZspectrum,  rirWXYZ, // experimental!
	rirFLUspectrum, rirFRDspectrum, rirBLDspectrum, rirBRUspectrum,

	<>irbuffer, <>bufsize, <>win, <>wdados, <>sprite, <>nfontes,
	<>controle, <>revGlobal, <>revGlobalBF, <>m, <>offset, <>textbuf, <>controle,
	<>sysex, <>mmcslave,
	<>synthRegistry, <>busini, <>ncan, <>swinbus,
	<>dec,
	<>triggerFunc, <>stopFunc,
	<>gbfbus, <>scInBus;
	classvar server, rirW, rirX, rirY, rirZ,
	rirFLU, rirFRD, rirBLD, rirBRU,
	bufsize, irbuffer,
	b2a, a2b,

	soa_a12_decoder_matrix, soa_a12_encoder_matrix,
	cart, spher, foa_a12_decoder_matrix,

	o, //debugging
	prjDr;
	classvar fftsize = 2048, server;

	*new { arg projDir, nsources = 1, rirWXYZ, srvr, decoder = nil;
		^super.new.initMosca(projDir, nsources, rirWXYZ, srvr, decoder);
	}

	*printSynthParams {
		var string =
		"

GUI Parameters usable in SynthDefs

\\level | level | 0 - 1 |
\\dopon | Doppler effect on/off | 0 or 1
\\dopamnt | Doppler ammount | 0 - 1 |
\\angle | Stereo angle | default 1.05 (60 degrees) | 0 - 3.14 |
\\glev | Global reverb level | 0 - 1 |
\\llev | Local reverb level | 0 - 1 |
\\mx | X coord | -450 - 450 |
\\my | Y coord | -450 - 450 |
\\mz | Z coord | -450 - 450 |
\\rotAngle | B-format rotation angle | -3.14 - 3.14 |
\\\directang | B-format directivity | 0 - 1.57 |
\\contr | Contraction: fade between WXYZ & W | 0 - 1 |
";
		^string;
		
	}

	initMosca { arg projDir, nsources, rirWXYZ, srvr, decoder;
		var makeSynthDefPlayers, revGlobTxt,
		espacAmbOutFunc, espacAmbEstereoOutFunc, revGlobalAmbFunc,
		playBFormatOutFunc, playMonoInFunc, playStereoInFunc, playBFormatInFunc,
		bufAformat, bufWXYZ,
		//synthRegistry = List[],
		
		testit; // remove at some point with other debugging stuff
		b2a = FoaDecoderMatrix.newBtoA;
		a2b = FoaEncoderMatrix.newAtoB;


		this.nfontes = nsources;
		playMonoInFunc = Array.newClear(3); // one for File, Stereo & BFormat;
		playStereoInFunc = Array.newClear(3);
		playBFormatInFunc = Array.newClear(3);

		this.synthRegistry = Array.newClear(this.nfontes);
		this.nfontes.do { arg x;
			this.synthRegistry[x] = List[];
		};

		// Note. this will replace swinbus 
		this.scInBus = Array.newClear(this.nfontes);
		this.nfontes.do { arg x;
			this.scInBus[x] = Bus.audio(srvr, 4);
		};
		
		// array of functions, 1 for each source (if defined), that will be launched on Automation's "play"
		this.triggerFunc = Array.newClear(this.nfontes);
		//companion to above. Launched by "Stop"
		this.stopFunc = Array.newClear(this.nfontes);

		o = OSCresponderNode(srvr.addr, '/tr', { |time, resp, msg| msg.postln }).add;  // debugging

		/*
		// REMOVE?
		this.swinbus = Array.newClear(this.nfontes * 4); // busses software input
		(this.nfontes * 4).do { arg x;
			this.swinbus[x] = Bus.audio(server, 1); 
		};
		*/

		
		///////////// Functions to substitute blocks of code in SynthDefs //////////////
		if (decoder.isNil) {
			espacAmbOutFunc = { |ambsinal, ambsinal1O, dec|
				Out.ar( 2, ambsinal); };
			espacAmbEstereoOutFunc = { |ambsinal1plus2, ambsinal1plus2_1O, dec|
				Out.ar( 2, ambsinal1plus2); };
			revGlobalAmbFunc = { |ambsinal, dec|
				Out.ar( 2, ambsinal); };
			playBFormatOutFunc = { |player, dec|
				Out.ar( 2, player); };
			
		} {
			espacAmbOutFunc = { |ambsinal, ambsinal1O, dec|
				Out.ar( 0, FoaDecode.ar(ambsinal1O, dec)); };
			espacAmbEstereoOutFunc = { |ambsinal1plus2, ambsinal1plus2_1O, dec|
				Out.ar( 0, FoaDecode.ar(ambsinal1plus2_1O, dec)); };
			revGlobalAmbFunc = { |ambsinal, dec|
				Out.ar( 0, FoaDecode.ar(ambsinal, dec)); };
			playBFormatOutFunc = { |player, dec|
				Out.ar( 0, FoaDecode.ar(player, dec)); };
		};


		////////////////// END Functions to substitute blocs of code /////////////
		
		server = srvr ? Server.default;
		prjDr = projDir;
		dec = decoder;
		//testit = OSCresponderNode(server.addr, '/tr', { |time, resp, msg| msg.postln }).add;  // debugging
		rirW = Buffer.readChannel(server, prjDr ++ "/rir/" ++ rirWXYZ, channels: [0]);
		rirX = Buffer.readChannel(server, prjDr ++ "/rir/" ++ rirWXYZ, channels: [1]);
		rirY = Buffer.readChannel(server, prjDr ++ "/rir/" ++ rirWXYZ, channels: [2]);
		rirZ = Buffer.readChannel(server, prjDr ++ "/rir/" ++ rirWXYZ, channels: [3]);

		bufWXYZ = Buffer.read(server, prjDr ++ "/rir/" ++ rirWXYZ);
		server.sync;
		bufAformat = Buffer.alloc(server, bufWXYZ.numFrames, bufWXYZ.numChannels);
		server.sync;
	
				
		{BufWr.ar(FoaDecode.ar(PlayBuf.ar(4, bufWXYZ, loop: 0, doneAction: 2), b2a),
			bufAformat, Phasor.ar(0, BufRateScale.kr(bufAformat), 0, BufFrames.kr(bufAformat)));
			Out.ar(0, Silent.ar);
		}.play;
		
		(bufAformat.numFrames / server.sampleRate).wait;
		
		
		bufAformat.write(prjDr ++ "/rir/rirFlu.wav", headerFormat: "wav", sampleFormat: "int24");
		server.sync;
		rirFLU = Buffer.readChannel(server, prjDr ++ "/rir/rirFlu.wav", channels: [0]);
		rirFRD = Buffer.readChannel(server, prjDr ++ "/rir/rirFlu.wav", channels: [1]);
		rirBLD = Buffer.readChannel(server, prjDr ++ "/rir/rirFlu.wav", channels: [2]);
		rirBRU = Buffer.readChannel(server, prjDr ++ "/rir/rirFlu.wav", channels: [3]);
		
		server.sync;
		
		
		bufsize = PartConv.calcBufSize(fftsize, rirW); 

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
		
		server.sync;

		/////////// START code for 2nd order matrices /////////////////////
		/*
			2nd-order FuMa-MaxN A-format decoder & encoder
			Use in conjunction with AtkMatrixMix*ar
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
			[ 0.707106781, 0.707106781, 0.707106781, 0.707106781, 0.707106781, 0.707106781, 0.707106781, 0.707106781, 0.707106781, 0.707106781, 0.707106781, 0.707106781 ],
			[ 0.850650808, 0.525731112, 0, 0.850650808, -0.525731112, 0, -0.850650808, -0.525731112, 0, -0.850650808, 0.525731112, 0 ],
			[ 0, -0.850650808, -0.525731112, 0, -0.850650808, 0.525731112, 0, 0.850650808, 0.525731112, 0, 0.850650808, -0.525731112 ],
			[ -0.525731112, 0, 0.850650808, 0.525731112, 0, -0.850650808, -0.525731112, 0, 0.850650808, 0.525731112, 0, -0.850650808 ],
			[ -0.0854101966, -0.5, 0.585410197, -0.0854101966, -0.5, 0.585410197, -0.0854101966, -0.5, 0.585410197, -0.0854101966, -0.5, 0.585410197 ],
			[ -0.894427191, 0, 0, 0.894427191, 0, 0, 0.894427191, 0, 0, -0.894427191, 0, 0 ],
			[ 0, 0, -0.894427191, 0, 0, -0.894427191, 0, 0, 0.894427191, 0, 0, 0.894427191 ],
			[ 0.723606798, -0.447213596, -0.276393202, 0.723606798, -0.447213596, -0.276393202, 0.723606798, -0.447213596, -0.276393202, 0.723606798, -0.447213596, -0.276393202 ],
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
		
		/////////// END code for 2nd order matrices /////////////////////



		/// START SYNTH DEFS ///////

		SynthDef.new("revGlobalAmb",  { arg gbus;
			var sig, ambsinal;
			sig = In.ar(gbus, 1) * 5;
			ambsinal = [
				PartConv.ar(sig, fftsize, rirWspectrum), 
				PartConv.ar(sig, fftsize, rirXspectrum), 
				PartConv.ar(sig, fftsize, rirYspectrum),
				PartConv.ar(sig, fftsize, rirZspectrum)
			];
			revGlobalAmbFunc.value(ambsinal, dec);
		}).add;
		
		
		SynthDef.new("revGlobalBFormatAmb",  { arg gbfbus;
			var sig = In.ar(gbfbus, 4);
			sig = FoaDecode.ar(sig, b2a);
			sig = [
				PartConv.ar(sig[0], fftsize, rirFLUspectrum), 
				PartConv.ar(sig[1], fftsize, rirFRDspectrum), 
				PartConv.ar(sig[2], fftsize, rirBLDspectrum),
				PartConv.ar(sig[3], fftsize, rirBRUspectrum)
			];
			sig = FoaEncode.ar(sig, a2b);
			revGlobalAmbFunc.value(sig, dec);
		}).add;
		

		SynthDef.new("espacAmb",  {
			arg el = 0, inbus, gbus, mx = -5000, my = -5000, mz = 0,
			dopon = 0, dopamnt = 0,
			glev = 0, llev = 0;
			var w, x, y, z, r, s, t, u, v, p, ambsinal, ambsinal1O,
			junto, rd, dopplershift, azim, dis, xatras, yatras,  
			//		globallev = 0.0001, locallev, gsig, fonte;
			globallev = 0.0004, locallev, gsig, fonte,
			soa_a12_sig;
			var lrev, scale = 565;
			var grevganho = 0.04; // needs less gain
			fonte = Cartesian.new;
			fonte.set(mx, my, mz);
			dis = (1 - (fonte.rho - scale)) / scale;
			azim = fonte.theta;
			el = fonte.phi;
			dis = Select.kr(dis < 0, [dis, 0]); 
			//SendTrig.kr(Impulse.kr(1),0,  azim); // debugging
			
			// high freq attenuation
			p = In.ar(inbus, 1);
			p = LPF.ar(p, (dis) * 18000 + 2000); // attenuate high freq with distance
			
			// Doppler
			rd = (1 - dis) * 340;
			rd = Lag.kr(rd, 1.0);
			dopplershift= DelayC.ar(p, 0.2, rd/1640.0 * dopon * dopamnt);
			p = dopplershift;
			
			// Global reverbearation
			globallev = 1 / (1 - dis).sqrt;
			globallev = globallev - 1.0; // lower tail of curve to zero
			globallev = Select.kr(globallev > 1, [globallev, 1]); 
			globallev = Select.kr(globallev < 0, [globallev, 0]);
			
			globallev = globallev * (glev*6);
			
			
			gsig = p * grevganho * globallev;
			Out.ar(gbus, gsig); //send part of direct signal global reverb synth
			
			// Local reverberation
			locallev = 1 - dis; 
			
			locallev = locallev  * (llev*8);
			
			
			lrev = PartConv.ar(p, fftsize, rirZspectrum.bufnum, 0.2 * locallev);
			junto = p + lrev;
			
			#w, x, y, z, r, s, t, u, v = FMHEncode0.ar(junto, azim, el, dis);
			
			//	ambsinal = [w, x, y, u, v]; 
			ambsinal = [w, x, y, z, r, s, t, u, v];

				soa_a12_sig = AtkMatrixMix.ar(ambsinal, soa_a12_decoder_matrix);
				 #w, x, y, z, r, s, t, u, v = AtkMatrixMix.ar(soa_a12_sig, soa_a12_encoder_matrix);
			
			ambsinal1O = [w, x, y, z];
			
			espacAmbOutFunc.value(ambsinal, ambsinal1O, dec);
			
		}).add;


		


		// This second version of espacAmb is necessary with B-format sources, because the doppler
		// in these is performed in the player itself


		
		SynthDef.new("espacAmb2",  { 
			arg el = 0, inbus, gbus, mx = -5000, my = -5000, mz = 0, dopon = 0,
			glev = 0, llev = 0.2;
			var w, x, y, z, r, s, t, u, v, p, ambsinal, ambsinal1O,
			junto, rd, dopplershift, azim, dis, xatras, yatras,  
			globallev = 0.0004, locallev, gsig, fonte;
			var lrev, scale = 565;
			var grevganho = 0.20;
			fonte = Cartesian.new;
			fonte.set(mx, my, mz);
			dis = (1 - (fonte.rho - scale)) / scale;
			azim = fonte.theta;
			el = fonte.phi;
			dis = Select.kr(dis < 0, [dis, 0]); 
			//SendTrig.kr(Impulse.kr(1),0,  azim); // debugging
			
			// high freq attenuation
			p = In.ar(inbus, 1);
			p = LPF.ar(p, (dis) * 18000 + 2000); // attenuate high freq with distance
			
			// Doppler
			
			/*
				rd = (1 - dis) * 340; 
				dopplershift= DelayC.ar(p, 0.2, rd/1640.0 * dopon);
				p = dopplershift;
			*/
			
			// Reverberação global
			globallev = 1 / (1 - dis).sqrt;
			globallev = globallev - 1.0; // lower tail of curve to zero
			globallev = Select.kr(globallev > 1, [globallev, 1]); 
			globallev = Select.kr(globallev < 0, [globallev, 0]);
			
			globallev = globallev * (glev*6);
			
			
			gsig = p * grevganho * globallev;
			Out.ar(gbus, gsig); //send part of direct signal global reverb synth
			
			// Reverberação local
			locallev = 1 - dis; 
			//		SendTrig.kr(Impulse.kr(1),0,  locallev); // debugging
			locallev = locallev * (llev*25);
			
			
			lrev = PartConv.ar(p, fftsize, rirZspectrum.bufnum, locallev);
			//SendTrig.kr(Impulse.kr(1),0,  lrev); // debugging
			junto = p + lrev;
			
			#w, x, y, z, r, s, t, u, v = FMHEncode0.ar(junto, azim, el, dis);
			
			//	ambsinal = [w, x, y, u, v]; 
			ambsinal = [w, x, y, z, r, s, t, u, v]; 
			
			ambsinal1O = [w, x, y, z];
			
			espacAmbOutFunc.value(ambsinal, ambsinal1O, dec);
			
		}).add;


		


		SynthDef.new("espacAmbEstereo",  {
			arg el = 0, inbus, gbus, mx = -5000, my = -5000, mz = 0, angle = 1.05,
			dopon = 0, dopamnt = 0, 
			glev = 0, llev = 0;
			var w, x, y, z, r, s, t, u, v, p, ambsinal,
			w1, x1, y1, z1, r1, s1, t1, u1, v1, p1, ambsinal1,
			w2, x2, y2, z2, r2, s2, t2, u2, v2, p2, ambsinal2, ambsinal1plus2, ambsinal1plus2_1O,
			junto, rd, dopplershift, azim, dis, 
			junto1, azim1, 
			junto2, azim2, 
			globallev = 0.0001, locallev, gsig, fonte;
			var lrev, scale = 565;
			var grevganho = 0.20;
			
			fonte = Cartesian.new;
			fonte.set(mx, my);
			
			azim1 = fonte.rotate(angle / -2).theta;
			azim2 = fonte.rotate(angle / 2).theta;
			
			fonte.set(mx, my, mz);
			el = fonte.phi;
			
			dis = (1 - (fonte.rho - scale)) / scale;
			
			dis = Select.kr(dis < 0, [dis, 0]); 

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
			globallev = globallev - 1.0; // lower tail of curve to zero
			globallev = Select.kr(globallev > 1, [globallev, 1]); // verifica se o "sinal" está mais do que 1
			globallev = Select.kr(globallev < 0, [globallev, 0]); 
			
			globallev = globallev * (glev*4);
			
			gsig = Mix.new(p) / 2 * grevganho * globallev;
			Out.ar(gbus, gsig); //send part of direct signal global reverb synth
			
			
			
			p1 = p[0];
			p2 = p[1];
			// Reverberação local
			locallev = 1 - dis; 
			
			locallev = locallev  * (llev*4);
			
			
			junto1 = p1 + PartConv.ar(p1, fftsize, rirZspectrum.bufnum, 1.0 * locallev);
			junto2 = p2 + PartConv.ar(p2, fftsize, rirZspectrum.bufnum, 1.0 * locallev);
			
			#w1, x1, y1, z1, r1, s1, t1, u1, v1 = FMHEncode0.ar(junto1, azim1, el, dis);
			#w2, x2, y2, z2, r2, s2, t2, u2, v2 = FMHEncode0.ar(junto2, azim2, el, dis);
			
			ambsinal1 = [w1, x1, y1, z1, r1, s1, t1, u1, v1]; 
			ambsinal2 = [w2, x2, y2, z2, r2, s2, t2, u2, v2];
			
			ambsinal1plus2 = ambsinal1 + ambsinal2;
			ambsinal1plus2_1O = [w1, x1, y1, z1] + [w2, x2, y2, z2];
			
			espacAmbEstereoOutFunc.value(ambsinal1plus2, ambsinal1plus2_1O, dec);
			
		}).add;
		


		
		makeSynthDefPlayers = { arg type, i = 0;    // 3 types : File, HWBus and SWBus - i duplicates with 0, 1 & 2

			SynthDef.new("playMono"++type, { arg outbus, bufnum = 0, rate = 1, 
				level = 0, tpos = 0, lp = 0, busini;
				var scaledRate, spos, playerRef;
				playerRef = Ref(0);
				playMonoInFunc[i].value(playerRef, busini, bufnum, scaledRate, tpos, spos, lp, rate);
				//SendTrig.kr(Impulse.kr(1),0,  funcString); // debugging
				Out.ar(outbus, playerRef.value * level);
			}).add;

			SynthDef.new("playStereo"++type, { arg outbus, bufnum = 0, rate = 1, 
				level = 0, tpos = 0, lp = 0, busini;
				//		var sig;
				var scaledRate, spos, playerRef;
				playerRef = Ref(0);
				playStereoInFunc[i].value(playerRef, busini, bufnum, scaledRate, tpos, spos, lp, rate);
				Out.ar(outbus, playerRef.value * level);
			}).add;

			
			
			SynthDef.new("playBFormat"++type, { arg outbus, bufnum = 0, rate = 1, 
				level = 0, tpos = 0, lp = 0, rotAngle = 0, tilAngle = 0, tumAngle = 0,
				mx = 0, my = 0, mz = 0, gbus, gbfbus, glev, llev, directang = 0, contr, dopon, dopamnt,
				busini;
				var scaledRate, playerRef, wsinal, spos, pushang = 0,
				azim, dis = 1, fonte, scale = 565, globallev, locallev, 
				gsig, lsig, rd, dopplershift;
				var grevganho = 0.20;			
				fonte = Cartesian.new;
				fonte.set(mx, my, mz);
				dis = (1 - (fonte.rho - scale)) / scale;
				pushang = (1 - dis) * pi / 2; // grau de deslocamento do campo sonoro. 0 = centrado. pi/2 = 100% deslocado
				azim = fonte.theta; // ângulo (azimuth) de deslocamento
				dis = Select.kr(dis < 0, [dis, 0]); 
				
				playerRef = Ref(0);
				playBFormatInFunc[i].value(playerRef, busini, bufnum, scaledRate, tpos, spos, lp, rate);
				
				rd = (1 - dis) * 340;
				rd = Lag.kr(rd, 1.0);
				dopplershift= DelayC.ar(playerRef.value, 0.2, rd/1640.0 * dopon * dopamnt);
				playerRef.value = dopplershift;
				
				wsinal = playerRef.value[0] * contr * level * dis * 2.0;
				
				Out.ar(outbus, wsinal);
				
				playerRef.value = FoaDirectO.ar(playerRef.value, directang); // diretividade ("tamanho")
				
				playerRef.value = FoaTransform.ar(playerRef.value, 'rotate', rotAngle, level * dis * (1 - contr));
				playerRef.value = FoaTransform.ar(playerRef.value, 'push', pushang, azim);

				//	Out.ar(2, player);
				playBFormatOutFunc.value(playerRef.value, dec);
				
				// Reverberação global
				globallev = 1 / (1 - dis).sqrt;
				globallev = globallev - 1.0; // lower tail of curve to zero
				globallev = Select.kr(globallev > 1, [globallev, 1]); // verifica se o "sinal" está mais do que 1
				globallev = Select.kr(globallev < 0, [globallev, 0]); 
				globallev = globallev * (glev* 6) * grevganho;
				
				gsig = playerRef.value[0] * globallev;
				
				locallev = 1 - dis; 
				
				//				locallev = locallev  * (llev*10) * grevganho;
				locallev = locallev  * (llev*5);
				lsig = playerRef.value[0] * locallev;

				//
				Out.ar(gbus, gsig + lsig); //send part of direct signal global reverb synth

								// trying again ... testing
				
				gsig = playerRef.value * globallev; // b-format
				Out.ar(gbfbus, gsig); 
				
				
			}).add;

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

		arg dur = 60;
		var fonte, dist, scale = 565, espacializador, mbus, sbus, ncanais, synt, fatual = 0, 
		itensdemenu, gbus, azimuth, event, brec, bplay, bload, bnodes, sombuf, funcs, 
		dopcheque,
		loopcheck, lpcheck, lp,
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
		
		dopnumbox, volslider, dirnumbox, dirslider, connumbox, conslider, cbox,
		angslider, bsalvar, bcarregar, bdados, xbox, ybox, abox, vbox, gbox, lbox, dbox, dpbox, dcheck,
		gslider, gnumbox, lslider, lnumbox, tfield, dopflag = 0, btestar, tocar, isPlay, isRec,
		atualizarvariaveis, updateSynthInArgs,
		testado,
		rnumbox, rslider, rbox, 
		znumbox, zslider, zbox, zlev, // z-axis
		xval, yval, zval,
		rlev, dlev, clev, cslider, dplev, dpslider, cnumbox;
		espacializador = Array.newClear(this.nfontes);
		doppler = Array.newClear(this.nfontes); 
		lp = Array.newClear(this.nfontes); 
		hwn = Array.newClear(this.nfontes); 
		scn = Array.newClear(this.nfontes); 
		mbus = Array.newClear(this.nfontes); 
		sbus = Array.newClear(this.nfontes); 
		ncanais = Array.newClear(this.nfontes);  // 0 = não, nem estéreo. 1 = mono. 2 = estéreo.
		this.ncan = Array.newClear(this.nfontes);  // 0 = não, nem estéreo. 1 = mono. 2 = estéreo.
		// note that ncan refers to # of channels in streamed sources.
		// ncanais is related to sources read from file
		this.busini = Array.newClear(this.nfontes); // initial bus # in streamed audio grouping (ie. mono, stereo or b-format)
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
		hwncheck = Array.newClear(this.nfontes); // stream check
		scncheck = Array.newClear(this.nfontes); // stream check
		tfield = Array.newClear(this.nfontes);
		
		testado = Array.newClear(this.nfontes);
		
		
		this.nfontes.do { arg i;
			doppler[i] = 0;
			angle[i] = 0;
			level[i] = 0;
			glev[i] = 0;
			llev[i] = 0;
			lp[i] = 0;
			hwn[i] = 0;
			scn[i] = 0;
			rlev[i] = 0;
			dlev[i] = 0;
			clev[i] = 0;
			zlev[i] = 0;
			dplev[i] = 0;
			
			this.ncan[i] = 0;
			this.busini[i] = 0;
			sprite[i, 0] = -20;
			sprite[i, 1] = -20;
			testado[i] = false;
		};
		
		
		
		novoplot = {
			arg mx, my, i, nfnts; 
			var btest;
			{
				win.drawFunc = {
					
					nfnts.do { arg ind;
						Pen.fillColor = Color(0.8,0.2,0.9);
						Pen.addArc(sprite[ind, 0]@sprite[ind, 1], 20, 0, 2pi);
						Pen.fill;
						(ind + 1).asString.drawCenteredIn(Rect(sprite[ind, 0] - 10, sprite[ind, 1] - 10, 20, 20), 
							Font.default, Color.white);
					};
					Pen.fillColor = Color.gray(0, 0.5);
					Pen.addArc(450@450, 20, 0, 2pi);
					Pen.fill;
				}
			}.defer;
			{ win.refresh; }.defer;
			
		};
		
		
		gbus = Bus.audio(server, 1); // global reverb bus
		this.gbfbus = Bus.audio(server, 4); // global b-format bus
		
		fonte = Point.new;
		win = Window.new("Mosca", Rect(0, 900, 900, 900)).front;
		wdados = Window.new("Data", Rect(900, 900, 990, (this.nfontes*20)+60 ));
		wdados.userCanClose = false;
		
		
		bdados = Button(win, Rect(280, 30, 90, 20))
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
			
			("tpos = " ++ tpos).postln;
			if ((path != "") && (hwncheck[i].value == false)) {
				{	
					
					if (sombuf[i].numChannels == 1)  // arquivo mono
					{ncanais[i] = 1;
						angle[i] = 0;
						{angnumbox.value = 0;}.defer;
						{angslider.value = 0;}.defer;
						
						
						if(revGlobal == nil) {
							revGlobal = Synth.new(\revGlobalAmb, [\gbus, gbus], addAction:\addToTail);
						};
						if (testado[i] == false) { // if source is testing don't relaunch synths
							synt[i] = Synth.new(\playMonoFile, [\outbus, mbus[i], 
								\bufnum, sombuf[i].bufnum, \rate, 1, \tpos, tpos, \lp, lp[i], \level, level[i]], 
								revGlobal, addAction: \addBefore).onFree({espacializador[i].free;
									espacializador[i] = nil; synt[i] = nil});
							
							espacializador[i] = Synth.new(\espacAmb, [\inbus, mbus[i], 
								\gbus, gbus, \dopon, doppler[i]], 
								synt[i], addAction: \addAfter);
						};
						atualizarvariaveis.value;
						
						




						
					}
					{if (sombuf[i].numChannels == 2) {ncanais[i] = 2; // arquivo estéreo
						angle[i] = pi/2;
						//						{angnumbox.value = pi/2;}.defer; 
						{angnumbox.value = 1.05;}.defer; // 60 degrees
						//						{angslider.value = 0.5;}.defer;
						{angslider.value = 0.33;}.defer;
						
						
						if(revGlobal == nil){
							revGlobal = Synth.new(\revGlobalAmb, [\gbus, gbus], addAction:\addToTail);
						};
						if (testado[i] == false) {
							synt[i] = Synth.new(\playStereoFile, [\outbus, sbus[i], 
								\bufnum, sombuf[i].bufnum, \rate, 1, \tpos, tpos, \lp, lp[i], \level, level[i]], 
								revGlobal, addAction: \addBefore).onFree({espacializador[i].free;
									//	addAction: \addToHead).onFree({espacializador[i].free;
									espacializador[i] = nil; synt[i] = nil});
							
							espacializador[i] = Synth.new(\espacAmbEstereo, [\inbus, sbus[i], \gbus, gbus,
								\dopon, doppler[i]], 
								synt[i], addAction: \addAfter);
						};
						
						atualizarvariaveis.value;
						
						//	~revGlobal = Synth.new(\revGlobalAmb, [\gbus, gbus], addAction:\addToTail);



						
					} {
						if (sombuf[i].numChannels == 4) {
							"B-format".postln;
							ncanais[i] = 4;
							angle[i] = 0;
							{angnumbox.value = 0;}.defer;
							{angslider.value = 0;}.defer;
							// reverb for non-contracted (full b-format) component

							// reverb for contracted (mono) component - and for rest too
							if(revGlobal == nil){
								revGlobal = Synth.new(\revGlobalAmb, [\gbus, gbus], addAction:\addToTail);
							};
							if(revGlobalBF == nil){
								revGlobalBF = Synth.new(\revGlobalBFormatAmb, [\gbfbus, this.gbfbus],
									addAction:\addToTail);
							};
							if (testado[i] == false) {
								synt[i] = Synth.new(\playBFormatFile, [\gbus, gbus, \gbfbus, this.gbfbus, \outbus,
									mbus[i], \bufnum, sombuf[i].bufnum, \contr, clev[i],
									\rate, 1, \tpos, tpos, \lp,
									lp[i], \level, level[i], \dopon, doppler[i]], 
									//					~revGlobal, addAction: \addBefore);
									revGlobal, addAction: \addBefore).onFree({espacializador[i].free;
										espacializador[i] = nil; synt[i] = nil;});
								//	xbox[i].valueAction = 1; // é preciso para aplicar rev global sem mexer com mouse
								
								espacializador[i] = Synth.new(\espacAmb2, [\inbus, mbus[i], \gbus, gbus, 
									\dopon, doppler[i]], 
									synt[i], addAction: \addAfter);
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
				("HELLO scncheck[i].value = " ++ i ++ " " ++ scncheck[i].value).postln;
				if ((scncheck[i].value) || (hwncheck[i].value)) {
					var x;
					("Streaming! ncan = " ++ this.ncan[i].value
						++ " & this.busini = " ++ this.busini[i].value).postln;
					x = case
					{ this.ncan[i] == 1 } {
						"Mono!".postln;
						
						("HELLO MONO scncheck[i].value = " ++ i ++ " " ++ scncheck[i].value).postln;

						
						if(revGlobal == nil){
							revGlobal = Synth.new(\revGlobalAmb, [\gbus, gbus], addAction:\addToTail);
						};
						if (testado[i] == false) {

							if (hwncheck[i].value) {
								synt[i] = Synth.new(\playMonoHWBus, [\outbus, mbus[i], \busini, this.busini[i],
									\level, level[i]], revGlobal,
									addAction: \addBefore).onFree({espacializador[i].free;
										espacializador[i] = nil; synt[i] = nil});
							} {
								synt[i] = Synth.new(\playMonoSWBus, [\outbus, mbus[i],
									\busini, this.scInBus[i], // use "index" method?
									\level, level[i]], revGlobal,
									addAction: \addBefore).onFree({espacializador[i].free;
										espacializador[i] = nil; synt[i] = nil});
							};
							
							
							espacializador[i] = Synth.new(\espacAmb, [\inbus, mbus[i], 
								\gbus, gbus, \dopon, doppler[i]], 
								synt[i], addAction: \addAfter);
						};
						atualizarvariaveis.value;
						
						



						
					}
					{ this.ncan[i] == 2 } {
						"Estéreo!".postln;
						ncanais[i] = 0; // just in case!
						angle[i] = pi/2;
						{angnumbox.value = pi/2;}.defer;
						{angslider.value = 0.5;}.defer;
						
						


						
						if(revGlobal == nil){
							revGlobal = Synth.new(\revGlobalAmb, [\gbus, gbus], addAction:\addToTail);
						};
						if (testado[i] == false) {

							if (hwncheck[i].value) {
								synt[i] = Synth.new(\playStereoHWBus, [\outbus, sbus[i], \busini, this.busini[i],
									\level, level[i]], revGlobal,
									addAction: \addBefore).onFree({espacializador[i].free;
										espacializador[i] = nil; synt[i] = nil});
							} {
								synt[i] = Synth.new(\playStereoSWBus, [\outbus, sbus[i],
									\busini, this.scInBus[i],
									\level, level[i]], revGlobal,
									addAction: \addBefore).onFree({espacializador[i].free;
										espacializador[i] = nil; synt[i] = nil});
							};
							
							espacializador[i] = Synth.new(\espacAmbEstereo, [\inbus, sbus[i], \gbus, gbus,
								\dopon, doppler[i]], 
								synt[i], addAction: \addAfter);
						};
						atualizarvariaveis.value;
						
						



						
					}
					{ this.ncan[i] == 4 } {
						//"B-format!".postln;
						
						"B-format ambisonic!!!".postln;
						//	~revGlobal = Synth.new(\revGlobalAmb, [\gbus, gbus], addAction:\addToTail);
						// reverb for non-contracted (full b-format) component

						// reverb for contracted (mono) component - no, now it's for both
						if(revGlobal == nil){
							revGlobal = Synth.new(\revGlobalAmb, [\gbus, gbus], addAction:\addToTail);
						};
						
						if (testado[i] == false) {
							if (hwncheck[i].value) {
								synt[i] = Synth.new(\playBFormatHWBus, [\gbfbus, this.gbfbus, \outbus, mbus[i],
									\contr, clev[i], \rate, 1, \tpos, tpos, \level, level[i], \dopon, doppler[i],
									\busini, this.busini[i]], 
									revGlobal, addAction: \addBefore).onFree({espacializador[i].free;
										espacializador[i] = nil; synt[i] = nil;});
							} {
								synt[i] = Synth.new(\playBFormatSWBus, [\gbfbus, this.gbfbus, \outbus, mbus[i],
									\contr, clev[i], \rate, 1, \tpos, tpos, \level, level[i], \dopon, doppler[i],
									\busini, this.scInBus[i] ], 
									revGlobal, addAction: \addBefore).onFree({espacializador[i].free;
										espacializador[i] = nil; synt[i] = nil;});
							};
							
							espacializador[i] = Synth.new(\espacAmb2, [\inbus, mbus[i], \gbus, gbus, 
								\dopon, doppler[i]], 
								synt[i], addAction: \addAfter);
						};
						atualizarvariaveis.value;
						
						




						
					};
					
					
				};
				
				
				
			};
			
		};
		
		
		
		btestar = Button(win, Rect(280, 50, 90, 20))
		.states_([
			["test", Color.black, Color.white],
			["stop", Color.white, Color.red]
		])
		.action_({ arg but;
			//	var testado = fatual;
			//	but.value.postln;
			{
			if(but.value == 1)
			{
				
				runTrigger.value(fatual);
				//	("FATUAL + " ++ fatual).postln;


				//atualizarvariaveis.value;
				tocar.value(fatual, 0);
				//		~testado = fatual;
					testado[fatual] = true;
				
				
			}
			{
				
				runStop.value(fatual);
				synt[fatual].free;
				synt[fatual] = nil;
					testado[fatual] = false;
			
				
			};
		}.defer;
		});
		
		
		
		
		bsalvar = Button(win, Rect(670, 40, 90, 20))
		.states_([
			["save auto", Color.black, Color.white],
			
		])
		.action_({
			//arg but;
			controle.seek;
			controle.snapshot; // rewind to zero
			controle.save(controle.presetDir);
			
			
		});
		
		
		
		
		bcarregar = Button(win, Rect(760, 40, 90, 20))
		.states_([
			["load auto", Color.black, Color.white],
		])
		.action_({
			//arg but;
			controle.load(controle.presetDir);
			controle.seek;
		});
		
		
		
		
		
		
		win.view.background = Color(0.7,0.8,0.8);
		
		
		win.drawFunc = {
			//paint origin
			Pen.fillColor = Color.white(0, 0.5);
			Pen.addArc(450@450, 20, 0, 2pi);
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
			zslider.value = (zlev[fatual] + 450) / 900;
			("Z-lev = " ++  zlev[fatual]).postln;
			
			dpslider.value = dplev[fatual];
			connumbox.value = clev[fatual];
			
			ncannumbox.value = this.ncan[fatual];
			busininumbox.value = this.busini[fatual];
			
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
		dopcheque.value = false;
		
		
		hwInCheck = CheckBox( win, Rect(10, 30, 100, 20), "HW Bus").action_({ arg butt;
			{hwncheck[fatual].valueAction = butt.value;}.defer;
			if (hwInCheck.value && scInCheck.value) {
				//scInCheck.value = false;
			};
		});

		scInCheck = CheckBox( win, Rect(104, 30, 100, 20), "SC-in").action_({ arg butt;
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
		
		
		textbuf = StaticText(win, Rect(810, 440, 90, 20));
		textbuf.string = "Z-Axis";
		znumbox = NumberBox(win, Rect(835, 705, 60, 20));
		znumbox.value = 0;
		znumbox.clipHi = 450;
		znumbox.clipLo = -450;
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
		
		
		zslider = Slider.new(win, Rect(855, 200, 20, 500));
		zslider.value = 0.5;
		zslider.action = {arg num;
			{znumbox.value = (450 - (num.value * 900)) * -1;}.defer;
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
		textbuf.string = "Global Reverberation";
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
		textbuf.string = "Local Reverberation";
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
		
		

		
		
		bload = Button(win, Rect(220, 30, 60, 20))
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
				

			}, 
				{
					"cancelado".postln;
					{tfield[fatual].value = "";}.defer;
					
				}
			);	
		});

		bnodes = Button(win, Rect(220, 50, 60, 20))
		.states_([
			["nodes", Color.black, Color.white],
		])
		.action_({
			server.plotTree;
		});

		
		textbuf = StaticText(wdados, Rect(15, 20, 50, 20));
		textbuf.string = "Dp";
		textbuf = StaticText(wdados, Rect(35, 20, 50, 20));
		textbuf.string = "Lp";
		textbuf = StaticText(wdados, Rect(55, 20, 50, 20));
		textbuf.string = "Hw";
		textbuf = StaticText(wdados, Rect(75, 20, 50, 20));
		textbuf.string = "Sw";

		textbuf = StaticText(wdados, Rect(100, 20, 50, 20));
		textbuf.string = "NCan";
		textbuf = StaticText(wdados, Rect(140, 20, 50, 20));
		textbuf.string = "SBus";

		textbuf = StaticText(wdados, Rect(180, 20, 50, 20));
		textbuf.string = "X";
		textbuf = StaticText(wdados, Rect(220, 20, 50, 20));
		textbuf.string = "Y";

		textbuf = StaticText(wdados, Rect(260, 20, 50, 20));
		textbuf.string = "Z";

		
		textbuf = StaticText(wdados, Rect(300, 20, 50, 20));
		textbuf.string = "Ang";
		textbuf = StaticText(wdados, Rect(340, 20, 50, 20));
		textbuf.string = "Vol";
		textbuf = StaticText(wdados, Rect(380, 20, 50, 20));
		textbuf.string = "Glob";
		textbuf = StaticText(wdados, Rect(420, 20, 50, 20));
		textbuf.string = "Loc";
		textbuf = StaticText(wdados, Rect(460, 20, 50, 20));
		textbuf.string = "Rot";
		textbuf = StaticText(wdados, Rect(500, 20, 50, 20));
		textbuf.string = "Dir";
		textbuf = StaticText(wdados, Rect(540, 20, 50, 20));
		textbuf.string = "Cntr";
		textbuf = StaticText(wdados, Rect(580, 20, 50, 20));
		textbuf.string = "DAmt";
		textbuf = StaticText(wdados, Rect(620, 20, 50, 20));
		textbuf.string = "File";

		
		this.nfontes.do { arg i;	
			dcheck[i] = CheckBox.new( wdados, Rect(15, 40 + (i*20), 40, 20))
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

			lpcheck[i] = CheckBox.new( wdados, Rect(35, 40 + (i*20), 40, 20))
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
			// testing
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
			
			scncheck[i] = CheckBox.new( wdados, Rect(75, 40 + (i*20), 40, 20))
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
			
			
			ncanbox[i] = NumberBox(wdados, Rect(100, 40 + (i*20), 40, 20));
			businibox[i] = NumberBox(wdados, Rect(140, 40 + (i*20), 40, 20));
			xbox[i] = NumberBox(wdados, Rect(180, 40 + (i*20), 40, 20));
			ybox[i] = NumberBox(wdados, Rect(220, 40+ (i*20), 40, 20));
			zbox[i] = NumberBox(wdados, Rect(260, 40+ (i*20), 40, 20));
			abox[i] = NumberBox(wdados, Rect(300, 40 + (i*20), 40, 20));
			vbox[i] = NumberBox(wdados, Rect(340, 40+ (i*20), 40, 20));
			gbox[i] = NumberBox(wdados, Rect(380, 40+ (i*20), 40, 20));
			lbox[i] = NumberBox(wdados, Rect(420, 40+ (i*20), 40, 20));
			rbox[i] = NumberBox(wdados, Rect(460, 40+ (i*20), 40, 20));
			dbox[i] = NumberBox(wdados, Rect(500, 40+ (i*20), 40, 20));
			cbox[i] = NumberBox(wdados, Rect(540, 40+ (i*20), 40, 20));
			dpbox[i] = NumberBox(wdados, Rect(580, 40+ (i*20), 40, 20));
			tfield[i] = TextField(wdados, Rect(620, 40+ (i*20), 350, 20));

			tfield[i].action = {arg path;
				if (path != "") {
					
					sombuf[i] = Buffer.read(server, path.value, action: {arg buf; 
						((buf.numFrames ) / buf.sampleRate).postln;
						//				(buf.sampleRate).postln;
					});
				}
				
			};
			
			xbox[i].action = {arg num;
				sprite[i, 1] = 450 + (num.value * -1);
				novoplot.value(num.value, ybox[i], i, this.nfontes);
				xval[i] = num.value;
				if(espacializador[i] != nil){
					espacializador[i].set(\mx, num.value);
					this.setSynths(i, \mx, num.value);
					synt[i].set(\mx, num.value);
				};
				
				
			};
			ybox[i].action = {arg num; 
				sprite[i, 0] = (num.value * -1 + 450);
				yval[i] = num.value;
				if(espacializador[i] != nil){
					espacializador[i].set(\my, num.value);
					this.setSynths(i, \my, num.value);
					synt[i].set(\my, num.value);
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
				"dirBox!".postln;
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

			zbox[i].action = {arg num;
				espacializador[i].set(\mz, num.value);
				zval[i] = num.value;
				this.setSynths(i, \mz, num.value);
				//"fooooob".postln;
				synt[i].set(\mz, num.value);
				//synt[i].set(\elev, num.value);
				zlev[i] = num.value;
				if(i == fatual) 
				{
					//var val = (450 - (num.value * 900)) * -1;
					zslider.value = (num.value + 450) / 900;
					znumbox.value = num.value;
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
					//var val = (450 - (num.value * 900)) * -1;
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
					//var val = (450 - (num.value * 900)) * -1;
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

	
		//controle = Automation(dur).front(win, Rect(450, 10, 400, 25));
		controle = Automation(dur, showLoadSave: false, minTimeStep: 0.001).front(win, Rect(450, 10, 400, 25));
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
				{runTrigger.value(i);}.defer;
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
			("Acabou! = " ++ controle.now).postln;
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
			
		};

		
		
		win.view.mouseMoveAction = {|view, x, y, modifiers | [x, y];

			xbox[fatual].valueAction = 450 - y;
			ybox[fatual].valueAction = (x - 450) * -1;
			win.drawFunc = {
				this.nfontes.do { arg i;	
					Pen.fillColor = Color(0.8,0.2,0.9);
					Pen.addArc(sprite[i, 0]@sprite[i, 1], 20, 0, 2pi);
					Pen.fill;
					(i + 1).asString.drawCenteredIn(Rect(sprite[i, 0] - 10, sprite[i, 1] - 10, 20, 20), 
						Font.default, Color.white);
				};
				// círculo central
				Pen.fillColor = Color.gray(0, 0.5);
				Pen.addArc(450@450, 20, 0, 2pi);
				Pen.fill;
				
			};
			
			win.refresh;
			
		};
		
		
		this.nfontes.do { arg x;
			mbus[x] = Bus.audio(server, 1); // passar som da fonte ao espacializador
			sbus[x] = Bus.audio(server, 2); // passar som da fonte ao espacializador
			//	bfbus[x] = Bus.audio(s, 4); // passar som da fonte ao espacializador
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
			
			wdados.close;
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
			
			
		});

		mmcslave = CheckBox( win, Rect(670, 65, 140, 20), "Slave to MMC").action_({ arg butt;
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

