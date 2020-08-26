/*
Mosca: SuperCollider class by Iain Mott, 2016. Licensed under a
Creative Commons Attribution-NonCommercial 4.0 International License
http://creativecommons.org/licenses/by-nc/4.0/
The class makes extensive use of the Ambisonic Toolkit (http://www.ambisonictoolkit.net/)
by Joseph Anderson and the Automation quark
(https://github.com/neeels/Automation) by Neels Hofmeyr.
Required Quarks : Automation, Ctk, XML and  MathLib
Required classes:
SC Plugins: https://github.com/supercollider/sc3-plugins
User must set up a project directory with subdirectoties "rir" and "auto"
RIRs should have the first 100 or 120ms silenced to act as "tail" reverberators
and must be placed in the "rir" directory.
Run help on the "Mosca" class in SuperCollider for detailed information
and code examples. Further information and sample RIRs and B-format recordings
may be downloaded here: http://escuta.org/mosca
*/

+ Mosca {

	ossia { |allCrtitical|

		var halfPi = MoscaUtils.halfPi();

		var ossiaParent, ossiasrc, ossiaAutomation, ossiaMasterPlay,
		ossiaMasterLib, ossiaMasterRev, ossiaAtk, ossiaJosh, previousScale;

		ossiasrc = Array.newClear(nfontes);
		ossiaorient = Array.newClear(nfontes);
		ossiaorigine = Array.newClear(nfontes);
		ossiacart = Array.newClear(nfontes);
		ossiasphe = Array.newClear(nfontes);
		ossialoop = Array.newClear(nfontes);
		ossialib = Array.newClear(nfontes);
		ossiaaud = Array.newClear(nfontes);
		ossialev = Array.newClear(nfontes);
		ossiadp = Array.newClear(nfontes);
		ossiaclsam = Array.newClear(nfontes);
		ossiadst = Array.newClear(nfontes);
		ossiadstam = Array.newClear(nfontes);
		ossiadstdel = Array.newClear(nfontes);
		ossiadstdec = Array.newClear(nfontes);
		ossiaangle = Array.newClear(nfontes);
		ossiarot = Array.newClear(nfontes);
		ossiadir = Array.newClear(nfontes);
		ossiactr = Array.newClear(nfontes);
		ossiaAtk = Array.newClear(nfontes);
		ossiaspread = Array.newClear(nfontes);
		ossiadiff = Array.newClear(nfontes);
		ossiaJosh = Array.newClear(nfontes);
		ossiarate = Array.newClear(nfontes);
		ossiawin = Array.newClear(nfontes);
		ossiarand = Array.newClear(nfontes);
		ossiaseekback = true;
		ossiaCartBack = true;
		ossiaSpheBack = true;
		previousScale = 1;

		ossiaParent = OSSIA_Node(parentOssiaNode, "Mosca");

		ossiaMasterPlay = OSSIA_Parameter(ossiaParent, "Audition_all", Boolean,
			critical:true);

		ossiaMasterPlay.callback_({ arg num;
			nfontes.do({ |i|
				ossiaaud[i].v_(num.value);
			});
		});

		ossiaMasterLib = OSSIA_Parameter(ossiaParent, "Library_all", String, [nil, nil, spatList],
			spatList.first, critical:true, repetition_filter:true);

		ossiaMasterLib.description_(spatList.asString);

		ossiaMasterLib.callback_({ arg string;

			nfontes.do({ |i|
				if (ossialib[i].v != string.value) {
					ossialib[i].v_(string.value);
				};
			});
		});

		ossiamaster = OSSIA_Parameter(ossiaParent, "Master_level", Float,
			[-96, 12],	0, 'clip', critical:allCrtitical);

		ossiamaster.unit_(OSSIA_gain.decibel);

		ossiamaster.callback_({ arg num;
			if (masterlevProxy.value != num.value) {
				masterlevProxy.valueAction = num.value;
			};
		});


		ossiaMasterRev = OSSIA_Parameter(ossiaParent, "Distant_Reverb_all", String,
			[nil, nil, ["no-reverb","freeverb","allpass", "A-format"]
				++ rirList], "no-reverb",critical:true, repetition_filter:true);

		ossiaMasterRev.description_((["no-reverb",
			"freeverb","allpass","A-format"] ++ rirList).asString);

		ossiaMasterRev.callback_({ arg string;

			nfontes.do({ |i|
				ossiadst[i].v_(string);
			});
		});

		ossiaorigine = OSSIA_Parameter(ossiaParent, "Origine", OSSIA_vec3f,
			domain:[[-20, -20, -20], [20, 20, 20]], default_value:[0, 0, 0],
			critical:allCrtitical, repetition_filter:true);

		ossiaorigine.unit_(OSSIA_position.cart3D);

		ossiaorigine.callback_({arg num;

			origine.set(num[0].value, num[1].value, num[2].value);

			ossiaCartBack = false;

			nfontes.do {  | i |
				var cart = (cartval[i] - origine)
				.rotate(headingnumboxProxy.value.neg)
				.tilt(pitchnumboxProxy.value.neg)
				.tumble(rollnumboxProxy.value.neg)
				/ ossiascale.v;

				ossiasphe[i].v_([cart.rho,
					(cart.theta - halfPi).wrap(-pi, pi), cart.phi]);

				zlev[i] = spheval[i].z;
			};

			ossiaCartBack = true;

			if (oxnumboxProxy.value != num[0].value) {
				oxnumboxProxy.valueAction = num[0].value;
			};
			if (oynumboxProxy.value != num[1].value) {
				oynumboxProxy.valueAction = num[1].value;
			};
			if (oznumboxProxy.value != num[2].value) {
				oznumboxProxy.valueAction = num[2].value;
			};
		});

		ossiaorient = OSSIA_Parameter(ossiaParent, "Orientation", OSSIA_vec3f,
			domain:[[-pi, -pi, -pi], [pi, pi, pi]], default_value:[0, 0, 0],
			bounding_mode:'wrap', critical:allCrtitical, repetition_filter:true);

		ossiaorient.unit_(OSSIA_orientation.euler);

		ossiaorient.callback_({arg num;

			ossiaCartBack = false;

			nfontes.do { | i |
				var euler = (cartval[i] - origine)
				.rotate(num.value[0].neg)
				.tilt(num.value[1].neg)
				.tumble(num.value[2].neg)
				/ ossiascale.v;

				ossiasphe[i].v_([euler.rho,
					(euler.theta - halfPi).wrap(-pi, pi), euler.phi]);

				zlev[i] = spheval[i].z;

				this.setSynths(i, \rotAngle, ossiarot[i].v + headingnumboxProxy.value);
			};

			ossiaCartBack = true;

			if (headingnumboxProxy.value != num[0].value) {
				headingnumboxProxy.valueAction = num[0].value;
			};
			if (pitchnumboxProxy.value != num[1].value) {
				pitchnumboxProxy.valueAction = num[1].value;
			};
			if (rollnumboxProxy.value != num[2].value) {
				rollnumboxProxy.valueAction = num[2].value;
			};
		});

		ossiascale = OSSIA_Parameter(ossiaParent, "Scale_factor", Float,
			[0.01, 10],	1, 'clip', critical:allCrtitical);

		ossiascale.callback_({ arg num;

			nfontes.do { | i |
				spheval[i].rho_(spheval[i].rho / previousScale);
				spheval[i].rho_(spheval[i].rho * num.value);

				cartval[i] = (spheval[i].tumble(rollnumboxProxy.value)
					.tilt(pitchnumboxProxy.value)
					.rotate(headingnumboxProxy.value)
					.asCartesian + origine);

				zlev[i] = spheval[i].z;

				this.setSynths(i, \radius, spheval[i].rho);
			};

			previousScale = num.value;

			if (scalefactProxy.value != num.value) {
				scalefactProxy.valueAction = num.value;
			};
		});

		ossiaremotectl = OSSIA_Parameter(ossiaParent, "Remote_Control", Boolean, default_value:true);

		ossiaremotectl.callback_({ |v|
			this.remoteCtl(v);
		});

		ossiacls = OSSIA_Parameter(ossiaParent, "Close_Reverb", String,
			[nil, nil, ["no-reverb","freeverb","allpass"] ++ rirList], "no-reverb",
			critical:true, repetition_filter:true);

		ossiacls.description_((["no-reverb","freeverb","allpass"] ++ rirList).asString);

		ossiacls.callback_({arg string;
			var index = (["no-reverb","freeverb","allpass"] ++ rirList).detectIndex({ arg item; item == string });

			if (clsrvboxProxy.value != index) {
				clsrvboxProxy.valueAction = index;
			};
		});

		ossiaclsdel = OSSIA_Parameter(ossiacls, "Close_room_delay", Float,
			[0, 1], 0.5, 'clip', critical:allCrtitical, repetition_filter:true);

		ossiaclsdel.callback_({arg num;
			if (clsrmboxProxy.value != num.value) {
				clsrmboxProxy.valueAction = num.value;
			};
		});

		ossiaclsdec = OSSIA_Parameter(ossiacls, "Close_damp_decay", Float,
			[0, 1], 0.5, 'clip', critical:allCrtitical, repetition_filter:true);

		ossiaclsdec.callback_({arg num;
			if (clsdmboxProxy.value != num.value) {
				clsdmboxProxy.valueAction = num.value;
			};
		});

		ossiaAutomation = OSSIA_Node(ossiaParent, "Automation_Quarck");

		ossiaplay = OSSIA_Parameter(ossiaAutomation, "Play", Boolean,
			critical:true, repetition_filter:true);

		ossiaplay.callback_({ arg bool;
			if (bool) {
				if (isPlay.not) {
					control.play; };
			} {
				if (isPlay) {
					control.stop; };
			};
		});

		ossiatrasportLoop = OSSIA_Parameter(ossiaAutomation, "Loop", Boolean,
			critical:true, repetition_filter:true);

		ossiatrasportLoop.callback_({ arg but;
			if (autoloop.value != but.value) {
				autoloop.valueAction = but.value;
			};
		});

		ossiatransport = OSSIA_Parameter(ossiaAutomation, "Transport", Float,
			[0, dur], 0, 'wrap', critical:true, repetition_filter:true);

		ossiatransport.unit_(OSSIA_time.second);

		ossiatransport.callback_({arg num;
			if (ossiaseekback) {
				control.seek(num.value);
			};
		});

		ossiarec = OSSIA_Parameter(ossiaAutomation, "Record", Boolean,
			critical:true, repetition_filter:true);

		ossiarec.callback_({ arg but;
			if (but.value) {
				control.enableRecording;
			} {
				control.stopRecording;
			};
		});

		nfontes.do({ |i|

			ossiasrc[i] = OSSIA_Node(ossiaParent, "Source_" ++ (i + 1));

			ossiacart[i] = OSSIA_Parameter(ossiasrc[i], "Cartesian", OSSIA_vec3f,
				domain:[[-20, -20, -20], [20, 20, 20]], default_value:[0, 20, 0],
				critical:allCrtitical, repetition_filter:true);

			ossiacart[i].unit_(OSSIA_position.cart3D);

			ossiacart[i].callback_({arg num;
				var sphe, sphediff;
				cartval[i].set(num.value[0], num.value[1], num.value[2]);
				sphe = (cartval[i] - origine)
				.rotate(headingnumboxProxy.value.neg)
				.tilt(pitchnumboxProxy.value.neg)
				.tumble(rollnumboxProxy.value.neg);

				sphediff = [sphe.rho, (sphe.theta - halfPi).wrap(-pi, pi), sphe.phi];

				ossiaCartBack = false;

				if (ossiaSpheBack && (ossiasphe[i].v != sphediff)) {
					ossiasphe[i].v_(sphediff);
				};

				if (xboxProxy[i].value != num[0].value) {
					xboxProxy[i].valueAction = num[0].value;
				};
				if (yboxProxy[i].value != num[1].value) {
					yboxProxy[i].valueAction = num[1].value;
				};
				if (zboxProxy[i].value != num[2].value) {
					zboxProxy[i].valueAction = num[2].value;
				};

				ossiaCartBack = true;
			});


			ossiasphe[i] = OSSIA_Parameter(ossiasrc[i], "Spherical", OSSIA_vec3f,
				domain:[[0, -pi, halfPi.neg], [20, pi, halfPi]],
				default_value:[20, 0, 0], critical:allCrtitical, repetition_filter:true);

			ossiasphe[i].unit_(OSSIA_position.spherical);

			ossiasphe[i].callback_({arg num;
				spheval[i].rho_(num.value[0] * ossiascale.v);
				spheval[i].theta_(num.value[1].wrap(-pi, pi) + halfPi);
				spheval[i].phi_(num.value[2].fold(halfPi.neg, halfPi));
				ossiaSpheBack = false;
				if (ossiaCartBack) {
					ossiacart[i].v_(
						((spheval[i].tumble(rollnumboxProxy.value)
							.tilt(pitchnumboxProxy.value)
							.rotate(headingnumboxProxy.value)
							.asCartesian) + origine).asArray);
				};

				if(espacializador[i].notNil) {
					espacializador[i].set(\radius, spheval[i].rho, \azim, spheval[i].theta, \elev, spheval[i].phi);
				};

				if (synt[i].notNil) {
					synt[i].do({ _.set(\radius, spheval[i].rho, \azim, spheval[i].theta, \elev, spheval[i].phi); });
				};

				ossiaSpheBack = true;
			});


			ossialib[i] = OSSIA_Parameter(ossiasrc[i], "Library", String, [nil, nil, spatList],
				spatList.first, critical:true, repetition_filter:true);

			ossialib[i].description_(spatList.asString);

			ossialib[i].callback_({arg string;
				var index = spatList.detectIndex({ arg item; item == string });

				if (libboxProxy[i].value != index) {
					libboxProxy[i].valueAction = index;
				};
			});


			ossiaaud[i] = OSSIA_Parameter(ossiasrc[i], "Audition", Boolean,
				critical:true, repetition_filter:true);

			ossiaaud[i].callback_({ arg num;
				if (audit[i] != num.value) {
					this.auditionFunc(i, num.value);
				};
			});


			ossialoop[i] = OSSIA_Parameter(ossiasrc[i], "Loop", Boolean,
				critical:true, repetition_filter:true);

			ossialoop[i].callback_({ arg but;
				if (lpcheckProxy[i].value != but.value) {
					lpcheckProxy[i].valueAction = but.value;
				};
			});


			ossialev[i] = OSSIA_Parameter(ossiasrc[i], "Level", Float, [-96, 12],
				0, 'clip', critical:allCrtitical, repetition_filter:true);

			ossialev[i].unit_(OSSIA_gain.decibel);

			ossialev[i].callback_({arg num;
				if (vboxProxy[i].value != num.value) {
					vboxProxy[i].valueAction = num.value;
				};
			});


			ossiadp[i] = OSSIA_Parameter(ossiasrc[i], "Doppler_amount", Float,
				[0, 1], 0, 'clip', critical:allCrtitical, repetition_filter:true);

			ossiadp[i].callback_({arg num;
				if (dpboxProxy[i].value != num.value) {
					dpboxProxy[i].valueAction = num.value;
				};
			});


			ossiaclsam[i] = OSSIA_Parameter(ossiasrc[i], "Close_amount", Float,
				[0, 1], 0, 'clip',
				critical:allCrtitical, repetition_filter:true);

			ossiaclsam[i].unit_(OSSIA_gain.linear);

			ossiaclsam[i].callback_({arg num;
				if (gboxProxy[i].value != num.value) {
					gboxProxy[i].valueAction = num.value;
				};
			});


			ossiadst[i] = OSSIA_Parameter(ossiasrc[i], "Distant_Reverb", String,
				[nil, nil, ["no-reverb","freeverb","allpass", "A-format"]
					++ rirList], "no-reverb", critical:true, repetition_filter:true);

			ossiadst[i].description_((["no-reverb","freeverb","allpass", "A-format"]
				++ rirList).asString);

			ossiadst[i].callback_({arg string;
				var index = (["no-reverb","freeverb","allpass"] ++ rirList).detectIndex({ arg item; item == string });

				if (dstrvboxProxy[i].value != index) {
					dstrvboxProxy[i].valueAction = index;
				};
			});


			ossiadstam[i] = OSSIA_Parameter(ossiadst[i], "Distant_amount", Float,
				[0, 1], 0, 'clip', critical:allCrtitical, repetition_filter:true);

			ossiadstam[i].unit_(OSSIA_gain.linear);

			ossiadstam[i].callback_({arg num;
				if (lboxProxy[i].value != num.value) {
					lboxProxy[i].valueAction = num.value
				};
			});



			ossiadstdel[i] = OSSIA_Parameter(ossiadst[i], "Distant_room_delay", Float,
				[0, 1], 0.5, 'clip',
				critical:allCrtitical, repetition_filter:true);

			ossiadstdel[i].callback_({arg num;
				if (rmboxProxy[i].value != num.value) {
					rmboxProxy[i].valueAction = num.value;
				};
			});



			ossiadstdec[i] = OSSIA_Parameter(ossiadst[i], "Distant_damp_decay", Float,
				[0, 1], 0.5, 'clip',
				critical:allCrtitical, repetition_filter:true);

			ossiadstdec[i].callback_({arg num;
				if (dmboxProxy[i].value != num.value) {
					dmboxProxy[i].valueAction = num.value;
				};
			});



			ossiaangle[i] = OSSIA_Parameter(ossiasrc[i], "Stereo_angle", Float,
				[0, pi], 1.05, 'clip',
				critical:allCrtitical, repetition_filter:true);

			ossiaangle[i].unit_(OSSIA_angle.radian);

			ossiaangle[i].description_("Stereo only");

			ossiaangle[i].callback_({arg num;
				if (aboxProxy[i].value != num.value) {
					aboxProxy[i].valueAction = num.value;
				};
			});



			ossiarot[i] = OSSIA_Parameter(ossiasrc[i], "Rotation", Float,
				[-pi, pi], 0, 'wrap',
				critical:allCrtitical, repetition_filter:true);

			ossiarot[i].unit_(OSSIA_angle.radian);

			ossiarot[i].description_("B-Format only");

			ossiarot[i].callback_({arg num;
				if (rboxProxy[i].value != num.value) {
					rboxProxy[i].valueAction = num.value;
				};
			});



			ossiadir[i] = OSSIA_Parameter(ossiasrc[i], "Diretivity", Float,
				[0, pi * 0.5], 0, 'clip',
				critical:allCrtitical, repetition_filter:true);

			ossiadir[i].description_("B-Format");

			ossiadir[i].callback_({arg num;
				if (dboxProxy[i].value != num.value) {
					dboxProxy[i].valueAction = num.value;
				};
			});


			ossiaAtk[i] = OSSIA_Node(ossiasrc[i], "Atk");

			ossiaspread[i] = OSSIA_Parameter(ossiaAtk[i], "Spread", Boolean,
				critical:true, repetition_filter:true);

			ossiaspread[i].description_("ATK only");

			ossiaspread[i].callback_({ arg but;
				if (spcheckProxy[i].value != but.value) {
					spcheckProxy[i].valueAction = but.value;
				};
			});


			ossiadiff[i] = OSSIA_Parameter(ossiaAtk[i], "Diffuse", Boolean,
				critical:true, repetition_filter:true);

			ossiadiff[i].description_("ATK only");

			ossiadiff[i].callback_({ arg but;
				if (dfcheckProxy[i].value != but.value) {
					dfcheckProxy[i].valueAction = but.value;
				};
			});


			ossiactr[i] = OSSIA_Parameter(ossiasrc[i], "Contraction", Float,
				[0, 1], 1.0, 'clip',
				critical:allCrtitical, repetition_filter:true);

			ossiactr[i].callback_({arg but;
				if (cboxProxy[i].value != but.value) {
					cboxProxy[i].valueAction = but.value
				};
			});

			ossiaJosh[i] = OSSIA_Node(ossiasrc[i], "Josh");

			ossiarate[i] = OSSIA_Parameter(ossiaJosh[i], "Grain_rate", Float,
				[1, 60], 10, 'clip',
				critical:allCrtitical, repetition_filter:true);

			ossiarate[i].unit_(OSSIA_time.frequency);

			ossiarate[i].description_("JoshGrain only");

			ossiarate[i].callback_({arg but;
				if (rateboxProxy[i].value != but.value) {
					rateboxProxy[i].valueAction = but.value
				};
			});


			ossiawin[i] = OSSIA_Parameter(ossiaJosh[i], "Window_size", Float,
				[0, 0.2], 0.1, 'clip',
				critical:allCrtitical, repetition_filter:true);

			ossiawin[i].unit_(OSSIA_time.second);

			ossiawin[i].description_("JoshGrain only");

			ossiawin[i].callback_({arg but;
				if (winboxProxy[i].value != but.value) {
					winboxProxy[i].valueAction = but.value
				};
			});


			ossiarand[i] = OSSIA_Parameter(ossiaJosh[i], "Randomize_window", Float,
				[0, 1], 0, 'clip',
				critical:allCrtitical, repetition_filter:true);

			ossiarand[i].description_("JoshGrain only");

			ossiarand[i].callback_({arg but;
				if (randboxProxy[i].value != but.value) {
					randboxProxy[i].valueAction = but.value
				};
			});

		});

	}
}