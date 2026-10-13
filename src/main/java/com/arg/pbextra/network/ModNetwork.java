package com.arg.pbextra.network;

import com.arg.pbextra.melee.MeleeImpactPacket;
import com.arg.pbextra.melee.MeleeRequestPacket;
import com.arg.pbextra.melee.MeleeResponsePacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

/**
 * The addon's own network channel. Kept separate from Point Blank's channel so the
 * addon never interferes with Point Blank's networking.
 *
 * <p>The NeoForge build registered the payloads through {@code PayloadRegistrar} and
 * dispatched with {@code PacketDistributor}. On Fabric the payload types are declared
 * with {@code PayloadTypeRegistry} and the receivers with {@code ServerPlayNetworking}
 * / {@code ClientPlayNetworking}; the payload classes themselves are unchanged, and
 * every receiver re-enters the game on the thread that owns the side it was delivered
 * on, which is what {@code IPayloadContext#enqueueWork} did.</p>
 */
public final class ModNetwork {

    private ModNetwork() {
    }

    /**
     * Declares every payload type and registers the server-bound receivers.
     * Runs from the common initialiser, so a dedicated server has the channel too.
     */
    public static void register() {
        // Client -> server
        PayloadTypeRegistry.serverboundPlay().register(MeleeRequestPacket.TYPE, MeleeRequestPacket.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(MeleeImpactPacket.TYPE, MeleeImpactPacket.STREAM_CODEC);

        // Server -> client
        PayloadTypeRegistry.clientboundPlay().register(MeleeResponsePacket.TYPE, MeleeResponsePacket.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(OpenUiEditorPacket.TYPE, OpenUiEditorPacket.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ReloadContentPacket.TYPE, ReloadContentPacket.STREAM_CODEC);

        ServerPlayNetworking.registerGlobalReceiver(MeleeRequestPacket.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            context.server().execute(() -> MeleeRequestPacket.handle(payload, player));
        });

        ServerPlayNetworking.registerGlobalReceiver(MeleeImpactPacket.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            context.server().execute(() -> MeleeImpactPacket.handle(payload, player));
        });
    }

    /**
     * Registers the client-bound receivers. Deliberately not part of
     * {@link #register()} so a dedicated server never loads a client-only handler
     * class such as {@code MeleeAnimationHandler} or {@code UiEditorScreen}.
     */
    public static void registerClient() {
        ClientPlayNetworking.registerGlobalReceiver(MeleeResponsePacket.TYPE,
                (payload, context) -> context.client().execute(() -> MeleeResponsePacket.handle(payload)));

        ClientPlayNetworking.registerGlobalReceiver(OpenUiEditorPacket.TYPE,
                (payload, context) -> context.client().execute(() -> OpenUiEditorPacket.handle(payload)));

        ClientPlayNetworking.registerGlobalReceiver(ReloadContentPacket.TYPE,
                (payload, context) -> context.client().execute(() -> ReloadContentPacket.handle(payload)));
    }
}
