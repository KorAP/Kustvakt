package de.ids_mannheim.korap.service;

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

import de.ids_mannheim.korap.config.FullConfiguration;
import de.ids_mannheim.korap.constant.OAuth2ClientType;
import de.ids_mannheim.korap.entity.OAuth2Client;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;

@Service
public class OAuth2Service {

    @Autowired
    private OAuth2ClientService clientService;
    @Autowired
    private FullConfiguration config;


    /** 
     *  RFC 6749:
     *  
     *  If the client type is confidential or the client was issued client
     *  credentials, the client MUST authenticate with the authorization server.
     * @param request 
     *  
     * @param authorization
     * @param grantType
     * @param scope 
     * @param password 
     * @param username 
     * @param clientId required for authorization_code grant, otherwise optional
     * @param redirectURI 
     * @param authorizationCode 
     * @return 
     * @throws KustvaktException
     * @throws OAuthProblemException 
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
                    oAuthRequest.getScopes());
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
        OAuth2Client client;
        if (authorization == null || authorization.isEmpty()) {
            client = clientService.authenticateClientById(clientId);
            if (client.getType().equals(OAuth2ClientType.CONFIDENTIAL)) {
                throw new KustvaktException(
                        StatusCodes.CLIENT_AUTHENTICATION_FAILED,
                        "Client authentication using authorization header is required.",
                        OAuthError.TokenResponse.INVALID_CLIENT);
            }
        }
        else {
            client = clientService.authenticateClientByBasicAuthorization(
                    authorization, clientId);
        }

        // TODO
        return null;
    }

    /** Confidential clients must authenticate
     * 
     * @param authorization
     * @param username
     * @param password
     * @param scopes
     * @return
     */
    private OAuthResponse requestAccessTokenWithPassword (String authorization,
            String username, String password, Set<String> scopes) {



        return null;
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
                .setExpiresIn(String.valueOf(config.getLongTokenTTL()))
                .setRefreshToken(refreshToken).buildJSONMessage();
        // scope
        return r;
    }
}
