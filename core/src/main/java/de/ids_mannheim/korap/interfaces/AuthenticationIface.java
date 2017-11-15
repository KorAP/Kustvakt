package de.ids_mannheim.korap.interfaces;

import de.ids_mannheim.korap.config.AuthenticationType;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.user.TokenContext;
import de.ids_mannheim.korap.user.User;

import java.util.Map;

public interface AuthenticationIface {

    public TokenContext getTokenContext(String authToken) throws KustvaktException;


    public TokenContext createTokenContext(User user, Map<String, Object> attr)
            throws KustvaktException;


    void removeUserSession (String token) throws KustvaktException;


    public TokenContext refresh (TokenContext context) throws KustvaktException;


    public AuthenticationType getIdentifier ();

}
