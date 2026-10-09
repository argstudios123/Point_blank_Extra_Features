package com.argos.pbextra.ammo;

import com.vicmatskiv.pointblank.item.AmmoItem;
import com.vicmatskiv.pointblank.item.FireModeInstance;
import com.vicmatskiv.pointblank.item.GunItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

import java.util.List;

// This Class will be resposible for showing Reserve Ammo which Vic dont wanna add them
public final class ReserveAmmoCounter {


    public static final int CREATIVE_RESERVE = 999;

    private static final long CACHE_NANOS = 50_000_000L;
    private static ItemStack cachedGun = ItemStack.EMPTY;
    private static FireModeInstance cachedFireMode;
    private static boolean cachedCreative;
    private static long cachedAt;
    private static int cachedReserve = -1;

    private ReserveAmmoCounter() {
    }

    public static int reserveForDisplay(GunItem gun, ItemStack gunStack, FireModeInstance fireMode,
                                        int magazineCapacity) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || fireMode == null || gunStack == null || gunStack.isEmpty()) {
            return magazineCapacity;
        }

        boolean creative = isCreative(player);
        if (cacheValid(gunStack, fireMode, creative)) {
            return cachedReserve;
        }

        int reserve = creative ? CREATIVE_RESERVE : countReserve(gun, gunStack, fireMode, player);

        cachedGun = gunStack;
        cachedFireMode = fireMode;
        cachedCreative = creative;
        cachedAt = System.nanoTime();
        cachedReserve = reserve;
        return reserve;
    }


    public static void invalidate() {
        cachedReserve = -1;
        cachedGun = ItemStack.EMPTY;
        cachedFireMode = null;
    }

    private static boolean cacheValid(ItemStack gunStack, FireModeInstance fireMode, boolean creative) {
        return cachedReserve >= 0
                && cachedGun == gunStack
                && cachedFireMode == fireMode
                && cachedCreative == creative
                && System.nanoTime() - cachedAt < CACHE_NANOS;
    }


    public static int countReserve(GunItem gun, ItemStack gunStack, FireModeInstance fireMode, Player player) {
        // Point Blank refuses to treat anything as a bullet for a gun that does not
        // list the fire mode being used.
        if (!GunItem.getFireModes(gunStack).contains(fireMode)) {
            return 0;
        }

        boolean defaultAmmoPool = fireMode.isUsingDefaultAmmoPool();
        List<AmmoItem> compatible = defaultAmmoPool ? gun.getCompatibleAmmo() : List.of();
        AmmoItem requiredAmmo = defaultAmmoPool ? null : fireMode.getAmmo();

        Inventory inventory = player.getInventory();
        int total = 0;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack slotStack = inventory.getItem(slot);
            if (slotStack.isEmpty()) {
                continue;
            }
            Item item = slotStack.getItem();
            boolean compatibleSlot = defaultAmmoPool ? compatible.contains(item) : item == requiredAmmo;
            if (compatibleSlot) {
                total += slotStack.getCount();
            }
        }
        return total;
    }

    private static boolean isCreative(LocalPlayer player) {
        MultiPlayerGameMode gameMode = Minecraft.getInstance().gameMode;
        if (gameMode != null) {
            GameType playerMode = gameMode.getPlayerMode();
            if (playerMode != null) {
                return playerMode.isCreative();
            }
        }
        return player.getAbilities().instabuild;
    }
}
