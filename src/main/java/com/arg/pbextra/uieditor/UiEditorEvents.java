package com.arg.pbextra.uieditor;

import com.arg.pbextra.PointBlankExtraFeatures;


/**
 * Applies the UI overrides the in-game editor generated on every startup.
 *
 * <p>The NeoForge build hooked this to {@code FMLCommonSetupEvent}; on Fabric the
 * common mod initialiser calls {@link #initialize()} instead, so the generated
 * overrides are still applied before any HUD is rendered.</p>
 */
public final class UiEditorEvents {

    private UiEditorEvents() {
    }

    public static void initialize() {
        int loaded = GeneratedUiStore.loadOverrides();
        if (loaded > 0) {
            PointBlankExtraFeatures.LOGGER.info("Applied {} generated UI override(s) from {}",
                    loaded, GeneratedUiStore.rootDirectory());
        }
    }
}
