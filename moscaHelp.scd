  (
	s.waitForBoot {
		~testMosca = Mosca.new("/home/iain/projects/ambisonics/chowning/ir/QL14_tail48kHz.amb", 
		"/home/iain/projects/ambisonics/chowning/ir/sbs_binaural_tail.wav", 21, Server.default);
		
	};
) 
