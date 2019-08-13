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
may be downloaded here: http://escuta.org/mosca
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
	var <nfontes,
	revGlobal, nonAmbi2FuMa, convertor,
	libnumbox, <>control,
	globDec,
	sysex, mmcslave,
	synthRegistry, busini, ncan,
	aux1, aux2, aux3, aux4, aux5,  // aux slider values
	triggerFunc, stopFunc,
	scInBus,
	fumabus,
	insertFlag,
	insertBus,
	<dur,
	<>looping,
	serport,
	offsetheading,
	libbox, lpcheck, dstrvbox, hwncheck, scncheck,
	spcheck, dfcheck,
	ncanbox, businibox,
	espacializador, synt,
	tfield,
	autoloopval,
	<>autoloop,
	streamdisk,
	streambuf, // streamrate, // apparently unused
	origine,
	oxnumboxProxy, oynumboxProxy, oznumboxProxy,
	pitch, pitchnumboxProxy,
	roll, rollnumboxProxy,
	heading, headingnumboxProxy,
	// head tracking
	trackarr, trackarr2, tracki, trackPort,
	// track2arr, track2arr2, track2i,
	headingOffset,
	cartval, spheval,
	recchans, recbus,
	// mark1, mark2,	// 4 number arrays for marker data // apparently unused

	// MOVED FROM the the gui method/////////////////////////

	cbox, clev, angle, ncanais, audit, gbus, gbfbus, n3dbus,
	gbixfbus, nonambibus,
	playEspacGrp, glbRevDecGrp,
	level, lp, lib, libName, convert, dstrv, dstrvtypes, clsrv,
	clsRvtypes,
	winCtl, originCtl, hwCtl,
	xbox, ybox, sombuf, sbus, mbus,
	rbox, abox, vbox, gbox, lbox, dbox, dpbox, zbox,
	a1check, a2check, a3check, a4check, a5check, a1box, a2box, a3box,
	a4box, a5box,
	stcheck,

	//oxbox, oybox, ozbox,
	//funcs, // apparently unused
	//lastx, lasty, // apparently unused
	zlev, znumbox, zslider,
	glev,
	lslider,
	llev, rlev, dlev,
	dplev,
	auxslider1, auxslider2, auxslider3, auxslider4, auxslider5,
	auxbutton1, auxbutton2, auxbutton3, auxbutton4, auxbutton5,
	aux1numbox, aux2numbox, aux3numbox, aux4numbox, aux5numbox,
	a1but, a2but, a3but, a4but, a5but,

	loopcheck, dstReverbox, clsReverbox, hwInCheck,
	hwn, scInCheck, scn,
	spreadcheck,
	diffusecheck, sp, df,

	hdtrk,

	atualizarvariaveis, updateSynthInArgs,

	<>runTriggers, <>runStops, <>runTrigger, <>runStop,
	// isRec, // apparently unused

	/////////////////////////////////////////////////////////

	// NEW PROXY VARIABLES /////////////

	<>rboxProxy, <>cboxProxy, <>aboxProxy, <>vboxProxy, <>gboxProxy, <>lboxProxy,
	<>dboxProxy, <>dpboxProxy, <>zboxProxy, <>yboxProxy, <>xboxProxy,
	<>a1checkProxy, <>a2checkProxy, <>a3checkProxy, <>a4checkProxy, <>a5checkProxy,
	<>a1boxProxy, <>a2boxProxy, <>a3boxProxy, <>a4boxProxy, <>a5boxProxy,
	<>stcheckProxy, <>tfieldProxy, <>libboxProxy, <>lpcheckProxy, <>dstrvboxProxy,
	<>clsrvboxProxy, <>hwncheckProxy, <>scncheckProxy, <>dfcheckProxy,
	<>spcheckProxy, <>ncanboxProxy, <>businiboxProxy,

	grainrate, ratebox, <>rateboxProxy, // setable granular rate
	winsize, winbox, <>winboxProxy, // setable granular window size
	winrand, randbox, <>randboxProxy,
	// setable granular window size random factor

	rm, rmbox, <>rmboxProxy, // setable local room size
	dm, dmbox, <>dmboxProxy, // setable local dampening

	clsrm, clsrmbox, <>clsrmboxProxy, // setable global room size
	clsdm, clsdmbox, <>clsdmboxProxy, // setable global dampening

	<>masterlevProxy, masterslider,

	ossiaorient, ossiaorigine, ossiaplay, ossiatrasportLoop,
	ossiatransport, ossiaseekback, ossiarec, ossiacart, ossiasphe, ossiaaud,
	ossialoop, ossialib, ossialev, ossiadp, ossiacls, ossiaclsam,
	ossiaclsdel, ossiaclsdec, ossiadst, ossiadstam, ossiadstdel, ossiadstdec,
	ossiaangle, ossiarot, ossiadir, ossiactr, ossiaspread, ossiadiff,
	ossiaCartBack, ossiaSpheBack, ossiarate, ossiawin, ossiarand, ossiamaster,
	ossiaMasterPlay, ossiaMasterLib, ossiaMasterRev;

	/////////////////////////////////////////


	classvar server,
	rirWspectrum, rirXspectrum, rirYspectrum, rirZspectrum, rirA12Spectrum,
	rirFLUspectrum, rirFRDspectrum, rirBLDspectrum, rirBRUspectrum,
	rirList, irSpecPar, wxyzSpecPar, zSpecPar, wSpecPar,
	spatList = #["Ambitools","HoaLib","ADTB","ATK","BF-FMH","JoshGrain","VBAP"],
	// list of spat libs
	lastN3D = 2, // last N3D lib index
	lastFUMA = 5, // last FUMA lib index
	b2a, a2b, n2m,
	blips,
	maxorder,
	convert_fuma,
	convert_n3d,
	convert_direct,
	azimuths, radiusses, elevations,
	numoutputs,
	longest_radius, highest_elevation, lowest_elevation,
	vbap_buffer,
	soa_a12_decoder_matrix, soa_a12_encoder_matrix,
	cart, spher, foa_a12_decoder_matrix,
	width, halfwidth, height, halfheight, novoplot, updateGuiCtl,
	lastGui, guiInt,
	lastAutomation = nil,
	firstTime,
	isPlay = false,
	playingBF,
	currentsource,
	guiflag, baudi,
	watcher, troutine, kroutine,
	updatesourcevariables, prjDr,
	plim = 120, // distance limit from origin where processes continue to run
	fftsize = 2048, halfPi = 1.5707963267949, rad2deg = 57.295779513082 ,
	offsetLag = 2.0,  // lag in seconds for incoming GPS data
	server, foaEncoderOmni, foaEncoderSpread, foaEncoderDiffuse;
	*new { arg projDir, nsources = 10, width = 800, dur = 180, rirBank,
		server = Server.local, parentOssiaNode, allCrtitical = false, decoder,
		maxorder = 1, speaker_array, outbus = 0, suboutbus, rawformat = \FuMa, rawoutbus,
		serport, offsetheading = 0, recchans = 2, recbus = 0, guiflag = true,
		guiint = 0.07, autoloop = false;

		^super.new.initMosca(projDir, nsources, width, dur, rirBank,
			server, parentOssiaNode, allCrtitical, decoder, maxorder, speaker_array,
			outbus, suboutbus, rawformat, rawoutbus, serport, offsetheading, recchans,
			recbus, guiflag, guiint, autoloop);
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

	initMosca { | projDir, nsources, iwidth, idur, rirBank, iserver, parentOssiaNode,
		allCrtitical, decoder, imaxorder, speaker_array, outbus, suboutbus,
		rawformat, rawoutbus, iserport, ioffsetheading, irecchans, irecbus,
		iguiflag, iguiint, iautoloop |

		var makeSynthDefPlayers, makeSpatialisers, subOutFunc, playInFunc,
		localReverbFunc, localReverbStereoFunc, perfectSphereFunc,
		bfOrFmh, spatFuncs, outPutFuncs,
		bFormNumChan = (imaxorder + 1).squared,
		// add the number of channels of the b format
		fourOrNine; // switch between 4 fuma and 9 ch Matrix

		nfontes = nsources;
		maxorder = imaxorder;
		server = iserver;
		b2a = FoaDecoderMatrix.newBtoA;
		a2b = FoaEncoderMatrix.newAtoB;
		n2m = FoaEncoderMatrix.newHoa1();
		foaEncoderOmni = FoaEncoderMatrix.newOmni;
		foaEncoderSpread = FoaEncoderKernel.newSpread (subjectID: 6, kernelSize: 2048,
			server:server, sampleRate:server.sampleRate.asInteger);
		foaEncoderDiffuse = FoaEncoderKernel.newDiffuse (subjectID: 3, kernelSize: 2048,
			server:server, sampleRate:server.sampleRate.asInteger);

		if (maxorder > 1) {
			bfOrFmh = FMHEncode1;
			fourOrNine = 9;
		} {
			bfOrFmh = BFEncode1;
			fourOrNine = 4;
		};

		nfontes.do { | i |
			synthRegistry[i] = List[];

			scInBus[i] = Bus.audio(server, 1);

			insertBus[0, i] = Bus.audio(server, fourOrNine);
			insertBus[1, i] = Bus.audio(server, fourOrNine);

			insertFlag[i] = 0;
		};

		n3dbus = Bus.audio(server, bFormNumChan); // global b-format ACN-SN3D bus
		fumabus = Bus.audio(server, fourOrNine);
		gbus = Bus.audio(server, 1); // global reverb bus
		gbfbus = Bus.audio(server, fourOrNine); // global b-format bus
		gbixfbus = Bus.audio(server, fourOrNine); // global n3d b-format bus
		playEspacGrp = Group.tail;
		glbRevDecGrp = Group.after(playEspacGrp);
		server.sync;

		synthRegistry = Array.newClear(nfontes);
		insertFlag = Array.newClear(nfontes);
		insertBus = Array2D.new(2, nfontes);
		scInBus = Array.newClear(nfontes);

		if (iwidth < 600) {
			width = 600;
		} {
			width = iwidth;
		};

		halfwidth = width * 0.5;
		height = width; // on init
		halfheight = halfwidth;
		dur = idur;
		serport = iserport;
		offsetheading = ioffsetheading;
		recchans = irecchans;
		recbus = irecbus;
		guiflag = iguiflag;

		currentsource = 0;
		lastGui = Main.elapsedTime;
		guiInt = iguiint;
		autoloopval = iautoloop;

		looping = false;

		if (serport.notNil) {
			hdtrk = true;
			SerialPort.devicePattern = serport;
			// needed in serKeepItUp routine - see below
			trackPort = SerialPort(serport, 115200, crtscts: true);
			//trackarr= [251, 252, 253, 254, nil, nil, nil, nil, nil, nil,
			//	nil, nil, nil, nil, nil, nil, nil, nil, 255];  //protocol
			trackarr= [251, 252, 253, 254, nil, nil, nil, nil, nil, nil, 255];
			//protocol
			trackarr2= trackarr.copy;
			tracki= 0;
			//track2arr=
			//[247, 248, 249, 250, nil, nil, nil, nil, nil, nil, nil, nil, 255];
			//protocol
			//track2arr2= trackarr.copy;
			//track2i= 0;


			trackPort.doneAction = {
				"Serial port down".postln;
				troutine.stop;
				troutine.reset;
			};


			troutine = Routine.new({
				inf.do{
					this.matchTByte(trackPort.read);
				};
			});

			kroutine = Routine.new({
				inf.do{
					if (trackPort.isOpen.not) // if serial port is closed
					{
						"Trying to reopen serial port!".postln;
						if (SerialPort.devices.includesEqual(serport))
						// and if device is actually connected
						{
							"Device connected! Opening port!".postln;
							troutine.stop;
							troutine.reset;
							trackPort = SerialPort(serport, 115200,
								crtscts: true);
							troutine.play; // start tracker routine again
						}
					};
					1.wait;
				};
			});

			headingOffset = offsetheading;

		};


		///////////////////// DECLARATIONS FROM gui /////////////////////


		espacializador = Array.newClear(nfontes);
		libName = Array.newClear(nfontes);
		lib = Array.newClear(nfontes);
		dstrv = Array.newClear(nfontes);
		convert = Array.newClear(nfontes);
		lp = Array.newClear(nfontes);
		sp = Array.newClear(nfontes);
		df = Array.newClear(nfontes);
		dstrvtypes = Array.newClear(nfontes);
		hwn = Array.newClear(nfontes);
		scn = Array.newClear(nfontes);
		mbus = Array.newClear(nfontes);
		sbus = Array.newClear(nfontes);
		ncanais = Array.newClear(nfontes);
		// 0 = não, nem estéreo. 1 = mono. 2 = estéreo.
		ncan = Array.newClear(nfontes);
		// 0 = não, nem estéreo. 1 = mono. 2 = estéreo.
		// note that ncan refers to # of channels in streamed sources.
		// ncanais is related to sources read from file
		busini = Array.newClear(nfontes);
		// initial bus # in streamed audio grouping
		// (ie. mono, stereo or b-format)
		aux1 = Array.newClear(nfontes);
		aux2 = Array.newClear(nfontes);
		aux3 = Array.newClear(nfontes);
		aux4 = Array.newClear(nfontes);
		aux5 = Array.newClear(nfontes);

		a1but = Array.newClear(nfontes);
		a2but = Array.newClear(nfontes);
		a3but = Array.newClear(nfontes);
		a4but = Array.newClear(nfontes);
		a5but = Array.newClear(nfontes);

		sombuf = Array.newClear(nfontes);
		//		xoffset = Array.fill(nfontes, 0);
		//		yoffset = Array.fill(nfontes, 0);
		synt = Array.newClear(nfontes);
		//sprite = Array2D.new(nfontes, 2);
		angle = Array.newClear(nfontes); // ângulo dos canais estereofônicos
		zlev = Array.newClear(nfontes);
		level = Array.newClear(nfontes);
		//	doplev = Array.newClear(nfontes);
		glev = Array.newClear(nfontes);
		llev = Array.newClear(nfontes);
		rm = Array.newClear(nfontes);
		dm = Array.newClear(nfontes);
		rlev = Array.newClear(nfontes);
		dlev = Array.newClear(nfontes);
		dplev = Array.newClear(nfontes);
		clev = Array.newClear(nfontes);
		grainrate = Array.newClear(nfontes);
		winsize = Array.newClear(nfontes);
		winrand = Array.newClear(nfontes);

		ncanbox = Array.newClear(nfontes);
		businibox = Array.newClear(nfontes);
		playingBF = Array.newClear(nfontes);


		//oxbox = Array.newClear(nfontes);
		//oybox = Array.newClear(nfontes);
		//ozbox = Array.newClear(nfontes);
		xbox = Array.newClear(nfontes);
		zbox = Array.newClear(nfontes);
		ybox = Array.newClear(nfontes);
		abox = Array.newClear(nfontes); // ângulo
		vbox = Array.newClear(nfontes);  // level
		gbox = Array.newClear(nfontes); // reverberação global
		lbox = Array.newClear(nfontes); // reverberação local
		rmbox = Array.newClear(nfontes); // local room size
		dmbox = Array.newClear(nfontes); // local dampening
		rbox = Array.newClear(nfontes); // rotação de b-format
		dbox = Array.newClear(nfontes); // diretividade de b-format
		cbox = Array.newClear(nfontes); // contrair b-format
		dpbox = Array.newClear(nfontes); // dop amount
		libbox = Array.newClear(nfontes); // libs
		lpcheck = Array.newClear(nfontes); // loop
		spcheck = Array.newClear(nfontes); // spread
		dfcheck = Array.newClear(nfontes); // diffuse
		dstrvbox = Array.newClear(nfontes); // distant reverb list
		ratebox = Array.newClear(nfontes); // grain rate
		winbox = Array.newClear(nfontes); // granular window size
		randbox = Array.newClear(nfontes); // granular randomize window
		hwncheck = Array.newClear(nfontes); // hardware-in check
		scncheck = Array.newClear(nfontes); // SuperCollider-in check
		a1box = Array.newClear(nfontes); // aux - array of num boxes in data window
		a2box = Array.newClear(nfontes); // aux - array of num boxes in data window
		a3box = Array.newClear(nfontes); // aux - array of num boxes in data window
		a4box = Array.newClear(nfontes); // aux - array of num boxes in data window
		a5box = Array.newClear(nfontes); // aux - array of num boxes in data window

		a1but = Array.newClear(nfontes); // aux - array of buttons in data window
		a2but = Array.newClear(nfontes); // aux - array of buttons in data window
		a3but = Array.newClear(nfontes); // aux - array of buttons in data window
		a4but = Array.newClear(nfontes); // aux - array of buttons in data window
		a5but = Array.newClear(nfontes); // aux - array of buttons in data window

		a1check = Array.newClear(nfontes); // aux - array of buttons in data window
		a2check = Array.newClear(nfontes); // aux - array of buttons in data window
		a3check = Array.newClear(nfontes); // aux - array of buttons in data window
		a4check = Array.newClear(nfontes); // aux - array of buttons in data window
		a5check = Array.newClear(nfontes); // aux - array of buttons in data window

		stcheck = Array.newClear(nfontes); // aux - array of buttons in data window

		firstTime = Array.newClear(nfontes);


		tfield = Array.newClear(nfontes);
		streamdisk = Array.newClear(nfontes);

		// busses to send audio from player to spatialiser synths
		nfontes.do { | x |
			mbus[x] = Bus.audio(server, 1);
			sbus[x] = Bus.audio(server, 2);
			//	bfbus[x] = Bus.audio(s, 4);
		};


		audit = Array.newClear(nfontes);

		origine = Cartesian();

		pitch = 0;
		roll = 0;
		heading = 0;

		clsRvtypes = ""; // initialise close reverb type
		clsrv = 0;
		clsrm = 0.5; // initialise close reverb room size
		clsdm = 0.5; // initialise close reverb dampening


		////////////////////////////////////////////////

		////////// ADDED NEW ARRAYS and other proxy stuff  //////////////////

		// these proxies behave like GUI elements. They eneable
		// the use of Automation without a GUI

		cartval = Array.fill(nfontes, {Cartesian(0, 200, 0)});
		spheval = Array.fill(nfontes, {|i| cartval[i].asSpherical});

		rboxProxy = Array.newClear(nfontes);
		cboxProxy = Array.newClear(nfontes);
		aboxProxy = Array.newClear(nfontes);
		vboxProxy = Array.newClear(nfontes);
		gboxProxy = Array.newClear(nfontes);
		lboxProxy = Array.newClear(nfontes);
		rmboxProxy = Array.newClear(nfontes);
		dmboxProxy = Array.newClear(nfontes);
		dboxProxy = Array.newClear(nfontes);
		dpboxProxy = Array.newClear(nfontes);
		zboxProxy = Array.newClear(nfontes);
		yboxProxy = Array.newClear(nfontes);
		xboxProxy = Array.newClear(nfontes);
		a1checkProxy = Array.newClear(nfontes);
		a2checkProxy = Array.newClear(nfontes);
		a3checkProxy = Array.newClear(nfontes);
		a4checkProxy = Array.newClear(nfontes);
		a5checkProxy = Array.newClear(nfontes);
		a1boxProxy = Array.newClear(nfontes);
		a2boxProxy = Array.newClear(nfontes);
		a3boxProxy = Array.newClear(nfontes);
		a4boxProxy = Array.newClear(nfontes);
		a5boxProxy = Array.newClear(nfontes);

		tfieldProxy = Array.newClear(nfontes);
		libboxProxy = Array.newClear(nfontes);
		lpcheckProxy = Array.newClear(nfontes);
		dstrvboxProxy = Array.newClear(nfontes);
		hwncheckProxy = Array.newClear(nfontes);
		scncheckProxy = Array.newClear(nfontes);
		dfcheckProxy = Array.newClear(nfontes);
		spcheckProxy = Array.newClear(nfontes);
		ncanboxProxy = Array.newClear(nfontes);
		businiboxProxy = Array.newClear(nfontes);
		stcheckProxy = Array.newClear(nfontes);
		rateboxProxy = Array.newClear(nfontes);
		winboxProxy = Array.newClear(nfontes);
		randboxProxy = Array.newClear(nfontes);

		//set up automationProxy for single parameters outside of the previous loop,
		// not to be docked
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

		control = Automation(dur, showLoadSave: false, showSnapshot: true,
			minTimeStep: 0.001);


		////////////// DOCK PROXIES /////////////


		// this should be done after the actions are assigned


		nfontes.do { | i |

			libName[i] = spatList[0];
			lib[i] = 0;
			dstrv[i] = 0;
			convert[i] = false;
			angle[i] = 1.05;
			level[i] = 1;
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
			streamdisk[i] = false;
			ncan[i] = 0;
			ncanais[i] = 0;
			busini[i] = 0;
			audit[i] = false;
			playingBF[i] = false;
			firstTime[i] = true;

			rboxProxy[i] = AutomationGuiProxy.new(0.0);
			cboxProxy[i] = AutomationGuiProxy.new(0.0);
			aboxProxy[i] = AutomationGuiProxy.new(1.0471975511966);
			vboxProxy[i] = AutomationGuiProxy.new(1.0);
			gboxProxy[i] = AutomationGuiProxy.new(0.0);
			lboxProxy[i] = AutomationGuiProxy.new(0.0);
			rmboxProxy[i]= AutomationGuiProxy.new(0.5);
			dmboxProxy[i]= AutomationGuiProxy.new(0.5);
			dboxProxy[i] = AutomationGuiProxy.new(0.0);
			dpboxProxy[i] = AutomationGuiProxy.new(0.0);
			zboxProxy[i] = AutomationGuiProxy.new(0.0);
			yboxProxy[i] = AutomationGuiProxy.new(200.0);
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


			libboxProxy[i].action_({ | num |

				libName[i] = spatList[num.value];

				if (guiflag) {
					{ libbox[i].value = num.value }.defer;
					if(i == currentsource) {
						{ updateGuiCtl.value(\lib, num.value) }.defer;
					};
				};

				if (ossialib[i].v != num.value) {
					ossialib[i].v_(num.value);
				};
			});


			dstrvboxProxy[i].action_({ | num |
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
				{ num.value >= 3 }
				{ dstrvtypes[i] = "_conv";
					this.setSynths(i, \rv, 0); };

				if (guiflag) {
					{dstrvbox[i].value = num.value}.defer;

					if (i == currentsource) {
						{ updateGuiCtl.value(\dstrv, num.value); }.defer;
						if (num.value == 3) {
							this.setSynths(i, \rv, 1);
						}{
							this.setSynths(i, \rv, 0);
						};
					};
				};

				if (ossiadst[i].v != num.value) {
					ossiadst[i].v_(num.value);
				};
			});


			xboxProxy[i].action = { | num |
				var sphe, sphediff;
				cartval[i].x_(num.value - origine.x);
				sphe = cartval[i].asSpherical.rotate(heading)
				.tilt(pitch).tumble(roll);

				sphediff = [sphe.rho, (sphe.theta - halfPi).wrap(-pi, pi), sphe.phi];
				if (ossiaSpheBack && (ossiasphe[i].v != sphediff)) {
					ossiaCartBack = false;
					ossiasphe[i].v_(sphediff);
					ossiaCartBack = true;
				};
				if (ossiacart[i].v[0] != num.value) {
					ossiacart[i].v_([num.value, yboxProxy[i].value,
						zboxProxy[i].value]);
				};

				if ( guiflag) {
					{xbox[i].value = num.value}.defer;
					{novoplot.value;}.defer;
				};
				if(espacializador[i].notNil || playingBF[i]) {
					espacializador[i].set(\azim, spheval[i].theta);
					this.setSynths(i, \azim, spheval[i].theta);
					synt[i].set(\azim, spheval[i].theta);
					espacializador[i].set(\elev, spheval[i].phi);
					this.setSynths(i, \elev, spheval[i].phi);
					synt[i].set(\elev, spheval[i].phi);
					espacializador[i].set(\radius, spheval[i].rho);
					this.setSynths(i, \radius, spheval[i].rho);
					synt[i].set(\radius, spheval[i].rho);
				};
			};

			yboxProxy[i].action = { | num |
				var sphe, sphediff;
				cartval[i].y_(num.value - origine.y);
				sphe = cartval[i].asSpherical.rotate(heading)
				.tilt(pitch).tumble(roll);

				sphediff = [sphe.rho, (sphe.theta - halfPi).wrap(-pi, pi), sphe.phi];
				if (ossiaSpheBack && (ossiasphe[i].v != sphediff)) {
					ossiaCartBack = false;
					ossiasphe[i].v_(sphediff);
					ossiaCartBack = true;
				};
				if (ossiacart[i].v[1] != num.value) {
					ossiacart[i].v_([xboxProxy[i].value, num.value,
						zboxProxy[i].value]);
				};

				if (guiflag) {
					{ybox[i].value = num.value}.defer;
					{novoplot.value;}.defer;
				};
				if(espacializador[i].notNil || playingBF[i]){
					espacializador[i].set(\azim, spheval[i].theta);
					this.setSynths(i, \azim, spheval[i].theta);
					synt[i].set(\azim, spheval[i].theta);
					espacializador[i].set(\elev, spheval[i].phi);
					this.setSynths(i, \elev, spheval[i].phi);
					synt[i].set(\elev, spheval[i].phi);
					espacializador[i].set(\radius, spheval[i].rho);
					this.setSynths(i, \radius, spheval[i].rho);
					synt[i].set(\radius, spheval[i].rho);
				};
			};

			zboxProxy[i].action = { | num |
				var sphe, sphediff;
				cartval[i].z_(num.value - origine.z);
				sphe = cartval[i].asSpherical.rotate(heading)
				.tilt(pitch).tumble(roll);

				sphediff = [sphe.rho, (sphe.theta - halfPi).wrap(-pi, pi), sphe.phi];
				if (ossiaSpheBack && (ossiasphe[i].v != sphediff)) {
					ossiaCartBack = false;
					ossiasphe[i].v_(sphediff);
					ossiaCartBack = true;
				};
				if (ossiacart[i].v[2] != num.value) {
					ossiacart[i].v_([xboxProxy[i].value, yboxProxy[i].value,
						num.value]);
				};
				zlev[i] = spheval[i].z;
				if (guiflag) {
					{zbox[i].value = num.value}.defer;
					{novoplot.value;}.defer;
				};
				if(espacializador[i].notNil || playingBF[i]){
					espacializador[i].set(\azim, spheval[i].theta);
					this.setSynths(i, \azim, spheval[i].theta);
					synt[i].set(\azim, spheval[i].theta);
					espacializador[i].set(\elev, spheval[i].phi);
					this.setSynths(i, \elev, spheval[i].phi);
					synt[i].set(\elev, spheval[i].phi);
					espacializador[i].set(\radius, spheval[i].rho);
					this.setSynths(i, \radius, spheval[i].rho);
					synt[i].set(\radius, spheval[i].rho);
				};
			};

			aboxProxy[i].action = { | num |
				if(espacializador[i].notNil) {
					angle[i] = num.value;
					espacializador[i].set(\angle, num.value);
					this.setSynths(i, \angle, num.value);
					angle[i] = num.value;
				};
				if (guiflag) {
					{abox[i].value = num.value}.defer;
					if(i == currentsource)
					{
						{winCtl[0][7].value = num.value}.defer;
						{winCtl[1][7].value = num.value / pi}.defer;
					};
				};

				if (ossiaangle[i].v != num.value) {
					ossiaangle[i].v_(num.value);
				};
			};

			vboxProxy[i].action = { | num |
				synt[i].set(\level, num.value);
				this.setSynths(i, \level, num.value);
				level[i] = num.value;

				if (guiflag) {
					{ vbox[i].value = num.value }.defer;
					if(i == currentsource)
					{
						{ winCtl[1][0].value = num.value * 0.5 }.defer;
						{ winCtl[0][0].value = num.value }.defer;
					};
				};

				if (ossialev[i].v != num.value) {
					ossialev[i].v_(num.value);
				};
			};

			gboxProxy[i].action = { | num |
				espacializador[i].set(\glev, num.value);
				this.setSynths(i, \glev, num.value);
				synt[i].set(\glev, num.value);
				glev[i] = num.value;
				if (guiflag) {
					{ gbox[i].value = num.value }.defer;
					if (i == currentsource)
					{
						{ winCtl[0][3].value = num.value }.defer;
						{ winCtl[1][3].value = num.value }.defer;
					};
				};

				if (ossiaclsam[i].v != num.value) {
					ossiaclsam[i].v_(num.value);
				};
			};

			lboxProxy[i].action = { | num |
				espacializador[i].set(\llev, num.value);
				this.setSynths(i, \llev, num.value);
				synt[i].set(\llev, num.value);
				llev[i] = num.value;
				if (guiflag) {
					{ lbox[i].value = num.value; }.defer;
					if (i == currentsource)
					{
						{ winCtl[0][4].value = num.value }.defer;
						{ winCtl[1][4].value = num.value }.defer;
					};
				};

				if (ossiadstam[i].v != num.value) {
					ossiadstam[i].v_(num.value);
				};
			};

			rmboxProxy[i].action = { | num |
				espacializador[i].set(\room, num.value);
				this.setSynths(i, \room, num.value);
				synt[i].set(\room, num.value);
				rm[i] = num.value;
				if (guiflag) {
					{ rmbox[i].value = num.value; }.defer;
					if (i == currentsource) {
						{ winCtl[0][5].value = num.value }.defer;
						{ winCtl[1][5].value = num.value }.defer;
					};
				};

				if (ossiadstdel[i].v != num.value) {
					ossiadstdel[i].v_(num.value);
				};
			};

			dmboxProxy[i].action = { | num |
				espacializador[i].set(\damp, num.value);
				this.setSynths(i, \damp, num.value);
				synt[i].set(\damp, num.value);
				dm[i] = num.value;
				if (guiflag) {
					{ dmbox[i].value = num.value; };
					if (i == currentsource) {
						{ winCtl[0][6].value = num.value }.defer;
						{ winCtl[1][6].value = num.value }.defer;
					};
				};

				if (ossiadstdec[i].v != num.value) {
					ossiadstdec[i].v_(num.value);
				};
			};

			rboxProxy[i].action = { | num |
				synt[i].set(\rotAngle, num.value);
				this.setSynths(i, \rotAngle, num.value);
				rlev[i] = num.value;
				if (guiflag) {
					{rbox[i].value = num.value}.defer;
					if(i == currentsource)
					{
						{winCtl[0][8].value = num.value}.defer;
						{winCtl[1][8].value = (num.value + pi) / 2pi}.defer;
					};
				};

				if (ossiarot[i].v != num.value) {
					ossiarot[i].v_(num.value);
				};
			};

			dboxProxy[i].action = { | num |
				synt[i].set(\directang, num.value);
				this.setSynths(i, \directang, num.value);
				dlev[i] = num.value;
				if (guiflag) {
					{dbox[i].value = num.value}.defer;
					if(i == currentsource)
					{
						{winCtl[0][9].value = num.value}.defer;
						{winCtl[1][9].value = num.value / halfPi}.defer;
					};
				};

				if (ossiadir[i].v != num.value) {
					ossiadir[i].v_(num.value);
				};
			};

			cboxProxy[i].action = { | num |
				synt[i].set(\contr, num.value);
				// TESTING
				espacializador[i].set(\contr, num.value);
				this.setSynths(i, \contr, num.value);
				clev[i] = num.value;
				if (guiflag) {
					{cbox[i].value = num.value}.defer;
					if (i == currentsource)
					{
						{winCtl[0][2].value = num.value}.defer;
						{winCtl[1][2].value = num.value}.defer;
					};
				};

				if (ossiactr[i].v != num.value) {
					ossiactr[i].v_(num.value);
				};
			};

			dpboxProxy[i].action = { | num |
				// used for b-format amb/bin only
				synt[i].set(\dopamnt, num.value);
				this.setSynths(i, \dopamnt, num.value);
				// used for the others
				espacializador[i].set(\dopamnt, num.value);
				dplev[i] = num.value;
				if (guiflag) {
					{dpbox[i].value = num.value}.defer;
					if(i == currentsource) {
						{winCtl[1][1].value = num.value}.defer;
						{winCtl[0][1].value = num.value}.defer;
					};
				};

				if (ossiadp[i].v != num.value) {
					ossiadp[i].v_(num.value);
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
					{a1box[i].value = num.value}.defer;
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
					{a2box[i].value = num.value}.defer;
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
					{a3box[i].value = num.value}.defer;
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
					{a4box[i].value = num.value}.defer;
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
					{a5box[i].value = num.value}.defer;
				};
			};

			a1checkProxy[i].action = { | but |

				if (but.value) {
					a1but[i] = 1;
					this.setSynths(i, \a1check, 1);
				}{
					this.a1but[i] = 0;
					setSynths(i, \a1check, 0);
				};

				if (guiflag) {

					{a1check[i].value = but.value}.defer;
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
					{a2check[i].value = but.value}.defer;
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
					{a3check[i].value = but.value}.defer;
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
					{a4check[i].value = but.value}.defer;
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
					{a5check[i].value = but.value}.defer;
				};
			};

			stcheckProxy[i].action = { | but |
				if (but.value) {
					streamdisk[i] = true;
				}{
					streamdisk[i] = false;
				};
				if (guiflag) {
					{stcheck[i].value = but.value}.defer;
				};
			};


			lpcheckProxy[i].action_({ | but |
				if (but.value) {
					lp[i] = 1;
					synt[i].set(\lp, 1);
					this.setSynths(i, \lp, 1);
				} {
					lp[i] = 0;
					synt[i].set(\lp, 0);
					this.setSynths(i, \lp, 0);
				};
				if (guiflag) {
					{ lpcheck[i].value = but.value }.defer;
					if(i==currentsource) {
						{ loopcheck.value = but.value }.defer;
					};
				};

				if (ossialoop[i].v != but.value.asBoolean) {
					ossialoop[i].v_(but.value.asBoolean);
				};
			});

			hwncheckProxy[i].action = { | but |
				if((i==currentsource) && guiflag) {{hwInCheck.value = but.value}.defer;
				};
				if (but.value == true) {
					if (guiflag) {
						{scncheck[i].value = false}.defer;
					};
					if((i==currentsource) && guiflag){{scInCheck.value = false}.defer;};
					hwn[i] = 1;
					scn[i] = 0;
					synt[i].set(\hwn, 1);
				}{
					hwn[i] = 0;
					synt[i].set(\hwn, 0);
				};
				if (guiflag) {
					{hwncheck[i].value = but.value}.defer;
					updateGuiCtl.value(\src);
				};
			};

			scncheckProxy[i].action_({ | but |
				if((i==currentsource) && guiflag) {{scInCheck.value = but.value}.defer;};
				if (but.value == true) {
					if (guiflag) {
						{hwncheck[i].value = false}.defer;
					};
					if((i==currentsource) && guiflag){{hwInCheck.value = false}.defer;};
					scn[i] = 1;
					hwn[i] = 0;
					synt[i].set(\scn, 1);
				}{
					scn[i] = 0;
					synt[i].set(\scn, 0);
				};
				if (guiflag) {
					{scncheck[i].value = but.value}.defer;
					updateGuiCtl.value(\src);
				};
			});

			spcheckProxy[i].action_({ | but |
				if((i==currentsource) && guiflag){{spreadcheck.value = but.value}.defer;};
				if (but.value) {
					if (guiflag) {
						{dfcheck[i].value = false}.defer;
					};
					if((i==currentsource) && guiflag){
						{diffusecheck.value = false}.defer;};
					sp[i] = 1;
					df[i] = 0;
					espacializador[i].set(\sp, 1);
					espacializador[i].set(\df, 0);
					synt[i].set(\sp, 1);
					this.setSynths(i, \ls, 1);
					ossiadiff[i].v_(false);
				} {
					sp[i] = 0;
					espacializador[i].set(\sp, 0);
					synt[i].set(\sp, 0);
					this.setSynths(i, \sp, 0);
				};
				if (guiflag) {
					{spcheck[i].value = but.value}.defer;
				};

				if (ossiaspread[i].v != but.value.asBoolean) {
					ossiaspread[i].v_(but.value.asBoolean);
				};
			});

			dfcheckProxy[i].action_({ | but |
				if((i==currentsource) && guiflag){
					{diffusecheck.value = but.value}.defer;};
				if (but.value) {
					if (guiflag) {
						{spcheck[i].value = false}.defer;
					};
					if((i==currentsource) && guiflag) {
						{spreadcheck.value = false}.defer;
					};
					df[i] = 1;
					sp[i] = 0;
					espacializador[i].set(\df, 1);
					espacializador[i].set(\sp, 0);
					synt[i].set(\df, 1);
					this.setSynths(i, \df, 1);
					ossiaspread[i].v_(false);
				} {
					df[i] = 0;
					espacializador[i].set(\df, 0);
					synt[i].set(\df, 0);
					this.setSynths(i, \df, 0);
				};
				if (guiflag) {
					{dfcheck[i].value = but.value}.defer;
				};

				if (ossiadiff[i].v != but.value.asBoolean) {
					ossiadiff[i].v_(but.value.asBoolean);
				};
			});

			ncanboxProxy[i].action = { | num |
				ncan[i] = num.value;
				if (guiflag ) {
					{ ncanbox[i].value = num.value }.defer;

					if (i == currentsource) {
						{ updateGuiCtl.value(\src); }.defer;
					};
				};
			};

			businiboxProxy[i].action = { | num |
				busini[i] = num.value;
				if (guiflag) {
					{ businibox[i].value = num.value }.defer;

					if (i == currentsource) {
						{ updateGuiCtl.value(\src); }.defer;
					};
				};
			};

			rateboxProxy[i].action = { | num |
				espacializador[i].set(\grainrate, num.value);
				this.setSynths(i, \grainrate, num.value);
				synt[i].set(\grainrate, num.value);
				grainrate[i] = num.value;
				if (guiflag) {
					{ ratebox[i].value = num.value }.defer;
					if (i == currentsource) {
						{ winCtl[0][10].value = num.value }.defer;
						{ winCtl[1][10].value = (num.value - 1) / 59 }.defer;
					};
				};

				if (ossiarate[i].v != num.value) {
					ossiarate[i].v_(num.value);
				};
			};

			winboxProxy[i].action = { | num |
				espacializador[i].set(\winsize, num.value);
				setSynths(i, \winsize, num.value);
				synt[i].set(\winsize, num.value);
				winsize[i] = num.value;
				if (guiflag) {
					{ winbox[i].value = num.value; }.defer;
					if (i == currentsource) {
						{ winCtl[0][11].value = num.value }.defer;
						{ winCtl[1][11].value = num.value * 5 }.defer;
					};
				};

				if (ossiawin[i].v != num.value) {
					ossiawin[i].v_(num.value);
				};
			};

			randboxProxy[i].action = { | num |
				espacializador[i].set(\winrand, num.value);
				setSynths(i, \winrand, num.value);
				synt[i].set(\winrand, num.value);
				winrand[i] = num.value;
				if (guiflag) {
					{ randbox[i].value = num.value; }.defer;
					if (i == currentsource) {
						{winCtl[0][12].value = num.value}.defer;
						{winCtl[1][12].value = num.value.sqrt}.defer;
					};
				};

				if (ossiarand[i].v != num.value) {
					ossiarand[i].v_(num.value);
				};
			};

			tfieldProxy[i].action = { | path |
				var sf = SoundFile.new;
				sf.openRead(path);
				ncanais[i] = sf.numChannels;
				sf.close;

				if (sombuf[i].notNil) {
					sombuf[i].free;
				};

				if (path.notNil || (path.value != "")) {
					if (streamdisk[i].not) {
						sombuf[i].free;
						sombuf[i] = Buffer.read(server, path.value, action: { | buf |
							"Loaded file".postln;
						});
					} {
						"To stream file".postln;
					};
				};

				if (guiflag) {
					{ tfield[i].value = path.value; }.defer;
					{ updateGuiCtl.value(\chan); }.defer;
				};

				ossiaaud[i].description =
				PathName(path.value).fileNameWithoutExtension;
			};

			control.dock(xboxProxy[i], "x_axisProxy_" ++ i);
			control.dock(yboxProxy[i], "y_axisProxy_" ++ i);
			control.dock(zboxProxy[i], "z_axisProxy_" ++ i);
			control.dock(vboxProxy[i], "levelProxy_" ++ i);
			control.dock(dpboxProxy[i], "dopamtProxy_" ++ i);
			control.dock(gboxProxy[i], "revglobalProxy_" ++ i);
			control.dock(dstrvboxProxy[i], "localrevkindProxy_" ++ i);
			control.dock(lboxProxy[i], "revlocalProxy_" ++ i);
			control.dock(rmboxProxy[i], "localroomProxy_" ++ i);
			control.dock(dmboxProxy[i], "localdampProxy_" ++ i);
			control.dock(aboxProxy[i], "angleProxy_" ++ i);
			control.dock(rboxProxy[i], "rotationProxy_" ++ i);
			control.dock(dboxProxy[i], "directivityProxy_" ++ i);
			control.dock(cboxProxy[i], "contractionProxy_" ++ i);
			control.dock(rateboxProxy[i], "grainrateProxy_" ++ i);
			control.dock(winboxProxy[i], "windowsizeProxy_" ++ i);
			control.dock(randboxProxy[i], "randomwindowProxy_" ++ i);
			control.dock(a1boxProxy[i], "aux1Proxy_" ++ i);
			control.dock(a2boxProxy[i], "aux2Proxy_" ++ i);
			control.dock(a3boxProxy[i], "aux3Proxy_" ++ i);
			control.dock(a4boxProxy[i], "aux4Proxy_" ++ i);
			control.dock(a5boxProxy[i], "aux5Proxy_" ++ i);
			control.dock(a1checkProxy[i], "aux1checkProxy_" ++ i);
			control.dock(a2checkProxy[i], "aux2checkProxy_" ++ i);
			control.dock(a3checkProxy[i], "aux3checkProxy_" ++ i);
			control.dock(a4checkProxy[i], "aux4checkProxy_" ++ i);
			control.dock(a5checkProxy[i], "aux5checkProxy_" ++ i);
			//control.dock(stcheckProxy[i], "stcheckProxy_" ++ i);

		};


		masterlevProxy.action_({ | num |

			globDec.set(\level, num.value);

			if (guiflag) {
				masterslider.value = num.value * 0.5;
			};

			if (ossiamaster.v != num.value) {
				ossiamaster.v_(num.value);
			};
		});


		clsrvboxProxy.action_({ | num |

			clsrv = num.value;

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
				{ revGlobal.set(\gate, 0) };
			} {
				if (convert_fuma) {
					if (convertor.notNil) {
						convertor.set(\gate, 1);
					} {
						convertor = Synth.new(\ambiConverter, [\gate, 1],
							target:glbRevDecGrp).onFree({
							convertor = nil;
						});
					};
				};

				if (revGlobal.notNil)
				{ revGlobal.set(\gate, 0); };

				revGlobal = Synth.new(\revGlobalAmb++clsRvtypes,
					[\gbfbus, gbfbus, \gbixfbus, gbixfbus, \gate, 1,
						\room, clsrm, \damp, clsdm] ++
					irSpecPar.value(max((clsrv - 3), 0)),
					glbRevDecGrp).register.onFree({
					if (revGlobal.isPlaying.not) {
						revGlobal = nil;
					};
					if (convertor.notNil) {
						if (convert_fuma) {
							if (this.converterNeeded(0).not) {
								convertor.set(\gate, 0);
							};
						};
					};
				});
			};

			if (guiflag) {
				{ updateGuiCtl.value(\clsrv, num.value) }.defer;
			};

			if (ossiacls.v != num.value) {
				ossiacls.v_(num.value);
			};
		});


		clsrmboxProxy.action_({ | num |
			glbRevDecGrp.set(\room, num.value);
			clsrm = num.value;

			if (guiflag) {
				{originCtl[0][0].value = num.value}.defer;
			};

			if (ossiaclsdel.v != num.value) {
				ossiaclsdel.v_(num.value);
			};
		});


		clsdmboxProxy.action_({ | num |

			glbRevDecGrp.set(\damp, num.value);

			clsdm = num.value;

			if (guiflag) {
				{originCtl[0][0].value = num.value}.defer;
			};

			if (ossiaclsdec.v != num.value) {
				ossiaclsdec.v_(num.value);
			};
		});


		oxnumboxProxy.action_({ | num |

			ossiaorigine.v_([num.value, oynumboxProxy.value,
				oznumboxProxy.value]);
			ossiaCartBack = false;

			nfontes.do {  | i |
				var sphe = (cartval[i].x_(cartval[i].x - num.value
					+ origine.x)).asSpherical.
				rotate(heading).tilt(pitch).tumble(roll);

				ossiasphe[i].v_([sphe.rho,
					(sphe.theta + halfPi).wrap(-pi, pi), sphe.phi]);

				zlev[i] = spheval[i].z;

				if(espacializador[i].notNil || playingBF[i]) {
					espacializador[i].set(\azim, spheval[i].theta);
					this.setSynths(i, \azim, spheval[i].theta);
					synt[i].set(\azim, spheval[i].theta);
					espacializador[i].set(\elev, spheval[i].phi);
					this.setSynths(i, \elev, spheval[i].phi);
					synt[i].set(\elev, spheval[i].phi);
					espacializador[i].set(\radius, spheval[i].rho);
					this.setSynths(i, \radius, spheval[i].rho);
					synt[i].set(\radius, spheval[i].rho);
				};
			};

			ossiaCartBack = true;

			origine.x_(num.value);

			if (guiflag) {
				{novoplot.value;}.defer;
				{originCtl[0][5].value = num.value;}.defer;
			};
		});



		oynumboxProxy.action_({ | num |

			ossiaorigine.v_([oxnumboxProxy.value, num.value,
				oznumboxProxy.value]);
			ossiaCartBack = false;

			nfontes.do { | i |
				var sphe = (cartval[i].y_(cartval[i].y - num.value
					+ origine.y)).asSpherical.
				rotate(heading).tilt(pitch).tumble(roll);

				ossiasphe[i].v_([sphe.rho,
					(sphe.theta + halfPi).wrap(-pi, pi), sphe.phi]);

				zlev[i] = spheval[i].z;

				if(espacializador[i].notNil || playingBF[i]) {
					espacializador[i].set(\azim, spheval[i].theta);
					this.setSynths(i, \azim, spheval[i].theta);
					synt[i].set(\azim, spheval[i].theta);
					espacializador[i].set(\elev, spheval[i].phi);
					this.setSynths(i, \elev, spheval[i].phi);
					synt[i].set(\elev, spheval[i].phi);
					espacializador[i].set(\radius, spheval[i].rho);
					this.setSynths(i, \radius, spheval[i].rho);
					synt[i].set(\radius, spheval[i].rho);
				};

				ossiaCartBack = true;
			};

			origine.y_(num.value);

			if (guiflag) {
				{novoplot.value;}.defer;
				{originCtl[0][6].value = num.value;}.defer;
			};
		});


		oznumboxProxy.action_({ | num |

			ossiaorigine.v_([oxnumboxProxy.value,
				oynumboxProxy.value, num.value]);
			ossiaCartBack = false;

			nfontes.do { | i |
				var sphe = (cartval[i].z_(cartval[i].z - num.value
					+ origine.z)).asSpherical.
				rotate(heading).tilt(pitch).tumble(roll);

				ossiasphe[i].v_([sphe.rho,
					(sphe.theta + halfPi).wrap(-pi, pi), sphe.phi]);

				zlev[i] = spheval[i].z;

				if(espacializador[i].notNil || playingBF[i]) {
					espacializador[i].set(\azim, spheval[i].theta);
					this.setSynths(i, \azim, spheval[i].theta);
					synt[i].set(\azim, spheval[i].theta);
					espacializador[i].set(\elev, spheval[i].phi);
					this.setSynths(i, \elev, spheval[i].phi);
					synt[i].set(\elev, spheval[i].phi);
					espacializador[i].set(\radius, spheval[i].rho);
					this.setSynths(i, \radius, spheval[i].rho);
					synt[i].set(\radius, spheval[i].rho);
				};

				ossiaCartBack = true;
			};

			origine.z_(num.value);

			if (guiflag) {
				{novoplot.value;}.defer;
				{originCtl[0][7].value = num.value;}.defer;
			};
		});



		headingnumboxProxy.action_({ | num |

			ossiaorient.v_([num.value, pitchnumboxProxy.value,
				rollnumboxProxy.value]);
			ossiaCartBack = false;

			nfontes.do { | i |
				var euler = cartval[i];

				euler = euler.rotate(num.value.neg).tilt(pitch).tumble(roll);
				//euler = euler.rotate(num.value).tilt(pitch).tumble(roll);

				ossiasphe[i].v_([euler.rho,
					euler.theta - halfPi, euler.phi]);

				zlev[i] = spheval[i].z;

				if(espacializador[i].notNil || playingBF[i]) {
					espacializador[i].set(\azim, spheval[i].theta);
					this.setSynths(i, \azim, spheval[i].theta);
					synt[i].set(\azim, spheval[i].theta);
					espacializador[i].set(\elev, spheval[i].phi);
					this.setSynths(i, \elev, spheval[i].phi);
					synt[i].set(\elev, spheval[i].phi);
					espacializador[i].set(\radius, spheval[i].rho);
					this.setSynths(i, \radius, spheval[i].rho);
					synt[i].set(\radius, spheval[i].rho);
				};
			};

			ossiaCartBack = true;
			heading = num.value.neg;

			if (guiflag) {
				{novoplot.value;}.defer;
				{originCtl[0][2].value = num.value;}.defer;
			};
		});


		pitchnumboxProxy.action_({ | num |

			ossiaorient.v_([headingnumboxProxy.value, num.value,
				rollnumboxProxy.value]);
			ossiaCartBack = false;

			nfontes.do { | i |
				var euler = cartval[i];

				euler = euler.rotate(heading).tilt(num.value.neg).tumble(roll);
				//	euler = euler.rotate(heading).tilt(num.value).tumble(roll);

				ossiasphe[i].v_([euler.rho,
					euler.theta - halfPi, euler.phi]);

				zlev[i] = spheval[i].z;

				if(espacializador[i].notNil || playingBF[i]) {
					espacializador[i].set(\azim, spheval[i].theta);
					this.setSynths(i, \azim, spheval[i].theta);
					synt[i].set(\azim, spheval[i].theta);
					espacializador[i].set(\elev, spheval[i].phi);
					this.setSynths(i, \elev, spheval[i].phi);
					synt[i].set(\elev, spheval[i].phi);
					espacializador[i].set(\radius, spheval[i].rho);
					this.setSynths(i, \radius, spheval[i].rho);
					synt[i].set(\radius, spheval[i].rho);
				};
			};

			ossiaCartBack = true;
			pitch = num.value.neg;

			if (guiflag) {
				{novoplot.value;}.defer;
				{originCtl[0][3].value = num.value;}.defer;
			};
		});


		rollnumboxProxy.action_({ | num |

			ossiaorient.v_([headingnumboxProxy.value,
				pitchnumboxProxy.value, num.value]);
			ossiaCartBack = false;

			nfontes.do { | i |
				var euler = cartval[i];

				//euler = euler.rotate(heading).tilt(pitch).tumble(num.value.neg);
				euler = euler.rotate(heading).tilt(pitch).tumble(num.value);

				ossiasphe[i].v_([euler.rho,
					euler.theta - halfPi, euler.phi]);

				zlev[i] = spheval[i].z;

				if(espacializador[i].notNil || playingBF[i]) {
					espacializador[i].set(\azim, spheval[i].theta);
					this.setSynths(i, \azim, spheval[i].theta);
					synt[i].set(\azim, spheval[i].theta);
					espacializador[i].set(\elev, spheval[i].phi);
					this.setSynths(i, \elev, spheval[i].phi);
					synt[i].set(\elev, spheval[i].phi);
					espacializador[i].set(\radius, spheval[i].rho);
					this.setSynths(i, \radius, spheval[i].rho);
					synt[i].set(\radius, spheval[i].rho);
				};
			};

			ossiaCartBack = true;
			roll = num.value.neg;

			if (guiflag) {
				{novoplot.value;}.defer;
				{originCtl[0][4].value = num.value;}.defer;
			};
		});


		control.dock(clsrvboxProxy, "globrevkindProxy");
		control.dock(clsrmboxProxy, "localroomProxy");
		control.dock(clsdmboxProxy, "localdampProxy");
		control.dock(oxnumboxProxy, "oxProxy");
		control.dock(oynumboxProxy, "oyProxy");
		control.dock(oznumboxProxy, "ozProxy");
		control.dock(pitchnumboxProxy, "pitchProxy");
		control.dock(rollnumboxProxy, "rollProxy");
		control.dock(headingnumboxProxy, "headingProxy");


		///////////////////////////////////////////////////


		playInFunc = Array.newClear(4);
		// one for File, Stereo, BFormat, Stream - streamed file;

		streambuf = Array.newClear(nfontes);

		// array of functions, 1 for each source (if defined),
		// that will be launched on Automation's "play"
		triggerFunc = Array.newClear(nfontes);
		//companion to above. Launched by "Stop"
		stopFunc = Array.newClear(nfontes);


		// can place headtracker rotations in these functions
		// don't forget that the synthdefs
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
			[ 0.11785113, 0.212662702, 0, -0.131432778, -0.0355875819, -0.279508497, 0,
				0.226127124, 0 ],
			[ 0.11785113, 0.131432778, -0.212662702, 0, -0.208333333, 0, 0, -0.139754249,
				-0.279508497 ],
			[ 0.11785113, 0, -0.131432778, 0.212662702, 0.243920915, 0, -0.279508497,
				-0.0863728757, 0 ],
			[ 0.11785113, 0.212662702, 0, 0.131432778, -0.0355875819, 0.279508497, 0,
				0.226127124, 0 ],
			[ 0.11785113, -0.131432778, -0.212662702, 0, -0.208333333, 0, 0, -0.139754249,
				0.279508497 ],
			[ 0.11785113, 0, 0.131432778, -0.212662702, 0.243920915, 0, -0.279508497,
				-0.0863728757, 0 ],
			[ 0.11785113, -0.212662702, 0, -0.131432778, -0.0355875819, 0.279508497, 0,
				0.226127124, 0 ],
			[ 0.11785113, -0.131432778, 0.212662702, 0, -0.208333333, 0, 0, -0.139754249,
				-0.279508497 ],
			[ 0.11785113, 0, 0.131432778, 0.212662702, 0.243920915, 0, 0.279508497,
				-0.0863728757, 0 ],
			[ 0.11785113, -0.212662702, 0, 0.131432778, -0.0355875819, -0.279508497, 0,
				0.226127124, 0 ],
			[ 0.11785113, 0.131432778, 0.212662702, 0, -0.208333333, 0, 0, -0.139754249,
				0.279508497 ],
			[ 0.11785113, 0, -0.131432778, -0.212662702, 0.243920915, 0, 0.279508497,
				-0.0863728757, 0 ],
		]);

		// a-12 encoder matrix
		soa_a12_encoder_matrix = Matrix.with([
			[ 0.707106781, 0.707106781, 0.707106781, 0.707106781, 0.707106781,
				0.707106781,0.707106781,
				0.707106781, 0.707106781, 0.707106781, 0.707106781, 0.707106781 ],
			[ 0.850650808, 0.525731112, 0, 0.850650808, -0.525731112, 0, -0.850650808,
				-0.525731112, 0,
				-0.850650808, 0.525731112, 0 ],
			[ 0, -0.850650808, -0.525731112, 0, -0.850650808, 0.525731112, 0, 0.850650808,
				0.525731112,
				0, 0.850650808, -0.525731112 ],
			[ -0.525731112, 0, 0.850650808, 0.525731112, 0, -0.850650808, -0.525731112, 0,
				0.850650808,
				0.525731112, 0, -0.850650808 ],
			[ -0.0854101966, -0.5, 0.585410197, -0.0854101966, -0.5, 0.585410197,
				-0.0854101966, -0.5,
				0.585410197, -0.0854101966, -0.5, 0.585410197 ],
			[ -0.894427191, 0, 0, 0.894427191, 0, 0, 0.894427191, 0, 0, -0.894427191,
				0, 0 ],
			[ 0, 0, -0.894427191, 0, 0, -0.894427191, 0, 0, 0.894427191, 0, 0,
				0.894427191 ],
			[ 0.723606798, -0.447213596, -0.276393202, 0.723606798, -0.447213596,
				-0.276393202,
				0.723606798, -0.447213596, -0.276393202, 0.723606798, -0.447213596,
				-0.276393202 ],
			[ 0, -0.894427191, 0, 0, 0.894427191, 0, 0, -0.894427191, 0, 0, 0.894427191,
				0 ],
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

		foa_a12_decoder_matrix =
		FoaEncoderMatrix.newDirections(spher).matrix.pseudoInverse;


		/////////// END code for 2nd order matrices /////////////////////

		prjDr = projDir;


		SynthDef(\blip, {
			var env = Env([0, 0.8, 1, 0], [0, 0.1, 0]);
			var blip = SinOsc.ar(1000) * EnvGen.kr(env, doneAction: 2);
			Out.ar(0, [blip, blip]);
		}).add;


		/// non ambisonc spatiaizers setup


		if (speaker_array.notNil) {

			var max_func, min_func, dimention, vbap_setup, adjust;

			nonambibus = outbus;

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

				azimuths = speaker_array.collect({ |val| val.pop });

				vbap_setup = VBAPSpeakerArray(dimention, azimuths.flat);
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

				azimuths = speaker_array.collect({ |val| val.pop });
			};

			vbap_buffer = Buffer.loadCollection(server, vbap_setup.getSetsAndMatrices);

			perfectSphereFunc = { |sig|
				sig = Array.fill(numoutputs, { |i| DelayN.ar(sig[i],
					delaytime:adjust[i][0], mul:adjust[i][1]) });
			};

		} {

			var emulate_array, vbap_setup;

			numoutputs = 26;

			nonambibus = Bus.audio(server, numoutputs);

			emulate_array = [ [ 0, 90 ], [ 0, 45 ], [ 90, 45 ], [ 180, 45 ], [ -90, 45 ],
				[ 45, 35 ], [ 135, 35 ], [ -135, 35 ], [ -45, 35 ], [ 0, 0 ], [ 45, 0 ],
				[ 90, 0 ], [ 135, 0 ], [ 180, 0 ], [ -135, 0 ], [ -90, 0 ], [ -45, 0 ],
				[ 45, -35 ], [ 135, -35 ], [ -135, -35 ], [ -45, -35 ], [ 0, -45 ],
				[ 90, -45 ], [ 180, -45 ], [ -90, -45 ], [ 0, -90 ] ];

			vbap_setup = VBAPSpeakerArray(3, emulate_array);
			// emulate 26-point Lebedev grid

			vbap_buffer = Buffer.loadCollection(server, vbap_setup.getSetsAndMatrices);

			longest_radius = 18;
			lowest_elevation = -90;
			highest_elevation = 90;

			perfectSphereFunc = { |sig|
				sig;
			};

			SynthDef.new("nonAmbi2FuMa", {
				var sig = In.ar(nonambibus, numoutputs);
				sig = FoaEncode.ar(sig,
					FoaEncoderMatrix.newDirections(emulate_array.degrad));
				Out.ar(fumabus, sig);
			}).add;

		};


		// define ambisonic decoder


		if (suboutbus.notNil) {
			subOutFunc = { |signal, sublevel = 1|
				var subOut = Mix.ar(signal) * sublevel * 0.5;
				Out.ar(suboutbus, subOut);
			};
		} {
			subOutFunc = { |signal, sublevel| };
		};


		if (decoder.isNil) {

			case
			{rawformat == \FuMa}
			{
				convert_fuma = false;
				convert_n3d = true;
				convert_direct = false;

				SynthDef.new("ambiConverter", { | gate = 1 |
					var n3dsig, env;
					env = EnvGen.kr(Env.asr(curve:\hold), gate, doneAction:2);
					n3dsig = In.ar(n3dbus, bFormNumChan);
					n3dsig = HOAConvert.ar(maxorder, n3dsig, \ACN_N3D, \FuMa) * env;
					Out.ar(fumabus, n3dsig);
				}).add;

				SynthDef.new("globDecodeSynth",  { | sub = 1, level = 1 |
					var sig, nonambi;
					sig = In.ar(fumabus, bFormNumChan) * level;
					nonambi = In.ar(nonambibus, numoutputs) * level;
					perfectSphereFunc.value(nonambi);
					subOutFunc.value(sig + nonambi, sub);
					Out.ar(rawoutbus, sig);
					Out.ar(outbus, nonambi);
				}).add;

			}
			{rawformat == \N3D}
			{
				convert_fuma = true;
				convert_n3d = false;
				convert_direct = true;

				SynthDef.new("ambiConverter", { | gate = 1 |
					var sig, env;
					env = EnvGen.kr(Env.asr(curve:\hold), gate, doneAction:2);
					sig = In.ar(fumabus, fourOrNine);
					sig = HOAConvert.ar(maxorder, sig, \FuMa, \ACN_N3D) * env;
					Out.ar(n3dbus, sig);
				}).add;

				SynthDef("globDecodeSynth", {
					| lf_hf=0, xover=400, sub = 1, level = 1 |
					var sig, nonambi;
					sig = In.ar(n3dbus, bFormNumChan) * level;
					nonambi = In.ar(nonambibus, numoutputs) * level;
					perfectSphereFunc.value(nonambi);
					subOutFunc.value(sig + nonambi, sub);
					Out.ar(rawoutbus, sig);
					Out.ar(outbus, nonambi);
				}).add;

			};

		} {

			case
			{ maxorder == 1 }
			{ convert_fuma = false;
				convert_n3d = true;
				convert_direct = false;

				SynthDef.new("ambiConverter", { | gate = 1 |
					var n3dsig, env;
					env = EnvGen.kr(Env.asr(curve:\hold), gate, doneAction:2);
					n3dsig = In.ar(n3dbus, 4);
					n3dsig = FoaEncode.ar(n3dsig, n2m) * env;
					Out.ar(fumabus, n3dsig);
				}).add;

				if (decoder == "internal") {

					if (elevations.isNil) {
						elevations = Array.fill(numoutputs, { 0 });
					};

					SynthDef.new("globDecodeSynth",  { | sub = 1, level = 1 |
						var sig, nonambi;
						sig = In.ar(fumabus, 4);
						sig = BFDecode1.ar1(sig[0], sig[1], sig[2], sig[3],
							speaker_array.collect(_.degrad), elevations.collect(_.degrad),
							longest_radius, radiusses);
						nonambi = In.ar(nonambibus, numoutputs);
						perfectSphereFunc.value(nonambi);
						sig = (sig + nonambi) * level;
						subOutFunc.value(sig, sub);
						Out.ar(outbus, sig);
					}).add;

				} {

					if (speaker_array.notNil) {
						SynthDef.new("globDecodeSynth",  { | sub = 1, level = 1 |
							var sig, nonambi;
							sig = In.ar(fumabus, 4);
							sig = FoaDecode.ar(sig, decoder);
							nonambi = In.ar(nonambibus, numoutputs);
							perfectSphereFunc.value(nonambi);
							sig = (sig + nonambi) * level;
							subOutFunc.value(sig, sub);
							Out.ar(outbus, sig);
						}).add;

					} {
						SynthDef.new("globDecodeSynth",  { | sub = 1, level = 1 |
							var sig, nonambi;
							sig = In.ar(fumabus, 4);
							sig = FoaDecode.ar(sig, decoder);
							sig = sig * level;
							subOutFunc.value(sig, sub);
							Out.ar(outbus, sig);
						}).add;
					}
				}
			}

			{ maxorder == 2 }
			{
				if (decoder == "internal") {

					convert_fuma = false;
					convert_n3d = true;
					convert_direct = false;

					if (elevations.isNil) {
						elevations = Array.fill(numoutputs, { 0 });
					};

					SynthDef.new("ambiConverter", { | gate = 1 |
						var n3dsig, env;
						env = EnvGen.kr(Env.asr(curve:\hold), gate, doneAction:2);
						n3dsig = In.ar(n3dbus, 9);
						n3dsig = HOAConvert.ar(2, n3dsig, \ACN_N3D, \FuMa) * env;
						Out.ar(fumabus, n3dsig);
					}).add;

					SynthDef.new("globDecodeSynth",  { | sub = 1, level = 1 |
						var sig, nonambi;
						sig = In.ar(fumabus, 9);
						sig = FMHDecode1.ar1(sig[0], sig[1], sig[2], sig[3], sig[4],
							sig[5], sig[6], sig[7], sig[8],
							azimuths.collect(_.degrad), elevations.collect(_.degrad),
							longest_radius, radiusses);
						nonambi = In.ar(nonambibus, numoutputs);
						perfectSphereFunc.value(nonambi);
						sig = (sig + nonambi) * level;
						subOutFunc.value(sig, sub);
						Out.ar(outbus, sig);
					}).add;

				} { // assume ADT Decoder
					convert_fuma = true;
					convert_n3d = false;
					convert_direct = true;

					SynthDef.new("ambiConverter", { | gate = 1 |
						var sig, env;
						env = EnvGen.kr(Env.asr(curve:\hold), gate, doneAction:2);
						sig = In.ar(fumabus, fourOrNine);
						sig = HOAConvert.ar(maxorder, sig, \FuMa, \ACN_N3D) * env;
						Out.ar(n3dbus, sig);
					}).add;

					SynthDef("globDecodeSynth", {
						| lf_hf=0, xover=400, sub = 1, level = 1 |
						var sig, nonambi;
						sig = In.ar(n3dbus, bFormNumChan);
						sig = decoder.ar(sig[0], sig[1], sig[2], sig[3], sig[4],
							sig[5], sig[6], sig[7], sig[8], 0, lf_hf, xover:xover);
						nonambi = In.ar(nonambibus, numoutputs);
						perfectSphereFunc.value(nonambi);
						sig = (sig + nonambi) * level;
						subOutFunc.value(sig, sub);
						Out.ar(outbus, sig);
					}).add;
				};
			}

			{ maxorder == 3 } // assume ADT Decoder
			{ convert_fuma = true;
				convert_n3d = false;
				convert_direct = true;

				SynthDef.new("ambiConverter", { | gate = 1 |
					var sig, env;
					env = EnvGen.kr(Env.asr(curve:\hold), gate, doneAction:2);
					sig = In.ar(fumabus, 9);
					sig = HOAConvert.ar(2, sig, \FuMa, \ACN_N3D) * env;
					Out.ar(n3dbus, sig);
				}).add;

				SynthDef("globDecodeSynth", {
					| lf_hf=0, xover=400, sub = 1, level = 1 |
					var sig, nonambi;
					sig = In.ar(n3dbus, bFormNumChan);
					sig = decoder.ar(sig[0], sig[1], sig[2], sig[3], sig[4],
						sig[5], sig[6], sig[7], sig[8], sig[9], sig[10], sig[11],
						sig[12], sig[13], sig[14], sig[15], 0, lf_hf, xover:xover);
					nonambi = In.ar(nonambibus, numoutputs);
					perfectSphereFunc.value(nonambi);
					sig = (sig + nonambi) * level;
					subOutFunc.value(sig, sub);
					Out.ar(outbus, sig);
				}).add;
			}

			{ maxorder == 4 } // assume ADT Decoder
			{ convert_fuma = true;
				convert_n3d = false;
				convert_direct = true;

				SynthDef.new("ambiConverter", { | gate = 1 |
					var sig, env;
					env = EnvGen.kr(Env.asr(curve:\hold), gate, doneAction:2);
					sig = In.ar(fumabus, 9);
					sig = HOAConvert.ar(2, sig, \FuMa, \ACN_N3D) * env;
					Out.ar(n3dbus, sig);
				}).add;

				SynthDef("globDecodeSynth", {
					| lf_hf=0, xover=400, sub = 1, level = 1 |
					var sig, nonambi;
					sig = In.ar(n3dbus, bFormNumChan);
					sig = decoder.ar(sig[0], sig[1], sig[2], sig[3], sig[4],
						sig[5], sig[6], sig[7], sig[8], sig[9], sig[10], sig[11],
						sig[12], sig[13], sig[14],sig[15], sig[16], sig[17], sig[18],
						sig[19], sig[20], sig[21], sig[22], sig[23], sig[24],
						0, lf_hf, xover:xover);
					nonambi = In.ar(nonambibus, numoutputs);
					perfectSphereFunc.value(nonambi);
					sig = (sig + nonambi) * level;
					subOutFunc.value(sig, sub);
					Out.ar(outbus, sig);
				}).add;
			}

			{ maxorder == 5 } // assume ADT Decoder
			{ convert_fuma = true;
				convert_n3d = false;
				convert_direct = true;

				SynthDef.new("ambiConverter", { | gate = 1 |
					var sig, env;
					env = EnvGen.kr(Env.asr(curve:\hold), gate, doneAction:2);
					sig = In.ar(fumabus, 9);
					sig = HOAConvert.ar(2, sig, \FuMa, \ACN_N3D) * env;
					Out.ar(n3dbus, sig);
				}).add;

				SynthDef("globDecodeSynth", {
					| lf_hf=0, xover=400, sub = 1, level = 1 |
					var sig, nonambi;
					sig = In.ar(n3dbus, bFormNumChan);
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
					sig = (sig + nonambi) * level;
					subOutFunc.value(sig, sub);
					Out.ar(outbus, sig);
				}).add;
			};
		};


		spatFuncs = Array.newClear(spatList.size);
		// contains the synthDef blocks for each spatialyers lib

		// Ambitools
		spatFuncs[0] = { |ref, input, radius, distance, azimuth, elevation, difu, spre,
			contract, win, rate, rand|
			ref.value = HOAEncoder.ar(maxorder,
				(ref.value + input), CircleRamp.kr(azimuth, 0.1, -pi, pi),
				Lag.kr(elevation), 0, 1, radius, longest_radius);
		};

		// HoaLib
		spatFuncs[1] = { |ref, input, radius, distance, azimuth, elevation, difu, spre,
			contract, win, rate, rand|
			var sig = LPF.ar(input, (1 - distance) * 18000 + 2000);
			// attenuate high freq with distance
			ref.value = HOALibEnc3D.ar(maxorder,
				(ref.value + sig) * (longest_radius / radius),
				CircleRamp.kr(azimuth, 0.1, -pi, pi), Lag.kr(elevation), 0);
		};

		// ADTB
		spatFuncs[2] = { |ref, input, radius, distance, azimuth, elevation, difu, spre,
			contract, win, rate, rand|
			var sig = LPF.ar(input, (1 - distance) * 18000 + 2000);
			// attenuate high freq with distance
			ref.value = HOAmbiPanner.ar(maxorder,
				(ref.value + sig) * (longest_radius / radius),
				CircleRamp.kr(azimuth, 0.1, -pi, pi), Lag.kr(elevation), 0);
		};

		// ATK
		spatFuncs[3] = { |ref, input, radius, distance, azimuth, elevation, difu, spre,
			contract, win, rate, rand|
			var diffuse, spread, omni,
			sig = LPF.ar(input, (1 - distance) * 18000 + 2000),
			// attenuate high freq with distance
			rad = longest_radius / radius;
			sig = (sig + ref.value) * rad;
			omni = FoaEncode.ar(sig, foaEncoderOmni);
			spread = FoaEncode.ar(sig, foaEncoderSpread);
			diffuse = FoaEncode.ar(sig, foaEncoderDiffuse);
			sig = Select.ar(difu, [omni, diffuse]);
			sig = Select.ar(spre, [sig, spread]);
			sig = FoaTransform.ar(sig, 'push', halfPi * contract, azimuth, elevation);
			sig = HPF.ar(sig, 20); // stops bass frequency blow outs by proximity
			ref.value = FoaTransform.ar(sig, 'proximity', rad);
		};

		// BF-FMH
		spatFuncs[4] = { |ref, input, radius, distance, azimuth, elevation, difu, spre,
			contract, win, rate, rand|
			var sig = LPF.ar(input, (1 - distance) * 18000 + 2000);
			// attenuate high freq with distance
			ref.value = bfOrFmh.ar(ref.value + sig, azimuth, elevation,
				longest_radius / radius, 0.5);
		};

		// joshGrain
		spatFuncs[5] = { |ref, input, radius, distance, azimuth, elevation, difu, spre,
			contract, win, rate, rand|
			var sig = LPF.ar(input, (1 - distance) * 18000 + 2000);
			// attenuate high freq with distance
			ref.value = MonoGrainBF.ar(ref.value + sig, win, rate, rand,
				azimuth, 1 - contract,
				elevation, 1 - contract, rho: longest_radius / radius,
				mul: ((0.5 - win) + (1 - (rate / 40))).clip(0, 1) * 0.5 );
		};

		// VBAP
		spatFuncs[6] = { |ref, input, radius, distance, azimuth, elevation, difu, spre,
			contract, win, rate, rand|
			var sig = LPF.ar(input, (1 - distance) * 18000 + 2000),
			// attenuate high freq with distance
			azi = azimuth * rad2deg, // convert to degrees
			elev = elevation * rad2deg, // convert to degrees
			elevexcess = Select.kr(elev < lowest_elevation, [0, elev.abs]);
			elevexcess = Select.kr(elev > highest_elevation, [0, elev]);
			// get elevation overshoot
			elev = elev.clip(lowest_elevation, highest_elevation);
			// restrict between min & max

			ref.value = VBAP.ar(numoutputs,
				(ref.value + sig) * (longest_radius / radius),
				vbap_buffer.bufnum, CircleRamp.kr(azi, 0.1, -180, 180), Lag.kr(elevation),
				((1 - contract) + (elevexcess / 90)) * 100) * 0.5;
		};

		outPutFuncs = Array.newClear(3);
		// contains the synthDef blocks for each spatialyers

		outPutFuncs[0] = { |dry, wet, globrev|
			Out.ar(gbixfbus, wet * globrev);
			Out.ar(n3dbus, wet);
		};

		outPutFuncs[1] = { |dry, wet, globrev|
			Out.ar(gbfbus, wet * globrev);
			Out.ar(fumabus, wet);
		};

		outPutFuncs[2] = { |dry, wet, globrev|
			Out.ar(gbus, dry * globrev);
			Out.ar(nonambibus, wet);
		};


		makeSpatialisers = { | rev_type |
			var out_type = 0;

			spatList.do{ |item, i|

				case
				{ i <= lastN3D } { out_type = 0 }
				{ (i > lastN3D) && (i <= lastFUMA) } { out_type = 1 }
				{ i > lastFUMA } { out_type = 2 };

				SynthDef.new(item++"Chowning"++rev_type, {
					| inbus, azim = 0, elev = 0, radius = 200,
					dopamnt = 0, glev = 0, llev = 0,
					insertFlag = 0, insertOut, insertBack,
					room = 0.5, damp = 05, wir, df, sp,
					contr = 1, grainrate = 10, winsize = 0.1, winrand = 0 |

					var rad = Lag.kr(radius),
					dis = rad * 0.01,
					globallev = (1 / dis.sqrt) - 1, //global reverberation
					locallev, lrevRef = Ref(0),
					az = azim - halfPi,
					p = In.ar(inbus, 1),
					rd = dis * 340, // Doppler
					cut = ((1 - dis) * 2).clip(0, 1);
					//make shure level is 0 when radius reaches 100
					rad = rad.clip(1, 50);

					p = DelayC.ar(p, 0.2, rd/1640.0 * dopamnt);

					localReverbFunc.value(lrevRef, p, wir, dis * llev,
						// local reverberation
						room, damp);

					spatFuncs[i].value(lrevRef, p, rad, dis, az, elev, df, sp, contr,
						winsize, grainrate, winrand);

					outPutFuncs[out_type].value(p * cut, lrevRef.value * cut,
						globallev.clip(0, 1) * glev);
				}).add;


				SynthDef.new(item++"StereoChowning"++rev_type, {
					| inbus, azim = 0, elev = 0, radius = 0,
					dopamnt = 0, glev = 0, llev = 0, angle = 1.05,
					insertFlag = 0, insertOut, insertBack,
					room = 0.5, damp = 05, wir, df, sp,
					contr = 1, grainrate = 10, winsize = 0.1, winrand = 0 |

					var rad = Lag.kr(radius),
					dis = rad * 0.01,
					globallev = (1 / dis.sqrt) - 1, //global reverberation
					lrev1Ref = Ref(0), lrev2Ref = Ref(0),
					az = Lag.kr(azim - halfPi),
					p = In.ar(inbus, 2),
					rd = dis * 340, // Doppler
					cut = ((1 - dis) * 2).clip(0, 1);
					//make shure level is 0 when radius reaches 100
					rad = rad.clip(1, 50);

					p = DelayC.ar(p, 0.2, rd/1640.0 * dopamnt);

					localReverbStereoFunc.value(lrev1Ref, lrev2Ref, p[0], p[1],
						wir, dis * llev, room, damp);

					spatFuncs[i].value(lrev1Ref, p[0], rad, dis, az - (angle * (1 - dis)),
						elev, df, sp, contr, winsize, grainrate, winrand);
					spatFuncs[i].value(lrev2Ref, p[1], rad, dis, az + (angle * (1 - dis)),
						elev, df, sp, contr, winsize, grainrate, winrand);

					outPutFuncs[out_type].value(Mix.new(p) * 0.5 * cut,
						(lrev1Ref.value + lrev2Ref.value) * 0.5 * cut,
						globallev.clip(0, 1) * glev);
				}).add;
			};


			SynthDef.new("ATK2Chowning"++rev_type, {
				| inbus, radius = 200,
				dopamnt = 0, glev = 0, llev = 0,
				insertFlag = 0, insertOut, insertBack,
				room = 0.5, damp = 05, wir|

				var rad = Lag.kr(radius),
				dis = rad * 0.01,
				globallev = (1 / dis.sqrt) - 1, //global reverberation
				lrevRef = Ref(0),
				p = In.ar(inbus, 1),
				rd = radius * 340, // Doppler
				cut = ((1 - dis) * 2).clip(0, 1);
				//make shure level is 0 when radius reaches plim
				rad = rad.clip(1, 50);

				p = DelayC.ar(p, 0.2, rd/1640.0 * dopamnt);

				localReverbFunc.value(lrevRef, p, wir, dis * llev, room, damp);
				p = HPF.ar(p, 20); // stops bass frequency blow outs by proximity
				p = FoaTransform.ar(p + lrevRef.value, 'proximity',
					rad * 50);

				outPutFuncs[out_type].value(p * cut, lrevRef.value * cut,
					globallev.clip(0, 1) * glev);
			}).add;

		}; //end makeSpatialisers

		// allpass reverbs
		if (maxorder == 1) {

			SynthDef.new("revGlobalAmb_pass", { | gate = 1, room = 0.5, damp = 0.5 |
				var env, temp, convsig, sig, sigx, sigf = In.ar(gbfbus, 4);
				sigx = In.ar(gbixfbus, 4);
				sig = In.ar(gbus, 1);
				sigx = FoaEncode.ar(sigx, n2m);
				env = EnvGen.kr(Env.asr(1), gate, doneAction:2);
				sigf = FoaDecode.ar(sigf, b2a);
				sig = sig + sigf + sigx;
				16.do({ sig = AllpassC.ar(sig, 0.08, room * { Rand(0, 0.08) }.dup(4) +
					{ Rand(0, 0.001) },
					damp * 2)});
				sig = FoaEncode.ar(sig, a2b);
				sig = sig * env;
				Out.ar(fumabus, sig);
			}).add;

		} {

			SynthDef.new("revGlobalAmb_pass", { | gate = 1, room = 0.5, damp = 0.5 |
				var env, w, x, y, z, r, s, t, u, v,
				soaSig, tmpsig, sig, sigx, sigf = In.ar(gbfbus, 9);
				sigx = In.ar(gbixfbus, 9);
				sigx = HOAConvert.ar(2, sigx, \FuMa, \ACN_N3D);
				sig = In.ar(gbus, 1);
				env = EnvGen.kr(Env.asr(1), gate, doneAction:2);
				sig = sig + sigf + sigx;
				sig = AtkMatrixMix.ar(sig, soa_a12_decoder_matrix);
				16.do({ sig = AllpassC.ar(sig, 0.08, room * { Rand(0, 0.08) }.dup(12) +
					{ Rand(0, 0.001) },
					damp * 2)});
				#w, x, y, z, r, s, t, u, v = AtkMatrixMix.ar(sig, soa_a12_encoder_matrix)
				* env;
				soaSig = [w, x, y, z, r, s, t, u, v];
				Out.ar(fumabus, soaSig);
			}).load(server);
		};

		//run the makeSpatialisers function for each types of local reverbs

		localReverbFunc = { | lrevRef, p, rirWspectrum, locallev, room, damp |
			var temp = p;
			16.do({ temp = AllpassC.ar(temp, 0.08, room * { Rand(0, 0.08) } +
				{ Rand(0, 0.001) },
				damp * 2)});
			lrevRef.value = temp * locallev;
		};

		localReverbStereoFunc = { | lrev1Ref, lrev2Ref, p1, p2, rirZspectrum,
			locallev, room, damp |
			var temp1 = p1, temp2 = p2;
			8.do({ temp1 = AllpassC.ar(temp1, 0.08, room * { Rand(0, 0.08) } +
				{ Rand(0, 0.001) },
				damp * 2)});
			8.do({ temp2 = AllpassC.ar(temp2, 0.08, room * { Rand(0, 0.08) } +
				{ Rand(0, 0.001) },
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
				sigx = FoaEncode.ar(sigx, n2m);
				env = EnvGen.kr(Env.asr(1), gate, doneAction:2);
				sigf = FoaDecode.ar(sigf, b2a);
				sig = sig + sigf + sigx;
				convsig = [
					FreeVerb2.ar(sig[0], sig[1], mix: 1, room: room, damp: damp),
					FreeVerb2.ar(sig[2], sig[3], mix: 1, room: room, damp: damp)];
				convsig = FoaEncode.ar(convsig.flat, a2b);
				convsig = convsig * env;
				Out.ar(fumabus, convsig);
			}).add;

		} {

			SynthDef.new("revGlobalAmb_free",  { | gate = 1, room = 0.5, damp = 0.5 |
				var env, w, x, y, z, r, s, t, u, v,
				soaSig, tmpsig, sig, sigx, sigf = In.ar(gbfbus, 9);
				sigx = In.ar(gbixfbus, 9);
				sigx = HOAConvert.ar(2, sigx, \FuMa, \ACN_N3D);
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
				#w, x, y, z, r, s, t, u, v = AtkMatrixMix.ar(tmpsig,
					soa_a12_encoder_matrix);
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

		localReverbStereoFunc = { | lrev1Ref, lrev2Ref, p1, p2, rirZspectrum,
			locallev, room, damp |
		};

		makeSpatialisers.value(rev_type:"");

		rirList = Array.newClear();

		server.sync;

		if (rirBank.notNil) {

			/////////// START loading rirBank /////////////////////

			var rirName, rirW, rirX, rirY, rirZ, bufWXYZ, rirFLU, rirFRD, rirBLD, rirBRU,
			bufAformat, bufAformat_soa_a12, rirA12, bufsize,
			// prepare list of impulse responses for close and distant reverb
			// selection menue

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

					rirW = rirW ++ [ Buffer.readChannel(server, item.fullPath,
						channels: [0]) ];
					rirX = rirX ++ [ Buffer.readChannel(server, item.fullPath,
						channels: [1]) ];
					rirY = rirY ++ [ Buffer.readChannel(server, item.fullPath,
						channels: [2]) ];
					rirZ = rirZ ++ [ Buffer.readChannel(server, item.fullPath,
						channels: [3]) ];

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

				bufAformat[count] = Buffer.alloc(server, bufWXYZ[count].numFrames,
					bufWXYZ[count].numChannels);
				bufAformat_soa_a12[count] = Buffer.alloc(server,
					bufWXYZ[count].numFrames, 12);
				// for second order conv

				if (File.exists(rirBank ++ "/" ++ item ++ "_Flu.wav").not) {

					("writing " ++ item ++ "_Flu.wav file in" ++ rirBank).postln;

					{BufWr.ar(FoaDecode.ar(PlayBuf.ar(4, bufWXYZ[count],
						loop: 0, doneAction: 2), b2a),
						bufAformat[count], Phasor.ar(0,
						BufRateScale.kr(bufAformat[count]),
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

					{BufWr.ar(AtkMatrixMix.ar(PlayBuf.ar(4, bufWXYZ[count],
						loop: 0, doneAction: 2),
						foa_a12_decoder_matrix),
					bufAformat_soa_a12[count],
					Phasor.ar(0, BufRateScale.kr(bufAformat[count]), 0,
						BufFrames.kr(bufAformat[count])));
					Out.ar(0, Silent.ar);
					}.play;

					(bufAformat[count].numFrames / server.sampleRate).wait;

					bufAformat_soa_a12[count].write(
						rirBank ++ "/" ++ item ++ "_SoaA12.wav",
						headerFormat: "wav", sampleFormat: "int24");

					"done".postln;

				};

			});

			"Loading rir bank".postln;

			rirList.do({ |item, count|

				server.sync;

				bufAformat[count].free;
				bufWXYZ[count].free;

				rirFLU[count] = Buffer.readChannel(server,
					rirBank ++ "/" ++ item ++ "_Flu.wav",
					channels: [0]);
				rirFRD[count] = Buffer.readChannel(server,
					rirBank ++ "/" ++ item ++ "_Flu.wav",
					channels: [1]);
				rirBLD[count] = Buffer.readChannel(server,
					rirBank ++ "/" ++ item ++ "_Flu.wav",
					channels: [2]);
				rirBRU[count] = Buffer.readChannel(server,
					rirBank ++ "/" ++ item ++ "_Flu.wav",
					channels: [3]);

				rirWspectrum[count] = Buffer.alloc(server, bufsize[count], 1);
				rirXspectrum[count] = Buffer.alloc(server, bufsize[count], 1);
				rirYspectrum[count] = Buffer.alloc(server, bufsize[count], 1);
				rirZspectrum[count] = Buffer.alloc(server, bufsize[count], 1);
				rirWspectrum[count].preparePartConv(rirW[count], fftsize);
				rirW[count].free;
				// don't need time domain data anymore, just needed spectral version
				rirXspectrum[count].preparePartConv(rirX[count], fftsize);
				rirX[count].free;
				rirYspectrum[count].preparePartConv(rirY[count], fftsize);
				rirY[count].free;
				rirZspectrum[count].preparePartConv(rirZ[count], fftsize);
				rirZ[count].free;

				rirFLUspectrum[count] = Buffer.alloc(server, bufsize[count], 1);
				rirFRDspectrum[count] = Buffer.alloc(server, bufsize[count], 1);
				rirBLDspectrum[count] = Buffer.alloc(server, bufsize[count], 1);
				rirBRUspectrum[count] = Buffer.alloc(server, bufsize[count], 1);
				rirFLUspectrum[count].preparePartConv(rirFLU[count], fftsize);
				rirFLU[count].free;
				rirFRDspectrum[count].preparePartConv(rirFRD[count], fftsize);
				rirFRD[count].free;
				rirBLDspectrum[count].preparePartConv(rirBLD[count], fftsize);
				rirBLD[count].free;
				rirBRUspectrum[count].preparePartConv(rirBRU[count], fftsize);
				rirBRU[count].free;

				/////////// END loading rirBank /////////////////////
			});

			if (maxorder == 1) {

				SynthDef.new("revGlobalAmb_conv",  { | gate = 1,
					fluir, frdir, bldir, bruir |
					var env, temp, convsig, sig, sigx, sigf = In.ar(gbfbus, 4);
					sigx = In.ar(gbixfbus, 4);
					sig = In.ar(gbus, 1);
					sigx = FoaEncode.ar(sigx, n2m);
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
					Out.ar(fumabus, convsig);
				}).add;

			} {

				rirList.do({ |item, count|

					rirA12 = Array.newClear(12);
					rirA12Spectrum = Array2D(rirNum, 12);
					12.do { | i |
						rirA12[i] = Buffer.readChannel(server,
							rirBank ++ "/" ++ item ++ "_SoaA12.wav",
							channels: [i]);
						rirA12Spectrum[count, i] = Buffer.alloc(server,
							bufsize[count], 1);
						rirA12Spectrum[count, i].preparePartConv(rirA12[i], fftsize);
						rirA12[i].free;
					};

				});

				SynthDef.new("revGlobalAmb_conv",  { | gate = 1, a0ir, a1ir, a2ir, a3ir,
					a4ir, a5ir, a6ir, a7ir, a8ir, a9ir, a10ir, a11ir |
					var env, w, x, y, z, r, s, t, u, v,
					soaSig, tmpsig, sig, sigx, sigf = In.ar(gbfbus, 9);
					sigx = In.ar(gbixfbus, 9);
					sigx = HOAConvert.ar(2, sigx, \FuMa, \ACN_N3D);
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
					#w, x, y, z, r, s, t, u, v = AtkMatrixMix.ar(tmpsig,
						soa_a12_encoder_matrix);
					soaSig = [w, x, y, z, r, s, t, u, v];
					Out.ar(fumabus, soaSig);
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

			if (maxorder == 1) {
				irSpecPar = { |i|
					[\fluir, rirFLUspectrum[i],
						\frdir, rirFRDspectrum[i],
						\bldir, rirBLDspectrum[i],
						\bruir, rirBRUspectrum[i]]
				};
			} {
				irSpecPar = { |i|
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

		};

		// Lauch GUI
		if(guiflag) {
			this.gui;
		};


		makeSynthDefPlayers = { | type, i = 0 |
			// 3 types : File, HWBus and SWBus - i duplicates with 0, 1 & 2

			SynthDef.new("playMono"++type, { | outbus, bufnum = 0, rate = 1,
				level = 1, tpos = 0, lp = 0, busini |
				var playerRef = Ref(0);
				playInFunc[i].value(playerRef, busini, bufnum, tpos, lp, rate, 1);
				Out.ar(outbus, playerRef.value * level);
			}).add;

			SynthDef.new("playStereo"++type, { | outbus, bufnum = 0, rate = 1,
				level = 1, tpos = 0, lp = 0, busini |
				var playerRef = Ref(0);
				playInFunc[i].value(playerRef, busini, bufnum, tpos, lp, rate, 2);
				Out.ar(outbus, playerRef.value * level);
			}).add;


			SynthDef.new("playBFormatATK"++type++"_4", {
				| bufnum = 0, rate = 1, level = 1, tpos = 0, lp = 0,
				rotAngle = 0, azim = 0, elev = 0, radius = 200,
				glev, llev, directang = 0, contr, dopamnt, busini,
				insertFlag = 0, insertOut, insertBack |

				var playerRef = Ref(0),
				pushang, az, ele, globallev,
				rd, dis = radius.clip(0.01, 1);

				az = azim - halfPi;
				pushang = dis * halfPi; // degree of sound field displacement

				playInFunc[i].value(playerRef, busini, bufnum, tpos, lp, rate, 4);
				playerRef.value = LPF.ar(playerRef.value, (1 - dis) * 18000 + 2000);
				// attenuate high freq with distance
				rd = Lag.kr(dis * 340); 				 // Doppler
				playerRef.value = DelayC.ar(playerRef.value, 0.2, rd/1640.0 * dopamnt);

				playerRef.value = FoaDirectO.ar(playerRef.value, directang);
				// directivity
				playerRef.value = FoaTransform.ar(playerRef.value, 'rotate', rotAngle);
				playerRef.value = FoaTransform.ar(playerRef.value, 'push',
					pushang, az, ele);

				globallev = (1 / radius.sqrt) - 1; // lower tail of curve to zero
				outPutFuncs[1].value(nil, playerRef.value, globallev);
			}).add;


			SynthDef.new("playBFormatAmbitools"++type++"_4", {
				| outbus, bufnum = 0, rate = 1,
				level = 1, tpos = 0, lp = 0, rotAngle = 0,
				azim = 0, elev = 0, radius = 0,
				glev, llev, directang = 0, contr, dopamnt,
				busini, insertFlag = 0 |

				var playerRef, wsinal, pushang = 0,
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
				playInFunc[i].value(playerRef, busini, bufnum, tpos, lp, rate, 4);

				rd = Lag.kr(dis * 340);
				playerRef.value = DelayC.ar(playerRef.value, 0.2, rd/1640.0 * dopamnt);

				wsinal = playerRef.value[0] * contr * level * dis * 2.0;

				//Out.ar(outbus, wsinal);

				// global reverb
				globallev = 1 / dis.sqrt;
				/*intens = globallev - 1;
				intens = intens.clip(0, 4);
				intens = intens * 0.25;*/

				playerRef.value = FoaDecode.ar(playerRef.value,
					FoaDecoderMatrix.newAmbix1);
				playerRef.value = HOATransRotateAz.ar(1, playerRef.value, rotAngle);
				playerRef.value = HOABeamDirac2Hoa.ar(1, playerRef.value, 1, az, ele,
					focus:contr * dis.sqrt) * (1 - dis.squared) * level;

				Out.ar(n3dbus, playerRef.value);

				globallev = globallev - 1.0; // lower tail of curve to zero
				globallev = globallev.clip(0, 1);
				globallev = globallev * glev * 6;

				gsig = playerRef.value[0] * globallev;

				//locallev = dis  * llev * 5;
				//lsig = playerRef.value[0] * locallev;

				//gsig = (playerRef.value * globallev) + (playerRef.value * locallev);
				// b-format
				Out.ar(gbixfbus, gsig);
			}).add;


			[9, 16, 25, 36].do { |item, count|

				SynthDef.new("playBFormatATK"++type++"_"++item, {
					| outbus, bufnum = 0, rate = 1,
					level = 1, tpos = 0, lp = 0, rotAngle = 0,
					azim = 0, elev = 0, radius = 0,
					glev, llev, directang = 0, contr, dopamnt,
					busini, insertFlag = 0, aFormatBusOutFoa, aFormatBusInFoa,
					aFormatBusOutSoa, aFormatBusInSoa |

					var playerRef, wsinal, pushang = 0,
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
					playInFunc[i].value(playerRef, busini, bufnum, tpos, lp, rate, item);

					rd = Lag.kr(dis * 340);
					playerRef.value = DelayC.ar(playerRef.value, 0.2,
						rd/1640.0 * dopamnt);

					wsinal = playerRef.value[0] * contr * Lag.kr(level) * dis * 2.0;

					Out.ar(outbus, wsinal);

					// global reverb
					globallev = 1 / dis.sqrt;
					intens = globallev - 1;
					intens = intens.clip(0, 4);
					intens = intens * 0.25;

					playerRef.value = FoaEncode.ar(playerRef.value,
						n2m);
					playerRef.value = FoaDirectO.ar(playerRef.value, directang);
					// directivity

					playerRef.value = FoaTransform.ar(playerRef.value, 'rotate', rotAngle,
						Lag.kr(level) * intens * (1 - contr));

					playerRef.value = FoaTransform.ar(playerRef.value, 'push', pushang,
						az, ele);

					// convert to A-format and send to a-format out busses
					aFormatFoa = FoaDecode.ar(playerRef.value, b2a);
					Out.ar(aFormatBusOutFoa, aFormatFoa);
					// aFormatSoa = AtkMatrixMix.ar(ambSigSoa, soa_a12_decoder_matrix);
					// Out.ar(aFormatBusOutSoa, aFormatSoa);

					// flag switchable selector of a-format signal (from insert or not)
					aFormatFoa = Select.ar(insertFlag, [aFormatFoa,
						InFeedback.ar(aFormatBusInFoa, 4)]);
					//aFormatSoa = Select.ar(insertFlag, [aFormatSoa,
					//InFeedback.ar(aFormatBusInSoa, 12)]);

					// convert back to b-format
					ambSigFoaProcessed = FoaEncode.ar(aFormatFoa, a2b);
					//ambSigSoaProcessed = AtkMatrixMix.ar(aFormatSoa,
					//soa_a12_encoder_matrix);

					// not sure if the b2a/a2b process degrades signal.
					// Just in case it does:
					playerRef.value = Select.ar(insertFlag, [playerRef.value,
						ambSigFoaProcessed]);
					//ambSigSoa = Select.ar(insertFlag, [ambSigSoa, ambSigSoaProcessed]);

					Out.ar(fumabus, playerRef.value);

					globallev = globallev - 1.0; // lower tail of curve to zero
					globallev = globallev.clip(0, 1);
					globallev = globallev * glev * 6;
					gsig = playerRef.value[0] * globallev;

					locallev = dis * llev * 5;
					lsig = playerRef.value[0] * locallev;

					gsig = (playerRef.value * globallev) + (playerRef.value * locallev);
					// b-format
					Out.ar(gbfbus, gsig);
				}).add;


				SynthDef.new("playBFormatAmbitools"++type++"_"++item, {
					| outbus, bufnum = 0, rate = 1,
					level = 1, tpos = 0, lp = 0, rotAngle = 0,
					azim = 0, elev = 0, radius = 0,
					glev, llev, directang = 0, contr, dopamnt,
					busini, insertFlag = 0 |

					var playerRef, wsinal, pushang = 0,
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
					playInFunc[i].value(playerRef, busini, bufnum, tpos, lp, rate, item);

					rd = Lag.kr(dis * 340);
					playerRef.value = DelayC.ar(playerRef.value, 0.2,
						rd/1640.0 * dopamnt);

					wsinal = playerRef.value[0] * contr * level * dis * 2.0;

					Out.ar(outbus, wsinal);

					// global reverb
					globallev = 1 / dis.sqrt;
					/*intens = globallev - 1;
					intens = intens.clip(0, 4);
					intens = intens * 0.25;*/

					playerRef.value = HOATransRotateAz.ar(count + 2, playerRef.value,
						rotAngle);
					playerRef.value = HOABeamDirac2Hoa.ar(count + 2, playerRef.value, 1,
						az, ele,
						focus:contr * dis.sqrt) * (1 - dis.squared) * level;

					Out.ar(n3dbus, playerRef.value);

					globallev = globallev - 1.0; // lower tail of curve to zero
					globallev = globallev.clip(0, 1);
					globallev = globallev * glev * 6;
					gsig = playerRef.value[0] * globallev;

					Out.ar(gbixfbus, gsig);
				}).add;
			};

		}; //end makeSynthDefPlayers

		// Make File-in SynthDefs

		playInFunc[0] = { | playerRef, busini, bufnum, tpos, lp = 0, rate, channum |
			// Note it needs all the variables
			var spos = tpos * BufSampleRate.kr(bufnum),
			scaledRate = rate * BufRateScale.kr(bufnum);
			playerRef.value = PlayBuf.ar(channum, bufnum, scaledRate, startPos: spos,
				loop: lp, doneAction:2);
		};

		makeSynthDefPlayers.("File", 0);

		// Make HWBus-in SynthDefs

		playInFunc[1] = { | playerRef, busini, bufnum, tpos, lp = 0, rate, channum |
			playerRef.value =  In.ar(busini + server.inputBus.index, channum);
		};

		makeSynthDefPlayers.("HWBus", 1);

		// Make SCBus-in SynthDefs

		playInFunc[2] = { | playerRef, busini, bufnum, tpos, lp = 0, rate, channum |
			playerRef.value =  In.ar(busini, channum);
		};

		makeSynthDefPlayers.("SWBus", 2);

		playInFunc[3] = { | playerRef, busini, bufnum, tpos, lp = 0, rate, channum |
			// Note it needs all the variables
			var trig;
			playerRef.value = DiskIn.ar(channum, bufnum, lp);
			trig = Done.kr(playerRef.value);
			FreeSelf.kr(trig);
		};

		makeSynthDefPlayers.("Stream", 3);


		//////// END SYNTHDEFS ///////////////


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
				this.setSynths(source, \azim, spheval[source].theta);
				this.setSynths(source, \elev, spheval[source].phi);
				this.setSynths(source, \radius, spheval[source].rho);

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

				this.setSynths(source, \a1check, a1but[source]);
				this.setSynths(source, \a2check, a2but[source]);
				this.setSynths(source, \a3check, a3but[source]);
				this.setSynths(source, \a4check, a4but[source]);
				this.setSynths(source, \a5check, a5but[source]);
			}.fork;
		};

		atualizarvariaveis = {

			nfontes.do { | i |
				//	updateSynthInArgs.value(i);

				if(espacializador[i] != nil) {
					espacializador[i].set(
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
						\azim, spheval[i].theta,
						\elev, spheval[i].phi,
						\radius, spheval[i].rho,
						//\xoffset, xoffset[i],
						//\yoffset, yoffset[i],
						\sp, sp[i],
						\df, df[i],
						\grainrate, grainrate[i];
						\winsize, winsize[i];
						\winrand, winrand[i];
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
						\rm, rm[i],
						\dm, dm[i],
						//\mx, xbox[i].value,
						//\my, ybox[i].value,

						// ERROR HERE?
						//						\mz, zbox[i].value,
						\azim, spheval[i].theta,
						\elev, spheval[i].phi,
						\radius, spheval[i].rho,
						//\xoffset, xoffset[i],
						//\yoffset, yoffset[i],
						//\mz, zval[i].value,
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
			if(espacializador[source] != nil) {
				espacializador[source].set(
					//	\mx, num.value  ???
					\angle, angle[source],
					\level, level[source], // ? or in player?
					\dopamnt, dplev[source],
					\glev, glev[source],
					\llev, llev[source],
					\rm, rm[source],
					\dm, dm[source],
					\azim, spheval[source].theta,
					\elev, spheval[source].phi,
					\radius, spheval[source].rho,
					\sp, sp[source],
					\df, df[source],
					\grainrate, grainrate[source];
					\winsize, winsize[source];
					\winrand, winrand[source];
				);
			};
			if(synt[source] != nil) {
				synt[source].set(
					\level, level[source],
					\rotAngle, rlev[source],
					\directang, dlev[source],
					\contr, clev[source],
					\dopamnt, dplev[source],
					\glev, glev[source],
					\llev, llev[source],
					\rm, rm[source],
					\dm, dm[source],
					\azim, spheval[source].theta,
					\elev, spheval[source].phi,
					\radius, spheval[source].rho,
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

				nfontes.do({
					| i |
					{
						//("scn = " ++ scn[i]).postln;
						if ((tfieldProxy[i].value != "") ||
							((scn[i] > 0) && (ncan[i] > 0))
							|| (hwncheckProxy[i].value && (ncan[i] > 0)) ) {
							//var source = Point.new;
							// should use cartesian but it's giving problems
							//source.set(xval[i] + xoffset[i],
							//yval[i] + yoffset[i]);
							//source.set(cartval[i].x, cartval[i].y);
							//("audit = " ++ audit[i]).postln;
							//("distance " ++ i ++ " = " ++ source.rho).postln;
							if (cartval[i].rho > plim) {
								firstTime[i] = true;
								if(synt[i].isPlaying) {
									//synthRegistry[i].free;
									runStop.value(i); // to kill SC input synths
									espacializador[i].free; // just in case...
									synt[i].free;
									synt[i] = nil;
									espacializador[i] = nil;
								};
							} {
								if(synt[i].isPlaying.not && (isPlay || audit[i])
									&& (firstTime[i]
										|| (tfieldProxy[i].value == ""))) {
									//this.triggerFunc[i].value; // play SC input synth
									firstTime[i] = false;
									runTrigger.value(i);

									if(lp[i] == 0) {

										//tocar.value(i, 1, force: true);
										this.newtocar(i, 0, force: true);
									} {
										// could remake this a random start point
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

					if (control.now > dur) {
						if (autoloopval) {
							control.seek; // note, onSeek not called
						} {
							this.blindControlStop; // stop everything
						};
					};
				};

				if (isPlay) {
					ossiaseekback = false;
					ossiatransport.v_(control.now);
					ossiaseekback = true;
				};
			});
		});


		watcher.play;

		///////////////

		if (serport.notNil) {
			//troutine = this.trackerRoutine; // start parsing of serial head tracker data
			//	kroutine = this.serialKeepItUp;
			troutine.play;
			kroutine.play;
		};

		/// OSSIA bindings

		this.ossia(parentOssiaNode, allCrtitical);

		//// LAUNCH INITIAL SYNTH

		globDec = Synth.new(\globDecodeSynth,
			target:glbRevDecGrp,addAction: \addToTail);

	} // end initMosca

	auditionFunc { |source, bool|
		if(isPlay.not) {
			if(bool) {
				firstTime[source] = true;
				//runTrigger.value(currentsource); - watcher does this now
				//tocar.value(currentsource, 0); // needed only by SC input
				//- and probably by HW - causes duplicates with file
				// as file playback is handled by the "watcher" routine
				audit[source] = true;
			} {
				runStop.value(source);
				synt[source].free;
				synt[source] = nil;
				audit[source] = false;
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
		h = h + headingOffset;
		if (h < -pi) {
			h = pi + (pi + h);
		};
		if (h > pi) {
			h = -pi - (pi - h);
		};

		r = (roll / 100) - pi;
		p = (pitch / 100) - pi;

		/////////// REVISE ////
		p = p.neg;
		r = r.neg;
		h = h.neg;
		///////////////////////

		pitchnumboxProxy.valueAction = p;
		rollnumboxProxy.valueAction = r;
		headingnumboxProxy.valueAction = h;
		nfontes.do { | i |

			if(espacializador[i].notNil) {

				espacializador[i].set(\azim, spheval[i].theta, \elev,
					spheval[i].phi,
					\radius, spheval[i].rho);
				this.setSynths(i, \azim, spheval[i].theta,
					\elev, spheval[i].phi,
					\radius, spheval[i].rho);
				synt[i].set(\azim, spheval[i].theta, \elev, spheval[i].phi,
					\radius, spheval[i].rho);
			};

		};

	}

	matchTByte { |byte|  // match incoming headtracker data

		if(trackarr[tracki].isNil or:{trackarr[tracki]==byte}, {
			trackarr2[tracki]= byte;
			tracki= tracki+1;
			if(tracki>=trackarr.size, {
				//				this.procTracker(trackarr2[4]<<8+trackarr2[5],
				//				trackarr2[6]<<8+trackarr2[7],
				//              trackarr2[8]<<8+trackarr2[9],
				if(hdtrk){
					this.procTracker(
						(trackarr2[5]<<8)+trackarr2[4],
						(trackarr2[7]<<8)+trackarr2[6],
						(trackarr2[9]<<8)+trackarr2[8]
						//,
						//(trackarr2[13]<<24) + (trackarr2[12]<<16) +
						//(trackarr2[11]<<8)
						//+ trackarr2[10],
						//(trackarr2[17]<<24) + (trackarr2[16]<<16) +
						//(trackarr2[15]<<8)
						//+ trackarr2[14]
					);
				};
				tracki= 0;
			});
		}, {
			tracki= 0;
		});
	}



	trackerRoutine { Routine.new
		( {
			inf.do{
				//trackPort.read.postln;
				this.matchTByte(trackPort.read);
			};
		})
	}

	serialKeepItUp {Routine.new({
		inf.do{
			if (trackPort.isOpen.not) // if serial port is closed
			{
				"Trying to reopen serial port!".postln;
				if (SerialPort.devices.includesEqual(serport))
				// and if device is actually connected
				{
					"Device connected! Opening port!".postln;
					trackPort = SerialPort(serport, 115200, crtscts: true);
					this.trackerRoutine; // start tracker routine again
				}

			};
			1.wait;
		};
	})}

	offsetHeading { // give offset to reset North
		| angle |
		headingOffset = angle;
	}


	registerSynth { // selection of Mosca arguments for use in synths
		| source, synth |
		synthRegistry[source-1].add(synth);
	}
	deregisterSynth { // selection of Mosca arguments for use in synths
		| source, synth |
		if(synthRegistry[source-1].notNil){
			synthRegistry[source-1].remove(synth);

		};
	}


	getSynthRegistry { // selection of Mosca arguments for use in synths
		| source |
		^synthRegistry[source-1];
	}

	getSCBus {
		|source |
		if (source > 0) {
			var bus = scInBus[source - 1].index;
			^bus
		}
	}


	setSynths {
		|source, param, value|

		synthRegistry[source].do({
			| item, i |

			if(item.notNil) {
				item.set(param, value);
			}
		});

	}

	getInsertIn {
		|source |
		if (source > 0) {
			var bus = insertBus[0,source-1];
			insertFlag[source-1]=1;
			espacializador[source-1].set(\insertFlag, 1);
			synt[source-1].set(\insertFlag, 1);
			^bus
		}
	}

	getInsertOut {
		|source |
		if (source > 0) {
			var bus = insertBus[1,source-1];
			insertFlag[source-1]=1;
			espacializador[source-1].set(\insertFlag, 1);
			synt[source-1].set(\insertFlag, 1);
			^bus
		}
	}

	releaseInsert {
		|source |
		if (source > 0) {
			insertFlag[source-1]=0;
			espacializador[source-1].set(\insertFlag, 0);
		}
	}

	// These methods relate to control of synths when SW Input delected
	// for source in GUI

	// Set by user. Registerred functions called by Automation's play
	setTriggerFunc {
		|source, function|
		if (source > 0) {
			triggerFunc[source-1] = function;
		}
	}

	// Companion stop method
	setStopFunc {
		|source, function|
		if (source > 0) {
			stopFunc[source-1] = function;
		}
	}

	clearTriggerFunc {
		|source|
		if (source > 0) {
			triggerFunc[source-1] = nil;
		}
	}

	clearStopFunc {
		|source|
		if (source > 0) {
			stopFunc[source-1] = nil;
		}
	}

	playAutomation {
		control.play;
	}

	// no longer necessary as added a autoloop creation argument
	playAutomationLooped {
		//autoloopval = true;
		autoloop.valueAction = true;
		control.play;
	}

	nonAmbi2FuMaNeeded { |i|
		if (i == nfontes) {
			^false.asBoolean;
		} {
			if ( (lib[i].value > lastFUMA) // pass ambisonic libs
				&& espacializador[i].notNil ) {
				^true.asBoolean;
			} {
				^this.nonAmbi2FuMaNeeded(i + 1);
			};
		};
	}

	converterNeeded { |i|
		if (i == nfontes) {
			^false.asBoolean;
		} {
			if (convert_fuma && (clsrv != 0)) {
				^true.asBoolean;
			} {
				if ( convert[i] && espacializador[i].notNil ) {
					^true.asBoolean;
				} {
					^this.converterNeeded(i + 1);
				};
			};
		};
	}


	newtocar {
		| i, tpos, force = false |
		var path = tfieldProxy[i].value;

		if (streamdisk[i]) {
			var sframe, srate;
			// sframe = tpos * srate;
			// stdur = sf.numFrames / srate; // needed?
			streambuf[i] = Buffer.cueSoundFile(server, path, 0,
				ncanais[i], 131072);
			//		streambuf[i] = srate; //??
			("Creating buffer for source: " ++ i).postln;
		};


		// Note: ncanais refers to number of channels in the context of
		// files on disk
		// ncan is number of channels for hardware or supercollider input
		// busini is the initial bus used for a particular stream
		// If we have ncan = 4 and busini = 7, the stream will enter
		// in buses 7, 8, 9 and 10.


		/// STREAM FROM DISK


		if ((path != "") && hwncheckProxy[i].value.not
			&& scncheckProxy[i].value.not
			&& streamdisk[i]) {
			"Content Streamed from disk".postln;
			if (audit[i].not || force) { // if source is testing don't relaunch synths

				case
				{ ncanais[i] == 1} {
					"1 channel".postln;

					synt[i] = Synth.new(\playMonoStream, [\outbus, mbus[i],
						\bufnum, streambuf[i].bufnum, \rate, 1, \tpos, tpos, \lp, lp[i],
						\level, level[i]],
					playEspacGrp).onFree({espacializador[i].free;
						espacializador[i] = nil; synt[i] = nil;
						streambuf[i].free;
					});

					// set lib, convert and dstrv variables when stynths are lauched
					// for the tracking functions to stay relevant

					lib[i] = libboxProxy[i].value;

					case
					{ libboxProxy[i].value <= lastN3D }
					{ convert[i] = convert_n3d; }
					{ (libboxProxy[i].value >= (lastN3D + 1)) &&
						(libboxProxy[i].value <= lastFUMA) }
					{ convert[i] = convert_fuma; }
					{ libboxProxy[i].value >= (lastFUMA + 1) }
					{ convert[i] = convert_direct; };

					dstrv[i] = dstrvboxProxy[i].value;

					if (convert[i]) {

						if (convertor.notNil) {
							convertor.set(\gate, 1);
						} {
							convertor = Synth.new(\ambiConverter, [\gate, 1],
								target:glbRevDecGrp).onFree({
								convertor = nil;
							});
						};
					};

					if (azimuths.isNil) {

						if ((libboxProxy[i].value > lastFUMA)
							&& nonAmbi2FuMa.isNil) {
							nonAmbi2FuMa = Synth.new(\nonAmbi2FuMa,
								target:glbRevDecGrp).onFree({
								nonAmbi2FuMa = nil;
							});
						};
					};

					espacializador[i] = Synth.new(libName[i]++"Chowning"
						++dstrvtypes[i],
						[\inbus, mbus[i], \insertFlag, insertFlag[i],
							\insertIn, insertBus[0,i],
							\insertOut, insertBus[1,i],
							\contr, clev[i], \room, rm[i], \damp, dm[i],
							\grainrate, grainrate[i], \winsize, winsize[i],
							\winrand, winrand[i]] ++
						wSpecPar.value(max(dstrvboxProxy[i].value - 3, 0)),
						synt[i], addAction:\addAfter).onFree({
						if (azimuths.isNil) {
							if (this.nonAmbi2FuMaNeeded(0).not
								&& nonAmbi2FuMa.notNil) {
								nonAmbi2FuMa.free;
							};
						};
						if (convertor.notNil) {
							if (convert[i]) {
								if (this.converterNeeded(0).not) {
									convertor.set(\gate, 0);
								};
							};
						};
					});

					//atualizarvariaveis.value;
					updatesourcevariables.value(i);

				}
				{ ncanais[i] == 2} {
					"2 channel".postln;

					synt[i] = Synth.new(\playStereoStream, [\outbus, sbus[i],
						\bufnum, streambuf[i].bufnum, \rate, 1, \tpos, tpos, \lp, lp[i],
						\level, level[i]],
					playEspacGrp).onFree({espacializador[i].free;
						espacializador[i] = nil; synt[i] = nil;
						streambuf[i].free;
					});

					// set lib, convert and dstrv variables when stynths are lauched
					// for the tracking functions to stay relevant

					lib[i] = libboxProxy[i].value;

					case
					{ libboxProxy[i].value <= lastN3D }
					{ convert[i] = convert_n3d; }
					{ (libboxProxy[i].value >= (lastN3D + 1))
						&& (libboxProxy[i].value <= lastFUMA) }
					{ convert[i] = convert_fuma; }
					{ libboxProxy[i].value >= (lastFUMA + 1) }
					{ convert[i] = convert_direct; };

					dstrv[i] = dstrvboxProxy[i].value;

					if (convert[i]) {

						if (convertor.notNil) {
							convertor.set(\gate, 1);
						} {
							convertor = Synth.new(\ambiConverter, [\gate, 1],
								target:glbRevDecGrp).onFree({
								convertor = nil;
							});
						};
					};

					if (azimuths.isNil) {

						if ((libboxProxy[i].value > lastFUMA)
							&& nonAmbi2FuMa.isNil) {
							nonAmbi2FuMa = Synth.new(\nonAmbi2FuMa,
								target:glbRevDecGrp).onFree({
								nonAmbi2FuMa = nil;
							});
						};
					};

					espacializador[i] = Synth.new(libName[i]++"StereoChowning"
						++dstrvtypes[i],
						[\inbus, sbus[i], \angle, angle[i],
							\insertFlag, insertFlag[i],
							\insertIn, insertBus[0,i],
							\insertOut, insertBus[1,i],
							\room, rm[i], \damp, dm[i], \contr, clev[i],
							\grainrate, grainrate[i], \winsize, winsize[i],
							\winrand, winrand[i]] ++
						zSpecPar.value(max(dstrvboxProxy[i].value - 3, 0)),
						synt[i], addAction: \addAfter).onFree({
						if (azimuths.isNil) {
							if (this.nonAmbi2FuMaNeeded(0).not
								&& nonAmbi2FuMa.notNil) {
								nonAmbi2FuMa.free;
							};
						};
						if (convertor.notNil) {
							if (convert[i]) {
								if (this.converterNeeded(0).not) {
									convertor.set(\gate, 0);
								};
							};
						};
					});

					updatesourcevariables.value(i);

				}
				{ ncanais[i] >= 4} {
					playingBF[i] = true;
					("contains "++ncanais[i]++" channels").postln;

					if ((libboxProxy[i].value >= (lastN3D + 1)) ||
						(dstrvboxProxy[i].value == 3)) {
						libboxProxy[i].valueAction = lastN3D + 1;
						lib[i] = lastN3D + 1;
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
						if (convertor.notNil) {
							convertor.set(\gate, 1);
						} {
							convertor = Synth.new(\ambiConverter, [\gate, 1],
								target:glbRevDecGrp).onFree({
								convertor = nil;
							});
						};
					};

					synt[i] = Synth.new("playBFormat"++libName[i]++"Stream_"
						++ncanais[i],
						[\outbus, mbus[i], \bufnum, streambuf[i].bufnum, \contr, clev[i],
							\rate, 1, \tpos, tpos, \lp, lp[i], \level, level[i],
							\insertFlag, insertFlag[i],
							\insertIn, insertBus[0,i],
							\insertOut, insertBus[1,i]],
						playEspacGrp).onFree({espacializador[i].free;
						espacializador[i] = nil; synt[i] = nil;
						playingBF[i] = false;
						streambuf[i].free;
					});

					espacializador[i] = Synth.new(\ATK2Chowning++dstrvtypes[i],
						[\insertFlag, insertFlag[i], \contr, clev[i],
							\insertIn, insertBus[0,i],
							\insertOut, insertBus[1,i],
							\room, rm[i], \damp, dm[i],
							\grainrate, grainrate[i], \winsize, winsize[i],
							\winrand, winrand[i]] ++
						wSpecPar.value(max(dstrvboxProxy[i].value - 3, 0)),
						synt[i], addAction: \addAfter).onFree({
						if (convertor.notNil) {
							if (convert[i]) {
								if (this.converterNeeded(0).not) {
									convertor.set(\gate, 0);
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
		if ((path != "") && (hwncheckProxy[i].value.not
			|| scncheckProxy[i].value.not)
		&& streamdisk[i].not) {

			//{
			case
			{ ncanais[i] == 1} { // arquivo mono

				synt[i] = Synth.new(\playMonoFile, [\outbus, mbus[i],
					\bufnum, sombuf[i].bufnum, \rate, 1, \tpos, tpos, \lp, lp[i],
					\level, level[i]],
				playEspacGrp).onFree({espacializador[i].free;
					espacializador[i] = nil;
					synt[i] = nil});

				// set lib, convert and dstrv variables when stynths are lauched
				// for the tracking functions to stay relevant

				lib[i] = libboxProxy[i].value;

				case
				{ libboxProxy[i].value <= lastN3D }
				{ convert[i] = convert_n3d; }
				{ (libboxProxy[i].value >= (lastN3D + 1))
					&& (libboxProxy[i].value <= lastFUMA) }
				{ convert[i] = convert_fuma; }
				{ libboxProxy[i].value >= (lastFUMA + 1) }
				{ convert[i] = convert_direct; };

				dstrv[i] = dstrvboxProxy[i].value;

				if (convert[i]) {

					if (convertor.notNil) {
						convertor.set(\gate, 1);
					} {
						convertor = Synth.new(\ambiConverter, [\gate, 1],
							target:glbRevDecGrp).onFree({
								convertor = nil;
						});
					};
				};

				if (azimuths.isNil) {

					if ((libboxProxy[i].value > lastFUMA)
						&& nonAmbi2FuMa.isNil) {
						nonAmbi2FuMa = Synth.new(\nonAmbi2FuMa,
							target:glbRevDecGrp).onFree({
							nonAmbi2FuMa = nil;
						});
					};
				};

				espacializador[i] = Synth.new(libName[i]++"Chowning"++dstrvtypes[i],
					[\inbus, mbus[i], \insertFlag, insertFlag[i],
						\insertIn, insertBus[0,i],
						\insertOut, insertBus[1,i],
						\room, rm[i], \damp, dm[i], \contr, clev[i],
						\grainrate, grainrate[i], \winsize, winsize[i],
						\winrand, winrand[i]]  ++
					wSpecPar.value(max(dstrvboxProxy[i].value - 3, 0)),
					synt[i], addAction: \addAfter).onFree({
					if (azimuths.isNil) {
						if (this.nonAmbi2FuMaNeeded(0).not
							&& nonAmbi2FuMa.notNil) {
							nonAmbi2FuMa.free;
						};
					};
						if (convertor.notNil) {
						if (convert[i]) {
							if (this.converterNeeded(0).not) {
								convertor.set(\gate, 0);
							};
						};
					};
				});

				updatesourcevariables.value(i);

			}
			{ ncanais[i] == 2 } {

				synt[i] = Synth.new(\playStereoFile, [\outbus, sbus[i],
					\bufnum, sombuf[i].bufnum, \rate, 1, \tpos, tpos, \lp, lp[i],
					\level, level[i]],
				playEspacGrp).onFree({espacializador[i].free;
					espacializador[i] = nil;
					synt[i] = nil});

				// set lib, convert and dstrv variables when stynths are lauched
				// for the tracking functions to stay relevant

				lib[i] = libboxProxy[i].value;

				case
				{ libboxProxy[i].value <= lastN3D }
				{ convert[i] = convert_n3d; }
				{ (libboxProxy[i].value >= (lastN3D + 1))
					&& (libboxProxy[i].value <= lastFUMA) }
				{ convert[i] = convert_fuma; }
				{ libboxProxy[i].value >= (lastFUMA + 1) }
				{ convert[i] = convert_direct; };

				dstrv[i] = dstrvboxProxy[i].value;

				if (convert[i]) {

					if (convertor.notNil) {
						convertor.set(\gate, 1);
					} {
						convertor = Synth.new(\ambiConverter, [\gate, 1],
							target:glbRevDecGrp).onFree({
							convertor = nil;
						});
					};
				};

				if (azimuths.isNil) {

					if ((libboxProxy[i].value > lastFUMA) && nonAmbi2FuMa.isNil) {
						nonAmbi2FuMa = Synth.new(\nonAmbi2FuMa,
							target:glbRevDecGrp).onFree({
							nonAmbi2FuMa = nil;
						});
					};
				};

				espacializador[i] = Synth.new(libName[i]++"StereoChowning"
					++dstrvtypes[i],
					[\inbus, sbus[i], \angle, angle[i],
						\insertFlag, insertFlag[i],
						\insertIn, insertBus[0,i],
						\insertOut, insertBus[1,i],
						\room, rm[i], \damp, dm[i], \contr, clev[i],
						\grainrate, grainrate[i], \winsize, winsize[i],
						\winrand, winrand[i]] ++
					zSpecPar.value(max(dstrvboxProxy[i].value - 3, 0)),
					synt[i], addAction: \addAfter).onFree({
					if (azimuths.isNil) {
						if (this.nonAmbi2FuMaNeeded(0).not
							&& nonAmbi2FuMa.notNil) {
							nonAmbi2FuMa.free;
						};
					};
					if (convertor.notNil) {
						if (convert[i]) {
							if (this.converterNeeded(0).not) {
								convertor.set(\gate, 0);
							};
						};
					};
				});

				//atualizarvariaveis.value;
				updatesourcevariables.value(i);

			}
			{ ncanais[i] >= 4 } {
				playingBF[i] = true;

				if ((libboxProxy[i].value >= (lastN3D + 1)) ||
					(dstrvboxProxy[i].value == 3)) {
					libboxProxy[i].valueAction = lastN3D + 1;
					lib[i] = lastN3D + 1;
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
					if (convertor.notNil) {
						convertor.set(\gate, 1);
					} {
						convertor = Synth.new(\ambiConverter, [\gate, 1],
							target:glbRevDecGrp).onFree({
							convertor = nil;
						});
					};
				};

				synt[i] = Synth.new("playBFormat"++libName[i]++"File_"++ncanais[i],
					[\outbus, mbus[i], \bufnum, sombuf[i].bufnum, \contr, clev[i],
						\rate, 1, \tpos, tpos, \lp,
						lp[i], \level, level[i],
						\insertFlag, insertFlag[i],
						\insertIn, insertBus[0,i],
						\insertOut, insertBus[1,i]],
					playEspacGrp).onFree({espacializador[i].free;
					espacializador[i] = nil;
					synt[i] = nil;
					playingBF[i] = false;
				});

				espacializador[i] = Synth.new(\ATK2Chowning++dstrvtypes[i],
					[\inbus, mbus[i], \insertFlag, insertFlag[i],
						\insertIn, insertBus[0,i],
						\insertOut, insertBus[1,i],
						\contr, clev[i], \room, rm[i], \damp, dm[i],
						\grainrate, grainrate[i], \winsize, winsize[i],
						\winrand, winrand[i]] ++
					wSpecPar.value(max(dstrvboxProxy[i].value - 3, 0)),
					synt[i], addAction: \addAfter).onFree({
					if (convertor.notNil) {
						if (convert[i]) {
							if (this.converterNeeded(0).not) {
								convertor.set(\gate, 0);
							};
						};
					};
				});

			};

			updatesourcevariables.value(i);

		} {
			if ((scncheckProxy[i].value) || (hwncheckProxy[i])) {

				case
				{ ncan[i] == 1 } {

					if (hwncheckProxy[i].value) {

						synt[i] = Synth.new(\playMonoHWBus, [\outbus, mbus[i],
							\busini, busini[i],\level, level[i]],
						playEspacGrp).onFree({espacializador[i].free;
							espacializador[i] = nil;
							synt[i] = nil});

					} {
						synt[i] = Synth.new(\playMonoSWBus, [\outbus, mbus[i],
							\busini, scInBus[i], // use "index" method?
							\level, level[i]],
						playEspacGrp).onFree({espacializador[i].free;
							espacializador[i] = nil;
							synt[i] = nil});
					};

					// set lib, convert and dstrv variables when stynths are lauched
					// for the tracking functions to stay relevant

					lib[i] = libboxProxy[i].value;

					case
					{ libboxProxy[i].value <= lastN3D }
					{ convert[i] = convert_n3d; }
					{ (libboxProxy[i].value >= (lastN3D + 1))
						&& (libboxProxy[i].value <= lastFUMA) }
					{ convert[i] = convert_fuma; }
					{ libboxProxy[i].value >= (lastFUMA + 1) }
					{ convert[i] = convert_direct; };

					dstrv[i] = dstrvboxProxy[i].value;

					if (convert[i]) {

						if (convertor.notNil) {
							convertor.set(\gate, 1);
						} {
							convertor = Synth.new(\ambiConverter, [\gate, 1],
								target:glbRevDecGrp).onFree({
								convertor = nil;
							});
						};
					};

					if (azimuths.isNil) {

						if ((libboxProxy[i].value > lastFUMA)
							&& nonAmbi2FuMa.isNil) {
							nonAmbi2FuMa = Synth.new(\nonAmbi2FuMa,
								target:glbRevDecGrp).onFree({
								nonAmbi2FuMa = nil;
							});
						};
					};

					espacializador[i] = Synth.new(libName[i]++"Chowning"
						++dstrvtypes[i],
						[\inbus, mbus[i], \insertFlag, insertFlag[i],
							\insertIn, insertBus[0,i],
							\insertOut, insertBus[1,i],
							\room, rm[i], \damp, dm[i], \contr, clev[i],
							\grainrate, grainrate[i], \winsize, winsize[i],
							\winrand, winrand[i]] ++
						wSpecPar.value(max(dstrvboxProxy[i].value - 3, 0)),
						synt[i], addAction: \addAfter).onFree({
						if (azimuths.isNil) {
							if (this.nonAmbi2FuMaNeeded(0).not
								&& nonAmbi2FuMa.notNil) {
								nonAmbi2FuMa.free;
							};
						};
						if (convertor.notNil) {
							if (convert[i]) {
								if (this.converterNeeded(0).not) {
									convertor.set(\gate, 0);
								};
							};
						};
					});

					//atualizarvariaveis.value;
					updatesourcevariables.value(i);

				}
				{ ncan[i] == 2 } {

					if (hwncheckProxy[i].value) {

						synt[i] = Synth.new(\playStereoHWBus,
							[\outbus, sbus[i], \busini,
							busini[i],
							\level, level[i]], playEspacGrp).onFree({
							espacializador[i].free;
							espacializador[i] = nil;
							synt[i] = nil});
					} {
						synt[i] = Synth.new(\playStereoSWBus, [\outbus, sbus[i],
							\busini, scInBus[i],
							\level, level[i]], playEspacGrp).onFree({
							espacializador[i].free;
							espacializador[i] = nil;
							synt[i] = nil});
					};

					// set lib, convert and dstrv variables when stynths are lauched
					// for the tracking functions to stay relevant

					lib[i] = libboxProxy[i].value;

					case
					{ libboxProxy[i].value <= lastN3D }
					{ convert[i] = convert_n3d; }
					{ (libboxProxy[i].value >= (lastN3D + 1))
						&& (libboxProxy[i].value <= lastFUMA) }
					{ convert[i] = convert_fuma; }
					{ libboxProxy[i].value >= (lastFUMA + 1) }
					{ convert[i] = convert_direct; };

					dstrv[i] = dstrvboxProxy[i].value;

					if (convert[i]) {

						if (convertor.notNil) {
							convertor.set(\gate, 1);
						} {
							convertor = Synth.new(\ambiConverter, [\gate, 1],
								target:glbRevDecGrp).onFree({
								convertor = nil;
							});
						};
					};

					if (azimuths.isNil) {

						if ((libboxProxy[i].value > lastFUMA)
							&& nonAmbi2FuMa.isNil) {
							nonAmbi2FuMa = Synth.new(\nonAmbi2FuMa,
								target:glbRevDecGrp).onFree({
								nonAmbi2FuMa = nil;
							});
						};
					};

					espacializador[i] = Synth.new(libName[i]++"StereoChowning"
						++dstrvtypes[i],
						[\inbus, sbus[i], \angle, angle[i],
							\insertFlag, insertFlag[i],
							\insertIn, insertBus[0,i],
							\insertOut, insertBus[1,i],
							\contr, clev[i], \room, rm[i], \damp, dm[i],
							\grainrate, grainrate[i], \winsize, winsize[i],
							\winrand, winrand[i]] ++
						zSpecPar.value(max(dstrvboxProxy[i].value - 3, 0)),
						synt[i], addAction: \addAfter).onFree({
						if (azimuths.isNil) {
							if (this.nonAmbi2FuMaNeeded(0).not
								&& nonAmbi2FuMa.notNil) {
								nonAmbi2FuMa.free;
							};
						};
						if (convertor.notNil) {
							if (convert[i]) {
								if (this.converterNeeded(0).not) {
									convertor.set(\gate, 0);
								};
							};
						};
					});

					//atualizadstrvariaveis.value;
					updatesourcevariables.value(i);

				}
				{ ncan[i] >= 4 } {

					if ((libboxProxy[i].value >= (lastN3D + 1)) ||
						(dstrvboxProxy[i].value == 3)) {
						libboxProxy[i].valueAction = lastN3D + 1;
						lib[i] = lastN3D + 1;
						convert[i] = convert_fuma;
					} {
						libboxProxy[i].valueAction = 0;
						lib[i] = 0;
						convert[i] = true;
						dstrv[i] = dstrvboxProxy[i].value;
					};

					if (convert[i]) {
						if (convertor.notNil) {
							convertor.set(\gate, 1);
						} {
							convertor = Synth.new(\ambiConverter, [\gate, 1],
								target:glbRevDecGrp).onFree({
								convertor = nil;
							});
						};
					};

					if (hwncheckProxy[i].value) {

						synt[i] = Synth.new(\playBFormat++libName[i]++"HWBus_"
							++ncan[i],
							[\gbfbus, gbfbus, \gbixfbus, gbixfbus, \outbus, mbus[i],
								\contr, clev[i], \rate, 1, \tpos, tpos, \level, level[i],
								\insertFlag, insertFlag[i],
								\insertIn, insertBus[0,i],
								\insertOut, insertBus[1,i],
								\busini, busini[i]],playEspacGrp).onFree({
							espacializador[i].free;
							espacializador[i] = nil;
							synt[i] = nil;
						});

					} {

						synt[i] = Synth.new(\playBFormat++libName[i]++"SWBus_"
							++ncan[i],
							[\gbfbus, gbfbus, \gbixfbus, gbixfbus, \outbus, mbus[i],
								\contr, clev[i], \rate, 1, \tpos, tpos,
								\level, level[i],
								\insertFlag, insertFlag[i],
								\insertIn, insertBus[0,i],
								\insertOut, insertBus[1,i],
								\busini, scInBus[i] ],playEspacGrp).onFree({
							espacializador[i].free;
							espacializador[i] = nil;
							synt[i] = nil;
						});
					};

					espacializador[i] = Synth.new(\ATK2Chowning++dstrvtypes[i],
						[\inbus, mbus[i], \insertFlag, insertFlag[i],
							\insertIn, insertBus[0,i],
							\insertOut, insertBus[1,i],
							\contr, clev[i], \room, rm[i], \damp, dm[i],
							\grainrate, grainrate[i], \winsize, winsize[i],
							\winrand, winrand[i]] ++
						wSpecPar.value(max(dstrvboxProxy[i].value - 3, 0)),
						synt[i], addAction: \addAfter).onFree({
						if (convertor.notNil) {
							if (convert[i]) {
								if (this.converterNeeded(0).not) {
									convertor.set(\gate, 0);
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
				tfieldProxy[i].valueAction = line;
			} {
				tfieldProxy[i].valueAction = "";
			};
		};

		nfontes.do { | i |
			var line = libf.getLine(1024);
			libboxProxy[i].valueAction = line.asInteger;
		};

		nfontes.do { | i |
			var line = loopedf.getLine(1024);
			var flag;
			if (line == "true") {flag = true;} {flag = false;};
			lpcheckProxy[i].valueAction = flag;
			//lp[i] 0 or 1
		};

		nfontes.do { | i |
			var line = aformatrevf.getLine(1024);
			dstrvboxProxy[i].valueAction = line.asInteger;
			//dstrv[i] 0 or 1
		};

		nfontes.do { | i |
			var line = spreadf.getLine(1024);
			var flag;
			if (line == "true") {flag = true;} {flag = false;};
			spcheckProxy[i].valueAction = flag;
		};

		nfontes.do { | i |
			var line = diffusef.getLine(1024);
			var flag;
			if (line == "true") {flag = true;} {flag = false;};
			dfcheckProxy[i].valueAction = flag;
		};
		nfontes.do { | i |
			var line = ncanf.getLine(1024);
			ncanboxProxy[i].valueAction = line.asInteger;
		};

		nfontes.do { | i |
			var line = businif.getLine(1024);
			businiboxProxy[i].valueAction = line.asInteger;
		};

		nfontes.do { | i |
			var line = stcheckf.getLine(1024);
			var flag;
			if (line == "true") {flag = true;} {flag = false;};
			stcheckProxy[i].valueAction = flag;
		};

		//		nfontes.do { arg i;
		//	var line = hwinf.getLine(1024);
		//	hwncheckProxy[i].valueAction = line;
		// };
		nfontes.do { | i |
			var line = hwinf.getLine(1024);
			var flag;
			if (line == "true") {flag = true;} {flag = false;};

			//("line = " ++ line.asString).postln;

			//hwncheckProxy[i].valueAction = line.booleanValue;
			// why, why, why is this asBoolean necessary!
			hwncheckProxy[i].valueAction = flag;
		};


		nfontes.do { | i |
			var line = scinf.getLine(1024);
			var flag;
			if (line == "true") {flag = true;} {flag = false;};
			scncheckProxy[i].value = flag;
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

		// delay necessary here because streamdisks take some time to register
		// after control.load
		//Routine {

		//1.wait;
		/*nfontes.do { arg i;

		var newpath = tfieldProxy[i].value;
		//	server.sync;
		if (streamdisk[i].not && (tfieldProxy[i].value != "")) {
		i.postln;
		newpath.postln;

		sombuf[i] = Buffer.read(server, newpath, action: {arg buf;
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
		nfontes.do { | i |
			firstTime[i]=true;
		};

		if(control.now < 0)
		{
			startTime = 0
		} {
			startTime = control.now
		};
		isPlay = true;
		control.play;

		//runTriggers.value;
	}

	blindControlStop {
		control.stop;
		runStops.value;
		nfontes.do { | i |
			// if sound is currently being "tested", don't switch off on stop
			// leave that for user
			if (audit[i] == false) {
				synt[i].free; // error check
			};
			//	espacializador[i].free;
		};
		isPlay = false;
	}



	free {

		control.quit;
		if (serport.notNil) {
			trackPort.close;
			//				this.trackerRoutine.stop;
			//				this.serialKeepItUp.stop;
		};

		troutine.stop;
		kroutine.stop;
		watcher.stop;

		fumabus.free;
		n3dbus.free;
		nfontes.do { | x |
			espacializador[x].free;
			mbus[x].free;
			sbus[x].free;
			//      bfbus.[x].free;
			sombuf[x].free;
			streambuf[x].free;
			synt[x].free;
			scInBus[x].free;
			//		kespac[x].stop;
		};
		MIDIIn.removeFuncFrom(\sysex, sysex);
		//MIDIIn.disconnect;
		if(revGlobal.notNil){
			revGlobal.free;
		};

		if(globDec.notNil){
			globDec.free
		};

		gbus.free;
		gbfbus.free;

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

		playEspacGrp.free;
		glbRevDecGrp.free;

	}


	gui {

		var sprite,
		furthest,
		dist,
		itensdemenu,
		event, brec, bplay, bload, bstream, loadOrStream, bnodes,
		dopcheque2,
		mouseButton,
		period,
		conslider,
		// check box for streamed from disk audio
		brecaudio,
		blipcheck,
		zAxis,
		bmark1, bmark2,
		bdados,
		win,
		wdados,
		waux,
		dialView,
		autoView,
		originView,
		cnumbox,
		textbuf,
		sourceName,
		sourceList,
		baux,
		bsalvar,
		bcarregar,
		sourceSelect,
		//m,
		moveSource,
		zoom_factor = 1,
		zSliderHeight = height * 2 / 3,
		hdtrkcheck;


		// Note there is an extreme amount repetition occurring here.
		// See the calling function. fix

		win = Window.new("Mosca", Rect(0, width, width, height)).front;
		win.background = Color.new255( 200, 200, 200 ); // OSSIA/score "HalfLight"

		win.drawFunc = {

			Pen.fillColor = Color.new255(0, 127, 229, 76); // OSSIA/score "Transparent1"
			Pen.addArc(halfwidth@halfheight, halfheight * zoom_factor, 0, 2pi);
			Pen.fill;

			Pen.strokeColor = Color.new255(37, 41, 48, 40);
			Pen.addArc(halfwidth@halfheight, halfheight * zoom_factor *
				0.01 * longest_radius, 0, 2pi);
			Pen.stroke;

			nfontes.do { |i|
				var x, y;
				var topView = spheval[i] * zoom_factor * 0.01;
				var lev = topView.z;
				var color = lev * 0.4;
				{x = halfwidth + (topView.x * halfheight)}.defer;
				{y = halfheight - (topView.y * halfheight)}.defer;
				Pen.addArc(x@y, 14 + (lev * 0.01 * halfheight * 2), 0, 2pi);
				if ((audit[i] || isPlay) && (lev.abs <= plim)) {
					Pen.fillColor = Color.new255(179, 90,209);
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
			period = Main.elapsedTime - lastGui;
			if (period > guiInt) {
				lastGui =  Main.elapsedTime;
				{
					{ zlev[currentsource] = spheval[currentsource].z; }.defer;
					{ zslider.value = (zlev[currentsource] * 0.01 + 1)
						* 0.5; }.defer;
					{ znumbox.value = zlev[currentsource]; }.defer;
					{ win.refresh; }.defer;
				}.defer(guiInt);
			};
		};

		updateGuiCtl = { |ctl, num|
			switch (ctl,
				\chan,
				{ var selector;

					if (hwncheckProxy[currentsource].value.not
						&& scncheckProxy[currentsource].value.not) {
						selector = ncanais[currentsource];
					} {
						selector = ncan[currentsource];
					};

					case
					{ (selector.value < 2) || (selector.value == 3) }
					{
						winCtl[0][8].visible = false;
						winCtl[1][8].visible = false;
						winCtl[2][8].visible = false;
						winCtl[0][9].visible = false;
						winCtl[1][9].visible = false;
						winCtl[2][9].visible = false;
						winCtl[0][7].visible = false;
						winCtl[1][7].visible = false;
						winCtl[2][7].visible = false;
					}
					{ selector.value == 2 }
					{
						winCtl[0][8].visible = false;
						winCtl[1][8].visible = false;
						winCtl[2][8].visible = false;
						winCtl[0][9].visible = false;
						winCtl[1][9].visible = false;
						winCtl[2][9].visible = false;
						winCtl[0][7].visible = true;
						winCtl[1][7].visible = true;
						winCtl[2][7].visible = true;
						winCtl[0][7].value = angle[currentsource];
						winCtl[1][7].value = angle[currentsource] / pi;
					}
					{ selector.value >= 4 }
					{
						winCtl[0][7].visible = false;
						winCtl[1][7].visible = false;
						winCtl[2][7].visible = false;
						winCtl[0][8].visible = true;
						winCtl[1][8].visible = true;
						winCtl[2][8].visible = true;
						winCtl[0][9].visible = true;
						winCtl[1][9].visible = true;
						winCtl[2][9].visible = true;
						winCtl[0][8].value = rlev[currentsource];
						winCtl[1][8].value = (rlev[currentsource] + pi) / 2pi;
						winCtl[0][9].value = dlev[currentsource];
						winCtl[1][9].value = dlev[currentsource] / halfPi;
					};
				},
				\lib,
				{ libnumbox.value = num.value;
					case
					{ libnumbox.value == (lastN3D + 1) }
					{
						winCtl[0][10].visible = false;
						winCtl[1][10].visible = false;
						winCtl[2][10].visible = false;
						winCtl[0][11].visible = false;
						winCtl[1][11].visible = false;
						winCtl[2][11].visible = false;
						winCtl[0][12].visible = false;
						winCtl[1][12].visible = false;
						winCtl[2][12].visible = false;

						spreadcheck.visible = true;
						diffusecheck.visible = true;
						if(sp[currentsource] == 1) {
							spreadcheck.value = true
						} {
							spreadcheck.value = false
						};
						if(df[currentsource] == 1) {
							diffusecheck.value = true
						}{
							diffusecheck.value = false
						};
					}
					{ libnumbox.value == lastFUMA }
					{
						spreadcheck.visible = false;
						diffusecheck.visible = false;

						winCtl[0][10].visible = true;
						winCtl[1][10].visible = true;
						winCtl[2][10].visible = true;
						winCtl[0][11].visible = true;
						winCtl[1][11].visible = true;
						winCtl[2][11].visible = true;
						winCtl[0][12].visible = true;
						winCtl[1][12].visible = true;
						winCtl[2][12].visible = true;

						winCtl[0][10].value = grainrate[currentsource];
						winCtl[1][10].value = (grainrate[currentsource] - 1) / 59;
						winCtl[0][11].value = winsize[currentsource];
						winCtl[1][11].value = winsize[currentsource] * 5;
						winCtl[0][12].value = winrand[currentsource];
						winCtl[1][12].value = winrand[currentsource].sqrt;
					}
					{ (libnumbox.value != (lastN3D + 1))
						&& (libnumbox.value != lastFUMA) }
					{
						winCtl[0][10].visible = false;
						winCtl[1][10].visible = false;
						winCtl[2][10].visible = false;
						winCtl[0][11].visible = false;
						winCtl[1][11].visible = false;
						winCtl[2][11].visible = false;
						winCtl[0][12].visible = false;
						winCtl[1][12].visible = false;
						winCtl[2][12].visible = false;

						spreadcheck.visible = false;
						diffusecheck.visible = false;
					};
				},
				\dstrv,
				{ dstReverbox.value = num.value;
					case
					{dstrvboxProxy[currentsource].value == 0}
					{
						winCtl[2][4].visible = false;
						winCtl[1][4].visible = false;
						winCtl[0][4].visible = false;
						winCtl[2][5].visible = false;
						winCtl[1][5].visible = false;
						winCtl[0][5].visible = false;
						winCtl[2][6].visible = false;
						winCtl[1][6].visible = false;
						winCtl[0][6].visible = false;
					}
					{(dstrvboxProxy[currentsource].value >= 0)
						&& (dstrvboxProxy[currentsource].value < 3)}
					{
						winCtl[2][4].visible = true;
						winCtl[1][4].visible = true;
						winCtl[0][4].visible = true;
						winCtl[2][5].visible = true;
						winCtl[1][5].visible = true;
						winCtl[0][5].visible = true;
						winCtl[2][6].visible = true;
						winCtl[1][6].visible = true;
						winCtl[0][6].visible = true;
						winCtl[0][4].value = llev[currentsource];
						winCtl[1][4].value = llev[currentsource];
						winCtl[0][5].value = rm[currentsource];
						winCtl[1][5].value = rm[currentsource];
						winCtl[0][6].value = dm[currentsource];
						winCtl[1][6].value = dm[currentsource];
					}
					{dstrvboxProxy[currentsource].value >= 3}
					{
						winCtl[2][4].visible = true;
						winCtl[1][4].visible = true;
						winCtl[0][4].visible = true;
						winCtl[2][5].visible = false;
						winCtl[1][5].visible = false;
						winCtl[0][5].visible = false;
						winCtl[2][6].visible = false;
						winCtl[1][6].visible = false;
						winCtl[0][6].visible = false;
						winCtl[0][4].value = llev[currentsource];
						winCtl[1][4].value = llev[currentsource];
					};
				},
				\clsrv,
				{ clsReverbox.value = num.value;
					case
					{clsrvboxProxy.value == 0}
					{
						winCtl[0][3].visible = false;
						winCtl[1][3].visible = false;
						winCtl[2][3].visible = false;

						originCtl[0][0].visible = false;
						originCtl[1][0].visible = false;
						originCtl[0][1].visible = false;
						originCtl[1][1].visible = false;
					}
					{(clsrvboxProxy.value >= 0)
						&& (clsrvboxProxy.value < 3)}
					{
						winCtl[0][3].visible = true;
						winCtl[1][3].visible = true;
						winCtl[2][3].visible = true;

						originCtl[0][0].visible = true;
						originCtl[1][0].visible = true;
						originCtl[0][1].visible = true;
						originCtl[1][1].visible = true;
					}
					{clsrvboxProxy.value >= 3}
					{
						winCtl[0][3].visible = true;
						winCtl[1][3].visible = true;
						winCtl[2][3].visible = true;

						originCtl[0][0].visible = false;
						originCtl[1][0].visible = false;
						originCtl[0][1].visible = false;
						originCtl[1][1].visible = false;
					};
				},
				\src,
				{case
					{hwn[currentsource] == scn[currentsource]}
					{
						hwInCheck.value = false;
						scInCheck.value = false;
						hwCtl[0][0].visible = false;
						hwCtl[1][0].visible = false;
						hwCtl[0][1].visible = false;
						hwCtl[1][1].visible = false;
						bload.visible = true;
						bstream.visible = true;
						loopcheck.visible = true;
						{ bstream.value = streamdisk[currentsource].value; }.defer;
						{ loopcheck.value = lp[currentsource].value; }.defer;
					}
					{hwn[currentsource] == 1}
					{
						hwInCheck.value = true;
						scInCheck.value = false;
						bload.visible = false;
						bstream.visible = false;
						loopcheck.visible = false;
						hwCtl[0][0].visible = true;
						hwCtl[1][0].visible = true;
						hwCtl[0][1].visible = true;
						hwCtl[1][1].visible = true;
						hwCtl[0][0].value = ncan[currentsource];
						hwCtl[0][1].value = busini[currentsource];
					}
					{scn[currentsource] == 1}
					{
						scInCheck.value = true;
						hwInCheck.value = false;
						bload.visible = false;
						bstream.visible = false;
						loopcheck.visible = false;
						hwCtl[0][0].visible = true;
						hwCtl[1][0].visible = true;
						hwCtl[0][1].visible = false;
						hwCtl[1][1].visible = false;
						hwCtl[0][0].value = ncan[currentsource];
					};
				};
			);
		};

		wdados = Window.new("Data", Rect(width, 0, 960, (nfontes*20)+60 ),
			scroll: true);
		wdados.userCanClose = false;
		wdados.alwaysOnTop = true;

		dialView = UserView(win, Rect(width - 100, 10, 180, 100));

		bdados = Button(dialView, Rect(0, 20, 90, 20))
		.states_([
			["show data", Color.black, Color.white],
			["hide data", Color.white, Color.blue]
		])
		.action_({ | but |
			if(but.value == 1)
			{wdados.front;}
			{wdados.visible = false;};
		});

		waux = Window.new("Auxiliary Controllers", Rect(width, (nfontes*20)+114,
			260, 250 ));
		waux.userCanClose = false;
		waux.alwaysOnTop = true;


		baux = Button(dialView, Rect(0, 0, 90, 20))
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

		winCtl = Array.newClear(3); // [0]numboxes, [1]sliders, [2]texts
		winCtl[0] = Array.newClear(13);
		winCtl[1] = Array.newClear(13);
		winCtl[2] = Array.newClear(13);

		baudi = Button(win,Rect(10, 90, 150, 20))
		.states_([
			["audition", Color.black, Color.green],
			["stop", Color.white, Color.red]
		])
		.action_({ | but |
			var bool = but.value.asBoolean;

			ossiaaud[currentsource].v_(bool);
			{ win.refresh; }.defer;
		});


		brecaudio = Button(dialView, Rect(0, 60, 90, 20))
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
				server.recChannels = recchans;
				// note the 2nd bus argument only works in SC 3.9
				server.record((prjDr ++ "/out.wav").standardizePath, recbus);

			}
			{
				server.stopRecording;
				"Recording stopped".postln;
			};

		});

		blipcheck = CheckBox(dialView, Rect(35, 80, 50, 15), "blips").action_({ | butt |
			if(butt.value) {
				//"Looping transport".postln;
				//autoloopval = true;
			} {
				//		autoloopval = false;
			};

		});

		if (serport.notNil) {
			hdtrkcheck = CheckBox(dialView, Rect(35, 95, 60, 15), "hdtrk").action_({ | butt |
				if(butt.value) {
					hdtrk = true;
				} {
					hdtrk = false;
					headingnumboxProxy.valueAction = 0;
					pitchnumboxProxy.valueAction = 0;
					rollnumboxProxy.valueAction = 0;
				};
			});
			hdtrkcheck.value = true;
		};

		autoView = UserView(win, Rect(10, width - 45, 325, 40));

		// save automation - adapted from chooseDirectoryDialog in AutomationGui.sc

		bsalvar = Button(autoView, Rect(0, 20, 80, 20))
		.states_([
			["save auto", Color.black, Color.white],

		])
		.action_({
			//arg but;
			var filenames;
			//			var arquivo = File((prjDr ++ "/auto/arquivos.txt")
			//.standardizePath,"w");
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
				filenames = File((textField.value ++
					"/filenames.txt").standardizePath,"w");

				libf = File((textField.value ++ "/lib.txt").standardizePath,"w");
				loopedf = File((textField.value ++ "/looped.txt").standardizePath,"w");
				aformatrevf = File((textField.value ++
					"/aformatrev.txt").standardizePath,"w");
				hwinf = File((textField.value ++ "/hwin.txt").standardizePath,"w");
				scinf = File((textField.value ++ "/scin.txt").standardizePath,"w");
				spreadf = File((textField.value ++ "/spread.txt").standardizePath,"w");
				diffusef = File((textField.value ++ "/diffuse.txt").standardizePath,"w");
				ncanf = File((textField.value ++ "/ncan.txt").standardizePath,"w");
				businif = File((textField.value ++ "/busini.txt").standardizePath,"w");
				stcheckf = File((textField.value ++ "/stcheck.txt").standardizePath,"w");


				nfontes.do { | i |
					if(tfieldProxy[i].value != "") {
						filenames.write(tfieldProxy[i].value ++ "\n")
					} {
						filenames.write("NULL\n")
					};

					libf.write(libbox[i].value.asString ++ "\n");
					loopedf.write(lpcheck[i].value.asString ++ "\n");
					aformatrevf.write(dstrvbox[i].value.asString ++ "\n");
					hwinf.write(hwncheck[i].value.asString ++ "\n");
					scinf.write(scncheck[i].value.asString ++ "\n");
					spreadf.write(spcheck[i].value.asString ++ "\n");
					diffusef.write(dfcheck[i].value.asString ++ "\n");
					ncanf.write(ncanbox[i].value.asString ++ "\n");
					businif.write(businibox[i].value.asString ++ "\n");
					stcheckf.write(stcheck[i].value.asString ++ "\n");

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
			var title = "Load: select automation directory", onSuccess, onFailure = nil,
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


		// seleção de fontes
		itensdemenu = Array.newClear(nfontes);
		nfontes.do { | i |
			itensdemenu[i] = "Source " ++ (i + 1).asString;
		};

		sourceName = StaticText(win, Rect(10, 10, 150, 20)).string = "Source 1";

		sourceSelect = { | item |
			currentsource = item.value;
			sourceName.string = itensdemenu[currentsource];
			updateGuiCtl.value(\chan);

			loopcheck.value = lp[currentsource].value;

			updateGuiCtl.value(\lib, libboxProxy[currentsource].value);

			updateGuiCtl.value(\dstrv, dstrvboxProxy[currentsource].value);

			updateGuiCtl.value(\src);

			winCtl[0][0].value = level[currentsource];
			winCtl[1][0].value = level[currentsource] * 0.5;
			winCtl[0][1].value = dplev[currentsource];
			winCtl[1][1].value = dplev[currentsource];
			winCtl[1][2].value = clev[currentsource];
			winCtl[1][2].value = clev[currentsource];
			winCtl[0][3].value = glev[currentsource];
			winCtl[1][3].value = glev[currentsource];

			zslider.value = (zlev[currentsource] * 0.01 + 1) * 0.5;
			znumbox.value = zlev[currentsource];

			auxslider1.value = aux1[currentsource];
			aux1numbox.value = aux1[currentsource];
			auxslider2.value = aux2[currentsource];
			aux2numbox.value = aux2[currentsource];
			auxslider3.value = aux3[currentsource];
			aux3numbox.value = aux3[currentsource];
			auxslider4.value = aux4[currentsource];
			aux4numbox.value = aux4[currentsource];
			auxslider5.value = aux5[currentsource];
			aux5numbox.value = aux5[currentsource];

			if(audit[currentsource]) {
				// don't change button if we are playing via automation
				// only if it is being played/streamed manually
				//	if (synt[currentsource] == nil){
				//		baudi.value = 0;
				//	} {
				baudi.value = 1;
				//	};
			} {
				baudi.value = 0;
			};
		};

		//offset = 60;

		hwInCheck = CheckBox( win, Rect(10, 30, 100, 20), "HW-in").action_({ | butt |
			{hwncheck[currentsource].valueAction = butt.value;}.defer;
		});

		scInCheck = CheckBox( win, Rect(85, 30, 60, 20), "SC-in").action_({ | butt |
			{scncheck[currentsource].valueAction = butt.value;}.defer;
		});


		loopcheck = CheckBox( win, Rect(10, 70, 80, 20), "Loop").action_({ | butt |
			{lpcheck[currentsource].valueAction = butt.value;}.defer;
		});
		loopcheck.value = false;


		/////////////////////////////////////////////////////////

		hwCtl = Array.newClear(2); // [0]numboxes, [1]texts
		hwCtl[0] = Array.newClear(2);
		hwCtl[1] = Array.newClear(2);

		hwCtl[1][0] = StaticText(win, Rect(55, 50, 200, 20));
		hwCtl[1][0].string = "Nb. of chans.";
		hwCtl[1][0].visible = false;
		hwCtl[0][0] = NumberBox(win, Rect(10, 50, 40, 20));
		hwCtl[0][0].value = 0;
		hwCtl[0][0].clipHi = 36;
		hwCtl[0][0].clipLo = 0;
		hwCtl[0][0].align = \center;
		hwCtl[0][0].action = { | num |
			{ncanbox[currentsource].valueAction = num.value;}.defer;
			ncan[currentsource] = num.value;
		};
		hwCtl[0][0].visible = false;

		hwCtl[1][1] = StaticText(win, Rect(55, 70, 240, 20));
		hwCtl[1][1].string = "Start Bus";
		hwCtl[1][1].visible = false;
		hwCtl[0][1] = NumberBox(win, Rect(10, 70, 40, 20));
		hwCtl[0][1].value = 0;
		hwCtl[0][1].clipLo = 0;
		hwCtl[0][1].align = \center;
		hwCtl[0][1].action = { | num |
			{businibox[currentsource].valueAction = num.value;}.defer;
			busini[currentsource] = num.value;
		};
		hwCtl[0][1].visible = false;


		/////////////////////////////////////////////////////////


		textbuf = StaticText(win, Rect(163, 110, 240, 20));
		textbuf.string = "Library";
		libnumbox = PopUpMenu( win, Rect(10, 110, 150, 20));
		libnumbox.items = spatList;
		libnumbox.action_({ | num |
			{libbox[currentsource].valueAction = num.value;}.defer;
		});
		libnumbox.value = 0;


		/////////////////////////////////////////////////////////


		zAxis = StaticText(win, Rect(width - 80, halfwidth - 10, 90, 20));
		zAxis.string = "Z-Axis";
		znumbox = NumberBox(win, Rect(width - 45, ((width - zSliderHeight) * 0.5)
			+ zSliderHeight, 40, 20));
		znumbox.value = 0;
		znumbox.decimals = 1;
		znumbox.clipHi = 100;
		znumbox.clipLo = -100;
		znumbox.step_(0.1);
		znumbox.scroll_step_(0.1);
		znumbox.align = \center;
		znumbox.action = { | num |
			{ zslider.value = (num.value * 0.005) + 0.5;
				moveSource.value(sprite[currentsource, 0], sprite[currentsource, 1])
			}.defer;
		};

		zslider = Slider.new(win, Rect(width - 35, ((width - zSliderHeight) * 0.5),
			20, zSliderHeight));
		zslider.value = 0.5;
		zslider.action = { | num |
			{ znumbox.valueAction = num.value - 0.5 * 200; }.defer;
		};


		////////////////////////////// Orientation //////////////


		originView = UserView(win, Rect(width - 285, height - 85, 265, 100));

		originCtl = Array.newClear(2); // [0]numboxes, [1]texts
		originCtl[0] = Array.newClear(8);
		originCtl[1] = Array.newClear(8);

		originCtl[0][2] = NumberBox(originView, Rect(230, 20, 40, 20));
		originCtl[0][2].align = \center;
		originCtl[0][2].clipHi = pi;
		originCtl[0][2].clipLo = -pi;
		originCtl[0][2].step_(0.01);
		originCtl[0][2].scroll_step_(0.01);

		originCtl[0][3] = NumberBox(originView, Rect(230, 40, 40, 20));
		originCtl[0][3].align = \center;
		originCtl[0][3].clipHi = pi;
		originCtl[0][3].clipLo = -pi;
		originCtl[0][3].step_(0.01);
		originCtl[0][3].scroll_step_(0.01);

		originCtl[0][4] = NumberBox(originView, Rect(230, 60, 40, 20));
		originCtl[0][4].align = \center;
		originCtl[0][4].clipHi = pi;
		originCtl[0][4].clipLo = -pi;
		originCtl[0][4].step_(0.01);
		originCtl[0][4].scroll_step_(0.01);


		originCtl[0][2].action = { | num |
			headingnumboxProxy.valueAction = num.value;
		};

		originCtl[0][3].action = { | num |
			pitchnumboxProxy.valueAction = num.value;
		};

		originCtl[0][4].action = { | num |
			rollnumboxProxy.valueAction = num.value;
		};

		originCtl[1][2] = StaticText(originView, Rect(215, 20, 12, 22));
		originCtl[1][2].string = "H:";
		originCtl[1][3] = StaticText(originView, Rect(215, 40, 12, 22));
		originCtl[1][3].string = "P:";
		originCtl[1][4] = StaticText(originView, Rect(215, 60, 12, 22));
		originCtl[1][4].string = "R:";

		textbuf = StaticText(originView, Rect(227, 0, 45, 20));
		textbuf.string = "Orient.";

		originCtl[0][5] = NumberBox(originView, Rect(170, 20, 40, 20));
		originCtl[0][5].align = \center;
		originCtl[0][5].step_(0.1);
		originCtl[0][5].scroll_step_(0.1);

		originCtl[0][6] = NumberBox(originView, Rect(170, 40, 40, 20));
		originCtl[0][6].align = \center;
		originCtl[0][6].step_(0.1);
		originCtl[0][6].scroll_step_(0.1);

		originCtl[0][7] = NumberBox(originView, Rect(170, 60, 40, 20));
		originCtl[0][7].align = \center;
		originCtl[0][7].step_(0.1);
		originCtl[0][7].scroll_step_(0.1);

		originCtl[0][5].action = { | num |
			oxnumboxProxy.valueAction = num.value;
		};

		originCtl[0][6].action = { | num |
			oynumboxProxy.valueAction = num.value;
		};

		originCtl[0][7].action = { | num |
			oznumboxProxy.valueAction = num.value;
		};

		originCtl[1][5] = StaticText(originView, Rect(155, 20, 12, 22));
		originCtl[1][5].string = "X:";
		originCtl[1][6] = StaticText(originView, Rect(155, 40, 12, 22));
		originCtl[1][6].string = "Y:";
		originCtl[1][7] = StaticText(originView, Rect(155, 60, 12, 22));
		originCtl[1][7].string = "Z:";

		textbuf = StaticText(originView, Rect(170, 0, 47, 20));
		textbuf.string = "Origin";


		////////////////////////////////////////////////////////////


		winCtl[2][0] = StaticText(win, Rect(163, 130, 50, 20));
		winCtl[2][0].string = "Level";
		winCtl[0][0] = NumberBox(win, Rect(10, 130, 40, 20));
		winCtl[0][0].value = 1;
		winCtl[0][0].clipHi = 2;
		winCtl[0][0].clipLo = 0;
		winCtl[0][0].step_(0.01);
		winCtl[0][0].scroll_step_(0.01);
		winCtl[0][0].align = \center;
		winCtl[0][0].action = { | num |
			{vbox[currentsource].valueAction = num.value;}.defer;
		};

		winCtl[1][0] = Slider(win, Rect(50, 130, 110, 20));
		winCtl[1][0].value = 0.5;
		winCtl[1][0].action = { | num |
			{vbox[currentsource].valueAction = num.value * 2;}.defer;
		};


		///////////////////////////////////////////////////////////////


		winCtl[2][1] = StaticText(win, Rect(163, 150, 120, 20));
		winCtl[2][1].string = "Doppler amount";
		winCtl[0][1] = NumberBox(win, Rect(10, 150, 40, 20));
		winCtl[0][1].value = 0;
		winCtl[0][1].clipHi = 1;
		winCtl[0][1].clipLo = 0;
		winCtl[0][1].step_(0.01);
		winCtl[0][1].scroll_step_(0.01);
		winCtl[0][1].align = \center;
		winCtl[0][1].action = { | num |
			{dpbox[currentsource].valueAction = num.value;}.defer;
		};

		winCtl[1][1] = Slider(win, Rect(50, 150, 110, 20));
		winCtl[1][1].value = 0;
		winCtl[1][1].action = { | num |
			{dpbox[currentsource].valueAction = num.value;}.defer;
		};


		/////////////////////////////////////////////////////////////////////////


		winCtl[2][2] = StaticText(win, Rect(163, 170, 170, 20));
		winCtl[2][2].string = "Contraction";
		winCtl[0][2] = NumberBox(win, Rect(10, 170, 40, 20));
		winCtl[0][2].value = 1;
		winCtl[0][2].clipHi = 1;
		winCtl[0][2].clipLo = 0;
		winCtl[0][2].step_(0.01);
		winCtl[0][2].scroll_step_(0.01);
		winCtl[0][2].align = \center;
		winCtl[0][2].action = { | num |
			{cbox[currentsource].valueAction = num.value;}.defer;
		};

		winCtl[1][2] = Slider.new(win, Rect(50, 170, 110, 20));
		winCtl[1][2].value = 1;
		winCtl[1][2].action = { | num |
			{cbox[currentsource].valueAction = num.value;}.defer;
		};


		/////////////////////////////////////////////////////////////////////////


		textbuf = StaticText(originView, Rect(136, 0, 20, 20));
		textbuf.string = "M";

		masterslider = Slider.new(originView, Rect(132, 20, 20, 60));
		masterslider.orientation(\vertical);
		masterslider.value = 0.5;
		masterslider.action = { | num |
			masterlevProxy.valueAction = num.value * 2;
		};

		/////////////////////////////////////////////////////////////////////////


		textbuf = StaticText(originView, Rect(0, 0, 150, 20));
		textbuf.string = "Cls. Reverb";
		clsReverbox = PopUpMenu(originView, Rect(0, 20, 130, 20));
		clsReverbox.items = ["no-reverb",
			"freeverb",
			"allpass"] ++ rirList;
		// add the list of impule response if one is provided by rirBank

		clsReverbox.action_({ | num |
			clsrvboxProxy.valueAction = num.value;
		});
		clsReverbox.value = 0;


		/////////////////////////////////////////////////////////////////////////


		originCtl[0][0] = StaticText(originView, Rect(43, 40, 100, 20));
		originCtl[0][0].string = "room/delay";
		originCtl[0][0].visible = false;
		originCtl[1][0] = NumberBox(originView, Rect(0, 40, 40, 20));
		originCtl[1][0].value = 0.5;
		originCtl[1][0].clipHi = 1;
		originCtl[1][0].clipLo = 0;
		originCtl[1][0].step_(0.01);
		originCtl[1][0].scroll_step_(0.01);
		originCtl[1][0].align = \center;
		originCtl[1][0].action = { | num |
			{clsrmboxProxy.valueAction = num.value;}.defer;
		};
		originCtl[1][0].visible = false;


		/////////////////////////////////////////////////////////////////////////


		originCtl[0][1] = StaticText(originView, Rect(43, 60, 100, 20));
		originCtl[0][1].string = "damp/decay";
		originCtl[0][1].visible = false;
		originCtl[1][1] = NumberBox(originView, Rect(0, 60, 40, 20));
		originCtl[1][1].value = 0.5;
		originCtl[1][1].clipHi = 1;
		originCtl[1][1].clipLo = 0;
		originCtl[1][1].step_(0.01);
		originCtl[1][1].scroll_step_(0.01);
		originCtl[1][1].align = \center;
		originCtl[1][1].action = { | num |
			{clsdmboxProxy.valueAction = num.value;}.defer;
		};
		originCtl[1][1].visible = false;


		/////////////////////////////////////////////////////////////////////////


		textbuf = StaticText(win, Rect(163, 190, 150, 20));
		textbuf.string = "Distant Reverb";
		dstReverbox = PopUpMenu(win, Rect(10, 190, 150, 20));
		dstReverbox.items = ["no-reverb", "freeverb", "allpass"] ++ rirList;
		// add the list of impule response if one is provided

		dstReverbox.action_({ | num |
			{dstrvbox[currentsource].valueAction = num.value;}.defer;
		});
		dstReverbox.value = 0;


		/////////////////////////////////////////////////////////////////////////


		winCtl[2][4] = StaticText(win, Rect(163, 210, 150, 20));
		winCtl[2][4].string = "Dst. amount";
		winCtl[2][4].visible = false;
		winCtl[0][4] = NumberBox(win, Rect(10, 210, 40, 20));
		winCtl[0][4].value = 0;
		winCtl[0][4].clipHi = 1;
		winCtl[0][4].clipLo = 0;
		winCtl[0][4].step_(0.01);
		winCtl[0][4].scroll_step_(0.01);
		winCtl[0][4].align = \center;
		winCtl[0][4].action = { | num |
			{lbox[currentsource].valueAction = num.value;}.defer;
		};
		winCtl[0][4].visible = false;

		winCtl[1][4] = Slider(win, Rect(50, 210, 110, 20));
		winCtl[1][4].value = 0;
		winCtl[1][4].action = { | num |
			{lbox[currentsource].valueAction = num.value;}.defer;
		};
		winCtl[1][4].visible = false;


		/////////////////////////////////////////////////////////////////////////


		winCtl[2][5] = StaticText(win, Rect(163, 230, 150, 20));
		winCtl[2][5].string = "Dst. room/delay";
		winCtl[2][5].visible = false;
		winCtl[0][5] = NumberBox(win, Rect(10, 230, 40, 20));
		winCtl[0][5].value = 0.5;
		winCtl[0][5].clipHi = 1;
		winCtl[0][5].clipLo = 0;
		winCtl[0][5].step_(0.01);
		winCtl[0][5].scroll_step_(0.01);
		winCtl[0][5].align = \center;
		winCtl[0][5].action = { | num |
			{rmbox[currentsource].valueAction = num.value;}.defer;
		};
		winCtl[0][5].visible = false;

		winCtl[1][5] = Slider.new(win, Rect(50, 230, 110, 20));
		winCtl[1][5].value = 0.5;
		winCtl[1][5].action = { | num |
			{rmbox[currentsource].valueAction = num.value;}.defer;
		};
		winCtl[1][5].visible = false;


		/////////////////////////////////////////////////////////////////////////


		winCtl[2][6] = StaticText(win, Rect(163, 250, 150, 20));
		winCtl[2][6].string = "Dst. damp/decay";
		winCtl[2][6].visible = false;
		winCtl[0][6] = NumberBox(win, Rect(10, 250, 40, 20));
		winCtl[0][6].value = 0.5;
		winCtl[0][6].clipHi = 1;
		winCtl[0][6].clipLo = 0;
		winCtl[0][6].step_(0.01);
		winCtl[0][6].scroll_step_(0.01);
		winCtl[0][6].align = \center;
		winCtl[0][6].action = { | num |
			{dmbox[currentsource].valueAction = num.value;}.defer;
		};
		winCtl[0][6].visible = false;

		winCtl[1][6] = Slider.new(win, Rect(50, 250, 110, 20));
		winCtl[1][6].value = 0.5;
		winCtl[1][6].action = { | num |
			{dmbox[currentsource].valueAction = num.value;}.defer;
		};
		winCtl[1][6].visible = false;


		/////////////////////////////////////////////////////////////////////////


		winCtl[2][3] = StaticText(win, Rect(163, 270, 150, 20));
		winCtl[2][3].string = "Cls. amount";
		winCtl[2][3].visible = false;
		winCtl[0][3] = NumberBox(win, Rect(10, 270, 40, 20));
		winCtl[0][3].value = 0;
		winCtl[0][3].clipHi = 1;
		winCtl[0][3].clipLo = 0;
		winCtl[0][3].step_(0.01);
		winCtl[0][3].scroll_step_(0.01);
		winCtl[0][3].align = \center;
		winCtl[0][3].action = { | num |
			{gbox[currentsource].valueAction = num.value;}.defer;
		};
		winCtl[0][3].visible = false;

		winCtl[1][3] = Slider.new(win, Rect(50, 270, 110, 20));
		winCtl[1][3].value = 0;
		winCtl[1][3].action = { | num |
			{gbox[currentsource].valueAction = num.value;}.defer;
		};
		winCtl[1][3].visible = false;


		/////////////////////////////////////////////////////////////////////////


		winCtl[2][7] = StaticText(win, Rect(163, 290, 100, 20));
		winCtl[2][7].string = "Stereo Angle";
		winCtl[2][7].visible = false;
		winCtl[0][7] = NumberBox(win, Rect(10, 290, 40, 20));
		winCtl[0][7].value = 1.0471975511966;
		winCtl[0][7].clipHi = pi;
		winCtl[0][7].clipLo = 0;
		winCtl[0][7].step_(0.01);
		winCtl[0][7].scroll_step_(0.01);
		winCtl[0][7].align = \center;
		winCtl[0][7].action = { | num |
			{abox[currentsource].valueAction = num.value;}.defer;
			if((ncanais[currentsource] == 2) || (ncan[currentsource] == 2)) {
				espacializador[currentsource].set(\angle, num.value);
				this.setSynths(currentsource, \angle, num.value);
				angle[currentsource] = num.value;
			};
		};
		winCtl[0][7].visible = false;

		winCtl[1][7] = Slider.new(win, Rect(50, 290, 110, 20));
		//	b = ControlSpec(0.0, 3.14, \linear, 0.01); // min, max, mapping, step
		winCtl[1][7].value = 1.0471975511966 / pi;
		winCtl[1][7].action = { | num |
			{abox[currentsource].valueAction = num.value * pi;}.defer;
			if((ncanais[currentsource] == 2) || (ncan[currentsource] == 2)) {
				//			espacializador[currentsource].set(\angle,
				//b.map(num.value));
				espacializador[currentsource].set(\angle, num.value * pi);
				this.setSynths(currentsource, \angle, num.value * pi);
				//			angle[currentsource] = b.map(num.value);
				angle[currentsource] = num.value * pi;
			};
		};
		winCtl[1][7].visible = false;


		/////////////////////////////////////////////////////////////////////////


		winCtl[2][8] = StaticText(win, Rect(163, 290, 150, 20));
		winCtl[2][8].string = "B-Format Rotation";
		winCtl[2][8].visible = false;
		winCtl[0][8] = NumberBox(win, Rect(10, 290, 40, 20));
		winCtl[0][8].value = 0;
		winCtl[0][8].clipHi = pi;
		winCtl[0][8].clipLo = -pi;
		winCtl[0][8].step_(0.01);
		winCtl[0][8].scroll_step_(0.01);
		winCtl[0][8].align = \center;
		winCtl[0][8].action = { | num |
			{rbox[currentsource].valueAction = num.value;}.defer;
		};
		winCtl[0][8].visible = false;

		winCtl[1][8] = Slider.new(win, Rect(50, 290, 110, 20));
		winCtl[1][8].value = 0.5;
		winCtl[1][8].action = { | num |
			{rbox[currentsource].valueAction = num.value * 2pi - pi;}.defer;
		};
		winCtl[1][8].visible = false;


		/////////////////////////////////////////////////////////////////////////


		winCtl[2][9] = StaticText(win, Rect(163, 310, 150, 20));
		winCtl[2][9].string = "B-Format Directivity";
		winCtl[2][9].visible = false;
		winCtl[0][9] = NumberBox(win, Rect(10, 310, 40, 20));
		winCtl[0][9].value = 0;
		winCtl[0][9].clipHi = pi * 0.5;
		winCtl[0][9].clipLo = 0;
		winCtl[0][9].step_(0.01);
		winCtl[0][9].scroll_step_(0.01);
		winCtl[0][9].align = \center;
		winCtl[0][9].action = { | num |
			{dbox[currentsource].valueAction = num.value;}.defer;
		};
		winCtl[0][9].visible = false;

		winCtl[1][9] = Slider.new(win, Rect(50, 310, 110, 20));
		winCtl[1][9].value = 0;
		winCtl[1][9].action = { | num |
			{dbox[currentsource].valueAction = num.value * pi * 0.5;}.defer;
		};
		winCtl[1][9].visible = false;


		/////////////////////////////////////////////////////////////////////////


		spreadcheck = CheckBox( win, Rect(10, 330, 80, 20), "Spread").action_({  | butt |
			{spcheck[currentsource].valueAction = butt.value;}.defer;
		});
		spreadcheck.value = false;
		spreadcheck.visible = false;


		diffusecheck = CheckBox( win, Rect(90, 330, 80, 20), "Diffuse").action_({ | butt |
			{dfcheck[currentsource].valueAction = butt.value;}.defer;
		});
		diffusecheck.value = false;
		diffusecheck.visible = false;


		/////////////////////////////////////////////////////////////////////////


		winCtl[2][10] = StaticText(win, Rect(163, 330, 200, 20));
		winCtl[2][10].string = "Grain rate";
		winCtl[2][10].visible = false;
		winCtl[0][10] = NumberBox(win, Rect(10, 330, 40, 20));
		winCtl[0][10].value = 10;
		winCtl[0][10].clipHi = 60;
		winCtl[0][10].clipLo = 1;
		winCtl[0][10].step_(0.01);
		winCtl[0][10].scroll_step_(0.01);
		winCtl[0][10].align = \center;
		winCtl[0][10].action = { | num |
			{ratebox[currentsource].valueAction = num.value;}.defer;
		};
		winCtl[0][10].visible = false;

		winCtl[1][10] = Slider.new(win, Rect(50, 330, 110, 20));
		winCtl[1][10].value = 0.15254237288136;
		winCtl[1][10].action = { | num |
			{ratebox[currentsource].valueAction = (num.value * 59) + 1;}.defer;
		};
		winCtl[1][10].visible = false;


		/////////////////////////////////////////////////////////////////////////


		winCtl[2][11] = StaticText(win, Rect(163, 350, 200, 20));
		winCtl[2][11].string = "Window size";
		winCtl[2][11].visible = false;
		winCtl[0][11] = NumberBox(win, Rect(10, 350, 40, 20));
		winCtl[0][11].value = 0.1;
		winCtl[0][11].clipHi = 0.2;
		winCtl[0][11].clipLo = 0;
		winCtl[0][11].step_(0.01);
		winCtl[0][11].scroll_step_(0.01);
		winCtl[0][11].align = \center;
		winCtl[0][11].action = { | num |
			{winbox[currentsource].valueAction = num.value;}.defer;
		};
		winCtl[0][11].visible = false;

		winCtl[1][11] = Slider.new(win, Rect(50, 350, 110, 20));
		winCtl[1][11].value = 0.5;
		winCtl[1][11].action = { | num |
			{winbox[currentsource].valueAction = num.value * 0.2;}.defer;
		};
		winCtl[1][11].visible = false;


		/////////////////////////////////////////////////////////


		winCtl[2][12] = StaticText(win, Rect(163, 370, 200, 20));
		winCtl[2][12].string = "Rand. win.";
		winCtl[2][12].visible = false;
		winCtl[0][12] = NumberBox(win, Rect(10, 370, 40, 20));
		winCtl[0][12].value = 0;
		winCtl[0][12].clipHi = 1;
		winCtl[0][12].clipLo = 0;
		winCtl[0][12].step_(0.01);
		winCtl[0][12].scroll_step_(0.01);
		winCtl[0][12].align = \center;
		winCtl[0][12].action = { | num |
			{randbox[currentsource].valueAction = num.value;}.defer;
		};
		winCtl[0][12].visible = false;

		winCtl[1][12] = Slider(win, Rect(50, 370, 110, 20));
		winCtl[1][12].value = 0;
		winCtl[1][12].action = { | num |
			{randbox[currentsource].valueAction = num.value.squared;}.defer;
		};
		winCtl[1][12].visible = false;


		/////////////////////////////////////////////////////////



		bload = Button(win, Rect(10, 50, 75, 20))
		.states_([
			["load", Color.black, Color.white],
		]).action_({ | but |
			loadOrStream.value(false);
		});

		bstream = Button(win, Rect(85, 50, 75, 20))
		.states_([
			["stream", Color.black, Color.white],
		]).action_({ | but |
			loadOrStream.value(true);
		});

		loadOrStream = { | str |
			synt[currentsource].free; // error check
			espacializador[currentsource].free;

			Dialog.openPanel(
				control.stopRecording;
				ossiarec.v_(false);
				{ | path |
					{
						if (str) {
							streamdisk[currentsource] = true;
							stcheck[currentsource].valueAction = true;
						};
						tfieldProxy[currentsource].valueAction = path;}.defer;
				},
				{
					"cancelled".postln;
					streamdisk[currentsource] = false;
					{tfieldProxy[currentsource].value = "";}.defer;
					{tfield[currentsource].value = "";}.defer;
					stcheck[currentsource].valueAction = false;
				};
			);
		};

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


		nfontes.do { | i |

			textbuf = StaticText(wdados, Rect(2, 40 + (i*20), 50, 20));
			textbuf.font = Font(Font.defaultSansFace, 9);
			textbuf.string = (i+1).asString;

			libbox[i] = NumberBox(wdados, Rect(20, 40 + (i*20), 25, 20));

			dstrvbox[i] = NumberBox.new(wdados, Rect(45, 40 + (i*20), 25, 20));

			lpcheck[i] = CheckBox.new(wdados, Rect(70, 40 + (i*20), 40, 20));

			lpcheck[i].action_({ | but |
				lpcheckProxy[i].valueAction = but.value;
			});

			hwncheck[i] = CheckBox.new( wdados, Rect(85, 40 + (i*20), 40, 20));

			hwncheck[i].action_({ | but |
				hwncheckProxy[i].valueAction = but.value;
			});


			scncheck[i] = CheckBox.new( wdados, Rect(100, 40 + (i*20), 40, 20));

			scncheck[i].action_({ | but |
				scncheckProxy[i].valueAction = but.value;
			});

			spcheck[i] = CheckBox.new(wdados, Rect(115, 40 + (i*20), 40, 20));

			spcheck[i].action_({ | but |
				spcheckProxy[i].valueAction = but.value;
			});



			dfcheck[i] = CheckBox.new(wdados, Rect(130, 40 + (i*20), 40, 20));

			dfcheck[i].action_({ | but |
				dfcheckProxy[i].valueAction = but.value;
			});


			/////////////////////////////////////////////////////////////////


			ncanbox[i] = NumberBox(wdados, Rect(145, 40 + (i*20), 25, 20));
			businibox[i] = NumberBox(wdados, Rect(170, 40 + (i*20), 25, 20));

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
				a1checkProxy[i].valueAction = but.value;
			};



			a2check[i] = CheckBox.new( wdados, Rect(765, 40 + (i*20), 40, 20));
			a2check[i].action = { | but |
				a2checkProxy[i].valueAction = but.value;
			};


			a3check[i] = CheckBox.new( wdados, Rect(780, 40 + (i*20), 40, 20));
			a3check[i].action = { | but |
				a3checkProxy[i].valueAction = but.value;
			};


			a4check[i] = CheckBox.new( wdados, Rect(795, 40 + (i*20), 40, 20));
			a4check[i].action = { | but |
				a4checkProxy[i].valueAction = but.value;
			};


			a5check[i] = CheckBox.new( wdados, Rect(810, 40 + (i*20), 40, 20));
			a5check[i].action = { | but |
				a5checkProxy[i].valueAction = but.value;
			};



			tfield[i] = TextField(wdados, Rect(825, 40+ (i*20), 100, 20));
			//tfield[i] = TextField(wdados, Rect(720, 40+ (i*20), 220, 20));

			stcheck[i] = CheckBox.new( wdados, Rect(925, 40 + (i*20), 40, 20));
			stcheck[i].action = { | but |
				stcheckProxy[i].valueAction = but.value;
			};

			textbuf = StaticText(wdados, Rect(940, 40 + (i*20), 50, 20));
			textbuf.font = Font(Font.defaultSansFace, 9);
			textbuf.string = (i+1).asString;

			libbox[i].font = Font(Font.defaultSansFace, 9);
			dstrvbox[i].font = Font(Font.defaultSansFace, 9);
			ncanbox[i].font = Font(Font.defaultSansFace, 9);
			businibox[i].font = Font(Font.defaultSansFace, 9);
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

			tfield[i].font = Font(Font.defaultSansFace, 9);

			xbox[i].decimals = 3;
			ybox[i].decimals = 3;
			zbox[i].decimals = 3;
			//oxbox[i].decimals = 0;
			//oybox[i].decimals = 0;
			//ozbox[i].decimals = 0;


			a1box[i].action = { | num |
				a1boxProxy[i].valueAction = num.value;
			};


			a2box[i].action = { | num |
				a2boxProxy[i].valueAction = num.value;
			};


			a3box[i].action = { | num |
				a3boxProxy[i].valueAction = num.value;
			};

			a4box[i].action = { | num |
				a4boxProxy[i].valueAction = num.value;
			};

			a5box[i].action = { | num |
				a5boxProxy[i].valueAction = num.value;
			};



			tfield[i].action = { | path |
				tfieldProxy[i].valueAction = path.value;
			};




			//// PROXY ACTIONS /////////

			// gradually pinching these and putting up above

			xbox[i].action = { | num |
				xboxProxy[i].valueAction = num.value;
			};


			ybox[i].action = { | num |
				yboxProxy[i].valueAction = num.value;
			};
			ybox[i].value = 200;


			zbox[i].action = { | num |
				zboxProxy[i].valueAction = num.value;
			};




			abox[i].clipHi = pi;
			abox[i].clipLo = 0;
			vbox[i].clipHi = 2.0;
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


			libbox[i].action = { | num |
				libboxProxy[i].valueAction = num.value;
			};
			libbox[i].value = 0;

			dstrvbox[i].action = { | num |
				dstrvboxProxy[i].valueAction = num.value;
			};
			dstrvbox[i].value = 0;

			abox[i].action = { | num |
				aboxProxy[i].valueAction = num.value;
			};

			vbox[i].action = { | num |
				vboxProxy[i].valueAction = num.value;
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
				gboxProxy[i].valueAction = num.value;
			};

			lbox[i].action = { | num |
				lboxProxy[i].valueAction = num.value;
			};

			rmbox[i].action = { | num |
				rmboxProxy[i].valueAction = num.value;
			};

			dmbox[i].action = { | num |
				dmboxProxy[i].valueAction = num.value;
			};

			rbox[i].action = { | num |
				rboxProxy[i].valueAction = num.value;
			};


			dbox[i].action = { | num |
				dboxProxy[i].valueAction = num.value;
			};


			cbox[i].action = { | num |
				cboxProxy[i].valueAction = num.value;
			};


			dpbox[i].action = { | num |
				dpboxProxy[i].valueAction = num.value;
			};


			ncanbox[i].action = { | num |
				ncanboxProxy[i].valueAction = num.value;
			};


			businibox[i].action = { | num |
				businiboxProxy[i].valueAction = num.value;
			};

			ratebox[i].action = { | num |
				rateboxProxy[i].valueAction = num.value;
			};

			winbox[i].action = { | num |
				winboxProxy[i].valueAction = num.value;
			};

			randbox[i].action = { | num |
				randboxProxy[i].valueAction = num.value;
			};



		};


		runTriggers = {
			nfontes.do({ | i |
				if(audit[i].not) {
					if(triggerFunc[i].notNil) {
						triggerFunc[i].value;
						//updateSynthInArgs.value(i);
					}
				}
			})
		};

		runTrigger = { | source, dirrect = false |
			//	if(scncheck[i]) {
			if(triggerFunc[source].notNil) {
				triggerFunc[source].value;
				if (dirrect && synt[source].isNil
					&& (spheval[source].rho < 1)) {
					this.newtocar(source, 0, force: true);
				} {
					//updateSynthInArgs.value(source);
				};
				"RUNNING TRIGGER".postln;
			};
		};

		runStops = {
			nfontes.do({ | i |
				if(audit[i].not) {
					if(stopFunc[i].notNil) {
						stopFunc[i].value;
					}
				}
			})
		};

		runStop = { | source, dirrect = false |
			if(stopFunc[source].notNil) {
				stopFunc[source].value;
				if (dirrect) {
					firstTime[source] = false;
					synt[source].free;
					synt[source] = nil;
				};
			}
		};


		//control = Automation(dur).front(win, Rect(halfwidth, 10, 400, 25));
		/*~autotest = control = Automation(dur, showLoadSave: false,
		showSnapshot: false,
		minTimeStep: 0.001).front(win,
		Rect(10, width - 80, 400, 22));
		*/
		//~autotest = control = Automation(dur, showLoadSave: false,
		//showSnapshot: false,
		//	minTimeStep: 0.001);
		control.front(autoView, Rect(0, 0, 325, 20));


		control.presetDir = prjDr ++ "/auto";
		//control.setMinTimeStep(2.0);
		control.onEnd = {
			//	control.stop;
			control.seek;
			if(autoloopval) {
				//control.play;
			};
			/*
			nfontes.do { arg i;
			if(synt[i].notNil) {
			synt[i].free;
			};
			};
			*/
		};

		control.onPlay = {
			var startTime;
			"ON PLAY".postln;


			/*nfontes.do { arg i;
			firstTime[i]=true;
			("NOW PLAYING = " ++ firstTime[i]).postln;*/
			if (looping) {
				nfontes.do { | i |
					firstTime[i]=true;
					//("HERE = " ++ firstTime[i]).postln;

				};
				looping = false;
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

			ossiaplay.v_(true);
			{win.refresh;}.defer;

		};


		control.onSeek = { |time|
			/*
			var wasplaying = isPlay;

			//("isPlay = " ++ isPlay).postln;
			//runStops.value; // necessary? doesn't seem to help prob of SC input

			//runStops.value;
			if(isPlay == true) {
			nfontes.do { arg i;
			if(audit[i].not) {
			synt[i].free;
			};
			};
			control.stop;
			};

			if(wasplaying) {
			{control.play}.defer(0.5); //delay necessary. may need more?
			};
			*/

			if (time == 0) {
				ossiaseekback = false;
				ossiatransport.v_(0);
				ossiaseekback = true;
			};

		};

		/*control.onStop = {
		runStops.value;
		"ON STOP".postln;
		nfontes.do { | i |
		// if sound is currently being "tested", don't switch off on stop
		// leave that for user
		if (audit[i] == false) {
		synt[i].free; // error check
		};
		//	espacializador[i].free;
		};
		isPlay = false;

		};
		*/

		control.onStop = {

			if(autoloopval.not) {
				//("Control now = " ++ control.now ++ " dur = " ++ dur).postln;
			};
			if(autoloopval.not || (control.now.round != dur)) {
				("I HAVE STOPPED. dur = " ++ dur ++ " now = " ++
					control.now).postln;
				runStops.value;
				nfontes.do { | i |
					// if sound is currently being "tested", don't switch off on stop
					// leave that for user
					if (audit[i] == false) {
						synt[i].free; // error check
					};
					//	espacializador[i].free;
				};
				isPlay = false;
				looping = false;
				nfontes.do { | i |
					firstTime[i]=true;
					//("HERE = " ++ firstTime[i]).postln;
				};

			} {
				( "Did not stop. dur = " ++ dur ++ " now = " ++
					control.now).postln;
				looping = true;
				control.play;
			};

			ossiaplay.v_(false);
			{win.refresh;}.defer;
		};


		furthest = halfheight * 20;

		sprite = Array2D.new(nfontes, 2);
		nfontes.do { | i |
			sprite.put(i, 0, 0);
			sprite.put(i, 1, furthest);
		};

		win.view.mouseDownAction = { | view, mx, my, modifiers, buttonNumber, clickCount |
			mouseButton = buttonNumber; // 0 = left, 2 = middle, 1 = right
			case
			{mouseButton == 0} {
				var closest = [0, furthest]; // save sources index and distance from click
				// initialize at the furthest point
				nfontes.do { | i |
					var dis = ((mx - sprite[i, 0].value).squared
						+ (my - sprite[i, 1].value).squared).sqrt;
					// claculate distance from click
					if(dis < closest[1].value) {
						closest[1] = dis;
						closest[0] = i;
					};
				};
				sourceSelect.value(closest[0].value);
				moveSource.value(mx, my);
			}
			{mouseButton == 1} {
				if (sourceList.isNil) {
					sourceList = ListView(win, Rect(mx,my,
						90,70)).items_(itensdemenu).value_(currentsource)
					.action_({ |sel|
						sourceSelect.value(sel.value);
						moveSource.value(mx + 45, my + 35);
						sourceList.close;
						sourceList = nil;
					});
				} {
					sourceList.close;
					sourceList = nil;
				};
			}
			{mouseButton == 2} {
				nfontes.do { | i |
					("" ++ i ++ " " ++ cartval[i]).postln;
					("" ++ i ++ " " ++ spheval[i]).postln;
					if(espacializador[i].notNil) {

						espacializador[i].set(\azim, spheval[i].theta,
							\elev, spheval[i].phi,
							\radius, spheval[i].rho);

						this.setSynths(i, \azim, spheval[i].theta,
							\elev, spheval[i].phi,
							\radius, spheval[i].rho);

						synt[i].set(\azim, spheval[i].theta,
							\elev, spheval[i].phi,
							\radius, spheval[i].rho);
					};
				};
			};
		};

		win.view.mouseMoveAction = {|view, mx, my, modifiers|
			if (mouseButton == 0) { // left button
				moveSource.value(mx, my);
			};
		};

		win.view.mouseWheelAction = {|view, mx, my, modifiers, dx, dy|
			if ((dy < 0) && (zoom_factor <= 10)) {
				zoom_factor = zoom_factor * 1.01;
				win.refresh;
			};

			if ((dy > 0) && (zoom_factor >= 0.55)) {
				zoom_factor = zoom_factor * 0.1;
				win.refresh;
			};
		};

		moveSource = { |x, y|

			var car2sphe = Cartesian((x - halfwidth) / halfheight,
				(halfheight - y) / halfheight,
				(zslider.value - 0.5) * 2 * zoom_factor);

			// save raw mouseposition for selecting closest source on click
			sprite.put(currentsource, 0, x);
			sprite.put(currentsource, 1, y);

			spheval[currentsource].rho_(car2sphe.rho);
			spheval[currentsource].theta_(car2sphe.theta);
			spheval[currentsource].phi_(car2sphe.phi);

			spheval[currentsource]  = (spheval[currentsource] / zoom_factor) * 100;

			ossiasphe[currentsource].v_([spheval[currentsource].rho,
				(spheval[currentsource].theta - halfPi).wrap(-pi, pi),
				spheval[currentsource].phi]);
		};

		win.view.onResize_({|view|

			width = view.bounds.width;
			halfwidth = width * 0.5;
			height = view.bounds.height;
			halfheight = height * 0.5;

			dialView.bounds_(Rect(width - 100, 10, 180, 150));

			zSliderHeight = height * 2 / 3;
			zslider.bounds_(Rect(width - 35, ((height - zSliderHeight) * 0.5),
				20, zSliderHeight));
			znumbox.bounds_(Rect(width - 45, ((height - zSliderHeight) * 0.5)
				+ zSliderHeight, 40, 20));
			zAxis.bounds_(Rect(width - 80, halfheight - 10, 90, 20));

			originView.bounds_(Rect(width - 275, height - 85, 270, 100));

			autoView.bounds_(Rect(10, height - 45, 325, 40));

			novoplot.value;

		});


		win.onClose_({

			wdados.close;
			waux.close;
			this.free;

		});


		mmcslave = CheckBox(autoView, Rect(163, 20, 140, 20),
			"Slave to MMC").action_({ | butt |
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

		autoloop = CheckBox(autoView, Rect(273, 20, 140, 20), "Loop").action_({
			| butt |
			//("Doppler is " ++ butt.value).postln;
			if(butt.value) {
				"Looping transport".postln;
				autoloopval = true;
			} {
				autoloopval = false;
			};

			ossiatrasportLoop.v_(butt.value);
		});

		autoloop.value = autoloopval;

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
					goto =  (sysex[7] * 3600) + (sysex[8] * 60) + sysex[9] +
					(sysex[10] / 30);
					control.seek(goto);

				};
			};
		};
		control.snapshot; // necessary to call at least once before saving automation
		// otherwise will get not understood errors on load
	}


}