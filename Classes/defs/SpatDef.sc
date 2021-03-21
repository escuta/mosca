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
	classvar <key, <format;
	const maxRadius = 50; // maximum radius accepeted by ambitools

	*initClass {

		defList = defList.add(this.asClass);

		key = "Ambitools";

		format = \N3D;
	}

	prSetFunc { | maxOrder, renderer, server |

		var speakerRadius = renderer.longestRadius,
		lim, converge;

		lim = maxRadius / (maxRadius + speakerRadius); // radiusRoot value at the maxRadius

		converge = { | radRoot | atenuator.value(radRoot.linlin(lim, 1, 0.5, 1)) };

		spatFunc = { |ref, input, radius, radiusRoot, azimuth, elevation, difu, spre,
			contract, win, rate, rand|
			var distance = aten2distance.value(radiusRoot.min(lim)),
			sig = HOAEncoder.ar(maxOrder,
				(ref.value * distance) // local reverb make up gain
				+ (input * converge.value(radiusRoot)),
				CircleRamp.kr(azimuth, 0.1, -pi, pi),
				Lag.kr(elevation),
				0, // gain
				1, // spherical
				distance * speakerRadius,
				speakerRadius);
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
	classvar <key, <format;

	*initClass {

		if (\HOALibEnc3D1.asClass.notNil) { defList = defList.add(this.asClass); };

		key = "HoaLib";

		format = \N3D;

	}

	prSetFunc { | maxOrder, renderer, server |

		spatFunc = { |ref, input, radius, radiusRoot, azimuth, elevation, difu, spre,
			contract, win, rate, rand|
			var sig = distFilter.value(input, radius);
			// attenuate high freq with distance
			sig = HOALibEnc3D.ar(maxOrder,
				ref.value + (sig * atenuator.value(radiusRoot)),
				CircleRamp.kr(azimuth, 0.1, -pi, pi),
				Lag.kr(elevation));
			ref.value = (sig * contract) +
			Silent.ar(renderer.bFormNumChan - 1).addFirst(Mix(sig) * (1 - contract));
		};
	}

	key { ^key; }

	format { ^format; }
}

//-------------------------------------------//
//                    ADT                    //
//-------------------------------------------//

ADTDef : SpatDef {
	classvar <key, <format;

	*initClass {

		if (\HOAmbiPanner1.asClass.notNil) { defList = defList.add(this.asClass); };

		key = "ADT";

		format = \N3D;
	}

	prSetFunc { | maxOrder, renderer, server |

		spatFunc = { |ref, input, radius, radiusRoot, azimuth, elevation, difu, spre,
			contract, win, rate, rand|
			var sig = distFilter.value(input, radius);
			// attenuate high freq with distance
			sig = HOAmbiPanner.ar(maxOrder,
				ref.value + (sig * atenuator.value(radiusRoot)),
				CircleRamp.kr(azimuth, 0.1, -pi, pi), Lag.kr(elevation));
			ref.value = (sig * contract) +
				Silent.ar(renderer.bFormNumChan - 1).addFirst(Mix(sig) * (1 - contract));
		};
	}

	key { ^key; }

	format { ^format; }
}

//-------------------------------------------//
//                   SC-HOA                  //
//-------------------------------------------//

SCHOADef : SpatDef {
	classvar <key, <format;

	*initClass {

		defList = defList.add(this.asClass);

		key = "SC-HOA";

		format = \N3D;
	}

	prSetFunc { | maxOrder, renderer, server |

		spatFunc = { |ref, input, radius, radiusRoot, azimuth, elevation, difu, spre,
			contract, win, rate, rand|
			var sig = distFilter.value(input, radius);
			// attenuate high freq with radius
			sig = HOASphericalHarmonics.coefN3D(maxOrder,
				CircleRamp.kr(azimuth, 0.1, -pi, pi), Lag.kr(elevation))
				* (ref.value + (sig * atenuator.value(radiusRoot)));
			ref.value = (sig * contract) +
			Silent.ar(renderer.bFormNumChan - 1).addFirst(Mix(sig) * (1 - contract));
		};
	}

	key { ^key; }

	format { ^format; }
}

//-------------------------------------------//
//                     ATK                   //
//-------------------------------------------//

ATKDef : SpatDef {
	classvar <key, <format;

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

		spatFunc = { |ref, input, radius, radiusRoot, azimuth, elevation, difu, spre,
			contract, win, rate, rand|
			var diffuse, spread, omni,
			sig = distFilter.value(input, radius);
			sig = ref.value + (sig * atenuator.value(radiusRoot));
			omni = FoaEncode.ar(sig, FoaEncoderMatrix.newOmni);
			spread = FoaEncode.ar(sig, foaEncoderSpread);
			diffuse = FoaEncode.ar(sig, foaEncoderDiffuse);
			sig = Select.ar(difu, [omni, diffuse]);
			sig = Select.ar(spre, [sig, spread]);
			sig = FoaTransform.ar(sig, 'push', MoscaUtils.halfPi * contract,
				CircleRamp.kr(azimuth, 0.1, -pi, pi), elevation);
			sig = HPF.ar(sig, 20); // stops bass frequency blow outs by proximity
			ref.value = FoaTransform.ar(sig, 'proximity', radius * renderer.longestRadius);
		};
	}

	key { ^key; }

	format { ^format; }
}

