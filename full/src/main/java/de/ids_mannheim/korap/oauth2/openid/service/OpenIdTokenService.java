package de.ids_mannheim.korap.oauth2.openid.service;

import java.net.URI;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.Date;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.AccessTokenResponse;
import com.nimbusds.oauth2.sdk.AuthorizationCodeGrant;
import com.nimbusds.oauth2.sdk.AuthorizationGrant;
import com.nimbusds.oauth2.sdk.GrantType;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.auth.ClientAuthenticationMethod;
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic;
import com.nimbusds.oauth2.sdk.auth.ClientSecretPost;
import com.nimbusds.oauth2.sdk.id.Audience;
import com.nimbusds.oauth2.sdk.id.Issuer;
import com.nimbusds.oauth2.sdk.id.Subject;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import com.nimbusds.oauth2.sdk.token.RefreshToken;
import com.nimbusds.oauth2.sdk.token.Tokens;
import com.nimbusds.openid.connect.sdk.OIDCTokenResponse;
import com.nimbusds.openid.connect.sdk.claims.IDTokenClaimsSet;
import com.nimbusds.openid.connect.sdk.token.OIDCTokens;

import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.oauth2.constant.OAuth2Error;
import de.ids_mannheim.korap.oauth2.entity.AccessScope;
import de.ids_mannheim.korap.oauth2.entity.Authorization;
import de.ids_mannheim.korap.oauth2.service.OAuth2TokenService;
import de.ids_mannheim.korap.utils.TimeUtils;

/**
 * ID Tokens MUST be signed using JWS and optionally both signed and
 * then encrypted using JWS [JWS] and JWE [JWE] respectively.
 * 
 * ID Tokens MUST NOT use none as the alg value unless the Response
 * Type used returns no ID Token from the Authorization Endpoint (such
 * as when using the Authorization Code Flow) and the Client
 * explicitly requested the use of none at Registration time.
 * 
 * ID Tokens SHOULD NOT use the JWS or JWE x5u, x5c, jku, or jwk
 * Header Parameter fields.
 * 
 * @author margaretha
 *
 */
@Service
public class OpenIdTokenService extends OAuth2TokenService {

    public AccessTokenResponse requestAccessToken (TokenRequest tokenRequest)
            throws KustvaktException {
        AuthorizationGrant grant = tokenRequest.getAuthorizationGrant();
        GrantType grantType = grant.getType();
        ClientAuthentication clientAuthentication =
                tokenRequest.getClientAuthentication();
        String[] clientCredentials =
                extractClientCredentials(clientAuthentication);

        if (grantType.equals(GrantType.AUTHORIZATION_CODE)) {
            AuthorizationCodeGrant codeGrant = (AuthorizationCodeGrant) grant;
            String authorizationCode =
                    codeGrant.getAuthorizationCode().getValue();
            URI redirectionURI = codeGrant.getRedirectionURI();
            String redirectURI = null;
            if (redirectionURI != null) {
                redirectURI = redirectionURI.toString();
            }
            Authorization authorization =
                    requestAccessTokenWithAuthorizationCode(authorizationCode,
                            redirectURI, clientCredentials[0],
                            clientCredentials[1]);
            return createsAccessTokenResponse(authorization);
        }
        else if (grantType.equals(GrantType.PASSWORD)) {

        }
        else if (grantType.equals(GrantType.CLIENT_CREDENTIALS)) {

        }
        else {
            throw new KustvaktException(StatusCodes.UNSUPPORTED_GRANT_TYPE,
                    grantType + " is not supported.",
                    OAuth2Error.UNSUPPORTED_GRANT_TYPE);
        }
        return null;
    }


