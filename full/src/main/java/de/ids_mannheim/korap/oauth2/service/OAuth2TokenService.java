package de.ids_mannheim.korap.oauth2.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.FullConfiguration;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.interfaces.AuthenticationManagerIface;
import de.ids_mannheim.korap.oauth2.constant.OAuth2Error;
import de.ids_mannheim.korap.oauth2.entity.Authorization;
import de.ids_mannheim.korap.oauth2.entity.OAuth2Client;

/**
 * OAuth2TokenService manages business logic related to OAuth2
 * requesting and creating access token.
 * 
 * @author margaretha
 *
 */
@Service
public class OAuth2TokenService {

    @Autowired
    private OAuth2ClientService clientService;

    @Autowired
    private OAuth2AuthorizationService authorizationService;

    @Autowired
    protected OAuth2ScopeService scopeService;

    @Autowired
    protected FullConfiguration config;
    @Autowired
    private AuthenticationManagerIface authenticationManager;

    /**
     * RFC 6749:
     * If the client type is confidential or the client was issued
     * client credentials, the client MUST authenticate with the
     * authorization server.
     * 
     * @param authorizationCode
     * @param redirectURI
     *            required if included in the authorization request
     * @param clientId
     *            required if there is no authorization header
     * @param clientSecret
     *            clilent_secret, required if client_secret was issued
     *            for the client in client registration.
     * @return an authorization
     * @throws OAuthSystemException
     * @throws KustvaktException
     */
    protected Authorization requestAccessTokenWithAuthorizationCode (
            String authorizationCode, String redirectURI, String clientId,
            String clientSecret) throws KustvaktException {

        Authorization authorization =
                authorizationService.retrieveAuthorization(authorizationCode);
        try {
            clientService.authenticateClient(clientId, clientSecret);
            authorization = authorizationService
                    .verifyAuthorization(authorization, clientId, redirectURI);
        }
        catch (KustvaktException e) {
            authorizationService.addTotalAttempts(authorization);
            throw e;
        }
        return authorization;
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
     * 
     * @param username
     *            username, required
     * @param password
     *            user password, required
     * @param scopes
     * @param clientId
     *            client_id, required
     * @param clientSecret
     *            client_secret, required if client_secret was issued
     *            for the client in client registration.
     * @return an OAuthResponse containing an access token if
     *         successful
     * @throws KustvaktException
     * @throws OAuthSystemException
     */
    protected void requestAccessTokenWithPassword (String username,
            String password, Set<String> scopes, String clientId,
            String clientSecret) throws KustvaktException {

        OAuth2Client client =
                clientService.authenticateClient(clientId, clientSecret);
        if (!client.isNative()) {
            throw new KustvaktException(StatusCodes.CLIENT_AUTHORIZATION_FAILED,
                    "Password grant is not allowed for third party clients",
                    OAuth2Error.UNAUTHORIZED_CLIENT);
        }

        authenticateUser(username, password, scopes);
        // verify or limit scopes ?
    }

    public void authenticateUser (String username, String password,
            Set<String> scopes) throws KustvaktException {
        if (username == null || username.isEmpty()) {
            throw new KustvaktException(StatusCodes.MISSING_PARAMETER,
                    "username is missing.", OAuth2Error.INVALID_REQUEST);
        }
        if (password == null || password.isEmpty()) {
            throw new KustvaktException(StatusCodes.MISSING_PARAMETER,
                    "password is missing", OAuth2Error.INVALID_REQUEST);
        }

        Map<String, Object> attributes = new HashMap<>();
        if (scopes != null && !scopes.isEmpty()) {
            attributes.put(Attributes.SCOPES, scopes);
        }
        authenticationManager.authenticate(
                config.getOAuth2passwordAuthentication(), username, password,
                attributes);
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
     * @return an OAuthResponse containing an access token if
     *         successful
     * @throws KustvaktException
     * @throws OAuthSystemException
     */
    protected Set<String> requestAccessTokenWithClientCredentials (
            String clientId, String clientSecret, Set<String> scopes)
            throws KustvaktException {

        if (clientSecret == null || clientSecret.isEmpty()) {
            throw new KustvaktException(
                    StatusCodes.CLIENT_AUTHENTICATION_FAILED,
                    "Missing parameters: client_secret",
                    OAuth2Error.INVALID_REQUEST);
        }

        // OAuth2Client client =
        clientService.authenticateClient(clientId, clientSecret);

        // if (client.isNative()) {
        // throw new KustvaktException(
        // StatusCodes.CLIENT_AUTHENTICATION_FAILED,
        // "Client credentials grant is not allowed for third party
        // clients",
        // OAuth2Error.UNAUTHORIZED_CLIENT);
        // }

        scopes = scopeService.filterScopes(scopes,
                config.getClientCredentialsScopes());
        return scopes;
    }



}
