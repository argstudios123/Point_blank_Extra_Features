package com.argos.pbextra.client;

import com.argos.pbextra.PointBlankExtraFeatures;
import com.argos.pbextra.ammo.ReserveAmmoCounter;
import com.argos.pbextra.config.PointBlankExtraConfig;
import com.argos.pbextra.data.WeaponExtraDataManager;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;


public final class ClientEvents {

    private ClientEvents() {
    }

    @Mod.EventBusSubscriber(modid = PointBlankExtraFeatures.MOD_ID,
            value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
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

    @Mod.EventBusSubscriber(modid = PointBlankExtraFeatures.MOD_ID,
            value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class ForgeBus {

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) {
                return;
            }
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
