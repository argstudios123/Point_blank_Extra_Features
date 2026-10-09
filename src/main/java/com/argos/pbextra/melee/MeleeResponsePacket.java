package com.argos.pbextra.melee;

import com.argos.pbextra.client.MeleeAnimationHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;


public final class MeleeResponsePacket {

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

    public static void handle(MeleeResponsePacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> MeleeAnimationHandler.playMeleeAnimation(
                            msg.isOffhand(), msg.getAnimationName(),
                            msg.getDurationMillis(), msg.getPreMeleeMillis()));
        });
        ctx.setPacketHandled(true);
    }
}

