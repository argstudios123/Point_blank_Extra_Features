package com.arg.pbextra.client.bulletholes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;


/**
 * One lightweight bullet-hole decal. No entity, block entity or ticking object:
 * the record stores a precomputed quad and is owned by
 * {@link BulletHoleDecals}.
 */
public final class BulletHoleDecal {

    final Block block;
    final BlockPos pos;
    final Direction face;
    final Identifier texture;

    /** Four world-space corners: x0,y0,z0,x1,y1,z1,x2,y2,z2,x3,y3,z3. */
    final float[] corners;

    final float normalX;
    final float normalY;
    final float normalZ;

    final int light;
    final long expiresAt;
    final int fadeTicks;
    final AABB bounds;

    /**
     * The local-space block shape box the decal was placed on. A slab or stair
     * can keep the same block while its shape changes; comparing this box with
     * the block's current shape catches those cases without per-decal ticking.
     */
    final AABB supportBox;

    BulletHoleDecal(Block block, BlockPos pos, Direction face, Identifier texture,
                    float[] corners, float normalX, float normalY, float normalZ,
                    AABB supportBox, int light, long expiresAt, int fadeTicks) {
        this.block = block;
        this.pos = pos;
        this.face = face;
        this.texture = texture;
        this.corners = corners;
        this.normalX = normalX;
        this.normalY = normalY;
        this.normalZ = normalZ;
        this.supportBox = supportBox;
        this.light = light;
        this.expiresAt = expiresAt;
        this.fadeTicks = fadeTicks;

        float minX = corners[0];
        float minY = corners[1];
        float minZ = corners[2];
        float maxX = minX;
        float maxY = minY;
        float maxZ = minZ;
        for (int i = 3; i < corners.length; i += 3) {
            minX = Math.min(minX, corners[i]);
            minY = Math.min(minY, corners[i + 1]);
            minZ = Math.min(minZ, corners[i + 2]);
            maxX = Math.max(maxX, corners[i]);
            maxY = Math.max(maxY, corners[i + 1]);
            maxZ = Math.max(maxZ, corners[i + 2]);
        }
        this.bounds = new AABB(minX, minY, minZ, maxX, maxY, maxZ).inflate(0.01D);
    }

    boolean isExpired(long gameTime) {
        return gameTime >= expiresAt;
    }

    /**
     * True when the current block shape still contains the exact local box this
     * decal was placed on. Full cubes, slabs, stairs, panes and other partial
     * shapes all reduce to the same inexpensive AABB comparison.
     *
     * <p>TaCZ's {@code BulletHoleParticle#shouldRemove()} checks air, an empty
     * collision shape and whether the particle position lies inside the
     * inflated overall collision bounding box. This adaptation uses the exact
     * support box instead, which also catches a slab/stair that keeps its block
     * id but moves the supporting face, and it runs from block-change hooks
     * rather than per-particle ticking.</p>
     */
    boolean supportBoxStillPresent(VoxelShape shape) {
        if (shape == null || shape.isEmpty()) {
            return false;
        }
        return supportBoxPresent(supportBox, shape.toAabbs());
    }

    static boolean supportBoxPresent(AABB supportBox, List<AABB> boxes) {
        if (supportBox == null || boxes == null || boxes.isEmpty()) {
            return false;
        }
        for (AABB box : boxes) {
            if (sameBox(box, supportBox)) {
                return true;
            }
        }
        return false;
    }

    private static boolean sameBox(AABB first, AABB second) {
        double epsilon = 1.0E-6D;
        return Math.abs(first.minX - second.minX) <= epsilon
                && Math.abs(first.minY - second.minY) <= epsilon
                && Math.abs(first.minZ - second.minZ) <= epsilon
                && Math.abs(first.maxX - second.maxX) <= epsilon
                && Math.abs(first.maxY - second.maxY) <= epsilon
                && Math.abs(first.maxZ - second.maxZ) <= epsilon;
    }

    /** Fades out over the last part of the configured lifetime. */
    int alpha(long gameTime) {
        long remaining = expiresAt - gameTime;
        if (remaining >= fadeTicks) {
            return 255;
        }
        if (remaining <= 0L) {
            return 0;
        }
        return (int) (255L * remaining / fadeTicks);
    }

    double centerX() {
        return (bounds.minX + bounds.maxX) * 0.5D;
    }

    double centerY() {
        return (bounds.minY + bounds.maxY) * 0.5D;
    }

    double centerZ() {
        return (bounds.minZ + bounds.maxZ) * 0.5D;
    }
}
