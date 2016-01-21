package de.ids_mannheim.korap.config;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.user.Attributes;
import de.ids_mannheim.korap.user.TokenContext;
import de.ids_mannheim.korap.user.User;
import de.ids_mannheim.korap.utils.TimeUtils;
import org.joda.time.DateTime;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.Map;

/**
 * @author hanl
 * @date 19/05/2014
 */
public class JWTSigner {

    private URL issuer;
    private JWSSigner signer;
    private JWSVerifier verifier;
    private final int defaultttl;

    public JWTSigner(final byte[] secret, URL issuer, final int defaultttl) {
        this.issuer = issuer;
        this.signer = new MACSigner(secret);
        this.verifier = new MACVerifier(secret);
        this.defaultttl = defaultttl;
    }

    public JWTSigner(final byte[] secret, String issuer)
            throws MalformedURLException {
        this(secret, new URL(issuer), 72 * 60 * 60);
    }

    public SignedJWT createJWT(User user, Map<String, String> attr) {
        return signContent(user, attr, defaultttl);
    }

    public SignedJWT signContent(User user, Map<String, String> attr, int ttl) {
        String scopes;

        JWTClaimsSet cs = new JWTClaimsSet();
        cs.setIssuerClaim(this.issuer.toString());

        if ((scopes = attr.get(Attributes.SCOPES)) != null) {
            Scopes claims = Scopes.mapScopes(scopes, user.getDetails());
            cs.setCustomClaims(claims.toMap());
        }

        cs.setSubjectClaim(user.getUsername());
        if (attr.get(Attributes.CLIENT_ID) != null)
            cs.setAudienceClaim(
                    new String[] { attr.get(Attributes.CLIENT_ID) });
        cs.setExpirationTimeClaim(
                TimeUtils.getNow().plusSeconds(ttl).getMillis());
        SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256),
                cs);
        try {
            signedJWT.sign(signer);
        }catch (JOSEException e) {
            return null;
        }
        return signedJWT;
    }

    /**
     * @param username
     * @param json
     * @return
     */
    public SignedJWT signContent(String username, String userclient,
            String json, int ttl) {
        JWTClaimsSet cs = new JWTClaimsSet();
        cs.setSubjectClaim(username);
        if (!json.isEmpty())
            cs.setCustomClaim("data", json);
        cs.setExpirationTimeClaim(
                TimeUtils.getNow().plusSeconds(ttl).getMillis());
        cs.setIssuerClaim(this.issuer.toString());

        if (!userclient.isEmpty())
            cs.setCustomClaim("userip", userclient);

        SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256),
                cs);
        try {
            signedJWT.sign(signer);
        }catch (JOSEException e) {
            return null;
        }
        return signedJWT;
    }

    public SignedJWT signContent(String username, String userclient,
            String json) {
        return signContent(username, userclient, json, defaultttl);
    }

    public SignedJWT createSignedToken(String username) {
        return createSignedToken(username, defaultttl);
    }

    // add client info
    public SignedJWT createSignedToken(String username, int ttl) {
        return signContent(username, "", "", ttl);
    }

    public SignedJWT verifyToken(String token) throws KustvaktException {
        SignedJWT client;
        try {
            client = SignedJWT.parse(token);
            if (!client.verify(verifier))
                throw new KustvaktException(StatusCodes.REQUEST_INVALID);

            if (!new DateTime(client.getJWTClaimsSet().getExpirationTimeClaim())
                    .isAfterNow())
                throw new KustvaktException(StatusCodes.EXPIRED,
                        "authentication token is expired", token);
        }catch (ParseException | JOSEException e) {
            //todo: message or entity, how to treat??!
            throw new KustvaktException(StatusCodes.ILLEGAL_ARGUMENT,
                    "token could not be verified", token);
        }
        return client;
    }

    // does not care about expiration times
    public String retrieveContent(String signedContent)
            throws KustvaktException {
        SignedJWT jwt;
        try {
            jwt = SignedJWT.parse(signedContent);
            if (!jwt.verify(verifier))
                throw new KustvaktException(StatusCodes.REQUEST_INVALID,
                        "token invalid", signedContent);
            return (String) jwt.getJWTClaimsSet().getCustomClaim("data");
        }catch (ParseException | JOSEException e) {
            return null;
        }
    }

    public TokenContext getTokenContext(String idtoken)
            throws ParseException, JOSEException, KustvaktException {
        SignedJWT signedJWT = verifyToken(idtoken);

        TokenContext c = new TokenContext();
        c.setUsername(signedJWT.getJWTClaimsSet().getSubjectClaim());
        if (signedJWT.getJWTClaimsSet().getAudienceClaim() != null)
            c.addContextParameter(Attributes.CLIENT_ID,
                    signedJWT.getJWTClaimsSet().getAudienceClaim()[0]);
        c.setExpirationTime(
                signedJWT.getJWTClaimsSet().getExpirationTimeClaim());
        c.setToken(idtoken);
        c.addParams(signedJWT.getJWTClaimsSet().getCustomClaims());
        return c;
    }

}
