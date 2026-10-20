package com.arg.pbextra.client.bulletholes;

import com.arg.pbextra.PointBlankExtraFeatures;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;


// Rendering hook for bulletholes
@EventBusSubscriber(modid = PointBlankExtraFeatures.MOD_ID, value = Dist.CLIENT)
public final class BulletHoleRenderer {

    private BulletHoleRenderer() {
    }

    @SubscribeEvent
    public static void onSubmitCustomGeometry(SubmitCustomGeometryEvent event) {
        BulletHoleDecals.render(event);
    }
}
