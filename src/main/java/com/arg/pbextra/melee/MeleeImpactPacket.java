package com.arg.pbextra.melee;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;


public final class MeleeImpactPacket implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<MeleeImpactPacket> TYPE =
            new CustomPacketPayload.Type<>(
                    Identifier.fromNamespaceAndPath("pointblankextra", "melee_impact"));

    public static final StreamCodec<FriendlyByteBuf, MeleeImpactPacket> STREAM_CODEC =
            StreamCodec.ofMember(MeleeImpactPacket::encode, MeleeImpactPacket::decode);

    private final String animationName;

    public MeleeImpactPacket(String animationName) {
        this.animationName = animationName == null ? "" : animationName;
    }

    public String getAnimationName() {
        return animationName;
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(MeleeImpactPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.animationName);
    }

    public static MeleeImpactPacket decode(FriendlyByteBuf buf) {
        return new MeleeImpactPacket(buf.readUtf());
    }

    /** Handled on the logical server, on its own thread (see {@code ModNetwork}). */
    public static void handle(MeleeImpactPacket msg, ServerPlayer player) {
        if (player != null) {
            MeleeManager.handleImpact(player, msg.animationName);
        }
    }
}
