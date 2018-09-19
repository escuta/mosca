 + Mosca {

	ossia { |parentNode, allCrtitical = false, mirrorTree, paramDepth|

		var ossiaParent = OSSIA_Node(parentNode, "Mosca");

		this.ossiasrc = Array.newClear(this.nfontes);
		this.ossiacart = Array.newClear(this.nfontes);
		this.ossialoop = Array.newClear(this.nfontes);
		this.ossiaaud = Array.newClear(this.nfontes);
		this.ossialib = Array.newClear(this.nfontes);
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
		this.ossiasdif = Array.newClear(this.nfontes);
		this.ossiaspre = Array.newClear(this.nfontes);

		this.ossiacls = OSSIA_Parameter(ossiaParent, "Close_Reverb", Integer,
				[0, (2 + rirList.size)], 0, 'clip',
				critical:true, repetition_filter:true);

			this.ossiacls.callback_({arg num;

				this.clsrv = num.value;
				case
				{ num.value == 1 }
				{ clsRvtypes = "_free"; }
				{ num.value == 2 }
				{ clsRvtypes = "_pass"; }
				{ num.value > 2 }
				{ clsRvtypes = "_conv"; };

				clsrvboxProxy.value = num.value;

				if (num.value == 0)
				{
					if(revGlobal.isPlaying)
					{ this.revGlobal.set(\gate, 0) };

					if(revGlobalBF.isPlaying)
					{ this.revGlobalBF.set(\gate, 0) };

					if(revGlobalSoa.isPlaying)
					{ this.revGlobalSoa.set(\gate, 0) };
				} {
					if(revGlobal.isPlaying) {
						this.revGlobal.set(\gate, 0);

						this.revGlobal = Synth.new(\revGlobalAmb++clsRvtypes, [\gbus, gbus, \gate, 1,
							\room, clsrm, \damp, clsdm,
							\wir, rirWspectrum[max((num.value - 3), 0)],
							\xir, rirXspectrum[max((num.value - 3), 0)],
							\yir, rirYspectrum[max((num.value - 3), 0)],
							\zir, rirZspectrum[max((num.value - 3), 0)]],
						this.glbRevDecGrp).register;
					} {
						this.revGlobal = Synth.new(\revGlobalAmb++clsRvtypes, [\gbus, gbus, \gate, 1,
							\room, clsrm, \damp, clsdm,
							\wir, rirWspectrum[max((num.value - 3), 0)],
							\xir, rirXspectrum[max((num.value - 3), 0)],
							\yir, rirYspectrum[max((num.value - 3), 0)],
							\zir, rirZspectrum[max((num.value - 3), 0)]],
						this.glbRevDecGrp).register;
					};

					if(this.globBfmtNeeded(0)) {
						if (revGlobalBF.isPlaying) {
							this.revGlobalBF.set(\gate, 0);

							this.revGlobalBF = Synth.new(\revGlobalBFormatAmb++clsRvtypes,
								[\gbfbus, gbfbus, \gate, 1, \room, clsrm, \damp, clsdm,
									\fluir, rirFLUspectrum[max((num.value - 3), 0)],
									\frdir, rirFRDspectrum[max((num.value - 3), 0)],
									\bldir, rirBLDspectrum[max((num.value - 3), 0)],
									\bruir, rirBRUspectrum[max((num.value - 3), 0)]],
								this.glbRevDecGrp).register;
						} {
							this.revGlobalBF = Synth.new(\revGlobalBFormatAmb++clsRvtypes,
								[\gbfbus, gbfbus, \gate, 1, \room, clsrm, \damp, clsdm,
									\fluir, rirFLUspectrum[max((num.value - 3), 0)],
									\frdir, rirFRDspectrum[max((num.value - 3), 0)],
									\bldir, rirBLDspectrum[max((num.value - 3), 0)],
									\bruir, rirBRUspectrum[max((num.value - 3), 0)]],
								this.glbRevDecGrp).register;
						};
					};

					if (this.maxorder > 1) {
						if (this.globSoaA12Needed(0)) {
							if (revGlobalSoa.isPlaying) {
								this.revGlobalSoa.set(\gate, 0);

								this.revGlobalSoa = Synth.new(\revGlobalSoaA12++clsRvtypes,
									[\soaBus, soaBus, \gate, 1, \room, clsrm, \damp, clsdm,
										\a0ir, rirA12Spectrum[max((num.value - 3), 0), 0],
										\a1ir, rirA12Spectrum[max((num.value - 3), 0), 1],
										\a2ir, rirA12Spectrum[max((num.value - 3), 0), 2],
										\a3ir, rirA12Spectrum[max((num.value - 3), 0), 3],
										\a4ir, rirA12Spectrum[max((num.value - 3), 0), 4],
										\a5ir, rirA12Spectrum[max((num.value - 3), 0), 5],
										\a6ir, rirA12Spectrum[max((num.value - 3), 0), 6],
										\a7ir, rirA12Spectrum[max((num.value - 3), 0), 7],
										\a8ir, rirA12Spectrum[max((num.value - 3), 0), 8],
										\a9ir, rirA12Spectrum[max((num.value - 3), 0), 9],
										\a10ir, rirA12Spectrum[max((num.value - 3), 0), 10],
										\a11ir, rirA12Spectrum[max((num.value - 3), 0), 11]],
									this.glbRevDecGrp).register;
							} {
								this.revGlobalSoa = Synth.new(\revGlobalSoaA12++clsRvtypes,
									[\soaBus, soaBus, \gate, 1, \room, clsrm, \damp, clsdm,
										\a0ir, rirA12Spectrum[max((num.value - 3), 0), 0],
										\a1ir, rirA12Spectrum[max((num.value - 3), 0), 1],
										\a2ir, rirA12Spectrum[max((num.value - 3), 0), 2],
										\a3ir, rirA12Spectrum[max((num.value - 3), 0), 3],
										\a4ir, rirA12Spectrum[max((num.value - 3), 0), 4],
										\a5ir, rirA12Spectrum[max((num.value - 3), 0), 5],
										\a6ir, rirA12Spectrum[max((num.value - 3), 0), 6],
										\a7ir, rirA12Spectrum[max((num.value - 3), 0), 7],
										\a8ir, rirA12Spectrum[max((num.value - 3), 0), 8],
										\a9ir, rirA12Spectrum[max((num.value - 3), 0), 9],
										\a10ir, rirA12Spectrum[max((num.value - 3), 0), 10],
										\a11ir, rirA12Spectrum[max((num.value - 3), 0), 11]],
									this.glbRevDecGrp).register;
							};
						};
					};
				};

				if (guiflag) {
					{ clsReverbox.value = num.value }.defer;
				};
			});



		this.nfontes.do({ |i|

			this.ossiasrc[i] = OSSIA_Node(ossiaParent, "Source_" ++ (i + 1));

			this.ossiacart[i] = OSSIA_Parameter(this.ossiasrc[i], "Cartesian", OSSIA_vec3f,
				domain:[[-20, -20, -20], [20, 20, 20]], default_value:[-20, -20, 0],
				bounding_mode:'wrap', critical:allCrtitical, repetition_filter:true);

			this.ossiacart[i].unit_(OSSIA_position.cart3D);

			this.ossiacart[i].callback_({arg num;
				var period = Main.elapsedTime - this.lastGui;
				this.cartval[i].x_(num[0].value);
				this.cartval[i].y_(num[1].value);
				this.cartval[i].z_(num[2].value);
				zlev[i] = this.cartval[i].z;
				this.spheval[i] = this.cartval[i].asSpherical;
				if (period > this.guiInt) {
					this.lastGui =  Main.elapsedTime;
					xboxProxy[i].value = num[0].value;
					yboxProxy[i].value = num[1].value;
					zboxProxy[i].value = num[2].value;
					if (guiflag) {
						{ this.xbox[i].value = num[0].value }.defer;
						{ this.ybox[i].value = num[1].value }.defer;
						{ this.zbox[i].value = num[2].value }.defer;
						if(i == currentsource) {
							{ zslider.value = (num[2].value + 1) / 2 }.defer;
							{ znumbox.value = num[2].value }.defer;
						};
						novoplot.value;
					};
				};
				if(this.espacializador[i].notNil || this.playingBF[i]) {
					this.espacializador[i].set(\azim, this.spheval[i].theta);
					this.setSynths(i, \azim, this.spheval[i].theta);
					this.synt[i].set(\azim, this.spheval[i].theta);
					this.espacializador[i].set(\elev, this.spheval[i].phi);
					this.setSynths(i, \elev, this.spheval[i].phi);
					this.synt[i].set(\elev, this.spheval[i].phi);
					this.espacializador[i].set(\radius, this.spheval[i].rho);
					this.setSynths(i, \radius, this.spheval[i].rho);
					this.synt[i].set(\radius, this.spheval[i].rho);
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
				if (but.value) {
					lp[i] = 1;
					this.synt[i].set(\lp, 1);
					this.setSynths(i, \lp, 1);
					this.lpcheckProxy[i].value = 1;
				} {
					lp[i] = 0;
					this.synt[i].set(\lp, 0);
					this.setSynths(i, \lp, 0);
					this.lpcheckProxy[i].value = 1;
				};
				if (guiflag) {
					{this.lpcheck[i].value = but.value}.defer;
					if(i==currentsource) {
						{loopcheck.value = but.value}.defer;
					};
				};
			});


			this.ossialib[i] = OSSIA_Parameter(this.ossiasrc[i], "Library", Integer, [0, 4], 0, 'clip',
				critical:true, repetition_filter:true);

			this.ossialib[i].callback_({arg num;
				case
				{ num.value == 0 }{ libName[i] = "ATK"; }
				{ num.value == 1 }{ libName[i] = "ambitools"; }
				{ num.value == 2 }{ libName[i] = "hoaLib"; }
				{ num.value == 3 }{ libName[i] = "ambiPanner"; }
				{ num.value == 4 }{ libName[i] = "VBAP"; };
				libboxProxy[i].value = num.value;
				if (guiflag) {
					{this.libbox[i].value = num.value}.defer;
					if(i == currentsource) {
						{libnumbox.value = num.value}.defer;
					};
				};
			});



			this.ossialev[i] = OSSIA_Parameter(this.ossiasrc[i], "Level", Float, [0, 1], 0, 'clip',
				critical:allCrtitical, repetition_filter:true);

			this.ossialev[i].unit_(OSSIA_gain.linear);

			this.ossialev[i].callback_({arg num;
				this.synt[i].set(\level, num.value);
				this.setSynths(i, \level, num.value);
				level[i] = num.value;
				vboxProxy[i].value = num.value;
				if (guiflag)
				{
					{vbox[i].value = num.value}.defer;
					if(i == currentsource)
					{
						{volslider.value = num.value}.defer;
						{volnumbox.value = num.value}.defer;
					};
				};
			});



			this.ossiadp[i] = OSSIA_Parameter(this.ossiasrc[i], "Doppler_amount", Float, [0, 1], 0, 'clip',
				critical:allCrtitical, repetition_filter:true);

			this.ossiadp[i].callback_({arg num;
				this.synt[i].set(\dopamnt, num.value);
				this.setSynths(i, \dopamnt, num.value);
				this.espacializador[i].set(\dopamnt, num.value);
				dplev[i] = num.value;
				dpboxProxy[i].value = num.value;
				if (guiflag) {
					{dpbox[i].value = num.value}.defer;
					if(i == currentsource) {
						{dpslider.value = num.value}.defer;
						{dopnumbox.value = num.value}.defer;
					};
				};
			});



			this.ossiaclsam[i] = OSSIA_Parameter(this.ossiasrc[i], "Cls._Afmt._amount", Float, [0, 1], 0, 'clip',
				critical:allCrtitical, repetition_filter:true);

			this.ossiaclsam[i].unit_(OSSIA_gain.linear);

			this.ossiaclsam[i].callback_({arg num;
				this.espacializador[i].set(\glev, num.value);
				this.setSynths(i, \glev, num.value);
				this.synt[i].set(\glev, num.value);
				glev[i] = num.value;
				gboxProxy[i].value = num.value;
				if (guiflag) {
					{gbox[i].value = num.value}.defer;
					if (i == currentsource)
					{
						{gslider.value = num.value}.defer;
						{gnumbox.value = num.value}.defer;
					};
				};
			});



			this.ossiadst[i] = OSSIA_Parameter(this.ossiasrc[i], "Distant_Reverb", Integer,
				[0, (3 + rirList.size)], 0, 'clip',
				critical:true, repetition_filter:true);

			this.ossiadst[i].callback_({arg num;
				case
				{ num.value == 0 }{ dstrvtypes[i] = ""; }
				{ num.value == 1 }{ dstrvtypes[i] = "_free"; }
				{ num.value == 2 }{ dstrvtypes[i] = "_pass"; }
				{ num.value >= 4 }{ dstrvtypes[i] = "_conv"; };
				this.dstrvboxProxy[i].value = num.value;
				if (guiflag) {
					{this.dstrvbox[i].value = num.value}.defer;

					if (i == currentsource)
					{
						{ dstReverbox.value = num.value }.defer;
						if (num.value == 3) {
							this.setSynths(i, \rv, 1);
						} {
							this.setSynths(i, \rv, 0);
						};
					};
				};
			});



			this.ossiadstam[i] = OSSIA_Parameter(this.ossiasrc[i], "Dst._amount", Float,
				[0, 1], 0, 'clip',
				critical:allCrtitical, repetition_filter:true);

			this.ossiadstam[i].unit_(OSSIA_gain.linear);

			this.ossiadstam[i].callback_({arg num;
				this.espacializador[i].set(\llev, num.value);
				this.setSynths(i, \llev, num.value);
				this.synt[i].set(\llev, num.value);
				llev[i] = num.value;
				lboxProxy[i].value = num.value;
				if (guiflag) {
					lbox[i].value = num.value;
					if (i == currentsource)
					{
						{lslider.value = num.value}.defer;
						{lnumbox.value = num.value}.defer;
					};
				};
			});



			this.ossiadstdel[i] = OSSIA_Parameter(this.ossiasrc[i], "Dst._room_delay", Float,
				[0, 1], 0.5, 'clip',
				critical:allCrtitical, repetition_filter:true);

			this.ossiadstdel[i].callback_({arg num;
				this.espacializador[i].set(\room, num.value);
				this.setSynths(i, \room, num.value);
				this.synt[i].set(\room, num.value);
				rm[i] = num.value;
				rmboxProxy[i].value = num.value;
				if (guiflag) {
					rmbox[i].value = num.value;
					if (i == currentsource) {
						{rmslider.value = num.value}.defer;
						{rmnumbox.value = num.value}.defer;
					};
				};
			});



			this.ossiadstdec[i] = OSSIA_Parameter(this.ossiasrc[i], "Dst._damp_decay", Float,
				[0, 1], 0.5, 'clip',
				critical:allCrtitical, repetition_filter:true);

			this.ossiadstdec[i].callback_({arg num;
				this.espacializador[i].set(\damp, num.value);
				this.setSynths(i, \damp, num.value);
				this.synt[i].set(\damp, num.value);
				dm[i] = num.value;
				dmboxProxy[i].value = num.value;
				if (guiflag) {
					dmbox[i].value = num.value;
					if (i == currentsource) {
						{dmslider.value = num.value}.defer;
						{dmnumbox.value = num.value}.defer;
					};
				};
			});



			this.ossiaangle[i] = OSSIA_Parameter(this.ossiasrc[i], "Angle_(stereo)", Float,
				[0, pi], 1.05, 'clip',
				critical:allCrtitical, repetition_filter:true);

			this.ossiaangle[i].callback_({arg num;
				angle[i] = num.value;
				if((this.ncanais[i]==2) || (this.ncan[i]==2)){
					this.espacializador[i].set(\angle, num.value);
					this.setSynths(i, \angle, num.value);
					angle[i] = num.value;
				};
				aboxProxy[i].value = num.value;
				if (guiflag) {
					{abox[i].value = num.value}.defer;
					if(i == currentsource)
					{
						{angnumbox.value = num.value}.defer;
						{angslider.value = num.value / pi}.defer;
					};
				};
			});



			this.ossiarot[i] = OSSIA_Parameter(this.ossiasrc[i], "Rotation_(B-Format)", Float,
				[-pi, pi], 0, 'wrap',
				critical:allCrtitical, repetition_filter:true);

			this.ossiarot[i].callback_({arg num;
				this.synt[i].set(\rotAngle, num.value);
				this.setSynths(i, \rotAngle, num.value);
				rlev[i] = num.value;
				rboxProxy[i].value = num.value;
				if (guiflag) {
					{rbox[i].value = num.value}.defer;
					if(i == currentsource)
					{
						{rslider.value = (num.value + pi) / 2pi}.defer;
						{rnumbox.value = num.value}.defer;
					};
				};
			});



			this.ossiadir[i] = OSSIA_Parameter(this.ossiasrc[i], "Diretivity_(B-Format)", Float,
				[0, pi * 0.5], 0, 'clip',
				critical:allCrtitical, repetition_filter:true);

			this.ossiadir[i].callback_({arg num;
				this.synt[i].set(\directang, num.value);
				this.setSynths(i, \directang, num.value);
				dlev[i] = num.value;
				dboxProxy[i].value = num.value;
				if (guiflag) {
					{dbox[i].value = num.value}.defer;
					if(i == currentsource)
					{
						{dirslider.value = num.value / (pi/2)}.defer;
						{dirnumbox.value = num.value}.defer;
					};
				};
			});



			this.ossiactr[i] = OSSIA_Parameter(this.ossiasrc[i], "Contraction(ATK & B-Format)", Float,
				[0, 1], 1, 'clip',
				critical:allCrtitical, repetition_filter:true);

			this.ossiactr[i].callback_({arg num;
				this.synt[i].set(\contr, num.value);
				this.espacializador[i].set(\contr, num.value);
				this.setSynths(i, \contr, num.value);
				clev[i] = num.value;
				cboxProxy[i].value = num.value;
				if (guiflag) {
					{cbox[i].value = num.value}.defer;
					if (i == currentsource)
					{
						{cslider.value = num.value}.defer;
						{connumbox.value = num.value}.defer;
					};
				};
			});

		});

	}

}