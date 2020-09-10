MoscaUtils { // virtual class holding constants for Mosca related classes

	*plim { ^1.2; } // distance limit from origin where processes continue to run

	*halfPi { ^1.5707963267949; }

	*rad2deg { ^57.295779513082; }

	*n2f { ^FoaEncoderMatrix.newHoa1(); }

	*fourOrNine { | order |
		if (order > 1) {
			^9;
		} {
			^4;
		};
	}

	*bfOrFmh { | order |
		if (order > 1) {
			^FMHEncode1;
		} {
			^BFEncode1;
		};
	}
}