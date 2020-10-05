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

//-------------------------------------------//
//                 AMBITOOLS                 //
//-------------------------------------------//

ABTDef : SpatDef {
	classvar <key, format;

	*initClass {

		defList = defList.add(this.asClass);

		key = "Ambitools";

		format = \N3D;
	}

	prSetFunc { | maxOrder, renderer, server |

		spatFunc = { |ref, input, distance, azimuth, elevation, difu, spre,
			contract, win, rate, rand|
			var sig = HOAEncoder.ar(maxOrder,
				(ref.value + input), CircleRamp.kr(azimuth, 0.1, -pi, pi),
				Lag.kr(elevation), 0, 1, distance.linlin(0, 0.75, renderer.quarterRadius,
					renderer.twoAndaHalfRadius), renderer.longestRadius);
			ref.value = (sig * contract) +
			Silent.ar(renderer.bFormNumChan - 1).addFirst(Mix(sig) * (1 - contract));
		};
	}

	key { ^key; }

	format { ^format; }
}

//-------------------------------------------//
//                   HOALIB                  //
//-------------------------------------------//

HOALibDef : SpatDef {
	classvar <key, format;

	*initClass {

		if (\HOALibEnc3D1.asClass.notNil) { defList = defList.add(this.asClass); };

		key = "HoaLib";

		format = \N3D;

	}

	prSetFunc { | maxOrder, renderer, server |

		spatFunc = { |ref, input, distance, azimuth, elevation, difu, spre,
			contract, win, rate, rand|
			var sig = distFilter.value(input, distance);
			// attenuate high freq with distance
			sig = HOALibEnc3D.ar(maxOrder,
				ref.value + (sig * (renderer.longestRadius / distance.linlin(0, 0.75, renderer.quarterRadius, renderer.twoAndaHalfRadius))),
				CircleRamp.kr(azimuth, 0.1, -pi, pi), Lag.kr(elevation));
			ref.value = (sig * contract) + Silent.ar(renderer.bFormNumChan - 1).addFirst(Mix(sig) * (1 - contract));
		};
	}

	key { ^key; }

	format { ^format; }
}

//-------------------------------------------//
//                    ADT                    //
//-------------------------------------------//

ADTDef : SpatDef {
	classvar <key, format;

	*initClass {

		if (\HOAmbiPanner1.asClass.notNil) { defList = defList.add(this.asClass); };

		key = "ADT";

		format = \N3D;
	}

	prSetFunc { | maxOrder, renderer, server |

		spatFunc = { |ref, input, distance, azimuth, elevation, difu, spre,
			contract, win, rate, rand|
			var sig = distFilter.value(input, distance);
			// attenuate high freq with distance
			sig = HOAmbiPanner.ar(maxOrder,
				ref.value + (sig * (renderer.longestRadius / distance.linlin(0, 0.75, renderer.quarterRadius, renderer.twoAndaHalfRadius))),
				CircleRamp.kr(azimuth, 0.1, -pi, pi), Lag.kr(elevation));
			ref.value = (sig * contract) + Silent.ar(renderer.bFormNumChan - 1).addFirst(Mix(sig) * (1 - contract));
		};
	}

	key { ^key; }

	format { ^format; }
}

//-------------------------------------------//
//                   SC-HOA                  //
//-------------------------------------------//

SCHOADef : SpatDef {
	classvar <key, format;

	*initClass {

		defList = defList.add(this.asClass);

		key = "SC-HOA";

		format = \N3D;
	}

	prSetFunc { | maxOrder, renderer, server |

		spatFunc = { |ref, input, distance, azimuth, elevation, difu, spre,
			contract, win, rate, rand|
			var sig = distFilter.value(input, distance);
			// attenuate high freq with distance
			sig = HOASphericalHarmonics.coefN3D(maxOrder,
				CircleRamp.kr(azimuth, 0.1, -pi, pi), Lag.kr(elevation))
			* (ref.value + (sig * (renderer.longestRadius / distance.linlin(0, 0.75, renderer.quarterRadius, renderer.twoAndaHalfRadius))));
			ref.value = (sig * contract) + Silent.ar(renderer.bFormNumChan - 1).addFirst(Mix(sig) * (1 - contract));
		};
	}

	key { ^key; }

	format { ^format; }
}

//-------------------------------------------//
//                     ATK                   //
//-------------------------------------------//

