package de.ids_mannheim.korap.web.filter;

import javax.ws.rs.ext.Provider;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import com.sun.jersey.spi.container.ContainerResponseFilter;
import com.sun.jersey.spi.container.ResourceFilter;

import de.ids_mannheim.korap.authentication.http.AuthorizationData;
import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.config.TokenType;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.interfaces.AuthenticationManagerIface;
import de.ids_mannheim.korap.user.TokenContext;
import de.ids_mannheim.korap.web.FullResponseHandler;
import de.ids_mannheim.korap.web.utils.KustvaktContext;

/**
 * @author hanl, margaretha
 * @date 28/01/2014
 * @last update 7/12/2017
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
    private FullResponseHandler kustvaktResponseHandler;

    @Override
    public ContainerRequest filter (ContainerRequest request) {
        String host = request.getHeaderValue(ContainerRequest.HOST);
        String ua = request.getHeaderValue(ContainerRequest.USER_AGENT);

        String authorization =
                request.getHeaderValue(ContainerRequest.AUTHORIZATION);

        if (authorization != null && !authorization.isEmpty()) {
            TokenContext context = null;
            AuthorizationData authData;
            try {
                authData = authorizationHandler
                        .parseAuthorizationHeaderValue(authorization);
                switch (authData.getAuthenticationScheme()) {
                    case BASIC:
                        context = userController.getTokenStatus(TokenType.BASIC,
                                authData.getToken(), host, ua);
                        break;
                    case SESSION:
                        context = userController.getTokenStatus(TokenType.SESSION,
                                authData.getToken(), host, ua);
                        break;
                    // EM: bearer or api
                    default:
                        context = userController.getTokenStatus(TokenType.API,
                                authData.getToken(), host, ua);
                        break;
                }
                checkContext(context, request);
                request.setSecurityContext(new KustvaktContext(context));
            }
            catch (KustvaktException e) {
                throw kustvaktResponseHandler.throwit(e);
            }
        }
        return request;
    }


    private void checkContext (TokenContext context, ContainerRequest request)
            throws KustvaktException {
        if (context == null) {
            throw new KustvaktException(StatusCodes.AUTHENTICATION_FAILED,
                    "Context is null.");
        }
        else if (!context.isValid()) {
            throw new KustvaktException(StatusCodes.AUTHENTICATION_FAILED,
                    "Context is not valid: "
                            + "missing username, password or authentication scheme.");
        }
        else if (context.isSecureRequired() && !request.isSecure()) {
            throw new KustvaktException(StatusCodes.AUTHENTICATION_FAILED,
                    "Request is not secure.");
        }
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
