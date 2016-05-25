package de.ids_mannheim.korap.web.filter;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import com.sun.jersey.spi.container.ContainerResponseFilter;
import com.sun.jersey.spi.container.ResourceFilter;
import de.ids_mannheim.korap.config.BeansFactory;
import de.ids_mannheim.korap.user.TokenContext;
import de.ids_mannheim.korap.user.User;
import de.ids_mannheim.korap.utils.NamingUtils;
import de.ids_mannheim.korap.utils.StringUtils;
import de.ids_mannheim.korap.web.utils.KorAPContext;
import de.ids_mannheim.korap.web.utils.KustvaktResponseHandler;

import javax.ws.rs.ext.Provider;

/**
 * @author hanl
 * @date 17/06/2014
 */
@Provider
public class AdminFilter implements ContainerRequestFilter, ResourceFilter {

    // check over ssl!
    @Override
    public ContainerRequest filter (ContainerRequest cr) {
        // todo:
        String host = cr.getHeaderValue(ContainerRequest.HOST);
        String ua = cr.getHeaderValue(ContainerRequest.USER_AGENT);

        String authentication = cr
                .getHeaderValue(ContainerRequest.AUTHORIZATION);

        if (authentication != null
                && authentication.endsWith(BeansFactory.getKustvaktContext()
                        .getConfiguration().getAdminToken())) {
            TokenContext c = new TokenContext();
            c.setUsername(User.ADMINISTRATOR_NAME);
            c.setTokenType(NamingUtils.getTokenType(authentication));
            c.setToken(NamingUtils.stripTokenType(authentication));
            cr.setSecurityContext(new KorAPContext(c));
        }
        else
            throw KustvaktResponseHandler.throwAuthenticationException();

        return cr;
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
