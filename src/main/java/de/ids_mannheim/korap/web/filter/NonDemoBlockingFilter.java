package de.ids_mannheim.korap.web.filter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.security.context.TokenContext;
import de.ids_mannheim.korap.web.KustvaktResponseHandler;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.SecurityContext;

/**
 * EM: pretty much identical to {@link BlockingFilter}, should be
 * deleted?
 * 
 * @author hanl
 * @date 11/12/2014
 *       <p/>
 *       endpoint filter to block access to an endpoint, in case no
 *       anonymous access should be allowed!
 */
@Component
@Priority(Priorities.AUTHORIZATION)
public class NonDemoBlockingFilter implements ContainerRequestFilter {

    @Autowired
    private KustvaktResponseHandler kustvaktResponseHandler;

    @Override
    public void filter (ContainerRequestContext request) {
        TokenContext context;
        SecurityContext securityContext = request.getSecurityContext();
        if (securityContext != null) {
            context = (TokenContext) securityContext.getUserPrincipal();
        }
        else {
            throw kustvaktResponseHandler.throwit(
                    new KustvaktException(StatusCodes.UNSUPPORTED_OPERATION));
        }

        if (context == null || context.isDemo()) {
            throw kustvaktResponseHandler.throwit(
                    new KustvaktException(StatusCodes.AUTHORIZATION_FAILED,
                            "Operation is not permitted for guest users"));
        }
    }
}
