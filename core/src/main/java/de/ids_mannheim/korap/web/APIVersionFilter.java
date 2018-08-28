package de.ids_mannheim.korap.web;

import java.util.List;

import javax.ws.rs.core.PathSegment;
import javax.ws.rs.ext.Provider;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import com.sun.jersey.spi.container.ContainerResponseFilter;
import com.sun.jersey.spi.container.ResourceFilter;

import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;

/** Checks API version in URL path. 
 * 
 * @author margaretha
 *
 */
@Component
@Provider
public class APIVersionFilter implements ContainerRequestFilter, ResourceFilter {

    @Autowired
    private KustvaktConfiguration config;
    @Autowired
    private CoreResponseHandler kustvaktResponseHandler;

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

        if (!config.getVersion().contains(version)) {
            throw kustvaktResponseHandler.throwit(
                    new KustvaktException(StatusCodes.UNSUPPORTED_API_VERSION,
                            "API " + version + " is unsupported.", version));
        }
        return request;
    }

}
