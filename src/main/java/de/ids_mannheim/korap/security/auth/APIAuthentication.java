package de.ids_mannheim.korap.security.auth;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jwt.SignedJWT;
import de.ids_mannheim.korap.config.JWTSigner;
import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.interfaces.AuthenticationIface;
import de.ids_mannheim.korap.user.Attributes;
import de.ids_mannheim.korap.user.TokenContext;
import de.ids_mannheim.korap.user.User;
import de.ids_mannheim.korap.utils.StringUtils;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

import java.text.ParseException;
import java.util.Map;

/**
 * Created by hanl on 5/23/14.
 */
public class APIAuthentication implements AuthenticationIface {

    private JWTSigner signedToken;

    public APIAuthentication(KustvaktConfiguration bconfig) {
        KustvaktConfiguration config = bconfig;
        this.signedToken = new JWTSigner(config.getSharedSecret(),
                config.getIssuer(), config.getTokenTTL());
    }

    @Cacheable(value = "id_tokens", key = "#authToken")
    @Override
    public TokenContext getUserStatus(String authToken)
            throws KustvaktException {
        try {
            authToken = StringUtils.stripTokenType(authToken);
            TokenContext c = signedToken.getTokenContext(authToken);
            c.setTokenType(Attributes.API_AUTHENTICATION);
            return c;
        }catch (JOSEException | ParseException e) {
            throw new KustvaktException(StatusCodes.ILLEGAL_ARGUMENT);
        }
    }

    @Override
    public TokenContext createUserSession(User user, Map<String, Object> attr)
            throws KustvaktException {
        TokenContext c = new TokenContext();
        c.setUsername(user.getUsername());
        SignedJWT jwt = signedToken.createJWT(user, attr);
        try {
            c.setExpirationTime(jwt.getJWTClaimsSet().getExpirationTimeClaim());
        }catch (ParseException e) {
            throw new KustvaktException(StatusCodes.ILLEGAL_ARGUMENT);
        }
        c.setTokenType(Attributes.API_AUTHENTICATION);
        c.setToken(jwt.serialize());
        CacheManager.getInstance().getCache("id_tokens")
                .put(new Element(c.getToken(), c));

        return c;
    }

    // todo: cache and set expiration to token expiration. if token in that cache, it is not to be used anymore!
    @CacheEvict(value = "id_tokens", key = "#token")
    @Override
    public void removeUserSession(String token) throws KustvaktException {
        // invalidate token!
    }

    @Override
    public TokenContext refresh(TokenContext context) throws KustvaktException {
        return null;
    }

    @Override
    public String getIdentifier() {
        return Attributes.API_AUTHENTICATION;
    }

}
