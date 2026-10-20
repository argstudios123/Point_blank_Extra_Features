package com.arg.pbextra.mixin;

import com.arg.pbextra.constraint.AdsConstraintManager;
import com.vicmatskiv.pointblank.client.render.GunItemRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.RenderPassInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;



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
