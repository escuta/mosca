MoscaStartup
{
	var config;
	var moscaInstance;
	//// Server Parameters
	var server = nil;
	var serverStarted = false;

	//Mosca Parameters
	var oscParent;
	var audioPort;
	var >decoder = nil;

	//// GUI variables
	//Window Parameters
	var window;
	var <>windowW = 1260;
	var <>windowH = 920;
	//layout and views
	var main;
	var bottom;
	var moscaOptions; //VLayout
	var serverOptions; //GridLayout
	var scrollView;
	var setupViews;

	//Buttons
	var startButton,stopButton,quitButton,advancedParamButton,exposeParamButton,reconnectButton;

	*new
	{
		^super.new.ctr();
	}
	ctr
	{
		config = MoscaConfig();
		config.autoConfig();
		oscParent = OSSIA_Device("SC");
		audioPort = "ossia score";
		window = Window.new("Mosca Startup", Rect(10,1000,windowW,windowH));
	}
	prExposeParameters{
		oscParent.exposeOSC(config.servIP,config.servPortRemote,config.servPortLocal);
		("Exposing OSC to "+config.servIP+ ", remote port: " +config.servPortRemote + " servLocalPort: "+config.servPortLocal).postln;
	}

	prReconnectWith{

		arg device;
		var devicePortName;
		var pipe,i,stop;
		var data,dataReader;
		devicePortName = nil;
		data = List();
		pipe = Pipe.new("jack_lsp -p","r");
		dataReader = pipe.getLine;
		while({dataReader.notNil;},{data.add(dataReader);dataReader = pipe.getLine;});
		pipe.close;
		data = Array2D.fromArray(data.size/2,2,data.asArray);
		i = 0;
		stop = false;
		while{(i<data.rows) && (stop == false)}
		{
			var row = data.rowAt(i);
			if(row[0].split($:)[0] == device){
				var currentProps;
				if(row[1].split($:)[1].split($,)[0].stripWhiteSpace() == "output"){
					devicePortName = (row[0].split($:)[1].split($_)[0]++$_);
					stop  = true;
				};
			};
			i = i + 1;
		};
		if(devicePortName.notNil)
		{
			config.audioOutputs.do
			{
				| i |
				i.postln;
				("jack_connect "++device.escapeChar($ )++":"++devicePortName ++ i	+ "supernova:input_" ++ (i + 1)).postln;
				Pipe("jack_connect "++device.escapeChar($ )++":"++devicePortName ++ i	+ "supernova:input_" ++ (i + 1), "w");
				Pipe("jack_disconnect "++device.escapeChar($ )++":"++devicePortName ++ i		+ "system:playback_" ++ (i + 1), "w")
			};
		}
		{//else
			("Error! Reconnexion failed").postln;
		};
	}

	prLoadFromFile{
		arg path;
		if(config.loadSpeakerSetup(path)){
			this.prDoInitialSetup();
		};
	}
	prSaveToFile{
		arg path;
		^config.saveSpeakerSetup(path);


	}
	prCheckConfig{
		var ok = true;
		if(config.spatSpeakers.size == 0)
		{
			config.spatSpeakers= nil;
			// ok = false;
			// "Speaker Configuration Missing!".postln;
			config.spatOrder = 1;
		}
		{
			config.spatSpeakers=MoscaUtils.cartesianToAED(config.spatSpeakers);
			config.spatOrder = max(sqrt(config.spatSpeakers.size)-1,1);
		};
		("Current spatialisation order"+config.spatOrder).postln;

		if(config.spatSub.size == 0)
		{
			config.spatSub = nil;
		};

		^ok;
	}
	prStartServer{

		if(this.prCheckConfig && moscaInstance == nil)
		{
			startButton.enabled = false;
			startButton.string = "Mosca - working";
			stopButton.enabled = true;

			"Starting Server - WIP".postln;
			// "SC_JACK_DEFAULT_INPUTS".setenv(audioPort);

			Server.killAll;
			Server.supernova;
			server = Server.local;
			server.options.sampleRate = 48000;
			server.options.memSize = config.memSize;
			server.options.blockSize = config.memBlockSize;
			server.options.numAudioBusChannels = config.audioChannels.asInteger;
			server.options.numInputBusChannels = config.audioInputs.asInteger;
			server.options.numOutputBusChannels = config.audioOutputs.asInteger;
			server.options.numWireBufs = config.audioWireBuffer;


			server.waitForBoot{
				decoder = FoaDecoderKernel.newCIPIC(21, server,server.options.sampleRate.asInteger);
				"Server Started!".postln;
				serverStarted = true;
				server.sync;
				"Mosca parameters".postln;
				("server:" +server + "of class"+server.class).postln;
				("nsources:"+ config.moscaSources.asInteger+ "of class" + config.moscaSources.asInteger.class).postln;
				("dur:" +config.moscaDuration.asInteger+"of class" + config.moscaDuration.asInteger.class ).postln;
				("speaker_array:" +config.spatSpeakers+"of class" + config.spatSpeakers.class ).postln;
				("maxorder:" +config.spatOrder.asInteger+"of class" + config.spatOrder.asInteger.class ).postln;
				("suboutbus:" +config.spatSub+"of class" + config.spatSub.class ).postln;
				("decoder:" +decoder+"of class" + decoder.class ).postln;
				("irBank:" +config.spatRirBank+"of class" + config.spatRirBank.class ).postln;
				("parentOssiaNode:" +oscParent+"of class" + oscParent.class ).postln;
				moscaInstance = Mosca(
					server: server,
					nsources: config.moscaSources.asInteger,
					dur: config.moscaDuration.asInteger,
					speaker_array: config.spatSpeakers,
					maxorder: config.spatOrder.asInteger,
					outbus: config.spatOut.asInteger,
					suboutbus: config.spatSub,
					decoder: decoder,
					irBank: config.spatRirBank,
					parentOssiaNode: oscParent
				);
				moscaInstance.gui();
				moscaInstance.mainWindow.win.onClose = FunctionList.new.addFunc(moscaInstance.mainWindow.win.onClose);
				moscaInstance.mainWindow.win.onClose.addFunc({startButton.enabled = true; startButton.string_("Display Mosca");});
				// moscaInstance.mainWindow.win.onClose.postln;
				server.options.numOutputBusChannels.do({ | i |
					Pipe("jack_visibledisconnect ossia' 'score:out_" ++ (i)
						+ "system:playback_" ++ (i + 1), "w")
				});
			}
		}
		{//else
			startButton.enabled = false;
			"Showing gui back".postln;
			moscaInstance.gui();
			moscaInstance.mainWindow.win.onClose = FunctionList.new.addFunc(moscaInstance.mainWindow.win.onClose);
			moscaInstance.mainWindow.win.onClose.addFunc({startButton.enabled = true; startButton.string_("Display Mosca");});
		}
	}
	prClose{
		if(moscaInstance!=nil){
			"Closing Mosca".postln;
			if(moscaInstance.mainWindow!=nil)
			{
				moscaInstance.mainWindow.win.close();
			};
			Server.killAll;
			startButton.string_("Start Mosca");
			stopButton.enabled = false;
		};
	}
	prCancel
	{
		this.prClose();

		window.close();
		"Cancelling, quitting".postln;
	}
	prTestSound{
		arg outputBus;

	}

	prParseSub{
		arg string;
		var regex = "^(([1-9][0-9]*|0)(,([1-9][0-9]*|0))*)?$";
		config.spatSub.clear;
		if((regex.matchRegexp(string)))
		{
			string = string.split($,);
			string.do{
				arg item;
				if(config.spatSub.indexOf(item.asInteger)==nil)
				{
					config.spatSub = config.spatSub.add(item.asInteger);
				}
			};

		string = config.spatSub.asArray.asString;
		string.remove(string.first);
		string.remove(string.last);
		string = string.replace($ ,);
		};
		^string;
	}

	gui
	{
		var txt = TextField(window);
		main = HLayout();
		bottom = HLayout();
		this.prMoscaOptionsGui();
		this.prServerOptionsGui();

		//bottom row : buttons
		startButton = Button.new().string_("Start Mosca");
		stopButton = Button.new.string_("Stop Mosca");
		stopButton.enabled = false;
		quitButton = Button.new().string_("Quit Mosca");
		advancedParamButton = Button.new().states_([["Show Advanced parameters"],["Hide Advanced parameters"]]);
		exposeParamButton = Button.new().string_("Expose OSC parameters").action_({this.prExposeParameters});
		reconnectButton = Button.new.string_("Reconnect Mosca with target (using jack)").action_({this.prReconnectWith(txt.string);});


		startButton.action = {this.prStartServer};
		stopButton.action = {this.prClose};
		quitButton.action = {this.prCancel};
		advancedParamButton.action = {serverOptions.visible = serverOptions.visible.not};

		//layout addition to main window
		bottom.add(VLayout([startButton],[stopButton]),1);
		bottom.add(VLayout([exposeParamButton],[quitButton]),1);
		bottom.add(VLayout([reconnectButton,1],[HLayout([StaticText.new.string_("Target")],[txt]),1]),1);
		// bottom.add(quitButton);
		bottom.add(advancedParamButton,1);

		main.add(moscaOptions,2);
		main.add(serverOptions,1);

		window.layout = VLayout();
		window.layout.add(main,2);
		window.layout.add(bottom,1);
		window.front;
		this.prDoInitialSetup();
	}

	prServerOptionsGui{

		//server option fields

		var blockSizeInput = EZNumber(window,label:" BlockSize ",
			controlSpec: ControlSpec.new(0.0,inf,\lin,1),
			initVal: config.memBlockSize,
			action:{|ez| ez.round=ez.value; config.memBlockSize=ez.value}
		);

		var memSizeInput = EZNumber(window,label:" MemSize",
			controlSpec: ControlSpec.new(0.0,inf,\lin,1),
			initVal: config.memSize,
			action:{|ez| ez.round=ez.value;config.memSize=ez.value}
		);

		var audioBusInput = EZNumber(window,label:" Audio Bus Channels ",
			controlSpec: ControlSpec.new(0.0,inf,\lin,1),
			initVal: config.audioChannels,
			action:{|ez| ez.round=ez.value;config.audioChannels=ez.value}
		);

		var inputBusInput = EZNumber(window,label:" Canaux d'entrée ",
			controlSpec: ControlSpec.new(0.0,inf,\lin,1),
			initVal: config.audioInputs,
			action:{|ez| ez.round=ez.value;config.audioInputs=ez.value}
		);
		var outputBusInput = EZNumber(window,label:" Canaux de sortie ",
			controlSpec: ControlSpec.new(0.0,inf,\lin,1),
			initVal: config.audioOutputs,
			action:{|ez| ez.round=ez.value;config.audioOutputs=ez.value}
		);
		var bufferInput = EZNumber(window,label:" Wire Buffers",
			controlSpec: ControlSpec.new(0.0,inf,\lin,1),
			initVal: config.audioWireBuffer,
			action:{|ez| ez.round=ez.value;config.audioWireBuffer=ez.value}
		);

		//server options
		var i = 1;

		serverOptions = CompositeView(window);

		// serverOptions.background = Color.rand;
		serverOptions.layout = GridLayout();
		serverOptions.layout.addSpanning(StaticText.new().string_("Server Memory options"),0,0,1,4,align:\center);
		serverOptions.layout.setColumnStretch(0,2);
		serverOptions.layout.setColumnStretch(1,1);
		serverOptions.layout.setColumnStretch(2,1);
		serverOptions.layout.setColumnStretch(3,2);

		[blockSizeInput,memSizeInput,audioBusInput,inputBusInput,outputBusInput,bufferInput].do{
			|ezNumber|
			ezNumber.labelView.align = \left;
			ezNumber.numberView.align = \right;
			serverOptions.layout.add(nil,i,0);
			serverOptions.layout.add(ezNumber.labelView,i,1);
			serverOptions.layout.add(ezNumber.numberView,i,2);
			serverOptions.layout.add(nil,i,3);
			i = i+1;
		};


		serverOptions.layout.addSpanning(nil,i,0,1,4);

		serverOptions.layout.setRowStretch(i,2);
		serverOptions.visible = false;
	}
	prDoInitialSetup{
		setupViews.size.postln;
		this.prClearSetupEntries();
		setupViews.size.postln;
		config.spatSpeakers.do{
			arg item;
			this.prAddSetupEntry(item);
		};
	}
	prAddSetupEntry{
		arg values = [0.0,0.0,0.0];
		var coords = [
			NumberBox.new().value_(values[0]),
			NumberBox.new().value_(values[1]),
			NumberBox.new().value_(values[2])];
		var index = setupViews.size();
		var view,entry;
		view = View();
		index.postln;
		entry = [view,coords];
		//the name field of the view is needed to be able to reach the right child as supercollider QT gui implementation lacks some methods
		view.name = index;
		view.layout_(
        HLayout(
			[StaticText().string_('x: '),stretch:1],
            [coords[0],stretch:2],
			[StaticText().string_('y: '),stretch:1],
			[coords[1],stretch:2],
			[StaticText().string_('z: '),stretch:1],
			[coords[2],stretch:2],
				// [Button().states_([["Test"]]).action_({"testing".postln;}),stretch:1],
				[Button().states_([["Delete"]]).action_({config.spatSpeakers.removeAt(view.name.asInteger);
this.prRemoveSetupEntry(view)}),stretch:1]
        )
    );
		setupViews.add(entry);
		//setup of action when updating field
		//must always match parent view name so it reaches the right address
		3.do{arg i;
			coords[i].action_({arg c;
				var idx  = c.parent.name.asInteger;
				config.spatSpeakers[idx][i]=c.value;});
        };
		scrollView.canvas.layout.insert(setupViews.last[0],setupViews.size()+1);
	}
	prRemoveSetupEntry{
		arg view;
		var idx,i;

		idx = view.name.asInteger;
		//decrement following entries view name
		i = idx;
		while{ i < (setupViews.size-1)}{
			var v = setupViews[i+1][0];
			v.name = v.name.asInteger-1;
			i = i+1;
		};
		setupViews[idx][0].remove;
		setupViews.removeAt(idx);
		setupViews.size.postln;
	}
	prClearSetupEntries{
		var n = setupViews.size();
		("Clearing all "++n++" entries").postln;
		n.do{
			this.prRemoveSetupEntry(setupViews.first[0]);
		};
	}
	prUpdateSetup{
		"Updating".postln
	}
	prSetupGui{
		scrollView = ScrollView();
		setupViews = List();
		//add setup input
		// scrollView.background_(Color.rand);
		scrollView.canvas = View();
		scrollView.canvas.layout = VLayout();
		scrollView.canvas.layout.add(StaticText.new().string_("Setup"),align:\center);
		scrollView.canvas.layout.add(View().background_(Color.black).layout_(
			VLayout(
			HLayout(
					[Button().string_("Load from a file").action_(
						{FileDialog({arg path;var res = this.prLoadFromFile(path);},stripResult: true);}
					),stretch:1],
					[Button().string_("Save to a File").action_(
						{FileDialog({arg path;var res = this.prSaveToFile(path);},fileMode:0,acceptMode:1,stripResult: true);

					}),stretch:1]
			),
			HLayout(
				[Button().string_("Add a output").action_({
					config.spatSpeakers.add([0,0,0]);
					this.prAddSetupEntry();

					setupViews.size.postln;
				}),stretch:1]
			))
		));
		scrollView.canvas.layout.add(nil,stretch:2);
	}

	prMoscaOptionsGui{
		//mosca option fields
		var nbSourcesInput = EZNumber(window,label:" Sources",
			controlSpec: ControlSpec.new(1.0,inf,\lin,1),
			initVal: config.moscaSources,
			action:{|ez| ez.round=ez.value;config.moscaSources=ez.value}
		);
		var outInput = EZNumber(window,label:" First output index",
			controlSpec: ControlSpec.new(0.0,inf,\lin,1),
			initVal: config.spatOut,
			action:{|ez| ez.round=ez.value;config.spatOut=ez.value}
		);
		var durationInput = EZNumber(window,label:" Automation duration (in seconds)",
			controlSpec: ControlSpec.new(0.0,inf,\lin,1),
			initVal: config.moscaDuration,
			action:{|ez| ez.round=ez.value;config.moscaDuration=ez.value}
		);
		var subInput = [
			StaticText.new(moscaOptions).string_(" Subs").align_(\left),
			TextField.new(moscaOptions).value_(
				{
					var str;
					if(config.spatSub.size == 0){
						str = "";
					}{
						str = String.new.ccatList(config.spatSub).stripWhiteSpace;str.removeAt(0);
					};
					str;

			}.value()).action_({
				arg txt;
				txt.value = this.prParseSub(txt.value);
			}).align_(\right)
		];

		var ipInput = TextField.new(moscaOptions).value_(config.servIP).align_(\center).action_{
			arg i;
			var fields = i.value.split($.);
			if(fields.size != 4)
			{
				i.value = config.servIP;
			}
			{
				var validIP = true;
				fields.do{
					arg f;
					var n;
					f = f.stripWhiteSpace;

					n = f.asInteger;
					if((n < 0) || (n > 255) ||(f.size <= 0) || (f.size > 3)){
						validIP = false;
					}
				};
				if(validIP)
				{
					i.value.postln;
					config.servIP = i.value;
				}
				{
					i.value = config.servIP;
				}
			}

		};
		var remotePortInput = NumberBox.new(moscaOptions).clipLo_(0).clipHi_(9999).step_(1).decimals_(0).value_(config.servPortRemote).action_({
			arg i;
			config.servPortRemote = i.value.asInteger;

		}).align_(\center);
		var localPortInput = NumberBox.new(moscaOptions).clipLo_(0).clipHi_(9999).step_(1).decimals_(0).value_(config.servPortLocal).action_({
			arg i;
			config.servPortLocal = i.value.asInteger;
		}).align_(\center);

		// var portList = PopUpMenu(window).items_(this.prGetPorts()).action_({arg i; audioPort=i.item});
		// var scanButton = Button(window).string_("Re-scan").action_{portList.items_(this.prGetPorts())};

		//Because there is a lack of certain QT bindings this variable is necessary to locate where are the different GUI element.
		var i = 1;
		"Setting mosca gui".postln;
		//mosca options
		moscaOptions = CompositeView(window);
		// moscaOptions.background = Color.rand;
		moscaOptions.layout = GridLayout();
		moscaOptions.layout.addSpanning(StaticText.new().string_("Mosca options"),0,0,1,4,align:\center);
		moscaOptions.layout.setColumnStretch(0,1);
		moscaOptions.layout.setColumnStretch(1,2);
		moscaOptions.layout.setColumnStretch(2,2);
		moscaOptions.layout.setColumnStretch(3,1);
		i = i+1;
		// moscaOptions.layout.add(nil,i,0);
		moscaOptions.layout.add(subInput[0],i,1);
		moscaOptions.layout.add(subInput[1],i,2);
		// moscaOptions.layout.add(nil,i,3);
		i = i+1;
		[nbSourcesInput,outInput,durationInput].do{
			|ezNumber|
			ezNumber.labelView.align = \left;
			ezNumber.numberView.align = \right;
			moscaOptions.layout.add(nil,i,0);
			moscaOptions.layout.add(ezNumber.labelView,i,1);
			moscaOptions.layout.add(ezNumber.numberView,i,2);
			moscaOptions.layout.add(nil,i,3);
			i = i+1;
		};
		this.prSetupGui();
	/*	moscaOptions.layout.addSpanning(HLayout(
			[portList],
			[scanButton]),i,1,1,2);
		i = i+1;*/
		moscaOptions.layout.addSpanning(nil,i,0,1,4);
		i = i + 1;
		moscaOptions.layout.addSpanning(scrollView,i,1,2,2);
		moscaOptions.layout.setRowStretch(i,3);
		i = i+1;
		moscaOptions.layout.addSpanning(nil,i,0,1,4);
		i = i+1;
		moscaOptions.layout.addSpanning(
			HLayout(
				[StaticText.new().string_("Listening server IP"),stretch:1],
				[StaticText.new().string_("Listening Port"),stretch:1],
				[StaticText.new().string_("Local Port"),stretch:1],
			),i,1,1,2
		);
		i = i+1;
		moscaOptions.layout.addSpanning(
			HLayout(
				[ipInput,stretch:1],
				[remotePortInput,stretch:1],
				[localPortInput,stretch:1]
			),i,1,1,2
		);
		// i = i+1;

	}

}
