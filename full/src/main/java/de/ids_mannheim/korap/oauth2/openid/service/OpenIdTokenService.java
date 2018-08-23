package de.ids_mannheim.korap.oauth2.openid.service;

import java.net.URI;
import java.security.PrivateKey;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
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
import com.nimbusds.oauth2.sdk.ResourceOwnerPasswordCredentialsGrant;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.auth.ClientAuthenticationMethod;
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic;
import com.nimbusds.oauth2.sdk.auth.ClientSecretPost;
import com.nimbusds.oauth2.sdk.id.Audience;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.Issuer;
import com.nimbusds.oauth2.sdk.id.Subject;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import com.nimbusds.oauth2.sdk.token.RefreshToken;
import com.nimbusds.oauth2.sdk.token.Tokens;
import com.nimbusds.openid.connect.sdk.Nonce;
import com.nimbusds.openid.connect.sdk.OIDCTokenResponse;
import com.nimbusds.openid.connect.sdk.claims.IDTokenClaimsSet;
import com.nimbusds.openid.connect.sdk.token.OIDCTokens;

import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.oauth2.constant.OAuth2Error;
import de.ids_mannheim.korap.oauth2.dao.AccessTokenDao;
import de.ids_mannheim.korap.oauth2.dao.RefreshTokenDao;
import de.ids_mannheim.korap.oauth2.entity.AccessScope;
import de.ids_mannheim.korap.oauth2.entity.Authorization;
import de.ids_mannheim.korap.oauth2.entity.OAuth2Client;
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

    @Autowired
    private AccessTokenDao tokenDao;
    @Autowired
    private RefreshTokenDao refreshDao;

    public AccessTokenResponse requestAccessToken (TokenRequest tokenRequest)
            throws KustvaktException {
        AuthorizationGrant grant = tokenRequest.getAuthorizationGrant();
        GrantType grantType = grant.getType();
        ClientAuthentication clientAuthentication =
                tokenRequest.getClientAuthentication();
        ClientID clientId = tokenRequest.getClientID();

        if (grantType.equals(GrantType.AUTHORIZATION_CODE)) {
            return requestAccessTokenWithAuthorizationCode(grant,
                    clientAuthentication, clientId);
        }
        else if (grantType.equals(GrantType.PASSWORD)) {
            ResourceOwnerPasswordCredentialsGrant passwordGrant =
                    (ResourceOwnerPasswordCredentialsGrant) grant;
            return requestAccessTokenWithPassword(passwordGrant.getUsername(),
                    passwordGrant.getPassword().getValue(),
                    tokenRequest.getScope(), clientAuthentication, clientId);
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

    /**
     * Third party apps must not be allowed to use password grant.
     * MH: password grant is only allowed for trusted clients (korap
     * frontend)
     * 
     * According to RFC 6749, client authentication is only required
     * for confidential clients and whenever client credentials are
     * provided. Moreover, client_id is optional for password grant,
     * but without it, the authentication server cannot check the
     * client type. To make sure that confidential clients
     * authenticate, client_id is made required (similar to
     * authorization code grant).
     * 
     * @param username
     *            username, required
     * @param password
     *            password, required
     * @param scope
     *            scope, optional
     * @param clientAuthentication
     * @param clientId
     * @return
     * @throws KustvaktException
     */
    private AccessTokenResponse requestAccessTokenWithPassword (String username,
            String password, Scope scope,
            ClientAuthentication clientAuthentication, ClientID clientId)
            throws KustvaktException {

        Set<String> scopeSet = null;
        if (scope != null) {
            scopeSet = new HashSet<String>();
            scopeSet.addAll(scope.toStringList());
        }
        else {
            scopeSet = config.getDefaultAccessScopes();
            scope = new Scope(scopeSet.toArray(new String[scopeSet.size()]));
        }

        ZonedDateTime authenticationTime;
        String clientIdStr = null;
        OAuth2Client client;
        if (clientAuthentication == null) {
            if (clientId == null) {
                throw new KustvaktException(StatusCodes.MISSING_PARAMETER,
                        "Missing parameters: client_id",
                        OAuth2Error.INVALID_REQUEST);
            }
            else {
                clientIdStr = clientId.getValue();
                client = clientService.authenticateClient(clientIdStr, null);
            }
        }
        else {
            String[] clientCredentials =
                    extractClientCredentials(clientAuthentication);
            clientIdStr = clientCredentials[0];
            client = clientService.authenticateClient(clientCredentials[0],
                    clientCredentials[1]);
        }

        if (!client.isSuper()) {
            throw new KustvaktException(StatusCodes.CLIENT_AUTHORIZATION_FAILED,
                    "Password grant is not allowed for third party clients",
                    OAuth2Error.UNAUTHORIZED_CLIENT);
        }

        authenticationTime = authenticateUser(username, password, scopeSet);

        AccessToken accessToken =
                new BearerAccessToken(config.getAccessTokenExpiry(), scope);

        RefreshToken refreshToken = new RefreshToken();
        Set<AccessScope> scopes = scopeService.convertToAccessScope(scopeSet);
        de.ids_mannheim.korap.oauth2.entity.RefreshToken rt =
                refreshDao.storeRefreshToken(refreshToken.getValue(), username,
                        authenticationTime, clientId.getValue(), scopes);
        tokenDao.storeAccessToken(accessToken.getValue(), rt, scopes, username,
                clientIdStr, authenticationTime);

        return createsAccessTokenResponse(accessToken, refreshToken, scope,
                clientIdStr, username, authenticationTime, null);
    }

    private AccessTokenResponse requestAccessTokenWithAuthorizationCode (
            AuthorizationGrant grant, ClientAuthentication clientAuthentication,
            ClientID clientId) throws KustvaktException {
        AuthorizationCodeGrant codeGrant = (AuthorizationCodeGrant) grant;
        String authorizationCode = codeGrant.getAuthorizationCode().getValue();
        URI redirectionURI = codeGrant.getRedirectionURI();
        String redirectURI = null;
        if (redirectionURI != null) {
            redirectURI = redirectionURI.toString();
        }

        Authorization authorization = null;
        if (clientAuthentication == null) {
            if (clientId == null) {
                throw new KustvaktException(StatusCodes.MISSING_PARAMETER,
                        "Missing parameters: client_id",
                        OAuth2Error.INVALID_REQUEST);
            }
            else {
                authorization = retrieveAuthorization(authorizationCode,
                        redirectURI, clientId.getValue(), null);
            }
        }
        else {
            String[] clientCredentials =
                    extractClientCredentials(clientAuthentication);
            authorization = retrieveAuthorization(authorizationCode,
                    redirectURI, clientCredentials[0], clientCredentials[1]);
        }

        return createsAccessTokenResponse(authorization);

    }

    private AccessTokenResponse createsAccessTokenResponse (
            Authorization authorization) throws KustvaktException {
        Set<AccessScope> scopes = authorization.getScopes();
        String[] scopeArray = scopes.stream().map(scope -> scope.toString())
                .toArray(String[]::new);
        Scope scope = new Scope(scopeArray);
        AccessToken accessToken =
                new BearerAccessToken(config.getAccessTokenExpiry(), scope);
        RefreshToken refreshToken = new RefreshToken();
        de.ids_mannheim.korap.oauth2.entity.RefreshToken rt =
                refreshDao.storeRefreshToken(refreshToken.getValue(),
                        authorization.getUserId(),
                        authorization.getUserAuthenticationTime(),
                        authorization.getClientId(), scopes);

        tokenDao.storeAccessToken(accessToken.getValue(), rt, scopes,
                authorization.getUserId(), authorization.getClientId(),
                authorization.getUserAuthenticationTime());

        return createsAccessTokenResponse(accessToken, refreshToken, scope,
                authorization.getClientId(), authorization.getUserId(),
                authorization.getUserAuthenticationTime(),
                authorization.getNonce());
    }

    private AccessTokenResponse createsAccessTokenResponse (
            AccessToken accessToken, RefreshToken refreshToken, Scope scope,
            String clientId, String userId,
            ZonedDateTime userAuthenticationTime, String nonce)
            throws KustvaktException {

        if (scope.contains("openid")) {
            JWTClaimsSet claims = createIdTokenClaims(clientId, userId,
                    userAuthenticationTime, nonce);
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

    private JWTClaimsSet createIdTokenClaims (String client_id, String username,
            ZonedDateTime authenticationTime, String nonce)
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

        Date authTime = Date.from(authenticationTime.toInstant());
        claims.setAuthenticationTime(authTime);
        if (nonce != null && !nonce.isEmpty()) {
            claims.setNonce(new Nonce(nonce));
        }

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
