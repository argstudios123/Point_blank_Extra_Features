package com.arg.pbextra.network;

import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class BulletHoleImpactPacketTest {

    @Test
    void codecRoundTripsSoundAndImpactVolume() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            BulletHoleImpactPacket packet = new BulletHoleImpactPacket(
                    Identifier.parse("pointblank:test_gun"),
                    new BlockPos(1, 2, 3),
                    Direction.NORTH,
                    new Vec3(1.5D, 2.5D, 3.5D),
                    Identifier.parse("minecraft:block.gravel.hit"),
                    0.5F,
                    true);

            BulletHoleImpactPacket.encode(packet, buffer);
            BulletHoleImpactPacket decoded = BulletHoleImpactPacket.decode(buffer);

            assertEquals(packet.weaponId(), decoded.weaponId());
            assertEquals(packet.pos(), decoded.pos());
            assertEquals(packet.face(), decoded.face());
            assertEquals(packet.location(), decoded.location());
            assertEquals(packet.soundId(), decoded.soundId());
            assertEquals(0.5F, decoded.impactVolume(), 0.0001F);
        } finally {
            buffer.release();
        }
    }

    @Test
    void codecAllowsNoSoundAndVisualDisable() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            BulletHoleImpactPacket packet = new BulletHoleImpactPacket(
                    null, BlockPos.ZERO, Direction.UP, Vec3.ZERO,
                    null, 0.0F, false);

            BulletHoleImpactPacket.encode(packet, buffer);
            BulletHoleImpactPacket decoded = BulletHoleImpactPacket.decode(buffer);

            assertNull(decoded.weaponId());
            assertNull(decoded.soundId());
            assertEquals(0.0F, decoded.impactVolume(), 0.0001F);
            assertFalse(decoded.visualEnabled());
        } finally {
            buffer.release();
        }
    }
}