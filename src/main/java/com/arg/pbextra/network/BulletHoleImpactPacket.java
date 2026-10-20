package com.arg.pbextra.network;

import com.arg.pbextra.client.bulletholes.BulletHoleDecals;
import com.arg.pbextra.client.bulletholes.BulletHoleParticles;
import com.arg.pbextra.client.bulletholes.BulletHoleSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;


/**
 * Server -> client impact notification for bullet-hole decals, block-debris
 * particles and optional material-impact sound.
 * <p>
 * Point Blank resolves the impact on the server for both hitscan shots and
 * projectiles, so the server sends the already computed block face, exact hit
 * point, selected sound-event id and configured impact volume to nearby
 * clients. Clients only create visual decals/debris and play the sound locally;
 * the server never stores or ticks any bullet-hole state.
 */
public record BulletHoleImpactPacket(Identifier weaponId, BlockPos pos,
                                     Direction face, Vec3 location,
                                     Identifier soundId, float impactVolume,
                                     boolean visualEnabled)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<BulletHoleImpactPacket> TYPE =
            new CustomPacketPayload.Type<>(
                    Identifier.fromNamespaceAndPath("pointblankextra", "bullet_hole_impact"));

    public static final StreamCodec<FriendlyByteBuf, BulletHoleImpactPacket> STREAM_CODEC =
            StreamCodec.ofMember(BulletHoleImpactPacket::encode,
                    BulletHoleImpactPacket::decode);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(BulletHoleImpactPacket message, FriendlyByteBuf buffer) {
        buffer.writeUtf(message.weaponId == null ? "" : message.weaponId.toString(), 256);
        buffer.writeBlockPos(message.pos);
        buffer.writeEnum(message.face);
        buffer.writeDouble(message.location.x);
        buffer.writeDouble(message.location.y);
        buffer.writeDouble(message.location.z);
        buffer.writeUtf(message.soundId == null ? "" : message.soundId.toString(), 256);
        buffer.writeFloat(message.impactVolume);
        buffer.writeBoolean(message.visualEnabled);
    }

    public static BulletHoleImpactPacket decode(FriendlyByteBuf buffer) {
        String weapon = buffer.readUtf(256);
        Identifier weaponId = weapon.isEmpty() ? null : Identifier.tryParse(weapon);
        BlockPos pos = buffer.readBlockPos();
        Direction face = buffer.readEnum(Direction.class);
        Vec3 location = new Vec3(buffer.readDouble(), buffer.readDouble(),
                buffer.readDouble());
        String sound = buffer.readUtf(256);
        Identifier soundId = sound.isEmpty() ? null : Identifier.tryParse(sound);
        float impactVolume = buffer.readFloat();
        boolean visualEnabled = buffer.readBoolean();
        return new BulletHoleImpactPacket(weaponId, pos, face, location, soundId,
                impactVolume, visualEnabled);
    }

    public static void handle(BulletHoleImpactPacket message, IPayloadContext context) {
        context.enqueueWork(() -> {
            BulletHoleSounds.play(message.weaponId, message.soundId,
                    message.location, message.impactVolume);
            if (message.visualEnabled) {
                // Debris is a separate effect from the persistent decal, but the
                // per-gun enabled flag gates every impact visual.
                BulletHoleParticles.spawn(message.weaponId, message.pos,
                        message.face, message.location);
                BulletHoleDecals.accept(
                        message.weaponId, message.pos, message.face, message.location);
            }
        });
    }
}
