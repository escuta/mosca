# Mosca
Mosca is a SuperCollider class for GUI-assisted production of ambisonic and binaural sound fields with simulated moving or stationary sound sources.

Written by Iain Mott, 2016. Licensed under a Creative Commons Attribution-NonCommercial 4.0 International License http://creativecommons.org/licenses/by-nc/4.0/

The class makes extensive use of the Ambisonic Toolkit (http://www.ambisonictoolkit.net/) by Joseph Anderson and the Automation quark (https://github.com/neeels/Automation) by Neels Hofmeyr. Required Quarks : Automation, Ctk, XML, MathLib. It is necessary to install the SC Plugins: https://github.com/supercollider/sc3-plugins

User must set up a project directory with subdirectoties "rir" and "auto". RIRs should have the first 100 or 120ms silenced to act as "tail" reverberators and must be placed in the "rir" directory. Run help on the "Mosca" class in SuperCollider for detailed information.

For more information and example RIRs and B-format recordings: http://escuta.org/mosca