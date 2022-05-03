package de.ids_mannheim.korap.authentication;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.constant.TokenType;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.oauth2.constant.OAuth2Error;
import de.ids_mannheim.korap.oauth2.dao.AccessTokenDao;
import de.ids_mannheim.korap.oauth2.entity.AccessToken;
import de.ids_mannheim.korap.oauth2.service.OAuth2ScopeServiceImpl;
import de.ids_mannheim.korap.security.context.TokenContext;
import de.ids_mannheim.korap.user.User;

/** Authentication provider for Bearer tokens
 * 
 * @author margaretha
 *
 */
@Component
public class OAuth2Authentication implements AuthenticationIface {

    @Autowired
    private AccessTokenDao accessDao;
    @Autowired
    private OAuth2ScopeServiceImpl scopeService;

    @Override
    public TokenContext getTokenContext (String authToken)
            throws KustvaktException {

        AccessToken accessToken = accessDao.retrieveAccessToken(authToken);
        if (accessToken.isRevoked()) {
            throw new KustvaktException(StatusCodes.INVALID_ACCESS_TOKEN,
                    "Access token is invalid", OAuth2Error.INVALID_TOKEN);
        }

        String scopes = scopeService
                .convertAccessScopesToString(accessToken.getScopes());

        TokenContext c = new TokenContext();
        c.setUsername(accessToken.getUserId());
        c.setExpirationTime(accessToken.getExpiryDate().toInstant().toEpochMilli());
        c.setToken(authToken);
        c.setTokenType(TokenType.BEARER);
        c.addContextParameter(Attributes.SCOPE, scopes);
        c.setAuthenticationTime(accessToken.getUserAuthenticationTime());
        return c;
    }

    @Override
    public TokenContext createTokenContext (User user, Map<String, Object> attr)
            throws KustvaktException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void removeUserSession (String token) throws KustvaktException {
        // TODO Auto-generated method stub

    }

    @Override
    public TokenContext refresh (TokenContext context)
            throws KustvaktException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public TokenType getTokenType () {
        return TokenType.BEARER;
    }

}
