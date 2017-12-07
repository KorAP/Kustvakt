package de.ids_mannheim.korap.authentication;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.authentication.http.TransferEncoding;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.TokenType;
import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.config.Scopes;
import de.ids_mannheim.korap.dao.UserDao;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.interfaces.AuthenticationIface;
import de.ids_mannheim.korap.interfaces.EncryptionIface;
import de.ids_mannheim.korap.user.KorAPUser;
import de.ids_mannheim.korap.user.TokenContext;
import de.ids_mannheim.korap.user.User;
import de.ids_mannheim.korap.utils.StringUtils;
import de.ids_mannheim.korap.utils.TimeUtils;

/** 
 * Implementation of encoding and decoding access token is moved to 
 * {@link TransferEncoding}. Moreover, implementation of HTTP 
 * Authentication framework, i.e. creation of authorization header, 
 * is defined in {@link HttpAuthorizationHandler}. 
 * 
 * Basic authentication is intended to be used with a database. It is 
 * currently only used for testing using a dummy DAO (@see {@link UserDao}) 
 * without passwords.
 *   
 * 
 * @author margaretha
 * @date 15/11/2017
 * 
 * @author hanl
 * @date 28/04/2015
 */
public class BasicAuthentication implements AuthenticationIface {

    @Autowired
    private TransferEncoding transferEncoding;
    @Autowired
    private KustvaktConfiguration config;
    @Autowired
    private EncryptionIface crypto;
    @Autowired
    private UserDao dao;

    public BasicAuthentication (KustvaktConfiguration config) {
        this.config = config;
    }

    @Override
    public TokenContext getTokenContext (String authToken)
            throws KustvaktException {
        String[] values = transferEncoding.decodeBase64(authToken);
        if (values != null) {
            TokenContext c = new TokenContext();
            c.setUsername(values[0]);
            c.setExpirationTime(TimeUtils.plusSeconds(this.config.getTokenTTL())
                    .getMillis());
            c.setTokenType(getTokenType());
            // todo: for production mode, set true
            c.setSecureRequired(false);
            // EM: is this secure?
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
    public TokenContext createTokenContext (User user, Map<String, Object> attr)
            throws KustvaktException {
        return null;
    }


    @Override
    public void removeUserSession (String token) throws KustvaktException {
        throw new KustvaktException(StatusCodes.NOT_SUPPORTED);
    }


    @Override
    public TokenContext refresh (TokenContext context)
            throws KustvaktException {
        return null;
    }


    @Override
    public TokenType getTokenType () {
        return TokenType.BASIC;
    }
}
