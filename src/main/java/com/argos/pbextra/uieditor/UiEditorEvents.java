package com.argos.pbextra.uieditor;

import com.argos.pbextra.PointBlankExtraFeatures;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;


@Mod.EventBusSubscriber(modid = PointBlankExtraFeatures.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.MOD)
public final class UiEditorEvents {

    private UiEditorEvents() {
    }

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            int loaded = GeneratedUiStore.loadOverrides();
            if (loaded > 0) {
                PointBlankExtraFeatures.LOGGER.info("Applied {} generated UI override(s) from {}",
                        loaded, GeneratedUiStore.rootDirectory());
            }
        });
    }
}
