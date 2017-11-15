package de.ids_mannheim.korap.authentication;

import com.nimbusds.jwt.SignedJWT;
import de.ids_mannheim.korap.config.JWTSigner;
import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.handlers.OAuthDb;
import de.ids_mannheim.korap.interfaces.AuthenticationIface;
import de.ids_mannheim.korap.interfaces.db.PersistenceClient;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.AuthenticationType;
import de.ids_mannheim.korap.user.TokenContext;
import de.ids_mannheim.korap.user.User;
import de.ids_mannheim.korap.utils.NamingUtils;
import de.ids_mannheim.korap.utils.StringUtils;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

import java.text.ParseException;
import java.util.Map;

/**
 * @author hanl
 * @date 12/11/2014
 */
public class OpenIDconnectAuthentication implements AuthenticationIface {

    private OAuthDb database;
    private KustvaktConfiguration config;


    public OpenIDconnectAuthentication (KustvaktConfiguration config,
                                        PersistenceClient client) {
        this.database = new OAuthDb(client);
        this.config = config;
    }


    @Override
    public TokenContext getTokenContext(String authToken)
            throws KustvaktException {
        return this.database.getContext(authToken);
    }


    @Override
    public TokenContext createTokenContext(User user, Map<String, Object> attr)
            throws KustvaktException {
        String cl_secret = (String) attr.get(Attributes.CLIENT_SECRET);
        if (cl_secret == null)
            throw new KustvaktException(StatusCodes.REQUEST_INVALID);
        attr.remove(cl_secret);
        JWTSigner signer = new JWTSigner(cl_secret.getBytes(),
                config.getIssuer(), config.getTokenTTL());
        TokenContext c = new TokenContext();
        c.setUsername(user.getUsername());
        SignedJWT jwt = signer.createJWT(user, attr);
        try {
            c.setExpirationTime(jwt.getJWTClaimsSet().getExpirationTimeClaim());
        }
        catch (ParseException e) {
            throw new KustvaktException(StatusCodes.ILLEGAL_ARGUMENT);
        }
        c.setAuthenticationType(AuthenticationType.OPENID);
        c.setToken(jwt.serialize());
        CacheManager.getInstance().getCache("id_tokens")
                .put(new Element(c.getToken(), c));
        return c;
    }


    @Override
    public void removeUserSession (String token) throws KustvaktException {
        // emit token from cache only
    }


    @Override
    public TokenContext refresh (TokenContext context) throws KustvaktException {
        throw new UnsupportedOperationException("method not supported");
    }


    @Override
    public AuthenticationType getIdentifier () {
        return AuthenticationType.OPENID;
    }
}
