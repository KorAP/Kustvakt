package de.ids_mannheim.korap.web.utils;

import jakarta.ws.rs.container.DynamicFeature;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.FeatureContext;
import jakarta.ws.rs.ext.Provider;

/**
 * Registers {@link jakarta.ws.rs.container.ContainerRequestFilter}
 * and {@link jakarta.ws.rs.container.ContainerResponseFilter}
 * classes for a resource method annotated with {@link ResourceFilters}.
 */
@Provider
public class ResourceFiltersFeature implements DynamicFeature {

    @Override
    public void configure (ResourceInfo resourceInfo, FeatureContext context) {
        ResourceFilters filtersAnnotation = resourceInfo.getResourceMethod().getAnnotation(ResourceFilters.class);
        if (filtersAnnotation == null)
            filtersAnnotation = resourceInfo.getResourceClass().getAnnotation(ResourceFilters.class);

        if (filtersAnnotation != null) {
            for (Class<?> filter : filtersAnnotation.value())
                context.register(filter);
        }
    }
}
