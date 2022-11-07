package de.ids_mannheim.korap.web.filter;

import java.security.Principal;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.glassfish.jersey.server.ContainerRequest;

import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.constant.TokenType;
import de.ids_mannheim.korap.security.context.KustvaktContext;
import de.ids_mannheim.korap.security.context.TokenContext;
import de.ids_mannheim.korap.user.User;
import de.ids_mannheim.korap.utils.TimeUtils;

/**
 * Created by hanl on 7/15/14.
 */
@Component
@Priority(Priorities.AUTHENTICATION)
public class DemoUserFilter implements ContainerRequestFilter {

    @Context
    UriInfo info;
    @Autowired
    private KustvaktConfiguration config;


    @Override
    public void filter (ContainerRequestContext request) {
        String host = request.getHeaderString(ContainerRequest.HOST);
        String ua = request.getHeaderString(ContainerRequest.USER_AGENT);
        String authentication = request
                .getHeaderString(ContainerRequest.AUTHORIZATION);

        // means that this is the public service
        if (authentication == null || authentication.isEmpty()) {
            Principal pr = null;
            SecurityContext securityContext = request.getSecurityContext();
            if (securityContext != null) {
                pr = securityContext.getUserPrincipal();
            }
            if (pr == null)
                request.setSecurityContext(new KustvaktContext(
                        createShorterToken(host, ua)));
        }
    }


    private TokenContext createShorterToken (String host, String agent) {
        User demo = User.UserFactory.getDemoUser();
        TokenContext c = new TokenContext();
        c.setUsername(demo.getUsername());
        c.setHostAddress(host);
        c.setUserAgent(agent);
        c.setExpirationTime(TimeUtils.plusSeconds(
                config
                        .getShortTokenTTL()).getMillis());
        c.setTokenType(TokenType.BASIC);
        return c;
    }
}
