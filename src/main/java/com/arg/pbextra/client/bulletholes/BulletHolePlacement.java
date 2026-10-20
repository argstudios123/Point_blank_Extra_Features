package com.arg.pbextra.client.bulletholes;

import com.arg.pbextra.bullethole.BulletHoleConfig;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;


// it will use Decay and have particle textures to use the
final class BulletHolePlacement {

    /** The same small tolerance the block-hit result can carry at a boundary. */
    static final double HIT_TOLERANCE = 1.0E-3D;

    /**
     * Smallest edge/corner decal as a fraction of the configured size. A hole
     * smaller than this reads as "no hole" in normal gameplay, so the placement
     * nudges slightly inward instead of shrinking without limit.
     */
    static final double MIN_VISIBLE_FRACTION = 0.5D;

    private static final double EPSILON = 1.0E-6D;
    private static final double SQRT_TWO = Math.sqrt(2.0D);

    private BulletHolePlacement() {
    }

    /**
     * Four local-space corners in the same order used by the renderer
     * ({@code c0, c1, c2, c3}).
     */
    record Placement(float size, Vec3 c0, Vec3 c1, Vec3 c2, Vec3 c3) {
    }

    /**
     * Selects the collision box whose face contains the projected hit point and
     * whose face plane is nearest to the impact along the face normal.
     */
    static AABB selectFaceBox(List<AABB> boxes, Direction face, Vec3 localHit) {
        if (boxes == null || boxes.isEmpty() || face == null || localHit == null) {
            return null;
        }

        Direction.Axis uAxis = uAxis(face);
        Direction.Axis vAxis = vAxis(face);
        Direction.Axis nAxis = face.getAxis();
        double u = axisValue(localHit, uAxis);
        double v = axisValue(localHit, vAxis);
        double n = axisValue(localHit, nAxis);

        AABB best = null;
        double bestDistance = Double.MAX_VALUE;
        for (AABB box : boxes) {
            if (box == null
                    || u < axisMin(box, uAxis) - HIT_TOLERANCE
                    || u > axisMax(box, uAxis) + HIT_TOLERANCE
                    || v < axisMin(box, vAxis) - HIT_TOLERANCE
                    || v > axisMax(box, vAxis) + HIT_TOLERANCE) {
                continue;
            }
            double plane = face.getAxisDirection() == Direction.AxisDirection.POSITIVE
                    ? axisMax(box, nAxis) : axisMin(box, nAxis);
            double distance = Math.abs(n - plane);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = box;
            }
        }
        return best;
    }


    static Placement place(AABB faceBox, Direction face, Vec3 localHit,
                           float requestedSize, double angle,
                           double surfaceOffset) {
        if (faceBox == null || face == null || localHit == null
                || !Float.isFinite(requestedSize)
                || requestedSize < BulletHoleConfig.MIN_SIZE
                || !Double.isFinite(angle)
                || !Double.isFinite(surfaceOffset)) {
            return null;
        }

        Direction.Axis uAxis = uAxis(face);
        Direction.Axis vAxis = vAxis(face);
        Direction.Axis nAxis = face.getAxis();

        double uMin = axisMin(faceBox, uAxis);
        double uMax = axisMax(faceBox, uAxis);
        double vMin = axisMin(faceBox, vAxis);
        double vMax = axisMax(faceBox, vAxis);
        double faceWidth = uMax - uMin;
        double faceHeight = vMax - vMin;

        double hitU = axisValue(localHit, uAxis);
        double hitV = axisValue(localHit, vAxis);

        if (!(faceWidth > 0.0D) || !(faceHeight > 0.0D)
                || hitU < uMin - HIT_TOLERANCE || hitU > uMax + HIT_TOLERANCE
                || hitV < vMin - HIT_TOLERANCE || hitV > vMax + HIT_TOLERANCE) {
            return null;
        }

        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        // A square of half-size h has this axis-aligned half-extent after an
        // arbitrary rotation. The same value applies to u and v.
        double rotationExtent = Math.abs(cos) + Math.abs(sin);

        double minHalf = BulletHoleConfig.MIN_SIZE * 0.5D;
        // Keep the previous conservative cap for very large configured sizes:
        // a square still fits anywhere in the face at any rotation.
        double maximumHalf = Math.min(faceWidth, faceHeight) / (2.0D * SQRT_TWO);
        double requestedHalf = Math.min(requestedSize * 0.5D, maximumHalf);
        double minimumVisibleHalf = Math.max(minHalf,
                requestedHalf * MIN_VISIBLE_FRACTION);
        double availableHalf = Math.min(
                Math.min(nonNegative(hitU - uMin), nonNegative(uMax - hitU)),
                Math.min(nonNegative(hitV - vMin), nonNegative(vMax - hitV)))
                / rotationExtent;

        double half;
        double uCenter;
        double vCenter;
        if (availableHalf + EPSILON >= minimumVisibleHalf) {
            // Keep the configured size and the exact impact point whenever the
            // rotated square fits. Near an edge this value shrinks smoothly,
            // but never below the smallest clearly visible decal.
            half = Math.min(requestedHalf, availableHalf);
            uCenter = clamp(hitU, uMin, uMax);
            vCenter = clamp(hitV, vMin, vMax);
        } else {
            // Even the minimum visible decal cannot be centred here. Move the
            // centre inward by the smallest amount that fits.
            half = minimumVisibleHalf;
            double inset = half * rotationExtent;
            Double fittedU = clampFitting(hitU, uMin, uMax, inset);
            Double fittedV = clampFitting(hitV, vMin, vMax, inset);
            if (fittedU == null || fittedV == null) {
                return null;
            }
            uCenter = fittedU;
            vCenter = fittedV;
        }

        double plane = face.getAxisDirection() == Direction.AxisDirection.POSITIVE
                ? axisMax(faceBox, nAxis) : axisMin(faceBox, nAxis);
        double[] center = new double[3];
        center[axisIndex(uAxis)] = uCenter;
        center[axisIndex(vAxis)] = vCenter;
        center[axisIndex(nAxis)] = plane
                + face.getStepX() * surfaceOffset
                + face.getStepY() * surfaceOffset
                + face.getStepZ() * surfaceOffset;

        Vec3 uDir = axisUnit(uAxis);
        Vec3 vDir = axisUnit(vAxis);
        Vec3 rotatedU = uDir.scale(cos).add(vDir.scale(sin));
        Vec3 rotatedV = vDir.scale(cos).subtract(uDir.scale(sin));
        Vec3 origin = new Vec3(center[0], center[1], center[2]);
        Vec3 c0 = origin.subtract(rotatedU.scale(half)).subtract(rotatedV.scale(half));
        Vec3 c1 = origin.add(rotatedU.scale(half)).subtract(rotatedV.scale(half));
        Vec3 c2 = origin.add(rotatedU.scale(half)).add(rotatedV.scale(half));
        Vec3 c3 = origin.subtract(rotatedU.scale(half)).add(rotatedV.scale(half));

        return new Placement((float) (half * 2.0D), c0, c1, c2, c3);
    }

    /** Projects a hit that may sit a fraction outside the box onto the face. */
    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double nonNegative(double value) {
        return Math.max(0.0D, value);
    }

    /**
     * Returns a centre that keeps {@code inset} of room on both sides, or
     * {@code null} when the face is too small to hold that extent at all.
     */
    private static Double clampFitting(double value, double min, double max,
                                       double inset) {
        double low = min + inset;
        double high = max - inset;
        if (low > high + EPSILON) {
            return null;
        }
        return clamp(value, low, high);
    }

    // The (u, v) basis is right-handed with the face normal, so the quad's
    // geometric winding matches the explicit normal supplied to the renderer.
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

    private static Vec3 axisUnit(Direction.Axis axis) {
        return switch (axis) {
            case X -> new Vec3(1.0D, 0.0D, 0.0D);
            case Y -> new Vec3(0.0D, 1.0D, 0.0D);
            case Z -> new Vec3(0.0D, 0.0D, 1.0D);
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
}
