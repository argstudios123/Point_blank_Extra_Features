package com.arg.pbextra.mixin;

import com.arg.pbextra.bullethole.BulletHoleImpactContext;
import com.vicmatskiv.pointblank.item.HurtingItem;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


/**
 * Publishes the firing item while Point Blank dispatches a block impact.
 * <p>
 * Point Blank's {@code BlockHitEvent} is deliberately reused (it is the single
 * path for both hitscan shots and projectiles), but it does not carry the gun
 * item. The event is posted synchronously from inside
 * {@code HurtingItem#handleBlockHit}, so this short-lived context is available
 * to the bullet-hole listener and is cleared on return.
 */
@Mixin(value = HurtingItem.class, remap = false)
public abstract class HurtingItemBulletHoleContextMixin {

    @Inject(
            method = "handleBlockHit(Lnet/minecraft/world/entity/LivingEntity;"
                    + "Lnet/minecraft/world/phys/BlockHitResult;"
                    + "Lnet/minecraft/world/entity/Entity;)V",
            at = @At("HEAD"),
            remap = false
    )
    private void pbextra$pushBulletHoleSource(LivingEntity player, BlockHitResult hitResult,
                                              Entity projectile, CallbackInfo ci) {
        BulletHoleImpactContext.push((HurtingItem) (Object) this);
    }

    @Inject(
            method = "handleBlockHit(Lnet/minecraft/world/entity/LivingEntity;"
                    + "Lnet/minecraft/world/phys/BlockHitResult;"
                    + "Lnet/minecraft/world/entity/Entity;)V",
            at = @At("RETURN"),
            remap = false
    )
    private void pbextra$popBulletHoleSource(LivingEntity player, BlockHitResult hitResult,
                                             Entity projectile, CallbackInfo ci) {
        BulletHoleImpactContext.pop();
    }
}
