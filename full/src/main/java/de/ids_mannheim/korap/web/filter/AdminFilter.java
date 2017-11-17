package de.ids_mannheim.korap.web.filter;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.ext.Provider;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ctc.wstx.util.StringUtil;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import com.sun.jersey.spi.container.ContainerResponseFilter;
import com.sun.jersey.spi.container.ResourceFilter;

import de.ids_mannheim.korap.authentication.framework.AuthorizationData;
import de.ids_mannheim.korap.authentication.framework.HttpAuthorizationHandler;
import de.ids_mannheim.korap.authentication.framework.TransferEncoding;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.AuthenticationType;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.interfaces.AuthenticationManagerIface;
import de.ids_mannheim.korap.user.TokenContext;
import de.ids_mannheim.korap.user.User;
import de.ids_mannheim.korap.utils.StringUtils;
import de.ids_mannheim.korap.web.utils.KustvaktContext;
import de.ids_mannheim.korap.web.utils.KustvaktResponseHandler;

/**
 * @author hanl, margaretha
 * @date 04/2017
 */
@Component
@Provider
public class AdminFilter implements ContainerRequestFilter, ResourceFilter {

    @Autowired
    private AuthenticationManagerIface authManager;

    @Autowired
    private KustvaktResponseHandler kustvaktResponseHandler;
    
    @Autowired
    private TransferEncoding transferEncoding;
    
    @Autowired
    private HttpAuthorizationHandler authorizationHandler;
    
    @Override
    public ContainerRequest filter (ContainerRequest cr) {
        String authorization =
                cr.getHeaderValue(ContainerRequest.AUTHORIZATION);
        
        AuthorizationData data;
        try {
            data = authorizationHandler.parseAuthorizationHeader(authorization);
            data = authorizationHandler.parseToken(data);
        }
        catch (KustvaktException e) {
            String authType = StringUtils.stripTokenType(authorization);
            throw kustvaktResponseHandler.throwAuthenticationException(e, authType);
        }
        
        String host = cr.getHeaderValue(ContainerRequest.HOST);
        String agent = cr.getHeaderValue(ContainerRequest.USER_AGENT);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(Attributes.HOST, host);
        attributes.put(Attributes.USER_AGENT, agent);
        try {
            // EM: fix me: AuthenticationType based on header value
            User user = authManager.authenticate(data.getAuthenticationType(),
                    data.getUsername(), data.getPassword(), attributes);
            if (!user.isAdmin()) {
                throw kustvaktResponseHandler.throwAuthenticationException(
                        "Admin authentication failed.", data.getAuthenticationType());
            }
            Map<String, Object> properties = cr.getProperties();
            properties.put("user", user);
        }
        catch (KustvaktException e) {
            throw kustvaktResponseHandler.throwAuthenticationException(e, data.getAuthenticationType());
        }

        TokenContext c = new TokenContext();
        c.setUsername(data.getUsername());
        c.setAuthenticationType(data.getAuthenticationType());
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
