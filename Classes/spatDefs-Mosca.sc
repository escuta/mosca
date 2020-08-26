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
//
// SpatDef {
//
// 	const halfPi = Mosca.halfPi, rad2deg = Mosca.rad2deg;
//
// 	classvar <spatList, spatFuncs, distFilter,
// 	// list of spat libs
// 	lastN3D = -1, // last N3D lib index
// 	lastFUMA = -1, // last FUMA lib index
// 	b2a, a2b, f2n, n2f;
//
// 	var bfOrFmh, fourOrNine, bFormNumChan,
// 	n3dbus, fumabus, gbus, gbfbus, gbixfbus,
// 	foaEncoderOmni, foaEncoderSpread, foaEncoderDiffuse,
// 	longest_radius, quarterRadius, twoAndaHalfRadius,
// 	lowest_elevation, highest_elevation;
//
// 	spatDef { |maxorder, bFormNumChan, bfOrFmh, fourOrNine|
//
// 		// all gains are suposed to match VBAP output levels
//
// 		// Ambitools
// 		if (\HOAAzimuthRotator1.asClass.notNil) {
// 			lastN3D = lastN3D + 1; // increment last N3D lib index
// 			lastFUMA = lastFUMA + 1; // increment last FUMA lib index
// 			spatList = spatList.add("Ambitools");
//
// 			spatFuncs = spatFuncs.add({ |ref, input, distance, azimuth, elevation, difu, spre,
// 				contract, win, rate, rand|
// 				var sig = HOAEncoder.ar(maxorder,
// 					(ref.value + input), CircleRamp.kr(azimuth, 0.1, -pi, pi),
// 				Lag.kr(elevation), 0, 1, distance.linlin(0, 0.75, quarterRadius, twoAndaHalfRadius), longest_radius);
// 				ref.value = (sig * contract) + Silent.ar(bFormNumChan - 1).addFirst(Mix(sig) * (1 - contract));
// 			});
// 		};
//
//
// 		// HoaLib
// 		if (\HOALibEnc3D1.asClass.notNil) {
// 			lastN3D = lastN3D + 1; // increment last N3D lib index
// 			lastFUMA = lastFUMA + 1; // increment last FUMA lib index
// 			spatList = spatList.add("HoaLib");
//
// 			spatFuncs = spatFuncs.add({ |ref, input, distance, azimuth, elevation, difu, spre,
// 				contract, win, rate, rand|
// 				var sig = LPF.ar(input, (1 - distance) * 18000 + 2000);
// 				// attenuate high freq with distance
// 				sig = HOALibEnc3D.ar(maxorder,
// 					ref.value + (sig * (longest_radius / distance.linlin(0, 0.75, quarterRadius, twoAndaHalfRadius))),
// 				CircleRamp.kr(azimuth, 0.1, -pi, pi), Lag.kr(elevation));
// 				ref.value = (sig * contract) + Silent.ar(bFormNumChan - 1).addFirst(Mix(sig) * (1 - contract));
// 			});
// 		};
//
// 		// ADT
// 		if (\HOAmbiPanner1.asClass.notNil) {
// 			lastN3D = lastN3D + 1; // increment last N3D lib index
// 			lastFUMA = lastFUMA + 1; // increment last FUMA lib index
// 			spatList = spatList.add("ADT");
//
// 			spatFuncs = spatFuncs.add({ |ref, input, distance, azimuth, elevation, difu, spre,
// 				contract, win, rate, rand|
// 				var sig = LPF.ar(input, (1 - distance) * 18000 + 2000);
// 				// attenuate high freq with distance
// 				sig = HOAmbiPanner.ar(maxorder,
// 					ref.value + (sig * (longest_radius / distance.linlin(0, 0.75, quarterRadius, twoAndaHalfRadius))),
// 				CircleRamp.kr(azimuth, 0.1, -pi, pi), Lag.kr(elevation));
// 				ref.value = (sig * contract) + Silent.ar(bFormNumChan - 1).addFirst(Mix(sig) * (1 - contract));
// 			});
// 		};
//
// 		// SC-HOA
// 		if (\HOASphericalHarmonics.asClass.notNil) {
// 			lastN3D = lastN3D + 1; // increment last N3D lib index
// 			lastFUMA = lastFUMA + 1; // increment last FUMA lib index
// 			spatList = spatList.add("SC-HOA");
//
// 			spatFuncs = spatFuncs.add({ |ref, input, distance, azimuth, elevation, difu, spre,
// 				contract, win, rate, rand|
// 				var sig = LPF.ar(input, (1 - distance) * 18000 + 2000);
// 				// attenuate high freq with distance
// 				sig = HOASphericalHarmonics.coefN3D(maxorder,
// 				CircleRamp.kr(azimuth, 0.1, -pi, pi), Lag.kr(elevation))
// 				* (ref.value + (sig * (longest_radius / distance.linlin(0, 0.75, quarterRadius, twoAndaHalfRadius))));
// 				ref.value = (sig * contract) + Silent.ar(bFormNumChan - 1).addFirst(Mix(sig) * (1 - contract));
// 			});
// 		};
//
//
// 		// ATK
// 		if (\FoaEncode.asClass.notNil) {
// 			lastFUMA = lastFUMA + 1; // increment last FUMA lib index
// 			spatList = spatList.add("ATK");
//
// 			spatFuncs = spatFuncs.add({ |ref, input, distance, azimuth, elevation, difu, spre,
// 				contract, win, rate, rand|
// 				var diffuse, spread, omni,
// 				sig = LPF.ar(input, (1 - distance) * 18000 + 2000),
// 				// attenuate high freq with distance
// 				rad = longest_radius / distance.linlin(0, 0.75, quarterRadius, twoAndaHalfRadius);
// 				sig = ref.value + (sig * rad);
// 				omni = FoaEncode.ar(sig, foaEncoderOmni);
// 				spread = FoaEncode.ar(sig, foaEncoderSpread);
// 				diffuse = FoaEncode.ar(sig, foaEncoderDiffuse);
// 				sig = Select.ar(difu, [omni, diffuse]);
// 				sig = Select.ar(spre, [sig, spread]);
// 				sig = FoaTransform.ar(sig, 'push', halfPi * contract, azimuth, elevation);
// 				sig = HPF.ar(sig, 20); // stops bass frequency blow outs by proximity
// 				ref.value = FoaTransform.ar(sig, 'proximity', distance);
// 			});
// 		};
//
// 		// BF-FMH
// 		if (\FMHEncode1.asClass.notNil) {
// 			lastFUMA = lastFUMA + 1; // increment last FUMA lib index
// 			spatList = spatList.add("BF-FMH");
//
// 			spatFuncs = spatFuncs.add({ |ref, input, distance, azimuth, elevation, difu, spre,
// 				contract, win, rate, rand|
// 				var sig = LPF.ar(input, (1 - distance) * 18000 + 2000);
// 				// attenuate high freq with distance
// 				sig = bfOrFmh.ar(ref.value + sig, azimuth, elevation,
// 				(longest_radius / distance.linlin(0, 0.75, quarterRadius, twoAndaHalfRadius)), 0.5);
// 				ref.value = (sig * contract) + Silent.ar(bFormNumChan - 1).addFirst(Mix(sig) * (1 - contract));
// 			});
// 		};
//
// 		// JoshGrain
// 		if (\MonoGrainBF.asClass.notNil) {
// 			lastFUMA = lastFUMA + 1; // increment last FUMA lib index
// 			spatList = spatList.add("Josh");
//
// 			spatFuncs = spatFuncs.add({ |ref, input, distance, azimuth, elevation, difu, spre,
// 				contract, win, rate, rand|
// 				var sig = LPF.ar(input, (1 - distance) * 18000 + 2000);
// 				// attenuate high freq with distance
// 				ref.value = MonoGrainBF.ar(ref.value + sig, win, rate, rand,
// 					azimuth, 1 - contract, elevation, 1 - contract,
// 					rho: (longest_radius / distance.linlin(0, 0.75, quarterRadius, twoAndaHalfRadius)) - 1,
// 				mul: ((0.5 - win) + (1 - (rate / 40))).clip(0, 1) * 0.5 );
// 			});
// 		};
//
// 		// VBAP
// 		if (\VBAP.asClass.notNil) {
// 			spatList = spatList.add("VBAP");
//
// 			spatFuncs = spatFuncs.add({ |ref, input, distance, azimuth, elevation, difu, spre,
// 				contract, win, rate, rand|
// 				var sig = LPF.ar(input, (1 - distance) * 18000 + 2000),
// 				// attenuate high freq with distance
// 				azi = azimuth * rad2deg, // convert to degrees
// 				elev = elevation * rad2deg, // convert to degrees
// 				elevexcess = Select.kr(elev < lowest_elevation, [0, elev.abs]);
// 				elevexcess = Select.kr(elev > highest_elevation, [0, elev]);
// 				// get elevation overshoot
// 				elev = elev.clip(lowest_elevation, highest_elevation);
// 				// restrict between min & max
// 				ref.value = VBAP.ar(numoutputs,
// 					ref.value + (sig * (longest_radius / distance.linlin(0, 0.75, quarterRadius, twoAndaHalfRadius))),
// 					vbap_buffer.bufnum, CircleRamp.kr(azi, 0.1, -180, 180), Lag.kr(elevation),
// 				((1 - contract) + (elevexcess / 90)) * 100);
// 			});
// 		};
//
// 	}
//
// 	makeSpatialisers { | rev_type |
// 		var out_type = 0;
//
// 		spatList.do { |item, i|
//
// 			case
// 			{ i <= lastN3D } { out_type = 0 }
// 			{ (i > lastN3D) && (i <= lastFUMA) } { out_type = 1 }
// 			{ i > lastFUMA } { out_type = 2 };
//
// 			playList.do { |play_type, j|
// 				var mono, stereo;
//
// 				mono = SynthDef(item++play_type++localReverbFunc[rev_type, 0], {
// 					| bufnum = 0, rate = 1, tpos = 0, lp = 0, busini,
// 					azim = 0, elev = 0, radius = 20, amp = 1,
// 					dopamnt = 0, glev = 0, llev = 0,
// 					insertFlag = 0, insertOut, insertBack,
// 					room = 0.5, damp = 05, wir, df, sp,
// 					contr = 1, grainrate = 10, winsize = 0.1, winrand = 0 |
//
// 					var rad = Lag.kr(radius),
// 					globallev = (1 / rad.sqrt) - 1, //global reverberation
// 					locallev, lrevRef = Ref(0),
// 					az = azim - halfPi,
// 					p = Ref(0),
// 					rd = rad * 340, // Doppler
// 					revCut = rad.lincurve(1, plim, 1, 0),
// 					cut = rad.linlin(0.75, 1, 1, 0);
// 					rad = rad.max(0.01);
//
// 					playInFunc[j].value(p, busini, bufnum, tpos, lp, rate, 1);
// 					p = p * amp;
// 					p = DelayC.ar(p, 0.2, rd/1640.0 * dopamnt);
//
// 					localReverbFunc[rev_type, 1].value(lrevRef, p, wir, rad * llev,
// 						// local reverberation
// 					room, damp);
//
// 					lrevRef.value = lrevRef.value * revCut;
//
// 					spatFuncs[i].value(lrevRef, p * cut, rad, az, elev, df, sp, contr,
// 					winsize, grainrate, winrand);
//
// 					outPutFuncs[out_type].value(p, lrevRef.value,
// 					globallev.clip(0, 1) * glev);
// 				});
//
//
// 				stereo = SynthDef(item++"Stereo"++play_type++localReverbFunc[rev_type, 0], {
// 					| bufnum = 0, rate = 1, tpos = 0, lp = 0, busini,
// 					azim = 0, elev = 0, radius = 20, amp = 1,
// 					dopamnt = 0, glev = 0, llev = 0, angle = 1.05,
// 					insertFlag = 0, insertOut, insertBack,
// 					room = 0.5, damp = 05, wir, df, sp,
// 					contr = 1, grainrate = 10, winsize = 0.1, winrand = 0 |
//
// 					var rad = Lag.kr(radius),
// 					globallev = (1 / rad.sqrt) - 1, //global reverberation
// 					lrev1Ref = Ref(0), lrev2Ref = Ref(0),
// 					az = Lag.kr(azim - halfPi),
// 					p = Ref(0),
// 					rd = rad * 340, // Doppler
// 					revCut = rad.lincurve(1, plim, 1, 0),
// 					cut = rad.linlin(0.75, 1, 1, 0);
// 					rad = rad.max(0.01);
//
// 					playInFunc[j].value(p, busini, bufnum, tpos, lp, rate, 2);
// 					p = p * amp;
// 					p = DelayC.ar(p, 0.2, rd/1640.0 * dopamnt);
//
// 					localReverbFunc[rev_type, 2].value(lrev1Ref, lrev2Ref, p[0], p[1],
// 					wir, rad * llev, room, damp);
//
// 					lrev1Ref.value = lrev1Ref.value * revCut;
// 					lrev2Ref.value = lrev2Ref.value * revCut;
//
// 					p = p * cut;
//
// 					spatFuncs[i].value(lrev1Ref, p[0], rad, az - (angle * (1 - rad)),
// 					elev, df, sp, contr, winsize, grainrate, winrand);
// 					spatFuncs[i].value(lrev2Ref, p[1], rad, az + (angle * (1 - rad)),
// 					elev, df, sp, contr, winsize, grainrate, winrand);
//
// 					outPutFuncs[out_type].value(Mix.ar(p) * 0.5,
// 						(lrev1Ref.value + lrev2Ref.value) * 0.5,
// 					globallev.clip(0, 1) * glev);
// 				});
//
// 				if (maxorder < 3) {
// 					mono.send(server);
// 					stereo.send(server);
// 				} {
// 					if (maxorder == 3) {
// 						mono.send(server);
// 						stereo.load(server);
// 					} {
// 						mono.load(server);
// 						stereo.load(server);
// 					};
// 				};
//
// 				if (item == "ATK") {
//
// 					// assume FuMa input
// 					SynthDef(\ATKBFormat++play_type++4, {
// 						| bufnum = 0, rate = 1, tpos = 0, lp = 0, busini,
// 						azim = 0, elev = 0, radius = 20, amp = 1,
// 						dopamnt = 0, glev = 0, llev = 0,
// 						insertFlag = 0, insertOut, insertBack,
// 						room = 0.5, damp = 05, wir, df, sp,
// 						contr = 0, directang = 1, rotAngle = 0 |
//
// 						var rad = Lag.kr(radius),
// 						pushang = 2 - (contr * 2),
// 						globallev = (1 / rad.sqrt) - 1, //global reverberation
// 						locallev, lrevRef = Ref(0),
// 						az = azim - halfPi,
// 						p = Ref(0),
// 						rd = rad * 340, // Doppler
// 						revCut = rad.lincurve(1, plim, 1, 0),
// 						cut = rad.linlin(0.75, 1, 1, 0);
// 						rad = rad.max(0.01);
// 						pushang = radius.linlin(pushang - 1, pushang, 0, halfPi); // degree of sound field displacement
//
//
// 						playInFunc[j].value(p, busini, bufnum, tpos, lp, rate, 4);
// 						p = p * amp;
// 						p = DelayC.ar(p, 0.2, rd/1640.0 * dopamnt);
//
// 						localReverbFunc[rev_type, 1].value(lrevRef, p[0], wir, rad * llev, room, damp);
// 						// local reverberation
//
// 						p = FoaDirectO.ar((lrevRef.value * revCut) + (p * cut), directang);
// 						// directivity
// 						p = FoaTransform.ar(p, 'rotate', rotAngle);
// 						p = FoaTransform.ar(p, 'push', pushang, az, elev);
//
// 						outPutFuncs[1].value(p, p,
// 						globallev.clip(0, 1) * glev);
// 					}).send(server);
//
// 					[9, 16, 25].do { |item, count|
// 						var ord = (item.sqrt) - 1;
// 						// assume N3D input
// 						SynthDef(\ATKBFormat++play_type++item, {
// 							| bufnum = 0, rate = 1, tpos = 0, lp = 0, busini,
// 							azim = 0, elev = 0, radius = 20, amp = 1,
// 							dopamnt = 0, glev = 0, llev = 0,
// 							insertFlag = 0, insertOut, insertBack,
// 							room = 0.5, damp = 05, wir, df, sp,
// 							contr = 0, directang = 1, rotAngle = 0 |
//
// 							var rad = Lag.kr(radius),
// 							pushang = 2 - (contr * 2),
// 							globallev = (1 / rad.sqrt) - 1, //global reverberation
// 							locallev, lrevRef = Ref(0),
// 							az = azim - halfPi,
// 							p = Ref(0),
// 							rd = rad * 340, // Doppler
// 							revCut = rad.lincurve(1, plim, 1, 0),
// 							cut = rad.linlin(0.75, 1, 1, 0);
// 							rad = rad.max(0.01);
// 							pushang = radius.linlin(pushang - 1, pushang, 0, halfPi); // degree of sound field displacement
//
// 							playInFunc[j].value(p, busini, bufnum, tpos, lp, rate, 4);
// 							p = p * amp;
// 							p = DelayC.ar(p, 0.2, rd/1640.0 * dopamnt);
//
// 							localReverbFunc[rev_type, 1].value(lrevRef, p[0], wir, rad * llev, room, damp);
// 							// local reverberation
//
// 							p = FoaEncode.ar((lrevRef.value * revCut) + (p * cut), n2f);
// 							p = FoaDirectO.ar(p, directang);
// 							// directivity
// 							p = FoaTransform.ar(p, 'rotate', rotAngle);
// 							p = FoaTransform.ar(p, 'push', pushang, az, elev);
//
// 							outPutFuncs[1].value(p, p,
// 							globallev.clip(0, 1) * glev);
// 						});
// 					};
// 				};
//
// 				if (item == "Ambitools") {
//
// 					// assume FuMa input
// 					SynthDef(\AmbitoolsBFormat++play_type++4, {
// 						| bufnum = 0, rate = 1, tpos = 0, lp = 0, busini,
// 						azim = 0, elev = 0, radius = 20, amp = 1,
// 						dopamnt = 0, glev = 0, llev = 0,
// 						insertFlag = 0, insertOut, insertBack,
// 						room = 0.5, damp = 05, wir, df, sp,
// 						contr = 0, rotAngle = 0|
//
// 						var rad = Lag.kr(radius),
// 						pushang = 2 - (contr * 2),
// 						globallev = (1 / rad.sqrt) - 1, //global reverberation
// 						locallev, lrevRef = Ref(0),
// 						az = azim - halfPi,
// 						p = Ref(0),
// 						rd = rad * 340, // Doppler
// 						revCut = rad.lincurve(1, plim, 1, 0),
// 						cut = rad.linlin(0.75, 1, 1, 0);
// 						rad = rad.max(0.01);
// 						pushang = radius.linlin(pushang - 1, pushang, 0, 1); // degree of sound field displacement
//
//
// 						playInFunc[j].value(p, busini, bufnum, tpos, lp, rate, 4);
// 						p = p * amp * (1 + (contr * 3));
// 						p = DelayC.ar(p, 0.2, rd/1640.0 * dopamnt);
//
// 						localReverbFunc[rev_type, 1].value(lrevRef, p[0], wir, rad * llev, room, damp);
// 						// local reverberation
//
// 						p = FoaDecode.ar((lrevRef.value * revCut) + (p * cut), f2n);
// 						p = HOATransRotateAz.ar(1, p, rotAngle);
// 						p = HOABeamDirac2Hoa.ar(1, p, az, elev, timer_manual:1, focus:pushang);
//
// 						outPutFuncs[0].value(p, p,
// 						globallev.clip(0, 1) * glev);
// 					}).send(server);
//
// 					[9, 16, 25].do { |item, count|
// 						var ord = (item.sqrt) - 1,
//
// 						// assume N3D input
// 						hoaSynth = SynthDef(\AmbitoolsBFormat++play_type++item, {
// 							| bufnum = 0, rate = 1, tpos = 0, lp = 0, busini,
// 							azim = 0, elev = 0, radius = 20, amp = 1,
// 							dopamnt = 0, glev = 0, llev = 0,
// 							insertFlag = 0, insertOut, insertBack,
// 							room = 0.5, damp = 05, wir, df, sp,
// 							contr = 0, rotAngle = 0|
//
// 							var rad = Lag.kr(radius),
// 							pushang = 2 - (contr * 2),
// 							globallev = (1 / rad.sqrt) - 1, //global reverberation
// 							locallev, lrevRef = Ref(0),
// 							az = azim - halfPi,
// 							p = Ref(0),
// 							rd = rad * 340, // Doppler
// 							revCut = rad.lincurve(1, plim, 1, 0),
// 							cut = rad.linlin(0.75, 1, 1, 0);
// 							rad = rad.max(0.01);
// 							pushang = radius.linlin(pushang - 1, pushang, 0, 1); // degree of sound field displacement
//
// 							playInFunc[j].value(p, busini, bufnum, tpos, lp, rate, item);
// 							p = p * amp * (1 + (contr * 3));
// 							p = DelayC.ar(p, 0.2, rd/1640.0 * dopamnt);
//
// 							localReverbFunc[rev_type, 1].value(lrevRef, p[0], wir, rad * llev, room, damp);
// 							// local reverberation
//
// 							p = HOATransRotateAz.ar(ord, lrevRef.value * revCut + (p * cut), rotAngle);
// 							p = HOABeamDirac2Hoa.ar(ord, p, az, elev, timer_manual:1, focus:pushang);
//
// 							outPutFuncs[0].value(p, p,
// 							globallev.clip(0, 1) * glev);
// 						});
//
// 						if (item > 16) {
// 							hoaSynth.load(server);
// 						} { hoaSynth.send(server);
// 						};
//
// 					};
// 				};
//
// 			};
// 		};
// 	}
//
// 	*new { |order, server|
//
// 		^super.new().spatCtr(order, server);
// 	}
//
// 	spatCtr { |order, server|
//
// 		if (order > 1) {
// 			bfOrFmh = FMHEncode1;
// 			fourOrNine = 9;
// 		} {
// 			bfOrFmh = BFEncode1;
// 			fourOrNine = 4;
// 		};
//
// 		foaEncoderOmni = FoaEncoderMatrix.newOmni;
// 		foaEncoderSpread = FoaEncoderKernel.newSpread (subjectID: 6, kernelSize: 2048,
// 		server:server, sampleRate:server.sampleRate.asInteger);
// 		foaEncoderDiffuse = FoaEncoderKernel.newDiffuse (subjectID: 3, kernelSize: 2048,
// 		server:server, sampleRate:server.sampleRate.asInteger);
//
// 		n3dbus = Bus.audio(server, bFormNumChan); // global b-format ACN-SN3D bus
// 		fumabus = Bus.audio(server, fourOrNine);
// 		gbus = Bus.audio(server, 1); // global reverb bus
// 		gbfbus = Bus.audio(server, fourOrNine); // global b-format bus
// 		gbixfbus = Bus.audio(server, fourOrNine); // global n3d b-format bus
// 	}
//
// 	intiClass {
//
// 		distFilter = { | in, dist |
// 			^LPF.ar(in, (1 - dist) * 18000 + 2000);
// 		};
//
// 		b2a = FoaDecoderMatrix.newBtoA;
// 		a2b = FoaEncoderMatrix.newAtoB;
// 		n2f = FoaEncoderMatrix.newHoa1();
// 		f2n = FoaDecoderMatrix.newHoa1();
// 	}
// }


