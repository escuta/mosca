 + Mosca {

	ossia { |parentNode, allCrtitical = false, mirrorTree, paramDepth|

		var ossiaParent = OSSIA_Node(parentNode, "Mosca");
		var ossiaAutomation, ossiaMasterPlay;

		this.ossiasrc = Array.newClear(this.nfontes);
		this.ossiaorient = Array.newClear(this.nfontes);
		this.ossiaorigine = Array.newClear(this.nfontes);
		this.ossiacart = Array.newClear(this.nfontes);
		this.ossiasphe = Array.newClear(this.nfontes);
		this.ossialoop = Array.newClear(this.nfontes);
		this.ossialib = Array.newClear(this.nfontes);
		this.ossiaaud = Array.newClear(this.nfontes);
		this.ossialev = Array.newClear(this.nfontes);
		this.ossiadp = Array.newClear(this.nfontes);
		this.ossiaclsam = Array.newClear(this.nfontes);
		this.ossiadst = Array.newClear(this.nfontes);
		this.ossiadstam = Array.newClear(this.nfontes);
		this.ossiadstdel = Array.newClear(this.nfontes);
		this.ossiadstdec = Array.newClear(this.nfontes);
		this.ossiaangle = Array.newClear(this.nfontes);
		this.ossiarot = Array.newClear(this.nfontes);
		this.ossiadir = Array.newClear(this.nfontes);
		this.ossiactr = Array.newClear(this.nfontes);
		this.ossiaspread = Array.newClear(this.nfontes);
		this.ossiadiff = Array.newClear(this.nfontes);
		this.ossiaback = true;


		ossiaMasterPlay = OSSIA_Parameter(ossiaParent, "Audition_all", Boolean,
			critical:true);

		ossiaMasterPlay.callback_({ arg num;
			this.nfontes.do({ |i|
				this.ossiaaud[i].v_(num.value);
			});
		});

		this.ossiaorigine = OSSIA_Parameter(ossiaParent, "Origine", OSSIA_vec3f,
			domain:[[-20, -20, -20], [20, 20, 20]], default_value:[0, 0, 0],
			bounding_mode:'wrap', critical:allCrtitical, repetition_filter:true);

		this.ossiaorigine.unit_(OSSIA_position.cart3D);

		this.ossiaorigine.callback_({arg num;

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

		this.ossiaorient = OSSIA_Parameter(ossiaParent, "Orentation", OSSIA_vec3f,
			domain:[[-pi, -pi, -pi], [pi, pi, pi]], default_value:[0, 0, 0],
			bounding_mode:'wrap', critical:allCrtitical, repetition_filter:true);

		this.ossiaorient.unit_(OSSIA_orientation.euler);

		this.ossiaorient.callback_({arg num;

			if (pitchnumboxProxy.value != num[0].value) {
				pitchnumboxProxy.valueAction = num[0].value;
			};
			if (rollnumboxProxy.value != num[1].value) {
				rollnumboxProxy.valueAction = num[1].value;
			};
			if (headingnumboxProxy.value != num[2].value) {
				headingnumboxProxy.valueAction = num[2].value;
			};

		});

		this.ossiacls = OSSIA_Parameter(ossiaParent, "Cls._Afmt._Reverb", Integer,
			[0, (2 + rirList.size)], 0, 'clip', critical:true, repetition_filter:true);

		this.ossiacls.callback_({arg num;
			if (clsrvboxProxy.value != num.value) {
				clsrvboxProxy.valueAction = num.value;
			};
		});

		this.ossiaclsdel = OSSIA_Parameter(ossiaParent, "Cls._room_delay", Float,
			[0, 1], 0.5, 'clip', critical:allCrtitical, repetition_filter:true);

		this.ossiaclsdel.callback_({arg num;
			if (clsrmboxProxy.value != num.value) {
				clsrmboxProxy.valueAction = num.value;
			};
		});

		ossiaAutomation = OSSIA_Node(ossiaParent, "Aoutmation_Quarck");

		this.ossiaplay = OSSIA_Parameter(ossiaAutomation, "Play", Boolean,
			critical:true, repetition_filter:true);

		this.ossiaplay.callback_({ arg but;
			if (but) {
				if (this.isPlay.not) {
					this.control.play; };
			} {
				if (this.isPlay) {
					this.control.stop; };
			};
		});

		this.ossiatrasportLoop = OSSIA_Parameter(ossiaAutomation, "Loop", Boolean,
			critical:true, repetition_filter:true);

		this.ossiatrasportLoop.callback_({ arg but;
			if (this.autoloop.value != but.value) {
				this.autoloop.valueAction = but.value;
			};
		});

		this.ossiatransport = OSSIA_Parameter(ossiaAutomation, "Transport", Float, [0, this.dur], 0, 'wrap',
				critical:true, repetition_filter:true);

		this.ossiatransport.unit_(OSSIA_time.second);

		this.ossiatransport.callback_({arg num;
			if (this.ossiaseekback) {
				this.control.seek(num.value);
			};
		});

        this.ossiarec = OSSIA_Parameter(ossiaAutomation, "Record", Boolean,
			critical:true, repetition_filter:true);

		this.ossiarec.callback_({ arg but;
			if (but.value) {
				this.control.enableRecording;
			} {
				this.control.stopRecording;
			}
		});

		this.nfontes.do({ |i|

			this.ossiasrc[i] = OSSIA_Node(ossiaParent, "Source_" ++ (i + 1));

			this.ossiacart[i] = OSSIA_Parameter(this.ossiasrc[i], "Cartesian", OSSIA_vec3f,
				domain:[[-20, -20, -20], [20, 20, 20]], default_value:[0, 20, 0],
				critical:allCrtitical, repetition_filter:true);

			this.ossiacart[i].unit_(OSSIA_position.cart3D);

			this.ossiacart[i].callback_({arg num;
				if (xboxProxy[i].value !== num[0].value) {
					xboxProxy[i].valueAction = num[0].value;
				};
				if (yboxProxy[i].value !== num[1].value) {
					yboxProxy[i].valueAction = num[1].value;
				};
				if (zboxProxy[i].value !== num[2].value) {
					zboxProxy[i].valueAction = num[2].value;
				};
			});


			this.ossiasphe[i] = OSSIA_Parameter(this.ossiasrc[i], "Spherical", OSSIA_vec3f,
				domain:[[0, -pi, -1.5707963267949], [20, pi, 1.5707963267949]],
				default_value:[20, 0, 0], critical:allCrtitical, repetition_filter:true);

			this.ossiasphe[i].unit_(OSSIA_position.spherical);

			this.ossiasphe[i].callback_({arg num;
				spheval[i].rho_(num.value[0]);
				spheval[i].theta_(num.value[1].wrap(-pi, pi) + 1.5707963267949);
				spheval[i].phi_(num.value[2].fold(-1.5707963267949, 1.5707963267949));
				if (this.ossiaback) {
					this.ossiacart[i].v_(spheval[i].rotate(pitch).tilt(roll).tumble(heading)
						.asCartesian.asArray);
				};
			});


			this.ossialib[i] = OSSIA_Parameter(this.ossiasrc[i], "Library", Integer, [0, 4], 3, 'clip',
				critical:true, repetition_filter:true);

			this.ossialib[i].callback_({arg num;
				if (libboxProxy[i].value != num.value) {
					libboxProxy[i].valueAction = num.value;
				};
			});


			this.ossiaaud[i] = OSSIA_Parameter(this.ossiasrc[i], "audition", Boolean,
				critical:true, repetition_filter:true);

			this.ossiaaud[i].callback_({ arg num;
				if(isPlay.not) {
					if(num)
					{
						this.firstTime[i] = true;
						testado[i] = true;
						if (guiflag && (i == currentsource))
						{btestar.value = 1};
					} {
						runStop.value(i);
						this.synt[i].free;
						this.synt[i] = nil;
						testado[i] = false;
						if (guiflag && (i == currentsource))
						{btestar.value = 0};
						("stopping Source " ++ (i + 1)).postln;
					};
				};
			});


			this.ossialoop[i] = OSSIA_Parameter(this.ossiasrc[i], "loop", Boolean,
				critical:true, repetition_filter:true);

			this.ossialoop[i].callback_({ arg but;
				if (this.lpcheckProxy[i].value != but.value) {
					this.lpcheckProxy[i].valueAction = but.value;
				};
			});



			this.ossialev[i] = OSSIA_Parameter(this.ossiasrc[i], "Level", Float, [0, 1], 0, 'clip',
				critical:allCrtitical, repetition_filter:true);

			this.ossialev[i].unit_(OSSIA_gain.linear);

			this.ossialev[i].callback_({arg num;
				if (vboxProxy[i].value != num.value) {
					vboxProxy[i].valueAction = num.value;
				};
			});



			this.ossiadp[i] = OSSIA_Parameter(this.ossiasrc[i], "Doppler_amount", Float, [0, 1], 0, 'clip',
				critical:allCrtitical, repetition_filter:true);

			this.ossiadp[i].callback_({arg num;
				if (dpboxProxy[i].value != num.value) {
					dpboxProxy[i].valueAction = num.value;
				};
			});


			this.ossiaclsam[i] = OSSIA_Parameter(this.ossiasrc[i], "Cls._Afmt._amount", Float,
				[0, 1], 0, 'clip',
				critical:allCrtitical, repetition_filter:true);

			this.ossiaclsam[i].unit_(OSSIA_gain.linear);

			this.ossiaclsam[i].callback_({arg num;
				if (gboxProxy[i].value != num.value) {
					gboxProxy[i].valueAction = num.value;
				};
			});


			this.ossiadst[i] = OSSIA_Parameter(this.ossiasrc[i], "Distant_Reverb", Integer,
				[0, (3 + rirList.size)], 0, 'clip',
				critical:true, repetition_filter:true);

			this.ossiadst[i].callback_({arg num;
				if (dstrvboxProxy[i].value != num.value) {
					dstrvboxProxy[i].valueAction = num.value;
				};
			});


			this.ossiadstam[i] = OSSIA_Parameter(this.ossiasrc[i], "Dst._amount", Float,
				[0, 1], 0, 'clip',
				critical:allCrtitical, repetition_filter:true);

			this.ossiadstam[i].unit_(OSSIA_gain.linear);

			this.ossiadstam[i].callback_({arg num;
				if (lboxProxy[i].value != num.value) {
					lboxProxy[i].valueAction = num.value
				};
			});



			this.ossiadstdel[i] = OSSIA_Parameter(this.ossiasrc[i], "Dst._room_delay", Float,
				[0, 1], 0.5, 'clip',
				critical:allCrtitical, repetition_filter:true);

			this.ossiadstdel[i].callback_({arg num;
				if (rmboxProxy[i].value != num.value) {
					rmboxProxy[i].valueAction = num.value;
				};
			});



			this.ossiadstdec[i] = OSSIA_Parameter(this.ossiasrc[i], "Dst._damp_decay", Float,
				[0, 1], 0.5, 'clip',
				critical:allCrtitical, repetition_filter:true);

			this.ossiadstdec[i].callback_({arg num;
				if (dmboxProxy[i].value != num.value) {
					dmboxProxy[i].valueAction = num.value;
				};
			});



			this.ossiaangle[i] = OSSIA_Parameter(this.ossiasrc[i], "Angle_(stereo)", Float,
				[0, pi], 1.05, 'clip',
				critical:allCrtitical, repetition_filter:true);

			this.ossiaangle[i].callback_({arg num;
				if (aboxProxy[i].value != num.value) {
					aboxProxy[i].valueAction = num.value;
				};
			});



			this.ossiarot[i] = OSSIA_Parameter(this.ossiasrc[i], "Rotation_(B-Format)", Float,
				[-pi, pi], 0, 'wrap',
				critical:allCrtitical, repetition_filter:true);

			this.ossiarot[i].callback_({arg num;
				if (rboxProxy[i].value != num.value) {
					rboxProxy[i].valueAction = num.value;
				};
			});



			this.ossiadir[i] = OSSIA_Parameter(this.ossiasrc[i], "Diretivity_(B-Format)", Float,
				[0, pi * 0.5], 0, 'clip',
				critical:allCrtitical, repetition_filter:true);

			this.ossiadir[i].callback_({arg num;
				if (dboxProxy[i].value != num.value) {
					dboxProxy[i].valueAction = num.value;
				};
			});



			this.ossiactr[i] = OSSIA_Parameter(this.ossiasrc[i], "Contraction_(ATK & B-Format)", Float,
				[0, 1], 1, 'clip',
				critical:allCrtitical, repetition_filter:true);

			this.ossiactr[i].callback_({arg num;
				if (cboxProxy[i].value != num.value) {
					cboxProxy[i].valueAction = num.value
				};
			});


			this.ossiaspread[i] = OSSIA_Parameter(this.ossiasrc[i], "Spread_(ATK)", Boolean,
				critical:true, repetition_filter:true);

			this.ossiaspread[i].callback_({ arg but;
				if (spcheckProxy[i].value != but.value) {
					//spcheckProxy[i].valueAction = but.value;
				};
			});


			this.ossiadiff[i] = OSSIA_Parameter(this.ossiasrc[i], "Diffuse_(ATK)", Boolean,
				critical:true, repetition_filter:true);

			this.ossiadiff[i].callback_({ arg but;
				if (dfcheckProxy[i].value != but.value) {
					//dfcheckProxy[i].valueAction = but.value;
				};
			});


		});

	}

}