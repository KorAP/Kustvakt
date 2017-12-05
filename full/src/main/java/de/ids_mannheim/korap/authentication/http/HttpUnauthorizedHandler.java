package de.ids_mannheim.korap.authentication.http;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.ids_mannheim.korap.config.FullConfiguration;

/** Implementation of HTTP authentication scheme (see RFC 7253 and 7617)
 *  for server creating responses with status 401 Unauthorized and 
 *  WWW-Authenticate header to unauthenticated requests.
 *    
 * @author margaretha
 *
 */
@Component
public class HttpUnauthorizedHandler {
    @Autowired
    private FullConfiguration config;

    public Response createUnauthenticatedResponse (String notification) {
        return Response.status(Response.Status.UNAUTHORIZED)
                .header(HttpHeaders.WWW_AUTHENTICATE,
                        config.getAuthenticationScheme()
                                + " realm=\"Kustvakt\"")
                .entity(notification)
                .build();
    }
}
