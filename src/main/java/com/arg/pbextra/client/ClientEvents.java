package com.arg.pbextra.client;

import com.arg.pbextra.ammo.ReserveAmmoCounter;
import com.arg.pbextra.config.PointBlankExtraConfig;
import com.arg.pbextra.data.WeaponDataReloadListener;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.PackType;

/**
 * Client side registration.
 *
 * <p>The NeoForge build used {@code Dist.CLIENT} event-bus subscribers for the key
 * mapping, the client reload listener, the config reload hook, the client setup and the
 * client tick. All of them are registered here from the Fabric client initialiser, so a
 * dedicated server never loads any of these classes.</p>
 */
public final class ClientEvents {

    /**
     * Entity id of the client player seen on the previous tick; {@code -1} when no player
     * has been tracked yet.
     */
    private static int lastPlayerId = -1;

    private ClientEvents() {
    }

    public static void register() {
        // "Melee Attack" under "Point Blank Extra Features" in Controls. Fabric's
        // key-mapping API also registers the KeyMapping.Category the key was built with,
        // which is what the NeoForge RegisterKeyMappingsEvent#registerCategory did.
        KeyMappingHelper.registerKeyMapping(ClientKeybinds.MELEE_KEY);

        // The client's resource manager is the one that can see the content pack
        // weapon JSON and the Point Blank UI definitions.
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES)
                .registerReloadListener(WeaponDataReloadListener.client());

        // The reserve ammo counter keeps a short lived cache; drop it as soon as the
        // configuration changes - the same thing ModConfigEvent.Reloading did.
        PointBlankExtraConfig.setChangeListener(ReserveAmmoCounter::invalidate);

        MeleeAnimationHandler.install();

        ClientTickEvents.END_CLIENT_TICK.register(ClientEvents::onClientTick);

        // The NeoForge build used ClientPlayerNetworkEvent.LoggingOut: never carry a
        // toggle or a switch blend from one session into the next. The connection is
        // already closing here, so the aiming state is only cleared locally.
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ToggleAimingController.onDisconnect();
            AdsSwitchBlend.reset();
            lastPlayerId = -1;
        });
    }

    private static void onClientTick(Minecraft minecraft) {
        // The NeoForge build used ClientPlayerNetworkEvent.Clone, which fires when the
        // client player entity is replaced (respawn, dimension change). Fabric has no
        // exact equivalent, so the replacement is detected from the player entity id -
        // the very thing the NeoForge handler reacted to. The connection is still alive
        // at this point, so the server is told about the aiming change.
        int playerId = minecraft.player == null ? -1 : minecraft.player.getId();
        if (playerId != lastPlayerId) {
            if (lastPlayerId != -1) {
                ToggleAimingController.onPlayerReplaced();
                AdsSwitchBlend.reset();
            }
            lastPlayerId = playerId;
        }

        if (minecraft.player == null || minecraft.level == null) {
            return;
        }
        while (ClientKeybinds.MELEE_KEY.consumeClick()) {
            MeleeInputHandler.onMeleeKeyPressed(minecraft);
        }
        MeleeAnimationHandler.tick();
    }
}