//-------------------------------------------//
//                   BFFMH                   //
//-------------------------------------------//

BFFMHDef : SpatDef {
	classvar <key, <format;

	*initClass {

		defList = defList.add(this.asClass);

		key = "BF-FMH";

		format = \FUMA;
	}

	prSetFunc { | maxOrder, renderer, server |
		var enc = MoscaUtils.bfOrFmh(maxOrder);

		spatFunc = { |ref, input, radius, radiusRoot, azimuth, elevation, difu, spre,
			contract, win, rate, rand|
			var sig = distFilter.value(input, radius);
			sig = enc.ar(ref.value + sig, azimuth, elevation,
				aten2distance.value(radiusRoot)); // invert to represnet distance
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
	classvar <key, <format;

	*initClass {

		defList = defList.add(this.asClass);

		key = "Josh";

		format = \FUMA;
	}

	prSetFunc { | maxOrder, renderer, server |

		spatFunc = { |ref, input, radius, radiusRoot, azimuth, elevation, difu, spre,
			contract, win, rate, rand|
			var sig = distFilter.value(input, radius);
			ref.value = MonoGrainBF.ar(ref.value + sig, win, rate, rand,
				azimuth, 1 - contract, elevation, 1 - contract,
				rho: aten2distance.value(radiusRoot), // invert to represent distance
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
	classvar <key, <format;

	*initClass {

		defList = defList.add(this.asClass);

		key = "VBAP";

		format = \NONAMBI;
	}

	prSetFunc { | maxOrder, renderer, server |

		spatFunc = { |ref, input, radius, radiusRoot, azimuth, elevation, difu, spre,
			contract, win, rate, rand|
			var sig = distFilter.value(input, radius),
			azi = azimuth * MoscaUtils.rad2deg, // convert to degrees
			elev = elevation * MoscaUtils.rad2deg, // convert to degrees
			elevexcess = Select.kr(elev < renderer.lowestElevation, [0, elev.abs]);
			elevexcess = Select.kr(elev > renderer.highestElevation, [0, elev]);
			// get elevation overshoot
			elev = elev.clip(renderer.lowestElevation, renderer.highestElevation);
			// restrict between min & max
			ref.value = VBAP.ar(renderer.numOutputs,
					ref.value + (sig * atenuator.value(radiusRoot)),
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
	classvar <defList, distFilter, atenuator, aten2distance;
	var <spatFunc;

	*initClass {

		Class.initClassTree(MoscaUtils);

		defList = [];

		distFilter = { | input, intens | // attenuate high freq with intens
			LPF.ar(input, (1 - intens) * 18000 + 2000);
		};

		atenuator = { | radRoot | (1 / radRoot - 1)};

		aten2distance = { | radRoot | 1 / atenuator.value(radRoot) };
	}

	*new { | maxOrder, renderer, server | ^super.new.prSetFunc(maxOrder, renderer, server); }
}
