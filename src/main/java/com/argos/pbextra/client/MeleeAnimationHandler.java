package com.argos.pbextra.client;

import com.mojang.logging.LogUtils;
import com.argos.pbextra.melee.MeleeAnimationController;
import com.argos.pbextra.melee.MeleeConstants;
import com.argos.pbextra.melee.MeleeImpactPacket;
import com.argos.pbextra.network.ModNetwork;
import com.vicmatskiv.pointblank.item.GunItem;
import com.vicmatskiv.pointblank.registry.SoundRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.keyframe.event.SoundKeyframeEvent;

import java.util.Locale;
import java.util.Map;


public final class MeleeAnimationHandler {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static MeleeAnimationController activeController;
    private static String activeAnimationName = "";
    private static double preMeleeMillis;
    private static double windowMillis;
    private static boolean impactAnnounced;
    private static boolean playbackObserved;
    private static double lastPositionMillis;

    private MeleeAnimationHandler() {
    }

    public static void install() {
        MeleeAnimationController.positionListener = MeleeAnimationHandler::onAnimationPosition;
        MeleeAnimationController.soundKeyframeListener = MeleeAnimationHandler::onSoundKeyframe;
    }


    public static void playMeleeAnimation(boolean offhand, String animationName,
                                          int durationMillis, int preMeleeMillis) {
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

        // Start from animation time 0, even if the previous swing is still playing.
        meleeController.restartAnimationTime();
        meleeController.triggerableAnim(MeleeConstants.MELEE_TRIGGER,
                RawAnimation.begin().thenPlay(animationName));
        if (!meleeController.tryTriggerAnimation(MeleeConstants.MELEE_TRIGGER)) {
            LOGGER.warn("Melee animation '{}' could not be triggered on controller '{}'",
                    animationName, MeleeConstants.MELEE_CONTROLLER);
            return;
        }

        activeController = meleeController;
        activeAnimationName = animationName;
        MeleeAnimationHandler.preMeleeMillis = Math.max(0, preMeleeMillis);
        windowMillis = Math.max(0, durationMillis);
        impactAnnounced = false;
        playbackObserved = false;
        lastPositionMillis = 0.0;

        LOGGER.info("Melee animation '{}' started on controller '{}' "
                        + "(hit at animation time {} ms, window {} ms)",
                animationName, MeleeConstants.MELEE_CONTROLLER,
                MeleeAnimationHandler.preMeleeMillis, windowMillis);
    }


    private static void onAnimationPosition(MeleeAnimationController controller,
                                            double positionTicks) {
        MeleeAnimationController active = activeController;
        if (active == null || controller != active) {
            return;
        }
        if (!controller.isPlayingTriggeredAnimation()
                || controller.getAnimationState() == AnimationController.State.STOPPED) {
            // Not this swing's animation playing any more. GeckoLib keeps returning
            // the age since the last animation started for a stopped controller, so
            // such positions must not be mistaken for playback.
            return;
        }

        double positionMillis = positionTicks * MeleeConstants.ANIMATION_MILLIS_PER_TICK;
        if (playbackObserved && positionMillis < lastPositionMillis) {
            // The animation restarted behind our back; treat the swing as new.
            impactAnnounced = false;
        }
        playbackObserved = true;
        lastPositionMillis = positionMillis;

        if (!impactAnnounced && positionMillis >= preMeleeMillis) {
            impactAnnounced = true;
            ModNetwork.CHANNEL.sendToServer(new MeleeImpactPacket(activeAnimationName));
            LOGGER.info("Melee '{}' reached its impact at animation time {} ms (preMelee {} ms)",
                    activeAnimationName, format(positionMillis), preMeleeMillis);
        }

        if (windowMillis > 0 && positionMillis >= windowMillis) {
            LOGGER.info("Melee '{}' played its whole {} ms window ({} ms); stopping the controller",
                    activeAnimationName, windowMillis, format(positionMillis));
            clear();
            // Stopping makes the controller report a finished animation, so it stops
            // producing pose data and Point Blank's own idle/fire/draw/reload
            // controllers take the pose back over on their own.
            controller.stop();
        }
    }

  // here this will play  sound effects from sounds keyframes
    private static void onSoundKeyframe(SoundKeyframeEvent<GunItem> event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        String soundName = event.getKeyframeData().getSound();
        SoundEvent soundEvent = SoundRegistry.getSoundEvent(soundName);
        if (soundEvent == null) {
            LOGGER.warn("Melee animation '{}' requests the sound '{}', which is not registered "
                            + "in Point Blank's sound registry (assets/pointblank/sounds.json)",
                    activeAnimationName, soundName);
            return;
        }

        player.playSound(soundEvent, 1.0F, 1.0F);
    }


    public static void tick() {
        MeleeAnimationController active = activeController;
        if (active != null && !active.isPlayingTriggeredAnimation()) {
            clear();
        }
    }

    private static void clear() {
        activeController = null;
        activeAnimationName = "";
        preMeleeMillis = 0.0;
        windowMillis = 0.0;
        impactAnnounced = false;
        playbackObserved = false;
        lastPositionMillis = 0.0;
    }

    private static String format(double millis) {
        return String.format(Locale.ROOT, "%.1f", millis);
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
}
