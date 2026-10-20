package com.arg.pbextra;

import com.arg.pbextra.command.PbExtraCommand;
import com.arg.pbextra.command.UiEditorCommand;
import com.arg.pbextra.data.WeaponExtraDataManager;
import com.arg.pbextra.melee.MeleeManager;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public final class ServerEvents {

    private ServerEvents() {
    }

    @EventBusSubscriber(modid = PointBlankExtraFeatures.MOD_ID)
    public static final class GameBus {

        @SubscribeEvent
        public static void onAddReloadListeners(AddServerReloadListenersEvent event) {
            event.addListener(
                    Identifier.fromNamespaceAndPath(PointBlankExtraFeatures.MOD_ID, "weapon_extra_data"),
                    (ResourceManagerReloadListener) WeaponExtraDataManager::reload);
        }

        @SubscribeEvent
        public static void onRegisterCommands(RegisterCommandsEvent event) {
            UiEditorCommand.register(event.getDispatcher());
            PbExtraCommand.register(event.getDispatcher());
        }

        @SubscribeEvent
        public static void onLivingDeath(LivingDeathEvent event) {
            if (event.getEntity() instanceof Player player) {
                MeleeManager.clearPlayerState(player.getUUID());
            }
        }

        @SubscribeEvent
        public static void onServerTick(ServerTickEvent.Post event) {
            MeleeManager.tick(event.getServer());
        }

        @SubscribeEvent
        public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
            // Do not keep attack state for a player who is no longer online.
            MeleeManager.clearPlayerState(event.getEntity().getUUID());
        }

        @SubscribeEvent
        public static void onPlayerClone(PlayerEvent.Clone event) {
            // Respawning replaces the player entity; an interrupted swing must
            // not be able to execute on the new one.
            MeleeManager.clearPlayerState(event.getOriginal().getUUID());
            MeleeManager.clearPlayerState(event.getEntity().getUUID());
        }
    }
}
