package com.argos.pbextra.network;

import com.argos.pbextra.melee.MeleeImpactPacket;
import com.argos.pbextra.melee.MeleeRequestPacket;
import com.argos.pbextra.melee.MeleeResponsePacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

/**
 * The addon's own network channel. Kept separate from Point Blank's channel so
 * the addon never interferes with Point Blank's networking.
 */
public final class ModNetwork {

    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath("pointblankextra", "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private ModNetwork() {
    }

    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(id++,
                MeleeRequestPacket.class,
                MeleeRequestPacket::encode,
                MeleeRequestPacket::decode,
                MeleeRequestPacket::handle);
        CHANNEL.registerMessage(id++,
                MeleeResponsePacket.class,
                MeleeResponsePacket::encode,
                MeleeResponsePacket::decode,
                MeleeResponsePacket::handle);
        CHANNEL.registerMessage(id++,
                MeleeImpactPacket.class,
                MeleeImpactPacket::encode,
                MeleeImpactPacket::decode,
                MeleeImpactPacket::handle);
        // Server -> client only: the editor's configuration is read and written on
        // the client because the HUD it edits is rendered there.
        CHANNEL.registerMessage(id++,
                OpenUiEditorPacket.class,
                OpenUiEditorPacket::encode,
                OpenUiEditorPacket::decode,
                OpenUiEditorPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        // Server -> client only: Point Blank's content pack data is re-read on the
        // client, so /pbextra reload only asks the clients to do the work.
        CHANNEL.registerMessage(id++,
                ReloadContentPacket.class,
                ReloadContentPacket::encode,
                ReloadContentPacket::decode,
                ReloadContentPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }
}
