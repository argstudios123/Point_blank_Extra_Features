package com.arg.pbextra.client.crosshair;

import com.arg.pbextra.crosshair.CustomCrosshairLayout;
import com.arg.pbextra.crosshair.CustomCrosshairPaths;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Resolves and caches custom crosshair textures from the client resource
 * manager, and reads their dimensions from the PNG header.
 *
 * <p>Resolution and decoding happen once per texture; failures are warned
 * about once and cached as unavailable, so a broken texture never causes a
 * per-frame lookup storm. Caches are cleared on a resource reload.</p>
 */
public final class CustomCrosshairTextures {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final Map<String, Optional<Identifier>> RESOLVED =
            new ConcurrentHashMap<>();
    private static final Map<Identifier, Optional<TextureInfo>> INFO =
            new ConcurrentHashMap<>();
    private static final Set<String> WARNED = ConcurrentHashMap.newKeySet();

    private static final byte[] PNG_SIGNATURE = {
            (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A
    };

    private CustomCrosshairTextures() {
    }

    /** Resolves a configured path to a resource-manager texture, or {@code null}. */
    public static Identifier resolve(String raw) {
        if (raw == null) {
            return null;
        }
        return RESOLVED.computeIfAbsent(raw, CustomCrosshairTextures::resolveUncached)
                .orElse(null);
    }

    private static Optional<Identifier> resolveUncached(String raw) {
        Identifier id = CustomCrosshairPaths.parse(raw);
        if (id == null) {
            warnOnce("path:" + raw, "Custom crosshair texture '{}' has an invalid path", raw);
            return Optional.empty();
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getResourceManager() == null
                || minecraft.getResourceManager().getResource(id).isEmpty()) {
            warnOnce("missing:" + raw,
                    "Custom crosshair texture '{}' is missing; leaving the normal crosshair",
                    id);
            return Optional.empty();
        }
        return Optional.of(id);
    }

    /** Width/height of a texture, or {@code null} if it cannot be read. */
    public static TextureInfo info(Identifier texture) {
        if (texture == null) {
            return null;
        }
        return INFO.computeIfAbsent(texture, CustomCrosshairTextures::readInfo)
                .orElse(null);
    }

    private static Optional<TextureInfo> readInfo(Identifier texture) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getResourceManager() == null) {
            return Optional.empty();
        }

        Resource resource = minecraft.getResourceManager().getResource(texture)
                .orElse(null);
        if (resource == null) {
            warnOnce("missing-png:" + texture,
                    "Custom crosshair texture '{}' disappeared; leaving the normal crosshair",
                    texture);
            return Optional.empty();
        }

        try {
            ImageHeader header = readHeader(resource);
            if (header == null) {
                warnOnce("bad-png:" + texture,
                        "Custom crosshair texture '{}' is not a readable PNG", texture);
                return Optional.empty();
            }
            if (header.width() <= 0 || header.height() <= 0
                    || header.width() > 8192 || header.height() > 8192) {
                warnOnce("bad-size:" + texture,
                        "Custom crosshair texture '{}' has invalid dimensions {}x{}",
                        texture, header.width(), header.height());
                return Optional.empty();
            }

            // Full decode: this matches Minecraft's texture loader and rejects
            // PNGs whose header is valid but whose image data is corrupt.
            try (InputStream stream = resource.open();
                 NativeImage image = NativeImage.read(stream)) {
                int width = image.getWidth();
                int height = image.getHeight();
                if (width <= 0 || height <= 0 || width > 8192 || height > 8192) {
                    warnOnce("bad-size:" + texture,
                            "Custom crosshair texture '{}' has invalid dimensions {}x{}",
                            texture, width, height);
                    return Optional.empty();
                }
                return Optional.of(new TextureInfo(width, height));
            }
        } catch (IOException | RuntimeException exception) {
            warnOnce("read-error:" + texture,
                    "Could not read custom crosshair texture '{}': {}",
                    texture, exception.getMessage());
            return Optional.empty();
        }
    }

    private static ImageHeader readHeader(Resource resource) throws IOException {
        try (InputStream stream = resource.open()) {
            byte[] header = stream.readNBytes(24);
            if (header.length < 24 || !isPng(header)) {
                return null;
            }
            return new ImageHeader(readInt(header, 16), readInt(header, 20));
        }
    }

    private static boolean isPng(byte[] header) {
        for (int i = 0; i < PNG_SIGNATURE.length; i++) {
            if (header[i] != PNG_SIGNATURE[i]) {
                return false;
            }
        }
        return header[12] == 'I' && header[13] == 'H'
                && header[14] == 'D' && header[15] == 'R';
    }

    private static int readInt(byte[] data, int offset) {
        return ((data[offset] & 0xFF) << 24)
                | ((data[offset + 1] & 0xFF) << 16)
                | ((data[offset + 2] & 0xFF) << 8)
                | (data[offset + 3] & 0xFF);
    }

    private static void warnOnce(String key, String message, Object... arguments) {
        if (WARNED.add(key)) {
            LOGGER.warn(message, arguments);
        }
    }

    /** Validates the 39x39 native atlas required by layout "pointblank". */
    public static boolean validateNativeLayout(Identifier texture, TextureInfo info) {
        if (info != null
                && CustomCrosshairLayout.POINTBLANK.accepts(info.width(), info.height())) {
            return true;
        }
        if (info != null) {
            warnOnce("native-layout:" + texture,
                    "Custom crosshair texture '{}' uses layout 'pointblank' but is "
                            + "{}x{}; expected 39x39, leaving the native crosshair",
                    texture, info.width(), info.height());
        }
        return false;
    }

    private record ImageHeader(int width, int height) {
    }

    /** Drops all caches after a resource reload. */
    public static void clear() {
        RESOLVED.clear();
        INFO.clear();
        WARNED.clear();
    }

    public record TextureInfo(int width, int height) {
    }
}
