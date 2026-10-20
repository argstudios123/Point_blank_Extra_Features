package com.arg.pbextra.client.bulletholes;

import com.arg.pbextra.bullethole.BulletHoleConfig;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Face-local placement tests for bullet holes.
 *
 * <p>The old implementation did not reject edge impacts; it always clamped the
 * whole decal centre inward. These tests pin down the replacement: a centred
 * impact keeps its configured size, edge and corner impacts stay centred while
 * the visible decal shrinks, and very-near-boundary impacts use a 50% minimum
 * with only the small inward nudge needed to stay on the face.</p>
 */
class BulletHolePlacementTest {

    private static final AABB FULL_CUBE = new AABB(0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D);
    private static final AABB BOTTOM_SLAB = new AABB(0.0D, 0.0D, 0.0D, 1.0D, 0.5D, 1.0D);
    private static final double OFFSET = 0.0025D;
    private static final double EPSILON = 1.0E-6D;
    private static final float SIZE = BulletHoleConfig.DEFAULT_SIZE;

    @Test
    void centreImpactKeepsConfiguredSizeOnEveryFace() {
        for (Direction face : Direction.values()) {
            BulletHolePlacement.Placement placement = BulletHolePlacement.place(
                    FULL_CUBE, face, hit(FULL_CUBE, face, 0.5D, 0.5D),
                    SIZE, 0.37D, OFFSET);

            assertNotNull(placement, face.toString());
            assertEquals(SIZE, placement.size(), 1.0E-5F, face.toString());
            assertInsideFace(FULL_CUBE, face, placement);
            assertCentredOn(FULL_CUBE, face, hit(FULL_CUBE, face, 0.5D, 0.5D),
                    placement);
        }
    }

    @Test
    void edgeImpactStaysCentredAndShrinksToFitOnEveryFace() {
        for (Direction face : Direction.values()) {
            Vec3 hit = hit(FULL_CUBE, face, 0.05D, 0.5D);
            BulletHolePlacement.Placement placement = BulletHolePlacement.place(
                    FULL_CUBE, face, hit, SIZE, Math.PI / 4.0D, OFFSET);

            assertNotNull(placement, face.toString());
            assertTrue(placement.size() < SIZE, face.toString());
            assertTrue(placement.size() >= SIZE * BulletHolePlacement.MIN_VISIBLE_FRACTION,
                    face.toString());
            assertInsideFace(FULL_CUBE, face, placement);
            assertCentredOn(FULL_CUBE, face, hit, placement);
        }
    }

    @Test
    void cornerImpactStaysCentredAndShrinksToFitOnEveryFace() {
        for (Direction face : Direction.values()) {
            Vec3 hit = hit(FULL_CUBE, face, 0.05D, 0.05D);
            BulletHolePlacement.Placement placement = BulletHolePlacement.place(
                    FULL_CUBE, face, hit, SIZE, 0.9D, OFFSET);

            assertNotNull(placement, face.toString());
            assertTrue(placement.size() < SIZE, face.toString());
            assertTrue(placement.size() >= SIZE * BulletHolePlacement.MIN_VISIBLE_FRACTION,
                    face.toString());
            assertInsideFace(FULL_CUBE, face, placement);
            assertCentredOn(FULL_CUBE, face, hit, placement);
        }
    }

    @Test
    void veryNearEdgeKeepsALegibleMinimumAndNudgesOnEveryFace() {
        for (Direction face : Direction.values()) {
            for (double fraction : new double[]{0.002D, 0.01D}) {
                Vec3 hit = hit(FULL_CUBE, face, fraction, fraction);
                BulletHolePlacement.Placement placement = BulletHolePlacement.place(
                        FULL_CUBE, face, hit, SIZE, Math.PI / 4.0D, OFFSET);

                assertNotNull(placement, face + " " + fraction);
                assertEquals(SIZE * BulletHolePlacement.MIN_VISIBLE_FRACTION,
                        placement.size(), 1.0E-5F, face + " " + fraction);
                assertInsideFace(FULL_CUBE, face, placement);
            }
        }
    }

    @Test
    void exactBoundaryFallsBackToMinimumVisibleSizeWithoutOverhanging() {
        for (Direction face : Direction.values()) {
            for (double u : new double[]{0.0D, 1.0D}) {
                for (double v : new double[]{0.0D, 1.0D}) {
                    Vec3 hit = hit(FULL_CUBE, face, u, v);
                    BulletHolePlacement.Placement placement = BulletHolePlacement.place(
                            FULL_CUBE, face, hit, SIZE, Math.PI / 4.0D, OFFSET);

                    assertNotNull(placement, face + " " + u + "," + v);
                    assertEquals(SIZE * BulletHolePlacement.MIN_VISIBLE_FRACTION,
                            placement.size(), 1.0E-5F, face + " " + u + "," + v);
                    assertInsideFace(FULL_CUBE, face, placement);
                }
            }
        }
    }

