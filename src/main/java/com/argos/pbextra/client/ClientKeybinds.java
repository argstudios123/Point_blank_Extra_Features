package com.argos.pbextra.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;


public final class ClientKeybinds {

    public static final String CATEGORY = "key.categories.pointblankextra";

    public static final KeyMapping MELEE_KEY = new KeyMapping(
            "key.pointblankextra.melee",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            CATEGORY
    );

    private ClientKeybinds() {
    }
}
