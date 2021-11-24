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
		var o;
		"Starting Server - WIP".postln;
		Server.killAll;
		"SC_JACK_DEFAULT_INPUTS".setenv(oscInputName);
		Server.supernova;
		server = Server.local;
		o = server.options;
		o.memSize = memSize;
		o.blockSize = blockSize;
		o.numAudioBusChannels = nbAudioBusChannels;
		o.numInputBusChannels = nbInputBusChannels;
		o.numOutputBusChannels = nbOutputBusChannels;



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

		startButton = Button.new().string_("Start Server");
		cancelButton = Button.new().string_("Cancel");

		startButton.action = {this.prStartServer};
		cancelButton.action = {this.prCancel};

		bottom.add(startButton);
		bottom.add(cancelButton);

		main.add(moscaOptions);
		main.add(serverOptions);
		layout.add(main,1);
		layout.add(bottom);

		window.layout = layout;
		window.front;
	}

}