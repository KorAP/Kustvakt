package de.ids_mannheim.korap.config;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTClaimsSet.Builder;
import com.nimbusds.jwt.SignedJWT;

import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.security.context.TokenContext;
import de.ids_mannheim.korap.user.GenericUserData;
import de.ids_mannheim.korap.user.User;
import de.ids_mannheim.korap.user.Userdata;
import de.ids_mannheim.korap.utils.TimeUtils;

/**
 * @author hanl
 * @date 19/05/2014
 */
public class JWTSigner {

    private URL issuer;
    private JWSSigner signer;
    private JWSVerifier verifier;
    private final int defaultttl;


    public JWTSigner (final byte[] secret, URL issuer, final int defaulttl)
            throws JOSEException {
        this.issuer = issuer;
        this.signer = new MACSigner(secret);
        this.verifier = new MACVerifier(secret);
        this.defaultttl = defaulttl;
    }


    public JWTSigner (final byte[] secret, String issuer)
            throws MalformedURLException, JOSEException {
        this(secret, new URL(issuer), 72 * 60 * 60);
    }


    public SignedJWT createJWT (User user, Map<String, Object> attr) {
        return signContent(user, attr, defaultttl);
    }


    public SignedJWT signContent (User user, Map<String, Object> attr,
            int ttl) {
        String scopes;

        Builder csBuilder = new JWTClaimsSet.Builder();
        csBuilder.issuer(this.issuer.toString());

        if ((scopes = (String) attr.get(Attributes.SCOPES)) != null) {
            Userdata data = new GenericUserData();
            data.readQuietly(attr, false);
            Scopes claims = Scopes.mapScopes(scopes, data);
            Map<String, Object> map = claims.toMap();
            for (String key : map.keySet()) {
                csBuilder.claim(key, map.get(key));
            }
        }

        csBuilder.subject(user.getUsername());
        if (attr.get(Attributes.CLIENT_ID) != null) {
            csBuilder.audience((String) attr.get(Attributes.CLIENT_ID));
        }
        csBuilder.expirationTime(TimeUtils.getNow().plusSeconds(ttl).toDate());
        csBuilder.claim(Attributes.AUTHENTICATION_TIME,
                attr.get(Attributes.AUTHENTICATION_TIME));
        SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256),
                csBuilder.build());
        try {
            signedJWT.sign(signer);
        }
        catch (JOSEException e) {
            e.printStackTrace();
        }
        return signedJWT;
    }


    /**
     * @param username
     * @param json
     * @return
     */
    public SignedJWT signContent (String username, String userclient,
            String json, int ttl) {
        Builder cs = new JWTClaimsSet.Builder();
        cs.subject(username);
        if (!json.isEmpty()) cs.claim("data", json);
        cs.expirationTime(TimeUtils.getNow().plusSeconds(ttl).toDate());
        cs.issuer(this.issuer.toString());

        if (!userclient.isEmpty()) cs.claim("userip", userclient);

        SignedJWT signedJWT =
                new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), cs.build());
        try {
            signedJWT.sign(signer);
        }
        catch (JOSEException e) {
            return null;
        }
        return signedJWT;
    }


    public SignedJWT signContent (String username, String userclient,
            String json) {
        return signContent(username, userclient, json, defaultttl);
    }


    public SignedJWT createSignedToken (String username) {
        return createSignedToken(username, defaultttl);
    }


    // add client info
    public SignedJWT createSignedToken (String username, int ttl) {
        return signContent(username, "", "", ttl);
    }


    public SignedJWT verifyToken (String token) throws KustvaktException {
        SignedJWT client;
        try {
            client = SignedJWT.parse(token);
            if (!client.verify(verifier))
                throw new KustvaktException(StatusCodes.INVALID_ACCESS_TOKEN,
                        "Json Web Signature (JWS) object verification failed.");

            if (!new DateTime(client.getJWTClaimsSet().getExpirationTime())
                    .isAfterNow())
                throw new KustvaktException(StatusCodes.EXPIRED,
                        "Authentication token is expired", token);
        }
        catch (ParseException | JOSEException e) {
            // todo: message or entity, how to treat??!
            throw new KustvaktException(StatusCodes.ILLEGAL_ARGUMENT,
                    "Token could not be verified", token);
        }
        return client;
    }


    // does not care about expiration times
    public String retrieveContent (String signedContent)
            throws KustvaktException {
        SignedJWT jwt;
        try {
            jwt = SignedJWT.parse(signedContent);
            if (!jwt.verify(verifier))
                throw new KustvaktException(StatusCodes.REQUEST_INVALID,
                        "token invalid", signedContent);
            return jwt.getJWTClaimsSet().getStringClaim("data");
        }
        catch (ParseException | JOSEException e) {
            return null;
        }
    }


    public TokenContext getTokenContext (String idtoken)
            throws ParseException, JOSEException, KustvaktException {
        SignedJWT signedJWT = verifyToken(idtoken);

        TokenContext c = new TokenContext();
        c.setUsername(signedJWT.getJWTClaimsSet().getSubject());
        List<String> audienceList = signedJWT.getJWTClaimsSet().getAudience();
        if (audienceList != null && !audienceList.isEmpty())
            c.addContextParameter(Attributes.CLIENT_ID,
                    signedJWT.getJWTClaimsSet().getAudience().get(0));
        c.setExpirationTime(
                signedJWT.getJWTClaimsSet().getExpirationTime().getTime());
        c.setAuthenticationTime((ZonedDateTime) signedJWT.getJWTClaimsSet()
                .getClaim(Attributes.AUTHENTICATION_TIME));
        c.setToken(idtoken);
        c.addParams(signedJWT.getJWTClaimsSet().getClaims());
        return c;
    }

}
