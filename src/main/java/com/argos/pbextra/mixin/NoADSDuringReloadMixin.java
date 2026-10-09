package com.argos.pbextra.mixin;

import com.argos.pbextra.config.PointBlankExtraConfig;
import com.argos.pbextra.data.WeaponExtraDataManager;
import com.vicmatskiv.pointblank.client.GunClientState;
import com.vicmatskiv.pointblank.item.GunItem;
import com.vicmatskiv.pointblank.network.AimingChangeRequestPacket;
import com.vicmatskiv.pointblank.network.Network;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = GunClientState.class, remap = false)
public abstract class NoADSDuringReloadMixin {

    @Inject(method = "tryReload", at = @At("RETURN"), remap = false)
    private void pointblankextra$clearAimOnReloadStart(
            LivingEntity player, ItemStack itemStack, CallbackInfoReturnable<Boolean> cir) {
        if (!Boolean.TRUE.equals(cir.getReturnValue())) {
            return;
        }
        GunClientState self = (GunClientState) (Object) this;
        if (!self.isAiming()) {
            return;
        }
        if (!pointblankextra$isDisableAdsDuringReload()) {
            return;
        }

        // Same two-step release as ClientEventHandler#toggleAiming: clear the client
        // aiming flag (interpolator ramps out) and sync the "aim" NBT tag to the server
        // so GunItem.isAiming(ItemStack) no longer forces sprint off.
        self.setAiming(false);
        if (player instanceof Player p) {
            Network.networkChannel.sendToServer(
                    new AimingChangeRequestPacket(self.getId(), p.getInventory().selected, false));
        }
    }

    @Unique
    private boolean pointblankextra$isDisableAdsDuringReload() {
        if (!PointBlankExtraConfig.ENABLE_MOD.get()) {
            return false;
        }
        GunItem gun = ((GunClientState) (Object) this).getGunItem();
        if (gun == null) {
            return false;
        }
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(gun);
        return key != null && WeaponExtraDataManager.isDisableAdsDuringReload(key);
    }
}
