package com.arg.pbextra.mixin;

import com.vicmatskiv.pointblank.item.FireModeInstance;
import com.vicmatskiv.pointblank.item.GunItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes the magazine of a fire mode that declares {@code "maxAmmoCapacity": "infinite"}
 * behave exactly like the magazine of a weapon whose infinite capacity is declared on the
 * gun itself.
 *
 * <p>Point Blank stores two magazines on a gun stack: {@code ammo} for fire modes that use
 * the gun's default ammo pool and {@code ammox.<ammo item>} for fire modes that bring their
 * own ammo item. {@code GunItem#ensureItemStack} seeds {@code ammo} to
 * {@link GunItem#INFINITE_AMMO} when the gun's own capacity is infinite, but always creates
 * {@code ammox} empty and never looks at the per-fire-mode capacity. A fire mode that
 * declares an infinite capacity together with its own ammo item therefore starts - and
 * stays - with a stored magazine of {@code 0} while its capacity reports
 * {@link GunItem#INFINITE_AMMO}. Every native consumer of {@code GunItem#getAmmo} then
 * disagrees with the capacity:</p>
 *
 * <ul>
 *   <li>{@code ClientEventHandler#tryAutoReload} reloads as soon as the client-side
 *       magazine reports {@code <= 0}, and {@code GunClientState#tryReload} forces the
 *       reload state machine directly, so the weapon visibly reloads on equip and on every
 *       fire mode switch;</li>
 *   <li>{@code GunClientState#syncAmmo} copies the same {@code 0} into the client
 *       {@code AmmoCount}, so the fire state machine's {@code hasAmmo} guard blocks firing
 *       until that unwanted reload has filled the magazine;</li>
 *   <li>{@code GunItem#handleClientHitScanFireRequest} and
 *       {@code handleClientProjectileFireRequest} skip the whole shot when the stored
 *       magazine is {@code <= 0};</li>
 *   <li>{@code GunItem#getPotentialReloadAmount} reports a full reload on an infinite
 *       magazine.</li>
 * </ul>
 *
 * <p>Reporting {@link GunItem#INFINITE_AMMO} for such a fire mode resolves all of them at
 * the single place they all read, and matches what Point Blank itself stores for a
 * gun-wide infinite capacity, so both flavours of infinite magazine end up identical.</p>
 */
@Mixin(value = GunItem.class, remap = false)
public abstract class GunItemInfiniteMagazineMixin {

    @Inject(
            method = "getAmmo(Lnet/minecraft/world/item/ItemStack;"
                    + "Lcom/vicmatskiv/pointblank/item/FireModeInstance;)I",
            at = @At("RETURN"),
            cancellable = true,
            remap = false
    )
    private static void pointblankextra$infiniteMagazineIsAlwaysFull(
            ItemStack gunStack, FireModeInstance fireMode, CallbackInfoReturnable<Integer> cir) {
        // An infinite magazine can never legitimately be empty, so only an empty (or
        // invalid) stored value has to be corrected. Keeping the check on that cold path
        // avoids the capacity lookup for every regular shot and sync.
        if (cir.getReturnValueI() > 0 || fireMode == null || gunStack == null) {
            return;
        }
        if (!(gunStack.getItem() instanceof GunItem gun)) {
            return;
        }
        if (gun.getMaxAmmoCapacity(gunStack, fireMode) == GunItem.INFINITE_AMMO) {
            cir.setReturnValue(GunItem.INFINITE_AMMO);
        }
    }

    /**
     * Refuses every reload attempt for an infinite magazine. Point Blank asks
     * {@code getPotentialReloadAmount} before starting a reload from its state machine, but
     * the automatic reload ("reload when the magazine is empty") and the reload key call
     * this method directly and force the reload state without ever consulting that
     * eligibility, so the decision has to be taken here as well.
     */
    @Inject(
            method = "tryReload(Lnet/minecraft/world/entity/player/Player;"
                    + "Lnet/minecraft/world/item/ItemStack;)Z",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void pointblankextra$infiniteMagazineNeverReloads(
            Player player, ItemStack gunStack, CallbackInfoReturnable<Boolean> cir) {
        if (gunStack == null || gunStack.isEmpty()
                || !(gunStack.getItem() instanceof GunItem gun)) {
            return;
        }
        FireModeInstance fireMode = GunItem.getFireModeInstance(gunStack);
        if (fireMode != null && gun.getMaxAmmoCapacity(gunStack, fireMode) == GunItem.INFINITE_AMMO) {
            cir.setReturnValue(false);
        }
    }
}
