package de.ids_mannheim.korap.security.auth;

import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;

import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.BeansFactory;
import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.config.Scopes;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.interfaces.AuthenticationIface;
import de.ids_mannheim.korap.interfaces.EncryptionIface;
import de.ids_mannheim.korap.interfaces.db.EntityHandlerIface;
import de.ids_mannheim.korap.user.KorAPUser;
import de.ids_mannheim.korap.user.TokenContext;
import de.ids_mannheim.korap.user.User;
import de.ids_mannheim.korap.utils.StringUtils;
import de.ids_mannheim.korap.utils.TimeUtils;

/**
 * @author hanl
 * @date 28/04/2015
 */
// todo: bean injection!
public class BasicHttpAuth implements AuthenticationIface {

    @Autowired
    private KustvaktConfiguration config;
    @Autowired
    private EncryptionIface crypto;
    @Autowired
    private EntityHandlerIface dao;
    
    public BasicHttpAuth() {

    }

    public BasicHttpAuth(KustvaktConfiguration config) {
        this.config = config;
    }


    public static String[] decode (String token) {
        //return OAuthUtils.decodeClientAuthenticationHeader(token);
        String[] tokens = token.split(" ");
        String encodedCred = null;
        if (!token.equals(tokens[0])) {
            if (tokens[0] != null && !tokens[0].isEmpty()) {
                if (!tokens[0].toLowerCase().equalsIgnoreCase("basic")) {
                    return null;
                }
                encodedCred = tokens[1];
            }
        } else {
            encodedCred = tokens[0];
        }
            if(encodedCred != null && !"".equals(encodedCred)) {
                String decodedCreds = new String(Base64.decodeBase64(encodedCred));
                if(decodedCreds.contains(":") && decodedCreds.split(":").length == 2) {
                    String[] creds = decodedCreds.split(":");
                    if ((creds[0] != null && !creds[0].isEmpty()) && (creds[1] != null && !creds[1].isEmpty()))
                        return decodedCreds.split(":");
                }
            }
        return null;
    }


    public static String encode (String user, String pass) {
        String s = user + ":" + pass;
        return Attributes.BASIC_AUTHENTICATION + " "
                + new String(Base64.encodeBase64(s.getBytes()));
    }


    @Override
    public TokenContext getTokenContext(String authToken)
            throws KustvaktException {
        //fixme: handle via constructor
        String[] values = decode(authToken);
        if (values != null) {
            TokenContext c = new TokenContext();
            User user = dao.getAccount(values[0]);
            if (user instanceof KorAPUser && ((KorAPUser) user).getPassword() != null) {
                boolean check = crypto.checkHash(values[1],
                        ((KorAPUser) user).getPassword());

                if (!check)
                    return null;
            }
            c.setUsername(values[0]);
            c.setExpirationTime(TimeUtils.plusSeconds(this.config.getTokenTTL()).getMillis());
            c.setTokenType(Attributes.BASIC_AUTHENTICATION);
            // todo: for production mode, set true
            c.setSecureRequired(false);
            c.setToken(StringUtils.stripTokenType(authToken));
            //            fixme: you can make queries, but user sensitive data is off limits?!
            c.addContextParameter(Attributes.SCOPES,
                    Scopes.Scope.search.toString());
            return c;
        }
        return null;
    }


    // not supported!
    @Override
    public TokenContext createTokenContext(User user, Map<String, Object> attr)
            throws KustvaktException {
        return null;
    }


    @Override
    public void removeUserSession (String token) throws KustvaktException {
        throw new KustvaktException(StatusCodes.NOT_SUPPORTED);
    }


    @Override
    public TokenContext refresh (TokenContext context) throws KustvaktException {
        return null;
    }


    @Override
    public String getIdentifier () {
        return Attributes.BASIC_AUTHENTICATION;
    }
}
