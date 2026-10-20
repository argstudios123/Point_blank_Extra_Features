package com.arg.pbextra.client.crosshair;

import com.arg.pbextra.PointBlankExtraFeatures;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;


/**
 * Client-side cache invalidation for custom crosshair textures.
 *
 * <p>The actual rendering is integrated with Point Blank's native crosshair
 * call by {@code ClientEventHandlerCrosshairMixin}; this class only clears the
 * resolved-texture and PNG-dimension caches after a resource reload.</p>
 */
@EventBusSubscriber(modid = PointBlankExtraFeatures.MOD_ID, value = Dist.CLIENT)
public final class CustomCrosshairClientEvents {

    private CustomCrosshairClientEvents() {
    }

    @SubscribeEvent
    public static void addReloadListener(AddClientReloadListenersEvent event) {
        event.addListener(
                Identifier.fromNamespaceAndPath(PointBlankExtraFeatures.MOD_ID,
                        "custom_crosshair"),
                (ResourceManagerReloadListener) resources -> CustomCrosshairTextures.clear());
    }
}
