package com.arg.pbextra.melee;

import com.arg.pbextra.client.MeleeAnimationHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;


public final class MeleeResponsePacket implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<MeleeResponsePacket> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath("pointblankextra", "melee_response"));

    public static final StreamCodec<FriendlyByteBuf, MeleeResponsePacket> STREAM_CODEC =
            StreamCodec.ofMember(MeleeResponsePacket::encode, MeleeResponsePacket::decode);

    private final boolean offhand;
    private final String animationName;
    private final int durationMillis;
    private final int preMeleeMillis;

    public MeleeResponsePacket(boolean offhand, String animationName,
                              int durationMillis, int preMeleeMillis) {
        this.offhand = offhand;
        this.animationName = animationName == null ? "" : animationName;
        this.durationMillis = Math.max(0, durationMillis);
        this.preMeleeMillis = Math.max(0, preMeleeMillis);
    }

    public boolean isOffhand() {
        return offhand;
    }

    public String getAnimationName() {
        return animationName;
    }

    /** Playback window in animation-time milliseconds; {@code 0} means "let the animation play out". */
    public int getDurationMillis() {
        return durationMillis;
    }

    /** Animation time at which the hit is applied, in milliseconds. */
    public int getPreMeleeMillis() {
        return preMeleeMillis;
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(MeleeResponsePacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.offhand);
        buf.writeUtf(msg.animationName);
        buf.writeVarInt(msg.durationMillis);
        buf.writeVarInt(msg.preMeleeMillis);
    }

    public static MeleeResponsePacket decode(FriendlyByteBuf buf) {
        return new MeleeResponsePacket(buf.readBoolean(), buf.readUtf(), buf.readVarInt(),
                buf.readVarInt());
    }

    public static void handle(MeleeResponsePacket msg, IPayloadContext context) {
        context.enqueueWork(() -> MeleeAnimationHandler.playMeleeAnimation(
                msg.isOffhand(), msg.getAnimationName(),
                msg.getDurationMillis(), msg.getPreMeleeMillis()));
    }
}
