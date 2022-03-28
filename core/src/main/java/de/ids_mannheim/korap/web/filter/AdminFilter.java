package de.ids_mannheim.korap.web.filter;

import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.ext.Provider;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import com.sun.jersey.spi.container.ContainerResponseFilter;

import de.ids_mannheim.korap.dao.AdminDao;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.security.context.TokenContext;
import de.ids_mannheim.korap.web.KustvaktResponseHandler;

/**
 * @author hanl, margaretha
 * 
 * @see {@link AuthenticationFilter}
 */
@Component
@Provider
public class AdminFilter extends AuthenticationFilter {

    private @Context ServletContext servletContext;
    @Autowired
    private AdminDao adminDao;
    @Autowired
    private KustvaktResponseHandler kustvaktResponseHandler;

    @Override
    public ContainerRequest filter (ContainerRequest request) {
        ContainerRequest superRequest = super.filter(request);
        String username = "guest";
        
        // legacy support for kustvakt core
        String adminToken = superRequest.getFormParameters().getFirst("token");
        if (adminToken != null && !adminToken.isEmpty()) {
            // startswith token=
            // adminToken = adminToken.substring(6);
            if (adminToken
                    .equals(servletContext.getInitParameter("adminToken"))) {
                return superRequest;
            }
        }

        SecurityContext securityContext = superRequest.getSecurityContext();
        TokenContext tokenContext = (TokenContext) securityContext
                .getUserPrincipal();
        
        if (tokenContext != null) {
            username = tokenContext.getUsername();
            if (adminDao.isAdmin(username)) {
                return superRequest;
            }
        }
        throw kustvaktResponseHandler.throwit(new KustvaktException(
                StatusCodes.AUTHORIZATION_FAILED,
                "Unauthorized operation for user: " + username, username));
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
