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
	var <win, wdados, waux, dialView, masterView, originView;
	var autoBut, <>ctlWidth = 370, loadBut, origineBut, fxBut;
	var zoomFactor = 1, currentSource = 0, sourceNum;
	var isPlay, origine, orientation, scale;
	var zAxis, zSlider, zNumBox, zEvent;
	var drawEvent, lastGui = 0;
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
		StaticText(win, Rect(6, 6, 50, 20)).string_("Source");
		sourceNum = StaticText(win, Rect(50, 6, 30, 20)).string_("1");

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

		// sub view for automation control, master volume, scale factor
		masterView = UserView(win, Rect(0, height - 72, 325, 72));
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
							if(dis < closest[1].value) {
								closest[1] = dis;
								closest[0] = i;
							};
						});

						this.sourceSelect(closest[0].value);
						this.moveSource(mx, my);
					},
					1,
					{
						sourceList = ListView(win,
							Rect(mx,my,90,70))
						.items_(sources.collect(
								{ | s | "Source " ++ (s.index + 1).asString }
						))
						.value_(-1) // to avoid the deffault to 1
						.action_({ | sel |
							this.sourceSelect(sel.value);
							this.moveSource(mx + 45, my + 35);
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

			if (mouseButton == 0) { this.moveSource(mx, my) };
			// left button
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

			// hdtrkcheck.bounds_(Rect(width - 105, height - 105, 265, 20));

			dialView.bounds_(Rect(width - 100, 10, 180, 150));

			masterView.bounds_(Rect(0, height - 72, 325, 72));

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

	sourceSelect
	{ | item |

		var src, topview;

		currentSource = item.value;

		src = sources[currentSource];

		sourceNum.string_(item.value + 1).asString;

		topview = src.coordinates.spheVal * zoomFactor;

		zNumBox.value_(topview.z);
		zSlider.value_((zNumBox.value * 0.5) + 0.5);

/*		updateGuiCtl.value(\chan);

		loopcheck.value = lpcheckProxy[currentSource].value;

		updateGuiCtl.value(\lib, libboxProxy[currentSource].value);

		updateGuiCtl.value(\dstrv, dstrvboxProxy[currentSource].value);

		updateGuiCtl.value(\src);

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

	moveSource
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

		var ossiaLoop, loopEvent, loopCheck;
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
				if (param.value != loopCheck.value)
				{ loopCheck.value_(param.value) };
			}.defer;
		};

		ossiaLoop.addDependant(loopEvent);

		loopCheck = CheckBox(canv, Rect(316, 23, 200, 20), "Loop")
		.focusColor_(palette.color('midlight', 'active'))
		.action_({ | check | ossiaLoop.v_(check.value) })
		.onClose_({ ossiaLoop.removeDependant(loopEvent) });
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