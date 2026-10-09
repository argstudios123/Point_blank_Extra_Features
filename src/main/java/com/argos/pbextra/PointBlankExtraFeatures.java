package com.argos.pbextra;

import com.argos.pbextra.config.PointBlankExtraConfig;
import com.argos.pbextra.network.ModNetwork;
import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(PointBlankExtraFeatures.MOD_ID)
public final class PointBlankExtraFeatures {

    public static final String MOD_ID = "pointblankextra";
    public static final String NAME = "Point Blank Extra Features";
    public static final Logger LOGGER = LogUtils.getLogger();

    public PointBlankExtraFeatures(FMLJavaModLoadingContext context) {
        context.registerConfig(
                ModConfig.Type.CLIENT,
                PointBlankExtraConfig.SPEC
        );

        ModNetwork.register();
    }
}
