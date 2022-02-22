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

MoscaSpatializer
{
	const playList = #["File","Stream","SCBus","EXBus"],
	// the diferent types of inputs for spatilizer synths
	minRadRoot = 0.076923076923077;

	var <spatInstances;

	classvar playInFunc,
	<defs, <spatList; // list of spat libs
	// class access

	// instance access
	spatList { ^spatList; }
	defs { ^defs; }

	*initClass
	{
		Class.initClassTree(SpatDef);

		defs = SpatDef.defList;
		spatList = defs.collect({ | item | item.key; });

		playInFunc = [ // one for File, Stream & Input;
			// for File-in SynthDefs
			{ | p, channum, bufnum, tpos = 0, lp = 0 |
				p.value = PlayBuf.ar(channum, bufnum, BufRateScale.kr(channum),
					startPos: tpos, loop: lp, doneAction:2);
			},
			// for Stream-in SynthDefs
			{ | p, channum, bufnum, lp = 0 |
				var trig;
				p.value = DiskIn.ar(channum, bufnum, lp);
				trig = Done.kr(p.value);
				FreeSelf.kr(trig);
			},
			// for SCBus-in SynthDefs
			{ | p, channum, busini |
				p.value = In.ar(busini, channum);
		}]; // Note, all variables are needed
	}

	*new { | server | ^super.new.ctr(server); }

	ctr
	{ | server |

		spatInstances = Ref(IdentityDictionary());

		defs.do({ | def |
			spatInstances.get.put(def.key.asSymbol, def.new());
		});

		// Make EXBus-in SynthDefs, seperate from the init class because it needs the server informaions
		playInFunc = playInFunc.add({ | p, channum, busini |
			p.value = In.ar(busini + server.inputBus.index - 1, channum);
		});
	}

	makeSpatialisers
	{ | server, maxOrder, effects, renderer, speaker_array |

		var plim = MoscaUtils.plim, name, metadata;

		spatInstances.get.do({ | def |
			def.setup(maxOrder, effects, renderer, server);
		});

		effects.effectInstances.get.do({ | effect |

			spatInstances.get.do({ | spat |

				playList.do({ | play_type, j |

					spat.channels.do({ | channels |

						name = spat.key ++ play_type ++ channels ++ effect.key;

						// if (spat.needsReCompile(name, maxOrder, speaker_array))
						// {
							SynthDef(name, {
								| outBus = #[0, 0], radAzimElev = #[10, 0, 0],
								amp = 1, dopamnt = 0, glev = 0 |

								var rad = Lag.kr(radAzimElev[0]),
								radRoot = rad.sqrt.clip(minRadRoot, 1),
								lrevRef = Ref(0),
								azimuth = radAzimElev[1] - MoscaUtils.halfPi,
								elevation = radAzimElev[2],
								revCut = rad.lincurve(1, plim, 1, 0),
								channum = channels,
								p = Ref(0);

								SynthDef.wrap(playInFunc[j], prependArgs: [ p, channum ]);

								p.value = DelayC.ar(p.value, 0.2, (rad * 340)/1640.0 * dopamnt); // Doppler

								// local effect
								SynthDef.wrap(effect.getFunc(channum), prependArgs: [ lrevRef, p, rad ]);

								lrevRef.value = lrevRef.value * revCut;
								p.value = p.value * amp;

								SynthDef.wrap(spat.getFunc(maxOrder, renderer, channum),
									prependArgs: [ lrevRef, p, rad, radRoot, azimuth, elevation ]);

								spat.fxOutFunc.value(p.value, lrevRef.value,
									(1 - radRoot) * glev, outBus[0]);

								Out.ar(outBus[1], lrevRef.value);
							}, metadata: spat.getMetadata(maxOrder, speaker_array)
							).store(mdPlugin: TextArchiveMDPlugin);

						postln("Compiling" + name + "SynthDef ("++ spat.format ++")");
						// }
					});
				})
			});
			server.sync;
		});
	}
}
