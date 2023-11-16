package de.ids_mannheim.korap.web.filter;

import org.glassfish.jersey.server.ContainerRequest;

import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.constant.TokenType;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.security.context.KustvaktContext;
import de.ids_mannheim.korap.security.context.TokenContext;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.SecurityContext;

/**
 * @author hanl
 * @date 08/02/2016
 */
@Priority(Priorities.AUTHENTICATION)
public class DemoFilter implements ContainerRequestFilter {

    @Override
    public void filter (ContainerRequestContext request) {
        String authentication = request
                .getHeaderString(ContainerRequest.AUTHORIZATION);
        if (authentication == null || authentication.isEmpty()) {
            if (request.getSecurityContext() == null) {
                request.setSecurityContext(createContext());
            }
        }
    }

    private SecurityContext createContext () {
        TokenContext context = new TokenContext();
        String token = null;
        try {
            token = HttpAuthorizationHandler
                    .createBasicAuthorizationHeaderValue("demo", "demo2015");
        }
        catch (KustvaktException e) {
            e.printStackTrace();
        }
        context.setToken(token);
        context.setTokenType(TokenType.BASIC);
        context.setUsername("demo");
        return new KustvaktContext(context);
    }
}
