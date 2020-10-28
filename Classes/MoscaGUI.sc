/*
* Mosca: SuperCollider class by Iain Mott, 2016. Licensed under a
* Creative Commons Attribution-NonCommercial 4.0 International License
* http://creativecommons.org/licenses/by-nc/4.0/
* The class makes extensive use of the Ambisonic Toolkit (http://www.ambisonictoolkit.net/)
* by Joseph Anderson and the Automation quark
* (https://github.com/neeels/Automation) by Neels Hofmeyr.
* Required Quarks : Automation, Ctk, XML and  MathLib
* Required classes:
* SC Plugins: https://github.com/supercollider/sc3-plugins
* User must set up a project directory with subdirectoties "rir" and "auto"
* RIRs should have the first 100 or 120ms silenced to act as "tail" reverberators
* and must be placed in the "rir" directory.
* Run help on the "Mosca" class in SuperCollider for detailed information
* and code examples. Further information and sample RIRs and B-format recordings
* may be downloaded here: http://escuta.org/mosca
*/

MoscaGUI
{
	var sources, control, guiInt, palette; // initial arguments
	var width, halfWidth, height, halfHeight; // size
	var <win, wdados, waux;
	var localView, ctlView, masterView, dialView, originView;
	var autoBut, <>ctlWidth = 370, loadBut, origineBut, fxBut;
	var zoomFactor = 1, currentSource = 0, sourceNum;
	var exInCheck, scInCheck, loopCheck;
	var isPlay, origine, orientation, scale;
	var zAxis, zSlider, zNumBox, zEvent;
	var drawEvent, dependant, lastGui = 0;
	var mouseButton, furthest, sourceList;

	classvar halfPi;

	*initClass
	{
		Class.initClassTree(MoscaUtils);
		halfPi = MoscaUtils.halfPi;
	}

	*new
	{ | aMosca, sources, size, palette, guiInt |

		var p;

		switch (palette,
			\ossia,
			{ p = OSSIA.palette },
			\original,
			{ p = MoscaUtils.palette },
			{ p = QtGUI.palette }
		);

		^super.newCopyArgs(sources, aMosca.control, guiInt, p).ctr(aMosca, size);
	}

	ctr
	{ | aMosca, size |

		// get ossia parameters
		isPlay = aMosca.ossiaParent.find("Automation/Play");

		origine = aMosca.ossiaParent.find("Origine");
		orientation = aMosca.ossiaParent.find("Orientation");
		scale = aMosca.ossiaParent.find("Scale_factor");

		// set initial size values
		width = size;

		if (width < 600) { width = 600 };

		halfWidth = width * 0.5;
		height = width; // on init
		halfHeight = halfWidth;

		// main window
		win = Window("Mosca", Rect(0, 0, width, height)).front; // main indow
		win.view.palette = palette;

		// source index
		StaticText(win, Rect(3, 3, 50, 20)).string_("Source");
		sourceNum = StaticText(win, Rect(50, 3, 30, 20)).string_("1");

		exInCheck = Button(win, Rect(3, 30, 74, 20), "EX-in")
		.focusColor_(palette.color('midlight', 'active'))
		.states_(
			[
				[
					"EX-in",
					palette.color('light', 'active'),
					palette.color('middark', 'active')
				],
				[
					"EX-in",
					palette.color('middark', 'active'),
					palette.color('light', 'active')
				]
			]
		).action_({ | butt | sources[currentSource].external.value_(butt.value) });

		scInCheck = Button( win, Rect(78, 30, 74, 20), "SC-in")
		.focusColor_(palette.color('midlight', 'active'))
		.states_(
			[
				[
					"SC-in",
					palette.color('light', 'active'),
					palette.color('middark', 'active')
				],
				[
					"SC-in",
					palette.color('middark', 'active'),
					palette.color('light', 'active')
				]
			]
		).action_({ | butt | sources[currentSource].scSynths.value_(butt.value) });

		loopCheck = Button( win, Rect(3, 70, 150, 20), "Loop")
		.focusColor_(palette.color('midlight', 'active'))
		.states_(
			[
				[
					"Loop",
					palette.color('light', 'active'),
					palette.color('middark', 'active')
				],
				[
					"Loop",
					palette.color('middark', 'active'),
					palette.color('light', 'active')
				]
			]
		).action_({ | butt | sources[currentSource].loop.value_(butt.value) });

		// sub view containing local effect parameter
		localView = UserView(win, Rect(0, height - 262, 156, 20));
		localView.addFlowLayout();

		// sub view for automation control, master volume, scale factor
		masterView = UserView(win, Rect(0, height - 76, 325, 72));
		masterView.addFlowLayout();

		fxBut = Button(masterView, Rect(0, height - 95, 320, 20))
		.focusColor_(palette.color('midlight', 'active'))
		.states_(
			[
				[
					"Global Effect",
					palette.color('light', 'active'),
					palette.color('middark', 'active')
				],
				[
					"Close Effect",
					palette.color('middark', 'active'),
					palette.color('light', 'active')
				]
			]
		).action_({ | butt |

			var widget = aMosca.ossiaParent.find("Global_effect");

			if (widget.window.notNil)
			{
				if (widget.window.isClosed)
				{
					widget.gui(childrenDepth: 2);
					widget.window.onClose_({ butt.value_(0)})
				} {
					widget.window.close
				}
			} {
				widget.gui(childrenDepth: 2);
				widget.window.onClose_({ butt.value_(0) })
			}
		});

		aMosca.ossiaParent.find("Master_level").gui(masterView);

		// z Axis widgets
		zAxis = StaticText(win);
		zAxis.string = "Z-Axis";
		zNumBox = NumberBox(win)
		.background_(palette.color('base', 'active'))
		.normalColor_(palette.color('windowText', 'active'))
		.decimals_(2)
		.step_(0.01)
		.scroll_step_(0.01)
		.align_(\center)
		.action_({ | num |

			zSlider.value_((num.value * 0.5) + 0.5);

			if (orientation.value == [0, 0, 0])
			{ // exeption to record z mouvements after XY automation
				sources[currentSource].coordinates.z
				.valueAction_(num.value + origine.value[2]);
			} {
				var sphe = sources[currentSource].coordinates.spheVal
				.asCartesian.z_(num.value).asSpherical;

				sources[currentSource].coordinates.azElDist.value_(
					[(sphe.theta - halfPi).wrap(-pi, pi).raddeg,
						sphe.phi.raddeg,
						sphe.rho]);
			};
		});

		zSlider = Slider(win).focusColor_(palette.color('midlight', 'active'))
		.background_(palette.color('middark', 'active'))
		.action_({ | num | zNumBox.valueAction_((num.value - 0.5) * 2) });

		// sub view for grouping global transforamtion widgets (effects, rotations, mouvements)
		originView = UserView(win, Rect(width - 88, height - 128, 88, 124));
		originView.addFlowLayout();

		aMosca.ossiaParent.find("Track_Center").gui(originView);

		origineBut = Button(originView, Rect(0, 0, 82, 20))
		.focusColor_(palette.color('midlight', 'active'))
		.states_(
			[
				[
					"Position",
					palette.color('light', 'active'),
					palette.color('middark', 'active')
				],
				[
					"Close Position",
					palette.color('middark', 'active'),
					palette.color('light', 'active')
				]
			]
		).action_({ | butt |

			var widget = aMosca.ossiaParent.find("Orientation");

			if (widget.window.notNil)
			{
				if (widget.window.isClosed)
				{
					widget.gui();
					aMosca.ossiaParent.find("Origine")
					.gui(widget.window);
					scale.gui(widget.window);
					widget.window.onClose_({ butt.value_(0) })
				} {
					widget.window.close
				}
			} {
				widget.gui();
				aMosca.ossiaParent.find("Origine")
				.gui(widget.window);
				scale.gui(widget.window);
				widget.window.onClose_({ butt.value_(0) })
			}
		});

		loadBut = Button(originView, Rect(0, 0, 82, 20))
		.focusColor_(palette.color('midlight', 'active'))
		.states_(
			[
				[
					"Load Auto",
					palette.color('light', 'active'),
					palette.color('middark', 'active')
				]
			]
		).action_({

			var bounds, dwin, textField, success = false;

			bounds = Rect(100,300,300,30);

			dwin = Window("Load: select automation directory", bounds)
			.onClose_({ if (success.not) { "Aborted load!".postln } });

			textField = TextField(dwin, Rect(0, 0, bounds.width, bounds.height))
			.value_(control.presetDir)
			.action_({ | tf |
				success = true;
				dwin.close;
				aMosca.loadData(tf.value);
			});

			dwin.front;
		});

		autoBut = Button(originView, Rect(0, 0, 82, 20))
		.focusColor_(palette.color('midlight', 'active'))
		.states_(
			[
				[
					"Control Auto",
					palette.color('light', 'active'),
					palette.color('middark', 'active')
				],
				[
					"Close Control",
					palette.color('middark', 'active'),
					palette.color('light', 'active')
				]
			]
		).action_({

			if (control.gui.notNil)
			{
				if (control.gui.win.isClosed)
				{
					this.automationControl(aMosca)
				} {
					control.gui.win.close
				}
			} {
				this.automationControl(aMosca)
			}
		});

		dialView = UserView(win); // extra options view

		// sub view containing the controls of the selected source
		ctlView = UserView(win, Rect(0, 90, 156, 20));
		ctlView.addFlowLayout();

		dependant = { | obj ... loadArgs |

			if (loadArgs[0] == \ctl)
			{ this.prUpdateCtl(obj) }
		};

		this.prSourceSelect(currentSource); // initialize with the first source

		// main mouse interaction for selecting and mooving sources
		win.view.mouseDownAction_(
			{ | view, mx, my, modifiers, buttonNumber, clickCount |

				mouseButton = buttonNumber; // 0 = left, 2 = middle, 1 = right

			if (sourceList.notNil)
				{
					sourceList.close;
					sourceList = nil;
				};

				switch (mouseButton,
					0,
					{
						var x = ((mx - halfWidth) / halfHeight) / zoomFactor;
						var y = ((halfHeight - my) / halfHeight) / zoomFactor;
						var closest = [0, furthest];
						// save sources index and distance from click
						// initialize at the furthest point

						sources.do({ | item, i |
							var dis = ((x - item.coordinates.spheVal.x).squared
								+ (y - item.coordinates.spheVal.y).squared).sqrt;

							// claculate distance from click
							if (dis < closest[1])
							{
								closest[1] = dis;
								closest[0] = i;
							};
						});

						if (closest[0] != currentSource)
						{ this.prSourceSelect(closest[0]) };

						this.prMoveSource(mx, my);
					},
					1,
					{
						sourceList = ListView(win, Rect(mx,my,90,70))
						.items_(sources.collect(
							{ | s | "Source " ++ (s.index + 1).asString }
						))
						.value_(-1) // to avoid the deffault to 1
						.action_({ | sel |

							if (sel.value != currentSource)
							{ this.prSourceSelect(sel.value) };

							this.prMoveSource(mx + 45, my + 35);

							sourceList.close;
							sourceList = nil;
						});
					},
					2,
					{
						sources.do { | item, i |
							("" ++ i ++ " " ++ item.cartVal.asArgsArray).postln;
							("" ++ i ++ " " ++ item.spheVal.asArray).postln;
						};
					}
				)
			}
		);

		win.view.mouseMoveAction_({ | view, mx, my, modifiers |

			// left button
			if (mouseButton == 0) { this.prMoveSource(mx, my) };
		});

		win.view.mouseWheelAction_({ | view, mx, my, modifiers, dx, dy |

			if ((dy < 0) && (zoomFactor <= 10))
			{
				zoomFactor = zoomFactor * 1.01;
				win.refresh;
			};

			if ((dy > 0) && (zoomFactor >= 0.55))
			{
				zoomFactor = zoomFactor * 0.99;
				win.refresh;
			};
		});

		win.view.onResize_({ | view |

			width = view.bounds.width;
			halfWidth = width * 0.5;
			height = view.bounds.height;
			halfHeight = height * 0.5;

			// set initial furthest source as 20 times the apparent radius
			furthest = halfHeight * 20;

			zSlider.bounds_(Rect(width - 35, (halfHeight * 0.5),
				20, halfHeight));

			zNumBox.bounds_(Rect(width - 45, (halfHeight * 0.5) + halfHeight,
				40, 20));

			zAxis.bounds_(Rect(width - 80, halfHeight - 10, 90, 20));

			originView.bounds_(Rect(width - 88, height - 128, 88, 124));

			// dialView.bounds_(Rect(width - 100, 10, 180, 150));

			localView.bounds_(localView.bounds.top_(height - 262));

			masterView.bounds_(masterView.bounds.top_(height - 76));

			drawEvent.value;
		});

		win.drawFunc_({ // draw circles for center, max distance & sources

			var plim = MoscaUtils.plim();

			Pen.fillColor = palette.color('middark', 'active');
			Pen.addArc(halfWidth@halfHeight, halfHeight * zoomFactor, 0, 2pi);
			Pen.fill;

			Pen.strokeColor = palette.color('midlight', 'active');
			Pen.addArc(halfWidth@halfHeight, halfHeight * zoomFactor * 0.25, 0, 2pi);
			Pen.stroke;

			sources.do({ | item, i |
				var x, y, numColor;
				var topView = item.coordinates.spheVal;
				var lev = topView.z;

				// z set NumBox and Slider
				if (i == currentSource) { zNumBox.valueAction_(lev) };

				topView = topView * zoomFactor;

				x = halfWidth + (topView.x * halfHeight);
				y = halfHeight - (topView.y * halfHeight);

				Pen.addArc(x@y, max((14 * zoomFactor) + (lev * halfHeight * 0.02), 0), 0, 2pi);

				if ((item.play.value || isPlay.value) && ((lev * zoomFactor) <= plim))
				{
					numColor = palette.color('window', 'active');

					Pen.fillColor = palette.color('light', 'active')
					.alpha_(55 + (item.contraction.value * 200));

					Pen.fill;
				} {
					numColor = palette.color('windowText', 'active');

					Pen.strokeColor = numColor;
					Pen.stroke;
				};

				(i + 1).asString.drawCenteredIn(Rect(x - 11, y - 10, 23, 20),
					Font.default, numColor);
			});

			Pen.fillColor = palette.color('midlight', 'active');
			Pen.addArc(halfWidth@halfHeight, 7, 0, 2pi);
			Pen.fill;
		});

		// triggers redrawing
		drawEvent = {
			var period = Main.elapsedTime - lastGui;

			if (period > guiInt)
			{
				lastGui = Main.elapsedTime;
				{ win.refresh }.defer;
			};
		};

		isPlay.addDependant(drawEvent);

		sources.do({ | item |

			item.play.node.addDependant(drawEvent);
			item.coordinates.azElDist.addDependant(drawEvent);
			item.contraction.node.addDependant(drawEvent);
		});
	}

	prSourceSelect
	{ | index |

		var source, topview;

		source = sources[index];

		sources[currentSource].removeDependant(dependant);
		source.addDependant(dependant);

		currentSource = index;

		sourceNum.string_(currentSource + 1).asString;

		topview = source.coordinates.spheVal * zoomFactor;

		zNumBox.value_(topview.z);
		zSlider.value_((zNumBox.value * 0.5) + 0.5);

		ctlView.close;

		// initialize with the first source
		source.library.node.gui(ctlView);
		source.play.node.gui(ctlView);
		source.level.node.gui(ctlView);
		source.contraction.node.gui(ctlView);
		source.doppler.node.gui(ctlView);
		source.globalAmount.node.gui(ctlView);

		//source.localEffect.node.gui(localView, 0);

/*		prUpdateCtl.value(\chan);

		loopcheck.value = lpcheckProxy[currentSource].value;

		prUpdateCtl.value(\lib, libboxProxy[currentSource].value);

		prUpdateCtl.value(\dstrv, dstrvboxProxy[currentSource].value);

		prUpdateCtl.value(\src);

		winCtl[0][0].value = vboxProxy[currentSource].value;
		winCtl[1][0].value = vboxProxy[currentSource].value.curvelin(inMin:-96, inMax:12, curve:-3);
		winCtl[0][1].value = dpboxProxy[currentSource].value;
		winCtl[1][1].value = dpboxProxy[currentSource].value;
		winCtl[1][2].value = cboxProxy[currentSource].value;
		winCtl[1][2].value = cboxProxy[currentSource].value;
		winCtl[0][3].value = ossiaclsam[currentSource].v;
		winCtl[1][3].value = ossiaclsam[currentSource].v;

		auxslider1.value = a1boxProxy[currentSource].value;
		aux1numbox.value = a1boxProxy[currentSource].value;
		auxslider2.value = a2boxProxy[currentSource].value;
		aux2numbox.value = a2boxProxy[currentSource].value;
		auxslider3.value = a3boxProxy[currentSource].value;
		aux3numbox.value = a3boxProxy[currentSource].value;
		auxslider4.value = a4boxProxy[currentSource].value;
		aux4numbox.value = a4boxProxy[currentSource].value;
		auxslider5.value = a5boxProxy[currentSource].value;
		aux5numbox.value = a5boxProxy[currentSource].value;

		if(audit[currentSource]) {
			// don't change button if we are playing via automation
			// only if it is being played/streamed manually
			//	if (synt[currentSource] == nil){
			//		baudi.value = 0;
			//	} {
			baudi.value = 1;
			//	};
		} {
			baudi.value = 0;
		};*/
	}

	prMoveSource
	{ |x, y|

		var point = Cartesian(
			(((x - halfWidth) / halfHeight) / zoomFactor),
			(((halfHeight - y) / halfHeight) / zoomFactor),
			zNumBox.value) / scale.value;

		if (orientation.value == [0, 0, 0])
		{
			sources[currentSource].coordinates.x.valueAction_(point.x + origine.value[0]);
			// exeption to record XY mouvements after Z automation
			sources[currentSource].coordinates.y.valueAction_(point.y + origine.value[1]);
		} {
			var sphe = point.asSpherical;

			sphe.theta.postln;

			sources[currentSource].coordinates.azElDist.value_(
				[(sphe.theta - halfPi).wrap(-pi, pi).raddeg,
					sphe.phi.raddeg,
					sphe.rho]);
		};
	}

	automationControl
	{ | instance |

		var ossiaLoop, loopEvent, loop;
		var canv = Window.new("Automation Control", Rect(0, 0, ctlWidth, 43)).front;

		// canv.background_(palette.color('base', 'active'));

		control.front(canv, Rect(0, 0, ctlWidth, 20));
		canv.onClose_({ autoBut.value_(0) });

		Button(canv, Rect(0, 23, 200, 20))
		.focusColor_(palette.color('midlight', 'active'))
		.states_(
			[
				[
					"Save Automation",
					palette.color('light', 'active'),
					palette.color('middark', 'active')
				]
			]
		).action_({

			var bounds, dwin, textField, success = false;

			bounds = Rect(100,300,300,30);

			dwin = Window("Save: select automation dir", bounds)
			.onClose_({ if (success.not) { "Aborted save".postln } });

			textField = TextField(dwin, Rect(0, 0, bounds.width, bounds.height))
			.value_(control.presetDir)
			.action_({ | tf |
				success = true;
				dwin.close;
				instance.saveData(tf.value);
			});

			dwin.front;
		});

		CheckBox(canv, Rect(206, 23, 200, 20), "Slave to MMC")
		.focusColor_(palette.color('midlight', 'active'))
		.action_({ | check | instance.slaveToMMC(check.value) });

		ossiaLoop = instance.ossiaParent.find("/Automation/Loop");

		loopEvent = { | param |
			{
				if (param.value != loop.value)
				{ loop.value_(param.value) };
			}.defer;
		};

		ossiaLoop.addDependant(loopEvent);

		loop = CheckBox(canv, Rect(316, 23, 200, 20), "Loop")
		.focusColor_(palette.color('midlight', 'active'))
		.action_({ | check | ossiaLoop.v_(check.value) })
		.onClose_({ ossiaLoop.removeDependant(loopEvent) });
	}

	prUpdateCtl
	{ | src |

		switch (src.chanNum,
			1,
			{
				src.angle.node.closeGui();
				src.rotation.node.closeGui();
			},
			2,
			{
				src.angle.node.gui(ctlView);
				src.rotation.node.closeGui();
			},
			{
				src.angle.node.closeGui();
				src.rotation.node.gui(ctlView);

				if (src.library.value == "ATK")
				{ src.directivity.node.gui(ctlView) }
			}
		);

		switch (src.library.value,
			"ATK",
			{
				src.spread.node.gui(ctlView);
				src.diffuse.node.gui(ctlView);

				src.josh.node.closeGui(1);
			},
			"Josh",
			{
				src.atk.node.closeGui(1);
				src.josh.node.gui(ctlView, 1);
			},
			{
				src.atk.node.closeGui(1);
				src.josh.node.closeGui(1);
			}
		);

		if (src.localEffect.value == "Clear")
		{
			src.localDelay.closeGui();
			src.localDecay.closeGui();
		} {
			src.localDelay.gui(localView);
			src.localDecay.gui(localView);
		};

/*			\dstrv,
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
		);*/
	}

	free
	{
		isPlay.removeDependant(drawEvent);

		sources.do { | item |

			item.play.node.removeDependant(drawEvent);
			item.coordinates.azElDist.removeDependant(drawEvent);
			item.contraction.node.removeDependant(drawEvent);
		};
	}
}