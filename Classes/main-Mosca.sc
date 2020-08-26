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


Mosca {

	const fftsize = 2048;

	classvar server,
	rirWspectrum, rirXspectrum, rirYspectrum, rirZspectrum, rirA12Spectrum,
	rirFLUspectrum, rirFRDspectrum, rirBLDspectrum, rirBRUspectrum,
	rirList, irSpecPar, wxyzSpecPar, zSpecPar, wSpecPar,
	spatList, spatFuncs,
	// list of spat libs
	lastN3D = -1, // last N3D lib index
	lastFUMA = -1, // last FUMA lib index
	playList = #["File","HWBus","SWBus","Stream"],
	b2a, a2b, n2f, f2n,
	blips,
	maxorder,
	convert_fuma,
	convert_n3d,
	convert_direct,
	azimuths, radiusses, elevations,
	numoutputs,
	longest_radius, quarterRadius, twoAndaHalfRadius, highest_elevation, lowest_elevation,
	vbap_buffer,
	soa_a12_decoder_matrix, soa_a12_encoder_matrix,
	cart, spher, foa_a12_decoder_matrix,
	novoplot, updateGuiCtl,
	lastAutomation = nil,
	firstTime,
	isPlay = false,
	playingBF,
	currentsource, baudi,
	watcher, prjDr,
	foaEncoderOmni, foaEncoderSpread, foaEncoderDiffuse;

	var <nfontes,
	revGlobal, nonAmbi2FuMa, convertor,
	libnumbox, <>control,
	globDec,
	sysex, mmcslave,
	synthRegistry, busini, ncan,
	triggerFunc, stopFunc, subOutFunc, playInFunc, outPutFuncs,
	localReverbFunc,
	scInBus,
	fumabus,
	insertFlag,
	insertBus,
	<dur,
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
	<>oxnumboxProxy, <>oynumboxProxy, <>oznumboxProxy,
	<>pitchnumboxProxy, <>rollnumboxProxy, <>headingnumboxProxy,
	cartval, spheval,
	recchans, recbus,
	// mark1, mark2,	// 4 number arrays for marker data // apparently unused

	// MOVED FROM the the gui method/////////////////////////

	cbox, audit, gbus, gbfbus, n3dbus,
	outbus, suboutbus, gbixfbus, nonambibus,
	playEspacGrp, glbRevDecGrp,
	lib, convert, dstrv, dstrvtypes, clsrv,
	clsRvtypes,
	winCtl, originCtl, hwCtl,
	xbox, ybox, sombuf,
	rbox, abox, vbox, gbox, lbox, dbox, dpbox, zbox,
	a1check, a2check, a3check, a4check, a5check, a1box, a2box, a3box,
	a4box, a5box,
	stcheck,

	//oxbox, oybox, ozbox,
	//funcs, // apparently unused
	//lastx, lasty, // apparently unused
	zlev, znumbox, zslider, guiflag = false,
	lslider,
	auxslider1, auxslider2, auxslider3, auxslider4, auxslider5,
	auxbutton1, auxbutton2, auxbutton3, auxbutton4, auxbutton5,
	aux1numbox, aux2numbox, aux3numbox, aux4numbox, aux5numbox,

	loopcheck, dstReverbox, clsReverbox, hwInCheck,
	hwn, scInCheck, scn,
	spreadcheck,
	diffusecheck,

	hdtrk, hdtrkcheck,

	//<>runTriggers, <>runStops, <>runTrigger, <>runStop,
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

	ratebox, <>rateboxProxy, // setable granular rate
	winbox, <>winboxProxy, // setable granular window size
	randbox, <>randboxProxy,
	// setable granular window size random factor

	rmbox, <>rmboxProxy, // setable local room size
	dmbox, <>dmboxProxy, // setable local dampening

	clsrmbox, <>clsrmboxProxy, // setable global room size
	clsdmbox, <>clsdmboxProxy, // setable global dampening

	<>masterlevProxy, masterBox, masterslider,
	<>scalefactProxy, scaleBox, scaleslider,

	<parentOssiaNode,
	ossiaorient, ossiaorigine, <ossiaremotectl, ossiaplay, ossiatrasportLoop,
	ossiatransport, ossiaseekback, ossiarec, ossiacart, ossiasphe, ossiaaud,
	ossialoop, ossialib, ossialev, ossiadp, ossiacls, ossiaclsam, ossiaclsdel,
	ossiaclsdec, ossiadst, ossiadstam, ossiadstdel, ossiadstdec, ossiaangle,
	ossiarot, ossiadir, ossiactr, ossiaspread, ossiadiff, ossiaCartBack,
	ossiaSpheBack, ossiarate, ossiawin, ossiarand, ossiaAval, ossiaAchek,
	ossiascale, ossiamaster, ossiaMasterPlay, ossiaMasterLib, ossiaMasterRev;

	/////////////////////////////////////////

	*new { arg projDir, nsources = 10, dur = 180, rirBank,
		server = Server.local, parentOssiaNode = OSSIA_Device("SC"), allCrtitical = false, decoder,
		maxorder = 1, speaker_array, outbus = 0, suboutbus, rawformat = \FuMa, rawoutbus, autoloop = false;

		^super.new.initMosca(projDir, nsources, dur, rirBank, server, parentOssiaNode,
			allCrtitical, decoder, maxorder, speaker_array, outbus, suboutbus, rawformat,
			rawoutbus, autoloop);
	}


	initMosca { | projDir, nsources, idur, rirBank, iserver, iparentOssiaNode, allCrtitical,
		decoder, imaxorder, speaker_array, ioutbus, isuboutbus, rawformat, rawoutbus, iautoloop |

		var subOutFunc,
		perfectSphereFunc, bfOrFmh,
		bFormNumChan = (imaxorder + 1).squared,
		// add the number of channels of the b format
		fourOrNine; // switch between 4 fuma and 9 ch Matrix

		nfontes = nsources;
		maxorder = imaxorder;
		server = iserver;
		parentOssiaNode = iparentOssiaNode;

		b2a = FoaDecoderMatrix.newBtoA;
		a2b = FoaEncoderMatrix.newAtoB;
		n2f = FoaEncoderMatrix.newHoa1();
		f2n = FoaDecoderMatrix.newHoa1();
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

		n3dbus = Bus.audio(server, bFormNumChan); // global b-format ACN-SN3D bus
		fumabus = Bus.audio(server, fourOrNine);
		gbus = Bus.audio(server, 1); // global reverb bus
		gbfbus = Bus.audio(server, fourOrNine); // global b-format bus
		gbixfbus = Bus.audio(server, fourOrNine); // global n3d b-format bus
		playEspacGrp = ParGroup.tail;
		glbRevDecGrp = ParGroup.after(playEspacGrp);

		synthRegistry = Array.newClear(nfontes);
		insertFlag = Array.newClear(nfontes);
		insertBus = Array2D.new(2, nfontes);
		scInBus = Array.newClear(nfontes);

		nfontes.do { | i |
			synthRegistry[i] = List[];

			// insertBus[0, i] = Bus.audio(server, fourOrNine);
			// insertBus[1, i] = Bus.audio(server, fourOrNine);

			insertFlag[i] = 0;
		};

		dur = idur;
		outbus = ioutbus;
		suboutbus = isuboutbus;

		autoloopval = iautoloop;

		///////////////////// DECLARATIONS FROM gui /////////////////////


		espacializador = Array.newClear(nfontes);
		lib = Array.newClear(nfontes);
		dstrv = Array.newClear(nfontes);
		convert = Array.newClear(nfontes);
		dstrvtypes = Array.newClear(nfontes);
		hwn = Array.newClear(nfontes);
		scn = Array.newClear(nfontes);
		ncan = Array.newClear(nfontes);
		// 0 = não, nem estéreo. 1 = mono. 2 = estéreo.
		busini = Array.newClear(nfontes);
		// initial bus # in streamed audio grouping
		// (ie. mono, stereo or b-format)

		sombuf = Array.newClear(nfontes);
		//		xoffset = Array.fill(nfontes, 0);
		//		yoffset = Array.fill(nfontes, 0);
		synt = Array.newClear(nfontes);
		//sprite = Array2D.new(nfontes, 2);
		zlev = Array.newClear(nfontes);
		//	doplev = Array.newClear(nfontes);

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

		a1check = Array.newClear(nfontes); // aux - array of buttons in data window
		a2check = Array.newClear(nfontes); // aux - array of buttons in data window
		a3check = Array.newClear(nfontes); // aux - array of buttons in data window
		a4check = Array.newClear(nfontes); // aux - array of buttons in data window
		a5check = Array.newClear(nfontes); // aux - array of buttons in data window

		stcheck = Array.newClear(nfontes); // aux - array of buttons in data window

		firstTime = Array.newClear(nfontes);

		tfield = Array.newClear(nfontes);
		streamdisk = Array.newClear(nfontes);

		audit = Array.newClear(nfontes);

		origine = Cartesian();

		clsRvtypes = ""; // initialise close reverb type
		clsrv = 0;

		////////////////////////////////////////////////

		////////// ADDED NEW ARRAYS and other proxy stuff  //////////////////

		// these proxies behave like GUI elements. They eneable
		// the use of Automation without a GUI

		cartval = Array.fill(nfontes, {Cartesian(0, 20, 0)});
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
		masterlevProxy = AutomationGuiProxy(0);
		scalefactProxy = AutomationGuiProxy(1);
		clsrvboxProxy = AutomationGuiProxy(0);
		clsrmboxProxy = AutomationGuiProxy(0.5); // cls roomsize proxy
		clsdmboxProxy = AutomationGuiProxy(0.5); // cls dampening proxy

		oxnumboxProxy = AutomationGuiProxy(0.0);
		oynumboxProxy = AutomationGuiProxy(0.0);
		oznumboxProxy = AutomationGuiProxy(0.0);

		pitchnumboxProxy = AutomationGuiProxy(0.0);
		rollnumboxProxy = AutomationGuiProxy(0.0);
		headingnumboxProxy = AutomationGuiProxy(0.0);

		control = Automation(dur, showLoadSave: false, showSnapshot: true,
			minTimeStep: 0.001);


		server.sync;

		this.spatDef(maxorder, bFormNumChan, bfOrFmh, fourOrNine);


		////////////// DOCK PROXIES /////////////


		// this should be done after the actions are assigned


		nfontes.do { | i |

			lib[i] = 0;
			dstrv[i] = 0;
			convert[i] = false;
			dstrvtypes[i] = ""; // initialise distants reverbs types
			hwn[i] = false;
			scn[i] = false;
			zlev[i] = 0;

			streamdisk[i] = false;
			ncan[i] = 1;
			busini[i] = 0;
			audit[i] = false;
			playingBF[i] = false;
			firstTime[i] = true;

			rboxProxy[i] = AutomationGuiProxy(0.0);
			cboxProxy[i] = AutomationGuiProxy(1.0);
			aboxProxy[i] = AutomationGuiProxy(1.0471975511966);
			vboxProxy[i] = AutomationGuiProxy(0.0);
			gboxProxy[i] = AutomationGuiProxy(0.0);
			lboxProxy[i] = AutomationGuiProxy(0.0);
			rmboxProxy[i]= AutomationGuiProxy(0.5);
			dmboxProxy[i]= AutomationGuiProxy(0.5);
			dboxProxy[i] = AutomationGuiProxy(0.0);
			dpboxProxy[i] = AutomationGuiProxy(0.0);
			zboxProxy[i] = AutomationGuiProxy(0.0);
			yboxProxy[i] = AutomationGuiProxy(20.0);
			xboxProxy[i] = AutomationGuiProxy(0.0);
			a1checkProxy[i] = AutomationGuiProxy(false);
			a2checkProxy[i] = AutomationGuiProxy(false);
			a3checkProxy[i] = AutomationGuiProxy(false);
			a4checkProxy[i] = AutomationGuiProxy(false);
			a5checkProxy[i] = AutomationGuiProxy(false);
			a1boxProxy[i] = AutomationGuiProxy(0.0);
			a2boxProxy[i] = AutomationGuiProxy(0.0);
			a3boxProxy[i] = AutomationGuiProxy(0.0);
			a4boxProxy[i] = AutomationGuiProxy(0.0);
			a5boxProxy[i] = AutomationGuiProxy(0.0);

			hwncheckProxy[i] = AutomationGuiProxy(false);

			tfieldProxy[i] = AutomationGuiProxy("");
			libboxProxy[i] = AutomationGuiProxy(0);
			lpcheckProxy[i] = AutomationGuiProxy(false);
			dstrvboxProxy[i] = AutomationGuiProxy(0);
			scncheckProxy[i] = AutomationGuiProxy(false);
			dfcheckProxy[i] = AutomationGuiProxy(false);
			spcheckProxy[i] = AutomationGuiProxy(false);
			ncanboxProxy[i] = AutomationGuiProxy(1);
			businiboxProxy[i] = AutomationGuiProxy(0);
			stcheckProxy[i] = AutomationGuiProxy(false);
			rateboxProxy[i] = AutomationGuiProxy(10.0);
			winboxProxy[i] = AutomationGuiProxy(0.1);
			randboxProxy[i] = AutomationGuiProxy(0);


			libboxProxy[i].action_({ | num |

				if (guiflag) {
					{ libbox[i].value = num.value }.defer;
					if(i == currentsource) {
						{ updateGuiCtl.value(\lib, num.value) }.defer;
					};
				};

				if (ossialib[i].v != spatList[num.value]) {
					ossialib[i].v_(spatList[num.value]);
				};
			});


			dstrvboxProxy[i].action_({ | num |
				var revArray = ["no-reverb","freeverb","allpass"] ++ rirList;

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

				if (ossiadst[i].v != revArray[num.value]) {
					ossiadst[i].v_(revArray[num.value]);
				};
			});


			xboxProxy[i].action = { | num |

				if (ossiaCartBack && (ossiacart[i].v[0] != num.value)) {
					ossiacart[i].v_([num.value, yboxProxy[i].value,
						zboxProxy[i].value]);
				};

				if (guiflag) {
					{xbox[i].value = num.value}.defer;
					{novoplot.value;}.defer;
				};
			};

			yboxProxy[i].action = { | num |

				if (ossiaCartBack && (ossiacart[i].v[1] != num.value)) {
					ossiacart[i].v_([xboxProxy[i].value, num.value,
						zboxProxy[i].value]);
				};

				if (guiflag) {
					{ybox[i].value = num.value}.defer;
					{novoplot.value;}.defer;
				};
			};

			zboxProxy[i].action = { | num |

				if (ossiaCartBack && (ossiacart[i].v[2] != num.value)) {
					ossiacart[i].v_([xboxProxy[i].value, yboxProxy[i].value,
						num.value]);
				};

				zlev[i] = spheval[i].z;
				if (guiflag) {
					{zbox[i].value = num.value}.defer;
					{novoplot.value;}.defer;
				};
			};

			aboxProxy[i].action = { | num |
				if(espacializador[i].notNil) {
					this.setSynths(i, \angle, num.value);
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
				this.setSynths(i, \amp, num.value.dbamp);

				if (guiflag) {
					{ vbox[i].value = num.value }.defer;
					if(i == currentsource)
					{
						{ winCtl[1][0].value = num.value.curvelin(inMin:-96, inMax:12, curve:-3)}.defer;
						{ winCtl[0][0].value = num.value }.defer;
					};
				};

				if (ossialev[i].v != num.value) {
					ossialev[i].v_(num.value);
				};
			};

			gboxProxy[i].action = { | num |
				this.setSynths(i, \glev, num.value);
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
				this.setSynths(i, \llev, num.value);
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
				this.setSynths(i, \room, num.value);
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
				this.setSynths(i, \damp, num.value);
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
				this.setSynths(i, \rotAngle, num.value  + headingnumboxProxy.value);
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
				this.setSynths(i, \directang, num.value);
				if (guiflag) {
					{dbox[i].value = num.value}.defer;
					if(i == currentsource)
					{
						{winCtl[0][9].value = num.value}.defer;
						{winCtl[1][9].value = num.value / MoscaUtils.halfPi()}.defer;
					};
				};

				if (ossiadir[i].v != num.value) {
					ossiadir[i].v_(num.value);
				};
			};

			cboxProxy[i].action = { | num |
				this.setSynths(i, \contr, num.value);
				if (guiflag) {
					{cbox[i].value = num.value}.defer;
					if (i == currentsource)
					{
						{winCtl[0][2].value = num.value;
						winCtl[1][2].value = num.value;
						novoplot.value;}.defer;
					};
				};

				if (ossiactr[i].v != num.value) {
					ossiactr[i].v_(num.value);
				};
			};

			dpboxProxy[i].action = { | num |
				this.setSynths(i, \dopamnt, num.value);
				if (guiflag) {
					{dpbox[i].value = num.value}.defer;
					if(i == currentsource) {
						{winCtl[1][1].value = num.value}.defer;
						{winCtl[0][1].value = num.value}.defer;
					};
				};

				if (ossiadp[i].value != num.value) {
					ossiadp[i].value_(num.value);
				};
			};


			a1boxProxy[i].action = { | num |
				this.setSynths(i, \aux1, num.value);
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
					this.setSynths(i, \a1check, 1);
				}{
					setSynths(i, \a1check, 0);
				};

				if (guiflag) {

					{a1check[i].value = but.value}.defer;
				};
			};

			a2checkProxy[i].action = { | but |

				if (but.value) {
					this.setSynths(i, \a2check, 1);
				}{
					this.setSynths(i, \a2check, 0);
				};
				if (guiflag) {
					{a2check[i].value = but.value}.defer;
				};
			};


			a3checkProxy[i].action = { | but |

				if (but.value) {
					this.setSynths(i, \a3check, 1);
				}{
					this.setSynths(i, \a3check, 0);
				};
				if (guiflag) {
					{a3check[i].value = but.value}.defer;
				};
			};

			a4checkProxy[i].action = { | but |

				if (but.value) {
					this.setSynths(i, \a4check, 1);
				}{
					this.setSynths(i, \a4check, 0);
				};
				if (guiflag) {
					{a4check[i].value = but.value}.defer;
				};
			};

			a5checkProxy[i].action = { | but |

				if (but.value) {
					this.setSynths(i, \a5check, 1);
				}{
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
					this.setSynths(i, \lp, 1);
				} {
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
					hwn[i] = true;
					scn[i] = false;
				}{
					hwn[i] = false;
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
					scn[i] = true;
					hwn[i] = false;
					ncanboxProxy[i].valueAction_(ncanboxProxy[i].value);
				}{
					scn[i] = false;
					if (scInBus[i].notNil) {
						scInBus[i].free;
						scInBus[i] = nil;
					};
					triggerFunc[i] = nil;
					stopFunc[i] = nil;
					synthRegistry[i].clear;
				};
				if (guiflag) {
					{scncheck[i].value = but.value}.defer;
					if (i == currentsource) {
						{ updateGuiCtl.value(\src); }.defer;
					};
				};
			});

			ncanboxProxy[i].action = { | num |
				ncan[i] = num.value.asInteger;

				this.setSCBus(i + 1, num.value);

				if (num.value < 4) {
					cboxProxy[i].valueAction_(1);
				} {
					cboxProxy[i].valueAction_(0.5);
				};

				if (guiflag ) {
					{ ncanbox[i].value = num.value }.defer;

					if (i == currentsource) {
						{ updateGuiCtl.value(\chan); }.defer;
					};
				};
			};

			spcheckProxy[i].action_({ | but |
				if((i==currentsource) && guiflag){{spreadcheck.value = but.value}.defer;};
				if (but.value) {
					if (guiflag) {
						{dfcheck[i].value = false}.defer;
					};
					if((i==currentsource) && guiflag){
						{diffusecheck.value = false}.defer;};
					this.setSynths(i, \sp, 1);
					this.setSynths(i, \df, 0);
					ossiadiff[i].v_(false);
				} {
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
					this.setSynths(i, \df, 0);
					this.setSynths(i, \sp, 1);
					ossiaspread[i].v_(false);
				} {
					this.setSynths(i, \df, 0);
				};
				if (guiflag) {
					{dfcheck[i].value = but.value}.defer;
				};

				if (ossiadiff[i].v != but.value.asBoolean) {
					ossiadiff[i].v_(but.value.asBoolean);
				};
			});

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
				this.setSynths(i, \grainrate, num.value);
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
				this.setSynths(i, \winsize, num.value);
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
				this.setSynths(i, \winrand, num.value);
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

				if (path != "") {
					var sf = SoundFile.new;
					sf.openRead(path);
					ncanboxProxy[i].valueAction_(sf.numChannels);
					sf.close;

					if (streamdisk[i].not) {
						if (sombuf[i].notNil) {
							sombuf[i].freeMsg({
								"Buffer freed".postln;
							});
						};

						sombuf[i] = Buffer.read(server, path.value, action: { | buf |
							"Loaded file".postln;
						});
					} {
						"To stream file".postln;
					};
				} {
					if (sombuf[i].notNil) {
						sombuf[i].freeMsg({
							sombuf[i] = nil;
							"Buffer freed".postln;
						});
					};
				};

				if (guiflag) {
					{ tfield[i].value = path.value; }.defer;
					{ updateGuiCtl.value(\chan); }.defer;
				};

				ossiaaud[i].description = PathName(path.value).fileNameWithoutExtension;
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

			globDec.set(\level, num.value.dbamp);

			if (guiflag) {
				{masterBox.value = num.value; }.defer;
				{masterslider.value = num.value.curvelin(inMin:-96, inMax:12, curve:-3); }.defer;
			};

			if (ossiamaster.v != num.value) {
				ossiamaster.v_(num.value);
			};
		});


		clsrvboxProxy.action_({ | num |
			var revArray = ["no-reverb","freeverb","allpass"] ++ rirList;

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
						convertor = Synth(\ambiConverter, [\gate, 1],
							target:glbRevDecGrp, addAction:\addAfter).onFree({
							convertor = nil;
						});
					};
				};

				if (revGlobal.notNil)
				{ revGlobal.set(\gate, 0); };

				revGlobal = Synth(\revGlobalAmb++clsRvtypes,
					[\gate, 1, \room, clsrmboxProxy.value, \damp, clsdmboxProxy.value] ++
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

			if (ossiacls.v != revArray[num.value]) {
				ossiacls.v_(revArray[num.value]);
			};
		});


		clsrmboxProxy.action_({ | num |
			revGlobal.set(\room, num.value);

			if (guiflag) {
				{originCtl[1][0].value = num.value}.defer;
			};

			if (ossiaclsdel.v != num.value) {
				ossiaclsdel.v_(num.value);
			};
		});


		clsdmboxProxy.action_({ | num |

			revGlobal.set(\damp, num.value);

			if (guiflag) {
				{originCtl[1][1].value = num.value}.defer;
			};

			if (ossiaclsdec.v != num.value) {
				ossiaclsdec.v_(num.value);
			};
		});


		oxnumboxProxy.action_({ | num |

			ossiaorigine.v_([num.value, oynumboxProxy.value,
				oznumboxProxy.value]);

			if (guiflag) {
				{novoplot.value;}.defer;
				{originCtl[0][5].value = num.value;}.defer;
			};
		});



		oynumboxProxy.action_({ | num |

			ossiaorigine.v_([oxnumboxProxy.value, num.value,
				oznumboxProxy.value]);

			if (guiflag) {
				{novoplot.value;}.defer;
				{originCtl[0][6].value = num.value;}.defer;
			};
		});


		oznumboxProxy.action_({ | num |

			ossiaorigine.v_([oxnumboxProxy.value,
				oynumboxProxy.value, num.value]);

			if (guiflag) {
				{novoplot.value;}.defer;
				{originCtl[0][7].value = num.value;}.defer;
			};
		});



		headingnumboxProxy.action_({ | num |

			ossiaorient.v_([num.value, pitchnumboxProxy.value,
				rollnumboxProxy.value]);

			if (guiflag) {
				{novoplot.value;}.defer;
				{originCtl[0][2].value = num.value;}.defer;
			};
		});


		pitchnumboxProxy.action_({ | num |

			ossiaorient.v_([headingnumboxProxy.value, num.value,
				rollnumboxProxy.value]);

			if (guiflag) {
				{novoplot.value;}.defer;
				{originCtl[0][3].value = num.value;}.defer;
			};
		});


		rollnumboxProxy.action_({ | num |

			ossiaorient.v_([headingnumboxProxy.value,
				pitchnumboxProxy.value, num.value]);

			if (guiflag) {
				{novoplot.value;}.defer;
				{originCtl[0][4].value = num.value;}.defer;
			};
		});


		scalefactProxy.action_({ | num |

			if (guiflag) {
				{novoplot.value;}.defer;
				{scaleBox.value = num.value; }.defer;
				{scaleslider.value = num.value.curvelin(inMin:0.01, inMax:10, curve:4); }.defer;
			};

			if (ossiascale.v != num.value) {
				ossiascale.v_(num.value);
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
		control.dock(scalefactProxy, "scaleProxy");


		///////////////////////////////////////////////////

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
		}).send(server);


		/// non ambisonc spatiaizers setup


		if (speaker_array.notNil) {

			var max_func, min_func, dimention, vbap_setup, adjust;

			numoutputs = speaker_array.size;

			nonambibus = Bus.audio(server, numoutputs);

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

				azimuths = speaker_array.flat;

				vbap_setup = VBAPSpeakerArray(dimention, azimuths);
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

				speaker_array.collect({ |val| val.pop });

				azimuths = speaker_array.flat;

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

			longest_radius = 1;
			lowest_elevation = -90;
			highest_elevation = 90;

			perfectSphereFunc = { |sig|
				sig;
			};

			SynthDef("nonAmbi2FuMa", {
				var sig = In.ar(nonambibus, numoutputs);
				sig = FoaEncode.ar(sig,
					FoaEncoderMatrix.newDirections(emulate_array.degrad));
				Out.ar(fumabus, sig);
			}).send(server);
		};

		quarterRadius = longest_radius / 4;
		twoAndaHalfRadius = longest_radius * 2.5;

		// define ambisonic decoder

		if (suboutbus.notNil) {
			subOutFunc = { |signal, sublevel = 1|
				var subOut = Mix(signal) * sublevel * 0.5;
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

				SynthDef("ambiConverter", { | gate = 1 |
					var n3dsig, env;
					env = EnvGen.kr(Env.asr(curve:\hold), gate, doneAction:2);
					n3dsig = In.ar(n3dbus, bFormNumChan);
					n3dsig = HOAConvert.ar(maxorder, n3dsig, \ACN_N3D, \FuMa) * env;
					Out.ar(fumabus, n3dsig);
				}).send(server);

				SynthDef("globDecodeSynth",  { | sub = 1, level = 1 |
					var sig, nonambi;
					sig = In.ar(fumabus, bFormNumChan);
					nonambi = In.ar(nonambibus, numoutputs);
					perfectSphereFunc.value(nonambi);
					sig = (sig + nonambi) * level;
					subOutFunc.value(sig, sub);
					Out.ar(rawoutbus, sig);
					Out.ar(outbus, nonambi);
				}).send(server);

			}
			{rawformat == \N3D}
			{
				convert_fuma = true;
				convert_n3d = false;
				convert_direct = true;

				SynthDef("ambiConverter", { | gate = 1 |
					var sig, env;
					env = EnvGen.kr(Env.asr(curve:\hold), gate, doneAction:2);
					sig = In.ar(fumabus, fourOrNine);
					sig = HOAConvert.ar(maxorder, sig, \FuMa, \ACN_N3D) * env;
					Out.ar(n3dbus, sig);
				}).send(server);

				SynthDef("globDecodeSynth", {
					| lf_hf=0, xover=400, sub = 1, level = 1 |
					var sig, nonambi;
					sig = In.ar(n3dbus, bFormNumChan) * level;
					nonambi = In.ar(nonambibus, numoutputs) * level;
					perfectSphereFunc.value(nonambi);
					subOutFunc.value(sig + nonambi, sub);
					Out.ar(rawoutbus, sig);
					Out.ar(outbus, nonambi);
				}).send(server);

			};

		} {

			case
			{ maxorder == 1 }
			{ convert_fuma = false;
				convert_n3d = true;
				convert_direct = false;

				SynthDef("ambiConverter", { | gate = 1 |
					var n3dsig, env;
					env = EnvGen.kr(Env.asr(curve:\hold), gate, doneAction:2);
					n3dsig = In.ar(n3dbus, 4);
					n3dsig = FoaEncode.ar(n3dsig, n2f) * env;
					Out.ar(fumabus, n3dsig);
				}).send(server);

				if (decoder == "internal") {

					if (elevations.isNil) {
						elevations = Array.fill(numoutputs, { 0 });
					};

					SynthDef("globDecodeSynth",  { | sub = 1, level = 1 |
						var sig, nonambi;
						sig = In.ar(fumabus, 4);
						sig = BFDecode1.ar1(sig[0], sig[1], sig[2], sig[3],
							azimuths.collect(_.degrad), elevations.collect(_.degrad),
							longest_radius, radiusses, mul: 0.5);
						nonambi = In.ar(nonambibus, numoutputs);
						perfectSphereFunc.value(nonambi);
						sig = (sig + nonambi) * level;
						subOutFunc.value(sig, sub);
						Out.ar(outbus, sig);
					}).send(server);

				} {

					if (speaker_array.notNil) {
						SynthDef("globDecodeSynth",  { | sub = 1, level = 1 |
							var sig, nonambi;
							sig = In.ar(fumabus, 4);
							sig = FoaDecode.ar(sig, decoder);
							nonambi = In.ar(nonambibus, numoutputs);
							perfectSphereFunc.value(nonambi);
							sig = (sig + nonambi) * level;
							subOutFunc.value(sig, sub);
							Out.ar(outbus, sig);
						}).send(server);

					} {
						SynthDef("globDecodeSynth",  { | sub = 1, level = 1 |
							var sig, nonambi;
							sig = In.ar(fumabus, 4);
							sig = FoaDecode.ar(sig, decoder);
							sig = sig * level;
							subOutFunc.value(sig, sub);
							Out.ar(outbus, sig);
						}).send(server);
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

					SynthDef("ambiConverter", { | gate = 1 |
						var n3dsig, env;
						env = EnvGen.kr(Env.asr(curve:\hold), gate, doneAction:2);
						n3dsig = In.ar(n3dbus, 9);
						n3dsig = HOAConvert.ar(2, n3dsig, \ACN_N3D, \FuMa) * env;
						Out.ar(fumabus, n3dsig);
					}).send(server);

					SynthDef("globDecodeSynth",  { | sub = 1, level = 1 |
						var sig, nonambi;
						sig = In.ar(fumabus, 9);
						sig = FMHDecode1.ar1(sig[0], sig[1], sig[2], sig[3], sig[4],
							sig[5], sig[6], sig[7], sig[8],
							azimuths.collect(_.degrad), elevations.collect(_.degrad),
							longest_radius, radiusses, 0.5);
						nonambi = In.ar(nonambibus, numoutputs);
						perfectSphereFunc.value(nonambi);
						sig = (sig + nonambi) * level;
						subOutFunc.value(sig, sub);
						Out.ar(outbus, sig);
					}).send(server);

				} { // assume ADT Decoder
					convert_fuma = true;
					convert_n3d = false;
					convert_direct = false;

					SynthDef("ambiConverter", { | gate = 1 |
						var sig, env;
						env = EnvGen.kr(Env.asr(curve:\hold), gate, doneAction:2);
						sig = In.ar(fumabus, fourOrNine);
						sig = HOAConvert.ar(maxorder, sig, \FuMa, \ACN_N3D) * env;
						Out.ar(n3dbus, sig);
					}).send(server);

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
					}).send(server);
				};
			}

			{ maxorder == 3 } // assume ADT Decoder
			{ convert_fuma = true;
				convert_n3d = false;
				convert_direct = false;

				SynthDef("ambiConverter", { | gate = 1 |
					var sig, env;
					env = EnvGen.kr(Env.asr(curve:\hold), gate, doneAction:2);
					sig = In.ar(fumabus, 9);
					sig = HOAConvert.ar(2, sig, \FuMa, \ACN_N3D) * env;
					Out.ar(n3dbus, sig);
				}).send(server);

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
				}).send(server);
			}

			{ maxorder == 4 } // assume ADT Decoder
			{ convert_fuma = true;
				convert_n3d = false;
				convert_direct = false;

				SynthDef("ambiConverter", { | gate = 1 |
					var sig, env;
					env = EnvGen.kr(Env.asr(curve:\hold), gate, doneAction:2);
					sig = In.ar(fumabus, 9);
					sig = HOAConvert.ar(2, sig, \FuMa, \ACN_N3D) * env;
					Out.ar(n3dbus, sig);
				}).send(server);

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
				}).send(server);
			}

			{ maxorder == 5 } // assume ADT Decoder
			{ convert_fuma = true;
				convert_n3d = false;
				convert_direct = false;

				SynthDef("ambiConverter", { | gate = 1 |
					var sig, env;
					env = EnvGen.kr(Env.asr(curve:\hold), gate, doneAction:2);
					sig = In.ar(fumabus, 9);
					sig = HOAConvert.ar(2, sig, \FuMa, \ACN_N3D) * env;
					Out.ar(n3dbus, sig);
				}).send(server);

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
				}).send(server);
			};
		};


		//// LAUNCH INITIAL SYNTH

		{ globDec = Synth(\globDecodeSynth,
				target:glbRevDecGrp,addAction: \addAfter); }.fork;

		// Make File-in SynthDefs

		playInFunc = Array.newClear(4);
		// one for File, Stereo, BFormat, Stream - streamed file;

		playInFunc[0] = { | playerRef, busini, bufnum, tpos, lp = 0, rate, channum |
			// Note it needs all the variables
			var spos = tpos * BufSampleRate.kr(bufnum),
			scaledRate = rate * BufRateScale.kr(bufnum);
			playerRef.value = PlayBuf.ar(channum, bufnum, scaledRate, startPos: spos,
				loop: lp, doneAction:2);
		};

		// Make HWBus-in SynthDefs

		playInFunc[1] = { | playerRef, busini, bufnum, tpos, lp = 0, rate, channum |
			playerRef.value = In.ar(busini + server.inputBus.index, channum);
		};

		// Make SCBus-in SynthDefs

		playInFunc[2] = { | playerRef, busini, bufnum, tpos, lp = 0, rate, channum |
			playerRef.value = In.ar(busini, channum);
		};

		playInFunc[3] = { | playerRef, busini, bufnum, tpos, lp = 0, rate, channum |
			// Note it needs all the variables
			var trig;
			playerRef.value = DiskIn.ar(channum, bufnum, lp);
			trig = Done.kr(playerRef.value);
			FreeSelf.kr(trig);
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
		//
		// if (Server.program.asString.endsWith("supernova")) {
		//
		// 	if (maxorder == 1) {
		//
		// 		SynthDef("revGlobalAmb_in", { | gate = 1, room = 0.5, damp = 0.5 |
		// 			var env, temp, convsig, sig, sigx, sigf = In.ar(gbfbus, 4);
		// 			sigx = In.ar(gbixfbus, 4);
		// 			sig = In.ar(gbus, 1);
		// 			sigx = FoaEncode.ar(sigx, n2f);
		// 			env = EnvGen.kr(Env.asr(1), gate, doneAction:2);
		// 			sig = sig + sigf + sigx;
		// 			sig = FoaDecode.ar(sig, b2a);
		// 			16.do({ sig = AllpassC.ar(sig, 0.08, room * { Rand(0, 0.08) }.dup(4) +
		// 				{ Rand(0, 0.001) },
		// 			damp * 2)});
		// 			sig = FoaEncode.ar(sig, a2b);
		// 			sig = sig * env;
		// 			Out.ar(fumabus, sig);
		// 		}).send(server);
		//
		//
		// 		SynthDef("parallelVerb_pass", { | gate = 1, room = 0.5, damp = 0.5 |
		// 			var env, temp, convsig, sig, sigx, sigf = In.ar(gbfbus, 4);
		// 			sigx = In.ar(gbixfbus, 4);
		// 			sig = In.ar(gbus, 1);
		// 			sigx = FoaEncode.ar(sigx, n2f);
		// 			env = EnvGen.kr(Env.asr(1), gate, doneAction:2);
		// 			sig = sig + sigf + sigx;
		// 			sig = FoaDecode.ar(sig, b2a);
		// 			16.do({ sig = AllpassC.ar(sig, 0.08, room * { Rand(0, 0.08) }.dup(4) +
		// 				{ Rand(0, 0.001) },
		// 			damp * 2)});
		// 			sig = FoaEncode.ar(sig, a2b);
		// 			sig = sig * env;
		// 			Out.ar(fumabus, sig);
		// 		}).send(server);
		//
		// 		SynthDef("revGlobalAmb_free",  { | gate = 1, room = 0.5, damp = 0.5 |
		// 			var env, temp, convsig, sig, sigx, sigf = In.ar(gbfbus, 4);
		// 			sigx = In.ar(gbixfbus, 4);
		// 			sig = In.ar(gbus, 1);
		// 			sigx = FoaEncode.ar(sigx, n2f);
		// 			env = EnvGen.kr(Env.asr(1), gate, doneAction:2);
		// 			sig = sig + sigf + sigx;
		// 			sigf = FoaDecode.ar(sigf, b2a);
		// 			convsig = [
		// 				FreeVerb2.ar(sig[0], sig[1], mix: 1, room: room, damp: damp),
		// 			FreeVerb2.ar(sig[2], sig[3], mix: 1, room: room, damp: damp)];
		// 			convsig = FoaEncode.ar(convsig.flat, a2b);
		// 			convsig = convsig * env;
		// 			Out.ar(fumabus, convsig);
		// 		}).send(server);
		//
		// 	} {
		//
		// 		SynthDef("revGlobalAmb_pass", { | gate = 1, room = 0.5, damp = 0.5 |
		// 			var env, w, x, y, z, r, s, t, u, v,
		// 			soaSig, tmpsig, sig, sigx, sigf = In.ar(gbfbus, 9);
		// 			sigx = In.ar(gbixfbus, 9);
		// 			sigx = HOAConvert.ar(2, sigx, \FuMa, \ACN_N3D);
		// 			sig = In.ar(gbus, 1);
		// 			env = EnvGen.kr(Env.asr(1), gate, doneAction:2);
		// 			sig = sig + sigf + sigx;
		// 			sig = AtkMatrixMix.ar(sig, soa_a12_decoder_matrix);
		// 			16.do({ sig = AllpassC.ar(sig, 0.08, room * { Rand(0, 0.08) }.dup(12) +
		// 				{ Rand(0, 0.001) },
		// 			damp * 2)});
		// 			#w, x, y, z, r, s, t, u, v = AtkMatrixMix.ar(sig, soa_a12_encoder_matrix)
		// 			* env;
		// 			soaSig = [w, x, y, z, r, s, t, u, v];
		// 			Out.ar(fumabus, soaSig);
		// 		}).load(server);
		//
		// 	};
		//
		// } {

			if (maxorder == 1) {

				SynthDef("revGlobalAmb_pass", { | gate = 1, room = 0.5, damp = 0.5 |
					var env, temp, convsig, sig, sigx, sigf = In.ar(gbfbus, 4);
					sigx = In.ar(gbixfbus, 4);
					sig = In.ar(gbus, 1);
					sigx = FoaEncode.ar(sigx, n2f);
					env = EnvGen.kr(Env.asr(1), gate, doneAction:2);
					sig = sig + sigf + sigx;
					sig = FoaDecode.ar(sig, b2a);
					16.do({ sig = AllpassC.ar(sig, 0.08, room * { Rand(0, 0.08) }.dup(4) +
						{ Rand(0, 0.001) },
						damp * 2)});
					sig = FoaEncode.ar(sig, a2b);
					sig = sig * env;
					Out.ar(fumabus, sig);
				}).send(server);

				SynthDef("revGlobalAmb_free",  { | gate = 1, room = 0.5, damp = 0.5 |
					var env, temp, convsig, sig, sigx, sigf = In.ar(gbfbus, 4);
					sigx = In.ar(gbixfbus, 4);
					sig = In.ar(gbus, 1);
					sigx = FoaEncode.ar(sigx, n2f);
					env = EnvGen.kr(Env.asr(1), gate, doneAction:2);
					sig = sig + sigf + sigx;
					sig = FoaDecode.ar(sig, b2a);
					convsig = [
					FreeVerb.ar(sig[0], mix: 1, room: room, damp: damp),
					FreeVerb.ar(sig[1], mix: 1, room: room, damp: damp),
					FreeVerb.ar(sig[2], mix: 1, room: room, damp: damp),
					FreeVerb.ar(sig[3], mix: 1, room: room, damp: damp)];
					convsig = FoaEncode.ar(convsig.flat, a2b);
					convsig = convsig * env;
					Out.ar(fumabus, convsig);
				}).send(server);

			} {

				SynthDef("revGlobalAmb_pass", { | gate = 1, room = 0.5, damp = 0.5 |
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

				SynthDef("revGlobalAmb_free",  { | gate = 1, room = 0.5, damp = 0.5 |
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
					tmpsig = tmpsig.flat * env;
					#w, x, y, z, r, s, t, u, v = AtkMatrixMix.ar(tmpsig,
						soa_a12_encoder_matrix);
					soaSig = [w, x, y, z, r, s, t, u, v];
					Out.ar(fumabus, soaSig);
				}).send(server);

			};
//		};

		//run the makeSpatialisers methode for each types of local reverbs

		localReverbFunc = Array2D(4, 3);

		localReverbFunc[0, 0] = "_pass";

		localReverbFunc[0, 1] = { | lrevRef, p, rirWspectrum, locallev, room, damp |
			var temp = p;
			16.do({ temp = AllpassC.ar(temp, 0.08, room * { Rand(0, 0.08) } +
				{ Rand(0, 0.001) },
				damp * 2)});
			lrevRef.value = temp * locallev;
		};

		localReverbFunc[0, 2] = { | lrev1Ref, lrev2Ref, p1, p2, rirZspectrum,
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
		//
		// localReverbFunc[0, 3] = { | lrev1Ref, lrev2Ref, p1, p2, rirZspectrum,
		// 	locallev, room, damp |
		// 	var temp1 = p1, temp2 = p2;
		// 	8.do({ temp1 = AllpassC.ar(temp1, 0.08, room * { Rand(0, 0.08) } +
		// 		{ Rand(0, 0.001) },
		// 	damp * 2)});
		// 	8.do({ temp2 = AllpassC.ar(temp2, 0.08, room * { Rand(0, 0.08) } +
		// 		{ Rand(0, 0.001) },
		// 	damp * 2)});
		// 	lrev1Ref.value = temp1 * locallev;
		// 	lrev2Ref.value = temp2 * locallev;
		// };


		// freeverb defs

		//run the makeSpatialisers methode for each types of local reverbs

		localReverbFunc[1, 0] = "_free";

		localReverbFunc[1, 1] = { | lrevRef, p, rirWspectrum, locallev, room = 0.5, damp = 0.5 |
			lrevRef.value = FreeVerb.ar(p, mix: 1, room: room, damp: damp, mul: locallev);
		};

		localReverbFunc[1, 2] = { | lrev1Ref, lrev2Ref, p1, p2, rirZspectrum, locallev,
			room = 0.5, damp = 0.5|
			var temp;
			temp = FreeVerb2.ar(p1, p2, mix: 1, room: room, damp: damp, mul: locallev);
			lrev1Ref.value = temp[0];
			lrev2Ref.value = temp[1];
		};

		// localReverbFunc[1, 3] = { | lrevRef, p, a0ir, a1ir, a2ir, a3ir,
		// 	a4ir, a5ir, a6ir, a7ir, a8ir, a9ir, a10ir, a11ir, locallev,
		// 	room = 0.5, damp = 0.5|
		// 	var temp, sig;
		//
		// 	if (maxorder == 1) {
		// 		sig = FoaDecode.ar(p, b2a);
		// 		temp = [
		// 			FreeVerb2.ar(sig[0], sig[1], mix: 1, room: room, damp: damp),
		// 		FreeVerb2.ar(sig[2], sig[3], mix: 1, room: room, damp: damp)];
		// 		lrevRef.value = FoaEncode.ar(temp.flat, a2b);
		// 	} {
		// 		sig = AtkMatrixMix.ar(p, soa_a12_decoder_matrix);
		// 		temp = [
		// 			FreeVerb2.ar(sig[0], sig[1], mix: 1, room: room, damp: damp),
		// 			FreeVerb2.ar(sig[2], sig[3], mix: 1, room: room, damp: damp),
		// 			FreeVerb2.ar(sig[4], sig[5], mix: 1, room: room, damp: damp),
		// 			FreeVerb2.ar(sig[6], sig[7], mix: 1, room: room, damp: damp),
		// 			FreeVerb2.ar(sig[8], sig[9], mix: 1, room: room, damp: damp),
		// 		FreeVerb2.ar(sig[10], sig[11], mix: 1, room: room, damp: damp)];
		// 		lrevRef.value = AtkMatrixMix.ar(temp.flat,
		// 		soa_a12_encoder_matrix);
		// 	}
		// };


		// function for no-reverb option

		localReverbFunc[2, 0] = "";

		localReverbFunc[2, 1] = { | lrevRef, p, rirWspectrum, locallev, room, damp|
		};

		localReverbFunc[2, 2] = { | lrev1Ref, lrev2Ref, p1, p2, rirZspectrum,
			locallev, room, damp |
		};

		// localReverbFunc[2, 3] = { | lrevRef, p, a0ir, a1ir, a2ir, a3ir,
		// 	a4ir, a5ir, a6ir, a7ir, a8ir, a9ir, a10ir, a11ir, locallev,
		// 	room, damp |
		// };

		rirList = Array.newClear();

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

				SynthDef("revGlobalAmb_conv",  { | gate = 1,
					fluir, frdir, bldir, bruir |
					var env, temp, convsig, sig, sigx, sigf = In.ar(gbfbus, 4);
					sigx = In.ar(gbixfbus, 4);
					sig = In.ar(gbus, 1);
					sigx = FoaEncode.ar(sigx, n2f);
					env = EnvGen.kr(Env.asr(1), gate, doneAction:2);
					sig = sig + sigf + sigx;
					sig = FoaDecode.ar(sig, b2a);
					convsig = [
						PartConv.ar(sig[0], fftsize, fluir),
						PartConv.ar(sig[1], fftsize, frdir),
						PartConv.ar(sig[2], fftsize, bldir),
						PartConv.ar(sig[3], fftsize, bruir)];
					convsig = FoaEncode.ar(convsig, a2b);
					convsig = convsig * env;
					Out.ar(fumabus, convsig);
				}).send(server);

			} {

				rirA12 = Array.newClear(12);
				rirA12Spectrum = Array2D(rirNum, 12);

				rirList.do({ |item, count|

					12.do { | i |
						rirA12[i] = Buffer.readChannel(server,
							rirBank ++ "/" ++ item ++ "_SoaA12.wav",
							channels: [i]);
						server.sync;
						rirA12Spectrum[count, i] = Buffer.alloc(server,
							bufsize[count], 1);
						server.sync;
						rirA12Spectrum[count, i].preparePartConv(rirA12[i], fftsize);
						server.sync;
						rirA12[i].free;
					};

				});

				SynthDef("revGlobalAmb_conv",  { | gate = 1, a0ir, a1ir, a2ir, a3ir,
					a4ir, a5ir, a6ir, a7ir, a8ir, a9ir, a10ir, a11ir |
					var env, w, x, y, z, r, s, t, u, v,
					soaSig, tmpsig, sig, sigx, sigf = In.ar(gbfbus, 9);
					sigx = In.ar(gbixfbus, 9);
					sigx = HOAConvert.ar(2, sigx, \ACN_N3D, \FuMa);
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
					tmpsig = tmpsig * env;
					#w, x, y, z, r, s, t, u, v = AtkMatrixMix.ar(tmpsig,
						soa_a12_encoder_matrix);
					soaSig = [w, x, y, z, r, s, t, u, v];
					Out.ar(fumabus, soaSig);
				}).send(server);

			};

			//run the makeSpatialisers methode for each types of local reverbs

			localReverbFunc[3, 0] = "_conv";

			localReverbFunc[3, 1] = { | lrevRef, p, wir, locallev, room, damp |
				lrevRef.value = PartConv.ar(p, fftsize, wir, locallev);
			};

			localReverbFunc[3, 2] = { | lrev1Ref, lrev2Ref, p1, p2, zir, locallev,
				room, damp |
				var temp1 = p1, temp2 = p2;
				temp1 = PartConv.ar(p1, fftsize, zir, locallev);
				temp2 = PartConv.ar(p2, fftsize, zir, locallev);
				lrev1Ref.value = temp1 * locallev;
				lrev2Ref.value = temp2 * locallev;
			};

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


		localReverbFunc.rowsDo({ |item, count| this.makeSpatialisers(count); });



		// this regulates file playing synths
		watcher = Routine.new({
			var plim = MoscaUtils.plim();
			"WATCHER!!!".postln;
			inf.do({
				0.1.wait;

				nfontes.do({ | i |

					if (spheval[i].rho >= plim) {
						if(espacializador[i].notNil) {
							this.runStop(i); // to kill SC input synths
							espacializador[i].free;
						};
						firstTime[i] = true;
					} {
						if((isPlay || audit[i]) && espacializador[i].isNil && (firstTime[i])) {
							// could remake this a random start point
							this.newtocar(i, 0, force: true);
							firstTime[i] = false;
						};
					};
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
			if (ossiatrasportLoop.v) {
				nfontes.do { | i |
					firstTime[i] = true;
					//("HERE = " ++ firstTime[i]).postln;

				};
				ossiatrasportLoop.v_(false);
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

			if (guiflag) {
				{novoplot.value;}.defer;
			};
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
				nfontes.do { | i |
					// if sound is currently being "tested", don't switch off on stop
					// leave that for user
					if (audit[i] == false) {
						this.runStop(i); // to kill SC input synths
						espacializador[i].free;
					};
				};
				isPlay = false;
				ossiatrasportLoop.v_(false);
				nfontes.do { | i |
					firstTime[i] = true;
					//("HERE = " ++ firstTime[i]).postln;
				};

			} {
				( "Did not stop. dur = " ++ dur ++ " now = " ++
					control.now).postln;
				ossiatrasportLoop.v_(true);
				control.play;
			};

			ossiaplay.v_(false);

			if (guiflag) {
				{novoplot.value;}.defer;
			};
		};

		/// OSSIA bindings

		this.ossia(allCrtitical);

	} // end initMosca


	free {

		control.quit;
		watcher.stop;

		this.freeTracker();

		fumabus.free;
		n3dbus.free;
		nfontes.do { | x |
			espacializador[x].free;
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

}