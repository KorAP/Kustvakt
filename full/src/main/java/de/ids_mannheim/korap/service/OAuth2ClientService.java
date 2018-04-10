package de.ids_mannheim.korap.service;

import java.sql.SQLException;

import org.apache.commons.validator.routines.UrlValidator;
import org.apache.oltu.oauth2.common.message.types.GrantType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import de.ids_mannheim.korap.authentication.http.AuthorizationData;
import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.constant.AuthenticationScheme;
import de.ids_mannheim.korap.constant.OAuth2ClientType;
import de.ids_mannheim.korap.dao.AdminDao;
import de.ids_mannheim.korap.dao.OAuth2ClientDao;
import de.ids_mannheim.korap.dto.OAuth2ClientDto;
import de.ids_mannheim.korap.entity.OAuth2Client;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.interfaces.EncryptionIface;
import de.ids_mannheim.korap.security.context.TokenContext;
import de.ids_mannheim.korap.web.input.OAuth2ClientJson;

@Service
public class OAuth2ClientService {

    @Autowired
    private OAuth2ClientDao clientDao;
    @Autowired
    private AdminDao adminDao;
    @Autowired
    private UrlValidator urlValidator;
    @Autowired
    private EncryptionIface encryption;
    @Autowired
    private HttpAuthorizationHandler authorizationHandler;


    public OAuth2ClientDto registerClient (OAuth2ClientJson clientJson,
            String registeredBy) throws KustvaktException {
        if (!urlValidator.isValid(clientJson.getUrl())) {
            throw new KustvaktException(StatusCodes.INVALID_ARGUMENT,
                    clientJson.getUrl() + " is invalid.", clientJson.getUrl());
        }
        if (!urlValidator.isValid(clientJson.getRedirectURI())) {
            throw new KustvaktException(StatusCodes.INVALID_ARGUMENT,
                    clientJson.getRedirectURI() + " is invalid.",
                    clientJson.getRedirectURI());
        }

        String secret = null;
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
        }

        String id = encryption.createRandomNumber();
        try {
            clientDao.registerClient(id, secret, clientJson.getName(),
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
                            cause.getMessage(), cause);
                }
                lastCause = cause;
            }
        }

        return new OAuth2ClientDto(id, secret);
    }


    public void deregisterPublicClient (String clientId, String username)
            throws KustvaktException {

        OAuth2Client client = clientDao.retrieveClientById(clientId);
        if (adminDao.isAdmin(username)) {
            clientDao.deregisterClient(client);
        }
        else if (client.getType().equals(OAuth2ClientType.CONFIDENTIAL)) {
            throw new KustvaktException(
                    StatusCodes.CLIENT_DEREGISTRATION_FAILED,
                    "Service is limited to public clients. To deregister "
                            + "confidential clients, use service at path: "
                            + "oauth2/client/deregister/confidential.");
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
        OAuth2Client client = authenticateClient(authorization, null, clientId);
        clientDao.deregisterClient(client);
    }

    public TokenContext requestAccessTokenByClientCredentials (
            String authorization, String grantType) throws KustvaktException {

        return null;
    }

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
     * @param authorization
     * @param grantType
     * @param clientId
     * @return
     * @throws KustvaktException
     */
    public OAuth2Client authenticateClient (String authorization,
            GrantType grantType, String clientId) throws KustvaktException {

        OAuth2Client client = clientDao.retrieveClientById(clientId);

        if (authorization == null || authorization.isEmpty()) {
            if (client.getType().equals(OAuth2ClientType.CONFIDENTIAL)
                    || grantType.equals(GrantType.CLIENT_CREDENTIALS)) {
                throw new KustvaktException(StatusCodes.AUTHENTICATION_FAILED,
                        "Authorization header is not found.");
            }
            // OAuth2 does not require client authentication
        }
        else {
            AuthorizationData authData = authorizationHandler
                    .parseAuthorizationHeaderValue(authorization);
            if (authData.getAuthenticationScheme()
                    .equals(AuthenticationScheme.BASIC)) {
                authorizationHandler.parseBasicToken(authData);
                if (!client.getId().equals(clientId)
                        || !client.getSecret().equals(authData.getPassword())) {
                    throw new KustvaktException(
                            StatusCodes.AUTHENTICATION_FAILED,
                            "Client credentials are incorrect.");
                }
            }
            else {
                throw new KustvaktException(
                        StatusCodes.UNSUPPORTED_AUTHENTICATION_SCHEME,
                        authData.getAuthenticationScheme().displayName()
                                + "is unsupported for client authentication.",
                        authData.getAuthenticationScheme().displayName());
            }
        }
        return client;
    }

}
