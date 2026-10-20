package com.arg.pbextra.client;

import com.arg.pbextra.melee.MeleeAnimationController;
import com.arg.pbextra.melee.MeleeConstants;
import com.mojang.logging.LogUtils;
import com.vicmatskiv.pointblank.client.GunClientState;
import com.vicmatskiv.pointblank.item.GunItem;
import com.vicmatskiv.pointblank.registry.SoundRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.state.KeyFrameEvent;
import com.geckolib.cache.animation.keyframeevent.SoundKeyframeData;

import java.util.Locale;
import java.util.Map;


/**
 * Plays the cosmetic melee animation on the client.
 * <p>
 * Damage is no longer reported from here: the server owns the {@code preMelee}
 * schedule. This handler only starts the selected GeckoLib animation, forwards
 * its sound keyframes and releases the controller once the configured playback
 * window has elapsed.
 * <p>
 * Each hand owns its own swing: the main hand and the off hand can swing
 * independently, and a swing must never outlive its window. Releasing a swing
 * uses {@link MeleeAnimationController#release()} ({@code reset()}), not
 * GeckoLib's {@code stopTriggeredAnimation()}: the latter keeps the controller
 * animating its last frame forever, which pins the gun pose (and the
 * {@code _camera_} bone) on top of every Point Blank controller.
 */
public final class MeleeAnimationHandler {

    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * Extra wall-clock time allowed after the configured window before the
     * controller is force-released. This is a safety net for animations whose
     * baked length is shorter than the configured duration, or for a controller
     * that never reports an animation time because it was not rendered.
     */
    private static final long WINDOW_GRACE_MILLIS = 150L;

    /** Fallback used when an animation has no configured duration. */
    private static final long UNDEFINED_DURATION_FALLBACK_MILLIS = 10_000L;

    /**
     * Blend used only when a swing is retriggered while the previous swing is
     * still running. A fresh swing starts with no transition so the animation is
     * not delayed, while a retrigger blends in from the current pose instead of
     * snapping back to the first frame.
     */
    private static final int RETRIGGER_TRANSITION_TICKS = 2;

    private static final ActiveSwing MAIN_HAND = new ActiveSwing();
    private static final ActiveSwing OFF_HAND = new ActiveSwing();

    /** Most recently started animation; used for sound-keyframe diagnostics only. */
    private static String lastTriggeredAnimationName = "";

    private MeleeAnimationHandler() {
    }

    public static void install() {
        MeleeAnimationController.soundKeyframeListener = MeleeAnimationHandler::onSoundKeyframe;
    }


    public static void playMeleeAnimation(boolean offhand, String animationName,
                                          int durationMillis) {
        if (animationName == null || animationName.isEmpty()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            return;
        }

        ItemStack stack = offhand ? player.getOffhandItem() : player.getMainHandItem();
        if (stack.isEmpty() || !(stack.getItem() instanceof GunItem gun)) {
            return;
        }

        if (!MeleeEligibility.isMeleeAllowed(player, offhand)) {
            return;
        }

        AnimationController<?> controller = findController(gun, stack, offhand);
        if (!(controller instanceof MeleeAnimationController meleeController)) {
            LOGGER.warn("Could not find melee animation controller '{}'; the melee animation "
                    + "'{}' was not played", MeleeConstants.MELEE_CONTROLLER, animationName);
            return;
        }

        ActiveSwing swing = swing(offhand);
        boolean retrigger = swing.isActive(meleeController);

        // Resolve dual-wield variants the same way Point Blank resolves every other
        // animation. Without this a one-handed right-hand grip would play the
        // two-handed melee pose, and the off hand would play the main-hand pose.
        String resolvedName = resolveAnimationName(gun, stack, offhand, animationName);

        meleeController.setTransitionTicks(retrigger ? RETRIGGER_TRANSITION_TICKS : 0);
        meleeController.triggerableAnim(MeleeConstants.MELEE_TRIGGER,
                RawAnimation.begin().thenPlay(resolvedName));
        if (!meleeController.triggerAnimation(MeleeConstants.MELEE_TRIGGER)) {
            LOGGER.warn("Melee animation '{}' could not be triggered on controller '{}'",
                    resolvedName, MeleeConstants.MELEE_CONTROLLER);
            return;
        }

        swing.start(meleeController, resolvedName, durationMillis);
        lastTriggeredAnimationName = resolvedName;

        LOGGER.debug("Melee animation '{}' started on controller '{}' ({} hand, window {} ms{})",
                resolvedName, MeleeConstants.MELEE_CONTROLLER,
                offhand ? "off" : "main",
                swing.configuredDurationMillis,
                retrigger ? ", retrigger blend" : "");
    }


