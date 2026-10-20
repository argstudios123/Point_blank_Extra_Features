package com.arg.pbextra.client;

import com.arg.pbextra.data.MeleeConfig;
import com.arg.pbextra.data.WeaponExtraDataManager;
import com.arg.pbextra.melee.MeleeRequestPacket;
import com.vicmatskiv.pointblank.item.GunItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;


/**
 * Client-side melee input.
 * <p>
 * Repeating attacks are driven by the key's held state ({@link
 * net.minecraft.client.KeyMapping#isDown()}) and this handler's own cooldown,
 * not by {@link net.minecraft.client.KeyMapping#consumeClick()}. Click events
 * only arrive for the initial press and for operating-system key-repeat events,
 * so using them as the repeat driver made a held swing sequence depend on the
 * OS. Clicks are still consumed so a quick tap that starts and ends between two
 * ticks is not lost, and so a backlog of repeat clicks is discarded.
 */
public final class MeleeInputHandler {

    private static final long MILLIS_PER_TICK = 50L;

    /**
     * The client asks one tick after the configured cooldown so network jitter
     * cannot make the request arrive at the server a tick too early (which would
     * cost a whole repeat cycle). The server remains authoritative and enforces
     * the real cooldown.
     */
    private static final int CLIENT_COOLDOWN_MARGIN_TICKS = 1;

    private static Identifier lastWeaponId;
    private static long nextAttackMillis;

    private MeleeInputHandler() {
    }


    /**
     * Called once per client tick.
     *
     * @param pressedThisTick whether at least one click was queued since the
     *                        previous tick (covers a tap shorter than one tick)
     */
    public static void tick(Minecraft minecraft, boolean pressedThisTick) {
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null || minecraft.screen != null) {
            return;
        }

        boolean held = ClientKeybinds.MELEE_KEY.isDown();
        if (!held && !pressedThisTick) {
            return;
        }

        attemptMelee(player);
    }


    /** Forgets local cooldown state when the session or the player changes. */
    public static void reset() {
        lastWeaponId = null;
        nextAttackMillis = 0L;
    }


    private static void attemptMelee(LocalPlayer player) {
        GunItem.OperableGunContext context = resolveGunContext(player);
        if (context == null || context.itemStack().isEmpty()) {
            return;
        }

        ItemStack stack = context.itemStack();
        if (!(stack.getItem() instanceof GunItem)) {
            return;
        }

        if (!MeleeEligibility.isMeleeAllowed(player, context.offhand())) {
            return;
        }

        MeleeConfig config = WeaponExtraDataManager.getMeleeConfig(stack);
        if (!config.isEnabled()) {
            return;
        }

        Identifier weaponId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (weaponId == null) {
            return;
        }

        long now = System.nanoTime() / 1_000_000L;
        if (weaponId.equals(lastWeaponId) && now < nextAttackMillis) {
            return;
        }

        // Local rate limiting only: the server independently validates the
        // cooldown. At least one tick between requests also avoids flooding the
        // server when a weapon is configured with a zero cooldown. A monotonic
        // wall clock is used so a level change cannot leave a stale game time
        // blocking the next attack.
        lastWeaponId = weaponId;
        long localCooldownTicks = Math.max(1L,
                (long) config.getCooldownTicks() + CLIENT_COOLDOWN_MARGIN_TICKS);
        nextAttackMillis = now + localCooldownTicks * MILLIS_PER_TICK;

        ClientPacketDistributor.sendToServer(new MeleeRequestPacket(context.offhand()));
    }


    private static GunItem.OperableGunContext resolveGunContext(LocalPlayer player) {
        GunItem.OperableGunContext context = GunItem.resolveMainHandGunContext(player);
        if (context != null && !context.itemStack().isEmpty()) {
            return context;
        }
        context = GunItem.resolveOffhandGunContext(player);
        if (context != null && !context.itemStack().isEmpty()) {
            return context;
        }
        return null;
    }
}
