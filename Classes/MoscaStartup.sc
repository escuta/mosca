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
	//Server Advanced Parameters
	var blockSize,memSize;
	var nbAudioBusChannels,nbWireBuffer,nbInputBusChannels,nbOutputBusChannels;


	//Mosca Parameters
	var oscParent;
	var oscInputName;
	var >decoder = nil;
	var <>order = -1;
	var <>sourceList;
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

		sourceList = [];
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
			server.sync;
			moscaInstance = Mosca(
				server: server,
				nsources: sourceList.size(),
				dur: defaultDuration,
				speaker_array: sourceList,
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

	gui
	{
		var layout = VLayout();
		var bottom = HLayout();
		var main = HLayout();
		var moscaOptions = VLayout();
		var serverOptions = VLayout();
		var startButton,cancelButton;
		//server option fields

		var blockSizeTxt = EZNumber(window,label:" BlockSize ",
			controlSpec: ControlSpec.new(0.0,inf,\lin,1),
			initVal: blockSize,
			labelWidth: 120,
			unitWidth: 400,
			action:{|ez| ez.round=ez.value; blockSize=ez.value}
		);

		var memSizeTxt = EZNumber(window,label:" MemSize",
			controlSpec: ControlSpec.new(0.0,inf,\lin,1),
			initVal: memSize,
			labelWidth: 120,
			unitWidth: 400,
			action:{|ez| ez.round=ez.value;memSize=ez.value}
		);

		var audioBusTxt = EZNumber(window,label:" Audio Bus Channels ",
			controlSpec: ControlSpec.new(0.0,inf,\lin,1),
			initVal: nbAudioBusChannels,
			labelWidth: 120,
			unitWidth: 400,
			action:{|ez| ez.round=ez.value;nbAudioBusChannels=ez.value}
		);

		var inputBusTxt = EZNumber(window,label:" Input Bus Channels ",
			controlSpec: ControlSpec.new(0.0,inf,\lin,1),
			initVal: nbInputBusChannels,
			labelWidth: 120,
			unitWidth: 400,
			action:{|ez| ez.round=ez.value;nbInputBusChannels=ez.value}
		);
		var outputBusTxt = EZNumber(window,label:" Input Bus Channels ",
			controlSpec: ControlSpec.new(0.0,inf,\lin,1),
			initVal: nbOutputBusChannels,
			labelWidth: 120,
			unitWidth: 400,
			action:{|ez| ez.round=ez.value;nbOutputBusChannels=ez.value}
		);








		moscaOptions.add(StaticText.new().string_("Mosca options"),0,\center);
		moscaOptions.add(StaticText.new().string_("Mosca options"),0,\center);
		//serveroptions
		serverOptions.add(StaticText.new().string_("Server options"),0,\center);
		serverOptions.add(blockSizeTxt.view);
		serverOptions.add(memSizeTxt.view);
		serverOptions.add(audioBusTxt.view);
		serverOptions.add(inputBusTxt.view);
		serverOptions.add(outputBusTxt.view);


		startButton = Button.new().string_("Start Server");
		cancelButton = Button.new().string_("Cancel");

		// startButton.action = {this.prStartServer};
		startButton.action = {"hello world".postln();};
		cancelButton.action = {this.prCancel};

		bottom.add(startButton);
		bottom.add(cancelButton);

		main.add(moscaOptions);
		main.add(serverOptions);
		layout.add(main);
		layout.add(bottom,1);

		window.layout = layout;
		window.front;
	}

}