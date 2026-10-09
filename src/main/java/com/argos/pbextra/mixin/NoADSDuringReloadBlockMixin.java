package com.argos.pbextra.mixin;

import com.argos.pbextra.config.PointBlankExtraConfig;
import com.argos.pbextra.data.WeaponExtraDataManager;
import com.vicmatskiv.pointblank.client.ClientEventHandler;
import com.vicmatskiv.pointblank.client.GunClientState;
import com.vicmatskiv.pointblank.item.GunItem;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(value = ClientEventHandler.class, remap = false)
public abstract class NoADSDuringReloadBlockMixin {

    @Inject(
            method = "toggleAiming(Lnet/minecraft/client/player/LocalPlayer;Lcom/vicmatskiv/pointblank/item/GunItem$OperableGunContext;Z)Z",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void pointblankextra$blockAimDuringReload(
            LocalPlayer player, GunItem.OperableGunContext context, boolean isAiming,
            CallbackInfoReturnable<Boolean> cir) {
        if (!isAiming) {
            return;
        }
        if (!PointBlankExtraConfig.ENABLE_MOD.get()) {
            return;
        }
        if (context == null || context.itemStack().isEmpty()) {
            return;
        }
        if (!WeaponExtraDataManager.isDisableAdsDuringReload(context.itemStack())) {
            return;
        }

        GunClientState state = GunClientState.getState(
                player, context.itemStack(), context.slotIndex(), context.offhand());
        if (state == null || !state.isReloading()) {
            return;
        }

        cir.setReturnValue(false);
    }
}
