package com.arg.pbextra.network;

import com.arg.pbextra.client.PbClientReload;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;


public final class ReloadContentPacket implements CustomPacketPayload {

    public static final ReloadContentPacket INSTANCE = new ReloadContentPacket();

    public static final CustomPacketPayload.Type<ReloadContentPacket> TYPE =
            new CustomPacketPayload.Type<>(
                    Identifier.fromNamespaceAndPath("pointblankextra", "reload_content"));

    public static final StreamCodec<FriendlyByteBuf, ReloadContentPacket> STREAM_CODEC =
            StreamCodec.ofMember(ReloadContentPacket::encode, ReloadContentPacket::decode);

    public ReloadContentPacket() {
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(ReloadContentPacket message, FriendlyByteBuf buffer) {
        // no payload
    }

    public static ReloadContentPacket decode(FriendlyByteBuf buffer) {
        return INSTANCE;
    }

    public static void handle(ReloadContentPacket message, IPayloadContext context) {
        context.enqueueWork(PbClientReload::begin);
    }
}