ATKDef : SpatDef {
	classvar <key, format;

	*initClass {

		defList = defList.add(this.asClass);

		key = "ATK";

		format = \FUMA;
	}

	prSetFunc { | maxOrder, renderer, server |
		var foaEncoderSpread, foaEncoderDiffuse;

		foaEncoderSpread = FoaEncoderKernel.newSpread (subjectID: 6, kernelSize: 2048,
			server:server, sampleRate:server.sampleRate.asInteger);
		foaEncoderDiffuse = FoaEncoderKernel.newDiffuse (subjectID: 3, kernelSize: 2048,
			server:server, sampleRate:server.sampleRate.asInteger);

		server.sync;

		spatFunc = { |ref, input, distance, azimuth, elevation, difu, spre,
			contract, win, rate, rand|
			var diffuse, spread, omni,
			sig = distFilter.value(input, distance),
			rad = renderer.longestRadius / distance.linlin(0, 0.75,
				renderer.quarterRadius, renderer.twoAndaHalfRadius);
			sig = ref.value + (sig * rad);
			omni = FoaEncode.ar(sig, FoaEncoderMatrix.newOmni);
			spread = FoaEncode.ar(sig, foaEncoderSpread);
			diffuse = FoaEncode.ar(sig, foaEncoderDiffuse);
			sig = Select.ar(difu, [omni, diffuse]);
			sig = Select.ar(spre, [sig, spread]);
			sig = FoaTransform.ar(sig, 'push', MoscaUtils.halfPi * contract, azimuth, elevation);
			sig = HPF.ar(sig, 20); // stops bass frequency blow outs by proximity
			ref.value = FoaTransform.ar(sig, 'proximity', distance);
		};
	}

	key { ^key; }

	format { ^format; }
}

//-------------------------------------------//
//                   BFFMH                   //
//-------------------------------------------//

BFFMHDef : SpatDef {
	classvar <key, format;

	*initClass {

		defList = defList.add(this.asClass);

		key = "BF-FMH";

		format = \FUMA;
	}

	prSetFunc { | maxOrder, renderer, server |
		var enc = MoscaUtils.bfOrFmh(maxOrder);

		spatFunc = { |ref, input, distance, azimuth, elevation, difu, spre,
			contract, win, rate, rand|
			var sig = distFilter.value(input, distance);
			sig = enc.ar(ref.value + sig, azimuth, elevation,
				(renderer.longestRadius / distance.linlin(0, 0.75,
					renderer.quarterRadius, renderer.twoAndaHalfRadius)), 0.5);
			ref.value = (sig * contract) +
			Silent.ar(renderer.bFormNumChan - 1).addFirst(Mix(sig) * (1 - contract));
		};
	}

	key { ^key; }

	format { ^format; }
}

//-------------------------------------------//
//                   JOSH                    //
//-------------------------------------------//

JOSHDef : SpatDef {
	classvar <key, format;

	*initClass {

		defList = defList.add(this.asClass);

		key = "Josh";

		format = \FUMA;
	}

	prSetFunc { | maxOrder, renderer, server |

		spatFunc = { |ref, input, distance, azimuth, elevation, difu, spre,
			contract, win, rate, rand|
			var sig = distFilter.value(input, distance);
			ref.value = MonoGrainBF.ar(ref.value + sig, win, rate, rand,
				azimuth, 1 - contract, elevation, 1 - contract,
				rho: (renderer.longestRadius / distance.linlin(0, 0.75,
					renderer.quarterRadius, renderer.twoAndaHalfRadius)) - 1,
				mul: ((0.5 - win) + (1 - (rate / 40))).clip(0, 1) * 0.5 );
		};
	}

	key { ^key; }

	format { ^format; }
}

//-------------------------------------------//
//                    VBAP                   //
//-------------------------------------------//

VBAPDef : SpatDef {
	classvar <key, format;

	*initClass {

		defList = defList.add(this.asClass);

		key = "VBAP";

		format = \NONAMBI;
	}

	prSetFunc { | maxOrder, renderer, server |

		spatFunc = { |ref, input, distance, azimuth, elevation, difu, spre,
			contract, win, rate, rand|
			var sig = distFilter.value(input, distance),
			azi = azimuth * MoscaUtils.rad2deg, // convert to degrees
			elev = elevation * MoscaUtils.rad2deg, // convert to degrees
			elevexcess = Select.kr(elev < renderer.lowestElevation, [0, elev.abs]);
			elevexcess = Select.kr(elev > renderer.highestElevation, [0, elev]);
			// get elevation overshoot
			elev = elev.clip(renderer.lowestElevation, renderer.highestElevation);
			// restrict between min & max
			ref.value = VBAP.ar(renderer.numOutputs,
				ref.value + (sig * (renderer.longestRadius / distance.linlin(0, 0.75,
					renderer.quarterRadius, renderer.twoAndaHalfRadius))),
				renderer.vbapBuffer.bufnum, CircleRamp.kr(azi, 0.1, -180, 180), Lag.kr(elevation),
				((1 - contract) + (elevexcess / 90)) * 100);
		};
	}

	key { ^key; }

	format { ^format; }
}

//-------------------------------------------//
//               Base Classe                 //
//-------------------------------------------//

SpatDef {
	classvar <defList, distFilter;
	var <spatFunc;

	*initClass {

		Class.initClassTree(MoscaUtils);

		defList = [];

		distFilter = { | input, distance | // attenuate high freq with distance
			LPF.ar(input, (1 - distance) * 18000 + 2000);
		};
	}

	*new { | maxOrder, renderer, server | ^super.new.prSetFunc(maxOrder, renderer, server); }
}