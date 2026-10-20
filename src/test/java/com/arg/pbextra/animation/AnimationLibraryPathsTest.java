package com.arg.pbextra.animation;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnimationLibraryPathsTest {

    @Test
    void nativeStringIsDeclaredButNotAdditive() {
        var element = JsonParser.parseString("\"walking_animations\"");

        assertEquals(List.of("walking_animations"),
                AnimationLibraryPaths.parseDeclared(element));
        assertTrue(AnimationLibraryPaths.parseAdditional(element).isEmpty());
    }

    @Test
    void arrayIsParsedInOrderForBothForms() {
        var element = JsonParser.parseString("""
                [
                  "default_animations/pistol_anims/walking_animations",
                  "default_animations/pistol_anims/idle_animations",
                  "default_animations/pistol_anims/dual_wield_animations"
                ]
                """);

        List<String> expected = List.of(
                "default_animations/pistol_anims/walking_animations",
                "default_animations/pistol_anims/idle_animations",
                "default_animations/pistol_anims/dual_wield_animations");
        assertEquals(expected, AnimationLibraryPaths.parseDeclared(element));
        assertEquals(expected, AnimationLibraryPaths.parseAdditional(element));
    }

    @Test
    void nonStringEntriesAndDuplicatesAreIgnored() {
        var element = JsonParser.parseString("""
                [
                  "default_animations/pistol_anims",
                  { "bad": true },
                  42,
                  "  default_animations/pistol_anims  ",
                  ""
                ]
                """);

        assertEquals(List.of("default_animations/pistol_anims"),
                AnimationLibraryPaths.parseAdditional(element));
    }

    @Test
    void relativePathsUseThePointBlankNamespace() {
        assertEquals("pointblank:default_animations/pistol_anims/walking_animations",
                AnimationLibraryPaths.normalizeIdentifier(
                        "default_animations/pistol_anims/walking_animations"));
        assertEquals("pointblank:walking_animations",
                AnimationLibraryPaths.normalizeIdentifier(" walking_animations "));
    }

    @Test
    void explicitNamespacesArePreserved() {
        assertEquals("mypack:default_animations/pistol_anims",
                AnimationLibraryPaths.normalizeIdentifier(
                        "mypack:default_animations/pistol_anims"));
    }

    @Test
    void invalidIdentifiersAreRejected() {
        assertNull(AnimationLibraryPaths.normalizeIdentifier(null));
        assertNull(AnimationLibraryPaths.normalizeIdentifier(""));
        assertNull(AnimationLibraryPaths.normalizeIdentifier("Upper_Case"));
        assertNull(AnimationLibraryPaths.normalizeIdentifier("has space"));
        assertNull(AnimationLibraryPaths.normalizeIdentifier("mypack:"));
        assertNull(AnimationLibraryPaths.normalizeIdentifier(":path"));
        assertNull(AnimationLibraryPaths.normalizeIdentifier("ns:a:b"));
        assertNull(AnimationLibraryPaths.normalizeIdentifier("/leading"));
        assertNull(AnimationLibraryPaths.normalizeIdentifier("trailing/"));
        assertNull(AnimationLibraryPaths.normalizeIdentifier("double//slash"));
        assertNull(AnimationLibraryPaths.normalizeIdentifier("../escape"));
        assertNull(AnimationLibraryPaths.normalizeIdentifier("a/../b"));
    }

    @Test
    void candidatePathsCoverTraditionalAndCustomAnimationRoots() {
        assertEquals(List.of(
                        "pointblank:animations/item/default_animations/pistol_anims/dual_wield_animations.animation.json",
                        "pointblank:geckolib/animations/item/default_animations/pistol_anims/dual_wield_animations.animation.json",
                        "pointblank:default_animations/pistol_anims/dual_wield_animations.animation.json"),
                AnimationLibraryPaths.candidateAnimationResources(
                        "pointblank:default_animations/pistol_anims/dual_wield_animations"));
    }

    @Test
    void customDirectoryAcceptsBothEntryStyles() {
        assertEquals("pointblank:default_animations/walking_animations",
                AnimationLibraryPaths.customLibraryIdentifier("walking_animations"));
        assertEquals("pointblank:default_animations/pistol_anims/aw_pistol_dualwield",
                AnimationLibraryPaths.customLibraryIdentifier(
                        "default_animations/pistol_anims/aw_pistol_dualwield"));
        assertEquals("mypack:default_animations/walking_animations",
                AnimationLibraryPaths.customLibraryIdentifier("mypack:walking_animations"));
        assertNull(AnimationLibraryPaths.customLibraryIdentifier("../escape"));
    }

    @Test
    void customCacheKeysKeepTraditionalLookupFirst() {
        assertEquals(List.of(
                        "pointblank:item/walking_animations",
                        "pointblank:default_animations/walking_animations"),
                AnimationLibraryPaths.candidateAnimationCacheKeys(
                        "pointblank:walking_animations"));

        // Entries that already target default_animations/ have no traditional
        // animations/item/ form, so only the custom-directory key is produced.
        assertEquals(List.of(
                        "pointblank:default_animations/pistol_anims/aw_pistol_dualwield"),
                AnimationLibraryPaths.candidateAnimationCacheKeys(
                        "pointblank:default_animations/pistol_anims/aw_pistol_dualwield"));
    }

    @Test
    void traditionalLibraryIdentifiersPreserveNamespacesAndSkipTheCustomRoot() {
        assertEquals("pointblank:item/walking_animations",
                AnimationLibraryPaths.traditionalLibraryIdentifier("walking_animations"));
        assertEquals("mypack:item/walking_animations",
                AnimationLibraryPaths.traditionalLibraryIdentifier("mypack:walking_animations"));

        // A configured custom-root entry must not produce a dead
        // pointblank:item/default_animations/... fallback.
        assertNull(AnimationLibraryPaths.traditionalLibraryIdentifier(
                "default_animations/pistol_anims/aw_pistol_dualwield"));
        assertNull(AnimationLibraryPaths.traditionalLibraryIdentifier(
                "mypack:default_animations/pistol_anims/aw_pistol_dualwield"));
        assertNull(AnimationLibraryPaths.traditionalLibraryIdentifier(null));
        assertNull(AnimationLibraryPaths.traditionalLibraryIdentifier(""));
    }

    @Test
    void customResourcesAreRemappedUnderGeckoLibAnimations() {
        assertEquals(
                "geckolib/animations/default_animations/pistol_anims/aw_pistol_dualwield.animation.json",
                AnimationLibraryPaths.customAnimationTargetPath(
                        "default_animations/pistol_anims/aw_pistol_dualwield.animation.json"));
        assertEquals(
                "geckolib/animations/default_animations/walking_animations.animation.json",
                AnimationLibraryPaths.customAnimationTargetPath(
                        "default_animations/walking_animations.animation.json"));
        assertEquals(
                "geckolib/animations/default_animations/walking_animations.animation.json",
                AnimationLibraryPaths.customAnimationTargetPath(
                        "walking_animations.animation.json"));
        assertNull(AnimationLibraryPaths.customAnimationTargetPath(
                "default_animations/readme.txt"));
    }

    @Test
    void dualWieldSuffixesAreDetectedAndStripped() {
        assertTrue(AnimationLibraryPaths.isDualWieldAnimationName(
                "animation.model.idle_dwright"));
        assertTrue(AnimationLibraryPaths.isDualWieldAnimationName(
                "animation.model.idle_dwleft"));
        assertFalse(AnimationLibraryPaths.isDualWieldAnimationName(
                "animation.model.idle"));

        assertEquals("animation.model.idle",
                AnimationLibraryPaths.stripDualWieldSuffix(
                        "animation.model.idle_dwright"));
        assertEquals("animation.model.idle",
                AnimationLibraryPaths.stripDualWieldSuffix(
                        "animation.model.idle_dwleft"));
        assertNull(AnimationLibraryPaths.stripDualWieldSuffix("animation.model.idle"));
    }

    @Test
    void nullAndMissingValuesAreEmpty() {
        assertTrue(AnimationLibraryPaths.parseDeclared(null).isEmpty());
        assertTrue(AnimationLibraryPaths.parseDeclared(JsonParser.parseString("null")).isEmpty());
        assertTrue(AnimationLibraryPaths.parseAdditional(JsonParser.parseString("\"native\"")).isEmpty());
    }
}
