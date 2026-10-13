package com.arg.pbextra.mixin;

import com.arg.pbextra.client.AdsSwitchBlend;
import com.vicmatskiv.pointblank.client.BiDirectionalInterpolator;
import com.vicmatskiv.pointblank.client.ClientEventHandler;
import com.vicmatskiv.pointblank.event.ViewportEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


/**
 * Bridges the ADS zoom across a weapon switch (see {@link AdsSwitchBlend}).
 *
 * <p>Point Blank's {@code onFovUpdate} reads the aiming progress of the currently held
 * weapon exactly once:</p>
 *
 * <pre>
 * gunClientState.getAnimationController("aiming")   // BiDirectionalInterpolator
 *         .getValue()                               // &lt;- redirected here
 * </pre>
 *
 * <p>and multiplies it into the zoom that is written into the FOV event. That single call
 * site is the only place where the outgoing and incoming weapons' aiming progress could be
 * blended, so the redirect is deliberately narrow: it wraps that one invocation, calls the
 * original getter and passes the result through the bridge. No aiming state, no packets, no
 * animation data and no camera rotation is touched, and nothing outside this method is
 * affected.</p>
 */
@Mixin(value = ClientEventHandler.class, remap = false)
public abstract class AdsSwitchFovMixin {

    @Redirect(
            method = "onFovUpdate",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/vicmatskiv/pointblank/client/BiDirectionalInterpolator;getValue()D"
            ),
            remap = false
    )
    private double pointblankextra$blendAimingProgress(BiDirectionalInterpolator aimingController) {
        return AdsSwitchBlend.blend((float) aimingController.getValue());
    }

    /**
     * Covers the early return of the same method.
     *
     * <p>When the main-hand item is not a gun (empty slot, block, tool, food, ...),
     * {@code onFovUpdate} returns before it reads any aiming progress and never writes the
     * FOV, so the redirect above is never reached and the event keeps the vanilla FOV - the
     * zoom would snap back in a single frame. Point Blank's method has several returns, so
     * this injection sits at every one of them; the bridge itself decides whether it has
     * anything to do, writing the event only while a fade it owns is running, so the FOV is
     * never written twice in the same pass.</p>
     */
    @Inject(
            method = "onFovUpdate",
            at = @At("RETURN"),
            remap = false
    )
    private void pointblankextra$bridgeFovWhenNoGun(ViewportEvent.ComputeFov event, CallbackInfo ci) {
        double bridged = AdsSwitchBlend.bridgeFov(event.useFovSetting(), event.getFOV());
        if (bridged != event.getFOV()) {
            event.setFOV(bridged);
        }
    }
}
