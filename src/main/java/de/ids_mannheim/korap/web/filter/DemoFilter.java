package de.ids_mannheim.korap.web.filter;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import com.sun.jersey.spi.container.ContainerResponseFilter;
import com.sun.jersey.spi.container.ResourceFilter;
import de.ids_mannheim.korap.security.auth.BasicHttpAuth;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.user.TokenContext;
import de.ids_mannheim.korap.web.utils.KorAPContext;

import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.ext.Provider;

/**
 * @author hanl
 * @date 08/02/2016
 */
@Provider
public class DemoFilter implements ContainerRequestFilter, ResourceFilter {

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
        String token = BasicHttpAuth.encode("demo", "demo2015");
        context.setToken(token);
        context.setTokenType(Attributes.BASIC_AUTHENTICATION);
        context.setUsername("demo");
        return new KorAPContext(context);
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
