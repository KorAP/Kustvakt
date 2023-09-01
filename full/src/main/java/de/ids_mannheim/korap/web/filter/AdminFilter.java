package de.ids_mannheim.korap.web.filter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.ids_mannheim.korap.dao.AdminDao;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.security.context.TokenContext;
import de.ids_mannheim.korap.utils.JerseyUtils;
import de.ids_mannheim.korap.web.KustvaktResponseHandler;
import jakarta.annotation.Priority;
import jakarta.servlet.ServletContext;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.SecurityContext;

/**
 * Verifies admin credentials or token before allowing access to
 * administrative services
 * 
 * @author hanl, margaretha
 * 
 * @see {@link AuthenticationFilter}
 */
@Component
@Priority(Priorities.AUTHENTICATION)
public class AdminFilter extends AuthenticationFilter {

    private @Context ServletContext servletContext;
    @Autowired
    private AdminDao adminDao;

    @Autowired
    private KustvaktResponseHandler kustvaktResponseHandler;

    @Override
    public void filter (ContainerRequestContext context) {
        super.filter(context);
        String username = "guest";
        String adminToken = JerseyUtils.getFormParameters(context).asMap()
                .getFirst("token");
        if (!checkAdminToken(adminToken)) {
            SecurityContext securityContext = context.getSecurityContext();
            TokenContext tokenContext = (TokenContext) securityContext
                    .getUserPrincipal();
            checkAdminCredentials(tokenContext, username);
        }
    }


    private boolean checkAdminToken (String adminToken) {
        if (adminToken != null && !adminToken.isEmpty()) {
            if (adminToken
                    .equals(servletContext.getInitParameter("adminToken"))) {
                return true;
            }
        }
        return false;
    }


    private void checkAdminCredentials (TokenContext tokenContext,
            String username) {
        if (tokenContext != null) {
            username = tokenContext.getUsername();
            if (adminDao.isAdmin(username)) {
                return;
            }
        }

        throw kustvaktResponseHandler.throwit(new KustvaktException(
                StatusCodes.AUTHORIZATION_FAILED,
                "Unauthorized operation for user: " + username, username));
    }
}
