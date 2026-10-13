package com.arg.pbextra.melee;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;


public final class MeleeImpactPacket implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<MeleeImpactPacket> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath("pointblankextra", "melee_impact"));

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

    public static void handle(MeleeImpactPacket msg, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                MeleeManager.handleImpact(player, msg.animationName);
            }
        });
    }
}
