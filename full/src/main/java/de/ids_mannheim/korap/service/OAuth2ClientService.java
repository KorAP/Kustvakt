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


    public void deregisterClient (String clientId, String username)
            throws KustvaktException {

        OAuth2Client client = clientDao.retrieveClientById(clientId);
        if (adminDao.isAdmin(username)) {
            clientDao.deregisterClient(client);
        }
        else if (client.getType().equals(OAuth2ClientType.CONFIDENTIAL)) {
            throw new KustvaktException(
                    StatusCodes.CLIENT_DEREGISTRATION_FAILED,
                    "Service is limited to public clients.");
        }
        else if (client.getRegisteredBy().equals(username)) {
            clientDao.deregisterClient(client);
        }
        else {
            throw new KustvaktException(StatusCodes.AUTHORIZATION_FAILED,
                    "Unauthorized operation for user: " + username, username);
        }
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
     * @param client_id
     * @return
     * @throws KustvaktException
     */
    public OAuth2Client authenticateClient (String authorization,
            GrantType grantType, String client_id) throws KustvaktException {

        OAuth2Client client = clientDao.retrieveClientById(client_id);

        if (authorization == null || authorization.isEmpty()) {
            if (client.getType().equals(OAuth2ClientType.CONFIDENTIAL)
                    || grantType.equals(GrantType.CLIENT_CREDENTIALS)) {
                new KustvaktException(StatusCodes.CLIENT_AUTHENTICATION_FAILED,
                        "Authorization header is not found.");
            }
            // OAuth2 does not require client authentication
        }
        else {
            AuthorizationData authData = authorizationHandler
                    .parseAuthorizationHeaderValue(authorization);
            if (authData.getAuthenticationScheme()
                    .equals(AuthenticationScheme.BASIC)) {
                if (!client.getSecret().equals(authData.getPassword())) {
                    new KustvaktException(
                            StatusCodes.CLIENT_AUTHENTICATION_FAILED,
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
