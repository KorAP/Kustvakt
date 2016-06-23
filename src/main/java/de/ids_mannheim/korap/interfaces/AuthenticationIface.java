package de.ids_mannheim.korap.interfaces;

import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.user.TokenContext;
import de.ids_mannheim.korap.user.User;

import java.util.Map;

public interface AuthenticationIface {

    TokenContext getTokenContext(String authToken) throws KustvaktException;


    TokenContext createTokenContext(User user, Map<String, Object> attr)
            throws KustvaktException;


    void removeUserSession (String token) throws KustvaktException;


    TokenContext refresh (TokenContext context) throws KustvaktException;


    String getIdentifier ();

}
