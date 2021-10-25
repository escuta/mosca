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

NonAmbiDef : SpatDef
{
	// class access
	classvar <format;
	var <busses, <fxOutFunc;

	// instance access
	format { ^format; }

	*initClass{ format = \NONAMBI; }

	prSetBusses
	{ | effects, renderer |

		busses = [effects.gBfBus, renderer.nonAmbiBus];
		fxOutFunc = { | dry, wet, globFx, fxBus |
			Out.ar(fxBus, dry * globFx); // effect.gBfBus
		};
	}

	getMetadata
	{ | maxOrder, speaker_array |

		if (speaker_array.isNil)
		{
			^(setup: MoscaUtils.emulate_array);
		} {
			^(setup: speaker_array);
		}
	}

	needsReCompile
	{ | name, maxOrder, speaker_array |

		var setup, desc = SynthDesc.read(
			SynthDef.synthDefDir ++ name ++
			".scsyndef")[name.asSymbol];

		if (speaker_array.isNil)
		{ setup = MoscaUtils.emulate_array; }
		{ setup = speaker_array; };

		if (desc.notNil)
		{ ^desc.metadata[\setup] != setup; }
		{ ^true; };
	}
}
