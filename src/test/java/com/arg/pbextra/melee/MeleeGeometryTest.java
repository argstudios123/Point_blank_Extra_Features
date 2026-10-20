package com.arg.pbextra.melee;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the pure melee cone/reach/timing maths. These do not need a
 * Minecraft runtime, which is why the geometry lives in its own class.
 */
class MeleeGeometryTest {

    private static final double EPSILON = 1.0E-6;

    @Test
    void targetStraightAheadIsInsideCone() {
        assertTrue(MeleeGeometry.isWithinCone(0.0, 3.0, 0.0, 1.0, 40.0));
    }

    @Test
    void targetsInsideTheHalfAngleAreIncluded() {
        // angleRange is the FULL width: 40 degrees means 20 degrees each side.
        assertTrue(insideCone(19.9, 40.0));
        assertTrue(insideCone(-19.9, 40.0));
        assertTrue(insideCone(20.0, 40.0));
    }

    @Test
    void targetsOutsideTheHalfAngleAreExcluded() {
        assertFalse(insideCone(20.1, 40.0));
        assertFalse(insideCone(-20.1, 40.0));
        assertFalse(insideCone(40.0, 40.0));
    }

    @Test
    void targetsBehindOrToTheSideAreExcluded() {
        assertFalse(MeleeGeometry.isWithinCone(0.0, -3.0, 0.0, 1.0, 40.0));
        assertFalse(MeleeGeometry.isWithinCone(3.0, 0.0, 0.0, 1.0, 40.0));
    }

    @Test
    void angleRangeIsAFullWidthNotAHalfWidth() {
        assertTrue(insideCone(25.0, 60.0));
        assertFalse(insideCone(25.0, 40.0));
    }

    @Test
    void zeroLengthOffsetIsHandledAsStraightAhead() {
        assertTrue(MeleeGeometry.isWithinCone(0.0, 0.0, 0.0, 1.0, 40.0));
        assertEquals(0.0, MeleeGeometry.angleDegrees(0.0, 0.0, 0.0, 1.0), EPSILON);
    }

    @Test
    void zeroLengthFacingVectorIsHandledWithoutNaN() {
        double angle = MeleeGeometry.angleDegrees(1.0, 0.0, 0.0, 0.0);
        assertFalse(Double.isNaN(angle));
        assertEquals(180.0, angle, EPSILON);
        assertFalse(MeleeGeometry.isWithinCone(1.0, 0.0, 0.0, 0.0, 40.0));
    }

    @Test
    void dotProductIsClampedBeforeAcos() {
        // Parallel and antiparallel vectors are the cases where floating point
        // error tends to push the cosine just outside [-1, 1].
        assertEquals(0.0, MeleeGeometry.angleDegrees(2.0, 0.0, 5.0, 0.0), EPSILON);
        assertEquals(180.0, MeleeGeometry.angleDegrees(2.0, 0.0, -5.0, 0.0), EPSILON);
    }

    @Test
    void fullCircleIncludesEveryDirection() {
        assertTrue(MeleeGeometry.isWithinCone(0.0, -3.0, 0.0, 1.0, 360.0));
        assertTrue(MeleeGeometry.isWithinCone(3.0, 0.0, 0.0, 1.0, 360.0));
    }

    @Test
    void zeroAngleRangeOnlyAllowsTheExactFacingLine() {
        assertTrue(MeleeGeometry.isWithinCone(0.0, 3.0, 0.0, 1.0, 0.0));
        assertFalse(insideCone(0.1, 0.0));
    }

    @Test
    void invalidAnglesAreRejected() {
        assertFalse(MeleeGeometry.isWithinCone(0.0, 3.0, 0.0, 1.0, -1.0));
        assertFalse(MeleeGeometry.isWithinCone(0.0, 3.0, 0.0, 1.0, Double.NaN));
    }

    @Test
    void millisAreRoundedUpToWholeTicks() {
        assertEquals(0, MeleeGeometry.millisToTicks(0));
        assertEquals(1, MeleeGeometry.millisToTicks(1));
        assertEquals(1, MeleeGeometry.millisToTicks(50));
        assertEquals(2, MeleeGeometry.millisToTicks(51));
        assertEquals(4, MeleeGeometry.millisToTicks(200));
        assertEquals(5, MeleeGeometry.millisToTicks(220));
        assertEquals(14, MeleeGeometry.millisToTicks(700));
    }

    @Test
    void boxDistanceIsZeroInsideAndMeasuresTheNearestFace() {
        assertEquals(0.0, MeleeGeometry.distanceSquaredToBox(
                0.0, 1.0, 0.0,
                -1.0, 0.0, -1.0,
                1.0, 2.0, 1.0), EPSILON);

        assertEquals(4.0, MeleeGeometry.distanceSquaredToBox(
                3.0, 1.0, 0.0,
                -1.0, 0.0, -1.0,
                1.0, 2.0, 1.0), EPSILON);

        assertEquals(3.0, MeleeGeometry.distanceSquaredToBox(
                -2.0, -1.0, -2.0,
                -1.0, 0.0, -1.0,
                1.0, 2.0, 1.0), EPSILON);
    }

    private static boolean insideCone(double degrees, double angleRange) {
        double radians = Math.toRadians(degrees);
        // Facing is +Z; rotate the offset by the angle around the Y axis.
        double offsetX = Math.sin(radians) * 3.0;
        double offsetZ = Math.cos(radians) * 3.0;
        return MeleeGeometry.isWithinCone(offsetX, offsetZ, 0.0, 1.0, angleRange);
    }
}
