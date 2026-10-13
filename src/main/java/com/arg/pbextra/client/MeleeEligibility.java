package com.arg.pbextra.client;

import com.mojang.logging.LogUtils;
import com.vicmatskiv.pointblank.client.GunClientState;
import net.minecraft.client.player.LocalPlayer;
import org.slf4j.Logger;


public final class MeleeEligibility {

    private static final Logger LOGGER = LogUtils.getLogger();

    private MeleeEligibility() {
    }


    public static boolean isMeleeAllowed(LocalPlayer player) {
        return check(player, GunClientState.getMainHeldState(player));
    }


    public static boolean isMeleeAllowed(LocalPlayer player, boolean offhand) {
        GunClientState state = offhand
                ? offhandState(player)
                : GunClientState.getMainHeldState(player);
        return check(player, state);
    }

    /**
     * Point Blank 26.1.2 exposes the offhand state through the offhand gun context
     * rather than a dedicated {@code getOffhandState} helper.
     */
    private static GunClientState offhandState(LocalPlayer player) {
        com.vicmatskiv.pointblank.item.GunItem.OperableGunContext context =
                com.vicmatskiv.pointblank.item.GunItem.resolveOffhandGunContext(player);
        if (context == null) {
            return null;
        }
        return GunClientState.getState(player, context.itemStack(), context.slotIndex(),
                context.offhand());
    }

    private static boolean check(LocalPlayer player, GunClientState state) {
        String blockedBy = blockedBy(player, state);
        if (blockedBy != null) {
            LOGGER.debug("Melee input ignored: {}", blockedBy);
            return false;
        }
        return true;
    }

    private static String blockedBy(LocalPlayer player, GunClientState state) {
        if (player.isSprinting()) {
            return "the player is sprinting";
        }
        if (state == null) {
            return null;
        }
        if (state.isDrawing()) {
            return "the weapon is being drawn";
        }
        if (state.isAiming()) {
            return "the weapon is aiming";
        }
        if (state.isFiring()) {
            return "the weapon is firing";
        }
        if (state.isReloading()) {
            return "the weapon is reloading";
        }
        if (state.isInspecting()) {
            return "the weapon is being inspected";
        }
        if (state.isHiding() || state.isHidden()) {
            return "the weapon is being holstered";
        }
        if (state.isChangingFireMode()) {
            return "the fire mode is being changed";
        }
        if (state.isOverheating()) {
            return "the weapon is overheating";
        }
        if (!state.isIdle()) {
            return "the weapon is in state " + state.getFireState();
        }
        return null;
    }
}
