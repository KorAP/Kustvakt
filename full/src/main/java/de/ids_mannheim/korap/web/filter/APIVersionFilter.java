package de.ids_mannheim.korap.web.filter;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.ids_mannheim.korap.config.KustvaktConfiguration;
import jakarta.annotation.Priority;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.PathSegment;

/**
 * Checks API version in URL path.
 * 
 * @author margaretha
 *
 */
@Component
@Priority(Integer.MIN_VALUE)
public class APIVersionFilter
        implements ContainerRequestFilter {

    @Autowired
    private KustvaktConfiguration config;

    public void filter (ContainerRequestContext request) {
        List<PathSegment> pathSegments = request.getUriInfo().getPathSegments();
        String version = pathSegments.get(0).getPath();

        if (!config.getSupportedVersions().contains(version)) {
            throw new NotFoundException();
            // throw kustvaktResponseHandler.throwit(
            // new
            // KustvaktException(StatusCodes.UNSUPPORTED_API_VERSION,
            // "API " + version + " is unsupported.", version));
        }
    }

}
