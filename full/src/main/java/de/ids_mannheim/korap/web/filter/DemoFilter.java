package de.ids_mannheim.korap.web.filter;

import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.ext.Provider;

import org.springframework.beans.factory.annotation.Autowired;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import com.sun.jersey.spi.container.ContainerResponseFilter;
import com.sun.jersey.spi.container.ResourceFilter;

import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.config.TokenType;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.user.TokenContext;
import de.ids_mannheim.korap.web.utils.KustvaktContext;

/**
 * @author hanl
 * @date 08/02/2016
 */
@Provider
public class DemoFilter implements ContainerRequestFilter, ResourceFilter {

    @Autowired
    private HttpAuthorizationHandler handler;
    
    @Override
    public ContainerRequest filter (ContainerRequest request) {
        String authentication = request
                .getHeaderValue(ContainerRequest.AUTHORIZATION);
        if (authentication == null || authentication.isEmpty()) {
            try {
                request.getUserPrincipal();
            }
            catch (UnsupportedOperationException e) {
                request.setSecurityContext(createContext());
            }
        }
        return request;
    }


    private SecurityContext createContext () {
        TokenContext context = new TokenContext();
        String token = null;
        try {
            token = handler.createBasicAuthorizationHeaderValue("demo", "demo2015");
        }
        catch (KustvaktException e) {
            e.printStackTrace();
        }
        context.setToken(token);
        context.setTokenType(TokenType.BASIC);
        context.setUsername("demo");
        return new KustvaktContext(context);
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
