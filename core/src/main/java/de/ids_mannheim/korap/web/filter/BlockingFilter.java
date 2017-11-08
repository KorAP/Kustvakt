package de.ids_mannheim.korap.web.filter;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import com.sun.jersey.spi.container.ContainerResponseFilter;
import com.sun.jersey.spi.container.ResourceFilter;
import de.ids_mannheim.korap.user.TokenContext;
import de.ids_mannheim.korap.web.utils.KustvaktResponseHandler;

import javax.ws.rs.ext.Provider;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
    KustvaktResponseHandler kustvaktResponseHandler;
    
    @Override
    public ContainerRequest filter (ContainerRequest request) {
        TokenContext context;
        try {
            context = (TokenContext) request.getUserPrincipal();
        }
        catch (UnsupportedOperationException e) {
            throw kustvaktResponseHandler.throwAuthenticationException("");
        }

        if(context == null || context.isDemo())
            throw kustvaktResponseHandler.throwAuthenticationException("");

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
