package de.ids_mannheim.korap.web.filter;

import java.security.Principal;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import com.sun.jersey.spi.container.ContainerResponseFilter;
import com.sun.jersey.spi.container.ResourceFilter;

import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.constant.TokenType;
import de.ids_mannheim.korap.security.context.KustvaktContext;
import de.ids_mannheim.korap.security.context.TokenContext;
import de.ids_mannheim.korap.user.User;
import de.ids_mannheim.korap.utils.TimeUtils;

/**
 * Created by hanl on 7/15/14.
 */
@Provider
@Component
public class DemoUserFilter implements ContainerRequestFilter, ResourceFilter {

    @Context
    UriInfo info;
    @Autowired
    private KustvaktConfiguration config;


    @Override
    public ContainerRequest filter (ContainerRequest request) {
        String host = request.getHeaderValue(ContainerRequest.HOST);
        String ua = request.getHeaderValue(ContainerRequest.USER_AGENT);
        String authentication = request
                .getHeaderValue(ContainerRequest.AUTHORIZATION);

        // means that this is the public service
        if (authentication == null || authentication.isEmpty()) {
            Principal pr = null;
            try {
                pr = request.getUserPrincipal();
            }
            catch (UnsupportedOperationException e) {
                // do nothing
            }
            if (pr == null)
                request.setSecurityContext(new KustvaktContext(
                        createShorterToken(host, ua)));

        }
        return request;
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


    @Override
    public ContainerRequestFilter getRequestFilter () {
        return this;
    }


    @Override
    public ContainerResponseFilter getResponseFilter () {
        return null;
    }
}
