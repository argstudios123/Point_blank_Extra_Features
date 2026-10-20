package com.arg.pbextra.mixin;

import com.arg.pbextra.animation.AnimationLibraryRegistry;
import com.google.gson.JsonObject;
import com.vicmatskiv.pointblank.item.GunItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


/**
 * Lets {@code firstPersonFallbackAnimations} keep accepting its existing string
 * form while also accepting an array of reusable animation library paths.
 * <p>
 * Point Blank's builder reads the field with {@code JsonUtil.getJsonString},
 * which cannot read a JSON array. Capturing and removing the array here keeps
 * the native parser safe. The actual fallback identifiers are appended by
 * {@link GunGeoModelAnimationLibrariesMixin}.
 */
@Mixin(targets = "com.vicmatskiv.pointblank.item.GunItem$Builder", remap = false)
public abstract class GunItemBuilderAnimationLibrariesMixin {

    @Inject(
            method = "withJsonObject(Lcom/google/gson/JsonObject;Z)"
                    + "Lcom/vicmatskiv/pointblank/item/GunItem$Builder;",
            at = @At("HEAD"),
            remap = false
    )
    private void pbextra$captureAnimationLibraries(JsonObject json, boolean flag,
                                                   CallbackInfoReturnable<GunItem.Builder> cir) {
        AnimationLibraryRegistry.captureBuilderJson(json);
    }
}
