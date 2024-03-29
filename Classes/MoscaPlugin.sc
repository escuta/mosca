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

MoscaPlugin
{
	getMetadata
	{ | maxOrder, speaker_array = nil |
		^(order: maxOrder);
	}

	needsReCompile
	{ | name, maxOrder, speaker_array = nil |

		var desc = SynthDesc.read(
			SynthDef.synthDefDir ++ name ++
			".scsyndef")[name.asSymbol];

		if (desc.notNil)
		{ ^desc.metadata[\order] != maxOrder; }
		{ ^true; };
	}

	prSetVars { | maxOrder, renderer, server | } // override this method to set specific variables.
	setParams { | parentOssiaNode, allCritical | ^[] } // override this method to set specific ossia parameters.
	getParams { | parentOssiaNode, nChan | ^[] } // override this method to get specific ossia parameters.
	setAction { | parentOssiaNode, source | } // and their actions
	getArgs { | parentOssiaNode, nChan = 1 | ^[] } // override this method to get specific Local arguments.
}