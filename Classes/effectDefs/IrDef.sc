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
* User must set up a project directory with subdirectoties "ir" and "auto"
* irs should have the first 100 or 120ms silenced to act as "tail" reverberators
* and must be placed in the "ir" directory.
* Run help on the "Mosca" class in SuperCollider for detailed information
* and code examples. Further information and sample irs and B-format recordings
* may be downloaded here: http://escuta.org/mosca
*/

IrDef
{
	const <key = "Conv"; // class access
	var irSpectrum, bufsize;
	var irWspectrum, irZspectrum;
	// var irXspectrum, irYspectrum;

	key { ^key } // instance access

	*new { | server, ir | ^super.new.ctr(server, ir) }

	ctr
	{ | server, ir |

		var bufWXYZ, bufAformat;

		bufWXYZ = Buffer.read(server, ir.fullPath);

		server.sync;

		bufAformat = Buffer.alloc(server, bufWXYZ.numFrames, bufWXYZ.numChannels);

		this.prLoadLocalIr(server, ir);
		this.prLoadGlobalIr(server, ir, bufWXYZ, bufAformat);
	}

	prLoadLocalIr
	{ | server, ir |

		var irW, irX, irY, irZ;

		irW = Buffer.readChannel(server, ir.fullPath, channels: [0]);
		// irX = Buffer.readChannel(server, ir.fullPath, channels: [1]);
		// irY = Buffer.readChannel(server, ir.fullPath, channels: [2]);
		irZ = Buffer.readChannel(server, ir.fullPath, channels: [3]);

		server.sync;

		bufsize = PartConv.calcBufSize(MoscaUtils.fftSize(), irW);

		irWspectrum = Buffer.alloc(server, bufsize, 1);
		// irXspectrum = Buffer.alloc(server, bufsize, 1);
		// irYspectrum = Buffer.alloc(server, bufsize, 1);
		irZspectrum = Buffer.alloc(server, bufsize, 1);

		// don't need time domain data anymore, just needed spectral version
		irWspectrum.preparePartConv(irW, MoscaUtils.fftSize());
		irW.free;
		// irXspectrum.preparePartConv(irX, MoscaUtils.fftSize());
		// irX.free;
		// irYspectrum.preparePartConv(irY, MoscaUtils.fftSize());
		// irY.free;
		irZspectrum.preparePartConv(irZ, MoscaUtils.fftSize());
		irZ.free;
	}

	prLoadGlobalIr
	{ | server, ir, bufWXYZ, bufAformat |

		var irA4, afmtDir;

		if (File.exists(ir.pathOnly ++ "Flu").not) {
			(ir.pathOnly ++ "Flu").makeDir
		};

		afmtDir = ir.pathOnly ++ "Flu/"
		++ ir.fileNameWithoutExtension ++ "_Flu.wav";

		if (File.exists(afmtDir).not)
		{
			("writing " ++ ir.fileNameWithoutExtension
				++ "_Flu.wav file in" + ir.pathOnly ++ "Flu").postln;

			{
				BufWr.ar(FoaDecode.ar(
					PlayBuf.ar(4, bufWXYZ, loop: 0, doneAction: 2),
					MoscaUtils.b2a()),
				bufAformat, Phasor.ar(0,
					BufRateScale.kr(bufAformat),
					0, BufFrames.kr(bufAformat)));
				Out.ar(0, Silent.ar);
			}.play(server);

			(bufAformat.numFrames / server.sampleRate).wait;

			bufAformat.write(afmtDir, headerFormat: "wav", sampleFormat: "int24",
				completionMessage:{ ("done writing" + ir.fileNameWithoutExtension
					++ "_Flu.wav").postln; });

			server.sync;

			bufAformat.free;
			bufWXYZ.free;
		};

		irA4 = Array.newClear(4);
		irSpectrum = Array.newClear(4);

		4.do({ | i |
			irA4[i] = Buffer.readChannel(server, afmtDir, channels: [i]);
			irSpectrum[i] = Buffer.alloc(server, bufsize, 1);
			server.sync;
			irSpectrum[i].preparePartConv(irA4[i], MoscaUtils.fftSize());
			server.sync;
			irA4[i].free;
		});
	}

	getArgs
	{ | parentOssiaNode, nChan |

		if (nChan == 2)
		{
			^[
				\llev, parentOssiaNode.find("Local_amount").value,
				\zir, irZspectrum
			]
		} {
			^[
				\llev, parentOssiaNode.find("Local_amount").value,
				\wir, irWspectrum
			]
		}
	}

	getGlobalArgs
	{ | parentOssiaNode |

		^[\a0ir, irSpectrum[0],
			\a1ir, irSpectrum[1],
			\a2ir, irSpectrum[2],
			\a3ir, irSpectrum[3]];
	}

	// wxyzSpecPar
	// {
	// 	^[\wir, irWspectrum,
	// 		// \xir, irXspectrum,
	// 		// \yir, irYspectrum,
	// 	\zir, irZspectrum];
	// }
}

Ir12chanDef : IrDef
{
	prLoadGlobalIr
	{ | server, ir, bufWXYZ, bufAformat |

		var bufAformat_soa_a12, irA12, afmtDir;

		if (File.exists(ir.pathOnly ++ "SoaA12").not) {
			(ir.pathOnly ++ "SoaA12").makeDir
		};

		afmtDir = ir.pathOnly ++ "SoaA12/"
		++ ir.fileNameWithoutExtension ++ "_SoaA12.wav";

		if (File.exists(afmtDir).not)
		{
			bufAformat_soa_a12 = Buffer.alloc(server, bufWXYZ.numFrames, 12);

			("writing " ++ ir.fileNameWithoutExtension
				++ "_SoaA12.wav file in " + ir.pathOnly ++ "SoaA12").postln;

			{
				BufWr.ar(AtkMatrixMix.ar(
					PlayBuf.ar(4, bufWXYZ, loop: 0, doneAction: 2),
					MoscaUtils.foa_a12_decoder_matrix()),
				bufAformat_soa_a12,
				Phasor.ar(0, BufRateScale.kr(bufAformat),
					0, BufFrames.kr(bufAformat)));
				Out.ar(0, Silent.ar);
			}.play(server);

			(bufAformat.numFrames / server.sampleRate).wait;

			bufAformat_soa_a12.write(afmtDir, headerFormat: "wav", sampleFormat: "int24",
				completionMessage:{ ("done writing" + ir.fileNameWithoutExtension
					++ "_SoaA12.wav").postln; });

			// server.sync;

			bufAformat_soa_a12.free;
			bufAformat.free;
			bufWXYZ.free;
		};

		irA12 = Array.newClear(12);
		irSpectrum = Array.newClear(12);

		12.do({ | i |
			irA12[i] = Buffer.readChannel(server, afmtDir, channels: [i]);
			irSpectrum[i] = Buffer.alloc(server, bufsize, 1);
			server.sync;
			irSpectrum[i].preparePartConv(irA12[i], MoscaUtils.fftSize());
			server.sync;
			irA12[i].free;
		});
	}

	getGlobalArgs
	{ | parentOssiaNode |

		^[\a0ir, irSpectrum[0],
			\a1ir, irSpectrum[1],
			\a2ir, irSpectrum[2],
			\a3ir, irSpectrum[3],
			\a4ir, irSpectrum[4],
			\a5ir, irSpectrum[5],
			\a6ir, irSpectrum[6],
			\a7ir, irSpectrum[7],
			\a8ir, irSpectrum[8],
			\a9ir, irSpectrum[9],
			\a10ir, irSpectrum[10],
			\a11ir, irSpectrum[11]];
	}
}
