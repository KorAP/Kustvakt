package de.ids_mannheim.korap.service;

import javax.servlet.http.HttpServletRequest;
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
    public OAuthResponse requestAccessToken (HttpServletRequest request,
            String authorization, String grantType, String authorizationCode,
            String redirectURI, String clientId, String username,
            String password, String scope) throws KustvaktException {

        if (grantType.equals(GrantType.AUTHORIZATION_CODE.toString())) {
            return requestAccessTokenWithAuthorizationCode(authorization,
                    authorizationCode, redirectURI, clientId);
        }
        else if (grantType.equals(GrantType.PASSWORD.toString())) {
            return requestAccessTokenWithPassword(authorization, username,
                    password, scope);
        }
        else if (grantType.equals(GrantType.CLIENT_CREDENTIALS.toString())) {
            return requestAccessTokenWithClientCredentials(authorization,
                    scope);
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
     * @param scope
     * @return
     */
    private OAuthResponse requestAccessTokenWithPassword (String authorization,
            String username, String password, String scope) {



        return null;
    }

    /** Clients must authenticate
     * 
     * @param authorization
     * @param scope
     * @return
     * @throws OAuthProblemException 
     * @throws KustvaktException 
     */
    private OAuthResponse requestAccessTokenWithClientCredentials (
            String authorization, String scope) throws KustvaktException {

        if (authorization == null || authorization.isEmpty()) {
            throw new KustvaktException(
                    StatusCodes.CLIENT_AUTHENTICATION_FAILED,
                    "Client authentication using authorization header is required.",
                    OAuthError.TokenResponse.INVALID_CLIENT);
        }
        else {
            OAuth2Client client =
                    clientService.authenticateClientByBasicAuthorization(
                            authorization, null);
            //TODO
        }
        return null;
    }


    /**
     * @param request  
     * @return 
     * @throws OAuthSystemException 
     * 
     */
    private OAuthResponse createsAccessTokenResponse (
            HttpServletRequest request) throws OAuthSystemException {
        OAuthTokenRequest oauthRequest = null;
        OAuthIssuer oauthIssuerImpl = new OAuthIssuerImpl(new MD5Generator());
        OAuthResponse r = null;
        try {
            oauthRequest = new OAuthTokenRequest(request);
            String authorizationCode = oauthRequest.getCode();

            String accessToken = oauthIssuerImpl.accessToken();
            String refreshToken = oauthIssuerImpl.refreshToken();

            r = OAuthASResponse.tokenResponse(HttpServletResponse.SC_OK)
                    .setAccessToken(accessToken)
                    .setTokenType(TokenType.BEARER.name())
                    .setExpiresIn(String.valueOf(config.getLongTokenTTL()))
                    .setRefreshToken(refreshToken).buildJSONMessage();
            // scope

        }
        catch (OAuthProblemException e) {
            r = OAuthResponse.errorResponse(HttpServletResponse.SC_UNAUTHORIZED)
                    .error(e).buildJSONMessage();
        }

        return r;
    }
}
