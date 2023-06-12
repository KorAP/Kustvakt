package de.ids_mannheim.korap.authentication;

import java.text.ParseException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jwt.SignedJWT;

import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.FullConfiguration;
import de.ids_mannheim.korap.config.JWTSigner;
import de.ids_mannheim.korap.constant.TokenType;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.security.context.TokenContext;
import de.ids_mannheim.korap.user.User;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;

/**
 * Authentication provider using JWT tokens
 * 
 * Created by hanl on 5/23/14.
 */
@Deprecated
public class APIAuthentication implements AuthenticationIface {

    private static Logger jlog = LogManager.getLogger(APIAuthentication.class);
    public static boolean DEBUG = false;
    
    private JWTSigner signedToken;

    public APIAuthentication (FullConfiguration config) throws JOSEException {
        this.signedToken = new JWTSigner(config.getSharedSecret(),
                config.getIssuer(), config.getTokenTTL());
    }

    /**
     * EM: for testing
     * 
     * @param signedToken
     */
    public APIAuthentication (JWTSigner signedToken) {
        this.signedToken = signedToken;
    }

    @Override
    public TokenContext getTokenContext (String authToken)
            throws KustvaktException {
        TokenContext context;
        // Element ein = invalided.get(authToken);
        try {
            context = signedToken.getTokenContext(authToken);
            context.setTokenType(getTokenType());
        }
        catch (JOSEException | ParseException ex) {
            throw new KustvaktException(StatusCodes.ILLEGAL_ARGUMENT);
        }
        // context = (TokenContext) e.getObjectValue();
        // throw new KustvaktException(StatusCodes.EXPIRED);
        return context;
    }


    @Override
    public TokenContext createTokenContext (User user, Map<String, Object> attr)
            throws KustvaktException {
        TokenContext c = new TokenContext();
        c.setUsername(user.getUsername());
        SignedJWT jwt = signedToken.createJWT(user, attr);
        try {
            c.setExpirationTime(
                    jwt.getJWTClaimsSet().getExpirationTime().getTime());
            if (DEBUG ) {
                jlog.debug(jwt.getJWTClaimsSet()
                        .getClaim(Attributes.AUTHENTICATION_TIME));
            }
            Date authTime = jwt.getJWTClaimsSet()
                    .getDateClaim(Attributes.AUTHENTICATION_TIME);
            ZonedDateTime time = ZonedDateTime.ofInstant(authTime.toInstant(),
                    ZoneId.of(Attributes.DEFAULT_TIME_ZONE));
            c.setAuthenticationTime(time);
        }
        catch (ParseException e) {
            throw new KustvaktException(StatusCodes.ILLEGAL_ARGUMENT);
        }
        c.setTokenType(getTokenType());
        c.setToken(jwt.serialize());
        // id_tokens.put(new Element(c.getToken(), c));
        return c;
    }


    @Override
    public TokenType getTokenType () {
        return TokenType.API;
    }
}
