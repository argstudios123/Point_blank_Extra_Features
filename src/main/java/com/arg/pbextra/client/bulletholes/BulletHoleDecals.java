package com.arg.pbextra.client.bulletholes;

import com.arg.pbextra.bullethole.BulletHoleConfig;
import com.arg.pbextra.config.PointBlankExtraConfig;
import com.arg.pbextra.data.WeaponExtraDataManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;


/**
 * Client-side bullet-hole decal store.
 * <p>
 * Decals are plain records held in chunk buckets plus a block-position index.
 * There is no entity, block entity, particle or per-decal tick. A block-change
 * notification removes affected decals immediately, chunk unloads drop that
 * chunk's decals, and one lightweight sweep per second expires old decals and
 * remains a fallback for support validity. The render pass only walks chunk
 * buckets within view and range.
 */
public final class BulletHoleDecals {

    /** Hard cap on retained decals across all dimensions. */
    private static final int MAX_GLOBAL_DECALS = 4_096;

    private static final int SWEEP_INTERVAL_TICKS = 20;

    private static final double MIN_RENDER_DISTANCE = 32.0D;
    private static final double MAX_RENDER_DISTANCE = 128.0D;

    /** Tiny outward offset, on top of the render type's depth offset. */
    private static final double SURFACE_OFFSET = 0.0025D;

    private static final LinkedHashSet<BulletHoleDecal> ALL_DECALS = new LinkedHashSet<>();
    private static final Map<Long, LinkedHashSet<BulletHoleDecal>> BY_CHUNK = new HashMap<>();
    /**
     * Lightweight block-position index. One O(1) lookup resolves all decals on a
     * changed block, so a single block update never scans the whole store or the
     * loaded chunks. Most positions hold one decal, so the extra map entry is
     * small and bounded by the global decal cap.
     */
    private static final Map<Long, LinkedHashSet<BulletHoleDecal>> BY_POSITION = new HashMap<>();
    private static final Map<Identifier, TagKey<Block>> TAG_CACHE = new HashMap<>();

    private static Level level;
    private static int sweepTicker;

    private BulletHoleDecals() {
    }

