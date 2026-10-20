package com.arg.pbextra.client.bulletholes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * Focused tests for immediate decal invalidation and the supporting-block index.
 *
 * <p>The existing test harness intentionally has no client level, renderer or
 * FML bootstrap. The invalidation decision is therefore split into a pure
 * collision-box check, and these tests exercise that check together with the
 * O(1) position-index removal path. The production block-change hooks call the
 * same two pieces with a real {@code BlockState}.</p>
 */
class BulletHoleDecalsTest {

    private static final AABB FULL_BOX = new AABB(0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D);
    private static final AABB SLAB_BOTTOM_BOX = new AABB(0.0D, 0.0D, 0.0D, 1.0D, 0.5D, 1.0D);
    private static final AABB SLAB_TOP_BOX = new AABB(0.0D, 0.5D, 0.0D, 1.0D, 1.0D, 1.0D);
    private static final Identifier TEXTURE = Identifier.parse(
            "pointblankextra:textures/bullet_holes/default.png");

    @BeforeEach
    void reset() {
        BulletHoleDecals.clear();
    }

    @AfterEach
    void cleanup() {
        BulletHoleDecals.clear();
    }

    @Test
    void immediateInvalidationRemovesOnlyAffectedDecalsAndIndexEntries() {
        BlockPos changed = new BlockPos(1, 64, 3);
        BlockPos neighbour = new BlockPos(2, 64, 3);
        BulletHoleDecal bottom = decal(changed, SLAB_BOTTOM_BOX);
        BulletHoleDecal top = decal(changed, SLAB_TOP_BOX);
        BulletHoleDecal adjacent = decal(neighbour, SLAB_BOTTOM_BOX);
        BulletHoleDecals.add(bottom, 128);
        BulletHoleDecals.add(top, 128);
        BulletHoleDecals.add(adjacent, 128);

        assertEquals(3, BulletHoleDecals.storedDecalCount());
        assertEquals(2, BulletHoleDecals.positionIndexSize());
        assertEquals(1, BulletHoleDecals.chunkBucketCount());

        // Simulate a slab that changed from bottom to top: only the decal whose
        // exact support box disappeared is invalidated.
        List<AABB> currentBoxes = List.of(SLAB_TOP_BOX);
        BulletHoleDecals.invalidateAt(changed,
                decal -> !BulletHoleDecal.supportBoxPresent(decal.supportBox, currentBoxes));

        assertEquals(2, BulletHoleDecals.storedDecalCount());
        assertEquals(2, BulletHoleDecals.positionIndexSize());
        assertEquals(1, BulletHoleDecals.chunkBucketCount());

        // Removing the remaining decal at the changed position must also drop
        // its index entry, while the neighbour stays indexed and stored.
        BulletHoleDecals.invalidateAt(changed, decal -> true);
        assertEquals(1, BulletHoleDecals.storedDecalCount());
        assertEquals(1, BulletHoleDecals.positionIndexSize());
        assertEquals(1, BulletHoleDecals.chunkBucketCount());
    }

    @Test
    void invalidationRemovesAllDecalsAtOnePositionAndClearsIndexes() {
        BlockPos pos = new BlockPos(4, 70, 6);
        BulletHoleDecal first = decal(pos, FULL_BOX);
        BulletHoleDecal second = decal(pos, FULL_BOX);
        BulletHoleDecals.add(first, 128);
        BulletHoleDecals.add(second, 128);

        // The block-change hook supplies a predicate that is false for an
        // intact support; this is the same path used for air/fluid/replacement.
        BulletHoleDecals.invalidateAt(pos, decal -> true);

        assertEquals(0, BulletHoleDecals.storedDecalCount());
        assertEquals(0, BulletHoleDecals.positionIndexSize());
        assertEquals(0, BulletHoleDecals.chunkBucketCount());
    }

    @Test
    void chunkCleanupDropsAllIndexEntriesForThatChunkOnly() {
        BlockPos first = new BlockPos(1, 64, 1);
        BlockPos second = new BlockPos(2, 64, 2);
        BlockPos otherChunk = new BlockPos(20, 64, 20);
        BulletHoleDecals.add(decal(first, FULL_BOX), 128);
        BulletHoleDecals.add(decal(second, FULL_BOX), 128);
        BulletHoleDecals.add(decal(otherChunk, FULL_BOX), 128);

        assertEquals(3, BulletHoleDecals.storedDecalCount());
        assertEquals(3, BulletHoleDecals.positionIndexSize());
        assertEquals(2, BulletHoleDecals.chunkBucketCount());

        BulletHoleDecals.removeChunkDecals(0, 0);

        assertEquals(1, BulletHoleDecals.storedDecalCount());
        assertEquals(1, BulletHoleDecals.positionIndexSize());
        assertEquals(1, BulletHoleDecals.chunkBucketCount());
    }

    private static BulletHoleDecal decal(BlockPos pos, AABB supportBox) {
        return new BulletHoleDecal(null, pos, Direction.NORTH, TEXTURE,
                new float[]{0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F,
                        1.0F, 1.0F, 0.0F, 0.0F, 1.0F, 0.0F},
                0.0F, 0.0F, -1.0F, supportBox, 0, 100L, 20);
    }
}