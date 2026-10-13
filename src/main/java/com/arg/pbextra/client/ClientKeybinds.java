package com.arg.pbextra.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
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

    private ClientKeybinds() {
    }
}
