package de.ids_mannheim.korap.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.Response.Status;

import org.apache.oltu.oauth2.as.issuer.OAuthIssuer;
import org.apache.oltu.oauth2.as.request.AbstractOAuthTokenRequest;
import org.apache.oltu.oauth2.as.response.OAuthASResponse;
import org.apache.oltu.oauth2.common.error.OAuthError;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.OAuthResponse;
import org.apache.oltu.oauth2.common.message.types.GrantType;
import org.apache.oltu.oauth2.common.message.types.TokenType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.FullConfiguration;
import de.ids_mannheim.korap.entity.OAuth2Client;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.interfaces.AuthenticationManagerIface;

@Service
public class OAuth2Service {

    @Autowired
    private OAuth2ClientService clientService;
    @Autowired
    private FullConfiguration config;
    @Autowired
    private AuthenticationManagerIface authenticationManager;
    @Autowired
    private OAuthIssuer oauthIssuer;

    /** 
     * OAuth2 describes various ways for requesting an access token. Kustvakt 
     * supports:
     * <ul>
     * <li> Authorization code grant: obtains authorization from a third party
     * application.
     * </li>
     * <li> Resource owner password grant: strictly for clients that are parts 
     *   of KorAP. Clients use user credentials, e.g. Kalamar (front-end) with 
     *   login form. 
     * </li>
     * <li> Client credentials grant: strictly for clients that are parts 
     *   of KorAP. Clients access their own resources, not on behalf of a 
     *   user.
     * </li>
     * </ul>  
     *  
     *  
     * @param request 
     *  
     * @param oAuthRequest
     * @param authorization
     * @return
     * @throws KustvaktException
     * @throws OAuthSystemException
     */
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
            return requestAccessTokenWithPassword(oAuthRequest.getUsername(),
                    oAuthRequest.getPassword(), oAuthRequest.getScopes(),
                    oAuthRequest.getClientId(), oAuthRequest.getClientSecret());
        }
        else if (grantType.equals(GrantType.CLIENT_CREDENTIALS.toString())) {
            return requestAccessTokenWithClientCredentials(
                    oAuthRequest.getClientId(), oAuthRequest.getClientSecret(),
                    oAuthRequest.getScopes());
        }
        else {
            throw new KustvaktException(StatusCodes.UNSUPPORTED_GRANT_TYPE,
                    grantType + " is not supported.",
                    OAuthError.TokenResponse.UNSUPPORTED_GRANT_TYPE);
        }

    }

    /**
     * RFC 6749: 
     *  If the client type is confidential or the client was issued client
     *  credentials, the client MUST authenticate with the authorization server.
     * 
     * @param authorizationCode
     * @param redirectURI required if included in the authorization request
     * @param clientId required if there is no authorization header
     * @param clientSecret clilent_secret, required if client_secret was issued 
     *  for the client in client registration.
     * @return
     * @throws OAuthSystemException
     * @throws KustvaktException
     */
    private OAuthResponse requestAccessTokenWithAuthorizationCode (
            String authorizationCode, String redirectURI, String clientId,
            String clientSecret)
            throws KustvaktException, OAuthSystemException {

        clientService.authenticateClient(clientId, clientSecret);

        // TODO
        // check authorization code
        // check redirectURI
        return createsAccessTokenResponse();
    }



    /**  Third party apps must not be allowed to use password grant.
     * MH: password grant is only allowed for trusted clients (korap frontend)
     *  
     * According to RFC 6749, client authentication is only required for 
     * confidential clients and whenever client credentials are provided.
     * Moreover, client_id is optional for password grant, but without it, 
     * the authentication server cannot check the client type. 
     * 
     * To make sure that confidential clients authenticate, client_id is made 
     * required (similar to authorization code grant).
     * 
     * 
     * @param username username, required
     * @param password user password, required
     * @param scopes
     * @param clientId client_id, required
     * @param clientSecret clilent_secret, required if client_secret was issued 
     *  for the client in client registration.
     * @return
     * @throws KustvaktException
     * @throws OAuthSystemException
     */
    private OAuthResponse requestAccessTokenWithPassword (String username,
            String password, Set<String> scopes, String clientId,
            String clientSecret)
            throws KustvaktException, OAuthSystemException {

        OAuth2Client client =
                clientService.authenticateClient(clientId, clientSecret);
        if (!client.isNative()) {
            throw new KustvaktException(StatusCodes.CLIENT_AUTHORIZATION_FAILED,
                    "Password grant is not allowed for third party clients",
                    OAuthError.TokenResponse.UNAUTHORIZED_CLIENT);
        }

        authenticateUser(username, password, scopes);
        return createsAccessTokenResponse();
    }

    public void authenticateUser (String username, String password,
            Set<String> scopes) throws KustvaktException {
        if (username == null || username.isEmpty()) {
            throw new KustvaktException(StatusCodes.MISSING_PARAMETER,
                    "username is missing.",
                    OAuthError.TokenResponse.INVALID_REQUEST);
        }
        if (password == null || password.isEmpty()) {
            throw new KustvaktException(StatusCodes.MISSING_PARAMETER,
                    "password is missing",
                    OAuthError.TokenResponse.INVALID_REQUEST);
        }

        Map<String, Object> attributes = new HashMap<>();
        if (scopes != null && !scopes.isEmpty()) {
            attributes.put(Attributes.SCOPES, scopes);
        }
        authenticationManager.authenticate(
                config.getOAuth2passwordAuthentication(), username, password,
                attributes);
    }

    /** Clients must authenticate
     * 
     * @param clientId client_id parameter, required
     * @param clientSecret client_secret parameter, required
     * @param scopes
     * @return
     * @throws KustvaktException
     * @throws OAuthSystemException
     */
    private OAuthResponse requestAccessTokenWithClientCredentials (
            String clientId, String clientSecret, Set<String> scopes)
            throws KustvaktException, OAuthSystemException {

        if (clientSecret == null || clientSecret.isEmpty()) {
            throw new KustvaktException(
                    StatusCodes.CLIENT_AUTHENTICATION_FAILED,
                    "Missing parameters: client_secret", "invalid_request");
        }

        clientService.authenticateClient(clientId, clientSecret);
        return createsAccessTokenResponse();
    }



    /** Creates an OAuthResponse containing an access token and a refresh token 
     *  with type Bearer.
     * 
     * @return an OAuthResponse containing an access token
     * @throws OAuthSystemException
     */
    private OAuthResponse createsAccessTokenResponse ()
            throws OAuthSystemException {
        String accessToken = oauthIssuer.accessToken();
        String refreshToken = oauthIssuer.refreshToken();

        OAuthResponse r =
                OAuthASResponse.tokenResponse(Status.OK.getStatusCode())
                        .setAccessToken(accessToken)
                        .setTokenType(TokenType.BEARER.toString())
                        .setExpiresIn(String.valueOf(config.getTokenTTL()))
                        .setRefreshToken(refreshToken).buildJSONMessage();
        // scope
        return r;
    }
}
