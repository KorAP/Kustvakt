package de.ids_mannheim.korap.oauth2.oltu.service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.core.Response.Status;

import org.apache.oltu.oauth2.as.request.AbstractOAuthTokenRequest;
import org.apache.oltu.oauth2.as.response.OAuthASResponse;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.OAuthResponse;
import org.apache.oltu.oauth2.common.message.types.GrantType;
import org.apache.oltu.oauth2.common.message.types.TokenType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.encryption.RandomCodeGenerator;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.oauth2.constant.OAuth2Error;
import de.ids_mannheim.korap.oauth2.dao.AccessTokenDao;
import de.ids_mannheim.korap.oauth2.entity.AccessScope;
import de.ids_mannheim.korap.oauth2.entity.AccessToken;
import de.ids_mannheim.korap.oauth2.entity.Authorization;
import de.ids_mannheim.korap.oauth2.entity.OAuth2Client;
import de.ids_mannheim.korap.oauth2.oltu.OAuth2RevokeTokenRequest;
import de.ids_mannheim.korap.oauth2.service.OAuth2TokenService;

@Service
public class OltuTokenService extends OAuth2TokenService {

    @Autowired
    private RandomCodeGenerator randomGenerator;

    @Autowired
    private AccessTokenDao tokenDao;

    public OAuthResponse requestAccessToken (
            AbstractOAuthTokenRequest oAuthRequest)
            throws KustvaktException, OAuthSystemException {

        String grantType = oAuthRequest.getGrantType();

        if (grantType.equals(GrantType.AUTHORIZATION_CODE.toString())) {
            return requestAccessTokenWithAuthorizationCode(
                    oAuthRequest.getCode(), oAuthRequest.getRedirectURI(),
                    oAuthRequest.getClientId(), oAuthRequest.getClientSecret());
        }
        else if (grantType.equals(GrantType.PASSWORD.toString())) {
            return requestAccessTokenWithPassword(oAuthRequest.getClientId(),
                    oAuthRequest.getClientSecret(), oAuthRequest.getUsername(),
                    oAuthRequest.getPassword(), oAuthRequest.getScopes());
        }
        else if (grantType.equals(GrantType.CLIENT_CREDENTIALS.toString())) {
            return requestAccessTokenWithClientCredentials(
                    oAuthRequest.getClientId(), oAuthRequest.getClientSecret(),
                    oAuthRequest.getScopes());
        }
        else if (grantType.equals(GrantType.REFRESH_TOKEN.toString())) {
            return requestAccessTokenWithRefreshToken(
                    oAuthRequest.getRefreshToken(), oAuthRequest.getScopes(),
                    oAuthRequest.getClientId(), oAuthRequest.getClientSecret());
        }
        else {
            throw new KustvaktException(StatusCodes.UNSUPPORTED_GRANT_TYPE,
                    grantType + " is not supported.",
                    OAuth2Error.UNSUPPORTED_GRANT_TYPE);
        }

    }

    /**
     * Revokes all access token associated with the given refresh
     * token, and creates a new access token with the given refresh
     * token and scopes. Thus, at one point of time, there is only one
     * active access token associated with a refresh token.
     * 
     * Client authentication is done using the given client
     * credentials.
     * 
     * @param refreshToken
     * @param scopes
     * @param clientId
     * @param clientSecret
     * @return if successful, a new access token
     * @throws KustvaktException
     * @throws OAuthSystemException
     */
    private OAuthResponse requestAccessTokenWithRefreshToken (
            String refreshToken, Set<String> scopes, String clientId,
            String clientSecret)
            throws KustvaktException, OAuthSystemException {

        if (refreshToken == null || refreshToken.isEmpty()) {
            throw new KustvaktException(StatusCodes.MISSING_PARAMETER,
                    "Missing parameters: refresh_token",
                    OAuth2Error.INVALID_REQUEST);
        }

        clientService.authenticateClient(clientId, clientSecret);
        List<AccessToken> accessTokenList =
                tokenDao.retrieveAccessTokenByRefreshToken(refreshToken);
        if (accessTokenList.isEmpty()) {
            throw new KustvaktException(StatusCodes.INVALID_REFRESH_TOKEN,
                    "Refresh token is not found", OAuth2Error.INVALID_GRANT);
        }

        AccessToken latestAccessToken = accessTokenList.get(0);
        AccessToken origin = accessTokenList.get(accessTokenList.size() - 1);

        if (!clientId.equals(latestAccessToken.getClientId())) {
            throw new KustvaktException(StatusCodes.CLIENT_AUTHORIZATION_FAILED,
                    "Client " + clientId + "is not authorized",
                    OAuth2Error.INVALID_CLIENT);
        }

        if (latestAccessToken.isRefreshTokenRevoked()) {
            throw new KustvaktException(StatusCodes.INVALID_REFRESH_TOKEN,
                    "Refresh token has been revoked.",
                    OAuth2Error.INVALID_GRANT);
        }
        else if (ZonedDateTime.now(ZoneId.of(Attributes.DEFAULT_TIME_ZONE))
                .isAfter(origin.getCreatedDate()
                        .plusSeconds(config.getRefreshTokenExpiry()))) {
            throw new KustvaktException(StatusCodes.INVALID_REFRESH_TOKEN,
                    "Refresh token is expired.", OAuth2Error.INVALID_GRANT);
        }

        Set<AccessScope> requestedScopes = latestAccessToken.getScopes();
        if (scopes != null && !scopes.isEmpty()) {
            requestedScopes = scopeService.verifyRefreshScope(scopes,
                    latestAccessToken.getScopes());
        }

        if (!latestAccessToken.isRevoked()) {
            latestAccessToken.setRevoked(true);
            tokenDao.updateAccessToken(latestAccessToken);
        }

        return createsAccessTokenResponse(scopes, requestedScopes, clientId,
                latestAccessToken.getUserId(),
                latestAccessToken.getUserAuthenticationTime(), refreshToken);
    }