+ Mosca {

	spatDef { |maxorder, bFormNumChan, bfOrFmh, fourOrNine|

		var halfPi = MoscaUtils.halfPi(), rad2deg = MoscaUtils.rad2deg();

		// all gains are suposed to match VBAP output levels

		// Ambitools
		if (\HOAAzimuthRotator1.asClass.notNil) {
			lastN3D = lastN3D + 1; // increment last N3D lib index
			lastFUMA = lastFUMA + 1; // increment last FUMA lib index
			spatList = spatList.add("Ambitools");

			spatFuncs = spatFuncs.add({ |ref, input, distance, azimuth, elevation, difu, spre,
				contract, win, rate, rand|
				var sig = HOAEncoder.ar(maxorder,
					(ref.value + input), CircleRamp.kr(azimuth, 0.1, -pi, pi),
					Lag.kr(elevation), 0, 1, distance.linlin(0, 0.75, quarterRadius, twoAndaHalfRadius), longest_radius);
				ref.value = (sig * contract) + Silent.ar(bFormNumChan - 1).addFirst(Mix(sig) * (1 - contract));
			});
		};


		// HoaLib
		if (\HOALibEnc3D1.asClass.notNil) {
			lastN3D = lastN3D + 1; // increment last N3D lib index
			lastFUMA = lastFUMA + 1; // increment last FUMA lib index
			spatList = spatList.add("HoaLib");

			spatFuncs = spatFuncs.add({ |ref, input, distance, azimuth, elevation, difu, spre,
				contract, win, rate, rand|
				var sig = LPF.ar(input, (1 - distance) * 18000 + 2000);
				// attenuate high freq with distance
				sig = HOALibEnc3D.ar(maxorder,
					ref.value + (sig * (longest_radius / distance.linlin(0, 0.75, quarterRadius, twoAndaHalfRadius))),
					CircleRamp.kr(azimuth, 0.1, -pi, pi), Lag.kr(elevation));
				ref.value = (sig * contract) + Silent.ar(bFormNumChan - 1).addFirst(Mix(sig) * (1 - contract));
			});
		};

		// ADT
		if (\HOAmbiPanner1.asClass.notNil) {
			lastN3D = lastN3D + 1; // increment last N3D lib index
			lastFUMA = lastFUMA + 1; // increment last FUMA lib index
			spatList = spatList.add("ADT");

			spatFuncs = spatFuncs.add({ |ref, input, distance, azimuth, elevation, difu, spre,
				contract, win, rate, rand|
				var sig = LPF.ar(input, (1 - distance) * 18000 + 2000);
				// attenuate high freq with distance
				sig = HOAmbiPanner.ar(maxorder,
					ref.value + (sig * (longest_radius / distance.linlin(0, 0.75, quarterRadius, twoAndaHalfRadius))),
					CircleRamp.kr(azimuth, 0.1, -pi, pi), Lag.kr(elevation));
				ref.value = (sig * contract) + Silent.ar(bFormNumChan - 1).addFirst(Mix(sig) * (1 - contract));
			});
		};

		// SC-HOA
		if (\HOASphericalHarmonics.asClass.notNil) {
			lastN3D = lastN3D + 1; // increment last N3D lib index
			lastFUMA = lastFUMA + 1; // increment last FUMA lib index
			spatList = spatList.add("SC-HOA");

			spatFuncs = spatFuncs.add({ |ref, input, distance, azimuth, elevation, difu, spre,
				contract, win, rate, rand|
				var sig = LPF.ar(input, (1 - distance) * 18000 + 2000);
				// attenuate high freq with distance
				sig = HOASphericalHarmonics.coefN3D(maxorder,
					CircleRamp.kr(azimuth, 0.1, -pi, pi), Lag.kr(elevation))
				* (ref.value + (sig * (longest_radius / distance.linlin(0, 0.75, quarterRadius, twoAndaHalfRadius))));
				ref.value = (sig * contract) + Silent.ar(bFormNumChan - 1).addFirst(Mix(sig) * (1 - contract));
			});
		};


		// ATK
		if (\FoaEncode.asClass.notNil) {
			lastFUMA = lastFUMA + 1; // increment last FUMA lib index
			spatList = spatList.add("ATK");

			spatFuncs = spatFuncs.add({ |ref, input, distance, azimuth, elevation, difu, spre,
				contract, win, rate, rand|
				var diffuse, spread, omni,
				sig = LPF.ar(input, (1 - distance) * 18000 + 2000),
				// attenuate high freq with distance
				rad = longest_radius / distance.linlin(0, 0.75, quarterRadius, twoAndaHalfRadius);
				sig = ref.value + (sig * rad);
				omni = FoaEncode.ar(sig, foaEncoderOmni);
				spread = FoaEncode.ar(sig, foaEncoderSpread);
				diffuse = FoaEncode.ar(sig, foaEncoderDiffuse);
				sig = Select.ar(difu, [omni, diffuse]);
				sig = Select.ar(spre, [sig, spread]);
				sig = FoaTransform.ar(sig, 'push', halfPi * contract, azimuth, elevation);
				sig = HPF.ar(sig, 20); // stops bass frequency blow outs by proximity
				ref.value = FoaTransform.ar(sig, 'proximity', distance);
			});
		};

		// BF-FMH
		if (\FMHEncode1.asClass.notNil) {
			lastFUMA = lastFUMA + 1; // increment last FUMA lib index
			spatList = spatList.add("BF-FMH");

			spatFuncs = spatFuncs.add({ |ref, input, distance, azimuth, elevation, difu, spre,
				contract, win, rate, rand|
				var sig = LPF.ar(input, (1 - distance) * 18000 + 2000);
				// attenuate high freq with distance
				sig = bfOrFmh.ar(ref.value + sig, azimuth, elevation,
					(longest_radius / distance.linlin(0, 0.75, quarterRadius, twoAndaHalfRadius)), 0.5);
				ref.value = (sig * contract) + Silent.ar(bFormNumChan - 1).addFirst(Mix(sig) * (1 - contract));
			});
		};

		// JoshGrain
		if (\MonoGrainBF.asClass.notNil) {
			lastFUMA = lastFUMA + 1; // increment last FUMA lib index
			spatList = spatList.add("Josh");

			spatFuncs = spatFuncs.add({ |ref, input, distance, azimuth, elevation, difu, spre,
				contract, win, rate, rand|
				var sig = LPF.ar(input, (1 - distance) * 18000 + 2000);
				// attenuate high freq with distance
				ref.value = MonoGrainBF.ar(ref.value + sig, win, rate, rand,
					azimuth, 1 - contract, elevation, 1 - contract,
					rho: (longest_radius / distance.linlin(0, 0.75, quarterRadius, twoAndaHalfRadius)) - 1,
					mul: ((0.5 - win) + (1 - (rate / 40))).clip(0, 1) * 0.5 );
			});
		};

		// VBAP
		if (\VBAP.asClass.notNil) {
			spatList = spatList.add("VBAP");

			spatFuncs = spatFuncs.add({ |ref, input, distance, azimuth, elevation, difu, spre,
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
					ref.value + (sig * (longest_radius / distance.linlin(0, 0.75, quarterRadius, twoAndaHalfRadius))),
					vbap_buffer.bufnum, CircleRamp.kr(azi, 0.1, -180, 180), Lag.kr(elevation),
					((1 - contract) + (elevexcess / 90)) * 100);
			});
		};

	}

	makeSpatialisers { | rev_type |
		var out_type = 0, halfPi = MoscaUtils.halfPi(), plim = MoscaUtils.plim();

		spatList.do { |item, i|

			case
			{ i <= lastN3D } { out_type = 0 }
			{ (i > lastN3D) && (i <= lastFUMA) } { out_type = 1 }
			{ i > lastFUMA } { out_type = 2 };

			playList.do { |play_type, j|
				var mono, stereo;

				mono = SynthDef(item++play_type++localReverbFunc[rev_type, 0], {
					| bufnum = 0, rate = 1, tpos = 0, lp = 0, busini,
					azim = 0, elev = 0, radius = 20, amp = 1,
					dopamnt = 0, glev = 0, llev = 0,
					insertFlag = 0, insertOut, insertBack,
					room = 0.5, damp = 05, wir, df, sp,
					contr = 1, grainrate = 10, winsize = 0.1, winrand = 0 |

					var rad = Lag.kr(radius),
					globallev = (1 / rad.sqrt) - 1, //global reverberation
					locallev, lrevRef = Ref(0),
					az = azim - halfPi,
					p = Ref(0),
					rd = rad * 340, // Doppler
					revCut = rad.lincurve(1, plim, 1, 0),
					cut = rad.linlin(0.75, 1, 1, 0);
					rad = rad.max(0.01);

					playInFunc[j].value(p, busini, bufnum, tpos, lp, rate, 1);
					p = p * amp;
					p = DelayC.ar(p, 0.2, rd/1640.0 * dopamnt);

					localReverbFunc[rev_type, 1].value(lrevRef, p, wir, rad * llev,
						// local reverberation
						room, damp);

					lrevRef.value = lrevRef.value * revCut;

					spatFuncs[i].value(lrevRef, p * cut, rad, az, elev, df, sp, contr,
						winsize, grainrate, winrand);

					outPutFuncs[out_type].value(p, lrevRef.value,
						globallev.clip(0, 1) * glev);
				});


				stereo = SynthDef(item++"Stereo"++play_type++localReverbFunc[rev_type, 0], {
					| bufnum = 0, rate = 1, tpos = 0, lp = 0, busini,
					azim = 0, elev = 0, radius = 20, amp = 1,
					dopamnt = 0, glev = 0, llev = 0, angle = 1.05,
					insertFlag = 0, insertOut, insertBack,
					room = 0.5, damp = 05, wir, df, sp,
					contr = 1, grainrate = 10, winsize = 0.1, winrand = 0 |

					var rad = Lag.kr(radius),
					globallev = (1 / rad.sqrt) - 1, //global reverberation
					lrev1Ref = Ref(0), lrev2Ref = Ref(0),
					az = Lag.kr(azim - halfPi),
					p = Ref(0),
					rd = rad * 340, // Doppler
					revCut = rad.lincurve(1, plim, 1, 0),
					cut = rad.linlin(0.75, 1, 1, 0);
					rad = rad.max(0.01);

					playInFunc[j].value(p, busini, bufnum, tpos, lp, rate, 2);
					p = p * amp;
					p = DelayC.ar(p, 0.2, rd/1640.0 * dopamnt);

					localReverbFunc[rev_type, 2].value(lrev1Ref, lrev2Ref, p[0], p[1],
						wir, rad * llev, room, damp);

					lrev1Ref.value = lrev1Ref.value * revCut;
					lrev2Ref.value = lrev2Ref.value * revCut;

					p = p * cut;

					spatFuncs[i].value(lrev1Ref, p[0], rad, az - (angle * (1 - rad)),
						elev, df, sp, contr, winsize, grainrate, winrand);
					spatFuncs[i].value(lrev2Ref, p[1], rad, az + (angle * (1 - rad)),
						elev, df, sp, contr, winsize, grainrate, winrand);

					outPutFuncs[out_type].value(Mix.ar(p) * 0.5,
						(lrev1Ref.value + lrev2Ref.value) * 0.5,
						globallev.clip(0, 1) * glev);
				});

				if (maxorder < 3) {
					mono.send(server);
					stereo.send(server);
				} {
					if (maxorder == 3) {
						mono.send(server);
						stereo.load(server);
					} {
						mono.load(server);
						stereo.load(server);
					};
				};

				if (item == "ATK") {

					// assume FuMa input
					SynthDef(\ATKBFormat++play_type++4, {
						| bufnum = 0, rate = 1, tpos = 0, lp = 0, busini,
						azim = 0, elev = 0, radius = 20, amp = 1,
						dopamnt = 0, glev = 0, llev = 0,
						insertFlag = 0, insertOut, insertBack,
						room = 0.5, damp = 05, wir, df, sp,
						contr = 0, directang = 1, rotAngle = 0 |

						var rad = Lag.kr(radius),
						pushang = 2 - (contr * 2),
						globallev = (1 / rad.sqrt) - 1, //global reverberation
						locallev, lrevRef = Ref(0),
						az = azim - halfPi,
						p = Ref(0),
						rd = rad * 340, // Doppler
						revCut = rad.lincurve(1, plim, 1, 0),
						cut = rad.linlin(0.75, 1, 1, 0);
						rad = rad.max(0.01);
						pushang = radius.linlin(pushang - 1, pushang, 0, halfPi); // degree of sound field displacement


						playInFunc[j].value(p, busini, bufnum, tpos, lp, rate, 4);
						p = p * amp;
						p = DelayC.ar(p, 0.2, rd/1640.0 * dopamnt);

						localReverbFunc[rev_type, 1].value(lrevRef, p[0], wir, rad * llev, room, damp);
						// local reverberation

						p = FoaDirectO.ar((lrevRef.value * revCut) + (p * cut), directang);
						// directivity
						p = FoaTransform.ar(p, 'rotate', rotAngle);
						p = FoaTransform.ar(p, 'push', pushang, az, elev);

						outPutFuncs[1].value(p, p,
							globallev.clip(0, 1) * glev);
					}).send(server);

					[9, 16, 25].do { |item, count|
						var ord = (item.sqrt) - 1;
						// assume N3D input
						SynthDef(\ATKBFormat++play_type++item, {
							| bufnum = 0, rate = 1, tpos = 0, lp = 0, busini,
							azim = 0, elev = 0, radius = 20, amp = 1,
							dopamnt = 0, glev = 0, llev = 0,
							insertFlag = 0, insertOut, insertBack,
							room = 0.5, damp = 05, wir, df, sp,
							contr = 0, directang = 1, rotAngle = 0 |

							var rad = Lag.kr(radius),
							pushang = 2 - (contr * 2),
							globallev = (1 / rad.sqrt) - 1, //global reverberation
							locallev, lrevRef = Ref(0),
							az = azim - halfPi,
							p = Ref(0),
							rd = rad * 340, // Doppler
							revCut = rad.lincurve(1, plim, 1, 0),
							cut = rad.linlin(0.75, 1, 1, 0);
							rad = rad.max(0.01);
							pushang = radius.linlin(pushang - 1, pushang, 0, halfPi); // degree of sound field displacement

							playInFunc[j].value(p, busini, bufnum, tpos, lp, rate, 4);
							p = p * amp;
							p = DelayC.ar(p, 0.2, rd/1640.0 * dopamnt);

							localReverbFunc[rev_type, 1].value(lrevRef, p[0], wir, rad * llev, room, damp);
							// local reverberation

							p = FoaEncode.ar((lrevRef.value * revCut) + (p * cut), n2f);
							p = FoaDirectO.ar(p, directang);
							// directivity
							p = FoaTransform.ar(p, 'rotate', rotAngle);
							p = FoaTransform.ar(p, 'push', pushang, az, elev);

							outPutFuncs[1].value(p, p,
								globallev.clip(0, 1) * glev);
						});
					};
				};

				if (item == "Ambitools") {

					// assume FuMa input
					SynthDef(\AmbitoolsBFormat++play_type++4, {
						| bufnum = 0, rate = 1, tpos = 0, lp = 0, busini,
						azim = 0, elev = 0, radius = 20, amp = 1,
						dopamnt = 0, glev = 0, llev = 0,
						insertFlag = 0, insertOut, insertBack,
						room = 0.5, damp = 05, wir, df, sp,
						contr = 0, rotAngle = 0|

						var rad = Lag.kr(radius),
						pushang = 2 - (contr * 2),
						globallev = (1 / rad.sqrt) - 1, //global reverberation
						locallev, lrevRef = Ref(0),
						az = azim - halfPi,
						p = Ref(0),
						rd = rad * 340, // Doppler
						revCut = rad.lincurve(1, plim, 1, 0),
						cut = rad.linlin(0.75, 1, 1, 0);
						rad = rad.max(0.01);
						pushang = radius.linlin(pushang - 1, pushang, 0, 1); // degree of sound field displacement


						playInFunc[j].value(p, busini, bufnum, tpos, lp, rate, 4);
						p = p * amp * (1 + (contr * 3));
						p = DelayC.ar(p, 0.2, rd/1640.0 * dopamnt);

						localReverbFunc[rev_type, 1].value(lrevRef, p[0], wir, rad * llev, room, damp);
						// local reverberation

						p = FoaDecode.ar((lrevRef.value * revCut) + (p * cut), f2n);
						p = HOATransRotateAz.ar(1, p, rotAngle);
						p = HOABeamDirac2Hoa.ar(1, p, az, elev, timer_manual:1, focus:pushang);

						outPutFuncs[0].value(p, p,
							globallev.clip(0, 1) * glev);
					}).send(server);

					[9, 16, 25].do { |item, count|
						var ord = (item.sqrt) - 1,

						// assume N3D input
						hoaSynth = SynthDef(\AmbitoolsBFormat++play_type++item, {
							| bufnum = 0, rate = 1, tpos = 0, lp = 0, busini,
							azim = 0, elev = 0, radius = 20, amp = 1,
							dopamnt = 0, glev = 0, llev = 0,
							insertFlag = 0, insertOut, insertBack,
							room = 0.5, damp = 05, wir, df, sp,
							contr = 0, rotAngle = 0|

							var rad = Lag.kr(radius),
							pushang = 2 - (contr * 2),
							globallev = (1 / rad.sqrt) - 1, //global reverberation
							locallev, lrevRef = Ref(0),
							az = azim - halfPi,
							p = Ref(0),
							rd = rad * 340, // Doppler
							revCut = rad.lincurve(1, plim, 1, 0),
							cut = rad.linlin(0.75, 1, 1, 0);
							rad = rad.max(0.01);
							pushang = radius.linlin(pushang - 1, pushang, 0, 1); // degree of sound field displacement

							playInFunc[j].value(p, busini, bufnum, tpos, lp, rate, item);
							p = p * amp * (1 + (contr * 3));
							p = DelayC.ar(p, 0.2, rd/1640.0 * dopamnt);

							localReverbFunc[rev_type, 1].value(lrevRef, p[0], wir, rad * llev, room, damp);
							// local reverberation

							p = HOATransRotateAz.ar(ord, lrevRef.value * revCut + (p * cut), rotAngle);
							p = HOABeamDirac2Hoa.ar(ord, p, az, elev, timer_manual:1, focus:pushang);

							outPutFuncs[0].value(p, p,
								globallev.clip(0, 1) * glev);
						});

						if (item > 16) {
							hoaSynth.load(server);
						} { hoaSynth.send(server);
						};

					};
				};

			};
		};
	}

}