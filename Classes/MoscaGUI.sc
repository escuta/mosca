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

MoscaGUI {
	var sources, control, width, halfwidth, height, halfheight; // initial arguments
	var win, zoom_factor = 1;

	*initClass {

	}

	*new { | size = 800, sources, control, palette = \ossia, isPlay |
		var p;

		switch (palette,
			{ \ossia },
			{ p = OSSIA.palette; },
			{ \original },
			{ p = MoscaUtils.palette; }
		);

		^super.newCopyArgs(sources, control, size).ctr(p, isPlay);
	}

	ctr { | palette, size, isPlay |

		width = size;

		if (width < 600) {
			width = 600;
		};

		halfwidth = width * 0.5;
		height = width; // on init
		halfheight = halfwidth;

		win = Window("Mosca", Rect(0, 0, width, height)).front;
		win.background = palette.window;

		win.drawFunc_({

			Pen.fillColor = palette.middark; // OSSIA/score "Transparent1"
			Pen.addArc(halfwidth@halfheight, halfheight * zoom_factor, 0, 2pi);
			Pen.fill;

			Pen.strokeColor = palette.midlight;
			Pen.addArc(halfwidth@halfheight, halfheight * zoom_factor * 0.25, 0, 2pi);
			Pen.stroke;

			sources.do { | item, i |
				var x, y, numColor;
				var topView = item.coordinates.spheval * zoom_factor;
				var lev = topView.z;

				x = halfwidth + (topView.x * halfheight);
				y = halfheight - (topView.y * halfheight);

				Pen.addArc(x@y, max(14 + (lev * halfheight * 0.02), 0), 0, 2pi);

				if ((item.play || isPlay) && (lev.abs <= MoscaUtils.plim())) {

					numColor = palette.light;

					Pen.fillColor = palette.light.alpha_(55 + (item.contraction.value * 200));
					Pen.fill;
				} {
					numColor = palette.light;

					Pen.strokeColor = palette.light;
					Pen.stroke;
				};

				(i + 1).asString.drawCenteredIn(Rect(x - 11, y - 10, 23, 20),
					Font.default, numColor);
			};

			Pen.fillColor = Color.new255(37, 41, 48, 40);
			Pen.addArc(halfwidth@halfheight, 14, 0, 2pi);
			Pen.fill;
		});
	}
}












