MoscaInitialize
{
	var window,sourceList;
	var <>windowW = 1260;
	var <>windowH = 920;
	*new
	{
		^super.new.ctr();
	}
	ctr
	{
		window = Window.new("Mosca Startup", Rect(10,1000,windowW,windowH));
		sourceList = [];
	}

	prHello
	{
		"ohioh".postln;
	}
	prStartServer{
		"Starting Server - WIP".postln;
	}
	prCancel
	{
		window.close();
		"Cancelling, quitting".postln;
	}

	gui
	{

		var startButton,cancelButton;

		startButton = Button.new(window,Rect(25,windowH-50,75,25)).string_("Start Server");
		cancelButton = Button.new(window,Rect(100,windowH-50,50,25)).string_("Cancel");

		startButton.action = {this.prStartServer};
		cancelButton.action = {this.prCancel};

		window.front;
	}

}