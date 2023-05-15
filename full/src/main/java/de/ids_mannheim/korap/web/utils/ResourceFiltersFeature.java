package de.ids_mannheim.korap.web.utils;

import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.ext.Provider;

/**
 * Registers {@link javax.ws.rs.container.ContainerRequestFilter}
 * and {@link javax.ws.rs.container.ContainerResponseFilter}
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
