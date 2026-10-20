package com.arg.pbextra.client;

import com.arg.pbextra.PointBlankExtraFeatures;
import com.arg.pbextra.ammo.ReserveAmmoCounter;
import com.arg.pbextra.client.bulletholes.BulletHoleDecals;
import com.arg.pbextra.client.bulletholes.BulletHolePipelines;
import com.arg.pbextra.client.gui.UiEditorScreen;
import com.arg.pbextra.config.PointBlankExtraConfig;
import com.arg.pbextra.data.WeaponExtraDataManager;
import com.arg.pbextra.uieditor.UiDefinitionKind;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;


public final class ClientEvents {

    private ClientEvents() {
    }

    @EventBusSubscriber(modid = PointBlankExtraFeatures.MOD_ID,
            value = Dist.CLIENT)
    public static final class ModBus {

        @SubscribeEvent
        public static void registerRenderPipelines(RegisterRenderPipelinesEvent event) {
            event.registerPipeline(BulletHolePipelines.BULLET_HOLE);
            PointBlankExtraFeatures.LOGGER.debug(
                    "Registered the bullet-hole decal render pipeline");
        }

        @SubscribeEvent
        public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
            event.registerCategory(ClientKeybinds.CATEGORY);
            event.register(ClientKeybinds.MELEE_KEY);
            event.register(ClientKeybinds.OPEN_AMMO_EDITOR_KEY);
        }


        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            MeleeAnimationHandler.install();
        }

        @SubscribeEvent
        public static void registerReloadListeners(AddClientReloadListenersEvent event) {
            event.addListener(
                    Identifier.fromNamespaceAndPath(PointBlankExtraFeatures.MOD_ID, "weapon_extra_data"),
                    (ResourceManagerReloadListener) WeaponExtraDataManager::reload);
            event.addListener(
                    Identifier.fromNamespaceAndPath(PointBlankExtraFeatures.MOD_ID, "bullet_holes"),
                    (ResourceManagerReloadListener) resources -> BulletHoleDecals.onResourceReload());
        }

        @SubscribeEvent
        public static void onConfigReload(ModConfigEvent.Reloading event) {
            if (event.getConfig().getSpec() == PointBlankExtraConfig.SPEC) {
                ReserveAmmoCounter.invalidate();
                BulletHoleDecals.onConfigReload();
            }
        }
    }

    @EventBusSubscriber(modid = PointBlankExtraFeatures.MOD_ID,
            value = Dist.CLIENT)
    public static final class GameBus {

        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            Minecraft minecraft = Minecraft.getInstance();
            BulletHoleDecals.tick(minecraft);
            if (minecraft.player == null || minecraft.level == null) {
                MeleeInputHandler.reset();
                MeleeAnimationHandler.stopAndClear();
                return;
            }

            // Drain queued clicks (initial press and OS key-repeat events) but
            // drive repeating attacks from the held state in MeleeInputHandler.
            boolean pressedThisTick = false;
            while (ClientKeybinds.MELEE_KEY.consumeClick()) {
                pressedThisTick = true;
            }
            MeleeInputHandler.tick(minecraft, pressedThisTick);
            while (ClientKeybinds.OPEN_AMMO_EDITOR_KEY.consumeClick()) {
                // The binding uses IN_GAME, so it cannot fire while any screen is
                // open; the null check also protects against opening a second editor
                // between the click being queued and this tick.
                if (minecraft.screen == null) {
                    UiEditorScreen.open(UiDefinitionKind.AMMO.id(), null);
                }
            }
            MeleeAnimationHandler.tick();
        }

        @SubscribeEvent
        public static void onClientLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
            // Never carry a toggle or a switch blend from one session into the next.
            ToggleAimingController.onDisconnect();
            AdsSwitchBlend.reset();
            MeleeInputHandler.reset();
            MeleeAnimationHandler.stopAndClear();
            BulletHoleDecals.clear();
        }

        @SubscribeEvent
        public static void onClientPlayerClone(ClientPlayerNetworkEvent.Clone event) {
            // Respawn and dimension changes replace the player: release the ADS as well.
            ToggleAimingController.onPlayerReplaced();
            AdsSwitchBlend.reset();
            MeleeInputHandler.reset();
            MeleeAnimationHandler.stopAndClear();
            BulletHoleDecals.clear();
        }
    }
}
