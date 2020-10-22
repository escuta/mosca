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
	var sources, control, guiInt; // initial arguments
	var width, halfWidth, height, halfheight; // size
	var <win, zoomFactor = 1, currentSource = 0;
	var drawEvent, lastGui = 0;

	*initClass {}

	*new
	{ | size, sources, control, palette, isPlay, guiInt |

		var p;

		switch (palette,
			\ossia,
			{ p = OSSIA.palette },
			\original,
			{ p = MoscaUtils.palette }
		);

		^super.newCopyArgs(sources, control, guiInt).ctr(p, size, isPlay);
	}

	ctr
	{ | palette, size, isPlay |

		width = size;

		if (width < 600) {
			width = 600;
		};

		halfWidth = width * 0.5;
		height = width; // on init
		halfheight = halfWidth;

		win = Window("Mosca", Rect(0, 0, width, height)).front;
		win.background = palette.color('window', 'active');

		win.drawFunc_({

			"redrawing".postln;

			Pen.fillColor = palette.color('middark', 'active');
			Pen.addArc(halfWidth@halfheight, halfheight * zoomFactor, 0, 2pi);
			Pen.fill;

			Pen.strokeColor = palette.color('midlight', 'active');
			Pen.addArc(halfWidth@halfheight, halfheight * zoomFactor * 0.25, 0, 2pi);
			Pen.stroke;

			sources.do({ | item, i |
				var x, y, numColor;
				var topView = item.coordinates.spheVal * zoomFactor;
				var lev = topView.z;

				x = halfWidth + (topView.x * halfheight);
				y = halfheight - (topView.y * halfheight);

				Pen.addArc(x@y, max(14 + (lev * halfheight * 0.02), 0), 0, 2pi);

				if ((item.play.value || isPlay.get()) && (lev.abs <= MoscaUtils.plim())) {

					numColor = palette.color('window', 'active');

					Pen.fillColor = palette.color('light', 'active')
					.alpha_(55 + (item.contraction.value * 200));

					Pen.fill;
				} {
					numColor = palette.color('baseText', 'active');

					Pen.strokeColor = palette.color('light', 'active');
					Pen.stroke;
				};

				(i + 1).asString.drawCenteredIn(Rect(x - 11, y - 10, 23, 20),
					Font.default, numColor);
			});

			Pen.fillColor = palette.color('middark', 'active');
			Pen.addArc(halfWidth@halfheight, 14, 0, 2pi);
			Pen.fill;
		});

		// triggers redrawing
		drawEvent = { this.novoplot() };

		sources.do({ | item |

			item.play.param.addDependant(drawEvent);
			item.coordinates.azElDist.addDependant(drawEvent);
			item.contraction.param.addDependant(drawEvent);
		});
	}

	novoplot
	{
		var period = Main.elapsedTime - lastGui;

		if (period > guiInt)
		{
			lastGui = Main.elapsedTime;
			{ win.refresh }.defer;
		};
	}

	free
	{
		sources.do { | item |

			item.play.param.removeDependant(drawEvent);
			item.coordinates.azElDist.removeDependant(drawEvent);
			item.contraction.param.removeDependant(drawEvent);
		};
	}
}