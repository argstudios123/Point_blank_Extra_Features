package com.argos.pbextra.melee;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;


public final class MeleeImpactPacket {

    private final String animationName;

    public MeleeImpactPacket(String animationName) {
        this.animationName = animationName == null ? "" : animationName;
    }

    public String getAnimationName() {
        return animationName;
    }

    public static void encode(MeleeImpactPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.animationName);
    }

    public static MeleeImpactPacket decode(FriendlyByteBuf buf) {
        return new MeleeImpactPacket(buf.readUtf());
    }

    public static void handle(MeleeImpactPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player != null) {
                MeleeManager.handleImpact(player, msg.animationName);
            }
        });
        ctx.setPacketHandled(true);
    }
}
