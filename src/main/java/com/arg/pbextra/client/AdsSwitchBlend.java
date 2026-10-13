package com.arg.pbextra.client;

import com.arg.pbextra.config.PointBlankExtraConfig;
import com.vicmatskiv.pointblank.item.GunItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;
import java.util.UUID;

/**
 * Compatibility bridge for the ADS camera when the held weapon changes.
 *
 * <p>Point Blank computes its ADS zoom in {@code ClientEventHandler#onFovUpdate} from the
 * aiming progress of the weapon that is in the main hand <em>right now</em>:</p>
 *
 * <pre>
 * float aimingProgress = (float) gunClientState.getAnimationController("aiming").getValue();
 * float fullZoomAdj    = zoomValue.update(AimingFeature.getZoom(stack)) * aimingProgress;
 * event.setFOV(incomingFov - incomingFov * fullZoomAdj - shake);
 * </pre>
 *
 * <p>Nothing blends two weapons' progress, because Point Blank never needs to: when the
 * player holds the use key across a switch it aims the new weapon in the same tick, and
 * when the key is not held neither weapon is aimed. The "outgoing weapon aimed, incoming
 * weapon not aimed" combination is only reachable through per-weapon toggle aiming, and
 * for that case the incoming weapon's progress is 0, so the zoom collapses in a single
 * frame - the abrupt camera snap.</p>
 *
 * <p>This class keeps the aiming progress the camera used on the previous frame and
 * cross-fades it to the new weapon's progress over {@link #FADE_MILLIS} ms, which is the
 * duration Point Blank registers for its own {@code "aiming"} interpolator
 * ({@code new GunItem$12(gunItem, 400L)}). Only that single value is affected; Point
 * Blank's aiming state, packets, animations, camera rotations and every other feature are
 * left alone.</p>
 *
 * <p>Weapon-to-weapon switches are handled by that progress fade. A switch from an aimed
 * weapon to an item Point Blank does not recognise as a gun takes a second path
 * ({@link #bridgeFov}), because Point Blank's FOV handler returns before reading any
 * aiming progress in that case - see that method for the details.</p>
 */
public final class AdsSwitchBlend {

    private AdsSwitchBlend() {
    }

    /** Matches the duration Point Blank uses for its own "aiming" interpolator. */
    private static final long FADE_MILLIS = 400L;

    private static final long FADE_NANOS = FADE_MILLIS * 1_000_000L;

    /**
     * If the previous frame is older than this, the weapon that was on screen cannot be
     * trusted any more (for example the player holstered to a non-gun, where Point Blank
     * skips its FOV path entirely). No fade is started in that case.
     */
    private static final long STALE_NANOS = 250L * 1_000_000L;

    /** Below this the camera was not really in ADS, so there is nothing to ramp out of. */
    private static final float MIN_OUTGOING_PROGRESS = 0.02F;

    /**
     * Smallest FOV difference (in degrees) that counts as "the camera was showing ADS".
     * Below it the previous frame was effectively at the normal FOV, so switching items
     * must not start any fade.
     */
    private static final float MIN_FOV_DELTA = 0.5F;

    private static int lastPlayerId = -1;

    /** {@link GunItem#getItemStackId(ItemStack)} of the weapon the camera last used. */
    private static UUID lastGunId;

    /** Aiming progress the camera actually used on the previous frame. */
    private static float lastDisplayedProgress;

    private static long lastUpdateNanos;

    private static float fadeFromProgress;

    private static long fadeStartNanos;

    private static boolean fading;

    /**
     * Per-pass FOV fade state. Point Blank's {@code onFovUpdate} runs twice per frame - once
     * for the world FOV ({@code useFovSetting()} true) and once for the hand FOV - and each
     * pass carries its own incoming FOV, so their bookkeeping must not be shared.
     */
    private static final class FovFade {

        /** Identity of the main-hand item seen on this pass last frame. */
        Object lastHeldKey;

        /** FOV this pass actually ended up with last frame. */
        float lastUsedFov;

        long lastUpdateNanos;

        float fadeFromFov;

        long fadeStartNanos;

        boolean fading;
    }

    /** Index 0 is the hand pass, index 1 the world pass (see {@link #bridgeFov}). */
    private static final FovFade[] FOV_FADES = { new FovFade(), new FovFade() };

    /**
     * Cross-fades the aiming progress Point Blank is about to use for the ADS zoom.
     *
     * @param vpbProgress the value Point Blank read from the held weapon's aiming
     *                    interpolator this frame
     * @return the value the FOV maths should use; equal to {@code vpbProgress} whenever no
     *         weapon switch is being bridged
     */
    public static float blend(float vpbProgress) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft == null ? null : minecraft.player;
        if (!PointBlankExtraConfig.ENABLE_MOD.get() || player == null) {
            reset();
            return vpbProgress;
        }

        long now = System.nanoTime();
        if (player.getId() != lastPlayerId) {
            // A new player entity means a new session: never carry a fade over.
            reset();
        }

        UUID gunId = currentGunId(player);
        boolean switched = gunId != null && !gunId.equals(lastGunId);
        boolean previousFrameUsable = now - lastUpdateNanos <= STALE_NANOS;

