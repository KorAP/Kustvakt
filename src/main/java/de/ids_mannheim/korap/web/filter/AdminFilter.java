package de.ids_mannheim.korap.web.filter;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import com.sun.jersey.spi.container.ContainerResponseFilter;
import com.sun.jersey.spi.container.ResourceFilter;
import de.ids_mannheim.korap.config.AdminSetup;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.BeansFactory;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.interfaces.AuthenticationManagerIface;
import de.ids_mannheim.korap.interfaces.EncryptionIface;
import de.ids_mannheim.korap.security.auth.BasicHttpAuth;
import de.ids_mannheim.korap.security.auth.KustvaktAuthenticationManager;
import de.ids_mannheim.korap.user.TokenContext;
import de.ids_mannheim.korap.user.User;
import de.ids_mannheim.korap.utils.NamingUtils;
import de.ids_mannheim.korap.utils.StringUtils;
import de.ids_mannheim.korap.web.utils.KustvaktContext;
import de.ids_mannheim.korap.web.utils.KustvaktResponseHandler;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.ext.Provider;

/**
 * @author hanl
 * @date 17/06/2014
 */
@Provider
public class AdminFilter implements ContainerRequestFilter, ResourceFilter {

//	private static AuthenticationManagerIface authManager = BeansFactory.getKustvaktContext()
//	        .getAuthenticationManager();
	
    @Override
    public ContainerRequest filter (ContainerRequest cr) {
        // todo:
        String host = cr.getHeaderValue(ContainerRequest.HOST);
        String agent = cr.getHeaderValue(ContainerRequest.USER_AGENT);
        String authentication = cr
                .getHeaderValue(ContainerRequest.AUTHORIZATION);
        
        //decode password
        String authenticationType = StringUtils.getTokenType(authentication);
        String authenticationCode = StringUtils.stripTokenType(authentication);
        String username = null, token=null;
        if (authenticationType.equals("basic")){
        	String[] authContent = BasicHttpAuth.decode(authenticationCode);
        	username = authContent[0];
        	token= authContent[1];
        }
        
//        if (authentication != null
//                && authentication.endsWith(BeansFactory.getKustvaktContext()
//                        .getConfiguration().getAdminToken())) {
        
//        EM: to do ssl
        if (authentication != null && cr.isSecure()) {
//            String token = StringUtils.stripTokenType(authentication);
//            EncryptionIface crypto = BeansFactory.getKustvaktContext()
//                    .getEncryption();
            
            // EM: Another method of authentification using admin token
//            if (crypto.checkHash(token, AdminSetup.getInstance().getHash())) {
                TokenContext c = new TokenContext();
                c.setUsername(username);
                c.setTokenType(authenticationType);
                c.setToken(token);
                c.setHostAddress(host);
                c.setUserAgent(agent);
                cr.setSecurityContext(new KustvaktContext(c));
                
//            }
        }
        else
            throw KustvaktResponseHandler.throwAuthenticationException("Unsecure connection.");
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
