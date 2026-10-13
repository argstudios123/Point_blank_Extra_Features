package com.arg.pbextra;

import com.arg.pbextra.config.PointBlankExtraConfig;
import com.arg.pbextra.impact.WaterLavaImpactParticles;
import com.arg.pbextra.network.ModNetwork;
import com.arg.pbextra.reload.PointBlankContentReload;
import com.arg.pbextra.uieditor.UiEditorEvents;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;

/**
 * Common entry point.
 *
 * <p>The NeoForge build used a {@code @Mod} constructor with the mod event bus and a
 * {@code ModContainer} config registration; on Fabric the same work is performed from
 * {@link ModInitializer#onInitialize()} and the loader's API events, which
 * {@link ServerEvents} registers.</p>
 */
public final class PointBlankExtraFeatures implements ModInitializer {

    public static final String MOD_ID = "pointblankextra";
    public static final String NAME = "Point Blank Extra Features";
    public static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public void onInitialize() {
        // The configuration is read here, before anything can query a value. This is
        // where the NeoForge build registered its client ModConfigSpec.
        PointBlankExtraConfig.initialize();

        // The addon's own payload channel, kept separate from Point Blank's. The
        // server-bound half runs from the common initialiser so a dedicated server has
        // the channel too; the client-bound half is registered by PointBlankExtraClient.
        ModNetwork.register();

        // Server side: resource reload listeners, commands and the death hook.
        ServerEvents.register();

        // Point Blank's own event buses only exist once every mod has been created,
        // which is guaranteed by the time a Fabric mod initialiser runs.
        WaterLavaImpactParticles.install();
        PointBlankContentReload.install();

        // The editor's generated UI overrides are applied on top of the content pack
        // definitions at startup (the NeoForge build did this from FMLCommonSetupEvent).
        UiEditorEvents.initialize();
    }
}
