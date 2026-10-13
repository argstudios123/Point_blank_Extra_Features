package com.arg.pbextra.mixin;

import com.arg.pbextra.constraint.AdsConstraintManager;
import com.vicmatskiv.pointblank.client.render.GunItemRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.RenderPassInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


/**
 * Point Blank 26.1.2 no longer animates bones through the old
 * {@code BlendingAnimationProcessor#tickAnimation} override: GeckoLib 5 computes a
 * {@link BoneSnapshots} set per render pass and Point Blank adjusts it in
 * {@code GunItemRenderer#adjustModelBonesForRender}.
 *
 * <p>The constraint is injected immediately before the first
 * {@code RenderPassInfo#model()} access inside that method, which is exactly the point
 * where Point Blank reads the {@code "_camera_"} bone to build its first-person camera
 * animation. Injecting there means the constraint corrects the snapshots the renderer
 * (and Point Blank's camera) is about to use, which is what the old
 * {@code at = RETURN} hook on {@code tickAnimation} achieved.</p>
 */
@Mixin(value = GunItemRenderer.class, remap = false)
public abstract class AdsConstraintProcessorMixin {

    @Inject(
            method = "adjustModelBonesForRender",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/geckolib/renderer/base/RenderPassInfo;model()Lcom/geckolib/cache/model/BakedGeoModel;",
                    ordinal = 0
            ),
            remap = false
    )
    private void pointblankextra$applyAdsConstraint(
            RenderPassInfo<?> renderPassInfo,
            BoneSnapshots snapshots,
            CallbackInfo ci) {

        AdsConstraintManager.apply(renderPassInfo, snapshots);
    }
}
