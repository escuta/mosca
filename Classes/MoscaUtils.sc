MoscaUtils // virtual class holding constants for Mosca related classes
{
	classvar palette, cart = #[
		0.850650808352E+00,
		0,
		-0.525731112119E+00,
		0.525731112119E+00,
		-0.850650808352E+00,
		0.000000000000E+00,
		0,
		-0.525731112119E+00,
		0.850650808352E+00,
		0.850650808352E+00,
		0,
		0.525731112119E+00,
		-0.525731112119E+00,
		-0.850650808352E+00,
		0,
		0,
		0.525731112119E+00,
		-0.850650808352E+00,
		-0.850650808352E+00,
		0,
		-0.525731112119E+00,
		-0.525731112119E+00,
		0.850650808352E+00,
		0,
		0,
		0.525731112119E+00,
		0.850650808352E+00,
		-0.850650808352E+00,
		0,
		0.525731112119E+00,
		0.525731112119E+00,
		0.850650808352E+00,
		0,
		0,
		-0.525731112119E+00,
		-0.850650808352E+00
	];

	*palette
	{
		if (palette.isNil)
		{
			palette = QPalette.auto(Color.fromHexString("#1d1c1a"), Color.new255( 200, 200, 200 ));

			palette.setColor(Color.new255( 200, 200, 200 ), 'window');
			palette.setColor(Color.fromHexString("#c0c0c0c0"), 'windowText');
			palette.setColor(Color.fromHexString("#222222"), 'button');
			palette.setColor(Color.fromHexString("#f0f0f0"), 'buttonText');
			palette.setColor(Color.fromHexString("#161514"), 'base');
			palette.setColor(Color.fromHexString("#1e1d1c"), 'alternateBase');
			palette.setColor(Color.fromHexString("#161514"), 'toolTipBase');
			palette.setColor(Color.fromHexString("#c0c0c0c0"), 'toolTipText');
			palette.setColor(Color.fromHexString("#9062400a"), 'highlight');
			palette.setColor(Color.fromHexString("#FDFDFD"), 'highlightText');

			palette.setColor(Color.new255(179, 90, 209, 255), 'light'); // welow slider
			palette.setColor(Color.new255(37, 41, 48, 40), 'midlight'); // brown contour
			palette.setColor(Color.new255(0, 127, 229, 76), 'middark'); // widget background
			palette.setColor(Color.white, 'baseText'); // green param
			palette.setColor(Color.fromHexString("#c58014"), 'brightText');
		};

		^palette;
	}

	*plim { ^1.1 } // distance limit from origin where processes continue to run

	*halfPi { ^1.5707963267949 }

	*rad2deg { ^57.295779513082 }

	*deg2rad { ^1.7453292519943 }

	*hoaChanns { ^[9, 16, 25] }

	*channels { ^[1, 2, 4] ++ this.hoaChanns() }

	*fftSize { ^2048 }

	*n2f { | sig | ^FoaEncoderMatrix.newHoa1() }

	*b2a { | sig | ^FoaDecoderMatrix.newBtoA() }

	*a2b { | sig | ^FoaEncoderMatrix.newAtoB() }

	*f2n { | sig | ^FoaDecoderMatrix.newHoa1() }

	*fourOrNine { | order | if (order > 1) { ^9 } { ^4 } }

	*fourOrTwelve { | order | if (order > 1) { ^12 } { ^4 } }

	// a-12 decoder matrix
	*soa_a12_decoder_matrix
	{
		^Matrix.with([
			[ 0.11785113, 0.212662702, 0, -0.131432778, -0.0355875819, -0.279508497, 0,
				0.226127124, 0 ],
			[ 0.11785113, 0.131432778, -0.212662702, 0, -0.208333333, 0, 0, -0.139754249,
				-0.279508497 ],
			[ 0.11785113, 0, -0.131432778, 0.212662702, 0.243920915, 0, -0.279508497,
				-0.0863728757, 0 ],
			[ 0.11785113, 0.212662702, 0, 0.131432778, -0.0355875819, 0.279508497, 0,
				0.226127124, 0 ],
			[ 0.11785113, -0.131432778, -0.212662702, 0, -0.208333333, 0, 0, -0.139754249,
				0.279508497 ],
			[ 0.11785113, 0, 0.131432778, -0.212662702, 0.243920915, 0, -0.279508497,
				-0.0863728757, 0 ],
			[ 0.11785113, -0.212662702, 0, -0.131432778, -0.0355875819, 0.279508497, 0,
				0.226127124, 0 ],
			[ 0.11785113, -0.131432778, 0.212662702, 0, -0.208333333, 0, 0, -0.139754249,
				-0.279508497 ],
			[ 0.11785113, 0, 0.131432778, 0.212662702, 0.243920915, 0, 0.279508497,
				-0.0863728757, 0 ],
			[ 0.11785113, -0.212662702, 0, 0.131432778, -0.0355875819, -0.279508497, 0,
				0.226127124, 0 ],
			[ 0.11785113, 0.131432778, 0.212662702, 0, -0.208333333, 0, 0, -0.139754249,
				0.279508497 ],
			[ 0.11785113, 0, -0.131432778, -0.212662702, 0.243920915, 0, 0.279508497,
				-0.0863728757, 0 ]
		]);
	}

	// a-12 encoder matrix
	*soa_a12_encoder_matrix
	{
		^Matrix.with([
			[ 0.707106781, 0.707106781, 0.707106781, 0.707106781,
				0.707106781, 0.707106781, 0.707106781, 0.707106781,
				0.707106781, 0.707106781, 0.707106781, 0.707106781 ],
			[ 0.850650808, 0.525731112, 0, 0.850650808, -0.525731112,
				0, -0.850650808, -0.525731112, 0,
				-0.850650808, 0.525731112, 0 ],
			[ 0, -0.850650808, -0.525731112, 0,
				-0.850650808, 0.525731112, 0, 0.850650808,
				0.525731112, 0, 0.850650808, -0.525731112 ],
			[ -0.525731112, 0, 0.850650808, 0.525731112,
				0, -0.850650808, -0.525731112, 0,
				0.850650808, 0.525731112, 0, -0.850650808 ],
			[ -0.0854101966, -0.5, 0.585410197, -0.0854101966,
				-0.5, 0.585410197, -0.0854101966, -0.5,
				0.585410197, -0.0854101966, -0.5, 0.585410197 ],
			[ -0.894427191, 0, 0, 0.894427191,
				0, 0, 0.894427191, 0,
				0, -0.894427191, 0, 0 ],
			[ 0, 0, -0.894427191, 0,
				0, -0.894427191, 0, 0,
				0.894427191, 0, 0, 0.894427191 ],
			[ 0.723606798, -0.447213596, -0.276393202, 0.723606798,
				-0.447213596, -0.276393202, 0.723606798, -0.447213596,
				-0.276393202, 0.723606798, -0.447213596, -0.276393202 ],
			[ 0, -0.894427191, 0, 0,
				0.894427191, 0, 0, -0.894427191,
				0, 0, 0.894427191, 0 ]
		]);
	}

	// 1st-order FuMa-MaxN decoder
	*foa_a12_decoder_matrix
	{
		var spher;

		// convert to angles -- use these directions
		spher = cart.clump(3).collect({ | cart, i |
			cart.asCartesian.asSpherical.angles;
		});

		^FoaEncoderMatrix.newDirections(spher).matrix.pseudoInverse;
	}

	// 1st-order N3D encoder
	*foa_n3d_encoder
	{
		^4.collect({ | i |
			var sphe = cart.clump(3)[i].asCartesian.asSpherical;
			HOASphericalHarmonics.coefN3D(1, sphe.theta(), sphe.phi());
		});
	}

	// 2nd-order N3D encoder
	*soa_n3d_encoder
	{
		^cart.clump(3).collect({ | cart |
			var sphe = cart.asCartesian.asSpherical;
			HOASphericalHarmonics.coefN3D(2, sphe.theta(), sphe.phi());
		});
	}

	*emulate_array
	{
		^[ [ 0, 90 ], [ 0, 45 ], [ 90, 45 ], [ 180, 45 ], [ -90, 45 ],
		[ 45, 35 ], [ 135, 35 ], [ -135, 35 ], [ -45, 35 ], [ 0, 0 ], [ 45, 0 ],
		[ 90, 0 ], [ 135, 0 ], [ 180, 0 ], [ -135, 0 ], [ -90, 0 ], [ -45, 0 ],
		[ 45, -35 ], [ 135, -35 ], [ -135, -35 ], [ -45, -35 ], [ 0, -45 ],
		[ 90, -45 ], [ 180, -45 ], [ -90, -45 ], [ 0, -90 ] ];
	*cartesianToAED
	{
		arg coordinatesList;
		var result;
		result = coordinatesList.collect({
			arg item,i;
			Cartesian(item[0],item[1],item[2]).asSpherical.rotate(pi/2)
		});
		result = result.collect({
			arg item,i;
			[item.theta.raddeg,item.phi.raddeg,item.rho]
		});
		^result;
	}
	*AEDToCartesian
	{
		arg coordinatesList;
		var result;
		result = coordinatesList.collect({
			arg item,i;
			Spherical(item[2], item[0].degrad,item[1].degrad).rotate(-pi/2).asCartesian.trunc(0.00000001)
		});
		^result;
	}

	*cartesianToSpherical
	{
		arg coordinatesList;
		var result;
		result = coordinatesList.collect({
			arg item,i;
			Cartesian(item[0],item[1],item[2]).asSpherical
		});
		^result;
	}
	*SphericalToCartesian
	{
		arg coordinatesList;
		var result;
		result = coordinatesList.collect({
			arg item,i;
			Spherical(item[0], item[1],item[2]).asCartesian.trunc(0.00000001)
		});
		^result;
	}
}
