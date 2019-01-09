package de.ids_mannheim.korap.web.filter;

import java.util.List;

import javax.ws.rs.core.PathSegment;
import javax.ws.rs.ext.Provider;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sun.jersey.api.NotFoundException;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import com.sun.jersey.spi.container.ContainerResponseFilter;
import com.sun.jersey.spi.container.ResourceFilter;

import de.ids_mannheim.korap.config.KustvaktConfiguration;

/**
 * Checks API version in URL path.
 * 
 * @author margaretha
 *
 */
@Component
@Provider
public class APIVersionFilter
        implements ContainerRequestFilter, ResourceFilter {

    @Autowired
    private KustvaktConfiguration config;

    @Override
    public ContainerRequestFilter getRequestFilter () {
        return this;
    }

    @Override
    public ContainerResponseFilter getResponseFilter () {
        return null;
    }

    @Override
    public ContainerRequest filter (ContainerRequest request) {
        List<PathSegment> pathSegments = request.getPathSegments();
        String version = pathSegments.get(0).getPath();

        if (!config.getSupportedVersions().contains(version)) {
            throw new NotFoundException(request.getRequestUri());
            // throw kustvaktResponseHandler.throwit(
            // new
            // KustvaktException(StatusCodes.UNSUPPORTED_API_VERSION,
            // "API " + version + " is unsupported.", version));
        }
        return request;
    }

}
