package com.arg.pbextra.data;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression coverage for the weapon JSON contract. The exact fixture from the
 * melee documentation must keep parsing with the same property names and
 * millisecond timings.
 */
class MeleeConfigTest {

    private static final String FIXTURE = """
            {
              "type": "Melee",
              "enabled": true,
              "damage": 8.0,
              "cooldown": 20,
              "reach": 3.0,
              "knockback": 3.0,
              "angleRange": 40,
              "damageCatsandOcalotsEntities": true,
              "meleeAnimations": [
                { "name": "animation.model.melee_1", "duration": 700, "preMelee": 200 },
                { "name": "animation.model.melee_2", "duration": 870, "preMelee": 220 },
                { "name": "animation.model.melee_3", "duration": 850, "preMelee": 220 }
              ]
            }
            """;

    @Test
    void fixtureParsesAllDocumentedProperties() {
        MeleeConfig config = parse(FIXTURE);

        assertTrue(config.isEnabled());
        assertEquals(8.0, config.getDamage(), 1.0E-9);
        assertEquals(20, config.getCooldownTicks());
        assertEquals(3.0, config.getReach(), 1.0E-9);
        assertEquals(3.0, config.getKnockback(), 1.0E-9);
        assertEquals(40.0, config.getAngleRange(), 1.0E-9);
        assertTrue(config.isDamageCatsAndOcelots());

        assertEquals(3, config.getAnimations().size());
        MeleeAnimation first = config.getAnimations().get(0);
        assertEquals("animation.model.melee_1", first.getName());
        assertEquals(700, first.getDurationMillis());
        assertEquals(200, first.getPreMeleeMillis());

        MeleeAnimation second = config.getAnimations().get(1);
        assertEquals(870, second.getDurationMillis());
        assertEquals(220, second.getPreMeleeMillis());

        MeleeAnimation third = config.getAnimations().get(2);
        assertEquals(850, third.getDurationMillis());
        assertEquals(220, third.getPreMeleeMillis());
    }

    @Test
    void missingOptionalPropertiesKeepBackwardsCompatibleDefaults() {
        MeleeConfig config = parse("""
                {
                  "enabled": true
                }
                """);

        assertEquals(MeleeConfig.DEFAULT_DAMAGE, config.getDamage(), 1.0E-9);
        assertEquals(MeleeConfig.DEFAULT_COOLDOWN_TICKS, config.getCooldownTicks());
        assertEquals(MeleeConfig.DEFAULT_REACH, config.getReach(), 1.0E-9);
        assertEquals(MeleeConfig.DEFAULT_KNOCKBACK, config.getKnockback(), 1.0E-9);
        assertEquals(MeleeConfig.DEFAULT_ANGLE_RANGE, config.getAngleRange(), 1.0E-9);
        assertFalse(config.isDamageCatsAndOcelots());
        assertTrue(config.getAnimations().isEmpty());
    }

    @Test
    void negativeValuesAreClamped() {
        MeleeConfig config = parse("""
                {
                  "enabled": true,
                  "damage": -1,
                  "cooldown": -5,
                  "reach": -2,
                  "knockback": -3,
                  "angleRange": -40
                }
                """);

        assertEquals(0.0, config.getDamage(), 1.0E-9);
        assertEquals(0, config.getCooldownTicks());
        assertEquals(0.0, config.getReach(), 1.0E-9);
        assertEquals(0.0, config.getKnockback(), 1.0E-9);
        assertEquals(0.0, config.getAngleRange(), 1.0E-9);
    }

    @Test
    void angleRangeIsCappedAtAFullCircle() {
        MeleeConfig config = parse("""
                {
                  "enabled": true,
                  "angleRange": 720
                }
                """);

        assertEquals(MeleeConfig.MAX_ANGLE_RANGE, config.getAngleRange(), 1.0E-9);
    }

    @Test
    void legacyAnimationNameArrayIsStillAccepted() {
        MeleeConfig config = parse("""
                {
                  "enabled": true,
                  "animations": ["animation.model.melee_1", "animation.model.melee_2"]
                }
                """);

        assertEquals(2, config.getAnimations().size());
        assertEquals("animation.model.melee_1", config.getAnimations().get(0).getName());
        assertEquals(0, config.getAnimations().get(0).getDurationMillis());
        assertEquals(0, config.getAnimations().get(0).getPreMeleeMillis());
    }

    @Test
    void malformedAnimationEntriesAreSkipped() {
        MeleeConfig config = parse("""
                {
                  "enabled": true,
                  "meleeAnimations": [
                    { "name": "animation.model.melee_1", "duration": 700, "preMelee": 200 },
                    { "duration": 500 },
                    "not an object"
                  ]
                }
                """);

        assertEquals(1, config.getAnimations().size());
        assertEquals("animation.model.melee_1", config.getAnimations().get(0).getName());
    }

    @Test
    void missingObjectIsDisabled() {
        assertFalse(MeleeConfig.fromJson(null).isEnabled());
    }

    private static MeleeConfig parse(String json) {
        JsonObject object = JsonParser.parseString(json).getAsJsonObject();
        return MeleeConfig.fromJson(object);
    }
}
