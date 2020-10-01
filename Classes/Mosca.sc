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
	var renderer, effects, center, sources;
	var ossiaMasterPlay, ossiaMasterLib;

	*new { | projDir, nsources = 10, dur = 180, irBank, server, parentOssiaNode,
		allCrtitical = false, decoder, maxorder = 1, speaker_array, outbus = 0,
		suboutbus, rawformat = \FUMA, rawoutbus, autoloop = false |

		^super.newCopyArgs(projDir, dur, server).ctr(nsources, irBank, parentOssiaNode,
			allCrtitical, decoder, maxorder, speaker_array, outbus, suboutbus, rawformat,
			rawoutbus, autoloop);
	}

	ctr { | nsources, irBank, parentOssiaNode, allCrtitical, decoder, maxOrder,
		speaker_array, outBus, subOutBus, rawFormat, rawOutBus, autoloop |

		var multyThread = Server.program.asString.endsWith("supernova");

		if (server.isNil) { server = Server.local; };

		if (parentOssiaNode.isNil) {
			ossiaParent = OSSIA_Device("Mosca");
		} {
			ossiaParent = OSSIA_Node(parentOssiaNode, "Mosca");
		};

		server.doWhenBooted({

			try({
				var spatList = SpatDef.defList.collect({ | item | item.key; });

				ossiaMasterPlay = OSSIA_Parameter(ossiaParent, "Audition_all", Boolean,
					critical:true);

				ossiaMasterLib = OSSIA_Parameter(ossiaParent, "Library_all", String,
					[nil, nil, spatList], spatList.first, critical:true, repetition_filter:true);

				ossiaMasterLib.description_(spatList.asString);

				renderer = MoscaRenderer(server, speaker_array, maxOrder, decoder,
					outBus, rawOutBus, rawFormat);

				renderer.setMasterControl(ossiaParent, allCrtitical);

				effects = MoscaEffects(server, maxOrder, multyThread, renderer, irBank);

				center = OssiaAutomationCenter(ossiaParent, allCrtitical);

				sources = Array.fill(nsources,
					{ | i | MoscaSource(i, server, ossiaParent, allCrtitical, spatList, effects, center); };
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