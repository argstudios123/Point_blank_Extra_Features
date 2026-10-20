package com.arg.pbextra.client.bulletholes;

import com.arg.pbextra.bullethole.BulletHoleConfig;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Client-side texture validation and render-type cache for bullet holes.
 * <p>
 * A configured texture is checked once against the resource manager; missing or
 * invalid textures fall back to the built-in default and are only warned about
 * once. Render types are memoised per resolved texture, so an impact never
 * rebuilds shader state.
 */
public final class BulletHoleTextures {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final Map<Identifier, Identifier> RESOLVED = new ConcurrentHashMap<>();
    private static final Map<Identifier, RenderType> RENDER_TYPES = new ConcurrentHashMap<>();
    private static final Set<Identifier> WARNED = ConcurrentHashMap.newKeySet();

    private BulletHoleTextures() {
    }

    /** Resolves a configured texture to a usable one, or {@code null} if unavailable. */
    public static Identifier resolve(Identifier requested) {
        if (requested == null) {
            return null;
        }
        return RESOLVED.computeIfAbsent(requested, BulletHoleTextures::resolveUncached);
    }

    private static Identifier resolveUncached(Identifier requested) {
        if (exists(requested)) {
            return requested;
        }

        if (WARNED.add(requested)) {
            LOGGER.warn("Bullet-hole texture '{}' is missing; using the built-in default", requested);
        }
        if (requested.equals(BulletHoleConfig.DEFAULT_TEXTURE)) {
            return null;
        }
        return exists(BulletHoleConfig.DEFAULT_TEXTURE)
                ? BulletHoleConfig.DEFAULT_TEXTURE : null;
    }

    private static boolean exists(Identifier texture) {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.getResourceManager() != null
                && minecraft.getResourceManager().getResource(texture).isPresent();
    }

    /** Translucent, lightmapped, z-offset decal render type, memoised per texture. */
    public static RenderType renderType(Identifier texture) {
        return RENDER_TYPES.computeIfAbsent(texture, BulletHoleTextures::createRenderType);
    }

    private static RenderType createRenderType(Identifier texture) {
        RenderSetup setup = RenderSetup.builder(BulletHolePipelines.BULLET_HOLE)
                .withTexture("Sampler0", texture)
                .useLightmap()
                .useOverlay()
                .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                .sortOnUpload()
                .setOutline(RenderSetup.OutlineProperty.NONE)
                .createRenderSetup();
        return RenderType.create("pointblankextra_bullet_hole", setup);
    }

    /** Drops cached validation/render types after a resource reload. */
    public static void clear() {
        RESOLVED.clear();
        RENDER_TYPES.clear();
    }
}
