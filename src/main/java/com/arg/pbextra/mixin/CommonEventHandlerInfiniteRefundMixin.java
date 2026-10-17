package com.arg.pbextra.mixin;

import com.vicmatskiv.pointblank.CommonEventHandler;
import com.vicmatskiv.pointblank.item.FireModeInstance;
import com.vicmatskiv.pointblank.item.GunItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Keeps Point Blank's attachment-removal ammo refund sane for infinite magazines.
 *
 * <p>{@code CommonEventHandler#onAttachmentRemoved} hands the player
 * {@code Math.min(ammo, capacity)} rounds of the fire mode's ammo item and refills the
 * magazine with the capacity. For a magazine of {@link GunItem#INFINITE_AMMO} that amount
 * is the infinite sentinel, so the following drop loop would be asked to drop roughly 33
 * million single item stacks - on a weapon whose magazine is infinite and therefore has
 * nothing to hand back in the first place.</p>
 *
 * <p>{@link GunItemInfiniteMagazineMixin} reports {@link GunItem#INFINITE_AMMO} for such a
 * magazine (as Point Blank already stores for a gun-wide infinite capacity), so this
 * redirect keeps the refund at zero for it and leaves finite weapons and fire modes with
 * their normal refund.</p>
 */
@Mixin(value = CommonEventHandler.class, remap = false)
public abstract class CommonEventHandlerInfiniteRefundMixin {

    @Redirect(
            method = "onAttachmentRemoved",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/vicmatskiv/pointblank/item/GunItem;getAmmo("
                            + "Lnet/minecraft/world/item/ItemStack;"
                            + "Lcom/vicmatskiv/pointblank/item/FireModeInstance;)I",
                    remap = false
            ),
            remap = false
    )
    private static int pointblankextra$noRefundForInfiniteMagazine(
            ItemStack gunStack, FireModeInstance fireMode) {
        if (gunStack != null && fireMode != null && gunStack.getItem() instanceof GunItem gun
                && gun.getMaxAmmoCapacity(gunStack, fireMode) == GunItem.INFINITE_AMMO) {
            return 0;
        }
        return GunItem.getAmmo(gunStack, fireMode);
    }
}
