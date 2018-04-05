package de.ids_mannheim.korap.web.filter;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.ext.Provider;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sun.jersey.api.client.ClientResponse.Status;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import com.sun.jersey.spi.container.ContainerResponseFilter;
import com.sun.jersey.spi.container.ResourceFilter;

import de.ids_mannheim.korap.authentication.http.AuthorizationData;
import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.authentication.http.TransferEncoding;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.constant.AuthenticationMethod;
import de.ids_mannheim.korap.dao.AdminDao;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.interfaces.AuthenticationManagerIface;
import de.ids_mannheim.korap.security.context.KustvaktContext;
import de.ids_mannheim.korap.security.context.TokenContext;
import de.ids_mannheim.korap.user.User;
import de.ids_mannheim.korap.utils.StringUtils;
import de.ids_mannheim.korap.web.FullResponseHandler;

/**
 * @author hanl, margaretha
 * @date 04/2017
 * 
 * @see AuthenticationFilter
 */
@Deprecated
@Component
@Provider
public class AdminFilter implements ContainerRequestFilter, ResourceFilter {

    @Autowired
    private AdminDao adminDao;
    @Autowired
    private AuthenticationManagerIface authManager;

    @Autowired
    private FullResponseHandler kustvaktResponseHandler;

    @Autowired
    private HttpAuthorizationHandler authorizationHandler;

    @Override
    public ContainerRequest filter (ContainerRequest cr) {
        String authorization =
                cr.getHeaderValue(ContainerRequest.AUTHORIZATION);

        AuthorizationData data;
        try {
            data = authorizationHandler.parseAuthorizationHeaderValue(authorization);
            data = authorizationHandler.parseBasicToken(data);
        }
        catch (KustvaktException e) {
            throw kustvaktResponseHandler.throwit(e);
        }

        String host = cr.getHeaderValue(ContainerRequest.HOST);
        String agent = cr.getHeaderValue(ContainerRequest.USER_AGENT);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(Attributes.HOST, host);
        attributes.put(Attributes.USER_AGENT, agent);
        try {
            // EM: fix me: AuthenticationType based on header value
            User user = authManager.authenticate(AuthenticationMethod.LDAP,
                    data.getUsername(), data.getPassword(), attributes);
            if (!adminDao.isAdmin(user.getUsername())) {
                throw new KustvaktException(StatusCodes.AUTHENTICATION_FAILED,
                        "Admin authentication failed.");
            }
            Map<String, Object> properties = cr.getProperties();
            properties.put("user", user);
        }
        catch (KustvaktException e) {
            throw kustvaktResponseHandler.throwit(e);
        }

        TokenContext c = new TokenContext();
        c.setUsername(data.getUsername());
        // EM: needs token type custom param in the authorization header
//        c.setTokenType();
        // MH: c.setTokenType(StringUtils.getTokenType(authentication));
        // EM: is this secure? Is token context not sent outside Kustvakt?
        c.setToken(data.getToken());
        c.setHostAddress(host);
        c.setUserAgent(agent);
        cr.setSecurityContext(new KustvaktContext(c));

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
