/*
* Mosca: SuperCollider class by Iain Mott, 2016 and Thibaud Keller, 2018. Licensed under a
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
	const playList = #["File","Stream","SCBus","EXBus"],
	// the diferent types of inputs for spatilizer synths
	minRadRoot = 0.076923076923077;

	classvar playInFunc, <defs, <spatList;
	// list of spat libs

	var outPutFuncs, <spatInstances;

	*initClass {

		Class.initClassTree(SpatDef);

		defs = SpatDef.defList;
		spatList = defs.collect({ | item | item.key; });

		playInFunc = [ // one for File, Stream & Input;
			// for File-in SynthDefs
			{ | p, bufnum, tpos, lp = 0, rate, channum |
				var scaledRate = rate * BufRateScale.kr(bufnum);
				p.value = PlayBuf.ar(channum, bufnum, scaledRate, startPos: tpos,
					loop: lp, doneAction:2);
			},
			// for Stream-in SynthDefs
			{ | p, bufnum, lp = 0, channum |
				var trig;
				p.value = DiskIn.ar(channum, bufnum, lp);
				trig = Done.kr(p.value);
				FreeSelf.kr(trig);
			},
			// for SCBus-in SynthDefs
			{ | p, busini, channum |
				p.value = In.ar(busini, channum);
		}]; // Note, all variables are needed
	}

	spatList { ^spatList; }

	defs { ^defs; }

	*new { | server |

		^super.new.ctr(server);
	}

	ctr { | server |

		spatInstances = Ref();

		// Make EXBus-in SynthDefs, seperate from the init class because it needs the server informaions
		playInFunc = playInFunc.add({ | playerRef, busini, channum |
			playerRef.value = In.ar(busini + server.inputBus.index, channum);
		});

		spatInstances.set(SpatDef.defList.collect({ | def | def.new() }));
	}

	makeSpatialisers { | server, maxOrder, effect, renderer |

		outPutFuncs = IdentityDictionary(3);
		outPutFuncs.put(\N3D, // contains the synthDef blocks for each spatialyers
			{ | dry, wet, globFx, fxBus, outBus |
				Out.ar(fxBus, wet * globFx); // effect.gBxBus
				Out.ar(outBus, wet); // renderer.n3dBus
			},
			\FUMA,
			{ | dry, wet, globFx, fxBus, outBus |
				Out.ar(fxBus, wet * globFx); // effect.gBfBus
				Out.ar(outBus, wet); // renderer.fumaBus
			},
			\NONAMBI,
			{ | dry, wet, globFx, fxBus, outBus |
				Out.ar(fxBus, dry * globFx); // effect.gBfBus
				Out.ar(outBus, wet); // renderer.nonAmbiBus
		});

		effect.defs.do({ | effect, count |
			var plim = MoscaUtils.plim, out_type = 0, name, metadata;

			spatInstances.get.do({ | spat |

/*				if (spat.format != \NONAMBI)
				{
					metadata = (setup: renderer.)*/

				playList.do({ | play_type, j |

					spat.channels.do({ | channels |

						name = spat.key ++ play_type ++ channels ++ effect.key;

						SynthDef(name, {
						| radAzimElev = #[10, 0, 0], amp = 1, dopamnt = 0,
							glev = 0, llev = 0, outBus = #[0, 0] |

							var rad = Lag.kr(radAzimElev[0]),
							radRoot = rad.sqrt.clip(minRadRoot, 1),
							lrevRef = Ref(0),
							azimuth = radAzimElev[1] - MoscaUtils.halfPi,
							elevation = radAzimElev[2],
							revCut = rad.lincurve(1, plim, 1, 0),
							locallev = rad * llev,
							channum = channels,
							p = Ref(0);

							SynthDef.wrap(playInFunc[j], prependArgs: [ p, channum ]);

							p = DelayC.ar(p, 0.2, (rad * 340)/1640.0 * dopamnt); // Doppler

							SynthDef.wrap(effect.getFunc(channum), prependArgs: [ lrevRef, p ]);
							// local reverberation

							lrevRef.value = lrevRef.value * revCut;

							p = p * amp;

							SynthDef.wrap(spat.getFunc(channum),
								prependArgs: [ lrevRef, p, rad, radRoot, azimuth, elevation ]);

							outPutFuncs.at(spat.format).value(p, lrevRef.value,
								(1 - radRoot) * glev, outBus[0], outBus[1]);
						}

						).load(server);

/*					name = spat.key ++ play_type ++ 2 ++ effect.key;

					SynthDef(name, {
						| bufnum = 0, rate = 1, tpos = 0, lp = 0, busini,
						radAzimElev = #[20, 0, 0], amp = 1,
						dopamnt = 0, glev = 0, llev = 0, angle = 1.05,
						insertFlag = 0, insertOut, insertBack, outBus = #[0, 0],
						room = 0.5, damp = 05, wir, vbapBuffer = 0,
						contr = 1, grainrate = 10, winsize = 0.1, winrand = 0 |

						var rad = Lag.kr(radAzimElev[0]),
						radRoot = rad.sqrt.clip(minRadRoot, 1),
						lrev1Ref = Ref(0),
						lrev2Ref = Ref(0),
						az = Lag.kr(radAzimElev[1] - halfPi),
						revCut = rad.lincurve(1, plim, 1, 0),
						p = Ref(0);

						playInFunc[j].value(p, busini, bufnum, tpos, lp, rate, 2);
						p = DelayC.ar(p, 0.2, (rad * 340)/1640.0 * dopamnt);

						effect.localStereoFunc.value(lrev1Ref, lrev2Ref, p[0], p[1],
							wir, rad * llev, room, damp); // local reverberation

						lrev1Ref.value = lrev1Ref.value * revCut;
						lrev2Ref.value = lrev2Ref.value * revCut;
						p = p * amp;

						spat.spatFunc.value(lrev1Ref, p[0], rad, radRoot, az + (angle * (1 - rad)),
							radAzimElev[2],  contr, winsize, grainrate, winrand, vbapBuffer);
						spat.spatFunc.value(lrev2Ref, p[1], rad, radRoot, az - (angle * (1 - rad)),
							radAzimElev[2],  contr, winsize, grainrate, winrand, vbapBuffer);

						outPutFuncs.at(spat.format).value(Mix.ar(p) * 0.5,
							(lrev1Ref.value + lrev2Ref.value) * 0.5,
							(1 - radRoot) * glev, outBus[0], outBus[1]);
					}).load(server);

					if (spat.key == "ATK") {

						// assume FuMa input
						name  = "ATK" ++ play_type ++ 4 ++ effect.key;

						SynthDef(name, {
							| bufnum = 0, rate = 1, tpos = 0, lp = 0, busini,
							radAzimElev = #[10, 0, 0], amp = 1,
							dopamnt = 0, glev = 0, llev = 0,
							insertFlag = 0, insertOut, insertBack,
							room = 0.5, damp = 05, wir, outBus = #[0, 0],
							contr = 0, directang = 1, rotAngle = 0 |

							var rad = Lag.kr(radAzimElev[0]),
							radRoot = rad.sqrt.clip(minRadRoot, 1),
							lrevRef = Ref(0),
							az = radAzimElev[1] - halfPi,
							revCut = rad.lincurve(1, plim, 1, 0),
							p = Ref(0),
							pushang = 2 - (contr * 2);
							pushang = rad.linlin(pushang - 1, pushang, 0, halfPi);
							// degree of sound field displacement

							playInFunc[j].value(p, busini, bufnum, tpos, lp, rate, 4);

							p = DelayC.ar(p, 0.2, (rad * 340)/1640.0 * dopamnt); // Doppler
							effect.localMonoFunc.value(lrevRef, p[0], wir, rad * llev, room, damp);
							// local reverberation

							lrevRef.value = lrevRef.value * revCut;
							p = p * amp;

							p = FoaDirectO.ar(p * ((1/radRoot) - 1), directang);
							// directivity
							p = FoaTransform.ar(p, 'rotate', rotAngle);
							p = FoaTransform.ar(p, 'push', pushang, az, radAzimElev[2]);

							outPutFuncs[1].value(p, p, (1 - radRoot) * glev, outBus[0], outBus[1]);
						}).load(server);

						MoscaUtils.hoaChanns.do({ | item, count |
							var ord = (item.sqrt) - 1;

							// assume N3D input
							name = "ATK" ++ play_type ++ item ++ effect.key;

							SynthDef(name, {
								| bufnum = 0, rate = 1, tpos = 0, lp = 0, busini,
								radAzimElev = #[10, 0, 0], amp = 1,
								dopamnt = 0, glev = 0, llev = 0,
								insertFlag = 0, insertOut, insertBack,
								room = 0.5, damp = 05, wir, outBus = #[0, 0],
								contr = 0, directang = 1, rotAngle = 0 |

								var rad = Lag.kr(radAzimElev[0]),
								radRoot = rad.sqrt.clip(minRadRoot, 1),
								lrevRef = Ref(0),
								az = radAzimElev[1] - halfPi,
								revCut = rad.lincurve(1, plim, 1, 0),
								p = Ref(0),
								pushang = 2 - (contr * 2);
								pushang = rad.linlin(pushang - 1, pushang, 0, halfPi);
								// degree of sound field displacement

								playInFunc[j].value(p, busini, bufnum, tpos, lp, rate, item);

								p = DelayC.ar(p, 0.2, (rad * 340)/1640.0 * dopamnt); // Doppler
								effect.localMonoFunc.value(lrevRef, p[0], wir, rad * llev, room, damp);
								// local reverberation

								lrevRef.value = lrevRef.value * revCut;
								p = p * amp;

								p = FoaEncode.ar(p * ((1/radRoot) - 1), MoscaUtils.n2f);
								p = FoaDirectO.ar(p, directang);
								// directivity
								p = FoaTransform.ar(p, 'rotate', rotAngle);
								p = FoaTransform.ar(p, 'push', pushang, az, radAzimElev[2]);

								outPutFuncs[1].value(p, p, (1 - radRoot) * glev, outBus[0], outBus[1]);
							}).load(server);
						});
					};

					if (spat.key == "Ambitools") {

						// assume FuMa input
						name = "Ambitools" ++ play_type ++ 4 ++ effect.key;

						SynthDef(name, {
							| bufnum = 0, rate = 1, tpos = 0, lp = 0, busini,
							radAzimElev = #[10, 0, 0], amp = 1,
							dopamnt = 0, glev = 0, llev = 0,
							insertFlag = 0, insertOut, insertBack,
							room = 0.5, damp = 05, wir, outBus = #[0, 0],
							contr = 0, rotAngle = 0|

							var rad = Lag.kr(radAzimElev[0]),
							radRoot = rad.sqrt.clip(minRadRoot, 1),
							lrevRef = Ref(0),
							az = radAzimElev[1] - halfPi,
							revCut = rad.lincurve(1, plim, 1, 0),
							p = Ref(0),
							pushang = 2 - (contr * 2);
							pushang = rad.linlin(pushang - 1, pushang, 0, halfPi);
							// degree of sound field displacement

							playInFunc[j].value(p, busini, bufnum, tpos, lp, rate, 4);

							p = DelayC.ar(p, 0.2, (rad * 340)/1640.0 * dopamnt); // Doppler
							effect.localMonoFunc.value(lrevRef, p[0], wir, rad * llev, room, damp);
							// local reverberation

							lrevRef.value = lrevRef.value * revCut;
							p = p * amp;

							p = FoaDecode.ar(p * ((1/radRoot) - 1), MoscaUtils.f2n);
							p = HOATransRotateAz.ar(1, p, rotAngle);
							p = HOABeamDirac2Hoa.ar(1, p, az, radAzimElev[2], timer_manual:1, focus:pushang);

							outPutFuncs[0].value(p, p, (1 - radRoot) * glev, outBus[0], outBus[1]);
						}).load(server);

						MoscaUtils.hoaChanns.do({ | item, count |
							var ord = (item.sqrt) - 1;

							// assume N3D input
							name = "Ambitools" ++ play_type ++ item ++ effect.key;

							SynthDef(name, {
								| bufnum = 0, rate = 1, tpos = 0, lp = 0, busini,
								radAzimElev = #[10, 0, 0], amp = 1,
								dopamnt = 0, glev = 0, llev = 0,
								insertFlag = 0, insertOut, insertBack,
								room = 0.5, damp = 05, wir, outBus = #[0, 0],
								contr = 0, rotAngle = 0|

								var rad = Lag.kr(radAzimElev[0]),
								radRoot = rad.sqrt.clip(minRadRoot, 1),
								lrevRef = Ref(0),
								az = radAzimElev[1] - halfPi,
								revCut = rad.lincurve(1, plim, 1, 0),
								p = Ref(0),
								pushang = 2 - (contr * 2);
								pushang = rad.linlin(pushang - 1, pushang, 0, halfPi);
								// degree of sound field displacement

								playInFunc[j].value(p, busini, bufnum, tpos, lp, rate, item);

								p = DelayC.ar(p, 0.2, (rad * 340)/1640.0 * dopamnt); // Doppler
								effect.localMonoFunc.value(lrevRef, p[0], wir, rad * llev, room, damp);
								// local reverberation

								lrevRef.value = lrevRef.value * revCut;
								p = p * amp;

								p = HOATransRotateAz.ar(ord, lrevRef.value * revCut +
									(p * ((1/radRoot) - 1)), rotAngle);
								p = HOABeamDirac2Hoa.ar(ord, p, az, radAzimElev[2], timer_manual:1, focus:pushang);

								outPutFuncs[0].value(p, p, (1 - radRoot) * glev, outBus[0], outBus[1]);
							}).load(server);
						})
						}*/
					})
				})
			})
		})
	}
}
