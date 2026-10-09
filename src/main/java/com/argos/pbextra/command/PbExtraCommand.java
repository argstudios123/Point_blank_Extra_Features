package com.argos.pbextra.command;

import com.argos.pbextra.PointBlankExtraFeatures;
import com.argos.pbextra.network.ModNetwork;
import com.argos.pbextra.network.ReloadContentPacket;
import com.argos.pbextra.reload.PointBlankContentReload;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraftforge.network.PacketDistributor;

import java.util.List;
import java.util.concurrent.CompletableFuture;
// Responsible to  for PB Extra command where you will
public final class PbExtraCommand {

    public static final String ROOT = "pbextra";

    public static final int REQUIRED_PERMISSION_LEVEL = UiEditorCommand.REQUIRED_PERMISSION_LEVEL;

    private PbExtraCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(ROOT)
                .requires(source -> source.hasPermission(REQUIRED_PERMISSION_LEVEL))
                .then(Commands.literal("reload")
                        .executes(PbExtraCommand::reload)));
    }


    private static int reload(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        MinecraftServer server = source.getServer();

        // 1) Re-discover the content packs of this JVM. The pack finder only hands
        //    back the aggregate pack Point Blank built at startup, so new folders
        //    and replaced zips only show up after Point Blank re-scans.
        String rescanWarning = PointBlankContentReload.rediscoverExtensionPacks();

        // 2) Server side: re-run the pack finders so the rebuilt aggregate pack is
        //    selected, then run vanilla's server resource reload.
        CompletableFuture<Void> serverReload;
        try {
            PackRepository packs = server.getPackRepository();
            packs.reload();
            serverReload = server.reloadResources(packs.getSelectedIds());
        } catch (RuntimeException exception) {
            PointBlankExtraFeatures.LOGGER.error("Point Blank content reload: server resource reload failed", exception);
            source.sendFailure(Component.literal(
                    "Server data reload failed: " + describe(exception) + " - clients were not asked to reload."));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("Reloading Point Blank content packs..."), false);
        serverReload.whenComplete((ignored, error) -> server.execute(() ->
                afterServerReload(source, rescanWarning, error)));
        return 1;
    }


    private static void afterServerReload(CommandSourceStack source, String rescanWarning, Throwable error) {
        MinecraftServer server = source.getServer();
        StringBuilder report = new StringBuilder();
        if (error != null) {
            PointBlankExtraFeatures.LOGGER.error("Point Blank content reload: server resource reload failed", error);
            report.append("Server data reload failed: ").append(describe(error)).append(". ");
        } else {
            report.append("Server data reloaded");
            if (rescanWarning != null) {
                report.append(" (pack rescan: ").append(rescanWarning).append(')');
            }
            report.append(". ");
        }

        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        for (ServerPlayer player : players) {
            ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), ReloadContentPacket.INSTANCE);
        }
        if (players.isEmpty()) {
            // A dedicated server can be reloaded from the console; there is simply
            // no client to refresh the assets and UI registries on.
            report.append("No client is connected, so no client side assets, UI definitions "
                    + "or generated overrides were reloaded.");
        } else {
            report.append("Asked ").append(players.size())
                    .append(" client(s) to reload their packs, textures, Point Blank UI definitions "
                            + "and generated overrides.");
        }

        Component message = Component.literal(report.toString());
        if (error != null) {
            source.sendFailure(message);
        } else {
            source.sendSuccess(() -> message, true);
        }
    }

    private static String describe(Throwable throwable) {
        Throwable cause = throwable;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        String message = cause.getMessage();
        return message == null || message.isBlank()
                ? cause.getClass().getSimpleName() : message;
    }
}
