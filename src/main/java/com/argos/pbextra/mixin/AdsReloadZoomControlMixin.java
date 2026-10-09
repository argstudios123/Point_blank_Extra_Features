package com.argos.pbextra.mixin;

import com.argos.pbextra.client.AdsReloadZoomTransition;
import com.argos.pbextra.config.PointBlankExtraConfig;
import com.argos.pbextra.data.AdsReloadZoomControlConfig;
import com.argos.pbextra.data.WeaponExtraDataManager;
import com.vicmatskiv.pointblank.client.ClientEventHandler;
import com.vicmatskiv.pointblank.client.GunClientState;
import com.vicmatskiv.pointblank.feature.AimingFeature;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;


@Mixin(value = ClientEventHandler.class, remap = false)
public abstract class AdsReloadZoomControlMixin {

    @Redirect(
            method = "onFovUpdate",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/vicmatskiv/pointblank/feature/AimingFeature;getZoom(Lnet/minecraft/world/item/ItemStack;)F",
                    remap = false
            ),
            remap = false
    )
    private float pointblankextra$relaxZoomDuringReload(ItemStack stack) {
        float zoom = AimingFeature.getZoom(stack);

        if (!PointBlankExtraConfig.ENABLE_MOD.get()) {
            return zoom;
        }

        AdsReloadZoomControlConfig config = WeaponExtraDataManager.getAdsReloadZoomControlConfig(stack);
        if (!config.isEnabled() || config.getAmount() <= 0.0F) {
            return zoom;
        }

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            return zoom;
        }

        // Same state lookup Point Blank itself performs inside onFovUpdate.
        GunClientState state = GunClientState.getState(
                player, stack, player.getInventory().selected, false);
        boolean reloading = state != null && state.isReloading();

        float progress = AdsReloadZoomTransition.progress(
                reloading, config.getZoomType());
        if (progress <= 0.0F) {
            return zoom;
        }

        return (float) (zoom * (1.0 - config.getAmount() * (double) progress));
    }
}

