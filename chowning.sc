Chowning {
	//	classvar fftsize = 2048;
	var <>myTestVar;
	var  <>kernelSize, <>scale, <>rirW, <>rirX, <>rirY, <>rirZ, <>rirL, <>rirR,
	<>rirWspectrum, <>rirXspectrum, <>rirYspectrum, <>rirZspectrum,
	<>rirLspectrum, <>rirRspectrum,
	<>irbuffer, <>bufsize, <>bufsizeBinaural;
	//	var <>fftsize = 2048;
	classvar server, rirW, rirX, rirY, rirZ, rirL, rirR,
	bufsize, bufsizeBinaural, irbuffer,
	rirWspectrum, rirXspectrum, rirYspectrum, rirZspectrum,
	rirLspectrum, rirRspectrum,
	binDecoder;
	classvar fftsize = 2048;

	/*
		(
		s.waitForBoot {
		
		~testChowning = Chowning.new("/home/iain/projects/ambisonics/chowning/ir/QL14_tail48kHz.amb", 
		"/home/iain/projects/ambisonics/chowning/ir/sbs_binaural_tail.wav", 21, Server.default);
		
		};
		)
	*/

	*new { arg rirWXYZ, rirBinaural, numCIPIC, server;
		server = server ? Server.default;
		rirW = Buffer.readChannel(server, rirWXYZ, channels: [0]);
		rirX = Buffer.readChannel(server, rirWXYZ, channels: [1]);
		rirY = Buffer.readChannel(server, rirWXYZ, channels: [2]);
		rirZ = Buffer.readChannel(server, rirWXYZ, channels: [3]);
		rirL = Buffer.readChannel(server, rirBinaural, channels: [0]);
		rirR = Buffer.readChannel(server, rirBinaural, channels: [1]);

		
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
		
		binDecoder = FoaDecoderKernel.newCIPIC(numCIPIC); // KEMAR head, use IDs 21 or 165
		
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
		

		
		
	}

}
