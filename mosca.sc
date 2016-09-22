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
	var  <>kernelSize, <>scale, <>rirW, <>rirX, <>rirY, <>rirZ, <>rirL, <>rirR,
	<>rirWspectrum, <>rirXspectrum, <>rirYspectrum, <>rirZspectrum,
	<>rirLspectrum, <>rirRspectrum,

	<>irbuffer, <>bufsize, <>bufsizeBinaural, <>win, <>wdados, <>sprite, <>nfontes,
	<>controle, <>revGlobal, <>revGlobalBF, <>m, <>offset, <>textbuf, <>controle,
	<>sysex, <>mmcslave;
	//	var <>fftsize = 2048;
	classvar server, rirW, rirX, rirY, rirZ, rirL, rirR,
	bufsize, bufsizeBinaural, irbuffer,
	rirWspectrum, rirXspectrum, rirYspectrum, rirZspectrum,
	rirLspectrum, rirRspectrum,
	binDecoder, prjDr;
	classvar fftsize = 2048, server;


	*new { arg projDir, rirWXYZ, rirBinaural, subjectID, srvr;
		server = srvr ? Server.default;
		//		nfontes = numFontes;
		//		sprite = Array2D.new(nfontes, 2);
		prjDr = projDir;
		rirW = Buffer.readChannel(server, prjDr ++ "/rir/" ++ rirWXYZ, channels: [0]);
		rirX = Buffer.readChannel(server, prjDr ++ "/rir/" ++ rirWXYZ, channels: [1]);
		rirY = Buffer.readChannel(server, prjDr ++ "/rir/" ++ rirWXYZ, channels: [2]);
		rirZ = Buffer.readChannel(server, prjDr ++ "/rir/" ++ rirWXYZ, channels: [3]);
		rirL = Buffer.readChannel(server, prjDr ++ "/rir/" ++ rirBinaural, channels: [0]);
		rirR = Buffer.readChannel(server, prjDr ++ "/rir/" ++ rirBinaural, channels: [1]);

		
		server.sync;
		
		bufsize = PartConv.calcBufSize(fftsize, rirW); 
		bufsizeBinaural = PartConv.calcBufSize(fftsize, rirL);

		rirWspectrum= Buffer.alloc(server, bufsize, 1);
		rirXspectrum= Buffer.alloc(server, bufsize, 1);
		rirYspectrum= Buffer.alloc(server, bufsize, 1);
		rirZspectrum= Buffer.alloc(server, bufsize, 1);
		// binaural
		rirLspectrum= Buffer.alloc(server, bufsizeBinaural, 1);
		rirRspectrum= Buffer.alloc(server, bufsizeBinaural, 1);

		rirWspectrum.preparePartConv(rirW, fftsize);
		rirXspectrum.preparePartConv(rirX, fftsize);
		rirYspectrum.preparePartConv(rirY, fftsize);
		rirZspectrum.preparePartConv(rirZ, fftsize);
		rirLspectrum.preparePartConv(rirL, fftsize);
		rirRspectrum.preparePartConv(rirR, fftsize);

		server.sync;

		rirW.free; // don't need time domain data anymore, just needed spectral version
		rirX.free;
		rirY.free;
		rirZ.free;
		
		rirL.free;
		rirR.free;
		server.sync;
		
		binDecoder = FoaDecoderKernel.newCIPIC(subjectID); // KEMAR head, use IDs 21 or 165
		
		//		server.sync;

		/// SYNTH DEFS ///////

		SynthDef.new("revGlobalAmb",  { arg gbus;
			var sig;
			sig = In.ar(gbus, 1) * 5; // precisa de ganho....
			
			Out.ar(2, [PartConv.ar(sig, fftsize, rirWspectrum.bufnum), 
				PartConv.ar(sig, fftsize, rirXspectrum.bufnum), 
			PartConv.ar(sig, fftsize, rirYspectrum.bufnum),
				PartConv.ar(sig, fftsize, rirZspectrum.bufnum)
			]);
			
		}).add;

		SynthDef.new("revGlobalBinaural",  { arg gbus;
			var sig;
			sig = In.ar(gbus, 1) * 5;
			//	SendTrig.kr(Impulse.kr(1), 0, sig); // debugging
			Out.ar(0, [PartConv.ar(sig, fftsize, rirLspectrum.bufnum), 
				PartConv.ar(sig, fftsize, rirRspectrum.bufnum)
			]);
		}).add;

		SynthDef.new("revGlobalBFormatAmb",  { arg gbfbus;
			var sig = In.ar(gbfbus, 4);
			
			//	SendTrig.kr(Impulse.kr(1), 0, sig[2]); // debugging
			
			Out.ar(2, [PartConv.ar(sig[0], fftsize, rirWspectrum.bufnum), 
				PartConv.ar(sig[1], fftsize, rirXspectrum.bufnum), 
				PartConv.ar(sig[2], fftsize, rirYspectrum.bufnum),
				PartConv.ar(sig[3], fftsize, rirZspectrum.bufnum)
			]);
		}).add;

		SynthDef.new("revGlobalBFormatBin",  { arg gbfbus;
			var sig = In.ar(gbfbus, 4);
			Out.ar(0, [PartConv.ar(sig, fftsize, rirLspectrum.bufnum), 
				PartConv.ar(sig, fftsize, rirRspectrum.bufnum)
			]);
			
		}).add;

		SynthDef.new("espacAmb",  {
			arg el = 0, inbus, gbus, mx = -5000, my = -5000, mz = 0, dopon = 0,
			glev = 0, llev = 0;
			var w, x, y, z, r, s, t, u, v, p, ambsinal, ambsinal1O,
			junto, rd, dopplershift, azim, dis, xatras, yatras,  
			//		globallev = 0.0001, locallev, gsig, fonte;
			globallev = 0.0004, locallev, gsig, fonte;
			var lrev, scale = 565;
			var grevganho = 0.04; // needs less gain
			
			mx = Lag.kr(mx, 2.0 * dopon);
			my = Lag.kr(my, 2.0 * dopon);
			mz = Lag.kr(mz, 2.0 * dopon);
			
			//		fonte = Point.new;
			fonte = Cartesian.new;
			//	fonte.set((mx - 450) * -1, 450 - my);
			fonte.set(mx, my, mz);
			dis = (1 - (fonte.rho - scale)) / scale;
			//	azim = fonte.rotate(-1.57).theta;
			azim = fonte.theta;
			el = fonte.phi;
			dis = Select.kr(dis < 0, [dis, 0]); 
			//SendTrig.kr(Impulse.kr(1),0,  azim); // debugging
			
			// atenuação de frequências agudas
			p = In.ar(inbus, 1);
			p = LPF.ar(p, (dis) * 18000 + 2000); // attenuate high freq with distance
			
			// Doppler
			rd = (1 - dis) * 340; 
		//		rd = Lag.kr(rd, 2.0);
			dopplershift= DelayC.ar(p, 0.2, rd/1640.0 * dopon);
			p = dopplershift;
			
			// Reverberação global
			globallev = 1 / (1 - dis).sqrt;
			globallev = globallev - 1.0; // lower tail of curve to zero
			globallev = Select.kr(globallev > 1, [globallev, 1]); // verifica se o "sinal" está mais do que 1
			globallev = Select.kr(globallev < 0, [globallev, 0]);
			
			globallev = globallev * (glev*6);
			
			
			gsig = p * grevganho * globallev;
			Out.ar(gbus, gsig); //send part of direct signal global reverb synth
			
			// Reverberação local
			locallev = 1 - dis; 
		
			locallev = locallev  * (llev*8);
			
			
			lrev = PartConv.ar(p, fftsize, rirZspectrum.bufnum, 0.2 * locallev);
			junto = p + lrev;
			
			#w, x, y, z, r, s, t, u, v = FMHEncode0.ar(junto, azim, el, dis);
			
			//	ambsinal = [w, x, y, u, v]; 
			ambsinal = [w, x, y, z, r, s, t, u, v]; 
			
			ambsinal1O = [w, x, y, z];
			
			//Out.ar( 0, FoaDecode.ar(ambsinal1O, ~decoder));
			//	Out.ar( 0, ambsinal);
			Out.ar( 2, ambsinal);
			
			
		}).add;


		SynthDef.new(\tocarBFormatBin, { arg outbus, bufnum = 0, rate = 1, 
			volume = 0, tpos = 0, lp = 0, rotAngle = 0, tilAngle = 0, tumAngle = 0,
			mx = 0, my = 0, mz = 0, gbfbus, gbusout, glev, llev, directang = 0, contr,
			dopon, dopamnt = 0;
			
			var scaledRate, player, wsinal, spos, pushang = 0,
			azim, dis = 1, fonte, scale = 565, globallev, locallev, 
			gsig, lsig, el,
			rd, dopplershift;
			var grevganho = 0.20;
			// 	fonte = Point.new;
			
			mx = Lag.kr(mx, 2.0 * dopon);
			my = Lag.kr(my, 2.0 * dopon);
			mz = Lag.kr(mz, 2.0 * dopon);
			
			fonte = Cartesian.new;
			fonte.set(mx, my, mz);
			dis = (1 - (fonte.rho - scale)) / scale;
			pushang = (1 - dis) * pi / 2; // grau de deslocamento do campo sonoro. 0 = centrado. pi/2 = 100% deslocado
			azim = fonte.theta; // ângulo (azimuth) de deslocamento
			
			el = fonte.phi;
			
			dis = Select.kr(dis < 0, [dis, 0]); 
			// 	spos = tpos * SampleRate.ir;
			spos = tpos * BufSampleRate.kr(bufnum);
			scaledRate = rate * BufRateScale.kr(bufnum); 
			player = PlayBuf.ar(4, bufnum, scaledRate, startPos: spos, loop: lp, doneAction:2);
			
			rd = (1 - dis) * 340; 
			dopplershift= DelayC.ar(player, 0.2, rd/1640.0 * dopon * dopamnt);
			player = dopplershift;
			
			
			//wsinal = player[0] * contr * volume * dis * 2.0; // why is contr (contração?) being used here?
			// it's being used because this wsinal is being incorrectly used for global reverb too. This should
			// only be used for contraction
			
			//		 wsinal = player[0] * volume * dis * 2.0;
			wsinal = player[0] * volume * contr * dis * 2.0;
			// SendTrig.kr(Impulse.kr(1),0, wsinal); // debugging
			//	Out.ar(gbusout wsinal);
			
			// this is the "contracted" component of the global reverb
			// and has a mono source derived from W. The full B-format or non-contracted
			// component is sent to the global b-format binaral reverberator. See "gsig" below.
			
			Out.ar(outbus, wsinal);
			//		Out.ar(outbus, wsinal * contr);
			//	~teste = contr * volume * dis;
			
			// 		SendTrig.kr(Impulse.kr(1),0, contr); // debugging
			
			player = FoaDirectO.ar(player, directang); // diretividade ("tamanho")
			
			player = FoaTransform.ar(player, 'rotate', rotAngle, volume * dis * (1 - contr));
			player = FoaTransform.ar(player, 'push', pushang, azim, el);
			
			
			//		 Decode direct signal
			
			server.sync;
			Out.ar( 0, FoaDecode.ar(player, binDecoder));
			
			// Reverberação global
			globallev = 1 / (1 - dis).sqrt;
			globallev = globallev - 1.0; // lower tail of curve to zero
			globallev = Select.kr(globallev > 1, [globallev, 1]); // verifica se o "sinal" está mais do que 1
			globallev = Select.kr(globallev < 0, [globallev, 0]); 
			globallev = globallev * (glev*4) * grevganho;
			
			gsig = player * globallev;
			
			
			
			locallev = 1 - dis; 
			
			locallev = locallev  * (llev*12) * grevganho;
			lsig = player * locallev;
			
			// SendTrig.kr(Impulse.kr(1),0, gsig); // debugging
			
			Out.ar(gbfbus, gsig + lsig); //send part of direct signal global reverb synth
			
			
		}).add;



	// This second version of espacAmb is necessary with B-format sources, because the doppler
	// in these is performed in the player itself


	
	
		SynthDef.new("espacAmb2",  {
			arg el = 0, inbus, gbus, mx = -5000, my = -5000, mz = 0, dopon = 0,
			glev = 0, llev = 0;
			var w, x, y, z, r, s, t, u, v, p, ambsinal, ambsinal1O,
			junto, rd, dopplershift, azim, dis, xatras, yatras,  
			//		globallev = 0.0001, locallev, gsig, fonte;
			globallev = 0.0004, locallev, gsig, fonte;
			//	bufL, bufR;
			var lrev, scale = 565;
			var grevganho = 0.20;
			//	bufL = Bus.audio(s, 1);
			//	bufR = Bus.audio(s, 1);
			
			//		xatras = DelayL.kr(mx, 1, 1); // atrazar mx e my para combinar com atraso do efeito Doppler
			//		yatras = DelayL.kr(my, 1, 1);
			
			
			// i think this is needed here too?, but not the doppler
			mx = Lag.kr(mx, 2.0 * dopon);
			my = Lag.kr(my, 2.0 * dopon);
			mz = Lag.kr(mz, 2.0 * dopon);
			
			
			//		fonte = Point.new;
			fonte = Cartesian.new;
			//	fonte.set((mx - 450) * -1, 450 - my);
			fonte.set(mx, my, mz);
			dis = (1 - (fonte.rho - scale)) / scale;
			//	azim = fonte.rotate(-1.57).theta;
			azim = fonte.theta;
			el = fonte.phi;
			dis = Select.kr(dis < 0, [dis, 0]); 
			//SendTrig.kr(Impulse.kr(1),0,  azim); // debugging
			
			// atenuação de frequências agudas
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
			globallev = Select.kr(globallev > 1, [globallev, 1]); // verifica se o "sinal" está mais do que 1
			globallev = Select.kr(globallev < 0, [globallev, 0]);
			
			globallev = globallev * (glev*6);
			
			
			gsig = p * grevganho * globallev;
			Out.ar(gbus, gsig); //send part of direct signal global reverb synth
			
			// Reverberação local
			locallev = 1 - dis; 
			
			locallev = locallev  * (llev*8);
			
			//locallev = Select.kr(locallev > 1, [locallev, 1]); 
			//SendTrig.kr(Impulse.kr(1),0,  locallev); // debugging
			
			lrev = PartConv.ar(p, fftsize, rirZspectrum.bufnum, 0.2 * locallev);
			junto = p + lrev;
			
			#w, x, y, z, r, s, t, u, v = FMHEncode0.ar(junto, azim, el, dis);
			
			//	ambsinal = [w, x, y, u, v]; 
			ambsinal = [w, x, y, z, r, s, t, u, v]; 
			
			ambsinal1O = [w, x, y, z];
			
			//Out.ar( 0, FoaDecode.ar(ambsinal1O, ~decoder));
			//	Out.ar( 0, ambsinal);
			Out.ar( 2, ambsinal);
			
			
		}).add;

			//new
	// Spatialise B-format material and decode it for binaural
	// note this is sending things 

		
		SynthDef.new("espacAmbParaBin",  {
			

			arg el = 0, inbus, gbus, mx = -5000, my = -5000, mz = 0, dopon = 0,
			glev = 0, llev = 0;
			var w, x, y, z, r, s, t, u, v, p, ambsinal, ambsinal1O,
			junto, rd, dopplershift, azim, dis, xatras, yatras,  
			globallev = 0.0004, locallev, gsig, fonte, fontenova;
			var lrev, scale = 565;
			var grevganho = 0.20;
			
			mx = Lag.kr(mx, 2.0 * dopon);
			my = Lag.kr(my, 2.0 * dopon);
			mz = Lag.kr(mz, 2.0 * dopon);
				
			//		fonte = Point.new;
			fontenova = Cartesian.new;
			
			fontenova.set(mx, my, mz);
			dis = (1 - (fontenova.rho - scale)) / scale;
			azim = fontenova.theta;
			el = fontenova.phi;
			dis = Select.kr(dis < 0, [dis, 0]); 
			
			// atenuação de frequências agudas
			p = In.ar(inbus, 1);
			
			// undo
			p = LPF.ar(p, (dis) * 18000 + 2000); // attenuate high freq with distance
			
			
			// Reverberação global
			globallev = 1 / (1 - dis).sqrt;
			globallev = globallev - 1.0; // lower tail of curve to zero

			
			globallev = Select.kr(globallev > 1, [globallev, 1]); // verifica se o "sinal" está mais do que 1
			globallev = Select.kr(globallev < 0, [globallev, 0]);
			
			globallev = globallev * (glev*4);
			
			
			gsig = p * grevganho * globallev;

			
			
			Out.ar(gbus, gsig); //send part of direct signal global reverb synth
			
			// Reverberação local
			locallev = 1 - dis; 
			
			locallev = locallev  * (llev*4);
			
			
			lrev = PartConv.ar(p, fftsize, rirZspectrum.bufnum, 0.7 * locallev);
			
			junto = p + lrev;
			
			#w, x, y, z, r, s, t, u, v = FMHEncode0.ar(junto, azim, el, dis);
			
			//	ambsinal = [w, x, y, u, v]; 
			ambsinal = [w, x, y, z, r, s, t, u, v]; 
			
			ambsinal1O = [w, x, y, z];
			server.sync;		
			Out.ar( 0, FoaDecode.ar(ambsinal1O, binDecoder));
			
		}).add;


		

	
	SynthDef.new("espacBin",  {
		arg el = 0, inbus, gbus, mx = -5000, my = -5000, dopon = 0,
		glev = 0, llev = 0, dopamnt = 0, mz = 0;
		var w, x, y, z, r, s, t, u, v, p, ambsinal, ambsinal1O,
		junto, rd, dopplershift, azim, dis, 
		globallev = 0.0001, locallev, gsig, fonte, fontenova;
		//	bufL, bufR;
		var lrev, scale = 565;
		var grevganho = 0.04;  // needs less gain
		//	bufL = Bus.audio(s, 1);
		//	bufR = Bus.audio(s, 1);
		
		//	fonte = Point.new;
		fontenova = Cartesian.new;
		//	fonte.set((mx - 450) * -1, 450 - my);
		fonte.set(mx, my);
		fontenova.set(mx, my, mz);
		//SendTrig.kr(Impulse.kr(1),0,  mz); // debugging

		//		dis = (1 - (fonte.rho - scale)) / scale;
		dis = (1 - (fontenova.rho - scale)) / scale;


		//		azim = fonte.theta;
		azim = fontenova.theta;

		// new!

		el = fontenova.phi;
		//	SendTrig.kr(Impulse.kr(1),0,  el); // debugging

		
		dis = Select.kr(dis < 0, [dis, 0]); 
		//SendTrig.kr(Impulse.kr(1),0,  azim); // debugging
		
		// atenuação de frequências agudas
		p = In.ar(inbus, 1);
		p = LPF.ar(p, (dis) * 18000 + 2000); // attenuate high freq with distance
		
		// Doppler
		rd = (1 - dis) * 340; 
		rd = Lag.kr(rd, 1.0);

		//		SendTrig.kr(Impulse.kr(1),0,  dopamnt); // debugging
		dopplershift= DelayC.ar(p, 0.2, rd/1640.0 * dopon * dopamnt);
		p = dopplershift;
		
		// Reverberação global
		globallev = 1 / (1 - dis).sqrt;
		globallev = globallev - 1.0; // lower tail of curve to zero
		globallev = Select.kr(globallev > 1, [globallev, 1]); // verifica se o "sinal" está mais do que 1
		globallev = Select.kr(globallev < 0, [globallev, 0]);
		
		globallev = globallev * (glev*4);
		
		
		gsig = p * grevganho * globallev;
		Out.ar(gbus, gsig); //send part of direct signal global reverb synth
		
		// Reverberação local
		locallev = 1 - dis; 
		
		locallev = locallev  * (llev*4);
		
		
		lrev = PartConv.ar(p, fftsize, rirZspectrum.bufnum, 0.2 * locallev);
		junto = p + lrev;
		
		#w, x, y, z, r, s, t, u, v = FMHEncode0.ar(junto, azim, el, dis);
		
		//	ambsinal = [w, x, y, u, v]; 
		ambsinal = [w, x, y, z, r, s, t, u, v]; 
		
		ambsinal1O = [w, x, y, z];
			server.sync;		
		
		Out.ar( 0, FoaDecode.ar(ambsinal1O, binDecoder));
	}).add;


		SynthDef.new("espacAmbEstereo",  {
			arg el = 0, inbus, gbus, mx = -5000, my = -5000, mz = 0, angulo = 1.57,
			dopon = 0, dopamnt = 0, 
			glev = 0, llev = 0;
			var w, x, y, z, r, s, t, u, v, p, ambsinal,
			w1, x1, y1, z1, r1, s1, t1, u1, v1, p1, ambsinal1,
			w2, x2, y2, z2, r2, s2, t2, u2, v2, p2, ambsinal2,
			junto, rd, dopplershift, azim, dis, 
			junto1, azim1, 
			junto2, azim2, 
			globallev = 0.0001, locallev, gsig, fonte;
			var lrev, scale = 565;
			var grevganho = 0.20;
			
			//	fonte = Point.new;
			fonte = Cartesian.new;
			fonte.set(mx, my);
			
			
			
			azim1 = fonte.rotate(angulo / -2).theta;
			azim2 = fonte.rotate(angulo / 2).theta;
			
			fonte.set(mx, my, mz);
			el = fonte.phi;
			
			dis = (1 - (fonte.rho - scale)) / scale;
			
			dis = Select.kr(dis < 0, [dis, 0]); 
			//	SendTrig.kr(Impulse.kr(1),0,  el); // debugging
			
			// atenuação de frequências agudas
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
			//	SendTrig.kr(Impulse.kr(1),0,  gsig); // debugging
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
			Out.ar( 2, ambsinal1 + ambsinal2);
			
			
		}).add;
		

		SynthDef.new("espacBinEstereo",  {
			arg el = 0, inbus, gbus, mx = -5000, my = -5000, angulo = 1.57, dopon = 0,
			glev = 0, llev = 0, dopamnt = 0, mz = 0;
			var w, x, y, z, r, s, t, u, v, p, ambsinal,
			w1, x1, y1, z1, r1, s1, t1, u1, v1, p1, ambsinal1,
			w2, x2, y2, z2, r2, s2, t2, u2, v2, p2, ambsinal2,
			junto, rd, dopplershift, azim, dis, 
			junto1, azim1, 
			junto2, azim2, binsinal1, binsinal2,
			globallev = 0.0001, locallev, gsig, fonte, fontenova, angtmp, foo;
			var lrev, scale = 565;
			var grevganho = 0.20;
			
			fonte = Point.new;
			fontenova = Cartesian.new;
			//	foo = Cartesian.new;
			
			fonte.set(mx, my);
			//	fontenova.set(mx, my, mz);
			// because of an apparent bug in Cartesian and its rotate method, let's set the z coord later
			fontenova.set(mx, my);
			azim1 = fontenova.rotate(angulo / -2).theta;
			azim2 = fontenova.rotate(angulo / 2).theta;
			
			
			// OK, now we're done with rotate, reset z
			fontenova.set(mx, my, mz);
			
			dis = (1 - (fontenova.rho - scale)) / scale;
			
			dis = Select.kr(dis < 0, [dis, 0]);
			
			el = fontenova.phi; // elevation
			
			// atenuação de frequências agudas
			p = In.ar(inbus, 2);
			p = LPF.ar(p, (dis) * 18000 + 2000); // attenuate high freq with distance
			
			// Doppler
			rd = (1 - dis) * 340; 
			rd = Lag.kr(rd, 1.0);
			//		SendTrig.kr(Impulse.kr(1),0,  dopamnt); // debugging
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
			binsinal1 = [w1, x1, y1, z1];
			binsinal2 = [w2, x2, y2, z2];

			server.sync;		

			Out.ar( 0, FoaDecode.ar(binsinal1, binDecoder) + FoaDecode.ar(binsinal2, binDecoder));
			
			
		}).add;


		SynthDef.new(\arquivoLoop, { arg outbus, bufnum = 0, rate = 1, 
			volume = 0, tpos = 0, lp = 0;
			var sig;
			var scaledRate, player, spos;
			// 	spos = tpos * SampleRate.ir *  BufSampleRate.kr(bufnum) / SampleRate.ir;
			spos = tpos * BufSampleRate.kr(bufnum);
			//	SendTrig.kr(Impulse.kr(1),0,  spos); // debugging
			scaledRate = rate * BufRateScale.kr(bufnum);
			player = PlayBuf.ar(1, bufnum, scaledRate, startPos: spos, loop: lp, doneAction:2);
			Out.ar(outbus, player * volume)
		}).add;
		
		
		SynthDef.new(\tocarStreamMono, { arg outbus, busini = 0, volume = 0;
			var player;
			player =  SoundIn.ar(busini, 1);
			//SendTrig.kr(Impulse.kr(1),0,  player); // debugging
			Out.ar(outbus, player * volume);
		}).add;
		
		SynthDef.new(\tocarStreamEstereo, { arg outbus, busini, volume = 0;
			var player;
			player =  [SoundIn.ar(busini), SoundIn.ar(busini + 1)];
			Out.ar(outbus, player * volume);
		}).add;
		
		
		SynthDef.new(\arquivoLoopEst, { arg outbus, bufnum = 0, rate = 1, 
			volume = 0, tpos = 0, lp = 0;
			var scaledRate, player, spos;
			spos = tpos * BufSampleRate.kr(bufnum);
			scaledRate = rate * BufRateScale.kr(bufnum); 
			player = PlayBuf.ar(2, bufnum, scaledRate, startPos: spos, loop: lp, doneAction:2);
			Out.ar(outbus, player * volume);
		}).add;
		
		
		SynthDef.new(\tocarBFormatAmb, { arg outbus, bufnum = 0, rate = 1, 
			volume = 0, tpos = 0, lp = 0, rotAngle = 0, tilAngle = 0, tumAngle = 0,
			mx = 0, my = 0, mz = 0, gbfbus, glev, llev, directang = 0, contr, dopon, dopamnt;
			var scaledRate, player, wsinal, spos, pushang = 0,
			azim, dis = 1, fonte, scale = 565, globallev, locallev, 
			gsig, lsig, rd, dopplershift;
			var grevganho = 0.20;
			
			mx = Lag.kr(mx, 2.0 * dopon);
			my = Lag.kr(my, 2.0 * dopon);
			mz = Lag.kr(mz, 2.0 * dopon);
			
			fonte = Cartesian.new;
			fonte.set(mx, my, mz);
			dis = (1 - (fonte.rho - scale)) / scale;
			pushang = (1 - dis) * pi / 2; // grau de deslocamento do campo sonoro. 0 = centrado. pi/2 = 100% deslocado
			azim = fonte.theta; // ângulo (azimuth) de deslocamento
			dis = Select.kr(dis < 0, [dis, 0]); 
			// 	spos = tpos * SampleRate.ir;
			spos = tpos * BufSampleRate.kr(bufnum);
			scaledRate = rate * BufRateScale.kr(bufnum); 
			player = PlayBuf.ar(4, bufnum, scaledRate, startPos: spos, loop: lp, doneAction:2);
			
			rd = (1 - dis) * 340; 
			dopplershift= DelayC.ar(player, 0.2, rd/1640.0 * dopon * dopamnt);
			player = dopplershift;
			
			wsinal = player[0] * contr * volume * dis * 2.0;
			
			Out.ar(outbus, wsinal);
			//	~teste = contr * volume * dis;
			
			//	SendTrig.kr(Impulse.kr(1),0, ~teste); // debugging
			
			player = FoaDirectO.ar(player, directang); // diretividade ("tamanho")
			
			player = FoaTransform.ar(player, 'rotate', rotAngle, volume * dis * (1 - contr));
			player = FoaTransform.ar(player, 'push', pushang, azim);
			Out.ar(2, player);
			
			// Reverberação global
			globallev = 1 / (1 - dis).sqrt;
			globallev = globallev - 1.0; // lower tail of curve to zero
			globallev = Select.kr(globallev > 1, [globallev, 1]); // verifica se o "sinal" está mais do que 1
			globallev = Select.kr(globallev < 0, [globallev, 0]); 
			globallev = globallev * (glev* 6) * grevganho;
			
			gsig = player * globallev;
			
			locallev = 1 - dis; 
			
			locallev = locallev  * (llev*18) * grevganho;
			lsig = player * locallev;
			
			
			Out.ar(gbfbus, gsig + lsig); //send part of direct signal global reverb synth
			
			
		}).add;

		

		
		SynthDef.new(\tocarStreamBFormatAmb, {
			// another near copy!
			arg outbus, volume = 0, rotAngle = 0, tilAngle = 0, tumAngle = 0,
			mx = 0, my = 0, mz = 0, gbfbus, glev, llev, directang = 0, contr, dopon, dopamnt, busini = 0;
			var scaledRate, player, wsinal, spos, pushang = 0,
			azim, dis = 1, fonte, scale = 565, globallev, locallev, 
			gsig, lsig, rd, dopplershift;
			var grevganho = 0.20;
			
			mx = Lag.kr(mx, 2.0 * dopon);
			my = Lag.kr(my, 2.0 * dopon);
			mz = Lag.kr(mz, 2.0 * dopon);
			
			// 	fonte = Point.new;
			//	fonte.set(mx, my);
			fonte = Cartesian.new;
			fonte.set(mx, my, mz);
			dis = (1 - (fonte.rho - scale)) / scale;
			pushang = (1 - dis) * pi / 2; // grau de eslocamento do campo sonoro. 0 = centrado. pi/2 = 100% deslocado
			azim = fonte.theta; // ângulo (azimuth) de deslocamento
			dis = Select.kr(dis < 0, [dis, 0]); 
			// 	spos = tpos * SampleRate.ir;
			
			//spos = tpos * BufSampleRate.kr(bufnum);
			//scaledRate = rate * BufRateScale.kr(bufnum); 
			
			
			//	player = PlayBuf.ar(4, bufnum, scaledRate, startPos: spos, loop: lp, doneAction:2);
			player =  [SoundIn.ar(busini), SoundIn.ar(busini + 1), SoundIn.ar(busini + 2), SoundIn.ar(busini + 3)];
			
			rd = (1 - dis) * 340; 
			dopplershift= DelayC.ar(player, 0.2, rd/1640.0 * dopon * dopamnt);
			player = dopplershift;
			
			wsinal = player[0] * contr * volume * dis * 2.0;
			
			Out.ar(outbus, wsinal);
			//~teste = contr * volume * dis;
			
			//	SendTrig.kr(Impulse.kr(1),0, ~teste); // debugging
			
			player = FoaDirectO.ar(player, directang); // diretividade ("tamanho")
			
			player = FoaTransform.ar(player, 'rotate', rotAngle, volume * dis * (1 - contr));
			player = FoaTransform.ar(player, 'push', pushang, azim);
			Out.ar(2, player);
			
			// Reverberação global
			globallev = 1 / (1 - dis).sqrt;
			globallev = globallev - 1.0; // lower tail of curve to zero
			globallev = Select.kr(globallev > 1, [globallev, 1]); // verifica se o "sinal" está mais do que 1
			globallev = Select.kr(globallev < 0, [globallev, 0]); 
			globallev = globallev * (glev* 6) * grevganho;
			
			gsig = player * globallev;
			
			
			
			locallev = 1 - dis; 
			
			locallev = locallev  * (llev*18) * grevganho;
			lsig = player * locallev;
			
			
			Out.ar(gbfbus, gsig + lsig); //send part of direct signal global reverb synth
			
			
		}).add;

		SynthDef.new(\tocarStreamBFormatBin, { arg outbus, 
			volume = 0, rotAngle = 0, tilAngle = 0, tumAngle = 0,
			mx = 0, my = 0, mz = 0, gbfbus, gbusout, glev, llev, directang = 0, contr,
			dopon, dopamnt = 0, busini = 0;
			
			var player, wsinal, pushang = 0,
			azim, dis = 1, fonte, scale = 565, globallev, locallev, 
			gsig, lsig, el,
			rd, dopplershift;
			var grevganho = 0.20;
			// 	fonte = Point.new;
			
			mx = Lag.kr(mx, 2.0 * dopon);
			my = Lag.kr(my, 2.0 * dopon);
			mz = Lag.kr(mz, 2.0 * dopon);
			
			fonte = Cartesian.new;
			fonte.set(mx, my, mz);
			dis = (1 - (fonte.rho - scale)) / scale;
			pushang = (1 - dis) * pi / 2; // grau de deslocamento do campo sonoro. 0 = centrado. pi/2 = 100% deslocado
			azim = fonte.theta; // ângulo (azimuth) de deslocamento
			
			el = fonte.phi;
			
			dis = Select.kr(dis < 0, [dis, 0]); 
			//	spos = tpos * BufSampleRate.kr(bufnum);
			//	scaledRate = rate * BufRateScale.kr(bufnum);
			
			player =  [SoundIn.ar(busini), SoundIn.ar(busini + 1), SoundIn.ar(busini + 2), SoundIn.ar(busini + 3)];
			//player = [WhiteNoise.ar(0.1), WhiteNoise.ar(0.1), WhiteNoise.ar(0.1), WhiteNoise.ar(0.1)];
			//player = PlayBuf.ar(4, bufnum, scaledRate, startPos: spos, loop: lp, doneAction:2);
			
			rd = (1 - dis) * 340; 
			dopplershift= DelayC.ar(player, 0.2, rd/1640.0 * dopon * dopamnt);
			player = dopplershift;
			
			
			//wsinal = player[0] * contr * volume * dis * 2.0; // why is contr (contração?) being used here?
			// it's being used because this wsinal is being incorrectly used for global reverb too. This should
			// only be used for contraction
			
			//		 wsinal = player[0] * volume * dis * 2.0;
			wsinal = player[0] * volume * contr * dis * 2.0;
			// SendTrig.kr(Impulse.kr(1),0, wsinal); // debugging
			//	Out.ar(gbusout wsinal);
			
			// this is the "contracted" component of the global reverb
			// and has a mono source derived from W. The full B-format or non-contracted
			// component is sent to the global b-format binaral reverberator. See "gsig" below.
			
			Out.ar(outbus, wsinal);
			//		Out.ar(outbus, wsinal * contr);
			//	~teste = contr * volume * dis;
			
			// 		SendTrig.kr(Impulse.kr(1),0, contr); // debugging
			
			player = FoaDirectO.ar(player, directang); // diretividade ("tamanho")

			player = FoaTransform.ar(player, 'rotate', rotAngle, volume * dis * (1 - contr));
			player = FoaTransform.ar(player, 'push', pushang, azim, el);
			
			
			//		 Decode direct signal

			server.sync;
			Out.ar( 0, FoaDecode.ar(player, binDecoder));
			
			// Reverberação global
			globallev = 1 / (1 - dis).sqrt;
			globallev = globallev - 1.0; // lower tail of curve to zero
			globallev = Select.kr(globallev > 1, [globallev, 1]); // verifica se o "sinal" está mais do que 1
			globallev = Select.kr(globallev < 0, [globallev, 0]); 
			globallev = globallev * (glev*4) * grevganho;
			
			gsig = player * globallev;
			
			
			
			locallev = 1 - dis; 
			
			locallev = locallev  * (llev*12) * grevganho;
			lsig = player * locallev;
			
			// SendTrig.kr(Impulse.kr(1),0, gsig); // debugging
			
			Out.ar(gbfbus, gsig + lsig); //send part of direct signal global reverb synth
			

		}).add;

				^super.newCopyArgs(rirWXYZ, rirBinaural, subjectID, server);

	} // end new
	
	openGui {

		arg nfontes = 1, dur = 60;
		var fonte, dist, scale = 565, espacializador, mbus, sbus, ncanais, synt, fatual = 0, 
		itensdemenu, gbus, gbfbus, azimuth, event, brec, bplay, bload, sombuf, funcs, 
		dopcheque,
		loopcheck, lpcheck, lp,
		streamcheck, strmcheck, strm,
		dopcheque2, doppler, angulo, volume, glev, 
		llev, angnumbox, volnumbox,
		ncannumbox, busininumbox, // for streams. ncan = number of channels (1, 2 or 4)
		// busini = initial bus number in range starting with "0"
		ncanbox, businibox, ncan, busini,
		novoplot,
		
		dopnumbox, volslider, dirnumbox, dirslider, connumbox, conslider, cbox,
		angslider, bsalvar, bcarregar, bdados, xbox, ybox, abox, vbox, gbox, lbox, dbox, dpbox, dcheck,
		gslider, gnumbox, lslider, lnumbox, tfield, dopflag = 0, btestar, tocar, isPlay, isRec,
		atualizarvariaveis,
		testado,
		rnumbox, rslider, rbox, 
		znumbox, zslider, zbox, zlev, // z-axis
		rlev, dlev, clev, cslider, dplev, dpslider, cnumbox,
		bAmbBinaural, render = "binaural";
		espacializador = Array.newClear(nfontes);
		//	espacializador2 = Array.newClear(nfontes); // used when b-format file is rendered as binaural
		doppler = Array.newClear(nfontes); 
		lp = Array.newClear(nfontes); 
		strm = Array.newClear(nfontes); 
		mbus = Array.newClear(nfontes); 
		sbus = Array.newClear(nfontes); 
		//	bfbus = Array.newClear(nfontes); 
		ncanais = Array.newClear(nfontes);  // 0 = não, nem estéreo. 1 = mono. 2 = estéreo.
		ncan = Array.newClear(nfontes);  // 0 = não, nem estéreo. 1 = mono. 2 = estéreo.
		// note that ncan refers to # of channels in streamed sources.
		// ncanais is related to sources read from file
		busini = Array.newClear(nfontes); // initial bus # in streamed audio grouping (ie. mono, stereo or b-format)
		sombuf = Array.newClear(nfontes); 
		synt = Array.newClear(nfontes);
		sprite = Array2D.new(nfontes, 2);
		funcs = Array.newClear(nfontes);
		angulo = Array.newClear(nfontes); // ângulo dos canais estereofônicos
		zlev = Array.newClear(nfontes); 
		volume = Array.newClear(nfontes); 
		//	doplev = Array.newClear(nfontes); 
		glev = Array.newClear(nfontes); 
		llev = Array.newClear(nfontes); 
		rlev = Array.newClear(nfontes); 
		dlev = Array.newClear(nfontes); 
		dplev = Array.newClear(nfontes); 
		clev = Array.newClear(nfontes); 

		ncanbox = Array.newClear(nfontes); 
		businibox = Array.newClear(nfontes); 
		
		
		xbox = Array.newClear(nfontes); 
		zbox = Array.newClear(nfontes); 
		ybox = Array.newClear(nfontes); 
		abox = Array.newClear(nfontes); // ângulo
		vbox = Array.newClear(nfontes);  // volume
		dcheck = Array.newClear(nfontes);  // Doppler check
		gbox = Array.newClear(nfontes); // reverberação global
		lbox = Array.newClear(nfontes); // reverberação local
		rbox = Array.newClear(nfontes); // rotação de b-format
		dbox = Array.newClear(nfontes); // diretividade de b-format
		cbox = Array.newClear(nfontes); // contrair b-format
		dpbox = Array.newClear(nfontes); // dop amount
		lpcheck = Array.newClear(nfontes); // loop
		strmcheck = Array.newClear(nfontes); // stream check
		tfield = Array.newClear(nfontes);
		
		testado = Array.newClear(nfontes);
		
		//GUI.swing; 
		
		//	~testek = Array.newClear(nfontes); 
		//	nfontes.do { arg i;
		//		funcs[i] = List[];
		//		kespac[i] = KtlLoop(\espac++i, { |ev| funcs[i].do(_.value(ev) ) });
		//		~testek[i] = KtlLoop(\espac++i, { |ev| funcs[i].do(_.value(ev) ) });
		//		funcs[i].add({ |ev|  espacializador[i].set(\mx, ev.x, \my, ev.y) });
		//		funcs[i].add({ |ev|  ~plotter.value(ev.x, ev.y, i, nfontes) });
		//	};
		
		
		nfontes.do { arg i;
		doppler[i] = 0;
			angulo[i] = 0;
			volume[i] = 0;
			glev[i] = 0;
			llev[i] = 0;
			lp[i] = 0;
			strm[i] = 0;
			rlev[i] = 0;
			dlev[i] = 0;
			clev[i] = 0;
			zlev[i] = 0;
			dplev[i] = 0;
			
			ncan[i] = 0;
			busini[i] = 0;
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
		gbfbus = Bus.audio(server, 4); // global b-format bus
		
		fonte = Point.new;
		win = Window.new("Mosca", Rect(0, 900, 900, 900)).front;
		wdados = Window.new("Data", Rect(900, 900, 960, (nfontes*20)+60 ));
		wdados.userCanClose = false;
		
		
		bdados = Button(win, Rect(280, 30, 90, 20))
		.states_([
			["open data", Color.black, Color.white],
			["close data", Color.white, Color.blue]
		])
		.action_({ arg but;
			//	but.value.postln;
			if(but.value == 1)
			{wdados.front;}
			{wdados.visible = false;};
		});
		
		
		bAmbBinaural = Button(win, Rect(370, 50, 90, 20))
		.states_([
			["binaural", Color.black, Color.white], ["ambisonic", Color.black, Color.white]
		])
		.action_({ arg but;
			//	but.value.postln;
			if(but.value == 1)
			{
				controle.stop;
				revGlobal.free;
				render = "ambisonic";
			}
			{
				controle.stop;
				revGlobal.free;
				render = "binaural";
			};
		});
		
		atualizarvariaveis = {
			"atualizando!".postln;
			
			nfontes.do { arg i;
				
				if(espacializador[i] != nil) {
					("atualizando espacializador # " ++ i).postln;
					espacializador[i].set(
						//	\mx, num.value  ???
						\dopon, doppler[i], // not needed...
						\angulo, angulo[i],
						\volume, volume[i], // ? or in player?
						\dopamnt, dplev[i],
						\glev, glev[i],
						\llev, llev[i],
						\mx, xbox[i].value,
						\my, ybox[i].value,
						\mz, zbox[i].value
					);
				};
				
				if(synt[i] != nil){
					synt[i].set(
						\volume, volume[i],
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
			if ((path != "") && (strmcheck[i].value == false)) {
				{	
					
					if (sombuf[i].numChannels == 1)  // arquivo mono
					{ncanais[i] = 1;
						angulo[i] = 0;
						{angnumbox.value = 0;}.defer;
						{angslider.value = 0;}.defer;
						
						if(render == "ambisonic") {
							if(revGlobal == nil) {
								revGlobal = Synth.new(\revGlobalAmb, [\gbus, gbus], addAction:\addToTail);
							};
							if (testado[i] == false) { // if source is testing don't relaunch synths
								synt[i] = Synth.new(\arquivoLoop, [\outbus, mbus[i], 
									\bufnum, sombuf[i].bufnum, \rate, 1, \tpos, tpos, \lp, lp[i], \volume, volume[i]], 
									revGlobal, addAction: \addBefore).onFree({espacializador[i].free;
										espacializador[i] = nil; synt[i] = nil});
								
								espacializador[i] = Synth.new(\espacAmb, [\inbus, mbus[i], 
									\gbus, gbus, \dopon, doppler[i]], 
									synt[i], addAction: \addAfter);
							};
							atualizarvariaveis.value;
							
						} {
							if(revGlobal == nil){
								revGlobal = Synth.new(\revGlobalBinaural, [\gbus, gbus], addAction:\addToTail);
							};
							
							if (testado[i] == false) { // if source is testing don't relaunch synths
								"Here I am!".postln;
								synt[i] = Synth.new(\arquivoLoop, [\outbus, mbus[i], 
									\bufnum, sombuf[i].bufnum, \rate, 1, \tpos, tpos, \lp, lp[i], \volume, volume[i]], 
									revGlobal, addAction: \addBefore).onFree({espacializador[i].free;
										espacializador[i] = nil; synt[i] = nil});
								
								espacializador[i] = Synth.new(\espacBin, [\inbus, mbus[i], 
									\gbus, gbus, \dopon, doppler[i]], 
									synt[i], addAction: \addAfter);
							};
							
							atualizarvariaveis.value;
						};
					}
					{if (sombuf[i].numChannels == 2) {ncanais[i] = 2; // arquivo estéreo
						angulo[i] = pi/2;
						{angnumbox.value = pi/2;}.defer;
						{angslider.value = 0.5;}.defer;
						
						if(render == "ambisonic") {
							
							if(revGlobal == nil){
								revGlobal = Synth.new(\revGlobalAmb, [\gbus, gbus], addAction:\addToTail);
							};
							if (testado[i] == false) {
								synt[i] = Synth.new(\arquivoLoopEst, [\outbus, sbus[i], 
									\bufnum, sombuf[i].bufnum, \rate, 1, \tpos, tpos, \lp, lp[i], \volume, volume[i]], 
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
							if(revGlobal == nil){
								revGlobal = Synth.new(\revGlobalBinaural, [\gbus, gbus], addAction:\addToTail);
							};
							if (testado[i] == false) {
								synt[i] = Synth.new(\arquivoLoopEst, [\outbus, sbus[i], 
									\bufnum, sombuf[i].bufnum, \rate, 1, \tpos, tpos, \lp, lp[i], \volume, volume[i]], 
									revGlobal, addAction: \addBefore).onFree({espacializador[i].free;
										espacializador[i] = nil; synt[i] = nil});
								
								espacializador[i] = Synth.new(\espacBinEstereo, [\inbus, sbus[i], \gbus, gbus,
									\dopon, doppler[i]], 
									synt[i], addAction: \addAfter);
							};
							atualizarvariaveis.value;
						};
					} {
						if (sombuf[i].numChannels == 4) {
							"B-format".postln;
							ncanais[i] = 4;
							angulo[i] = 0;
							{angnumbox.value = 0;}.defer;
							{angslider.value = 0;}.defer;
							
							// B-format file with ambisonic render
							if(render == "ambisonic") {
								
								//	~revGlobal = Synth.new(\revGlobalAmb, [\gbus, gbus], addAction:\addToTail);
								// reverb for non-contracted (full b-format) component
								if(revGlobalBF == nil){
									revGlobalBF = Synth.new(\revGlobalBFormatAmb, [\gbfbus, gbfbus], addAction:\addToTail);
								};
								// reverb for contracted (mono) component
								if(revGlobal == nil){
									revGlobal = Synth.new(\revGlobalAmb, [\gbus, gbus], addAction:\addToTail);
								};
								if (testado[i] == false) {
									synt[i] = Synth.new(\tocarBFormatAmb, [\gbfbus, gbfbus, \outbus, mbus[i],
										\bufnum, sombuf[i].bufnum, \contr, clev[i], \rate, 1, \tpos, tpos, \lp,
										lp[i], \volume, volume[i], \dopon, doppler[i]], 
										//					~revGlobal, addAction: \addBefore);
										revGlobalBF, addAction: \addBefore).onFree({espacializador[i].free;
											espacializador[i] = nil; synt[i] = nil;});
									//	xbox[i].valueAction = 1; // é preciso para aplicar rev global sem mexer com mouse
									
									espacializador[i] = Synth.new(\espacAmb2, [\inbus, mbus[i], \gbus, gbus, 
										\dopon, doppler[i]], 
										synt[i], addAction: \addAfter);
								};
								atualizarvariaveis.value;
								
							} {
								
								// B-format file with binaural render
								// reverb for contracted (mono) component
								if(revGlobal == nil){
									revGlobal = Synth.new(\revGlobalBinaural, [\gbus, gbus], addAction:\addToTail);
								};
								// reverb for non-contracted (full b-format) component
								if(revGlobalBF == nil){
									revGlobalBF = Synth.new(\revGlobalBFormatBin, [\gbfbus, gbfbus], addAction:\addToTail);
								};
								if (testado[i] == false) {
									synt[i] = Synth.new(\tocarBFormatBin, [\gbfbus, gbfbus, \outbus, mbus[i],
										\bufnum, sombuf[i].bufnum, \contr, clev[i], \rate, 1, \tpos, tpos,
										\lp, lp[i], \volume, volume[i], \dopon, doppler[i]], 
										revGlobal, addAction: \addBefore).onFree({espacializador[i].free;
											espacializador[i] = nil; synt[i] = nil;});
									
									// is this still being used?
									
									espacializador[i] = Synth.new(\espacAmbParaBin, [\inbus, mbus[i], \gbus, gbus, 
										\dopon, doppler[i]], 
										synt[i], addAction: \addAfter);
								};
								atualizarvariaveis.value;
								
								
							};
							
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
				if (strmcheck[i].value) {
					var x;
					("Streaming! ncan = " ++ ncan[i]
						++ " & busini = " ++ busini[i]).postln;
					x = case
					{ ncan[i] == 1 } {
						"Mono!".postln;
						if (render == "binaural")
						{
							if(revGlobal == nil){
								revGlobal = Synth.new(\revGlobalBinaural, [\gbus, gbus], addAction:\addToTail);
							};
							if (testado[i] == false) {
								synt[i] = Synth.new(\tocarStreamMono, [\outbus, mbus[i], \busini, busini[i],
									\volume, volume[i]], revGlobal,
									addAction: \addBefore).onFree({espacializador[i].free;
										espacializador[i] = nil; synt[i] = nil});
								
								
								espacializador[i] = Synth.new(\espacBin, [\inbus, mbus[i], 
									\gbus, gbus, \dopon, doppler[i]], 
									synt[i], addAction: \addAfter);
							};
							atualizarvariaveis.value;
							
						}{
							if(revGlobal == nil){
								revGlobal = Synth.new(\revGlobalAmb, [\gbus, gbus], addAction:\addToTail);
							};
							if (testado[i] == false) {
								synt[i] = Synth.new(\tocarStreamMono, [\outbus, mbus[i], \busini, busini[i],
									\volume, volume[i]], revGlobal,
									addAction: \addBefore).onFree({espacializador[i].free;
										espacializador[i] = nil; synt[i] = nil});
								
								
								espacializador[i] = Synth.new(\espacAmb, [\inbus, mbus[i], 
									\gbus, gbus, \dopon, doppler[i]], 
									synt[i], addAction: \addAfter);
							};
							atualizarvariaveis.value;
							
						};
					}
					{ ncan[i] == 2 } {
						"Estéreo!".postln;
						ncanais[i] = 0; // just in case!
						angulo[i] = pi/2;
						{angnumbox.value = pi/2;}.defer;
						{angslider.value = 0.5;}.defer;
						
						if (render == "binaural")
						{
							if(revGlobal == nil){
								revGlobal = Synth.new(\revGlobalBinaural, [\gbus, gbus], addAction:\addToTail);
							};
							if (testado[i] == false) {
								synt[i] = Synth.new(\tocarStreamEstereo, [\outbus, sbus[i], \busini, busini[i],
									\volume, volume[i]], revGlobal,
									addAction: \addBefore).onFree({espacializador[i].free;
										espacializador[i] = nil; synt[i] = nil});
								
								espacializador[i] = Synth.new(\espacBinEstereo, [\inbus, sbus[i], \gbus, gbus,
									\dopon, doppler[i]], 
									synt[i], addAction: \addAfter);
							};
							atualizarvariaveis.value;
							
						}{
							if(revGlobal == nil){
								revGlobal = Synth.new(\revGlobalAmb, [\gbus, gbus], addAction:\addToTail);
							};
							if (testado[i] == false) {
								synt[i] = Synth.new(\tocarStreamEstereo, [\outbus, sbus[i], \busini, busini[i],
									\volume, volume[i]], revGlobal,
									addAction: \addBefore).onFree({espacializador[i].free;
										espacializador[i] = nil; synt[i] = nil});
								
								espacializador[i] = Synth.new(\espacAmbEstereo, [\inbus, sbus[i], \gbus, gbus,
									\dopon, doppler[i]], 
									synt[i], addAction: \addAfter);
							};
							atualizarvariaveis.value;
							
						};
					}
					{ ncan[i] == 4 } {
						//"B-format!".postln;
						if (render == "binaural")
						{
							"B-format binaural!!!".postln;
							// B-format file with binaural render
							// reverb for contracted (mono) component
							if(revGlobal == nil){
								revGlobal = Synth.new(\revGlobalBinaural, [\gbus, gbus], addAction:\addToTail);
							};
							// reverb for non-contracted (full b-format) component
							if(revGlobalBF == nil){
								revGlobalBF = Synth.new(\revGlobalBFormatBin, [\gbfbus, gbfbus], addAction:\addToTail);
							};
							if (testado[i] == false) {
								synt[i] = Synth.new(\tocarStreamBFormatBin, [\gbfbus, gbfbus, \outbus, mbus[i],
									\contr, clev[i], \volume, volume[i], \dopon, doppler[i], \busini, busini[i]], 
									revGlobal, addAction: \addBefore).onFree({espacializador[i].free;
										espacializador[i] = nil; synt[i] = nil;});
								espacializador[i] = Synth.new(\espacAmbParaBin, [\inbus, mbus[i], \gbus, gbus, 
									\dopon, doppler[i]], synt[i], addAction: \addAfter);
							};
							atualizarvariaveis.value;
							
						}{
							"B-format ambisonic!!!".postln;
							//	~revGlobal = Synth.new(\revGlobalAmb, [\gbus, gbus], addAction:\addToTail);
							// reverb for non-contracted (full b-format) component
							if(revGlobalBF == nil){
								revGlobalBF = Synth.new(\revGlobalBFormatAmb, [\gbfbus, gbfbus], addAction:\addToTail);
							};
							// reverb for contracted (mono) component
							if(revGlobal == nil){
								revGlobal = Synth.new(\revGlobalAmb, [\gbus, gbus], addAction:\addToTail);
							};
							
							if (testado[i] == false) {
								synt[i] = Synth.new(\tocarStreamBFormatAmb, [\gbfbus, gbfbus, \outbus, mbus[i],
									\contr, clev[i], \rate, 1, \tpos, tpos, \volume, volume[i], \dopon, doppler[i],
									\busini, busini[i]], 
									revGlobalBF, addAction: \addBefore).onFree({espacializador[i].free;
										espacializador[i] = nil; synt[i] = nil;});
								
								espacializador[i] = Synth.new(\espacAmb2, [\inbus, mbus[i], \gbus, gbus, 
									\dopon, doppler[i]], 
									synt[i], addAction: \addAfter);
							};
							atualizarvariaveis.value;
							
						};
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
			if(but.value == 1)
			{
				tocar.value(fatual, 0);
				//		~testado = fatual;
				testado[fatual] = true;
				
			}
			{
				//	("rrrrrrr: " ++ synt[~testado]).postln;
				
				//		synt[~testado].free;
				synt[fatual].free;
				synt[fatual] = nil;
				testado[fatual] = false;
				
				
				
				// STOP TEST
				
				//		~revGlobal.free;
				//		~revGlobalBF.free;
				//		~revGlobal = nil;
				//		~revGlobalBF = nil;
				
				//now done elsewhere
				//espacializador[~testado].free;
			};
		});
		
		
		
		
		bsalvar = Button(win, Rect(670, 40, 90, 20))
		.states_([
			["save auto", Color.black, Color.white],
			
		])
		.action_({ arg but;
			var arquivo = File((prjDr ++ "/auto/arquivos.txt").standardizePath,"w");
			var dop = File((prjDr ++ "/auto/doppler.txt").standardizePath,"w");
			var looped = File((prjDr ++ "/auto/loop.txt").standardizePath,"w");
			var streamed = File((prjDr ++ "/auto/stream.txt").standardizePath,"w");
			var string;
			("Arg is " ++ but.value.asString).postln;
			string = nil;
			nfontes.do { arg i;
				("textfield = " ++ tfield[i].value).postln;
				
				if(tfield[i].value != "") {arquivo.write(tfield[i].value ++ "\n")} {arquivo.write("NULL\n")};
				
				dop.write(doppler[i].value.asString ++ "\n");
				looped.write(lp[i].value.asString ++ "\n");
				if(strm[i].value > 0)
				{
					
					streamed.write(ncan[i].asString ++ Char.tab ++  busini[i].asString ++ "\n");
				}
				{streamed.write("NULL\n")};
			};
			arquivo.close;
			dop.close;
			looped.close;
			streamed.close;
			controle.save(controle.presetDir);
			
		});
		
		
		
		
		bcarregar = Button(win, Rect(760, 40, 90, 20))
		.states_([
			["load auto", Color.black, Color.white],
		])
		.action_({ arg but;
			var f;
			var arquivo = File((prjDr ++ "/auto/arquivos.txt").standardizePath,"r");
			var dop = File((prjDr ++ "/auto/doppler.txt").standardizePath,"r");
			var looped = File((prjDr ++ "/auto/loop.txt").standardizePath,"r");
			var streamed = FileReader((prjDr ++ "/auto/stream.txt").standardizePath, delimiter: Char.tab); 
			
			
			but.value.postln;
			nfontes.do { arg i;
				var line = arquivo.getLine(1024);
				if(line!="NULL"){tfield[i].valueAction = line};
				
				line = dop.getLine(1024);
				//		doppler[i] = line;
				dcheck[i].valueAction = line;
				
				line = looped.getLine(1024);
				//			lp[i] = line;
				lpcheck[i].valueAction = line;
				
				f = File(prjDr ++ "/auto/stream.txt", "r"); f.isOpen;
				
				// streamed stuff
				line = streamed.next;
				if(line[0] != "NULL"){
					strmcheck[i].valueAction = true;
					// ("Linha " ++ i.asString ++ " = " ++ line[0] ++ " e " ++ line[1]).postln;
					ncanbox[i].valueAction = line[0].asFloat;
					businibox[i].valueAction = line[1].asFloat;
				};
				
			};
			arquivo.close;		
			dop.close;
			looped.close;
			f.close;
			streamed.close;
			controle.load(controle.presetDir);
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
		itensdemenu = Array.newClear(nfontes);
		nfontes.do { arg i;
			itensdemenu[i] = "Source " ++ (i + 1).asString;
		};
		
		m = PopUpMenu(win,Rect(10,10,80,20));
		m.items = itensdemenu; 
		m.action = { arg menu;
			fatual = menu.value;
			
			if(doppler[fatual] == 1){dopcheque.value = true}{dopcheque.value = false};
			if(lp[fatual] == 1){loopcheck.value = true}{loopcheck.value = false};
			
			if(strm[fatual] == 1){streamcheck.value = true}{streamcheck.value = false};
			
			angnumbox.value = angulo[fatual];
			angslider.value = angulo[fatual] / pi;
			volnumbox.value = volume[fatual];
			dopnumbox.value = dplev[fatual];
			volslider.value = volume[fatual];
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
			
			ncannumbox.value = ncan[fatual];
			busininumbox.value = busini[fatual];
			
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
		
		
		dopcheque = CheckBox( win, Rect(94, 10, 80, 20), "Doppler").action_({ arg butt;
			("Doppler is " ++ butt.value).postln;
			dcheck[fatual].valueAction = butt.value;
		});
		dopcheque.value = false;
		
		loopcheck = CheckBox( win, Rect(164, 10, 80, 20), "Loop").action_({ arg butt;
			("Loop is " ++ butt.value).postln;
			lpcheck[fatual].valueAction = butt.value;
		});
		dopcheque.value = false;
		
		
		streamcheck = CheckBox( win, Rect(10, 30, 100, 20), "Live Input").action_({ arg butt;
			("Streaming is " ++ butt.value).postln;
			strmcheck[fatual].valueAction = butt.value;
		});
		dopcheque.value = false;
		
		
		
		//	~ncantexto = StaticText(~win, Rect(55, -10 + ~offset, 200, 20));
		//	~ncantexto.string = "No. of channels (1, 2 or 4, Live)";
		textbuf = StaticText(win, Rect(55, -10 + offset, 200, 20));
		textbuf.string = "No. of channels (1, 2 or 4, Live)";
		ncannumbox = NumberBox(win, Rect(10, -10 + offset, 40, 20));
		ncannumbox.value = 0;
		ncannumbox.clipHi = 4;
		ncannumbox.clipLo = 0;
		//angnumbox.step_(0.1); 
		//angnumbox.scroll_step=0.1;
		ncannumbox.align = \center;
		ncannumbox.action = {arg num;
			
			
			ncanbox[fatual].valueAction = num.value;
			ncan[fatual] = num.value;
			
		};
		
		
		
		textbuf = StaticText(win, Rect(55, 10 + offset, 240, 20));
		textbuf.string = "Start Bus (0-31, Live)";
		busininumbox = NumberBox(win, Rect(10, 10 + offset, 40, 20));
		busininumbox.value = 0;
		busininumbox.clipHi = 31;
		busininumbox.clipLo = 0;
		//angnumbox.step_(0.1); 
		//angnumbox.scroll_step=0.1;
		busininumbox.align = \center;
		busininumbox.action = {arg num; 
			businibox[fatual].valueAction = num.value;
			busini[fatual] = num.value;
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
			abox[fatual].valueAction = num.value;
			if((ncanais[fatual]==2) || (ncan[fatual]==2)){
				espacializador[fatual].set(\angulo, num.value);
				angulo[fatual] = num.value;
				("ângulo = " ++ num.value).postln; 
			}
			{angnumbox.value = 0;};
		};
		
		angslider = Slider.new(win, Rect(50, 110 + offset, 110, 20));
		//	b = ControlSpec(0.0, 3.14, \linear, 0.01); // min, max, mapping, step
		
		angslider.action = {arg num;
			abox[fatual].valueAction = num.value * pi;
			if((ncanais[fatual]==2) || (ncan[fatual]==2)) {
				angnumbox.value = num.value * pi;
				//			espacializador[fatual].set(\angulo, b.map(num.value));
				espacializador[fatual].set(\angulo, num.value * pi);
				//			angulo[fatual] = b.map(num.value);
				angulo[fatual] = num.value * pi;
			}{angnumbox.value = num.value * pi;};
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
			zbox[fatual].valueAction = num.value;
			if(ncanais[fatual]==2){
				espacializador[fatual].set(\elev, num.value);
				zlev[fatual] = num.value;
				("Z-Axis = " ++ num.value).postln; 
			}
			{znumbox.value = 0;};
		};
		
	//	elslider = Slider.new(~win, Rect(865, 200, 20, 500));
	//	b = ControlSpec(0.0, 3.14, \linear, 0.01); // min, max, mapping, step
	
	/*	elslider.action = {arg num;
		elbox[fatual].valueAction = num.value;
		if(ncanais[fatual]==2) {
			elnumbox.value = num.value * pi;
			//			espacializador[fatual].set(\angulo, b.map(num.value));
			espacializador[fatual].set(\elev, num.value * pi);
			//			angulo[fatual] = b.map(num.value);
			elev[fatual] = num.value * pi;
		}{elnumbox.value = num.value * pi;};
	};
	*/
	
	zslider = Slider.new(win, Rect(855, 200, 20, 500));
	zslider.value = 0.5;
	zslider.action = {arg num;
		znumbox.value = (450 - (num.value * 900)) * -1;
		zbox[fatual].valueAction = znumbox.value;
		zlev[fatual] = znumbox.value;
		
		
	};




	////////////////////////////////////////////////////////////

			
	textbuf = StaticText(win, Rect(163, 30 + offset, 50, 20));
	textbuf.string = "Volume";
	volnumbox = NumberBox(win, Rect(10, 30 + offset, 40, 20));
	volnumbox.value = 0;
	volnumbox.clipHi = pi;
	volnumbox.clipLo = 0;
	volnumbox.step_(0.1); 
	volnumbox.scroll_step=0.1;
	volnumbox.align = \center;
	volnumbox.action = {arg num; 
		vbox[fatual].valueAction = num.value;
		
		//		synt[fatual].set(\volume, num.value);
		//		volume[fatual] = num.value;
		//		("volume = " ++ num.value).postln; 
	};
	// stepsize?
	volslider = Slider.new(win, Rect(50, 30 + offset, 110, 20));
	volslider.value = 0;
	volslider.action = {arg num;
		vbox[fatual].valueAction = num.value;
		
		//		volnumbox.value = num.value;
		//		synt[fatual].set(\volume, num.value);
		//		volume[fatual] = num.value;
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
		dpbox[fatual].valueAction = num.value;
		
		//		synt[fatual].set(\volume, num.value);
		//		volume[fatual] = num.value;
		//		("volume = " ++ num.value).postln; 
	};
	// stepsize?
	dpslider = Slider.new(win, Rect(50, 50 + offset, 110, 20));
	dpslider.value = 0;
	dpslider.action = {arg num;
		dpbox[fatual].valueAction = num.value;
		dopnumbox.value = num.value;
		
		
		//		volnumbox.value = num.value;
		//		synt[fatual].set(\volume, num.value);
		//		volume[fatual] = num.value;
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
		gbox[fatual].valueAction = num.value;
		
		//		synt[fatual].set(\volume, num.value);
		//		volume[fatual] = num.value;
		//		("volume = " ++ num.value).postln; 
	};
	// stepsize?
	gslider = Slider.new(win, Rect(50, 70 + offset, 110, 20));
	gslider.value = 0;
	gslider.action = {arg num;
		gbox[fatual].valueAction = num.value;
		
		//		volnumbox.value = num.value;
		//		synt[fatual].set(\volume, num.value);
		//		volume[fatual] = num.value;
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
		lbox[fatual].valueAction = num.value;
		
	};
	// stepsize?
	lslider = Slider.new(win, Rect(50, 90 + offset, 110, 20));
	lslider.value = 0;
	lslider.action = {arg num;
		lbox[fatual].valueAction = num.value;
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
		rbox[fatual].valueAction = num.value;
		
		//		synt[fatual].set(\volume, num.value);
		//		volume[fatual] = num.value;
		//		("volume = " ++ num.value).postln; 
	};
	// stepsize?
	rslider = Slider.new(win, Rect(50, 130 + offset, 110, 20));
	rslider.value = 0.5;
	rslider.action = {arg num;
		rbox[fatual].valueAction = num.value * 6.28 - pi;
		rnumbox.value = num.value * 2pi - pi;
		
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
		dbox[fatual].valueAction = num.value;
	};
	// stepsize?
	dirslider = Slider.new(win, Rect(50, 150 + offset, 110, 20));
	dirslider.value = 0;
	dirslider.action = {arg num;
		dbox[fatual].valueAction = num.value * pi/2;
		dirnumbox.value = num.value * pi/2;
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
		cbox[fatual].valueAction = num.value;
		
		//		synt[fatual].set(\volume, num.value);
		//		volume[fatual] = num.value;
		//		("volume = " ++ num.value).postln; 
	};
	// stepsize?
	cslider = Slider.new(win, Rect(50, 170 + offset, 110, 20));
	cslider.value = 0;
	cslider.action = {arg num;
		cbox[fatual].valueAction = num.value;
		connumbox.value = num.value;
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
		
		
		//		sombuf[fatual] = Buffer.loadDialog(s, action: { 

		Dialog.openPanel({ 
			arg path;

			{tfield[fatual].valueAction = path;}.defer;
			

		}, 
			{
				"cancelado".postln;
				{tfield[fatual].value = "";}.defer;
			
			}
		);

		//		});
		
		
		
		
		
	});

		
	textbuf = StaticText(wdados, Rect(10, 20, 50, 20));
	textbuf.string = "Dp";
	textbuf = StaticText(wdados, Rect(35, 20, 50, 20));
	textbuf.string = "Lp";
	textbuf = StaticText(wdados, Rect(55, 20, 50, 20));
	textbuf.string = "Str";

	textbuf = StaticText(wdados, Rect(80, 20, 50, 20));
	textbuf.string = "NCan";
	textbuf = StaticText(wdados, Rect(120, 20, 50, 20));
	textbuf.string = "SBus";

	textbuf = StaticText(wdados, Rect(160, 20, 50, 20));
	textbuf.string = "X";
	textbuf = StaticText(wdados, Rect(200, 20, 50, 20));
	textbuf.string = "Y";

		textbuf = StaticText(wdados, Rect(240, 20, 50, 20));
	textbuf.string = "Z";

		
	textbuf = StaticText(wdados, Rect(280, 20, 50, 20));
	textbuf.string = "Ang";
	textbuf = StaticText(wdados, Rect(320, 20, 50, 20));
	textbuf.string = "Vol";
	textbuf = StaticText(wdados, Rect(360, 20, 50, 20));
	textbuf.string = "Glob";
	textbuf = StaticText(wdados, Rect(400, 20, 50, 20));
	textbuf.string = "Loc";
	textbuf = StaticText(wdados, Rect(440, 20, 50, 20));
	textbuf.string = "Rot";
	textbuf = StaticText(wdados, Rect(480, 20, 50, 20));
	textbuf.string = "Dir";
	textbuf = StaticText(wdados, Rect(520, 20, 50, 20));
	textbuf.string = "Cntr";
	textbuf = StaticText(wdados, Rect(560, 20, 50, 20));
	textbuf.string = "DAmt";
	textbuf = StaticText(wdados, Rect(600, 20, 50, 20));
	textbuf.string = "File";

		
		nfontes.do { arg i;	
		dcheck[i] = CheckBox.new( wdados, Rect(10, 40 + (i*20), 40, 20))
		.action_({ arg but;
			if(i==fatual){dopcheque.value = but.value;};
			if (but.value == true) {
				//	"Aqui!!!".postln;
				doppler[i] = 1;
				espacializador[i].set(\dopon, 1);
			}{
				doppler[i] = 0;
				espacializador[i].set(\dopon, 0);
			};
		});

		lpcheck[i] = CheckBox.new( wdados, Rect(35, 40 + (i*20), 40, 20))
		.action_({ arg but;
			if(i==fatual){loopcheck.value = but.value;};
			if (but.value == true) {
				//	"Aqui!!!".postln;
				lp[i] = 1;
				synt[i].set(\lp, 1);
			}{
				lp[i] = 0;
				synt[i].set(\lp, 0);
			};
		});

				strmcheck[i] = CheckBox.new( wdados, Rect(55, 40 + (i*20), 40, 20))
		.action_({ arg but;
			if(i==fatual){streamcheck.value = but.value;};
			if (but.value == true) {
				strm[i] = 1;
				synt[i].set(\strm, 1);
			}{
				strm[i] = 0;
				synt[i].set(\strm, 0);
			};
		});

		
		ncanbox[i] = NumberBox(wdados, Rect(80, 40 + (i*20), 40, 20));
		businibox[i] = NumberBox(wdados, Rect(120, 40 + (i*20), 40, 20));

		xbox[i] = NumberBox(wdados, Rect(160, 40 + (i*20), 40, 20));
		ybox[i] = NumberBox(wdados, Rect(200, 40+ (i*20), 40, 20));
		zbox[i] = NumberBox(wdados, Rect(240, 40+ (i*20), 40, 20));


		abox[i] = NumberBox(wdados, Rect(280, 40 + (i*20), 40, 20));
		vbox[i] = NumberBox(wdados, Rect(320, 40+ (i*20), 40, 20));
		gbox[i] = NumberBox(wdados, Rect(360, 40+ (i*20), 40, 20));
		lbox[i] = NumberBox(wdados, Rect(400, 40+ (i*20), 40, 20));
		rbox[i] = NumberBox(wdados, Rect(440, 40+ (i*20), 40, 20));

		dbox[i] = NumberBox(wdados, Rect(480, 40+ (i*20), 40, 20));
		cbox[i] = NumberBox(wdados, Rect(520, 40+ (i*20), 40, 20));
		dpbox[i] = NumberBox(wdados, Rect(560, 40+ (i*20), 40, 20));
		
		tfield[i] = TextField(wdados, Rect(600, 40+ (i*20), 350, 20));
		
		tfield[i].action = {arg path;
			if (path != "") {
					
			sombuf[i] = Buffer.read(server, path.value, action: {arg buf; 
				//{(tfield[i].value.asString ++ " tem duração de " ++ (buf.numFrames / buf.sampleRate).asString).postln;}.defer;
				//				((buf.numFrames / buf.numChannels ) / s.actualSampleRate).postln;
				((buf.numFrames ) / buf.sampleRate).postln;
				//				(buf.sampleRate).postln;
			});
				//	("Buffer " ++ i.asString ++ " tem " ++ (sombuf[i].duration).asString ++ "segundos").postln;
				//("Length = " ++ ((sombuf[i].numFrames / sombuf[i].numChannels ) / 48000).asString).postln;
			}
			
			// carregar buffer e gerenciar/lançar synths
			
		};
		
		xbox[i].action = {arg num; 
			var dist;
			//		num.value.postln;

			sprite[i, 1] = 450 + (num.value * -1);

			novoplot.value(num.value, ybox[i], i, nfontes);

			//novoplot(num.value, ybox[i], i, nfontes);
			//~testeme = espacializador;
			//	("X Value =  " ++ (espacializador[i]).asString).postln;
			
			//			espacializador[i] = nil;
			//			synt[i] = nil;

			if(espacializador[i] != nil){
					espacializador[i].set(\mx, num.value);			
				synt[i].set(\mx, num.value);
			};
			
			
		};
		ybox[i].action = {arg num; 
			//	num.value.postln;
			sprite[i, 0] = (num.value * -1 + 450);

			//			espacializador[i] = nil;
			//			synt[i] = nil;

			if(espacializador[i] != nil){
					espacializador[i].set(\my, num.value);
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
			angulo[i] = num.value;
			if((ncanais[i]==2) || (ncan[i]==2)){
				espacializador[i].set(\angulo, num.value);
				angulo[i] = num.value;
			};
			if(i == fatual) 
			{
				angnumbox.value = num.value;
				angslider.value = num.value / pi;
			};
			
		}; 
		vbox[i].action = {arg num;
			synt[i].set(\volume, num.value);
			volume[i] = num.value;
			//	("volume = " ++ num.value).postln; 
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
		ncanbox[i].action = {arg num;
			espacializador[i].set(\mz, num.value);
			synt[i].set(\mz, num.value);
			ncan[i] = num.value;
			if(i == fatual )
			{
				//var val = (450 - (num.value * 900)) * -1;
				//	ncanslider.value = num.value;
				ncannumbox.value = num.value;
			};
		}; 
		businibox[i].action = {arg num;
			espacializador[i].set(\mz, num.value);
			synt[i].set(\mz, num.value);
			busini[i] = num.value;
			if(i == fatual) 
			{
				//var val = (450 - (num.value * 900)) * -1;
				//	ncanslider.value = num.value;
				busininumbox.value = num.value;
			};
		}; 


		
	

		
	};

		




		
	controle = Automation(dur).front(win, Rect(450, 10, 400, 25));
	controle.presetDir = prjDr ++ "/auto";
	controle.onEnd = {
        controle.stop;
        "controle is stopped".postln;
        controle.seek;
		nfontes.do { arg i;	
			synt[i].free; // error check
			//	espacializador[i].free;
			
		};
    };
	
	controle.onPlay = {
		var startTime;
		if(controle.now < 0)
		{
			startTime = 0
		}
		{ 
			startTime = controle.now
		};
		nfontes.do { arg i;	
			var loaded, dur, looped;
			{loaded = tfield[i].value;}.defer;
			looped = lp[i];
			if(lp[i] != 1){
				{tocar.value(i, startTime);}.defer;
			}			
			{
				var dur = sombuf[i].numFrames / sombuf[i].sampleRate;
				{tocar.value(i, dur.rand);}.defer;
			};

		};
		
		isPlay = true;
	};

		
	
	controle.onSeek = {
		//	("onSeek = " ++ ~controle.now).postln;
		if(isPlay == true) {
			nfontes.do { arg i;	
					synt[i].free; // error check
				//	espacializador[i].free;
			};
			// JUST COMMENTED OUT
			//~controle.stop;
			//		{tocar.value(fatual, ~controle.now);}.defer;
		};
    };

	controle.onStop = {
		("Acabou! = " ++ controle.now).postln;
		nfontes.do { arg i;
			// if sound is currently being "tested", don't switch off on stop
			// leave that for user
			if (testado[i] == false) {
				synt[i].free; // error check
			};
			//	espacializador[i].free;
			isPlay = false;
		};
		revGlobal.free;
		revGlobalBF.free;
		revGlobal = nil;
		revGlobalBF = nil;

    };

		
	
	nfontes.do { arg i;
		// save the bus/streamed audio settings outside of automation on a once-off basis
		//		~controle.dock(ncanbox[i], "ncanais_" ++ i);
		//		~controle.dock(businibox[i], "businicial_" ++ i);
		controle.dock(xbox[i], "x_axis_" ++ i);
		controle.dock(ybox[i], "y_axis_" ++ i);
		controle.dock(zbox[i], "z_axis_" ++ i);
		controle.dock(vbox[i], "volume_" ++ i);
		controle.dock(dpbox[i], "dopamt_" ++ i);
		controle.dock(abox[i], "ângulo_" ++ i);
		controle.dock(gbox[i], "revglobal_" ++ i);
		controle.dock(lbox[i], "revlocal_" ++ i);
		controle.dock(rbox[i], "rotação_" ++ i);
		controle.dock(dbox[i], "diretividade_" ++ i);
		controle.dock(cbox[i], "contração_" ++ i);
		//			~controle.dock(tfield[i], "arquivo_" ++ i);
	};

		
			
		win.view.mouseMoveAction = {|view, x, y, modifiers | [x, y];
		//	x = DelayL.kr(x, 1.0, 1.0, 1, 0);
		//	y = DelayL.kr(y, 1.0, 1.0, 1, 0);
		//		xbox[fatual].valueAction = x;
		//		ybox[fatual].valueAction = y;
		//	fonte.set((mx - 450) * -1, 450 - my);

		xbox[fatual].valueAction = 450 - y;
		ybox[fatual].valueAction = (x - 450) * -1;
		win.drawFunc = {
			nfontes.do { arg i;	
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
	
		
	nfontes.do { arg x;
		mbus[x] = Bus.audio(server, 1); // passar som da fonte ao espacializador
		sbus[x] = Bus.audio(server, 2); // passar som da fonte ao espacializador
		//	bfbus[x] = Bus.audio(s, 4); // passar som da fonte ao espacializador
		if (dopflag == 0, {
			
		};, {
			//	espacializador[x] = Synth.new(\amb2d, [\inbus, mbus[x], \gbus, gbus]);
		});
		//	synt[x] = Synth.new(\arquivoLoop, [\outbus, bus[x]]);
	};
		
	
	win.onClose_({ 
		controle.quit;
		nfontes.do { arg x;
			espacializador[x].free;
			mbus[x].free;
			sbus[x].free;
			//	bfbus.[x].free;
			sombuf[x].free;
			synt[x].free;
			MIDIIn.removeFuncFrom(\sysex, sysex);
			//		kespac[x].stop;
		};
		revGlobal.free;
		revGlobalBF.free;
		
		wdados.close;
		gbus.free;
		gbfbus.free;
		// The following should be removed, but...
		// and if all that doesn't do it!:
		server.freeAll;
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
