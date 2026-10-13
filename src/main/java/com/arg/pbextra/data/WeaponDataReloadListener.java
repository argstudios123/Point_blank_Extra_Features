package com.arg.pbextra.data;

import com.arg.pbextra.PointBlankExtraFeatures;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

/**
 * Rebuilds the weapon extra-data index whenever the resource manager reloads.
 *
 * <p>The NeoForge build passed {@code WeaponExtraDataManager::reload} straight to
 * {@code AddReloadListenerEvent} / {@code RegisterClientReloadListenersEvent}.
 * Fabric's {@code ResourceManagerHelper} needs a named listener with an identifier, so
 * this thin wrapper provides exactly the same behaviour on both sides: it implements
 * {@link ResourceManagerReloadListener} for the synchronous reload callback and
 * {@link IdentifiableResourceReloadListener} for the identifier Fabric requires.</p>
 */
public final class WeaponDataReloadListener
        implements IdentifiableResourceReloadListener, ResourceManagerReloadListener {

    private static final WeaponDataReloadListener SERVER = new WeaponDataReloadListener("weapon_data");
    private static final WeaponDataReloadListener CLIENT = new WeaponDataReloadListener("weapon_data_client");

    private final Identifier id;

    private WeaponDataReloadListener(String path) {
        this.id = Identifier.fromNamespaceAndPath(PointBlankExtraFeatures.MOD_ID, path);
    }

    /** Listener for {@code PackType.SERVER_DATA}. */
    public static WeaponDataReloadListener server() {
        return SERVER;
    }

    /** Listener for {@code PackType.CLIENT_RESOURCES}. */
    public static WeaponDataReloadListener client() {
        return CLIENT;
    }

    @Override
    public Identifier getFabricId() {
        return id;
    }

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        WeaponExtraDataManager.reload(resourceManager);
    }
}