        if (switched) {
            if (!anyFovFadeActive() && previousFrameUsable
                    && lastDisplayedProgress > MIN_OUTGOING_PROGRESS) {
                // Keep the ADS factor the camera was showing and ramp it to whatever the
                // weapon now in hand is doing, at Point Blank's own aiming speed. Using the
                // value that was actually displayed keeps the ramp continuous even when the
                // player switches weapons again while a fade is still running.
                fadeFromProgress = lastDisplayedProgress;
                fadeStartNanos = now;
                fading = true;
            } else {
                fading = false;
            }
        }

        lastPlayerId = player.getId();
        lastGunId = gunId;
        lastUpdateNanos = now;

        float displayed = vpbProgress;
        if (fading && !anyFovFadeActive()) {
            // Only one of the two mechanisms writes the FOV in a given frame. If the FOV
            // fade owns the transition, pass Point Blank's own value through and let the
            // FOV fade (which continues from the value that was displayed) do the easing.
            long elapsed = now - fadeStartNanos;
            if (elapsed >= FADE_NANOS) {
                fading = false;
            } else {
                float remaining = 1.0F - (float) elapsed / (float) FADE_NANOS;
                displayed = vpbProgress + (fadeFromProgress - vpbProgress) * remaining;
            }
        }

        lastDisplayedProgress = displayed;
        return displayed;
    }

    /** Drops all fade state (logout, respawn, player change, config change, ...). */
    public static void reset() {
        lastPlayerId = -1;
        lastGunId = null;
        lastDisplayedProgress = 0.0F;
        lastUpdateNanos = 0L;
        fadeFromProgress = 0.0F;
        fadeStartNanos = 0L;
        fading = false;
        resetFovFades();
    }

    /**
     * Smooths the FOV back to normal when the player leaves an aimed gun for something
     * Point Blank does not treat as a gun - an empty slot, a block, a tool, food, or any
     * other ordinary item.
     *
     * <p>Point Blank's {@code onFovUpdate} returns before it touches the FOV when the main
     * hand does not hold a gun, so on that frame no aiming progress is read at all and
     * {@link #blend(float)} cannot help. This method keeps the FOV that was actually on
     * screen while the gun was aimed and eases it to the FOV the game reports now, so the
     * camera always converges back to exactly the value it would have had without ADS -
     * the FOV is never recomputed here, only eased towards the incoming value.</p>
     *
     * @param useFovSetting {@code ViewportEvent.ComputeFov#useFovSetting()} - selects the
     *                      pass, because Point Blank's handler runs once per pass per frame
     * @param fov           the FOV currently in the event
     * @return the FOV the event should carry; the input value whenever no fade is running
     */
    public static double bridgeFov(boolean useFovSetting, double fov) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft == null ? null : minecraft.player;
        if (!PointBlankExtraConfig.ENABLE_MOD.get() || player == null
                || !minecraft.options.getCameraType().isFirstPerson()) {
            resetFovFades();
            return fov;
        }

        FovFade state = FOV_FADES[useFovSetting ? 1 : 0];
        long now = System.nanoTime();
        Object heldKey = heldKey(player);
        boolean itemChanged = !Objects.equals(heldKey, state.lastHeldKey);
        boolean gunHeld = currentGunId(player) != null;
        float target = (float) fov;

        if (itemChanged && !gunHeld && !state.fading
                && now - state.lastUpdateNanos <= STALE_NANOS
                && state.lastUsedFov <= target - MIN_FOV_DELTA) {
            // An aimed gun was replaced by a non-gun item: keep the zoomed FOV that was on
            // screen and ease it back to the normal FOV reported now.
            state.fadeFromFov = state.lastUsedFov;
            state.fadeStartNanos = now;
            state.fading = true;
        }

        double result = fov;
        if (state.fading) {
            long elapsed = now - state.fadeStartNanos;
            if (elapsed >= FADE_NANOS || target <= state.fadeFromFov) {
                // Either the fade finished, or something now wants more zoom than the value
                // we kept (the player re-entered ADS): hand the FOV straight back.
                state.fading = false;
            } else {
                float remaining = 1.0F - (float) elapsed / (float) FADE_NANOS;
                result = target + (state.fadeFromFov - target) * remaining;
            }
        }

        state.lastHeldKey = heldKey;
        state.lastUpdateNanos = now;
        state.lastUsedFov = (float) result;
        return result;
    }

    private static UUID currentGunId(LocalPlayer player) {
        ItemStack stack = GunItem.getMainHeldGunItemStack(player);
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof GunItem)) {
            return null;
        }
        return GunItem.getItemStackId(stack);
    }

    /**
     * Identity of the main-hand item: the gun's stack id for guns, otherwise the item
     * itself, and {@code null} for an empty hand. Detecting the change of this key is what
     * marks a switch to a non-gun item; this is deliberately <em>not</em> routed through
     * {@link #reset()}, because the outgoing zoom has to survive the switch.
     */
    private static Object heldKey(LocalPlayer player) {
        ItemStack stack = GunItem.getMainHeldGunItemStack(player);
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        if (stack.getItem() instanceof GunItem) {
            UUID id = GunItem.getItemStackId(stack);
            if (id != null) {
                return id;
            }
        }
        return stack.getItem();
    }

    private static boolean anyFovFadeActive() {
        return FOV_FADES[0].fading || FOV_FADES[1].fading;
    }

    private static void resetFovFades() {
        for (FovFade state : FOV_FADES) {
            state.lastHeldKey = null;
            state.lastUsedFov = 0.0F;
            state.lastUpdateNanos = 0L;
            state.fadeFromFov = 0.0F;
            state.fadeStartNanos = 0L;
            state.fading = false;
        }
    }
}

