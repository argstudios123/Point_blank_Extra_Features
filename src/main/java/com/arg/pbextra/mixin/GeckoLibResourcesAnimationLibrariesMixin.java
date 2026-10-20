package com.arg.pbextra.mixin;

import com.arg.pbextra.animation.AnimationLibraryResourceManager;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;


/**
 * Adds the optional {@code assets/<namespace>/default_animations/} directory to
 * GeckoLib's animation bake pass.
 * <p>
 * GeckoLib's {@code loadAnimations} passes its {@link ResourceManager} to
 * {@code bakeAllResources}, which lists {@code geckolib/animations}. Wrapping
 * that manager here keeps the normal GeckoLib loader, cache keys and error
 * handling unchanged while making the custom directory visible only to this
 * one bake pass.
 */
@Mixin(targets = "com.geckolib.cache.GeckoLibResources", remap = false)
public abstract class GeckoLibResourcesAnimationLibrariesMixin {

    @ModifyVariable(
            method = "loadAnimations",
            at = @At("HEAD"),
            argsOnly = true,
            remap = false
    )
    private static ResourceManager pbextra$includeDefaultAnimations(ResourceManager manager) {
        return AnimationLibraryResourceManager.wrap(manager);
    }
}
