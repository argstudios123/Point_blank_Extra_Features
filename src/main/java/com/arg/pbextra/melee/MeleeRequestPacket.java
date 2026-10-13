package com.arg.pbextra.melee;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;


public final class MeleeRequestPacket implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<MeleeRequestPacket> TYPE =
            new CustomPacketPayload.Type<>(
                    Identifier.fromNamespaceAndPath("pointblankextra", "melee_request"));

    public static final StreamCodec<FriendlyByteBuf, MeleeRequestPacket> STREAM_CODEC =
            StreamCodec.ofMember(MeleeRequestPacket::encode, MeleeRequestPacket::decode);

    private final boolean offhand;

    public MeleeRequestPacket(boolean offhand) {
        this.offhand = offhand;
    }

    public boolean isOffhand() {
        return offhand;
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(MeleeRequestPacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.offhand);
    }

    public static MeleeRequestPacket decode(FriendlyByteBuf buf) {
        return new MeleeRequestPacket(buf.readBoolean());
    }

    public static void handle(MeleeRequestPacket msg, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                MeleeManager.handleRequest(player, msg.offhand);
            }
        });
    }
}
