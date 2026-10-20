package com.arg.pbextra.melee;


/**
 * Pure geometry and timing helpers used by the melee hit detection.
 * <p>
 * Nothing in this class touches Minecraft classes, which keeps the angular and
 * reach calculations independently testable.
 */
public final class MeleeGeometry {

    /** Minecraft runs at 20 ticks per second, so one tick is 50 ms. */
    public static final double MILLIS_PER_TICK = 50.0;

    /** A full circle; an angle range of 360 degrees covers every direction. */
    public static final double FULL_CIRCLE_DEGREES = 360.0;

    private static final double LENGTH_EPSILON_SQUARED = 1.0E-12;
    private static final double ANGLE_EPSILON_DEGREES = 1.0E-6;

    private MeleeGeometry() {
    }

    /**
     * Converts configured animation-time milliseconds to whole server ticks,
     * rounding up so a hit never lands before its configured time.
     */
    public static int millisToTicks(int millis) {
        if (millis <= 0) {
            return 0;
        }
        return (int) ((millis + 49L) / (long) MILLIS_PER_TICK);
    }

    /**
     * Returns the unsigned angle in degrees between two horizontal vectors.
     * Zero-length offsets count as straight ahead; a zero-length facing vector
     * has no direction and therefore counts as 180 degrees (never in front).
     */
    public static double angleDegrees(double offsetX, double offsetZ,
                                      double facingX, double facingZ) {
        double offsetLengthSquared = offsetX * offsetX + offsetZ * offsetZ;
        double facingLengthSquared = facingX * facingX + facingZ * facingZ;

        if (offsetLengthSquared <= LENGTH_EPSILON_SQUARED) {
            // The target is directly above or below the attacker. There is no
            // horizontal direction, so it is treated as straight ahead; the
            // reach check still decides whether it can be hit at all.
            return 0.0;
        }
        if (facingLengthSquared <= LENGTH_EPSILON_SQUARED) {
            return 180.0;
        }

        double dot = offsetX * facingX + offsetZ * facingZ;
        double denominator = Math.sqrt(offsetLengthSquared) * Math.sqrt(facingLengthSquared);
        double cosine = dot / denominator;

        // acos is only defined on [-1, 1]; floating point error can move a
        // parallel or antiparallel dot product just outside that range.
        if (cosine > 1.0) {
            cosine = 1.0;
        } else if (cosine < -1.0) {
            cosine = -1.0;
        }

        return Math.toDegrees(Math.acos(cosine));
    }

    /**
     * Tests whether a horizontal offset lies inside a frontal cone.
     *
     * @param offsetX          horizontal X distance from the attacker to the target
     * @param offsetZ          horizontal Z distance from the attacker to the target
     * @param facingX          horizontal X component of the attacker's facing direction
     * @param facingZ          horizontal Z component of the attacker's facing direction
     * @param angleRangeDegrees FULL width of the cone in degrees, so 40 means
     *                          20 degrees on either side of the facing direction
     */
    public static boolean isWithinCone(double offsetX, double offsetZ,
                                       double facingX, double facingZ,
                                       double angleRangeDegrees) {
        if (!Double.isFinite(angleRangeDegrees) || angleRangeDegrees < 0.0) {
            return false;
        }
        if (angleRangeDegrees >= FULL_CIRCLE_DEGREES) {
            return true;
        }

        double halfAngle = angleRangeDegrees / 2.0;
        return angleDegrees(offsetX, offsetZ, facingX, facingZ)
                <= halfAngle + ANGLE_EPSILON_DEGREES;
    }

    /**
     * Squared distance from a point to the nearest point of an axis-aligned box.
     * The result is 0 when the point is inside the box, which makes this suitable
     * for reach checks against large entities.
     */
    public static double distanceSquaredToBox(double pointX, double pointY, double pointZ,
                                              double minX, double minY, double minZ,
                                              double maxX, double maxY, double maxZ) {
        double dx = distanceToInterval(pointX, minX, maxX);
        double dy = distanceToInterval(pointY, minY, maxY);
        double dz = distanceToInterval(pointZ, minZ, maxZ);
        return dx * dx + dy * dy + dz * dz;
    }

    private static double distanceToInterval(double value, double min, double max) {
        if (value < min) {
            return min - value;
        }
        if (value > max) {
            return value - max;
        }
        return 0.0;
    }
}
