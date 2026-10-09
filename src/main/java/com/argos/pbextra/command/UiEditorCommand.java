package com.argos.pbextra.command;

import com.argos.pbextra.network.ModNetwork;
import com.argos.pbextra.network.OpenUiEditorPacket;
import com.argos.pbextra.uieditor.UiDefinitionKind;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

import java.util.List;
import java.util.Locale;


public final class UiEditorCommand {

    /** Vanilla operator / permission level required to open the editor. */
    public static final int REQUIRED_PERMISSION_LEVEL = 2;

    private static final String ROOT = "pbui";
    private static final List<String> KIND_IDS = List.of("ammo", "heat");

    private UiEditorCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(ROOT)
                .requires(source -> source.hasPermission(REQUIRED_PERMISSION_LEVEL))
                .then(Commands.literal("open")
                        .then(Commands.argument("kind", StringArgumentType.word())
                                .suggests((context, builder) ->
                                        SharedSuggestionProvider.suggest(KIND_IDS, builder))
                                .executes(context -> open(context, null))
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .executes(context -> open(context,
                                                StringArgumentType.getString(context, "id")))))));
    }

    private static int open(CommandContext<CommandSourceStack> context, String id) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("The UI editor can only be opened by a player."));
            return 0;
        }
        String kindId = StringArgumentType.getString(context, "kind").toLowerCase(Locale.ROOT);
        if (!KIND_IDS.contains(kindId)) {
            source.sendFailure(Component.literal(
                    "Unknown UI kind '" + kindId + "'. Use 'ammo' or 'heat'."));
            return 0;
        }
        UiDefinitionKind kind = UiDefinitionKind.byId(kindId);
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new OpenUiEditorPacket(kind, id));
        source.sendSuccess(() -> Component.literal("Opening the " + kind.displayName() + " editor"
                + (id == null || id.isBlank() ? "." : " for '" + id.trim() + "'.")), false);
        return 1;
    }
}
