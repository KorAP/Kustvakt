package de.ids_mannheim.korap.web.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.web.filter.DemoUserFilter;
import jakarta.ws.rs.container.DynamicFeature;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.FeatureContext;
import jakarta.ws.rs.ext.Provider;

/**
 * Registers {@link jakarta.ws.rs.container.ContainerRequestFilter}
 * and {@link jakarta.ws.rs.container.ContainerResponseFilter}
 * classes for a resource method annotated with
 * {@link ResourceFilters}.
 */
@Provider
public class ResourceFiltersFeature implements DynamicFeature {

    @Autowired
    public KustvaktConfiguration config;
    
    @Override
    public void configure (ResourceInfo resourceInfo, FeatureContext context) {
        ResourceFilters filtersAnnotation = resourceInfo.getResourceMethod()
                .getAnnotation(ResourceFilters.class);
        if (filtersAnnotation == null)
            filtersAnnotation = resourceInfo.getResourceClass()
                    .getAnnotation(ResourceFilters.class);
        
        if (filtersAnnotation != null) {
            Class<?>[] filterArray = filtersAnnotation.value();
            List<Class<?>> filterList = new ArrayList<>(Arrays.asList(filterArray));
            if (config.isLoginRequired()) {
                filterList.remove(DemoUserFilter.class);
            }
            for (Class<?> filter : filterList) {
                context.register(filter);
            }
        }
    }
}
