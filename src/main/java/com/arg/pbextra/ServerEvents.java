package com.arg.pbextra;

import com.arg.pbextra.command.PbExtraCommand;
import com.arg.pbextra.command.UiEditorCommand;
import com.arg.pbextra.data.WeaponDataReloadListener;
import com.arg.pbextra.melee.MeleeManager;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.entity.player.Player;

/**
 * Server side and shared registration.
 *
 * <p>The NeoForge build used an {@code @EventBusSubscriber} class with
 * {@code AddReloadListenerEvent}, {@code RegisterCommandsEvent} and
 * {@code LivingDeathEvent} subscribers. The Fabric equivalents are registered here
 * from the common mod initialiser.</p>
 */
public final class ServerEvents {

    private ServerEvents() {
    }

    public static void register() {
        // Point Blank's own weapon JSON lives in the server data pack, so the weapon
        // index is first built from the server's resource manager.
        ResourceManagerHelper.get(PackType.SERVER_DATA)
                .registerReloadListener(WeaponDataReloadListener.server());

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            UiEditorCommand.register(dispatcher);
            PbExtraCommand.register(dispatcher);
        });

        // An interrupted swing must not be reported after the player died.
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            if (entity instanceof Player player) {
                MeleeManager.clearCooldown(player.getUUID());
            }
        });
    }
}