    /**
     * Polls the active swings once per client tick. GeckoLib 5 has no overridable
     * tick hook, so the controller's own animation clock is sampled here.
     * <p>
     * A freshly triggered controller has no animation point until it is rendered
     * for the first time. It must therefore not be treated as "finished" before
     * that first render; only the configured window (or the fallback deadline)
     * ends the swing.
     */
    public static void tick() {
        tick(MAIN_HAND);
        tick(OFF_HAND);
    }


    private static void tick(ActiveSwing swing) {
        MeleeAnimationController controller = swing.controller;
        if (controller == null) {
            return;
        }

        long now = nowMillis();
        if (!swing.animationObserved && controller.isPlayingTriggeredAnimation()) {
            swing.animationObserved = true;
        }

        boolean windowElapsed;
        if (swing.configuredDurationMillis > 0) {
            double animationMillis = controller.getCurrentAnimationTime()
                    * MeleeConstants.ANIMATION_MILLIS_PER_SECOND;
            boolean animationReachedWindow = swing.animationObserved
                    && animationMillis >= swing.configuredDurationMillis;
            boolean wallClockReachedWindow = now - swing.startMillis
                    >= swing.configuredDurationMillis + WINDOW_GRACE_MILLIS;
            windowElapsed = animationReachedWindow || wallClockReachedWindow;
        } else {
            boolean bakedAnimationFinished = swing.animationObserved
                    && controller.hasAnimationFinished();
            boolean fallbackElapsed = now - swing.startMillis
                    >= UNDEFINED_DURATION_FALLBACK_MILLIS;
            windowElapsed = bakedAnimationFinished || fallbackElapsed;
        }

        if (!windowElapsed) {
            return;
        }

        LOGGER.debug("Melee animation '{}' finished its {} ms window; releasing controller '{}'",
                swing.animationName, format(swing.configuredDurationMillis),
                MeleeConstants.MELEE_CONTROLLER);
        swing.clear();
        // reset() clears the animation point, timeline and raw animation so this
        // controller stops overriding Point Blank's pose/camera bones. Point
        // Blank's idle, walking, recoil and camera controllers are never stopped,
        // so they take over again on the next frame.
        controller.release();
    }


    /** Plays a sound requested by a sound keyframe of the active swing. */
    private static void onSoundKeyframe(KeyFrameEvent<GunItem, SoundKeyframeData> event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        String soundName = event.keyframeData().getSound();
        SoundEvent soundEvent = SoundRegistry.getSoundEvent(soundName);
        if (soundEvent == null) {
            LOGGER.warn("Melee animation '{}' requests the sound '{}', which is not registered "
                            + "in Point Blank's sound registry (assets/pointblank/sounds.json)",
                    lastTriggeredAnimationName, soundName);
            return;
        }

        player.playSound(soundEvent, 1.0F, 1.0F);
    }


    /** Drops both active-swing records without touching GeckoLib's controllers. */
    public static void clear() {
        MAIN_HAND.clear();
        OFF_HAND.clear();
        lastTriggeredAnimationName = "";
    }

