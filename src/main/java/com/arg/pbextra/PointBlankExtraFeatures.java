package com.arg.pbextra;

import com.arg.pbextra.config.PointBlankExtraConfig;
import com.arg.pbextra.network.ModNetwork;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.slf4j.Logger;

@Mod(PointBlankExtraFeatures.MOD_ID)
public final class PointBlankExtraFeatures {

    public static final String MOD_ID = "pointblankextra";
    public static final String NAME = "Point Blank Extra Features";
    public static final Logger LOGGER = LogUtils.getLogger();

    public PointBlankExtraFeatures(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(
                ModConfig.Type.CLIENT,
                PointBlankExtraConfig.SPEC
        );

        modEventBus.addListener((RegisterPayloadHandlersEvent event) -> ModNetwork.register(event));
    }
}
