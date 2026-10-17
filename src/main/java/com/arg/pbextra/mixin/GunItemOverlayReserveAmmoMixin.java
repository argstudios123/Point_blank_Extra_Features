package com.arg.pbextra.mixin;

import com.arg.pbextra.ammo.ReserveAmmoCounter;
import com.arg.pbextra.config.PointBlankExtraConfig;
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
        // A custom ammo UI formats its capacity text on its own, so it keeps the reserve.
        return pointblankextra$reserveValue(gun, gunStack, fireMode, false);
    }


    @Redirect(
            method = "renderGunOverlay2(Lnet/minecraft/client/gui/GuiGraphicsExtractor;"
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
        return pointblankextra$reserveValue(gun, gunStack, fireMode, true);
    }


    @Unique
    private static int pointblankextra$reserveValue(GunItem gun, ItemStack gunStack,
                                                    FireModeInstance fireMode,
                                                    boolean keepInfiniteCapacity) {
        int magazineCapacity = gun.getMaxAmmoCapacity(gunStack, fireMode);
        if (!PointBlankExtraConfig.ENABLE_MOD.get()
                || !PointBlankExtraConfig.ENABLE_RESERVE_AMMO_COUNTER.get()) {
            return magazineCapacity;
        }

        if (keepInfiniteCapacity && magazineCapacity == GunItem.INFINITE_AMMO) {
            // Point Blank's default HUD builds the text as "ammo/capacity" and shows its
            // infinite indicator when the capacity is the infinite sentinel. It decides that
            // on the value returned here, so replacing it with the reserve would defeat the
            // check and print the raw infinite ammo count instead. Weapons with an infinite
            // capacity therefore keep Point Blank's own indicator.
            return magazineCapacity;
        }

        return ReserveAmmoCounter.reserveForDisplay(gun, gunStack, fireMode, magazineCapacity);
    }
}
