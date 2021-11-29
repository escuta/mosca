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
			serverStarted = true;
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
		var main = GridLayout();
		var bottom = HLayout();
		var moscaOptions = VLayout();
		var serverOptions = GridLayout();
		var startButton,cancelButton;
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
		var i = 1;
		main.setColumnStretch(0,1);
		main.setColumnStretch(1,1);
		//mosca options
		moscaOptions.add(StaticText.new().string_("Mosca options"),align:\center);
		moscaOptions.add(nil,2);
		//server options
		serverOptions.addSpanning(StaticText.new().string_("Server options"),0,0,1,4,align:\center);
		serverOptions.setColumnStretch(0,2);
		serverOptions.setColumnStretch(1,1);
		serverOptions.setColumnStretch(2,1);
		serverOptions.setColumnStretch(3,2);

		[blockSizeInput,memSizeInput,audioBusInput,inputBusInput,outputBusInput,bufferInput].do{
			|ezNumber|
			ezNumber.labelView.align = \left;
			ezNumber.numberView.align = \right;
			serverOptions.add(nil,i,0);
			serverOptions.add(ezNumber.labelView,i,1);
			serverOptions.add(ezNumber.numberView,i,2);
			serverOptions.add(nil,i,3);
			i = i+1;
		};
		serverOptions.addSpanning(nil,i,0,1,4);
		serverOptions.setRowStretch(i,2);

		//bottom row
		startButton = Button.new().string_("Start Server");
		cancelButton = Button.new().string_("Cancel");

		// startButton.action = {this.prStartServer};
		startButton.action = {"hello world".postln();};
		cancelButton.action = {this.prCancel};

		//layout addition to main window
		bottom.add(startButton);
		bottom.add(cancelButton);

		main.add(moscaOptions,0,0);
		main.add(serverOptions,0,1);
		main.add(bottom,1,0);

		window.layout = main;
		window.front;
	}

}
