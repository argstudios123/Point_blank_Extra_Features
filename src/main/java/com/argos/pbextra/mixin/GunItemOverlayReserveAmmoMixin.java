package com.argos.pbextra.mixin;

import com.argos.pbextra.ammo.ReserveAmmoCounter;
import com.argos.pbextra.config.PointBlankExtraConfig;
import com.vicmatskiv.pointblank.client.gui.GunItemOverlay;
import com.vicmatskiv.pointblank.item.FireModeInstance;
import com.vicmatskiv.pointblank.item.GunItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;


@Mixin(value = GunItemOverlay.class, remap = false)
public abstract class GunItemOverlayReserveAmmoMixin {

    @Redirect(
            method = "renderCustomOverlay",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/vicmatskiv/pointblank/item/GunItem;getMaxAmmoCapacity("
                            + "Lnet/minecraft/world/item/ItemStack;"
                            + "Lcom/vicmatskiv/pointblank/item/FireModeInstance;)I",
                    ordinal = 1,
                    remap = false
            ),
            remap = false
    )
    private static int pointblankextra$reserveForCustomAmmoUi(GunItem gun, ItemStack gunStack,
                                                              FireModeInstance fireMode) {
        return pointblankextra$reserveValue(gun, gunStack, fireMode);
    }


    @Redirect(
            method = "renderGunOverlay2(Lnet/minecraft/client/gui/GuiGraphics;"
                    + "Lnet/minecraft/world/item/ItemStack;"
                    + "Lcom/vicmatskiv/pointblank/Config$AmmoIndicatorPlacement;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/vicmatskiv/pointblank/item/GunItem;getMaxAmmoCapacity("
                            + "Lnet/minecraft/world/item/ItemStack;"
                            + "Lcom/vicmatskiv/pointblank/item/FireModeInstance;)I",
                    ordinal = 0,
                    remap = false
            ),
            remap = false
    )
    private static int pointblankextra$reserveForDefaultCounter(GunItem gun, ItemStack gunStack,
                                                                FireModeInstance fireMode) {
        return pointblankextra$reserveValue(gun, gunStack, fireMode);
    }


    @Unique
    private static int pointblankextra$reserveValue(GunItem gun, ItemStack gunStack,
                                                    FireModeInstance fireMode) {
        int magazineCapacity = gun.getMaxAmmoCapacity(gunStack, fireMode);
        if (!PointBlankExtraConfig.ENABLE_MOD.get()
                || !PointBlankExtraConfig.ENABLE_RESERVE_AMMO_COUNTER.get()) {
            return magazineCapacity;
        }
        return ReserveAmmoCounter.reserveForDisplay(gun, gunStack, fireMode, magazineCapacity);
    }
}
