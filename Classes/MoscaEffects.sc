// Replace the setAction method in MoscaEffects.sc with this improved version

setAction
{
	var currentEffectType = nil; // Track what effect is currently loaded
	
	ossiaGlobal.action_({ | num |
		var newEffectType = num.value;
		
		this.changed(\ctl);
		
		// Check if we're actually changing the effect TYPE (not just parameters)
		if (currentEffectType != newEffectType) {
			
			// We need to switch effects
			if (globalFx.isPlaying) { 
				var oldFx = globalFx;
				globalFx = nil;
				
				oldFx.onFree({
					if (newEffectType != "Clear") {
						this.prCreateGlobalFx(newEffectType);
						currentEffectType = newEffectType;
					} {
						currentEffectType = nil;
					};
				});
				
				oldFx.set(\gate, 0);
			} {
				// No previous synth - create immediately if not "Clear"
				if (newEffectType != "Clear") {
					this.prCreateGlobalFx(newEffectType);
					currentEffectType = newEffectType;
				} {
					currentEffectType = nil;
				};
			};
		} {
			// Same effect type - just update parameters without recreating
			if (globalFx.isPlaying && (newEffectType != "Clear")) {
				var instance = effectInstances.get.at(newEffectType.asSymbol);
				var args = instance.getGlobalArgs(ossiaGlobal.node);
				globalFx.set(*args.flat); // Update parameters only
				("Updating " ++ newEffectType ++ " parameters without restart").postln;
			};
		};
	});

	ossiaDelay.action_({ | num |
		if (globalFx.isPlaying) { 
			globalFx.set(\room, num.value);
		};
	});

	ossiaDecay.action_({ | num | 
		if (globalFx.isPlaying) { 
			globalFx.set(\damp, num.value);
		};
	});
}

// Add this new helper method to MoscaEffects class
prCreateGlobalFx
{ | effectName |
	var instance = effectInstances.get.at(effectName.asSymbol);
	var args = instance.getGlobalArgs(ossiaGlobal.node);
	
	// Create b2Fx converter if needed
	if (b2Fx.isNil) {
		b2Fx = Synth(\b2Fx, target: transformGrp, addAction: \addBefore)
		.onFree({ b2Fx = nil });
	};
	
	// Create the global effect synth
	globalFx = Synth(\globalFx ++ instance.key,
		[\gate, 1] ++ args, transformGrp).register.onFree({
			if (globalFx.isPlaying.not) { b2Fx.free };
		}
	);
	
	("Created global effect: " ++ effectName).postln;
}
