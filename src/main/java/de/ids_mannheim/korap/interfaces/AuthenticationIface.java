package de.ids_mannheim.korap.interfaces;

import de.ids_mannheim.korap.exceptions.KorAPException;
import de.ids_mannheim.korap.ext.security.AccessLevel;
import de.ids_mannheim.korap.user.TokenContext;
import de.ids_mannheim.korap.user.User;

import java.util.Map;

public abstract class AuthenticationIface {

    public abstract TokenContext getUserStatus(String authToken)
            throws KorAPException;

    public abstract TokenContext createUserSession(User user,
            Map<String, Object> attr) throws KorAPException;

    public abstract void removeUserSession(String token) throws KorAPException;

    public abstract AccessLevel[] retrieveLevelAccess(String authToken)
            throws KorAPException;

    public abstract String getIdentifier();

}
