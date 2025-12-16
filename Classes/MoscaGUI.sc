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
*
* v3.3 - Added Name header back (now properly positioned as first column)
*/

MoscaGUI
{
	var sources, global, control, guiInt, palette; // initial arguments
	var width, halfWidth, height, halfHeight; // size
	var <win, wData, dataView, ctlView, localView, auxView, masterView, dialView, originView;
	var autoBut, loadBut, originBut, drawBut, saveBut, writeBut, fxBut;
	var trackOriginBut, trackOrientBut, trackCentre, trackOrient;
	var zoomFactor = 1, currentSource = 0, sourceNum, sourceName;
	var exInCheck, scInCheck, loopCheck, chanPopUp, busNumBox, bLoad, bStream, bAux;
	var bNodes, bMeters, bData, <recNumBox, bBlip, bRecAudio;
	var origin, orientation, scale;
	var zAxis, zSlider, zNumBox;
	var drawEvent, ctlEvent, loopEvent, sourceNameEvent, lastGui = 0;
	var mouseButton, furthest, sourceList;
	var graphicWidth, graphicHeight, drawOnImage;
	var drawing = false, lastRx = nil, lastRy = nil, rx, ry, rotatedGraphicCoords;
	var rotated, lastRotated, annotateText = nil, annotate = false;
	var printText;
	var maxUndo, undoAr, storeGraphicUndo, getGraphicUndo;
	
	classvar halfPi;

	*initClass
	{
		Class.initClassTree(MoscaUtils);
		halfPi = MoscaUtils.halfPi;
	}

	*new
	{ | aMosca, size, palette, guiInt |

		var p;

		switch (palette,
			\ossia,
			{ p = OSSIA.palette },
			\original,
			{ p = MoscaUtils.palette },
			{ p = QtGUI.palette }
		);

		^super.newCopyArgs(aMosca.sources, aMosca.effects, aMosca.control, guiInt, p).ctr(aMosca, size);
	}

	ctr
	{ | aMosca, size |

		// get ossia parameters
		origin = aMosca.ossiaParent.find("Origin");
		orientation = aMosca.ossiaParent.find("Orientation");
		scale = aMosca.ossiaParent.find("Scale_factor");
		aMosca.orient = orientation; // used in drawing
		maxUndo = aMosca.maxundo;
		undoAr = [];

		// set initial size values
		width = size;

		if (width < 600) { width = 600 };

		halfWidth = width * 0.5;
		height = width; // on init
		halfHeight = halfWidth;
		if(aMosca.graphicpath.notNil){
			var gwidth, gheight;
			aMosca.graphicImage = Image.open(aMosca.graphicpath);
			gwidth = aMosca.graphicImage.width;
			gheight = aMosca.graphicImage.height;
			aMosca.graphicOrigin = Point((gwidth / -2), (gheight / -2));
		};



		// main window
		win = Window("Mosca", (width)@(height)).front; // main indow
		win.view.palette_(palette);
		aMosca.window = win;

		if(aMosca.graphicpath.notNil){

			rotatedGraphicCoords = { | x, y |

				var halfWidth = win.view.bounds.width / 2;
				var halfHeight = win.view.bounds.height / 2;
				var gHalfWidth = aMosca.graphicImage.width / 2;
				var gHalfHeight = aMosca.graphicImage.height / 2;
				var zoomFactor = aMosca.zoomfactor;
				var graphicx, graphicy;
				var windowscale = halfHeight / (size / 2);
				var gOrigin = Point( ((origin.value[0] * halfHeight
					* scale.value / windowscale )
					+ (aMosca.graphicImage.width / 2)),
					((origin.value[1] * halfHeight * -1 * scale.value / windowscale )
						+ (aMosca.graphicImage.height / 2)) );
				var orient = aMosca.orient.value[0];
				var winx, winy;
				winx = (((x - halfWidth) / halfHeight) / zoomFactor);
				winy = (((halfHeight - y) / halfHeight) / zoomFactor);
				graphicx = (((winx * (size / 2)  ) ) + gHalfWidth
					+  (origin.value[0] * scale.value * size / 2)) ;
				graphicy =  (gHalfHeight - (winy * (size / 2) )
					- (origin.value[1] * scale.value * size / 2) ) ;
				orient = orient * -1;
				rx = ((graphicx - gOrigin.x) * orient.cos)
				- ((graphicy - gOrigin.y) * (orient.sin)) + (gOrigin.x );

				ry = ((graphicx - gOrigin.x) * orient.sin)
				+ ((graphicy - gOrigin.y) * (orient.cos)) + (gOrigin.y );
				Point(rx,ry)
			};

			printText = { | image, x, y, string, fontSize |
				var font = Font("Monaco", fontSize);
				image.draw{
					Pen.stringAtPoint(string, x@y, font, Color.black); 
				};
				win.refresh;
			};

			storeGraphicUndo = { | graphic |
				var gtmp = Image.fromImage(graphic);
				if (undoAr.size < maxUndo) {
					undoAr = undoAr.addFirst(gtmp);
				} {
					var tmpAr = Array(0);
					undoAr = undoAr.addFirst(gtmp);
					maxUndo.do({ arg item, i;
						tmpAr = tmpAr.add(undoAr[i]);
					});
					undoAr = tmpAr;
				};
			};

			getGraphicUndo = {
				if(undoAr[0].notNil == true) {
					undoAr.removeAt(0);
				};
			};

			
			
		};
		// source index
		StaticText(win, Rect(4, 3, 50, 20)).string_("Source");
		sourceNum = StaticText(win, Rect(50, 3, 30, 20)).string_("1");

		sourceName = TextField(win, Rect(85, 3, 150, 20))
		.string_("name")
		.stringColor_(Color.gray(0.5))
		.background_(palette.color('base', 'active'))
		.action_({ | field |
			var text;
			text = field.string;
			sources.get[currentSource].sourceName.valueAction_(text);
			if (text.isEmpty) {
				field.stringColor_(Color.gray(0.5));
				field.string_("name");
			} {
				field.stringColor_(palette.color('windowText', 'active'));
			};
		})
		.keyDownAction_({ | field, char, modifiers, unicode, keycode |
			if (unicode == 13) { // Enter/Return key
				win.refresh;
			};
		})
		.focusGainedAction_({ | field |
			if (field.string == "name") {
				field.string_("");
				field.stringColor_(palette.color('windowText', 'active'));
			};
		})
		.focusLostAction_({ | field |
			if (field.string.isEmpty) {
				field.string_("name");
				field.stringColor_(Color.gray(0.5));
			};
		});

		sourceNameEvent = { | param |
			var text = param.value;
			("GUI received name update: " ++ text).postln;
			{
				if (text.isEmpty || text.isNil) {
					sourceName.string_("name");
					sourceName.stringColor_(Color.gray(0.5));
				} {
					sourceName.string_(text);
					sourceName.stringColor_(palette.color('windowText', 'active'));
				};
				win.refresh; // Update map display
			}.defer;
		};

		sources.get[currentSource].sourceName.node.addDependant(sourceNameEvent);

		exInCheck = Button(win, Rect(4, 30, 79, 20), "EX-in")
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
		).action_({ | butt | sources.get[currentSource].external.valueAction_(butt.value.asBoolean) });

		scInCheck = Button( win, Rect(83, 30, 79, 20), "SC-in")
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
		).action_({ | butt | sources.get[currentSource].scSynths.valueAction_(butt.value.asBoolean) });

		bLoad = Button(win, Rect(4, 50, 79, 20), "Load")
		.focusColor_(palette.color('midlight', 'active'))
		.states_(
			[
				[
					"Load",
					palette.color('light', 'active'),
					palette.color('middark', 'active')
				]
			]
		).action_({

			sources.get[currentSource].stream.valueAction_(false);
			this.prFileDialog(sources.get[currentSource].file);
		});

		bLoad.visible_(false);

		bStream = Button(win, Rect(83, 50, 79, 20), "Stream")
		.focusColor_(palette.color('midlight', 'active'))
		.states_(
			[
				[
					"Stream",
					palette.color('light', 'active'),
					palette.color('middark', 'active')
				]
			]
		).action_({

			sources.get[currentSource].stream.valueAction_(true);
			this.prFileDialog(sources.get[currentSource].file);
		});

		bStream.visible_(false);

		chanPopUp = EZPopUpMenu(
			parentView: win,
			bounds: Rect(4, 50, 158, 20),
			label: "No. chans",
			items: MoscaUtils.channels(),
			globalAction: { | obj | sources.get[currentSource].nChan.valueAction_(obj.item) },
			gap: 0@0
		).setColors(
			stringColor: palette.color('baseText', 'active'),
			menuStringColor: palette.color('light', 'active')
		);

		chanPopUp.labelView.align_(\left);
		chanPopUp.visible_(false);

		busNumBox = EZNumber(
			parent: win,
			bounds: Rect(4, 70, 158, 20),
			label: "Bus index",
			numberWidth: 78,
			action: { | obj | sources.get[currentSource].busInd.valueAction_(obj.value) },
			gap: 0@0
		).setColors(
			stringColor: palette.color('baseText', 'active'),
			numNormalColor: palette.color('windowText', 'active')
		);

		busNumBox.labelView.align_(\left);
		busNumBox.numberView.maxDecimals_(0).step_(1).scroll_step_(1);
		busNumBox.controlSpec.maxval_(inf);
		busNumBox.visible_(false);

		loopCheck = Button(win, Rect(4, 70, 158, 20))
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
		).action_({ | butt | sources.get[currentSource].loop.valueAction_(butt.value) });

		loopEvent = { | param |
			{
				if (param.value != loopCheck.value)
				{ loopCheck.value_(param.value) };
			}.defer;
		};

		sources.get[currentSource].loop.node.addDependant(loopEvent);

		loopCheck.visible_(false);

		bAux = Button(win, Rect(4, 70, 158, 20))
		.focusColor_(palette.color('midlight', 'active'))
		.states_(
			[
				[
					"Aux",
					palette.color('light', 'active'),
					palette.color('middark', 'active')
				],
				[
					"Close Aux",
					palette.color('middark', 'active'),
					palette.color('light', 'active')
				]
			]
		).action_({ | butt |

			if (butt.value == 1)
			{
				auxView = sources.get[currentSource].auxiliary.gui(childrenDepth: 2);
				auxView.onClose_({ butt.value_(0) })
			} {
				sources.get[currentSource].auxiliary.closeGui(auxView);
			}
		});

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

			if (butt.value == 1)
			{
				var window = global.ossiaGlobal.node.gui(childrenDepth: 2);
				window.onClose_({ butt.value_(0) })
			} {
				global.ossiaGlobal.node.closeGui(childrenDepth: 1);
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
				sources.get[currentSource].coordinates.z
				.valueAction_((num.value + origin.value[2]) / scale.value);
			} {
				var sphe = sources.get[currentSource].coordinates.spheVal
				.asCartesian.z_(num.value).asSpherical;

				sources.get[currentSource].coordinates.azElDist.value_(
					[(sphe.theta - halfPi).wrap(-pi, pi).raddeg,
						sphe.phi.raddeg,
						sphe.rho / scale.value]);
			};
		});

		zSlider = Slider(win).focusColor_(palette.color('midlight', 'active'))
		.background_(palette.color('middark', 'active'))
		.action_({ | num | zNumBox.valueAction_((num.value - 0.5) * 2) });

		// sub view for grouping global transforamtion widgets (effects, rotations, mouvements)
		//originView = UserView(win, Rect(width - 94, height - 152, 94, 148));
		if(aMosca.graphicpath.notNil){
			originView = UserView(win, Rect(width - 194, height - 176, 94,
				172));
		}{
			originView = UserView(win, Rect(width - 94, height - 128, 94,
				124));
		};
		originView.addFlowLayout();

		// Track_Centre custom buttons - stacked compact layout
		trackCentre = aMosca.ossiaParent.find("Track_Centre");
		trackOrient = aMosca.ossiaParent.find("Track_Orientation");
		
		// Origin tracking button
		trackOriginBut = Button(originView, Rect(0, 0, 88, 20))
		.focusColor_(palette.color('midlight', 'active'))
		.states_(
			[
				[
					"Origin ✓",
					palette.color('middark', 'active'),
					palette.color('light', 'active')
				],
				[
					"Origin",
					palette.color('light', 'active'),
					palette.color('middark', 'active')
				]
			]
		).action_({ | butt |
			trackCentre.v_(butt.value.asBoolean.not); // inverted: button ON = tracking ON
		}).value_(trackCentre.v.asBoolean.not); // initialize from current Track_Centre value
		
		// Orientation tracking button - now functional!
		trackOrientBut = Button(originView, Rect(0, 0, 88, 20))
		.focusColor_(palette.color('midlight', 'active'))
		.states_(
			[
				[
					"Orient ✓",
					palette.color('middark', 'active'),
					palette.color('light', 'active')
				],
				[
					"Orient",
					palette.color('light', 'active'),
					palette.color('middark', 'active')
				]
			]
		).action_({ | butt |
			trackOrient.v_(butt.value.asBoolean.not); // inverted: button ON = tracking ON
		}).value_(trackOrient.v.asBoolean.not); // initialize from current Track_Orientation value
		
		originBut = Button(originView, Rect(0, 0, 88, 20))
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

			if (butt.value == 1)
			{
				var window = orientation.gui();
				origin.gui(window);
				scale.gui(window);
				window.onClose_({ butt.value_(0) });
			} {
				orientation.closeGui();
				origin.closeGui();
				scale.closeGui();
			}
		});
		if(aMosca.graphicpath.notNil){
			//drawBut = Button(originView, Rect(0, 0, 88, 20))
			drawBut = Button(originView, Rect(0, 0, 41, 20))
			.focusColor_(palette.color('midlight', 'active'))
			.states_(
				[
					[
						"Draw",
						palette.color('light', 'active'),
						palette.color('middark', 'active')
					],
					[
						"Stop ",
						palette.color('middark', 'active'),
						palette.color('light', 'active')
					]
				]
			).action_({ | butt |
				
				if (butt.value == 1)
				{
					storeGraphicUndo.value(aMosca.graphicImage);
					drawing = true;
					annotate = false;
					fork { while { drawing == true }
						{ defer { win.refresh;  }; 1.wait; } };
				} {
					drawing = false;
					annotate = false;
				}
			});
			
			
			//		annotate graphic
			writeBut = Button(originView, Rect(0, 0, 41, 20))
			.focusColor_(palette.color('midlight', 'active'))
			.states_(
				[
					[
						"Write",
						palette.color('light', 'active'),
						palette.color('middark', 'active')
					] 
				]
			).action_({ | butt |
				
				//if (butt.value == 1)
				//{
				var bounds, dwin, success = false, path, textField;
				//drawing = false;
				drawBut.valueAction = 0;
				annotate = true;
				annotateText = nil;
				bounds = Rect(200,300,430,20);
				//path = PathName(aMosca.graphicpath);
				dwin = Window("Annotate: enter text and click on graphic", bounds)
				.onClose_({ if (success.not) { "Decided not to write!".postln;
					annotateText = nil; annotate = false;
				drawing = false} });
				
				textField = TextField(dwin, Rect(0, 0, bounds.width, bounds.height))
				.value_("")
				.action_({ | tf |
					("Click on graphic with mouse to place text: ").postln;
					//aMosca.graphicImage.write(tf.value, quality: -1);
					success = true;
					annotate = true;
					drawing = false;
					annotateText = tf.value;
				dwin.close;
					
				});
				
				dwin.front;

					//drawing = true;
					//lastRx = lastRy = nil; // first mouse click to set
					//	}  
			});
			//};
				
			//			saveBut = Button(originView, Rect(0, 0, 88, 20))
			saveBut = Button(originView, Rect(0, 0, 88, 20))
			.focusColor_(palette.color('midlight', 'active'))
			.states_(
				[
					[
						"Save",
						palette.color('light', 'active'),
						palette.color('middark', 'active')
					] 
				]
			).action_({ | butt |
				
				//if (butt.value == 1)
				//{
				var bounds, dwin, textField, success = false, path;
				
				bounds = Rect(100,400,400,30);
				path = PathName(aMosca.graphicpath);
				dwin = Window("Save annotations: choose file name", bounds)
				.onClose_({ if (success.not) { "Aborted save!".postln } });
				
				textField = TextField(dwin, Rect(0, 0, bounds.width, bounds.height))
				.value_(path.pathOnly)
				.action_({ | tf |
					("Saving: " + tf.value).postln;
					aMosca.graphicImage.write(tf.value, quality: -1);
					success = true;
				dwin.close;
					
				});
				
				dwin.front;

					//drawing = true;
					//lastRx = lastRy = nil; // first mouse click to set
					//	}  
			});
			////////////////////



			drawBut = Button(originView, Rect(0, 0, 41, 20))
			.focusColor_(palette.color('midlight', 'active'))
			.states_(
				[
					[
						"M1",
						palette.color('light', 'active'),
						palette.color('middark', 'active')
					]
				]
			).action_({ | butt |
				if (butt.value == 0)
				{
					aMosca.mark1[0] = aMosca.center.ossiaOrigin.v[0]; // x1
					aMosca.mark1[1] = aMosca.center.ossiaOrigin.v[1]; // y1
					aMosca.mark1[2] = aMosca.gnssLat;
					aMosca.mark1[3] = aMosca.gnssLon;
					("mark1: " + aMosca.mark1[0] + "," + aMosca.mark1[1] + ","
					+ aMosca.mark1[2] + "," + aMosca.mark1[3]).postln;
				} 
			});
			
			
			//		annotate graphic
			writeBut = Button(originView, Rect(0, 0, 41, 20))
			.focusColor_(palette.color('midlight', 'active'))
			.states_(
				[
					[
						"M2",
						palette.color('light', 'active'),
						palette.color('middark', 'active')
					] 
				]
			).action_({ | butt |
				if (butt.value == 0)
				{
					aMosca.mark2[0] = aMosca.center.ossiaOrigin.v[0]; // x1
					aMosca.mark2[1] = aMosca.center.ossiaOrigin.v[1]; // y1
					aMosca.mark2[2] = aMosca.gnssLat;
					aMosca.mark2[3] = aMosca.gnssLon;
					("mark2: " + aMosca.mark2[0] + "," + aMosca.mark2[1] + ","
					+ aMosca.mark2[2] + "," + aMosca.mark2[3]).postln;
				} 
				 
			});
			
			/////////////////

		};
				

		
		loadBut = Button(originView, Rect(0, 0, 88, 20))
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
			.value_(control.get.presetDir)
			.action_({ | tf |
				success = true;
				dwin.close;
				aMosca.loadAutomation(tf.value);
			});

			dwin.front;
		});

		autoBut = Button(originView, Rect(0, 0, 88, 20))
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
		).action_({ | butt |

			if (butt.value == 1)
			{
				this.prAutoControl(aMosca)
			} {
				control.get.gui.win.close
			}
		});

		dialView = UserView(win, Rect(width - 94, 0, 94, 150)); // extra options view
		dialView.addFlowLayout();

		bData = Button(dialView, Rect(0, 0, 88, 20))
		.focusColor_(palette.color('midlight', 'active'))
		.states_(
			[
				[
					"Data",
					palette.color('light', 'active'),
					palette.color('middark', 'active')
				],
				[
					"Close Data",
					palette.color('middark', 'active'),
					palette.color('light', 'active')
				]
			]
		).action_({ | butt |

			if (butt.value == 1)
			{
				this.prDataGui();
			} {
				wData.close;
			}
		});

		bNodes = Button(dialView, Rect(0, 0, 88, 20))
		.focusColor_(palette.color('midlight', 'active'))
		.states_(
			[
				[
					"Show Nodes",
					palette.color('light', 'active'),
					palette.color('middark', 'active')
				]
			]
		).action_({ aMosca.server.plotTree });

		bMeters = Button(dialView, Rect(0, 0, 88, 20))
		.focusColor_(palette.color('midlight', 'active'))
		.states_(
			[
				[
					"Show Meters",
					palette.color('light', 'active'),
					palette.color('middark', 'active')
				]
			]
		).action_({ aMosca.server.meter });

		recNumBox = EZNumber(
			parent: dialView,
			numberWidth: 20,
			bounds: Rect(0, 0, 88, 20),
			label: "Rec chans",
			gap: 0@0
		).setColors(
			stringColor: palette.color('baseText', 'active'),
			numNormalColor: palette.color('windowText', 'active')
		);

		recNumBox.labelView.align_(\left);
		recNumBox.numberView.maxDecimals_(0).step_(1).scroll_step_(1);
		recNumBox.controlSpec.maxval_(inf);
		recNumBox.value_(2);

		bBlip = Button(dialView, Rect(0, 0, 88, 20))
		.focusColor_(palette.color('midlight', 'active'))
		.states_(
			[
				[
					"Bilps",
					palette.color('light', 'active'),
					palette.color('middark', 'active')
				],
				[
					"Bilps",
					palette.color('middark', 'active'),
					palette.color('light', 'active')
				]
			]
		);

		bRecAudio = Button(dialView, Rect(0, 0, 88, 20))
		.focusColor_(palette.color('midlight', 'active'))
		.states_(
			[
				[
					"Record Audio",
					palette.color('light', 'active'),
					palette.color('middark', 'active')
				],
				[
					"Stop Rec.",
					palette.color('middark', 'active'),
					palette.color('light', 'active')
				]
			]
		).action_({ | butt |

			if (butt.value ==  1)
			{
				aMosca.recordAudio(bBlip.value.asBoolean,
					recNumBox.value.asInteger);
			} {
				aMosca.stopRecording();
			}
		});

		// sub view containing the controls of the selected source
		ctlView = UserView(win, Rect(0, 90, 164, 20));
		ctlView.addFlowLayout();

		ctlEvent = { | obj ... loadArgs |

			if (loadArgs[0] == \ctl)
			{ { this.prUpdateCtl(sources.get[currentSource]) }.defer }
		};

		global.addDependant(ctlEvent);

		this.prSourceSelect(currentSource);

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
						sources.get.do({ | item, i |
							var dis = ((x - item.coordinates.spheVal.x).squared
								+ (y - item.coordinates.spheVal.y).squared).sqrt;

							// claculate distance from click
							if (dis < closest[1])
							{
								closest[1] = dis;
								closest[0] = i;
							};
						});

						if( (drawing == false) && (annotate == false) ) {
							if (closest[0] != currentSource)
							{ this.prSourceSelect(closest[0]) };
							this.prMoveSource(mx, my);
						} {
							if ( (annotate == true) &&
								annotateText.notNil)
							{
								var rotated = rotatedGraphicCoords.value(mx, my );
								storeGraphicUndo.value(aMosca.graphicImage);
								printText.(aMosca.graphicImage, rx, ry,
									annotateText, aMosca.fontsize);
								//annotateText = nil;
								drawing = false;
								annotate = false;
							};
				
							lastRx = nil;
							lastRy = nil;
						};
					},
					1,
					{
						sourceList = ListView(win, Rect(mx,my,90,70))
						.items_(this.prSetSrcList())
						.value_(-1) // to avoid the deffault to 1
						.action_({ | sel |

							if (sel.value != currentSource)
							{
								switch(sel.value,
									sources.get.size,
									{ aMosca.addSource() },
									sources.get.size + 1,
									{ aMosca.removeSource() },
									{
										this.prSourceSelect(sel.value);
										this.prMoveSource(mx + 45, my + 35);
										sourceList.close;
										sourceList = nil;
									}
								)
							}
						})
					},
					2,
					{
						sources.get.do { | item, i |
							("" ++ i ++ " " ++ item.coordinates.cartVal).postln;
							("" ++ i ++ " " ++ item.coordinates.spheVal).postln;
						}
					}
				)
			}
		);
		
		
		win.view.mouseMoveAction_({ | view, mx, my, modifiers |

			// left button
			if (mouseButton == 0) {
				if ((drawing == false) && (annotate == false) ) {
					this.prMoveSource(mx, my)
				} {
					
					rotated = rotatedGraphicCoords.value(mx, my );
					{
					if( (rotated.x != lastRx ||
						rotated.y != lastRy) && lastRx.notNil )
					{
						lastRotated = Point(lastRx, lastRy);
						
						aMosca.graphicImage.draw({ arg image;
							Pen.width = 3;
							Pen.strokeColor=Color.black;
							Pen.line(lastRotated, rotated);
							Pen.perform([\stroke, \fill].choose);
						}) ;
						//	win.refresh;
					
					};
				}.defer;
					lastRx = rotated.x;
					lastRy = rotated.y;
					
				};

			};
		});

		win.view.mouseWheelAction_({ | view, mx, my, modifiers, dx, dy |

			if ((dy < 0) && (zoomFactor <= 10))
			{
				aMosca.zoomfactor = zoomFactor = zoomFactor * 1.01;
				win.refresh;
			};

			if ((dy > 0) && (zoomFactor >= 0.55))
			{
				aMosca.zoomfactor = zoomFactor = zoomFactor * 0.99;
				win.refresh;
			};
		});

		win.view.onResize_({ | view |

			width = view.bounds.width;
			halfWidth = width * 0.5;
			height = view.bounds.height;
			halfHeight = height * 0.5;

			// set initial furthest source as 10 times the apparent radius
			furthest = halfHeight * 10;
			if(aMosca.graphicpath.notNil){
				var ztop, zheight = (halfHeight * 0.8) - 40; // reduced height to leave more space
				ztop = halfHeight - (zheight * 0.5) - 20; // raise by 20 pixels
				zSlider.bounds_(Rect(width - 35, ztop,
				20, zheight));
				zNumBox.bounds_(Rect(width - 45, ztop + zheight + 4, 40, 20));
			}{
				zSlider.bounds_(Rect(width - 35, (halfHeight * 0.5),
				20, halfHeight - 40)); // reduced height
				zNumBox.bounds_(Rect(width - 45, (halfHeight * 0.5)
					+ halfHeight - 36, 40, 20)); // raised position
			};


			zAxis.bounds_(Rect(width - 80, halfHeight - 30, 90, 20)); // raised by 20 pixels

			if(aMosca.graphicpath.notNil){
				//originView.bounds_(Rect(width - 94, height - 176, 94, 172));
				originView.bounds_(Rect(width - 94, height - 200, 94, 192));
			}{
				originView.bounds_(Rect(width - 94, height - 128, 94, 148));
			};
			dialView.bounds_(Rect(width - 94, 3, 94, 150));

			masterView.bounds_(masterView.bounds.top_(height - 76));

			drawEvent.value;
		});

		if(aMosca.graphicpath.notNil){
			win.view.keyDownAction_({
				| view, char, modifiers, unicode |
				if (unicode == 26){                //ctl-z
					var width, height;
					var graphic = getGraphicUndo.value;
					if(graphic.notNil == true) {
						"Undo!".postln;
						width = aMosca.graphicImage.width;
						height = aMosca.graphicImage.height;
						aMosca.graphicImage = graphic;
						aMosca.graphicOrigin = Point((graphic.width / -2),
							(graphic.height / -2));
						win.refresh;
						//aMosca.scaleGraphic(aMosca.graphicScale);
					};
				};				
			});
		};

		win.drawFunc_({ // draw circles for center, max distance & sources

			var plim = MoscaUtils.plim();
			Pen.fillColor = palette.color('middark', 'active');
			Pen.addArc(halfWidth@halfHeight, halfHeight * zoomFactor, 0, 2pi);
			Pen.fill;

			if(aMosca.graphicpath.notNil){
				var windowscale = halfHeight / (size / 2);
				var hsgh, hsgw;
				hsgw = aMosca.graphicImage.width * windowscale * zoomFactor / 2;
				hsgh = aMosca.graphicImage.height * windowscale * zoomFactor / 2;
				graphicWidth = aMosca.graphicImage.width; 
				graphicHeight = aMosca.graphicImage.height; 
				Pen.use {
					Pen.rotate(aMosca.center.ossiaOrient.v[0],
						halfWidth, halfHeight); // leave as width, height
					Pen.scale(zoomFactor * windowscale, zoomFactor * windowscale);
					Pen.translate( (origin.value[0] * halfHeight
						* -1 * scale.value / windowscale),
						(origin.value[1] * halfHeight * scale.value / windowscale ) );
					Pen.drawImage( Point( (halfWidth / zoomFactor / windowscale)
						- (hsgw / zoomFactor / windowscale),
						(halfHeight / zoomFactor / windowscale)
						- (hsgh / zoomFactor / windowscale)  ),
						aMosca.graphicImage, operation: 'sourceIn', opacity:0.99);


				};
			};

			Pen.strokeColor = palette.color('midlight', 'active');
			Pen.addArc(halfWidth@halfHeight, halfHeight * zoomFactor * 0.25, 0, 2pi);
			Pen.stroke;

			sources.get.do({ | item, i |
				var x, y, numColor, displayText;
				var topView = item.coordinates.spheVal;
				var lev = topView.z;

				// z set NumBox and Slider
				if (i == currentSource) {
					zNumBox.value_(lev);
					zSlider.value_((lev * 0.5) + 0.5);
				};

				topView = topView * zoomFactor;

				x = halfWidth + (topView.x * halfHeight);
				y = halfHeight - (topView.y * halfHeight);

				Pen.addArc(x@y, max((14 * zoomFactor) + (lev * halfHeight * 0.02), 0), 0, 2pi);

				if (item.play.value && ((lev * zoomFactor) <= plim))
				{
					numColor = palette.color('window', 'active');

					Pen.fillColor = palette.color('baseText', 'active')	// light
					.alpha_(0.2 + (item.contraction.value * 0.8));

					Pen.fill;
				} {
					numColor = palette.color('alternateBase', 'active'); // windowText

					Pen.strokeColor = numColor;
					Pen.stroke;
				};

				(i + 1).asString.drawCenteredIn(Rect(x - 11, y - 10, 23, 20),
					Font.default, numColor);
				
				// Draw name to the right of circle if exists
				displayText = item.sourceName.value;
				if (displayText.notEmpty && (displayText != "name")) {
					displayText.drawLeftJustIn(Rect(x + 15, y - 10, 100, 20),
						Font.default.size_(11), numColor);
				};
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

		sources.get.do({ | item | this.addSource(item) });
	}

	addSource
	{ | aSource |

		aSource.play.addDependant(drawEvent);
		aSource.coordinates.azElDist.addDependant(drawEvent);
		aSource.reach.node.addDependant(drawEvent);
		aSource.contraction.node.addDependant(drawEvent);
		this.prAddData(aSource);

		if (sourceList.notNil)
		{
			sourceList.items_(this.prSetSrcList());
			sourceList.value_(aSource.index + 3);
		}
	}

	removeSource
	{ | aSourceIndex |

		if (aSourceIndex == currentSource)
		{
			// reset source selection completly
			currentSource = 0;
			this.prSourceSelect(currentSource);

			// redraw as a point should desapear
			win.refresh;
		};

		if (sourceList.notNil)
		{
			sourceList.items_(this.prSetSrcList());
			sourceList.value_(sourceList.items.size - 1);
		};
	}

	free
	{
		global.removeDependant(ctlEvent);

		sources.get[currentSource].removeDependant(ctlEvent);
		sources.get[currentSource].loop.node.removeDependant(loopEvent);
	}

	prSetSrcList
	{
		^sources.get.collect(
			{ | s | "Source " ++ (s.index + 1).asString }
		) ++ '+ Source' ++ ' - Source' ++ '';
	}

	prFileDialog
	{ | aFileNode |

		Dialog.openPanel({ | path |

			aFileNode.valueAction_(path);
		});
	}

	prSourceSelect
	{ | index |

		var source, topview, name;

		source = sources.get[index];

		sources.get[currentSource].removeDependant(ctlEvent);
		source.addDependant(ctlEvent);

		sources.get[currentSource].loop.node.removeDependant(loopEvent);
		source.loop.node.addDependant(loopEvent);

		sources.get[currentSource].sourceName.node.removeDependant(sourceNameEvent);
		source.sourceName.node.addDependant(sourceNameEvent);

		if (bAux.value == 1) { bAux.valueAction_(0) };

		currentSource = index;

		sourceNum.string_(currentSource + 1).asString;

		name = source.sourceName.value;
		{
			if (name.isEmpty || name.isNil) {
				sourceName.string_("name");
				sourceName.stringColor_(Color.gray(0.5));
			} {
				sourceName.string_(name);
				sourceName.stringColor_(palette.color('windowText', 'active'));
			};
		}.defer;

		topview = source.coordinates.spheVal * zoomFactor;

		zNumBox.value_(topview.z);
		zSlider.value_((zNumBox.value * 0.5) + 0.5);

		this.prUpdateCtl(source);
	}

	prUpdateCtl
	{ | src |

		ctlView.removeAll;
		ctlView.decorator.reset;

		// sub view containing local effect parameter
		localView = UserView(ctlView, Rect(0, 0, 164, 40));
		localView.addFlowLayout(0@0);

		src.localEffect.node.gui(localView);

		src.library.node.gui(ctlView);
		src.play.gui(ctlView);
		src.level.node.gui(ctlView);
		src.reach.node.gui(ctlView);
		src.contraction.node.gui(ctlView);
		src.doppler.node.gui(ctlView);

		switch (src.chanNum,
			1,
			{
				src.angle.node.closeGui(ctlView);
				src.rotation.node.closeGui(ctlView);
			},
			2,
			{
				src.angle.node.gui(ctlView);
				src.rotation.node.closeGui(ctlView);
			},
			{
				src.angle.node.closeGui(ctlView);
				src.rotation.node.gui(ctlView);
			}
		);

		src.getLibParams().do({ | item |
			item.gui(ctlView) });

		if (src.localEffect.value == "Clear")
		{
			src.localAmount.node.closeGui(localView);
			src.localDelay.node.closeGui(localView);
			src.localDecay.node.closeGui(localView);
		} {
			if ((src.localEffect.value != "AllPass") &&
				(src.localEffect.value != "FreeVerb")) {
					src.localAmount.node.gui(localView);
				} {
					src.localAmount.node.gui(localView);
					src.localDelay.node.gui(localView);
					src.localDecay.node.gui(localView);
				};
		};
		if (global.ossiaGlobal.value == "Clear")
		{
			src.globalAmount.node.closeGui(ctlView);
		} {
			src.globalAmount.node.gui(ctlView);
		};

		ctlView.asView.decorator.reFlow(ctlView.asView);
		// use the resize function of any OSSIA_Parameter
		src.globalAmount.node.resizeLayout(ctlView);

		if (src.external.value || src.scSynths.value)
		{
			var i = MoscaUtils.channels.detectIndex({ | item | item == src.nChan.value });

			chanPopUp.visible_(true);
			chanPopUp.value_(i);

			if (src.scSynths.value)
			{
				exInCheck.value_(false);
				busNumBox.visible_(false);
			} {
				scInCheck.value_(false);
				busNumBox.visible_(true);
				busNumBox.value_(src.busInd.value);
			};

			bLoad.visible_(false);
			bStream.visible_(false);
			loopCheck.visible_(false);
		} {
			busNumBox.visible_(false);
			chanPopUp.visible_(false);

			bLoad.visible_(true);
			bStream.visible_(true);
			loopCheck.visible_(true);
		};

		bAux.bounds_(bAux.bounds.top_(ctlView.bounds.top + ctlView.bounds.height));
	}

	prMoveSource
	{ |x, y|

		var src, point = Cartesian(
			(((x - halfWidth) / halfHeight) / zoomFactor),
			(((halfHeight - y) / halfHeight) / zoomFactor),
			zNumBox.value) / scale.value;
		src = sources.get[currentSource].coordinates;
		if (orientation.value == [0, 0, 0])
		{
			src.x.valueAction_(point.x + origin.value[0]);
			// exeption to record XY mouvements after Z automation
			src.y.valueAction_(point.y + origin.value[1]);
		} {
			var sphe = point.asSpherical;

			src.azElDist.value_(
				[(sphe.theta - halfPi).wrap(-pi, pi).raddeg,
					sphe.phi.raddeg,
					sphe.rho]);
		};
	}

	prAutoControl
	{ | instance |

		var ossiaLoop, loopEvent, loop, ossiaSync, syncEvent, sync;
		var canv = Window.new("Automation Control", Rect(0, 0, width, 43)).front;

		// snapshot when opening the window for the first time
		if (control.get.gui.isNil && control.get.now == 0) { control.get.snapshot };

		control.get.front(canv, Rect(0, 0, width, 20));
		canv.onClose_({ autoBut.value_(0) });

		Button(canv, Rect(0, 23, 150, 20))
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
			.value_(control.get.presetDir)
			.action_({ | tf |
				success = true;
				dwin.close;
				instance.saveAutomation(tf.value);
			});

			dwin.front;
		});

		CheckBox(canv, Rect(156, 23, 200, 20), "Slave to MMC")
		.focusColor_(palette.color('midlight', 'active'))
		.action_({ | check | instance.slaveToMMC(check.value) })
		.value_(instance.slaved);

		ossiaLoop = instance.ossiaParent.find("/Automation/Loop");

		loopEvent = { | param |
			{
				if (param.value != loop.value)
				{ loop.value_(param.value) };
			}.defer;
		};

		ossiaLoop.addDependant(loopEvent);

		loop = CheckBox(canv, Rect(266, 23, 200, 20), "Loop")
		.focusColor_(palette.color('midlight', 'active'))
		.action_({ | check | ossiaLoop.v_(check.value) })
		.onClose_({ ossiaLoop.removeDependant(loopEvent) })
		.value_(ossiaLoop.v);

		ossiaSync = instance.ossiaParent.find("/Automation/Sync_files");

		syncEvent = { | param |
			{
				if (param.value != sync.value)
				{ sync.value_(param.value) };
			}.defer;
		};

		ossiaSync.addDependant(syncEvent);

		sync = CheckBox(canv, Rect(326, 23, 200, 20), "Sync_files")
		.focusColor_(palette.color('midlight', 'active'))
		.action_({ | check | ossiaSync.v_(check.value) })
		.onClose_({ ossiaSync.removeDependant(syncEvent) })
		.value_(ossiaSync.v);
	}

	prDataGui
	{
		var bounds, strings = [ "Name", "File", "St", "Lp", "Ex", "Sc", "No. Chans", "Bus Index",
			"Local Fx", "Loc. amt.", "Delay", "Decay", "Library"," X", " Y", " Z",
			"Azimuth", "Elevation", "Distance", "Play", "Level", "Reach", "Contract.",
			"Doppler", "Gl. amt.", "St. angle", "B-F. rot.", "Direct.", "Gr. Rate",
			"Win. size", "Rnd. size", "Aux 1", "C1", "Aux 2", "C2", "Aux 3", "C3",
			"Aux 4", "C4", "Aux 5", "C5" ]; // txt indicators

		//wData = Window("Data", Rect(width, 0, 1962, (sources.get.size * 20) + 60),
			wData = Window("Data", Rect(width + 20, 0, Window.screenBounds.width - 10, (sources.get.size * 20) + 80),
				scroll: true).front;
		//		("Width: " + Window.screenBounds.width).postln;

		wData.onClose_({
			wData = nil;
			bData.value_(0);
		}).view.palette_(palette);

		dataView = UserView(wData, Rect(0, 24, 2070, (sources.get.size * 20) + 80));
		dataView.addFlowLayout;

		sources.get.do( this.prAddData(_) );

		bounds = dataView.children.collect(_.bounds);

		strings.do({ | item, i |

			var bound = bounds[i + 1];

			StaticText(wData, Rect(bound.left, 4, bound.width, 20))
			.font_(Font(Font.defaultSansFace, 9))
			.string_(item);
		});
	}

	prAddData
	{ | source |

		if (wData.notNil) {

			StaticText(dataView, 20@20)
			.font_(Font(Font.defaultSansFace, 9))
			.string_((source.index + 1).asString);

			source.src.gui(dataView, 2, \minimal);
		};
	}

	prRemoveData
	{ | source |

		if (wData.notNil) {

			source.src.closeGui(dataView, 2);
			dataView.children.last.remove;
		};
	}
}
