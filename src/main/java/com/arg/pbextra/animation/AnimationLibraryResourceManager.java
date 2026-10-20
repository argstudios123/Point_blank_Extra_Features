package com.arg.pbextra.animation;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.slf4j.Logger;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;


// Resource manager for
public final class AnimationLibraryResourceManager implements ResourceManager {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String GECKOLIB_ANIMATIONS_ROOT = "geckolib/animations";

    private final ResourceManager delegate;


    private AnimationLibraryResourceManager(ResourceManager delegate) {
        this.delegate = delegate;
    }


    /** Wraps a resource manager once; safe to call if it is already wrapped. */
    public static ResourceManager wrap(ResourceManager delegate) {
        if (delegate == null || delegate instanceof AnimationLibraryResourceManager) {
            return delegate;
        }
        return new AnimationLibraryResourceManager(delegate);
    }


    @Override
    public Optional<Resource> getResource(Identifier location) {
        Optional<Resource> direct = delegate.getResource(location);
        if (direct.isPresent()) {
            return direct;
        }

        // Make the synthetic identifiers usable through the normal
        // ResourceProvider API as well. This is not required by GeckoLib's
        // baking path, but keeps the wrapper transparent to other callers.
        String path = location.getPath();
        String syntheticPrefix = GECKOLIB_ANIMATIONS_ROOT + "/"
                + AnimationLibraryPaths.CUSTOM_LIBRARY_ROOT + "/";
        if (path.startsWith(syntheticPrefix)) {
            String relative = path.substring(GECKOLIB_ANIMATIONS_ROOT.length() + 1);
            return delegate.getResource(
                    Identifier.fromNamespaceAndPath(location.getNamespace(), relative));
        }

        return Optional.empty();
    }


    @Override
    public Set<String> getNamespaces() {
        return delegate.getNamespaces();
    }


    @Override
    public List<Resource> getResourceStack(Identifier location) {
        return delegate.getResourceStack(location);
    }


    @Override
    public Map<Identifier, Resource> listResources(String path,
                                                   Predicate<Identifier> predicate) {
        Map<Identifier, Resource> original = delegate.listResources(path, predicate);
        if (!GECKOLIB_ANIMATIONS_ROOT.equals(path)) {
            return original;
        }

        Map<Identifier, Resource> custom;
        try {
            custom = delegate.listResources(
                    AnimationLibraryPaths.CUSTOM_LIBRARY_ROOT, ignored -> true);
        } catch (Exception e) {
            LOGGER.debug("Could not list reusable animation libraries from '{}': {}",
                    AnimationLibraryPaths.CUSTOM_LIBRARY_ROOT, e.getMessage());
            return original;
        }

        if (custom.isEmpty()) {
            return original;
        }

        Map<Identifier, Resource> combined = new LinkedHashMap<>(original);
        int added = 0;
        for (Map.Entry<Identifier, Resource> entry : custom.entrySet()) {
            Identifier source = entry.getKey();
            String targetPath = AnimationLibraryPaths.customAnimationTargetPath(
                    source.getPath());
            if (targetPath == null) {
                continue;
            }

            Identifier target = Identifier.fromNamespaceAndPath(
                    source.getNamespace(), targetPath);
            if (predicate != null && !predicate.test(target)) {
                continue;
            }

            // The delegate's traditional resources win if a pack happens to
            // provide the same synthetic identifier as well.
            if (!combined.containsKey(target)) {
                combined.put(target, entry.getValue());
                added++;
            }
        }

        if (added > 0) {
            LOGGER.debug("Exposed {} reusable animation resource(s) from '{}' to GeckoLib",
                    added, AnimationLibraryPaths.CUSTOM_LIBRARY_ROOT);
        }
        return combined;
    }


    @Override
    public Map<Identifier, List<Resource>> listResourceStacks(String path,
                                                              Predicate<Identifier> predicate) {
        return delegate.listResourceStacks(path, predicate);
    }


    @Override
    public Stream<PackResources> listPacks() {
        return delegate.listPacks();
    }
}
