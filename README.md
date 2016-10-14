# Mosca
Mosca is a SuperCollider class for GUI-assisted production of ambisonic sound fields with simulated moving or stationary sound sources. Soundfields may be decoded using a variety of built in 1st order ambisonic SuperCollider decoders (including binaural) or with external 2nd order decoders.

Mosca is written by Iain Mott, 2016 and licensed under a Creative Commons Attribution-NonCommercial 4.0 International License http://creativecommons.org/licenses/by-nc/4.0/

The class makes extensive use of the Ambisonic Toolkit (ATK, see: http://www.ambisonictoolkit.net/) by Joseph Anderson and the Automation quark (https://github.com/neeels/Automation) by Neels Hofmeyr. Required Quarks : Automation, Ctk and MathLib. It is necessary to install the SC Plugins to use yhe ATK: https://github.com/supercollider/sc3-plugins

User must set up a project directory with subdirectories "rir" and "auto". RIRs should have the first 100 or 120ms silenced to act as "tail" reverberator and must be placed in the "rir" directory. Run help on the "Mosca" class in SuperCollider for detailed information and code examples.

For further information and example RIRs and B-format recordings: http://escuta.org/mosca