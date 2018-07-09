package de.ids_mannheim.korap.oauth2.oltu.service;

import java.time.ZonedDateTime;
import java.util.Set;

import javax.ws.rs.core.Response.Status;

import org.apache.oltu.oauth2.as.issuer.OAuthIssuer;
import org.apache.oltu.oauth2.as.request.AbstractOAuthTokenRequest;
import org.apache.oltu.oauth2.as.response.OAuthASResponse;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.OAuthResponse;
import org.apache.oltu.oauth2.common.message.types.GrantType;
import org.apache.oltu.oauth2.common.message.types.TokenType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.oauth2.constant.OAuth2Error;
import de.ids_mannheim.korap.oauth2.dao.AccessTokenDao;
import de.ids_mannheim.korap.oauth2.entity.AccessScope;
import de.ids_mannheim.korap.oauth2.entity.Authorization;
import de.ids_mannheim.korap.oauth2.entity.OAuth2Client;
import de.ids_mannheim.korap.oauth2.service.OAuth2TokenService;

@Service
public class OltuTokenService extends OAuth2TokenService {

    @Autowired
    private OAuthIssuer oauthIssuer;

    @Autowired
    private AccessTokenDao tokenDao;

    public OAuthResponse requestAccessToken (
            AbstractOAuthTokenRequest oAuthRequest)
            throws KustvaktException, OAuthSystemException {

        String grantType = oAuthRequest.getGrantType();

        if (grantType.equals(GrantType.AUTHORIZATION_CODE.toString())) {
            Authorization authorization = retrieveAuthorization(
                    oAuthRequest.getCode(), oAuthRequest.getRedirectURI(),
                    oAuthRequest.getClientId(), oAuthRequest.getClientSecret());
            return createsAccessTokenResponse(authorization);
        }
        else if (grantType.equals(GrantType.PASSWORD.toString())) {
            return requestAccessTokenWithPassword(oAuthRequest);
        }
        else if (grantType.equals(GrantType.CLIENT_CREDENTIALS.toString())) {
            ZonedDateTime authenticationTime =
                    requestAccessTokenWithClientCredentials(
                            oAuthRequest.getClientId(),
                            oAuthRequest.getClientSecret(),
                            oAuthRequest.getScopes());

            Set<String> scopes =
                    scopeService.filterScopes(oAuthRequest.getScopes(),
                            config.getClientCredentialsScopes());
            return createsAccessTokenResponse(scopes, null, authenticationTime);
        }
        else {
            throw new KustvaktException(StatusCodes.UNSUPPORTED_GRANT_TYPE,
                    grantType + " is not supported.",
                    OAuth2Error.UNSUPPORTED_GRANT_TYPE);
        }

    }

    private OAuthResponse requestAccessTokenWithPassword (
            AbstractOAuthTokenRequest oAuthRequest)
            throws KustvaktException, OAuthSystemException {

        OAuth2Client client = clientService.authenticateClient(
                oAuthRequest.getClientId(), oAuthRequest.getClientSecret());
        if (!client.isNative()) {
            throw new KustvaktException(StatusCodes.CLIENT_AUTHORIZATION_FAILED,
                    "Password grant is not allowed for third party clients",
                    OAuth2Error.UNAUTHORIZED_CLIENT);
        }

        Set<String> scopes = oAuthRequest.getScopes();
        if (scopes == null || scopes.isEmpty()) {
            scopes = config.getDefaultAccessScopes();
        }

        ZonedDateTime authenticationTime = authenticateUser(
                oAuthRequest.getUsername(), oAuthRequest.getPassword(), scopes);

        return createsAccessTokenResponse(scopes, oAuthRequest.getUsername(),
                authenticationTime);

    }

    /**
     * Creates an OAuthResponse containing an access token and a
     * refresh token with type Bearer.
     * 
     * @param authenticationTime
     * 
     * @return an OAuthResponse containing an access token
     * @throws OAuthSystemException
     * @throws KustvaktException
     */
    private OAuthResponse createsAccessTokenResponse (Set<String> scopes,
            String userId, ZonedDateTime authenticationTime)
            throws OAuthSystemException, KustvaktException {

        String accessToken = oauthIssuer.accessToken();
        // String refreshToken = oauthIssuer.refreshToken();

        Set<AccessScope> accessScopes =
                scopeService.convertToAccessScope(scopes);
        tokenDao.storeAccessToken(accessToken, accessScopes, userId,
                authenticationTime);

        return OAuthASResponse.tokenResponse(Status.OK.getStatusCode())
                .setAccessToken(accessToken)
                .setTokenType(TokenType.BEARER.toString())
                .setExpiresIn(String.valueOf(config.getTokenTTL()))
                // .setRefreshToken(refreshToken)
                .setScope(String.join(" ", scopes)).buildJSONMessage();
    }

    private OAuthResponse createsAccessTokenResponse (
            Authorization authorization)
            throws OAuthSystemException, KustvaktException {
        String accessToken = oauthIssuer.accessToken();
        // String refreshToken = oauthIssuer.refreshToken();

        tokenDao.storeAccessToken(authorization, accessToken);

        String scopes = scopeService
                .convertAccessScopesToString(authorization.getScopes());

        OAuthResponse r =
                OAuthASResponse.tokenResponse(Status.OK.getStatusCode())
                        .setAccessToken(accessToken)
                        .setTokenType(TokenType.BEARER.toString())
                        .setExpiresIn(String.valueOf(config.getTokenTTL()))
                        // .setRefreshToken(refreshToken)
                        .setScope(scopes).buildJSONMessage();
        return r;
    }
}
