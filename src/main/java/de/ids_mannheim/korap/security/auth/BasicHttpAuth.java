package de.ids_mannheim.korap.security.auth;

import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.interfaces.AuthenticationIface;
import de.ids_mannheim.korap.user.Attributes;
import de.ids_mannheim.korap.user.TokenContext;
import de.ids_mannheim.korap.user.User;
import de.ids_mannheim.korap.utils.StringUtils;
import org.apache.commons.codec.binary.Base64;

import java.util.Map;

/**
 * @author hanl
 * @date 28/04/2015
 */
public class BasicHttpAuth implements AuthenticationIface {

    public static String[] decode(String token) {
        if (StringUtils.getTokenType(token)
                .equals(Attributes.BASIC_AUTHENTICATION)) {
            token = StringUtils.stripTokenType(token);
            String[] sp = new String(Base64.decodeBase64(token)).split(":", 2);
            sp[0].replaceAll(" ", "");
            sp[1].replaceAll(" ", "");
            return sp;
        }
        return null;
    }

    public static String encode(String user, String pass) {
        String s = user + ":" + pass;
        return new String(Base64.encodeBase64(s.getBytes()));
    }

    @Override
    public TokenContext getUserStatus(String authToken) throws
            KustvaktException {
        authToken = StringUtils.stripTokenType(authToken);
        String[] values = decode(authToken);
        if (values != null) {
            TokenContext c = new TokenContext();
            c.setUsername(values[0]);
            c.setTokenType(Attributes.BASIC_AUTHENTICATION);
            c.setSecureRequired(true);
            c.setToken(authToken);
            //            fixme: you can make queries, but user sensitive data is off limits?!
            //            c.addContextParameter(Attributes.SCOPES,
            //                    Scopes.Scope.search.toString());
            return c;
        }
        return null;
    }

    // not supported!
    @Override
    public TokenContext createUserSession(User user, Map<String, Object> attr)
            throws KustvaktException {
        return null;
    }

    @Override
    public void removeUserSession(String token) throws KustvaktException {
    }

    @Override
    public TokenContext refresh(TokenContext context) throws KustvaktException {
        return null;
    }


    @Override
    public String getIdentifier() {
        return Attributes.BASIC_AUTHENTICATION;
    }
}