    /** Releases and forgets both active swings (used when leaving the world). */
    public static void stopAndClear() {
        MeleeAnimationController mainHand = MAIN_HAND.controller;
        MeleeAnimationController offHand = OFF_HAND.controller;

        clear();

        if (mainHand != null) {
            mainHand.release();
        }
        if (offHand != null && offHand != mainHand) {
            offHand.release();
        }
    }

    /**
     * Resolves dual-wield variants for the melee animation through Point Blank's
     * own resolver. If the variant (or the base animation) is not baked, Point
     * Blank returns the base name and the trigger simply fails to resolve, which
     * leaves the swing cosmetic-only, exactly as before.
     */
    private static String resolveAnimationName(GunItem gun, ItemStack stack,
                                               boolean offhand, String animationName) {
        // Only dual-wield weapons have suffixed variants. Leaving every other
        // weapon untouched avoids depending on renderer state for the common case.
        if (!gun.isDualWieldingEnabled(stack)) {
            return animationName;
        }

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return animationName;
        }

        GunClientState state = offhand
                ? offhandState(player)
                : GunClientState.getMainHeldState(player);

        try {
            String resolved = gun.resolveAvailableAnimationName(player, state, stack, animationName);
            return resolved == null || resolved.isEmpty() ? animationName : resolved;
        } catch (RuntimeException e) {
            LOGGER.debug("Could not resolve a dual-wield melee variant for '{}': {}",
                    animationName, e.getMessage());
            return animationName;
        }
    }


    private static GunClientState offhandState(LocalPlayer player) {
        GunItem.OperableGunContext context = GunItem.resolveOffhandGunContext(player);
        if (context == null) {
            return null;
        }
        return GunClientState.getState(player, context.itemStack(), context.slotIndex(),
                context.offhand());
    }


    private static ActiveSwing swing(boolean offhand) {
        return offhand ? OFF_HAND : MAIN_HAND;
    }


    private static String format(double millis) {
        return String.format(Locale.ROOT, "%.1f", millis);
    }

    /** Monotonic millisecond clock used only for window deadlines. */
    private static long nowMillis() {
        return System.nanoTime() / 1_000_000L;
    }

    private static AnimationController<GeoAnimatable> findController(
            GunItem gun, ItemStack stack, boolean offhand) {

        ItemDisplayContext context = offhand
                ? ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                : ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;

        AnimationController<GeoAnimatable> controller =
                gun.getGeoAnimationController(MeleeConstants.MELEE_CONTROLLER, stack, context);
        if (controller != null) {
            return controller;
        }

        controller = gun.getGeoAnimationController(MeleeConstants.MELEE_CONTROLLER, stack);
        if (controller != null) {
            return controller;
        }

        Map<String, AnimationController<GeoAnimatable>> controllers =
                gun.getGeoAnimationControllers(stack, context);
        if (controllers != null && controllers.containsKey(MeleeConstants.MELEE_CONTROLLER)) {
            return controllers.get(MeleeConstants.MELEE_CONTROLLER);
        }

        controllers = gun.getGeoAnimationControllers(stack);
        if (controllers != null && controllers.containsKey(MeleeConstants.MELEE_CONTROLLER)) {
            return controllers.get(MeleeConstants.MELEE_CONTROLLER);
        }

        return null;
    }


    /** One swing's bookkeeping. The main hand and the off hand never share it. */
    private static final class ActiveSwing {

        private MeleeAnimationController controller;
        private String animationName = "";
        private long startMillis;
        private long configuredDurationMillis;
        private boolean animationObserved;

        private boolean isActive(MeleeAnimationController candidate) {
            return controller != null && controller == candidate;
        }

        private void start(MeleeAnimationController controller, String animationName,
                           int durationMillis) {
            this.controller = controller;
            this.animationName = animationName;
            this.startMillis = nowMillis();
            this.configuredDurationMillis = Math.max(0, durationMillis);
            this.animationObserved = false;
        }

        private void clear() {
            this.controller = null;
            this.animationName = "";
            this.startMillis = 0L;
            this.configuredDurationMillis = 0L;
            this.animationObserved = false;
        }
    }
}

