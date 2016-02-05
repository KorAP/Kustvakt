package de.ids_mannheim.korap.web.filter;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import com.sun.jersey.spi.container.ContainerResponseFilter;
import com.sun.jersey.spi.container.ResourceFilter;
import de.ids_mannheim.korap.config.BeanConfiguration;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.interfaces.AuthenticationManagerIface;
import de.ids_mannheim.korap.user.TokenContext;
import de.ids_mannheim.korap.web.utils.KorAPContext;
import de.ids_mannheim.korap.web.utils.KustvaktResponseHandler;

import javax.ws.rs.ext.Provider;

/**
 * @author hanl
 * @date 28/01/2014
 */
@Provider
public class AuthFilter implements ContainerRequestFilter, ResourceFilter {

    private AuthenticationManagerIface userController;

    public AuthFilter() {
        this.userController = BeanConfiguration.getBeans()
                .getAuthenticationManager();
    }

    @Override
    public ContainerRequest filter(ContainerRequest request) {
        String host = request.getHeaderValue(ContainerRequest.HOST);
        String ua = request.getHeaderValue(ContainerRequest.USER_AGENT);

        String authentication = request
                .getHeaderValue(ContainerRequest.AUTHORIZATION);
        if (authentication != null && !authentication.isEmpty()) {
            TokenContext context;
            try {
                context = userController
                        .getTokenStatus(authentication, host, ua);
            }catch (KustvaktException e) {
                throw KustvaktResponseHandler.throwAuthenticationException();
            }
            // fixme: give reason why access is not granted?
            if (context != null && context.isValid() && (
                    (context.isSecureRequired() && request.isSecure())
                            | !context.isSecureRequired()))
                request.setSecurityContext(new KorAPContext(context));
            else
                throw KustvaktResponseHandler.throwAuthenticationException();
        }
        return request;
    }

    @Override
    public ContainerRequestFilter getRequestFilter() {
        return this;
    }

    @Override
    public ContainerResponseFilter getResponseFilter() {
        return null;
    }
}
