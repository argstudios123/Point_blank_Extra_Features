package com.arg.pbextra.uieditor;

import com.arg.pbextra.PointBlankExtraFeatures;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;


@EventBusSubscriber(modid = PointBlankExtraFeatures.MOD_ID)
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
