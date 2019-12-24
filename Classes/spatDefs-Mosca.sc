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

	spatDef { |maxorder, bFormNumChan, bfOrFmh, fourOrNine|

		// Ambitools
		if (\HOAAzimuthRotator1.asClass.notNil) {
			spatList = spatList.add("Ambitools");

			spatFuncs = spatFuncs.add({ |ref, input, radius, distance, azimuth, elevation, difu, spre,
				contract, win, rate, rand|
				var sig = HOAEncoder.ar(maxorder,
					(ref.value + input), CircleRamp.kr(azimuth, 0.1, -pi, pi),
					Lag.kr(elevation), 0, 1, Lag.kr(radius), longest_radius);
				ref.value = (sig * contract) + [sig[0] * (1 - contract), Silent.ar(bFormNumChan - 1)];
			});
		};


		// HoaLib
		if (\HOALibEnc3D1.asClass.notNil) {
			spatList = spatList.add("HoaLib");

			spatFuncs = spatFuncs.add({ |ref, input, radius, distance, azimuth, elevation, difu, spre,
				contract, win, rate, rand|
				var sig = LPF.ar(input, (1 - distance) * 18000 + 2000);
				// attenuate high freq with distance
				sig = HOALibEnc3D.ar(maxorder,
					(ref.value + sig) * Lag.kr(longest_radius / radius),
					CircleRamp.kr(azimuth, 0.1, -pi, pi), Lag.kr(elevation), 0);
				ref.value = (sig * contract) + [sig[0] * (1 - contract), Silent.ar(bFormNumChan - 1)];
			});
		};

		// ADT
		if (\HOAmbiPanner1.asClass.notNil) {
			spatList = spatList.add("ADT");

			spatFuncs = spatFuncs.add({ |ref, input, radius, distance, azimuth, elevation, difu, spre,
				contract, win, rate, rand|
				var sig = LPF.ar(input, (1 - distance) * 18000 + 2000);
				// attenuate high freq with distance
				sig = HOAmbiPanner.ar(maxorder,
					(ref.value + sig) * Lag.kr(longest_radius / radius),
					CircleRamp.kr(azimuth, 0.1, -pi, pi), Lag.kr(elevation), 0);
				ref.value = (sig * contract) + [sig[0] * (1 - contract), Silent.ar(bFormNumChan - 1)];
			});
		};


		// ATK
		if (\FoaEncode.asClass.notNil) {
			spatList = spatList.add("ATK");

			spatFuncs = spatFuncs.add({ |ref, input, radius, distance, azimuth, elevation, difu, spre,
				contract, win, rate, rand|
				var diffuse, spread, omni,
				sig = LPF.ar(input, (1 - distance) * 18000 + 2000),
				// attenuate high freq with distance
				rad = Lag.kr(longest_radius / radius);
				sig = (sig + ref.value) * rad;
				omni = FoaEncode.ar(sig, foaEncoderOmni);
				spread = FoaEncode.ar(sig, foaEncoderSpread);
				diffuse = FoaEncode.ar(sig, foaEncoderDiffuse);
				sig = Select.ar(difu, [omni, diffuse]);
				sig = Select.ar(spre, [sig, spread]);
				sig = FoaTransform.ar(sig, 'push', halfPi * contract, azimuth, elevation);
				sig = HPF.ar(sig, 20); // stops bass frequency blow outs by proximity
				ref.value = FoaTransform.ar(sig, 'proximity', radius);
			});
		};

		// BF-FMH
		if (\FMHEncode1.asClass.notNil) {
			spatList = spatList.add("BF-FMH");

			spatFuncs = spatFuncs.add({ |ref, input, radius, distance, azimuth, elevation, difu, spre,
				contract, win, rate, rand|
				var sig = LPF.ar(input, (1 - distance) * 18000 + 2000);
				// attenuate high freq with distance
				sig = bfOrFmh.ar(ref.value + sig, azimuth, elevation,
					Lag.kr(longest_radius / radius), 0.5);
				ref.value = (sig * contract) + [sig[0] * (1 - contract), Silent.ar(fourOrNine - 1)];
			});
		};

		// joshGrain
		if (\MonoGrainBF.asClass.notNil) {
			spatList = spatList.add("josh");

			spatFuncs = spatFuncs.add({ |ref, input, radius, distance, azimuth, elevation, difu, spre,
				contract, win, rate, rand|
				var sig = LPF.ar(input, (1 - distance) * 18000 + 2000);
				// attenuate high freq with distance
				ref.value = MonoGrainBF.ar(ref.value + sig, win, rate, rand,
					azimuth, 1 - contract, elevation, 1 - contract,
					rho: Lag.kr(longest_radius / radius),
					mul: ((0.5 - win) + (1 - (rate / 40))).clip(0, 1) * 0.5 );
			});
		};

		// VBAP
		if (\VBAP.asClass.notNil) {
			spatList = spatList.add("VBAP");

			spatFuncs = spatFuncs.add({ |ref, input, radius, distance, azimuth, elevation, difu, spre,
			contract, win, rate, rand|
			var sig = LPF.ar(input, (1 - distance) * 18000 + 2000),
			// attenuate high freq with distance
			azi = azimuth * rad2deg, // convert to degrees
			elev = elevation * rad2deg, // convert to degrees
			elevexcess = Select.kr(elev < lowest_elevation, [0, elev.abs]);
			elevexcess = Select.kr(elev > highest_elevation, [0, elev]);
			// get elevation overshoot
			elev = elev.clip(lowest_elevation, highest_elevation);
			// restrict between min & max
			ref.value = VBAP.ar(numoutputs,
				(ref.value + sig) * (longest_radius / radius),
				vbap_buffer.bufnum, CircleRamp.kr(azi, 0.1, -180, 180), Lag.kr(elevation),
				((1 - contract) + (elevexcess / 90)) * 100) * 0.5;
			});
		};

	}

}