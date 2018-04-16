package de.ids_mannheim.korap.service;

import java.sql.SQLException;

import org.apache.commons.validator.routines.UrlValidator;
import org.apache.oltu.oauth2.common.error.OAuthError;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import de.ids_mannheim.korap.authentication.http.AuthorizationData;
import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.config.FullConfiguration;
import de.ids_mannheim.korap.constant.AuthenticationScheme;
import de.ids_mannheim.korap.constant.OAuth2ClientType;
import de.ids_mannheim.korap.dao.AdminDao;
import de.ids_mannheim.korap.dao.OAuth2ClientDao;
import de.ids_mannheim.korap.dto.OAuth2ClientDto;
import de.ids_mannheim.korap.entity.OAuth2Client;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.interfaces.EncryptionIface;
import de.ids_mannheim.korap.web.input.OAuth2ClientJson;

/** According to RFC 6749, an authorization server MUST: 
 * <ul>
 * <li>
 * require client authentication for confidential clients or for any
 * client that was issued client credentials (or with other authentication 
 * requirements),
 * </li>
 * 
 * <li>authenticate the client if client authentication is included
 * </li>
 * </ul>
 * 
 * @author margaretha
 *
 */
@Service
public class OAuth2ClientService {

    @Autowired
    private OAuth2ClientDao clientDao;
    @Autowired
    private AdminDao adminDao;
    @Autowired
    private UrlValidator urlValidator;
    @Autowired
    private UrlValidator httpsValidator;
    @Autowired
    private EncryptionIface encryption;
    @Autowired
    private HttpAuthorizationHandler authorizationHandler;
    @Autowired
    private FullConfiguration config;


    public OAuth2ClientDto registerClient (OAuth2ClientJson clientJson,
            String registeredBy) throws KustvaktException {
        if (!urlValidator.isValid(clientJson.getUrl())) {
            throw new KustvaktException(StatusCodes.INVALID_ARGUMENT,
                    clientJson.getUrl() + " is invalid.",
                    OAuthError.TokenResponse.INVALID_REQUEST);
        }
        if (!httpsValidator.isValid(clientJson.getRedirectURI())) {
            throw new KustvaktException(StatusCodes.HTTPS_REQUIRED,
                    clientJson.getRedirectURI()
                            + " is invalid. RedirectURI requires https.",
                    OAuthError.TokenResponse.INVALID_REQUEST);
        }

        String secret = null;
        String secretHashcode = null;
        if (clientJson.getType().equals(OAuth2ClientType.CONFIDENTIAL)) {
            // RFC 6749:
            // The authorization server MUST NOT issue client passwords or other
            // client credentials to native application (clients installed and 
            // executed on the device used by the resource owner e.g. desktop  
            // application, native mobile application) or user-agent-based
            // application clients for client authentication.  The authorization
            // server MAY issue a client password or other credentials
            // for a specific installation of a native application client on a
            // specific device.

            secret = encryption.createToken();
            secretHashcode = encryption.secureHash(secret,
                    config.getPasscodeSaltField());
        }

        String id = encryption.createRandomNumber();
        try {
            clientDao.registerClient(id, secretHashcode, clientJson.getName(),
                    clientJson.getType(), clientJson.getUrl(),
                    clientJson.getUrl().hashCode(), clientJson.getRedirectURI(),
                    registeredBy);
        }
        catch (Exception e) {
            Throwable cause = e;
            Throwable lastCause = null;
            while ((cause = cause.getCause()) != null
                    && !cause.equals(lastCause)) {
                if (cause instanceof SQLException) {
                    throw new KustvaktException(
                            StatusCodes.CLIENT_REGISTRATION_FAILED,
                            cause.getMessage(),
                            OAuthError.TokenResponse.INVALID_REQUEST);
                }
                lastCause = cause;
            }
        }

        return new OAuth2ClientDto(id, secret);
    }


    public void deregisterPublicClient (String clientId, String username)
            throws KustvaktException {

        OAuth2Client client = retrieveClientById(clientId);
        if (adminDao.isAdmin(username)) {
            clientDao.deregisterClient(client);
        }
        else if (client.getType().equals(OAuth2ClientType.CONFIDENTIAL)) {
            throw new KustvaktException(
                    StatusCodes.CLIENT_DEREGISTRATION_FAILED,
                    "Service is limited to public clients. To deregister "
                            + "confidential clients, use service at path: "
                            + "oauth2/client/deregister/confidential.",
                    OAuthError.TokenResponse.INVALID_REQUEST);
        }
        else if (client.getRegisteredBy().equals(username)) {
            clientDao.deregisterClient(client);
        }
        else {
            throw new KustvaktException(StatusCodes.AUTHORIZATION_FAILED,
                    "Unauthorized operation for user: " + username, username);
        }
    }


    public void deregisterConfidentialClient (String authorization,
            String clientId) throws KustvaktException {
        OAuth2Client client =
                authenticateClientByBasicAuthorization(authorization, clientId);
        clientDao.deregisterClient(client);
    }

    public OAuth2Client authenticateClientById (String clientId)
            throws KustvaktException {
        if (clientId == null || clientId.isEmpty()) {
            throw new KustvaktException(
                    StatusCodes.CLIENT_AUTHENTICATION_FAILED,
                    "client_id is missing.",
                    OAuthError.TokenResponse.INVALID_CLIENT);
        }
        else {
            return retrieveClientById(clientId);
        }
    }

    public OAuth2Client authenticateClientByBasicAuthorization (
            String authorization, String clientId) throws KustvaktException {

        if (authorization == null || authorization.isEmpty()) {
            throw new KustvaktException(
                    StatusCodes.CLIENT_AUTHENTICATION_FAILED,
                    "Authorization header is not found.",
                    OAuthError.TokenResponse.INVALID_CLIENT);
        }
        else {
            AuthorizationData authData = authorizationHandler
                    .parseAuthorizationHeaderValue(authorization);
            if (authData.getAuthenticationScheme()
                    .equals(AuthenticationScheme.BASIC)) {
                authorizationHandler.parseBasicToken(authData);
                return verifyClientCredentials(clientId, authData);
            }
            else {
                throw new KustvaktException(
                        StatusCodes.CLIENT_AUTHENTICATION_FAILED,
                        "Client authentication with " + authData
                                .getAuthenticationScheme().displayName()
                                + "is not supported",
                        "invalid_client");
            }
        }
    }

    private OAuth2Client verifyClientCredentials (String clientId,
            AuthorizationData authData) throws KustvaktException {

        OAuth2Client client = retrieveClientById(authData.getUsername());
        // EM: not sure if this is necessary
        if (clientId != null && !clientId.isEmpty()) {
            if (!client.getId().equals(clientId)) {
                throw new KustvaktException(
                        StatusCodes.CLIENT_AUTHENTICATION_FAILED,
                        "Invalid client credentials.",
                        OAuthError.TokenResponse.INVALID_CLIENT);
            }
        }
        if (!encryption.checkHash(authData.getPassword(), client.getSecret(),
                config.getPasscodeSaltField())) {
            throw new KustvaktException(
                    StatusCodes.CLIENT_AUTHENTICATION_FAILED,
                    "Invalid client credentials.",
                    OAuthError.TokenResponse.INVALID_CLIENT);
        }
        return client;
    }

    public OAuth2Client retrieveClientById (String clientId)
            throws KustvaktException {
        return clientDao.retrieveClientById(clientId);
    }
}
