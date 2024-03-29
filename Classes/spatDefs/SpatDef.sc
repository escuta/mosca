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

//-------------------------------------------//
//                Base Class                 //
//-------------------------------------------//

SpatDef : MoscaPlugin
{
	classvar <defList, distFilter, atenuator, aten2distance;

	setup
	{ | maxOrder, effects, renderer, server |

		this.prSetVars(maxOrder, renderer, server);
		this.prSetBusses(effects, renderer);
	}

	*initClass
	{
		Class.initClassTree(MoscaUtils);

		defList = [];

		distFilter = { | p, intens | // attenuate high freq with intens
			LPF.ar(p, (1 - intens) * 18000 + 2000);
		};

		atenuator = { | radRoot | 1 / radRoot - 1 };

		aten2distance = { | radRoot | 1 / atenuator.value(radRoot) };
	}

	getMetadata
	{ | maxOrder, speaker_array |
		^(order: maxOrder);
	}

	needsReCompile
	{ | name, maxOrder, speaker_array |

		var desc = SynthDesc.read(
			SynthDef.synthDefDir ++ name ++
			".scsyndef")[name.asSymbol];

		if (desc.notNil)
		{ ^desc.metadata[\order] != maxOrder; }
		{ ^true; };
	}

	format {} // override this method to get the spat format (FUMA, N3D, NONAMBI).

	prSetBusses { | effects, renderer | } // override this method to set output busses.
}
