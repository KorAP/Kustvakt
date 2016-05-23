package de.ids_mannheim.korap.web.utils;

import de.ids_mannheim.korap.user.TokenContext;

import javax.ws.rs.core.SecurityContext;
import java.security.Principal;

/**
 * @author hanl
 * @date 13/05/2014
 * 
 *       wrapper for REST security context
 * 
 */
public class KorAPContext implements SecurityContext {

    private TokenContext user;


    public KorAPContext (final TokenContext user) {
        this.user = user;
    }


    @Override
    public Principal getUserPrincipal () {
        return this.user;
    }


    @Override
    public boolean isUserInRole (String role) {
        throw new UnsupportedOperationException();
    }


    @Override
    public boolean isSecure () {
        return false;
    }


    @Override
    public String getAuthenticationScheme () {
        return SecurityContext.BASIC_AUTH;
    }
}