    /** Network entry point: creates one decal, unless every gate forbids it. */
    public static void accept(Identifier weaponId, BlockPos pos, Direction face,
                              Vec3 location) {
        if (!PointBlankExtraConfig.areBulletHolesEnabled()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Level currentLevel = minecraft.level;
        if (currentLevel == null || minecraft.player == null
                || pos == null || face == null || location == null) {
            return;
        }

        // Ignore stale or absurdly distant packets.
        if (Vec3.atCenterOf(pos).distanceToSqr(minecraft.player.position())
                > 256.0D * 256.0D) {
            return;
        }

        if (level != currentLevel) {
            clear();
            level = currentLevel;
        }

        BulletHoleConfig config = weaponId == null
                ? BulletHoleConfig.BUILT_IN_DEFAULTS
                : WeaponExtraDataManager.getData(weaponId).getBulletHoles();
        if (!config.isEnabled()) {
            return;
        }

        BulletHoleDecal decal = createDecal(currentLevel, pos, face, location, config);
        if (decal != null) {
            add(decal, config.getMaxPerChunk());
        }
    }

    /** Per-client-tick maintenance: level changes, master switch and lazy expiry. */
    public static void tick(Minecraft minecraft) {
        Level currentLevel = minecraft.level;
        if (currentLevel == null) {
            if (level != null || !ALL_DECALS.isEmpty()) {
                clear();
            }
            return;
        }

        if (level != currentLevel) {
            clear();
            level = currentLevel;
        }

        if (!PointBlankExtraConfig.areBulletHolesEnabled()) {
            clear();
            return;
        }

        if (++sweepTicker < SWEEP_INTERVAL_TICKS) {
            return;
        }
        sweepTicker = 0;
        sweep(currentLevel, currentLevel.getGameTime());
    }

    /** Removes every decal and drops the level reference. */
    public static void clear() {
        ALL_DECALS.clear();
        BY_CHUNK.clear();
        BY_POSITION.clear();
        level = null;
        sweepTicker = 0;
    }

    /**
     * Immediately drops decals supported by {@code pos} when that block no longer
     * supports them. Called synchronously from the mixin/event hooks, so a broken
     * or replaced block never leaves a decal hovering until the next sweep.
     *
     * <p>Only the decals registered at this exact position are inspected; all
     * neighbouring blocks keep their decals. The block identity, fluid/air state
     * and the exact local shape box are checked. The last check also covers
     * partial shapes: a slab/stair that keeps its block id but changes shape no
     * longer contains the original support box and loses the decal.</p>
     */
    public static void onBlockChanged(BlockPos pos, BlockState newState) {
        onBlockChanged(level, pos, newState);
    }

    /**
     * Block-change entry point with an explicit level/block getter. Passing the
     * getter avoids relying on the cached {@link #level} reference and makes the
     * shape validation usable from the client hook and from tests.
     */
    public static void onBlockChanged(BlockGetter blockGetter, BlockPos pos,
                                      BlockState newState) {
        if (pos == null || newState == null || BY_POSITION.isEmpty()) {
            return;
        }
        // Do not compute a block shape for positions that carry no decals.
        if (!BY_POSITION.containsKey(pos.asLong())) {
            return;
        }
        boolean eligible = isEligibleSupport(newState);
        VoxelShape shape = eligible && blockGetter != null
                ? newState.getShape(blockGetter, pos) : null;
        invalidateAt(pos, decal -> !isSupportValid(newState, shape, decal));
    }

    /**
     * Removes matching decals at one position and keeps all three indexes
     * consistent. Package-private so the invalidation/index cleanup can be
     * exercised without a client world or a rendering context.
     */
    static void invalidateAt(BlockPos pos, Predicate<BulletHoleDecal> invalid) {
        if (pos == null || invalid == null) {
            return;
        }
        LinkedHashSet<BulletHoleDecal> decals = BY_POSITION.get(pos.asLong());
        if (decals == null || decals.isEmpty()) {
            return;
        }
        Iterator<BulletHoleDecal> iterator = decals.iterator();
        while (iterator.hasNext()) {
            BulletHoleDecal decal = iterator.next();
            if (invalid.test(decal)) {
                iterator.remove();
                ALL_DECALS.remove(decal);
                removeFromBucket(decal);
            }
        }
        if (decals.isEmpty()) {
            BY_POSITION.remove(pos.asLong());
        }
    }

    /** Drops every decal in a chunk that is unloading, loading or being replaced. */
    public static void removeChunkDecals(int chunkX, int chunkZ) {
        LinkedHashSet<BulletHoleDecal> bucket = BY_CHUNK.remove(chunkKey(chunkX, chunkZ));
        if (bucket == null) {
            return;
        }
        for (BulletHoleDecal decal : bucket) {
            ALL_DECALS.remove(decal);
            removeFromPosition(decal);
        }
    }

    /** Resource reloads invalidate textures and previously baked decal state. */
    public static void onResourceReload() {
        BulletHoleTextures.clear();
        clear();
    }

    /** Config reloads must clear decals immediately when the master switch is off. */
    public static void onConfigReload() {
        if (!PointBlankExtraConfig.areBulletHolesEnabled()) {
            clear();
            BulletHoleTextures.clear();
        }
    }

    static void add(BulletHoleDecal decal, int maxPerChunk) {
        while (ALL_DECALS.size() >= MAX_GLOBAL_DECALS) {
            evictOldestGlobal();
        }

        LinkedHashSet<BulletHoleDecal> bucket = BY_CHUNK.computeIfAbsent(
                chunkKey(decal.pos), key -> new LinkedHashSet<>());
        while (bucket.size() >= maxPerChunk) {
            Iterator<BulletHoleDecal> iterator = bucket.iterator();
            if (!iterator.hasNext()) {
                break;
            }
            BulletHoleDecal oldest = iterator.next();
            iterator.remove();
            ALL_DECALS.remove(oldest);
            removeFromPosition(oldest);
        }

        bucket.add(decal);
        ALL_DECALS.add(decal);
        BY_POSITION.computeIfAbsent(decal.pos.asLong(), key -> new LinkedHashSet<>())
                .add(decal);
    }

    private static void evictOldestGlobal() {
        Iterator<BulletHoleDecal> iterator = ALL_DECALS.iterator();
        if (!iterator.hasNext()) {
            return;
        }
        BulletHoleDecal oldest = iterator.next();
        iterator.remove();
        removeFromBucket(oldest);
        removeFromPosition(oldest);
    }

    private static void removeFromBucket(BulletHoleDecal decal) {
        long key = chunkKey(decal.pos);
        LinkedHashSet<BulletHoleDecal> bucket = BY_CHUNK.get(key);
        if (bucket == null) {
            return;
        }
        bucket.remove(decal);
        if (bucket.isEmpty()) {
            BY_CHUNK.remove(key);
        }
    }

    private static long chunkKey(BlockPos pos) {
        return chunkKey(pos.getX() >> 4, pos.getZ() >> 4);
    }

    /**
     * Internal chunk key. Kept independent of {@code ChunkPos.pack} so the
     * invalidation/index core can be unit-tested without bootstrapping the
     * entire chunk-status registry.
     */
    static long chunkKey(int chunkX, int chunkZ) {
        return ((long) chunkX & 0xFFFFFFFFL)
                | (((long) chunkZ & 0xFFFFFFFFL) << 32);
    }

    private static void removeFromPosition(BulletHoleDecal decal) {
        long key = decal.pos.asLong();
        LinkedHashSet<BulletHoleDecal> decals = BY_POSITION.get(key);
        if (decals == null) {
            return;
        }
        decals.remove(decal);
        if (decals.isEmpty()) {
            BY_POSITION.remove(key);
        }
    }

    static int storedDecalCount() {
        return ALL_DECALS.size();
    }

    static int positionIndexSize() {
        return BY_POSITION.size();
    }

    static int chunkBucketCount() {
        return BY_CHUNK.size();
    }

    private static void sweep(Level level, long gameTime) {
        // Multiple decals often share one block. Resolve each block shape at
        // most once per sweep; no per-tick work is added to normal decals.
        Map<Long, VoxelShape> shapeCache = new HashMap<>();
        Iterator<BulletHoleDecal> iterator = ALL_DECALS.iterator();
        while (iterator.hasNext()) {
            BulletHoleDecal decal = iterator.next();
            if (decal.isExpired(gameTime)
                    || !isSupportValid(level, decal, shapeCache)) {
                iterator.remove();
                removeFromBucket(decal);
                removeFromPosition(decal);
            }
        }
        BY_CHUNK.values().removeIf(Set::isEmpty);
        BY_POSITION.values().removeIf(Set::isEmpty);
    }

    private static boolean isSupportValid(Level level, BulletHoleDecal decal,
                                          Map<Long, VoxelShape> shapeCache) {
        if (!level.isLoaded(decal.pos)) {
            return false;
        }
        BlockState state = level.getBlockState(decal.pos);
        if (!isEligibleSupport(state) || state.getBlock() != decal.block) {
            return false;
        }
        VoxelShape shape = shapeCache.computeIfAbsent(decal.pos.asLong(),
                key -> state.getShape(level, decal.pos));
        return decal.supportBoxStillPresent(shape);
    }

    private static boolean isSupportValid(BlockState state, VoxelShape shape,
                                          BulletHoleDecal decal) {
        return isEligibleSupport(state)
                && state.getBlock() == decal.block
                && (shape == null || decal.supportBoxStillPresent(shape));
    }

    /**
     * Air and fluids cannot carry a decal. Transparency is deliberately not a
     * criterion: glass, stained glass and other non-opaque blocks are valid
     * supports, and their texture still comes from the normal selection chain.
     */
    private static boolean isEligibleSupport(BlockState state) {
        return !state.isAir() && state.getFluidState().isEmpty();
    }

    /** Submits this frame's visible decals, grouped by texture/render type. */
    public static void render(SubmitCustomGeometryEvent event) {
        if (!PointBlankExtraConfig.areBulletHolesEnabled() || ALL_DECALS.isEmpty()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Level currentLevel = minecraft.level;
        if (currentLevel == null || level != currentLevel) {
            return;
        }

        CameraRenderState camera = event.getLevelRenderState().cameraRenderState;
        if (camera == null || camera.pos == null) {
            return;
        }
        Vec3 cameraPos = camera.pos;
        Frustum frustum = camera.cullFrustum;

        double renderDistance = Math.max(MIN_RENDER_DISTANCE,
                Math.min(MAX_RENDER_DISTANCE,
                        minecraft.options.getEffectiveRenderDistance() * 16.0D));
        double renderDistanceSqr = renderDistance * renderDistance;
        int chunkRadius = Mth.ceil(renderDistance / 16.0D);
        int cameraChunkX = Mth.floor(cameraPos.x) >> 4;
        int cameraChunkZ = Mth.floor(cameraPos.z) >> 4;

        Map<Identifier, List<BulletHoleDecal>> groups = new LinkedHashMap<>();
        for (int dx = -chunkRadius; dx <= chunkRadius; dx++) {
            for (int dz = -chunkRadius; dz <= chunkRadius; dz++) {
                Set<BulletHoleDecal> bucket = BY_CHUNK.get(
                        chunkKey(cameraChunkX + dx, cameraChunkZ + dz));
                if (bucket == null || bucket.isEmpty()) {
                    continue;
                }
                for (BulletHoleDecal decal : bucket) {
                    double ddx = decal.centerX() - cameraPos.x;
                    double ddy = decal.centerY() - cameraPos.y;
                    double ddz = decal.centerZ() - cameraPos.z;
                    if (ddx * ddx + ddy * ddy + ddz * ddz > renderDistanceSqr) {
                        continue;
                    }
                    if (frustum != null && !frustum.isVisible(decal.bounds)) {
                        continue;
                    }
                    groups.computeIfAbsent(decal.texture,
                            key -> new ArrayList<>()).add(decal);
                }
            }
        }

        if (groups.isEmpty()) {
            return;
        }

        long gameTime = event.getLevelRenderState().gameTime;
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        for (Map.Entry<Identifier, List<BulletHoleDecal>> entry : groups.entrySet()) {
            RenderType renderType = BulletHoleTextures.renderType(entry.getKey());
            List<BulletHoleDecal> decals = entry.getValue();
            event.getSubmitNodeCollector().submitCustomGeometry(pose, renderType,
                    (poseEntry, consumer) -> draw(poseEntry, consumer, decals, gameTime));
        }
        pose.popPose();
    }

    private static void draw(PoseStack.Pose pose, VertexConsumer consumer,
                             List<BulletHoleDecal> decals, long gameTime) {
        for (BulletHoleDecal decal : decals) {
            int alpha = decal.alpha(gameTime);
            if (alpha <= 0) {
                continue;
            }
            float[] c = decal.corners;
            vertex(consumer, pose, decal, c, 0, 0.0F, 0.0F, alpha);
            vertex(consumer, pose, decal, c, 3, 1.0F, 0.0F, alpha);
            vertex(consumer, pose, decal, c, 6, 1.0F, 1.0F, alpha);
            vertex(consumer, pose, decal, c, 9, 0.0F, 1.0F, alpha);
        }
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose,
                               BulletHoleDecal decal, float[] corners, int index,
                               float u, float v, int alpha) {
        consumer.addVertex(pose, corners[index], corners[index + 1], corners[index + 2])
                .setColor(255, 255, 255, alpha)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(decal.light)
                .setNormal(pose, decal.normalX, decal.normalY, decal.normalZ);
    }

    private static BulletHoleDecal createDecal(Level level, BlockPos pos, Direction face,
                                                Vec3 location, BulletHoleConfig config) {
        BlockState state = level.getBlockState(pos);
        if (!isEligibleSupport(state)) {
            return null;
        }

        VoxelShape shape = state.getShape(level, pos);
        if (shape.isEmpty()) {
            return null;
        }

        Vec3 localHit = location.subtract(pos.getX(), pos.getY(), pos.getZ());
        AABB faceBox = BulletHolePlacement.selectFaceBox(shape.toAabbs(), face, localHit);
        if (faceBox == null) {
            return null;
        }

        double angle = randomAngle(pos, face, localHit);
        BulletHolePlacement.Placement placement = BulletHolePlacement.place(
                faceBox, face, localHit, config.getSize(), angle, SURFACE_OFFSET);
        if (placement == null) {
            return null;
        }

        Vec3 c0 = placement.c0().add(pos.getX(), pos.getY(), pos.getZ());
        Vec3 c1 = placement.c1().add(pos.getX(), pos.getY(), pos.getZ());
        Vec3 c2 = placement.c2().add(pos.getX(), pos.getY(), pos.getZ());
        Vec3 c3 = placement.c3().add(pos.getX(), pos.getY(), pos.getZ());
        float[] corners = {
                (float) c0.x, (float) c0.y, (float) c0.z,
                (float) c1.x, (float) c1.y, (float) c1.z,
                (float) c2.x, (float) c2.y, (float) c2.z,
                (float) c3.x, (float) c3.y, (float) c3.z
        };

        Identifier blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        Identifier requested = config.selectTexture(blockId,
                tag -> state.is(tagKey(tag)));
        Identifier texture = BulletHoleTextures.resolve(requested);
        if (texture == null) {
            return null;
        }

        int light = LevelRenderer.getLightCoords(level, pos.relative(face));
        int lifetime = config.getLifetimeTicks();
        int fadeTicks = Math.max(20, lifetime / 5);
        return new BulletHoleDecal(state.getBlock(), pos, face, texture, corners,
                face.getStepX(), face.getStepY(), face.getStepZ(), faceBox, light,
                level.getGameTime() + lifetime, fadeTicks);
    }

    private static TagKey<Block> tagKey(Identifier tag) {
        return TAG_CACHE.computeIfAbsent(tag, id -> TagKey.create(Registries.BLOCK, id));
    }

    private static double randomAngle(BlockPos pos, Direction face, Vec3 localHit) {
        long seed = pos.asLong() * 0x9E3779B97F4A7C15L
                ^ ((long) face.ordinal() << 56)
                ^ Double.doubleToLongBits(localHit.x)
                ^ (Double.doubleToLongBits(localHit.y) << 1)
                ^ (Double.doubleToLongBits(localHit.z) << 2);
        return new Random(seed).nextDouble() * Math.PI * 2.0D;
    }

}
