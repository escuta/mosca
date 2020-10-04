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

Mosca {
	var projDir, dur, server, <ossiaParent; // initial rguments
	var renderer, effects, center, sources, <control;
	var ossiaMasterPlay, ossiaMasterLib;

	*new { | projDir, nsources = 10, dur = 180, irBank, server, parentOssiaNode,
		allCritical = false, decoder, maxorder = 1, speaker_array, outbus = 0,
		suboutbus, rawformat = \FUMA, rawoutbus, autoloop = false |

		^super.newCopyArgs(projDir, dur, server).ctr(nsources, irBank, parentOssiaNode,
			allCritical, decoder, maxorder, speaker_array, outbus, suboutbus, rawformat,
			rawoutbus, autoloop);
	}

	ctr { | nsources, irBank, parentOssiaNode, allCritical, decoder, maxOrder,
		speaker_array, outBus, subOutBus, rawFormat, rawOutBus, autoloop |

		var multyThread = false;
		// Server.program.asString.endsWith("supernova");

		if (server.isNil) { server = Server.local; };

		if (parentOssiaNode.isNil) {
			ossiaParent = OSSIA_Device("Mosca");
		} {
			ossiaParent = OSSIA_Node(parentOssiaNode, "Mosca");
		};

		control = Automation(dur, showLoadSave: false, showSnapshot: true,
			minTimeStep: 0.001);

		server.doWhenBooted({

			var spatList = SpatDef.defList.collect({ | item | item.key; });

			ossiaMasterPlay = OSSIA_Parameter(ossiaParent, "Audition_all", Boolean,
				critical:true);

			ossiaMasterLib = OSSIA_Parameter(ossiaParent, "Library_all", String,
				[nil, nil, spatList], "Ambitools", critical:true, repetition_filter:true);

			ossiaMasterLib.description_(spatList.asString);

			renderer = MoscaRenderer(server, speaker_array, maxOrder, decoder, outBus,
				subOutBus, rawOutBus, rawFormat, ossiaParent, allCritical, control);

			effects = MoscaEffects(server, maxOrder, multyThread, renderer, irBank, ossiaParent, allCritical, control);

			center = OssiaAutomationCenter(ossiaParent, allCritical);

			sources = Array.fill(nsources,
				{ | i | MoscaSource(i, server, ossiaParent, allCritical, spatList, effects, center); };
			);

			center.setActions(sources);

			effects.sendReverbs(multyThread, server);

			renderer.launchRenderer(server, server.defaultGroup);
		},
		{ ^Error("server not booted").throw; }
		);
	}
}