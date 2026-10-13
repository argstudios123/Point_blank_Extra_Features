package com.arg.pbextra.network;

import com.arg.pbextra.client.gui.UiEditorScreen;
import com.arg.pbextra.uieditor.UiDefinitionKind;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server-to-client request that opens the in-game UI editor.
 *
 * <p>The request is only ever sent by {@code UiEditorCommand}, after the server
 * has verified the permission of the command source, so a client can never open
 * the editor on its own. Nothing but the UI kind and the definition id travels:
 * the configuration itself is read and written on the client, because the HUD it
 * edits is rendered there.</p>
 */
public final class OpenUiEditorPacket implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<OpenUiEditorPacket> TYPE =
            new CustomPacketPayload.Type<>(
                    Identifier.fromNamespaceAndPath("pointblankextra", "open_ui_editor"));

    public static final StreamCodec<FriendlyByteBuf, OpenUiEditorPacket> STREAM_CODEC =
            StreamCodec.ofMember(OpenUiEditorPacket::encode, OpenUiEditorPacket::decode);

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

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(OpenUiEditorPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.kindId);
        buf.writeUtf(msg.definitionId);
    }

    public static OpenUiEditorPacket decode(FriendlyByteBuf buf) {
        return new OpenUiEditorPacket(buf.readUtf(), buf.readUtf());
    }

    public static void handle(OpenUiEditorPacket msg, IPayloadContext context) {
        context.enqueueWork(() -> UiEditorScreen.open(msg.kindId, msg.definitionId));
    }
}
