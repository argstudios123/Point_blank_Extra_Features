package com.arg.pbextra;

import com.arg.pbextra.command.PbExtraCommand;
import com.arg.pbextra.command.UiEditorCommand;
import com.arg.pbextra.data.WeaponExtraDataManager;
import com.arg.pbextra.melee.MeleeManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

public final class ServerEvents {

    private ServerEvents() {
    }

    @EventBusSubscriber(modid = PointBlankExtraFeatures.MOD_ID)
    public static final class GameBus {

        @SubscribeEvent
        public static void onAddReloadListeners(AddReloadListenerEvent event) {
            event.addListener((ResourceManagerReloadListener) WeaponExtraDataManager::reload);
        }

        @SubscribeEvent
        public static void onRegisterCommands(RegisterCommandsEvent event) {
            UiEditorCommand.register(event.getDispatcher());
            PbExtraCommand.register(event.getDispatcher());
        }

        @SubscribeEvent
        public static void onLivingDeath(LivingDeathEvent event) {
            if (event.getEntity() instanceof Player player) {
                MeleeManager.clearCooldown(player.getUUID());
            }
        }
    }
}
