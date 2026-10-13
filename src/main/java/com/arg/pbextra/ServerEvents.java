package com.arg.pbextra;

import com.arg.pbextra.command.PbExtraCommand;
import com.arg.pbextra.command.UiEditorCommand;
import com.arg.pbextra.data.WeaponExtraDataManager;
import com.arg.pbextra.melee.MeleeManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

public final class ServerEvents {

    private ServerEvents() {
    }

    @Mod.EventBusSubscriber(modid = PointBlankExtraFeatures.MOD_ID,
            bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class ForgeBus {

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
