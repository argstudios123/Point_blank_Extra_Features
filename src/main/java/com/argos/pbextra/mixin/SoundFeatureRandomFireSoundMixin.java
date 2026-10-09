package com.argos.pbextra.mixin;

import com.mojang.datafixers.util.Pair;
import com.vicmatskiv.pointblank.feature.ConditionContext;
import com.vicmatskiv.pointblank.feature.Features;
import com.vicmatskiv.pointblank.feature.SoundFeature;
import com.vicmatskiv.pointblank.util.Conditions;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;



@Mixin(value = SoundFeature.class, remap = false)
public abstract class SoundFeatureRandomFireSoundMixin {

    @Inject(method = "getFireSoundAndVolume", at = @At("HEAD"), cancellable = true, remap = false)
    private static void pointblankextra$pickRandomFireSound(
            ItemStack itemStack, CallbackInfoReturnable<SoundFeature.SoundDescriptor> cir) {

        List<Features.EnabledFeature> enabledFeatures =
                Features.getEnabledFeatures(itemStack, SoundFeature.class);

        // Weapons that never declare a random fire sound are left entirely to Point
        // Blank, so this mixin changes nothing for them.
        boolean hasRandomFireSound = false;
        for (Features.EnabledFeature enabledFeature : enabledFeatures) {
            if (pointblankextra$hasRandomFireSound((SoundFeature) enabledFeature.feature())) {
                hasRandomFireSound = true;
                break;
            }
        }
        if (!hasRandomFireSound) {
            return;
        }

        for (Features.EnabledFeature enabledFeature : enabledFeatures) {
            SoundFeature soundFeature = (SoundFeature) enabledFeature.feature();
            List<Pair<SoundFeature.SoundDescriptor, Predicate<ConditionContext>>> fireSounds =
                    ((SoundFeatureAccessor) soundFeature).pointblankextra$getFireSounds();

            int randomFireSoundCount = 0;
            for (Pair<SoundFeature.SoundDescriptor, Predicate<ConditionContext>> fireSound : fireSounds) {
                if (fireSound.getSecond() == Conditions.RANDOM_PICK) {
                    ++randomFireSoundCount;
                }
            }
            int randomValue = randomFireSoundCount > 0
                    ? ThreadLocalRandom.current().nextInt(randomFireSoundCount)
                    : 0;

            // The context Point Blank itself tests non-random conditions with.
            ConditionContext context = new ConditionContext(itemStack);
            int randomIndex = 0;
            for (Pair<SoundFeature.SoundDescriptor, Predicate<ConditionContext>> fireSound : fireSounds) {
                Predicate<ConditionContext> condition = fireSound.getSecond();
                ConditionContext fireSoundContext;
                if (condition == Conditions.RANDOM_PICK) {
                    // RANDOM_PICK compares the two samples, so the drawn index and the
                    // position of this sound among the random ones select exactly one of
                    // them per fire event.
                    fireSoundContext = new ConditionContext(
                            null, itemStack, itemStack, null, null, randomValue, randomIndex);
                    ++randomIndex;
                } else {
                    fireSoundContext = context;
                }

                if (!condition.test(fireSoundContext)) {
                    continue;
                }

                cir.setReturnValue(fireSound.getFirst());
                return;
            }
        }
    }

    private static boolean pointblankextra$hasRandomFireSound(SoundFeature soundFeature) {
        for (Pair<SoundFeature.SoundDescriptor, Predicate<ConditionContext>> fireSound
                : ((SoundFeatureAccessor) soundFeature).pointblankextra$getFireSounds()) {
            if (fireSound.getSecond() == Conditions.RANDOM_PICK) {
                return true;
            }
        }
        return false;
    }
}
