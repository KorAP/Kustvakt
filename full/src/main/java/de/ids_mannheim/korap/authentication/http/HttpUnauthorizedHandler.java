package de.ids_mannheim.korap.authentication.http;

import java.util.EnumSet;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.springframework.stereotype.Component;

import de.ids_mannheim.korap.config.AuthenticationScheme;

/** Implementation of HTTP authentication scheme (see RFC 7253 and 7617)
 *  for server creating responses with status 401 Unauthorized and 
 *  WWW-Authenticate header to unauthenticated requests.
 *    
 * @author margaretha
 *
 */
@Component
public class HttpUnauthorizedHandler {

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
