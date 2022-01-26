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
//                 NOREVERB                  //
//-------------------------------------------//

ClearDef : EffectDef
{
	classvar <key; // class access

	*initClass { key = "Clear"; }

	// keep this one virtual
	*new1stOrder { }
	*new2ndOrder { }

	*getFunc { | nChanns | ^{ | lrevRef, p, rad, llev | }}

	*getArgs { | parentOssiaNode, nChan = 1 | ^[] }

	*getGlobalArgs { | parentOssiaNode, nChan = 4 | ^[] }
}
