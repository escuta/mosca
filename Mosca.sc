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

/*
AutomationGuiProxy {
    var <>val, <>function;
    *new { arg val;
        ^super.new.initAutomationProxy(val);
    }
    initAutomationProxy { arg ival;
        this.val = ival;
			this.bounds(Rect(0,0,0,0)); // set fake bounds to keep Automation happy!
    }
    value {
        ^this.val;
    }
    value_ { arg value;
        this.val = value;
    }
	mapToGlobal { arg point;
		_QWidget_MapToGlobal
		^this.primitiveFailed;
	}

	absoluteBounds {
		^this.bounds.moveToPoint( this.mapToGlobal( 0@0 ) );
	}
	bounds {
		^this.getProperty(\geometry)
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
*/


AutomationGuiProxy : QView {
	var <>val, <>function, <>action;
	*new { arg val;
		^super.new.initAutomationProxy(val);
	}
	initAutomationProxy { arg ival;
		this.val = ival;
		this.bounds(Rect(0,0,0,0)); // set fake bounds to keep Automation happy!
	}
	value {
		^this.val;
	}
	value_ { arg value;
		this.val = value;
	}
	mapToGlobal { arg point;
		_QWidget_MapToGlobal
		^this.primitiveFailed;
	}

	absoluteBounds {
		^this.bounds.moveToPoint( this.mapToGlobal( 0@0 ) );
	}
	bounds {
		^this.getProperty(\geometry)
	}

	bounds_ { arg rect;
		this.setProperty(\geometry, rect.asRect )
	}

	doAction { this.action.value(this.val)
	}
	valueAction_ { |val| this.value_(val).doAction
	}
}




