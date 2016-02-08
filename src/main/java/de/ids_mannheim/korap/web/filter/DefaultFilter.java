package de.ids_mannheim.korap.web.filter;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import com.sun.jersey.spi.container.ContainerResponseFilter;
import com.sun.jersey.spi.container.ResourceFilter;
import de.ids_mannheim.korap.config.BeanConfiguration;
import de.ids_mannheim.korap.user.TokenContext;
import de.ids_mannheim.korap.user.User;
import de.ids_mannheim.korap.utils.TimeUtils;
import de.ids_mannheim.korap.web.utils.KorAPContext;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;

/**
 * Created by hanl on 7/15/14.
 */
@Provider
public class DefaultFilter implements ContainerRequestFilter, ResourceFilter {

    @Context
    UriInfo info;

    @Override
    public ContainerRequest filter(ContainerRequest request) {
        String host = request.getHeaderValue(ContainerRequest.HOST);
        String ua = request.getHeaderValue(ContainerRequest.USER_AGENT);
        String authentication = request
                .getHeaderValue(ContainerRequest.AUTHORIZATION);

        // means that this is the public service
        if (authentication == null || authentication.isEmpty()) {
            try {
                request.getUserPrincipal();
            }catch (UnsupportedOperationException e) {
                request.setSecurityContext(
                        new KorAPContext(createShorterToken(host, ua)));
            }
        }
        return request;
    }

    private TokenContext createShorterToken(String host, String agent) {
        User demo = User.UserFactory.getDemoUser();
        TokenContext c = new TokenContext();
        c.setUsername(demo.getUsername());
        c.setHostAddress(host);
        c.setUserAgent(agent);
        c.setExpirationTime(TimeUtils.plusSeconds(
                BeanConfiguration.getBeans().getConfiguration()
                        .getShortTokenTTL()).getMillis());
        return c;
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
