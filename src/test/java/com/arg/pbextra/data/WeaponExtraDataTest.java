package com.arg.pbextra.data;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeaponExtraDataTest {

    @Test
    void nativeStringFallbackIsDeclaredButNotAdditive() {
        WeaponExtraData data = parse("""
                {
                  "toggle_aiming": true,
                  "firstPersonFallbackAnimations": "walking_animations",
                  "melee": { "enabled": true, "damage": 8.0 }
                }
                """);

        assertTrue(data.isToggleAiming());
        assertTrue(data.getMelee().isEnabled());
        assertEquals(List.of(), data.getAdditionalFirstPersonFallbacks());
        assertEquals(List.of("walking_animations"), data.getDeclaredFirstPersonFallbacks());
    }

    @Test
    void arrayFallbackIsAdditiveAndDeclaredInOrder() {
        WeaponExtraData data = parse("""
                {
                  "firstPersonFallbackAnimations": [
                    "default_animations/pistol_anims/walking_animations",
                    "default_animations/pistol_anims/idle_animations",
                    "default_animations/pistol_anims/dual_wield_animations"
                  ]
                }
                """);

        List<String> expected = List.of(
                "default_animations/pistol_anims/walking_animations",
                "default_animations/pistol_anims/idle_animations",
                "default_animations/pistol_anims/dual_wield_animations");
        assertEquals(expected, data.getAdditionalFirstPersonFallbacks());
        assertEquals(expected, data.getDeclaredFirstPersonFallbacks());
    }

    @Test
    void missingFallbackIsEmpty() {
        WeaponExtraData data = parse("""
                {
                  "toggle_aiming": false
                }
                """);

        assertEquals(List.of(), data.getAdditionalFirstPersonFallbacks());
        assertEquals(List.of(), data.getDeclaredFirstPersonFallbacks());
    }

    private static WeaponExtraData parse(String json) {
        return WeaponExtraData.fromJson(JsonParser.parseString(json).getAsJsonObject());
    }
}
