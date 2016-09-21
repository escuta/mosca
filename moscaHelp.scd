// To use mosca, you first need to create a project directory. Within it include two further directories named "rir" and "auto". The rir directory must contain at least one b-format rir and one binaural rir. The auto directory is empty until you begin to save automation data from the Mosca GUI. For convenience, please download the "moscaproject.zip" file on the following page which contains the file structure, example RIRs and B-format recordings: http://escuta.org/mosca

// Please see the Help File for the Mosca class

// Please see 

(
	s.waitForBoot {
		~testMosca = Mosca.new("/home/iain/projects/ambisonics/moscaproject",
			"QL14_tail48kHz.amb", "sbs_binaural_tail.wav", 21, Server.local);
	};
)