package de.ids_mannheim.korap.web.filter;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.ext.Provider;

import org.glassfish.jersey.server.ContainerRequest;

import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.constant.TokenType;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.security.context.KustvaktContext;
import de.ids_mannheim.korap.security.context.TokenContext;

/**
 * @author hanl
 * @date 08/02/2016
 */
@Provider
public class DemoFilter implements ContainerRequestFilter {

    @Override
    public void filter (ContainerRequestContext request) {
        String authentication =
                request.getHeaderValue(ContainerRequest.AUTHORIZATION);
        if (authentication == null || authentication.isEmpty()) {
            try {
                request.getUserPrincipal();
            }
            catch (UnsupportedOperationException e) {
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
