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

+ Mosca {

	gui { | width = 800, guiint = 0.07 |

		var furthest,
		itensdemenu,
		event, brec, bplay, bload, bstream, loadOrStream, bnodes, meters,
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
		sourceName,
		sourceList,
		baux,
		bsalvar,
		bcarregar,
		sourceSelect,
		moveSource,
		zoom_factor = 1,
		zSliderHeight,
		lastGui = Main.elapsedTime,
		halfwidth, height, halfheight,
		halfPi = MoscaUtils.halfPi(),
		iguiint;

		guiflag = true;

		if (width < 600) {
			width = 600;
		};

		halfwidth = width * 0.5;
		height = width; // on init
		halfheight = halfwidth;
		zSliderHeight = height * 2 / 3;

		currentsource = 0;
		iguiint = guiint;

		// Note there is an extreme amount repetition occurring here.
		// See the calling function. fix

		win = Window("Mosca", Rect(0, 0, width, height)).front;
		win.background = Color.new255( 200, 200, 200 ); // OSSIA/score "HalfLight"

		win.drawFunc = {

			Pen.fillColor = Color.new255(0, 127, 229, 76); // OSSIA/score "Transparent1"
			Pen.addArc(halfwidth@halfheight, halfheight * zoom_factor, 0, 2pi);
			Pen.fill;

			Pen.strokeColor = Color.new255(37, 41, 48, 40);
			Pen.addArc(halfwidth@halfheight, halfheight * zoom_factor * 0.25, 0, 2pi);
			Pen.stroke;

			nfontes.do { |i|
				var x, y;
				var topView = spheval[i] * zoom_factor;
				var lev = topView.z;
				{x = halfwidth + (topView.x * halfheight)}.defer;
				{y = halfheight - (topView.y * halfheight)}.defer;
				Pen.addArc(x@y, max(14 + (lev * halfheight * 0.02), 0), 0, 2pi);
				if ((audit[i] || isPlay) && (lev.abs <= MoscaUtils.plim())) {
					Pen.fillColor = Color.new255(179, 90, 209, 55 + (cboxProxy[i].value * 200));
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
			if (period > iguiint) {
				lastGui =  Main.elapsedTime;
				{
					{ zlev[currentsource] = spheval[currentsource].z; }.defer;
					{ zslider.value = (zlev[currentsource] + 1)
						* 0.5; }.defer;
					{ znumbox.value = zlev[currentsource]; }.defer;
					{ win.refresh; }.defer;
				}.defer(iguiint);
			};
		};

		updateGuiCtl = { |ctl, num|
			var chans = ncan[currentsource];

			switch (ctl,
				\chan,
				{ case
					{ chans.value == 1 }
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
					{ chans.value == 2 }
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
						winCtl[0][7].value = ossiaangle[currentsource].v;
						winCtl[1][7].value = ossiaangle[currentsource].v / pi;
					}
					{ chans.value >= 4 }
					{
						winCtl[0][7].visible = false;
						winCtl[1][7].visible = false;
						winCtl[2][7].visible = false;
						winCtl[0][8].visible = true;
						winCtl[1][8].visible = true;
						winCtl[2][8].visible = true;
						winCtl[0][8].value = ossiarot[currentsource].v;
						winCtl[1][8].value = (ossiarot[currentsource].v + pi) / 2pi;
						if ( libnumbox.value == (lastN3D + 1) ) {
							winCtl[0][9].visible = true;
							winCtl[1][9].visible = true;
							winCtl[2][9].visible = true;
							winCtl[0][9].value = ossiadir[currentsource].v;
							winCtl[1][9].value = ossiadir[currentsource].v / halfPi;
						} {
							winCtl[0][9].visible = false;
							winCtl[1][9].visible = false;
							winCtl[2][9].visible = false;
						};
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

						spreadcheck.value = ossiaspread[currentsource].v;

						diffusecheck.value = ossiadiff[currentsource].v;

						if ( chans.value >= 4 ) {
							winCtl[0][9].visible = true;
							winCtl[1][9].visible = true;
							winCtl[2][9].visible = true;
							winCtl[0][9].value = ossiadir[currentsource].v;
							winCtl[1][9].value = ossiadir[currentsource].v / halfPi;
						} {
							winCtl[0][9].visible = false;
							winCtl[1][9].visible = false;
							winCtl[2][9].visible = false;
						};
					}
					{ libnumbox.value == lastFUMA }
					{
						spreadcheck.visible = false;
						diffusecheck.visible = false;
						winCtl[0][9].visible = false;
						winCtl[1][9].visible = false;
						winCtl[2][9].visible = false;

						winCtl[0][10].visible = true;
						winCtl[1][10].visible = true;
						winCtl[2][10].visible = true;
						winCtl[0][11].visible = true;
						winCtl[1][11].visible = true;
						winCtl[2][11].visible = true;
						winCtl[0][12].visible = true;
						winCtl[1][12].visible = true;
						winCtl[2][12].visible = true;

						winCtl[0][10].value = rateboxProxy[currentsource].value;
						winCtl[1][10].value = (rateboxProxy[currentsource].value - 1) / 59;
						winCtl[0][11].value = winboxProxy[currentsource].value;
						winCtl[1][11].value = winboxProxy[currentsource].value * 5;
						winCtl[0][12].value = randboxProxy[currentsource].value;
						winCtl[1][12].value = randboxProxy[currentsource].value.sqrt;
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
						winCtl[0][9].visible = false;
						winCtl[1][9].visible = false;
						winCtl[2][9].visible = false;
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
						winCtl[0][4].value = ossiadstam[currentsource].v;
						winCtl[1][4].value = ossiadstam[currentsource].v;
						winCtl[0][5].value = ossiadstdec[currentsource].v;
						winCtl[1][5].value = ossiadstdec[currentsource].v;
						winCtl[0][6].value = ossiadstdec[currentsource].v;
						winCtl[1][6].value = ossiadstdec[currentsource].v;
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
						winCtl[0][4].value = ossiadstam[currentsource].v;
						winCtl[1][4].value = ossiadstam[currentsource].v;
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
						{ loopcheck.value = ossialoop[currentsource].v; }.defer;
					}
					{hwn[currentsource]}
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
						hwCtl[0][0].value =
						switch(ncan[currentsource],
							1, { 0 },
							2, { 1 },
							4, { 2 },
							9, { 3 },
							16, { 4 },
							25, { 5 },
							36, { 6 });
						hwCtl[0][1].value = busini[currentsource];
					}
					{scn[currentsource]}
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
						hwCtl[0][0].value =
						switch(ncan[currentsource],
							1, { 0 },
							2, { 1 },
							4, { 2 },
							9, { 3 },
							16, { 4 },
							25, { 5 },
							36, { 6 });
					};
				};
			);
		};

		wdados = Window("Data", Rect(width, 0, 960, (nfontes*20)+60 ),
			scroll: true);
		wdados.userCanClose = false;
		wdados.alwaysOnTop = true;

		dialView = UserView(win, Rect(width - 100, 10, 180, 120));

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

		waux = Window("Auxiliary Controllers", Rect(width, (nfontes*20)+114,
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


		auxslider1 = Slider(waux, Rect(40, 20, 20, 160));
		auxslider2 = Slider(waux, Rect(80, 20, 20, 160));
		auxslider3 = Slider(waux, Rect(120, 20, 20, 160));
		auxslider4 = Slider(waux, Rect(160, 20, 20, 160));
		auxslider5 = Slider(waux, Rect(200, 20, 20, 160));

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

		bnodes = Button(dialView, Rect(0, 40, 90, 20))
		.states_([
			["show nodes", Color.black, Color.white],
		])
		.action_({
			server.plotTree;
		});

		meters = Button(dialView, Rect(0, 60, 90, 20))
		.states_([
			["show meters", Color.black, Color.white],
		])
		.action_({
			server.meter;
		});

		brecaudio = Button(dialView, Rect(0, 80, 90, 20))
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
				//server.recChannels = numoutputs;
				// note the 2nd bus argument only works in SC 3.9
				server.record((prjDr ++ "/out.wav").standardizePath, node:globDec);

			}
			{
				server.stopRecording;
				"Recording stopped".postln;
			};

		});

		blipcheck = CheckBox(dialView, Rect(35, 100, 50, 15), "blips").action_({ | butt |
			if(butt.value) {
				//"Looping transport".postln;
				//autoloopval = true;
			} {
				//		autoloopval = false;
			};

		});

		winCtl = Array.newClear(3); // [0]numboxes, [1]sliders, [2]texts
		winCtl[0] = Array.newClear(13);
		winCtl[1] = Array.newClear(13);
		winCtl[2] = Array.newClear(13);

		baudi = Button(win,Rect(10, 90, 150, 20))
		.states_([
			["Audition", Color.black, Color.green],
			["Stop", Color.white, Color.red]
		])
		.action_({ | but |
			var bool = but.value.asBoolean;

			ossiaaud[currentsource].v_(bool);
			{ win.refresh; }.defer;
		});

		autoView = UserView(win);

		StaticText(autoView, Rect(0, 40, 325, 20)).string_("Scale Factor");

		scaleBox = NumberBox(autoView, Rect(285, 40, 40, 20));
		scaleBox.value = 1;
		scaleBox.clipHi = 10;
		scaleBox.clipLo = 0.01;
		scaleBox.step_(0.01);
		scaleBox.scroll_step_(0.01);
		scaleBox.action = { | num |
			scalefactProxy.valueAction = num.value;
			{scaleslider.value = num.value.curvelin(inMin:0.01, inMax:10, curve:4);}.defer;
		};

		scaleslider = Slider(autoView, Rect(80, 40, 205, 20));
		scaleslider.value = 0.46059446575451;
		scaleslider.action = { | num |
			{scaleBox.valueAction = num.value.lincurve(outMin:0.01, outMax:10, curve:4);}.defer;
		};

		StaticText(autoView, Rect(0, 60, 325, 20)).string_("Master Level");

		masterBox = NumberBox(autoView, Rect(285, 60, 40, 20));
		masterBox.value = 0;
		masterBox.clipHi = 12;
		masterBox.clipLo = -96;
		masterBox.step_(0.01);
		masterBox.scroll_step_(0.01);
		masterBox.action = { | num |
			masterlevProxy.valueAction = num.value;
			{masterslider.value = num.value.curvelin(inMin:-96, inMax:12, curve:-3);}.defer;
		};

		masterslider = Slider(autoView, Rect(80, 60, 205, 20));
		masterslider.value = 0.62065661124753;
		masterslider.action = { | num |
			{masterBox.valueAction = num.value.lincurve(outMin:-96, outMax:12, curve:-3);}.defer;
		};

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
			dwin = Window(title, bounds);
			dwin.onClose = {
				if (success.not){
					onFailure.value(textField.value);
					"Aborted save".postln;
				};
			};
			textField = TextField(dwin, Rect(0, 0, bounds.width, bounds.height));
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
			dwin = Window(title, bounds);
			dwin.onClose = {
				if (success.not){
					onFailure.value(textField.value);
					"Aborted load!".postln;
				};
			};
			textField = TextField(dwin, Rect(0, 0, bounds.width, bounds.height));
			textField.value = preset;
			textField.action = {
				success = true;
				onSuccess.value(textField.value);
				dwin.close;
				lastAutomation = textField.value;
				this.loadAutomationData(textField.value);
			};
			dwin.front;
		});


		// seleção de fontes
		itensdemenu = Array.newClear(nfontes);
		nfontes.do { | i |
			itensdemenu[i] = "Source " ++ (i + 1).asString;
		};

		sourceName = StaticText(win, Rect(10, 10, 150, 20)).string_("Source 1");

		sourceSelect = { | item |
			currentsource = item.value;
			sourceName.string = itensdemenu[currentsource];
			updateGuiCtl.value(\chan);

			loopcheck.value = lpcheckProxy[currentsource].value;

			updateGuiCtl.value(\lib, libboxProxy[currentsource].value);

			updateGuiCtl.value(\dstrv, dstrvboxProxy[currentsource].value);

			updateGuiCtl.value(\src);

			winCtl[0][0].value = vboxProxy[currentsource].value;
			winCtl[1][0].value = vboxProxy[currentsource].value.curvelin(inMin:-96, inMax:12, curve:-3);
			winCtl[0][1].value = dpboxProxy[currentsource].value;
			winCtl[1][1].value = dpboxProxy[currentsource].value;
			winCtl[1][2].value = cboxProxy[currentsource].value;
			winCtl[1][2].value = cboxProxy[currentsource].value;
			winCtl[0][3].value = ossiaclsam[currentsource].v;
			winCtl[1][3].value = ossiaclsam[currentsource].v;

			zslider.value = (zlev[currentsource] + 1) * 0.5;
			znumbox.value = zlev[currentsource];

			auxslider1.value = a1boxProxy[currentsource].value;
			aux1numbox.value = a1boxProxy[currentsource].value;
			auxslider2.value = a2boxProxy[currentsource].value;
			aux2numbox.value = a2boxProxy[currentsource].value;
			auxslider3.value = a3boxProxy[currentsource].value;
			aux3numbox.value = a3boxProxy[currentsource].value;
			auxslider4.value = a4boxProxy[currentsource].value;
			aux4numbox.value = a4boxProxy[currentsource].value;
			auxslider5.value = a5boxProxy[currentsource].value;
			aux5numbox.value = a5boxProxy[currentsource].value;

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
		hwCtl[0][0] = PopUpMenu(win, Rect(10, 50, 40, 20));
		hwCtl[0][0].items = ["1", "2", "4", "9", "16", "25"];
		hwCtl[0][0].action = { | num |
			var nbChans = [1, 2, 4, 9, 16, 25];
			{ncanbox[currentsource].valueAction = nbChans[num.value];}.defer;
		};
		hwCtl[0][0].visible = false;

		hwCtl[1][1] = StaticText(win, Rect(55, 70, 240, 20));
		hwCtl[1][1].string = "Start Bus";
		hwCtl[1][1].visible = false;
		hwCtl[0][1] = NumberBox(win, Rect(10, 70, 40, 20));
		hwCtl[0][1].value = 0;
		hwCtl[0][1].clipLo = 0;
		hwCtl[0][1].step = 1;
		hwCtl[0][1].scroll_step = 1;
		hwCtl[0][1].align = \center;
		hwCtl[0][1].action = { | num |
			{businibox[currentsource].valueAction = num.value;}.defer;
			busini[currentsource] = num.value;
		};
		hwCtl[0][1].visible = false;


		/////////////////////////////////////////////////////////


		StaticText(win, Rect(163, 110, 240, 20)).string_("Library");
		libnumbox = PopUpMenu( win, Rect(10, 110, 150, 20));
		libnumbox.items = spatList;
		libnumbox.action_({ | num |
			{libbox[currentsource].valueAction = num.value;}.defer;
		});
		libnumbox.value = 0;


		/////////////////////////////////////////////////////////


		zAxis = StaticText(win);
		zAxis.string = "Z-Axis";
		znumbox = NumberBox(win);
		znumbox.value = 0;
		znumbox.decimals = 2;
		//znumbox.clipHi = 100;
		//znumbox.clipLo = -100;
		znumbox.step_(0.01);
		znumbox.scroll_step_(0.01);
		znumbox.align = \center;
		znumbox.action = { | num |
			{ zslider.value = (num.value * 0.5) + 0.5;
				if(ossiaorient.v == [0, 0, 0]) {
					zboxProxy[currentsource].valueAction = num.value + origine.z;
					// exeption to record z mouvements after XY automation
				} {
				spheval[currentsource] = spheval[currentsource].asCartesian.z_(num.value).asSpherical;

				ossiasphe[currentsource].v_([spheval[currentsource].rho,
						(spheval[currentsource].theta - halfPi).wrap(-pi, pi),
					spheval[currentsource].phi]);
				};

			}.defer;
		};

		zslider = Slider(win);
		zslider.value = 0.5;
		zslider.action = { | num |
			{ znumbox.valueAction = num.value - 0.5 * 2; }.defer;
		};


		////////////////////////////// Orientation //////////////


		originView = UserView(win);

		originCtl = Array.newClear(2); // [0]numboxes, [1]texts
		originCtl[0] = Array.newClear(8);
		originCtl[1] = Array.newClear(8);

		originCtl[0][2] = NumberBox(originView, Rect(210, 20, 40, 20));
		originCtl[0][2].align = \center;
		originCtl[0][2].clipHi = pi;
		originCtl[0][2].clipLo = -pi;
		originCtl[0][2].step_(0.01);
		originCtl[0][2].scroll_step_(0.01);

		originCtl[0][3] = NumberBox(originView, Rect(210, 40, 40, 20));
		originCtl[0][3].align = \center;
		originCtl[0][3].clipHi = pi;
		originCtl[0][3].clipLo = -pi;
		originCtl[0][3].step_(0.01);
		originCtl[0][3].scroll_step_(0.01);

		originCtl[0][4] = NumberBox(originView, Rect(210, 60, 40, 20));
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

		originCtl[1][2] = StaticText(originView, Rect(195, 20, 12, 22));
		originCtl[1][2].string = "H:";
		originCtl[1][3] = StaticText(originView, Rect(195, 40, 12, 22));
		originCtl[1][3].string = "P:";
		originCtl[1][4] = StaticText(originView, Rect(195, 60, 12, 22));
		originCtl[1][4].string = "R:";

		StaticText(originView, Rect(207, 0, 45, 20)).string_("Orient.");

		originCtl[0][5] = NumberBox(originView, Rect(150, 20, 40, 20));
		originCtl[0][5].align = \center;
		originCtl[0][5].step_(0.01);
		originCtl[0][5].scroll_step_(0.01);

		originCtl[0][6] = NumberBox(originView, Rect(150, 40, 40, 20));
		originCtl[0][6].align = \center;
		originCtl[0][6].step_(0.01);
		originCtl[0][6].scroll_step_(0.01);

		originCtl[0][7] = NumberBox(originView, Rect(150, 60, 40, 20));
		originCtl[0][7].align = \center;
		originCtl[0][7].step_(0.01);
		originCtl[0][7].scroll_step_(0.01);

		originCtl[0][5].action = { | num |
			oxnumboxProxy.valueAction = num.value;
		};

		originCtl[0][6].action = { | num |
			oynumboxProxy.valueAction = num.value;
		};

		originCtl[0][7].action = { | num |
			oznumboxProxy.valueAction = num.value;
		};

		originCtl[1][5] = StaticText(originView, Rect(135, 20, 12, 22));
		originCtl[1][5].string = "X:";
		originCtl[1][6] = StaticText(originView, Rect(135, 40, 12, 22));
		originCtl[1][6].string = "Y:";
		originCtl[1][7] = StaticText(originView, Rect(135, 60, 12, 22));
		originCtl[1][7].string = "Z:";

		StaticText(originView, Rect(150, 0, 47, 20)).string_("Origin");

		hdtrkcheck = CheckBox(win, text:"Track").action_({ | butt |
			this.remoteCtl(butt.value);
		});
		hdtrkcheck.value = true;

		////////////////////////////////////////////////////////////


		winCtl[2][0] = StaticText(win, Rect(163, 130, 50, 20));
		winCtl[2][0].string = "Level";
		winCtl[0][0] = NumberBox(win, Rect(10, 130, 40, 20));
		winCtl[0][0].value = 0;
		winCtl[0][0].clipHi = 12;
		winCtl[0][0].clipLo = -96;
		winCtl[0][0].step_(0.1);
		winCtl[0][0].scroll_step_(0.1);
		winCtl[0][0].align = \center;
		winCtl[0][0].action = { | num |
			{vbox[currentsource].valueAction = num.value;}.defer;
		};

		winCtl[1][0] = Slider(win, Rect(50, 130, 110, 20));
		winCtl[1][0].value = 0.62065661124753;
		winCtl[1][0].action = { | num |
			{vbox[currentsource].valueAction = num.value.lincurve(outMin:-96, outMax:12, curve:-3);}.defer;
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

		winCtl[1][2] = Slider(win, Rect(50, 170, 110, 20));
		winCtl[1][2].value = 1;
		winCtl[1][2].action = { | num |
			{cbox[currentsource].valueAction = num.value;}.defer;
		};


		/////////////////////////////////////////////////////////////////////////


		StaticText(originView, Rect(0, 0, 150, 20)).string_("Close Reverb");
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


		StaticText(win, Rect(163, 190, 150, 20)).string_("Distant Reverb");
		dstReverbox = PopUpMenu(win, Rect(10, 190, 150, 20));
		dstReverbox.items = ["no-reverb", "freeverb", "allpass"] ++ rirList;
		// add the list of impule response if one is provided

		dstReverbox.action_({ | num |
			{dstrvbox[currentsource].valueAction = num.value;}.defer;
		});
		dstReverbox.value = 0;


		/////////////////////////////////////////////////////////////////////////


		winCtl[2][4] = StaticText(win, Rect(163, 210, 150, 20));
		winCtl[2][4].string = "Distant amount";
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
		winCtl[2][5].string = "Distant room/delay";
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

		winCtl[1][5] = Slider(win, Rect(50, 230, 110, 20));
		winCtl[1][5].value = 0.5;
		winCtl[1][5].action = { | num |
			{rmbox[currentsource].valueAction = num.value;}.defer;
		};
		winCtl[1][5].visible = false;


		/////////////////////////////////////////////////////////////////////////


		winCtl[2][6] = StaticText(win, Rect(163, 250, 150, 20));
		winCtl[2][6].string = "Distant damp/decay";
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

		winCtl[1][6] = Slider(win, Rect(50, 250, 110, 20));
		winCtl[1][6].value = 0.5;
		winCtl[1][6].action = { | num |
			{dmbox[currentsource].valueAction = num.value;}.defer;
		};
		winCtl[1][6].visible = false;


		/////////////////////////////////////////////////////////////////////////


		winCtl[2][3] = StaticText(win, Rect(163, 270, 150, 20));
		winCtl[2][3].string = "Close amount";
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

		winCtl[1][3] = Slider(win, Rect(50, 270, 110, 20));
		winCtl[1][3].value = 0;
		winCtl[1][3].action = { | num |
			{gbox[currentsource].valueAction = num.value;}.defer;
		};
		winCtl[1][3].visible = false;


		/////////////////////////////////////////////////////////////////////////


		winCtl[2][7] = StaticText(win, Rect(163, 290, 100, 20));
		winCtl[2][7].string = "Stereo angle";
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
			if(ncan[currentsource] == 2) {
				espacializador[currentsource].set(\angle, num.value);
				this.setSynths(currentsource, \angle, num.value);
			};
		};
		winCtl[0][7].visible = false;

		winCtl[1][7] = Slider(win, Rect(50, 290, 110, 20));
		//	b = ControlSpec(0.0, 3.14, \linear, 0.01); // min, max, mapping, step
		winCtl[1][7].value = 1.0471975511966 / pi;
		winCtl[1][7].action = { | num |
			{abox[currentsource].valueAction = num.value * pi;}.defer;
			if(ncan[currentsource] == 2) {
				//			espacializador[currentsource].set(\angle,
				//b.map(num.value));
				espacializador[currentsource].set(\angle, num.value * pi);
				this.setSynths(currentsource, \angle, num.value * pi);
				//			angle[currentsource] = b.map(num.value);
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

		winCtl[1][8] = Slider(win, Rect(50, 290, 110, 20));
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

		winCtl[1][9] = Slider(win, Rect(50, 310, 110, 20));
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

		winCtl[1][10] = Slider(win, Rect(50, 330, 110, 20));
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

		winCtl[1][11] = Slider(win, Rect(50, 350, 110, 20));
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

		StaticText(wdados, Rect(20, 20, 50, 20))
		.font_(Font(Font.defaultSansFace, 9))
		.string_("Lib");
		StaticText(wdados, Rect(45, 20, 50, 20))
		.font_(Font(Font.defaultSansFace, 9))
		.string_("Rv");
		StaticText(wdados, Rect(70, 20, 50, 20))
		.font_(Font(Font.defaultSansFace, 9))
		.string_("Lp");
		StaticText(wdados, Rect(85, 20, 50, 20))
		.font_(Font(Font.defaultSansFace, 9))
		.string_("Hw");
		StaticText(wdados, Rect(100, 20, 50, 20))
		.font_(Font(Font.defaultSansFace, 9))
		.string_("Sc");

		StaticText(wdados, Rect(115, 20, 50, 20))
		.font_(Font(Font.defaultSansFace, 9))
		.string_("Sp");
		StaticText(wdados, Rect(130, 20, 50, 20))
		.font_(Font(Font.defaultSansFace, 9))
		.string_("Df");

		StaticText(wdados, Rect(145, 20, 50, 20))
		.font_(Font(Font.defaultSansFace, 9))
		.string_("NCan");
		StaticText(wdados, Rect(170, 20, 50, 20))
		.font_(Font(Font.defaultSansFace, 9))
		.string_("SBus");

		StaticText(wdados, Rect(208, 20, 50, 20))
		.font_(Font(Font.defaultSansFace, 9))
		.string_("X");
		StaticText(wdados, Rect(241, 20, 50, 20))
		.font_(Font(Font.defaultSansFace, 9))
		.string_("Y");

		StaticText(wdados, Rect(274, 20, 50, 20))
		.font_(Font(Font.defaultSansFace, 9))
		.string_("Z");

		StaticText(wdados, Rect(300, 20, 50, 20))
		.font_(Font(Font.defaultSansFace, 9))
		.string_("Lev");
		StaticText(wdados, Rect(325, 20, 50, 20))
		.font_(Font(Font.defaultSansFace, 9))
		.string_("DAmt");
		StaticText(wdados, Rect(350, 20, 50, 20))
		.font_(Font(Font.defaultSansFace, 9))
		.string_("Prox");
		StaticText(wdados, Rect(375, 20, 50, 20))
		.font_(Font(Font.defaultSansFace, 9))
		.string_("Dist");
		StaticText(wdados, Rect(400, 20, 50, 20))
		.font_(Font(Font.defaultSansFace, 9))
		.string_("Room");
		StaticText(wdados, Rect(425, 20, 50, 20))
		.font_(Font(Font.defaultSansFace, 9))
		.string_("Damp");
		StaticText(wdados, Rect(450, 20, 50, 20))
		.font_(Font(Font.defaultSansFace, 9))
		.string_("Ang");
		StaticText(wdados, Rect(475, 20, 50, 20))
		.font_(Font(Font.defaultSansFace, 9))
		.string_("Rot");
		StaticText(wdados, Rect(500, 20, 50, 20))
		.font_(Font(Font.defaultSansFace, 9))
		.string_("Dir");
		StaticText(wdados, Rect(525, 20, 50, 20))
		.font_(Font(Font.defaultSansFace, 9))
		.string_("Cont");
		StaticText(wdados, Rect(550, 20, 50, 20))
		.font_(Font(Font.defaultSansFace, 9))
		.string_("Rate");
		StaticText(wdados, Rect(575, 20, 50, 20))
		.font_(Font(Font.defaultSansFace, 9))
		.string_("Win");
		StaticText(wdados, Rect(600, 20, 50, 20))
		.font_(Font(Font.defaultSansFace, 9))
		.string_("Rand");

		StaticText(wdados, Rect(625, 20, 50, 20))
		.font_(Font(Font.defaultSansFace, 9))
		.string_("A1");
		StaticText(wdados, Rect(650, 20, 50, 20))
		.font_(Font(Font.defaultSansFace, 9))
		.string_("A2");
		StaticText(wdados, Rect(675, 20, 50, 20))
		.font_(Font(Font.defaultSansFace, 9))
		.string_("A3");
		StaticText(wdados, Rect(700, 20, 50, 20))
		.font_(Font(Font.defaultSansFace, 9))
		.string_("A4");
		StaticText(wdados, Rect(725, 20, 50, 20))
		.font_(Font(Font.defaultSansFace, 9))
		.string_("A5");

		StaticText(wdados, Rect(750, 20, 50, 20))
		.font_(Font(Font.defaultSansFace, 9))
		.string_("a1");
		StaticText(wdados, Rect(765, 20, 50, 20))
		.font_(Font(Font.defaultSansFace, 9))
		.string_("a2");
		StaticText(wdados, Rect(780, 20, 50, 20))
		.font_(Font(Font.defaultSansFace, 9))
		.string_("a3");
		StaticText(wdados, Rect(795, 20, 50, 20))
		.font_(Font(Font.defaultSansFace, 9))
		.string_("a4");
		StaticText(wdados, Rect(810, 20, 50, 20))
		.font_(Font(Font.defaultSansFace, 9))
		.string_("a5");


		StaticText(wdados, Rect(825, 20, 50, 20))
		.font_(Font(Font.defaultSansFace, 9))
		.string_("File");

		StaticText(wdados, Rect(925, 20, 50, 20))
		.font_(Font(Font.defaultSansFace, 9))
		.string_("St");


		nfontes.do { | i |

			StaticText(wdados, Rect(2, 40 + (i*20), 50, 20))
			.font_(Font(Font.defaultSansFace, 9))
			.string_((i+1).asString);

			libbox[i] = NumberBox(wdados, Rect(20, 40 + (i*20), 25, 20));

			dstrvbox[i] = NumberBox(wdados, Rect(45, 40 + (i*20), 25, 20));

			lpcheck[i] = CheckBox(wdados, Rect(70, 40 + (i*20), 40, 20));

			lpcheck[i].action_({ | but |
				lpcheckProxy[i].valueAction = but.value;
			});

			hwncheck[i] = CheckBox( wdados, Rect(85, 40 + (i*20), 40, 20));

			hwncheck[i].action_({ | but |
				hwncheckProxy[i].valueAction = but.value;
			});


			scncheck[i] = CheckBox( wdados, Rect(100, 40 + (i*20), 40, 20));

			scncheck[i].action_({ | but |
				scncheckProxy[i].valueAction = but.value;
			});

			spcheck[i] = CheckBox(wdados, Rect(115, 40 + (i*20), 40, 20));

			spcheck[i].action_({ | but |
				spcheckProxy[i].valueAction = but.value;
			});



			dfcheck[i] = CheckBox(wdados, Rect(130, 40 + (i*20), 40, 20));

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

			a1check[i] = CheckBox( wdados, Rect(750, 40 + (i*20), 40, 20));
			a1check[i].action = { | but |
				a1checkProxy[i].valueAction = but.value;
			};



			a2check[i] = CheckBox( wdados, Rect(765, 40 + (i*20), 40, 20));
			a2check[i].action = { | but |
				a2checkProxy[i].valueAction = but.value;
			};


			a3check[i] = CheckBox( wdados, Rect(780, 40 + (i*20), 40, 20));
			a3check[i].action = { | but |
				a3checkProxy[i].valueAction = but.value;
			};


			a4check[i] = CheckBox( wdados, Rect(795, 40 + (i*20), 40, 20));
			a4check[i].action = { | but |
				a4checkProxy[i].valueAction = but.value;
			};


			a5check[i] = CheckBox( wdados, Rect(810, 40 + (i*20), 40, 20));
			a5check[i].action = { | but |
				a5checkProxy[i].valueAction = but.value;
			};



			tfield[i] = TextField(wdados, Rect(825, 40+ (i*20), 100, 20));

			stcheck[i] = CheckBox( wdados, Rect(925, 40 + (i*20), 40, 20));
			stcheck[i].action = { | but |
				stcheckProxy[i].valueAction = but.value;
			};

			StaticText(wdados, Rect(940, 40 + (i*20), 50, 20))
			.font_(Font(Font.defaultSansFace, 9))
			.string_((i+1).asString);

			libbox[i].font = Font(Font.defaultSansFace, 9);
			dstrvbox[i].font = Font(Font.defaultSansFace, 9);
			ncanbox[i].font = Font(Font.defaultSansFace, 9);
			businibox[i].font = Font(Font.defaultSansFace, 9);
			xbox[i].font = Font(Font.defaultSansFace, 9);
			ybox[i].font = Font(Font.defaultSansFace, 9);
			zbox[i].font = Font(Font.defaultSansFace, 9);

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
			ybox[i].value = 20;


			zbox[i].action = { | num |
				zboxProxy[i].valueAction = num.value;
			};


			abox[i].clipHi = pi;
			abox[i].clipLo = 0;
			vbox[i].clipHi = 12;
			vbox[i].clipLo = -96;
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
			libbox[i].clipHi = spatList.size -1;
			libbox[i].clipLo = 0;
			businibox[i].clipLo = 0;

			vbox[i].scroll_step = 0.1;
			abox[i].scroll_step = 0.01;
			vbox[i].step = 0.1;
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
			businibox[i].scroll_step = 1;
			businibox[i].step = 1;


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

		control.front(autoView, Rect(0, 0, 325, 20));

		furthest = halfheight * 20;

		win.view.mouseDownAction = { | view, mx, my, modifiers, buttonNumber, clickCount |
			mouseButton = buttonNumber; // 0 = left, 2 = middle, 1 = right

			if (sourceList.notNil) {
				sourceList.close;
				sourceList = nil;
			};

			case
			{mouseButton == 0} {
				var x = ((mx - halfwidth) / halfheight) / zoom_factor,
				y = ((halfheight - my) / halfheight) / zoom_factor,
				closest = [0, furthest]; // save sources index and distance from click
				// initialize at the furthest point

				nfontes.do { | i |
					var dis = ((x - spheval[i].x).squared
						+ (y - spheval[i].y).squared).sqrt;
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
				sourceList = ListView(win, Rect(mx,my,
					90,70)).items_(itensdemenu).value_(-1) // to avoid the deffault to 1
				.action_({ |sel|
					sourceSelect.value(sel.value);
					moveSource.value(mx + 45, my + 35);
					sourceList.close;
					sourceList = nil;
				});
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
				zoom_factor = zoom_factor * 0.99;
				win.refresh;
			};
		};

		moveSource = { |x, y|

			var point = Cartesian(
				(((x - halfwidth) / halfheight) / zoom_factor),
				(((halfheight - y) / halfheight) / zoom_factor),
				znumbox.value) / scalefactProxy.value;

			if(ossiaorient.v == [0, 0, 0]) {
				xboxProxy[currentsource].valueAction = point.x + origine.x;
				// exeption to record XY mouvements after Z automation
				yboxProxy[currentsource].valueAction = point.y + origine.y;
			} {
			spheval[currentsource] = point.asSpherical;

			ossiasphe[currentsource].v_([spheval[currentsource].rho,
					(spheval[currentsource].theta - halfPi).wrap(-pi, pi),
			spheval[currentsource].phi]);
			};
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

			originView.bounds_(Rect(width - 255, height - 85, 250, 100));
			hdtrkcheck.bounds_(Rect(width - 105, height - 105, 265, 20));

			autoView.bounds_(Rect(10, height - 85, 325, 80));

			novoplot.value;

		});


		win.onClose_({

			wdados.close;
			waux.close;
			guiflag = false;

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