    private AccessTokenResponse createsAccessTokenResponse (
            Authorization authorization) throws KustvaktException {
        Set<AccessScope> scopes = authorization.getScopes();
        String[] scopeArray = scopes.stream().map(scope -> scope.toString())
                .toArray(String[]::new);
        Scope scope = new Scope(scopeArray);
        AccessToken accessToken =
                new BearerAccessToken(config.getTokenTTL(), scope);
        RefreshToken refreshToken = new RefreshToken();

        if (scope.contains("openid")) {
            JWTClaimsSet claims = createIdTokenClaims(
                    authorization.getClientId(), authorization.getUserId());
            SignedJWT idToken = signIdToken(claims,
                    // default
                    new JWSHeader(JWSAlgorithm.RS256),
                    config.getRsaPrivateKey());
            OIDCTokens tokens =
                    new OIDCTokens(idToken, accessToken, refreshToken);
            return new OIDCTokenResponse(tokens);
        }
        else {
            Tokens tokens = new Tokens(accessToken, refreshToken);
            return new AccessTokenResponse(tokens);
        }
    }


    private String[] extractClientCredentials (
            ClientAuthentication clientAuthentication)
            throws KustvaktException {

        ClientAuthenticationMethod method = clientAuthentication.getMethod();
        String clientSecret;
        String clientId;
        if (method.equals(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)) {
            ClientSecretBasic basic = (ClientSecretBasic) clientAuthentication;
            clientSecret = basic.getClientSecret().getValue();
            clientId = basic.getClientID().getValue();
        }
        else if (method.equals(ClientAuthenticationMethod.CLIENT_SECRET_POST)) {
            ClientSecretPost post = (ClientSecretPost) clientAuthentication;
            clientSecret = post.getClientSecret().getValue();
            clientId = post.getClientID().getValue();
        }
        else {
            // client authentication method is not supported
            throw new KustvaktException(
                    StatusCodes.UNSUPPORTED_AUTHENTICATION_METHOD,
                    method.getValue() + " is not supported.",
                    OAuth2Error.INVALID_CLIENT);
        }
        return new String[] { clientId, clientSecret };
    }

    private JWTClaimsSet createIdTokenClaims (String client_id, String username)
            throws KustvaktException {
        // A locally unique and never reassigned identifier within the
        // Issuer for the End-User
        Subject sub = new Subject(username);
        Issuer iss = new Issuer(config.getIssuerURI());
        Audience aud = new Audience(client_id);
        ArrayList<Audience> audList = new ArrayList<Audience>(1);
        audList.add(aud);
        Date iat = TimeUtils.getNow().toDate();
        Date exp =
                TimeUtils.getNow().plusSeconds(config.getTokenTTL()).toDate();

        IDTokenClaimsSet claims =
                new IDTokenClaimsSet(iss, sub, audList, exp, iat);
        try {
            return claims.toJWTClaimsSet();
        }
        catch (ParseException e) {
            throw new KustvaktException(StatusCodes.ID_TOKEN_CLAIM_ERROR,
                    e.getMessage());
        }
    }

    /**
     * id token should be signed and additionally encrypted
     * according to keys and algorithms the client specified
     * during registration
     * 
     * Currently supporting only:
     * default algorithm = RSA SHA-256 (RS256)
     * 
     * @param jwtClaimsSet
     *            id token claim set
     * @param jwsHeader
     *            jws header
     * @param privateKey
     * 
     * @return
     * @throws KustvaktException
     */
    private SignedJWT signIdToken (JWTClaimsSet jwtClaimsSet,
            JWSHeader jwsHeader, PrivateKey privateKey)
            throws KustvaktException {

        SignedJWT idToken = new SignedJWT(jwsHeader, jwtClaimsSet);
        JWSSigner signer = null;
        if (jwsHeader.getAlgorithm().equals(JWSAlgorithm.RS256)) {
            signer = new RSASSASigner(privateKey);
        }
        else {
            throw new KustvaktException(StatusCodes.ID_TOKEN_SIGNING_FAILED,
                    "Unsupported algorithm "
                            + jwsHeader.getAlgorithm().getName());
        }

        try {
            idToken.sign(signer);
        }
        catch (JOSEException e) {
            throw new KustvaktException(StatusCodes.ID_TOKEN_SIGNING_FAILED,
                    e.getMessage());
        }

        return idToken;
    }
}
