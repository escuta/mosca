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
	*new { | val |
		^super.new.initAutomationProxy(val);
	}
	initAutomationProxy { | ival |
		this.val = ival;
		this.bounds(Rect(0,0,0,0)); // set fake bounds to keep Automation happy!
	}
	value {
		^this.val;
	}
	value_ { | value |
		this.val = value;
	}
	mapToGlobal { | point |
		_QWidget_MapToGlobal
		^this.primitiveFailed;
	}

	absoluteBounds {
		^this.bounds.moveToPoint( this.mapToGlobal( 0@0 ) );
	}
	bounds {
		^this.getProperty(\geometry)
	}

	bounds_ { | rect |
		this.setProperty(\geometry, rect.asRect )
	}

	doAction {
		this.action.value(this.val)
	}
	valueAction_ { |val|
		this.value_(val).doAction
	}
}


Mosca {
	var //<>sprite,
	<>nfontes,
	<>revGlobal, <>nonAmbi2FuMa, <>convertor,
	<>libnumbox, <>control,
	<>globDec,
	<>sysex, <>mmcslave,
	<>synthRegistry, <>busini, <>ncan,
	<>aux1, <>aux2, <>aux3, <>aux4, <>aux5,  // aux slider values
	<>triggerFunc, <>stopFunc,
	<>scInBus,
	<>fumabus,
	<>insertFlag,
	<>insertBus,
	<>dur,
	<>plim, // distance limit from origin where processes continue to run
	<>looping,
	<>serport,
	<>offsetheading,
	<>libbox, <>lpcheck, <>dstrvbox, <>hwncheck, <>scncheck,
	<>spcheck, <>dfcheck,
	<>ncanbox, <>businibox,
	<>espacializador, <>synt,
	<>tfield,
	<>autoloopval,
	<>autoloop,
	<>streamdisk,
	<>streambuf, // <>streamrate, // apparently unused
	<>origine,
	<>oxnumbox, <>oynumbox, <>oznumbox,
	<>oxnumboxProxy, <>oynumboxProxy, <>oznumboxProxy,
	<>pitch, <>pitchnumbox, <>pitchnumboxProxy,
	<>roll, <>rollnumbox, <>rollnumboxProxy,
	<>heading, <>headingnumbox, <>headingnumboxProxy,
	// head tracking
	<>trackarr, <>trackarr2, <>tracki, <>trackPort,
	//<>track2arr, <>track2arr2, <>track2i,
	<>headingOffset,
	<>cartval, <>spheval,
	<>recchans, <>recbus,
	// <>mark1, <>mark2,	// 4 number arrays for marker data // apparently unused

	// MOVED FROM the the gui method/////////////////////////

	<>angnumbox, <>cbox, <>clev, <>angle, <>ncanais, <>testado,	<>gbus, <>gbfbus, <>ambixbus, <>gbixfbus,
	<>nonambibus,
	<>playEspacGrp, <>glbRevDecGrp,
	<>level, <>lp, <>lib, <>libName, <>convert, <>dstrv, <>dstrvtypes, <>clsrv, <>clsRvtypes,
	<>angslider, <>connumbox, <>cslider,
	<>xbox, <>ybox, <>sombuf, <>sbus, <>soaRevBus, <>mbus,
	<>rbox, <>abox, <>vbox, <>gbox, <>lbox, <>dbox, <>dpbox, <>zbox,
	<>a1check, <>a2check, <>a3check, <>a4check, <>a5check, <>a1box, <>a2box, <>a3box, <>a4box, <>a5box,
	<>stcheck,

	//<>oxbox, <>oybox, <>ozbox,
	//<>funcs, // apparently unused
	//<>lastx, <>lasty, // apparently unused
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
	<>spreadcheck,
	<>diffusecheck, <>sp, <>df, <>ncannumbox, <>busininumbox,

	<>atualizarvariaveis, <>updateSynthInArgs,

	<>runTriggers, <>runStops, <>runTrigger, <>runStop,
	// <>isRec, // apparently unused

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
	<>spcheckProxy, <>ncanboxProxy, <>businiboxProxy,

	<>grainrate, <>rateslider, <>ratenumbox, <>ratebox, <>rateboxProxy, // setable granular rate
	<>winsize, <>winslider, <>winumbox, <>winbox, <>winboxProxy, // setable granular window size
	<>winrand, <>randslider, <>randnumbox, <>randbox, <>randboxProxy,
	// setable granular window size random factor

	<>rm, <>rmslider, <>rmnumbox, <>rmbox, <>rmboxProxy, // setable local room size
	<>dm, <>dmslider, <>dmnumbox, <>dmbox, <>dmboxProxy, // setable local dampening

	<>clsrm, //<>clsrmslider,
	<>clsrmnumbox, <>clsrmbox, <>clsrmboxProxy, // setable global room size
	<>clsdm, //<>clsdmslider,
	<>clsdmnumbox, <>clsdmbox, <>clsdmboxProxy, // setable global dampening

	<>masterlevProxy, <>masterslider,

	<>ossiasrc, <>ossiaorient, <>ossiaorigine, <>ossiaplay, <>ossiatrasportLoop, <>ossiatransport,
	<>ossiaseekback, <>ossiarec, <>ossiacart, <>ossiasphe, <>ossiaaud, <>ossialoop, <>ossialib,
	<>ossialev, <>ossiadp, <>ossiacls, <>ossiaclsam, <>ossiaclsdel, <>ossiaclsdec, <>ossiadst,
	<>ossiadstam, <>ossiadstdel, <>ossiadstdec, <>ossiaangle, <>ossiarot, <>ossiadir, <>ossiactr,
	<>ossiaspread, <>ossiadiff, <>ossiaCartBack, <>ossiaSpheBack, <>ossiarate, <>ossiawin, <>ossiarand,
	<>ossiamaster;


	/////////////////////////////////////////


	classvar server,
	rirWspectrum, rirXspectrum, rirYspectrum, rirZspectrum, rirA12Spectrum,
	rirFLUspectrum, rirFRDspectrum, rirBLDspectrum, rirBRUspectrum,
	rirList, a12SpecPar, a4SpecPar, wxyzSpecPar, zSpecPar, wSpecPar,
	spatList = #["Ambitools","HoaLib","ADTB","AmbIEM","ATK","BF-FMH","JoshGrain","VBAP"], // list of spat libs
	lastSN3D = 3, // last SN3D lib index
	lastFUMA = 6, // last FUMA lib index
	b2a, a2b,
	blips,
	maxorder,
	convert_fuma,
	convert_ambix,
	convert_direct,
	speaker_array,
	numoutputs,
	longest_radius, limit_radius, highest_elevation, lowest_elevation,
	vbap_buffer,
	soa_a12_decoder_matrix, soa_a12_encoder_matrix,
	cart, spher, foa_a12_decoder_matrix,
	width, halfwidth, height, halfheight, novoplot,
	lastGui, guiInt,
	lastAutomation = nil,
	firstTime,
	isPlay = false,
	playingBF,
	currentsource,
	guiflag, btestar,
	watcher, troutine, kroutine,
	updatesourcevariables,
	//o, //debugging
	prjDr, fftsize = 2048, halfPi = 1.5707963267949,
	offsetLag = 2.0,  // lag in seconds for incoming GPS data
	server, foaEncoderOmni, foaEncoderSpread, foaEncoderDiffuse;
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

	initMosca { | projDir, nsources, iwidth, idur, rirBank, iserver, decoder, ispeaker_array,
		outbus, suboutbus, rawbusfuma, rawbusambix, imaxorder, iserport, ioffsetheading,
		irecchans, irecbus, iguiflag, iguiint, iautoloop |
		var makeSynthDefPlayers, makeSpatialisers, subOutFunc, playInFunc,
		localReverbFunc, localReverbStereoFunc, perfectSphereFunc,
		iemConvert, spatFuncs, outPutFuncs,
		bFormNumChan = (imaxorder + 1).squared, // add the number of channels of the b format
		fourOrTwelve; // switch between 4 fuma and 12 ch Matrix

		this.nfontes = nsources;
		maxorder = imaxorder;

		server = iserver;
		b2a = FoaDecoderMatrix.newBtoA;
		a2b = FoaEncoderMatrix.newAtoB;
		foaEncoderOmni = FoaEncoderMatrix.newOmni;
		foaEncoderSpread = FoaEncoderKernel.newSpread (subjectID: 6, kernelSize: 2048);
		foaEncoderDiffuse = FoaEncoderKernel.newDiffuse (subjectID: 3, kernelSize: 2048);

		if (maxorder > 1) {
			this.fumabus = Bus.audio(server, 9);
			this.soaRevBus = Bus.audio(server, 9);

			fourOrTwelve = 12;

		} {
			this.fumabus = Bus.audio(server, 4);

			fourOrTwelve = 4;
		};

		this.synthRegistry = Array.newClear(this.nfontes);
		this.insertFlag = Array.newClear(this.nfontes);
		this.insertBus = Array2D.new(2, this.nfontes);
		this.scInBus = Array.newClear(this.nfontes);

		this.nfontes.do { | i |
			this.synthRegistry[i] = List[];

			this.scInBus[i] = Bus.audio(server, 1);

			this.insertBus[0, i] =  Bus.audio(server, fourOrTwelve);

			this.insertFlag[i] = 0;
		};

		this.ambixbus = Bus.audio(server, bFormNumChan); // global b-format ACN-SN3D bus
		this.gbus = Bus.audio(server, 1); // global reverb bus
		this.gbfbus = Bus.audio(server, fourOrTwelve); // global b-format bus
		this.gbixfbus = Bus.audio(server, fourOrTwelve); // global ambix b-format bus
		this.playEspacGrp = Group.tail;
		this.glbRevDecGrp = Group.after(this.playEspacGrp);
		server.sync;

		speaker_array = ispeaker_array;

		//this.lock = ilock;

		if (iwidth < 600) {
			width = 600;
		} {
			width = iwidth;
		};
		halfwidth = width * 0.5;
		height = width; // on init
		halfheight = halfwidth;
		this.dur = idur;
		this.serport = iserport;
		this.offsetheading = ioffsetheading;
		this.recchans = irecchans;
		this.recbus = irecbus;
		guiflag = iguiflag;

		currentsource = 0;
		this.plim = 1.2;
		lastGui = Main.elapsedTime;
		guiInt = iguiint;
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
				troutine.stop;
				troutine.reset;
			};


			troutine = Routine.new({
				inf.do{
					this.matchTByte(this.trackPort.read);
				};
			});

			kroutine = Routine.new({
				inf.do{
					if (this.trackPort.isOpen.not) // if serial port is closed
					{
						"Trying to reopen serial port!".postln;
						if (SerialPort.devices.includesEqual(this.serport))
						// and if device is actually connected
						{
							"Device connected! Opening port!".postln;
							troutine.stop;
							troutine.reset;
							this.trackPort = SerialPort(this.serport, 115200, crtscts: true);
							troutine.play; // start tracker routine again
						}

					};
					1.wait;
				};
			});

			this.headingOffset = this.offsetheading;

		};

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
		//sprite = Array2D.new(this.nfontes, 2);
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
		grainrate = Array.newClear(this.nfontes);
		winsize = Array.newClear(this.nfontes);
		winrand = Array.newClear(this.nfontes);

		this.ncanbox = Array.newClear(this.nfontes);
		this.businibox = Array.newClear(this.nfontes);
		playingBF = Array.newClear(this.nfontes);


		//oxbox = Array.newClear(this.nfontes);
		//oybox = Array.newClear(this.nfontes);
		//ozbox = Array.newClear(this.nfontes);
		xbox = Array.newClear(this.nfontes);
		zbox = Array.newClear(this.nfontes);
		ybox = Array.newClear(this.nfontes);
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
		ratebox = Array.newClear(this.nfontes); // grain rate
		winbox = Array.newClear(this.nfontes); // granular window size
		randbox = Array.newClear(this.nfontes); // granular randomize window
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

		firstTime = Array.newClear(this.nfontes);


		this.tfield = Array.newClear(this.nfontes);
		this.streamdisk = Array.newClear(this.nfontes);

		// busses to send audio from player to spatialiser synths
		this.nfontes.do { | x |
			mbus[x] = Bus.audio(server, 1);
			sbus[x] = Bus.audio(server, 2);
			//	bfbus[x] = Bus.audio(s, 4);
		};


		testado = Array.newClear(this.nfontes);

		origine = Cartesian();

		pitch = 0;
		roll = 0;
		heading = 0;

		clsRvtypes = ""; // initialise close reverb type
		clsrv = 0;
		clsrm = 0.5; // initialise close reverb room size
		clsdm = 0.5; // initialise close reverb dampening

		this.nfontes.do { | i |
			libName[i] = spatList[lastSN3D + 1]; // initialize original ATK encoding
			lib[i] = lastSN3D + 1;
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
			hwn[i] = 0;
			scn[i] = 0;
			rlev[i] = 0;
			dlev[i] = 0;
			clev[i] = 1;
			zlev[i] = 0;
			dplev[i] = 0;
			grainrate[i] = 10;
			winsize[i] = 0.1;
			winrand[i] = 0;

			aux1[i] = 0;
			aux2[i] = 0;
			aux3[i] = 0;
			aux4[i] = 0;
			aux5[i] = 0;
			this.streamdisk[i] = false;
			this.ncan[i] = 0;
			this.busini[i] = 0;
			//sprite[i, 0] = 20;
			//sprite[i, 1] = 0;
			testado[i] = false;
			playingBF[i] = false;
			firstTime[i] = true;
		};

		////////////////////////////////////////////////

		////////// ADDED NEW ARRAYS and other proxy stuff  //////////////////

		// these proxies behave like GUI elements. They eneable
		// the use of Automation without a GUI

		cartval = Array.fill(this.nfontes, {Cartesian(0, 20, 0)});
		spheval = Array.fill(this.nfontes, {|i| cartval[i].asSpherical});

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
		spcheckProxy = Array.newClear(this.nfontes);
		ncanboxProxy = Array.newClear(this.nfontes);
		businiboxProxy = Array.newClear(this.nfontes);
		stcheckProxy = Array.newClear(this.nfontes);
		rateboxProxy = Array.newClear(this.nfontes);
		winboxProxy = Array.newClear(this.nfontes);
		randboxProxy = Array.newClear(this.nfontes);


		this.nfontes.do { | i |
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
			libboxProxy[i] = AutomationGuiProxy.new(lib[i]);
			lpcheckProxy[i] = AutomationGuiProxy.new(false);
			dstrvboxProxy[i] = AutomationGuiProxy.new(0);
			scncheckProxy[i] = AutomationGuiProxy.new(false);
			dfcheckProxy[i] = AutomationGuiProxy.new(false);
			spcheckProxy[i] = AutomationGuiProxy.new(false);
			ncanboxProxy[i] = AutomationGuiProxy.new(0);
			businiboxProxy[i] = AutomationGuiProxy.new(0);
			stcheckProxy[i] = AutomationGuiProxy.new(false);
			rateboxProxy[i] = AutomationGuiProxy.new(10.0);
			winboxProxy[i] = AutomationGuiProxy.new(0.1);
			randboxProxy[i] = AutomationGuiProxy.new(0);


		};

		//set up automationProxy for single parameters outside of the previous loop, not to be docked
		masterlevProxy = AutomationGuiProxy.new(1);
		clsrvboxProxy = AutomationGuiProxy.new(0);
		clsrmboxProxy = AutomationGuiProxy.new(0.5); // cls roomsize proxy
		clsdmboxProxy = AutomationGuiProxy.new(0.5); // cls dampening proxy

		oxnumboxProxy = AutomationGuiProxy.new(0.0);
		oynumboxProxy = AutomationGuiProxy.new(0.0);
		oznumboxProxy = AutomationGuiProxy.new(0.0);

		pitchnumboxProxy = AutomationGuiProxy.new(0.0);
		rollnumboxProxy = AutomationGuiProxy.new(0.0);
		headingnumboxProxy = AutomationGuiProxy.new(0.0);

		this.control = Automation(this.dur, showLoadSave: false, showSnapshot: true,
			minTimeStep: 0.001);


		////////////// DOCK PROXIES /////////////


		// this should be done after the actions are assigned


		this.nfontes.do { | i |

			this.libboxProxy[i].action_({ | num |

				libName[i] = spatList[num.value];

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


			this.dstrvboxProxy[i].action_({ | num |
				case
				{ num.value == 0 }
				{ dstrvtypes[i] = "";
					this.setSynths(i, \rv, 0); }
				{ num.value == 1 }
				{ dstrvtypes[i] = "_free";
					this.setSynths(i, \rv, 0); }
				{ num.value == 2 }
				{ dstrvtypes[i] = "_pass";
					this.setSynths(i, \rv, 0); }
				{ num.value == 3 }
				{ this.setSynths(i, \rv, 1); }
				{ num.value >= 4 }
				{ dstrvtypes[i] = "_conv";
					this.setSynths(i, \rv, 0); };

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


			this.xboxProxy[i].action = { | num |
				var sphe, sphetest;
				this.cartval[i].x_(num.value - origine.x);
				sphe = this.cartval[i].asSpherical.rotate(pitch.neg).tilt(roll.neg).tumble(heading.neg);
				if (this.ossiacart.isNil) {
					this.spheval[i] = sphe;
				} {
					sphetest = [sphe.rho, (sphe.theta - halfPi).wrap(-pi, pi), sphe.phi];
					if (this.ossiaSpheBack && (this.ossiasphe[i].v != sphetest)) {
						this.ossiaCartBack_(false);
						this.ossiasphe[i].v_(sphetest);
						this.ossiaCartBack_(true);
					};
					if (this.ossiacart[i].v[0] != num.value) {
						this.ossiacart[i].v_([num.value, yboxProxy[i].value, zboxProxy[i].value]);
					};
				};
				if ( guiflag) {
					{novoplot.value;}.defer;
					{this.xbox[i].value = num.value}.defer;
				};
				if(this.espacializador[i].notNil || playingBF[i]) {
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
			};

			this.yboxProxy[i].action = { | num |
				var sphe, sphetest;
				this.cartval[i].y_(num.value - origine.y);
				sphe = this.cartval[i].asSpherical.rotate(pitch.neg).tilt(roll.neg).tumble(heading.neg);
				if (this.ossiacart.isNil) {
					this.spheval[i] = sphe;
				} {
					sphetest = [sphe.rho, (sphe.theta - halfPi).wrap(-pi, pi), sphe.phi];
					if (this.ossiaSpheBack && (this.ossiasphe[i].v != sphetest)) {
						this.ossiaCartBack_(false);
						this.ossiasphe[i].v_(sphetest);
						this.ossiaCartBack_(true);
					};
					if (this.ossiacart[i].v[1] != num.value) {
						this.ossiacart[i].v_([xboxProxy[i].value, num.value, zboxProxy[i].value]);
					};
				};
				if (guiflag) {
					{novoplot.value;}.defer;
					{this.ybox[i].value = num.value}.defer;
				};
				if(this.espacializador[i].notNil || playingBF[i]){
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
			};

			this.zboxProxy[i].action = { | num |
				var sphe, sphetest;
				this.cartval[i].z_(num.value - origine.z);
				sphe = this.cartval[i].asSpherical.rotate(pitch.neg).tilt(roll.neg).tumble(heading.neg);
				if (this.ossiacart.isNil) {
					this.spheval[i] = sphe;
				} {
					sphetest = [sphe.rho, (sphe.theta - halfPi).wrap(-pi, pi), sphe.phi];
					if (this.ossiaSpheBack && (this.ossiasphe[i].v != sphetest)) {
						this.ossiaCartBack_(false);
						this.ossiasphe[i].v_(sphetest);
						this.ossiaCartBack_(true);
					};
					if (this.ossiacart[i].v[2] != num.value) {
						this.ossiacart[i].v_([xboxProxy[i].value, yboxProxy[i].value, num.value]);
					};
				};
				zlev[i] = this.spheval[i].z;
				if (guiflag) {
					{novoplot.value;}.defer;
					{zbox[i].value = num.value}.defer;
				};
				if(this.espacializador[i].notNil || playingBF[i]){
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
			};

			this.aboxProxy[i].action = { | num |
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

			vboxProxy[i].action = { | num |
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



			gboxProxy[i].action = { | num |
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

			lboxProxy[i].action = { | num |
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

			rmboxProxy[i].action = { | num |
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

			dmboxProxy[i].action = { | num |
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

			rboxProxy[i].action = { | num |
				this.synt[i].set(\rotAngle, num.value);
				this.setSynths(i, \rotAngle, num.value);
				rlev[i] = num.value;
				if (guiflag) {
					{rbox[i].value = num.value}.defer;
					if(i == currentsource)
					{
						//num.value * 6.28 - pi;
						{rslider.value = (num.value + pi) * 0.5pi}.defer;
						{rnumbox.value = num.value}.defer;
					};
				};
				if (this.ossiarot.notNil) {
					if (this.ossiarot[i].v != num.value) {
						this.ossiarot[i].v_(num.value);
					};
				};
			};

			dboxProxy[i].action = { | num |
				this.synt[i].set(\directang, num.value);
				this.setSynths(i, \directang, num.value);
				dlev[i] = num.value;
				if (guiflag) {
					{dbox[i].value = num.value}.defer;
					if(i == currentsource)
					{
						//num.value * pi* 0.5;
						{dirslider.value = num.value / (pi* 0.5)}.defer;
						{dirnumbox.value = num.value}.defer;
					};
				};
				if (this.ossiadir.notNil) {
					if (this.ossiadir[i].v != num.value) {
						this.ossiadir[i].v_(num.value);
					};
				};
			};

			cboxProxy[i].action = { | num |
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

			dpboxProxy[i].action = { | num |
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


			a1boxProxy[i].action = { | num |
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

			a2boxProxy[i].action = { | num |
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

			a3boxProxy[i].action = { | num |
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

			a4boxProxy[i].action = { | num |
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

			a5boxProxy[i].action = { | num |
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

			a1checkProxy[i].action = { | but |

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

			a2checkProxy[i].action = { | but |

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


			a3checkProxy[i].action = { | but |

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


			a4checkProxy[i].action = { | but |

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


			a5checkProxy[i].action = { | but |

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


			stcheckProxy[i].action = { | but |
				if (but.value) {
					this.streamdisk[i] = true;
				}{
					this.streamdisk[i] = false;
				};
				if (guiflag) {
					{this.stcheck[i].value = but.value}.defer;
				};
			};


			this.lpcheckProxy[i].action_({ | but |
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

			this.hwncheckProxy[i].action = { | but |
				if((i==currentsource) && guiflag) {
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
				if (guiflag) {
					{this.hwncheck[i].value = but.value}.defer;
				};
			};

			this.scncheckProxy[i].action_({ | but |
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

			this.spcheckProxy[i].action_({ | but |
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
					if (this.ossiadiff.notNil) {
						this.ossiadiff[i].v_(false);
					};
				} {
					sp[i] = 0;
					this.espacializador[i].set(\sp, 0);
					this.synt[i].set(\sp, 0);
					this.setSynths(i, \sp, 0);
				};
				if (guiflag) {
					{this.spcheck[i].value = but.value}.defer;
				};
				if (this.ossiaspread.notNil) {
					if (this.ossiaspread[i].v != but.value.asBoolean) {
						this.ossiaspread[i].v_(but.value.asBoolean);
					};
				};
			});

			this.dfcheckProxy[i].action_({ | but |
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
					if (this.ossiaspread.notNil) {
						this.ossiaspread[i].v_(false);
					};
				} {
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

			this.ncanboxProxy[i].action = { | num |
				this.espacializador[i].set(\elev, this.spheval[i].phi);
				this.setSynths(i, \elev, this.spheval[i].phi);
				this.synt[i].set(\elev, this.spheval[i].phi);
				this.ncan[i] = num.value;
				if((i == currentsource) && guiflag )
				{
					//var val = (halfwidth - (num.value * width)) * -1;
					//	ncanslider.value = num.value;
					{ncannumbox.value = num.value}.defer;
				};
				if (guiflag) {
					{this.ncanbox[i].value = num.value}.defer;
				};
			};

			this.businiboxProxy[i].action = { | num |
				this.espacializador[i].set(\elev, this.spheval[i].phi);
				this.setSynths(i, \elev, this.spheval[i].phi);
				this.synt[i].set(\elev, this.spheval[i].phi);
				this.busini[i] = num.value;
				if((i == currentsource) && guiflag)
				{
					//var val = (halfwidth - (num.value * width)) * -1;
					//	ncanslider.value = num.value;
					{busininumbox.value = num.value}.defer;
				};
				if (guiflag) {
					{this.businibox[i].value = num.value}.defer;
				};
			};

			this.rateboxProxy[i].action = { | num |
				this.espacializador[i].set(\grainrate, num.value);
				this.setSynths(i, \grainrate, num.value);
				this.synt[i].set(\grainrate, num.value);
				grainrate[i] = num.value;
				if (guiflag) {
					ratebox[i].value = num.value;
					if (i == currentsource) {
						{rateslider.value = (num.value - 1) / 59}.defer;
						{ratenumbox.value = num.value}.defer;
					};
				};
				if (this.ossiarate.notNil) {
					if (this.ossiarate[i].v != num.value) {
						this.ossiarate[i].v_(num.value);
					};
				};
			};

			this.winboxProxy[i].action = { | num |
				this.espacializador[i].set(\winsize, num.value);
				this.setSynths(i, \winsize, num.value);
				this.synt[i].set(\winsize, num.value);
				winsize[i] = num.value;
				if (guiflag) {
					winbox[i].value = num.value;
					if (i == currentsource) {
						{winslider.value = num.value * 5}.defer;
						{winumbox.value = num.value}.defer;
					};
				};
				if (this.ossiawin.notNil) {
					if (this.ossiawin[i].v != num.value) {
						this.ossiawin[i].v_(num.value);
					};
				};
			};

			this.randboxProxy[i].action = { | num |
				this.espacializador[i].set(\winrand, num.value);
				this.setSynths(i, \winrand, num.value);
				this.synt[i].set(\winrand, num.value);
				winrand[i] = num.value;
				if (guiflag) {
					randbox[i].value = num.value;
					if (i == currentsource) {
						{randslider.value = num.value.sqrt}.defer;
						{randnumbox.value = num.value}.defer;
					};
				};
				if (this.ossiawin.notNil) {
					if (this.ossiarand[i].v != num.value) {
						this.ossiarand[i].v_(num.value);
					};
				};
			};

			this.tfieldProxy[i].action = { | path |
				if (sombuf[i].notNil) {
					sombuf[i].free;
				};

				if (path.notNil || (path.value != "")) {
					if (this.streamdisk[i].not) {
						sombuf[i] = Buffer.read(server, path.value, action: { | buf |
							"Loaded file".postln;
						});
					} {
						"To stream file".postln;
					};
				};

				if (guiflag) {
					{this.tfield[i].value = path.value}.defer;
				};
				if (this.ossiaaud.notNil) {
					this.ossiaaud[i].description = PathName(path.value).fileNameWithoutExtension;
				};
			};

			control.dock(this.xboxProxy[i], "x_axisProxy_" ++ i);
			control.dock(this.yboxProxy[i], "y_axisProxy_" ++ i);
			control.dock(this.zboxProxy[i], "z_axisProxy_" ++ i);
			control.dock(this.vboxProxy[i], "levelProxy_" ++ i);
			control.dock(this.dpboxProxy[i], "dopamtProxy_" ++ i);
			control.dock(this.gboxProxy[i], "revglobalProxy_" ++ i);
			control.dock(this.dstrvboxProxy[i], "localrevkindProxy_" ++ i);
			control.dock(this.lboxProxy[i], "revlocalProxy_" ++ i);
			control.dock(this.rmboxProxy[i], "localroomProxy_" ++ i);
			control.dock(this.dmboxProxy[i], "localdampProxy_" ++ i);
			control.dock(this.aboxProxy[i], "angleProxy_" ++ i);
			control.dock(this.rboxProxy[i], "rotationProxy_" ++ i);
			control.dock(this.dboxProxy[i], "directivityProxy_" ++ i);
			control.dock(this.cboxProxy[i], "contractionProxy_" ++ i);
			control.dock(this.rateboxProxy[i], "grainrateProxy_" ++ i);
			control.dock(this.winboxProxy[i], "windowsizeProxy_" ++ i);
			control.dock(this.randboxProxy[i], "randomwindowProxy_" ++ i);
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


		this.masterlevProxy.action_({ | num |

			this.globDec.set(\level, num.value);

			if (guiflag) {
				this.masterslider.value = num.value;
			};

			if (this.ossiamaster.notNil) {
				if (this.ossiamaster.v != num.value) {
					this.ossiamaster.v_(num.value);
				};
			};

		});


		this.clsrvboxProxy.action_({ | num |

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

				if (revGlobal.notNil)
				{ this.revGlobal.set(\gate, 0); };

				this.revGlobal = Synth.new(\revGlobalAmb++clsRvtypes,
					[\gbfbus, gbfbus, \gbixfbus, gbixfbus, \gate, 1,
						\room, clsrm, \damp, clsdm] ++
					a4SpecPar.value(max((clsrv - 2), 0)),
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

			if (guiflag) {
				{ clsReverbox.value = num.value }.defer;
			};
			if (this.ossiacls.notNil) {
				if (this.ossiacls.v != num.value) {
					this.ossiacls.v_(num.value);
				};
			};
		});


		this.clsrmboxProxy.action_({ | num |

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


		this.clsdmboxProxy.action_({ | num |

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


		this.oxnumboxProxy.action_({ | num |

			if (this.ossiaorigine.isNil) {

				this.nfontes.do { | i |
					this.spheval[i] = (cartval[i].x_(cartval[i].x - num.value
						+ origine.x)).asSpherical.
					rotate(pitch.neg).tilt(roll.neg).tumble(heading.neg);

					this.zlev[i] = this.spheval[i].z;

					if(this.espacializador[i].notNil || playingBF[i]) {
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
				};
			} {

				this.ossiaorigine.v_([num.value, this.oynumboxProxy.value, this.oznumboxProxy.value]);
				this.ossiaCartBack = false;

				this.nfontes.do {  | i |
					var sphe = (cartval[i].x_(cartval[i].x - num.value
						+ origine.x)).asSpherical.
					rotate(pitch.neg).tilt(roll.neg).tumble(heading.neg);

					this.ossiasphe[i].v_([sphe.rho,
						(sphe.theta - halfPi).wrap(-pi, pi), sphe.phi]);

					this.zlev[i] = this.spheval[i].z;

					if(this.espacializador[i].notNil || playingBF[i]) {
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
				};

				this.ossiaCartBack = true;

			};

			origine.x_(num.value);

			if (guiflag) {
				{novoplot.value;}.defer;
				{this.oxnumbox.value = num.value;}.defer;
			};
		});



		this.oynumboxProxy.action_({ | num |

			if (this.ossiaorigine.isNil) {

				this.nfontes.do { | i |
					this.spheval[i] = (cartval[i].y_(cartval[i].y - num.value
						+ origine.y)).asSpherical.
					rotate(pitch.neg).tilt(roll.neg).tumble(heading.neg);

					this.zlev[i] = this.spheval[i].z;

					if(this.espacializador[i].notNil || playingBF[i]) {
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
				};
			} {

				this.ossiaorigine.v_([this.oxnumboxProxy.value, num.value, this.oznumboxProxy.value]);
				this.ossiaCartBack = false;

				this.nfontes.do { | i |
					var sphe = (cartval[i].y_(cartval[i].y - num.value
						+ origine.y)).asSpherical.
					rotate(pitch.neg).tilt(roll.neg).tumble(heading.neg);

					this.ossiasphe[i].v_([sphe.rho,
						(sphe.theta - halfPi).wrap(-pi, pi), sphe.phi]);

					this.zlev[i] = this.spheval[i].z;

					if(this.espacializador[i].notNil || playingBF[i]) {
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
				};

				this.ossiaCartBack = true;

			};

			origine.y_(num.value);

			if (guiflag) {
				{novoplot.value;}.defer;
				{this.oynumbox.value = num.value;}.defer;
			};
		});


		this.oznumboxProxy.action_({ | num |

			if (this.ossiaorigine.isNil) {

				this.nfontes.do { | i |
					var sphe = this.spheval[i];
					this.spheval[i] = (cartval[i].z_(cartval[i].z - num.value
						+ origine.z)).asSpherical.
					rotate(pitch.neg).tilt(roll.neg).tumble(heading.neg);

					this.zlev[i] = this.spheval[i].z;

					if(this.espacializador[i].notNil || playingBF[i]) {
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
				};
			} {

				this.ossiaorigine.v_([this.oxnumboxProxy.value, this.oynumboxProxy.value, num.value]);
				this.ossiaCartBack = false;

				this.nfontes.do { | i |
					var sphe = (cartval[i].z_(cartval[i].z - num.value
						+ origine.z)).asSpherical.
					rotate(pitch.neg).tilt(roll.neg).tumble(heading.neg);

					this.ossiasphe[i].v_([sphe.rho,
						(sphe.theta - halfPi).wrap(-pi, pi), sphe.phi]);

					this.zlev[i] = this.spheval[i].z;

					if(this.espacializador[i].notNil || playingBF[i]) {
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
				};

				this.ossiaCartBack = true;

			};

			origine.z_(num.value);

			if (guiflag) {
				{novoplot.value;}.defer;
				{this.oznumbox.value = num.value;}.defer;
			};
		});



		this.pitchnumboxProxy.action_({ | num |

			if (this.ossiaorient.isNil) {

				this.nfontes.do { | i |
					this.spheval[i] = this.spheval[i].rotate(pitch - num.value);

					this.zlev[i] = this.spheval[i].z;

					if(this.espacializador[i].notNil || playingBF[i]) {
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
				};
			} {

				this.ossiaorient.v_([num.value, this.rollnumboxProxy.value, this.headingnumboxProxy.value]);
				this.ossiaCartBack = false;

				this.nfontes.do { | i |
					var rot = this.spheval[i].rotate(pitch - num.value);

					this.ossiasphe[i].v_([rot.rho,
						(rot.theta - halfPi).wrap(-pi, pi), rot.phi]);

					this.zlev[i] = this.spheval[i].z;

					if(this.espacializador[i].notNil || playingBF[i]) {
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
				};

				this.ossiaCartBack = true;

			};

			pitch = num.value;

			if (guiflag) {
				{novoplot.value;}.defer;
				{this.pitchnumbox.value = num.value;}.defer;
			};
		});


		this.rollnumboxProxy.action_({ | num |

			if (this.ossiaorient.isNil) {

				this.nfontes.do { | i |
					this.spheval[i] = this.spheval[i].tilt(roll - num.value);

					this.zlev[i] = this.spheval[i].z;

					if(this.espacializador[i].notNil || playingBF[i]) {
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
				};
			} {

				this.ossiaorient.v_([this.pitchnumboxProxy.value, num.value, this.headingnumboxProxy.value]);
				this.ossiaCartBack = false;

				this.nfontes.do { | i |
					var rot = this.spheval[i].tilt(roll - num.value);

					this.ossiasphe[i].v_([rot.rho,
						(rot.theta - halfPi).wrap(-pi, pi), rot.phi]);

					this.zlev[i] = this.spheval[i].z;

					if(this.espacializador[i].notNil || playingBF[i]) {
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
				};

				this.ossiaCartBack = true;

			};

			roll = num.value;

			if (guiflag) {
				{novoplot.value;}.defer;
				{this.rollnumbox.value = num.value;}.defer;
			};
		});


		this.headingnumboxProxy.action_({ | num |
			if (this.ossiaorient.isNil) {

				this.nfontes.do { | i |
					this.spheval[i] = this.spheval[i].tumble(heading - num.value);

					this.zlev[i] = this.spheval[i].z;

					if(this.espacializador[i].notNil || playingBF[i]) {
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
				};
			} {

				this.ossiaorient.v_([this.pitchnumboxProxy.value, this.rollnumboxProxy.value, num.value]);
				this.ossiaCartBack = false;

				this.nfontes.do { | i |
					var rot = this.spheval[i].tumble(heading - num.value);

					this.ossiasphe[i].v_([rot.rho,
						(rot.theta - halfPi).wrap(-pi, pi), rot.phi]);

					this.zlev[i] = this.spheval[i].z;

					if(this.espacializador[i].notNil || playingBF[i]) {
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
				};

				this.ossiaCartBack = true;

			};

			heading = num.value;

			if (guiflag) {
				{novoplot.value;}.defer;
				{this.headingnumbox.value = num.value;}.defer;
			};
		});


		control.dock(this.clsrvboxProxy, "globrevkindProxy");
		control.dock(this.clsrmboxProxy, "localroomProxy");
		control.dock(this.clsdmboxProxy, "localdampProxy");
		control.dock(this.oxnumboxProxy, "oxProxy");
		control.dock(this.oynumboxProxy, "oyProxy");
		control.dock(this.oznumboxProxy, "ozProxy");
		control.dock(this.pitchnumboxProxy, "pitchProxy");
		control.dock(this.rollnumboxProxy, "rollProxy");
		control.dock(this.headingnumboxProxy, "headingProxy");


		///////////////////////////////////////////////////


		playInFunc = Array.newClear(4); // one for File, Stereo, BFormat, Stream - streamed file;

		this.streambuf = Array.newClear(this.nfontes);

		// array of functions, 1 for each source (if defined), that will be launched on Automation's "play"
		this.triggerFunc = Array.newClear(this.nfontes);
		//companion to above. Launched by "Stop"
		this.stopFunc = Array.newClear(this.nfontes);


		// can place headtracker rotations in these functions - don't forget that the synthdefs
		// need to have there values for heading, roll and pitch "set" by serial routine
		// NO - DON'T PUT THIS HERE - Make a global synth with a common input bus


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
		spher = cart.clump(3).collect({ | cart, i |
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


		/// non ambisonc spatiaizers setup


		if (speaker_array.notNil) {

			var max_func, min_func, dimention, vbap_setup, radiusses, elevations, adjust;

			this.nonambibus = outbus;

			numoutputs = speaker_array.size;

			max_func = { |x| // extract the highest value from an array
				var rep = 0;
				x.do{ |item|
					if(item > rep,
						{ rep = item };
					)
				};
				rep };

			case
			{ speaker_array[0].size < 2 || speaker_array[0].size > 3 }
			{ ^"bad speaker array".postln }
			{ speaker_array[0].size == 2 }
			{ dimention = 2;

				radiusses = Array.newFrom(speaker_array).collect({ |val| val[1] });
				longest_radius = max_func.value(radiusses);

				adjust = Array.fill(numoutputs, { |i|
					[(longest_radius - radiusses[i]) / 334, longest_radius/radiusses[i]];
				});

				lowest_elevation = 0;
				highest_elevation = 0;

				speaker_array.collect({ |val| val.pop });

				vbap_setup = VBAPSpeakerArray(dimention, speaker_array.flat);
			}
			{ speaker_array[0].size == 3 }
			{ dimention = 3;

				radiusses = Array.newFrom(speaker_array).collect({ |val| val[2] });
				longest_radius = max_func.value(radiusses);

				adjust = Array.fill(numoutputs, { |i|
					[(longest_radius - radiusses[i]) / 334, longest_radius/radiusses[i]];
				});

				min_func = { |x| // extract the lowest value from an array
					var rep = 0;
					x.do{ |item|
						if(item < rep,
							{ rep = item };
					) };
					rep };

				elevations = Array.newFrom(speaker_array).collect({ |val| val[1] });
				lowest_elevation = min_func.value(elevations);
				highest_elevation = max_func.value(elevations);

				speaker_array.collect({ |val| val.pop });

				vbap_setup = VBAPSpeakerArray(dimention, speaker_array);
			};

			vbap_buffer = Buffer.loadCollection(server, vbap_setup.getSetsAndMatrices);

			perfectSphereFunc = { |sig|
				sig = Array.fill(numoutputs, { |i| DelayN.ar(sig[i],
					delaytime:adjust[i][0], mul:adjust[i][1]) });
			};

		} {

			var emulate_array, vbap_setup;

			numoutputs = 26;

			this.nonambibus = Bus.audio(server, numoutputs);

			emulate_array = [ [ 0, 90 ], [ 0, 45 ], [ 90, 45 ], [ 180, 45 ], [ -90, 45 ], [ 45, 35 ],
				[ 135, 35 ], [ -135, 35 ], [ -45, 35 ], [ 0, 0 ], [ 45, 0 ], [ 90, 0 ], [ 135, 0 ],
				[ 180, 0 ], [ -135, 0 ], [ -90, 0 ], [ -45, 0 ], [ 45, -35 ], [ 135, -35 ], [ -135, -35 ],
				[ -45, -35 ], [ 0, -45 ], [ 90, -45 ], [ 180, -45 ], [ -90, -45 ], [ 0, -90 ] ];

			vbap_setup = VBAPSpeakerArray(3, emulate_array);
			// emulate 26-point Lebedev grid

			vbap_buffer = Buffer.loadCollection(server, vbap_setup.getSetsAndMatrices);

			longest_radius = 3;
			lowest_elevation = -90;
			highest_elevation = 90;

			perfectSphereFunc = { |sig|
				sig;
			};

			SynthDef.new("nonAmbi2FuMa", {
				var sig = In.ar(nonambibus, numoutputs);
				sig = FoaEncode.ar(sig, FoaEncoderMatrix.newDirections(emulate_array.degrad));
				Out.ar(fumabus, sig);
			}).add;

		};

		limit_radius = longest_radius * 0.01; // prevent amplitue spike


		// define ambisonic decoder


		if (decoder.notNil) {

			if (suboutbus.notNil) {
				subOutFunc = { |signal, sublevel = 1|
					var subOut = Mix.ar(signal) * sublevel * 0.5;
					Out.ar(suboutbus, subOut);
				};
			} {
				subOutFunc = { |signal, sublevel| };
			};

			case
			{ maxorder == 1 }
			{ convert_fuma = false;
				convert_ambix = true;
				convert_direct = false;

				iemConvert = { |in, azi = 0, elev = 0, level = 0|
					var ambSig = PanAmbi1O.ar(in, azi, elev, level.dbamp);
					[ambSig[0],ambSig[2],ambSig[3],ambSig[1]];
				};

				SynthDef.new("ambiConverter", { | gate = 1 |
					var ambixsig, env;
					env = EnvGen.kr(Env.asr(curve:\hold), gate, doneAction:2);
					ambixsig = In.ar(this.ambixbus, 4);
					ambixsig = FoaEncode.ar(ambixsig, FoaEncoderMatrix.newAmbix1) * env;
					Out.ar(fumabus, ambixsig);
				}).add;

				if (speaker_array.notNil) {
					SynthDef.new("globDecodeSynth",  { | heading = 0, roll = 0, pitch = 0,
						sub = 1, level = 1 |
						var sig, nonambi;
						sig = In.ar(this.fumabus, 4);
						sig = FoaDecode.ar(sig, decoder);
						nonambi = In.ar(nonambibus, numoutputs);
						perfectSphereFunc.value(nonambi);
						sig = (sig + nonambi) * level;
						subOutFunc.value(sig, sub);
						Out.ar(outbus, sig);
					}).add;
				} {
					SynthDef.new("globDecodeSynth",  { | heading = 0, roll = 0, pitch = 0,
						sub = 1, level = 1 |
						var sig, nonambi;
						sig = In.ar(this.fumabus, 4);
						sig = FoaDecode.ar(sig, decoder);
						sig = sig * level;
						subOutFunc.value(sig, sub);
						Out.ar(outbus, sig);
					}).add;
				}
			}

			{ maxorder == 2 }
			{ convert_fuma = true;
				convert_ambix = false;
				convert_direct = true;

				iemConvert = { |in, azi = 0, elev = 0, level = 0|
					var ambSig = PanAmbi2O.ar(in, azi, elev, level.dbamp);
					[ambSig[0],ambSig[2],ambSig[3],
						ambSig[1],ambSig[5],ambSig[7],
						ambSig[8],ambSig[6],ambSig[4]];
				};

				SynthDef.new("ambiConverter", { | gate = 1 |
					var sig, env;
					env = EnvGen.kr(Env.asr(curve:\hold), gate, doneAction:2);
					sig = In.ar(this.fumabus, 9);
					sig = HOAConvert.ar(2, sig, \FuMa, \ACN_SN3D) * env;
					Out.ar(this.ambixbus, sig);
				}).add;

				if ((decoder.class == String) || (decoder.class == Symbol)) {
					var setup;

					setup = DecodeAmbi2O.addSetup(decoder,
						Array.newFrom(speaker_array).collect({ |val| val[0] }),
						Array.newFrom(speaker_array).collect({ |val| val[1] }) );

					SynthDef("globDecodeSynth", {
						| lf_hf = 0, xover = 400, sub = 1, level = 1 |
						var sig, nonambi;
						sig = In.ar(this.ambixbus, bFormNumChan);
						sig = setup.ar(sig, decoder);
						nonambi = In.ar(nonambibus, numoutputs);
						sig = (sig + nonambi) * level;
						perfectSphereFunc.value(sig);
						subOutFunc.value(sig, sub);
						Out.ar(outbus, sig);
					}).add;

				} {

					SynthDef("globDecodeSynth", {
						| lf_hf=0, xover=400, sub = 1, level = 1 |
						var sig, nonambi;
						sig = In.ar(this.ambixbus, bFormNumChan);
						sig = decoder.ar(sig[0], sig[1], sig[2], sig[3], sig[4],
							sig[5], sig[6], sig[7], sig[8], 0, lf_hf, xover:xover);
						nonambi = In.ar(nonambibus, numoutputs);
						perfectSphereFunc.value(nonambi);
						sig = ((sig * 2) + nonambi) * level; // *2 make up for generaly low output
						subOutFunc.value(sig, sub);
						Out.ar(outbus, sig);
					}).add;
				};
			}

			{ maxorder == 3 }
			{ convert_fuma = true;
				convert_ambix = false;
				convert_direct = true;

				iemConvert = { |in, azi = 0, elev = 0, level = 0|
					var ambSig = PanAmbi3O.ar(in, azi, elev, level.dbamp);
					[ambSig[0],ambSig[2],ambSig[3], ambSig[1],
						ambSig[5],ambSig[7],ambSig[8],ambSig[6],
						ambSig[4],ambSig[10],ambSig[12],ambSig[14],
						ambSig[15],ambSig[13],ambSig[11],ambSig[9]];
				};

				SynthDef.new("ambiConverter", { | gate = 1 |
					var sig, env;
					env = EnvGen.kr(Env.asr(curve:\hold), gate, doneAction:2);
					sig = In.ar(this.fumabus, 9);
					sig = HOAConvert.ar(2, sig, \FuMa, \ACN_SN3D) * env;
					Out.ar(this.ambixbus, sig);
				}).add;

				SynthDef("globDecodeSynth", {
					| lf_hf=0, xover=400, sub = 1, level = 1 |
					var sig, nonambi;
					sig = In.ar(this.ambixbus, bFormNumChan);
					sig = decoder.ar(sig[0], sig[1], sig[2], sig[3], sig[4],
						sig[5], sig[6], sig[7], sig[8], sig[9], sig[10], sig[11],
						sig[12], sig[13], sig[14], sig[15], 0, lf_hf, xover:xover);
					nonambi = In.ar(nonambibus, numoutputs);
					perfectSphereFunc.value(nonambi);
					sig = ((sig * 2) + nonambi) * level; // *2 make up for generaly low output
					subOutFunc.value(sig, sub);
					Out.ar(outbus, sig);
				}).add;
			}

			{ maxorder == 4 }
			{ convert_fuma = true;
				convert_ambix = false;
				convert_direct = true;

				iemConvert = { |in, azi = 0, elev = 0, level = 0|
					var ambSig = PanAmbi3O.ar(in, azi, elev, level.dbamp);
					[ambSig[0],ambSig[2],ambSig[3], ambSig[1],
						ambSig[5],ambSig[7],ambSig[8],ambSig[6],
						ambSig[4],ambSig[10],ambSig[12],ambSig[14],
						ambSig[15],ambSig[13],ambSig[11],ambSig[9]];
				};

				SynthDef.new("ambiConverter", { | gate = 1 |
					var sig, env;
					env = EnvGen.kr(Env.asr(curve:\hold), gate, doneAction:2);
					sig = In.ar(this.fumabus, 9);
					sig = HOAConvert.ar(2, sig, \FuMa, \ACN_SN3D) * env;
					Out.ar(this.ambixbus, sig);
				}).add;

				SynthDef("globDecodeSynth", {
					| lf_hf=0, xover=400, sub = 1, level = 1 |
					var sig, nonambi;
					sig = In.ar(this.ambixbus, bFormNumChan);
					sig = decoder.ar(sig[0], sig[1], sig[2], sig[3], sig[4],
						sig[5], sig[6], sig[7], sig[8], sig[9], sig[10], sig[11],
						sig[12], sig[13], sig[14],sig[15], sig[16], sig[17], sig[18],
						sig[19], sig[20], sig[21], sig[22], sig[23], sig[24],
						0, lf_hf, xover:xover);
					nonambi = In.ar(nonambibus, numoutputs);
					perfectSphereFunc.value(nonambi);
					sig = ((sig * 2) + nonambi) * level; // *2 make up for generaly low output
					subOutFunc.value(sig, sub);
					Out.ar(outbus, sig);
				}).add;
			}

			{ maxorder == 5 }
			{ convert_fuma = true;
				convert_ambix = false;
				convert_direct = true;

				iemConvert = { |in, azi = 0, elev = 0, level = 0|
					var ambSig = PanAmbi3O.ar(in, azi, elev, level.dbamp);
					[ambSig[0],ambSig[2],ambSig[3], ambSig[1],
						ambSig[5],ambSig[7],ambSig[8],ambSig[6],
						ambSig[4],ambSig[10],ambSig[12],ambSig[14],
						ambSig[15],ambSig[13],ambSig[11],ambSig[9]];
				};

				SynthDef.new("ambiConverter", { | gate = 1 |
					var sig, env;
					env = EnvGen.kr(Env.asr(curve:\hold), gate, doneAction:2);
					sig = In.ar(this.fumabus, 9);
					sig = HOAConvert.ar(2, sig, \FuMa, \ACN_SN3D) * env;
					Out.ar(this.ambixbus, sig);
				}).add;

				SynthDef("globDecodeSynth", {
					| lf_hf=0, xover=400, sub = 1, level = 1 |
					var sig, nonambi;
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
					nonambi = In.ar(nonambibus, numoutputs);
					perfectSphereFunc.value(nonambi);
					sig = ((sig * 2) + nonambi) * level; // *2 make up for generaly low output
					subOutFunc.value(sig, sub);
					Out.ar(outbus, sig);
				}).add;
			};
		};


		spatFuncs = Array.newClear(spatList.size); // contains the synthDef blocks for each spatialyers lib

		// Ambitools
		spatFuncs[0] = { |ref, input, radius, azimuth, elevation, difu, spre, contract, win, rate, rand|
			ref.value = HOAEncoder.ar(maxorder,
				(ref.value + input) * (1 - radius), CircleRamp.kr(azimuth, 0.1, -pi, pi),
				Lag.kr(elevation), 6, 1, Lag.kr((radius * 50)), longest_radius);
		};

		// HoaLib
		spatFuncs[1] = { |ref, input, radius, azimuth, elevation, difu, spre, contract, win, rate, rand|
			ref.value = HOALibEnc3D.ar(maxorder,
				(ref.value + input) * (1 - radius) * (longest_radius / (radius * 50)),
				CircleRamp.kr(azimuth, 0.1, -pi, pi), Lag.kr(elevation), 8);
		};

		// ADTB
		spatFuncs[2] = { |ref, input, radius, azimuth, elevation, difu, spre, contract, win, rate, rand|
			ref.value = HOAmbiPanner.ar(maxorder,
				(ref.value + input) * (1 - radius) * (longest_radius / (radius * 50)),
				CircleRamp.kr(azimuth, 0.1, -pi, pi), Lag.kr(elevation), 8);
		};

		// AmbIEM
		spatFuncs[3] = { |ref, input, radius, azimuth, elevation, difu, spre, contract, win, rate, rand|
			ref.value = iemConvert.value(
				(ref.value + input) * (1 - radius) * (longest_radius / (radius * 50)),
				CircleRamp.kr(azimuth, 0.1, -pi, pi), Lag.kr(elevation), 6);
		};

		// ATK
		spatFuncs[4] = { |ref, input, radius, azimuth, elevation, difu, spre, contract, win, rate, rand|
			var sig, diffuse, spread, omni, rad = radius * 50, globallev = (1 / radius.sqrt) - 1;
			// lower tail of curve to zero
			sig = (ref.value + input) * (1 - radius) * (longest_radius / rad);
			omni = FoaEncode.ar(sig, foaEncoderOmni);
			spread = FoaEncode.ar(sig, foaEncoderSpread);
			diffuse = FoaEncode.ar(sig, foaEncoderDiffuse);
			sig = Select.ar(difu, [omni, diffuse]);
			sig = Select.ar(spre, [sig, spread]);
			sig = FoaTransform.ar(sig, 'push', halfPi * contract, azimuth, elevation);

			ref.value = FoaTransform.ar(sig, 'proximity', rad, 8)
		};

		// BF-FMH
		spatFuncs[5] = { |ref, input, radius, azimuth, elevation, difu, spre, contract, win, rate, rand|
			ref.value = FMHEncode1.ar((ref.value + input) * (1 - radius) * (1 - radius),
				azimuth, elevation, longest_radius / (radius * 50), 10)
		};

		// joshGrain
		spatFuncs[6] = { |ref, input, radius, azimuth, elevation, difu, spre, contract, win, rate, rand|
			ref.value = MonoGrainBF.ar(ref.value + input, win, rate, rand, azimuth, 1 - contract,
				elevation, 1 - contract, rho: Lag.kr(longest_radius / (radius * 50)),
				mul: 1 + (0.5 - win) + (1 - (rate / 40))
			);
		};

		// VBAP
		spatFuncs[7] = { |ref, input, radius, azimuth, elevation, difu, spre, contract, win, rate, rand|
			var elevexcess, elev, azi, rad2deg = 57.295779513082;
			azi = azimuth * rad2deg; // convert to degrees
			elev = elevation * rad2deg; // convert to degrees
			elevexcess = Select.kr(elev < lowest_elevation, [0, elev.abs]);
			elevexcess = Select.kr(elev > highest_elevation, [0, elev]); // get elevation overshoot
			elev = elev.clip(lowest_elevation, highest_elevation); // restrict to between min & max

			ref.value = VBAP.ar(numoutputs,
				(ref.value + input) * (1 - radius) * (longest_radius / (radius * 50)),
				vbap_buffer.bufnum, CircleRamp.kr(azimuth, 0.1, -180, 180), Lag.kr(elevation),
				((1 - contract) + (elevexcess / 90)) * 100);
		};

		outPutFuncs = Array.newClear(3);
		// contains the synthDef blocks for each spatialyers and global reverb outputs

		outPutFuncs[0] = { |dry, wet, globrev|
			Out.ar(gbixfbus, wet * globrev.clip(0, 1));
			Out.ar(ambixbus, wet);
		};

		outPutFuncs[1] = { |dry, wet, globrev|
			Out.ar(gbfbus, wet * globrev.clip(0, 1));
			Out.ar(fumabus, wet);
		};

		outPutFuncs[2] = { |dry, wet, globrev|
			Out.ar(gbfbus, dry * globrev.clip(0, 1));
			Out.ar(nonambibus, wet);
		};


		makeSpatialisers = { | rev_type |
			var out_type = 0;

			spatList.do{ |item, i|

				case
				{ i <= lastSN3D } { out_type = 0 }
				{ (i > lastSN3D) && (i <= lastFUMA) } { out_type = 1 }
				{ i > lastFUMA } { out_type = 2 };

				SynthDef.new(item++"Chowning"++rev_type,  {
					| inbus, azim = 0, elev = 0, radius = 0,
					dopamnt = 0, glev = 0, llev = 0,
					insertFlag = 0, insertOut, insertBack,
					room = 0.5, damp = 05, wir, df, sp,
					contr = 1, grainrate = 10, winsize = 0.1, winrand = 0 |

					var rd, az,
					p, globallev, lrevRef = Ref(0),
					dis = radius.clip(limit_radius, 1);
					az = azim - halfPi;
					p = In.ar(inbus, 1);
					p = LPF.ar(p, (1 - dis) * 18000 + 2000); // attenuate high freq with distance

					// Doppler
					rd = Lag.kr(dis * 340);
					p = DelayC.ar(p, 0.2, rd/1640.0 * dopamnt);

					// applie distance attenuation before mixxing in reverb to keep trail off
					localReverbFunc.value(lrevRef, p, wir, dis * llev, room, damp);

					spatFuncs[i].value(lrevRef, p, dis, az, elev, df, sp, contr,
						winsize, grainrate, winrand);

					// Global reverberation
					globallev = (1 / radius.sqrt) - 1; // lower tail of curve to zero

					outPutFuncs[out_type].value(p, lrevRef.value, globallev);
				}).add;


				SynthDef.new(item++"StereoChowning"++rev_type,  {
					| inbus, azim = 0, elev = 0, radius = 0,
					dopamnt = 0, glev = 0, llev = 0, angle = 1.05,
					insertFlag = 0, insertOut, insertBack,
					room = 0.5, damp = 05, wir, df, sp,
					contr = 1, grainrate = 10, winsize = 0.1, winrand = 0 |

					var rd, az, p, globallev,
					lrev1Ref = Ref(0), lrev2Ref = Ref(0),
					dis = radius.clip(limit_radius, 1);
					az = azim - halfPi;
					p = In.ar(inbus, 2);
					p = LPF.ar(p, (1 - dis) * 18000 + 2000); // attenuate high freq with distance

					// Doppler
					rd = Lag.kr(dis * 340);
					p = DelayC.ar(p, 0.2, rd/1640.0 * dopamnt);

					// applie distance attenuation before mixxing in reverb to keep trail off
					localReverbStereoFunc.value(lrev1Ref, lrev2Ref, p[0], p[1], wir, dis * llev, room, damp);

					spatFuncs[i].value(lrev1Ref, p[0], dis, az - (angle * (1 - dis)),
						elev, df, sp, contr, winsize, grainrate, winrand);
					spatFuncs[i].value(lrev2Ref, p[1], dis, az + (angle * (1 - dis)),
						elev, df, sp, contr, winsize, grainrate, winrand);

					// Global reverberation
					globallev = (1 / radius.sqrt) - 1; // lower tail of curve to zero

					outPutFuncs[out_type].value(Mix.new(p) * 0.5,
						lrev1Ref.value + lrev2Ref.value, globallev);
				}).add;

			};


			SynthDef.new("ATK2Chowning"++rev_type,  {
				| inbus, radius = 0,
				dopamnt = 0, glev = 0, llev = 0,
				insertFlag = 0, insertOut, insertBack,
				room = 0.5, damp = 05, wir|

				var ambSig, rd,
				globallev, gsig, p, lrevRef = Ref(0),
				dis = radius.clip(limit_radius, 1);
				p = In.ar(inbus, 1);
				p = LPF.ar(p, (1 - dis) * 18000 + 2000); // attenuate high freq with distance

				// Doppler
				rd = Lag.kr(dis * 340);
				p = DelayC.ar(p, 0.2, rd/1640.0 * dopamnt);

				// Global reverberation
				globallev = 1 / dis.sqrt; // lower tail of curve to zero
				globallev = globallev / 3; // scale it so that it values 1 close to origin
				gsig = p * globallev.clip(0, 1) * glev;
				Out.ar(gbus, gsig); // send part of direct signal global reverb synth

				// applie distance attenuation before mixxing in reverb to keep trail off
				localReverbFunc.value(lrevRef, p, wir, dis * llev, room, damp);
				p = FoaTransform.ar(p + lrevRef.value, 'proximity',
					(dis * 50).clip(0.1, 50));

				Out.ar(fumabus, p);
			}).add;

		}; //end makeSpatialisers


		// allpass reverbs
		if (maxorder == 1) {

			SynthDef.new("revGlobalAmb_pass",  { | gate = 1, room = 0.5, damp = 0.5 |
				var env, temp, convsig, sig, sigx, sigf = In.ar(gbfbus, 4);
				sigx = In.ar(gbixfbus, 4);
				sig = In.ar(gbus, 1);
				sigx = FoaEncode.ar(sigx, FoaEncoderMatrix.newAmbix1);
				env = EnvGen.kr(Env.asr(1), gate, doneAction:2);
				sigf = FoaDecode.ar(sigf, b2a);
				sig = sig + sigf + sigx;
				16.do({ sig = AllpassC.ar(sig, 0.08, room * { Rand(0, 0.08) }.dup(4) + { Rand(0, 0.001) },
					damp * 2)});
				sig = FoaEncode.ar(sig, a2b);
				sig = sig * env;
				Out.ar(this.fumabus, sig);
			}).add;

		} {

			SynthDef.new("revGlobalAmb_pass",  { | gate = 1, room = 0.5, damp = 0.5 |
				var env, w, x, y, z, r, s, t, u, v,
				soaSig, tmpsig, sig, sigx, sigf = In.ar(soaRevBus, 9);
				sigx = In.ar(gbixfbus, 9);
				sigx = HOAConvert.ar(2, sigx, \FuMa, \ACN_SN3D);
				sig = In.ar(gbus, 1);
				env = EnvGen.kr(Env.asr(1), gate, doneAction:2);
				sig = sig + sigf + sigx;
				sig = AtkMatrixMix.ar(sig, soa_a12_decoder_matrix);
				16.do({ sig = AllpassC.ar(sig, 0.08, room * { Rand(0, 0.08) }.dup(12) + { Rand(0, 0.001) },
					damp * 2)});
				#w, x, y, z, r, s, t, u, v = AtkMatrixMix.ar(sig, soa_a12_encoder_matrix) * env;
				soaSig = [w, x, y, z, r, s, t, u, v];
				Out.ar(this.fumabus, soaSig);
			}).load(server);
		};

		//run the makeSpatialisers function for each types of local reverbs

		localReverbFunc = { | lrevRef, p, rirWspectrum, locallev, room, damp |
			var temp;
			temp = p;
			16.do({ temp = AllpassC.ar(temp, 0.08, room * { Rand(0, 0.08) } + { Rand(0, 0.001) },
				damp * 2)});
			lrevRef.value = temp * locallev;
		};

		localReverbStereoFunc = { | lrev1Ref, lrev2Ref, p1, p2, rirZspectrum, locallev, room, damp |
			var temp1 = p1, temp2 = p2;
			8.do({ temp1 = AllpassC.ar(temp1, 0.08, room * { Rand(0, 0.08) } + { Rand(0, 0.001) },
				damp * 2)});
			8.do({ temp2 = AllpassC.ar(temp2, 0.08, room * { Rand(0, 0.08) } + { Rand(0, 0.001) },
				damp * 2)});
			lrev1Ref.value = temp1 * locallev;
			lrev2Ref.value = temp2 * locallev;
		};

		makeSpatialisers.value(rev_type:"_pass");

		// freeverb defs

		if (maxorder == 1) {

			SynthDef.new("revGlobalAmb_free",  { | gate = 1, room = 0.5, damp = 0.5 |
				var env, temp, convsig, sig, sigx, sigf = In.ar(gbfbus, 4);
				sigx = In.ar(gbixfbus, 4);
				sig = In.ar(gbus, 1);
				sigx = FoaEncode.ar(sigx, FoaEncoderMatrix.newAmbix1);
				env = EnvGen.kr(Env.asr(1), gate, doneAction:2);
				sigf = FoaDecode.ar(sigf, b2a);
				sig = sig + sigf + sigx;
				convsig = [
					FreeVerb2.ar(sig[0], sig[1], mix: 1, room: room, damp: damp),
					FreeVerb2.ar(sig[2], sig[3], mix: 1, room: room, damp: damp)];
				convsig = FoaEncode.ar(convsig.flat, a2b);
				convsig = convsig * env;
				Out.ar(this.fumabus, convsig);
			}).add;

		} {

			SynthDef.new("revGlobalSoaA12_free",  { | gate = 1, room = 0.5, damp = 0.5 |
				var env, w, x, y, z, r, s, t, u, v,
				soaSig, tmpsig, sig, sigx, sigf = In.ar(soaRevBus, 9);
				sigx = In.ar(gbixfbus, 9);
				sigx = HOAConvert.ar(2, sigx, \FuMa, \ACN_SN3D);
				sig = In.ar(gbus, 1);
				env = EnvGen.kr(Env.asr(1), gate, doneAction:2);
				sig = sig + sigf + sigx;
				sig = AtkMatrixMix.ar(sig, soa_a12_decoder_matrix);
				tmpsig = [
					FreeVerb2.ar(sig[0], sig[1], mix: 1, room: room, damp: damp),
					FreeVerb2.ar(sig[2], sig[3], mix: 1, room: room, damp: damp),
					FreeVerb2.ar(sig[4], sig[5], mix: 1, room: room, damp: damp),
					FreeVerb2.ar(sig[6], sig[7], mix: 1, room: room, damp: damp),
					FreeVerb2.ar(sig[8], sig[9], mix: 1, room: room, damp: damp),
					FreeVerb2.ar(sig[10], sig[11], mix: 1, room: room, damp: damp)];
				tmpsig = tmpsig.flat * 4 * env;
				#w, x, y, z, r, s, t, u, v = AtkMatrixMix.ar(tmpsig, soa_a12_encoder_matrix);
				soaSig = [w, x, y, z, r, s, t, u, v];
				Out.ar(fumabus, soaSig);
			}).add;

		};


		//run the makeSpatialisers function for each types of local reverbs


		localReverbFunc = { | lrevRef, p, rirWspectrum, locallev, room = 0.5, damp = 0.5 |
			lrevRef.value = FreeVerb.ar(p, mix: 1, room: room, damp: damp, mul: locallev);
		};

		localReverbStereoFunc = { | lrev1Ref, lrev2Ref, p1, p2, rirZspectrum, locallev,
			room = 0.5, damp = 0.5|
			var temp;
			temp = FreeVerb2.ar(p1, p2, mix: 1, room: room, damp: damp, mul: locallev);
			lrev1Ref.value = temp[0];
			lrev2Ref.value = temp[1];
		};

		makeSpatialisers.value(rev_type:"_free");

		// function for no-reverb option

		localReverbFunc = { | lrevRef, p, rirWspectrum, locallev, room, damp|
		};

		localReverbStereoFunc = { | lrev1Ref, lrev2Ref, p1, p2, rirZspectrum, locallev, room, damp |
		};

		makeSpatialisers.value(rev_type:"");

		rirList = Array.newClear();

		server.sync;

		if (rirBank.notNil) {

			/////////// START loading rirBank /////////////////////

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

			//server.sync;

			"Loading rir bank".postln;

			rirList.do({ |item, count|

				bufAformat[count].free;
				bufWXYZ[count].free;

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
				//server.sync;
				rirWspectrum[count].preparePartConv(rirW[count], fftsize);
				//server.sync;
				rirW[count].free; // don't need time domain data anymore, just needed spectral version
				rirXspectrum[count].preparePartConv(rirX[count], fftsize);
				//server.sync;
				rirX[count].free;
				rirYspectrum[count].preparePartConv(rirY[count], fftsize);
				//server.sync;
				rirY[count].free;
				rirZspectrum[count].preparePartConv(rirZ[count], fftsize);
				//server.sync;
				rirZ[count].free;

				rirFLUspectrum[count] = Buffer.alloc(server, bufsize[count], 1);
				rirFRDspectrum[count] = Buffer.alloc(server, bufsize[count], 1);
				rirBLDspectrum[count] = Buffer.alloc(server, bufsize[count], 1);
				rirBRUspectrum[count] = Buffer.alloc(server, bufsize[count], 1);
				//server.sync;
				rirFLUspectrum[count].preparePartConv(rirFLU[count], fftsize);
				//server.sync;
				rirFLU[count].free;
				rirFRDspectrum[count].preparePartConv(rirFRD[count], fftsize);
				//server.sync;
				rirFRD[count].free;
				rirBLDspectrum[count].preparePartConv(rirBLD[count], fftsize);
				//server.sync;
				rirBLD[count].free;
				rirBRUspectrum[count].preparePartConv(rirBRU[count], fftsize);
				//server.sync;
				rirBRU[count].free;

				/////////// END loading rirBank /////////////////////
			});

			if (maxorder == 1) {

				SynthDef.new("revGlobalAmb_conv",  { | gbfbus, gbixfbus, gate = 1,
					fluir, frdir, bldir, bruir |
					var env, temp, convsig, sig, sigx, sigf = In.ar(gbfbus, 4);
					sigx = In.ar(gbixfbus, 4);
					sig = In.ar(gbus, 1);
					sigx = FoaEncode.ar(sigx, FoaEncoderMatrix.newAmbix1);
					env = EnvGen.kr(Env.asr(1), gate, doneAction:2);
					sigf = FoaDecode.ar(sigf, b2a);
					sig = sig + sigf + sigx;
					convsig = [
						PartConv.ar(sig[0], fftsize, fluir),
						PartConv.ar(sig[1], fftsize, frdir),
						PartConv.ar(sig[2], fftsize, bldir),
						PartConv.ar(sig[3], fftsize, bruir)];
					convsig = FoaEncode.ar(convsig, a2b);
					convsig = convsig * env;
					Out.ar(this.fumabus, convsig);
				}).add;

			} {

				rirList.do({ |item, count|

					rirA12 = Array.newClear(12);
					rirA12Spectrum = Array2D(rirNum, 12);
					12.do { | i |
						rirA12[i] = Buffer.readChannel(server, rirBank ++ "/" ++ item ++ "_SoaA12.wav",
							channels: [i]);
						//server.sync;
						rirA12Spectrum[count, i] = Buffer.alloc(server, bufsize[count], 1);
						//server.sync;
						rirA12Spectrum[count, i].preparePartConv(rirA12[i], fftsize);
						//server.sync;
						rirA12[i].free;
					};
				});

				SynthDef.new("revGlobalAmb_conv",  { | gate = 1, a0ir, a1ir, a2ir, a3ir,
					a4ir, a5ir, a6ir, a7ir, a8ir, a9ir, a10ir, a11ir |
					var env, w, x, y, z, r, s, t, u, v,
					soaSig, tmpsig, sig, sigx, sigf = In.ar(soaRevBus, 9);
					sigx = In.ar(gbixfbus, 9);
					sigx = HOAConvert.ar(2, sigx, \FuMa, \ACN_SN3D);
					sig = In.ar(gbus, 1);
					env = EnvGen.kr(Env.asr(1), gate, doneAction:2);
					sig = sig + sigf + sigx;
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
						PartConv.ar(sig[11], fftsize, a11ir)];
					tmpsig = tmpsig * 4 * env;
					#w, x, y, z, r, s, t, u, v = AtkMatrixMix.ar(tmpsig, soa_a12_encoder_matrix);
					soaSig = [w, x, y, z, r, s, t, u, v];
					Out.ar(this.fumabus, soaSig);
				}).add;

			};

			//run the makeSpatialisers function for each types of local reverbs

			localReverbFunc = { | lrevRef, p, wir, locallev, room, damp |
				lrevRef.value = PartConv.ar(p, fftsize, wir, locallev);
			};

			localReverbStereoFunc = { | lrev1Ref, lrev2Ref, p1, p2, zir, locallev,
				room, damp |
				var temp1 = p1, temp2 = p2;
				temp1 = PartConv.ar(p1, fftsize, zir, locallev);
				temp2 = PartConv.ar(p2, fftsize, zir, locallev);
				lrev1Ref.value = temp1 * locallev;
				lrev2Ref.value = temp2 * locallev;
			};


			makeSpatialisers.value(rev_type:"_conv");

			// create fonctions to pass rri busses as Synth arguments

			wSpecPar = {|i|
				[\wir, rirWspectrum[i]]
			};

			zSpecPar = {|i|
				[\zir, rirZspectrum[i]]
			};

			wxyzSpecPar = {|i|
				[\wir, rirWspectrum[i],
					\xir, rirXspectrum[i],
					\yir, rirYspectrum[i],
					\zir, rirZspectrum[i]]
			};

			a4SpecPar = { |i|
				[\fluir, rirFLUspectrum[i],
					\frdir, rirFRDspectrum[i],
					\bldir, rirBLDspectrum[i],
					\bruir, rirBRUspectrum[i]]
			};

			a12SpecPar = { |i|
				[\a0ir, rirA12Spectrum[i, 0],
					\a1ir, rirA12Spectrum[i, 1],
					\a2ir, rirA12Spectrum[i, 2],
					\a3ir, rirA12Spectrum[i, 3],
					\a4ir, rirA12Spectrum[i, 4],
					\a5ir, rirA12Spectrum[i, 5],
					\a6ir, rirA12Spectrum[i, 6],
					\a7ir, rirA12Spectrum[i, 7],
					\a8ir, rirA12Spectrum[i, 8],
					\a9ir, rirA12Spectrum[i, 9],
					\a10ir, rirA12Spectrum[i, 10],
					\a11ir, rirA12Spectrum[i, 11]]
			};

		};


		SynthDef.new("espacAFormatVerb", {
			| inbus, azim = 0, elev = 0, radius = 0,
			dopamnt = 0,
			glev = 0, llev = 0, contr = 1,
			gbfbus,
			sp = 0, df = 0
			//,insertFlag = 0, aFormatBusOutFoa, aFormatBusInFoa,
			//aFormatBusOutSoa, aFormatBusInSoa,
			//aFormatFoa, aFormatSoa, ambSigFoaProcessed, ambSigSoaProcessed
			|

			var p, ambSigSoa, ambSigFoa,
			junto, rd, az, ele, dis, xatras, yatras,
			globallev, locallev, gsig,
			intens,
			omni, spread, diffuse,
			soa_a12_sig;

			var lrev;
			var ambSigRef = Ref(0);
			contr = Lag.kr(contr);
			dis = radius;

			az = azim - halfPi;
			// az = CircleRamp.kr(az, 0.1, -pi, pi);
			// ele = Lag.kr(elev);
			ele = elev;
			dis = Select.kr(dis < 0, [dis, 0]);
			dis = Select.kr(dis > 1, [dis, 1]);

			// high freq attenuation
			p = In.ar(inbus, 1);
			p = LPF.ar(p, (1 - dis) * 18000 + 2000); // attenuate high freq with distance

			// Doppler
			rd = Lag.kr(dis * 340);
			p = DelayC.ar(p, 0.2, rd/1640.0 * dopamnt);

			// Global reverberation & intensity
			globallev = 1 / dis.sqrt;
			intens = globallev - 1;
			intens = intens.clip(0, 4);
			intens = intens * 0.25;

			globallev = globallev - 1.0; // lower tail of curve to zero
			globallev = globallev.clip(0, 1);
			globallev = globallev * glev;
			gsig = p * globallev;

			// Local reverberation
			locallev = dis  * llev;

			junto = p;

			// do second order encoding

			ambSigRef.value = FMHEncode0.ar(junto, az, ele, intens);

			omni = FoaEncode.ar(junto, foaEncoderOmni);
			spread = FoaEncode.ar(junto, foaEncoderSpread);
			diffuse = FoaEncode.ar(junto, foaEncoderDiffuse);
			junto = Select.ar(df, [omni, diffuse]);
			junto = Select.ar(sp, [junto, spread]);

			ambSigFoa = FoaTransform.ar(junto, 'push', halfPi * contr, az, ele, intens);

			ambSigSoa = [ambSigRef[0].value, ambSigRef[1].value, ambSigRef[2].value, ambSigRef[3].value,
				ambSigRef[4].value, ambSigRef[5].value, ambSigRef[6].value, ambSigRef[7].value,
				ambSigRef[8].value];

			dis = dis * 5.0;
			dis = Select.kr(dis < 0.001, [dis, 0.001]);
			//ambSigFoa = HPF.ar(ambSigFoa, 20); // stops bass frequency blow outs by proximity
			ambSigFoa = FoaTransform.ar(ambSigFoa, 'proximity', dis);

			/*
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
			*/
			//reverbOutFunc.value(soaRevBus, gbfbus, ambSigSoa, ambSigFoa, globallev, locallev);

			Out.ar(this.fumabus, ambSigFoa);
		}).load(server);


		SynthDef.new("ATK2AFormat", {
			| inbus, azim = 0, elev = 0, radius = 0,
			glev = 0, llev = 0.2,
			insertFlag = 0, aFormatBusOutFoa, aFormatBusInFoa,
			aFormatBusOutSoa, aFormatBusInSoa |
			var w, x, y, z, r, s, t, u, v, p, ambSigSoa, ambSigFoa,
			aFormatFoa, aFormatSoa, ambSigFoaProcessed, ambSigSoaProcessed,
			junto, rd, az, ele, dis,
			globallev = 0.0004, locallev, gsig;
			var lrev, intens;
			var ambSigRef = Ref(0);
			dis = radius;

			az = azim - halfPi;
			//az = CircleRamp.kr(az, 0.1, -pi, pi);
			//ele = Lag.kr(elev);
			ele = elev;
			dis = Select.kr(dis < 0, [dis, 0]);
			dis = Select.kr(dis > 1, [dis, 1]);

			// high freq attenuation
			p = In.ar(inbus, 1);
			p = LPF.ar(p, (1 - dis) * 18000 + 2000); // attenuate high freq with distance

			// Reverberação global
			globallev = 1 / dis.sqrt;
			intens = globallev - 1;
			intens = intens.clip(0, 4);
			intens = intens * 0.25;

			globallev = globallev - 1.0; // lower tail of curve to zero
			globallev = globallev.clip(0, 1);
			globallev = globallev * glev;
			gsig = p * globallev;

			// Reverberação local
			locallev = dis * llev;

			junto = p;

			ambSigRef.value = FMHEncode0.ar(junto, az, ele, intens);

			ambSigFoa = [ambSigRef[0].value, ambSigRef[1].value, ambSigRef[2].value,
				ambSigRef[3].value];
			ambSigSoa = [ambSigRef[0].value, ambSigRef[1].value, ambSigRef[2].value,
				ambSigRef[3].value,
				ambSigRef[4].value, ambSigRef[5].value, ambSigRef[6].value, ambSigRef[7].value,
				ambSigRef[8].value];

			//Out.ar(soaRevBus, (ambSigSoa*globallev) + (ambSigSoa*locallev));

			dis = dis * 5.0;
			dis = Select.kr(dis < 0.001, [dis, 0.001]);
			//ambSigFoa = HPF.ar(ambSigFoa, 20); // stops bass frequency blow outs by proximity
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

			Out.ar(this.fumabus, ambSigSoa);
		}).add;


		makeSynthDefPlayers = { | type, i = 0 |
			// 3 types : File, HWBus and SWBus - i duplicates with 0, 1 & 2

			SynthDef.new("playMono"++type, { | outbus, bufnum = 0, rate = 1,
				level = 0, tpos = 0, lp = 0, busini |
				var scaledRate, spos, playerRef;
				//SendTrig.kr(Impulse.kr(1), 101,  tpos); // debugging
				playerRef = Ref(0);
				playInFunc[i].value(playerRef, busini, bufnum, scaledRate, tpos, spos, lp, rate, 1);
				Out.ar(outbus, playerRef.value * Lag.kr(level));
			}).add;

			SynthDef.new("playStereo"++type, { | outbus, bufnum = 0, rate = 1,
				level = 0, tpos = 0, lp = 0, busini |
				var scaledRate, spos, playerRef;
				playerRef = Ref(0);
				playInFunc[i].value(playerRef, busini, bufnum, scaledRate, tpos, spos, lp, rate, 2);
				Out.ar(outbus, playerRef.value * Lag.kr(level));
			}).add;

			SynthDef.new("playBFormatATK"++type++"_4", {
				| outbus, bufnum = 0, rate = 1,
				level = 0, tpos = 0, lp = 0, rotAngle = 0, tilAngle = 0, tumAngle = 0,
				azim = 0, elev = 0, radius = 0,
				glev, llev, directang = 0, contr, dopamnt, busini,
				insertFlag = 0, aFormatBusOutFoa, aFormatBusInFoa,
				aFormatBusOutSoa, aFormatBusInSoa |

				var scaledRate, playerRef, wsinal, spos, pushang = 0,
				aFormatFoa, aFormatSoa, ambSigFoaProcessed, ambSigSoaProcessed,

				az, ele, dis, globallev, locallev,
				gsig, lsig, rd, intens;

				dis = radius;

				az = azim - halfPi;
				// az = CircleRamp.kr(az, 0.1, -pi, pi);
				// ele = Lag.kr(elev);
				ele = elev;
				pushang = dis * halfPi; // degree of sound field displacement
				dis = Select.kr(dis < 0, [dis, 0]);
				dis = Select.kr(dis > 1, [dis, 1]);
				playerRef = Ref(0);
				playInFunc[i].value(playerRef, busini, bufnum, scaledRate, tpos, spos, lp, rate, 4);

				rd = Lag.kr(dis * 340);
				playerRef.value = DelayC.ar(playerRef.value, 0.2, rd/1640.0 * dopamnt);

				wsinal = playerRef.value[0] * contr * Lag.kr(level) * dis * 2.0;

				Out.ar(outbus, wsinal);

				// global reverb
				globallev = 1 / dis.sqrt;
				intens = globallev - 1;
				intens = intens.clip(0, 4);
				intens = intens * 0.25;

				playerRef.value = FoaDirectO.ar(playerRef.value, directang); // directivity

				playerRef.value = FoaTransform.ar(playerRef.value, 'rotate', rotAngle,
					Lag.kr(level) * intens * (1 - contr));

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

				Out.ar(this.fumabus, playerRef.value);

				globallev = globallev - 1.0; // lower tail of curve to zero
				globallev = globallev.clip(0, 1);
				globallev = globallev * glev * 6;

				gsig = playerRef.value[0] * globallev;

				locallev = dis  * llev * 5;
				lsig = playerRef.value[0] * locallev;

				gsig = (playerRef.value * globallev) + (playerRef.value * locallev); // b-format
				Out.ar(gbfbus, gsig);
			}).add;


			SynthDef.new("playBFormatAmbitools"++type++"_4", {
				| outbus, bufnum = 0, rate = 1,
				level = 0, tpos = 0, lp = 0, rotAngle = 0, tilAngle = 0, tumAngle = 0,
				azim = 0, elev = 0, radius = 0,
				glev, llev, directang = 0, contr, dopamnt,
				busini, insertFlag = 0 |

				var scaledRate, playerRef, wsinal, spos, pushang = 0,
				aFormatFoa, aFormatSoa, ambSigFoaProcessed, ambSigSoaProcessed,

				az, ele, dis, globallev, locallev,
				gsig, //lsig, intens,
				rd;

				dis = radius;

				az = azim - halfPi;
				az = CircleRamp.kr(az, 0.1, -pi, pi);
				ele = Lag.kr(elev);
				// ele = elev;
				dis = Select.kr(dis < 0, [dis, 0]);
				dis = Select.kr(dis > 1, [dis, 1]);
				playerRef = Ref(0);
				playInFunc[i].value(playerRef, busini, bufnum, scaledRate, tpos, spos, lp, rate, 4);

				rd = Lag.kr(dis * 340);
				playerRef.value = DelayC.ar(playerRef.value, 0.2, rd/1640.0 * dopamnt);

				wsinal = playerRef.value[0] * contr * level * dis * 2.0;

				//Out.ar(outbus, wsinal);

				// global reverb
				globallev = 1 / dis.sqrt;
				/*intens = globallev - 1;
				intens = intens.clip(0, 4);
				intens = intens * 0.25;*/

				playerRef.value = FoaDecode.ar(playerRef.value, FoaDecoderMatrix.newAmbix1);
				playerRef.value = HOATransRotateAz.ar(1, playerRef.value, rotAngle);
				playerRef.value = HOABeamDirac2Hoa.ar(1, playerRef.value, 1, az, ele,
					focus:contr * dis.sqrt) * (1 - dis.squared) * level;

				Out.ar(this.ambixbus, playerRef.value);

				globallev = globallev - 1.0; // lower tail of curve to zero
				globallev = globallev.clip(0, 1);
				globallev = globallev * glev * 6;

				gsig = playerRef.value[0] * globallev;

				//locallev = dis  * llev * 5;
				//lsig = playerRef.value[0] * locallev;

				//gsig = (playerRef.value * globallev) + (playerRef.value * locallev); // b-format
				Out.ar(gbixfbus, gsig);
			}).add;


			[9, 16, 25, 36].do { |item, count|

				SynthDef.new("playBFormatATK"++type++"_"++item, {
					| outbus, bufnum = 0, rate = 1,
					level = 0, tpos = 0, lp = 0, rotAngle = 0, tilAngle = 0, tumAngle = 0,
					azim = 0, elev = 0, radius = 0,
					glev, llev, directang = 0, contr, dopamnt,
					busini,
					insertFlag = 0, aFormatBusOutFoa, aFormatBusInFoa,
					aFormatBusOutSoa, aFormatBusInSoa |

					var scaledRate, playerRef, wsinal, spos, pushang = 0,
					aFormatFoa, aFormatSoa, ambSigFoaProcessed, ambSigSoaProcessed,

					az, ele, dis, globallev, locallev,
					gsig, lsig, rd,
					intens;
					dis = radius;

					az = azim - halfPi;
					// az = CircleRamp.kr(az, 0.1, -pi, pi);
					// ele = Lag.kr(elev);
					ele = elev;
					pushang = dis * halfPi; // degree of sound field displacement
					dis = Select.kr(dis < 0, [dis, 0]);
					dis = Select.kr(dis > 1, [dis, 1]);
					playerRef = Ref(0);
					playInFunc[i].value(playerRef, busini, bufnum, scaledRate, tpos, spos, lp, rate, item);

					rd = Lag.kr(dis * 340);
					playerRef.value = DelayC.ar(playerRef.value, 0.2, rd/1640.0 * dopamnt);

					wsinal = playerRef.value[0] * contr * Lag.kr(level) * dis * 2.0;

					Out.ar(outbus, wsinal);

					// global reverb
					globallev = 1 / dis.sqrt;
					intens = globallev - 1;
					intens = intens.clip(0, 4);
					intens = intens * 0.25;

					playerRef.value = FoaEncode.ar(playerRef.value, FoaEncoderMatrix.newAmbix1);
					playerRef.value = FoaDirectO.ar(playerRef.value, directang); // directivity

					playerRef.value = FoaTransform.ar(playerRef.value, 'rotate', rotAngle,
						Lag.kr(level) * intens * (1 - contr));

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

					Out.ar(this.fumabus, playerRef.value);

					globallev = globallev - 1.0; // lower tail of curve to zero
					globallev = globallev.clip(0, 1);
					globallev = globallev * glev * 6;
					gsig = playerRef.value[0] * globallev;

					locallev = dis * llev * 5;
					lsig = playerRef.value[0] * locallev;

					gsig = (playerRef.value * globallev) + (playerRef.value * locallev); // b-format
					Out.ar(gbfbus, gsig);
				}).add;


				SynthDef.new("playBFormatAmbitools"++type++"_"++item, {
					| outbus, bufnum = 0, rate = 1,
					level = 0, tpos = 0, lp = 0, rotAngle = 0, tilAngle = 0, tumAngle = 0,
					azim = 0, elev = 0, radius = 0,
					glev, llev, directang = 0, contr, dopamnt,
					busini, insertFlag = 0 |

					var scaledRate, playerRef, wsinal, spos, pushang = 0,
					aFormatFoa, aFormatSoa, ambSigFoaProcessed, ambSigSoaProcessed,

					az, ele, dis, globallev, locallev, gsig, //lsig, intens,
					rd;
					dis = radius;

					az = azim - halfPi;
					az = CircleRamp.kr(az, 0.1, -pi, pi);
					ele = Lag.kr(elev);
					// ele = elev;
					dis = Select.kr(dis < 0, [dis, 0]);
					dis = Select.kr(dis > 1, [dis, 1]);
					playerRef = Ref(0);
					playInFunc[i].value(playerRef, busini, bufnum, scaledRate, tpos, spos, lp, rate, item);

					rd = Lag.kr(dis * 340);
					playerRef.value = DelayC.ar(playerRef.value, 0.2, rd/1640.0 * dopamnt);

					wsinal = playerRef.value[0] * contr * level * dis * 2.0;

					Out.ar(outbus, wsinal);

					// global reverb
					globallev = 1 / dis.sqrt;
					/*intens = globallev - 1;
					intens = intens.clip(0, 4);
					intens = intens * 0.25;*/

					playerRef.value = HOATransRotateAz.ar(count + 2, playerRef.value, rotAngle);
					playerRef.value = HOABeamDirac2Hoa.ar(count + 2, playerRef.value, 1, az, ele,
						focus:contr * dis.sqrt) * (1 - dis.squared) * level;

					Out.ar(this.ambixbus, playerRef.value);

					globallev = globallev - 1.0; // lower tail of curve to zero
					globallev = globallev.clip(0, 1);
					globallev = globallev * glev * 6;
					gsig = playerRef.value[0] * globallev;

					Out.ar(gbixfbus, gsig);
				}).add;
			};

		}; //end makeSynthDefPlayers

		// Make File-in SynthDefs

		playInFunc[0] = {
			| playerRef, busini, bufnum, scaledRate, tpos, spos, lp = 0, rate, channum |
			// Note it needs all the variables
			spos = tpos * BufSampleRate.kr(bufnum);
			scaledRate = rate * BufRateScale.kr(bufnum);
			playerRef.value = PlayBuf.ar(channum, bufnum, scaledRate, startPos: spos, loop: lp, doneAction:2);
		};

		makeSynthDefPlayers.("File", 0);

		// Make HWBus-in SynthDefs

		playInFunc[1] = {
			| playerRef, busini, bufnum, scaledRate, tpos, spos, lp = 0, rate, channum |
			playerRef.value =  In.ar(busini + server.inputBus.index, channum);
		};

		makeSynthDefPlayers.("HWBus", 1);

		// Make SCBus-in SynthDefs

		playInFunc[2] = {
			| playerRef, busini, bufnum, scaledRate, tpos, spos, lp = 0, rate, channum |
			playerRef.value =  In.ar(busini, channum);
		};

		makeSynthDefPlayers.("SWBus", 2);

		playInFunc[3] = {
			| playerRef, busini, bufnum, scaledRate, tpos, spos, lp = 0, rate, channum |
			// Note it needs all the variables
			var trig;
			playerRef.value = DiskIn.ar(channum, bufnum, lp);
			trig = Done.kr(playerRef.value);
			FreeSelf.kr(trig);
		};

		makeSynthDefPlayers.("Stream", 3);


		//////// END SYNTHDEFS ///////////////

		///// launch GUI was here

		updateSynthInArgs = { | source |
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
				this.setSynths(source, \grainrate, grainrate[source]);
				this.setSynths(source, \winsize, winsize[source]);
				this.setSynths(source, \winrand, winrand[source]);

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

			this.nfontes.do { | i |
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
						\df, df[i],
						\grainrate, grainrate[i];
						\winsize, winsize[i];
						\winrand, winrand[i];
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
						\df, df[i],
						\grainrate, grainrate[i];
						\winsize, winsize[i];
						\winrand, winrand[i];
					);
				};
			};
		};

		//source only version (perhaps phase put other

		updatesourcevariables = {
			| source |
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
					\df, df[source],
					\grainrate, grainrate[source];
					\winsize, winsize[source];
					\winrand, winrand[source];
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
					\grainrate, grainrate[source];
					\winsize, winsize[source];
					\winrand, winrand[source];
				);
			};
		};



		// this regulates file playing synths
		watcher = Routine.new({
			"WATCHER!!!".postln;
			inf.do({
				0.1.wait;

				this.nfontes.do({
					| i |
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
								firstTime[i] = true;
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
									&& (firstTime[i] || (this.tfieldProxy[i].value == ""))) {
									//this.triggerFunc[i].value; // play SC input synth
									firstTime[i] = false;
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

				if(guiflag.not) {
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

				if (isPlay && this.ossiatransport.notNil) {
					this.ossiaseekback = false;
					this.ossiatransport.v_(this.control.now);
					this.ossiaseekback = true;
				};

			});
		});




		watcher.play;

		///////////////

		//// LAUNCH GUI
		if (this.serport.notNil) {
			//troutine = this.trackerRoutine; // start parsing of serial head tracker data
			//	kroutine = this.serialKeepItUp;
			troutine.play;
			kroutine.play;
		};


		//server.sync;


		if(guiflag) {
			this.gui;
		};

		//// LAUNCH INITIAL SYNTH

		if (decoder.notNil) {

			this.globDec = Synth.new(\globDecodeSynth,
				target:this.glbRevDecGrp,addAction: \addToTail);
		};


	} // end initMosca

	auditionFunc { |source, bool|
		if(isPlay.not) {
			if(bool) {
				firstTime[source] = true;
				//runTrigger.value(currentsource); - watcher does this now
				//tocar.value(currentsource, 0); // needed only by SC input
				//- and probably by HW - causes duplicates with file
				// as file playback is handled by the "watcher" routine
				testado[source] = true;
			} {
				runStop.value(source);
				this.synt[source].free;
				this.synt[source] = nil;
				testado[source] = false;
				"stopping!".postln;
			};
		};
	}

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
		this.pitchnumboxProxy.valueAction = p;
		this.rollnumboxProxy.valueAction = r;
		this.headingnumboxProxy.valueAction = h;
		this.nfontes.do { | i |

			/*if (guiflag) {
			sprite[i, 0] = halfwidth + (this.cartval[i].x * halfheight);
			sprite[i, 1] = halfheight - (this.cartval[i].y * halfheight);
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
			| item, i |

			if(item.notNil) {
				item.set(param, value);
			}
		});

	}

	getInsertIn {
		|source |
		if (source > 0) {
			var bus = this.insertBus[0,source-1];
			this.insertFlag[source-1]=1;
			this.espacializador[source-1].set(\insertFlag, 1);
			this.synt[source-1].set(\insertFlag, 1);
			^bus
		}
	}

	getInsertOut {
		|source |
		if (source > 0) {
			var bus = this.insertBus[1,source-1];
			this.insertFlag[source-1]=1;
			this.espacializador[source-1].set(\insertFlag, 1);
			this.synt[source-1].set(\insertFlag, 1);
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

	nonAmbi2FuMaNeeded { |i|
		if (i == this.nfontes) {
			^false.asBoolean;
		} {
			if ( (this.lib[i].value > (lastFUMA + 1)) // pass ambisonic libs
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
		| i, tpos, force = false |
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
			"Content Streamed from disk".postln;
			if (testado[i].not || force) { // if source is testing don't relaunch synths

				case
				{ this.streambuf[i].numChannels == 1} {
					"1 channel".postln;

					this.synt[i] = Synth.new(\playMonoStream, [\outbus, mbus[i],
						\bufnum, streambuf[i].bufnum, \rate, 1, \tpos, tpos, \lp, lp[i],
						\level, level[i]],
					this.playEspacGrp).onFree({this.espacializador[i].free;
						this.espacializador[i] = nil; this.synt[i] = nil;
						this.streambuf[i].free;
					});

					// set lib, convert and dstrv variables when stynths are lauched
					// for the tracking functions to stay relevant

					lib[i] = libboxProxy[i].value;

					case
					{ libboxProxy[i].value <= lastSN3D }
					{ convert[i] = convert_ambix; }
					{ (libboxProxy[i].value >= (lastSN3D + 1)) && (libboxProxy[i].value <= lastFUMA) }
					{ convert[i] = convert_fuma; }
					{ libboxProxy[i].value >= (lastFUMA + 1) }
					{ convert[i] = convert_direct; };

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

						if ((this.libboxProxy[i].value > lastFUMA) && this.nonAmbi2FuMa.isNil) {
							this.nonAmbi2FuMa = Synth.new(\nonAmbi2FuMa,
								target:this.glbRevDecGrp).onFree({
								this.nonAmbi2FuMa = nil;
							});
						};
					};

					this.espacializador[i] = Synth.new(libName[i]++"Chowning"++dstrvtypes[i],
						[\inbus, mbus[i], \insertFlag, this.insertFlag[i],
							\insertIn, this.insertBus[0,i],
							\insertOut, this.insertBus[1,i],
							\contr, clev[i], \room, rm[i], \damp, dm[i],
							\grainrate, grainrate[i], \winsize, winsize[i], \winrand, winrand[i]] ++
						wSpecPar.value(max(this.dstrvboxProxy[i].value - 4, 0)),
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

					//atualizarvariaveis.value;
					updatesourcevariables.value(i);

				}
				{ this.streambuf[i].numChannels == 2} {
					"2 channel".postln;
					this.ncanais[i] = 2;

					this.synt[i] = Synth.new(\playStereoStream, [\outbus, sbus[i],
						\bufnum, streambuf[i].bufnum, \rate, 1, \tpos, tpos, \lp, lp[i],
						\level, level[i]],
					this.playEspacGrp).onFree({this.espacializador[i].free;
						this.espacializador[i] = nil; this.synt[i] = nil;
						this.streambuf[i].free;
					});

					// set lib, convert and dstrv variables when stynths are lauched
					// for the tracking functions to stay relevant

					lib[i] = libboxProxy[i].value;

					case
					{ libboxProxy[i].value <= lastSN3D }
					{ convert[i] = convert_ambix; }
					{ (libboxProxy[i].value >= (lastSN3D + 1)) && (libboxProxy[i].value <= lastFUMA) }
					{ convert[i] = convert_fuma; }
					{ libboxProxy[i].value >= (lastFUMA + 1) }
					{ convert[i] = convert_direct; };

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

						if ((this.libboxProxy[i].value > lastFUMA) && this.nonAmbi2FuMa.isNil) {
							this.nonAmbi2FuMa = Synth.new(\nonAmbi2FuMa,
								target:this.glbRevDecGrp).onFree({
								this.nonAmbi2FuMa = nil;
							});
						};
					};

					this.espacializador[i] = Synth.new(libName[i]++"StereoChowning"++dstrvtypes[i],
						[\inbus, sbus[i], \angle, angle[i],
							\insertFlag, this.insertFlag[i],
							\insertIn, this.insertBus[0,i],
							\insertOut, this.insertBus[1,i],
							\room, rm[i], \damp, dm[i], \contr, clev[i],
							\grainrate, grainrate[i], \winsize, winsize[i], \winrand, winrand[i]] ++
						zSpecPar.value(max(this.dstrvboxProxy[i].value - 4, 0)),
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

					updatesourcevariables.value(i);

				}
				{ this.streambuf[i].numChannels >= 4} {
					playingBF[i] = true;
					ncanais[i] = this.streambuf[i].numChannels;
					("contains "++ncanais[i]++" channels").postln;

					if ((libboxProxy[i].value >= (lastSN3D + 1)) ||
						(dstrvboxProxy[i].value == 3)) {
						libboxProxy[i].valueAction = lastSN3D + 1;
						lib[i] = lastSN3D + 1;
						convert[i] = convert_fuma;
					} {
						libboxProxy[i].valueAction = 0;
						lib[i] = 0;
						convert[i] = true;
						dstrv[i] = dstrvboxProxy[i].value;
					};

					// set lib, convert and dstrv variables when stynths are lauched
					// for the tracking functions to stay relevant

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

					this.synt[i] = Synth.new("playBFormat"++libName[i]++"Stream_"++ncanais[i],
						[\outbus, mbus[i], \bufnum, streambuf[i].bufnum, \contr, clev[i],
							\rate, 1, \tpos, tpos, \lp, lp[i], \level, level[i],
							\insertFlag, this.insertFlag[i],
							\insertIn, this.insertBus[0,i],
							\insertOut, this.insertBus[1,i]],
						this.playEspacGrp).onFree({this.espacializador[i].free;
						this.espacializador[i] = nil; this.synt[i] = nil;
						playingBF[i] = false;
						this.streambuf[i].free;
					});

					this.espacializador[i] = Synth.new(\ATK2Chowning++dstrvtypes[i],
						[\insertFlag, this.insertFlag[i], \contr, clev[i],
							\insertIn, this.insertBus[0,i],
							\insertOut, this.insertBus[1,i],
							\room, rm[i], \damp, dm[i],
							\grainrate, grainrate[i], \winsize, winsize[i], \winrand, winrand[i]] ++
						wSpecPar.value(max(this.dstrvboxProxy[i].value - 4, 0)),
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
							if (convert[i]) {
								if (this.converterNeeded(0).not) {
									this.convertor.set(\gate, 0);
								};
							};
						};
					});
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
			case
			{ this.sombuf[i].numChannels == 1} { // arquivo mono
				//"Am I mono?".postln;
				ncanais[i] = 1;

				this.synt[i] = Synth.new(\playMonoFile, [\outbus, mbus[i],
					\bufnum, sombuf[i].bufnum, \rate, 1, \tpos, tpos, \lp, lp[i],
					\level, level[i]], this.playEspacGrp).onFree({this.espacializador[i].free;
					this.espacializador[i] = nil;
					this.synt[i] = nil});

				// set lib, convert and dstrv variables when stynths are lauched
				// for the tracking functions to stay relevant

				lib[i] = libboxProxy[i].value;

				case
				{ libboxProxy[i].value <= lastSN3D }
				{ convert[i] = convert_ambix; }
				{ (libboxProxy[i].value >= (lastSN3D + 1)) && (libboxProxy[i].value <= lastFUMA) }
				{ convert[i] = convert_fuma; }
				{ libboxProxy[i].value >= (lastFUMA + 1) }
				{ convert[i] = convert_direct; };

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

					if ((this.libboxProxy[i].value > lastFUMA) && this.nonAmbi2FuMa.isNil) {
						this.nonAmbi2FuMa = Synth.new(\nonAmbi2FuMa,
							target:this.glbRevDecGrp).onFree({
							this.nonAmbi2FuMa = nil;
						});
					};
				};

				this.espacializador[i] = Synth.new(libName[i]++"Chowning"++dstrvtypes[i],
					[\inbus, mbus[i], \insertFlag, this.insertFlag[i],
						\insertIn, this.insertBus[0,i],
						\insertOut, this.insertBus[1,i],
						\room, rm[i], \damp, dm[i], \contr, clev[i],
						\grainrate, grainrate[i], \winsize, winsize[i], \winrand, winrand[i]]  ++
					wSpecPar.value(max(this.dstrvboxProxy[i].value - 4, 0)),
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

				updatesourcevariables.value(i);

			}
			{ this.sombuf[i].numChannels == 2 } {

				ncanais[i] = 2; // arquivo estéreo

				this.synt[i] = Synth.new(\playStereoFile, [\outbus, sbus[i],
					\bufnum, sombuf[i].bufnum, \rate, 1, \tpos, tpos, \lp, lp[i],
					\level, level[i]], this.playEspacGrp).onFree({this.espacializador[i].free;
					this.espacializador[i] = nil;
					this.synt[i] = nil});

				// set lib, convert and dstrv variables when stynths are lauched
				// for the tracking functions to stay relevant

				lib[i] = libboxProxy[i].value;

				case
				{ libboxProxy[i].value <= lastSN3D }
				{ convert[i] = convert_ambix; }
				{ (libboxProxy[i].value >= (lastSN3D + 1)) && (libboxProxy[i].value <= lastFUMA) }
				{ convert[i] = convert_fuma; }
				{ libboxProxy[i].value >= (lastFUMA + 1) }
				{ convert[i] = convert_direct; };

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

					if ((this.libboxProxy[i].value > lastFUMA) && this.nonAmbi2FuMa.isNil) {
						this.nonAmbi2FuMa = Synth.new(\nonAmbi2FuMa,
							target:this.glbRevDecGrp).onFree({
							this.nonAmbi2FuMa = nil;
						});
					};
				};

				this.espacializador[i] = Synth.new(libName[i]++"StereoChowning"++dstrvtypes[i],
					[\inbus, sbus[i], \angle, angle[i],
						\insertFlag, this.insertFlag[i],
						\insertIn, this.insertBus[0,i],
						\insertOut, this.insertBus[1,i],
						\room, rm[i], \damp, dm[i], \contr, clev[i],
						\grainrate, grainrate[i], \winsize, winsize[i], \winrand, winrand[i]] ++
					zSpecPar.value(max(this.dstrvboxProxy[i].value - 4, 0)),
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

				//atualizarvariaveis.value;
				updatesourcevariables.value(i);

			}
			{ this.sombuf[i].numChannels >= 4 } {
				playingBF[i] = true;
				ncanais[i] = sombuf[i].numChannels;

				if ((libboxProxy[i].value >= (lastSN3D + 1)) ||
					(dstrvboxProxy[i].value == 3)) {
					libboxProxy[i].valueAction = lastSN3D + 1;
					lib[i] = lastSN3D + 1;
					convert[i] = convert_fuma;
				} {
					libboxProxy[i].valueAction = 0;
					lib[i] = 0;
					convert[i] = true;
					dstrv[i] = dstrvboxProxy[i].value;
				};

				// set lib, convert and dstrv variables when stynths are lauched
				// for the tracking functions to stay relevant

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

				this.synt[i] = Synth.new("playBFormat"++libName[i]++"File_"++ncanais[i],
					[\outbus, mbus[i], \bufnum, sombuf[i].bufnum, \contr, clev[i],
						\rate, 1, \tpos, tpos, \lp,
						lp[i], \level, level[i],
						\insertFlag, this.insertFlag[i],
						\insertIn, this.insertBus[0,i],
						\insertOut, this.insertBus[1,i]],
					this.playEspacGrp).onFree({this.espacializador[i].free;
					this.espacializador[i] = nil;
					this.synt[i] = nil;
					playingBF[i] = false;
				});

				this.espacializador[i] = Synth.new(\ATK2Chowning++dstrvtypes[i],
					[\inbus, mbus[i], \insertFlag, this.insertFlag[i],
						\insertIn, this.insertBus[0,i],
						\insertOut, this.insertBus[1,i],
						\contr, clev[i], \room, rm[i], \damp, dm[i],
						\grainrate, grainrate[i], \winsize, winsize[i], \winrand, winrand[i]] ++
					wSpecPar.value(max(this.dstrvboxProxy[i].value - 4, 0)),
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
						if (convert[i]) {
							if (this.converterNeeded(0).not) {
								this.convertor.set(\gate, 0);
							};
						};
					};
				});

			};

			updatesourcevariables.value(i);

		} {
			ncanais[i] = 0 ; // outro tipo de arquivo, faz nada.

			if ((this.scncheckProxy[i].value) || (this.hwncheckProxy[i])) {

				case
				{ this.ncan[i] == 1 } {

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

					// set lib, convert and dstrv variables when stynths are lauched
					// for the tracking functions to stay relevant

					lib[i] = libboxProxy[i].value;

					case
					{ libboxProxy[i].value <= lastSN3D }
					{ convert[i] = convert_ambix; }
					{ (libboxProxy[i].value >= (lastSN3D + 1)) && (libboxProxy[i].value <= lastFUMA) }
					{ convert[i] = convert_fuma; }
					{ libboxProxy[i].value >= (lastFUMA + 1) }
					{ convert[i] = convert_direct; };

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

						if ((this.libboxProxy[i].value > lastFUMA) && this.nonAmbi2FuMa.isNil) {
							this.nonAmbi2FuMa = Synth.new(\nonAmbi2FuMa,
								target:this.glbRevDecGrp).onFree({
								this.nonAmbi2FuMa = nil;
							});
						};
					};

					this.espacializador[i] = Synth.new(libName[i]++"Chowning"++dstrvtypes[i],
						[\inbus, mbus[i], \insertFlag, this.insertFlag[i],
							\insertIn, this.insertBus[0,i],
							\insertOut, this.insertBus[1,i],
							\room, rm[i], \damp, dm[i], \contr, clev[i],
							\grainrate, grainrate[i], \winsize, winsize[i], \winrand, winrand[i]] ++
						wSpecPar.value(max(this.dstrvboxProxy[i].value - 4, 0)),
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

					//atualizarvariaveis.value;
					updatesourcevariables.value(i);

				}
				{ this.ncan[i] == 2 } {
					ncanais[i] = 0; // just in case!

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

					// set lib, convert and dstrv variables when stynths are lauched
					// for the tracking functions to stay relevant

					lib[i] = libboxProxy[i].value;

					case
					{ libboxProxy[i].value <= lastSN3D }
					{ convert[i] = convert_ambix; }
					{ (libboxProxy[i].value >= (lastSN3D + 1)) && (libboxProxy[i].value <= lastFUMA) }
					{ convert[i] = convert_fuma; }
					{ libboxProxy[i].value >= (lastFUMA + 1) }
					{ convert[i] = convert_direct; };

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

						if ((this.libboxProxy[i].value > lastFUMA) && this.nonAmbi2FuMa.isNil) {
							this.nonAmbi2FuMa = Synth.new(\nonAmbi2FuMa,
								target:this.glbRevDecGrp).onFree({
								this.nonAmbi2FuMa = nil;
							});
						};
					};

					this.espacializador[i] = Synth.new(libName[i]++"StereoChowning"++dstrvtypes[i],
						[\inbus, sbus[i], \angle, angle[i],
							\insertFlag, this.insertFlag[i],
							\insertIn, this.insertBus[0,i],
							\insertOut, this.insertBus[1,i],
							\contr, clev[i], \room, rm[i], \damp, dm[i],
							\grainrate, grainrate[i], \winsize, winsize[i], \winrand, winrand[i]] ++
						zSpecPar.value(max(this.dstrvboxProxy[i].value - 4, 0)),
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

					//atualizadstrvariaveis.value;
					updatesourcevariables.value(i);

				}
				{ this.ncan[i] >= 4 } {

					if ((libboxProxy[i].value >= (lastSN3D + 1)) ||
						(dstrvboxProxy[i].value == 3)) {
						libboxProxy[i].valueAction = lastSN3D + 1;
						lib[i] = lastSN3D + 1;
						convert[i] = convert_fuma;
					} {
						libboxProxy[i].valueAction = 0;
						lib[i] = 0;
						convert[i] = true;
						dstrv[i] = dstrvboxProxy[i].value;
					};

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

					if (this.hwncheckProxy[i].value) {

						this.synt[i] = Synth.new(\playBFormat++libName[i]++"HWBus_"++this.ncan[i],
							[\gbfbus, gbfbus, \gbixfbus, gbixfbus, \outbus, mbus[i],
								\contr, clev[i], \rate, 1, \tpos, tpos, \level, level[i],
								\insertFlag, this.insertFlag[i],
								\insertIn, this.insertBus[0,i],
								\insertOut, this.insertBus[1,i],
								\busini, this.busini[i]],this.playEspacGrp).onFree({
							this.espacializador[i].free;
							this.espacializador[i] = nil;
							this.synt[i] = nil;
						});

					} {

						this.synt[i] = Synth.new(\playBFormat++libName[i]++"SWBus_"++this.ncan[i],
							[\gbfbus, gbfbus, \gbixfbus, gbixfbus, \outbus, mbus[i],
								\contr, clev[i], \rate, 1, \tpos, tpos,
								\level, level[i],
								\insertFlag, this.insertFlag[i],
								\insertIn, this.insertBus[0,i],
								\insertOut, this.insertBus[1,i],
								\busini, this.scInBus[i] ],this.playEspacGrp).onFree({
							this.espacializador[i].free;
							this.espacializador[i] = nil;
							this.synt[i] = nil;
						});
					};

					this.espacializador[i] = Synth.new(\ATK2Chowning++dstrvtypes[i],
						[\inbus, mbus[i], \insertFlag, this.insertFlag[i],
							\insertIn, this.insertBus[0,i],
							\insertOut, this.insertBus[1,i],
							\contr, clev[i], \room, rm[i], \damp, dm[i],
							\grainrate, grainrate[i], \winsize, winsize[i], \winrand, winrand[i]] ++
						wSpecPar.value(max(this.dstrvboxProxy[i].value - 4, 0)),
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
							if (convert[i]) {
								if (this.converterNeeded(0).not) {
									this.convertor.set(\gate, 0);
								};
							};
						};
					});

					//atualizarvariaveis.value;
					updatesourcevariables.value(i);

				};
			};
		};
	}

	loadNonAutomationData { | path |
		var libf, loopedf, aformatrevf, hwinf, scinf,
		spreadf, diffusef, ncanf, businif, stcheckf, filenames;
		//("THE PATH IS " ++ path ++ "/filenames.txt").postln;
		filenames = File((path ++ "/filenames.txt").standardizePath,"r");

		libf = File((path ++ "/lib.txt").standardizePath,"r");
		loopedf = File((path ++ "/looped.txt").standardizePath,"r");
		aformatrevf = File((path ++ "/aformatrev.txt").standardizePath,"r");
		hwinf = File((path ++ "/hwin.txt").standardizePath,"r");
		scinf = File((path ++ "/scin.txt").standardizePath,"r");
		spreadf = File((path ++ "/spread.txt").standardizePath,"r");
		diffusef = File((path ++ "/diffuse.txt").standardizePath,"r");
		ncanf = File((path ++ "/ncan.txt").standardizePath,"r");
		businif = File((path ++ "/busini.txt").standardizePath,"r");
		stcheckf = File((path ++ "/stcheck.txt").standardizePath,"r");


		//{	("BEFORE ACTION - stream = " ++ stcheckProxy[0].value).postln;}.defer;


		nfontes.do { | i |
			var line = filenames.getLine(1024);
			if(line!="NULL") {
				this.tfieldProxy[i].valueAction = line;
			} {
				this.tfieldProxy[i].valueAction = "";
			};
		};

		nfontes.do { | i |
			var line = libf.getLine(1024);
			this.libboxProxy[i].valueAction = line.asInt;
		};

		nfontes.do { | i |
			var line = loopedf.getLine(1024);
			var flag;
			if (line == "true") {flag = true;} {flag = false;};
			this.lpcheckProxy[i].valueAction = flag;
			//lp[i] 0 or 1
		};

		nfontes.do { | i |
			var line = aformatrevf.getLine(1024);
			this.dstrvboxProxy[i].valueAction = line.asInt;
			//dstrv[i] 0 or 1
		};

		nfontes.do { | i |
			var line = spreadf.getLine(1024);
			var flag;
			if (line == "true") {flag = true;} {flag = false;};
			this.spcheckProxy[i].valueAction = flag;
		};

		nfontes.do { | i |
			var line = diffusef.getLine(1024);
			var flag;
			if (line == "true") {flag = true;} {flag = false;};
			this.dfcheckProxy[i].valueAction = flag;
		};
		nfontes.do { | i |
			var line = ncanf.getLine(1024);
			this.ncanboxProxy[i].valueAction = line.asInt;
		};

		nfontes.do { | i |
			var line = businif.getLine(1024);
			this.businiboxProxy[i].valueAction = line.asInt;
		};

		nfontes.do { | i |
			var line = stcheckf.getLine(1024);
			var flag;
			if (line == "true") {flag = true;} {flag = false;};
			this.stcheckProxy[i].valueAction = flag;
		};

		//		nfontes.do { arg i;
		//	var line = hwinf.getLine(1024);
		//	this.hwncheckProxy[i].valueAction = line;
		// };
		nfontes.do { | i |
			var line = hwinf.getLine(1024);
			var flag;
			if (line == "true") {flag = true;} {flag = false;};

			//("line = " ++ line.asString).postln;

			//this.hwncheckProxy[i].valueAction = line.booleanValue;
			// why, why, why is this asBoolean necessary!
			this.hwncheckProxy[i].valueAction = flag;
		};


		nfontes.do { | i |
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
		spreadf.close;
		diffusef.close;
		ncanf.close;
		businif.close;
		stcheckf.close;

		//"RARARARARAR".postln;

		// delay necessary here because streamdisks take some time to register after control.load
		//Routine {

		//1.wait;
		/*this.nfontes.do { arg i;

		var newpath = this.tfieldProxy[i].value;
		//	server.sync;
		if (this.streamdisk[i].not && (this.tfieldProxy[i].value != "")) {
		i.postln;
		newpath.postln;

		this.sombuf[i] = Buffer.read(server, newpath, action: {arg buf;
		"Loaded file".postln;
		});
		};

		};*/
		//	}.play;
		//	watcher.play;

	}

	// Automation call-back doesn' seem to work with no GUI, so these duplicate
	// control.onPlay, etc.
	blindControlPlay {
		var startTime;
		this.nfontes.do { | i |
			firstTime[i]=true;
		};

		if(control.now < 0)
		{
			startTime = 0
		}
		{
			startTime = control.now
		};
		isPlay = true;
		this.control.play;

		//runTriggers.value;
	}

	blindControlStop {
		this.control.stop;
		runStops.value;
		this.nfontes.do { | i |
			// if sound is currently being "tested", don't switch off on stop
			// leave that for user
			if (testado[i] == false) {
				this.synt[i].free; // error check
			};
			//	this.espacializador[i].free;
		};
		isPlay = false;
	}



	free {

		this.control.quit;
		if (this.serport.notNil) {
			this.trackPort.close;
			//				this.trackerRoutine.stop;
			//				this.serialKeepItUp.stop;
		};

		troutine.stop;
		kroutine.stop;
		watcher.stop;

		this.fumabus.free;
		this.ambixbus.free;
		this.nfontes.do { | x |
			this.espacializador[x].free;
			this.aFormatBusFoa[0,x].free;
			this.aFormatBusFoa[1,x].free;
			this.aFormatBusSoa[0,x].free;
			this.aFormatBusSoa[1,x].free;
			this.mbus[x].free;
			this.sbus[x].free;
			//      bfbus.[x].free;
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

		if (maxorder > 1) {
			this.soaRevBus.free;
		};

		rirList.do { |item, count|
			rirWspectrum[count].free;
			rirXspectrum[count].free;
			rirYspectrum[count].free;
			rirZspectrum[count].free;
			rirFRDspectrum[count].free;
			rirBLDspectrum[count].free;
			rirFLUspectrum[count].free;
			rirBRUspectrum[count].free;
			if (maxorder > 1) {
				12.do { | i |
					rirA12Spectrum[count, i].free;
				};
			};
		};

		foaEncoderOmni.free;
		foaEncoderSpread.free;
		foaEncoderDiffuse.free;
		b2a.free;
		a2b.free;

		this.playEspacGrp.free;
		this.glbRevDecGrp.free;

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
		mouseButton, //dragStartScreen,
		//novoplot,
		period,
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

		win,
		wdados,
		waux,
		dialView,
		autoView,
		orientView,

		//rlev,
		//dlev,
		//dplev, dpslider,
		cnumbox,
		textbuf,
		m,
		//plotlock = true,
		//aux1numbox, aux2numbox, aux3numbox, aux4numbox, aux5numbox,
		zSliderHeight = height * 2 / 3;
		//dragStartScreen = Point.new;
		//dragStartMap = Point.new;

		//////// Huge lot declarations removed //////////


		////////////////////////////////////////////////


		//this.offsetHeading(this.offsetheading);
		//("headingoffset variable = " ++ this.offsetheading).postln;






		// Note there is an extreme amount repetition occurring here. See the calling function. fix

		win = Window.new("Mosca", Rect(0, width, width, height)).front;
		win.background = Color.new255( 200, 200, 200 ); // OSSIA/score "HalfLight"

		win.drawFunc = {

			Pen.fillColor = Color.new255(0, 127, 229, 76); // OSSIA/score "Transparent1"
			Pen.addArc(halfwidth@halfheight, halfheight, 0, 2pi);
			Pen.fill;

			this.nfontes.do { |i|
				var x, y;
				var topView = this.spheval[i];
				var lev = this.spheval[i].z;
				var color = lev * 0.4;
				{x = halfwidth + (topView.x * halfheight)}.defer;
				{y = halfheight - (topView.y * halfheight)}.defer;
				Pen.addArc(x@y, 14, 0, 2pi);
				if ((this.testado[i] || isPlay) && (lev.abs <= this.plim)) {
					if (lev <= 0) {
						Pen.fillColor = Color.new255(179 + (lev * 176), 90 - (lev * 60), 209 - (lev * 41));
						// from OSSIA/score "Emphasis3" down to "Pulse2"
					} {
						Pen.fillColor = Color.new255(179 + (lev * 20), 90 - (lev * 59), 209 - (lev * 165));
						// up to "Tender1"
					};
					Pen.fill;
				} {
					Pen.strokeColor = Color.white;
					Pen.stroke;
				};
				(i + 1).asString.drawCenteredIn(Rect(x - 11, y - 10, 23, 20),
					Font.default, Color.white);
			};

			Pen.fillColor = Color.new255(37, 41, 48, 40);
			Pen.addArc(halfwidth@halfheight, 14, 0, 2pi);
			Pen.fill;

		};


		novoplot = { |dirrect = false|

			//plotlock = false;

			period = Main.elapsedTime - lastGui;
			if ((period > guiInt) /*&& plotlock*/) {
				lastGui =  Main.elapsedTime;
				{
					{ this.zlev[currentsource] = this.spheval[currentsource].z; }.defer;
					{ zslider.value = (this.zlev[currentsource] + 1) * 0.5; }.defer;
					{ znumbox.value = this.zlev[currentsource]; }.defer;
					{ win.refresh; }.defer;
				}.defer(guiInt);
			};

			//plotlock = true;

		};

		/*novoplot = {
		arg mx, my, i, nfnts;
		var btest;
		{
		win.drawFunc = {
		Pen.fillColor = Color(0.6,0.8,0.8);
		Pen.addArc(halfwidth@halfwidth, halfwidth, 0, 2pi);
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
		Pen.addArc(halfwidth@halfwidth, 20, 0, 2pi);
		Pen.fill;
		}
		}.defer;
		{ win.refresh; }.defer;
		};*/

		wdados = Window.new("Data", Rect(width, 0, 960, (this.nfontes*20)+60 ), scroll: true);
		wdados.userCanClose = false;
		wdados.alwaysOnTop = true;

		dialView = UserView(win, Rect(width - 190, 10, 180, 80));

		bdados = Button(dialView, Rect(90, 20, 90, 20))
		.states_([
			["show data", Color.black, Color.white],
			["hide data", Color.white, Color.blue]
		])
		.action_({ | but |
			if(but.value == 1)
			{wdados.front;}
			{wdados.visible = false;};
		});

		waux = Window.new("Auxiliary Controllers", Rect(width, (this.nfontes*20)+114, 260, 250 ));
		waux.userCanClose = false;
		waux.alwaysOnTop = true;


		baux = Button(dialView, Rect(90, 40, 90, 20))
		.states_([
			["show aux", Color.black, Color.white],
			["hide aux", Color.white, Color.blue]
		])
		.action_({ | but |
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
		.action_({ | butt, mod |
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
		.action_({ | butt, mod |
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
		.action_({ | butt, mod |
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
		.action_({ | butt, mod |
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
		.action_({ | butt, mod |
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


		aux1numbox.action = { | num |
			a1box[currentsource].valueAction = num.value;
			//this.aux1[currentsource] = num.value;
			auxslider1.value = num.value;
		};

		auxslider1.action = { | num |
			a1box[currentsource].valueAction = num.value;
			aux1numbox.value = num.value;
		};
		auxslider2.action = { | num |
			a2box[currentsource].valueAction = num.value;
			aux2numbox.value = num.value;
		};
		auxslider3.action = { | num |
			a3box[currentsource].valueAction = num.value;
			aux3numbox.value = num.value;
		};
		auxslider4.action = { | num |
			a4box[currentsource].valueAction = num.value;
			aux4numbox.value = num.value;
		};
		auxslider5.action = { | num |
			a5box[currentsource].valueAction = num.value;
			aux5numbox.value = num.value;
		};


		btestar = Button(dialView, Rect(0, 60, 90, 20))
		.states_([
			["audition", Color.black, Color.white],
			["stop", Color.white, Color.red]
		])
		.action_({ | but |
			var bool = but.value.asBoolean;
			if (this.ossiaaud.isNil) {
				this.auditionFunc(currentsource, bool);
			} {
				this.ossiaaud[currentsource].v_(bool);
			};

			{win.refresh;}.defer;

		});


		brecaudio = Button(dialView, Rect(0, 0, 90, 20))
		.states_([
			["record audio", Color.red, Color.white],
			["stop", Color.white, Color.red]
		])
		.action_({ | but |
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

		blipcheck = CheckBox(dialView, Rect(95, 3, 60, 15), "blips").action_({ | butt |
			if(butt.value) {
				//"Looping transport".postln;
				//this.autoloopval = true;
			} {
				//		this.autoloopval = false;
			};

		});


		autoView = UserView(win, Rect(10, width - 45, 325, 40));

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
			spreadf, diffusef, ncanf,
			businif, stcheckf;

			//////////////


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
				spreadf = File((textField.value ++ "/spread.txt").standardizePath,"w");
				diffusef = File((textField.value ++ "/diffuse.txt").standardizePath,"w");
				ncanf = File((textField.value ++ "/ncan.txt").standardizePath,"w");
				businif = File((textField.value ++ "/busini.txt").standardizePath,"w");
				stcheckf = File((textField.value ++ "/stcheck.txt").standardizePath,"w");


				nfontes.do { | i |
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
				spreadf.close;
				diffusef.close;
				ncanf.close;
				businif.close;
				stcheckf.close;

				///////


				control.save(textField.value);
				lastAutomation = textField.value;

			};
			dwin.front;



		});

		// load automation - adapted from chooseDirectoryDialog in AutomationGui.sc

		bcarregar = Button(autoView, Rect(80, 20, 80, 20))
		.states_([
			["load auto", Color.black, Color.white],
		])
		.action_({
			var title = "Select Automation directory", onSuccess, onFailure = nil,
			preset = nil, bounds, dwin, textField, success = false;

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
				lastAutomation = textField.value;
				this.loadNonAutomationData(textField.value);

			};
			dwin.front;




		});


		/*
		bsnap = Button(win, Rect(170, width - 40, 25, 20))
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
		Pen.addArc(halfwidth@halfheight, halfheight, 0, 2pi);
		Pen.fill;


		Pen.fillColor = Color.gray(0, 0.5);
		Pen.addArc(halfwidth@halfheight, 20, 0, 2pi);
		Pen.fill;

		//	Pen.width = 10;
		};*/

		// seleção de fontes
		itensdemenu = Array.newClear(this.nfontes);
		this.nfontes.do { | i |
			itensdemenu[i] = "Source " ++ (i + 1).asString;
		};

		m = PopUpMenu(win,Rect(10, 10, 150, 20));
		m.items = itensdemenu;
		m.action = { | menu |
			currentsource = menu.value;

			libnumbox.value = this.libboxProxy[currentsource].value;

			dstReverbox.value = this.dstrvboxProxy[currentsource].value;

			if(lp[currentsource] == 1){loopcheck.value = true}{loopcheck.value = false};

			if(sp[currentsource] == 1){spreadcheck.value = true}{spreadcheck.value = false};
			if(df[currentsource] == 1){diffusecheck.value = true}{diffusecheck.value = false};

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
			rslider.value = (rlev[currentsource] + pi) * 0.5pi;
			rnumbox.value = rlev[currentsource];
			dirslider.value = dlev[currentsource] / (pi * 0.5);
			dirnumbox.value = dlev[currentsource];
			cslider.value = clev[currentsource];
			zslider.value = (zlev[currentsource] + 1) * 0.5;
			znumbox.value = zlev[currentsource];
			rateslider.value = grainrate[currentsource] * 60;
			ratenumbox.value = grainrate[currentsource];
			winslider.value = winsize[currentsource] * 5;
			winumbox.value = winsize[currentsource];
			randslider.value = winrand[currentsource];
			randnumbox.value = winrand[currentsource];

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
				//	if (this.synt[currentsource] == nil){
				//		btestar.value = 0;
				//	} {
				btestar.value = 1;
				//	};
			} {
				btestar.value = 0;
			};
		};

		//offset = 60;

		hwInCheck = CheckBox( win, Rect(10, 30, 100, 20), "HW-in").action_({ | butt |
			{this.hwncheck[currentsource].valueAction = butt.value;}.defer;
			if (hwInCheck.value && scInCheck.value) {
			};
		});

		scInCheck = CheckBox( win, Rect(90, 30, 60, 20), "SC-in").action_({ | butt |
			{this.scncheck[currentsource].valueAction = butt.value;}.defer;
			if (scInCheck.value && hwInCheck.value) {
			};
		});

		loopcheck = CheckBox( win, Rect(163, 30, 80, 20), "Loop").action_({ | butt |
			{this.lpcheck[currentsource].valueAction = butt.value;}.defer;
		});
		loopcheck.value = false;


		/////////////////////////////////////////////////////////


		textbuf = StaticText(win, Rect(55, 50, 200, 20));
		textbuf.string = "No. of chans. (HW & SC-in)";
		ncannumbox = NumberBox(win, Rect(10, 50, 40, 20));
		ncannumbox.value = 0;
		ncannumbox.clipHi = 36;
		ncannumbox.clipLo = 0;
		ncannumbox.align = \center;
		ncannumbox.action = { | num |
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
		busininumbox.action = { | num |
			{this.businibox[currentsource].valueAction = num.value;}.defer;
			this.busini[currentsource] = num.value;
		};


		/////////////////////////////////////////////////////////


		textbuf = StaticText(win, Rect(163, 90, 240, 20));
		textbuf.string = "Library";
		libnumbox = PopUpMenu( win, Rect(10, 90, 150, 20));
		libnumbox.items = spatList;
		libnumbox.action_({ | num |
			{this.libbox[currentsource].valueAction = num.value;}.defer;
		});
		libnumbox.value = lastSN3D + 1;


		/////////////////////////////////////////////////////////


		zAxis = StaticText(win, Rect(width - 80, halfwidth - 10, 90, 20));
		zAxis.string = "Z-Axis";
		znumbox = NumberBox(win, Rect(width - 45, ((width - zSliderHeight) * 0.5)
			+ zSliderHeight, 40, 20));
		znumbox.value = 0;
		znumbox.clipHi = 1;
		znumbox.clipLo = -1;
		znumbox.step_(0.01);
		znumbox.scroll_step_(0.01);
		znumbox.align = \center;
		znumbox.action = { | num |
			{zslider.valueAction = (num.value * 2) -1;}.defer;
		};


		zslider = Slider.new(win, Rect(width - 35, ((width - zSliderHeight) * 0.5),
			20, zSliderHeight));
		zslider.value = 0.5;
		zslider.action = { | num |
			var scale = (0.5 - num.value) * -2;

			this.cartval[currentsource].x_(this.spheval[currentsource].x);
			this.cartval[currentsource].y_(this.spheval[currentsource].y);
			this.cartval[currentsource].z_(scale);

			this.cartval[currentsource] = this.cartval[currentsource]
			.tumble(heading).tilt(roll).rotate(pitch);

			xbox[currentsource].valueAction = this.cartval[currentsource].x + origine.x;
			ybox[currentsource].valueAction = this.cartval[currentsource].y + origine.y;
			zbox[currentsource].valueAction = this.cartval[currentsource].z + origine.z;

		};


		////////////////////////////// Orientation //////////////

		orientView = UserView(win, Rect(width - 285, height - 85, 285, 100));

		this.pitchnumbox = NumberBox(orientView, Rect(240, 20, 40, 20));
		pitchnumbox.align = \center;
		pitchnumbox.clipHi = pi;
		pitchnumbox.clipLo = -pi;
		pitchnumbox.step_(0.01);
		pitchnumbox.scroll_step_(0.01);

		this.rollnumbox = NumberBox(orientView, Rect(240, 40, 40, 20));
		rollnumbox.align = \center;
		rollnumbox.clipHi = pi;
		rollnumbox.clipLo = -pi;
		rollnumbox.step_(0.01);
		rollnumbox.scroll_step_(0.01);

		this.headingnumbox = NumberBox(orientView, Rect(240, 60, 40, 20));
		headingnumbox.align = \center;
		headingnumbox.clipHi = pi;
		headingnumbox.clipLo = -pi;
		headingnumbox.step_(0.01);
		headingnumbox.scroll_step_(0.01);


		this.pitchnumbox.action = { | num |
			this.pitchnumboxProxy.valueAction = num.value;
		};

		this.rollnumbox.action = { | num |
			this.rollnumboxProxy.valueAction = num.value;
		};

		this.headingnumbox.action = { | num |
			this.headingnumboxProxy.valueAction = num.value;
		};

		textbuf = StaticText(orientView, Rect(225, 20, 12, 22));
		textbuf.string = "P:";
		textbuf = StaticText(orientView, Rect(225, 40, 12, 22));
		textbuf.string = "R:";
		textbuf = StaticText(orientView, Rect(225, 60, 12, 22));
		textbuf.string = "H:";

		textbuf = StaticText(orientView, Rect(237, 0, 45, 20));
		textbuf.string = "Orient.";

		this.oxnumbox = NumberBox(orientView, Rect(180, 20, 40, 20));
		oxnumbox.align = \center;
		oxnumbox.step_(0.01);
		oxnumbox.scroll_step_(0.01);

		this.oynumbox = NumberBox(orientView, Rect(180, 40, 40, 20));
		oynumbox.align = \center;
		oynumbox.step_(0.01);
		oynumbox.scroll_step_(0.01);

		this.oznumbox = NumberBox(orientView, Rect(180, 60, 40, 20));
		oznumbox.align = \center;
		oznumbox.step_(0.01);
		oznumbox.scroll_step_(0.01);

		this.oxnumbox.action = { | num |
			this.oxnumboxProxy.valueAction = num.value;
		};

		this.oynumbox.action = { | num |
			this.oynumboxProxy.valueAction = num.value;
		};

		this.oznumbox.action = { | num |
			this.oznumboxProxy.valueAction = num.value;
		};

		textbuf = StaticText(orientView, Rect(165, 20, 12, 22));
		textbuf.string = "X:";
		textbuf = StaticText(orientView, Rect(165, 40, 12, 22));
		textbuf.string = "Y:";
		textbuf = StaticText(orientView, Rect(165, 60, 12, 22));
		textbuf.string = "Z:";

		textbuf = StaticText(orientView, Rect(175, 0, 47, 20));
		textbuf.string = "Origine";


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
		volnumbox.action = { | num |
			{vbox[currentsource].valueAction = num.value;}.defer;

		};
		volslider = Slider.new(win, Rect(50, 110, 110, 20));
		volslider.value = 0;
		volslider.action = { | num |
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
		dopnumbox.action = { | num |
			{dpbox[currentsource].valueAction = num.value;}.defer;

		};
		// stepsize?
		dpslider = Slider.new(win, Rect(50, 130, 110, 20));
		dpslider.value = 0;
		dpslider.action = { | num |
			{dpbox[currentsource].valueAction = num.value;}.defer;
			{dopnumbox.value = num.value;}.defer;
		};


		/////////////////////////////////////////////////////////////////////////



		textbuf = StaticText(orientView, Rect(5, 0, 20, 20));
		textbuf.string = "M";

		masterslider = Slider.new(orientView, Rect(0, 20, 20, 60));
		masterslider.orientation(\vertical);
		masterslider.value = 1;
		masterslider.action = { | num |
			this.masterlevProxy.valueAction = num.value;
		};

		/////////////////////////////////////////////////////////////////////////


		textbuf = StaticText(orientView, Rect(35, 0, 150, 20));
		textbuf.string = "Cls./Afmt. Reverb";
		clsReverbox = PopUpMenu(orientView, Rect(20, 20, 130, 20));
		clsReverbox.items = ["no-reverb",
			"freeverb",
			"allpass"] ++ rirList;
		// add the list of impule response if one is provided by rirBank

		clsReverbox.action_({ | num |
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
		gnumbox.action = { | num |
			{gbox[currentsource].valueAction = num.value;}.defer;

		};
		// stepsize?
		gslider = Slider.new(win, Rect(50, 150, 110, 20));
		gslider.value = 0;
		gslider.action = { | num |
			{gbox[currentsource].valueAction = num.value;}.defer;
		};


		/////////////////////////////////////////////////////////////////////////


		textbuf = StaticText(orientView, Rect(63, 40, 100, 20));
		textbuf.string = "room/delay";
		clsrmnumbox = NumberBox(orientView, Rect(20, 40, 40, 20));
		clsrmnumbox.value = 0.5;
		clsrmnumbox.clipHi = 1;
		clsrmnumbox.clipLo = 0;
		clsrmnumbox.step_(0.01);
		clsrmnumbox.scroll_step_(0.01);
		clsrmnumbox.align = \center;
		clsrmnumbox.action = { | num |
			{clsrmboxProxy.valueAction = num.value;}.defer;

		};
		// stepsize?
		/*clsrmslider = Slider.new(win, Rect(50, 190, 110, 20));
		clsrmslider.value = 0.5;
		clsrmslider.action = {arg num;
		{clsrmboxProxy.valueAction = num.value;}.defer;
		};*/


		/////////////////////////////////////////////////////////////////////////


		textbuf = StaticText(orientView, Rect(63, 60, 100, 20));
		textbuf.string = "damp/decay";
		clsdmnumbox = NumberBox(orientView, Rect(20, 60, 40, 20));
		clsdmnumbox.value = 0.5;
		clsdmnumbox.clipHi = 1;
		clsdmnumbox.clipLo = 0;
		clsdmnumbox.step_(0.01);
		clsdmnumbox.scroll_step_(0.01);
		clsdmnumbox.align = \center;
		clsdmnumbox.action = { | num |
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
		dstReverbox.items = ["no-reverb", "freeverb", "allpass"] ++ rirList;
		// add the list of impule response if one is provided

		dstReverbox.action_({ | num |
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
		lnumbox.action = { | num |
			{lbox[currentsource].valueAction = num.value;}.defer;

		};
		// stepsize?
		lslider = Slider.new(win, Rect(50, 190, 110, 20));
		lslider.value = 0;
		lslider.action = { | num |
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
		rmnumbox.action = { | num |
			{rmbox[currentsource].valueAction = num.value;}.defer;

		};
		// stepsize?
		rmslider = Slider.new(win, Rect(50, 210, 110, 20));
		rmslider.value = 0.5;
		rmslider.action = { | num |
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
		dmnumbox.action = { | num |
			{dmbox[currentsource].valueAction = num.value;}.defer;

		};
		// stepsize?
		dmslider = Slider.new(win, Rect(50, 230, 110, 20));
		dmslider.value = 0.5;
		dmslider.action = { | num |
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
		angnumbox.action = { | num |
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
		angslider.action = { | num |
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
		rnumbox.action = { | num |
			{rbox[currentsource].valueAction = num.value;}.defer;

		};
		// stepsize?
		rslider = Slider.new(win, Rect(50, 270, 110, 20));
		rslider.value = 0.5;
		rslider.action = { | num |
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
		dirnumbox.action = { | num |
			{dbox[currentsource].valueAction = num.value;}.defer;
		};
		// stepsize?
		dirslider = Slider.new(win, Rect(50, 290, 110, 20));
		dirslider.value = 0;
		dirslider.action = { | num |
			{dbox[currentsource].valueAction = num.value * pi * 0.5;}.defer;
			{dirnumbox.value = num.value * pi * 0.5;}.defer;
		};


		/////////////////////////////////////////////////////////////////////////


		spreadcheck = CheckBox( win, Rect(10, 310, 80, 20), "Spread").action_({  | butt |
			{this.spcheck[currentsource].valueAction = butt.value;}.defer;
		});
		spreadcheck.value = false;

		diffusecheck = CheckBox( win, Rect(90, 310, 80, 20), "Diffuse").action_({ | butt |
			{this.dfcheck[currentsource].valueAction = butt.value;}.defer;
		});
		diffusecheck.value = false;


		/////////////////////////////////////////////////////////////////////////


		textbuf = StaticText(win, Rect(163, 330, 170, 20));
		textbuf.string = "Contraction";
		connumbox = NumberBox(win, Rect(10, 330, 40, 20));
		connumbox.value = 1;
		connumbox.clipHi = 1;
		connumbox.clipLo = 0;
		connumbox.step_(0.01);
		connumbox.scroll_step_(0.01);
		connumbox.align = \center;
		connumbox.action = { | num |
			{cbox[currentsource].valueAction = num.value;}.defer;

		};
		// stepsize?
		cslider = Slider.new(win, Rect(50, 330, 110, 20));
		cslider.value = 1;
		cslider.action = { | num |
			{cbox[currentsource].valueAction = num.value;}.defer;
			{connumbox.value = num.value;}.defer;
		};


		/////////////////////////////////////////////////////////////////////////


		textbuf = StaticText(win, Rect(163, 350, 200, 20));
		textbuf.string = "Grain rate (JoshGrain)";
		ratenumbox = NumberBox(win, Rect(10, 350, 40, 20));
		ratenumbox.value = 10;
		ratenumbox.clipHi = 60;
		ratenumbox.clipLo = 1;
		ratenumbox.step_(0.01);
		ratenumbox.scroll_step_(0.01);
		ratenumbox.align = \center;
		ratenumbox.action = { | num |
			{ratebox[currentsource].valueAction = num.value;}.defer;

		};
		// stepsize?
		rateslider = Slider.new(win, Rect(50, 350, 110, 20));
		rateslider.value = 0.16949152542373;
		rateslider.action = { | num |
			{ratebox[currentsource].valueAction = (num.value * 59) + 1;}.defer;
		};


		/////////////////////////////////////////////////////////////////////////


		textbuf = StaticText(win, Rect(163, 370, 200, 20));
		textbuf.string = "Window size (JoshGrain)";
		winumbox = NumberBox(win, Rect(10, 370, 40, 20));
		winumbox.value = 0.1;
		winumbox.clipHi = 0.2;
		winumbox.clipLo = 0;
		winumbox.step_(0.01);
		winumbox.scroll_step_(0.01);
		winumbox.align = \center;
		winumbox.action = { | num |
			{winbox[currentsource].valueAction = num.value;}.defer;

		};
		// stepsize?
		winslider = Slider.new(win, Rect(50, 370, 110, 20));
		winslider.value = 0.5;
		winslider.action = { | num |
			{winbox[currentsource].valueAction = num.value * 0.2;}.defer;
		};

		/////////////////////////////////////////////////////////


		textbuf = StaticText(win, Rect(163, 390, 200, 20));
		textbuf.string = "Rand. win. (JoshGrain)";
		randnumbox = NumberBox(win, Rect(10, 390, 40, 20));
		randnumbox.value = 0;
		randnumbox.clipHi = 1;
		randnumbox.clipLo = 0;
		randnumbox.step_(0.01);
		randnumbox.scroll_step_(0.01);
		randnumbox.align = \center;
		randnumbox.action = { | num |
			{randbox[currentsource].valueAction = num.value;}.defer;

		};
		// stepsize?
		randslider = Slider.new(win, Rect(50, 390, 110, 20));
		randslider.value = 0;
		randslider.action = { | num |
			{randbox[currentsource].valueAction = num.value.squared;}.defer;
		};

		/////////////////////////////////////////////////////////



		bload = Button(dialView, Rect(0, 20, 90, 20))
		.states_([
			["load audio", Color.black, Color.white],
		])
		.action_({ | but |
			this.synt[currentsource].free; // error check
			this.espacializador[currentsource].free;


			Dialog.openPanel(
				control.stopRecording;
				if (this.ossiarec.notNil) {
					this.ossiarec.v_(false);
				};
				//control.stop;

				{
					| path |
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

		bstream = Button(dialView, Rect(0, 40, 90, 20))
		.states_([
			["stream audio", Color.black, Color.white],
		])
		.action_({ | but |
			this.synt[currentsource].free; // error check
			this.espacializador[currentsource].free;

			Dialog.openPanel(
				control.stopRecording;
				if (this.ossiarec.notNil) {
					this.ossiarec.v_(false);
				};
				//control.stop;
				//control.seek;
				{
					| path |
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


		bnodes = Button(dialView, Rect(90, 60, 90, 20))
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

		textbuf = StaticText(wdados, Rect(300, 20, 50, 20));
		textbuf.font = Font(Font.defaultSansFace, 9);
		textbuf.string = "Lev";
		textbuf = StaticText(wdados, Rect(325, 20, 50, 20));
		textbuf.font = Font(Font.defaultSansFace, 9);
		textbuf.string = "DAmt";
		textbuf = StaticText(wdados, Rect(350, 20, 50, 20));
		textbuf.font = Font(Font.defaultSansFace, 9);
		textbuf.string = "Prox";
		textbuf = StaticText(wdados, Rect(375, 20, 50, 20));
		textbuf.font = Font(Font.defaultSansFace, 9);
		textbuf.string = "Dist";
		textbuf = StaticText(wdados, Rect(400, 20, 50, 20));
		textbuf.font = Font(Font.defaultSansFace, 9);
		textbuf.string = "Room";
		textbuf = StaticText(wdados, Rect(425, 20, 50, 20));
		textbuf.font = Font(Font.defaultSansFace, 9);
		textbuf.string = "Damp";
		textbuf = StaticText(wdados, Rect(450, 20, 50, 20));
		textbuf.font = Font(Font.defaultSansFace, 9);
		textbuf.string = "Ang";
		textbuf = StaticText(wdados, Rect(475, 20, 50, 20));
		textbuf.font = Font(Font.defaultSansFace, 9);
		textbuf.string = "Rot";
		textbuf = StaticText(wdados, Rect(500, 20, 50, 20));
		textbuf.font = Font(Font.defaultSansFace, 9);
		textbuf.string = "Dir";
		textbuf = StaticText(wdados, Rect(525, 20, 50, 20));
		textbuf.font = Font(Font.defaultSansFace, 9);
		textbuf.string = "Cont";
		textbuf = StaticText(wdados, Rect(550, 20, 50, 20));
		textbuf.font = Font(Font.defaultSansFace, 9);
		textbuf.string = "Rate";
		textbuf = StaticText(wdados, Rect(575, 20, 50, 20));
		textbuf.font = Font(Font.defaultSansFace, 9);
		textbuf.string = "Win";
		textbuf = StaticText(wdados, Rect(600, 20, 50, 20));
		textbuf.font = Font(Font.defaultSansFace, 9);
		textbuf.string = "Rand";

		textbuf = StaticText(wdados, Rect(625, 20, 50, 20));
		textbuf.font = Font(Font.defaultSansFace, 9);
		textbuf.string = "A1";
		textbuf = StaticText(wdados, Rect(650, 20, 50, 20));
		textbuf.font = Font(Font.defaultSansFace, 9);
		textbuf.string = "A2";
		textbuf = StaticText(wdados, Rect(675, 20, 50, 20));
		textbuf.font = Font(Font.defaultSansFace, 9);
		textbuf.string = "A3";
		textbuf = StaticText(wdados, Rect(700, 20, 50, 20));
		textbuf.font = Font(Font.defaultSansFace, 9);
		textbuf.string = "A4";
		textbuf = StaticText(wdados, Rect(725, 20, 50, 20));
		textbuf.font = Font(Font.defaultSansFace, 9);
		textbuf.string = "A5";

		textbuf = StaticText(wdados, Rect(750, 20, 50, 20));
		textbuf.font = Font(Font.defaultSansFace, 9);
		textbuf.string = "a1";
		textbuf = StaticText(wdados, Rect(765, 20, 50, 20));
		textbuf.font = Font(Font.defaultSansFace, 9);
		textbuf.string = "a2";
		textbuf = StaticText(wdados, Rect(780, 20, 50, 20));
		textbuf.font = Font(Font.defaultSansFace, 9);
		textbuf.string = "a3";
		textbuf = StaticText(wdados, Rect(795, 20, 50, 20));
		textbuf.font = Font(Font.defaultSansFace, 9);
		textbuf.string = "a4";
		textbuf = StaticText(wdados, Rect(810, 20, 50, 20));
		textbuf.font = Font(Font.defaultSansFace, 9);
		textbuf.string = "a5";


		textbuf = StaticText(wdados, Rect(825, 20, 50, 20));
		textbuf.font = Font(Font.defaultSansFace, 9);
		textbuf.string = "File";

		textbuf = StaticText(wdados, Rect(925, 20, 50, 20));
		textbuf.font = Font(Font.defaultSansFace, 9);
		textbuf.string = "St";





		this.nfontes.do { | i |

			textbuf = StaticText(wdados, Rect(2, 40 + (i*20), 50, 20));
			textbuf.font = Font(Font.defaultSansFace, 9);
			textbuf.string = (i+1).asString;

			this.libbox[i] = NumberBox(wdados, Rect(20, 40 + (i*20), 25, 20));

			this.dstrvbox[i] = NumberBox.new(wdados, Rect(45, 40 + (i*20), 25, 20));

			this.lpcheck[i] = CheckBox.new(wdados, Rect(70, 40 + (i*20), 40, 20));

			this.lpcheck[i].action_({ | but |
				this.lpcheckProxy[i].valueAction = but.value;
			});

			this.hwncheck[i] = CheckBox.new( wdados, Rect(85, 40 + (i*20), 40, 20));

			this.hwncheck[i].action_({ | but |
				this.hwncheckProxy[i].valueAction = but.value;
			});


			this.scncheck[i] = CheckBox.new( wdados, Rect(100, 40 + (i*20), 40, 20));

			this.scncheck[i].action_({ | but |
				this.scncheckProxy[i].valueAction = but.value;
			});

			this.spcheck[i] = CheckBox.new(wdados, Rect(115, 40 + (i*20), 40, 20));

			this.spcheck[i].action_({ | but |
				this.spcheckProxy[i].valueAction = but.value;
			});



			this.dfcheck[i] = CheckBox.new(wdados, Rect(130, 40 + (i*20), 40, 20));

			this.dfcheck[i].action_({ | but |
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

			vbox[i] = NumberBox(wdados, Rect(300, 40 + (i*20), 25, 20));
			dpbox[i] = NumberBox(wdados, Rect(325, 40+ (i*20), 25, 20));
			gbox[i] = NumberBox(wdados, Rect(350, 40+ (i*20), 25, 20));
			lbox[i] = NumberBox(wdados, Rect(375, 40+ (i*20), 25, 20));
			rmbox[i] = NumberBox(wdados, Rect(400, 40+ (i*20), 25, 20));
			dmbox[i] = NumberBox(wdados, Rect(425, 40+ (i*20), 25, 20));
			abox[i] = NumberBox(wdados, Rect(450, 40+ (i*20), 25, 20));
			rbox[i] = NumberBox(wdados, Rect(475, 40+ (i*20), 25, 20));
			dbox[i] = NumberBox(wdados, Rect(500, 40+ (i*20), 25, 20));
			cbox[i] = NumberBox(wdados, Rect(525, 40+ (i*20), 25, 20));
			ratebox[i] = NumberBox(wdados, Rect(550, 40+ (i*20), 25, 20));
			winbox[i] = NumberBox(wdados, Rect(575, 40+ (i*20), 25, 20));
			randbox[i] = NumberBox(wdados, Rect(600, 40+ (i*20), 25, 20));

			a1box[i] = NumberBox(wdados, Rect(625, 40+ (i*20), 25, 20));
			a2box[i] = NumberBox(wdados, Rect(650, 40+ (i*20), 25, 20));
			a3box[i] = NumberBox(wdados, Rect(675, 40+ (i*20), 25, 20));
			a4box[i] = NumberBox(wdados, Rect(700, 40+ (i*20), 25, 20));
			a5box[i] = NumberBox(wdados, Rect(725, 40+ (i*20), 25, 20));


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

			a1check[i] = CheckBox.new( wdados, Rect(750, 40 + (i*20), 40, 20));
			a1check[i].action = { | but |
				this.a1checkProxy[i].valueAction = but.value;
			};



			a2check[i] = CheckBox.new( wdados, Rect(765, 40 + (i*20), 40, 20));
			a2check[i].action = { | but |
				this.a2checkProxy[i].valueAction = but.value;
			};


			a3check[i] = CheckBox.new( wdados, Rect(780, 40 + (i*20), 40, 20));
			a3check[i].action = { | but |
				this.a3checkProxy[i].valueAction = but.value;
			};


			a4check[i] = CheckBox.new( wdados, Rect(795, 40 + (i*20), 40, 20));
			a4check[i].action = { | but |
				this.a4checkProxy[i].valueAction = but.value;
			};


			a5check[i] = CheckBox.new( wdados, Rect(810, 40 + (i*20), 40, 20));
			a5check[i].action = { | but |
				this.a5checkProxy[i].valueAction = but.value;
			};



			this.tfield[i] = TextField(wdados, Rect(825, 40+ (i*20), 100, 20));
			//this.tfield[i] = TextField(wdados, Rect(720, 40+ (i*20), 220, 20));

			stcheck[i] = CheckBox.new( wdados, Rect(925, 40 + (i*20), 40, 20));
			stcheck[i].action = { | but |
				this.stcheckProxy[i].valueAction = but.value;
			};

			textbuf = StaticText(wdados, Rect(940, 40 + (i*20), 50, 20));
			textbuf.font = Font(Font.defaultSansFace, 9);
			textbuf.string = (i+1).asString;

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
			ratebox[i].font = Font(Font.defaultSansFace, 9);
			winbox[i].font = Font(Font.defaultSansFace, 9);
			randbox[i].font = Font(Font.defaultSansFace, 9);

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


			a1box[i].action = { | num |
				this.a1boxProxy[i].valueAction = num.value;
			};


			a2box[i].action = { | num |
				this.a2boxProxy[i].valueAction = num.value;
			};


			a3box[i].action = { | num |
				this.a3boxProxy[i].valueAction = num.value;
			};

			a4box[i].action = { | num |
				this.a4boxProxy[i].valueAction = num.value;
			};

			a5box[i].action = { | num |
				this.a5boxProxy[i].valueAction = num.value;
			};



			this.tfield[i].action = { | path |
				this.tfieldProxy[i].valueAction = path.value;
			};




			//// PROXY ACTIONS /////////

			// gradually pinching these and putting up above

			this.xbox[i].action = { | num |
				this.xboxProxy[i].valueAction = num.value;
			};


			ybox[i].action = { | num |
				this.yboxProxy[i].valueAction = num.value;
			};
			ybox[i].value = 20;


			zbox[i].action = { | num |
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
			ratebox[i].clipHi = 60;
			ratebox[i].clipLo = 1;
			winbox[i].clipHi = 0.2;
			winbox[i].clipLo = 0;
			randbox[i].clipHi = 1.0;
			randbox[i].clipLo = 0;

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
			ratebox[i].scroll_step = 0.1;
			ratebox[i].step = 0.1;
			winbox[i].scroll_step = 0.01;
			winbox[i].step = 0.01;


			this.libbox[i].action = { | num |
				this.libboxProxy[i].valueAction = num.value;
			};
			libbox[i].value = lastSN3D + 1;

			this.dstrvbox[i].action = { | num |
				this.dstrvboxProxy[i].valueAction = num.value;
			};
			dstrvbox[i].value = 0;

			abox[i].action = { | num |
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

			vbox[i].action = { | num |
				this.vboxProxy[i].valueAction = num.value;
			};


			abox[i].value = 1.0471975511966;

			gbox[i].value = 0;
			lbox[i].value = 0;
			rmbox[i].value = 0.5;
			dmbox[i].value = 0.5;
			cbox[i].value = 1;
			ratebox[i].value = 10;
			winbox[i].value = 0.1;
			randbox[i].value = 0;

			gbox[i].action = { | num |
				this.gboxProxy[i].valueAction = num.value;
			};

			lbox[i].action = { | num |
				this.lboxProxy[i].valueAction = num.value;
			};

			rmbox[i].action = { | num |
				this.rmboxProxy[i].valueAction = num.value;
			};

			dmbox[i].action = { | num |
				this.dmboxProxy[i].valueAction = num.value;
			};

			rbox[i].action = { | num |
				this.rboxProxy[i].valueAction = num.value;
			};


			dbox[i].action = { | num |
				this.dboxProxy[i].valueAction = num.value;
			};


			cbox[i].action = { | num |
				this.cboxProxy[i].valueAction = num.value;
			};


			dpbox[i].action = { | num |
				this.dpboxProxy[i].valueAction = num.value;
			};


			this.ncanbox[i].action = { | num |
				this.ncanboxProxy[i].valueAction = num.value;
			};


			this.businibox[i].action = { | num |
				this.businiboxProxy[i].valueAction = num.value;
			};

			ratebox[i].action = { | num |
				this.rateboxProxy[i].valueAction = num.value;
			};

			winbox[i].action = { | num |
				this.winboxProxy[i].valueAction = num.value;
			};

			randbox[i].action = { | num |
				this.randboxProxy[i].valueAction = num.value;
			};



		};


		runTriggers = {
			this.nfontes.do({ | i |
				if(testado[i].not) {
					if(this.triggerFunc[i].notNil) {
						this.triggerFunc[i].value;
						//updateSynthInArgs.value(i);
					}
				}
			})
		};

		runTrigger = { | source, dirrect = false |
			//	if(scncheck[i]) {
			if(this.triggerFunc[source].notNil) {
				this.triggerFunc[source].value;
				if (dirrect && this.synt[source].isNil && (this.spheval[source].rho < 1)) {
					this.newtocar(source, 0, force: true);
				} {
					//updateSynthInArgs.value(source);
				};
				"RUNNING TRIGGER".postln;
			};
		};

		runStops = {
			this.nfontes.do({ | i |
				if(testado[i].not) {
					if(this.stopFunc[i].notNil) {
						this.stopFunc[i].value;
					}
				}
			})
		};

		runStop = { | source, dirrect = false |
			if(this.stopFunc[source].notNil) {
				this.stopFunc[source].value;
				if (dirrect) {
					firstTime[source] = false;
					this.synt[source].free;
					this.synt[source] = nil;
				};
			}
		};


		//control = Automation(dur).front(win, Rect(halfwidth, 10, 400, 25));
		/*~autotest = control = Automation(this.dur, showLoadSave: false, showSnapshot: false,
		minTimeStep: 0.001).front(win,
		Rect(10, width - 80, 400, 22));
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
			firstTime[i]=true;
			("NOW PLAYING = " ++ firstTime[i]).postln;*/
			if (this.looping) {
				this.nfontes.do { | i |
					firstTime[i]=true;
					//("HERE = " ++ firstTime[i]).postln;

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
			isPlay = true;
			//runTriggers.value;

			if (this.ossiaplay.notNil) {
				this.ossiaplay.v_(true);
			};

			{win.refresh;}.defer;

		};


		this.control.onSeek = { |time|
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

			if ((time == 0) && ossiatransport.notNil) {
				this.ossiaseekback = false;
				this.ossiatransport.v_(0);
				this.ossiaseekback = true;
			};

		};

		/*this.control.onStop = {
		runStops.value;
		"ON STOP".postln;
		this.nfontes.do { | i |
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

		this.control.onStop = {

			if(this.autoloopval.not) {
				//("Control now = " ++ control.now ++ " dur = " ++ this.dur).postln;
			};
			if(this.autoloopval.not || (this.control.now.round != this.dur)) {
				("I HAVE STOPPED. dur = " ++ this.dur ++ " now = " ++ this.control.now).postln;
				runStops.value;
				this.nfontes.do { | i |
					// if sound is currently being "tested", don't switch off on stop
					// leave that for user
					if (testado[i] == false) {
						this.synt[i].free; // error check
					};
					//	this.espacializador[i].free;
				};
				isPlay = false;
				this.looping = false;
				this.nfontes.do { | i |
					firstTime[i]=true;
					//("HERE = " ++ firstTime[i]).postln;
				};

			} {
				( "Did not stop. dur = " ++ this.dur ++ " now = " ++ this.control.now).postln;
				this.looping = true;
				this.control.play;
			};

			if (this.ossiaplay.notNil) {
				this.ossiaplay.v_(false);
			};

			{win.refresh;}.defer;
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

		win.view.mouseDownAction = { | view, x, y, modifiers, buttonNumber, clickCount |
			mouseButton = buttonNumber; // 0 = left, 2 = middle, 1 = right
			if((mouseButton == 1) || (mouseButton == 2)) {
				//dragStartScreen.x = y - halfwidth;
				//dragStartScreen.y = x - halfwidth;
				this.nfontes.do { | i |
					("" ++ i ++ " " ++ this.cartval[i]).postln;
					("" ++ i ++ " " ++ this.spheval[i]).postln;
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
			} /*{
			this.cartval[currentsource].x_((x - halfwidth) / halfheight);
			this.cartval[currentsource].y_((halfheight - y) / halfheight);
			this.cartval[currentsource].z_((this.zslider.value * 2) - 1);

			this.cartval[currentsource] = this.cartval[currentsource]
			.tumble(heading).tilt(roll).rotate(pitch);

			xbox[currentsource].valueAction = this.cartval[currentsource].x + origine.x;
			ybox[currentsource].valueAction = this.cartval[currentsource].y + origine.y;
			zbox[currentsource].valueAction = this.cartval[currentsource].z + origine.z;
			}*/
		};

		win.view.mouseMoveAction = {|view, x, y, modifiers|
			var point;

			if (mouseButton == 0) { // left button

				this.cartval[currentsource].x_((x - halfwidth) / halfheight);
				this.cartval[currentsource].y_((halfheight - y) / halfheight);
				this.cartval[currentsource].z_((this.zslider.value * 2) - 1);

				this.cartval[currentsource] = this.cartval[currentsource]
				.tumble(heading).tilt(roll).rotate(pitch);

				xbox[currentsource].valueAction = this.cartval[currentsource].x + origine.x;
				ybox[currentsource].valueAction = this.cartval[currentsource].y + origine.y;
				zbox[currentsource].valueAction = this.cartval[currentsource].z + origine.z;

			};

		};


		win.view.onResize_({|view|

			width = view.bounds.width;
			halfwidth = width * 0.5;
			height = view.bounds.height;
			halfheight = height * 0.5;

			dialView.bounds_(Rect(width - 190, 10, 180, 80));

			zSliderHeight = height * 2 / 3;
			zslider.bounds_(Rect(width - 35, ((height - zSliderHeight) * 0.5),
				20, zSliderHeight));
			znumbox.bounds_(Rect(width - 45, ((height - zSliderHeight) * 0.5)
				+ zSliderHeight, 40, 20));
			zAxis.bounds_(Rect(width - 80, halfheight - 10, 90, 20));

			orientView.bounds_(Rect(width - 285, height - 85, 285, 100));

			autoView.bounds_(Rect(10, height - 45, 325, 40));

			novoplot.value;

		});


		win.onClose_({

			wdados.close;
			waux.close;
			this.free;

		});

		mmcslave = CheckBox(autoView, Rect(163, 20, 140, 20), "Slave to MMC").action_({ | butt |
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

		this.autoloop = CheckBox(autoView, Rect(273, 20, 140, 20), "Loop").action_({ | butt |
			//("Doppler is " ++ butt.value).postln;
			if(butt.value) {
				"Looping transport".postln;
				this.autoloopval = true;
			} {
				this.autoloopval = false;
			};

			if (this.ossiatrasportLoop.notNil) {
				this.ossiatrasportLoop.v_(butt.value);
			}

		});

		this.autoloop.value = this.autoloopval;

		sysex  = { | src, sysex |
			// This should be more elaborate - other things might trigger it...fix this!
			if(sysex[3] == 6){
				("We have : " ++ sysex[4] ++ " type action").postln;

				case
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