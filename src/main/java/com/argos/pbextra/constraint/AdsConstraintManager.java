package com.argos.pbextra.constraint;

import com.argos.pbextra.config.PointBlankExtraConfig;
import com.argos.pbextra.data.WeaponExtraDataManager;
import com.mojang.logging.LogUtils;
import com.vicmatskiv.pointblank.client.BiDirectionalInterpolator;
import com.vicmatskiv.pointblank.client.GunClientState;
import com.vicmatskiv.pointblank.client.GunStateListener;
import com.vicmatskiv.pointblank.client.controller.BlendingAnimationProcessor;
import com.vicmatskiv.pointblank.item.GunItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.state.BoneSnapshot;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class AdsConstraintManager {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Set<String> WARNED_WEAPONS = ConcurrentHashMap.newKeySet();

    // Temporary debug tracing: throttled so a 60 fps render cannot spam the log.
    private static long lastGateTraceTime;
    private static long lastCorrectionTraceTime;


    private static CoreGeoBone correctedCamera;
    private static float correctedCameraRotX;
    private static float correctedCameraRotY;
    private static float correctedCameraRotZ;
    private static float correctedCameraPosX;
    private static float correctedCameraPosY;
    private static float correctedCameraPosZ;

    private AdsConstraintManager() {
    }

    private static boolean gateTraceDue() {
        long now = System.currentTimeMillis();
        if (now - lastGateTraceTime >= 1000L) {
            lastGateTraceTime = now;
            return true;
        }
        return false;
    }

    private static boolean correctionTraceDue() {
        long now = System.currentTimeMillis();
        if (now - lastCorrectionTraceTime >= 1000L) {
            lastCorrectionTraceTime = now;
            return true;
        }
        return false;
    }

    public static void apply(BlendingAnimationProcessor<?> processor, AnimationState<?> state) {
        if (!PointBlankExtraConfig.ENABLE_MOD.get()) {
            return;
        }

        ItemDisplayContext displayContext = state.getData(DataTickets.ITEM_RENDER_PERSPECTIVE);
        if (displayContext != ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                && displayContext != ItemDisplayContext.FIRST_PERSON_LEFT_HAND) {
            return;
        }

        ItemStack stack = state.getData(DataTickets.ITEMSTACK);
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof GunItem gun)) {
            return;
        }

        AdsConstraintConfig config = WeaponExtraDataManager.getAdsConstraintConfig(stack);

        LocalPlayer player = Minecraft.getInstance().player;
        GunClientState gunState = player == null ? null : GunClientState.getMainHeldState(player);
        double progress = gunState == null ? 0.0 : resolveAimingProgress(gunState);
        CoreGeoBone root = processor.getBone(AdsConstraintConstants.ROOT_BONE);
        CoreGeoBone constraint = processor.getBone(AdsConstraintConstants.CONSTRAINT_BONE);
        CoreGeoBone camera = config.isCamera() ? processor.getBone(AdsConstraintConstants.CAMERA_BONE) : null;

        if (gateTraceDue()) {
            LOGGER.debug(
                    "AdsConstraint: item={} enabled={} cameraFlag={} player={} gunState={} gunMatches={} progress={} main={} constraint={} cameraBone={}",
                    ForgeRegistries.ITEMS.getKey(stack.getItem()),
                    config.isEnabled(),
                    config.isCamera(),
                    player != null,
                    gunState != null,
                    gunState != null && gunState.getGunItem() == gun,
                    progress,
                    root != null,
                    constraint != null,
                    camera != null);
        }

        if (!config.isEnabled()) {
            return;
        }
        if (player == null) {
            return;
        }
        if (gunState == null || gunState.getGunItem() != gun) {
            return;
        }

        // Point Blank turns the weapon model's "_camera_" bone into first-person camera
        // movement: GunItemRenderer#renderRecursively captures its rotX/rotY/rotZ while the
        // weapon is rendered and ClientEventHandler#onPreRenderHandEvent applies them
        // (negated, on X/Y/Z) to the first-person pose stack. That rotation therefore *is*
        // Point Blank's camera animation - the equip/draw, idle, firing, reload and inspect
        // animations all drive it. The camera constraint below therefore never resets the
        // bone and never works from its own previous result: it scales the value the
        // animation just wrote, once, and only while ADS is active.

        if (progress <= 0.0) {
            return;
        }
        if (root == null || constraint == null) {
            // This model has no weapon-root bones, so the pose constraint cannot be applied.
            // The camera constraint below does not depend on them and still runs.
            warnMissingBonesOnce(gun, root);
        } else {
            // The weapon root always uses the per-axis fractions read from the model's
            // "constraint" bone; this is the original, unchanged behaviour.
            applyCorrectiveTransform(root,
                    AdsConstraintMath.rotationRetention(constraint.getRotX()),
                    AdsConstraintMath.rotationRetention(constraint.getRotY()),
                    AdsConstraintMath.rotationRetention(constraint.getRotZ()),
                    AdsConstraintMath.positionRetention(constraint.getPosX()),
                    AdsConstraintMath.positionRetention(constraint.getPosY()),
                    AdsConstraintMath.positionRetention(constraint.getPosZ()),
                    progress);
        }

        if (camera != null) {
            float rotRetX;
            float rotRetY;
            float rotRetZ;
            float posRetX;
            float posRetY;
            float posRetZ;

            if (config.hasCameraConstraint()) {
                // An explicit cameraConstraint applies one uniform retained fraction on every
                // axis of the camera bone - this is the option that makes the camera keep only
                // that fraction of its animated movement while ADS.
                float cameraRetention = config.getCameraConstraint();
                rotRetX = cameraRetention;
                rotRetY = cameraRetention;
                rotRetZ = cameraRetention;
                posRetX = cameraRetention;
                posRetY = cameraRetention;
                posRetZ = cameraRetention;
            } else if (constraint != null) {
                // Backwards compatible: without cameraConstraint the camera keeps using the
                // same per-axis fractions as the weapon root.
                rotRetX = AdsConstraintMath.rotationRetention(constraint.getRotX());
                rotRetY = AdsConstraintMath.rotationRetention(constraint.getRotY());
                rotRetZ = AdsConstraintMath.rotationRetention(constraint.getRotZ());
                posRetX = AdsConstraintMath.positionRetention(constraint.getPosX());
                posRetY = AdsConstraintMath.positionRetention(constraint.getPosY());
                posRetZ = AdsConstraintMath.positionRetention(constraint.getPosZ());
            } else {
                // No cameraConstraint and no "constraint" bone to read fractions from: leave the
                // camera exactly as the weapon animation produced it.
                rotRetX = AdsConstraintMath.FULL_RETENTION;
                rotRetY = AdsConstraintMath.FULL_RETENTION;
                rotRetZ = AdsConstraintMath.FULL_RETENTION;
                posRetX = AdsConstraintMath.FULL_RETENTION;
                posRetY = AdsConstraintMath.FULL_RETENTION;
                posRetZ = AdsConstraintMath.FULL_RETENTION;
            }

            // The camera bone's rotation is Point Blank's first-person camera animation, so
            // the constraint is only ever applied to a value the animation wrote since the
            // last correction - never to this addon's own previous output. That keeps every
            // equip/draw, idle, firing, reload and inspect camera animation intact, makes the
            // correction idempotent and stops it from accumulating (creeping further towards
            // the bind pose) while a pose is held.
            if (!cameraAuthoredSinceLastCorrection(camera)) {
                rotRetX = AdsConstraintMath.FULL_RETENTION;
                rotRetY = AdsConstraintMath.FULL_RETENTION;
                rotRetZ = AdsConstraintMath.FULL_RETENTION;
                posRetX = AdsConstraintMath.FULL_RETENTION;
                posRetY = AdsConstraintMath.FULL_RETENTION;
                posRetZ = AdsConstraintMath.FULL_RETENTION;
            }

            applyCorrectiveTransform(camera, rotRetX, rotRetY, rotRetZ,
                    posRetX, posRetY, posRetZ, progress);
            rememberCameraCorrection(camera);
        }
    }



    private static boolean cameraAuthoredSinceLastCorrection(CoreGeoBone camera) {
        if (camera != correctedCamera) {
            return true;
        }
        return camera.getRotX() != correctedCameraRotX
                || camera.getRotY() != correctedCameraRotY
                || camera.getRotZ() != correctedCameraRotZ
                || camera.getPosX() != correctedCameraPosX
                || camera.getPosY() != correctedCameraPosY
                || camera.getPosZ() != correctedCameraPosZ;
    }

    /** Remembers what the last camera correction left on the bone. */
    private static void rememberCameraCorrection(CoreGeoBone camera) {
        correctedCamera = camera;
        correctedCameraRotX = camera.getRotX();
        correctedCameraRotY = camera.getRotY();
        correctedCameraRotZ = camera.getRotZ();
        correctedCameraPosX = camera.getPosX();
        correctedCameraPosY = camera.getPosY();
        correctedCameraPosZ = camera.getPosZ();
    }


    private static void applyCorrectiveTransform(CoreGeoBone target,
            float rotRetX, float rotRetY, float rotRetZ,
            float posRetX, float posRetY, float posRetZ,
            double progress) {
        boolean constrainRotation = AdsConstraintMath.isConstrained(rotRetX)
                || AdsConstraintMath.isConstrained(rotRetY)
                || AdsConstraintMath.isConstrained(rotRetZ);
        boolean constrainPosition = AdsConstraintMath.isConstrained(posRetX)
                || AdsConstraintMath.isConstrained(posRetY)
                || AdsConstraintMath.isConstrained(posRetZ);

        boolean trace = correctionTraceDue();
        float beforeRotX = target.getRotX();
        float beforeRotY = target.getRotY();
        float beforeRotZ = target.getRotZ();
        float beforePosX = target.getPosX();
        float beforePosY = target.getPosY();
        float beforePosZ = target.getPosZ();

        if (!constrainRotation && !constrainPosition) {
            if (trace) {
                LOGGER.debug("AdsConstraint '{}': SKIPPED (all retention=1). rot=({},{},{}) pos=({},{},{})",
                        target.getName(), rotRetX, rotRetY, rotRetZ, posRetX, posRetY, posRetZ);
            }
            return;
        }

        BoneSnapshot bind = target.getInitialSnapshot();

        if (constrainRotation) {
            target.updateRotation(
                    AdsConstraintMath.applyCorrection(target.getRotX(), bind.getRotX(), rotRetX, progress),
                    AdsConstraintMath.applyCorrection(target.getRotY(), bind.getRotY(), rotRetY, progress),
                    AdsConstraintMath.applyCorrection(target.getRotZ(), bind.getRotZ(), rotRetZ, progress));
        }

        if (constrainPosition) {
            target.updatePosition(
                    AdsConstraintMath.applyCorrection(target.getPosX(), bind.getOffsetX(), posRetX, progress),
                    AdsConstraintMath.applyCorrection(target.getPosY(), bind.getOffsetY(), posRetY, progress),
                    AdsConstraintMath.applyCorrection(target.getPosZ(), bind.getOffsetZ(), posRetZ, progress));
        }

        if (trace) {
            LOGGER.debug(
                    "AdsConstraint '{}': progress={} retention rot=({},{},{}) pos=({},{},{}) rot ({},{},{})->({},{},{}) pos ({},{},{})->({},{},{})",
                    target.getName(), progress, rotRetX, rotRetY, rotRetZ, posRetX, posRetY, posRetZ,
                    beforeRotX, beforeRotY, beforeRotZ, target.getRotX(), target.getRotY(), target.getRotZ(),
                    beforePosX, beforePosY, beforePosZ, target.getPosX(), target.getPosY(), target.getPosZ());
        }

        target.resetStateChanges();
    }


    private static double resolveAimingProgress(GunClientState gunState) {
        GunStateListener aiming =
                gunState.getAnimationController(AdsConstraintConstants.AIMING_CONTROLLER_ID);
        if (aiming instanceof BiDirectionalInterpolator interpolator) {
            double value = interpolator.getValue();
            if (Double.isFinite(value)) {
                return Mth.clamp(value, 0.0, 1.0);
            }
        }
        return 0.0;
    }

    private static void warnMissingBonesOnce(GunItem gun, CoreGeoBone rootBone) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(gun);
        String key = id == null ? gun.getClass().getName() : id.toString();
        if (WARNED_WEAPONS.add(key)) {
            LOGGER.warn("AdsConstraint is enabled for '{}' but the model is missing the '{}' bone{}; "
                            + "the constraint is skipped for this weapon",
                    key,
                    rootBone == null ? AdsConstraintConstants.ROOT_BONE : AdsConstraintConstants.CONSTRAINT_BONE,
                    rootBone == null ? " (the weapon root)" : "");
        }
    }
}
