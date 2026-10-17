package com.arg.pbextra.constraint;

import com.arg.pbextra.config.PointBlankExtraConfig;
import com.arg.pbextra.data.WeaponExtraDataManager;
import com.mojang.logging.LogUtils;
import com.vicmatskiv.pointblank.client.BiDirectionalInterpolator;
import com.vicmatskiv.pointblank.client.GunClientState;
import com.vicmatskiv.pointblank.client.GunStateListener;
import com.vicmatskiv.pointblank.constant.PointBlankDataTickets;
import com.vicmatskiv.pointblank.item.GunItem;
import com.geckolib.animation.state.BoneSnapshot;
import com.geckolib.cache.model.GeoBone;
import com.geckolib.constant.DataTickets;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.RenderPassInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


public final class AdsConstraintManager {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Set<String> WARNED_WEAPONS = ConcurrentHashMap.newKeySet();

    // Temporary debug tracing: throttled so a 60 fps render cannot spam the log.
    private static long lastGateTraceTime;
    private static long lastCorrectionTraceTime;

    private static BoneSnapshot correctedCamera;
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

    public static void apply(RenderPassInfo<?> renderPassInfo, BoneSnapshots snapshots) {
        if (!PointBlankExtraConfig.ENABLE_MOD.get()) {
            return;
        }

        ItemDisplayContext displayContext =
                renderPassInfo.renderState().getGeckolibData(DataTickets.ITEM_RENDER_PERSPECTIVE);
        if (displayContext != ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                && displayContext != ItemDisplayContext.FIRST_PERSON_LEFT_HAND) {
            return;
        }

        ItemStack stack =
                renderPassInfo.renderState().getGeckolibData(PointBlankDataTickets.ITEMSTACK);
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof GunItem gun)) {
            return;
        }

        AdsConstraintConfig config = WeaponExtraDataManager.getAdsConstraintConfig(stack);

        LocalPlayer player = Minecraft.getInstance().player;
        GunClientState gunState = player == null ? null : GunClientState.getMainHeldState(player);
        double progress = gunState == null ? 0.0 : resolveAimingProgress(gunState);

        Optional<BoneSnapshot> root = snapshots.get(AdsConstraintConstants.ROOT_BONE);
        Optional<BoneSnapshot> constraint = snapshots.get(AdsConstraintConstants.CONSTRAINT_BONE);
        Optional<BoneSnapshot> camera = config.isCamera()
                ? snapshots.get(AdsConstraintConstants.CAMERA_BONE)
                : Optional.empty();

        if (gateTraceDue()) {
            LOGGER.debug(
                    "AdsConstraint: item={} enabled={} cameraFlag={} player={} gunState={} gunMatches={} progress={} main={} constraint={} cameraBone={}",
                    BuiltInRegistries.ITEM.getKey(stack.getItem()),
                    config.isEnabled(),
                    config.isCamera(),
                    player != null,
                    gunState != null,
                    gunState != null && gunState.getGunItem() == gun,
                    progress,
                    root.isPresent(),
                    constraint.isPresent(),
                    camera.isPresent());
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
        // movement: GunItemRenderer#adjustModelBonesForRender captures its rotX/rotY/rotZ
        // while the weapon is rendered and ClientEventHandler#onPreRenderHandEvent applies
        // them (negated, on X/Y/Z) to the first-person pose stack. That rotation therefore
        // *is* Point Blank's camera animation - the equip/draw, idle, firing, reload and
        // inspect animations all drive it. The camera constraint below therefore never
        // resets the bone and never works from its own previous result: it scales the
        // value the animation just wrote, once, and only while ADS is active.

        if (progress <= 0.0) {
            return;
        }

        // GeckoLib 5 keeps a snapshot's rotation/translation relative to the bone's authored
        // (bind) transform, so the constraint's bind comparison value is 0 for both rotation
        // and translation - exactly the deviation the old absolute values compared against.
        GeoBone constraintBone = renderPassInfo.model()
                .getBone(AdsConstraintConstants.CONSTRAINT_BONE)
                .orElse(null);

        if (root.isEmpty() || constraint.isEmpty()) {
            // This model has no weapon-root bones, so the pose constraint cannot be applied.
            // The camera constraint below does not depend on them and still runs.
            warnMissingBonesOnce(gun, root.isPresent());
        } else {
            // The weapon root always uses the per-axis fractions read from the model's
            // "constraint" bone; this is the original, unchanged behaviour.
            BoneSnapshot constraintSnapshot = constraint.get();
            applyCorrectiveTransform(root.get(),
                    AdsConstraintMath.rotationRetention(constraintRotation(constraintBone, constraintSnapshot, Axis.X)),
                    AdsConstraintMath.rotationRetention(constraintRotation(constraintBone, constraintSnapshot, Axis.Y)),
                    AdsConstraintMath.rotationRetention(constraintRotation(constraintBone, constraintSnapshot, Axis.Z)),
                    AdsConstraintMath.positionRetention(constraintSnapshot.getTranslateX()),
                    AdsConstraintMath.positionRetention(constraintSnapshot.getTranslateY()),
                    AdsConstraintMath.positionRetention(constraintSnapshot.getTranslateZ()),
                    progress);
        }

        if (camera.isPresent()) {
            BoneSnapshot cameraSnapshot = camera.get();
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
            } else if (constraint.isPresent()) {
                // Backwards compatible: without cameraConstraint the camera keeps using the
                // same per-axis fractions as the weapon root.
                BoneSnapshot constraintSnapshot = constraint.get();
                rotRetX = AdsConstraintMath.rotationRetention(constraintRotation(constraintBone, constraintSnapshot, Axis.X));
                rotRetY = AdsConstraintMath.rotationRetention(constraintRotation(constraintBone, constraintSnapshot, Axis.Y));
                rotRetZ = AdsConstraintMath.rotationRetention(constraintRotation(constraintBone, constraintSnapshot, Axis.Z));
                posRetX = AdsConstraintMath.positionRetention(constraintSnapshot.getTranslateX());
                posRetY = AdsConstraintMath.positionRetention(constraintSnapshot.getTranslateY());
                posRetZ = AdsConstraintMath.positionRetention(constraintSnapshot.getTranslateZ());
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
            if (!cameraAuthoredSinceLastCorrection(cameraSnapshot)) {
                rotRetX = AdsConstraintMath.FULL_RETENTION;
                rotRetY = AdsConstraintMath.FULL_RETENTION;
                rotRetZ = AdsConstraintMath.FULL_RETENTION;
                posRetX = AdsConstraintMath.FULL_RETENTION;
                posRetY = AdsConstraintMath.FULL_RETENTION;
                posRetZ = AdsConstraintMath.FULL_RETENTION;
            }

            applyCorrectiveTransform(cameraSnapshot, rotRetX, rotRetY, rotRetZ,
                    posRetX, posRetY, posRetZ, progress);
            rememberCameraCorrection(cameraSnapshot);
        }
    }

    /** The three rotation axes of a bone snapshot. */
    private enum Axis { X, Y, Z }

    /**
     * GeckoLib 5 stores only the animated delta in a snapshot, while the old GeoBone getters
     * returned the absolute animated rotation. Point Blank itself reads a bone's animation the
     * same way ({@code baseRotX() + snapshot.getRotX()}), so the constraint bone's authored
     * per-axis fraction is read the same way here.
     */
    private static float constraintRotation(GeoBone bone, BoneSnapshot snapshot, Axis axis) {
        float base = 0.0F;
        if (bone != null) {
            base = switch (axis) {
                case X -> bone.baseRotX();
                case Y -> bone.baseRotY();
                case Z -> bone.baseRotZ();
            };
        }
        float animated = switch (axis) {
            case X -> snapshot.getRotX();
            case Y -> snapshot.getRotY();
            case Z -> snapshot.getRotZ();
        };
        return base + animated;
    }

    private static boolean cameraAuthoredSinceLastCorrection(BoneSnapshot camera) {
        if (camera != correctedCamera) {
            return true;
        }
        return camera.getRotX() != correctedCameraRotX
                || camera.getRotY() != correctedCameraRotY
                || camera.getRotZ() != correctedCameraRotZ
                || camera.getTranslateX() != correctedCameraPosX
                || camera.getTranslateY() != correctedCameraPosY
                || camera.getTranslateZ() != correctedCameraPosZ;
    }

    /** Remembers what the last camera correction left on the snapshot. */
    private static void rememberCameraCorrection(BoneSnapshot camera) {
        correctedCamera = camera;
        correctedCameraRotX = camera.getRotX();
        correctedCameraRotY = camera.getRotY();
        correctedCameraRotZ = camera.getRotZ();
        correctedCameraPosX = camera.getTranslateX();
        correctedCameraPosY = camera.getTranslateY();
        correctedCameraPosZ = camera.getTranslateZ();
    }

    private static void applyCorrectiveTransform(BoneSnapshot target,
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
        float beforePosX = target.getTranslateX();
        float beforePosY = target.getTranslateY();
        float beforePosZ = target.getTranslateZ();

        if (!constrainRotation && !constrainPosition) {
            if (trace) {
                LOGGER.debug("AdsConstraint '{}': SKIPPED (all retention=1). rot=({},{},{}) pos=({},{},{})",
                        target.getBone().name(), rotRetX, rotRetY, rotRetZ,
                        posRetX, posRetY, posRetZ);
            }
            return;
        }

        // In GeckoLib 5 a snapshot's values are already relative to the bone's bind
        // transform, so the bind comparison value is 0 on every axis.
        if (constrainRotation) {
            target.setRotation(
                    AdsConstraintMath.applyCorrection(target.getRotX(), 0.0F, rotRetX, progress),
                    AdsConstraintMath.applyCorrection(target.getRotY(), 0.0F, rotRetY, progress),
                    AdsConstraintMath.applyCorrection(target.getRotZ(), 0.0F, rotRetZ, progress));
        }

        if (constrainPosition) {
            target.setTranslation(
                    AdsConstraintMath.applyCorrection(target.getTranslateX(), 0.0F, posRetX, progress),
                    AdsConstraintMath.applyCorrection(target.getTranslateY(), 0.0F, posRetY, progress),
                    AdsConstraintMath.applyCorrection(target.getTranslateZ(), 0.0F, posRetZ, progress));
        }

        if (trace) {
            LOGGER.debug(
                    "AdsConstraint '{}': progress={} retention rot=({},{},{}) pos=({},{},{}) rot ({},{},{})->({},{},{}) pos ({},{},{})->({},{},{})",
                    target.getBone().name(), progress, rotRetX, rotRetY, rotRetZ, posRetX, posRetY, posRetZ,
                    beforeRotX, beforeRotY, beforeRotZ, target.getRotX(), target.getRotY(), target.getRotZ(),
                    beforePosX, beforePosY, beforePosZ, target.getTranslateX(), target.getTranslateY(), target.getTranslateZ());
        }
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

    private static void warnMissingBonesOnce(GunItem gun, boolean rootBonePresent) {
        Identifier id = BuiltInRegistries.ITEM.getKey(gun);
        String key = id == null ? gun.getClass().getName() : id.toString();
        if (WARNED_WEAPONS.add(key)) {
            LOGGER.warn("AdsConstraint is enabled for '{}' but the model is missing the '{}' bone{}; "
                            + "the constraint is skipped for this weapon",
                    key,
                    rootBonePresent ? AdsConstraintConstants.CONSTRAINT_BONE : AdsConstraintConstants.ROOT_BONE,
                    rootBonePresent ? "" : " (the weapon root)");
        }
    }
}
