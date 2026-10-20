package com.arg.pbextra.client.bulletholes;

import com.arg.pbextra.PointBlankExtraFeatures;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;


/**
 * Secondary client-side invalidation hook for decals whose support changed.
 *
 * <p>The primary hook is {@code ClientLevelBulletHoleMixin}, which runs directly
 * after {@code setServerVerifiedBlockState}/{@code setBlock}. This listener
 * additionally covers neighbour notifications that arrive through a path not
 * going through those methods. It is registered with {@code receiveCanceled}
 * so another mod cannot suppress cleanup. Only decals at the changed position
 * are looked up (O(1)); the periodic sweep remains a fallback but is never the
 * mechanism that makes a broken block's decals disappear.</p>
 */
@EventBusSubscriber(modid = PointBlankExtraFeatures.MOD_ID, value = Dist.CLIENT)
public final class BulletHoleBlockListener {

    private BulletHoleBlockListener() {
    }

    @SubscribeEvent(receiveCanceled = true)
    public static void onNeighborNotify(BlockEvent.NeighborNotifyEvent event) {
        if (event.getLevel().isClientSide()) {
            BulletHoleDecals.onBlockChanged(event.getLevel(), event.getPos(), event.getState());
        }
    }

    /** A chunk going away must take its decals with it, without a global clear. */
    @SubscribeEvent(receiveCanceled = true)
    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            ChunkPos chunkPos = event.getChunk().getPos();
            BulletHoleDecals.removeChunkDecals(chunkPos.x(), chunkPos.z());
        }
    }

    /**
     * A chunk that is (re)loaded starts from authoritative server blocks. Any
     * decals previously held for that chunk are stale; dropping them here also
     * covers chunk replacement paths that do not emit an unload event.
     */
    @SubscribeEvent(receiveCanceled = true)
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel().isClientSide()) {
            ChunkPos chunkPos = event.getChunk().getPos();
            BulletHoleDecals.removeChunkDecals(chunkPos.x(), chunkPos.z());
        }
    }
}