Mosca {
	var <>win, <>wdados, <>waux, <>sprite, <>nfontes,
	<>revGlobal, <>revGlobalSoa, <>revGlobalBF, <>nonAmbi2FuMa, <>convertor,
	<>m, <>libnumbox, <>textbuf, <>control,
	<>globDec,
	<>sysex, <>mmcslave,
	<>synthRegistry, <>busini, <>ncan,
	<>aux1, <>aux2, <>aux3, <>aux4, <>aux5,  // aux slider values
	<>triggerFunc, <>stopFunc,
	<>scInBus,
	<>globTBus,
	<>width, <>halfwidth, <>height, <>halfheight,
	<>insertFlag,
	<>aFormatBusFoa, <>aFormatBusSoa,
	<>dur,
	<>plim, // distance limit from origin where processes continue to run
	<>looping,
	<>firstTime,
	<>playingBF,
	<>maxorder,
	<>serport,
	<>offsetheading,
	<>libbox, <>lpcheck, <>dstrvbox, <>hwncheck, <>scncheck,
	//comment out all linear parameters
	//<>lncheck,
	<>spcheck, <>dfcheck,
	<>ncanbox, <>businibox,
	<>espacializador, <>synt,
	<>lastAutomation = nil,
	<>tfield,
	<>autoloopval,
	<>autoloop,
	<>streamdisk,
	<>streambuf, // <>streamrate, // apparently unused
	// head tracking
	<>trackarr, <>trackarr2, <>tracki, <>trackPort,
	//<>track2arr, <>track2arr2, <>track2i,
	<>headingnumbox, <>rollnumbox, <>pitchnumbox,
	<>headingnumboxProxy, <>rollnumboxProxy, <>pitchnumboxProxy,
	<>headingOffset,
	<>troutine, <>kroutine, <>watcher,
	<>cartval, <>spheval,
	<>recchans, <>recbus,
	// <>mark1, <>mark2,	// 4 number arrays for marker data // apparently unused

	<>lastGui, <>guiInt,

	// MOVED FROM the the gui method/////////////////////////

	<>currentsource,
	<>angnumbox, <>cbox, <>clev, <>angle, <>ncanais, <>testado,	<>gbus, <>gbfbus, <>ambixbus, <>nonambibus,
	<>playEspacGrp, <>glbRevDecGrp,
	<>level, <>lp, <>lib, <>libName, <>convert, <>dstrv, <>dstrvtypes, <>clsrv, <>clsRvtypes,
	//comment out all linear parameters
	//<>ln,
	<>angslider, <>connumbox, <>cslider,
	<>xbox, <>ybox, <>sombuf, <>sbus, <>updatesourcevariables, <>soaBus, <>mbus,
	<>rbox, <>abox, <>vbox, <>gbox, <>lbox, <>dbox, <>dpbox, <>zbox,
	<>a1check, <>a2check, <>a3check, <>a4check, <>a5check, <>a1box, <>a2box, <>a3box, <>a4box, <>a5box,
	<>stcheck,

	//<>oxbox, <>oybox, <>ozbox,
	// <>funcs, // apparently unused


	<>novoplot, // <>lastx, <>lasty, // apparently unused
	<>zlev, <>znumbox, <>zslider,
	<>volslider, <>volnumbox, <>glev, <>gslider, <>gnumbox, <>lslider,
	<>lnumbox, <>llev, <>rnumbox, <>rslider, <>rlev, <>dlev,
	<>dirnumbox, <>dirslider, <>dplev, <>dpslider, <>dopnumbox,
	<>auxslider1, <>auxslider2, <>auxslider3, <>auxslider4, <>auxslider5,
	<>auxbutton1, <>auxbutton2, <>auxbutton3, <>auxbutton4, <>auxbutton5,
	<>aux1numbox, <>aux2numbox, <>aux3numbox, <>aux4numbox, <>aux5numbox,
	<>a1but, <>a2but, <>a3but, <>a4but, <>a5but,

	<>loopcheck, <>dstReverbox, <>clsReverbox, <>hwInCheck,
	<>hwn, <>scInCheck, <>scn,
	//comment out all linear parameters
	//<>lincheck,
	<>spreadcheck,
	<>diffusecheck, <>sp, <>df, <>ncannumbox, <>busininumbox,

	<>atualizarvariaveis, <>updateSynthInArgs,

	<>runTriggers, <>runStops, <>runTrigger, <>runStop,
	<>isPlay = false, // <>isRec, // apparently unused

	/////////////////////////////////////////////////////////

	// NEW PROXY VARIABLES /////////////

	<>rboxProxy, <>cboxProxy, <>aboxProxy, <>vboxProxy, <>gboxProxy, <>lboxProxy,
	<>dboxProxy,
	<>dpboxProxy, <>zboxProxy, <>yboxProxy, <>xboxProxy,
	<>a1checkProxy, <>a2checkProxy, <>a3checkProxy, <>a4checkProxy, <>a5checkProxy, <>a1boxProxy,
	<>a2boxProxy, <>a3boxProxy, <>a4boxProxy, <>a5boxProxy,

	<>stcheckProxy, <>tfieldProxy, <>libboxProxy, <>lpcheckProxy, <>dstrvboxProxy, <>clsrvboxProxy,
	<>hwncheckProxy,
	<>scncheckProxy, <>dfcheckProxy,
	//comment out all linear parameters
	//<>lncheckProxy,
	<>spcheckProxy, <>ncanboxProxy, <>businiboxProxy,

	<>guiflag, <>btestar,

	<>rm, <>rmslider, <>rmnumbox, <>rmbox, <>rmboxProxy, // setable local room size
	<>dm, <>dmslider, <>dmnumbox, <>dmbox, <>dmboxProxy, // setable local dampening

	<>clsrm, <>clsrmslider, <>clsrmnumbox, <>clsrmbox, <>clsrmboxProxy, // setable global room size
	<>clsdm, <>clsdmslider, <>clsdmnumbox, <>clsdmbox, <>clsdmboxProxy, // setable global dampening

	<>ossiasrc, <>ossiacart, <>ossiasphe, <>ossiaaud, <>ossialoop, <>ossialib, <>ossialev, <>ossiadp,
	<>ossiacls, <>ossiaclsam, <>ossiaclsdel, <>ossiaclsdec, <>ossiadst, <>ossiadstam,
	<>ossiadstdel, <>ossiadstdec, <>ossiaangle, <>ossiarot, <>ossiadir, <>ossiactr,
	<>ossiasprea, <>ossiadiff;



	/////////////////////////////////////////






	classvar server,
	rirWspectrum, rirXspectrum, rirYspectrum, rirZspectrum, rirA12Spectrum,
	rirFLUspectrum, rirFRDspectrum, rirBLDspectrum, rirBRUspectrum,
	rirList,
	b2a, a2b,
	blips,
	convert_fuma,
	convert_ambix,
	convert_direct,
	speaker_array,
	numoutputs,
	radius_max,
    vbap_buffer,
	soa_a12_decoder_matrix, soa_a12_encoder_matrix,
	cart, spher, foa_a12_decoder_matrix,

	//o, //debugging
	prjDr;
	classvar fftsize = 2048,
	offsetLag = 2.0,  // lag in seconds for incoming GPS data
	server;
	classvar foaEncoderOmni, foaEncoderSpread, foaEncoderDiffuse;
	*new { arg projDir, nsources = 10, width = 800, dur = 180, rirBank,
		server = Server.local, decoder, speaker_array, outbus = 0, suboutbus,
		rawbusfuma = 0, rawbusambix = 9, maxorder = 1,
		serport, offsetheading = 0, recchans = 2, recbus = 0, guiflag = true,
		guiint = 0.07, autoloop = false;
		^super.new.initMosca(projDir, nsources, width, dur, rirBank,
			server, decoder, speaker_array, outbus, suboutbus, rawbusfuma, rawbusambix, maxorder, serport,
			offsetheading, recchans, recbus, guiflag, guiint, autoloop);
	}



	*printSynthParams {
		var string =
		"

GUI Parameters usable in SynthDefs

\\level | level | 0 - 1 |
\\dopamnt | Doppler ammount | 0 - 1 |
\\angle | Stereo angle | default 1.05 (60 degrees) | 0 - 3.14 |
\\glev | Global/Close reverb level | 0 - 1 |
\\llev | Local/Distant reverb level | 0 - 1 |
\\azim | azimuth coord | -3.14 - 3.14 |
\\elev | elevation coord | -3.14 - 3.14 |
\\radius | spherical radius | 0 - 1 |
\\rotAngle | B-format rotation angle | -3.14 - 3.14 |
\\directang | B-format directivity | 0 - 1.57 |
\\contr | Contraction: fade between WXYZ & W | 0 - 1 |
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

	initMosca { arg projDir, nsources, iwidth, idur, rirBank, iserver, decoder, ispeaker_array,
		outbus, suboutbus, rawbusfuma, rawbusambix, imaxorder, iserport, ioffsetheading,
		irecchans, irecbus, iguiflag, iguiint, iautoloop;
		var makeSynthDefPlayers, makeSpatialisers, revGlobTxt, subOutFunc,
		espacAmbOutFunc, ambixOutFunc, espacAmbEstereoOutFunc, revGlobalAmbFunc,
		playBFormatOutFunc, playMonoInFunc, playStereoInFunc, playBFormatInFunc,
		revGlobalSoaOutFunc,
		prepareAmbSigFunc,
		localReverbFunc, localReverbStereoFunc,
		reverbOutFunc, nonAmbiFunc,
		bFormNumChan = (imaxorder + 1).squared; // add the number of channels of the b format
		                                        // depending on maxorder
		server = iserver;
		b2a = FoaDecoderMatrix.newBtoA;
		a2b = FoaEncoderMatrix.newAtoB;
		foaEncoderOmni = FoaEncoderMatrix.newOmni;
		//server.sync;
		foaEncoderSpread = FoaEncoderKernel.newSpread (subjectID: 6, kernelSize: 2048);
		//server.sync;
		foaEncoderDiffuse = FoaEncoderKernel.newDiffuse (subjectID: 3, kernelSize: 2048);
		//server.sync;
		this.globTBus = Bus.audio(server, bFormNumChan.clip(4, 9));
		this.ambixbus = Bus.audio(server, bFormNumChan); // global b-format ACN-SN3D bus
		server.sync;
		this.playEspacGrp = Group.tail;
		this.glbRevDecGrp = Group.after(this.playEspacGrp);

		speaker_array = ispeaker_array;


		//server.sync;
		//this.lock = ilock;


		if (iwidth < 600) {
			this.width = 600;
		} {
			this.width = iwidth;
		};
		this.halfwidth = this.width / 2;
		this.height = this.width; // on init
		this.halfheight = this.halfwidth;
		this.dur = idur;
		this.maxorder = imaxorder;
		this.serport = iserport;
		this.offsetheading = ioffsetheading;
		this.recchans = irecchans;
		this.recbus = irecbus;
		this.guiflag = iguiflag;

		this.currentsource = 0;
		this.plim = 1.2;
		this.lastGui = Main.elapsedTime;
		this.guiInt = iguiint;
		this.autoloopval = iautoloop;

		this.looping = false;

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




		// apparently unused
		// this.mark1 = Array.newClear(4);
		// this.mark2 = Array.newClear(4);


		this.nfontes = nsources;

		///////////////////// DECLARATIONS FROM gui /////////////////////


		this.espacializador = Array.newClear(this.nfontes);
		libName = Array.newClear(this.nfontes);
		lib = Array.newClear(this.nfontes);
		dstrv = Array.newClear(this.nfontes);
		convert = Array.newClear(this.nfontes);
		lp = Array.newClear(this.nfontes);
		sp = Array.newClear(this.nfontes);
		df = Array.newClear(this.nfontes);
		dstrvtypes = Array.newClear(this.nfontes);
		//comment out all linear parameters
		//ln = Array.newClear(this.nfontes);
		hwn = Array.newClear(this.nfontes);
		scn = Array.newClear(this.nfontes);
		mbus = Array.newClear(this.nfontes);
		sbus = Array.newClear(this.nfontes);
		ncanais = Array.newClear(this.nfontes);  // 0 = não, nem estéreo. 1 = mono. 2 = estéreo.
		this.ncan = Array.newClear(this.nfontes);  // 0 = não, nem estéreo. 1 = mono. 2 = estéreo.
		// note that ncan refers to # of channels in streamed sources.
		// ncanais is related to sources read from file
		this.busini = Array.newClear(this.nfontes); // initial bus # in streamed audio grouping
		                                            // (ie. mono, stereo or b-format)
		this.aux1 = Array.newClear(this.nfontes);
		this.aux2 = Array.newClear(this.nfontes);
		this.aux3 = Array.newClear(this.nfontes);
		this.aux4 = Array.newClear(this.nfontes);
		this.aux5 = Array.newClear(this.nfontes);

		this.a1but = Array.newClear(this.nfontes);
		this.a2but = Array.newClear(this.nfontes);
		this.a3but = Array.newClear(this.nfontes);
		this.a4but = Array.newClear(this.nfontes);
		this.a5but = Array.newClear(this.nfontes);

		sombuf = Array.newClear(this.nfontes);
		//		xoffset = Array.fill(this.nfontes, 0);
		//		yoffset = Array.fill(this.nfontes, 0);
		this.synt = Array.newClear(this.nfontes);
		sprite = Array2D.new(this.nfontes, 2);
		// funcs = Array.newClear(this.nfontes); // apparently unused
		angle = Array.newClear(this.nfontes); // ângulo dos canais estereofônicos
		zlev = Array.newClear(this.nfontes);
		level = Array.newClear(this.nfontes);
		//	doplev = Array.newClear(this.nfontes);
		glev = Array.newClear(this.nfontes);
		llev = Array.newClear(this.nfontes);
		rm = Array.newClear(this.nfontes);
		dm = Array.newClear(this.nfontes);
		rlev = Array.newClear(this.nfontes);
		dlev = Array.newClear(this.nfontes);
		dplev = Array.newClear(this.nfontes);
		clev = Array.newClear(this.nfontes);

		this.ncanbox = Array.newClear(this.nfontes);
		this.businibox = Array.newClear(this.nfontes);
		this.playingBF = Array.newClear(this.nfontes);


		//oxbox = Array.newClear(this.nfontes);
		//oybox = Array.newClear(this.nfontes);
		//ozbox = Array.newClear(this.nfontes);
		xbox = Array.newClear(this.nfontes);
		zbox = Array.newClear(this.nfontes);
		ybox = Array.newClear(this.nfontes);
		//lastx = Array.newClear(this.nfontes); // apparently unused
		// lasty = Array.newClear(this.nfontes); // apparently unused
		abox = Array.newClear(this.nfontes); // ângulo
		vbox = Array.newClear(this.nfontes);  // level
		gbox = Array.newClear(this.nfontes); // reverberação global
		lbox = Array.newClear(this.nfontes); // reverberação local
		rmbox = Array.newClear(this.nfontes); // local room size
		dmbox = Array.newClear(this.nfontes); // local dampening
		rbox = Array.newClear(this.nfontes); // rotação de b-format
		dbox = Array.newClear(this.nfontes); // diretividade de b-format
		cbox = Array.newClear(this.nfontes); // contrair b-format
		dpbox = Array.newClear(this.nfontes); // dop amount
		this.libbox = Array.newClear(this.nfontes); // libs
		this.lpcheck = Array.newClear(this.nfontes); // loop
		this.spcheck = Array.newClear(this.nfontes); // spread
		this.dfcheck = Array.newClear(this.nfontes); // diffuse
		this.dstrvbox = Array.newClear(this.nfontes); // distant reverb list
		//comment out all linear parameters
		//this.lncheck = Array.newClear(this.nfontes); // linear intensity
		this.hwncheck = Array.newClear(this.nfontes); // hardware-in check
		this.scncheck = Array.newClear(this.nfontes); // SuperCollider-in check
		a1box = Array.newClear(this.nfontes); // aux - array of num boxes in data window
		a2box = Array.newClear(this.nfontes); // aux - array of num boxes in data window
		a3box = Array.newClear(this.nfontes); // aux - array of num boxes in data window
		a4box = Array.newClear(this.nfontes); // aux - array of num boxes in data window
		a5box = Array.newClear(this.nfontes); // aux - array of num boxes in data window

		this.a1but = Array.newClear(this.nfontes); // aux - array of buttons in data window
		this.a2but = Array.newClear(this.nfontes); // aux - array of buttons in data window
		this.a3but = Array.newClear(this.nfontes); // aux - array of buttons in data window
		this.a4but = Array.newClear(this.nfontes); // aux - array of buttons in data window
		this.a5but = Array.newClear(this.nfontes); // aux - array of buttons in data window

		a1check = Array.newClear(this.nfontes); // aux - array of buttons in data window
		a2check = Array.newClear(this.nfontes); // aux - array of buttons in data window
		a3check = Array.newClear(this.nfontes); // aux - array of buttons in data window
		a4check = Array.newClear(this.nfontes); // aux - array of buttons in data window
		a5check = Array.newClear(this.nfontes); // aux - array of buttons in data window

		stcheck = Array.newClear(this.nfontes); // aux - array of buttons in data window

		this.firstTime = Array.newClear(this.nfontes);


		this.tfield = Array.newClear(this.nfontes);
		this.streamdisk = Array.newClear(this.nfontes);

				// busses to send audio from player to spatialiser synths
		this.nfontes.do { arg x;
			mbus[x] = Bus.audio(server, 1);
			sbus[x] = Bus.audio(server, 2);
			//	bfbus[x] = Bus.audio(s, 4);
		};


		testado = Array.newClear(this.nfontes);

		clsRvtypes = ""; // initialise close reverb type
		clsrv = 0;
		clsrm = 0.5; // initialise close reverb room size
		clsdm = 0.5; // initialise close reverb dampening


		this.nfontes.do { arg i;
			libName[i] = "ATK"; // initialize original ATK encoding
			lib[i] = 3;
			dstrv[i] = 0;
			convert[i] = false;
			angle[i] = 1.05;
			level[i] = 0;
			glev[i] = 0;
			llev[i] = 0;
			rm[i] = 0.5;
			dm[i] = 0.5;
			lp[i] = 0;
			sp[i] = 0;
			df[i] = 0;
			dstrvtypes[i] = ""; // initialise distants reverbs types
			//comment out all linear parameters
			//ln[i] = "";
			hwn[i] = 0;
			scn[i] = 0;
			rlev[i] = 0;
			dlev[i] = 0;
			clev[i] = 1;
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

		////////////////////////////////////////////////

				////////// ADDED NEW ARRAYS and other proxy stuff  //////////////////

		// these proxies behave like GUI elements. They eneable
		// the use of Automation without a GUI

		cartval = Array.fill(this.nfontes, {Cartesian(-20, -20, 0)} );
		spheval = Array.fill(this.nfontes, {|i| cartval[i].asSpherical} );

		rboxProxy = Array.newClear(this.nfontes);
		cboxProxy = Array.newClear(this.nfontes);
		aboxProxy = Array.newClear(this.nfontes);
		vboxProxy = Array.newClear(this.nfontes);
		gboxProxy = Array.newClear(this.nfontes);
		lboxProxy = Array.newClear(this.nfontes);
		rmboxProxy = Array.newClear(this.nfontes);
		dmboxProxy = Array.newClear(this.nfontes);
		dboxProxy = Array.newClear(this.nfontes);
		dpboxProxy = Array.newClear(this.nfontes);
		zboxProxy = Array.newClear(this.nfontes);
		yboxProxy = Array.newClear(this.nfontes);
		xboxProxy = Array.newClear(this.nfontes);
		a1checkProxy = Array.newClear(this.nfontes);
		a2checkProxy = Array.newClear(this.nfontes);
		a3checkProxy = Array.newClear(this.nfontes);
		a4checkProxy = Array.newClear(this.nfontes);
		a5checkProxy = Array.newClear(this.nfontes);
		a1boxProxy = Array.newClear(this.nfontes);
		a2boxProxy = Array.newClear(this.nfontes);
		a3boxProxy = Array.newClear(this.nfontes);
		a4boxProxy = Array.newClear(this.nfontes);
		a5boxProxy = Array.newClear(this.nfontes);

		tfieldProxy = Array.newClear(this.nfontes);
		libboxProxy = Array.newClear(this.nfontes);
		lpcheckProxy = Array.newClear(this.nfontes);
		dstrvboxProxy = Array.newClear(this.nfontes);
		hwncheckProxy = Array.newClear(this.nfontes);
		scncheckProxy = Array.newClear(this.nfontes);
		dfcheckProxy = Array.newClear(this.nfontes);
		//comment out all linear parameters
		//lncheckProxy = Array.newClear(this.nfontes);
		spcheckProxy = Array.newClear(this.nfontes);
		ncanboxProxy = Array.newClear(this.nfontes);
		businiboxProxy = Array.newClear(this.nfontes);
		stcheckProxy = Array.newClear(this.nfontes);


		this.nfontes.do { arg i;
			rboxProxy[i] = AutomationGuiProxy.new(0.0);
			cboxProxy[i] = AutomationGuiProxy.new(0.0);
			aboxProxy[i] = AutomationGuiProxy.new(1.0471975511966);
			vboxProxy[i] = AutomationGuiProxy.new(0.0);
			gboxProxy[i] = AutomationGuiProxy.new(0.0);
			lboxProxy[i] = AutomationGuiProxy.new(0.0);
			rmboxProxy[i]= AutomationGuiProxy.new(0.5);
			dmboxProxy[i]= AutomationGuiProxy.new(0.5);
			dboxProxy[i] = AutomationGuiProxy.new(0.0);
			dpboxProxy[i] = AutomationGuiProxy.new(0.0);
			zboxProxy[i] = AutomationGuiProxy.new(0.0);
			yboxProxy[i] = AutomationGuiProxy.new(20.0);
			xboxProxy[i] = AutomationGuiProxy.new(0.0);
			a1checkProxy[i] = AutomationGuiProxy.new(false);
			a2checkProxy[i] = AutomationGuiProxy.new(false);
			a3checkProxy[i] = AutomationGuiProxy.new(false);
			a4checkProxy[i] = AutomationGuiProxy.new(false);
			a5checkProxy[i] = AutomationGuiProxy.new(false);
			a1boxProxy[i] = AutomationGuiProxy.new(0.0);
			a2boxProxy[i] = AutomationGuiProxy.new(0.0);
			a3boxProxy[i] = AutomationGuiProxy.new(0.0);
			a4boxProxy[i] = AutomationGuiProxy.new(0.0);
			a5boxProxy[i] = AutomationGuiProxy.new(0.0);

			hwncheckProxy[i] = AutomationGuiProxy.new(false);

			tfieldProxy[i] = AutomationGuiProxy.new("");
			libboxProxy[i] = AutomationGuiProxy.new(3);
			lpcheckProxy[i] = AutomationGuiProxy.new(false);
			dstrvboxProxy[i] = AutomationGuiProxy.new(0);
			scncheckProxy[i] = AutomationGuiProxy.new(false);
			dfcheckProxy[i] = AutomationGuiProxy.new(false);
			//comment out all linear parameters
			//lncheckProxy[i] = AutomationGuiProxy.new(false);
			spcheckProxy[i] = AutomationGuiProxy.new(false);
			ncanboxProxy[i] = AutomationGuiProxy.new(0);
			businiboxProxy[i] = AutomationGuiProxy.new(0);
			stcheckProxy[i] = AutomationGuiProxy.new(false);


		};

		//set up automationProxy for single parameters outside of the previous loop, not to be docked
		clsrvboxProxy = AutomationGuiProxy.new(0);
		clsrmboxProxy = AutomationGuiProxy.new(0.5); // cls roomsize proxy
		clsdmboxProxy = AutomationGuiProxy.new(0.5); // cls dampening proxy


		headingnumboxProxy = AutomationGuiProxy.new(0.0);
		rollnumboxProxy = AutomationGuiProxy.new(0.0);
		pitchnumboxProxy = AutomationGuiProxy.new(0.0);

		this.control = Automation(this.dur, showLoadSave: false, showSnapshot: true,
			minTimeStep: 0.001);


		////////////// DOCK PROXIES /////////////


		// this should be done after the actions are assigned


		this.nfontes.do { arg i;

			this.libboxProxy[i].action_({ arg num;
				case
				{ num.value == 0 }
				{ libName[i] = "ambitools" }
				{ num.value == 1 }
				{ libName[i] = "hoaLib" }
				{ num.value == 2 }
				{ libName[i] = "ambiPanner" }
				{ num.value == 3 }
				{ libName[i] = "ATK" }
				{ num.value == 4 }
				{ libName[i] = "VBAP" };

				if (guiflag) {
					{this.libbox[i].value = num.value}.defer;
					if(i == currentsource) {
						{libnumbox.value = num.value}.defer;
					};
				};
				if (this.ossialib.notNil) {
					if (this.ossialib[i].v != num.value) {
						this.ossialib[i].v_(num.value);
					};
				};
			});


			this.dstrvboxProxy[i].action_({ arg num;
				case
				{ num.value == 0 }{ dstrvtypes[i] = ""; }
				{ num.value == 1 }{ dstrvtypes[i] = "_free"; }
				{ num.value == 2 }{ dstrvtypes[i] = "_pass"; }
				{ num.value >= 4 }{ dstrvtypes[i] = "_conv"; };

				if (guiflag) {
					{this.dstrvbox[i].value = num.value}.defer;

					if (i == currentsource)
					{
						{ dstReverbox.value = num.value }.defer;
						if (num.value == 3) {
							this.setSynths(i, \rv, 1);
						}{
							this.setSynths(i, \rv, 0);
						};
					};
				};
				if (this.ossiadst.notNil) {
					if (this.ossiadst[i].v != num.value) {
						this.ossiadst[i].v_(num.value);
					};
				};
			});


			this.xboxProxy[i].action = {arg num;
				this.cartval[i].x_(num.value);
				this.spheval[i] = this.cartval[i].asSpherical;
				if ( guiflag) {
					var period = Main.elapsedTime - this.lastGui;
					//{sprite[i, 0] = this.halfwidth + (num.value * this.halfheight)}.defer;
					if (period > this.guiInt) {
						this.lastGui =  Main.elapsedTime;
						{novoplot.value}.defer;
						{this.xbox[i].value = num.value}.defer;
					};
				};
				if(this.espacializador[i].notNil || this.playingBF[i]) {
					this.espacializador[i].set(\azim, this.spheval[i].theta);
					this.setSynths(i, \azim, this.spheval[i].theta);
					this.synt[i].set(\azim, this.spheval[i].theta);
					this.espacializador[i].set(\elev, this.spheval[i].phi);
					this.setSynths(i, \elev, this.spheval[i].phi);
					this.synt[i].set(\elev, this.spheval[i].phi);
					this.espacializador[i].set(\radius, this.spheval[i].rho);
					this.setSynths(i, \radius, this.spheval[i].rho);
					this.synt[i].set(\radius, this.spheval[i].rho);
				};
				if (this.ossiacart.notNil) {
					if (this.ossiacart[i].v[0] != num.value) {
						this.ossiacart[i].v_([num.value, yboxProxy[i].value, zboxProxy[i].value]);
					};
				};
			};

			this.yboxProxy[i].action = {arg num;
				this.cartval[i].y_(num.value);
				this.spheval[i] = this.cartval[i].asSpherical;
				if (guiflag) {
					var period = Main.elapsedTime - this.lastGui;
					//{sprite[i, 1] = this.halfheight - (num.value * this.halfheight)}.defer;
					if (period > this.guiInt) {
						this.lastGui =  Main.elapsedTime;
						{novoplot.value}.defer;
						{this.ybox[i].value = num.value}.defer;
					};
				};
				if(this.espacializador[i].notNil || this.playingBF[i]){
					this.espacializador[i].set(\azim, this.spheval[i].theta);
					this.setSynths(i, \azim, this.spheval[i].theta);
					this.synt[i].set(\azim, this.spheval[i].theta);
					this.espacializador[i].set(\elev, this.spheval[i].phi);
					this.setSynths(i, \elev, this.spheval[i].phi);
					this.synt[i].set(\elev, this.spheval[i].phi);
					this.espacializador[i].set(\radius, this.spheval[i].rho);
					this.setSynths(i, \radius, this.spheval[i].rho);
					this.synt[i].set(\radius, this.spheval[i].rho);
				};
				if (this.ossiacart.notNil) {
					if (this.ossiacart[i].v[1] != num.value) {
						this.ossiacart[i].v_([xboxProxy[i].value, num.value, zboxProxy[i].value]);
					};
				};
			};

			this.zboxProxy[i].action = {arg num;
				this.cartval[i].z_(num.value);
				this.spheval[i] = this.cartval[i].asSpherical;
				zlev[i] = this.cartval[i].z;
				if (guiflag) {
					{zbox[i].value = num.value}.defer;
					if(i == currentsource) {
						{zslider.value = (num.value + 1) / 2}.defer;
						{znumbox.value = num.value}.defer;
					};
				};
				if(this.espacializador[i].notNil || this.playingBF[i]){
					this.espacializador[i].set(\azim, this.spheval[i].theta);
					this.setSynths(i, \azim, this.spheval[i].theta);
					this.synt[i].set(\azim, this.spheval[i].theta);
					this.espacializador[i].set(\elev, this.spheval[i].phi);
					this.setSynths(i, \elev, this.spheval[i].phi);
					this.synt[i].set(\elev, this.spheval[i].phi);
					this.espacializador[i].set(\radius, this.spheval[i].rho);
					this.setSynths(i, \radius, this.spheval[i].rho);
					this.synt[i].set(\radius, this.spheval[i].rho);
				};
				if (this.ossiacart.notNil) {
					if (this.ossiacart[i].v[2] != num.value) {
						this.ossiacart[i].v_([xboxProxy[i].value, yboxProxy[i].value, num.value]);
					};
				};
			};

			this.aboxProxy[i].action = {arg num;
				//("ncanais = " ++ this.ncanais[i]).postln;
				if(this.espacializador[i].notNil) {
					angle[i] = num.value;
					this.espacializador[i].set(\angle, num.value);
					this.setSynths(i, \angle, num.value);
					angle[i] = num.value;
				};
				if (guiflag) {
					{abox[i].value = num.value}.defer;
					if(i == currentsource)
					{
						{angnumbox.value = num.value}.defer;
						{angslider.value = num.value / pi}.defer;
					};
				};
				if (this.ossiaangle.notNil) {
					if (this.ossiaangle[i].v != num.value) {
						this.ossiaangle[i].v_(num.value);
					};
				};
			};

			vboxProxy[i].action = {arg num;
				this.synt[i].set(\level, num.value);
				this.setSynths(i, \level, num.value);
				level[i] = num.value;

				if (guiflag)
				{
					{vbox[i].value = num.value}.defer;

					if(i == currentsource)
					{
						{volslider.value = num.value}.defer;
						{volnumbox.value = num.value}.defer;
					};
				};
				if (this.ossialev.notNil) {
					if (this.ossialev[i].v != num.value) {
						this.ossialev[i].v_(num.value);
					};
				};
			};



			gboxProxy[i].action = {arg num;
				this.espacializador[i].set(\glev, num.value);
				this.setSynths(i, \glev, num.value);
				this.synt[i].set(\glev, num.value);
				glev[i] = num.value;
				if (guiflag) {
					{gbox[i].value = num.value}.defer;
					if (i == currentsource)
					{
						{gslider.value = num.value}.defer;
						{gnumbox.value = num.value}.defer;
					};
				};
				if (this.ossiaclsam.notNil) {
					if (this.ossiaclsam[i].v != num.value) {
						this.ossiaclsam[i].v_(num.value);
					};
				};
			};

			lboxProxy[i].action = {arg num;
				this.espacializador[i].set(\llev, num.value);
				this.setSynths(i, \llev, num.value);
				this.synt[i].set(\llev, num.value);
				llev[i] = num.value;
				if (guiflag) {
					lbox[i].value = num.value;
					if (i == currentsource)
					{
						{lslider.value = num.value}.defer;
						{lnumbox.value = num.value}.defer;
					};
				};
				if (this.ossiadstam.notNil) {
					if (this.ossiadstam[i].v != num.value) {
						this.ossiadstam[i].v_(num.value);
					};
				};
			};

			rmboxProxy[i].action = {arg num;
				this.espacializador[i].set(\room, num.value);
				this.setSynths(i, \room, num.value);
				this.synt[i].set(\room, num.value);
				rm[i] = num.value;
				if (guiflag) {
					rmbox[i].value = num.value;
					if (i == currentsource) {
						{rmslider.value = num.value}.defer;
						{rmnumbox.value = num.value}.defer;
					};
				};
				if (this.ossiadstdel.notNil) {
					if (this.ossiadstdel[i].v != num.value) {
						this.ossiadstdel[i].v_(num.value);
					};
				};
			};

			dmboxProxy[i].action = {arg num;
				this.espacializador[i].set(\damp, num.value);
				this.setSynths(i, \damp, num.value);
				this.synt[i].set(\damp, num.value);
				dm[i] = num.value;
				if (guiflag) {
					dmbox[i].value = num.value;
						if (i == currentsource) {
						{dmslider.value = num.value}.defer;
						{dmnumbox.value = num.value}.defer;
					};
				};
				if (this.ossiadstdec.notNil) {
					if (this.ossiadstdec[i].v != num.value) {
						this.ossiadstdec[i].v_(num.value);
					};
				};
			};

			rboxProxy[i].action = {arg num;
				this.synt[i].set(\rotAngle, num.value);
				this.setSynths(i, \rotAngle, num.value);
				rlev[i] = num.value;
				if (guiflag) {
					{rbox[i].value = num.value}.defer;
					if(i == currentsource)
					{
						//num.value * 6.28 - pi;
						{rslider.value = (num.value + pi) / 2pi}.defer;
						{rnumbox.value = num.value}.defer;
					};
				};
				if (this.ossiarot.notNil) {
					if (this.ossiarot[i].v != num.value) {
						this.ossiarot[i].v_(num.value);
					};
				};
			};

			dboxProxy[i].action = {arg num;
				this.synt[i].set(\directang, num.value);
				this.setSynths(i, \directang, num.value);
				dlev[i] = num.value;
				if (guiflag) {
					{dbox[i].value = num.value}.defer;
					if(i == currentsource)
					{
						//num.value * pi/2;
						{dirslider.value = num.value / (pi/2)}.defer;
						{dirnumbox.value = num.value}.defer;
					};
				};
				if (this.ossiadir.notNil) {
					if (this.ossiadir[i].v != num.value) {
						this.ossiadir[i].v_(num.value);
					};
				};
			};

			cboxProxy[i].action = {arg num;
				this.synt[i].set(\contr, num.value);
				// TESTING
				this.espacializador[i].set(\contr, num.value);
				this.setSynths(i, \contr, num.value);
				clev[i] = num.value;
				if (guiflag) {
					{cbox[i].value = num.value}.defer;
					if (i == currentsource)
					{
						{cslider.value = num.value}.defer;
						{connumbox.value = num.value}.defer;
					};
				};
				if (this.ossiactr.notNil) {
					if (this.ossiactr[i].v != num.value) {
						this.ossiactr[i].v_(num.value);
					};
				};
			};

			dpboxProxy[i].action = {arg num;
				// used for b-format amb/bin only
				this.synt[i].set(\dopamnt, num.value);
				this.setSynths(i, \dopamnt, num.value);
				// used for the others
				this.espacializador[i].set(\dopamnt, num.value);
				dplev[i] = num.value;
				if (guiflag) {
					{dpbox[i].value = num.value}.defer;
					if(i == currentsource) {
						{dpslider.value = num.value}.defer;
						{dopnumbox.value = num.value}.defer;
					};
				};
				if (this.ossiadp.notNil) {
					if (this.ossiadp[i].v != num.value) {
						this.ossiadp[i].v_(num.value);
					};
				};
			};


			a1boxProxy[i].action = {arg num;
				this.setSynths(i, \aux1, num.value);
				aux1[i] = num.value;
				if((i == currentsource) && guiflag)
				{
					{auxslider1.value = num.value}.defer;
					{aux1numbox.value = num.value}.defer;
				};
				if (guiflag) {
					{this.a1box[i].value = num.value}.defer;
				};
			};

			a2boxProxy[i].action = {arg num;
				this.setSynths(i, \aux2, num.value);
				aux2[i] = num.value;
				if((i == currentsource) && guiflag)
				{
					{auxslider2.value = num.value}.defer;
					{aux2numbox.value = num.value}.defer;
				};
				if (guiflag) {
					{this.a2box[i].value = num.value}.defer;
				};
			};

			a3boxProxy[i].action = {arg num;
				this.setSynths(i, \aux3, num.value);
				aux3[i] = num.value;
				if((i == currentsource) && guiflag)
				{
					{auxslider3.value = num.value}.defer;
					{aux3numbox.value = num.value}.defer;
				};
				if (guiflag) {
					{this.a3box[i].value = num.value}.defer;
				};
			};

			a4boxProxy[i].action = {arg num;
				this.setSynths(i, \aux4, num.value);
				aux4[i] = num.value;
				if((i == currentsource) && guiflag)
				{
					{auxslider4.value = num.value}.defer;
					{aux4numbox.value = num.value}.defer;
				};
				if (guiflag) {
					{this.a4box[i].value = num.value}.defer;
				};
			};

			a5boxProxy[i].action = {arg num;
				this.setSynths(i, \aux5, num.value);
				aux5[i] = num.value;
				if((i == currentsource) && guiflag)
				{
					{auxslider5.value = num.value}.defer;
					{aux5numbox.value = num.value}.defer;
				};
				if (guiflag) {
					{this.a5box[i].value = num.value}.defer;
				};
			};

			a1checkProxy[i].action = { arg but;

				if (but.value) {
					this.a1but[i] = 1;
					this.setSynths(i, \a1check, 1);
				}{
					this.a1but[i] = 0;
					this.setSynths(i, \a1check, 0);
				};

				if (guiflag) {

					{this.a1check[i].value = but.value}.defer;
				};
			};

			a2checkProxy[i].action = { arg but;

				if (but.value) {
					a2but[i] = 1;
					this.setSynths(i, \a2check, 1);
				}{
					a2but[i] = 0;
					this.setSynths(i, \a2check, 0);
				};
				if (guiflag) {
					{this.a2check[i].value = but.value}.defer;
				};
			};


			a3checkProxy[i].action = { arg but;

				if (but.value) {
					a3but[i] = 1;
					this.setSynths(i, \a3check, 1);
				}{
					a3but[i] = 0;
					this.setSynths(i, \a3check, 0);
				};
				if (guiflag) {
					{this.a3check[i].value = but.value}.defer;
				};
			};



			a4checkProxy[i].action = { arg but;

				if (but.value) {
					a4but[i] = 1;
					this.setSynths(i, \a4check, 1);
				}{
					a4but[i] = 0;
					this.setSynths(i, \a4check, 0);
				};
				if (guiflag) {
					{this.a4check[i].value = but.value}.defer;
				};
			};


			a5checkProxy[i].action = { arg but;

				if (but.value) {
					a5but[i] = 1;
					this.setSynths(i, \a5check, 1);
				}{
					a5but[i] = 0;
					this.setSynths(i, \a5check, 0);
				};
				if (guiflag) {
					{this.a5check[i].value = but.value}.defer;
				};
			};



			stcheckProxy[i].action = { arg but;
				if (but.value) {
					this.streamdisk[i] = true;
				}{
					this.streamdisk[i] = false;
				};
				if (guiflag) {
					{this.stcheck[i].value = but.value}.defer;
				};
			};

			this.lpcheckProxy[i].action_({ arg but;
				if (but.value) {
					lp[i] = 1;
					this.synt[i].set(\lp, 1);
					this.setSynths(i, \lp, 1);
				} {
					lp[i] = 0;
					this.synt[i].set(\lp, 0);
					this.setSynths(i, \lp, 0);
				};
				if (guiflag) {
					{this.lpcheck[i].value = but.value}.defer;
					if(i==currentsource) {
						{loopcheck.value = but.value}.defer;
					};
				};
				if (this.ossialoop.notNil) {
					if (this.ossialoop[i].v != but.value.asBoolean) {
						this.ossialoop[i].v_(but.value.asBoolean);
					};
				};
			});

			/// testing

			this.hwncheckProxy[i].action = { arg but;
				if((i==this.currentsource) && guiflag) {
					{hwInCheck.value = but.value}.defer;
				};
				if (but.value == true) {
					if (guiflag) {
						{this.scncheck[i].value = false}.defer;
					};
					if((i==currentsource) && guiflag){{scInCheck.value = false}.defer;};
					hwn[i] = 1;
					scn[i] = 0;
					this.synt[i].set(\hwn, 1);
				}{
					hwn[i] = 0;
					this.synt[i].set(\hwn, 0);
				};
				if (this.guiflag) {
					{this.hwncheck[i].value = but.value}.defer;
				};
			};

			this.scncheckProxy[i].action_({ arg but;
				if((i==currentsource) && guiflag){{scInCheck.value = but.value}.defer;};
				if (but.value == true) {
					if (guiflag) {
						{this.hwncheck[i].value = false}.defer;
					};
					if((i==currentsource) && guiflag){{hwInCheck.value = false}.defer;};
					scn[i] = 1;
					hwn[i] = 0;
					this.synt[i].set(\scn, 1);
				}{
					scn[i] = 0;
					this.synt[i].set(\scn, 0);
				};
				if (guiflag) {
					{this.scncheck[i].value = but.value}.defer;
				};
			});


			//comment out all linear parameters
/*
			this.lncheckProxy[i].action_({ arg but;
				if((i==currentsource) && guiflag){{lincheck.value = but.value}.defer;};
				if (but.value) {
					ln[i] = "_linear";
					this.setSynths(i, \ln, 1);
				}{
					ln[i] = "";
					this.setSynths(i, \ln, 0);
				};
				if (guiflag) {
					{this.lncheck[i].value = but.value}.defer;
				};
			});
*/

			this.spcheckProxy[i].action_({ arg but;
				if((i==currentsource) && guiflag){{spreadcheck.value = but.value}.defer;};
				if (but.value) {
					if (guiflag) {
						{this.dfcheck[i].value = false}.defer;
					};
					if((i==currentsource) && guiflag){{diffusecheck.value = false}.defer;};
					sp[i] = 1;
					df[i] = 0;
					this.espacializador[i].set(\sp, 1);
					this.espacializador[i].set(\df, 0);
					this.synt[i].set(\sp, 1);
					this.setSynths(i, \ls, 1);
				} {
					sp[i] = 0;
					this.espacializador[i].set(\sp, 0);
					this.synt[i].set(\sp, 0);
					this.setSynths(i, \sp, 0);
				};
				if (guiflag) {
					{this.spcheck[i].value = but.value}.defer;
				};
				if (this.ossiasprea.notNil) {
					if (this.ossiasprea[i].v != but.value.asBoolean) {
						this.ossiasprea[i].v_(but.value.asBoolean);
					};
				};
			});

			this.dfcheckProxy[i].action_({ arg but;
				if((i==currentsource) && guiflag){{diffusecheck.value = but.value}.defer;};
				if (but.value) {
					if (guiflag) {
						{this.spcheck[i].value = false}.defer;
					};
					if((i==currentsource) && guiflag){{spreadcheck.value = false}.defer;};
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
				if (guiflag) {
					{this.dfcheck[i].value = but.value}.defer;
				};
				if (this.ossiadiff.notNil) {
					if (this.ossiadiff[i].v != but.value.asBoolean) {
						this.ossiadiff[i].v_(but.value.asBoolean);
					};
				};
			});

			this.ncanboxProxy[i].action = {arg num;
				this.espacializador[i].set(\elev, this.spheval[i].phi);
				this.setSynths(i, \elev, this.spheval[i].phi);
				this.synt[i].set(\elev, this.spheval[i].phi);
				this.ncan[i] = num.value;
				if((i == currentsource) && guiflag )
				{
					//var val = (this.halfwidth - (num.value * width)) * -1;
					//	ncanslider.value = num.value;
					{ncannumbox.value = num.value}.defer;
				};
				if (guiflag) {
					{this.ncanbox[i].value = num.value}.defer;
				};
			};

			this.businiboxProxy[i].action = {arg num;
				this.espacializador[i].set(\elev, this.spheval[i].phi);
				this.setSynths(i, \elev, this.spheval[i].phi);
				this.synt[i].set(\elev, this.spheval[i].phi);
				this.busini[i] = num.value;
				if((i == currentsource) && guiflag)
				{
					//var val = (this.halfwidth - (num.value * width)) * -1;
					//	ncanslider.value = num.value;
					{busininumbox.value = num.value}.defer;
				};
				if (guiflag) {
					{this.businibox[i].value = num.value}.defer;
				};
			};


			this.tfieldProxy[i].action = {arg path;
				if ( (path.notNil || (path != "")) && this.streamdisk[i].not ) {
					sombuf[i] = Buffer.read(server, path.value, action: {arg buf;
						"Loaded file".postln;
					});
				} {
					"To stream file".postln;
				};
				if (guiflag) {
					{this.tfield[i].value = path.value}.defer;
				};
			};

			control.dock(this.xboxProxy[i], "x_axisProxy_" ++ i);
			control.dock(this.yboxProxy[i], "y_axisProxy_" ++ i);
			control.dock(this.zboxProxy[i], "z_axisProxy_" ++ i);
			control.dock(this.vboxProxy[i], "levelProxy_" ++ i);
			control.dock(this.dpboxProxy[i], "dopamtProxy_" ++ i);
			control.dock(this.aboxProxy[i], "angleProxy_" ++ i);
			control.dock(this.gboxProxy[i], "revglobalProxy_" ++ i);
			control.dock(this.lboxProxy[i], "revlocalProxy_" ++ i);
			control.dock(this.rmboxProxy[i], "localroomProxy_" ++ i);
			control.dock(this.dmboxProxy[i], "localdampProxy_" ++ i);
			control.dock(this.rboxProxy[i], "rotationProxy_" ++ i);
			control.dock(this.dboxProxy[i], "directivityProxy_" ++ i);
			control.dock(this.cboxProxy[i], "contractionProxy_" ++ i);
			control.dock(this.a1boxProxy[i], "aux1Proxy_" ++ i);
			control.dock(this.a2boxProxy[i], "aux2Proxy_" ++ i);
			control.dock(this.a3boxProxy[i], "aux3Proxy_" ++ i);
			control.dock(this.a4boxProxy[i], "aux4Proxy_" ++ i);
			control.dock(this.a5boxProxy[i], "aux5Proxy_" ++ i);
			control.dock(this.a1checkProxy[i], "aux1checkProxy_" ++ i);
			control.dock(this.a2checkProxy[i], "aux2checkProxy_" ++ i);
			control.dock(this.a3checkProxy[i], "aux3checkProxy_" ++ i);
			control.dock(this.a4checkProxy[i], "aux4checkProxy_" ++ i);
			control.dock(this.a5checkProxy[i], "aux5checkProxy_" ++ i);
			//control.dock(this.stcheckProxy[i], "stcheckProxy_" ++ i);
		};


		///// these next few are not to be docked


		this.clsrvboxProxy.action_({ arg num;

			this.clsrv = num.value;

			case
			{ num.value == 1 }
			{ clsRvtypes = "_free"; }
			{ num.value == 2 }
			{ clsRvtypes = "_pass"; }
			{ num.value > 2 }
			{ clsRvtypes = "_conv"; };

			if (num.value == 0)
			{
				if (revGlobal.notNil)
				{ this.revGlobal.set(\gate, 0) };

				if (revGlobalBF.notNil)
				{ this.revGlobalBF.set(\gate, 0) };

				if (revGlobalSoa.notNil)
				{ this.revGlobalSoa.set(\gate, 0) };

			} {

				if (convert_fuma) {
					if (this.convertor.notNil) {
						this.convertor.set(\gate, 1);
					} {
						this.convertor = Synth.new(\ambiConverter, [\gate, 1],
							target:this.glbRevDecGrp).onFree({
							this.convertor = nil;
						});
					};
				};

				if (revGlobal.notNil) {

					this.revGlobal.set(\gate, 0);

					this.revGlobal = Synth.new(\revGlobalAmb++clsRvtypes, [\gbus, gbus, \gate, 1,
						\room, clsrm, \damp, clsdm,
						\wir, rirWspectrum[max((num.value - 3), 0)],
						\xir, rirXspectrum[max((num.value - 3), 0)],
						\yir, rirYspectrum[max((num.value - 3), 0)],
						\zir, rirZspectrum[max((num.value - 3), 0)]],
					this.glbRevDecGrp).register.onFree({
						if (this.revGlobal.isPlaying.not) {
							this.revGlobal = nil;
						};
						if (this.convertor.notNil) {
							if (convert_fuma) {
								if (this.converterNeeded(0).not) {
									this.convertor.set(\gate, 0);
								};
							};
						};
					});

				} {
					this.revGlobal = Synth.new(\revGlobalAmb++clsRvtypes, [\gbus, gbus, \gate, 1,
						\room, clsrm, \damp, clsdm,
						\wir, rirWspectrum[max((num.value - 3), 0)],
						\xir, rirXspectrum[max((num.value - 3), 0)],
						\yir, rirYspectrum[max((num.value - 3), 0)],
						\zir, rirZspectrum[max((num.value - 3), 0)]],
					this.glbRevDecGrp).register.onFree({
						if (this.revGlobal.isPlaying.not) {
							this.revGlobal = nil;
						};
						if (this.convertor.notNil) {
							if (convert_fuma) {
								if (this.converterNeeded(0).not) {
									this.convertor.set(\gate, 0);
								};
							};
						};
					});

				};

				if (this.globBfmtNeeded(0)) {
					if (revGlobalBF.notNil) {
						this.revGlobalBF.set(\gate, 0);

						this.revGlobalBF = Synth.new(\revGlobalBFormatAmb++clsRvtypes,
							[\gbfbus, gbfbus, \gate, 1, \room, clsrm, \damp, clsdm,
								\fluir, rirFLUspectrum[max((num.value - 3), 0)],
								\frdir, rirFRDspectrum[max((num.value - 3), 0)],
								\bldir, rirBLDspectrum[max((num.value - 3), 0)],
								\bruir, rirBRUspectrum[max((num.value - 3), 0)]],
							this.glbRevDecGrp).register.onFree({
							if (this.revGlobalBF.isPlaying.not) {
								this.revGlobalBF = nil;
							};
							if (this.convertor.notNil) {
								if (convert_fuma) {
									if (this.converterNeeded(0).not) {
										this.convertor.set(\gate, 0);
									};
								};
							};
						});

					} {
						this.revGlobalBF = Synth.new(\revGlobalBFormatAmb++clsRvtypes,
							[\gbfbus, gbfbus, \gate, 1, \room, clsrm, \damp, clsdm,
								\fluir, rirFLUspectrum[max((num.value - 3), 0)],
								\frdir, rirFRDspectrum[max((num.value - 3), 0)],
								\bldir, rirBLDspectrum[max((num.value - 3), 0)],
								\bruir, rirBRUspectrum[max((num.value - 3), 0)]],
							this.glbRevDecGrp).register.onFree({
							if (this.revGlobalBF.isPlaying.not) {
								this.revGlobalBF = nil;
							};
							if (this.convertor.notNil) {
								if (convert_fuma) {
									if (this.converterNeeded(0).not) {
										this.convertor.set(\gate, 0);
									};
								};
							};
						});

					};
				};

				if (this.maxorder > 1) {
					if (this.globSoaA12Needed(0)) {
						if (revGlobalSoa.notNil) {
							this.revGlobalSoa.set(\gate, 0);

							this.revGlobalSoa = Synth.new(\revGlobalSoaA12++clsRvtypes,
								[\soaBus, soaBus, \gate, 1, \room, clsrm, \damp, clsdm,
									\a0ir, rirA12Spectrum[max((num.value - 3), 0), 0],
									\a1ir, rirA12Spectrum[max((num.value - 3), 0), 1],
									\a2ir, rirA12Spectrum[max((num.value - 3), 0), 2],
									\a3ir, rirA12Spectrum[max((num.value - 3), 0), 3],
									\a4ir, rirA12Spectrum[max((num.value - 3), 0), 4],
									\a5ir, rirA12Spectrum[max((num.value - 3), 0), 5],
									\a6ir, rirA12Spectrum[max((num.value - 3), 0), 6],
									\a7ir, rirA12Spectrum[max((num.value - 3), 0), 7],
									\a8ir, rirA12Spectrum[max((num.value - 3), 0), 8],
									\a9ir, rirA12Spectrum[max((num.value - 3), 0), 9],
									\a10ir, rirA12Spectrum[max((num.value - 3), 0), 10],
									\a11ir, rirA12Spectrum[max((num.value - 3), 0), 11]],
								this.glbRevDecGrp).onFree({
								this.revGlobalSoa = nil;
							});

						} {
							this.revGlobalSoa = Synth.new(\revGlobalSoaA12++clsRvtypes,
								[\soaBus, soaBus, \gate, 1, \room, clsrm, \damp, clsdm,
									\a0ir, rirA12Spectrum[max((num.value - 3), 0), 0],
									\a1ir, rirA12Spectrum[max((num.value - 3), 0), 1],
									\a2ir, rirA12Spectrum[max((num.value - 3), 0), 2],
									\a3ir, rirA12Spectrum[max((num.value - 3), 0), 3],
									\a4ir, rirA12Spectrum[max((num.value - 3), 0), 4],
									\a5ir, rirA12Spectrum[max((num.value - 3), 0), 5],
									\a6ir, rirA12Spectrum[max((num.value - 3), 0), 6],
									\a7ir, rirA12Spectrum[max((num.value - 3), 0), 7],
									\a8ir, rirA12Spectrum[max((num.value - 3), 0), 8],
									\a9ir, rirA12Spectrum[max((num.value - 3), 0), 9],
									\a10ir, rirA12Spectrum[max((num.value - 3), 0), 10],
									\a11ir, rirA12Spectrum[max((num.value - 3), 0), 11]],
								this.glbRevDecGrp).onFree({
								this.revGlobalSoa = nil;
							});
						};
					};
				};
			};

			if (guiflag) {
				{ clsReverbox.value = num.value }.defer;
			};
			if (this.ossiacls.notNil) {
				if (this.ossiacls.v != num.value) {
					this.ossiacls.v_(num.value);
				};
			};
		});


		clsrmboxProxy.action_({arg num;

			this.glbRevDecGrp.set(\room, num.value);

			clsrm = num.value;

			if (guiflag) {
				//{clsrmslider.value = num.value}.defer;
				{clsrmnumbox.value = num.value}.defer;
				//clsrmbox.value = num.value;
			};
			if (this.ossiaclsdel.notNil) {
				if (this.ossiaclsdel.v != num.value) {
					this.ossiaclsdel.v_(num.value);
				};
			};
		});


		clsdmboxProxy.action_({arg num;

			this.glbRevDecGrp.set(\damp, num.value);

			clsdm = num.value;

			if (guiflag) {
				//{clsdmslider.value = num.value}.defer;
				{clsdmnumbox.value = num.value}.defer;
				//clsdmbox.value = num.value;
			};
			if (this.ossiaclsdec.notNil) {
				if (this.ossiaclsdec.v != num.value) {
					this.ossiaclsdec.v_(num.value);
				};
			};
		});


		this.headingnumboxProxy.action_({ arg num;
			this.globDec.set(\heading, num.value);
			if (guiflag) {
				{this.headingnumbox.value = num.value;}.defer;
			};
		});

		this.rollnumboxProxy.action_({ arg num;
			this.globDec.set(\roll, num.value);
			if (guiflag) {
				{this.rollnumbox.value = num.value;}.defer;
			};
		});

		this.pitchnumboxProxy.action_({arg num;
			this.globDec.set(\pitch, num.value);
			if (guiflag) {
				{this.pitchnumbox.value = num.value;}.defer;
			};
		});



		///////////////////////////////////////////////////



		playMonoInFunc = Array.newClear(4); // one for File, Stereo, BFormat, Stream - streamed file;
		playStereoInFunc = Array.newClear(4);
		playBFormatInFunc = Array.newClear(4);
		this.synthRegistry = Array.newClear(this.nfontes);
		this.nfontes.do { arg i;
			this.synthRegistry[i] = List[];
		};

		this.streambuf = Array.newClear(this.nfontes);
		// this.streamrate = Array.newClear(this.nfontes); // apparently unused

		this.scInBus = Array.newClear(this.nfontes);
		this.nfontes.do { arg i;
			this.scInBus[i] = Bus.audio(server, 4);
		};

		this.insertFlag = Array.newClear(this.nfontes);
		this.aFormatBusFoa = Array2D.new(2, this.nfontes);
		this.aFormatBusSoa = Array2D.new(2, this.nfontes);

		this.nfontes.do { arg i;
			this.aFormatBusFoa[0, i] =  Bus.audio(server, 4);
			//server.sync;
		};
		this.nfontes.do { arg i;
			this.aFormatBusFoa[1, i] =  Bus.audio(server, 4);
			//server.sync;
		};
		this.nfontes.do { arg i;
			this.aFormatBusSoa[0, i] =  Bus.audio(server, 12);
			//server.sync;
		};
		this.nfontes.do { arg i;
			this.aFormatBusSoa[1, i] =  Bus.audio(server, 12);
			//server.sync;
		};
		this.nfontes.do { arg i;
			this.insertFlag[i] = 0;
		};


		// array of functions, 1 for each source (if defined), that will be launched on Automation's "play"
		this.triggerFunc = Array.newClear(this.nfontes);
		//companion to above. Launched by "Stop"
		this.stopFunc = Array.newClear(this.nfontes);


		// can place headtracker rotations in these functions - don't forget that the synthdefs
		// need to have there values for heading, roll and pitch "set" by serial routine
		// NO - DON'T PUT THIS HERE - Make a global synth with a common input bus

		///////////// Functions to substitute blocks of code in SynthDefs //////////////

		if (decoder.notNil) {

			if(this.maxorder == 1) {

				espacAmbOutFunc = { |ambsinal, ambsinal1O|
					Out.ar(this.globTBus, ambsinal1O);
				};
				ambixOutFunc = { |ambsinal|
					Out.ar(this.ambixbus, ambsinal);
				};
				espacAmbEstereoOutFunc = { |ambsinal1plus2, ambsinal1plus2_1O|
					Out.ar(this.globTBus, ambsinal1plus2_1O);
				};
				revGlobalAmbFunc = { |ambsinal|
					Out.ar(this.globTBus, ambsinal);
				};
				revGlobalSoaOutFunc = { |soaSig, foaSig|
					Out.ar(this.globTBus, foaSig);
				};
				playBFormatOutFunc = { |player|
					Out.ar(this.globTBus, player);
				};
				reverbOutFunc = { |soaBus, gbfbus, ambsinal, ambsinal1O, globallev, locallev |
					Out.ar(gbfbus, (ambsinal1O*globallev) + (ambsinal1O*locallev));};

			} {
				espacAmbOutFunc = { |ambsinal, ambsinal1O|
					Out.ar(this.globTBus, ambsinal);
				};
				ambixOutFunc = { |ambsinal|
					Out.ar(this.ambixbus, ambsinal);
				};
				espacAmbEstereoOutFunc = { |ambsinal1plus2, ambsinal1plus2_1O|
					Out.ar(this.globTBus, ambsinal1plus2);
				};
				revGlobalAmbFunc = { |ambsinal|
					Out.ar(this.globTBus, ambsinal);
				};
				revGlobalSoaOutFunc = { |soaSig, foaSig|
					Out.ar(this.globTBus, soaSig);
				};
				playBFormatOutFunc = { |player|
					Out.ar(this.globTBus, player);
				};
				reverbOutFunc = { |soaBus, gbfbus, ambsinal, ambsinal1O, globallev, locallev |
					Out.ar(gbfbus, (ambsinal*globallev) + (ambsinal*locallev));};
			}

		} {
			if(this.maxorder == 1) {
				espacAmbOutFunc = { |ambsinal, ambsinal1O|
					Out.ar(rawbusfuma, ambsinal1O);
				};
				ambixOutFunc = { |ambsinal|
					Out.ar(rawbusambix, ambsinal);
				};
				espacAmbEstereoOutFunc = { |ambsinal1plus2, ambsinal1plus2_1O|
					Out.ar(rawbusfuma, ambsinal1plus2_1O);
				};
				revGlobalAmbFunc = { |ambsinal|
					Out.ar(rawbusfuma, ambsinal);
				};
				revGlobalSoaOutFunc = { |soaSig, foaSig|
					Out.ar(rawbusfuma, foaSig);
				};
				reverbOutFunc = { |soaBus, gbfbus, ambsinal, ambsinal1O, globallev, locallev |
					Out.ar(gbfbus, (ambsinal1O*globallev) + (ambsinal1O*locallev));
				};

			} {
				espacAmbOutFunc = { |ambsinal, ambsinal1O|
					Out.ar(rawbusfuma, ambsinal);
				};
				ambixOutFunc = { |ambsinal|
					Out.ar(rawbusambix, ambsinal);
				};
				espacAmbEstereoOutFunc = { |ambsinal1plus2, ambsinal1plus2_1O|
					Out.ar(rawbusfuma, ambsinal1plus2);
				};
				revGlobalAmbFunc = { |ambsinal|
					Out.ar(rawbusfuma, ambsinal);
				};
				revGlobalSoaOutFunc = { |soaSig, foaSig|
					Out.ar(rawbusfuma, soaSig);
				};
				reverbOutFunc = { |soaBus, gbfbus, ambsinal, ambsinal1O, globallev, locallev |
					Out.ar(soaBus, (ambsinal*globallev) + (ambsinal*locallev));
				};

			};

			playBFormatOutFunc = { |player|
				Out.ar(rawbusfuma, player);
			};

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


		/////////// END code for 2nd order matrices /////////////////////


		prjDr = projDir;


		SynthDef(\blip, {
			var env = Env([0, 0.8, 1, 0], [0, 0.1, 0]);
			var blip = SinOsc.ar(1000) * EnvGen.kr(env, doneAction: 2);
			Out.ar(0, [blip, blip]);
		}).add;



		if (decoder.notNil) {

			if (suboutbus.notNil) {
				subOutFunc = { |signal, sublevel|
					var subOut = Mix.ar(signal) * sublevel;
					Out.ar(suboutbus, signal);
				};
			} {
				subOutFunc = { |signal, sublevel| };
			};

			case
			{ this.maxorder == 1 }
			{ convert_fuma = false;
				convert_ambix = true;

				SynthDef.new("ambiConverter", { arg gate = 1;
					var ambixsig, env;
					env = EnvGen.kr(Env.asr(curve:\hold), gate, doneAction:2);
					ambixsig = In.ar(this.ambixbus, 4);
					ambixsig = FoaEncode.ar(ambixsig, FoaEncoderMatrix.newAmbix1) * env;
					espacAmbOutFunc.value(ambixsig, ambixsig);
				}).add;

				if (this.serport.notNil) {
					SynthDef.new("globDecodeSynth",  { arg heading=0, roll=0, pitch=0, sub = 1;
						var sig;
						sig = In.ar(this.globTBus, 4);
						sig = FoaTransform.ar(sig, 'rtt',  Lag.kr(heading, 0.01),
							Lag.kr(roll, 0.01),
							Lag.kr(pitch, 0.01));
						sig = FoaDecode.ar(sig, decoder);
						nonAmbiFunc.value(sig);
						subOutFunc.value(sig, sub * 0.55);
						Out.ar(outbus, sig);
					}).add;
				} {
					SynthDef.new("globDecodeSynth",  { arg heading=0, roll=0, pitch=0, sub = 1;
						var sig;
						sig = In.ar(this.globTBus, 4);
						sig = FoaDecode.ar(sig, decoder);
						nonAmbiFunc.value(sig);
						subOutFunc.value(sig, sub * 0.55);
						Out.ar(outbus, sig);
					}).add;
				}
			}

			{ this.maxorder == 2 }
			{ convert_fuma = true;
				convert_ambix = false;

				SynthDef.new("ambiConverter", { arg gate = 1;
					var sig, env;
					env = EnvGen.kr(Env.asr(curve:\hold), gate, doneAction:2);
					sig = In.ar(this.globTBus, 9);
					sig = HOAConvert.ar(2, sig, \FuMa, \ACN_SN3D) * env;
					ambixOutFunc.value(sig);
				}).add;

				SynthDef("globDecodeSynth", {
					arg lf_hf=0, xover=400, sub = 1;
					var sig;
					sig = In.ar(this.ambixbus, bFormNumChan);
					sig = decoder.ar(sig[0], sig[1], sig[2], sig[3], sig[4],
						sig[5], sig[6], sig[7], sig[8], 0, lf_hf, xover:xover);
					nonAmbiFunc.value(sig);
					subOutFunc.value(sig, sub);
					sig = sig * 2; //make upd for generaly low output
					Out.ar(outbus, sig);
				}).add;
			}

			{ this.maxorder == 3 }
			{ convert_fuma = true;
				convert_ambix = false;

				SynthDef.new("ambiConverter", { arg gate = 1;
					var sig, env;
					env = EnvGen.kr(Env.asr(curve:\hold), gate, doneAction:2);
					sig = In.ar(this.globTBus, 9);
					sig = HOAConvert.ar(2, sig, \FuMa, \ACN_SN3D) * env;
					ambixOutFunc.value(sig);
				}).add;

				SynthDef("globDecodeSynth", {
					arg lf_hf=0, xover=400, sub = 1;
					var sig;
					sig = In.ar(this.ambixbus, bFormNumChan);
					sig = decoder.ar(sig[0], sig[1], sig[2], sig[3], sig[4],
						sig[5], sig[6], sig[7], sig[8], sig[9], sig[10], sig[11],
						sig[12], sig[13], sig[14], sig[15], 0, lf_hf, xover:xover);
					nonAmbiFunc.value(sig);
					subOutFunc.value(sig, sub);
					sig = sig * 2; //make upd for generaly low output
					Out.ar(outbus, sig);
				}).add;
			}

			{ this.maxorder == 4 }
			{ convert_fuma = true;
				convert_ambix = false;

				SynthDef.new("ambiConverter", { arg gate = 1;
					var sig, env;
					env = EnvGen.kr(Env.asr(curve:\hold), gate, doneAction:2);
					sig = In.ar(this.globTBus, 9);
					sig = HOAConvert.ar(2, sig, \FuMa, \ACN_SN3D) * env;
					ambixOutFunc.value(sig);
				}).add;

				SynthDef("globDecodeSynth", {
					arg lf_hf=0, xover=400, sub = 1;
					var ambixsig, sig;
					sig = In.ar(this.ambixbus, bFormNumChan);
					sig = decoder.ar(sig[0], sig[1], sig[2], sig[3], sig[4],
						sig[5], sig[6], sig[7], sig[8], sig[9], sig[10], sig[11],
						sig[12], sig[13], sig[14],sig[15], sig[16], sig[17], sig[18],
						sig[19], sig[20], sig[21], sig[22], sig[23], sig[24],
						0, lf_hf, xover:xover);
					nonAmbiFunc.value(sig);
					subOutFunc.value(sig, sub);
					sig = sig * 2; //make upd for generaly low output
					Out.ar(outbus, sig);
				}).add;
			}

			{ this.maxorder == 5 }
			{ convert_fuma = true;
				convert_ambix = false;

				SynthDef.new("ambiConverter", { arg gate = 1;
					var sig, env;
					env = EnvGen.kr(Env.asr(curve:\hold), gate, doneAction:2);
					sig = In.ar(this.globTBus, 9);
					sig = HOAConvert.ar(2, sig, \FuMa, \ACN_SN3D) * env;
					ambixOutFunc.value(sig);
				}).add;

				SynthDef("globDecodeSynth", {
					arg lf_hf=0, xover=400, sub = 1;
					var sig;
					sig = In.ar(this.ambixbus, bFormNumChan);
					sig = decoder.ar(sig[0], sig[1], sig[2], sig[3], sig[4],
						sig[5], sig[6], sig[7], sig[8], sig[9], sig[10], sig[11],
						sig[12], sig[13], sig[14], sig[15], sig[16], sig[17],
						sig[18], sig[19], sig[20], sig[21], sig[22], sig[23],
						sig[24], sig[15], sig[16], sig[17], sig[18], sig[19],
						sig[20], sig[21], sig[22], sig[23], sig[24], sig[25],
						sig[26], sig[27], sig[28], sig[29], sig[30], sig[31],
						sig[32], sig[33], sig[34], sig[35],
						0, lf_hf, xover:xover);
					nonAmbiFunc.value(sig);
					subOutFunc.value(sig, sub);
					sig = sig * 2; //make upd for generaly low output
					Out.ar(outbus, sig);
				}).add;
			};
		};

		/// non ambisonc spatiaizers setup

		if (speaker_array.notNil) {

			var max_func, dimention, vbap_setup, radiusses, adjust;

			this.nonambibus = outbus;

			numoutputs = speaker_array.size;

			max_func = { |x|
				var rep = 0;
				x.do{ |item|
					if(item > rep,
						{ rep = item };
				) };
				rep };

			radiusses = Array.newFrom(speaker_array).collect({ |val| val[2] });

			case
			{ speaker_array[0].size < 2 || speaker_array[0].size > 3 }
			{ ^"bad speaker array".postln }
			{ speaker_array[0].size == 2 }
			{ dimention = 2 }
			{ speaker_array[0].size == 3 }
			{ dimention = 3;

				radius_max = max_func.value(radiusses);

				adjust = Array.fill(numoutputs, { |i|
					[(radius_max - radiusses[i]) / 334, radius_max/radiusses[i]];
				});
			};

			nonAmbiFunc = { |sig|
				var in = In.ar(nonambibus, numoutputs);
				in = Array.fill(numoutputs, { |i| DelayN.ar(in[i],
					delaytime:adjust[i][0], mul:adjust[i][1]) });
				sig = sig + in;
			};

			speaker_array.collect({ |val| val.pop });

			vbap_setup = VBAPSpeakerArray(dimention, speaker_array);

			vbap_buffer = Buffer.loadCollection(server, vbap_setup.getSetsAndMatrices);

			convert_direct = false;

		} {

			var emulate_array, vbap_setup;

			numoutputs = 18;

			this.nonambibus = Bus.audio(server, numoutputs);

			emulate_array = [[-45, 50], [-135, 50], [135, 50], [45, 50], [-30, 20], [-90, 20],
				[-150, 20], [150, 20], [90, 20], [30, 20], [-22.5, -3], [-67.5, -3], [-112.5, -3],
				[-157.5, -3], [157.5, -3], [112.5, -3], [67.5, -3], [22.5, -3]];

			vbap_setup = VBAPSpeakerArray(3, emulate_array);
			//emulate the dome at "Le SCRIME" (scrime.u-bordeaux.fr)

			vbap_buffer = Buffer.loadCollection(server, vbap_setup.getSetsAndMatrices);

			radius_max = 2.829;

			nonAmbiFunc = { |sig|
				sig;
			};

			SynthDef.new("nonAmbi2FuMa", { //arg inbus = nonambibus;
				var sig = In.ar(nonambibus, numoutputs);
				sig = FoaEncode.ar(sig, FoaEncoderMatrix.newDirections(emulate_array.degrad));
				espacAmbOutFunc.value(sig, sig);
			}).add;

			convert_direct = convert_fuma;

		};


		makeSpatialisers = { arg rev_type;

		//comment out all linear parameters
/*
		makeSpatialisers = { arg linear = false;
			if(linear) {
				linear = "_linear";
			} {
				linear = "";
			};

			SynthDef.new("espacAFormatVerb"++linear,  {
*/


			SynthDef.new("ATKChowning"++rev_type,  {
				arg inbus, gbus, soaBus, azim = 0, elev = 0, radius = 0,
				dopamnt = 0, sp, df,
				glev = 0, llev = 0, contr = 1,
				insertFlag = 0, aFormatBusOutFoa, aFormatBusInFoa,
				aFormatBusOutSoa, aFormatBusInSoa,
				aFormatFoa, aFormatSoa, ambSigFoaProcessed, ambSigSoaProcessed,
				room = 0.5, damp = 0.5, wir;

				var wRef, xRef, yRef, zRef, rRef, sRef, tRef, uRef, vRef, pRef,
				ambSigSoa, ambSigFoa,
				junto, rd, dopplershift, az, ele, dis, xatras, yatras,
				globallev, locallev, gsig,
				intens,
				spread, diffuse, omni,
				soa_a12_sig;
				var lrev, p;
				var grevganho = 0.04; // needs less gain
				var w, x, y, z, r, s, t, u, v;
				var ambSigRef = Ref(0);
				var lrevRef = Ref(0);
				contr = Lag.kr(contr, 0.1);
				dis = radius;

				az = azim - 1.5707963267949;
				// az = CircleRamp.kr(az, 0.1, -pi, pi);
				ele = elev;
				// ele = Lag.kr(elev, 0.1);
				dis = Select.kr(dis < 0, [dis, 0]);
				dis = Select.kr(dis > 1, [dis, 1]);

				// high freq attenuation
				p = In.ar(inbus, 1);
				p = LPF.ar(p, (1 - dis) * 18000 + 2000); // attenuate high freq with distance
				// Doppler
				rd = dis * 340;
				rd = Lag.kr(rd, 1.0);
				dopplershift = DelayC.ar(p, 0.2, rd/1640.0 * dopamnt);
				p = dopplershift;

				// Global reverberation & intensity
				globallev = 1 / dis.sqrt;
				intens = globallev - 1;
				intens = Select.kr(intens > 4, [intens, 4]);
				intens = Select.kr(intens < 0, [intens, 0]);
				intens = intens / 4;

				globallev = globallev - 1.0; // lower tail of curve to zero
				globallev = globallev / 3; // scale it so that it values 1 close to origin
				globallev = Select.kr(globallev > 1, [globallev, 1]);
				globallev = Select.kr(globallev < 0, [globallev, 0]);

				globallev = globallev * Lag.kr(glev, 0.1);
				gsig = p * globallev;

				Out.ar(gbus, gsig); //send part of direct signal global reverb synth

				// Local reverberation
				locallev = dis;
				locallev = locallev  * Lag.kr(llev, 0.1);

				localReverbFunc.value(lrevRef, p, fftsize, wir, locallev, room, damp);

				junto = (p + lrevRef.value) * 3.5; // match other spatializers gain

				// do second order encoding
				//comment out all linear parameters
				//prepareAmbSigFunc.value(ambSigRef, junto, az, el, intens: intens, dis: dis);
				ambSigRef.value = FMHEncode0.ar(junto, az, ele, intens);

				ambSigFoa = [ambSigRef[0].value, ambSigRef[1].value, ambSigRef[2].value, ambSigRef[3].value];
				ambSigSoa = [ambSigRef[0].value, ambSigRef[1].value, ambSigRef[2].value, ambSigRef[3].value,
					ambSigRef[4].value, ambSigRef[5].value, ambSigRef[6].value, ambSigRef[7].value,
					ambSigRef[8].value];

				omni = FoaEncode.ar(junto, foaEncoderOmni);
				spread = FoaEncode.ar(junto, foaEncoderSpread);
				diffuse = FoaEncode.ar(junto, foaEncoderDiffuse);
				junto = Select.ar(df, [omni, diffuse]);
				junto = Select.ar(sp, [junto, spread]);

				ambSigFoa = FoaTransform.ar(junto, 'push', 1.5707963267949 * contr, az, ele, intens);

				dis = dis * 5.0;
				dis = Select.kr(dis < 0.001, [dis, 0.001]);
				ambSigFoa = HPF.ar(ambSigFoa, 20); // stops bass frequency blow outs by proximity
				ambSigFoa = FoaTransform.ar(ambSigFoa, 'proximity', dis);

				ambSigSoa = [ambSigRef[0].value, ambSigRef[1].value, ambSigRef[2].value, ambSigRef[3].value,
					ambSigRef[4].value, ambSigRef[5].value, ambSigRef[6].value, ambSigRef[7].value,
					ambSigRef[8].value];

				// convert to A-format and send to a-format out busses
				aFormatFoa = FoaDecode.ar(ambSigFoa, b2a);
				Out.ar(aFormatBusOutFoa, aFormatFoa);
				aFormatSoa = AtkMatrixMix.ar(ambSigSoa, soa_a12_decoder_matrix);
				Out.ar(aFormatBusOutSoa, aFormatSoa);

				// flag switchable selector of a-format signal (from insert or not)
				aFormatFoa = Select.ar(insertFlag, [aFormatFoa, InFeedback.ar(aFormatBusInFoa, 4)]);
				aFormatSoa = Select.ar(insertFlag, [aFormatSoa, InFeedback.ar(aFormatBusInSoa, 12)]);

				// convert back to b-format
				ambSigFoaProcessed = FoaEncode.ar(aFormatFoa, a2b);
				ambSigSoaProcessed = AtkMatrixMix.ar(aFormatSoa, soa_a12_encoder_matrix);

				// not sure if the b2a/a2b process degrades signal. Just in case it does:
				ambSigFoa = Select.ar(insertFlag, [ambSigFoa, ambSigFoaProcessed]);
				ambSigSoa = Select.ar(insertFlag, [ambSigSoa, ambSigSoaProcessed]);

				espacAmbOutFunc.value(ambSigSoa, ambSigFoa);
			}).load(server);




			SynthDef.new("ambitoolsChowning"++rev_type,  {
				arg inbus, gbus, azim = 0, elev = 0, radius = 0,
				dopamnt = 0, glev = 0, llev = 0,
				room = 0.5, damp = 05, wir;

				var ambSig,junto, rd, dopplershift, az, ele, dis, xatras, yatras,
				globallev, locallev, gsig, intens;

				var p;
				var grevganho = 0.04; // needs less gain
				var lrevRef = Ref(0);
				dis = radius;

				az = azim - 1.5707963267949;
				az = CircleRamp.kr(az, 0.1, -pi, pi);
				ele = Lag.kr(elev, 0.1);
				dis = Select.kr(dis < (radius_max * 0.05), [ dis, (radius_max * 0.05) ]);
				dis = Select.kr(dis > 1, [dis, 1]);
				p = In.ar(inbus, 1);

				// Doppler
				rd = dis * 340;
				rd = Lag.kr(rd, 1.0);
				dopplershift = DelayC.ar(p, 0.2, rd/1640.0 * dopamnt);
				p = dopplershift;

				// Global reverberation & intensity
				globallev = 1 / dis.sqrt;
				intens = globallev - 1;
				intens = Select.kr(intens > 4, [intens, 4]);
				intens = Select.kr(intens < 0, [intens, 0]);
				intens = intens / 4;

				globallev = globallev - 1.0; // lower tail of curve to zero
				globallev = globallev / 3; // scale it so that it values 1 close to origin
				globallev = Select.kr(globallev > 1, [globallev, 1]);
				globallev = Select.kr(globallev < 0, [globallev, 0]);

				globallev = globallev * Lag.kr(glev, 0.1);
				gsig = p * globallev;

				Out.ar(gbus, gsig); //send part of direct signal global reverb synth

				//Local reverberation
				locallev = dis;
				locallev = locallev * Lag.kr(llev, 0.1);

				//applie distance attenuation before mixxing in reverb to keep trail off
				p = p * (1 - dis);

				localReverbFunc.value(lrevRef, p, fftsize, wir, locallev, room, damp);

				junto = p + lrevRef.value;

				//dis = Select.kr(dis < 0.5, [dis, 0.5]);
				ambSig = HOAEncoder.ar(this.maxorder, junto, az, ele, 6,
					plane_spherical:1, radius: VarLag.kr(dis.squared * 50), speaker_radius: radius_max);

				ambixOutFunc.value(ambSig);
			}).load(server);



			SynthDef.new("hoaLibChowning"++rev_type,  {
				arg inbus, gbus, azim = 0, elev = 0, radius = 0,
				dopamnt = 0, glev = 0, llev = 0,
				room = 0.5, damp = 0.5, wir;

				var ambSig,junto, rd, dopplershift, az, ele, dis, xatras, yatras,
				globallev, locallev, gsig, intens;

				var p;
				var grevganho = 0.04; // needs less gain
				var lrevRef = Ref(0);
				dis = radius;

				az = azim - 1.5707963267949;
				az = CircleRamp.kr(az, 0.1, -pi, pi);
				ele = Lag.kr(elev, 0.1);
				dis = Select.kr(dis < 0, [dis, 0]);
				dis = Select.kr(dis > 1, [dis, 1]);
				p = In.ar(inbus, 1);
				p = LPF.ar(p, (1 - dis) * 18000 + 2000); // attenuate high freq with distance

				// Doppler
				rd = dis * 340;
				rd = Lag.kr(rd, 1.0);
				dopplershift= DelayC.ar(p, 0.2, rd/1640.0 * dopamnt);
				p = dopplershift;

				// Global reverberation & intensity
				globallev = 1 / dis.sqrt;
				intens = globallev - 1;
				intens = Select.kr(intens > 4, [intens, 4]);
				intens = Select.kr(intens < 0, [intens, 0]);
				intens = intens / 4;

				globallev = globallev - 1.0; // lower tail of curve to zero
				globallev = globallev / 3; // scale it so that it values 1 close to origin
				globallev = Select.kr(globallev > 1, [globallev, 1]);
				globallev = Select.kr(globallev < 0, [globallev, 0]);

				globallev = globallev * Lag.kr(glev, 0.1);
				gsig = p * globallev;

				Out.ar(gbus, gsig); //send part of direct signal global reverb synth

				// Local reverberation
				locallev = dis;
				locallev = locallev  * Lag.kr(llev, 0.1);

				//applie distance attenuation before mixxing in reverb to keep trail off
				p = p * (1 - dis).squared;

				localReverbFunc.value(lrevRef, p, fftsize, wir, locallev, room, damp);

				junto = p + lrevRef.value;

				//dis = Select.kr(dis < 0.5, [dis, 0.5]);
				ambSig = HOALibEnc3D.ar(this.maxorder, junto, az, ele, 8);

				ambixOutFunc.value(ambSig);
			}).load(server);



			SynthDef.new("ambiPannerChowning"++rev_type,  {
				arg inbus, gbus, azim = 0, elev = 0, radius = 0,
				dopamnt = 0, glev = 0, llev = 0,
				room = 0.5, damp = 0.5, wir;

				var ambSig,junto, rd, dopplershift, az, ele, dis, xatras, yatras,
				globallev, locallev, gsig, intens;

				var p;
				var grevganho = 0.04; // needs less gain
				var lrevRef = Ref(0);
				dis = radius;

				az = azim - 1.5707963267949;
				az = CircleRamp.kr(az, 0.1, -pi, pi);
				ele = Lag.kr(elev, 0.1);
				dis = Select.kr(dis < 0, [dis, 0]);
				dis = Select.kr(dis > 1, [dis, 1]);
				p = In.ar(inbus, 1);
				p = LPF.ar(p, (1 - dis) * 18000 + 2000); // attenuate high freq with distance

				// Doppler
				rd = dis * 340;
				rd = Lag.kr(rd, 1.0);
				dopplershift= DelayC.ar(p, 0.2, rd/1640.0 * dopamnt);
				p = dopplershift;

				// Global reverberation & intensity
				globallev = 1 / dis.sqrt;
				intens = globallev - 1;
				intens = Select.kr(intens > 4, [intens, 4]);
				intens = Select.kr(intens < 0, [intens, 0]);
				intens = intens / 4;

				globallev = globallev - 1.0; // lower tail of curve to zero
				globallev = globallev / 3; // scale it so that it values 1 close to origin
				globallev = Select.kr(globallev > 1, [globallev, 1]);
				globallev = Select.kr(globallev < 0, [globallev, 0]);

				globallev = globallev * Lag.kr(glev, 0.1);
				gsig = p * globallev;

				Out.ar(gbus, gsig); //send part of direct signal global reverb synth

				// Local reverberation
				locallev = dis;
				locallev = locallev  * Lag.kr(llev, 0.1);

				//applie distance attenuation before mixxing in reverb to keep trail off
				p = p * (1 - dis).squared;

				localReverbFunc.value(lrevRef, p, fftsize, wir, locallev, room, damp);

				junto = p + lrevRef.value;

				//dis = Select.kr(dis < 0.5, [dis, 0.5]);
				ambSig = HOAmbiPanner.ar(this.maxorder, junto, az, ele, 8);

				ambixOutFunc.value(ambSig);
			}).load(server);




			SynthDef.new("VBAPChowning"++rev_type,  {
				arg inbus, gbus, azim = 0, elev = 0, radius = 0,
				dopamnt = 0, glev = 0, llev = 0, contr = 1,
				room = 0.5, damp = 0.5, wir;

				var sig, junto, rd, dopplershift, az, ele, dis, xatras, yatras,
				globallev, locallev, gsig, intens;

				var p;
				var grevganho = 0.04; // needs less gain
				var lrevRef = Ref(0);
				dis = radius;

				az = (azim - 1.5707963267949);
				az = CircleRamp.kr(az, 0.1, -pi, pi);
				az = az * 57.295779513082; // convert to degrees
				ele = Lag.kr(elev * 57.295779513082, 0.1); // convert to degrees
				dis = Select.kr(dis < 0, [dis, 0]);
				dis = Select.kr(dis > 1, [dis, 1]);
				p = In.ar(inbus, 1);
				p = LPF.ar(p, (1 - dis) * 18000 + 2000); // attenuate high freq with distance

				// Doppler
				rd = dis * 340;
				rd = Lag.kr(rd, 1.0);
				dopplershift= DelayC.ar(p, 0.2, rd/1640.0 * dopamnt);
				p = dopplershift;

				// Global reverberation & intensity
				globallev = 1 / dis.sqrt;
				intens = globallev - 1;
				intens = Select.kr(intens > 4, [intens, 4]);
				intens = Select.kr(intens < 0, [intens, 0]);
				intens = intens / 4;

				globallev = globallev - 1.0; // lower tail of curve to zero
				globallev = globallev / 3; // scale it so that it values 1 close to origin
				globallev = Select.kr(globallev > 1, [globallev, 1]);
				globallev = Select.kr(globallev < 0, [globallev, 0]);

				globallev = globallev * Lag.kr(glev, 0.1);
				gsig = (p * globallev);

				Out.ar(gbus, gsig); //send part of direct signal global reverb synth

				// Local reverberation
				locallev = dis;
				locallev = locallev  * Lag.kr(llev, 0.1);

				//applie distance attenuation before mixxing in reverb to keep trail off
				p = p * (1 - dis).squared * 1.5; // match other spatializers gain

				localReverbFunc.value(lrevRef, p, fftsize, wir, locallev, room, damp);

				junto = p + lrevRef.value;

				//dis = Select.kr(dis < 0.5, [dis, 0.5]);
				sig = VBAP.ar(numoutputs, junto, vbap_buffer.bufnum,
					az, ele, (1 - contr) * 100);

				Out.ar(nonambibus, sig);
			}).load(server);


			// This second version of espacAmb is used with contracted B-format sources


			SynthDef.new("ATK2Chowning"++rev_type,  {
				arg inbus, gbus, azim = 0, elev = 0, radius = 0,
				glev = 0, llev = 0.2,
				insertFlag = 0, aFormatBusOutFoa, aFormatBusInFoa,
				aFormatBusOutSoa, aFormatBusInSoa,
				room = 0.5, damp = 0.5, wir;

				var w, x, y, z, r, s, t, u, v, p, ambSigSoa, ambSigFoa,
				junto, rd, dopplershift, az, ele, dis, xatras, yatras,
				globallev = 0.0004, locallev, gsig,
				aFormatFoa, aFormatSoa, ambSigFoaProcessed, ambSigSoaProcessed;
				var lrev,
				intens;
				var ambSigRef = Ref(0);
				var lrevRef = Ref(0);
				var grevganho = 0.20;
				dis = radius;

				az = azim - 1.5707963267949;
				// az = CircleRamp.kr(az, 0.1, -pi, pi);
				ele = elev;
				// ele = Lag.kr(elev, 0.1);
				dis = Select.kr(dis < 0, [dis, 0]);
				dis = Select.kr(dis > 1, [dis, 1]);

				// high freq attenuation
				p = In.ar(inbus, 1);
				p = LPF.ar(p, (1 - dis) * 18000 + 2000); // attenuate high freq with distance

				// Reverberação global
				globallev = 1 / dis.sqrt;
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
				locallev = dis;
				locallev = locallev * Lag.kr(llev, 0.1);

				localReverbFunc.value(lrevRef, p, fftsize, wir, locallev, room, damp);

				junto = (p + lrevRef.value) * 3.5; // match other spatializers gain

				//comment out all linear parameters
				//prepareAmbSigFunc.value(ambSigRef, junto, azim, el, intens: intens, dis: dis);
				ambSigRef.value = FMHEncode0.ar(junto, az, ele, intens);

				ambSigFoa = [ambSigRef[0].value, ambSigRef[1].value, ambSigRef[2].value,
					ambSigRef[3].value];
				ambSigSoa = [ambSigRef[0].value, ambSigRef[1].value, ambSigRef[2].value,
					ambSigRef[3].value,
					ambSigRef[4].value, ambSigRef[5].value, ambSigRef[6].value, ambSigRef[7].value,
					ambSigRef[8].value];

				dis = dis * 5.0;
				dis = Select.kr(dis < 0.001, [dis, 0.001]);
				ambSigFoa = HPF.ar(ambSigFoa, 20); // stops bass frequency blow outs by proximity
				ambSigFoa = FoaTransform.ar(ambSigFoa, 'proximity', dis);

				// convert to A-format and send to a-format out busses
				aFormatFoa = FoaDecode.ar(ambSigFoa, b2a);
				Out.ar(aFormatBusOutFoa, aFormatFoa);
				aFormatSoa = AtkMatrixMix.ar(ambSigSoa, soa_a12_decoder_matrix);
				Out.ar(aFormatBusOutSoa, aFormatSoa);

				// flag switchable selector of a-format signal (from insert or not)
				aFormatFoa = Select.ar(insertFlag, [aFormatFoa, InFeedback.ar(aFormatBusInFoa, 4)]);
				aFormatSoa = Select.ar(insertFlag, [aFormatSoa, InFeedback.ar(aFormatBusInSoa, 12)]);

				// convert back to b-format
				ambSigFoaProcessed = FoaEncode.ar(aFormatFoa, a2b);
				ambSigSoaProcessed = AtkMatrixMix.ar(aFormatSoa, soa_a12_encoder_matrix);

				// not sure if the b2a/a2b process degrades signal. Just in case it does:
				ambSigFoa = Select.ar(insertFlag, [ambSigFoa, ambSigFoaProcessed]);
				ambSigSoa = Select.ar(insertFlag, [ambSigSoa, ambSigSoaProcessed]);

				espacAmbOutFunc.value(ambSigSoa, ambSigFoa);
			}).load(server);



			SynthDef.new("ATKStereoChowning"++rev_type,  {
				arg inbus, gbus, soaBus, azim = 0, elev = 0, radius = 0,
				angle = 1.05,
				dopamnt = 0,
				glev = 0, llev = 0, contr=1,
				sp, df,
				insertFlag = 0, aFormatBusOutFoa, aFormatBusInFoa,
				aFormatBusOutSoa, aFormatBusInSoa,
				room = 0.5, damp = 0.5, zir;

				var w, x, y, z, r, s, t, u, v, p, ambSigSoa,
				w1, x1, y1, z1, r1, s1, t1, u1, v1, p1, ambSigSoa1,
				w2, x2, y2, z2, r2, s2, t2, u2, v2, p2, ambSigSoa2, ambSigSoa1plus2, ambSigFoa1plus2,
				junto, rd, dopplershift, az, ele, dis,
				junto1, azim1,
				junto2, azim2,
				omni1, spread1, diffuse1,
				omni2, spread2, diffuse2,
				globallev = 0.0001, locallev, gsig,
				aFormatFoa, aFormatSoa, ambSigFoaProcessed, ambSigSoaProcessed;
				var lrev,
				intens;
				var grevganho = 0.20;
				var soaSigLRef = Ref(0);
				var soaSigRRef = Ref(0);
				var lrev1Ref =  Ref(0);
				var lrev2Ref =  Ref(0);
				contr = Lag.kr(contr, 0.1);

				dis = 1 - radius;

				az = azim - 1.5707963267949;
				// azim1 = CircleRamp.kr(az - (angle * dis), 0.1, -pi, pi);
				// azim2 = CircleRamp.kr(az + (angle * dis), 0.1, -pi, pi);
				azim1 = az - (angle * dis);
				azim2 = az + (angle * dis);

				p = In.ar(inbus, 2);
				p = LPF.ar(p, dis * 18000 + 2000); // attenuate high freq with distance

				// ele = Lag.kr(elev, 0.1);
				ele = elev;
				dis = Select.kr(dis < 0, [dis, 0]);
				dis = Select.kr(dis > 1, [dis, 1]);

				// Doppler
				rd = dis * 340;
				rd = Lag.kr(rd, 1.0);
				dopplershift  = DelayC.ar(p, 0.2, rd/1640.0 * dopamnt);
				p = dopplershift;

				// Reverberação global
				globallev = 1 / dis.sqrt;
				intens = globallev - 1;
				intens = Select.kr(intens > 4, [intens, 4]);
				intens = Select.kr(intens < 0, [intens, 0]);
				intens = intens / 4;

				globallev = globallev - 1.0; // lower tail of curve to zero
				globallev = globallev / 3; //scale so it values 1 close to origin

				globallev = Select.kr(globallev > 1, [globallev, 1]);
				// verifica se o "sinal" está mais do que 1

				globallev = Select.kr(globallev < 0, [globallev, 0]);

				globallev = globallev * Lag.kr(glev, 0.1);

				gsig = Mix.new(p) / 2 * globallev;
				Out.ar(gbus, gsig); //send part of direct signal global reverb synth

				p1 = p[0];
				p2 = p[1];

				// Reverberação local
				locallev = dis;

				locallev = locallev  * Lag.kr(llev, 0.1);

				localReverbStereoFunc.value(lrev1Ref, lrev2Ref, p1, p2, fftsize, zir, locallev,
					room, damp);

				junto1 = (p1 + lrev1Ref.value) * 2; // match other spatializers gain
				junto2 = (p2 + lrev2Ref.value) * 2; // match other spatializers gain

				//comment out all linear parameters
				//prepareAmbSigFunc.value(soaSigLRef, junto1, azim1, el, intens: intens, dis: dis);
				//prepareAmbSigFunc.value(soaSigRRef, junto2, azim2, el, intens: intens, dis: dis);
				soaSigLRef.value = FMHEncode0.ar(junto1, azim1, ele, intens);
				soaSigRRef.value = FMHEncode0.ar(junto2, azim2, ele, intens);

				ambSigSoa1 = [soaSigLRef[0].value, soaSigLRef[1].value, soaSigLRef[2].value,
					soaSigLRef[3].value, soaSigLRef[4].value, soaSigLRef[5].value, soaSigLRef[6].value,
					soaSigLRef[7].value, soaSigLRef[8].value];

				ambSigSoa2 = [soaSigRRef[0].value, soaSigRRef[1].value, soaSigRRef[2].value,
					soaSigRRef[3].value, soaSigRRef[4].value, soaSigRRef[5].value, soaSigRRef[6].value,
					soaSigRRef[7].value, soaSigRRef[8].value];

				ambSigSoa1plus2 = ambSigSoa1 + ambSigSoa2;

				omni1 = FoaEncode.ar(junto1, foaEncoderOmni);
				spread1 = FoaEncode.ar(junto1, foaEncoderSpread);
				diffuse1 = FoaEncode.ar(junto1, foaEncoderDiffuse);
				junto1 = Select.ar(df, [omni1, diffuse1]);
				junto1 = Select.ar(sp, [junto1, spread1]);

				omni2 = FoaEncode.ar(junto2, foaEncoderOmni);
				spread2 = FoaEncode.ar(junto2, foaEncoderSpread);
				diffuse2 = FoaEncode.ar(junto2, foaEncoderDiffuse);
				junto2 = Select.ar(df, [omni2, diffuse2]);
				junto2 = Select.ar(sp, [junto2, spread2]);

				ambSigFoa1plus2 = FoaTransform.ar(junto1, 'push', 1.5707963267949 * contr,
					azim1, ele, intens) +
				FoaTransform.ar(junto2, 'push', 1.5707963267949 * contr,
					azim2, ele, intens);

				dis = dis * 5.0;
				dis = Select.kr(dis < 0.001, [dis, 0.001]);
				ambSigFoa1plus2 = HPF.ar(ambSigFoa1plus2, 20); // stops bass frequency blow outs by proximity
				ambSigFoa1plus2 = FoaTransform.ar(ambSigFoa1plus2, 'proximity', dis);

				// convert to A-format and send to a-format out busses
				aFormatFoa = FoaDecode.ar(ambSigFoa1plus2, b2a);
				Out.ar(aFormatBusOutFoa, aFormatFoa);
				aFormatSoa = AtkMatrixMix.ar(ambSigSoa1plus2, soa_a12_decoder_matrix);
				Out.ar(aFormatBusOutSoa, aFormatSoa);

				// flag switchable selector of a-format signal (from insert or not)
				aFormatFoa = Select.ar(insertFlag, [aFormatFoa, InFeedback.ar(aFormatBusInFoa, 4)]);
				aFormatSoa = Select.ar(insertFlag, [aFormatSoa, InFeedback.ar(aFormatBusInSoa, 12)]);

				// convert back to b-format
				ambSigFoaProcessed = FoaEncode.ar(aFormatFoa, a2b);
				ambSigSoaProcessed = AtkMatrixMix.ar(aFormatSoa, soa_a12_encoder_matrix);

				// not sure if the b2a/a2b process degrades signal. Just in case it does:
				ambSigFoa1plus2 = Select.ar(insertFlag, [ambSigFoa1plus2, ambSigFoaProcessed]);
				ambSigSoa1plus2 = Select.ar(insertFlag, [ambSigSoa1plus2, ambSigSoaProcessed]);

				espacAmbEstereoOutFunc.value(ambSigSoa1plus2, ambSigFoa1plus2);
			}).load(server);




			SynthDef.new("ambitoolsStereoChowning"++rev_type,  {
				arg inbus, gbus, azim = 0, elev = 0, radius = 0,
				angle = 1.05,
				dopamnt = 0, glev = 0, llev = 0,
				room = 0.5, damp = 0.5, zir;

				var sig, junto1, junto2, rd, dopplershift,
				az, azim1, azim2, ele, dis, xatras, yatras,
				globallev, locallev, gsig, intens;

				var p, p1, p2;
				var grevganho = 0.04; // needs less gain
				var lrev1Ref =  Ref(0);
				var lrev2Ref =  Ref(0);
				dis = 1 - radius;

				az = azim - 1.5707963267949;
				azim1 = CircleRamp.kr(az - (angle * dis), 0.1, -pi, pi);
				azim2 = CircleRamp.kr(az + (angle * dis), 0.1, -pi, pi);

				dis = radius;

				ele = Lag.kr(elev, 0.1);
				dis = Select.kr(dis < (radius_max * 0.05), [ dis, (radius_max * 0.05) ]);
				dis = Select.kr(dis > 1, [dis, 1]);

				p = In.ar(inbus, 2);

				// Doppler
				rd = dis * 340;
				rd = Lag.kr(rd, 1.0);
				dopplershift = DelayC.ar(p, 0.2, rd/1640.0 * dopamnt);
				p = dopplershift;

				// Global reverberation & intensity
				globallev = 1 / dis.sqrt;
				intens = globallev - 1;
				intens = Select.kr(intens > 4, [intens, 4]);
				intens = Select.kr(intens < 0, [intens, 0]);
				intens = intens / 4;

				globallev = globallev - 1.0; // lower tail of curve to zero
				globallev = globallev / 3; // scale it so that it values 1 close to origin
				globallev = Select.kr(globallev > 1, [globallev, 1]);
				globallev = Select.kr(globallev < 0, [globallev, 0]);

				globallev = globallev * Lag.kr(glev, 0.1);
				gsig = p * globallev;

				gsig = Mix.new(p) / 2 * globallev;
				Out.ar(gbus, gsig); //send part of direct signal global reverb synth

				//applie distance attenuation before mixxing in reverb to keep trail off
				p = p * (1 - dis);

				p1 = p[0];
				p2 = p[1];

				// Local reverberation
				locallev = dis;
				locallev = locallev * Lag.kr(llev, 0.1);

				localReverbStereoFunc.value(lrev1Ref, lrev2Ref, p1, p2, fftsize, zir, locallev,
					room, damp);

				junto1 = p1 + lrev1Ref.value;
				junto2 = p2 + lrev2Ref.value;

				//dis = Select.kr(dis < 0.5, [dis, 0.5]);
				sig	 = HOAEncoder.ar(this.maxorder, junto1, azim1, ele,
					plane_spherical:1, radius: VarLag.kr(dis.squared * 50)) +
				HOAEncoder.ar(this.maxorder, junto2, azim2, ele,
					plane_spherical:1, radius: VarLag.kr(dis.squared * 50));

				ambixOutFunc.value(sig);
			}).load(server);



			SynthDef.new("hoaLibStereoChowning"++rev_type,  {
				arg inbus, gbus, azim = 0, elev = 0, radius = 0,
				angle = 1.05,
				dopamnt = 0, glev = 0, llev = 0,
				room = 0.5, damp = 0.5, zir;

				var sig, junto1, junto2, rd, dopplershift,
				az, azim1, azim2, ele, dis, xatras, yatras,
				globallev, locallev, gsig, intens;

				var p, p1, p2;
				var grevganho = 0.04; // needs less gain
				var lrev1Ref =  Ref(0);
				var lrev2Ref =  Ref(0);
				dis = 1 - radius;

				az = azim - 1.5707963267949;
				azim1 = CircleRamp.kr(az - (angle * dis), 0.1, -pi, pi);
				azim2 = CircleRamp.kr(az + (angle * dis), 0.1, -pi, pi);

				p = In.ar(inbus, 2);
				p = LPF.ar(p, dis * 18000 + 2000); // attenuate high freq with distance

				dis = radius;

				ele = Lag.kr(elev, 0.1);
				dis = Select.kr(dis < 0, [dis, 0]);
				dis = Select.kr(dis > 1, [dis, 1]);

				// Doppler
				rd = dis * 340;
				rd = Lag.kr(rd, 1.0);
				dopplershift = DelayC.ar(p, 0.2, rd/1640.0 * dopamnt);
				p = dopplershift;

				// Global reverberation & intensity
				globallev = 1 / dis.sqrt;
				intens = globallev - 1;
				intens = Select.kr(intens > 4, [intens, 4]);
				intens = Select.kr(intens < 0, [intens, 0]);
				intens = intens / 4;

				globallev = globallev - 1.0; // lower tail of curve to zero
				globallev = globallev / 3; // scale it so that it values 1 close to origin
				globallev = Select.kr(globallev > 1, [globallev, 1]);
				globallev = Select.kr(globallev < 0, [globallev, 0]);

				globallev = globallev * Lag.kr(glev, 0.1);
				gsig = p * globallev;

				gsig = Mix.new(p) / 2 * globallev;
				Out.ar(gbus, gsig); //send part of direct signal global reverb synth

				//applie distance attenuation before mixxing in reverb to keep trail off
				p = p * (1 - dis).squared;

				p1 = p[0];
				p2 = p[1];

				// Local reverberation
				locallev = dis;
				locallev = locallev * Lag.kr(llev, 0.1);

				localReverbStereoFunc.value(lrev1Ref, lrev2Ref, p1, p2, fftsize, zir, locallev,
					room, damp);

				junto1 = p1 + lrev1Ref.value;
				junto2 = p2 + lrev2Ref.value;

				//dis = Select.kr(dis < 0.5, [dis, 0.5]);
				sig	 = HOALibEnc3D.ar(this.maxorder, junto1, azim1, ele, 2) +
				HOALibEnc3D.ar(this.maxorder, junto2, azim2, ele, 2);

				ambixOutFunc.value(sig);
			}).load(server);



			SynthDef.new("ambiPannerStereoChowning"++rev_type,  {
				arg inbus, gbus, azim = 0, elev = 0, radius = 0,
				angle = 1.05,
				dopamnt = 0, glev = 0, llev = 0,
				room = 0.5, damp = 0.5, zir;

				var sig, junto1, junto2, rd, dopplershift,
				az, azim1, azim2, ele, dis, xatras, yatras,
				globallev, locallev, gsig, intens;

				var p, p1, p2;
				var grevganho = 0.04; // needs less gain
				var lrev1Ref =  Ref(0);
				var lrev2Ref =  Ref(0);
				dis = 1 - radius;

				az = azim - 1.5707963267949;
				azim1 = CircleRamp.kr(az - (angle * dis), 0.1, -pi, pi);
				azim2 = CircleRamp.kr(az + (angle * dis), 0.1, -pi, pi);

				p = In.ar(inbus, 2);
				p = LPF.ar(p, dis * 18000 + 2000); // attenuate high freq with distance

				dis = radius;

				ele = Lag.kr(elev, 0.1);
				dis = Select.kr(dis < 0, [dis, 0]);
				dis = Select.kr(dis > 1, [dis, 1]);

				// Doppler
				rd = dis * 340;
				rd = Lag.kr(rd, 1.0);
				dopplershift = DelayC.ar(p, 0.2, rd/1640.0 * dopamnt);
				p = dopplershift;

				// Global reverberation & intensity
				globallev = 1 / dis.sqrt;
				intens = globallev - 1;
				intens = Select.kr(intens > 4, [intens, 4]);
				intens = Select.kr(intens < 0, [intens, 0]);
				intens = intens / 4;

				globallev = globallev - 1.0; // lower tail of curve to zero
				globallev = globallev / 3; // scale it so that it values 1 close to origin
				globallev = Select.kr(globallev > 1, [globallev, 1]);
				globallev = Select.kr(globallev < 0, [globallev, 0]);

				globallev = globallev * Lag.kr(glev, 0.1);
				gsig = p * globallev;

				gsig = Mix.new(p) / 2 * globallev;
				Out.ar(gbus, gsig); //send part of direct signal global reverb synth

				//applie distance attenuation before mixxing in reverb to keep trail off
				p = p * (1 - dis).squared;

				p1 = p[0];
				p2 = p[1];

				// Local reverberation
				locallev = dis;
				locallev = locallev * Lag.kr(llev, 0.1);

				localReverbStereoFunc.value(lrev1Ref, lrev2Ref, p1, p2, fftsize, zir, locallev,
					room, damp);

				junto1 = p1 + lrev1Ref.value;
				junto2 = p2 + lrev2Ref.value;

				//dis = Select.kr(dis < 0.5, [dis, 0.5]);
				sig	 = HOAmbiPanner.ar(this.maxorder, junto1, azim1, ele, 2) +
				HOAmbiPanner.ar(this.maxorder, junto2, azim2, ele, 2);

				ambixOutFunc.value(sig);
			}).load(server);



			SynthDef.new("VBAPStereoChowning"++rev_type,  {
				arg inbus, gbus, azim = 0, elev = 0, radius = 0,
				angle = 1.05, contr = 1,
				dopamnt = 0, glev = 0, llev = 0,
				room = 0.5, damp = 0.5, zir;

				var sig, junto1, junto2, rd, dopplershift,
				az, azim1, azim2, ele, dis, xatras, yatras,
				globallev, locallev, gsig, intens;

				var p, p1, p2;
				var grevganho = 0.04; // needs less gain
				var lrev1Ref =  Ref(0);
				var lrev2Ref =  Ref(0);
				dis = 1 - radius;

				az = azim - 1.5707963267949;
				azim1 = CircleRamp.kr(az - (angle * dis), 0.1, -pi, pi);
				azim2 = CircleRamp.kr(az + (angle * dis), 0.1, -pi, pi);
				azim1 = azim1 * 57.295779513082; // convert to degrees
				azim2 = azim2 * 57.295779513082; // convert to degrees

				p = In.ar(inbus, 2);
				p = LPF.ar(p, dis * 18000 + 2000); // attenuate high freq with distance

				dis = radius;

				ele = Lag.kr(elev, 0.1);
				dis = Select.kr(dis < 0, [dis, 0]);
				dis = Select.kr(dis > 1, [dis, 1]);

				// Doppler
				rd = dis * 340;
				rd = Lag.kr(rd, 1.0);
				dopplershift = DelayC.ar(p, 0.2, rd/1640.0 * dopamnt);
				p = dopplershift;

				// Global reverberation & intensity
				globallev = 1 / dis.sqrt;
				intens = globallev - 1;
				intens = Select.kr(intens > 4, [intens, 4]);
				intens = Select.kr(intens < 0, [intens, 0]);
				intens = intens / 4;

				globallev = globallev - 1.0; // lower tail of curve to zero
				globallev = globallev / 3; // scale it so that it values 1 close to origin
				globallev = Select.kr(globallev > 1, [globallev, 1]);
				globallev = Select.kr(globallev < 0, [globallev, 0]);

				globallev = globallev * Lag.kr(glev, 0.1);
				gsig = p * globallev;

				gsig = Mix.new(p) / 2 * globallev;
				Out.ar(gbus, gsig); //send part of direct signal global reverb synth

				//applie distance attenuation before mixxing in reverb to keep trail off
				p = p * (1 - dis).squared;

				p1 = p[0];
				p2 = p[1];

				// Local reverberation
				locallev = dis;
				locallev = locallev * Lag.kr(llev, 0.1);

				localReverbStereoFunc.value(lrev1Ref, lrev2Ref, p1, p2, fftsize, zir, locallev,
					room, damp);

				junto1 = p1 + lrev1Ref.value;
				junto2 = p2 + lrev2Ref.value;

				//dis = Select.kr(dis < 0.5, [dis, 0.5]);
				sig = VBAP.ar(numoutputs, junto1, vbap_buffer.bufnum, azim1, ele, contr) +
				VBAP.ar(numoutputs, junto2, vbap_buffer.bufnum, azim2, ele, contr);

				Out.ar(nonambibus, sig);
			}).load(server);



		}; //end makeSpatialisers



		SynthDef.new("espacAFormatVerb", {
			arg inbus, gbus, soaBus, azim = 0, elev = 0, radius = 0,
			dopamnt = 0,
			glev = 0, llev = 0, contr = 1,
			gbfbus,
			sp = 0, df = 0,
			insertFlag = 0, aFormatBusOutFoa, aFormatBusInFoa,
			aFormatBusOutSoa, aFormatBusInSoa,
			aFormatFoa, aFormatSoa, ambSigFoaProcessed, ambSigSoaProcessed;

			var p, ambSigSoa, ambSigFoa,
			junto, rd, dopplershift, az, ele, dis, xatras, yatras,
			globallev, locallev, gsig,
			intens,
			omni, spread, diffuse,
			soa_a12_sig;

			var lrev;
			var grevganho = 0.04; // needs less gain
			var ambSigRef = Ref(0);
			contr = Lag.kr(contr, 0.1);
			dis = radius;

			az = azim - 1.5707963267949;
			// az = CircleRamp.kr(az, 0.1, -pi, pi);
			// ele = Lag.kr(elev, 0.1);
			ele = elev;
			dis = Select.kr(dis < 0, [dis, 0]);
			dis = Select.kr(dis > 1, [dis, 1]);

			// high freq attenuation
			p = In.ar(inbus, 1);
			p = LPF.ar(p, (1 - dis) * 18000 + 2000); // attenuate high freq with distance
			// Doppler
			rd = dis * 340;
			rd = Lag.kr(rd, 1.0);
			dopplershift= DelayC.ar(p, 0.2, rd/1640.0 * dopamnt);
			p = dopplershift;

			// Global reverberation & intensity
			globallev = 1 / dis.sqrt;
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
			locallev = dis;
			locallev = locallev  * Lag.kr(llev, 0.1);
			junto = p * 3.5; // match other spatializers gain

			// do second order encoding
			//comment out all linear parameters
			//prepareAmbSigFunc.value(ambSigRef, junto, azim, el, intens: intens, dis: dis);
			ambSigRef.value = FMHEncode0.ar(junto, az, ele, intens);

			omni = FoaEncode.ar(junto, foaEncoderOmni);
			spread = FoaEncode.ar(junto, foaEncoderSpread);
			diffuse = FoaEncode.ar(junto, foaEncoderDiffuse);
			junto = Select.ar(df, [omni, diffuse]);
			junto = Select.ar(sp, [junto, spread]);

			ambSigFoa = FoaTransform.ar(junto, 'push', 1.5707963267949 * contr, az, ele, intens);

			ambSigSoa = [ambSigRef[0].value, ambSigRef[1].value, ambSigRef[2].value, ambSigRef[3].value,
				ambSigRef[4].value, ambSigRef[5].value, ambSigRef[6].value, ambSigRef[7].value,
				ambSigRef[8].value];

			dis = dis * 5.0;
			dis = Select.kr(dis < 0.001, [dis, 0.001]);
			ambSigFoa = HPF.ar(ambSigFoa, 20); // stops bass frequency blow outs by proximity
			ambSigFoa = FoaTransform.ar(ambSigFoa, 'proximity', dis);


			// convert to A-format and send to a-format out busses
			aFormatFoa = FoaDecode.ar(ambSigFoa, b2a);
			Out.ar(aFormatBusOutFoa, aFormatFoa);
			aFormatSoa = AtkMatrixMix.ar(ambSigSoa, soa_a12_decoder_matrix);
			Out.ar(aFormatBusOutSoa, aFormatSoa);

			// flag switchable selector of a-format signal (from insert or not)
			aFormatFoa = Select.ar(insertFlag, [aFormatFoa, InFeedback.ar(aFormatBusInFoa, 4)]);
			aFormatSoa = Select.ar(insertFlag, [aFormatSoa, InFeedback.ar(aFormatBusInSoa, 12)]);

			// convert back to b-format
			ambSigFoaProcessed = FoaEncode.ar(aFormatFoa, a2b);
			ambSigSoaProcessed = AtkMatrixMix.ar(aFormatSoa, soa_a12_encoder_matrix);

			// not sure if the b2a/a2b process degrades signal. Just in case it does:
			ambSigFoa = Select.ar(insertFlag, [ambSigFoa, ambSigFoaProcessed]);
			ambSigSoa = Select.ar(insertFlag, [ambSigSoa, ambSigSoaProcessed]);

			reverbOutFunc.value(soaBus, gbfbus, ambSigSoa, ambSigFoa, globallev, locallev);

			espacAmbOutFunc.value(ambSigSoa, ambSigFoa);
		}).load(server);




		SynthDef.new("ATK2AFormat",  {
			arg inbus, gbus, azim = 0, elev = 0, radius = 0,
			glev = 0, llev = 0.2, soaBus,
			insertFlag = 0, aFormatBusOutFoa, aFormatBusInFoa,
			aFormatBusOutSoa, aFormatBusInSoa;
			var w, x, y, z, r, s, t, u, v, p, ambSigSoa, ambSigFoa,
			aFormatFoa, aFormatSoa, ambSigFoaProcessed, ambSigSoaProcessed,
			junto, rd, dopplershift, az, ele, dis, xatras, yatras,
			globallev = 0.0004, locallev, gsig;
			var lrev, intens;
			var grevganho = 0.20;
			var ambSigRef = Ref(0);
			dis = radius;

			az = azim - 1.5707963267949;
			//az = CircleRamp.kr(az, 0.1, -pi, pi);
			//ele = Lag.kr(elev, 0.1);
			ele = elev;
			dis = Select.kr(dis < 0, [dis, 0]);
			dis = Select.kr(dis > 1, [dis, 1]);

			// high freq attenuation
			p = In.ar(inbus, 1);
			//p = LPF.ar(p, (1 - dis) * 18000 + 2000); // attenuate high freq with distance

			// Reverberação global
			globallev = 1 / dis.sqrt;
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
			locallev = dis;
			locallev = locallev * Lag.kr(llev, 0.1);

			junto = p * 3.5; // match other spatializers gain

			//comment out all linear parameters
			//prepareAmbSigFunc.value(ambSigRef, junto, azim, el, intens: intens, dis: dis);
			ambSigRef.value = FMHEncode0.ar(junto, az, ele, intens);

			ambSigFoa = [ambSigRef[0].value, ambSigRef[1].value, ambSigRef[2].value,
				ambSigRef[3].value];
			ambSigSoa = [ambSigRef[0].value, ambSigRef[1].value, ambSigRef[2].value,
				ambSigRef[3].value,
				ambSigRef[4].value, ambSigRef[5].value, ambSigRef[6].value, ambSigRef[7].value,
				ambSigRef[8].value];

			Out.ar(soaBus, (ambSigSoa*globallev) + (ambSigSoa*locallev));

			dis = dis * 5.0;
			dis = Select.kr(dis < 0.001, [dis, 0.001]);
			ambSigFoa = HPF.ar(ambSigFoa, 20); // stops bass frequency blow outs by proximity
			ambSigFoa = FoaTransform.ar(ambSigFoa, 'proximity', dis);

			// convert to A-format and send to a-format out busses
			aFormatFoa = FoaDecode.ar(ambSigFoa, b2a);
			Out.ar(aFormatBusOutFoa, aFormatFoa);
			aFormatSoa = AtkMatrixMix.ar(ambSigSoa, soa_a12_decoder_matrix);
			Out.ar(aFormatBusOutSoa, aFormatSoa);

			// flag switchable selector of a-format signal (from insert or not)
			aFormatFoa = Select.ar(insertFlag, [aFormatFoa, InFeedback.ar(aFormatBusInFoa, 4)]);
			aFormatSoa = Select.ar(insertFlag, [aFormatSoa, InFeedback.ar(aFormatBusInSoa, 12)]);

			// convert back to b-format
			ambSigFoaProcessed = FoaEncode.ar(aFormatFoa, a2b);
			ambSigSoaProcessed = AtkMatrixMix.ar(aFormatSoa, soa_a12_encoder_matrix);

			// not sure if the b2a/a2b process degrades signal. Just in case it does:
			ambSigFoa = Select.ar(insertFlag, [ambSigFoa, ambSigFoaProcessed]);
			ambSigSoa = Select.ar(insertFlag, [ambSigSoa, ambSigSoaProcessed]);

			espacAmbOutFunc.value(ambSigSoa, ambSigFoa);
		}).add;



		SynthDef.new("espacEstereoAFormat",  {
			arg inbus, gbus, soaBus, gbfbus, azim = 0, elev = 0, radius = 0,
			angle = 1.05, dopamnt = 0, sp, df,
			glev = 0, llev = 0, contr = 1,
			insertFlag = 0, aFormatBusOutFoa, aFormatBusInFoa,
			aFormatBusOutSoa, aFormatBusInSoa,
			aFormatFoa, aFormatSoa, ambSigFoaProcessed, ambSigSoaProcessed;

			var w, x, y, z, r, s, t, u, v, p, ambSigSoa,
			w1, x1, y1, z1, r1, s1, t1, u1, v1, p1, ambSigSoa1,
			w2, x2, y2, z2, r2, s2, t2, u2, v2, p2, ambSigSoa2, ambSigSoa1plus2, ambSigFoa1plus2,
			junto, rd, dopplershift, az, ele, dis,
			junto1, azim1,
			junto2, azim2,
			omni1, spread1, diffuse1,
			omni2, spread2, diffuse2,
			intens,
			globallev = 0.0001, locallev, gsig;
			var lrev;
			var grevganho = 0.20;
			var soaSigLRef = Ref(0);
			var soaSigRRef = Ref(0);
			contr = Lag.kr(contr, 0.1);

			dis = 1 - radius;

			az = azim - 1.5707963267949;
			// azim1 = CircleRamp.kr(az - (angle * dis), 0.1, -pi, pi);
			// azim2 = CircleRamp.kr(az + (angle * dis), 0.1, -pi, pi);
			azim1 = az - (angle * dis);
			azim2 = az + (angle * dis);


			p = In.ar(inbus, 2);
			p = LPF.ar(p, dis * 18000 + 2000); // attenuate high freq with distance

			dis = radius;

			// ele = Lag.kr(elev, 0.1);
			ele = elev;
			dis = Select.kr(dis < 0, [dis, 0]);
			dis = Select.kr(dis > 1, [dis, 1]);

			// Doppler
			rd = dis * 340;
			rd = Lag.kr(rd, 1.0);
			dopplershift= DelayC.ar(p, 0.2, rd/1640.0 * dopamnt);
			p = dopplershift;

			// Reverberação global
			globallev = 1 / dis.sqrt;
			intens = globallev - 1;
			intens = Select.kr(intens > 4, [intens, 4]);
			intens = Select.kr(intens < 0, [intens, 0]);
			intens = intens / 4;

			globallev = globallev - 1.0; // lower tail of curve to zero
			globallev = Select.kr(globallev > 1, [globallev, 1]);
			// verifica se o "sinal" está mais do que 1

			globallev = Select.kr(globallev < 0, [globallev, 0]);

			globallev = globallev * Lag.kr(glev, 0.1);

			p1 = p[0];
			p2 = p[1];

			// Reverberação local
			locallev = dis;

			locallev = locallev  * Lag.kr(llev, 0.1);

			junto1 = p1 * 2; // match other spatializers gain
			junto2 = p2 * 2; // match other spatializers gain

			//comment out all linear parameters
			//prepareAmbSigFunc.value(soaSigLRef, junto1, azim1, el, intens: intens, dis: dis);
			//prepareAmbSigFunc.value(soaSigRRef, junto2, azim2, el, intens: intens, dis: dis);
			soaSigLRef.value = FMHEncode0.ar(junto1, azim1, ele, intens);
			soaSigRRef.value = FMHEncode0.ar(junto2, azim2, ele, intens);

			ambSigSoa1 = [soaSigLRef[0].value, soaSigLRef[1].value, soaSigLRef[2].value,
				soaSigLRef[3].value, soaSigLRef[4].value, soaSigLRef[5].value, soaSigLRef[6].value,
				soaSigLRef[7].value, soaSigLRef[8].value];

			ambSigSoa2 = [soaSigRRef[0].value, soaSigRRef[1].value, soaSigRRef[2].value,
				soaSigRRef[3].value, soaSigRRef[4].value, soaSigRRef[5].value, soaSigRRef[6].value,
				soaSigRRef[7].value, soaSigRRef[8].value];

			omni1 = FoaEncode.ar(junto1, foaEncoderOmni);
			spread1 = FoaEncode.ar(junto1, foaEncoderSpread);
			diffuse1 = FoaEncode.ar(junto1, foaEncoderDiffuse);
			junto1 = Select.ar(df, [omni1, diffuse1]);
			junto1 = Select.ar(sp, [junto1, spread1]);

			omni2 = FoaEncode.ar(junto2, foaEncoderOmni);
			spread2 = FoaEncode.ar(junto2, foaEncoderSpread);
			diffuse2 = FoaEncode.ar(junto2, foaEncoderDiffuse);
			junto2 = Select.ar(df, [omni2, diffuse2]);
			junto2 = Select.ar(sp, [junto2, spread2]);

			ambSigFoa1plus2 = FoaTransform.ar(junto1, 'push', 1.5707963267949 * contr, azim1,
				ele, intens) +
			FoaTransform.ar(junto2, 'push', 1.5707963267949 * contr, azim2, ele, intens);

			ambSigSoa1plus2 = ambSigSoa1 + ambSigSoa2;

			dis = dis * 5.0;
			dis = Select.kr(dis < 0.001, [dis, 0.001]);
			ambSigFoa1plus2 = HPF.ar(ambSigFoa1plus2, 20); // stops bass frequency blow outs by proximity
			ambSigFoa1plus2 = FoaTransform.ar(ambSigFoa1plus2, 'proximity', dis);

			// convert to A-format and send to a-format out busses
			aFormatFoa = FoaDecode.ar(ambSigFoa1plus2, b2a);
			Out.ar(aFormatBusOutFoa, aFormatFoa);
			aFormatSoa = AtkMatrixMix.ar(ambSigSoa1plus2, soa_a12_decoder_matrix);
			Out.ar(aFormatBusOutSoa, aFormatSoa);

			// flag switchable selector of a-format signal (from insert or not)
			aFormatFoa = Select.ar(insertFlag, [aFormatFoa, InFeedback.ar(aFormatBusInFoa, 4)]);
			aFormatSoa = Select.ar(insertFlag, [aFormatSoa, InFeedback.ar(aFormatBusInSoa, 12)]);

			// convert back to b-format
			ambSigFoaProcessed  = FoaEncode.ar(aFormatFoa, a2b);
			ambSigSoaProcessed = AtkMatrixMix.ar(aFormatSoa, soa_a12_encoder_matrix);

			// not sure if the b2a/a2b process degrades signal. Just in case it does:
			ambSigFoa1plus2 = Select.ar(insertFlag, [ambSigFoa1plus2, ambSigFoaProcessed]);
			ambSigSoa1plus2 = Select.ar(insertFlag, [ambSigSoa1plus2, ambSigSoaProcessed]);

			reverbOutFunc.value(soaBus, gbfbus, ambSigSoa1plus2, ambSigFoa1plus2, globallev, locallev);

			espacAmbEstereoOutFunc.value(ambSigSoa1plus2, ambSigFoa1plus2);
		}).load(server);


		rirList = Array.newClear();

		if (rirBank.notNil) {

			var rirName, rirW, rirX, rirY, rirZ, bufWXYZ, rirFLU, rirFRD, rirBLD, rirBRU,
			bufAformat, bufAformat_soa_a12, rirA12, bufsize,
			// prepare list of impulse responses for close and distant reverb selection menue

			rirPath = PathName(rirBank),
			rirNum = 0; // initialize

			rirW = [];
			rirX = [];
			rirY = [];
			rirZ = [];
			bufWXYZ = [];

			rirPath.entries.do({ |item, count|

				if (item.extension == "amb") {
					rirNum = rirNum + 1;

					rirName = item.fileNameWithoutExtension;
					rirList = rirList ++ [rirName];

					rirW = rirW ++ [ Buffer.readChannel(server, item.fullPath, channels: [0]) ];
					rirX = rirX ++ [ Buffer.readChannel(server, item.fullPath, channels: [1]) ];
					rirY = rirY ++ [ Buffer.readChannel(server, item.fullPath, channels: [2]) ];
					rirZ = rirZ ++ [ Buffer.readChannel(server, item.fullPath, channels: [3]) ];

					bufWXYZ = bufWXYZ ++ [ Buffer.read(server, item.fullPath) ];
				};

			});


			bufAformat = Array.newClear(rirNum);
			bufAformat_soa_a12 = Array.newClear(rirNum);
			rirFLU = Array.newClear(rirNum);
			rirFRD = Array.newClear(rirNum);
			rirBLD = Array.newClear(rirNum);
			rirBRU = Array.newClear(rirNum);
			bufsize = Array.newClear(rirNum);

			rirWspectrum = Array.newClear(rirNum);
			rirXspectrum = Array.newClear(rirNum);
			rirYspectrum = Array.newClear(rirNum);
			rirZspectrum = Array.newClear(rirNum);

			rirFLUspectrum = Array.newClear(rirNum);
			rirFRDspectrum = Array.newClear(rirNum);
			rirBLDspectrum = Array.newClear(rirNum);
			rirBRUspectrum = Array.newClear(rirNum);

			server.sync;

			rirList.do({ |item, count|

				bufsize[count] = PartConv.calcBufSize(fftsize, rirW[count]);

				bufAformat[count] = Buffer.alloc(server, bufWXYZ[count].numFrames, bufWXYZ[count].numChannels);
				bufAformat_soa_a12[count] = Buffer.alloc(server, bufWXYZ[count].numFrames, 12);
				// for second order conv

				//server.sync;

				if (File.exists(rirBank ++ "/" ++ item ++ "_Flu.wav").not) {

					("writing " ++ item ++ "_Flu.wav file in" ++ rirBank).postln;

					{BufWr.ar(FoaDecode.ar(PlayBuf.ar(4, bufWXYZ[count], loop: 0, doneAction: 2), b2a),
						bufAformat[count], Phasor.ar(0, BufRateScale.kr(bufAformat[count]),
							0, BufFrames.kr(bufAformat[count])));
						Out.ar(0, Silent.ar);
					}.play;

					(bufAformat[count].numFrames / server.sampleRate).wait;

					bufAformat[count].write(rirBank ++ "/" ++ item ++ "_Flu.wav",
						headerFormat: "wav", sampleFormat: "int24");

					"done".postln;

				};


				if (File.exists(rirBank ++ "/" ++ item ++ "_SoaA12.wav").not) {

					("writing " ++ item ++ "_SoaA12.wav file in " ++ rirBank).postln;

					{BufWr.ar(AtkMatrixMix.ar(PlayBuf.ar(4, bufWXYZ[count], loop: 0, doneAction: 2),
						foa_a12_decoder_matrix),
					bufAformat_soa_a12[count],
					Phasor.ar(0, BufRateScale.kr(bufAformat[count]), 0, BufFrames.kr(bufAformat[count])));
					Out.ar(0, Silent.ar);
					}.play;

					(bufAformat[count].numFrames / server.sampleRate).wait;

					bufAformat_soa_a12[count].write(rirBank ++ "/" ++ item ++ "_SoaA12.wav",
						headerFormat: "wav", sampleFormat: "int24");

					"done".postln;

				};

			});

			server.sync;

			"Loading rir bank".postln;

			rirList.do({ |item, count|

				rirFLU[count] = Buffer.readChannel(server, rirBank ++ "/" ++ item ++ "_Flu.wav",
					channels: [0]);
				rirFRD[count] = Buffer.readChannel(server, rirBank ++ "/" ++ item ++ "_Flu.wav",
					channels: [1]);
				rirBLD[count] = Buffer.readChannel(server, rirBank ++ "/" ++ item ++ "_Flu.wav",
					channels: [2]);
				rirBRU[count] = Buffer.readChannel(server, rirBank ++ "/" ++ item ++ "_Flu.wav",
					channels: [3]);

				//server.sync;

				rirWspectrum[count] = Buffer.alloc(server, bufsize[count], 1);
				rirXspectrum[count] = Buffer.alloc(server, bufsize[count], 1);
				rirYspectrum[count] = Buffer.alloc(server, bufsize[count], 1);
				rirZspectrum[count] = Buffer.alloc(server, bufsize[count], 1);
				server.sync;
				rirWspectrum[count].preparePartConv(rirW[count], fftsize);
				//server.sync;
				rirXspectrum[count].preparePartConv(rirX[count], fftsize);
				//server.sync;
				rirYspectrum[count].preparePartConv(rirY[count], fftsize);
				//server.sync;
				rirZspectrum[count].preparePartConv(rirZ[count], fftsize);

				//server.sync;

				rirFLUspectrum[count] = Buffer.alloc(server, bufsize[count], 1);
				rirFRDspectrum[count] = Buffer.alloc(server, bufsize[count], 1);
				rirBLDspectrum[count] = Buffer.alloc(server, bufsize[count], 1);
				rirBRUspectrum[count] = Buffer.alloc(server, bufsize[count], 1);
				//server.sync;
				rirFLUspectrum[count].preparePartConv(rirFLU[count], fftsize);
				//server.sync;
				rirFRDspectrum[count].preparePartConv(rirFRD[count], fftsize);
				//server.sync;
				rirBLDspectrum[count].preparePartConv(rirBLD[count], fftsize);
				//server.sync;
				rirBRUspectrum[count].preparePartConv(rirBRU[count], fftsize);

				//server.sync;

				rirA12 = Array.newClear(12);
				rirA12Spectrum = Array2D(rirNum, 12);
				12.do { arg i;
					rirA12[i] = Buffer.readChannel(server, rirBank ++ "/" ++ item ++ "_SoaA12.wav",
						channels: [i]);
					server.sync;
					rirA12Spectrum[count, i] = Buffer.alloc(server, bufsize[count], 1);
					//server.sync;
					rirA12Spectrum[count, i].preparePartConv(rirA12[i], fftsize);
					//server.sync;
					rirA12[i].free;
				};

				rirW[count].free; // don't need time domain data anymore, just needed spectral version
				rirX[count].free;
				rirY[count].free;
				rirZ[count].free;
				rirFLU[count].free;
				rirFRD[count].free;
				rirBLD[count].free;
				rirBRU[count].free;
				bufAformat[count].free;
				bufWXYZ[count].free;

				(rirNum - count).postln;

			});


			//server.sync;


			/// START SYNTH DEFS ///////



			SynthDef.new("revGlobalAmb_conv",  { arg gbus, gate = 1, wir, xir, yir, zir;
				var env, sig, convsig;
				env = EnvGen.kr(Env.asr(1), gate, doneAction:2);
				sig = In.ar(gbus, 1);
				convsig = [
					PartConv.ar(sig, fftsize, wir),
					PartConv.ar(sig, fftsize, xir),
					PartConv.ar(sig, fftsize, yir),
					PartConv.ar(sig, fftsize, zir)
				];
				convsig = convsig * env;
				revGlobalAmbFunc.value(convsig);
			}).add;


			SynthDef.new("revGlobalBFormatAmb_conv",  { arg gbfbus, gate = 1, fluir, frdir, bldir, bruir;
				var env, convsig, sig = In.ar(gbfbus, 4);
				env = EnvGen.kr(Env.asr(1), gate, doneAction:2);
				sig = FoaDecode.ar(sig, b2a);
				convsig = [
					PartConv.ar(sig[0], fftsize, fluir),
					PartConv.ar(sig[1], fftsize, frdir),
					PartConv.ar(sig[2], fftsize, bldir),
					PartConv.ar(sig[3], fftsize, bruir)
				];
				convsig = FoaEncode.ar(convsig, a2b);
				convsig = convsig * env;
				revGlobalAmbFunc.value(convsig);
			}).add;

			if (this.maxorder > 1) {

				SynthDef.new("revGlobalSoaA12_conv",  { arg soaBus, gate = 1, a0ir, a1ir, a2ir, a3ir,
					a4ir, a5ir, a6ir, a7ir, a8ir, a9ir, a10ir, a11ir;
					var env, w, x, y, z, r, s, t, u, v,
					foaSig, soaSig, tmpsig;
					var sig = In.ar(soaBus, 9);
					env = EnvGen.kr(Env.asr(1), gate, doneAction:2);
					sig = AtkMatrixMix.ar(sig, soa_a12_decoder_matrix);
					tmpsig = [
						PartConv.ar(sig[0], fftsize, a0ir),
						PartConv.ar(sig[1], fftsize, a1ir),
						PartConv.ar(sig[2], fftsize, a2ir),
						PartConv.ar(sig[3], fftsize, a3ir),
						PartConv.ar(sig[4], fftsize, a4ir),
						PartConv.ar(sig[5], fftsize, a5ir),
						PartConv.ar(sig[6], fftsize, a6ir),
						PartConv.ar(sig[7], fftsize, a7ir),
						PartConv.ar(sig[8], fftsize, a8ir),
						PartConv.ar(sig[9], fftsize, a9ir),
						PartConv.ar(sig[10], fftsize, a10ir),
						PartConv.ar(sig[11], fftsize, a11ir),
					];

					tmpsig = tmpsig * 4 * env;
					#w, x, y, z, r, s, t, u, v = AtkMatrixMix.ar(tmpsig, soa_a12_encoder_matrix);
					foaSig = [w, x, y, z] ;
					soaSig = [w, x, y, z, r, s, t, u, v];
					revGlobalSoaOutFunc.value(soaSig, foaSig);
				}).add;

			};


			//run the makeSpatialisers function for each types of local reverbs


			localReverbFunc = { | lrevRef, p, fftsize, wir, locallev, room, damp |
				lrevRef.value = PartConv.ar(p, fftsize, wir, locallev);
			};

			localReverbStereoFunc = { | lrev1Ref, lrev2Ref, p1, p2, fftsize, zir, locallev,
				room, damp |
				var temp1 = p1, temp2 = p2;
				temp1 = PartConv.ar(p1, fftsize, zir, locallev);
				temp2 = PartConv.ar(p2, fftsize, zir, locallev);
				lrev1Ref.value = temp1 * locallev;
				lrev2Ref.value = temp2 * locallev;
			};


			makeSpatialisers.value(rev_type:"_conv");


		} {

			// empty arrays for SynthDefs, in case no rir banks are provided
			rirFLUspectrum = Array.newClear(3);
			rirFRDspectrum = Array.newClear(3);
			rirBLDspectrum = Array.newClear(3);
			rirBRUspectrum = Array.newClear(3);

			rirA12Spectrum = Array2D(3, 12);

			rirWspectrum = Array.newClear(4);
			rirXspectrum = Array.newClear(4);
			rirYspectrum = Array.newClear(4);
			rirZspectrum = Array.newClear(4);

		};


		// allpass reverbs


		SynthDef.new("revGlobalBFormatAmb_pass",  { arg gbfbus, gate = 1, room = 0.5, damp = 0.5;
			var env, temp, sig = In.ar(gbfbus, 4);
			env = EnvGen.kr(Env.asr(1), gate, doneAction:2);
			sig = FoaDecode.ar(sig, b2a);
			16.do({ sig = AllpassC.ar(sig, 0.08, room * { Rand(0, 0.08) }.dup(4) + { Rand(0, 0.001) },
				damp * 2)});
			sig = FoaEncode.ar(sig, a2b);
			sig = sig * env;
			revGlobalAmbFunc.value(sig);
		}).add;

		if (this.maxorder > 1) {

			SynthDef.new("revGlobalSoaA12_pass",  { arg soaBus, gate = 1, room = 0.5, damp = 0.5;
				var env, w, x, y, z, r, s, t, u, v,
				foaSig, soaSig, tmpsig;
				var sig = In.ar(soaBus, 9);
				env = EnvGen.kr(Env.asr, gate, doneAction:2);
				sig = AtkMatrixMix.ar(sig, soa_a12_decoder_matrix);
				16.do({ sig = AllpassC.ar(sig, 0.08, room * { Rand(0, 0.08) }.dup(12) + { Rand(0, 0.001) },
					damp * 2)});
				#w, x, y, z, r, s, t, u, v = AtkMatrixMix.ar(sig, soa_a12_encoder_matrix) * env;
				foaSig = [w, x, y, z];
				soaSig = [w, x, y, z, r, s, t, u, v];
				revGlobalSoaOutFunc.value(soaSig, foaSig);
			}).load(server);

		};

		//run the makeSpatialisers function for each types of local reverbs

		SynthDef.new("revGlobalAmb_pass",  { arg gbus, gate = 1, room = 0.5, damp = 0.5;
			var env, sig = In.ar(gbus, 1);
			env = EnvGen.kr(Env.asr(1), gate, doneAction:2);
			16.do({ sig = AllpassC.ar(sig, 0.08, room * { Rand(0, 0.08) }.dup(4) + { Rand(0, 0.001) },
				damp * 2)});
			sig = sig / 4; // running too hot, so attenuate
			sig = sig * env;
			sig = FoaEncode.ar(sig, a2b);
			revGlobalAmbFunc.value(sig);
		}).add;

		localReverbFunc = { | lrevRef, p, fftsize, rirWspectrum, locallev, room, damp |
			var temp;
			temp = p;
			16.do({ temp = AllpassC.ar(temp, 0.08, room * { Rand(0, 0.08) } + { Rand(0, 0.001) },
				damp * 2)});
			lrevRef.value = temp * locallev;
		};

		localReverbStereoFunc = { | lrev1Ref, lrev2Ref, p1, p2, fftsize, rirZspectrum, locallev, room, damp |
			var temp1 = p1, temp2 = p2;
			16.do({ temp1 = AllpassC.ar(temp1, 0.08, room * { Rand(0, 0.08) } + { Rand(0, 0.001) },
				damp * 2)});
			16.do({ temp2 = AllpassC.ar(temp2, 0.08, room * { Rand(0, 0.08) } + { Rand(0, 0.001) },
				damp * 2)});
			lrev1Ref.value = temp1 * locallev;
			lrev2Ref.value = temp2 * locallev;
		};

		makeSpatialisers.value(rev_type:"_pass");


		// freeverb defs


		SynthDef.new("revGlobalBFormatAmb_free",  { arg gbfbus, gate = 1, room = 0.5, damp = 0.5;
			var env, convsig, sig = In.ar(gbfbus, 4);
			env = EnvGen.kr(Env.asr, gate, doneAction:2);
			sig = FoaDecode.ar(sig, b2a);
			convsig = [
				FreeVerb.ar(sig[0], mix: 1, room: room, damp: damp),
				FreeVerb.ar(sig[1], mix: 1, room: room, damp: damp),
				FreeVerb.ar(sig[2], mix: 1, room: room, damp: damp),
				FreeVerb.ar(sig[3], mix: 1, room: room, damp: damp)];
			convsig = FoaEncode.ar(convsig, a2b);
			convsig = convsig * env;
			revGlobalAmbFunc.value(convsig);
		}).add;

		if (this.maxorder > 1) {

			SynthDef.new("revGlobalSoaA12_free",  { arg soaBus, gate = 1, room = 0.5, damp = 0.5;
				var env, w, x, y, z, r, s, t, u, v,
				foaSig, soaSig, tmpsig;
				var sig = In.ar(soaBus, 9);
				env = EnvGen.kr(Env.asr(1), gate, doneAction:2);
				sig = AtkMatrixMix.ar(sig, soa_a12_decoder_matrix);
				tmpsig = [
					FreeVerb.ar(sig[0], mix: 1, room: room, damp: damp),
					FreeVerb.ar(sig[1], mix: 1, room: room, damp: damp),
					FreeVerb.ar(sig[2], mix: 1, room: room, damp: damp),
					FreeVerb.ar(sig[3], mix: 1, room: room, damp: damp),
					FreeVerb.ar(sig[4], mix: 1, room: room, damp: damp),
					FreeVerb.ar(sig[5], mix: 1, room: room, damp: damp),
					FreeVerb.ar(sig[6], mix: 1, room: room, damp: damp),
					FreeVerb.ar(sig[7], mix: 1, room: room, damp: damp),
					FreeVerb.ar(sig[8], mix: 1, room: room, damp: damp),
					FreeVerb.ar(sig[9], mix: 1, room: room, damp: damp),
					FreeVerb.ar(sig[10], mix: 1, room: room, damp: damp),
					FreeVerb.ar(sig[11], mix: 1, room: room, damp: damp)];

				tmpsig = tmpsig * 4 * env;
				#w, x, y, z, r, s, t, u, v = AtkMatrixMix.ar(tmpsig, soa_a12_encoder_matrix);
				foaSig = [w, x, y, z];
				soaSig = [w, x, y, z, r, s, t, u, v];
				revGlobalSoaOutFunc.value(soaSig, foaSig);
			}).add;

		};


		//run the makeSpatialisers function for each types of local reverbs


		SynthDef.new("revGlobalAmb_free",  { arg gbus, gate = 1, room = 0.5, damp = 0.5;
			var env, sig, convsig;
			env = EnvGen.kr(Env.asr(1), gate, doneAction:2);
			sig = In.ar(gbus, 1);
			convsig = [
				FreeVerb.ar(sig, mix: 1, room: room, damp: damp),
				FreeVerb.ar(sig, mix: 1, room: room, damp: damp),
				FreeVerb.ar(sig, mix: 1, room: room, damp: damp),
				FreeVerb.ar(sig, mix: 1, room: room, damp: damp)];
			convsig = FoaEncode.ar(convsig, a2b);
			convsig = convsig * env;
			revGlobalAmbFunc.value(convsig);
		}).add;


		localReverbFunc = { | lrevRef, p, fftsize, rirWspectrum, locallev, room = 0.5, damp = 0.5 |
			lrevRef.value = FreeVerb.ar(p * locallev, mix: 1, room: room, damp: damp);
		};

		localReverbStereoFunc = { | lrev1Ref, lrev2Ref, p1, p2, fftsize, rirZspectrum, locallev,
			room = 0.5, damp = 0.5|
			var temp1 = p1, temp2 = p2;
			temp1 = FreeVerb.ar(p1 * locallev, mix: 1, room: room, damp: damp);
			temp2 = FreeVerb.ar(p2 * locallev, mix: 1, room: room, damp: damp);
			lrev1Ref.value = temp1 * locallev;
			lrev2Ref.value = temp2 * locallev;

		};

		makeSpatialisers.value(rev_type:"_free");


		// function for no-reverb option

		localReverbFunc = { | lrevRef, p, fftsize, rirWspectrum, locallev, room, damp|
		};

		localReverbStereoFunc = { | lrev1Ref, lrev2Ref, p1, p2, fftsize, rirZspectrum, locallev, room, damp |
		};

		makeSpatialisers.value(rev_type:"");


		//comment out all linear parameters

/*
		prepareAmbSigFunc = { |ambSigRef, junto, azim, el, intens, dis|
			ambSigRef.value = FMHEncode0.ar(junto, azim, el, intens);
		};
		makeSpatialisers.value(linear: false);

		prepareAmbSigFunc = { |ambSigRef, junto, azim, el, intens, dis|
			ambSigRef.value = FMHEncode0.ar(junto, azim, el, dis);
		};
		makeSpatialisers.value(linear: true);
*/

		makeSynthDefPlayers = { arg type, i = 0;
		// 3 types : File, HWBus and SWBus - i duplicates with 0, 1 & 2

			SynthDef.new("playMono"++type, { arg outbus, bufnum = 0, rate = 1,
				level = 0, tpos = 0, lp = 0, busini;
				var scaledRate, spos, playerRef;
				//SendTrig.kr(Impulse.kr(1), 101,  tpos); // debugging
				playerRef = Ref(0);
				playMonoInFunc[i].value(playerRef, busini, bufnum, scaledRate, tpos, spos, lp, rate);

				Out.ar(outbus, playerRef.value * Lag.kr(level, 0.1));
			}).add;

			SynthDef.new("playStereo"++type, { arg outbus, bufnum = 0, rate = 1,
				level = 0, tpos = 0, lp = 0, busini;
				var scaledRate, spos, playerRef;
				playerRef = Ref(0);
				playStereoInFunc[i].value(playerRef, busini, bufnum, scaledRate, tpos, spos, lp, rate);
				Out.ar(outbus, playerRef.value * Lag.kr(level, 0.1));
			}).add;


			SynthDef.new("playBFormat"++type, { arg outbus, bufnum = 0, rate = 1,
				level = 0, tpos = 0, lp = 0, rotAngle = 0, tilAngle = 0, tumAngle = 0,
				azim = 0, elev = 0, radius = 0,
				gbus, gbfbus, glev, llev, directang = 0, contr, dopamnt,
				busini,
				insertFlag = 0, aFormatBusOutFoa, aFormatBusInFoa,
				aFormatBusOutSoa, aFormatBusInSoa;

				var scaledRate, playerRef, wsinal, spos, pushang = 0,
				aFormatFoa, aFormatSoa, ambSigFoaProcessed, ambSigSoaProcessed,

				az, ele, dis, globallev, locallev,
				gsig, lsig, rd, dopplershift,
				intens;
				var grevganho = 0.20;
				dis = radius;

				az = azim - 1.5707963267949;
				// az = CircleRamp.kr(az, 0.1, -pi, pi);
				// ele = Lag.kr(elev, 0.1);
				ele = elev;
				pushang = dis * 1.5707963267949; // degree of sound field displacement
				dis = Select.kr(dis < 0, [dis, 0]);
				dis = Select.kr(dis > 1, [dis, 1]);
				playerRef = Ref(0);
				playBFormatInFunc[i].value(playerRef, busini, bufnum, scaledRate, tpos, spos, lp, rate);

				rd = dis * 340;
				rd = Lag.kr(rd, 1.0);
				dopplershift= DelayC.ar(playerRef.value, 0.2, rd/1640.0 * dopamnt);
				playerRef.value = dopplershift;

				wsinal = playerRef.value[0] * contr * Lag.kr(level, 0.1) * dis * 2.0;

				Out.ar(outbus, wsinal);

				// global reverb
				globallev = 1 / dis.sqrt;
				intens = globallev - 1;
				intens = Select.kr(intens > 4, [intens, 4]);
				intens = Select.kr(intens < 0, [intens, 0]);
				intens = intens / 4;

				playerRef.value = FoaDirectO.ar(playerRef.value, directang); // directivity

				//comment out all linear parameters
				//prepareRotateFunc.value(dis, intens, playerRef, contr, rotAngle, Lag.kr(level, 0.1));
				playerRef.value = FoaTransform.ar(playerRef.value, 'rotate', rotAngle,
							Lag.kr(level, 0.1) * intens * (1 - contr));

				playerRef.value = FoaTransform.ar(playerRef.value, 'push', pushang, az, ele);

				// convert to A-format and send to a-format out busses
				aFormatFoa = FoaDecode.ar(playerRef.value, b2a);
				Out.ar(aFormatBusOutFoa, aFormatFoa);
				// aFormatSoa = AtkMatrixMix.ar(ambSigSoa, soa_a12_decoder_matrix);
				// Out.ar(aFormatBusOutSoa, aFormatSoa);

				// flag switchable selector of a-format signal (from insert or not)
				aFormatFoa = Select.ar(insertFlag, [aFormatFoa, InFeedback.ar(aFormatBusInFoa, 4)]);
				//aFormatSoa = Select.ar(insertFlag, [aFormatSoa, InFeedback.ar(aFormatBusInSoa, 12)]);

				// convert back to b-format
				ambSigFoaProcessed = FoaEncode.ar(aFormatFoa, a2b);
				//ambSigSoaProcessed = AtkMatrixMix.ar(aFormatSoa, soa_a12_encoder_matrix);

				// not sure if the b2a/a2b process degrades signal. Just in case it does:
				playerRef.value = Select.ar(insertFlag, [playerRef.value, ambSigFoaProcessed]);
				//ambSigSoa = Select.ar(insertFlag, [ambSigSoa, ambSigSoaProcessed]);

				playBFormatOutFunc.value(playerRef.value);

				globallev = globallev - 1.0; // lower tail of curve to zero
				globallev = Select.kr(globallev > 1, [globallev, 1]);
				globallev = Select.kr(globallev < 0, [globallev, 0]);
				globallev = globallev * Lag.kr(glev, 0.1) * 6;

				gsig = playerRef.value[0] * globallev;

				locallev = dis;

				locallev = locallev  * Lag.kr(llev, 0.1) * 5;
				lsig = playerRef.value[0] * locallev;

				gsig = (playerRef.value * globallev) + (playerRef.value * locallev); // b-format
				Out.ar(gbfbus, gsig);

			}).add;

			//comment out all linear parameters

/*			2.do {   // make linear and non-linear versions
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
					mx = 0, my = 0, mz = 0, gbus, gbfbus, glev, llev, directang = 0, contr, dopamnt,
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
					dopplershift= DelayC.ar(playerRef.value, 0.2, rd/1640.0 * dopamnt);
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
					playerRef.value = FoaDirectO.ar(playerRef.value, directang); // directivity


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
*/
		}; //end makeSynthDefPlayers

		// Make File-in SynthDefs

		playMonoInFunc[0] = {
			arg playerRef, busini, bufnum, scaledRate, tpos, spos, lp = 0, rate;
			// Note it needs all the variables

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
			arg playerRef, busini, bufnum, scaledRate, tpos, spos, lp = 0, rate;
			// Note it needs all the variables

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

		///// launch GUI was here
		gbus = Bus.audio(server, 1); // global reverb bus
		gbfbus = Bus.audio(server, 4); // global b-format bus
		soaBus = Bus.audio(server, 9);

		updateSynthInArgs = { arg source;
			{
				server.sync;
				this.setSynths(source, \angle, angle[source]);
				this.setSynths(source, \level, level[source]);
				this.setSynths(source, \dopamnt, dplev[source]);
				this.setSynths(source, \glev, glev[source]);
				this.setSynths(source, \llev, llev[source]);
				this.setSynths(source, \rm, rm[source]);
				this.setSynths(source, \dm, dm[source]);
				this.setSynths(source, \azim, this.spheval[source].theta);
				this.setSynths(source, \elev, this.spheval[source].phi);
				this.setSynths(source, \radius, this.spheval[source].rho);

				//	this.setSynths(source, \sp, sp[source]);
				//	this.setSynths(source, \df, df[source]);

				this.setSynths(source, \rotAngle, rlev[source]);
				this.setSynths(source, \directang, dlev[source]);
				this.setSynths(source, \contr, clev[source]);

				this.setSynths(source, \aux1, aux1[source]);
				this.setSynths(source, \aux2, aux2[source]);
				this.setSynths(source, \aux3, aux3[source]);
				this.setSynths(source, \aux4, aux4[source]);
				this.setSynths(source, \aux5, aux5[source]);

				this.setSynths(source, \a1check, this.a1but[source]);
				this.setSynths(source, \a2check, this.a2but[source]);
				this.setSynths(source, \a3check, this.a3but[source]);
				this.setSynths(source, \a4check, this.a4but[source]);
				this.setSynths(source, \a5check, this.a5but[source]);


			}.fork;
		};

		atualizarvariaveis = {


			this.nfontes.do { arg i;
				//	updateSynthInArgs.value(i);

				if(this.espacializador[i] != nil) {
					this.espacializador[i].set(
						//	\mx, num.value  ???
						\angle, angle[i],
						\level, level[i], // ? or in player?
						\dopamnt, dplev[i],
						\glev, glev[i],
						\llev, llev[i],
						\rm, rm[i],
						\dm, dm[i],
						//\mx, xbox[i].value,
						//\my, ybox[i].value,
						//\mz, zbox[i].value,
						\azim, this.spheval[i].theta,
						\elev, this.spheval[i].phi,
						\radius, this.spheval[i].rho,
						//\xoffset, this.xoffset[i],
						//\yoffset, this.yoffset[i],
						\sp, sp[i],
						\df, df[i];
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
						\rm, rm[i],
						\dm, dm[i],
						//\mx, xbox[i].value,
						//\my, ybox[i].value,

						// ERROR HERE?
						//						\mz, zbox[i].value,
						\azim, this.spheval[i].theta,
						\elev, this.spheval[i].phi,
						\radius, this.spheval[i].rho,
						//\xoffset, this.xoffset[i],
						//\yoffset, this.yoffset[i],
						//\mz, this.zval[i].value,
						\sp, sp[i],
						\df, df[i];
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
					\angle, angle[source],
					\level, level[source], // ? or in player?
					\dopamnt, dplev[source],
					\glev, glev[source],
					\llev, llev[source],
					\rm, rm[source],
					\dm, dm[source],
					\azim, this.spheval[source].theta,
					\elev, this.spheval[source].phi,
					\radius, this.spheval[source].rho,
					\sp, sp[source],
					\df, df[source];
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
					\rm, rm[source],
					\dm, dm[source],
					\azim, this.spheval[source].theta,
					\elev, this.spheval[source].phi,
					\radius, this.spheval[source].rho,
					\sp, sp[source],
					\df, df[source];
				);
			};
		};



		// this regulates file playing synths
		this.watcher = Routine.new({
			"WATCHER!!!".postln;
			inf.do({
				0.1.wait;

				this.nfontes.do({
					arg i;
						{
						//("scn = " ++ scn[i]).postln;
						if ((this.tfieldProxy[i].value != "") || ((scn[i] > 0) && (this.ncan[i] > 0))
							|| (this.hwncheckProxy[i].value && (this.ncan[i] > 0)) ) {
							//var source = Point.new;  // should use cartesian but it's giving problems
							//source.set(this.xval[i] + this.xoffset[i], this.yval[i] + this.yoffset[i]);
							//source.set(this.cartval[i].x, this.cartval[i].y);
							//("testado = " ++ testado[i]).postln;
							//("distance " ++ i ++ " = " ++ source.rho).postln;
							if (this.cartval[i].rho > this.plim) {
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
										&& (this.firstTime[i] || (this.tfieldProxy[i].value == ""))) {
											//this.triggerFunc[i].value; // play SC input synth
											this.firstTime[i] = false;
											runTrigger.value(i);

											if(lp[i] == 0) {

												//tocar.value(i, 1, force: true);
												this.newtocar(i, 0, force: true);
											} {   // could remake this a random start point in future
												//tocar.value(i, 1, force: true);
												this.newtocar(i, 0, force: true);
											};
										};

								};
							};
						}.defer;   // CHECK THIS DEFER
				});

				if(this.guiflag.not) {
					// when there is no gui, Automation callback does not work,
					// so here we monitor when the transport reaches end

					if (this.control.now > this.dur) {
						if (this.autoloopval) {
							this.control.seek; // note, onSeek not called
						} {
							this.blindControlStop; // stop everything
						};
					};
				};


			});
		});




		this.watcher.play;

		///////////////

		//// LAUNCH GUI
		if (this.serport.notNil) {
			//this.troutine = this.trackerRoutine; // start parsing of serial head tracker data
			//	this.kroutine = this.serialKeepItUp;
			this.troutine.play;
			this.kroutine.play;
		};


		server.sync;


		if(guiflag) {
			this.gui;
		};

		//// LAUNCH INITIAL SYNTH

		//if (this.serport.notNil) { // comment out serial port prerequisit
		if (decoder.notNil) {

			this.globDec = Synth.new(\globDecodeSynth,
			target:this.glbRevDecGrp,addAction: \addToTail);
		};
		//	};


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
		this.headingnumboxProxy.valueAction = h;
		this.rollnumboxProxy.valueAction = r;
		this.pitchnumboxProxy.valueAction = p;
		this.nfontes.do { arg i;

			/*if (guiflag) {
				sprite[i, 0] = this.halfwidth + (this.cartval[i].x * this.halfheight);
				sprite[i, 1] = this.halfheight - (this.cartval[i].y * this.halfheight);
			};*/

			if(this.espacializador[i].notNil) {

				this.espacializador[i].set(\azim, this.spheval[i].theta, \elev, this.spheval[i].phi,
					\radius, this.spheval[i].rho);
				this.setSynths(i, \azim, this.spheval[i].theta, \elev, this.spheval[i].phi,
					\radius, this.spheval[i].rho);
				this.synt[i].set(\azim, this.spheval[i].theta, \elev, this.spheval[i].phi,
					\radius, this.spheval[i].rho);
			};

		};

	}

	matchTByte { |byte|  // match incoming headtracker data

        if(this.trackarr[this.tracki].isNil or:{this.trackarr[this.tracki]==byte}, {
			this.trackarr2[this.tracki]= byte;
			this.tracki= this.tracki+1;
			if(this.tracki>=this.trackarr.size, {
				//				this.procTracker(this.trackarr2[4]<<8+this.trackarr2[5],
				//				this.trackarr2[6]<<8+this.trackarr2[7],
				//              this.trackarr2[8]<<8+this.trackarr2[9],
				this.procTracker(
					(this.trackarr2[5]<<8)+this.trackarr2[4],
					(this.trackarr2[7]<<8)+this.trackarr2[6], (this.trackarr2[9]<<8)+this.trackarr2[8]
					//,
					//(this.trackarr2[13]<<24) + (this.trackarr2[12]<<16) + (this.trackarr2[11]<<8)
					//+ this.trackarr2[10],
					//(this.trackarr2[17]<<24) + (this.trackarr2[16]<<16) + (this.trackarr2[15]<<8)
					//+ this.trackarr2[14]
				);
				this.tracki= 0;
			});
        }, {
			this.tracki= 0;
        });
	}



	trackerRoutine { Routine.new
		( {
			inf.do{
				//this.trackPort.read.postln;
				this.matchTByte(this.trackPort.read);
			};
		})
	}

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

	playAutomation {
		this.control.play;
	}

	// no longer necessary as added a autoloop creation argument
	playAutomationLooped {
		//this.autoloopval = true;
		this.autoloop.valueAction = true;
		this.control.play;
	}

	globBfmtNeeded { |i|
		if (i == this.nfontes) {
			^false.asBoolean;
		} {
			if ( ((this.dstrv[i].value == 3)  // A-fomat reverb swich
				|| this.playingBF[i].asBoolean) && this.espacializador[i].notNil ) {
				^true.asBoolean;
			} {
				^this.globBfmtNeeded(i + 1);
			};
		};
	}


	globSoaA12Needed { |i|
		if (i == this.nfontes) {
			^false.asBoolean;
		} {
			if ( (this.dstrv[i].value == 3) // A-fomat reverb swich
				&& this.espacializador[i].notNil ) {
				^true.asBoolean;
			} {
				^this.globSoaA12Needed(i + 1);
			};
		};
	}


	nonAmbi2FuMaNeeded { |i|
		if (i == this.nfontes) {
			^false.asBoolean;
		} {
			if ( (this.lib[i].value > 4) // pass ambisonic libs
				&& this.espacializador[i].notNil ) {
				^true.asBoolean;
			} {
				^this.nonAmbi2FuMaNeeded(i + 1);
			};
		};
	}

	converterNeeded { |i|
		if (i == this.nfontes) {
			^false.asBoolean;
		} {
			if (convert_fuma && (this.clsrv != 0)) {
				^true.asBoolean;
			} {
				if ( this.convert[i] && this.espacializador[i].notNil ) {
					^true.asBoolean;
				} {
					^this.converterNeeded(i + 1);
				};
			};
		};
	}


	newtocar {
		arg i, tpos, force = false;
		var path = this.tfieldProxy[i].value, stdur;

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


		/// STREAM FROM DISK


		if ((path != "") && this.hwncheckProxy[i].value.not
			&& this.scncheckProxy[i].value.not
			&& this.streamdisk[i]) {
			var x;
			"Content Streamed from disk".postln;
			if (testado[i].not || force) { // if source is testing don't relaunch synths
				x = case
				{ this.streambuf[i].numChannels == 1} {
					"1 channel".postln;

					// comment out automatic settings, prefer sensible deffaults
					/*if (guiflag) {
					{angnumbox.value = 0;}.defer;
					{angslider.value = 0;}.defer;
					};
					cboxProxy[i].valueAction = 1;
					clev[i] = 1;
					if((i == currentsource) && guiflag) {
					cslider.value = 1;
					connumbox.value = 1;
					};*/

					this.synt[i] = Synth.new(\playMonoStream, [\outbus, mbus[i],
						\bufnum, streambuf[i].bufnum, \rate, 1, \tpos, tpos, \lp, lp[i],
						\level, level[i]],
					this.playEspacGrp).onFree({this.espacializador[i].free;
						this.espacializador[i] = nil; this.synt[i] = nil;
						this.streambuf[i].free;
					});

					if (this.dstrvboxProxy[i].value == 3) { // A-fomat reverb swich

						libboxProxy[i].valueAction = 3;

						// set lib, convert and dstrv variables when stynths are lauched
						// for the tracking functions to stay relevant

						lib[i] = 3;
						dstrv[i] = 3;
						convert[i] = convert_fuma;

						if (convert_fuma) {
							if (this.convertor.notNil) {
								this.convertor.set(\gate, 1);
							} {
								this.convertor = Synth.new(\ambiConverter, [\gate, 1],
									target:this.glbRevDecGrp).onFree({
									this.convertor = nil;
								});
							};
						};

						if (clsrv > 0) {
							if (revGlobalBF.isNil) {
								this.revGlobalBF = Synth.new(\revGlobalBFormatAmb++clsRvtypes,
									[\gbfbus, gbfbus, \gate, 1, \room, clsrm, \damp, clsdm,
									\fluir, rirFLUspectrum[max((clsrv - 3), 0)],
										\frdir, rirFRDspectrum[max((clsrv - 3), 0)],
										\bldir, rirBLDspectrum[max((clsrv - 3), 0)],
										\bruir, rirBRUspectrum[max((clsrv - 3), 0)]],
									this.glbRevDecGrp).register.onFree({
									if (this.revGlobalBF.isPlaying.not) {
										this.revGlobalBF = nil;
									};
								});
							} {
								this.revGlobalBF.set(\gate, 1);
							};

							if (this.maxorder > 1) {
								if(revGlobalSoa.isNil) {
									this.revGlobalSoa = Synth.new(\revGlobalSoaA12++clsRvtypes,
										[\soaBus, soaBus, \gate, 1, \room, clsrm, \damp, clsdm,
											\a0ir, rirA12Spectrum[max((clsrv - 3), 0), 0],
											\a1ir, rirA12Spectrum[max((clsrv - 3), 0), 1],
											\a2ir, rirA12Spectrum[max((clsrv - 3), 0), 2],
											\a3ir, rirA12Spectrum[max((clsrv - 3), 0), 3],
											\a4ir, rirA12Spectrum[max((clsrv - 3), 0), 4],
											\a5ir, rirA12Spectrum[max((clsrv - 3), 0), 5],
											\a6ir, rirA12Spectrum[max((clsrv - 3), 0), 6],
											\a7ir, rirA12Spectrum[max((clsrv - 3), 0), 7],
											\a8ir, rirA12Spectrum[max((clsrv - 3), 0), 8],
											\a9ir, rirA12Spectrum[max((clsrv - 3), 0), 9],
											\a10ir, rirA12Spectrum[max((clsrv - 3), 0), 10],
											\a11ir, rirA12Spectrum[max((clsrv - 3), 0), 11]],
										this.glbRevDecGrp).register.onFree({
										if (this.revGlobalSoa.isPlaying.not) {
											this.revGlobalSoa = nil;
										};
									});
								} {
									this.revGlobalSoa.set(\gate, 1);
								};
							};
						};

						//comment out all linear parameters
						//this.espacializador[i] = Synth.new(\espacAFormatVerb++ln[i],

						this.espacializador[i] = Synth.new(\espacAFormatVerb,
							[\inbus, mbus[i], \angle, angle[i],
								\soaBus, soaBus, \gbfbus, gbfbus, \contr, clev[i],
								\insertFlag, this.insertFlag[i],
								\aFormatBusInFoa, this.aFormatBusFoa[0,i].index,
								\aFormatBusOutFoa, this.aFormatBusFoa[1,i].index,
								\aFormatBusInSoa, this.aFormatBusSoa[0,i].index,
								\aFormatBusOutSoa, this.aFormatBusSoa[1,i].index],
							this.synt[i], addAction: \addAfter).onFree({
							if (this.revGlobalSoa.notNil) {
								if (this.globSoaA12Needed(0).not) {
									this.revGlobalSoa.set(\gate, 0);
								};
							};
							if (this.revGlobalBF.notNil) {
								if (this.globBfmtNeeded(0).not) {
									this.revGlobalBF.set(\gate, 0);
								};
							};
							if (this.convertor.notNil) {
								if (convert_fuma) {
									if (this.converterNeeded(0).not) {
										this.convertor.set(\gate, 0);
									};
								};
							};
						});

					} {
						// set lib, convert and dstrv variables when stynths are lauched
						// for the tracking functions to stay relevant

						case
						{ libboxProxy[i].value == 0 }
						{ convert[i] = convert_ambix;
							lib[i] = libboxProxy[i].value; }
						{ libboxProxy[i].value == 1 }
						{ convert[i] = convert_ambix;
							lib[i] = libboxProxy[i].value; }
						{ libboxProxy[i].value == 2 }
						{ convert[i] = convert_ambix;
							lib[i] = libboxProxy[i].value; }
						{ libboxProxy[i].value == 3 }
						{ convert[i] = convert_fuma;
							lib[i] = libboxProxy[i].value; }
						{ libboxProxy[i].value == 4 }
						{ convert[i] = convert_direct;
							lib[i] = libboxProxy[i].value; };

						dstrv[i] = dstrvboxProxy[i].value;

						if (convert[i]) {

							if (this.convertor.notNil) {
								this.convertor.set(\gate, 1);
							} {
								this.convertor = Synth.new(\ambiConverter, [\gate, 1],
									target:this.glbRevDecGrp).onFree({
									this.convertor = nil;
								});
							};
						};

						if (speaker_array.isNil) {

							if ((this.libboxProxy[i].value > 3) && this.nonAmbi2FuMa.isNil) {
								this.nonAmbi2FuMa = Synth.new(\nonAmbi2FuMa,
									target:this.glbRevDecGrp).onFree({
									this.nonAmbi2FuMa = nil;
								});
							};
						};


						//comment out all linear parameters
						//{this.espacializador[i] = Synth.new(\espacAmbChowning++ln[i],

						this.espacializador[i] = Synth.new(libName[i]++"Chowning"++dstrvtypes[i],
							[\inbus, mbus[i],
								\gbus, gbus,
								\insertFlag, this.insertFlag[i],
								\aFormatBusInFoa, this.aFormatBusFoa[0,i].index,
								\aFormatBusOutFoa, this.aFormatBusFoa[1,i].index,
								\aFormatBusInSoa, this.aFormatBusSoa[0,i].index,
								\aFormatBusOutSoa, this.aFormatBusSoa[1,i].index,
								\contr, clev[i], \room, rm[i], \damp, dm[i],
								\wir, rirWspectrum[max(this.dstrvboxProxy[i].value - 4, 0)]],
							this.synt[i], addAction:\addAfter).onFree({
							if (speaker_array.isNil) {
								if (this.nonAmbi2FuMaNeeded(0).not
									&& this.nonAmbi2FuMa.notNil) {
									this.nonAmbi2FuMa.free;
								};
							};
							if (this.convertor.notNil) {
								if (convert[i]) {
									if (this.converterNeeded(0).not) {
										this.convertor.set(\gate, 0);
									};
								};
							};
						});

					};
					//atualizarvariaveis.value;
					updatesourcevariables.value(i);

				}
				{ this.streambuf[i].numChannels == 2} {
					"2 channel".postln;
					this.ncanais[i] = 2;

					// comment out automatic settings, prefer sensible deffaults
					/*angle[i] = pi/2;
					cboxProxy[i].valueAction = 1;
										};
					clev[i] = 1;
					if((i == currentsource) && guiflag) {
						cslider.value = 1;
						connumbox.value = 1;
					};
					if (guiflag) {
						{angnumbox.value = 1.05;}.defer; // 60 degrees
						{angslider.value = 0.33;}.defer;
					};*/

					this.synt[i] = Synth.new(\playStereoStream, [\outbus, sbus[i],
						\bufnum, streambuf[i].bufnum, \rate, 1, \tpos, tpos, \lp, lp[i],
						\level, level[i]],
					this.playEspacGrp).onFree({this.espacializador[i].free;
						this.espacializador[i] = nil; this.synt[i] = nil;
						this.streambuf[i].free;
					});

					if (this.dstrvboxProxy[i].value == 3) { // A-fomat reverb swich

						libboxProxy[i].valueAction = 3;

						// set lib, convert and dstrv variables when stynths are lauched
						// for the tracking functions to stay relevant

						lib[i] = 3;
						dstrv[i] = 3;
						convert[i] = convert_fuma;

						if (convert_fuma) {
							if (this.convertor.notNil) {
								this.convertor.set(\gate, 1);
							} {
								this.convertor = Synth.new(\ambiConverter, [\gate, 1],
									target:this.glbRevDecGrp).onFree({
									this.convertor = nil;
								});
							};
						};

						if (clsrv > 0) {
							if (revGlobalBF.isNil) {
								this.revGlobalBF = Synth.new(\revGlobalBFormatAmb++clsRvtypes,
									[\gbfbus, gbfbus, \gate, 1, \room, clsrm, \damp, clsdm,
										\fluir, rirFLUspectrum[max((clsrv - 3), 0)],
										\frdir, rirFRDspectrum[max((clsrv - 3), 0)],
										\bldir, rirBLDspectrum[max((clsrv - 3), 0)],
										\bruir, rirBRUspectrum[max((clsrv - 3), 0)]],
									this.glbRevDecGrp).register.onFree({
									if (this.revGlobalBF.isPlaying.not) {
										this.revGlobalBF = nil;
									};
								});
							} {
								this.revGlobalBF.set(\gate, 1);
							};

							if (this.maxorder > 1) {
								if(revGlobalSoa.isNil) {
									this.revGlobalSoa = Synth.new(\revGlobalSoaA12++clsRvtypes,
										[\soaBus, soaBus, \gate, 1, \room, clsrm, \damp, clsdm,
											\a0ir, rirA12Spectrum[max((clsrv - 3), 0), 0],
											\a1ir, rirA12Spectrum[max((clsrv - 3), 0), 1],
											\a2ir, rirA12Spectrum[max((clsrv - 3), 0), 2],
											\a3ir, rirA12Spectrum[max((clsrv - 3), 0), 3],
											\a4ir, rirA12Spectrum[max((clsrv - 3), 0), 4],
											\a5ir, rirA12Spectrum[max((clsrv - 3), 0), 5],
											\a6ir, rirA12Spectrum[max((clsrv - 3), 0), 6],
											\a7ir, rirA12Spectrum[max((clsrv - 3), 0), 7],
											\a8ir, rirA12Spectrum[max((clsrv - 3), 0), 8],
											\a9ir, rirA12Spectrum[max((clsrv - 3), 0), 9],
											\a10ir, rirA12Spectrum[max((clsrv - 3), 0), 10],
											\a11ir, rirA12Spectrum[max((clsrv - 3), 0), 11]],
										this.glbRevDecGrp).register.onFree({
										if (this.revGlobalSoa.isPlaying.not) {
											this.revGlobalSoa = nil;
										};
									});
								} {
									this.revGlobalSoa.set(\gate, 1);
								};

							};

						};

						//comment out all linear parameters
						//this.espacializador[i] = Synth.new(\espacAmbEstereoAFormat++ln[i],

						this.espacializador[i] = Synth.new(\espacEstereoAFormat,
							[\inbus, sbus[i], \angle, angle[i],
								\gbus, gbus, \soaBus, soaBus, \gbfbus, gbfbus, \contr, clev[i],
								\insertFlag, this.insertFlag[i],
								\aFormatBusInFoa, this.aFormatBusFoa[0,i].index,
								\aFormatBusOutFoa, this.aFormatBusFoa[1,i].index,
								\aFormatBusInSoa, this.aFormatBusSoa[0,i].index,
								\aFormatBusOutSoa, this.aFormatBusSoa[1,i].index],
							this.synt[i], addAction: \addAfter).onFree({
							if (this.revGlobalSoa.notNil) {
								if (this.globSoaA12Needed(0).not) {
									this.revGlobalSoa.set(\gate, 0);
								};
							};
							if (this.revGlobalBF.notNil) {
								if (this.globBfmtNeeded(0).not) {
									this.revGlobalBF.set(\gate, 0);
								};
							};
							if (this.convertor.notNil) {
								if (convert_fuma) {
									if (this.converterNeeded(0).not) {
										this.convertor.set(\gate, 0);
									};
								};
							};
						});

					} {
						// set lib, convert and dstrv variables when stynths are lauched
						// for the tracking functions to stay relevant

						case
						{ libboxProxy[i].value == 0 }
						{ convert[i] = convert_ambix;
							lib[i] = libboxProxy[i].value; }
						{ libboxProxy[i].value == 1 }
						{ convert[i] = convert_ambix;
							lib[i] = libboxProxy[i].value; }
						{ libboxProxy[i].value == 2 }
						{ convert[i] = convert_ambix;
							lib[i] = libboxProxy[i].value; }
						{ libboxProxy[i].value == 3 }
						{ convert[i] = convert_fuma;
							lib[i] = libboxProxy[i].value; }
						{ libboxProxy[i].value == 4 }
						{ convert[i] = convert_direct;
							lib[i] = libboxProxy[i].value; };

						dstrv[i] = dstrvboxProxy[i].value;

						if (convert[i]) {

							if (this.convertor.notNil) {
								this.convertor.set(\gate, 1);
							} {
								this.convertor = Synth.new(\ambiConverter, [\gate, 1],
									target:this.glbRevDecGrp).onFree({
									this.convertor = nil;
								});
							};
						};

						if (speaker_array.isNil) {

							if ((this.libboxProxy[i].value > 3) && this.nonAmbi2FuMa.isNil) {
								this.nonAmbi2FuMa = Synth.new(\nonAmbi2FuMa,
									target:this.glbRevDecGrp).onFree({
									this.nonAmbi2FuMa = nil;
								});
							};
						};

						//comment out all linear parameters
						//this.espacializador[i] = Synth.new(\espacEstereoChowning++ln[i],

						this.espacializador[i] = Synth.new(libName[i]++"StereoChowning"++dstrvtypes[i],
							[\inbus, sbus[i],
								\gbus, gbus, \angle, angle[i],
								\insertFlag, this.insertFlag[i],
								\aFormatBusInFoa, this.aFormatBusFoa[0,i].index,
								\aFormatBusOutFoa, this.aFormatBusFoa[1,i].index,
								\aFormatBusInSoa, this.aFormatBusSoa[0,i].index,
								\aFormatBusOutSoa, this.aFormatBusSoa[1,i].index,
								\room, rm[i], \damp, dm[i], \contr, clev[i],
								\zir, rirZspectrum[max(this.dstrvboxProxy[i].value - 4, 0)]],
							this.synt[i], addAction: \addAfter).onFree({
							if (speaker_array.isNil) {
								if (this.nonAmbi2FuMaNeeded(0).not
									&& this.nonAmbi2FuMa.notNil) {
									this.nonAmbi2FuMa.free;
								};
							};
							if (this.convertor.notNil) {
								if (convert[i]) {
									if (this.converterNeeded(0).not) {
										this.convertor.set(\gate, 0);
									};
								};
							};
						});

					};
					updatesourcevariables.value(i);

				}
				{ this.streambuf[i].numChannels >= 4} {
					"4 channel".postln;
					this.playingBF[i] = true;
					ncanais[i] = 4;

					// comment out automatic settings, prefer sensible deffaults
					/*angle[i] = 0;
					if (guiflag) {
						{angnumbox.value = 0;}.defer;
					};
					cboxProxy[i].valueAction = 0;
					clev[i] = 0;
					if((i == currentsource) && guiflag) {
						cslider.value = 0;
						connumbox.value = 0;
					};
					if (guiflag) {
						{angslider.value = 0;}.defer;
					};*/

					libboxProxy[i].valueAction = 3;

					// set lib, convert and dstrv variables when stynths are lauched
					// for the tracking functions to stay relevant

					lib[i] = 3;
					convert[i] = convert_fuma;
					dstrv[i] = dstrvboxProxy[i].value;

					if (convert_fuma) {
						if (this.convertor.notNil) {
							this.convertor.set(\gate, 1);
						} {
							this.convertor = Synth.new(\ambiConverter, [\gate, 1],
								target:this.glbRevDecGrp).onFree({
								this.convertor = nil;
							});
						};
					};

					this.synt[i] = Synth.new(\playBFormatStream, [\gbus, gbus, \gbfbus,
						gbfbus, \outbus,
						mbus[i], \bufnum, streambuf[i].bufnum, \contr, clev[i],
						\rate, 1, \tpos, tpos, \lp,
						lp[i], \level, level[i],
						\insertFlag, this.insertFlag[i],
						\aFormatBusInFoa, this.aFormatBusFoa[0,i].index,
						\aFormatBusOutFoa, this.aFormatBusFoa[1,i].index,
						\aFormatBusInSoa, this.aFormatBusSoa[0,i].index,
						\aFormatBusOutSoa, this.aFormatBusSoa[1,i].index],
					this.playEspacGrp).onFree({this.espacializador[i].free;
						this.espacializador[i] = nil; this.synt[i] = nil;
						this.playingBF[i] = false;
						this.streambuf[i].free;
					});

					if (clsrv > 0) {
						if (revGlobalBF.isNil) {
							this.revGlobalBF = Synth.new(\revGlobalBFormatAmb++clsRvtypes,
								[\gbfbus, gbfbus, \gate, 1, \room, clsrm, \damp, clsdm,
									\fluir, rirFLUspectrum[max((clsrv - 3), 0)],
									\frdir, rirFRDspectrum[max((clsrv - 3), 0)],
									\bldir, rirBLDspectrum[max((clsrv - 3), 0)],
									\bruir, rirBRUspectrum[max((clsrv - 3), 0)]],
								this.glbRevDecGrp).register.onFree({
								if (this.revGlobalBF.isPlaying.not) {
									this.revGlobalBF = nil;
								};
							});
						} {
							this.revGlobalBF.set(\gate, 1);
						};

						if (this.dstrvboxProxy[i].value == 3) { // A-fomat reverb swich

							if (this.maxorder > 1) {
								if(revGlobalSoa.isNil) {
									this.revGlobalSoa = Synth.new(\revGlobalSoaA12++clsRvtypes,
										[\soaBus, soaBus, \gate, 1, \room, clsrm, \damp, clsdm,
											\a0ir, rirA12Spectrum[max((clsrv - 3), 0), 0],
											\a1ir, rirA12Spectrum[max((clsrv - 3), 0), 1],
											\a2ir, rirA12Spectrum[max((clsrv - 3), 0), 2],
											\a3ir, rirA12Spectrum[max((clsrv - 3), 0), 3],
											\a4ir, rirA12Spectrum[max((clsrv - 3), 0), 4],
											\a5ir, rirA12Spectrum[max((clsrv - 3), 0), 5],
											\a6ir, rirA12Spectrum[max((clsrv - 3), 0), 6],
											\a7ir, rirA12Spectrum[max((clsrv - 3), 0), 7],
											\a8ir, rirA12Spectrum[max((clsrv - 3), 0), 8],
											\a9ir, rirA12Spectrum[max((clsrv - 3), 0), 9],
											\a10ir, rirA12Spectrum[max((clsrv - 3), 0), 10],
											\a11ir, rirA12Spectrum[max((clsrv - 3), 0), 11]],
										this.glbRevDecGrp).register.onFree({
										if (this.revGlobalSoa.isPlaying.not) {
											this.revGlobalSoa = nil;
										};
									});
								} {
									this.revGlobalSoa.set(\gate, 1);
								};

							};
						};

						//comment out all linear parameters
						//this.espacializador[i] = Synth.new(\espacAmb2AFormat++ln[i],

						this.espacializador[i] = Synth.new(\ATK2AFormat, [\inbus, mbus[i],
							\gbus, gbus, \soaBus, soaBus,
							\insertFlag, this.insertFlag[i], \contr, clev[i],
							\aFormatBusInFoa, this.aFormatBusFoa[0,i].index,
							\aFormatBusOutFoa, this.aFormatBusFoa[1,i].index,
							\aFormatBusInSoa, this.aFormatBusSoa[0,i].index,
							\aFormatBusOutSoa, this.aFormatBusSoa[1,i].index],
						this.synt[i], addAction: \addAfter).onFree({
							if (this.revGlobalSoa.notNil) {
								if (this.globSoaA12Needed(0).not) {
									this.revGlobalSoa.set(\gate, 0);
								};
							};
							if (this.revGlobalBF.notNil) {
								if (this.globBfmtNeeded(0).not) {
									this.revGlobalBF.set(\gate, 0);
								};
							};
							if (this.convertor.notNil) {
								if (convert_fuma) {
									if (this.converterNeeded(0).not) {
										this.convertor.set(\gate, 0);
									};
								};
							};
						});

					} {

						//comment out all linear parameters
							//this.synt[i] = Synth.new(\playBFormatStream++ln[i], [\gbus, gbus, \gbfbus,

						this.espacializador[i] = Synth.new(\ATK2Chowning++dstrvtypes[i],
							[\inbus, mbus[i], \gbus, gbus,
								\insertFlag, this.insertFlag[i], \contr, clev[i],
								\aFormatBusInFoa, this.aFormatBusFoa[0,i].index,
								\aFormatBusOutFoa, this.aFormatBusFoa[1,i].index,
								\aFormatBusInSoa, this.aFormatBusSoa[0,i].index,
								\aFormatBusOutSoa, this.aFormatBusSoa[1,i].index,
								\room, rm[i], \damp, dm[i],
								\wir, rirWspectrum[max(this.dstrvboxProxy[i].value - 4, 0)]],
							this.synt[i], addAction: \addAfter).onFree({
							if (this.revGlobalSoa.notNil) {
								if (this.globSoaA12Needed(0).not) {
									this.revGlobalSoa.set(\gate, 0);
								};
							};
							if (this.revGlobalBF.notNil) {
								if (this.globBfmtNeeded(0).not) {
									this.revGlobalBF.set(\gate, 0);
								};
							};
							if (this.convertor.notNil) {
								if (convert_fuma) {
									if (this.converterNeeded(0).not) {
										this.convertor.set(\gate, 0);
									};
								};
							};
						});

					};

				};

				updatesourcevariables.value(i);
			};


		};

		/// END STREAM FROM DISK

		// check this logic - what should override what?
		if ((path != "") && (this.hwncheckProxy[i].value.not
			|| this.scncheckProxy[i].value.not)
		&& this.streamdisk[i].not) {

			//{

			if (sombuf[i].numChannels == 1)  // arquivo mono
			{
				//"Am I mono?".postln;
				ncanais[i] = 1;

				// comment out automatic settings, prefer sensible deffaults
				/*angle[i] = 0;
				if (guiflag) {
					{angnumbox.value = 0;}.defer;
					{angslider.value = 0;}.defer;
				};
				cboxProxy[i].valueAction = 1;
				clev[i] = 1;
				if((i == currentsource) && guiflag) {
					cslider.value = 1;
					connumbox.value = 1;
				};*/


				this.synt[i] = Synth.new(\playMonoFile, [\outbus, mbus[i],
					\bufnum, sombuf[i].bufnum, \rate, 1, \tpos, tpos, \lp, lp[i],
					\level, level[i]], this.playEspacGrp).onFree({this.espacializador[i].free;
					this.espacializador[i] = nil;
					this.synt[i] = nil});


				if (this.dstrvboxProxy[i].value == 3) { // A-fomat reverb swich

					libboxProxy[i].valueAction = 3;

					// set lib, convert and dstrv variables when stynths are lauched
					// for the tracking functions to stay relevant

					lib[i] = 3;
					dstrv[i] = 3;
					convert[i] = convert_fuma;

					if (convert_fuma) {
						if (this.convertor.notNil) {
							this.convertor.set(\gate, 1);
						} {
							this.convertor = Synth.new(\ambiConverter, [\gate, 1],
								target:this.glbRevDecGrp).onFree({
								this.convertor = nil;
							});
						};
					};

					if (clsrv > 0) {
						if (revGlobalBF.isNil) {
							this.revGlobalBF = Synth.new(\revGlobalBFormatAmb++clsRvtypes,
								[\gbfbus, gbfbus, \gate, 1, \room, clsrm, \damp, clsdm,
									\fluir, rirFLUspectrum[max((clsrv - 3), 0)],
									\frdir, rirFRDspectrum[max((clsrv - 3), 0)],
									\bldir, rirBLDspectrum[max((clsrv - 3), 0)],
									\bruir, rirBRUspectrum[max((clsrv - 3), 0)]],
								this.glbRevDecGrp).register.onFree({
							if (this.revGlobalBF.isPlaying.not) {
									this.revGlobalBF = nil;
								};
							});
						} {
							this.revGlobalBF.set(\gate, 1);
						};

						if (this.maxorder > 1) {
							if(revGlobalSoa.isNil) {
								this.revGlobalSoa = Synth.new(\revGlobalSoaA12++clsRvtypes,
									[\soaBus, soaBus, \gate, 1, \room, clsrm, \damp, clsdm,
										\a0ir, rirA12Spectrum[max((clsrv - 3), 0), 0],
										\a1ir, rirA12Spectrum[max((clsrv - 3), 0), 1],
										\a2ir, rirA12Spectrum[max((clsrv - 3), 0), 2],
										\a3ir, rirA12Spectrum[max((clsrv - 3), 0), 3],
										\a4ir, rirA12Spectrum[max((clsrv - 3), 0), 4],
										\a5ir, rirA12Spectrum[max((clsrv - 3), 0), 5],
										\a6ir, rirA12Spectrum[max((clsrv - 3), 0), 6],
										\a7ir, rirA12Spectrum[max((clsrv - 3), 0), 7],
										\a8ir, rirA12Spectrum[max((clsrv - 3), 0), 8],
										\a9ir, rirA12Spectrum[max((clsrv - 3), 0), 9],
										\a10ir, rirA12Spectrum[max((clsrv - 3), 0), 10],
										\a11ir, rirA12Spectrum[max((clsrv - 3), 0), 11]],
									this.glbRevDecGrp).register.onFree({
									if (this.revGlobalSoa.isPlaying.not) {
										this.revGlobalSoa = nil;
									};
								});
							} {
								this.revGlobalSoa.set(\gate, 1);
							};
						};
					};

					//comment out all linear parameters
					//this.espacializador[i] = Synth.new(\espacAFormatVerb++ln[i],

					this.espacializador[i] = Synth.new(\espacAFormatVerb,
						[\inbus, mbus[i], \angle, angle[i],
							\soaBus, soaBus, \gbfbus, gbfbus, \contr, clev[i],
							\insertFlag, this.insertFlag[i],
							\aFormatBusInFoa, this.aFormatBusFoa[0,i].index,
							\aFormatBusOutFoa, this.aFormatBusFoa[1,i].index,
							\aFormatBusInSoa, this.aFormatBusSoa[0,i].index,
							\aFormatBusOutSoa, this.aFormatBusSoa[1,i].index],
						this.synt[i], addAction: \addAfter).onFree({
						if (this.revGlobalSoa.notNil) {
							if (this.globSoaA12Needed(0).not) {
								this.revGlobalSoa.set(\gate, 0);
							};
						};
						if (this.revGlobalBF.notNil) {
							if (this.globBfmtNeeded(0).not) {
								this.revGlobalBF.set(\gate, 0);
							};
						};
						if (this.convertor.notNil) {
							if (convert_fuma) {
								if (this.converterNeeded(0).not) {
									this.convertor.set(\gate, 0);
								};
							};
						};
					});

				} {
					// set lib, convert and dstrv variables when stynths are lauched
					// for the tracking functions to stay relevant

					case
					{ libboxProxy[i].value == 0 }
					{ convert[i] = convert_ambix;
						lib[i] = libboxProxy[i].value; }
					{ libboxProxy[i].value == 1 }
					{ convert[i] = convert_ambix;
						lib[i] = libboxProxy[i].value; }
					{ libboxProxy[i].value == 2 }
					{ convert[i] = convert_ambix;
						lib[i] = libboxProxy[i].value; }
					{ libboxProxy[i].value == 3 }
					{ convert[i] = convert_fuma;
						lib[i] = libboxProxy[i].value; }
					{ libboxProxy[i].value == 4 }
					{ convert[i] = convert_direct;
						lib[i] = libboxProxy[i].value; };

					dstrv[i] = dstrvboxProxy[i].value;

					if (convert[i]) {

						if (this.convertor.notNil) {
							this.convertor.set(\gate, 1);
						} {
							this.convertor = Synth.new(\ambiConverter, [\gate, 1],
								target:this.glbRevDecGrp).onFree({
								this.convertor = nil;
							});
						};
					};

					if (speaker_array.isNil) {

						if ((this.libboxProxy[i].value > 3) && this.nonAmbi2FuMa.isNil) {
							this.nonAmbi2FuMa = Synth.new(\nonAmbi2FuMa,
								target:this.glbRevDecGrp).onFree({
								this.nonAmbi2FuMa = nil;
							});
						};
					};

					//comment out all linear parameters
					//{this.espacializador[i] = Synth.new(\espacAmbChowning++ln[i],

					this.espacializador[i] = Synth.new(libName[i]++"Chowning"++dstrvtypes[i],
						[\inbus, mbus[i],
							\gbus, gbus,
							\insertFlag, this.insertFlag[i],
							\aFormatBusInFoa, this.aFormatBusFoa[0,i].index,
							\aFormatBusOutFoa, this.aFormatBusFoa[1,i].index,
							\aFormatBusInSoa, this.aFormatBusSoa[0,i].index,
							\aFormatBusOutSoa, this.aFormatBusSoa[1,i].index,
							\room, rm[i], \damp, dm[i], \contr, clev[i],
							\wir, rirWspectrum[max(this.dstrvboxProxy[i].value - 4, 0)]],
						this.synt[i], addAction: \addAfter).onFree({
						if (speaker_array.isNil) {
							if (this.nonAmbi2FuMaNeeded(0).not
								&& this.nonAmbi2FuMa.notNil) {
								this.nonAmbi2FuMa.free;
							};
						};
						if (this.convertor.notNil) {
							if (convert[i]) {
								if (this.converterNeeded(0).not) {
									this.convertor.set(\gate, 0);
								};
							};
						};
					});

				};
				updatesourcevariables.value(i);


			}
			{if (sombuf[i].numChannels == 2) {

				ncanais[i] = 2; // arquivo estéreo

				// comment out automatic settings, prefer sensible deffaults
				/*angle[i] = pi/2;
				cboxProxy[i].valueAction = 1;
				clev[i] = 1;
				if((i == currentsource) && guiflag) {
					cslider.value = 1;
					connumbox.value = 1;
				};
				if (guiflag) {
					{angnumbox.value = 1.05;}.defer; // 60 degrees
					{angslider.value = 0.33;}.defer;
				};*/

				this.synt[i] = Synth.new(\playStereoFile, [\outbus, sbus[i],
					\bufnum, sombuf[i].bufnum, \rate, 1, \tpos, tpos, \lp, lp[i],
					\level, level[i]], this.playEspacGrp).onFree({this.espacializador[i].free;
					this.espacializador[i] = nil;
					this.synt[i] = nil});

				if (this.dstrvboxProxy[i].value == 3) { // A-fomat reverb swich

					libboxProxy[i].valueAction = 3;

					// set lib, convert and dstrv variables when stynths are lauched
					// for the tracking functions to stay relevant

					lib[i] = 3;
					dstrv[i] = 3;
					convert[i] = convert_fuma;

					if (convert_fuma) {
						if (this.convertor.notNil) {
							this.convertor.set(\gate, 1);
						} {
							this.convertor = Synth.new(\ambiConverter, [\gate, 1],
								target:this.glbRevDecGrp).onFree({
								this.convertor = nil;
							});
						};
					};

					if (clsrv > 0) {
						if (revGlobalBF.isNil) {
							this.revGlobalBF = Synth.new(\revGlobalBFormatAmb++clsRvtypes,
								[\gbfbus, gbfbus, \gate, 1, \room, clsrm, \damp, clsdm,
									\fluir, rirFLUspectrum[max((clsrv - 3), 0)],
									\frdir, rirFRDspectrum[max((clsrv - 3), 0)],
									\bldir, rirBLDspectrum[max((clsrv - 3), 0)],
									\bruir, rirBRUspectrum[max((clsrv - 3), 0)]],
								this.glbRevDecGrp).register.onFree({
								if (this.revGlobalBF.isPlaying.not) {
									this.revGlobalBF = nil;
								};
							});
						} {
							this.revGlobalBF.set(\gate, 1);
						};
					};

					if (this.maxorder > 1) {
						if(revGlobalSoa.isNil) {
							this.revGlobalSoa = Synth.new(\revGlobalSoaA12++clsRvtypes,
								[\soaBus, soaBus, \gate, 1, \room, clsrm, \damp, clsdm,
									\a0ir, rirA12Spectrum[max((clsrv - 3), 0), 0],
									\a1ir, rirA12Spectrum[max((clsrv - 3), 0), 1],
									\a2ir, rirA12Spectrum[max((clsrv - 3), 0), 2],
									\a3ir, rirA12Spectrum[max((clsrv - 3), 0), 3],
									\a4ir, rirA12Spectrum[max((clsrv - 3), 0), 4],
									\a5ir, rirA12Spectrum[max((clsrv - 3), 0), 5],
									\a6ir, rirA12Spectrum[max((clsrv - 3), 0), 6],
									\a7ir, rirA12Spectrum[max((clsrv - 3), 0), 7],
									\a8ir, rirA12Spectrum[max((clsrv - 3), 0), 8],
									\a9ir, rirA12Spectrum[max((clsrv - 3), 0), 9],
									\a10ir, rirA12Spectrum[max((clsrv - 3), 0), 10],
									\a11ir, rirA12Spectrum[max((clsrv - 3), 0), 11]],
								this.glbRevDecGrp).register.onFree({
								if (this.revGlobalSoa.isPlaying.not) {
									this.revGlobalSoa = nil;
								};
							});
						} {
							this.revGlobalSoa.set(\gate, 1);
						};
					};

					//comment out all linear parameters
					//this.espacializador[i] = Synth.new(\espacEstereoAFormat++ln[i], [\inbus, sbus[i],

					this.espacializador[i] = Synth.new(\espacEstereoAFormat,
						[\inbus, sbus[i], \contr, clev[i], \angle, angle[i],
							\gbus, gbus, \soaBus, soaBus, \gbfbus, gbfbus,
							\insertFlag, this.insertFlag[i],
							\aFormatBusInFoa, this.aFormatBusFoa[0,i].index,
							\aFormatBusOutFoa, this.aFormatBusFoa[1,i].index,
							\aFormatBusInSoa, this.aFormatBusSoa[0,i].index,
							\aFormatBusOutSoa, this.aFormatBusSoa[1,i].index],
						this.synt[i], addAction: \addAfter).onFree({
						if (this.revGlobalSoa.notNil) {
							if (this.globSoaA12Needed(0).not) {
								this.revGlobalSoa.set(\gate, 0);
							};
						};
						if (this.revGlobalBF.notNil) {
							if (this.globBfmtNeeded(0).not) {
								this.revGlobalBF.set(\gate, 0);
							};
						};
						if (this.convertor.notNil) {
							if (convert_fuma) {
								if (this.converterNeeded(0).not) {
									this.convertor.set(\gate, 0);
								};
							};
						};
					});

				} {
					// set lib, convert and dstrv variables when stynths are lauched
					// for the tracking functions to stay relevant

					case
					{ libboxProxy[i].value == 0 }
					{ convert[i] = convert_ambix;
						lib[i] = libboxProxy[i].value; }
					{ libboxProxy[i].value == 1 }
					{ convert[i] = convert_ambix;
						lib[i] = libboxProxy[i].value; }
					{ libboxProxy[i].value == 2 }
					{ convert[i] = convert_ambix;
						lib[i] = libboxProxy[i].value; }
					{ libboxProxy[i].value == 3 }
					{ convert[i] = convert_fuma;
						lib[i] = libboxProxy[i].value; }
					{ libboxProxy[i].value == 4 }
					{ convert[i] = convert_direct;
						lib[i] = libboxProxy[i].value; };

					dstrv[i] = dstrvboxProxy[i].value;

					if (convert[i]) {

						if (this.convertor.notNil) {
							this.convertor.set(\gate, 1);
						} {
							this.convertor = Synth.new(\ambiConverter, [\gate, 1],
								target:this.glbRevDecGrp).onFree({
								this.convertor = nil;
							});
						};
					};

					if (speaker_array.isNil) {

						if ((this.libboxProxy[i].value > 3) && this.nonAmbi2FuMa.isNil) {
							this.nonAmbi2FuMa = Synth.new(\nonAmbi2FuMa,
								target:this.glbRevDecGrp).onFree({
								this.nonAmbi2FuMa = nil;
							});
						};
					};

					//comment out all linear parameters
					//this.espacializador[i] = Synth.new(\espacEstereoChowning++ln[i],

					this.espacializador[i] = Synth.new(libName[i]++"StereoChowning"++dstrvtypes[i],
						[\inbus, sbus[i],
							\gbus, gbus, \angle, angle[i],
							\insertFlag, this.insertFlag[i],
							\aFormatBusInFoa, this.aFormatBusFoa[0,i].index,
							\aFormatBusOutFoa, this.aFormatBusFoa[1,i].index,
							\aFormatBusInSoa, this.aFormatBusSoa[0,i].index,
							\aFormatBusOutSoa, this.aFormatBusSoa[1,i].index,
							\room, rm[i], \damp, dm[i], \contr, clev[i],
							\zir, rirZspectrum[max(this.dstrvboxProxy[i].value - 4, 0)]],
						this.synt[i], addAction: \addAfter).onFree({
						if (speaker_array.isNil) {
							if (this.nonAmbi2FuMaNeeded(0).not
								&& this.nonAmbi2FuMa.notNil) {
								this.nonAmbi2FuMa.free;
							};
						};
						if (this.convertor.notNil) {
							if (convert[i]) {
								if (this.converterNeeded(0).not) {
									this.convertor.set(\gate, 0);
								};
							};
						};
					});

				};
				//atualizarvariaveis.value;
				updatesourcevariables.value(i);

				//	~revGlobal = Synth.new(\revGlobalAmb, [\gbus, gbus], addAction:\addToTail);


			} {
				if (sombuf[i].numChannels >= 4) {
					this.playingBF[i] = true;
					ncanais[i] = 4;

					// comment out automatic settings, prefer sensible deffaults
					/*angle[i] = 0;
					if (guiflag) {
						{angnumbox.value = 0;}.defer;
					};
					cboxProxy[i].valueAction = 0;
					clev[i] = 0;
					if((i == currentsource) && guiflag) {
						cslider.value = 0;
						connumbox.value = 0;
					};
					if (guiflag) {
						{angslider.value = 0;}.defer;
					};*/

					libboxProxy[i].valueAction = 3;

					// set lib, convert and dstrv variables when stynths are lauched
					// for the tracking functions to stay relevant

					lib[i] = 3;
					convert[i] = convert_fuma;
					dstrv[i] = dstrvboxProxy[i].value;

					if (convert_fuma) {
						if (this.convertor.notNil) {
							this.convertor.set(\gate, 1);
						} {
							this.convertor = Synth.new(\ambiConverter, [\gate, 1],
								target:this.glbRevDecGrp).onFree({
								this.convertor = nil;
							});
						};
					};

					this.synt[i] = Synth.new(\playBFormatFile, [\gbus, gbus, \gbfbus,
						gbfbus, \outbus,
						mbus[i], \bufnum, sombuf[i].bufnum, \contr, clev[i],
						\rate, 1, \tpos, tpos, \lp,
						lp[i], \level, level[i],
						\insertFlag, this.insertFlag[i],
						\aFormatBusInFoa, this.aFormatBusFoa[0,i].index,
						\aFormatBusOutFoa, this.aFormatBusFoa[1,i].index,
						\aFormatBusInSoa, this.aFormatBusSoa[0,i].index,
						\aFormatBusOutSoa, this.aFormatBusSoa[1,i].index],
					this.playEspacGrp).onFree({this.espacializador[i].free;
						this.espacializador[i] = nil;
						this.synt[i] = nil;
						this.playingBF[i] = false;
					});

					if (clsrv > 0) {
						if (revGlobalBF.isNil) {
							this.revGlobalBF = Synth.new(\revGlobalBFormatAmb++clsRvtypes,
								[\gbfbus, gbfbus, \gate, 1, \room, clsrm, \damp, clsdm,
									\fluir, rirFLUspectrum[max((clsrv - 3), 0)],
									\frdir, rirFRDspectrum[max((clsrv - 3), 0)],
									\bldir, rirBLDspectrum[max((clsrv - 3), 0)],
									\bruir, rirBRUspectrum[max((clsrv - 3), 0)]],
								this.glbRevDecGrp).register.onFree({
								if (this.revGlobalBF.isPlaying.not) {
									this.revGlobalBF = nil;
								};
							});
						} {
							this.revGlobalBF.set(\gate, 1);
						};

						// reverb for contracted (mono) component - and for rest too
						if (this.dstrvboxProxy[i].value == 3) { // A-fomat reverb swich

							dstrv[i] = 3;

							if (this.maxorder > 1) {
								if(revGlobalSoa.isNil) {
									this.revGlobalSoa = Synth.new(\revGlobalSoaA12++clsRvtypes,
										[\soaBus, soaBus, \gate, 1, \room, clsrm, \damp, clsdm,
											\a0ir, rirA12Spectrum[max((clsrv - 3), 0), 0],
											\a1ir, rirA12Spectrum[max((clsrv - 3), 0), 1],
											\a2ir, rirA12Spectrum[max((clsrv - 3), 0), 2],
											\a3ir, rirA12Spectrum[max((clsrv - 3), 0), 3],
											\a4ir, rirA12Spectrum[max((clsrv - 3), 0), 4],
											\a5ir, rirA12Spectrum[max((clsrv - 3), 0), 5],
											\a6ir, rirA12Spectrum[max((clsrv - 3), 0), 6],
											\a7ir, rirA12Spectrum[max((clsrv - 3), 0), 7],
											\a8ir, rirA12Spectrum[max((clsrv - 3), 0), 8],
											\a9ir, rirA12Spectrum[max((clsrv - 3), 0), 9],
											\a10ir, rirA12Spectrum[max((clsrv - 3), 0), 10],
											\a11ir, rirA12Spectrum[max((clsrv - 3), 0), 11]],
										this.glbRevDecGrp).register.onFree({
										if (this.revGlobalSoa.isPlaying.not) {
											this.revGlobalSoa = nil;
										};
									});
								} {
									this.revGlobalSoa.set(\gate, 1);
								};
							};

							//comment out all linear parameters
						//this.espacializador[i] = Synth.new(\espacAmb2AFormat++ln[i],

							this.espacializador[i] = Synth.new(\ATK2AFormat,
								[\inbus, mbus[i],
									\gbus, gbus, \soaBus, soaBus, \contr, clev[i],
									\insertFlag, this.insertFlag[i],
									\aFormatBusInFoa, this.aFormatBusFoa[0,i].index,
									\aFormatBusOutFoa, this.aFormatBusFoa[1,i].index,
									\aFormatBusInSoa, this.aFormatBusSoa[0,i].index,
									\aFormatBusOutSoa, this.aFormatBusSoa[1,i].index],
								this.synt[i], addAction: \addAfter).onFree({
								if (this.revGlobalSoa.notNil) {
									if (this.globSoaA12Needed(0).not) {
										this.revGlobalSoa.set(\gate, 0);
									};
								};
								if (this.revGlobalBF.notNil) {
									if (this.globBfmtNeeded(0).not) {
										this.revGlobalBF.set(\gate, 0);
									};
								};
								if (this.convertor.notNil) {
									if (convert_fuma) {
										if (this.converterNeeded(0).not) {
											this.convertor.set(\gate, 0);
										};
									};
								};
							});

						};

					} {
						//comment out all linear parameters
						//this.synt[i] = Synth.new(\playBFormatFile++ln[i], [\gbus, gbus,

						this.espacializador[i] = Synth.new(\ATK2Chowning++dstrvtypes[i],
							[\inbus, mbus[i], \gbus, gbus,
								\insertFlag, this.insertFlag[i],
								\aFormatBusInFoa, this.aFormatBusFoa[0,i].index,
								\aFormatBusOutFoa, this.aFormatBusFoa[1,i].index,
								\aFormatBusInSoa, this.aFormatBusSoa[0,i].index,
								\aFormatBusOutSoa, this.aFormatBusSoa[1,i].index,
								\contr, clev[i], \room, rm[i], \damp, dm[i],
								\wir, rirWspectrum[max(this.dstrvboxProxy[i].value - 4, 0)]],
							this.synt[i], addAction: \addAfter).onFree({
							if (this.revGlobalSoa.notNil) {
								if (this.globSoaA12Needed(0).not) {
									this.revGlobalSoa.set(\gate, 0);
								};
							};
							if (this.revGlobalBF.notNil) {
								if (this.globBfmtNeeded(0).not) {
									this.revGlobalBF.set(\gate, 0);
								};
							};
							if (this.convertor.notNil) {
								if (convert_fuma) {
									if (this.converterNeeded(0).not) {
										this.convertor.set(\gate, 0);
									};
								};
							};
						});

					};
					updatesourcevariables.value(i);

				}

				{ncanais[i] = 0; // outro tipo de arquivo, faz nada.
				};
			};

			};
			if(control.doRecord == false){
				if (guiflag) {
					{	xboxProxy[i].valueAction = xbox[i].value;
						yboxProxy[i].valueAction = ybox[i].value;
					}.defer;
				};

			};

		} {

			if ((this.scncheckProxy[i].value) || (this.hwncheckProxy[i])) {
				var x;
				x = case
				{ this.ncan[i] == 1 } {

					// comment out automatic settings, prefer sensible deffaults
					/*cboxProxy[i].valueAction = 1;
					clev[i] = 1;
					if((i == currentsource) && guiflag) {
					cslider.value = 1;
					connumbox.value = 1;
					};*/

					if (this.hwncheckProxy[i].value) {

						this.synt[i] = Synth.new(\playMonoHWBus, [\outbus, mbus[i], \busini,
							this.busini[i],
							\level, level[i]], this.playEspacGrp).onFree({this.espacializador[i].free;
							this.espacializador[i] = nil;
							this.synt[i] = nil});

					} {
						this.synt[i] = Synth.new(\playMonoSWBus, [\outbus, mbus[i],
							\busini, this.scInBus[i], // use "index" method?
							\level, level[i]], this.playEspacGrp).onFree({this.espacializador[i].free;
							this.espacializador[i] = nil;
							this.synt[i] = nil});
					};


					if (this.dstrvboxProxy[i].value == 3) { // A-fomat reverb swich

						libboxProxy[i].valueAction = 3;

						// set lib, convert and dstrv variables when stynths are lauched
						// for the tracking functions to stay relevant

						lib[i] = 3;
						dstrv[i] = 3;
						convert[i] = convert_fuma;

						if (convert_fuma) {
							if (this.convertor.notNil) {
								this.convertor.set(\gate, 1);
							} {
								this.convertor = Synth.new(\ambiConverter, [\gate, 1],
									target:this.glbRevDecGrp).onFree({
									this.convertor = nil;
								});
							};
						};

						if (clsrv > 0) {
							if (revGlobalBF.isNil) {
								this.revGlobalBF = Synth.new(\revGlobalBFormatAmb++clsRvtypes,
									[\gbfbus, gbfbus, \gate, 1, \room, clsrm, \damp, clsdm,
										\fluir, rirFLUspectrum[max((clsrv - 3), 0)],
										\frdir, rirFRDspectrum[max((clsrv - 3), 0)],
										\bldir, rirBLDspectrum[max((clsrv - 3), 0)],
										\bruir, rirBRUspectrum[max((clsrv - 3), 0)]],
									this.glbRevDecGrp).register.onFree({
									if (this.revGlobalBF.isPlaying.not) {
										this.revGlobalBF = nil;
									};
								});
							} {
								this.revGlobalBF.set(\gate, 1);
							};
						};

						if (this.maxorder > 1) {
							if (revGlobalSoa.isNil) {
								this.revGlobalSoa = Synth.new(\revGlobalSoaA12++clsRvtypes,
									[\soaBus, soaBus, \gate, 1, \room, clsrm, \damp, clsdm,
										\a0ir, rirA12Spectrum[max((clsrv - 3), 0), 0],
										\a1ir, rirA12Spectrum[max((clsrv - 3), 0), 1],
										\a2ir, rirA12Spectrum[max((clsrv - 3), 0), 2],
										\a3ir, rirA12Spectrum[max((clsrv - 3), 0), 3],
										\a4ir, rirA12Spectrum[max((clsrv - 3), 0), 4],
										\a5ir, rirA12Spectrum[max((clsrv - 3), 0), 5],
										\a6ir, rirA12Spectrum[max((clsrv - 3), 0), 6],
										\a7ir, rirA12Spectrum[max((clsrv - 3), 0), 7],
										\a8ir, rirA12Spectrum[max((clsrv - 3), 0), 8],
										\a9ir, rirA12Spectrum[max((clsrv - 3), 0), 9],
										\a10ir, rirA12Spectrum[max((clsrv - 3), 0), 10],
										\a11ir, rirA12Spectrum[max((clsrv - 3), 0), 11]],
									this.glbRevDecGrp).register.onFree({
									if (this.revGlobalSoa.isPlaying.not) {
										this.revGlobalSoa = nil;
									};
								});
							} {
								this.revGlobalSoa.set(\gate, 1);
							};
						};
						//comment out all linear parameters
						//this.espacializador[i] = Synth.new(\espacAFormatVerb++ln[i],

						this.espacializador[i] = Synth.new(\espacAFormatVerb,
							[\inbus, mbus[i],
								\soaBus, soaBus, \gbfbus, gbfbus,
								\insertFlag, this.insertFlag[i], \contr, clev[i],
								\aFormatBusInFoa, this.aFormatBusFoa[0,i].index,
								\aFormatBusOutFoa, this.aFormatBusFoa[1,i].index,
								\aFormatBusInSoa, this.aFormatBusSoa[0,i].index,
								\aFormatBusOutSoa, this.aFormatBusSoa[1,i].index],
							this.synt[i], addAction: \addAfter).onFree({
							if (this.revGlobalSoa.notNil) {
								if (this.globSoaA12Needed(0).not) {
									this.revGlobalSoa.set(\gate, 0);
								};
							};
							if (this.revGlobalBF.notNil) {
								if (this.globBfmtNeeded(0).not) {
									this.revGlobalBF.set(\gate, 0);
								};
							};
							if (this.convertor.notNil) {
								if (convert_fuma) {
									if (this.converterNeeded(0).not) {
										this.convertor.set(\gate, 0);
									};
								};
							};
						});

					} {
						// set lib, convert and dstrv variables when stynths are lauched
						// for the tracking functions to stay relevant

						case
						{ libboxProxy[i].value == 0 }
						{ convert[i] = convert_ambix;
							lib[i] = libboxProxy[i].value; }
						{ libboxProxy[i].value == 1 }
						{ convert[i] = convert_ambix;
							lib[i] = libboxProxy[i].value; }
						{ libboxProxy[i].value == 2 }
						{ convert[i] = convert_ambix;
							lib[i] = libboxProxy[i].value; }
						{ libboxProxy[i].value == 3 }
						{ convert[i] = convert_fuma;
							lib[i] = libboxProxy[i].value; }
						{ libboxProxy[i].value == 4 }
						{ convert[i] = convert_direct;
							lib[i] = libboxProxy[i].value; };

						dstrv[i] = dstrvboxProxy[i].value;

						if (convert[i]) {

							if (this.convertor.notNil) {
								this.convertor.set(\gate, 1);
							} {
								this.convertor = Synth.new(\ambiConverter, [\gate, 1],
									target:this.glbRevDecGrp).onFree({
									this.convertor = nil;
								});
							};
						};

						if (speaker_array.isNil) {

							if ((this.libboxProxy[i].value > 3) && this.nonAmbi2FuMa.isNil) {
								this.nonAmbi2FuMa = Synth.new(\nonAmbi2FuMa,
									target:this.glbRevDecGrp).onFree({
									this.nonAmbi2FuMa = nil;
								});
							};
						};

						//comment out all linear parameters
						//{this.espacializador[i] = Synth.new(\espacAmbChowning++ln[i],

						this.espacializador[i] = Synth.new(libName[i]++"Chowning"++dstrvtypes[i],
							[\inbus, mbus[i],
								\gbus, gbus,
								\insertFlag, this.insertFlag[i],
								\aFormatBusInFoa, this.aFormatBusFoa[0,i].index,
								\aFormatBusOutFoa, this.aFormatBusFoa[1,i].index,
								\aFormatBusInSoa, this.aFormatBusSoa[0,i].index,
								\aFormatBusOutSoa, this.aFormatBusSoa[1,i].index,
								\room, rm[i], \damp, dm[i], \contr, clev[i],
								\wir, rirWspectrum[max(this.dstrvboxProxy[i].value - 4, 0)]],
							this.synt[i], addAction: \addAfter).onFree({
							if (speaker_array.isNil) {
								if (this.nonAmbi2FuMaNeeded(0).not
									&& this.nonAmbi2FuMa.notNil) {
									this.nonAmbi2FuMa.free;
								};
							};
							if (this.convertor.notNil) {
								if (convert[i]) {
									if (this.converterNeeded(0).not) {
										this.convertor.set(\gate, 0);
									};
								};
							};
						});

					};

					//atualizarvariaveis.value;
					updatesourcevariables.value(i);


				}
				{ this.ncan[i] == 2 } {
					ncanais[i] = 0; // just in case!

					// comment out automatic settings, prefer sensible deffaults
					/*angle[i] = pi/2;
					if (guiflag) {
					{angnumbox.value = pi/2;}.defer;
					{angslider.value = 0.5;}.defer;
					};

					cboxProxy[i].valueAction = 1;
					clev[i] = 1;
					if((i == currentsource) && guiflag) {
					cslider.value = 1;
					connumbox.value = 1;
					};*/

					if (this.hwncheckProxy[i].value) {

						this.synt[i] = Synth.new(\playStereoHWBus, [\outbus, sbus[i], \busini,
							this.busini[i],
							\level, level[i]], this.playEspacGrp).onFree({
							this.espacializador[i].free;
							this.espacializador[i] = nil;
							this.synt[i] = nil});
					} {
						this.synt[i] = Synth.new(\playStereoSWBus, [\outbus, sbus[i],
							\busini, this.scInBus[i],
							\level, level[i]], this.playEspacGrp).onFree({
							this.espacializador[i].free;
							this.espacializador[i] = nil;
							this.synt[i] = nil});
					};


					if (this.dstrvboxProxy[i].value == 3) { // A-fomat reverb swich

						libboxProxy[i].valueAction = 3;

						// set lib, convert and dstrv variables when stynths are lauched
						// for the tracking functions to stay relevant

						lib[i] = 3;
						dstrv[i] = 3;
						convert[i] = convert_fuma;

						if (convert_fuma) {
							if (this.convertor.notNil) {
								this.convertor.set(\gate, 1);
							} {
								this.convertor = Synth.new(\ambiConverter, [\gate, 1],
									target:this.glbRevDecGrp).onFree({
									this.convertor = nil;
								});
							};
						};

						if (clsrv > 0) {
							if (revGlobalBF.isNil) {
								this.revGlobalBF = Synth.new(\revGlobalBFormatAmb++clsRvtypes,
									[\gbfbus, gbfbus, \gate, 1, \room, clsrm, \damp, clsdm,
										\fluir, rirFLUspectrum[max((clsrv - 3), 0)],
										\frdir, rirFRDspectrum[max((clsrv - 3), 0)],
										\bldir, rirBLDspectrum[max((clsrv - 3), 0)],
										\bruir, rirBRUspectrum[max((clsrv - 3), 0)]],
									this.glbRevDecGrp).register.onFree({
									if (this.revGlobalBF.isPlaying.not) {
										this.revGlobalBF = nil;
									};
								});
							} {
								this.revGlobalBF.set(\gate, 1);
							};

							if (this.maxorder > 1) {
								if (revGlobalSoa.isNil) {
									this.revGlobalSoa = Synth.new(\revGlobalSoaA12++clsRvtypes,
										[\soaBus, soaBus, \gate, 1, \room, clsrm, \damp, clsdm,
											\a0ir, rirA12Spectrum[max((clsrv - 3), 0), 0],
											\a1ir, rirA12Spectrum[max((clsrv - 3), 0), 1],
											\a2ir, rirA12Spectrum[max((clsrv - 3), 0), 2],
											\a3ir, rirA12Spectrum[max((clsrv - 3), 0), 3],
											\a4ir, rirA12Spectrum[max((clsrv - 3), 0), 4],
											\a5ir, rirA12Spectrum[max((clsrv - 3), 0), 5],
											\a6ir, rirA12Spectrum[max((clsrv - 3), 0), 6],
											\a7ir, rirA12Spectrum[max((clsrv - 3), 0), 7],
											\a8ir, rirA12Spectrum[max((clsrv - 3), 0), 8],
											\a9ir, rirA12Spectrum[max((clsrv - 3), 0), 9],
											\a10ir, rirA12Spectrum[max((clsrv - 3), 0), 10],
											\a11ir, rirA12Spectrum[max((clsrv - 3), 0), 11]],
										this.glbRevDecGrp).register.onFree({
										if (this.revGlobalSoa.isPlaying.not) {
											this.revGlobalSoa = nil;
										};
									});
								} {
									this.revGlobalSoa.set(\gate, 1);
								};
							};
						};

						//comment out all linear parameters
						//this.espacializador[i] = Synth.new(\espacEstereoAFormat++ln[i],

						this.espacializador[i] = Synth.new(\espacEstereoAFormat,
							[\inbus, sbus[i], \gbus, gbus, \angle, angle[i],
								\soaBus, soaBus, \gbfbus, gbfbus, \contr, clev[i],
								\insertFlag, this.insertFlag[i],
								\aFormatBusInFoa, this.aFormatBusFoa[0,i].index,
								\aFormatBusOutFoa, this.aFormatBusFoa[1,i].index,
								\aFormatBusInSoa, this.aFormatBusSoa[0,i].index,
								\aFormatBusOutSoa, this.aFormatBusSoa[1,i].index],
							this.synt[i], addAction: \addAfter).onFree({
							if (this.revGlobalSoa.notNil) {
								if (this.globSoaA12Needed(0).not) {
									this.revGlobalSoa.set(\gate, 0);
								};
							};
							if (this.revGlobalBF.notNil) {
								if (this.globBfmtNeeded(0).not) {
									this.revGlobalBF.set(\gate, 0);
								};
							};
							if (this.convertor.notNil) {
								if (convert_fuma) {
									if (this.converterNeeded(0).not) {
										this.convertor.set(\gate, 0);
									};
								};
							};
						});

					} {
						// set lib, convert and dstrv variables when stynths are lauched
						// for the tracking functions to stay relevant

						case
						{ libboxProxy[i].value == 0 }
						{ convert[i] = convert_ambix;
							lib[i] = libboxProxy[i].value; }
						{ libboxProxy[i].value == 1 }
						{ convert[i] = convert_ambix;
							lib[i] = libboxProxy[i].value; }
						{ libboxProxy[i].value == 2 }
						{ convert[i] = convert_ambix;
							lib[i] = libboxProxy[i].value; }
						{ libboxProxy[i].value == 3 }
						{ convert[i] = convert_fuma;
							lib[i] = libboxProxy[i].value; }
						{ libboxProxy[i].value == 4 }
						{ convert[i] = convert_direct;
							lib[i] = libboxProxy[i].value; };

						dstrv[i] = dstrvboxProxy[i].value;

						if (convert[i]) {

							if (this.convertor.notNil) {
								this.convertor.set(\gate, 1);
							} {
								this.convertor = Synth.new(\ambiConverter, [\gate, 1],
									target:this.glbRevDecGrp).onFree({
									this.convertor = nil;
								});
							};
						};

						if (this.libboxProxy[i].value > 3) {

							if (speaker_array.isNil && this.nonAmbi2FuMa.isNil) {
								this.nonAmbi2FuMa = Synth.new(\nonAmbi2FuMa,
									target:this.glbRevDecGrp).onFree({
									this.nonAmbi2FuMa = nil;
								});
							};
						};

						//comment out all linear parameters
						//this.espacializador[i] = Synth.new(\espacEstereoChowning++ln[i],

						this.espacializador[i] = Synth.new(libName[i]++"StereoChowning"++dstrvtypes[i],
							[\inbus, sbus[i],
								\gbus, gbus, \angle, angle[i],
								\insertFlag, this.insertFlag[i],
								\aFormatBusInFoa, this.aFormatBusFoa[0,i].index,
								\aFormatBusOutFoa, this.aFormatBusFoa[1,i].index,
								\aFormatBusInSoa, this.aFormatBusSoa[0,i].index,
								\aFormatBusOutSoa, this.aFormatBusSoa[1,i].index,
								\contr, clev[i], \room, rm[i], \damp, dm[i],
								\zir, rirZspectrum[max(this.dstrvboxProxy[i].value - 4, 0)]],
							this.synt[i], addAction: \addAfter).onFree({
							if (speaker_array.isNil) {
								if (this.nonAmbi2FuMaNeeded(0).not
									&& this.nonAmbi2FuMa.notNil) {
									this.nonAmbi2FuMa.free;
								};
							};
							if (this.convertor.notNil) {
								if (convert[i]) {
									if (this.converterNeeded(0).not) {
										this.convertor.set(\gate, 0);
									};
								};
							};
						});

					};
					//atualizadstrvariaveis.value;
					updatesourcevariables.value(i);

				}
				{ this.ncan[i] >= 4 } {

					// comment out automatic settings, prefer sensible deffaults
					/*cboxProxy[i].valueAction = 0;
					clev[i] = 0;
					if((i == currentsource) && guiflag) {
					cslider.value = 0;
					connumbox.value = 0;
					};*/


					libboxProxy[i].valueAction = 3;

					// set lib, convert and dstrv variables when stynths are lauched
					// for the tracking functions to stay relevant

					lib[i] = 3;
					dstrv[i] = dstrvboxProxy[i].value;
					convert[i] = convert_fuma;

					if (convert_fuma) {
						if (this.convertor.notNil) {
							this.convertor.set(\gate, 1);
						} {
							this.convertor = Synth.new(\ambiConverter, [\gate, 1],
								target:this.glbRevDecGrp).onFree({
								this.convertor = nil;
							});
						};
					};

					if (this.hwncheckProxy[i].value) {

						//comment out all linear parameters
						//this.synt[i] = Synth.new(\playBFormatHWBus++ln[i], [\gbfbus, gbfbus,

						this.synt[i] = Synth.new(\playBFormatHWBus, [\gbfbus, gbfbus,
							\outbus, mbus[i],
							\contr, clev[i], \rate, 1, \tpos, tpos, \level, level[i],
							\insertFlag, this.insertFlag[i],
							\aFormatBusInFoa, this.aFormatBusFoa[0,i].index,
							\aFormatBusOutFoa, this.aFormatBusFoa[1,i].index,
							\aFormatBusInSoa, this.aFormatBusSoa[0,i].index,
							\aFormatBusOutSoa, this.aFormatBusSoa[1,i].index,
							\busini, this.busini[i]],this.playEspacGrp).onFree({
							this.espacializador[i].free;
							this.espacializador[i] = nil;
							this.synt[i] = nil;
						});

					} {

						//comment out all linear parameters
						//this.synt[i] = Synth.new(\playBFormatSWBus++ln[i], [\gbfbus, gbfbus,

						this.synt[i] = Synth.new(\playBFormatSWBus, [\gbfbus, gbfbus,
							\outbus, mbus[i], \contr, clev[i], \rate, 1, \tpos, tpos,
							\level, level[i],
							\insertFlag, this.insertFlag[i],
							\aFormatBusInFoa, this.aFormatBusFoa[0,i].index,
							\aFormatBusOutFoa, this.aFormatBusFoa[1,i].index,
							\aFormatBusInSoa, this.aFormatBusSoa[0,i].index,
							\aFormatBusOutSoa, this.aFormatBusSoa[1,i].index,
							\busini, this.scInBus[i] ],this.playEspacGrp).onFree({
							this.espacializador[i].free;
							this.espacializador[i] = nil;
							this.synt[i] = nil;
						});
					};

					if (clsrv > 0) {
						if (revGlobalBF.isNil) {
							this.revGlobalBF = Synth.new(\revGlobalBFormatAmb++clsRvtypes,
								[\gbfbus, gbfbus, \gate, 1, \room, clsrm, \damp, clsdm,
									\fluir, rirFLUspectrum[max((clsrv - 3), 0)],
									\frdir, rirFRDspectrum[max((clsrv - 3), 0)],
									\bldir, rirBLDspectrum[max((clsrv - 3), 0)],
									\bruir, rirBRUspectrum[max((clsrv - 3), 0)]],
								this.glbRevDecGrp).register.onFree({
								if (this.revGlobalBF.isPlaying.not) {
									this.revGlobalBF = nil;
								};
							});
						} {
							this.revGlobalBF.set(\gate, 1);
						};
					};

					if (this.dstrvboxProxy[i].value == 3) { // A-fomat reverb swich

						dstrv[i] = 3;

						if (this.maxorder > 1) {
							if (revGlobalSoa.isNil) {
								this.revGlobalSoa = Synth.new(\revGlobalSoaA12++clsRvtypes,
									[\soaBus, soaBus, \gate, 1, \room, clsrm, \damp, clsdm,
										\a0ir, rirA12Spectrum[max((clsrv - 3), 0), 0],
										\a1ir, rirA12Spectrum[max((clsrv - 3), 0), 1],
										\a2ir, rirA12Spectrum[max((clsrv - 3), 0), 2],
										\a3ir, rirA12Spectrum[max((clsrv - 3), 0), 3],
										\a4ir, rirA12Spectrum[max((clsrv - 3), 0), 4],
										\a5ir, rirA12Spectrum[max((clsrv - 3), 0), 5],
										\a6ir, rirA12Spectrum[max((clsrv - 3), 0), 6],
										\a7ir, rirA12Spectrum[max((clsrv - 3), 0), 7],
										\a8ir, rirA12Spectrum[max((clsrv - 3), 0), 8],
										\a9ir, rirA12Spectrum[max((clsrv - 3), 0), 9],
										\a10ir, rirA12Spectrum[max((clsrv - 3), 0), 10],
										\a11ir, rirA12Spectrum[max((clsrv - 3), 0), 11]],
									this.glbRevDecGrp).register.onFree({
									if (this.revGlobalSoa.isPlaying.not) {
										this.revGlobalSoa = nil;
									};
								});
							} {
								this.revGlobalSoa.set(\gate, 1);
							};
						};

							//comment out all linear parameters
							//this.espacializador[i] = Synth.new(\espacAmb2AFormat++ln[i], [\inbus, mbus[i],

							this.espacializador[i] = Synth.new(\ATK2AFormat,
								[\inbus, mbus[i],
								\gbus, gbus, \soaBus, soaBus, \contr, clev[i],
								\insertFlag, this.insertFlag[i],
								\aFormatBusInFoa, this.aFormatBusFoa[0,i].index,
								\aFormatBusOutFoa, this.aFormatBusFoa[1,i].index,
								\aFormatBusInSoa, this.aFormatBusSoa[0,i].index,
								\aFormatBusOutSoa, this.aFormatBusSoa[1,i].index],
							this.synt[i], addAction: \addAfter).onFree({
							if (this.revGlobalSoa.notNil) {
								if (this.globSoaA12Needed(0).not) {
									this.revGlobalSoa.set(\gate, 0);
								};
							};
							if (this.revGlobalBF.notNil) {
								if (this.globBfmtNeeded(0).not) {
									this.revGlobalBF.set(\gate, 0);
								};
							};
							if (this.convertor.notNil) {
								if (convert_fuma) {
									if (this.converterNeeded(0).not) {
										this.convertor.set(\gate, 0);
									};
								};
							};
						});

					} {

						//comment out all linear parameters
						//this.espacializador[i] = Synth.new(\espacAmb2Chowning++ln[i], [\inbus, mbus[i],

						this.espacializador[i] = Synth.new(\ATK2Chowning++dstrvtypes[i],
							[\inbus, mbus[i],
								\gbus, gbus,
								\insertFlag, this.insertFlag[i],
								\aFormatBusInFoa, this.aFormatBusFoa[0,i].index,
								\aFormatBusOutFoa, this.aFormatBusFoa[1,i].index,
								\aFormatBusInSoa, this.aFormatBusSoa[0,i].index,
								\aFormatBusOutSoa, this.aFormatBusSoa[1,i].index,
								\contr, clev[i], \room, rm[i], \damp, dm[i],
								\wir, rirWspectrum[max(this.dstrvboxProxy[i].value - 4, 0)]],
							this.synt[i], addAction: \addAfter).onFree({
							if (this.revGlobalSoa.notNil) {
								if (this.globSoaA12Needed(0).not) {
									this.revGlobalSoa.set(\gate, 0);
								};
							};
							if (this.revGlobalBF.notNil) {
								if (this.globBfmtNeeded(0).not) {
									this.revGlobalBF.set(\gate, 0);
								};
							};
							if (this.convertor.notNil) {
								if (convert_fuma) {
									if (this.converterNeeded(0).not) {
										this.convertor.set(\gate, 0);
									};
								};
							};
						});

					};

					//atualizarvariaveis.value;
					updatesourcevariables.value(i);

				};

			};

		};

	}

	loadNonAutomationData { arg path;
		var libf, loopedf, aformatrevf, hwinf, scinf,
		//comment out all linear parameters
		//linearf,
		spreadf, diffusef, ncanf, businif, stcheckf, filenames;
		//("THE PATH IS " ++ path ++ "/filenames.txt").postln;
		filenames = File((path ++ "/filenames.txt").standardizePath,"r");

		libf = File((path ++ "/lib.txt").standardizePath,"r");
		loopedf = File((path ++ "/looped.txt").standardizePath,"r");
		aformatrevf = File((path ++ "/aformatrev.txt").standardizePath,"r");
		hwinf = File((path ++ "/hwin.txt").standardizePath,"r");
		scinf = File((path ++ "/scin.txt").standardizePath,"r");
		//comment out all linear parameters
		//linearf = File((path ++ "/linear.txt").standardizePath,"r");
		spreadf = File((path ++ "/spread.txt").standardizePath,"r");
		diffusef = File((path ++ "/diffuse.txt").standardizePath,"r");
		ncanf = File((path ++ "/ncan.txt").standardizePath,"r");
		businif = File((path ++ "/busini.txt").standardizePath,"r");
		stcheckf = File((path ++ "/stcheck.txt").standardizePath,"r");


		//{	("BEFORE ACTION - stream = " ++ stcheckProxy[0].value).postln;}.defer;


		nfontes.do { arg i;
			var line = filenames.getLine(1024);
			if(line!="NULL"){this.tfieldProxy[i].valueAction = line};
		};

		nfontes.do { arg i;
			var line = libf.getLine(1024);
			this.libboxProxy[i].valueAction = line.asInt;
		};

		nfontes.do { arg i;
			var line = loopedf.getLine(1024);
			var flag;
			if (line == "true") {flag = true;} {flag = false;};
			this.lpcheckProxy[i].valueAction = flag;
			//lp[i] 0 or 1
		};

		nfontes.do { arg i;
			var line = aformatrevf.getLine(1024);
			this.dstrvboxProxy[i].valueAction = line.asInt;
			//dstrv[i] 0 or 1
		};

		//comment out all linear parameters
/*
		nfontes.do { arg i;
			var line = linearf.getLine(1024);
			var flag;
			if (line == "true") {flag = true;} {flag = false;};
			this.lncheckProxy[i].valueAction = flag;
		};
*/
		nfontes.do { arg i;
			var line = spreadf.getLine(1024);
			var flag;
			if (line == "true") {flag = true;} {flag = false;};
			this.spcheckProxy[i].valueAction = flag;
		};

		nfontes.do { arg i;
			var line = diffusef.getLine(1024);
			var flag;
			if (line == "true") {flag = true;} {flag = false;};
			this.dfcheckProxy[i].valueAction = flag;
		};
		nfontes.do { arg i;
			var line = ncanf.getLine(1024);
			this.ncanboxProxy[i].valueAction = line.asInt;
		};

		nfontes.do { arg i;
			var line = businif.getLine(1024);
			this.businiboxProxy[i].valueAction = line.asInt;
		};

		nfontes.do { arg i;
			var line = stcheckf.getLine(1024);
			var flag;
			if (line == "true") {flag = true;} {flag = false;};
			this.stcheckProxy[i].valueAction = flag;
		};

//		nfontes.do { arg i;
//	var line = hwinf.getLine(1024);
//	this.hwncheckProxy[i].valueAction = line;
// };
        nfontes.do { arg i;
			var line = hwinf.getLine(1024);
			var flag;
			if (line == "true") {flag = true;} {flag = false;};

			//("line = " ++ line.asString).postln;

			//this.hwncheckProxy[i].valueAction = line.booleanValue;
			// why, why, why is this asBoolean necessary!
			this.hwncheckProxy[i].valueAction = flag;
		};


		nfontes.do { arg i;
			var line = scinf.getLine(1024);
			var flag;
			if (line == "true") {flag = true;} {flag = false;};
			this.scncheckProxy[i].value = flag;
		};


		filenames.close;

		libf.close;
		loopedf.close;
		aformatrevf.close;
		hwinf.close;
		scinf.close;
		//comment out all linear parameters
		//linearf.close;
		spreadf.close;
		diffusef.close;
		ncanf.close;
		businif.close;
		stcheckf.close;

		//"RARARARARAR".postln;

		// delay necessary here because streamdisks take some time to register after control.load
		//Routine {

		//1.wait;
		this.nfontes.do { arg i;

			var newpath = this.tfieldProxy[i].value;
			//	server.sync;
			if (this.streamdisk[i].not && (this.tfieldProxy[i].value != "")) {
				i.postln;
				newpath.postln;
				this.sombuf[i] = Buffer.read(server, newpath, action: {arg buf;
					"Loaded file".postln;
				});
			};

		};
		//	}.play;
		//	this.watcher.play;

	}

	// Automation call-back doesn' seem to work with no GUI, so these duplicate
	// control.onPlay, etc.
	blindControlPlay {
		var startTime;
		this.nfontes.do { arg i;
			this.firstTime[i]=true;
		};

		if(control.now < 0)
		{
			startTime = 0
		}
		{
			startTime = control.now
		};
		this.isPlay = true;
		this.control.play;

		//runTriggers.value;
	}

	blindControlStop {
		this.control.stop;
		runStops.value;
		this.nfontes.do { arg i;
			// if sound is currently being "tested", don't switch off on stop
			// leave that for user
			if (testado[i] == false) {
				this.synt[i].free; // error check
			};
			//	this.espacializador[i].free;
		};
		this.isPlay = false;
	}



	free {

		this.control.quit;
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
			this.mbus[x].free;
			this.sbus[x].free;
			//	bfbus.[x].free;
			this.sombuf[x].free;
			this.streambuf[x].free;
			this.synt[x].free;
			this.scInBus[x].free;
			//		kespac[x].stop;
		};
		MIDIIn.removeFuncFrom(\sysex, sysex);
		//MIDIIn.disconnect;
		if(this.revGlobal.notNil){
			this.revGlobal.free;
		};
		if(this.revGlobalBF.notNil){
			this.revGlobalBF.free;
		};
		if(this.revGlobalSoa.notNil){
			this.revGlobalSoa.free;
		};

		if(this.globDec.notNil){
			this.globDec.free
		};


		this.gbus.free;
		this.gbfbus.free;

		if(this.rirWspectrum.notNil){
			this.rirWspectrum.free; };
		if(this.rirXspectrum.notNil){
			this.rirXspectrum.free;};
		if(this.rirYspectrum.notNil){
			this.rirYspectrum.free;};
		if(this.rirZspectrum.notNil){
			this.rirZspectrum.free;};
		if(this.rirFRDspectrum.notNil){
			this.rirFRDspectrum.free;};
		if(this.rirBLDspectrum.notNil){
			this.rirBLDspectrum.free;};
		if(this.rirFLUspectrum.notNil){
			this.rirFLUspectrum.free;};
		if(this.rirBRUspectrum.notNil){
			this.rirBRUspectrum.free;};
		this.soaBus.free;
		if (this.maxorder > 1) {
			12.do { arg i;
				if(rirA12Spectrum[i].notNil){
					rirA12Spectrum[i].free;};
			};
		};
		foaEncoderOmni.free;
		foaEncoderSpread.free;
		foaEncoderDiffuse.free;
		b2a.free;
		a2b.free;
	}


    gui {

		//arg dur = 120;
		var fonte, dist,
		itensdemenu,
		//gbus, gbfbus,
		//azimuth,
		event, brec, bplay, bload, bstream, bnodes,
		//funcs,
		//lastAutomation = nil,
		//	loopcheck,
		//lpcheck,

		//spreadcheck,
		//spcheck,
		//sp,
		//diffusecheck,
		//dfcheck,
		//df,
		//dstReverbox,
		//rvbox,

		//lincheck,
		//lncheck,

		//hwInCheck,
		//hwncheck,
		//hwn, scInCheck,
		//scncheck,
		//scn,
		dopcheque2,
		//glev,
		//llev,
		//volnumbox,
		//ncannumbox, busininumbox, // for streams. ncan = number of channels (1, 2 or 4)
		// busini = initial bus number in range starting with "0"
		//ncanbox, businibox,
		mouseButton, dragStartScreen,
		//novoplot,
		//runTriggers, runStops, runTrigger, runStop,

		//dopnumbox,
		//volslider,
		//dirnumbox, dirslider,
		conslider,

		//a1but, a2but, a3but, a4but, a5but, // variable
		// data windows representation of a1but etc (ie. as checkbox)
		// check box for streamed from disk audio
		bsalvar, bcarregar, bsnap, bdados, baux,
		//lastx, lasty, lastz,
		//gslider,
		//gnumbox, lslider, lnumbox,
		brecaudio,
		blipcheck,
		//tocar,
		//isPlay = false, isRec,
		//atualizarvariaveis, updateSynthInArgs,

		//auxslider1, auxslider2, auxslider3, auxslider4, auxslider5, // aux sliders in control window
		//auxbutton1, auxbutton2, auxbutton3, auxbutton4, auxbutton5, // aux sliders in control window

		//rnumbox, rslider,
		//znumbox, zslider,
		//zlev,
		zAxis,
		bmark1, bmark2,

		dialView,
		autoView,
		orientView,

		//rlev,
		//dlev,
		//dplev, dpslider,
		cnumbox,
		//aux1numbox, aux2numbox, aux3numbox, aux4numbox, aux5numbox,
		zSliderHeight = this.height * 2 / 3;
		dragStartScreen = Point.new;
		//dragStartMap = Point.new;

		//////// Huge lot declarations removed //////////


		////////////////////////////////////////////////


		//this.offsetHeading(this.offsetheading);
		//("headingoffset variable = " ++ this.offsetheading).postln;






		// Note there is an extreme amount repetition occurring here. See the calling function. fix

		win = Window.new("Mosca", Rect(0, this.width, this.width, this.height)).front;

		dialView = UserView(win, Rect(this.width - 190, 10, 180, 80));

		novoplot = {

			{
				win.drawFunc = {

					Pen.fillColor = Color(0.6,0.8,0.8);
					Pen.addArc(this.halfwidth@this.halfheight, this.halfheight, 0, 2pi);
					Pen.fill;

					this.nfontes.do { |i|
						{sprite[i, 0] = this.halfwidth + (this.cartval[i].x * this.halfheight)}.defer;
						{sprite[i, 1] = this.halfheight - (this.cartval[i].y * this.halfheight)}.defer;
						Pen.fillColor = Color(0.8,0.2,0.9);
						Pen.addArc(sprite[i, 0]@sprite[i, 1], 20, 0, 2pi);
						Pen.fill;
						(i + 1).asString.drawCenteredIn(Rect(sprite[i, 0] - 10,
							sprite[i, 1] - 10, 20, 20),
						Font.default, Color.white);
					};

					Pen.fillColor = Color.gray(0, 0.5);
					Pen.addArc(this.halfwidth@this.halfheight, 20, 0, 2pi);
					Pen.fill;
				};

			}.defer;

			{win.refresh}.defer;
		};

		/*novoplot = {
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
						(ind + 1).asString.drawCenteredIn(Rect(sprite[ind, 0] - 10,
							sprite[ind, 1] - 10, 20, 20),
						Font.default, Color.white);
					};
					Pen.fillColor = Color.gray(0, 0.5);
					Pen.addArc(this.halfwidth@this.halfwidth, 20, 0, 2pi);
					Pen.fill;
				}
			}.defer;
			{ win.refresh; }.defer;
		};*/

		// fonte = Point.new; // apparently unused
		wdados = Window.new("Data", Rect(this.width, 0, 955, (this.nfontes*20)+60 ), scroll: true);
		wdados.userCanClose = false;
		wdados.alwaysOnTop = true;

		bdados = Button(dialView, Rect(0, 0, 90, 20))
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


		baux = Button(dialView, Rect(0, 20, 90, 20))
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
			a1check[currentsource].valueAction = 1;
		})
		.action_({ arg butt, mod;
			//a.release(0.3);
			a1check[currentsource].valueAction = 0;
		});

		auxbutton2 = Button(waux, Rect(80, 210, 20, 20))
		.states_([
			["2", Color.black, Color.gray]
		])
		.mouseDownAction_({
			//a = {EnvGen.kr(Env.adsr, doneAction:2) * SinOsc.ar(440, 0, 0.4)}.play;
			a2check[currentsource].valueAction = 1;
		})
		.action_({ arg butt, mod;
			//a.release(0.3);
			a2check[currentsource].valueAction = 0;
		});
		auxbutton3 = Button(waux, Rect(120, 210, 20, 20))
		.states_([
			["3", Color.black, Color.gray]
		])
		.mouseDownAction_({
			//a = {EnvGen.kr(Env.adsr, doneAction:2) * SinOsc.ar(440, 0, 0.4)}.play;
			a3check[currentsource].valueAction = 1;
		})
		.action_({ arg butt, mod;
			//a.release(0.3);
			a3check[currentsource].valueAction = 0;
		});
		auxbutton4 = Button(waux, Rect(160, 210, 20, 20))
		.states_([
			["4", Color.black, Color.gray]
		])
		.mouseDownAction_({
			//a = {EnvGen.kr(Env.adsr, doneAction:2) * SinOsc.ar(440, 0, 0.4)}.play;
			a4check[currentsource].valueAction = 1;
		})
		.action_({ arg butt, mod;
			//a.release(0.3);
			a4check[currentsource].valueAction = 0;
		});
		auxbutton5 = Button(waux, Rect(200, 210, 20, 20))
		.states_([
			["5", Color.black, Color.gray]
		])
		.mouseDownAction_({
			//a = {EnvGen.kr(Env.adsr, doneAction:2) * SinOsc.ar(440, 0, 0.4)}.play;
			a5check[currentsource].valueAction = 1;
		})
		.action_({ arg butt, mod;
			//a.release(0.3);
			a5check[currentsource].valueAction = 0;
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
			a1box[currentsource].valueAction = num.value;
			//this.aux1[currentsource] = num.value;
			auxslider1.value = num.value;
		};

		auxslider1.action = {arg num;
			a1box[currentsource].valueAction = num.value;
			aux1numbox.value = num.value;
		};
		auxslider2.action = {arg num;
			a2box[currentsource].valueAction = num.value;
			aux2numbox.value = num.value;
		};
		auxslider3.action = {arg num;
			a3box[currentsource].valueAction = num.value;
			aux3numbox.value = num.value;
		};
		auxslider4.action = {arg num;
			a4box[currentsource].valueAction = num.value;
			aux4numbox.value = num.value;
		};
		auxslider5.action = {arg num;
			a5box[currentsource].valueAction = num.value;
			aux5numbox.value = num.value;
		};





		btestar = Button(dialView, Rect(90, 40, 90, 20))
		.states_([
			["audition", Color.black, Color.white],
			["stop", Color.white, Color.red]
		])
		.action_({ arg but;
			if (this.ossiaaud.isNil) {
				{ if(isPlay.not) {
					if(but.value == 1) {
						this.firstTime[currentsource] = true;
						//testado[currentsource] = true;
						//runTrigger.value(currentsource); - watcher does this now
						//tocar.value(currentsource, 0); // needed only by SC input
						//- and probably by HW - causes duplicates with file
						// as file playback is handled by the "watcher" routine
						testado[currentsource] = true;
					} {

						//testado[currentsource] = false;
						runStop.value(currentsource);
						this.synt[currentsource].free;
						this.synt[currentsource] = nil;
						testado[currentsource] = false;
						"stopping!".postln;
					};
				} {
					but.value = 0;
				}
				}.defer;
			} {
				if(but.value == 1)
				{
					this.ossiaaud[currentsource].v_(true);
				} {
					this.ossiaaud[currentsource].v_(false);
				}
			};
		});


		brecaudio = Button(dialView, Rect(0, 60, 90, 20))
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

		blipcheck = CheckBox(dialView, Rect(95, 53, 60, 40), "blips").action_({ arg butt;
			if(butt.value) {
				//"Looping transport".postln;
				//this.autoloopval = true;
			} {
				//		this.autoloopval = false;
			};

		});


		autoView = UserView(win, Rect(10, this.width - 45, 325, 40));

		// save automation - adapted from chooseDirectoryDialog in AutomationGui.sc

		bsalvar = Button(autoView, Rect(0, 20, 80, 20))
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

			/*	var loopedf = File((prjDr ++ "/auto/looped.txt").standardizePath,"w");
				var aformatrevf = File((prjDr ++ "/auto/aformatrev.txt").standardizePath,"w");
				var hwinf = File((prjDr ++ "/auto/hwin.txt").standardizePath,"w");
				var scinf = File((prjDr ++ "/auto/scin.txt").standardizePath,"w");
				var linearf = File((prjDr ++ "/auto/linear.txt").standardizePath,"w");
				var spreadf = File((prjDr ++ "/auto/spread.txt").standardizePath,"w");
				var diffusef = File((prjDr ++ "/auto/diffuse.txt").standardizePath,"w");
			*/
			var libf, loopedf, aformatrevf, hwinf, scinf,
			//comment out all linear parameters
			//linearf,
			spreadf, diffusef, ncanf,
			businif, stcheckf;

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
            textField = GUI.textField.new(dwin, Rect(0, 0, bounds.width, bounds.height));
            textField.value = preset;
            textField.action = {
                success = true;
                onSuccess.value(textField.value);
                dwin.close;

				("FILE IS " ++ textField.value ++ "/filenames.txt").postln;
				("mkdir -p" + textField.value).systemCmd;
				filenames = File((textField.value ++ "/filenames.txt").standardizePath,"w");

				libf = File((textField.value ++ "/lib.txt").standardizePath,"w");
				loopedf = File((textField.value ++ "/looped.txt").standardizePath,"w");
				aformatrevf = File((textField.value ++ "/aformatrev.txt").standardizePath,"w");
				hwinf = File((textField.value ++ "/hwin.txt").standardizePath,"w");
				scinf = File((textField.value ++ "/scin.txt").standardizePath,"w");
				//comment out all linear parameters
				//linearf = File((textField.value ++ "/linear.txt").standardizePath,"w");
				spreadf = File((textField.value ++ "/spread.txt").standardizePath,"w");
				diffusef = File((textField.value ++ "/diffuse.txt").standardizePath,"w");
				ncanf = File((textField.value ++ "/ncan.txt").standardizePath,"w");
				businif = File((textField.value ++ "/busini.txt").standardizePath,"w");
				stcheckf = File((textField.value ++ "/stcheck.txt").standardizePath,"w");


				nfontes.do { arg i;
					if(this.tfieldProxy[i].value != "") {
						filenames.write(this.tfieldProxy[i].value ++ "\n")
					} {
						filenames.write("NULL\n")
					};

					libf.write(this.libbox[i].value.asString ++ "\n");
					loopedf.write(this.lpcheck[i].value.asString ++ "\n");
					aformatrevf.write(this.dstrvbox[i].value.asString ++ "\n");
					hwinf.write(this.hwncheck[i].value.asString ++ "\n");
					scinf.write(this.scncheck[i].value.asString ++ "\n");
					//comment out all linear parameters
					//linearf.write(this.lncheck[i].value.asString ++ "\n");
					spreadf.write(this.spcheck[i].value.asString ++ "\n");
					diffusef.write(this.dfcheck[i].value.asString ++ "\n");
					ncanf.write(this.ncanbox[i].value.asString ++ "\n");
					businif.write(this.businibox[i].value.asString ++ "\n");
					stcheckf.write(this.stcheck[i].value.asString ++ "\n");

					// REMEMBER AUX?

				};
				filenames.close;

				////////

				libf.close;
				loopedf.close;
				aformatrevf.close;
				hwinf.close;
				scinf.close;
				//comment out all linear parameters
				//linearf.close;
				spreadf.close;
				diffusef.close;
				ncanf.close;
				businif.close;
				stcheckf.close;

				///////


				control.save(textField.value);
				this.lastAutomation = textField.value;

            };
            dwin.front;



		});

		// load automation - adapted from chooseDirectoryDialog in AutomationGui.sc

		bcarregar = Button(autoView, Rect(80, 20, 80, 20))
		.states_([
			["load auto", Color.black, Color.white],
		])
		.action_({
			var title="Select Automation directory", onSuccess, onFailure=nil,
			preset=nil, bounds,  dwin, textField, success=false;

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
					"Aborted load!".postln;
                };
            };
            textField = GUI.textField.new(dwin, Rect(0, 0, bounds.width, bounds.height));
            textField.value = preset;
            textField.action = {
                success = true;
                onSuccess.value(textField.value);
                dwin.close;
				control.load(textField.value);
				//control.seek;
				this.lastAutomation = textField.value;
				this.loadNonAutomationData(textField.value);

			};
			dwin.front;




		});


		/*
			bsnap = Button(win, Rect(170, this.width - 40, 25, 20))
			.states_([
			["[ô]", Color.black, Color.white],
			])
			.action_({ arg but;
			if(control.now>0) {
			control.seek; // go to 0.0
			"Snapshot: Transport must be at zero seconds. Please try again.".postln;
			} {
			control.snapshot;  // only take snapshot at 0.0
			"Snapshot taken".postln;
			}

			});
		*/

		// no longer neded since win.view.onResize method is evaluated on init
		/*this.win.drawFunc = {
			//paint origin
			Pen.fillColor = Color(0.6,0.8,0.8);
			Pen.addArc(this.halfwidth@this.halfheight, this.halfheight, 0, 2pi);
			Pen.fill;


			Pen.fillColor = Color.gray(0, 0.5);
			Pen.addArc(this.halfwidth@this.halfheight, 20, 0, 2pi);
			Pen.fill;

			//	Pen.width = 10;
		};*/

		// seleção de fontes
		itensdemenu = Array.newClear(this.nfontes);
		this.nfontes.do { arg i;
			itensdemenu[i] = "Source " ++ (i + 1).asString;
		};

		m = PopUpMenu(win,Rect(10, 10, 150, 20));
		m.items = itensdemenu;
		m.action = { arg menu;
			currentsource = menu.value;

			libnumbox.value = this.libboxProxy[currentsource].value;

			dstReverbox.value = this.dstrvboxProxy[currentsource].value;

			if(lp[currentsource] == 1){loopcheck.value = true}{loopcheck.value = false};

			if(sp[currentsource] == 1){spreadcheck.value = true}{spreadcheck.value = false};
			if(df[currentsource] == 1){diffusecheck.value = true}{diffusecheck.value = false};

			//comment out all linear parameters
			//if(ln[currentsource] == "_linear"){lincheck.value = true}{lincheck.value = false};

			if(hwn[currentsource] == 1){hwInCheck.value = true}{hwInCheck.value = false};
			if(scn[currentsource] == 1){scInCheck.value = true}{scInCheck.value = false};

			angnumbox.value = angle[currentsource];
			angslider.value = angle[currentsource] / pi;
			volnumbox.value = level[currentsource];
			dopnumbox.value = dplev[currentsource];
			volslider.value = level[currentsource];
			gnumbox.value = glev[currentsource];
			gslider.value = glev[currentsource];
			lnumbox.value = llev[currentsource];
			lslider.value = llev[currentsource];
			rmslider.value = rm[currentsource];
			rmnumbox.value = rm[currentsource];
			dmslider.value = dm[currentsource];
			dmnumbox.value = dm[currentsource];
			rslider.value = (rlev[currentsource] + pi) / 2pi;
			rnumbox.value = rlev[currentsource];
			dirslider.value = dlev[currentsource] / (pi/2);
			dirnumbox.value = dlev[currentsource];
			cslider.value = clev[currentsource];
			zslider.value = (zlev[currentsource] + 1) * 0.5;
			znumbox.value = zlev[currentsource];

			dpslider.value = dplev[currentsource];
			connumbox.value = clev[currentsource];

			ncannumbox.value = this.ncan[currentsource];
			busininumbox.value = this.busini[currentsource];

			auxslider1.value = this.aux1[currentsource];
			aux1numbox.value = this.aux1[currentsource];
			auxslider2.value = this.aux2[currentsource];
			aux2numbox.value = this.aux2[currentsource];
			auxslider3.value = this.aux3[currentsource];
			aux3numbox.value = this.aux3[currentsource];
			auxslider4.value = this.aux4[currentsource];
			aux4numbox.value = this.aux4[currentsource];
			auxslider5.value = this.aux5[currentsource];
			aux5numbox.value = this.aux5[currentsource];

			if(testado[currentsource]) {  // don't change button if we are playing via automation
				// only if it is being played/streamed manually
				if (this.synt[currentsource] == nil){
					btestar.value = 0;
				} {
					btestar.value = 1;
				};
			} {
				btestar.value = 0;
			};
		};

		//offset = 60;

		//comment out all linear parameters
/*
		lincheck = CheckBox( win, Rect(184, 10, 180, 20), "Linear intensity (ATK)").action_({ arg butt;
			{this.lncheck[currentsource].valueAction = butt.value;}.defer;
		});
		lincheck.value = false;
*/

		hwInCheck = CheckBox( win, Rect(10, 30, 100, 20), "HW-in").action_({ arg butt;
			{this.hwncheck[currentsource].valueAction = butt.value;}.defer;
			if (hwInCheck.value && scInCheck.value) {
			};
		});

		scInCheck = CheckBox( win, Rect(90, 30, 60, 20), "SC-in").action_({ arg butt;
			{this.scncheck[currentsource].valueAction = butt.value;}.defer;
			if (scInCheck.value && hwInCheck.value) {
			};
		});

		loopcheck = CheckBox( win, Rect(163, 30, 80, 20), "Loop").action_({ arg butt;
			{this.lpcheck[currentsource].valueAction = butt.value;}.defer;
		});
		loopcheck.value = false;


		/////////////////////////////////////////////////////////


		textbuf = StaticText(win, Rect(55, 50, 200, 20));
		textbuf.string = "No. of chans. (HW & SC-in)";
		ncannumbox = NumberBox(win, Rect(10, 50, 40, 20));
		ncannumbox.value = 0;
		ncannumbox.clipHi = 4;
		ncannumbox.clipLo = 0;
		ncannumbox.align = \center;
		ncannumbox.action = {arg num;

			{this.ncanbox[currentsource].valueAction = num.value;}.defer;
			this.ncan[currentsource] = num.value;

		};


		/////////////////////////////////////////////////////////


		textbuf = StaticText(win, Rect(55, 70, 240, 20));
		textbuf.string = "Start Bus (HW-in)";
		busininumbox = NumberBox(win, Rect(10, 70, 40, 20));
		busininumbox.value = 0;
		busininumbox.clipLo = 0;
		busininumbox.align = \center;
		busininumbox.action = {arg num;
			{this.businibox[currentsource].valueAction = num.value;}.defer;
			this.busini[currentsource] = num.value;
		};


		/////////////////////////////////////////////////////////


		textbuf = StaticText(win, Rect(163, 90, 240, 20));
		textbuf.string = "Library";
		libnumbox = PopUpMenu( win, Rect(10, 90, 150, 20));
		libnumbox.items = ["ambitools               (ACN SN3D)",
			"HoaLib                   (mono & stereo)",
			"ADTB                      (mono & stereo)",
			"ATK                        (FuMa)",
			"VBAP                      (mono & stereo)"];
		libnumbox.action_({ arg num;
			{this.libbox[currentsource].valueAction = num.value;}.defer;
		});
		libnumbox.value = 3;


		/////////////////////////////////////////////////////////


		zAxis = StaticText(win, Rect(this.width - 80, this.halfwidth - 10, 90, 20));
		zAxis.string = "Z-Axis";
		znumbox = NumberBox(win, Rect(this.width - 45, ((this.width - zSliderHeight) / 2)
			+ zSliderHeight, 40, 20));
		znumbox.value = 0;
		znumbox.clipHi = 1;
		znumbox.clipLo = -1;
		znumbox.step_(00.1);
		znumbox.scroll_step_(0.01);
		znumbox.align = \center;
		znumbox.action = {arg num;
			{zbox[currentsource].valueAction = num.value;}.defer;
			if(ncanais[currentsource]==2)   {
				this.espacializador[currentsource].set(\elev, num.value);
				this.setSynths(currentsource, \elev, num.value);
				zlev[currentsource] = num.value;
			};
		};


		zslider = Slider.new(win, Rect(this.width - 35, ((this.width - zSliderHeight) / 2),
			20, zSliderHeight));
		zslider.value = 0.5;
		zslider.action = {arg num;
			{znumbox.value = (0.5 - num.value) * -2;}.defer;
			{zbox[currentsource].valueAction = znumbox.value;}.defer;
			{zlev[currentsource] = znumbox.value;}.defer;


		};


		////////////////////////////// Orientation //////////////


		//if (this.serport.notNil) { //comment out serial port prerequisit

		    orientView = UserView(win, Rect(this.width - 265, this.height - 85, 265, 100));

			this.headingnumbox = NumberBox(orientView, Rect(220, 20, 40, 20));
				headingnumbox.align = \center;
			this.rollnumbox = NumberBox(orientView, Rect(220, 40, 40, 20));
				rollnumbox.align = \center;
			this.pitchnumbox = NumberBox(orientView, Rect(220, 60, 40, 20));
		        pitchnumbox.align = \center;

			this.headingnumbox.action = {arg num;
				this.headingnumboxProxy.valueAction = num.value;
			};


			this.rollnumbox.action = {arg num;
				this.rollnumboxProxy.valueAction = num.value;
			};

			this.pitchnumbox.action = {arg num;
				this.pitchnumboxProxy.valueAction = num.value;
			};

			textbuf = StaticText(orientView, Rect(205, 20, 12, 20));
			textbuf.string = "H:";
			textbuf = StaticText(orientView, Rect(205, 40, 10, 22));
			textbuf.string = "R:";
			textbuf = StaticText(orientView, Rect(205, 60, 10, 22));
			textbuf.string = "P:";


			textbuf = StaticText(orientView, Rect(217, 0, 45, 20));
			textbuf.string = "Orient.";

		//}; //comment out the prerequisit for the serial port


		////////////////////////////////////////////////////////////


		textbuf = StaticText(win, Rect(163, 110, 50, 20));
		textbuf.string = "Level";
		volnumbox = NumberBox(win, Rect(10, 110, 40, 20));
		volnumbox.value = 0;
		volnumbox.clipHi = pi;
		volnumbox.clipLo = 0;
		volnumbox.step_(0.01);
		volnumbox.scroll_step_(0.01);
		volnumbox.align = \center;
		volnumbox.action = {arg num;
			{vbox[currentsource].valueAction = num.value;}.defer;

		};
		volslider = Slider.new(win, Rect(50, 110, 110, 20));
		volslider.value = 0;
		volslider.action = {arg num;
			{vbox[currentsource].valueAction = num.value;}.defer;
		};


		///////////////////////////////////////////////////////////////


		textbuf= StaticText(win, Rect(163, 130, 120, 20));
		textbuf.string = "Doppler amount";
		dopnumbox = NumberBox(win, Rect(10, 130, 40, 20));
		dopnumbox.value = 0;
		dopnumbox.clipHi = 1;
		dopnumbox.clipLo = 0;
		dopnumbox.step_(0.01);
		dopnumbox.scroll_step_(0.01);
		dopnumbox.align = \center;
		dopnumbox.action = {arg num;
			{dpbox[currentsource].valueAction = num.value;}.defer;

		};
		// stepsize?
		dpslider = Slider.new(win, Rect(50, 130, 110, 20));
		dpslider.value = 0;
		dpslider.action = {arg num;
			{dpbox[currentsource].valueAction = num.value;}.defer;
			{dopnumbox.value = num.value;}.defer;
		};


		/////////////////////////////////////////////////////////////////////////


		textbuf = StaticText(orientView, Rect(0, 0, 150, 20));
		textbuf.string = "Cls./Afmt. Reverb";
		clsReverbox = PopUpMenu(orientView, Rect(0, 20, 130, 20));
		clsReverbox.items = ["no-reverb",
			"freeverb",
			"allpass"] ++ rirList;
		// add the list of impule response if one is provided by rirBank

		clsReverbox.action_({ arg num;
			//{
				this.clsrvboxProxy.valueAction = num.value;
				//this.rvbox.valueAction = num.value;

			//}.defer;
		});
		clsReverbox.value = 0;


		/////////////////////////////////////////////////////////////////////////



		textbuf = StaticText(win, Rect(163, 150, 150, 20));
		textbuf.string = "Cls./Afmt. amount";
		gnumbox = NumberBox(win, Rect(10, 150, 40, 20));
		gnumbox.value = 0;
		gnumbox.clipHi = 1;
		gnumbox.clipLo = 0;
		gnumbox.step_(0.01);
		gnumbox.scroll_step_(0.01);
		gnumbox.align = \center;
		gnumbox.action = {arg num;
			{gbox[currentsource].valueAction = num.value;}.defer;

		};
		// stepsize?
		gslider = Slider.new(win, Rect(50, 150, 110, 20));
		gslider.value = 0;
		gslider.action = {arg num;
			{gbox[currentsource].valueAction = num.value;}.defer;
		};


		/////////////////////////////////////////////////////////////////////////


		textbuf = StaticText(orientView, Rect(43, 40, 150, 20));
		textbuf.string = "room/delay";
		clsrmnumbox = NumberBox(orientView, Rect(0, 40, 40, 20));
		clsrmnumbox.value = 0.5;
		clsrmnumbox.clipHi = 1;
		clsrmnumbox.clipLo = 0;
		clsrmnumbox.step_(0.01);
		clsrmnumbox.scroll_step_(0.01);
		clsrmnumbox.align = \center;
		clsrmnumbox.action = {arg num;
			{clsrmboxProxy.valueAction = num.value;}.defer;

		};
		// stepsize?
		/*clsrmslider = Slider.new(win, Rect(50, 190, 110, 20));
		clsrmslider.value = 0.5;
		clsrmslider.action = {arg num;
			{clsrmboxProxy.valueAction = num.value;}.defer;
		};*/


		/////////////////////////////////////////////////////////////////////////


		textbuf = StaticText(orientView, Rect(43, 60, 150, 20));
		textbuf.string = "damp/decay";
		clsdmnumbox = NumberBox(orientView, Rect(0, 60, 40, 20));
		clsdmnumbox.value = 0.5;
		clsdmnumbox.clipHi = 1;
		clsdmnumbox.clipLo = 0;
		clsdmnumbox.step_(0.01);
		clsdmnumbox.scroll_step_(0.01);
		clsdmnumbox.align = \center;
		clsdmnumbox.action = {arg num;
			{clsdmboxProxy.valueAction = num.value;}.defer;
		};

		// stepsize?
		/*clsdmslider = Slider.new(win, Rect(50, 210, 110, 20));
		clsdmslider.value = 0.5;
		clsdmslider.action = {arg num;
			{clsdmboxProxy.valueAction = num.value;}.defer;
		};*/


		/////////////////////////////////////////////////////////////////////////


		textbuf = StaticText(win, Rect(163, 170, 150, 20));
		textbuf.string = "Distant Reverb";
		dstReverbox = PopUpMenu( win, Rect(10, 170, 150, 20));
		dstReverbox.items = ["no-reverb",
			"freeverb",
			"allpass",
			"A-format                (ATK)"] ++ rirList;
		// add the list of impule response if one is provided

		dstReverbox.action_({ arg num;
			{this.dstrvbox[currentsource].valueAction = num.value;}.defer;
		});
		dstReverbox.value = 0;


		/////////////////////////////////////////////////////////////////////////


		textbuf = StaticText(win, Rect(163, 190, 150, 20));
		textbuf.string = "Dst. amount";
		lnumbox = NumberBox(win, Rect(10, 190, 40, 20));
		lnumbox.value = 0;
		lnumbox.clipHi = 1;
		lnumbox.clipLo = 0;
		lnumbox.step_(0.01);
		lnumbox.scroll_step_(0.01);
		lnumbox.align = \center;
		lnumbox.action = {arg num;
			{lbox[currentsource].valueAction = num.value;}.defer;

		};
		// stepsize?
		lslider = Slider.new(win, Rect(50, 190, 110, 20));
		lslider.value = 0;
		lslider.action = {arg num;
			{lbox[currentsource].valueAction = num.value;}.defer;
		};


		/////////////////////////////////////////////////////////////////////////


		textbuf = StaticText(win, Rect(163, 210, 150, 20));
		textbuf.string = "Dst. room/delay";
		rmnumbox = NumberBox(win, Rect(10, 210, 40, 20));
		rmnumbox.value = 0.5;
		rmnumbox.clipHi = 1;
		rmnumbox.clipLo = 0;
		rmnumbox.step_(0.01);
		rmnumbox.scroll_step_(0.01);
		rmnumbox.align = \center;
		rmnumbox.action = {arg num;
			{rmbox[currentsource].valueAction = num.value;}.defer;

		};
		// stepsize?
		rmslider = Slider.new(win, Rect(50, 210, 110, 20));
		rmslider.value = 0.5;
		rmslider.action = {arg num;
			{rmbox[currentsource].valueAction = num.value;}.defer;
		};


		/////////////////////////////////////////////////////////////////////////


		textbuf = StaticText(win, Rect(163, 230, 150, 20));
		textbuf.string = "Dst. damp/decay";
		dmnumbox = NumberBox(win, Rect(10, 230, 40, 20));
		dmnumbox.value = 0.5;
		dmnumbox.clipHi = 1;
		dmnumbox.clipLo = 0;
		dmnumbox.step_(0.01);
		dmnumbox.scroll_step_(0.01);
		dmnumbox.align = \center;
		dmnumbox.action = {arg num;
			{dmbox[currentsource].valueAction = num.value;}.defer;

		};
		// stepsize?
		dmslider = Slider.new(win, Rect(50, 230, 110, 20));
		dmslider.value = 0.5;
		dmslider.action = {arg num;
			{dmbox[currentsource].valueAction = num.value;}.defer;
		};


		/////////////////////////////////////////////////////////////////////////


		textbuf = StaticText(win, Rect(163, 250, 100, 20));
		textbuf.string = "Angle (stereo)";
		angnumbox = NumberBox(win, Rect(10, 250, 40, 20));
		angnumbox.value = 1.0471975511966;
		angnumbox.clipHi = pi;
		angnumbox.clipLo = 0;
		angnumbox.step_(0.01);
		angnumbox.scroll_step_(0.01);
		angnumbox.align = \center;
		angnumbox.action = {arg num;
			{abox[currentsource].valueAction = num.value;}.defer;
			if((ncanais[currentsource]==2) || (this.ncan[currentsource]==2)){
				this.espacializador[currentsource].set(\angle, num.value);
				this.setSynths(currentsource, \angle, num.value);
				angle[currentsource] = num.value;
			};
		};

		angslider = Slider.new(win, Rect(50, 250, 110, 20));
		//	b = ControlSpec(0.0, 3.14, \linear, 0.01); // min, max, mapping, step
		angslider.value = 1.0471975511966 / pi;
		angslider.action = {arg num;
			{abox[currentsource].valueAction = num.value * pi;}.defer;
			if((ncanais[currentsource]==2) || (this.ncan[currentsource]==2)) {
				{angnumbox.value = num.value * pi;}.defer;
				//			this.espacializador[currentsource].set(\angle, b.map(num.value));
				this.espacializador[currentsource].set(\angle, num.value * pi);
				this.setSynths(currentsource, \angle, num.value * pi);
				//			angle[currentsource] = b.map(num.value);
				angle[currentsource] = num.value * pi;
			}{{angnumbox.value = num.value * pi;}.defer;};
		};


		/////////////////////////////////////////////////////////////////////////


		textbuf = StaticText(win, Rect(163, 270, 150, 20));
		textbuf.string = "Rotation (B-Format)";
		rnumbox = NumberBox(win, Rect(10, 270, 40, 20));
		rnumbox.value = 0;
		rnumbox.clipHi = pi;
		rnumbox.clipLo = -pi;
		rnumbox.step_(0.01);
		rnumbox.scroll_step_(0.01);
		rnumbox.align = \center;
		rnumbox.action = {arg num;
			{rbox[currentsource].valueAction = num.value;}.defer;

		};
		// stepsize?
		rslider = Slider.new(win, Rect(50, 270, 110, 20));
		rslider.value = 0.5;
		rslider.action = {arg num;
			{rbox[currentsource].valueAction = num.value * 6.28 - pi;}.defer;
			{rnumbox.value = num.value * 2pi - pi;}.defer;

		};


		/////////////////////////////////////////////////////////////////////////


		textbuf = StaticText(win, Rect(163, 290, 150, 20));
		textbuf.string = "Directivity (B-Format)";
		dirnumbox = NumberBox(win, Rect(10, 290, 40, 20));
		dirnumbox.value = 0;
		dirnumbox.clipHi = pi * 0.5;
		dirnumbox.clipLo = 0;
		dirnumbox.step_(0.01);
		dirnumbox.scroll_step_(0.01);
		dirnumbox.align = \center;
		dirnumbox.action = {arg num;
			{dbox[currentsource].valueAction = num.value;}.defer;
		};
		// stepsize?
		dirslider = Slider.new(win, Rect(50, 290, 110, 20));
		dirslider.value = 0;
		dirslider.action = {arg num;
			{dbox[currentsource].valueAction = num.value * pi * 0.5;}.defer;
			{dirnumbox.value = num.value * pi * 0.5;}.defer;
		};


		/////////////////////////////////////////////////////////////////////////


		textbuf = StaticText(win, Rect(163, 310, 170, 20));
		textbuf.string = "Contraction (ATK & VBAP)";
		connumbox = NumberBox(win, Rect(10, 310, 40, 20));
		connumbox.value = 1;
		connumbox.clipHi = 1;
		connumbox.clipLo = 0;
		connumbox.step_(0.01);
		connumbox.scroll_step_(0.01);
		connumbox.align = \center;
		connumbox.action = {arg num;
			{cbox[currentsource].valueAction = num.value;}.defer;

		};
		// stepsize?
		cslider = Slider.new(win, Rect(50, 310, 110, 20));
		cslider.value = 1;
		cslider.action = {arg num;
			{cbox[currentsource].valueAction = num.value;}.defer;
			{connumbox.value = num.value;}.defer;
		};

		spreadcheck = CheckBox( win, Rect(10, 330, 80, 20), "Spread").action_({ arg butt;
			{this.spcheck[currentsource].valueAction = butt.value;}.defer;
		});
		spreadcheck.value = false;
		diffusecheck = CheckBox( win, Rect(90, 330, 80, 20), "Diffuse").action_({ arg butt;
			{this.dfcheck[currentsource].valueAction = butt.value;}.defer;
		});
		diffusecheck.value = false;



		/////////////////////////////////////////////////////////



		bload = Button(dialView, Rect(90, 0, 90, 20))
		.states_([
			["load audio", Color.black, Color.white],
		])
		.action_({ arg but;
			this.synt[currentsource].free; // error check
			this.espacializador[currentsource].free;


			Dialog.openPanel(
				control.stopRecording;
				//control.stop;


				{
					arg path;

					{
						this.streamdisk[currentsource] = false;
						this.tfieldProxy[currentsource].valueAction = path;}.defer;
					stcheck[currentsource].valueAction = false;




				},
				{
					this.streamdisk[currentsource] = false;
					"cancelled".postln;

					{this.tfield[currentsource].value = "";}.defer;
					{this.tfieldProxy[currentsource].value = "";}.defer;
					stcheckProxy[currentsource].valueAction = false;
				}
			);

		});

		bstream = Button(dialView, Rect(90, 20, 90, 20))
		.states_([
			["stream audio", Color.black, Color.white],
		])
		.action_({ arg but;
			this.synt[currentsource].free; // error check
			this.espacializador[currentsource].free;

			Dialog.openPanel(
				control.stopRecording;
				//control.stop;
				//control.seek;
				{
					arg path;

					{
						this.streamdisk[currentsource] = true;
						this.tfieldProxy[currentsource].valueAction = path;}.defer;
					stcheck[currentsource].valueAction = true;

				},
				{
					"cancelled".postln;
					this.streamdisk[currentsource] = false;
					{this.tfieldProxy[currentsource].value = "";}.defer;
					{this.tfield[currentsource].value = "";}.defer;
					stcheck[currentsource].valueAction = false;
				}

			);

		});


		bnodes = Button(dialView, Rect(0, 40, 90, 20))
		.states_([
			["show nodes", Color.black, Color.white],
		])
		.action_({
			server.plotTree;
		});

		textbuf = StaticText(wdados, Rect(20, 20, 50, 20));
		textbuf.font = Font(Font.defaultSansFace, 9);
		textbuf.string = "Lib";
		textbuf = StaticText(wdados, Rect(45, 20, 50, 20));
		textbuf.font = Font(Font.defaultSansFace, 9);
		textbuf.string = "Rv";
		textbuf = StaticText(wdados, Rect(70, 20, 50, 20));
		textbuf.font = Font(Font.defaultSansFace, 9);
		textbuf.string = "Lp";
		textbuf = StaticText(wdados, Rect(85, 20, 50, 20));
		textbuf.font = Font(Font.defaultSansFace, 9);
		textbuf.string = "Hw";
		textbuf = StaticText(wdados, Rect(100, 20, 50, 20));
		textbuf.font = Font(Font.defaultSansFace, 9);
		textbuf.string = "Sc";

		//comment out all linear parameters
		//textbuf = StaticText(wdados, Rect(105, 20, 50, 20));
		//textbuf.font = Font(Font.defaultSansFace, 9);
		//textbuf.string = "Ln";

		textbuf = StaticText(wdados, Rect(115, 20, 50, 20));
		textbuf.font = Font(Font.defaultSansFace, 9);
		textbuf.string = "Sp";
		textbuf = StaticText(wdados, Rect(130, 20, 50, 20));
		textbuf.font = Font(Font.defaultSansFace, 9);
		textbuf.string = "Df";

		textbuf = StaticText(wdados, Rect(145, 20, 50, 20));
		textbuf.font = Font(Font.defaultSansFace, 9);
		textbuf.string = "NCan";
		textbuf = StaticText(wdados, Rect(170, 20, 50, 20));
		textbuf.font = Font(Font.defaultSansFace, 9);
		textbuf.string = "SBus";

		textbuf = StaticText(wdados, Rect(208, 20, 50, 20));
		textbuf.font = Font(Font.defaultSansFace, 9);
		textbuf.string = "X";
		textbuf = StaticText(wdados, Rect(241, 20, 50, 20));
		textbuf.font = Font(Font.defaultSansFace, 9);
		textbuf.string = "Y";

		textbuf = StaticText(wdados, Rect(274, 20, 50, 20));
		textbuf.font = Font(Font.defaultSansFace, 9);
		textbuf.string = "Z";

		/*
		textbuf = StaticText(wdados, Rect(299, 20, 50, 20));
		textbuf.font = Font(Font.defaultSansFace, 9);
		textbuf.string = "O.x";
		textbuf = StaticText(wdados, Rect(333, 20, 50, 20));
		textbuf.font = Font(Font.defaultSansFace, 9);
		textbuf.string = "O.y";
		textbuf = StaticText(wdados, Rect(366, 20, 50, 20));
		textbuf.font = Font(Font.defaultSansFace, 9);
		textbuf.string = "O.z";
		*/

		textbuf = StaticText(wdados, Rect(350, 20, 50, 20));
		textbuf.font = Font(Font.defaultSansFace, 9);
		textbuf.string = "Lev";
		textbuf = StaticText(wdados, Rect(375, 20, 50, 20));
		textbuf.font = Font(Font.defaultSansFace, 9);
		textbuf.string = "DAmt";
		textbuf = StaticText(wdados, Rect(400, 20, 50, 20));
		textbuf.font = Font(Font.defaultSansFace, 9);
		textbuf.string = "Prox";
		textbuf = StaticText(wdados, Rect(425, 20, 50, 20));
		textbuf.font = Font(Font.defaultSansFace, 9);
		textbuf.string = "Dist";
		textbuf = StaticText(wdados, Rect(450, 20, 50, 20));
		textbuf.font = Font(Font.defaultSansFace, 9);
		textbuf.string = "Room";
		textbuf = StaticText(wdados, Rect(475, 20, 50, 20));
		textbuf.font = Font(Font.defaultSansFace, 9);
		textbuf.string = "Damp";
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

			textbuf = StaticText(wdados, Rect(5, 40 + (i*20), 50, 20));
			textbuf.font = Font(Font.defaultSansFace, 9);
			textbuf.string = (i+1).asString;

			this.libbox[i] = NumberBox(wdados, Rect(20, 40 + (i*20), 25, 20));

			this.dstrvbox[i] = NumberBox.new(wdados, Rect(45, 40 + (i*20), 25, 20));

			this.lpcheck[i] = CheckBox.new(wdados, Rect(70, 40 + (i*20), 40, 20));

			this.lpcheck[i].action_({ arg but;
				this.lpcheckProxy[i].valueAction = but.value;
			});

			this.hwncheck[i] = CheckBox.new( wdados, Rect(85, 40 + (i*20), 40, 20));

			this.hwncheck[i].action_({ arg but;
				this.hwncheckProxy[i].valueAction = but.value;
			});


			this.scncheck[i] = CheckBox.new( wdados, Rect(100, 40 + (i*20), 40, 20));

			this.scncheck[i].action_({ arg but;
				this.scncheckProxy[i].valueAction = but.value;
			});


			//comment out all linear parameters
/*
			this.lncheck[i] = CheckBox.new( wdados, Rect(105, 40 + (i*20), 40, 20));

			this.lncheck[i].action_({ arg but;
				this.lncheckProxy[i].valueAction = but.value;
			});
*/

			this.spcheck[i] = CheckBox.new(wdados, Rect(115, 40 + (i*20), 40, 20));

			this.spcheck[i].action_({ arg but;
				this.spcheckProxy[i].valueAction = but.value;
			});





			this.dfcheck[i] = CheckBox.new(wdados, Rect(130, 40 + (i*20), 40, 20));

			this.dfcheck[i].action_({ arg but;
				this.dfcheckProxy[i].valueAction = but.value;
			});


			/////////////////////////////////////////////////////////////////


			this.ncanbox[i] = NumberBox(wdados, Rect(145, 40 + (i*20), 25, 20));
			this.businibox[i] = NumberBox(wdados, Rect(170, 40 + (i*20), 25, 20));

			xbox[i] = NumberBox(wdados, Rect(195, 40 + (i*20), 33, 20));
			ybox[i] = NumberBox(wdados, Rect(228, 40+ (i*20), 33, 20));
			zbox[i] = NumberBox(wdados, Rect(261, 40+ (i*20), 33, 20));

			//oxbox[i] = NumberBox(wdados, Rect(300, 40 + (i*20), 33, 20));
			//oybox[i] = NumberBox(wdados, Rect(333, 40+ (i*20), 33, 20));
			//ozbox[i] = NumberBox(wdados, Rect(366, 40+ (i*20), 33, 20));

			vbox[i] = NumberBox(wdados, Rect(350, 40 + (i*20), 25, 20));
			dpbox[i] = NumberBox(wdados, Rect(375, 40+ (i*20), 25, 20));
			gbox[i] = NumberBox(wdados, Rect(400, 40+ (i*20), 25, 20));
			lbox[i] = NumberBox(wdados, Rect(425, 40+ (i*20), 25, 20));
			rmbox[i] = NumberBox(wdados, Rect(450, 40+ (i*20), 25, 20));
			dmbox[i] = NumberBox(wdados, Rect(475, 40+ (i*20), 25, 20));
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

			a1check[i] = CheckBox.new( wdados, Rect(725, 40 + (i*20), 40, 20));
			a1check[i].action = { arg but;
				this.a1checkProxy[i].valueAction = but.value;
			};



			a2check[i] = CheckBox.new( wdados, Rect(740, 40 + (i*20), 40, 20));
			a2check[i].action = { arg but;
				this.a2checkProxy[i].valueAction = but.value;
			};


			a3check[i] = CheckBox.new( wdados, Rect(755, 40 + (i*20), 40, 20));
			a3check[i].action = { arg but;
				this.a3checkProxy[i].valueAction = but.value;
			};


			a4check[i] = CheckBox.new( wdados, Rect(770, 40 + (i*20), 40, 20));
			a4check[i].action = { arg but;
				this.a4checkProxy[i].valueAction = but.value;
			};


			a5check[i] = CheckBox.new( wdados, Rect(785, 40 + (i*20), 40, 20));
			a5check[i].action = { arg but;
				this.a5checkProxy[i].valueAction = but.value;
			};



			this.tfield[i] = TextField(wdados, Rect(800, 40+ (i*20), 125, 20));
			//this.tfield[i] = TextField(wdados, Rect(720, 40+ (i*20), 220, 20));

			stcheck[i] = CheckBox.new( wdados, Rect(925, 40 + (i*20), 40, 20));
			stcheck[i].action = { arg but;
				this.stcheckProxy[i].valueAction = but.value;
			};


			this.libbox[i].font = Font(Font.defaultSansFace, 9);
			this.dstrvbox[i].font = Font(Font.defaultSansFace, 9);
			this.ncanbox[i].font = Font(Font.defaultSansFace, 9);
			this.businibox[i].font = Font(Font.defaultSansFace, 9);
			xbox[i].font = Font(Font.defaultSansFace, 9);
			ybox[i].font = Font(Font.defaultSansFace, 9);
			zbox[i].font = Font(Font.defaultSansFace, 9);
			//oxbox[i].font = Font(Font.defaultSansFace, 9);
			//oybox[i].font = Font(Font.defaultSansFace, 9);
			//ozbox[i].font = Font(Font.defaultSansFace, 9);


			abox[i].font = Font(Font.defaultSansFace, 9);
			vbox[i].font = Font(Font.defaultSansFace, 9);
			gbox[i].font = Font(Font.defaultSansFace, 9);
			lbox[i].font = Font(Font.defaultSansFace, 9);
			rmbox[i].font = Font(Font.defaultSansFace, 9);
			dmbox[i].font = Font(Font.defaultSansFace, 9);
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
			//oxbox[i].decimals = 0;
			//oybox[i].decimals = 0;
			//ozbox[i].decimals = 0;


			a1box[i].action = {arg num;
				this.a1boxProxy[i].valueAction = num.value;
			};


			a2box[i].action = {arg num;
				this.a2boxProxy[i].valueAction = num.value;
			};


			a3box[i].action = {arg num;
				this.a3boxProxy[i].valueAction = num.value;
			};

			a4box[i].action = {arg num;
				this.a4boxProxy[i].valueAction = num.value;
			};

			a5box[i].action = {arg num;
				this.a5boxProxy[i].valueAction = num.value;
			};



			this.tfield[i].action = {arg path;
				this.tfieldProxy[i].valueAction = path.value;
			};




			//// PROXY ACTIONS /////////

			// gradually pinching these and putting up above

			this.xbox[i].action = {arg num;
				this.xboxProxy[i].valueAction = num.value;
			};


			ybox[i].action = {arg num;
				this.yboxProxy[i].valueAction = num.value;
			};
			ybox[i].value = 20;


			zbox[i].action = {arg num;
				this.zboxProxy[i].valueAction = num.value;
			};




			abox[i].clipHi = pi;
			abox[i].clipLo = 0;
			vbox[i].clipHi = 1.0;
			vbox[i].clipLo = 0;
			gbox[i].clipHi = 1.0;
			gbox[i].clipLo = 0;
			lbox[i].clipHi = 1.0;
			lbox[i].clipLo = 0;
			rmbox[i].clipHi = 1.0;
			rmbox[i].clipLo = 0;
			dmbox[i].clipHi = 1.0;
			dmbox[i].clipLo = 0;

			vbox[i].scroll_step = 0.01;
			abox[i].scroll_step = 0.01;
			vbox[i].step = 0.01;
			abox[i].step = 0.01;
			gbox[i].scroll_step = 0.01;
			lbox[i].scroll_step = 0.01;
			gbox[i].step = 0.01;
			lbox[i].step = 0.01;
			rmbox[i].scroll_step = 0.01;
			rmbox[i].step = 0.01;
			dmbox[i].scroll_step = 0.01;
			dmbox[i].step = 0.01;


			this.libbox[i].action = {arg num;
				this.libboxProxy[i].valueAction = num.value;
			};
			libbox[i].value = 3;

			this.dstrvbox[i].action = {arg num;
				this.dstrvboxProxy[i].valueAction = num.value;
			};
			dstrvbox[i].value = 0;

			abox[i].action = {arg num;
				this.aboxProxy[i].valueAction = num.value;
				/*	angle[i] = num.value;
					if((ncanais[i]==2) || (this.ncan[i]==2)){
					this.espacializador[i].set(\angle, num.value);
					this.setSynths(i, \angle, num.value);
					angle[i] = num.value;
					};
					if(i == currentsource)
					{
					angnumbox.value = num.value;
					angslider.value = num.value / pi;
				*/
			};

			vbox[i].action = {arg num;
				this.vboxProxy[i].valueAction = num.value;
			};


			abox[i].value = 1.0471975511966;

			gbox[i].value = 0;
			lbox[i].value = 0;
			rmbox[i].value = 0.5;
			dmbox[i].value = 0.5;

			gbox[i].action = {arg num;
				this.gboxProxy[i].valueAction = num.value;
			};

			lbox[i].action = {arg num;
				this.lboxProxy[i].valueAction = num.value;
			};

			rmbox[i].action = {arg num;
				this.rmboxProxy[i].valueAction = num.value;
			};

			dmbox[i].action = {arg num;
				this.dmboxProxy[i].valueAction = num.value;
			};

			rbox[i].action = {arg num;
				this.rboxProxy[i].valueAction = num.value;
			};


			dbox[i].action = {arg num;
				this.dboxProxy[i].valueAction = num.value;
			};


			cbox[i].action = {arg num;
				this.cboxProxy[i].valueAction = num.value;
			};


			dpbox[i].action = {arg num;
				this.dpboxProxy[i].valueAction = num.value;
			};


			this.ncanbox[i].action = {arg num;
				this.ncanboxProxy[i].valueAction = num.value;
			};


			this.businibox[i].action = {arg num;
				this.businiboxProxy[i].valueAction = num.value;
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
				"RUNNING TRIGGER".postln;
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


		//control = Automation(dur).front(win, Rect(this.halfwidth, 10, 400, 25));
		/*~autotest = control = Automation(this.dur, showLoadSave: false, showSnapshot: false,
			minTimeStep: 0.001).front(win,
			Rect(10, this.width - 80, 400, 22));
		*/
		//~autotest = control = Automation(this.dur, showLoadSave: false, showSnapshot: false,
		//	minTimeStep: 0.001);
		control.front(autoView, Rect(0, 0, 325, 20));


		control.presetDir = prjDr ++ "/auto";
		//control.setMinTimeStep(2.0);
		control.onEnd = {
			//	control.stop;
			control.seek;
			if(this.autoloopval) {
				//control.play;
			};
			/*
			this.nfontes.do { arg i;
				if(this.synt[i].notNil) {
					this.synt[i].free;
				};
			};
			*/
		};

		this.control.onPlay = {
			var startTime;
			"ON PLAY".postln;


			/*this.nfontes.do { arg i;
				this.firstTime[i]=true;
				("NOW PLAYING = " ++ this.firstTime[i]).postln;*/
			if (this.looping) {
				this.nfontes.do { arg i;
					this.firstTime[i]=true;
					//("HERE = " ++ this.firstTime[i]).postln;

				};
				this.looping = false;
				"Was looping".postln;



			};
			if(control.now < 0)
			{
				startTime = 0
			}
			{
				startTime = control.now
			};
			this.isPlay = true;
			//runTriggers.value;
		};


		this.control.onSeek = {
			/*
			var wasplaying = isPlay;

			//("isPlay = " ++ isPlay).postln;
			//runStops.value; // necessary? doesn't seem to help prob of SC input

			//runStops.value;
			if(isPlay == true) {
				this.nfontes.do { arg i;
					if(this.testado[i].not) {
						this.synt[i].free;
					};
				};
				control.stop;
			};

			if(wasplaying) {
				{control.play}.defer(0.5); //delay necessary. may need more?
			};
			*/
		};

		/*this.control.onStop = {
			runStops.value;
			"ON STOP".postln;
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
		*/

				control.onStop = {

			if(this.autoloopval.not) {
				//("Control now = " ++ control.now ++ " dur = " ++ this.dur).postln;
			};
			if(this.autoloopval.not || (this.control.now.round != this.dur)) {
				("I HAVE STOPPED. dur = " ++ this.dur ++ " now = " ++ this.control.now).postln;
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
				this.looping = false;
				this.nfontes.do { arg i;
					this.firstTime[i]=true;
					//("HERE = " ++ this.firstTime[i]).postln;
				};

			} {
				( "Did not stop. dur = " ++ this.dur ++ " now = " ++ this.control.now).postln;
				this.looping = true;
				this.control.play;
			};
		};

		///////////////// PROXY WILL REMOVE THIS //////////////

		/*


		this.nfontes.do { arg i;
			//	control.dock(xbox[i], "x_axis_" ++ i);
			//control.dock(ybox[i], "y_axis_" ++ i);
			//control.dock(zbox[i], "z_axis_" ++ i);



			//control.dock(vbox[i], "level_" ++ i);
			//control.dock(dpbox[i], "dopamt_" ++ i);

			//control.dock(abox[i], "angle_" ++ i);
			//control.dock(gbox[i], "revglobal_" ++ i);
			//control.dock(lbox[i], "revlocal_" ++ i);
			//control.dock(rbox[i], "rotation_" ++ i);
			//control.dock(dbox[i], "diretividade_" ++ i);
			//control.dock(cbox[i], "contraction_" ++ i);


			//control.dock(a1box[i], "aux1_" ++ i);
			//control.dock(a2box[i], "aux2_" ++ i);
			//control.dock(a3box[i], "aux3_" ++ i);
			//control.dock(a4box[i], "aux4_" ++ i);
			//control.dock(a5box[i], "aux5_" ++ i);

			//control.dock(a1check[i], "aux1check_" ++ i);
			//control.dock(a2check[i], "aux2check_" ++ i);
			//control.dock(a3check[i], "aux3check_" ++ i);
			//control.dock(a4check[i], "aux4check_" ++ i);
			//control.dock(a5check[i], "aux5check_" ++ i);

			//control.dock(stcheck[i], "stcheck_" ++ i);



		};

		//////////////////////////////////////////

			~myview = UserView.new(win,win.view.bounds);

			~myview.mouseDownAction = { |x, y, modifiers, buttonNumber, clickCount|
			buttonNumber.postln;
			};
		*/

		win.view.mouseDownAction = { arg view, x, y, modifiers, buttonNumber, clickCount;
			mouseButton = buttonNumber; // 0 = left, 2 = middle, 1 = right
			if((mouseButton == 1) || (mouseButton == 2)) {
				dragStartScreen.x = y - this.halfwidth;
				dragStartScreen.y = x - this.halfwidth;
				this.nfontes.do { arg i;
					("" ++ i ++ " " ++ this.cartval[i]).postln;
					if(this.espacializador[i].notNil) {

						this.espacializador[i].set(\azim, this.spheval[i].theta, \elev, this.spheval[i].phi,
							\radius, this.spheval[i].rho);
						//,
						//\xoffset, this.xoffset[i], \yoffset, this.yoffset[i]);
						this.setSynths(i, \azim, this.spheval[i].theta, \elev, this.spheval[i].phi,
							\radius, this.spheval[i].rho);
						//,
						//\xoffset, this.xoffset[i], \yoffset, this.yoffset[i]);
						this.synt[i].set(\azim, this.spheval[i].theta, \elev, this.spheval[i].phi,
							\radius, this.spheval[i].rho);
						//,
						//\xoffset, this.xoffset[i], \yoffset, this.yoffset[i]);
					};
				};
			};
		};

		win.view.mouseMoveAction = {|view, x, y, modifiers|
			var period = Main.elapsedTime - this.lastGui;
			//[x, y];
			if(mouseButton == 0) { // left button
				xbox[currentsource].valueAction = (x - this.halfwidth) / this.halfheight;
				ybox[currentsource].valueAction = (this.halfheight - y) / this.halfheight;

				/*win.drawFunc = {
					// big circle
					Pen.fillColor = Color(0.6,0.8,0.8);
					Pen.addArc(this.halfwidth@this.halfheight, this.halfheight, 0, 2pi);
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
					Pen.addArc(this.halfwidth@this.halfheight, 20, 0, 2pi);
					Pen.fill;

				};*/

			};
			if (period > this.guiInt) {
				this.lastGui =  Main.elapsedTime;
				{novoplot.value}.defer;
			};

		};


		win.view.onResize_({|view|

			this.width = view.bounds.width;
			this.halfwidth = this.width * 0.5;
			this.height = view.bounds.height;
			this.halfheight = this.height * 0.5;

			dialView.bounds_(Rect(this.width - 190, 10, 180, 80));

			zSliderHeight = this.height * 2 / 3;
			zslider.bounds_(Rect(this.width - 35, ((this.height - zSliderHeight) / 2),
				20, zSliderHeight));
			znumbox.bounds_(Rect(this.width - 45, ((this.height - zSliderHeight) / 2)
				+ zSliderHeight, 40, 20));
			zAxis.bounds_(Rect(this.width - 80, this.halfheight - 10, 90, 20));

			orientView.bounds_(Rect(this.width - 265, this.height - 85, 265, 100));

			autoView.bounds_(Rect(10, this.height - 45, 325, 40));

			novoplot.value;

		});


		win.onClose_({

			wdados.close;
			waux.close;
			this.free;

		});

		mmcslave = CheckBox(autoView, Rect(163, 20, 140, 20), "Slave to MMC").action_({ arg butt;
			//("Doppler is " ++ butt.value).postln;
			if(butt.value) {
				"Slaving transport to MMC".postln;
				MIDIIn.addFuncTo(\sysex, sysex);
			} {
				"MIDI input closed".postln;
				MIDIIn.removeFuncFrom(\sysex, sysex);
			};

			//	dcheck[currentsource].valueAction = butt.value;
		});

		this.autoloop = CheckBox(autoView, Rect(273, 20, 140, 20), "Loop").action_({ arg butt;
			//("Doppler is " ++ butt.value).postln;
			if(butt.value) {
				"Looping transport".postln;
				this.autoloopval = true;
			} {
				this.autoloopval = false;
			};

		});

		this.autoloop.value = this.autoloopval;

		sysex  = { arg src, sysex;
			// This should be more elaborate - other things might trigger it...fix this!
			if(sysex[3] == 6){ var x;
				("We have : " ++ sysex[4] ++ " type action").postln;

				x = case
				{ sysex[4] == 1 } {

					"Stop".postln;
					control.stop;
				}
				{ sysex[4] == 2 } {
					"Play".postln;
					control.play;

				}
				{ sysex[4] == 3 } {
					"Deffered Play".postln;
					control.play;

				}
				{ sysex[4] == 68 } { var goto;
					("Go to event: " ++ sysex[7] ++ "hr " ++ sysex[8] ++ "min "
						++ sysex[9] ++ "sec and " ++ sysex[10] ++ "frames").postln;
					goto =  (sysex[7] * 3600) + (sysex[8] * 60) + sysex[9] + (sysex[10] / 30);
					control.seek(goto);

				};
			};
		};
		control.snapshot; // necessary to call at least once before saving automation
		                  // otherwise will get not understood errors on load
	}


}