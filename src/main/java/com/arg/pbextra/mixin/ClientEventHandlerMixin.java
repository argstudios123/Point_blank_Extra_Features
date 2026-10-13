package com.arg.pbextra.mixin;

import com.arg.pbextra.client.ToggleAimingController;
import com.vicmatskiv.pointblank.client.ClientEventHandler;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;


/**
 * Feeds the {@link ToggleAimingController} state machine into Point Blank's per-tick
 * right-mouse state.
 *
 * <p>All of the toggle logic lives in {@link ToggleAimingController}; this class is only
 * the injection point. Point Blank computes the right-mouse state once per client tick in
 * {@code ClientEventHandler#onClientTick} and immediately uses it for the ADS
 * press/release edge detection and for {@code syncSingleGunAiming}. Replacing that single
 * value therefore makes Point Blank treat an active toggle exactly like a held button,
 * while leaving every other weapon, all guards and all native ADS transitions untouched.</p>
 *
 * <p>Vic's Point Blank 2.2.0 for NeoForge 26.1.2 stores that boolean in local variable
 * slot 14 of {@code onClientTick}, assigned straight from
 * {@code isMouseButtonDown(mc, mc.options.keyUse.isDown(), 1)}. The slot can be
 * re-verified with {@code javap -p -c -l} on the Point Blank jar: the
 * {@code LocalVariableTable} of {@code onClientTick} names slot 14
 * {@code rightMouseButtonDown} and slot 20 is the unrelated
 * {@code queuedVanillaToGunDraw} flag (that slot belonged to the right-mouse boolean in
 * the 1.21.1 build). Targeting a slot that does not exist is a hard injection failure, but
 * a wrong slot holding another boolean fails silently, so re-check the table whenever the
 * dependency is updated.</p>
 */
@Mixin(value = ClientEventHandler.class, remap = false)
public abstract class ClientEventHandlerMixin {

    @ModifyVariable(
            method = "onClientTick",
            at = @At("STORE"),
            index = 14,
            remap = false
    )
    private boolean pointblankextra$applyToggleAiming(boolean physicalState) {
        return ToggleAimingController.resolveRightMouseState(
                Minecraft.getInstance().player, physicalState);
    }
}
