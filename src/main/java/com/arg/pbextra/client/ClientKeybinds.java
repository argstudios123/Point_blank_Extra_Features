package com.arg.pbextra.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import org.lwjgl.glfw.GLFW;


public final class ClientKeybinds {

    public static final KeyMapping.Category CATEGORY =
            new KeyMapping.Category(Identifier.fromNamespaceAndPath("pointblankextra", "main"));

    public static final KeyMapping MELEE_KEY = new KeyMapping(
            "key.pointblankextra.melee",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            CATEGORY
    );

    /** Default binding for opening the ammo UI editor: CTRL + G. */
    public static final KeyMapping OPEN_AMMO_EDITOR_KEY = new KeyMapping(
            "key.pointblankextra.open_ammo_editor",
            KeyConflictContext.IN_GAME,
            KeyModifier.CONTROL,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            CATEGORY
    );

    private ClientKeybinds() {
    }
}
