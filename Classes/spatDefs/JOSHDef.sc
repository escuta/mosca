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

JOSHDef : FumaDef
{
	// class access
	const <channels = #[ 1, 2 ]; // possible number of channels
	classvar <key;

	// instance access
	key { ^key; }
	channels { ^channels; }

	*initClass
	{
		defList = defList.add(this.asClass);
		key = "Josh";
	}

	setParams
	{ | parentOssiaNode, allCritical |

		var josh, rate, window, random;

		josh = OSSIA_Node(parentOssiaNode, "Josh");

		rate = OssiaAutomationProxy(josh, "Grain_rate", Float,
			[1, 60], 10, 'clip', critical:allCritical);

		rate.node.unit_(OSSIA_time.frequency).description_("JoshGrain only");

		window = OssiaAutomationProxy(josh, "Window_size", Float,
			[0, 0.2], 0.1, 'clip', critical:allCritical);

		window.node.unit_(OSSIA_time.second).description_("JoshGrain only");

		random = OssiaAutomationProxy(josh, "Random_size", Float,
			[0, 1], 0, 'clip', critical:allCritical);

		random.node.description_("JoshGrain only");

		^[rate, window, random];
	}

	setAction
	{ | parentOssiaNode, source |

		parentOssiaNode.find("Josh/Grain_rate")
		.callback_({ | val | source.setSynths(\grainrate, val.value) });

		parentOssiaNode.find("Josh/Window_size")
		.callback_({ | val | source.setSynths(\winsize, val.value) });

		parentOssiaNode.find("Josh/Random_size")
		.callback_({ | val | source.setSynths(\winrand, val.value) });
	}

	getParams
	{ | parentOssiaNode, nChan |

		^parentOssiaNode.find("Josh").children();
	}

	getArgs
	{ | parentOssiaNode, nChan |

		^[
			\grainrate, parentOssiaNode.find("Josh/Grain_rate").value,
			\winsize, parentOssiaNode.find("Josh/Window_size").value,
			\winrand, parentOssiaNode.find("Josh/Random_size").value,
		]
	}

	getFunc { | maxOrder, renderer, nChanns |

		if (nChanns == 1)
		{
			^{ | lrevRef, p, rad, radRoot, azimuth, elevation, contract, winsize, grainrate, winrand |
				var azelwinrand = 1 - contract,
				sig = distFilter.value(p.value, rad);
				lrevRef.value = MonoGrainBF.ar(lrevRef.value + sig, winsize, grainrate, winrand,
					azimuth, azelwinrand, elevation, azelwinrand,
					rho: aten2distance.value(radRoot), // invert to represent distance
					mul: ((0.5 - winsize) + (1 - (grainrate / 40))).clip(0, 1) * 0.5);
			}
		} {
			^{ | lrevRef, p, rad, radRoot, azimuth, elevation, contract, angle winsize, grainrate, winrand |
				var azelwinrand = 1 - contract,
				rho = aten2distance.value(radRoot), // invert to represent distance
				mul = ((0.5 - winsize) + (1 - (grainrate / 40))).clip(0, 1) * 0.5,
				sig = distFilter.value(p.value, rad);
				sig = lrevRef.value + sig;
				lrevRef.value = MonoGrainBF.ar(sig[0], winsize, grainrate, winrand,
					azimuth + (angle * (1 - rad)), azelwinrand, elevation, azelwinrand,
					rho: rho, mul: mul)
				+ MonoGrainBF.ar(sig[1], winsize, grainrate, winrand,
					azimuth + (angle * (1 - rad)), azelwinrand, elevation, azelwinrand,
					rho: rho, mul: mul);
			}
		};
	}
}