package com.arg.pbextra.mixin;

import com.arg.pbextra.client.bulletholes.BulletHoleDecals;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


/**
 * Direct client-side block-change hook for bullet-hole invalidation.
 *
 * <p>The previous listener used {@code BlockEvent.NeighborNotifyEvent}. That
 * event is a useful general notification, but it is not a complete hook for the
 * client block-update path: when {@code BlockStatePredictionHandler} consumes a
 * server update, {@code setServerVerifiedBlockState} can return without calling
 * {@code Level.setBlock} and therefore without firing a neighbour notification.
 * The decal then stayed visible until the periodic sweep.</p>
 *
 * <p>These two injections cover every client block mutation used by this
 * version: server block updates/section batches (and Point Blank's custom
 * explosion packets) through {@code setServerVerifiedBlockState}, and local
 * prediction, piston movement and direct {@code level.setBlock} calls through
 * the {@code ClientLevel#setBlock} override. They call an O(1) position-index
 * invalidation immediately after the state has been applied, before the next
 * frame.</p>
 */
@Mixin(value = ClientLevel.class, remap = false)
public abstract class ClientLevelBulletHoleMixin {

    @Inject(
            method = "setServerVerifiedBlockState(Lnet/minecraft/core/BlockPos;"
                    + "Lnet/minecraft/world/level/block/state/BlockState;I)V",
            at = @At("RETURN"),
            remap = false
    )
    private void pbextra$invalidateAfterServerState(BlockPos pos, BlockState state,
                                                    int updateFlag, CallbackInfo ci) {
        BulletHoleDecals.onBlockChanged((ClientLevel) (Object) this, pos, state);
    }

    @Inject(
            method = "setBlock(Lnet/minecraft/core/BlockPos;"
                    + "Lnet/minecraft/world/level/block/state/BlockState;II)Z",
            at = @At("RETURN"),
            remap = false
    )
    private void pbextra$invalidateAfterLocalState(BlockPos pos, BlockState state,
                                                   int updateFlags, int updateLimit,
                                                   CallbackInfoReturnable<Boolean> cir) {
        if (Boolean.TRUE.equals(cir.getReturnValue())) {
            BulletHoleDecals.onBlockChanged((ClientLevel) (Object) this, pos, state);
        }
    }
}