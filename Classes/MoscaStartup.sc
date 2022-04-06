MoscaConfig{
	//IP config
	var <>servIP = "127.0.0.1";
	var <>servPortRemote = 9997;
	var <>servPortLocal = 9996;
	//server memory parameters
	var <>memSize = 16384;
	var <>memBlockSize = 64;
	//serv audio parameters
	var <>audioChannels = 1068;
	var <>audioWireBuffer = 64;
	var <>audioInputs = 2;
	var <>audioOutputs = 2;
	//audio spatialisation parameters
	var <>spatOrder = 1;
	var <>spatSpeakers = nil;
	var <>spatSub = nil;
	var <>spatOut = 0;
	var <>spatRirBank = nil;
	//mosca specific
	var <>moscaSources = 4;
	var <>moscaDuration = 10;

	*new{
		^super.new.ctr();
	}

	ctr{
		spatSpeakers = List[];
		spatSub = List[];
	}
	autoConfig{
		if(File.exists("~/.config/Mosca/config.cfg".standardizePath) == false){
			this.saveConfig();
		}
		{//else
			this.loadConfig();
		};
	}
	prReadProp{
		arg reader,prop;
		var res;
		res = reader.getLine();
        if(res.contains(prop)){
			res = res.split($=);
			if(res.size >= 2)
			{
				res = res[0].stripWhiteSpace();
			}
		};
		^nil;
	}
	loadConfig{
		var reader;
		reader = FileReader("~/.config/Mosca/config.cfg","r");
		if(reader.getLine().contains("[Address]")){
				servIP = this.prReadProp("ip");
				servPortRemote = this.prReadProp("remotePort");
				if(servPortRemote!=nil){servPortRemote = servPortRemote.asInteger;};
				servPortLocal = this.prReadProp("localPort");
				if(servPortLocal!=nil){servPortRemote = servPortLocal.asInteger;};
		};
		if(reader.getLine().contains("[Memory]")){
				memSize = this.prReadProp("size");
				if(memSize!=nil){memSize = memSize.asInteger;};
				memBlockSize = this.prReadProp("blockSize");
				if(memBlockSize!=nil){memBlockSize = memBlockSize.asInteger;};
		};
		if(reader.getLine().contains("[Memory]"))
		{
			audioChannels = this.prReadProp("channels");
			if(audioChannels!=nil){audioChannels = audioChannels.asInteger;};
			audioWireBuffer = this.prReadProp("wirebuffers");
			if(audioWireBuffer!=nil){audioWireBuffer=audioWireBuffer.asInteger;};
			audioInputs = this.prReadProp("inputs");
			if(audioInputs!=nil){audioInputs=audioInputs.asInteger;};
			audioOutputs = this.prReadProp("inputs");
			if(audioOutputs!=nil){audioOutputs=audioOutputs.asInteger;};
		};
		if(reader.getLine().contains("[Spatialisation]")){
					/*reader.write("order ="+spatOrder++"\n");
					writer.write("speakerConfig ="+spatSpeaker++"\n");
					writer.write("sub ="+spatSub++"\n");
					writer.write("out ="+spatOut++"\n");
					writer.write("rirBank = "+spatRirBank++"\n");*/
		};

        if(reader.getLine().contains("[Mosca]")){
				/*	writer.write("sources ="+moscaSources++"\n");
					writer.write("duration ="+mosaDuration++"\n");*/
		};
		reader.close();


	}

	saveConfig{
		/*var writer = File("~/.config/Mosca/config.cfg","w");
		writer.write("[Address]\n");
		writer.write("ip ="+servIp++"\n");
		writer.write("remotePort ="+servPortRemote++"\n");
		writer.write("localPort ="+servPortLocal++"\n");
		writer.write("[Memory]\n");
		writer.write("size ="+memSize++"\n");
		writer.write("blockSize ="+memBlockSize++"\n");
		writer.write("[Audio]\n");
		writer.write("channels ="+audioChannels+"\n");
		writer.write("wireBuffers ="+audioWireBuffer++"\n");
		writer.write("inputs ="+audioInputs++"\n");
		writer.write("outputs ="+audioOutputs++"\n");
		writer.write("[Spatialisation]\n");
		writer.write("order ="+spatOrder++"\n");
		writer.write("speakerConfig ="+spatSpeaker++"\n");
		writer.write("sub ="+spatSub++"\n");
		writer.write("out ="+spatOut++"\n");
		writer.write("rirBank = "+spatRirBank++"\n");
		writer.write("[Mosca]\n");
		writer.write("sources ="+moscaSources++"\n");
		writer.write("duration ="+mosaDuration++"\n");
		writer.close();*/
	}



}
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
	var startButton,cancelButton,advancedParamButton,exposeParamButton;

	*new
	{
		^super.new.ctr();
	}
	ctr
	{
		config = MoscaConfig();
		oscParent = OSSIA_Device("SC");
		audioPort = "ossia score";
		window = Window.new("Mosca Startup", Rect(10,1000,windowW,windowH));
	}
	prExposeParameters{
		oscParent.exposeOSC(config.servIP,config.servPortRemote,config.servPortLocal);
		("Exposing OSC to "+config.servIP+ ", remote port: " +config.servPortRemote + " servLocalPort: "+config.servPortLocal).postln;
	}

	prLoadFromFile{
		arg path;
		var file,data;
		var idx = 0;
		var parsingError = false;
		var newData = List[];
		file = CSVFileReader.read(path);
		data = file.collect(_.collect(_.interpret));
		while{((idx < data.size) && (parsingError.not)) && (true)}
		{
			if(data[idx].size != 3)
			{
				parsingError = true;
			}
			{//else
				newData.add(data[idx]);
				idx = idx + 1;
			};
		};

		if(parsingError){
			"Error During Parsing".postln;
		}{
			"Loading Speaker Configuration File".postln;
			//if load is okay then replace current setup values and update view as well
			("new data List").postln;
			newData.postcs;

			setupViews.size.postln;
			this.prClearSetupEntries();
			setupViews.size.postln;
			newData.do{
				arg item;
				this.prAddSetupEntry(item);
			};


		};
		^parsingError.not;
	}
	prSaveToFile{
		arg path;
		var file,data;
		var idx = 0;
		var parsingError = false;
		"Saving Speaker Configuration File".postln;
		if(config.spatSpeakers.size != 0){
			file = CSVFileWriter(path);
			config.spatSpeakers.do{
				arg row;
				file.writeLine(row);
			};
			file.close();
			"Speaker Configuration File saved".postln;
		}
		{
			parsingError = true;
			"Speaker Setup is empty! Cancelling save".postln;
		}
		^parsingError;
	}
	prCheckConfig{
		var ok = true;
		if(config.spatSpeakers.size == 0)
		{
			config.spatSpeakers= nil;
			// ok = false;
			// "Speaker Configuration Missing!".postln;
			config.spatOrder = 0;
		}
		{
			config.spatSpeakers=MoscaUtils.cartesianToAED(config.spatSpeakers);
			config.spatOrder = sqrt(config.spatSpeakers.size)-1;
		};

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
			startButton.string = "Mosca - en marche";

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
				moscaInstance = Mosca(
					server: server,
					nsources: config.moscaSources.asInteger,
					dur: config.moscaDuration.asInteger,
					speaker_array: config.spatSpeakers,
					maxorder: config.spatOrder,
					outbus: config.spatOut,
					suboutbus: config.spatSub,
					decoder: decoder,
					rirBank: config.spatRirBank,
					parentOssiaNode: oscParent;
				);
				moscaInstance.gui();
				moscaInstance.mainWindow.win.onClose = FunctionList.new.addFunc(moscaInstance.mainWindow.win.onClose);
				moscaInstance.mainWindow.win.onClose.addFunc({startButton.enabled = true; startButton.string_("Afficher Mosca");});
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
			moscaInstance.mainWindow.win.onClose.addFunc({startButton.enabled = true; startButton.string_("Afficher Mosca");});
		}
	}
	prCancel
	{
		if(moscaInstance!=nil){
			"Closing Mosca".postln;
			if(moscaInstance.mainWindow!=nil)
			{
				moscaInstance.mainWindow.win.close();
			};
			Server.killAll;
		};
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
		string.postln;
		^string;
	}

	gui
	{

		main = HLayout();
		bottom = HLayout();
		this.prMoscaOptionsGui();
		this.prServerOptionsGui();

		//bottom row : buttons
		startButton = Button.new().string_("Démarrer Mosca");
		cancelButton = Button.new().string_("Fermer");
		advancedParamButton = Button.new().states_([["Paramètres Avancés"],["Fermer Paramètres Avancés"]]);
		exposeParamButton = Button.new().string_("Exposer les paramètres OSC").action_({this.prExposeParameters});

		startButton.action = {this.prStartServer};
		cancelButton.action = {this.prCancel};
		advancedParamButton.action = {serverOptions.visible = serverOptions.visible.not};

		//layout addition to main window
		bottom.add(startButton);
		bottom.add(exposeParamButton);
		bottom.add(cancelButton);
		bottom.add(advancedParamButton);

		main.add(moscaOptions,2);
		main.add(serverOptions,1);

		window.layout = VLayout();
		window.layout.add(main,2);
		window.layout.add(bottom,1);
		window.front;
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
			[Button().states_([["Test"]]).action_({"testing".postln;}),stretch:1],
				[Button().states_([["Delete"]]).action_({this.prRemoveSetupEntry(view)}),stretch:1]
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
        config.spatSpeakers.add([coords[0].value,coords[1].value,coords[2].value]);
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
		config.spatSpeakers.removeAt(idx);
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
					[Button().string_("Charger depuis un fichier").action_(
						{FileDialog({arg path;var res = this.prLoadFromFile(path);},stripResult: true);}
					),stretch:1],
					[Button().string_("Sauvegarder dans un fichier").action_(
						{FileDialog({arg path;var res = this.prSaveToFile(path);},fileMode:0,acceptMode:1,stripResult: true);

					}),stretch:1]
			),
			HLayout(
				[Button().string_("Ajouter une sortie").action_({
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
		var outInput = EZNumber(window,label:" Index Bus Première Sortie",
			controlSpec: ControlSpec.new(0.0,inf,\lin,1),
			initVal: config.spatOut,
			action:{|ez| ez.round=ez.value;config.spatOut=ez.value}
		);
		var durationInput = EZNumber(window,label:" Durée des automations (s)",
			controlSpec: ControlSpec.new(0.0,inf,\lin,1),
			initVal: config.moscaDuration,
			action:{|ez| ez.round=ez.value;config.moscaDuration=ez.value}
		);
		var subInput = [
			StaticText.new(moscaOptions).string_(" Subs").align_(\left),
			TextField.new(moscaOptions).action_({
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
				[StaticText.new().string_("Adresse IP du serveur distant"),stretch:1],
				[StaticText.new().string_("Port Distant"),stretch:1],
				[StaticText.new().string_("Port Local"),stretch:1],
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
