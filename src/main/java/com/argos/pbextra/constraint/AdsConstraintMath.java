package com.argos.pbextra.constraint;

public final class AdsConstraintMath {

    public static final float FULL_RETENTION = 1.0F;
    public static final float NO_RETENTION = 0.0F;

    private AdsConstraintMath() {
    }

    public static float rotationRetention(float boneRotation) {
        if (!Float.isFinite(boneRotation)) {
            return FULL_RETENTION;
        }
        return clampRetention(Math.abs((float) Math.toDegrees(boneRotation)));
    }

    public static float positionRetention(float bonePosition) {
        if (!Float.isFinite(bonePosition)) {
            return FULL_RETENTION;
        }
        return clampRetention(Math.abs(bonePosition));
    }

    public static float clampRetention(float retention) {
        if (!Float.isFinite(retention) || retention <= NO_RETENTION) {
            return NO_RETENTION;
        }
        return Math.min(retention, FULL_RETENTION);
    }

    public static boolean isConstrained(float retention) {
        return retention < FULL_RETENTION;
    }


    public static float applyCorrection(float animated, float bind, float retention, double weight) {
        if (!isConstrained(retention) || !(weight > 0.0)) {
            return animated;
        }
        if (!Float.isFinite(animated) || !Float.isFinite(bind)) {
            return animated;
        }

        float deviation = animated - bind;
        if (deviation == 0.0F) {
            return animated;
        }

        float correction = (float) (deviation * ((double) retention - 1.0) * weight);
        return animated + correction;
    }
}
