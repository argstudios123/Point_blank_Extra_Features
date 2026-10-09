package com.argos.pbextra.network;

import com.argos.pbextra.client.gui.UiEditorScreen;
import com.argos.pbextra.uieditor.UiDefinitionKind;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Server-to-client request that opens the in-game UI editor.
 *
 * <p>The request is only ever sent by {@code UiEditorCommand}, after the server
 * has verified the permission of the command source, so a client can never open
 * the editor on its own. Nothing but the UI kind and the definition id travels:
 * the configuration itself is read and written on the client, because the HUD it
 * edits is rendered there.</p>
 */
public final class OpenUiEditorPacket {

    private final String kindId;
    private final String definitionId;

    public OpenUiEditorPacket(UiDefinitionKind kind, String definitionId) {
        this(kind == null ? UiDefinitionKind.AMMO.id() : kind.id(), definitionId);
    }

    public OpenUiEditorPacket(String kindId, String definitionId) {
        this.kindId = kindId == null ? UiDefinitionKind.AMMO.id() : kindId;
        this.definitionId = definitionId == null ? "" : definitionId;
    }

    public String kindId() {
        return kindId;
    }

    public String definitionId() {
        return definitionId;
    }

    public static void encode(OpenUiEditorPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.kindId);
        buf.writeUtf(msg.definitionId);
    }

    public static OpenUiEditorPacket decode(FriendlyByteBuf buf) {
        return new OpenUiEditorPacket(buf.readUtf(), buf.readUtf());
    }

    public static void handle(OpenUiEditorPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> UiEditorScreen.open(msg.kindId, msg.definitionId)));
        ctx.setPacketHandled(true);
    }
}
