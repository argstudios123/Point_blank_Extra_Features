package com.argos.pbextra.melee;

import com.argos.pbextra.network.ModNetwork;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;


public final class MeleeRequestPacket {

    private final boolean offhand;

    public MeleeRequestPacket(boolean offhand) {
        this.offhand = offhand;
    }

    public boolean isOffhand() {
        return offhand;
    }

    public static void encode(MeleeRequestPacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.offhand);
    }

    public static MeleeRequestPacket decode(FriendlyByteBuf buf) {
        return new MeleeRequestPacket(buf.readBoolean());
    }

    public static void handle(MeleeRequestPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player != null) {
                MeleeManager.handleRequest(player, msg.offhand);
            }
        });
        ctx.setPacketHandled(true);
    }
}
