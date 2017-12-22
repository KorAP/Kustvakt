package de.ids_mannheim.korap.web.filter;

import javax.ws.rs.ext.Provider;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import com.sun.jersey.spi.container.ContainerResponseFilter;
import com.sun.jersey.spi.container.ResourceFilter;

import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.user.TokenContext;
import de.ids_mannheim.korap.web.FullResponseHandler;

/**
 * @author hanl
 * @date 11/12/2014
 *       <p/>
 *       endpoint filter to block access to an endpoint, in case no
 *       anonymous access should be allowed!
 */
@Component
@Provider
public class BlockingFilter implements ContainerRequestFilter, ResourceFilter {

    @Autowired
    private FullResponseHandler kustvaktResponseHandler;

    @Override
    public ContainerRequest filter (ContainerRequest request) {
        TokenContext context;

        try {
            context = (TokenContext) request.getUserPrincipal();
        }
        catch (UnsupportedOperationException e) {
            throw kustvaktResponseHandler.throwit(new KustvaktException(
                    StatusCodes.UNSUPPORTED_OPERATION, e.getMessage(), e));
        }

        if (context == null || context.isDemo()) {
            throw kustvaktResponseHandler.throwit(new KustvaktException(
                    StatusCodes.AUTHORIZATION_FAILED,
                    "Operation is not permitted for user: guest", "guest"));
        }


        return request;
    }


    @Override
    public ContainerRequestFilter getRequestFilter () {
        return this;
    }


    @Override
    public ContainerResponseFilter getResponseFilter () {
        return null;
    }
}
