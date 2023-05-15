package de.ids_mannheim.korap.authentication;

import java.util.Map;

import de.ids_mannheim.korap.constant.TokenType;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.security.context.TokenContext;
import de.ids_mannheim.korap.user.User;

public interface AuthenticationIface {

    public TokenContext getTokenContext(String authToken) throws KustvaktException;


    public TokenContext createTokenContext(User user, Map<String, Object> attr)
            throws KustvaktException;


    void removeUserSession (String token) throws KustvaktException;


    public TokenContext refresh (TokenContext context) throws KustvaktException;


    public TokenType getTokenType ();

}
