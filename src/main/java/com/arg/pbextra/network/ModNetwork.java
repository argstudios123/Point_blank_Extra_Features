package com.arg.pbextra.network;

import com.arg.pbextra.melee.MeleeImpactPacket;
import com.arg.pbextra.melee.MeleeRequestPacket;
import com.arg.pbextra.melee.MeleeResponsePacket;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * The addon's own network channel. Kept separate from Point Blank's channel so
 * the addon never interferes with Point Blank's networking.
 */
public final class ModNetwork {

    private static final String PROTOCOL_VERSION = "1";

    private ModNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToServer(
                MeleeRequestPacket.TYPE,
                MeleeRequestPacket.STREAM_CODEC,
                MeleeRequestPacket::handle);
        // Server -> client: the server picks the animation and tells the shooter to play it.
        registrar.playToClient(
                MeleeResponsePacket.TYPE,
                MeleeResponsePacket.STREAM_CODEC,
                MeleeResponsePacket::handle);
        registrar.playToServer(
                MeleeImpactPacket.TYPE,
                MeleeImpactPacket.STREAM_CODEC,
                MeleeImpactPacket::handle);
        // Server -> client only: the editor's configuration is read and written on
        // the client because the HUD it edits is rendered there.
        registrar.playToClient(
                OpenUiEditorPacket.TYPE,
                OpenUiEditorPacket.STREAM_CODEC,
                OpenUiEditorPacket::handle);
        // Server -> client only: Point Blank's content pack data is re-read on the
        // client, so /pbextra reload only asks the clients to do the work.
        registrar.playToClient(
                ReloadContentPacket.TYPE,
                ReloadContentPacket.STREAM_CODEC,
                ReloadContentPacket::handle);
    }
}
