package de.ids_mannheim.korap.web;

import java.util.EnumSet;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import de.ids_mannheim.korap.constant.AuthenticationScheme;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.interfaces.db.AuditingIface;

/** KustvaktResponseHandler includes exceptions regarding authorization. 
 * 
 * @author margaretha
 *
 */
public class KustvaktResponseHandler extends CoreResponseHandler {

    public KustvaktResponseHandler (AuditingIface iface) {
        super(iface);
    }

    @Override
    public WebApplicationException throwit (KustvaktException e) {
        Response r;

        if (e.getStatusCode() == StatusCodes.USER_REAUTHENTICATION_REQUIRED
                || e.getStatusCode() == StatusCodes.AUTHORIZATION_FAILED
                || e.getStatusCode() >= StatusCodes.AUTHENTICATION_FAILED) {
            String notification = buildNotification(e.getStatusCode(),
                    e.getMessage(), e.getEntity());
            r = createUnauthenticatedResponse(notification);
        }
        else if (e.hasNotification()) {
            r = Response.status(getStatus(e.getStatusCode()))
                    .entity(e.getNotification()).build();
        }
        else {
            String notification = buildNotification(e.getStatusCode(),
                    e.getMessage(), e.getEntity());
            r = Response.status(getStatus(e.getStatusCode()))
                    .entity(notification).build();
        }
        return new WebApplicationException(r);
    }

    public Response createUnauthenticatedResponse (String notification) {
        ResponseBuilder builder = Response.status(Response.Status.UNAUTHORIZED);

        for (AuthenticationScheme s : EnumSet
                .allOf(AuthenticationScheme.class)) {
            builder = builder.header(HttpHeaders.WWW_AUTHENTICATE,
                    s.displayName() + " realm=\"Kustvakt\"");
        }

        return builder.entity(notification).build();
    }
}
