package com.arg.pbextra.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
 * Prevents an active muzzle-flash list from being modified while it is being
 * iterated on the render thread.
 * <p>
 * {@code GunClientState#getMuzzleFlashEffects()} returns a live view of its
 * backing {@link ArrayList}. Point Blank adds/removes muzzle-flash effects from
 * its state-ticker thread, while {@code MuzzleFlashItemLayer} iterates that
 * view on the render thread. Under sustained/dual-wield fire this produced
 * {@link java.util.ConcurrentModificationException} inside
 * {@code renderMuzzleFlashForBone}, which aborts GeckoLib's render pass for that
 * frame (the gun and hands visibly flicker/snap).
 * <p>
 * Iterating over a copy keeps the render pass deterministic without changing
 * Point Blank's effect lifecycle.
 */
@Mixin(targets = "com.vicmatskiv.pointblank.client.render.layer.MuzzleFlashItemLayer", remap = false)
public abstract class MuzzleFlashConcurrencyMixin {

    @Redirect(
            method = "renderMuzzleFlashForBone",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/List;iterator()Ljava/util/Iterator;"
            ),
            remap = false
    )
    private Iterator<?> pbextra$iterateMuzzleFlashSnapshot(List<?> effects) {
        return new ArrayList<>(effects).iterator();
    }
}
