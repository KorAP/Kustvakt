package de.ids_mannheim.korap.web.filter;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.ext.Provider;

import org.springframework.beans.factory.annotation.Autowired;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import com.sun.jersey.spi.container.ContainerResponseFilter;
import com.sun.jersey.spi.container.ResourceFilter;

import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.BeansFactory;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.interfaces.AuthenticationManagerIface;
import de.ids_mannheim.korap.security.auth.BasicHttpAuth;
import de.ids_mannheim.korap.user.TokenContext;
import de.ids_mannheim.korap.user.User;
import de.ids_mannheim.korap.utils.StringUtils;
import de.ids_mannheim.korap.web.utils.KustvaktContext;
import de.ids_mannheim.korap.web.utils.KustvaktResponseHandler;

/**
 * @author hanl, margaretha
 * @date 04/2017
 */
@Provider
public class AdminFilter implements ContainerRequestFilter, ResourceFilter {

    @Autowired
	private AuthenticationManagerIface authManager;

	@Override
	public ContainerRequest filter(ContainerRequest cr) {
		String authentication = cr.getHeaderValue(ContainerRequest.AUTHORIZATION);
		if (authentication == null) {
			throw KustvaktResponseHandler.throwAuthenticationException("The authorization header value is missing.");
		}

		// decode password
		String authenticationType = StringUtils.getTokenType(authentication);
		String authenticationCode = StringUtils.stripTokenType(authentication);
		String username = null, token = null;
		int tokenType = 0;
		
		if (authenticationType.equals(Attributes.BASIC_AUTHENTICATION)) {
			String[] authContent = BasicHttpAuth.decode(authenticationCode);
			username = authContent[0];
			token = authContent[1];
		}
		
		String host = cr.getHeaderValue(ContainerRequest.HOST);
		String agent = cr.getHeaderValue(ContainerRequest.USER_AGENT);
		Map<String, Object> attributes = new HashMap<>();
		attributes.put(Attributes.HOST, host);
		attributes.put(Attributes.USER_AGENT, agent);
		try {
			User user = authManager.authenticate(tokenType, username, token, attributes);
			if (!user.isAdmin()){
				throw KustvaktResponseHandler.throwAuthenticationException("Admin authentication failed.");
			}
			Map<String, Object> properties = cr.getProperties();
			properties.put("user", user);
		} catch (KustvaktException e) {
			throw KustvaktResponseHandler.throwAuthenticationException("User authentication failed.");
		}

		TokenContext c = new TokenContext();
		c.setUsername(username);
		c.setTokenType(authenticationType);
		c.setToken(token);
		c.setHostAddress(host);
		c.setUserAgent(agent);
		cr.setSecurityContext(new KustvaktContext(c));

		return cr;
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
