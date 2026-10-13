package com.arg.pbextra.client;

import com.arg.pbextra.PointBlankExtraFeatures;
import com.arg.pbextra.ammo.ReserveAmmoCounter;
import com.arg.pbextra.config.PointBlankExtraConfig;
import com.arg.pbextra.data.WeaponExtraDataManager;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;


public final class ClientEvents {

    private ClientEvents() {
    }

    @EventBusSubscriber(modid = PointBlankExtraFeatures.MOD_ID,
            value = Dist.CLIENT)
    public static final class ModBus {

        @SubscribeEvent
        public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
            event.register(ClientKeybinds.MELEE_KEY);
        }


        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            MeleeAnimationHandler.install();
        }

        @SubscribeEvent
        public static void registerReloadListeners(RegisterClientReloadListenersEvent event) {
            event.registerReloadListener((ResourceManagerReloadListener) WeaponExtraDataManager::reload);
        }

        @SubscribeEvent
        public static void onConfigReload(ModConfigEvent.Reloading event) {
            if (event.getConfig().getSpec() == PointBlankExtraConfig.SPEC) {
                ReserveAmmoCounter.invalidate();
            }
        }
    }

    @EventBusSubscriber(modid = PointBlankExtraFeatures.MOD_ID,
            value = Dist.CLIENT)
    public static final class GameBus {

        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null || minecraft.level == null) {
                return;
            }
            while (ClientKeybinds.MELEE_KEY.consumeClick()) {
                MeleeInputHandler.onMeleeKeyPressed(minecraft);
            }
            MeleeAnimationHandler.tick();
        }
    }
}
