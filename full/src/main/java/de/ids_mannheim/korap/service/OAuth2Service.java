package de.ids_mannheim.korap.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import org.apache.oltu.oauth2.as.issuer.MD5Generator;
import org.apache.oltu.oauth2.as.issuer.OAuthIssuer;
import org.apache.oltu.oauth2.as.issuer.OAuthIssuerImpl;
import org.apache.oltu.oauth2.as.request.OAuthTokenRequest;
import org.apache.oltu.oauth2.as.response.OAuthASResponse;
import org.apache.oltu.oauth2.common.error.OAuthError;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
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

    /** 
     *  RFC 6749:
     *  
     *  If the client type is confidential or the client was issued client
     *  credentials, the client MUST authenticate with the authorization server.
     * @param request 
     *  
     * @param oAuthRequest
     * @param authorization
     * @return
     * @throws KustvaktException
     * @throws OAuthSystemException
     */
    public OAuthResponse requestAccessToken (OAuthTokenRequest oAuthRequest,
            String authorization)
            throws KustvaktException, OAuthSystemException {

        String grantType = oAuthRequest.getGrantType();

        if (grantType.equals(GrantType.AUTHORIZATION_CODE.toString())) {
            return requestAccessTokenWithAuthorizationCode(authorization,
                    oAuthRequest.getCode(), oAuthRequest.getRedirectURI(),
                    oAuthRequest.getClientId());
        }
        else if (grantType.equals(GrantType.PASSWORD.toString())) {
            return requestAccessTokenWithPassword(authorization,
                    oAuthRequest.getUsername(), oAuthRequest.getPassword(),
                    oAuthRequest.getScopes(), oAuthRequest.getClientId());
        }
        else if (grantType.equals(GrantType.CLIENT_CREDENTIALS.toString())) {
            return requestAccessTokenWithClientCredentials(authorization,
                    oAuthRequest.getScopes());
        }
        else {
            throw new KustvaktException(StatusCodes.UNSUPPORTED_GRANT_TYPE,
                    grantType + " is not supported.",
                    OAuthError.TokenResponse.UNSUPPORTED_GRANT_TYPE);
        }

    }

    /** Confidential clients must authenticate
     * 
     * @param authorization
     * @param authorizationCode
     * @param redirectURI
     * @param clientId required if there is no authorization header
     * @return
     * @throws OAuthSystemException
     * @throws KustvaktException
     * @throws OAuthProblemException 
     */
    private OAuthResponse requestAccessTokenWithAuthorizationCode (
            String authorization, String authorizationCode, String redirectURI,
            String clientId) throws KustvaktException {
        OAuth2Client client =
                clientService.authenticateClient(authorization, clientId);

        // TODO
        return null;
    }



    /**  Third party apps must not be allowed to use password grant.
     * MH: password grant is only allowed for trusted clients (korap frontend)
     *  
     * A similar rule to that of authorization code grant is additionally 
     * applied, namely client_id is required when authorization header is not 
     * available.
     * 
     * According to RFC 6749, client_id is optional for password grant, 
     * but without it, server would not be able to check the client 
     * type, thus cannot make sure that confidential clients authenticate. 
     * 
     * @param authorization
     * @param username
     * @param password
     * @param scopes
     * @param clientId
     * @return
     * @throws KustvaktException
     * @throws OAuthSystemException 
     */
    private OAuthResponse requestAccessTokenWithPassword (String authorization,
            String username, String password, Set<String> scopes,
            String clientId) throws KustvaktException, OAuthSystemException {

        OAuth2Client client =
                clientService.authenticateClient(authorization, clientId);

        if (!client.isNative()) {
            throw new KustvaktException(StatusCodes.CLIENT_AUTHORIZATION_FAILED,
                    "Password grant is not allowed for third party clients",
                    OAuthError.TokenResponse.UNAUTHORIZED_CLIENT);
        }

        authenticateUser(username, password, scopes);
        return createsAccessTokenResponse();
    }

    private void authenticateUser (String username, String password,
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
     * @param authorization
     * @param scopes
     * @param request 
     * @return
     * @throws KustvaktException 
     * @throws OAuthSystemException 
     */
    private OAuthResponse requestAccessTokenWithClientCredentials (
            String authorization, Set<String> scopes)
            throws KustvaktException, OAuthSystemException {

        if (authorization == null || authorization.isEmpty()) {
            throw new KustvaktException(
                    StatusCodes.CLIENT_AUTHENTICATION_FAILED,
                    "Client authentication using authorization header is required.",
                    OAuthError.TokenResponse.INVALID_CLIENT);
        }
        else {
            clientService.authenticateClientByBasicAuthorization(authorization,
                    null);
            return createsAccessTokenResponse();
        }
    }


    /**
     * @param request  
     * @return 
     * @throws OAuthSystemException 
     * 
     */
    private OAuthResponse createsAccessTokenResponse ()
            throws OAuthSystemException {
        OAuthIssuer oauthIssuerImpl = new OAuthIssuerImpl(new MD5Generator());
        OAuthResponse r = null;

        String accessToken = oauthIssuerImpl.accessToken();
        String refreshToken = oauthIssuerImpl.refreshToken();

        r = OAuthASResponse.tokenResponse(HttpServletResponse.SC_OK)
                .setAccessToken(accessToken)
                .setTokenType(TokenType.BEARER.toString())
                .setExpiresIn(String.valueOf(config.getTokenTTL()))
                .setRefreshToken(refreshToken).buildJSONMessage();
        // scope
        return r;
    }
}
