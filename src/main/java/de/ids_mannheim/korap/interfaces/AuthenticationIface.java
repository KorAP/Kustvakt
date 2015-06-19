package de.ids_mannheim.korap.interfaces;

import de.ids_mannheim.korap.exceptions.KorAPException;
import de.ids_mannheim.korap.user.TokenContext;
import de.ids_mannheim.korap.user.User;

import java.util.Map;

public interface AuthenticationIface {

    public TokenContext getUserStatus(String authToken) throws KorAPException;

    public TokenContext createUserSession(User user, Map<String, Object> attr)
            throws KorAPException;

    public void removeUserSession(String token) throws KorAPException;

    public TokenContext refresh(TokenContext context) throws KorAPException;

    public String getIdentifier();

}
