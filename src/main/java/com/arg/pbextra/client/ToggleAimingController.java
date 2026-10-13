package com.arg.pbextra.client;

import com.arg.pbextra.config.PointBlankExtraConfig;
import com.arg.pbextra.data.WeaponExtraDataManager;
import com.vicmatskiv.pointblank.Platform;
import com.vicmatskiv.pointblank.client.GunClientState;
import com.vicmatskiv.pointblank.item.GunItem;
import com.vicmatskiv.pointblank.network.AimingChangeRequestPacket;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/**
 * Client-side state machine behind the per-weapon {@code "toggle_aiming"} option.
 *
 * <p>Point Blank 2.2.0 derives aiming from a single "right mouse button is down" boolean
 * that {@code ClientEventHandler#onClientTick} computes once per client tick. That one
 * value feeds three things: the press/release edge detection that starts and ends ADS
 * ({@code rightMouseButtonDown()} / {@code rightMouseButtonRelease()}), the per-tick
 * {@code syncSingleGunAiming(player, boolean)} call, and the {@code rightMouseButtonDown}
 * field Point Blank keeps for its own edge detection.</p>
 *
 * <p>The toggle therefore does not add its own input handling: it only substitutes the
 * value Point Blank sees, so every native ADS transition, guard and interpolation keeps
 * working. Turning the toggle on looks exactly like the player holding the button;
 * turning it off looks exactly like a release.</p>
 *
 * <p>The toggle belongs to one specific weapon state (the {@link GunClientState} of the
 * gun that was aimed). As soon as the player holds another weapon the toggle is dropped
 * and the weapon that was aimed is taken out of ADS through Point Blank's own aiming-off
 * transition - {@code setAiming(false)} followed by an {@link AimingChangeRequestPacket} -
 * which is the same two steps Point Blank itself performs in
 * {@code GunHandSwapCoordinator#prepareOutgoingStateForSwap} and in
 * {@code ClientEventHandler#toggleAiming}. Ending the ADS on the weapon that is being
 * holstered lets Point Blank's aiming interpolator retract its ADS pose during the
 * holster/draw animation, instead of leaving it latched at full ADS and cutting to the
 * new weapon's hip pose.</p>
 */
public final class ToggleAimingController {

    private ToggleAimingController() {
    }

    /** Entity id of the player the current toggle belongs to. */
    private static int ownerPlayerId = -1;

    /** {@link GunClientState#getId()} of the weapon the current toggle belongs to. */
    private static UUID ownerStateId;

    /** Inventory slot the aimed weapon is held in (needed for the server side packet). */
    private static int ownerSlot = -1;

    private static boolean ownerOffhand;

    /** Whether the player toggled ADS on for {@link #ownerStateId}. */
    private static boolean toggled;

    /** Physical right-mouse state observed on the previous tick (for press detection). */
    private static boolean previousPhysical;

    /**
     * Replaces the right-mouse state Point Blank is about to use for this tick.
     *
     * @param player        the client player, may be {@code null} while no world is loaded
     * @param physicalState the raw right-mouse state Point Blank computed itself
     * @return the state Point Blank must use instead; the toggle state for toggle-aiming
     *         weapons, {@code physicalState} unchanged for everything else
     */
    public static boolean resolveRightMouseState(LocalPlayer player, boolean physicalState) {
        if (!PointBlankExtraConfig.ENABLE_MOD.get() || player == null) {
            clear();
            return physicalState;
        }

        // A different player entity means a new session; never carry state over.
        if (player.getId() != ownerPlayerId) {
            clear();
        }

        // Point Blank handles dual-wield aiming entirely on its own.
        if (GunItem.hasGunsInMainAndAltSlots(player)) {
            releaseOwnerAiming();
            clear();
            return physicalState;
        }

        GunItem.OperableGunContext context = GunItem.resolveMainHandGunContext(player);
        ItemStack stack = context == null ? ItemStack.EMPTY : context.itemStack();
        if (stack.isEmpty()
                || !(stack.getItem() instanceof GunItem gun)
                || !gun.isAimingEnabled()
                || !WeaponExtraDataManager.isToggleAiming(stack)) {
            // Ordinary weapons, non-guns and toggle-disabled weapons keep native input.
            releaseOwnerAiming();
            clear();
            return physicalState;
        }

        GunClientState state = GunClientState.getState(
                player, stack, context.slotIndex(), context.offhand());
        UUID stateId = state == null ? null : state.getId();
        if (stateId == null) {
            releaseOwnerAiming();
            clear();
            return physicalState;
        }

        if (ownerStateId != null && !ownerStateId.equals(stateId)) {
            // The aimed weapon was switched away from: end its ADS through Point Blank's
            // own transition so the holster animation can retract the ADS pose smoothly.
            releaseOwnerAiming();
            clear();
        }

        if (ownerStateId == null) {
            // Adopt the weapon now being held, starting from a clean toggle state.
            ownerPlayerId = player.getId();
            ownerStateId = stateId;
            ownerSlot = context.slotIndex();
            ownerOffhand = context.offhand();
            toggled = false;
            previousPhysical = physicalState;
        } else if (toggled && !isOwnerAiming()) {
            // Point Blank ended the ADS on its own (weapon rules, death, respawn, ...).
            // The toggle is void, so the player is never left stuck in a phantom ADS.
            toggled = false;
            previousPhysical = physicalState;
        }

        boolean justPressed = physicalState && !previousPhysical;
        previousPhysical = physicalState;
        if (justPressed) {
            toggled = !toggled;
        }
        return toggled;
    }

    /**
     * Drops the toggle when the client leaves the world. The aiming state is cleared
     * locally only: Point Blank keeps its client gun states in static maps and the
     * connection is already closing, so sending an aiming packet here would be unsafe.
     */
    public static void onDisconnect() {
        clearOwnerAiming(false);
        clear();
    }

    /**
     * Drops the toggle when the client player is replaced (respawn, dimension change).
     * The connection is still alive, so the server is told about the aiming change.
     */
    public static void onPlayerReplaced() {
        clearOwnerAiming(true);
        clear();
    }

    private static boolean isOwnerAiming() {
        GunClientState state = ownerStateId == null ? null : GunClientState.getState(ownerStateId);
        return state != null && state.isAiming();
    }

    /**
     * Takes the weapon that owns the toggle out of ADS using Point Blank's own aiming-off
     * transition, exactly like {@code ClientEventHandler#toggleAiming(player, context, false)}
     * and {@code GunHandSwapCoordinator#prepareOutgoingStateForSwap} do.
     */
    private static void releaseOwnerAiming() {
        clearOwnerAiming(true);
    }

    private static void clearOwnerAiming(boolean notifyServer) {
        if (!toggled || ownerStateId == null) {
            return;
        }

        GunClientState state = GunClientState.getState(ownerStateId);
        if (state == null || !state.isAiming()) {
            return;
        }

        state.setAiming(false);
        if (notifyServer && !ownerOffhand) {
            Platform.getInstance().getNetworkService().sendToServer(
                    new AimingChangeRequestPacket(ownerStateId, ownerSlot, false));
        }
    }

    private static void clear() {
        ownerPlayerId = -1;
        ownerStateId = null;
        ownerSlot = -1;
        ownerOffhand = false;
        toggled = false;
        previousPhysical = false;
    }
}
