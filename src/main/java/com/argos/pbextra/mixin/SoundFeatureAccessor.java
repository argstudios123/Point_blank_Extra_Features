package com.argos.pbextra.mixin;

import com.mojang.datafixers.util.Pair;
import com.vicmatskiv.pointblank.feature.ConditionContext;
import com.vicmatskiv.pointblank.feature.SoundFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;
import java.util.function.Predicate;



@Mixin(value = SoundFeature.class, remap = false)
public interface SoundFeatureAccessor {

    @Accessor("fireSounds")
    List<Pair<SoundFeature.SoundDescriptor, Predicate<ConditionContext>>> pointblankextra$getFireSounds();
}
