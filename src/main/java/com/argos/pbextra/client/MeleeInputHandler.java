package com.argos.pbextra.client;

import com.argos.pbextra.data.MeleeConfig;
import com.argos.pbextra.data.WeaponExtraDataManager;
import com.argos.pbextra.melee.MeleeRequestPacket;
import com.argos.pbextra.network.ModNetwork;
import com.vicmatskiv.pointblank.item.GunItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


public final class MeleeInputHandler {

    private static final Map<UUID, Long> LOCAL_LAST_MELEE_TICK = new ConcurrentHashMap<>();

    private MeleeInputHandler() {
    }

    public static void onMeleeKeyPressed(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null) {
            return;
        }

        GunItem.OperableGunContext context = resolveGunContext(player);
        if (context == null || context.itemStack().isEmpty()) {
            return;
        }

        ItemStack stack = context.itemStack();
        if (!(stack.getItem() instanceof GunItem)) {
            return;
        }

        if (!MeleeEligibility.isMeleeAllowed(player)) {
            return;
        }

        MeleeConfig config = WeaponExtraDataManager.getMeleeConfig(stack);
        if (!config.isEnabled()) {
            return;
        }

        long now = minecraft.level.getGameTime();
        Long last = LOCAL_LAST_MELEE_TICK.get(player.getUUID());
        if (last != null && (now - last) < config.getCooldownTicks()) {
            return;
        }
        LOCAL_LAST_MELEE_TICK.put(player.getUUID(), now);

        ModNetwork.CHANNEL.sendToServer(new MeleeRequestPacket(context.offhand()));
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
