package com.argos.pbextra.mixin;

import com.argos.pbextra.constraint.AdsConstraintManager;
import com.vicmatskiv.pointblank.client.controller.BlendingAnimationProcessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.model.CoreGeoModel;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationState;


@Mixin(value = BlendingAnimationProcessor.class, remap = false)
public abstract class AdsConstraintProcessorMixin {

    @Inject(method = "tickAnimation", at = @At("RETURN"), remap = false)
    private void pointblankextra$applyAdsConstraint(
            GeoAnimatable animatable,
            CoreGeoModel<?> model,
            AnimatableManager<?> animatableManager,
            double animTime,
            AnimationState<?> state,
            boolean crashWhenCantFindBone,
            CallbackInfo ci) {

        AdsConstraintManager.apply((BlendingAnimationProcessor<?>) (Object) this, state);
    }
}
