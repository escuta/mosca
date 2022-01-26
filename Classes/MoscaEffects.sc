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

MoscaEffects {
	classvar <defs;
	var <effectList, <effectInstances;
	var <gBfBus, <gBxBus, afmtBus, <transformGrp;
	var globalFx, b2Fx;
	var encodeFunc, decodeFunc, busChans, fxChans;
	var <ossiaGlobal, <ossiaDelay, <ossiaDecay;

	*initClass
	{
		defs = Array.newFrom(EffectDef.subclasses);
	}

	*new { | irBank | ^super.new().ctr(irBank) }

	ctr
	{ | irBank |

		effectInstances = Ref(IdentityDictionary());

		if (irBank.isNil)
		{
			defs.removeAt(defs.detectIndex({ | item |
				item == ConvolutionDef.asClass }));

			effectList = defs.collect({ | item | item.key });
		} {
			effectList = defs;

			effectList.removeAt(defs.detectIndex({ | item |
				item == ConvolutionDef.asClass }));

			PathName(irBank).entries.do({ | ir |
				if (ir.extension == "amb") {
					effectList ++ ir.fileNameWithoutExtension;
				}
			});
		};
	}

	setup
	{ | server, sourceGroup, multyThread, maxOrder, renderer |

		busChans = MoscaUtils.fourOrNine(maxOrder);
		fxChans = MoscaUtils.fourOrTwelve(maxOrder);

		gBfBus = Bus.audio(server, busChans); // global b-format bus
		gBxBus = Bus.audio(server, busChans); // global n3d b-format bus
		afmtBus = Bus.audio(server, fxChans); // global a-format bus
		transformGrp = ParGroup.after(sourceGroup);

		if (multyThread)
		{
		} {
			if (maxOrder == 1)
			{
				defs.do({ | def |
					effectInstances.get.put(def.key.asSymbol, def.new1stOrder());
				});

				decodeFunc = {
					var sigf = In.ar(gBfBus, 4);
					var sigx = In.ar(gBxBus, 4);
					sigx = FoaEncode.ar(sigx, MoscaUtils.n2f());
					sigf = sigf + sigx;
					FoaDecode.ar(sigf, MoscaUtils.b2a());
				};

				if (renderer.format != \FUMA)
				{
					var enc = MoscaUtils.foa_n3d_encoder();

					encodeFunc = { | sig |
						var convsig = sig * enc;
						Out.ar(renderer.n3dBus, convsig);
					};
				} {
					encodeFunc = { | sig |
						var convsig = FoaEncode.ar(sig, MoscaUtils.a2b());
						Out.ar(renderer.fumaBus, convsig);
					};
				};
			} {
				defs.do({ | def |
					effectInstances.get.put(def.key.asSymbol, def.new2ndOrder());
				});

				decodeFunc = {
					var sigf = In.ar(gBfBus, 9);
					var sigx = In.ar(gBxBus, 9);
					sigx = HOAConvert.ar(2, sigx, \FuMa, \ACN_N3D);
					sigf = sigf + sigx;
					AtkMatrixMix.ar(sigf, MoscaUtils.soa_a12_decoder_matrix());
				};

				if (renderer.format != \FUMA)
				{
					var enc = MoscaUtils.soa_n3d_encoder();

					encodeFunc = { | sig |
						sig = (sig * enc).sum;
						Out.ar(renderer.n3dBus, sig);
					};
				} {
					encodeFunc = { | sig |
						var convsig = AtkMatrixMix.ar(sig,
							MoscaUtils.soa_a12_encoder_matrix());
						Out.ar(renderer.fumaBus, convsig);
					};
				};
			};
		};
	}

	finalise
	{ | multyThread, server, maxOrder, irBank |

		SynthDef(\b2Fx, {
			var sig = decodeFunc.value();
			Out.ar(afmtBus, sig);
		}).send(server);

		if (multyThread) {

		} {
			effectInstances.get.do({ | effect |

				if (effect != ClearDef.asClass)
				{
					var name = \globalFx ++ effect.key;

					if (effect.needsReCompile(name, maxOrder))
					{
						SynthDef(name, { | gate = 1 |
							var sig = In.ar(afmtBus, fxChans);
							sig = sig * EnvGen.kr(Env.asr(curve:\hold), gate, doneAction:2);
							sig = SynthDef.wrap(effect.globalFunc, prependArgs: [ sig ]);
							encodeFunc.value(sig);
						}, metadata: effect.getMetadata(maxOrder)
						).store(mdPlugin: TextArchiveMDPlugin);

						postln("Compiling" + name + "SynthDef");
					};
				}
			});
		};

		server.sync;

		if (irBank.notNil)
		{
			effectInstances.get.removeAt(\Conv);

			if (maxOrder == 1)
			{
				PathName(irBank).entries.do({ | ir |
					if (ir.extension == "amb") {
						effectInstances.get.put(ir.fileNameWithoutExtension.asSymbol,
							IrDef(server, ir));
					}
				})
			} {
				PathName(irBank).entries.do({ | ir |
					if (ir.extension == "amb") {
						effectInstances.get.put(ir.fileNameWithoutExtension.asSymbol,
							Ir12chanDef(server, ir));
					}
				})
			}
		};

		postln("Done with effects");
	}

	setParam
	{ | ossiaParent, allCritical |

		ossiaGlobal = OssiaAutomationProxy(ossiaParent, "Global_effect", String,
			[nil, nil, effectList], "Clear", critical:true, repetition_filter:true);

		ossiaGlobal.node.description_(effectList.asString);

		ossiaDelay = OssiaAutomationProxy(ossiaGlobal.node, "Room_delay", Float,
			[0, 1], 0.5, 'clip', critical:allCritical, repetition_filter:true);

		ossiaDecay = OssiaAutomationProxy(ossiaGlobal.node, "Damp_decay", Float,
			[0, 1], 0.5, 'clip', critical:allCritical, repetition_filter:true);
	}

	setAction
	{
		ossiaGlobal.action_({ | num |

			this.changed(\ctl);

			if (globalFx.isPlaying) { globalFx.set(\gate, 0) };

			if (num.value != "Clear")
			{
				var instance = effectInstances.get.at(ossiaGlobal.value.asSymbol),
				args = instance.getGlobalArgs(ossiaGlobal.node);

				// deals with converting and encoding global fx busses
				if (b2Fx.isNil)
				{
					b2Fx = Synth(\b2Fx, target: transformGrp, addAction: \addBefore)
					.onFree({ b2Fx = nil });
				};

				globalFx = Synth(\globalFx ++ instance.key,
					[\gate, 1] ++ args, transformGrp).register.onFree(
					{
						if (globalFx.isPlaying.not) { b2Fx.free };
					}
				);
			};
		});

		ossiaDelay.action_({ | num |
			if (globalFx.isPlaying) { globalFx.set(\room, num.value) };
		});

		ossiaDecay.action_({ | num | if (globalFx.isPlaying) { globalFx.set(\damp, num.value) } });
	}

	dockTo
	{ | automation |

		ossiaGlobal.dockTo(automation, "globProxy");
		ossiaDelay.dockTo(automation, "globDelProxy");
		ossiaDecay.dockTo(automation, "globDecProxy");
	}
}
