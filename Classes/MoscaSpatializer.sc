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

MoscaSpatializer {
	const playList = #["File","Stream","SCBus","EXBus"];
	// the diferent types of inputs to spatilizer synths

	classvar playInFunc, defs, <spatList;
	// list of spat libs

	var outPutFuncs, spatInstances;

	*initClass {

		Class.initClassTree(SpatDef);

		defs = SpatDef.defList;
		spatList = defs.collect({ | item | item.key; });

		playInFunc = [ // one for File, Stream & Input;
			// for File-in SynthDefs
			{ | playerRef, busini, bufnum, tpos, lp = 0, rate, channum |
				var spos = tpos * BufSampleRate.kr(bufnum),
				scaledRate = rate * BufRateScale.kr(bufnum);
				playerRef.value = PlayBuf.ar(channum, bufnum, scaledRate, startPos: spos,
					loop: lp, doneAction:2);
			},
			// for Stream-in SynthDefs
			{ | playerRef, busini, bufnum, tpos, lp = 0, rate, channum |
				var trig;
				playerRef.value = DiskIn.ar(channum, bufnum, lp);
				trig = Done.kr(playerRef.value);
				FreeSelf.kr(trig);
			},
			// for SCBus-in SynthDefs
			{ | playerRef, busini, bufnum, tpos, lp = 0, rate, channum |
				playerRef.value = In.ar(busini, channum);
		}]; // Note, all variables are needed
	}

	spatList { ^spatList; }

	defs { ^defs; }

	*new { | server |

		^super.new.ctr(server);
	}

	ctr { | server |

		// Make EXBus-in SynthDefs, seperate from the init class because it needs the server informaions
		playInFunc = playInFunc.add({ | playerRef, busini, bufnum, tpos, lp = 0, rate, channum |
			playerRef.value = In.ar(busini + server.inputBus.index, channum);
		});
	}

	initSpat { | order, renderer, server |

		spatInstances = SpatDef.defList.collect({ | def | def.new(order, renderer, server); });
	}

	makeSpatialisers { | server, maxOrder, renderer, effect |
		var out_type = 0;

		outPutFuncs = [ // contains the synthDef blocks for each spatialyers, 0 = n3d, 1 = fuma, 2 = nonAmbi
			{ | dry, wet, globrev |
				Out.ar(effect.gBxBus, wet * globrev);
				Out.ar(renderer.n3dBus, wet);
			},
			{ | dry, wet, globrev |
				Out.ar(effect.gBfBus, wet * globrev);
				Out.ar(renderer.fumaBus, wet);
			},
			{ | dry, wet, globrev |
				Out.ar(effect.gBfBus, dry * globrev);
				Out.ar(renderer.nonAmbiBus, wet);
		}];

		effect.defs.do({ | effect, count |

			spatInstances.do({ | spat, i |

				case
				{ spat.format == \N3D } { out_type = 0 }
				{ spat.format == \FUMA } { out_type = 1 }
				{ out_type = 2 };

				playList.do({ | play_type, j |
					var mono, stereo;

					mono = SynthDef(spat.key ++ play_type ++ 1 ++ effect.key, {
						| bufnum = 0, rate = 1, tpos = 0, lp = 0, busini,
						azim = 0, elev = 0, radius = 20, amp = 1,
						dopamnt = 0, glev = 0, llev = 0,
						insertFlag = 0, insertOut, insertBack,
						room = 0.5, damp = 05, wir, df, sp,
						contr = 1, grainrate = 10, winsize = 0.1, winrand = 0 |

						var rad = Lag.kr(radius),
						globallev = (1 / rad.sqrt) - 1, //global reverberation
						locallev, lrevRef = Ref(0),
						az = azim - MoscaUtils.halfPi,
						p = Ref(0),
						rd = rad * 340, // Doppler
						revCut = rad.lincurve(1, MoscaUtils.plim, 1, 0),
						cut = rad.linlin(0.75, 1, 1, 0);
						rad = rad.max(0.01);

						playInFunc[j].value(p, busini, bufnum, tpos, lp, rate, 1);
						p = p * amp;
						p = DelayC.ar(p, 0.2, rd/1640.0 * dopamnt);

						effect.localMonoFunc.value(lrevRef, p, wir, rad * llev,
							room, damp); // local reverberation

						lrevRef.value = lrevRef.value * revCut;

						spat.spatFunc.value(lrevRef, p * cut, rad, az, elev, df, sp, contr,
							winsize, grainrate, winrand);

						outPutFuncs[out_type].value(p, lrevRef.value,
							globallev.clip(0, 1) * glev);
					});

					stereo = SynthDef(spat.key ++ play_type ++ 2 ++ effect.key, {
						| bufnum = 0, rate = 1, tpos = 0, lp = 0, busini,
						azim = 0, elev = 0, radius = 20, amp = 1,
						dopamnt = 0, glev = 0, llev = 0, angle = 1.05,
						insertFlag = 0, insertOut, insertBack,
						room = 0.5, damp = 05, wir, df, sp,
						contr = 1, grainrate = 10, winsize = 0.1, winrand = 0 |

						var rad = Lag.kr(radius),
						globallev = (1 / rad.sqrt) - 1, //global reverberation
						lrev1Ref = Ref(0), lrev2Ref = Ref(0),
						az = Lag.kr(azim - MoscaUtils.halfPi),
						p = Ref(0),
						rd = rad * 340, // Doppler
						revCut = rad.lincurve(1, MoscaUtils.plim, 1, 0),
						cut = rad.linlin(0.75, 1, 1, 0);
						rad = rad.max(0.01);

						playInFunc[j].value(p, busini, bufnum, tpos, lp, rate, 2);
						p = p * amp;
						p = DelayC.ar(p, 0.2, rd/1640.0 * dopamnt);

						effect.localStereoFunc.value(lrev1Ref, lrev2Ref, p[0], p[1],
							wir, rad * llev, room, damp); // local reverberation

						lrev1Ref.value = lrev1Ref.value * revCut;
						lrev2Ref.value = lrev2Ref.value * revCut;

						p = p * cut;

						spat.spatFunc.value(lrev1Ref, p[0], rad, az - (angle * (1 - rad)),
							elev, df, sp, contr, winsize, grainrate, winrand);
						spat.spatFunc.value(lrev2Ref, p[1], rad, az + (angle * (1 - rad)),
							elev, df, sp, contr, winsize, grainrate, winrand);

						outPutFuncs[out_type].value(Mix.ar(p) * 0.5,
							(lrev1Ref.value + lrev2Ref.value) * 0.5,
							globallev.clip(0, 1) * glev);
					});

					if (maxOrder < 3) {
						mono.send(server);
						stereo.send(server);
					} {
						if (maxOrder == 3) {
							mono.send(server);
							stereo.load(server);
						} {
							mono.load(server);
							stereo.load(server);
						};
					};

					if (spat.key == \ATK) {

						// assume FuMa input
						SynthDef(\ATKBFormat ++ play_type ++ 4 ++ effect.key, {
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
							az = azim - MoscaUtils.halfPi,
							p = Ref(0),
							rd = rad * 340, // Doppler
							revCut = rad.lincurve(1, MoscaUtils.plim, 1, 0),
							cut = rad.linlin(0.75, 1, 1, 0);
							rad = rad.max(0.01);
							pushang = radius.linlin(pushang - 1, pushang, 0, MoscaUtils.halfPi); // degree of sound field displacement


							playInFunc[j].value(p, busini, bufnum, tpos, lp, rate, 4);
							p = p * amp;
							p = DelayC.ar(p, 0.2, rd/1640.0 * dopamnt);

							effect.localMonoFunc.value(lrevRef, p[0], wir, rad * llev, room, damp);
							// local reverberation

							p = FoaDirectO.ar((lrevRef.value * revCut) + (p * cut), directang);
							// directivity
							p = FoaTransform.ar(p, 'rotate', rotAngle);
							p = FoaTransform.ar(p, 'push', pushang, az, elev);

							outPutFuncs[1].value(p, p, globallev.clip(0, 1) * glev);
						}).send(server);

						[9, 16, 25].do({ | item, count |
							var ord = (item.sqrt) - 1,

							// assume N3D input
							hoaSynth = SynthDef(\ATKBFormat ++ play_type ++ item ++ effect.key, {
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
								az = azim - MoscaUtils.halfPi,
								p = Ref(0),
								rd = rad * 340, // Doppler
								revCut = rad.lincurve(1, MoscaUtils.plim, 1, 0),
								cut = rad.linlin(0.75, 1, 1, 0);
								rad = rad.max(0.01);
								pushang = radius.linlin(pushang - 1, pushang, 0, MoscaUtils.halfPi); // degree of sound field displacement

								playInFunc[j].value(p, busini, bufnum, tpos, lp, rate, 4);
								p = p * amp;
								p = DelayC.ar(p, 0.2, rd/1640.0 * dopamnt);

								effect.localMonoFunc.value(lrevRef, p[0], wir, rad * llev, room, damp);
								// local reverberation

								p = FoaEncode.ar((lrevRef.value * revCut) + (p * cut), MoscaUtils.n2f);
								p = FoaDirectO.ar(p, directang);
								// directivity
								p = FoaTransform.ar(p, 'rotate', rotAngle);
								p = FoaTransform.ar(p, 'push', pushang, az, elev);

								outPutFuncs[1].value(p, p, globallev.clip(0, 1) * glev);
							});

							if (item > 16) {
								hoaSynth.load(server);
							} {
								hoaSynth.send(server);
							};
						});
					};

					if (spat.key == \Ambitools) {

						// assume FuMa input
						SynthDef(\AmbitoolsBFormat ++ play_type ++ 4 ++ effect.key, {
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
							az = azim - MoscaUtils.halfPi,
							p = Ref(0),
							rd = rad * 340, // Doppler
							revCut = rad.lincurve(1, MoscaUtils.plim, 1, 0),
							cut = rad.linlin(0.75, 1, 1, 0);
							rad = rad.max(0.01);
							pushang = radius.linlin(pushang - 1, pushang, 0, 1); // degree of sound field displacement


							playInFunc[j].value(p, busini, bufnum, tpos, lp, rate, 4);
							p = p * amp * (1 + (contr * 3));
							p = DelayC.ar(p, 0.2, rd/1640.0 * dopamnt);

							effect.localMonoFunc.value(lrevRef, p[0], wir, rad * llev, room, damp);
							// local reverberation

							p = FoaDecode.ar((lrevRef.value * revCut) + (p * cut), MoscaUtils.f2n);
							p = HOATransRotateAz.ar(1, p, rotAngle);
							p = HOABeamDirac2Hoa.ar(1, p, az, elev, timer_manual:1, focus:pushang);

							outPutFuncs[0].value(p, p,
								globallev.clip(0, 1) * glev);
						}).send(server);

						[9, 16, 25].do({ | item, count |
							var ord = (item.sqrt) - 1,

							// assume N3D input
							hoaSynth = SynthDef(\AmbitoolsBFormat ++ play_type ++ item ++ effect.key, {
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
								az = azim - MoscaUtils.halfPi,
								p = Ref(0),
								rd = rad * 340, // Doppler
								revCut = rad.lincurve(1, MoscaUtils.plim, 1, 0),
								cut = rad.linlin(0.75, 1, 1, 0);
								rad = rad.max(0.01);
								pushang = radius.linlin(pushang - 1, pushang, 0, 1); // degree of sound field displacement

								playInFunc[j].value(p, busini, bufnum, tpos, lp, rate, item);
								p = p * amp * (1 + (contr * 3));
								p = DelayC.ar(p, 0.2, rd/1640.0 * dopamnt);

								effect.localMonoFunc.value(lrevRef, p[0], wir, rad * llev, room, damp);
								// local reverberation

								p = HOATransRotateAz.ar(ord, lrevRef.value * revCut + (p * cut), rotAngle);
								p = HOABeamDirac2Hoa.ar(ord, p, az, elev, timer_manual:1, focus:pushang);

								outPutFuncs[0].value(p, p, globallev.clip(0, 1) * glev);
							});

							if (item > 16) {
								hoaSynth.load(server);
							} {
								hoaSynth.send(server);
							};
						});
					};
				});
			});
		});
	}
}
