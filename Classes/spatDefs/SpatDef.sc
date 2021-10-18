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

SpatDef
{
	var busses, format;

	classvar <defList, distFilter, atenuator, aten2distance;

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

	*new
	{ | maxOrder, effects, renderer, server |

		^super.new().prSetVars(maxOrder, renderer, server).prSetBusses(effects, renderer);
	}

	prSetBusses
	{ | effects, renderer |

		switch(format,
			\N3D,
			{
				busses = [effects.gBxBus, renderer.n3dBus];
			},
			\FUMA,
			{
				busses = [effects.gBfBus, renderer.fumaBus];
			},
			\NONAMBI,
			{
				busses = [effects.gBfBus, renderer.nonAmbiBus];
			}
		);
	}

	prSetVars{ | maxOrder, renderer, server | } // override this method to set specific variables.
	setParams{ | parentOssiaNode | } // override this method to set specific ossia parameters.
	getArgs{ ^nil } // override this method to get specific Syth arguments.
}
