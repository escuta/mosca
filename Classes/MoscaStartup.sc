MoscaStartup
{
	//// Server Parameters
	var server = nil;
	var <>servRemoteIP = "127.0.0.1";
	var <>servRemotePort = 9997;
	var <>servLocalPort = 9996;
	var serverStarted = false;
	//Server Advanced Parameters
	var blockSize,memSize;
	var nbAudioBusChannels,nbWireBuffer,nbInputBusChannels,nbOutputBusChannels;


	//Mosca Parameters
	var oscParent;
	var oscInputName;
	var >decoder = nil;
	var <>order = -1;
	var <>setupList = nil;
	var <>sources = 1;
	var <>out = 0;
	var <>sub;
	var moscaInstance;
	var rirBank = nil;
	var <>duration = 20;

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

		oscParent = OSSIA_Device("SC");
		oscInputName = "ossia score";
		window = Window.new("Mosca Startup", Rect(10,1000,windowW,windowH));

		//reading initial servers parameters;

		memSize = 16384;
		blockSize = 64;
		nbAudioBusChannels = 1068;
		nbInputBusChannels = 32;
		nbOutputBusChannels = 16;
		nbWireBuffer = 64;
		// initial mosca options values
		setupList = List[];
		sub = List[];

	}
	prExposeParameters{
		oscParent.exposeOSC(servRemoteIP,servRemotePort,servLocalPort);
		("Exposing OSC to "+servRemoteIP+ ", remote port: " +servRemotePort + " servLocalPort: "+servLocalPort).postln;
	}
	prReconnectMosca{
		var o = server.options;
		o.numOutputBusChannels.do({ | i |
			i.postln();
			Pipe("jack_disconnect ossia' 'score:out_" ++ (i)
				+ "system:playback_" ++ (i + 1), "w")
		});
		o.numOutputBusChannels.do({ | i |
			i.postln();
			Pipe("jack_connect ossia' 'score:out_" ++ (i)
				+ "supernova:input_" ++ (i + 1), "w")
		});


	}
	prHello
	{
		"ohioh".postln;
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
		if(setupList.size != 0){
			file = CSVFileWriter(path);
			setupList.do{
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
		if(setupList.size == 0)
		{
			setupList = nil;
			// ok = false;
			// "Speaker Configuration Missing!".postln;
			order = 0;
		}
		{
			setupList=MoscaUtils.cartesianToAED(setupList);
			order = sqrt(setupList.size)-1;
			order.postln;
		};

		if(sub.size == 0)
		{
			sub = nil;
		};

		^ok;
	}
	prStartServer{

		if(this.prCheckConfig)
		{
			"Starting Server - WIP".postln;
			"SC_JACK_DEFAULT_INPUTS".setenv(oscInputName);

			Server.killAll;
			Server.supernova;
			server = Server.local;
			server.options.sampleRate = 48000;
			server.options.memSize = memSize;
			server.options.blockSize = blockSize;
			server.options.numAudioBusChannels = nbAudioBusChannels.asInteger;
			server.options.numInputBusChannels = nbInputBusChannels.asInteger;
			server.options.numOutputBusChannels = nbOutputBusChannels.asInteger;
			server.options.numWireBufs = nbWireBuffer;



			server.waitForBoot{
				decoder = FoaDecoderKernel.newCIPIC(21, server,server.options.sampleRate.asInteger);
				"Server Started!".postln;
				server.options.sampleRate.postln;
				serverStarted = true;
				server.sync;
				moscaInstance = Mosca(
					server: server,
					nsources: sources.asInteger,
					dur: duration.asInteger,
					speaker_array: setupList,
					maxorder: 1,
					outbus: out,
					suboutbus: sub,
					decoder: decoder,
					rirBank: rirBank,
					parentOssiaNode: oscParent;
				).gui();
			}
		}
	}
	prCancel
	{
		window.close();
		"Cancelling, quitting".postln;
	}
	prTestSound{
		arg outputBus;

	}

	prParseSub{
		arg string;
		var regex = "^(([1-9][0-9]*|0)(,([1-9][0-9]*|0))*)?$";
		sub.clear;
		if((regex.matchRegexp(string)))
		{
			string = string.split($,);
			string.do{
				arg item;
				if(sub.indexOf(item.asInteger)==nil)
				{
					sub = sub.add(item.asInteger);
				}
			};

		string = sub.asArray.asString;
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
		startButton = Button.new().string_("Start Server");
		cancelButton = Button.new().string_("Cancel");
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
			initVal: blockSize,
			action:{|ez| ez.round=ez.value; blockSize=ez.value}
		);

		var memSizeInput = EZNumber(window,label:" MemSize",
			controlSpec: ControlSpec.new(0.0,inf,\lin,1),
			initVal: memSize,
			action:{|ez| ez.round=ez.value;memSize=ez.value}
		);

		var audioBusInput = EZNumber(window,label:" Audio Bus Channels ",
			controlSpec: ControlSpec.new(0.0,inf,\lin,1),
			initVal: nbAudioBusChannels,
			action:{|ez| ez.round=ez.value;nbAudioBusChannels=ez.value}
		);

		var inputBusInput = EZNumber(window,label:" Canaux d'entrée ",
			controlSpec: ControlSpec.new(0.0,inf,\lin,1),
			initVal: nbInputBusChannels,
			action:{|ez| ez.round=ez.value;nbInputBusChannels=ez.value}
		);
		var outputBusInput = EZNumber(window,label:" Canaux de sortie ",
			controlSpec: ControlSpec.new(0.0,inf,\lin,1),
			initVal: nbOutputBusChannels,
			action:{|ez| ez.round=ez.value;nbOutputBusChannels=ez.value}
		);
		var bufferInput = EZNumber(window,label:" Wire Buffers",
			controlSpec: ControlSpec.new(0.0,inf,\lin,1),
			initVal: nbWireBuffer,
			action:{|ez| ez.round=ez.value;nbWireBuffer=ez.value}
		);
		//server options
		var i = 1;
		serverOptions = CompositeView(window);

		serverOptions.background = Color.rand;
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
		("Adding new entry at coords"++values).postln;
		view = View();
		index.postln;
		entry = [view,coords];
		//the name field of the view is needed to be able to reach the right child as supercollider QT gui implementation lacks some methods
		view.name = index;
		view.background_(Color.rand).layout_(
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
				setupList[idx][i]=c.value;});
        };
        setupList.add([coords[0].value,coords[1].value,coords[2].value]);
		scrollView.canvas.layout.insert(setupViews.last[0],setupViews.size()+1);
	}
	prRemoveSetupEntry{
		arg view;
		var idx,i;

		idx = view.name.asInteger;
		("Removing vie at pos "++idx).postln;
		//decrement following entries view name
		i = idx;
		while{ i < (setupViews.size-1)}{
			var v = setupViews[i+1][0];
			v.name = v.name.asInteger-1;
			i = i+1;
		};
		setupViews[idx][0].remove;
		setupViews.removeAt(idx);
		setupList.removeAt(idx);
		setupViews.size.postln;
	}
	prClearSetupEntries{
		var n = setupViews.size();
		("Clearing all "++n++" entries").postln;
		n.do{
			this.prRemoveSetupEntry(setupViews.first[0]);
		};
	}

	prGetPorts{
		var list;
		var ports = Set();
		list = "jack_lsp".unixCmdGetStdOut.split($\n);
		list.pop();
		if(list.size != 0){
			list.do{
				arg item;
				ports.add(item.split($:)[0]);
			};
		};
		^(ports.asArray);
	}

	prUpdateSetup{
		"Updating".postln
	}
	prSetupGui{
		scrollView = ScrollView();
		setupViews = List();
		//add setup input
		scrollView.background_(Color.rand);
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
			initVal: sources,
			action:{|ez| ez.round=ez.value;sources=ez.value}
		);
		var outInput = EZNumber(window,label:" Index Bus Première Sortie",
			controlSpec: ControlSpec.new(0.0,inf,\lin,1),
			initVal: out,
			action:{|ez| ez.round=ez.value;out=ez.value}
		);
		var durationInput = EZNumber(window,label:" Durée des automations (s)",
			controlSpec: ControlSpec.new(0.0,inf,\lin,1),
			initVal: duration,
			action:{|ez| ez.round=ez.value;duration=ez.value}
		);
		var subInput = [
			StaticText.new(moscaOptions).string_(" Subs").align_(\left),
			TextField.new(moscaOptions).action_({
				arg txt;
				txt.value = this.prParseSub(txt.value);
			}).align_(\right)
		];
		var portList = PopUpMenu(window).items_(this.prGetPorts());
		var scanButton = Button(window).string_("Re-scan").action_{portList.items_(this.prGetPorts())};
		var i = 1;
		"Setting mosca gui".postln;
		//mosca options
		moscaOptions = CompositeView(window);
		moscaOptions.background = Color.rand;
		moscaOptions.layout = GridLayout();
		moscaOptions.layout.children.postln;
		moscaOptions.layout.addSpanning(StaticText.new().string_("Mosca options").background_(Color.rand),0,0,1,4,align:\center);
		moscaOptions.layout.setColumnStretch(0,2);
		moscaOptions.layout.setColumnStretch(1,1);
		moscaOptions.layout.setColumnStretch(2,1);
		moscaOptions.layout.setColumnStretch(3,2);
		i = i+1;
		moscaOptions.layout.add(nil,i,0);
		moscaOptions.layout.add(subInput[0],i,1);
		moscaOptions.layout.add(subInput[1],i,2);
		moscaOptions.layout.add(nil,i,3);
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
		moscaOptions.layout.addSpanning(HLayout(
			[portList],
			[scanButton]),i,1,1,2);
		i = i+1;
		moscaOptions.layout.addSpanning(nil,i,0,1,4);
		i = i + 1;
		moscaOptions.layout.addSpanning(scrollView,i,1,2,2);
		i = i+1;
		moscaOptions.layout.addSpanning(nil,i,0,1,4);
		i = i+1;
		moscaOptions.layout.setRowStretch(i,2);

	}

}
