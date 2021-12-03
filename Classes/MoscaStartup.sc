MoscaStartup
{
	//Gui Parameters
	var window;
	var <>windowW = 1260;
	var <>windowH = 920;
	//Server Parameters
	var server = nil;
	var <>servRemoteIP = "127.0.0.1";
	var <>servRemotePort = 9997;
	var <>servLocalPort = 9980;
	var serverStarted = false;
	//Server Advanced Parameters
	var blockSize,memSize;
	var nbAudioBusChannels,nbWireBuffer,nbInputBusChannels,nbOutputBusChannels;


	//Mosca Parameters
	var oscParent;
	var oscInputName;
	var >decoder = nil;
	var <>order = -1;
	var <>setupList;
	var <>sources = -1;
	var <>out = -1;
	var <>sub;
	var moscaInstance;
	var rirBank;

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

		memSize = 8192*12;
		blockSize = 48;
		nbAudioBusChannels = 2048;
		nbInputBusChannels = 44;
		nbOutputBusChannels = 44;
		nbWireBuffer = 512;

		setupList = List[];
		sub = List[];
	}
	prExposeParameters{
		oscParent.exposeOSC(servRemoteIP,servRemotePort,servLocalPort);
	}

	prHello
	{
		"ohioh".postln;
	}
	prStartServer{
		var defaultDuration = 9;

		"Starting Server - WIP".postln;
		"SC_JACK_DEFAULT_INPUTS".setenv(oscInputName);

		Server.killAll;
		Server.supernova;
		server = Server.local;
		server.options.memSize = memSize;
		server.options.blockSize = blockSize;
		server.options.numAudioBusChannels = nbAudioBusChannels;
		server.options.numInputBusChannels = nbInputBusChannels;
		server.options.numOutputBusChannels = nbOutputBusChannels;
		server.options.numWireBufs = nbWireBuffer;



		server.waitForBoot{
			serverStarted = true;
			server.sync;
			moscaInstance = Mosca(
				server: server,
				nsources: sources,
				dur: defaultDuration,
				speaker_array: setupList,
				maxorder: order,
				outbus: out,
				suboutbus: sub,
				decoder: decoder,
				rirBank: rirBank,
				parentOssiaNode: oscParent;
			).gui();
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
		var main = HLayout();
		var bottom = HLayout();
		var moscaOptions; //VLayout
		var serverOptions; //GridLayout
		var startButton,cancelButton,advancedParamButton;

		moscaOptions = this.prMoscaOptionsGui();
		serverOptions = this.prServerOptionsGui();

		//bottom row : buttons
		startButton = Button.new().string_("Start Server");
		cancelButton = Button.new().string_("Cancel");
		advancedParamButton = Button.new().states_([["Paramètres Avancés"],["Fermer Paramètres Avancés"]]);
		// startButton.action = {this.prStartServer};
		startButton.action = {"hello world".postln();};
		cancelButton.action = {this.prCancel};
		advancedParamButton.action = {serverOptions.visible = serverOptions.visible.not};

		//layout addition to main window
		bottom.add(startButton);
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
		var serverOptions;
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

		var inputBusInput = EZNumber(window,label:" Input Bus Channels ",
			controlSpec: ControlSpec.new(0.0,inf,\lin,1),
			initVal: nbInputBusChannels,
			action:{|ez| ez.round=ez.value;nbInputBusChannels=ez.value}
		);
		var outputBusInput = EZNumber(window,label:" Output Bus Channels ",
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
		serverOptions.layout.addSpanning(StaticText.new().string_("Server options"),0,0,1,4,align:\center);
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
		^serverOptions;
	}
	prAddSetupEntry{
		arg entries;
		var coords = [NumberBox.new(),NumberBox.new(),NumberBox.new()];
		var index = entries.size();
		var view,entry;
		view = View();
		index.postln;
		entry = [view,coords];
		view.name = index;
		("Hello source n°"+view.name.asInteger).postln;
		view.background_(Color.rand).layout_(
        HLayout(
				// [StaticText().string_("Sortie n°"+index.asString),stretch:1],
			[StaticText().string_('x: '),stretch:1],
            [coords[0],stretch:2],
			[StaticText().string_('y: '),stretch:1],
			[coords[1],stretch:2],
			[StaticText().string_('z: '),stretch:1],
			[coords[2],stretch:2],
			[Button().states_([["Test"]]).action_({"testing".postln;}),stretch:1],
			[Button().states_([["Delete"]]).action_({
					var idx;
					idx = view.name.asInteger;
					("Bye Source n°"+ view.name.asInteger).postln;
					idx.postln;
					//decrement following entries view name
					//BUG: 03122021: rmontferme: current implementation causes outof bound access as name of view isn't updated
					/*(entries.size-idx).do{
						arg i;
						var v;
						v = entries[i][0];
						v.name.postln;
						v.name = (v.name.asInteger-1);
						v.name.postln;
					};*/
					entries.removeAt(index);
					setupList.removeAt(index);
					view.remove;
					view.name.postln;

				}),stretch:1]
        )
    );
		entries.insert(index,entry);

        3.do{arg i;
			coords[i].action_({arg c; setupList[index][i]=c.value; setupList.do{|i| i.postln};"modification".postln;});
        };
        setupList.insert(index,[coords[0].value,coords[1].value,coords[2].value]);
		setupList.do{
			|i|
			i.postln;
		};
		"Added".postln;

	}

	prSetupGui{
		var scrollView = ScrollView();
		var setupViews;
		setupViews = List();
		//add setup input
		scrollView.background_(Color.rand);
		scrollView.canvas = View();
		scrollView.canvas.layout = VLayout();
		scrollView.canvas.layout.add(StaticText.new().string_("Setup"),align:\center);
		scrollView.canvas.layout.add(View().background_(Color.black).layout_(
			HLayout(
				[Button().string_("Ajouter une sortie").action_({
					setupViews.size.postln;
					this.prAddSetupEntry(setupViews);
					scrollView.canvas.layout.insert(setupViews.last[0],setupViews.size()+1);
				}),stretch:1],
				[
					Button().string_("Charger depuis un fichier"),stretch:1
				]
			)
		));
		scrollView.canvas.layout.add(nil,stretch:2);
		^scrollView;
	}
	prMoscaOptionsGui{
		var moscaOptions;
		//mosca option fields
		var nbSourcesInput = EZNumber(window,label:" Sources",
			controlSpec: ControlSpec.new(0.0,inf,\lin,1),
			initVal: sources,
			action:{|ez| ez.round=ez.value;sources=ez.value}
		);
		var outInput = EZNumber(window,label:" Out",
			controlSpec: ControlSpec.new(0.0,inf,\lin,1),
			initVal: out,
			action:{|ez| ez.round=ez.value;out=ez.value}
		);
		var subInput = [
			StaticText.new(moscaOptions).string_("Subs").align_(\left),
			TextField.new(moscaOptions).action_({
				arg txt;
				txt.value = this.prParseSub(txt.value);
			}).align_(\right)
		];

		var scrollView;

		var i = 1;
		"Setting mosca gui".postln;
		//mosca options
		moscaOptions = CompositeView(window);
		moscaOptions.background = Color.rand;
		moscaOptions.layout = GridLayout();
		moscaOptions.layout.children.postln;
		moscaOptions.layout.addSpanning(StaticText.new().string_("Mosca options"),0,0,1,4,align:\center);
		moscaOptions.layout.setColumnStretch(0,2);
		moscaOptions.layout.setColumnStretch(1,1);
		moscaOptions.layout.setColumnStretch(2,1);
		moscaOptions.layout.setColumnStretch(3,2);
		moscaOptions.layout.add(Button.new().string_("Test").action_({
			|b|
			"Test".postln;
		}),1,1);
		i = i+1;
		// subInput[0].align = \left;
		// subInput[1].align = \center;
		moscaOptions.layout.add(nil,i,0);
		moscaOptions.layout.add(subInput[0],i,1);
		moscaOptions.layout.add(subInput[1],i,2);
		moscaOptions.layout.add(nil,i,3);
		i = i+1;
		[nbSourcesInput,outInput].do{
			|ezNumber|
			ezNumber.labelView.align = \left;
			ezNumber.numberView.align = \right;
			moscaOptions.layout.add(nil,i,0);
			moscaOptions.layout.add(ezNumber.labelView,i,1);
			moscaOptions.layout.add(ezNumber.numberView,i,2);
			moscaOptions.layout.add(nil,i,3);
			i = i+1;
		};
		scrollView = this.prSetupGui();
		moscaOptions.layout.addSpanning(scrollView,i,1,2,2);
		i = i+1;

		moscaOptions.layout.addSpanning(nil,i,0,1,4);
		moscaOptions.layout.setRowStretch(i,2);
		^moscaOptions;

	}

}
