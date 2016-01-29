package de.ids_mannheim.korap.security.auth;

import de.ids_mannheim.korap.config.BeanConfiguration;
import de.ids_mannheim.korap.config.Scopes;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.interfaces.AuthenticationIface;
import de.ids_mannheim.korap.interfaces.EncryptionIface;
import de.ids_mannheim.korap.interfaces.db.EntityHandlerIface;
import de.ids_mannheim.korap.user.Attributes;
import de.ids_mannheim.korap.user.KorAPUser;
import de.ids_mannheim.korap.user.TokenContext;
import de.ids_mannheim.korap.user.User;
import de.ids_mannheim.korap.utils.StringUtils;
import org.apache.commons.codec.binary.Base64;
import org.apache.oltu.oauth2.common.utils.OAuthUtils;

import java.util.Map;

/**
 * @author hanl
 * @date 28/04/2015
 */
public class BasicHttpAuth implements AuthenticationIface {

    public static String[] decode(String token) {
        return OAuthUtils.decodeClientAuthenticationHeader(token);

        //        String t = StringUtils.getTokenType(token);
        //        if (t != null && t.toUpperCase()
        //                .equals(Attributes.BASIC_AUTHENTICATION.toUpperCase())) {
        //            token = StringUtils.stripTokenType(token);
        //            String[] sp = new String(Base64.decodeBase64(token)).split(":", 2);
        //            sp[0].replaceAll(" ", "");
        //            sp[1].replaceAll(" ", "");
        //            return sp;
        //        }
        //        return null;
    }

    public static String encode(String user, String pass) {
        String s = user + ":" + pass;
        return Attributes.BASIC_AUTHENTICATION + " " + new String(
                Base64.encodeBase64(s.getBytes()));
    }

    @Override
    public TokenContext getUserStatus(String authToken)
            throws KustvaktException {
        EncryptionIface crypto = BeanConfiguration.getBeans().getEncryption();
        EntityHandlerIface dao = BeanConfiguration.getBeans()
                .getUserDBHandler();
        TokenContext c = new TokenContext();
        String[] values = decode(authToken);
        if (values != null) {
            User user = dao.getAccount(values[0]);
            if (user instanceof KorAPUser) {
                boolean check = crypto
                        .checkHash(values[1], ((KorAPUser) user).getPassword());
                if (!check)
                    return c;
            }
            c.setUsername(values[0]);
            c.setTokenType(Attributes.BASIC_AUTHENTICATION);
            // todo: for production mode, set true
            c.setSecureRequired(false);
            c.setToken(StringUtils.stripTokenType(authToken));
            //            fixme: you can make queries, but user sensitive data is off limits?!
            c.addContextParameter(Attributes.SCOPES,
                    Scopes.Scope.search.toString());
        }
        return c;
    }

    // not supported!
    @Override
    public TokenContext createUserSession(User user, Map<String, Object> attr)
            throws KustvaktException {
        return null;
    }

    @Override
    public void removeUserSession(String token) throws KustvaktException {
        throw new KustvaktException(StatusCodes.NOT_SUPPORTED);
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