    @Test
    void impactAwayFromTheImmediateEdgeKeepsFullSizeOnEveryFace() {
        for (Direction face : Direction.values()) {
            Vec3 hit = hit(FULL_CUBE, face, 0.1D, 0.1D);
            BulletHolePlacement.Placement placement = BulletHolePlacement.place(
                    FULL_CUBE, face, hit, SIZE, Math.PI / 4.0D, OFFSET);

            assertNotNull(placement, face.toString());
            assertEquals(SIZE, placement.size(), 1.0E-5F, face.toString());
            assertInsideFace(FULL_CUBE, face, placement);
            assertCentredOn(FULL_CUBE, face, hit, placement);
        }
    }

    @Test
    void largeConfiguredSizeStillFitsInsideTheFace() {
        BulletHolePlacement.Placement placement = BulletHolePlacement.place(
                FULL_CUBE, Direction.SOUTH, hit(FULL_CUBE, Direction.SOUTH, 0.5D, 0.5D),
                1.0F, Math.PI / 4.0D, OFFSET);

        assertNotNull(placement);
        assertEquals((float) (1.0D / Math.sqrt(2.0D)), placement.size(), 1.0E-5F);
        assertInsideFace(FULL_CUBE, Direction.SOUTH, placement);
        assertCentredOn(FULL_CUBE, Direction.SOUTH,
                hit(FULL_CUBE, Direction.SOUTH, 0.5D, 0.5D), placement);
    }

    @Test
    void partialShapeTopFaceKeepsTheDecalOnItsSupportingPlane() {
        Vec3 hit = hit(BOTTOM_SLAB, Direction.UP, 0.05D, 0.5D);
        BulletHolePlacement.Placement placement = BulletHolePlacement.place(
                BOTTOM_SLAB, Direction.UP, hit, SIZE, 0.4D, OFFSET);

        assertNotNull(placement);
        assertInsideFace(BOTTOM_SLAB, Direction.UP, placement);
        assertCentredOn(BOTTOM_SLAB, Direction.UP, hit, placement);
        assertEquals(0.5D + OFFSET, centroid(placement).y, EPSILON);
    }

    @Test
    void rotationSweepNeverLeavesTheFaceAtEdgesOrCorners() {
        for (Direction face : Direction.values()) {
            for (double fraction : new double[]{0.002D, 0.01D, 0.05D, 0.5D}) {
                for (int step = 0; step < 16; step++) {
                    double angle = Math.PI * 2.0D * step / 16.0D;
                    Vec3 hit = hit(FULL_CUBE, face, fraction, fraction);
                    BulletHolePlacement.Placement placement = BulletHolePlacement.place(
                            FULL_CUBE, face, hit, SIZE, angle, OFFSET);

                    assertNotNull(placement, face + " " + fraction + " " + angle);
                    assertTrue(placement.size() >= SIZE
                            * BulletHolePlacement.MIN_VISIBLE_FRACTION - 1.0E-5F);
                    assertInsideFace(FULL_CUBE, face, placement);
                }
            }
        }
    }

    @Test
    void everyTexturePixelImpactProducesAVisibleDecalOnEveryFace() {
        for (Direction face : Direction.values()) {
            for (int pixelU = 0; pixelU <= 16; pixelU++) {
                for (int pixelV = 0; pixelV <= 16; pixelV++) {
                    Vec3 hit = hit(FULL_CUBE, face, pixelU / 16.0D,
                            pixelV / 16.0D);
                    BulletHolePlacement.Placement placement = BulletHolePlacement.place(
                            FULL_CUBE, face, hit, SIZE, 0.73D, OFFSET);

                    assertNotNull(placement,
                            face + " pixel " + pixelU + "," + pixelV);
                    assertTrue(placement.size() >= SIZE
                                    * BulletHolePlacement.MIN_VISIBLE_FRACTION
                                    - 1.0E-5F,
                            face + " pixel " + pixelU + "," + pixelV);
                    assertInsideFace(FULL_CUBE, face, placement);
                }
            }
        }
    }

    @Test
    void faceTooSmallForMinimumDecalIsRejected() {
        AABB thinFace = new AABB(0.0D, 0.0D, 0.0D, 0.005D, 1.0D, 1.0D);
        assertNull(BulletHolePlacement.place(
                thinFace, Direction.SOUTH, hit(thinFace, Direction.SOUTH, 0.5D, 0.5D),
                SIZE, Math.PI / 4.0D, OFFSET));
    }

    @Test
    void impactOutsideTheSelectedFaceIsRejected() {
        Vec3 boundary = hit(FULL_CUBE, Direction.SOUTH, 1.0D, 0.5D);
        Vec3 outside = boundary.add(0.01D, 0.0D, 0.0D);
        assertNull(BulletHolePlacement.place(
                FULL_CUBE, Direction.SOUTH, outside, SIZE, 0.0D, OFFSET));
    }

    @Test
    void selectFaceBoxAcceptsAnExactCornerAndRejectsAFarHit() {
        List<AABB> boxes = List.of(FULL_CUBE);
        Vec3 corner = hit(FULL_CUBE, Direction.SOUTH, 0.0D, 0.0D);

        assertEquals(FULL_CUBE, BulletHolePlacement.selectFaceBox(
                boxes, Direction.SOUTH, corner));
        assertNull(BulletHolePlacement.selectFaceBox(
                boxes, Direction.SOUTH, corner.add(-0.01D, 0.0D, 0.0D)));
    }

