package de.ids_mannheim.korap.web.filter;

import javax.ws.rs.ext.Provider;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import com.sun.jersey.spi.container.ContainerResponseFilter;
import com.sun.jersey.spi.container.ResourceFilter;

import de.ids_mannheim.korap.authentication.framework.AuthorizationData;
import de.ids_mannheim.korap.authentication.framework.HttpAuthorizationHandler;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.interfaces.AuthenticationManagerIface;
import de.ids_mannheim.korap.user.TokenContext;
import de.ids_mannheim.korap.utils.StringUtils;
import de.ids_mannheim.korap.web.utils.KustvaktContext;
import de.ids_mannheim.korap.web.utils.KustvaktResponseHandler;

/**
 * @author hanl
 * @date 28/01/2014
 */
@Component
@Provider
public class AuthenticationFilter
        implements ContainerRequestFilter, ResourceFilter {

    @Autowired
    private HttpAuthorizationHandler authorizationHandler;

    @Autowired
    private AuthenticationManagerIface userController;

    @Autowired
    private KustvaktResponseHandler kustvaktResponseHandler;

    //    public AuthFilter () {
    //        this.userController = BeansFactory.getKustvaktContext()
    //                .getAuthenticationManager();
    //    }


    @Override
    public ContainerRequest filter (ContainerRequest request) {
        String host = request.getHeaderValue(ContainerRequest.HOST);
        String ua = request.getHeaderValue(ContainerRequest.USER_AGENT);

        String authorization =
                request.getHeaderValue(ContainerRequest.AUTHORIZATION);


        if (authorization != null && !authorization.isEmpty()) {
            TokenContext context;
            AuthorizationData authData;
            try {
                authData = authorizationHandler
                        .parseAuthorizationHeader(authorization);
                context = userController.getTokenStatus(
                        authData.getAuthenticationType(), authData.getToken(), host,
                        ua);
            }
            catch (KustvaktException e) {
                String authType = StringUtils.stripTokenType(authorization);
                throw kustvaktResponseHandler
                        .throwAuthenticationException(e, authType);
            }
            // fixme: give reason why access is not granted?
            if (context != null && context.isValid()
                    && ((context.isSecureRequired() && request.isSecure())
                            | !context.isSecureRequired()))
                request.setSecurityContext(new KustvaktContext(context));
            else
                throw kustvaktResponseHandler.throwAuthenticationException(
                        new KustvaktException(StatusCodes.UNAUTHORIZED_OPERATION), 
                        authData.getAuthenticationType());
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
