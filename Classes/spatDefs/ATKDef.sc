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
//                 ATK BASE                  //
//-------------------------------------------//

ATKBaseDef : SpatDef
{
	var <format, encoder, // specific variables
	<busses, <fxOutFunc;

	getFunc
	{ | maxOrder, renderer, nChanns |

		switch (nChanns,
			1,
			{
				^{ | lrevRef, p, rad, radRoot, azimuth, elevation, contract |
					var sig = distFilter.value(p.value, rad);
					sig = lrevRef.value + (sig * atenuator.value(radRoot));
					sig = FoaEncode.ar(sig, encoder);
					sig = FoaTransform.ar(sig, 'push', MoscaUtils.halfPi * contract,
						CircleRamp.kr(azimuth, 0.1, -pi, pi), elevation);
					//SendTrig.kr(Impulse.kr(1), 0, CircleRamp.kr(azimuth, 0.1, -pi, pi) ); // debug
					sig = HPF.ar(sig, 20); // stops bass frequency blow outs by proximity
					lrevRef.value = FoaTransform.ar(sig, 'proximity', rad * renderer.longestRadius);
				};
			},
			2,
			{
				^{ | lrevRef, p, rad, radRoot, azimuth, elevation, contract, angle |
					var l, r, sig = distFilter.value(p.value, rad),
					contr = MoscaUtils.halfPi * contract, az1, az2;
					az1 = azimuth + ((angle/2.0) * (1 - rad));
					az2 = azimuth - ((angle/2.0) * (1 - rad));
					az1 = CircleRamp.kr(az1, 0.1, -pi, pi);
					az2 = CircleRamp.kr(az2, 0.1, -pi, pi);
					sig = lrevRef.value + (sig * atenuator.value(radRoot));
					l = FoaEncode.ar(sig[0], encoder);
					r = FoaEncode.ar(sig[1], encoder);
					//					SendTrig.kr(Impulse.kr(1), 0, azimuth ); // debug
					l = FoaTransform.ar(l, 'push', contr, az1, elevation);
					r = FoaTransform.ar(r, 'push', contr, az2, elevation);
					sig = HPF.ar(l + r, 20); // stops bass frequency blow outs by proximity
					lrevRef.value = FoaTransform.ar(sig, 'proximity', rad * renderer.longestRadius);
				};
			},
			4,
			{ // assume FuMa input
				^{ | lrevRef, p, rad, radRoot, azimuth, elevation, contract, rotAngle, directang,
					linear = 1 |
					var sig, pushang = 2 - (contract * 2), pushang2, linearsig, linearscale;
					pushang = rad.linlin(pushang - 1, pushang, 0, MoscaUtils.halfPi);
					pushang2 = rad * MoscaUtils.halfPi;
					linearscale = (12 - (rad * 12));
					linearscale = Select.kr(linearscale > 0, [0, linearscale]); //negs rolled off at zero
					linearsig = lrevRef.value + (p.value * linearscale  );
					sig = lrevRef.value + (p.value * ((1/radRoot) - 1) );
					sig = Select.ar(linear > 0, [sig, linearsig]);
					sig = FoaDirectO.ar(sig, directang);
					// directivity
					sig = FoaTransform.ar(sig, 'rotate', rotAngle);
					// handle contraction fader
					lrevRef.value = FoaTransform.ar(sig, 'push', pushang, azimuth, elevation);
					// allow field to "move"
					lrevRef.value = FoaTransform.ar(lrevRef.value, 'push', pushang2, azimuth, elevation);
				};
			},
			{ // assume N3D input
				var ord = (nChanns.sqrt) - 1;
				^{ | lrevRef, p, rad, radRoot, azimuth, elevation, contract, rotAngle, directang |
					var sig, pushang = 2 - (contract * 2);
					pushang = rad.linlin(pushang - 1, pushang, 0, MoscaUtils.halfPi);
					sig = lrevRef.value + (p.value * ((1/radRoot) - 1));
					sig = FoaEncode.ar(sig, MoscaUtils.n2f);
					sig = FoaDirectO.ar(sig, directang);
					// directivity
					sig = FoaTransform.ar(sig, 'rotate', rotAngle);
					lrevRef.value = FoaTransform.ar(sig, 'push', pushang, azimuth, elevation);
				};
			}
		)
	}

	prSetBusses
	{ | effects, renderer |

		busses = [effects.gBfBus, renderer.fumaBus];
		fxOutFunc = { | dry, wet, globFx, fxBus |
			Out.ar(fxBus, wet * globFx); // effect.gBfBus
		};
	}
}

//-------------------------------------------//
//                   ATK                     //
//-------------------------------------------//

ATKDef : ATKBaseDef
{
	// class access
	const <channels = #[ 1, 2, 4, 16, 25, 36 ]; // possible number of channels
	classvar <key;

	// instance access
	key { ^key; }
	channels { ^channels; }

	*initClass
	{
		defList = defList.add(this.asClass);
		key = "ATK";
	}

	setParams
	{ | parentOssiaNode, allCritical |

		var direct = OssiaAutomationProxy(parentOssiaNode,
			"Directivity", Float, [0, 90], 0,
			'clip', critical:allCritical);

		direct.node.unit_(OSSIA_angle.degree).description_("ATK B-Format only");

		^[direct];
	}

	setAction
	{ | parentOssiaNode, source |

		parentOssiaNode.find("Directivity")
		.callback_({ | val | source.setSynths(\directang, val.value.degrad) });
	}

	getParams
	{ | parentOssiaNode, nChan |

		if (nChan > 2)
		{ ^[parentOssiaNode.find("Directivity")] }
		{ ^[] }
	}

	getArgs
	{ | parentOssiaNode, nChan |

		if (nChan > 2)
		{ ^[\directang, parentOssiaNode.find("Directivity").value] }
		{ ^[] }
	}

	prSetVars
	{ | maxOrder, renderer, server |
		format = \FUMA;
		encoder = FoaEncoderMatrix.newOmni;
	}
}

//-------------------------------------------//
//                ATK SPREAD                 //
//-------------------------------------------//

ATKDfDef : ATKBaseDef
{
	// class acces
	const <channels = #[ 1, 2 ]; // possible number of channels
	classvar <key;

	// instance access
	key { ^key; }
	channels { ^channels; }

	*initClass
	{
		defList = defList.add(this.asClass);
		key = "ATK_sp";
	}

	// allways recompile as the graprequires an kernel instance
	needsReCompile{ | name, maxOrder, speaker_array | ^true }

	prSetVars
	{ | maxOrder, renderer, server |
		format = \FUMA;
		encoder = FoaEncoderKernel.newSpread(subjectID: 6, kernelSize: 2048,
			server:server, sampleRate:server.sampleRate.asInteger);
		server.sync;
	}
}

//-------------------------------------------//
//                 ATK DIFFUSE               //
//-------------------------------------------//

ATKSpDef : ATKBaseDef
{
	// class acces
	const <channels = #[ 1, 2 ]; // possible number of channels
	classvar <key;

	// instance access
	key { ^key; }
	channels { ^channels; }

	*initClass
	{
		defList = defList.add(this.asClass);
		key = "ATK_df";
	}

	// allways recompile as the graprequires an kernel instance
	needsReCompile{ | name, maxOrder, speaker_array | ^true }

	prSetVars
	{ | maxOrder, renderer, server |
		format = \FUMA;
		encoder = FoaEncoderKernel.newDiffuse(subjectID: 3, kernelSize: 2048,
			server:server, sampleRate:server.sampleRate.asInteger);
		server.sync;
	}
}
