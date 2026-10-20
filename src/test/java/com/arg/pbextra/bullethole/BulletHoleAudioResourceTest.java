package com.arg.pbextra.bullethole;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Verifies that the sound ids referenced by the Java defaults are actually
 * backed by the mod's {@code sounds.json} and OGG files. This is the wiring
 * that lets {@code pointblankextra:bullet_holes.default} resolve through the
 * normal SoundEvent registry instead of being skipped as a missing sound.
 */
class BulletHoleAudioResourceTest {

    private static final String SOUNDS_JSON = "assets/pointblankextra/sounds.json";

    @Test
    void everyDeclaredBulletHoleSoundHasAnExistingOggResource() throws Exception {
        JsonObject root = readSoundsJson();
        assertFalse(root.isEmpty());

        for (String eventId : root.keySet()) {
            JsonObject event = root.getAsJsonObject(eventId);
            List<String> sounds = soundNames(event);
            assertFalse(sounds.isEmpty(), eventId + " declares no sounds");
            for (String sound : sounds) {
                assertOgg(sound);
            }
        }
    }

    @Test
    void defaultImpactSoundIsDeclaredAndBackedByExistingAssets() throws Exception {
        JsonObject root = readSoundsJson();
        String key = BulletHoleDefaults.DEFAULT_SOUND.getPath();
        assertTrue(root.has(key),
                () -> "sounds.json must declare " + key);

        List<String> sounds = soundNames(root.getAsJsonObject(key));
        assertFalse(sounds.isEmpty());
        for (String sound : sounds) {
            assertOgg(sound);
        }
    }

    @Test
    void builtInMaterialSoundIdsAreDeclared() throws Exception {
        JsonObject root = readSoundsJson();
        assertTrue(root.has(BulletHoleDefaults.DEFAULT_SOUND_PATH));
        assertTrue(root.has(BulletHoleDefaults.WOOD_SOUND_PATH));
        assertTrue(root.has(BulletHoleDefaults.GLASS_SOUND_PATH));
        assertTrue(root.has(BulletHoleDefaults.DIRT_SOUND_PATH));
        assertTrue(root.has(BulletHoleDefaults.METAL_SOUND_PATH));
        assertTrue(root.has(BulletHoleDefaults.METAL_SOUND_PATH));
    }

    @Test
    void allBundledImpactSoundsAreMonoForPositionalPlayback() throws Exception {
        URL directory = BulletHoleAudioResourceTest.class.getClassLoader()
                .getResource("assets/pointblankextra/sounds/bullet_holes");
        assertNotNull(directory, "bullet-hole sound directory");

        List<Path> files;
        Path root = Paths.get(directory.toURI());
        try (Stream<Path> walk = Files.walk(root)) {
            files = walk.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".ogg"))
                    .sorted()
                    .toList();
        }

        assertFalse(files.isEmpty(), "no bundled impact sounds found");
        for (Path file : files) {
            byte[] data = Files.readAllBytes(file);
            assertEquals(1, oggVorbisChannels(data),
                    file.getFileName() + " must be mono so OpenAL spatializes it");
        }
    }

    /**
     * Reads the channel count from the first Ogg page's Vorbis identification
     * header. Layout: "OggS", header fields, segment table, then packet type 1,
     * "vorbis", version (4 bytes) and the channel count byte.
     */
    private static int oggVorbisChannels(byte[] data) {
        assertTrue(data.length >= 28, "truncated OGG file");
        assertEquals('O', data[0]);
        assertEquals('g', data[1]);
        assertEquals('g', data[2]);
        assertEquals('S', data[3]);

        int pageSegments = data[26] & 0xFF;
        int packet = 27 + pageSegments;
        assertTrue(packet + 12 <= data.length, "truncated Vorbis header");
        assertEquals(1, data[packet] & 0xFF,
                "first packet must be a Vorbis identification header");
        assertEquals("vorbis", new String(data, packet + 1, 6,
                StandardCharsets.ISO_8859_1));
        return data[packet + 11] & 0xFF;
    }

    private static JsonObject readSoundsJson() throws Exception {
        try (InputStream stream = BulletHoleAudioResourceTest.class.getClassLoader()
                .getResourceAsStream(SOUNDS_JSON)) {
            assertNotNull(stream, SOUNDS_JSON);
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(reader).getAsJsonObject();
            }
        }
    }

    private static List<String> soundNames(JsonObject event) {
        List<String> names = new ArrayList<>();
        if (event == null || !event.has("sounds") || !event.get("sounds").isJsonArray()) {
            return names;
        }
        for (JsonElement element : event.getAsJsonArray("sounds")) {
            if (element.isJsonPrimitive()) {
                names.add(element.getAsString());
            } else if (element.isJsonObject()
                    && element.getAsJsonObject().has("name")) {
                names.add(element.getAsJsonObject().get("name").getAsString());
            }
        }
        return names;
    }

    private static void assertOgg(String soundName) throws Exception {
        Identifier id = Identifier.parse(soundName);
        String path = "assets/" + id.getNamespace() + "/sounds/" + id.getPath() + ".ogg";
        try (InputStream stream = BulletHoleAudioResourceTest.class.getClassLoader()
                .getResourceAsStream(path)) {
            assertNotNull(stream, path);
            byte[] header = stream.readNBytes(4);
            assertTrue(header.length == 4
                            && header[0] == 'O' && header[1] == 'g'
                            && header[2] == 'g' && header[3] == 'S',
                    path + " must contain Ogg audio");
        }
    }
}
