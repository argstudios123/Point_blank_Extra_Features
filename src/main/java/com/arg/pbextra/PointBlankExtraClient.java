package com.arg.pbextra;

import com.arg.pbextra.client.ClientEvents;
import com.arg.pbextra.network.ModNetwork;
import net.fabricmc.api.ClientModInitializer;

/**
 * Client entry point.
 *
 * <p>Everything the addon registers on the client lives behind this class, so a
 * dedicated server never loads a client-only class: the NeoForge build achieved the
 * same separation with {@code Dist.CLIENT} event subscribers.</p>
 */
public final class PointBlankExtraClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // The addon's client-bound payload handlers.
        ModNetwork.registerClient();

        ClientEvents.register();
    }
}