    /**
     * Issues an access token for the specified client if the
     * authorization code is valid and client successfully
     * authenticates.
     * 
     * @param code
     *            authorization code, required
     * @param redirectUri
     *            client redirect uri, required if specified in the
     *            authorization request
     * @param clientId
     *            client id, required
     * @param clientSecret
     *            client secret, required
     * @return an {@link OAuthResponse}
     * @throws OAuthSystemException
     * @throws KustvaktException
     */
    private OAuthResponse requestAccessTokenWithAuthorizationCode (String code,
            String redirectUri, String clientId, String clientSecret)
            throws OAuthSystemException, KustvaktException {
        Authorization authorization = retrieveAuthorization(code, redirectUri,
                clientId, clientSecret);

        Set<String> scopes = scopeService
                .convertAccessScopesToStringSet(authorization.getScopes());
        return createsAccessTokenResponse(scopes, authorization.getScopes(),
                authorization.getClientId(), authorization.getUserId(),
                authorization.getUserAuthenticationTime());

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
     * @param clientId
     *            client_id, required
     * @param clientSecret
     *            client_secret, required if client_secret was issued
     *            for the client in client registration.
     * @param username
     *            username, required
     * @param password
     *            password, required
     * @param scopes
     *            authorization scopes, optional
     * @return an {@link OAuthResponse}
     * @throws KustvaktException
     * @throws OAuthSystemException
     */
    private OAuthResponse requestAccessTokenWithPassword (String clientId,
            String clientSecret, String username, String password,
            Set<String> scopes) throws KustvaktException, OAuthSystemException {

        OAuth2Client client =
                clientService.authenticateClient(clientId, clientSecret);
        if (!client.isSuper()) {
            throw new KustvaktException(StatusCodes.CLIENT_AUTHORIZATION_FAILED,
                    "Password grant is not allowed for third party clients",
                    OAuth2Error.UNAUTHORIZED_CLIENT);
        }

        if (scopes == null || scopes.isEmpty()) {
            scopes = new HashSet<String>(1);
            scopes.add("all");
//            scopes = config.getDefaultAccessScopes();
        }

        ZonedDateTime authenticationTime =
                authenticateUser(username, password, scopes);

        Set<AccessScope> accessScopes =
                scopeService.convertToAccessScope(scopes);
        return createsAccessTokenResponse(scopes, accessScopes, clientId,
                username, authenticationTime);
    }

    /**
     * Clients must authenticate.
     * Client credentials grant is limited to native clients.
     * 
     * @param clientId
     *            client_id parameter, required
     * @param clientSecret
     *            client_secret parameter, required
     * @param scopes
     *            authorization scopes, optional
     * @return an {@link OAuthResponse}
     * @throws KustvaktException
     * @throws OAuthSystemException
     */
    protected OAuthResponse requestAccessTokenWithClientCredentials (
            String clientId, String clientSecret, Set<String> scopes)
            throws KustvaktException, OAuthSystemException {

        if (clientSecret == null || clientSecret.isEmpty()) {
            throw new KustvaktException(
                    StatusCodes.CLIENT_AUTHENTICATION_FAILED,
                    "Missing parameters: client_secret",
                    OAuth2Error.INVALID_REQUEST);
        }

        // OAuth2Client client =
        clientService.authenticateClient(clientId, clientSecret);

        // if (!client.isNative()) {
        // throw new KustvaktException(
        // StatusCodes.CLIENT_AUTHENTICATION_FAILED,
        // "Client credentials grant is not allowed for third party
        // clients",
        // OAuth2Error.UNAUTHORIZED_CLIENT);
        // }

        ZonedDateTime authenticationTime =
                ZonedDateTime.now(ZoneId.of(Attributes.DEFAULT_TIME_ZONE));

        scopes = scopeService.filterScopes(scopes,
                config.getClientCredentialsScopes());
        Set<AccessScope> accessScopes =
                scopeService.convertToAccessScope(scopes);
        return createsAccessTokenResponse(scopes, accessScopes, clientId, null,
                authenticationTime);
    }

    /**
     * Creates an OAuthResponse containing an access token of type
     * Bearer. By default, MD generator is used to generates access
     * token of 128 bit values, represented in hexadecimal comprising
     * 32 bytes. The generated value is subsequently encoded in
     * Base64.
     * 
     * <br /><br />
     * Additionally, a refresh token is issued. It can be used to
     * request a new access token without requiring user
     * reauthentication.
     * 
     * @param scopes
     *            a set of access token scopes in String
     * @param accessScopes
     *            a set of access token scopes in {@link AccessScope}
     * @param clientId
     *            a client id
     * @param userId
     *            a user id
     * @param authenticationTime
     *            the user authentication time
     * @return an {@link OAuthResponse}
     * @throws OAuthSystemException
     * @throws KustvaktException
     */
    private OAuthResponse createsAccessTokenResponse (Set<String> scopes,
            Set<AccessScope> accessScopes, String clientId, String userId,
            ZonedDateTime authenticationTime)
            throws OAuthSystemException, KustvaktException {

        String refreshToken = randomGenerator.createRandomCode();
        return createsAccessTokenResponse(scopes, accessScopes, clientId,
                userId, authenticationTime, refreshToken);
    }

    private OAuthResponse createsAccessTokenResponse (Set<String> scopes,
            Set<AccessScope> accessScopes, String clientId, String userId,
            ZonedDateTime authenticationTime, String refreshToken)
            throws OAuthSystemException, KustvaktException {
        String accessToken = randomGenerator.createRandomCode();

        tokenDao.storeAccessToken(accessToken, refreshToken, accessScopes,
                userId, clientId, authenticationTime);

        return OAuthASResponse.tokenResponse(Status.OK.getStatusCode())
                .setAccessToken(accessToken)
                .setTokenType(TokenType.BEARER.toString())
                .setExpiresIn(String.valueOf(config.getAccessTokenExpiry()))
                .setRefreshToken(refreshToken)
                .setScope(String.join(" ", scopes)).buildJSONMessage();
    }

    public void revokeToken (OAuth2RevokeTokenRequest revokeTokenRequest)
            throws KustvaktException {
        String clientId = revokeTokenRequest.getClientId();
        String clientSecret = revokeTokenRequest.getClientSecret();
        String token = revokeTokenRequest.getToken();
        String tokenType = revokeTokenRequest.getTokenType();

        clientService.authenticateClient(clientId, clientSecret);
        if (tokenType != null && tokenType.equals("refresh_token")) {
            if (!revokeRefreshToken(token)) {
                revokeAccessToken(token);
            }
            return;
        }

        if (!revokeAccessToken(token)) {
            revokeRefreshToken(token);
        }
    }

    private boolean revokeAccessToken (String token) throws KustvaktException {
        try {
            AccessToken accessToken = tokenDao.retrieveAccessToken(token);
            accessToken.setRevoked(true);
            tokenDao.updateAccessToken(accessToken);
            return true;
        }
        catch (KustvaktException e) {
            if (!e.getStatusCode().equals(StatusCodes.INVALID_ACCESS_TOKEN)) {
                return false;
            }
            throw e;
        }
    }

    private boolean revokeRefreshToken (String token) throws KustvaktException {
        List<AccessToken> accessTokenList =
                tokenDao.retrieveAccessTokenByRefreshToken(token);
        if (accessTokenList.isEmpty()) {
            return false;
        }

        for (AccessToken accessToken : accessTokenList) {
            accessToken.setRevoked(true);
            accessToken.setRefreshTokenRevoked(true);
            tokenDao.updateAccessToken(accessToken);
        }
        return true;
    }
}