    private static Vec3 hit(AABB box, Direction face, double uFraction,
                            double vFraction) {
        Direction.Axis uAxis = uAxis(face);
        Direction.Axis vAxis = vAxis(face);
        double[] coordinates = {
                (box.minX + box.maxX) * 0.5D,
                (box.minY + box.maxY) * 0.5D,
                (box.minZ + box.maxZ) * 0.5D
        };
        coordinates[axisIndex(uAxis)] = axisMin(box, uAxis)
                + axisLength(box, uAxis) * uFraction;
        coordinates[axisIndex(vAxis)] = axisMin(box, vAxis)
                + axisLength(box, vAxis) * vFraction;
        coordinates[axisIndex(face.getAxis())] =
                face.getAxisDirection() == Direction.AxisDirection.POSITIVE
                        ? axisMax(box, face.getAxis()) : axisMin(box, face.getAxis());
        return new Vec3(coordinates[0], coordinates[1], coordinates[2]);
    }

    private static void assertInsideFace(AABB box, Direction face,
                                         BulletHolePlacement.Placement placement) {
        double plane = face.getAxisDirection() == Direction.AxisDirection.POSITIVE
                ? axisMax(box, face.getAxis()) : axisMin(box, face.getAxis());
        double expectedNormal = plane
                + face.getStepX() * OFFSET
                + face.getStepY() * OFFSET
                + face.getStepZ() * OFFSET;
        Direction.Axis uAxis = uAxis(face);
        Direction.Axis vAxis = vAxis(face);

        for (Vec3 corner : corners(placement)) {
            assertTrue(axisValue(corner, uAxis) >= axisMin(box, uAxis) - EPSILON,
                    face + " u below face: " + corner);
            assertTrue(axisValue(corner, uAxis) <= axisMax(box, uAxis) + EPSILON,
                    face + " u above face: " + corner);
            assertTrue(axisValue(corner, vAxis) >= axisMin(box, vAxis) - EPSILON,
                    face + " v below face: " + corner);
            assertTrue(axisValue(corner, vAxis) <= axisMax(box, vAxis) + EPSILON,
                    face + " v above face: " + corner);
            assertEquals(expectedNormal, axisValue(corner, face.getAxis()), EPSILON,
                    face + " normal offset: " + corner);
        }
    }

    private static void assertCentredOn(AABB box, Direction face, Vec3 hit,
                                        BulletHolePlacement.Placement placement) {
        Vec3 centre = centroid(placement);
        assertEquals(axisValue(hit, uAxis(face)), axisValue(centre, uAxis(face)),
                EPSILON, face + " u centre");
        assertEquals(axisValue(hit, vAxis(face)), axisValue(centre, vAxis(face)),
                EPSILON, face + " v centre");
    }

    private static Vec3 centroid(BulletHolePlacement.Placement placement) {
        Vec3 sum = placement.c0().add(placement.c1())
                .add(placement.c2()).add(placement.c3());
        return sum.scale(0.25D);
    }

    private static List<Vec3> corners(BulletHolePlacement.Placement placement) {
        return List.of(placement.c0(), placement.c1(),
                placement.c2(), placement.c3());
    }

    private static Direction.Axis uAxis(Direction face) {
        return switch (face) {
            case UP -> Direction.Axis.Z;
            case DOWN -> Direction.Axis.X;
            case SOUTH -> Direction.Axis.X;
            case NORTH -> Direction.Axis.Y;
            case EAST -> Direction.Axis.Y;
            case WEST -> Direction.Axis.Z;
        };
    }

    private static Direction.Axis vAxis(Direction face) {
        return switch (face) {
            case UP -> Direction.Axis.X;
            case DOWN -> Direction.Axis.Z;
            case SOUTH -> Direction.Axis.Y;
            case NORTH -> Direction.Axis.X;
            case EAST -> Direction.Axis.Z;
            case WEST -> Direction.Axis.Y;
        };
    }

    private static int axisIndex(Direction.Axis axis) {
        return switch (axis) {
            case X -> 0;
            case Y -> 1;
            case Z -> 2;
        };
    }

    private static double axisValue(Vec3 vec, Direction.Axis axis) {
        return switch (axis) {
            case X -> vec.x;
            case Y -> vec.y;
            case Z -> vec.z;
        };
    }

    private static double axisMin(AABB box, Direction.Axis axis) {
        return switch (axis) {
            case X -> box.minX;
            case Y -> box.minY;
            case Z -> box.minZ;
        };
    }

    private static double axisMax(AABB box, Direction.Axis axis) {
        return switch (axis) {
            case X -> box.maxX;
            case Y -> box.maxY;
            case Z -> box.maxZ;
        };
    }

    private static double axisLength(AABB box, Direction.Axis axis) {
        return axisMax(box, axis) - axisMin(box, axis);
    }
}

