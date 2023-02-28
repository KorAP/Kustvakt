package de.ids_mannheim.korap.web.utils;

import java.util.List;

import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.ext.Provider;

import org.glassfish.jersey.model.internal.CommonConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import de.ids_mannheim.korap.web.filter.APIVersionFilter;
import edu.emory.mathcs.backport.java.util.Arrays;

/**
 * Registers {@link javax.ws.rs.container.ContainerRequestFilter}
 * and {@link javax.ws.rs.container.ContainerResponseFilter}
 * classes for a resource method annotated with {@link ResourceFilters}.
 */
@Provider
@Component
public class SearchResourceFiltersFeature implements DynamicFeature {

    @Value("${search.resource.filters:AuthenticationFilter,DemoUserFilter}")
    private String[] resourceFilters;
    
    @Override
    public void configure (ResourceInfo resourceInfo, FeatureContext context) {
        SearchResourceFilters filters = resourceInfo.getResourceMethod()
                .getAnnotation(SearchResourceFilters.class);
        if (filters != null) {
            CommonConfig con = (CommonConfig) context.getConfiguration();
            con.getComponentBag().clear();
        }        
        else {
            filters = resourceInfo.getResourceClass()
            .getAnnotation(SearchResourceFilters.class);
        }
        
        if (filters != null) {
            List<?> list = Arrays.asList(resourceFilters);
            if (!list.contains("APIVersionFilter")) {
                context.register(APIVersionFilter.class);
            }
            
             for(String c : resourceFilters) {
                    try {
                        context.register(Class.forName("de.ids_mannheim.korap.web.filter." + c));
                    }
                    catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
            }
        }
    }
}
