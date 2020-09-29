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

Mosca {
	var projDir, dur, server, <ossiaParent; // initial rguments
	var renderer, effects, center, sources;

	*new { | projDir, nsources = 10, dur = 180, rirBank, server, parentOssiaNode,
		allCrtitical = false, decoder, maxorder = 1, speaker_array, outbus = 0,
		suboutbus, rawformat = \FUMA, rawoutbus, autoloop = false |

		^super.newCopyArgs(projDir, dur, server).initMosca(nsources, rirBank, parentOssiaNode,
			allCrtitical, decoder, maxorder, speaker_array, outbus, suboutbus, rawformat,
			rawoutbus, autoloop);
	}

	initMosca { | nsources, rirBank, parentOssiaNode, allCrtitical, decoder, maxOrder,
		speaker_array, outBus, subOutBus, rawFormat, rawOutBus, autoloop |

		var multyThread;

		if (server.isNil) { server = Server.local; };

		multyThread = Server.program.asString.endsWith("supernova");

		if (parentOssiaNode.isNil) {
			ossiaParent = OSSIA_Device("Mosca");
		} {
			ossiaParent = OSSIA_Node(parentOssiaNode, "Mosca");
		};

		server.doWhenBooted({

			try({
				var spatList = SpatDef.defList.collect({ | item | item.defName; });
				var fxList = EffectDef.defList.collect({ | item | item.defName; });

				renderer = MoscaRenderer(server, speaker_array, maxOrder, decoder,
					outBus, rawOutBus, rawFormat);

				effects = MoscaEffects(server, maxOrder, multyThread, renderer, rirBank);

				center = OssiaAutomationCenter(ossiaParent, allCrtitical);

				sources = Array.fill(nsources,
					{ | i | MoscaSource(i, server, ossiaParent, allCrtitical, spatList, fxList, center); };
				);

				center.setActions(sources);
			},
			{ | error | ^Error(error).throw; });

			renderer.launchRenderer(server, server.defaultGroup);
		},
		{ ^Error("server not booted").throw; }
		);
	}
}