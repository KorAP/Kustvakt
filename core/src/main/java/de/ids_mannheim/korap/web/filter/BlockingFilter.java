package de.ids_mannheim.korap.web.filter;

import javax.ws.rs.ext.Provider;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;

import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.security.context.TokenContext;
import de.ids_mannheim.korap.web.KustvaktResponseHandler;

/**
 * @author hanl
 * @date 11/12/2014
 *       <p/>
 *       endpoint filter to block access to an endpoint, in case no
 *       anonymous access should be allowed!
 */
@Component
@Provider
public class BlockingFilter implements ContainerRequestFilter {

    @Autowired
    private KustvaktResponseHandler kustvaktResponseHandler;

    @Override
    public void filter (ContainerRequestContext request) {
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
                    "Unauthorized operation for user: guest", "guest"));
        }
    }
}
