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

	gui {

		var sprite,
		furthest,
		dist,
		itensdemenu,
		event, brec, bplay, bload, bstream, loadOrStream, bnodes,
		dopcheque2,
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
		textbuf,
		sourceName,
		sourceList,
		baux,
		bsalvar,
		bcarregar,
		sourceSelect,
		//m,
		moveSource,
		zoom_factor = 1,
		zSliderHeight = height * 2 / 3,
		hdtrkcheck;


		// Note there is an extreme amount repetition occurring here.
		// See the calling function. fix

		win = Window("Mosca", Rect(0, width, width, height)).front;
		win.background = Color.new255( 200, 200, 200 ); // OSSIA/score "HalfLight"

		win.drawFunc = {

			Pen.fillColor = Color.new255(0, 127, 229, 76); // OSSIA/score "Transparent1"
			Pen.addArc(halfwidth@halfheight, halfheight * zoom_factor, 0, 2pi);
			Pen.fill;

			Pen.strokeColor = Color.new255(37, 41, 48, 40);
			Pen.addArc(halfwidth@halfheight, halfheight * zoom_factor *
				0.01 * longest_radius, 0, 2pi);
			Pen.stroke;

			nfontes.do { |i|
				var x, y;
				var topView = spheval[i] * zoom_factor * 0.01;
				var lev = topView.z;
				var color = lev * 0.4;
				{x = halfwidth + (topView.x * halfheight)}.defer;
				{y = halfheight - (topView.y * halfheight)}.defer;
				Pen.addArc(x@y, 14 + (lev * 0.01 * halfheight * 2), 0, 2pi);
				if ((audit[i] || isPlay) && (lev.abs <= plim)) {
					Pen.fillColor = Color.new255(179, 90,209);
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
			if (period > guiInt) {
				lastGui =  Main.elapsedTime;
				{
					{ zlev[currentsource] = spheval[currentsource].z; }.defer;
					{ zslider.value = (zlev[currentsource] * 0.01 + 1)
						* 0.5; }.defer;
					{ znumbox.value = zlev[currentsource]; }.defer;
					{ win.refresh; }.defer;
				}.defer(guiInt);
			};
		};

		updateGuiCtl = { |ctl, num|
			switch (ctl,
				\chan,
				{ var selector;

					if (hwncheckProxy[currentsource].value.not
						&& scncheckProxy[currentsource].value.not) {
						selector = ncanais[currentsource];
					} {
						selector = ncan[currentsource];
					};

					case
					{ (selector.value < 2) || (selector.value == 3) }
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
					{ selector.value == 2 }
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
						winCtl[0][7].value = angle[currentsource];
						winCtl[1][7].value = angle[currentsource] / pi;
					}
					{ selector.value >= 4 }
					{
						winCtl[0][7].visible = false;
						winCtl[1][7].visible = false;
						winCtl[2][7].visible = false;
						winCtl[0][8].visible = true;
						winCtl[1][8].visible = true;
						winCtl[2][8].visible = true;
						winCtl[0][9].visible = true;
						winCtl[1][9].visible = true;
						winCtl[2][9].visible = true;
						winCtl[0][8].value = rlev[currentsource];
						winCtl[1][8].value = (rlev[currentsource] + pi) / 2pi;
						winCtl[0][9].value = dlev[currentsource];
						winCtl[1][9].value = dlev[currentsource] / halfPi;
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
						if(sp[currentsource] == 1) {
							spreadcheck.value = true
						} {
							spreadcheck.value = false
						};
						if(df[currentsource] == 1) {
							diffusecheck.value = true
						}{
							diffusecheck.value = false
						};
					}
					{ libnumbox.value == lastFUMA }
					{
						spreadcheck.visible = false;
						diffusecheck.visible = false;

						winCtl[0][10].visible = true;
						winCtl[1][10].visible = true;
						winCtl[2][10].visible = true;
						winCtl[0][11].visible = true;
						winCtl[1][11].visible = true;
						winCtl[2][11].visible = true;
						winCtl[0][12].visible = true;
						winCtl[1][12].visible = true;
						winCtl[2][12].visible = true;

						winCtl[0][10].value = grainrate[currentsource];
						winCtl[1][10].value = (grainrate[currentsource] - 1) / 59;
						winCtl[0][11].value = winsize[currentsource];
						winCtl[1][11].value = winsize[currentsource] * 5;
						winCtl[0][12].value = winrand[currentsource];
						winCtl[1][12].value = winrand[currentsource].sqrt;
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
						winCtl[0][4].value = llev[currentsource];
						winCtl[1][4].value = llev[currentsource];
						winCtl[0][5].value = rm[currentsource];
						winCtl[1][5].value = rm[currentsource];
						winCtl[0][6].value = dm[currentsource];
						winCtl[1][6].value = dm[currentsource];
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
						winCtl[0][4].value = llev[currentsource];
						winCtl[1][4].value = llev[currentsource];
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
						{ loopcheck.value = lp[currentsource].value; }.defer;
					}
					{hwn[currentsource] == 1}
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
						hwCtl[0][0].value = ncan[currentsource];
						hwCtl[0][1].value = busini[currentsource];
					}
					{scn[currentsource] == 1}
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
						hwCtl[0][0].value = ncan[currentsource];
					};
				};
			);
		};

		wdados = Window("Data", Rect(width, 0, 960, (nfontes*20)+60 ),
			scroll: true);
		wdados.userCanClose = false;
		wdados.alwaysOnTop = true;

		dialView = UserView(win, Rect(width - 100, 10, 180, 100));

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

		winCtl = Array.newClear(3); // [0]numboxes, [1]sliders, [2]texts
		winCtl[0] = Array.newClear(13);
		winCtl[1] = Array.newClear(13);
		winCtl[2] = Array.newClear(13);

		baudi = Button(win,Rect(10, 90, 150, 20))
		.states_([
			["audition", Color.black, Color.green],
			["stop", Color.white, Color.red]
		])
		.action_({ | but |
			var bool = but.value.asBoolean;

			ossiaaud[currentsource].v_(bool);
			{ win.refresh; }.defer;
		});


		brecaudio = Button(dialView, Rect(0, 60, 90, 20))
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
				server.recChannels = recchans;
				// note the 2nd bus argument only works in SC 3.9
				server.record((prjDr ++ "/out.wav").standardizePath, recbus);

			}
			{
				server.stopRecording;
				"Recording stopped".postln;
			};

		});

		blipcheck = CheckBox(dialView, Rect(35, 80, 50, 15), "blips").action_({ | butt |
			if(butt.value) {
				//"Looping transport".postln;
				//autoloopval = true;
			} {
				//		autoloopval = false;
			};

		});

		if (serport.notNil) {
			hdtrkcheck = CheckBox(dialView, Rect(35, 95, 60, 15), "hdtrk").action_({ | butt |
				if(butt.value) {
					hdtrk = true;
				} {
					hdtrk = false;
					headingnumboxProxy.valueAction = 0;
					pitchnumboxProxy.valueAction = 0;
					rollnumboxProxy.valueAction = 0;
				};
			});
			hdtrkcheck.value = true;
		};

		autoView = UserView(win, Rect(10, width - 45, 325, 40));

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
				control.load(textField.value);
				//control.seek;
				lastAutomation = textField.value;
				this.loadNonAutomationData(textField.value);
			};
			dwin.front;
		});


		// seleção de fontes
		itensdemenu = Array.newClear(nfontes);
		nfontes.do { | i |
			itensdemenu[i] = "Source " ++ (i + 1).asString;
		};

		sourceName = StaticText(win, Rect(10, 10, 150, 20)).string = "Source 1";

		sourceSelect = { | item |
			currentsource = item.value;
			sourceName.string = itensdemenu[currentsource];
			updateGuiCtl.value(\chan);

			loopcheck.value = lp[currentsource].value;

			updateGuiCtl.value(\lib, libboxProxy[currentsource].value);

			updateGuiCtl.value(\dstrv, dstrvboxProxy[currentsource].value);

			updateGuiCtl.value(\src);

			winCtl[0][0].value = level[currentsource];
			winCtl[1][0].value = level[currentsource] * 0.5;
			winCtl[0][1].value = dplev[currentsource];
			winCtl[1][1].value = dplev[currentsource];
			winCtl[1][2].value = clev[currentsource];
			winCtl[1][2].value = clev[currentsource];
			winCtl[0][3].value = glev[currentsource];
			winCtl[1][3].value = glev[currentsource];

			zslider.value = (zlev[currentsource] * 0.01 + 1) * 0.5;
			znumbox.value = zlev[currentsource];

			auxslider1.value = aux1[currentsource];
			aux1numbox.value = aux1[currentsource];
			auxslider2.value = aux2[currentsource];
			aux2numbox.value = aux2[currentsource];
			auxslider3.value = aux3[currentsource];
			aux3numbox.value = aux3[currentsource];
			auxslider4.value = aux4[currentsource];
			aux4numbox.value = aux4[currentsource];
			auxslider5.value = aux5[currentsource];
			aux5numbox.value = aux5[currentsource];

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
		hwCtl[0][0] = NumberBox(win, Rect(10, 50, 40, 20));
		hwCtl[0][0].value = 0;
		hwCtl[0][0].clipHi = 36;
		hwCtl[0][0].clipLo = 0;
		hwCtl[0][0].align = \center;
		hwCtl[0][0].action = { | num |
			{ncanbox[currentsource].valueAction = num.value;}.defer;
			ncan[currentsource] = num.value;
		};
		hwCtl[0][0].visible = false;

		hwCtl[1][1] = StaticText(win, Rect(55, 70, 240, 20));
		hwCtl[1][1].string = "Start Bus";
		hwCtl[1][1].visible = false;
		hwCtl[0][1] = NumberBox(win, Rect(10, 70, 40, 20));
		hwCtl[0][1].value = 0;
		hwCtl[0][1].clipLo = 0;
		hwCtl[0][1].align = \center;
		hwCtl[0][1].action = { | num |
			{businibox[currentsource].valueAction = num.value;}.defer;
			busini[currentsource] = num.value;
		};
		hwCtl[0][1].visible = false;


		/////////////////////////////////////////////////////////


		textbuf = StaticText(win, Rect(163, 110, 240, 20));
		textbuf.string = "Library";
		libnumbox = PopUpMenu( win, Rect(10, 110, 150, 20));
		libnumbox.items = spatList;
		libnumbox.action_({ | num |
			{libbox[currentsource].valueAction = num.value;}.defer;
		});
		libnumbox.value = 0;


		/////////////////////////////////////////////////////////


		zAxis = StaticText(win, Rect(width - 80, halfwidth - 10, 90, 20));
		zAxis.string = "Z-Axis";
		znumbox = NumberBox(win, Rect(width - 45, ((width - zSliderHeight) * 0.5)
			+ zSliderHeight, 40, 20));
		znumbox.value = 0;
		znumbox.decimals = 1;
		znumbox.clipHi = 100;
		znumbox.clipLo = -100;
		znumbox.step_(0.1);
		znumbox.scroll_step_(0.1);
		znumbox.align = \center;
		znumbox.action = { | num |
			{ zslider.value = (num.value * 0.005) + 0.5;
				moveSource.value(sprite[currentsource, 0], sprite[currentsource, 1])
			}.defer;
		};

		zslider = Slider(win, Rect(width - 35, ((width - zSliderHeight) * 0.5),
			20, zSliderHeight));
		zslider.value = 0.5;
		zslider.action = { | num |
			{ znumbox.valueAction = num.value - 0.5 * 200; }.defer;
		};


		////////////////////////////// Orientation //////////////


		originView = UserView(win, Rect(width - 285, height - 85, 265, 100));

		originCtl = Array.newClear(2); // [0]numboxes, [1]texts
		originCtl[0] = Array.newClear(8);
		originCtl[1] = Array.newClear(8);

		originCtl[0][2] = NumberBox(originView, Rect(230, 20, 40, 20));
		originCtl[0][2].align = \center;
		originCtl[0][2].clipHi = pi;
		originCtl[0][2].clipLo = -pi;
		originCtl[0][2].step_(0.01);
		originCtl[0][2].scroll_step_(0.01);

		originCtl[0][3] = NumberBox(originView, Rect(230, 40, 40, 20));
		originCtl[0][3].align = \center;
		originCtl[0][3].clipHi = pi;
		originCtl[0][3].clipLo = -pi;
		originCtl[0][3].step_(0.01);
		originCtl[0][3].scroll_step_(0.01);

		originCtl[0][4] = NumberBox(originView, Rect(230, 60, 40, 20));
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

		originCtl[1][2] = StaticText(originView, Rect(215, 20, 12, 22));
		originCtl[1][2].string = "H:";
		originCtl[1][3] = StaticText(originView, Rect(215, 40, 12, 22));
		originCtl[1][3].string = "P:";
		originCtl[1][4] = StaticText(originView, Rect(215, 60, 12, 22));
		originCtl[1][4].string = "R:";

		textbuf = StaticText(originView, Rect(227, 0, 45, 20));
		textbuf.string = "Orient.";

		originCtl[0][5] = NumberBox(originView, Rect(170, 20, 40, 20));
		originCtl[0][5].align = \center;
		originCtl[0][5].step_(0.1);
		originCtl[0][5].scroll_step_(0.1);

		originCtl[0][6] = NumberBox(originView, Rect(170, 40, 40, 20));
		originCtl[0][6].align = \center;
		originCtl[0][6].step_(0.1);
		originCtl[0][6].scroll_step_(0.1);

		originCtl[0][7] = NumberBox(originView, Rect(170, 60, 40, 20));
		originCtl[0][7].align = \center;
		originCtl[0][7].step_(0.1);
		originCtl[0][7].scroll_step_(0.1);

		originCtl[0][5].action = { | num |
			oxnumboxProxy.valueAction = num.value;
		};

		originCtl[0][6].action = { | num |
			oynumboxProxy.valueAction = num.value;
		};

		originCtl[0][7].action = { | num |
			oznumboxProxy.valueAction = num.value;
		};

		originCtl[1][5] = StaticText(originView, Rect(155, 20, 12, 22));
		originCtl[1][5].string = "X:";
		originCtl[1][6] = StaticText(originView, Rect(155, 40, 12, 22));
		originCtl[1][6].string = "Y:";
		originCtl[1][7] = StaticText(originView, Rect(155, 60, 12, 22));
		originCtl[1][7].string = "Z:";

		textbuf = StaticText(originView, Rect(170, 0, 47, 20));
		textbuf.string = "Origin";


		////////////////////////////////////////////////////////////


		winCtl[2][0] = StaticText(win, Rect(163, 130, 50, 20));
		winCtl[2][0].string = "Level";
		winCtl[0][0] = NumberBox(win, Rect(10, 130, 40, 20));
		winCtl[0][0].value = 1;
		winCtl[0][0].clipHi = 2;
		winCtl[0][0].clipLo = 0;
		winCtl[0][0].step_(0.01);
		winCtl[0][0].scroll_step_(0.01);
		winCtl[0][0].align = \center;
		winCtl[0][0].action = { | num |
			{vbox[currentsource].valueAction = num.value;}.defer;
		};

		winCtl[1][0] = Slider(win, Rect(50, 130, 110, 20));
		winCtl[1][0].value = 0.5;
		winCtl[1][0].action = { | num |
			{vbox[currentsource].valueAction = num.value * 2;}.defer;
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


		textbuf = StaticText(originView, Rect(136, 0, 20, 20));
		textbuf.string = "M";

		masterslider = Slider(originView, Rect(132, 20, 20, 60));
		masterslider.orientation(\vertical);
		masterslider.value = 0.5;
		masterslider.action = { | num |
			masterlevProxy.valueAction = num.value * 2;
		};

		/////////////////////////////////////////////////////////////////////////


		textbuf = StaticText(originView, Rect(0, 0, 150, 20));
		textbuf.string = "Cls. Reverb";
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


		textbuf = StaticText(win, Rect(163, 190, 150, 20));
		textbuf.string = "Distant Reverb";
		dstReverbox = PopUpMenu(win, Rect(10, 190, 150, 20));
		dstReverbox.items = ["no-reverb", "freeverb", "allpass"] ++ rirList;
		// add the list of impule response if one is provided

		dstReverbox.action_({ | num |
			{dstrvbox[currentsource].valueAction = num.value;}.defer;
		});
		dstReverbox.value = 0;


		/////////////////////////////////////////////////////////////////////////


		winCtl[2][4] = StaticText(win, Rect(163, 210, 150, 20));
		winCtl[2][4].string = "Dst. amount";
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
		winCtl[2][5].string = "Dst. room/delay";
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
		winCtl[2][6].string = "Dst. damp/decay";
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
		winCtl[2][3].string = "Cls. amount";
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
		winCtl[2][7].string = "Stereo Angle";
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
			if((ncanais[currentsource] == 2) || (ncan[currentsource] == 2)) {
				espacializador[currentsource].set(\angle, num.value);
				this.setSynths(currentsource, \angle, num.value);
				angle[currentsource] = num.value;
			};
		};
		winCtl[0][7].visible = false;

		winCtl[1][7] = Slider(win, Rect(50, 290, 110, 20));
		//	b = ControlSpec(0.0, 3.14, \linear, 0.01); // min, max, mapping, step
		winCtl[1][7].value = 1.0471975511966 / pi;
		winCtl[1][7].action = { | num |
			{abox[currentsource].valueAction = num.value * pi;}.defer;
			if((ncanais[currentsource] == 2) || (ncan[currentsource] == 2)) {
				//			espacializador[currentsource].set(\angle,
				//b.map(num.value));
				espacializador[currentsource].set(\angle, num.value * pi);
				this.setSynths(currentsource, \angle, num.value * pi);
				//			angle[currentsource] = b.map(num.value);
				angle[currentsource] = num.value * pi;
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

		bnodes = Button(dialView, Rect(0, 40, 90, 20))
		.states_([
			["show nodes", Color.black, Color.white],
		])
		.action_({
			server.plotTree;
		});

		textbuf = StaticText(wdados, Rect(20, 20, 50, 20));
		textbuf.font = Font(Font.defaultSansFace, 9);
		textbuf.string = "Lib";
		textbuf = StaticText(wdados, Rect(45, 20, 50, 20));
		textbuf.font = Font(Font.defaultSansFace, 9);
		textbuf.string = "Rv";
		textbuf = StaticText(wdados, Rect(70, 20, 50, 20));
		textbuf.font = Font(Font.defaultSansFace, 9);
		textbuf.string = "Lp";
		textbuf = StaticText(wdados, Rect(85, 20, 50, 20));
		textbuf.font = Font(Font.defaultSansFace, 9);
		textbuf.string = "Hw";
		textbuf = StaticText(wdados, Rect(100, 20, 50, 20));
		textbuf.font = Font(Font.defaultSansFace, 9);
		textbuf.string = "Sc";

		textbuf = StaticText(wdados, Rect(115, 20, 50, 20));
		textbuf.font = Font(Font.defaultSansFace, 9);
		textbuf.string = "Sp";
		textbuf = StaticText(wdados, Rect(130, 20, 50, 20));
		textbuf.font = Font(Font.defaultSansFace, 9);
		textbuf.string = "Df";

		textbuf = StaticText(wdados, Rect(145, 20, 50, 20));
		textbuf.font = Font(Font.defaultSansFace, 9);
		textbuf.string = "NCan";
		textbuf = StaticText(wdados, Rect(170, 20, 50, 20));
		textbuf.font = Font(Font.defaultSansFace, 9);
		textbuf.string = "SBus";

		textbuf = StaticText(wdados, Rect(208, 20, 50, 20));
		textbuf.font = Font(Font.defaultSansFace, 9);
		textbuf.string = "X";
		textbuf = StaticText(wdados, Rect(241, 20, 50, 20));
		textbuf.font = Font(Font.defaultSansFace, 9);
		textbuf.string = "Y";

		textbuf = StaticText(wdados, Rect(274, 20, 50, 20));
		textbuf.font = Font(Font.defaultSansFace, 9);
		textbuf.string = "Z";

		/*
		textbuf = StaticText(wdados, Rect(299, 20, 50, 20));
		textbuf.font = Font(Font.defaultSansFace, 9);
		textbuf.string = "O.x";
		textbuf = StaticText(wdados, Rect(333, 20, 50, 20));
		textbuf.font = Font(Font.defaultSansFace, 9);
		textbuf.string = "O.y";
		textbuf = StaticText(wdados, Rect(366, 20, 50, 20));
		textbuf.font = Font(Font.defaultSansFace, 9);
		textbuf.string = "O.z";
		*/

		textbuf = StaticText(wdados, Rect(300, 20, 50, 20));
		textbuf.font = Font(Font.defaultSansFace, 9);
		textbuf.string = "Lev";
		textbuf = StaticText(wdados, Rect(325, 20, 50, 20));
		textbuf.font = Font(Font.defaultSansFace, 9);
		textbuf.string = "DAmt";
		textbuf = StaticText(wdados, Rect(350, 20, 50, 20));
		textbuf.font = Font(Font.defaultSansFace, 9);
		textbuf.string = "Prox";
		textbuf = StaticText(wdados, Rect(375, 20, 50, 20));
		textbuf.font = Font(Font.defaultSansFace, 9);
		textbuf.string = "Dist";
		textbuf = StaticText(wdados, Rect(400, 20, 50, 20));
		textbuf.font = Font(Font.defaultSansFace, 9);
		textbuf.string = "Room";
		textbuf = StaticText(wdados, Rect(425, 20, 50, 20));
		textbuf.font = Font(Font.defaultSansFace, 9);
		textbuf.string = "Damp";
		textbuf = StaticText(wdados, Rect(450, 20, 50, 20));
		textbuf.font = Font(Font.defaultSansFace, 9);
		textbuf.string = "Ang";
		textbuf = StaticText(wdados, Rect(475, 20, 50, 20));
		textbuf.font = Font(Font.defaultSansFace, 9);
		textbuf.string = "Rot";
		textbuf = StaticText(wdados, Rect(500, 20, 50, 20));
		textbuf.font = Font(Font.defaultSansFace, 9);
		textbuf.string = "Dir";
		textbuf = StaticText(wdados, Rect(525, 20, 50, 20));
		textbuf.font = Font(Font.defaultSansFace, 9);
		textbuf.string = "Cont";
		textbuf = StaticText(wdados, Rect(550, 20, 50, 20));
		textbuf.font = Font(Font.defaultSansFace, 9);
		textbuf.string = "Rate";
		textbuf = StaticText(wdados, Rect(575, 20, 50, 20));
		textbuf.font = Font(Font.defaultSansFace, 9);
		textbuf.string = "Win";
		textbuf = StaticText(wdados, Rect(600, 20, 50, 20));
		textbuf.font = Font(Font.defaultSansFace, 9);
		textbuf.string = "Rand";

		textbuf = StaticText(wdados, Rect(625, 20, 50, 20));
		textbuf.font = Font(Font.defaultSansFace, 9);
		textbuf.string = "A1";
		textbuf = StaticText(wdados, Rect(650, 20, 50, 20));
		textbuf.font = Font(Font.defaultSansFace, 9);
		textbuf.string = "A2";
		textbuf = StaticText(wdados, Rect(675, 20, 50, 20));
		textbuf.font = Font(Font.defaultSansFace, 9);
		textbuf.string = "A3";
		textbuf = StaticText(wdados, Rect(700, 20, 50, 20));
		textbuf.font = Font(Font.defaultSansFace, 9);
		textbuf.string = "A4";
		textbuf = StaticText(wdados, Rect(725, 20, 50, 20));
		textbuf.font = Font(Font.defaultSansFace, 9);
		textbuf.string = "A5";

		textbuf = StaticText(wdados, Rect(750, 20, 50, 20));
		textbuf.font = Font(Font.defaultSansFace, 9);
		textbuf.string = "a1";
		textbuf = StaticText(wdados, Rect(765, 20, 50, 20));
		textbuf.font = Font(Font.defaultSansFace, 9);
		textbuf.string = "a2";
		textbuf = StaticText(wdados, Rect(780, 20, 50, 20));
		textbuf.font = Font(Font.defaultSansFace, 9);
		textbuf.string = "a3";
		textbuf = StaticText(wdados, Rect(795, 20, 50, 20));
		textbuf.font = Font(Font.defaultSansFace, 9);
		textbuf.string = "a4";
		textbuf = StaticText(wdados, Rect(810, 20, 50, 20));
		textbuf.font = Font(Font.defaultSansFace, 9);
		textbuf.string = "a5";


		textbuf = StaticText(wdados, Rect(825, 20, 50, 20));
		textbuf.font = Font(Font.defaultSansFace, 9);
		textbuf.string = "File";

		textbuf = StaticText(wdados, Rect(925, 20, 50, 20));
		textbuf.font = Font(Font.defaultSansFace, 9);
		textbuf.string = "St";


		nfontes.do { | i |

			textbuf = StaticText(wdados, Rect(2, 40 + (i*20), 50, 20));
			textbuf.font = Font(Font.defaultSansFace, 9);
			textbuf.string = (i+1).asString;

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
			//tfield[i] = TextField(wdados, Rect(720, 40+ (i*20), 220, 20));

			stcheck[i] = CheckBox( wdados, Rect(925, 40 + (i*20), 40, 20));
			stcheck[i].action = { | but |
				stcheckProxy[i].valueAction = but.value;
			};

			textbuf = StaticText(wdados, Rect(940, 40 + (i*20), 50, 20));
			textbuf.font = Font(Font.defaultSansFace, 9);
			textbuf.string = (i+1).asString;

			libbox[i].font = Font(Font.defaultSansFace, 9);
			dstrvbox[i].font = Font(Font.defaultSansFace, 9);
			ncanbox[i].font = Font(Font.defaultSansFace, 9);
			businibox[i].font = Font(Font.defaultSansFace, 9);
			xbox[i].font = Font(Font.defaultSansFace, 9);
			ybox[i].font = Font(Font.defaultSansFace, 9);
			zbox[i].font = Font(Font.defaultSansFace, 9);
			//oxbox[i].font = Font(Font.defaultSansFace, 9);
			//oybox[i].font = Font(Font.defaultSansFace, 9);
			//ozbox[i].font = Font(Font.defaultSansFace, 9);


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
			ybox[i].value = 200;


			zbox[i].action = { | num |
				zboxProxy[i].valueAction = num.value;
			};




			abox[i].clipHi = pi;
			abox[i].clipLo = 0;
			vbox[i].clipHi = 2.0;
			vbox[i].clipLo = 0;
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

			vbox[i].scroll_step = 0.01;
			abox[i].scroll_step = 0.01;
			vbox[i].step = 0.01;
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


		runTriggers = {
			nfontes.do({ | i |
				if(audit[i].not) {
					if(triggerFunc[i].notNil) {
						triggerFunc[i].value;
						//updateSynthInArgs.value(i);
					}
				}
			})
		};

		runTrigger = { | source, dirrect = false |
			//	if(scncheck[i]) {
			if(triggerFunc[source].notNil) {
				triggerFunc[source].value;
				if (dirrect && synt[source].isNil
					&& (spheval[source].rho < 1)) {
					this.newtocar(source, 0, force: true);
				} {
					//updateSynthInArgs.value(source);
				};
				"RUNNING TRIGGER".postln;
			};
		};

		runStops = {
			nfontes.do({ | i |
				if(audit[i].not) {
					if(stopFunc[i].notNil) {
						stopFunc[i].value;
					}
				}
			})
		};

		runStop = { | source, dirrect = false |
			if(stopFunc[source].notNil) {
				stopFunc[source].value;
				if (dirrect) {
					firstTime[source] = false;
					synt[source].free;
					synt[source] = nil;
				};
			}
		};


		//control = Automation(dur).front(win, Rect(halfwidth, 10, 400, 25));
		/*~autotest = control = Automation(dur, showLoadSave: false,
		showSnapshot: false,
		minTimeStep: 0.001).front(win,
		Rect(10, width - 80, 400, 22));
		*/
		//~autotest = control = Automation(dur, showLoadSave: false,
		//showSnapshot: false,
		//	minTimeStep: 0.001);
		control.front(autoView, Rect(0, 0, 325, 20));


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
			if (looping) {
				nfontes.do { | i |
					firstTime[i]=true;
					//("HERE = " ++ firstTime[i]).postln;

				};
				looping = false;
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
			{win.refresh;}.defer;

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
				runStops.value;
				nfontes.do { | i |
					// if sound is currently being "tested", don't switch off on stop
					// leave that for user
					if (audit[i] == false) {
						synt[i].free; // error check
					};
					//	espacializador[i].free;
				};
				isPlay = false;
				looping = false;
				nfontes.do { | i |
					firstTime[i]=true;
					//("HERE = " ++ firstTime[i]).postln;
				};

			} {
				( "Did not stop. dur = " ++ dur ++ " now = " ++
					control.now).postln;
				looping = true;
				control.play;
			};

			ossiaplay.v_(false);
			{win.refresh;}.defer;
		};


		furthest = halfheight * 20;

		sprite = Array2D(nfontes, 2);
		nfontes.do { | i |
			sprite.put(i, 0, 0);
			sprite.put(i, 1, furthest);
		};

		win.view.mouseDownAction = { | view, mx, my, modifiers, buttonNumber, clickCount |
			mouseButton = buttonNumber; // 0 = left, 2 = middle, 1 = right
			case
			{mouseButton == 0} {
				var closest = [0, furthest]; // save sources index and distance from click
				// initialize at the furthest point
				nfontes.do { | i |
					var dis = ((mx - sprite[i, 0].value).squared
						+ (my - sprite[i, 1].value).squared).sqrt;
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
				if (sourceList.isNil) {
					sourceList = ListView(win, Rect(mx,my,
						90,70)).items_(itensdemenu).value_(currentsource)
					.action_({ |sel|
						sourceSelect.value(sel.value);
						moveSource.value(mx + 45, my + 35);
						sourceList.close;
						sourceList = nil;
					});
				} {
					sourceList.close;
					sourceList = nil;
				};
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

						synt[i].set(\azim, spheval[i].theta,
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
				zoom_factor = zoom_factor * 0.1;
				win.refresh;
			};
		};

		moveSource = { |x, y|

			var car2sphe = Cartesian((x - halfwidth) / halfheight,
				(halfheight - y) / halfheight,
				(zslider.value - 0.5) * 2 * zoom_factor);

			// save raw mouseposition for selecting closest source on click
			sprite.put(currentsource, 0, x);
			sprite.put(currentsource, 1, y);

			spheval[currentsource].rho_(car2sphe.rho);
			spheval[currentsource].theta_(car2sphe.theta);
			spheval[currentsource].phi_(car2sphe.phi);

			spheval[currentsource]  = (spheval[currentsource] / zoom_factor) * 100;

			ossiasphe[currentsource].v_([spheval[currentsource].rho,
				(spheval[currentsource].theta - halfPi).wrap(-pi, pi),
				spheval[currentsource].phi]);
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

			originView.bounds_(Rect(width - 275, height - 85, 270, 100));

			autoView.bounds_(Rect(10, height - 45, 325, 40));

			novoplot.value;

		});


		win.onClose_({

			wdados.close;
			waux.close;
			this.free;

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