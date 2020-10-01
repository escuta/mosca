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
	var multyThread, <defs, <irs;
	var <gBfBus, <gBxBus, glbFxGrp, globalFx;
	var encodeFunc, decodeFunc;
	var ossiaGlobal, ossiaDel, ossiaDec;

	*new { | server, maxOrder, multyThread, renderer, irBank |

		^super.newCopyArgs(multyThread).ctr(server, maxOrder, renderer, irBank);
	}

	ctr { | server, maxOrder, renderer, irBank |
		var busChans = MoscaUtils.fourOrNine(maxOrder);

		gBfBus = Bus.audio(server, busChans); // global b-format bus
		gBxBus = Bus.audio(server, busChans); // global n3d b-format bus
		glbFxGrp = ParGroup.tail(server.defaultGroup);

		defs = Array.newFrom(EffectDef.defList);

		if (irBank.notNil) {
			this.prLoadir(server, maxOrder, irBank);
			defs = defs.add(ConVerbDef);
		};

		if (multyThread) {
		} {
			if (maxOrder == 1) {

				decodeFunc = {
					var sigf = In.ar(gBfBus, 4);
					var sigx = In.ar(gBxBus, 4);
					sigx = FoaEncode.ar(sigx, MoscaUtils.n2f());
					sigf = sigf + sigx;
					^sigf = FoaDecode.ar(sigf, MoscaUtils.b2a());
				};

				if (renderer.convertFuma) {
					var enc = MoscaUtils.foa_n3d_encoder();

					encodeFunc = { | sig, gate |
						var convsig = sig * enc;
						convsig = convsig * EnvGen.kr(Env.asr(1), gate, doneAction:2);
						Out.ar(renderer.n3dBus, convsig);
					};
				} {
					encodeFunc = { | sig, gate |
						var convsig = FoaEncode.ar(sig, MoscaUtils.a2b());
						convsig = convsig * EnvGen.kr(Env.asr(1), gate, doneAction:2);
						Out.ar(renderer.fumaBus, convsig);
					};
				};
			} {
				decodeFunc = {
					var sigf = In.ar(gBfBus, 9);
					var sigx = In.ar(gBxBus, 9);
					sigx = HOAConvert.ar(2, sigx, \FuMa, \ACN_N3D);
					sigf = sigf + sigx;
					^sigf = AtkMatrixMix.ar(sigf, MoscaUtils.soa_a12_decoder_matrix());
				};

				if (renderer.convertFuma) {
					var enc = MoscaUtils.soa_n3d_encoder();

					encodeFunc = { | sig, gate |
						var convsig = sig * enc;
						convsig = convsig * EnvGen.kr(Env.asr(1), gate, doneAction:2);
						Out.ar(renderer.n3dBus, convsig);
					};
				} {
					encodeFunc = { | sig, gate |
						var convsig = AtkMatrixMix.ar(sig,
							MoscaUtils.soa_a12_encoder_matrix());
						convsig = convsig * EnvGen.kr(Env.asr(1), gate, doneAction:2);
						Out.ar(renderer.fumaBus, convsig);
					};
				}
			};
		};
	}

	setGlobalControl { | ossiaParent, allCritical, automation, fxList |

		ossiaGlobal = OssiaAutomationProxy(ossiaParent, "Global_effect", String,
			[nil, nil, fxList], "Clear", critical:true, repetition_filter:true);

		ossiaGlobal.param.description_(fxList.asString);

		ossiaGlobal.action_({ | num |

			if (globalFx.isPlaying) { globalFx.set(\gate, 0) };

			if (num.value != "Clear") {

				globalFx = Synth(\globalFx_ ++ num.value,
					[\gate, 1, \room, ossiaDel.value, \damp, ossiaDec.value],
					glbFxGrp).register;
			};
		});

		ossiaDel = OssiaAutomationProxy(ossiaGlobal, "Room_delay", Float,
			[0, 1], 0.5, 'clip', critical:allCritical, repetition_filter:true);

		ossiaDel.action_({ | num | globalFx.set(\room, num.value); });

		ossiaDec = OssiaAutomationProxy(ossiaGlobal, "Damp_decay", Float,
			[0, 1], 0.5, 'clip', critical:allCritical, repetition_filter:true);

		ossiaDec.action_({ | num |	globalFx.set(\damp, num.value); });
	}

	prLoadIr { | server, maxOrder, irBank | // prepare list of impulse responses for local and global reverb

		PathName(irBank).entries.do({ | ir | irs = irs.add(IrDef(server, ir)); });
	}

	sendReverbs { | multyThread, server |

		if (multyThread) {
		} {
			defs.do({ | item |

				SynthDef(\globalFx_ ++ item.defName, { | gate = 1, room = 0.5, damp = 0.5,
					a0ir, a1ir, a2ir, a3ir, a4ir, a5ir, a6ir, a7ir, a8ir, a9ir, a10ir, a11ir |
					var sig = decodeFunc.vaue();
					sig = _.globalRevFunc.value(sig, room, damp,a0ir, a1ir, a2ir, a3ir, a4ir,
						a5ir, a6ir, a7ir, a8ir, a9ir, a10ir, a11ir);
					encodeFunc.value(sig, gate);
				}).send(server);
			});
		};
	}
}