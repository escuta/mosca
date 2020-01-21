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
			lastN3D = lastN3D + 1; // increment last N3D lib index
			lastFUMA = lastFUMA + 1; // increment last FUMA lib index
			spatList = spatList.add("Ambitools");

			spatFuncs = spatFuncs.add({ |ref, input, radius, distance, azimuth, elevation, difu, spre,
				contract, win, rate, rand|
				var sig = HOAEncoder.ar(maxorder,
					(ref.value + input), CircleRamp.kr(azimuth, 0.1, -pi, pi),
					Lag.kr(elevation), 0, 1, Lag.kr(radius), longest_radius);
				ref.value = (sig * contract) + Silent.ar(bFormNumChan - 1).addFirst(sig[0] * (1 - contract));
			});
		};


		// HoaLib
		if (\HOALibEnc3D1.asClass.notNil) {
			lastN3D = lastN3D + 1; // increment last N3D lib index
			lastFUMA = lastFUMA + 1; // increment last FUMA lib index
			spatList = spatList.add("HoaLib");

			spatFuncs = spatFuncs.add({ |ref, input, radius, distance, azimuth, elevation, difu, spre,
				contract, win, rate, rand|
				var sig = LPF.ar(input, (1 - distance) * 18000 + 2000);
				// attenuate high freq with distance
				sig = HOALibEnc3D.ar(maxorder,
					ref.value + (sig * Lag.kr(longest_radius / radius)),
					CircleRamp.kr(azimuth, 0.1, -pi, pi), Lag.kr(elevation), 0);
				ref.value = (sig * contract) + Silent.ar(bFormNumChan - 1).addFirst(sig[0] * (1 - contract));
			});
		};

		// ADT
		if (\HOAmbiPanner1.asClass.notNil) {
			lastN3D = lastN3D + 1; // increment last N3D lib index
			lastFUMA = lastFUMA + 1; // increment last FUMA lib index
			spatList = spatList.add("ADT");

			spatFuncs = spatFuncs.add({ |ref, input, radius, distance, azimuth, elevation, difu, spre,
				contract, win, rate, rand|
				var sig = LPF.ar(input, (1 - distance) * 18000 + 2000);
				// attenuate high freq with distance
				sig = HOAmbiPanner.ar(maxorder,
					ref.value + (sig * Lag.kr(longest_radius / radius)),
					CircleRamp.kr(azimuth, 0.1, -pi, pi), Lag.kr(elevation), 0);
				ref.value = (sig * contract) + Silent.ar(bFormNumChan - 1).addFirst(sig[0] * (1 - contract));
			});
		};

		// SC-HOA
		if (\HOASphericalHarmonics.asClass.notNil) {
			lastN3D = lastN3D + 1; // increment last N3D lib index
			lastFUMA = lastFUMA + 1; // increment last FUMA lib index
			spatList = spatList.add("SC-HOA");

			spatFuncs = spatFuncs.add({ |ref, input, radius, distance, azimuth, elevation, difu, spre,
				contract, win, rate, rand|
				var sig = LPF.ar(input, (1 - distance) * 18000 + 2000);
				// attenuate high freq with distance
				sig = HOASphericalHarmonics.coefN3D(maxorder,
					CircleRamp.kr(azimuth, 0.1, -pi, pi), Lag.kr(elevation))
				* (ref.value + (sig * Lag.kr(longest_radius / radius)));
				ref.value = (sig * contract) + Silent.ar(bFormNumChan - 1).addFirst(sig[0] * (1 - contract));
			});
		};


		// ATK
		if (\FoaEncode.asClass.notNil) {
			lastFUMA = lastFUMA + 1; // increment last FUMA lib index
			spatList = spatList.add("ATK");

			spatFuncs = spatFuncs.add({ |ref, input, radius, distance, azimuth, elevation, difu, spre,
				contract, win, rate, rand|
				var diffuse, spread, omni,
				sig = LPF.ar(input, (1 - distance) * 18000 + 2000),
				// attenuate high freq with distance
				rad = Lag.kr(longest_radius / radius);
				sig = ref.value + (sig * rad);
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
			lastFUMA = lastFUMA + 1; // increment last FUMA lib index
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

		// JoshGrain
		if (\MonoGrainBF.asClass.notNil) {
			lastFUMA = lastFUMA + 1; // increment last FUMA lib index
			spatList = spatList.add("Josh");

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
					ref.value + (sig * (longest_radius / radius)),
				vbap_buffer.bufnum, CircleRamp.kr(azi, 0.1, -180, 180), Lag.kr(elevation),
				((1 - contract) + (elevexcess / 90)) * 100) * 0.5;
			});
		};

	}

	makeSpatialisers { | rev_type |
		var out_type = 0;

		spatList.do { |item, i|

			case
			{ i <= lastN3D } { out_type = 0 }
			{ (i > lastN3D) && (i <= lastFUMA) } { out_type = 1 }
			{ i > lastFUMA } { out_type = 2 };

			playList.do { |play_type, j|

				SynthDef(item++play_type++localReverbFunc[rev_type, 0], {
					| bufnum = 0, rate = 1, tpos = 0, lp = 0, busini,
					azim = 0, elev = 0, radius = 200, level = 1,
					dopamnt = 0, glev = 0, llev = 0,
					insertFlag = 0, insertOut, insertBack,
					room = 0.5, damp = 05, wir, df, sp,
					contr = 1, grainrate = 10, winsize = 0.1, winrand = 0 |

					var rad = Lag.kr(radius),
					dis = rad * 0.01,
					globallev = (1 / dis.sqrt) - 1, //global reverberation
					locallev, lrevRef = Ref(0),
					az = azim - halfPi,
					p = Ref(0),
					rd = dis * 340, // Doppler
					cut = ((1 - dis) * 2).clip(0, 1);
					//make shure level is 0 when radius reaches 100
					rad = rad.clip(1, 50);

					playInFunc[j].value(p, busini, bufnum, tpos, lp, rate, 1);
					p = p * level;
					p = DelayC.ar(p, 0.2, rd/1640.0 * dopamnt);

					localReverbFunc[rev_type, 1].value(lrevRef, p, wir, dis * llev,
						// local reverberation
						room, damp);

					spatFuncs[i].value(lrevRef, p, rad, dis, az, elev, df, sp, contr,
						winsize, grainrate, winrand);

					outPutFuncs[out_type].value(p * cut, lrevRef.value * cut,
						globallev.clip(0, 1) * glev);
				}).send(server);


				SynthDef(item++"Stereo"++play_type++localReverbFunc[rev_type, 0], {
					| bufnum = 0, rate = 1, tpos = 0, lp = 0, busini,
					azim = 0, elev = 0, radius = 0, level = 1,
					dopamnt = 0, glev = 0, llev = 0, angle = 1.05,
					insertFlag = 0, insertOut, insertBack,
					room = 0.5, damp = 05, wir, df, sp,
					contr = 1, grainrate = 10, winsize = 0.1, winrand = 0 |

					var rad = Lag.kr(radius),
					dis = rad * 0.01,
					globallev = (1 / dis.sqrt) - 1, //global reverberation
					lrev1Ref = Ref(0), lrev2Ref = Ref(0),
					az = Lag.kr(azim - halfPi),
					p = Ref(0),
					rd = dis * 340, // Doppler
					cut = ((1 - dis) * 2).clip(0, 1);
					//make shure level is 0 when radius reaches 100
					rad = rad.clip(1, 50);

					playInFunc[j].value(p, busini, bufnum, tpos, lp, rate, 2);
					p = p * level;
					p = DelayC.ar(p, 0.2, rd/1640.0 * dopamnt);

					localReverbFunc[rev_type, 2].value(lrev1Ref, lrev2Ref, p[0], p[1],
						wir, dis * llev, room, damp);

					spatFuncs[i].value(lrev1Ref, p[0], rad, dis, az - (angle * (1 - dis)),
						elev, df, sp, contr, winsize, grainrate, winrand);
					spatFuncs[i].value(lrev2Ref, p[1], rad, dis, az + (angle * (1 - dis)),
						elev, df, sp, contr, winsize, grainrate, winrand);

					outPutFuncs[out_type].value(Mix.ar(p) * 0.5 * cut,
						(lrev1Ref.value + lrev2Ref.value) * 0.5 * cut,
						globallev.clip(0, 1) * glev);
				}).send(server);

				if (item == "ATK") {

					// assume FuMa input
					SynthDef(\ATKBFormat++play_type++4, {
						| bufnum = 0, rate = 1, tpos = 0, lp = 0, busini,
						azim = 0, elev = 0, radius = 200, level = 1,
						dopamnt = 0, glev = 0, llev = 0,
						insertFlag = 0, insertOut, insertBack,
						room = 0.5, damp = 05, wir, df, sp,
						contr = 0, directang = 1, rotAngle = 0 |

						var rad = Lag.kr(radius),
						dis = rad * 0.01,
						pushang = contr * halfPi, // degree of sound field displacement
						globallev = (1 / dis.sqrt) - 1, //global reverberation
						locallev, lrevRef = Ref(0),
						az = azim - halfPi,
						p = Ref(0),
						rd = dis * 340, // Doppler
						cut = ((1 - dis) * 2).clip(0, 1);
						//make shure level is 0 when radius reaches 100
						rad = rad.clip(1, 50);

						playInFunc[j].value(p, busini, bufnum, tpos, lp, rate, 4);
						p = p * level;
						p = DelayC.ar(p, 0.2, rd/1640.0 * dopamnt);

						localReverbFunc[rev_type, 1].value(lrevRef, p[0], wir, dis * llev, room, damp);
						// local reverberation

						p = FoaDirectO.ar(lrevRef.value + p, directang);
						// directivity
						p = FoaTransform.ar(p, 'rotate', rotAngle);
						p = FoaTransform.ar(p, 'push', pushang, az, elev);

						p = p * cut;

						outPutFuncs[1].value(p, p,
							globallev.clip(0, 1) * glev);
					}).send(server);

					[9, 16, 25].do { |item, count|
						var ord = (item.sqrt) - 1;

						// assume N3D input
						SynthDef(\ATKBFormat++play_type++item, {
							| bufnum = 0, rate = 1, tpos = 0, lp = 0, busini,
							azim = 0, elev = 0, radius = 200, level = 1,
							dopamnt = 0, glev = 0, llev = 0,
							insertFlag = 0, insertOut, insertBack,
							room = 0.5, damp = 05, wir, df, sp,
							contr = 0, directang = 1, rotAngle = 0 |

							var rad = Lag.kr(radius),
							dis = rad * 0.01,
							pushang = contr * halfPi, // degree of sound field displacement
							globallev = (1 / dis.sqrt) - 1, //global reverberation
							locallev, lrevRef = Ref(0),
							az = azim - halfPi,
							p = Ref(0),
							rd = dis * 340, // Doppler
							cut = ((1 - dis) * 2).clip(0, 1);
							//make shure level is 0 when radius reaches 100
							rad = rad.clip(1, 50);

							playInFunc[j].value(p, busini, bufnum, tpos, lp, rate, 4);
							p = p * level;
							p = DelayC.ar(p, 0.2, rd/1640.0 * dopamnt);

							localReverbFunc[rev_type, 1].value(lrevRef, p[0], wir, dis * llev, room, damp);
							// local reverberation

							p = FoaEncode.ar(lrevRef.value + p, n2f);
							p = FoaDirectO.ar(p, directang);
							// directivity
							p = FoaTransform.ar(p, 'rotate', rotAngle);
							p = FoaTransform.ar(p, 'push', pushang, az, elev);

							p = p * cut;

							outPutFuncs[1].value(p, p,
								globallev.clip(0, 1) * glev);
						}).send(server);
					};
				};

				if (item == "Ambitools") {

					// assume FuMa input
					SynthDef(\AmbitoolsBFormat++play_type++4, {
						| bufnum = 0, rate = 1, tpos = 0, lp = 0, busini,
						azim = 0, elev = 0, radius = 200, level = 1,
						dopamnt = 0, glev = 0, llev = 0,
						insertFlag = 0, insertOut, insertBack,
						room = 0.5, damp = 05, wir, df, sp,
						contr = 0, rotAngle = 0|

						var rad = Lag.kr(radius),
						dis = rad * 0.01,
						globallev = (1 / dis.sqrt) - 1, //global reverberation
						locallev, lrevRef = Ref(0),
						az = azim - halfPi,
						p = Ref(0),
						rd = dis * 340, // Doppler
						cut = ((1 - dis) * 2).clip(0, 1);

						playInFunc[j].value(p, busini, bufnum, tpos, lp, rate, 4);
						p = p * level * (1 + (contr * 3));
						p = DelayC.ar(p, 0.2, rd/1640.0 * dopamnt);

						localReverbFunc[rev_type, 1].value(lrevRef, p[0], wir, dis * llev, room, damp);
						// local reverberation

						p = FoaDecode.ar(lrevRef.value + p, f2n);
						p = HOATransRotateAz.ar(1, p, rotAngle);
						p = HOABeamDirac2Hoa.ar(1, p, az, elev, timer_manual:1, focus:contr);

						p = p * cut;

						outPutFuncs[0].value(p, p,
							globallev.clip(0, 1) * glev);
					}).send(server);

					[9, 16, 25].do { |item, count|
						var ord = (item.sqrt) - 1;

						// assume N3D input
						SynthDef(\AmbitoolsBFormat++play_type++item, {
							| bufnum = 0, rate = 1, tpos = 0, lp = 0, busini,
							azim = 0, elev = 0, radius = 200, level = 1,
							dopamnt = 0, glev = 0, llev = 0,
							insertFlag = 0, insertOut, insertBack,
							room = 0.5, damp = 05, wir, df, sp,
							contr = 0, rotAngle = 0|

							var rad = Lag.kr(radius),
							dis = rad * 0.01,
							pushang = dis * halfPi, // degree of sound field displacement
							globallev = (1 / dis.sqrt) - 1, //global reverberation
							locallev, lrevRef = Ref(0),
							az = azim - halfPi,
							p = Ref(0),
							rd = dis * 340, // Doppler
							cut = ((1 - dis) * 2).clip(0, 1);
							//make shure level is 0 when radius reaches 100
							rad = rad.clip(1, 50);

							playInFunc[j].value(p, busini, bufnum, tpos, lp, rate, item);
							p = p * level * (1 + (contr * 3));
							p = DelayC.ar(p, 0.2, rd/1640.0 * dopamnt);

							localReverbFunc[rev_type, 1].value(lrevRef, p[0], wir, dis * llev, room, damp);
							// local reverberation

							p = HOATransRotateAz.ar(ord, lrevRef.value + p, rotAngle);
							p = HOABeamDirac2Hoa.ar(ord, p, az, elev, timer_manual:1, focus:contr);

							p = p * cut;

							outPutFuncs[0].value(p, p,
								globallev.clip(0, 1) * glev);
						}).send(server);
					};
				};

			};
		};
	}

}