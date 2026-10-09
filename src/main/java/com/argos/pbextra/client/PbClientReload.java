package com.argos.pbextra.client;

import com.argos.pbextra.PointBlankExtraFeatures;
import com.argos.pbextra.reload.PointBlankContentReload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

import java.util.concurrent.CompletableFuture;

public final class PbClientReload {

    private static boolean inProgress;

    private PbClientReload() {
    }

    public static boolean isInProgress() {
        return inProgress;
    }

    public static void begin() {
        Minecraft minecraft = Minecraft.getInstance();
        if (!minecraft.isSameThread()) {
            minecraft.execute(PbClientReload::begin);
            return;
        }
        if (inProgress) {
            report("A Point Blank content reload is already running.", true);
            return;
        }
        inProgress = true;

        String rediscoveryWarning = minecraft.getSingleplayerServer() == null
                ? PointBlankContentReload.rediscoverExtensionPacks() : null;

        CompletableFuture<Void> reload;
        try {
            reload = minecraft.reloadResourcePacks();
        } catch (RuntimeException exception) {
            inProgress = false;
            PointBlankExtraFeatures.LOGGER.error("Could not start the resource reload", exception);
            report("Could not start the resource reload: " + describe(exception), true);
            return;
        }
        reload.whenComplete((ignored, error) -> minecraft.execute(() -> {
            inProgress = false;
            applyAndReport(error, rediscoveryWarning);
        }));
    }

    private static void applyAndReport(Throwable resourceReloadError, String rediscoveryWarning) {
        if (resourceReloadError != null) {
            PointBlankExtraFeatures.LOGGER.error("Point Blank content reload: resource reload failed", resourceReloadError);
            report("Resource reload failed: " + describe(resourceReloadError), true);
            return;
        }
        PointBlankContentReload.Result result;
        try {
            result = PointBlankContentReload.reload(Minecraft.getInstance().getResourceManager());
        } catch (RuntimeException exception) {
            PointBlankExtraFeatures.LOGGER.error("Point Blank content reload: reading definitions failed", exception);
            report("Reload failed while reading Point Blank definitions: " + describe(exception), true);
            return;
        }
        StringBuilder message = new StringBuilder(result.summary());
        if (rediscoveryWarning != null) {
            message.append(" (pack rescan: ").append(rediscoveryWarning).append(')');
        }
        report(message.toString(), result.hasProblems());
        String details = result.details();
        if (!details.isEmpty()) {
            report(details, true);
        }
    }

    private static void report(String message, boolean problem) {
        Component component = Component.literal("[Point Blank Extra] " + message)
                .withStyle(problem ? ChatFormatting.RED : ChatFormatting.GRAY);
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            player.displayClientMessage(component, false);
        } else {
            PointBlankExtraFeatures.LOGGER.info(message);
        }
        if (problem) {
            PointBlankExtraFeatures.LOGGER.warn(message);
        } else {
            PointBlankExtraFeatures.LOGGER.info(message);
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
