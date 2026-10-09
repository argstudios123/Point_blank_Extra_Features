package com.argos.pbextra.network;

import com.argos.pbextra.client.PbClientReload;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;


public final class ReloadContentPacket {

    public static final ReloadContentPacket INSTANCE = new ReloadContentPacket();

    public ReloadContentPacket() {
    }

    public static void encode(ReloadContentPacket message, FriendlyByteBuf buffer) {
        // no payload
    }

    public static ReloadContentPacket decode(FriendlyByteBuf buffer) {
        return INSTANCE;
    }

    public static void handle(ReloadContentPacket message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> PbClientReload.begin()));
        context.setPacketHandled(true);
    }
}
