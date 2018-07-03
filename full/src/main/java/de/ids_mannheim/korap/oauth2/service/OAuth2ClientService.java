package de.ids_mannheim.korap.oauth2.service;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.SQLException;

import org.apache.commons.validator.routines.UrlValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import de.ids_mannheim.korap.config.FullConfiguration;
import de.ids_mannheim.korap.dao.AdminDao;
import de.ids_mannheim.korap.dto.OAuth2ClientDto;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.interfaces.EncryptionIface;
import de.ids_mannheim.korap.oauth2.constant.OAuth2ClientType;
import de.ids_mannheim.korap.oauth2.constant.OAuth2Error;
import de.ids_mannheim.korap.oauth2.dao.OAuth2ClientDao;
import de.ids_mannheim.korap.oauth2.entity.OAuth2Client;
import de.ids_mannheim.korap.web.input.OAuth2ClientJson;

/**
 * According to RFC 6749, an authorization server MUST:
 * <ul>
 * <li>
 * require client authentication for confidential clients or for any
 * client that was issued client credentials (or with other
 * authentication
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

    // private Logger jlog =
    // Logger.getLogger(OAuth2ClientService.class);

    @Autowired
    private OAuth2ClientDao clientDao;
    @Autowired
    private AdminDao adminDao;
    @Autowired
    private UrlValidator redirectURIValidator;
    @Autowired
    private UrlValidator urlValidator;
    @Autowired
    private EncryptionIface encryption;
    @Autowired
    private FullConfiguration config;

    public OAuth2ClientDto registerClient (OAuth2ClientJson clientJson,
            String registeredBy) throws KustvaktException {
        String url = clientJson.getUrl();
        int urlHashCode = 0;
        if (url != null && !url.isEmpty()) {
            urlHashCode = clientJson.getUrl().hashCode();
            if (!redirectURIValidator.isValid(url)) {
                throw new KustvaktException(StatusCodes.INVALID_ARGUMENT,
                        url + " is invalid.", OAuth2Error.INVALID_REQUEST);
            }
        }

        String redirectURI = clientJson.getRedirectURI();
        if (redirectURI != null && !redirectURI.isEmpty()
                && !urlValidator.isValid(redirectURI)) {
            throw new KustvaktException(StatusCodes.INVALID_ARGUMENT,
                    redirectURI + " is invalid.", OAuth2Error.INVALID_REQUEST);
        }

        boolean isNative = isNativeClient(url, redirectURI);

        String secret = null;
        String secretHashcode = null;
        if (clientJson.getType().equals(OAuth2ClientType.CONFIDENTIAL)) {
            // RFC 6749:
            // The authorization server MUST NOT issue client
            // passwords or other client credentials to native
            // application (clients installed and executed on the
            // device used by the resource owner e.g. desktop
            // application, native mobile application) or
            // user-agent-based application clients for client
            // authentication. The authorization server MAY issue a
            // client password or other credentials for a specific
            // installation of a native application client on a
            // specific device.

            secret = encryption.createToken();
            secretHashcode = encryption.secureHash(secret,
                    config.getPasscodeSaltField());
        }

        String id = encryption.createRandomNumber();
        try {
            clientDao.registerClient(id, secretHashcode, clientJson.getName(),
                    clientJson.getType(), isNative, url, urlHashCode,
                    redirectURI, registeredBy, clientJson.getDescription());
        }
        catch (Exception e) {
            Throwable cause = e;
            Throwable lastCause = null;
            while ((cause = cause.getCause()) != null
                    && !cause.equals(lastCause)) {
                if (cause instanceof SQLException) {
                    break;
                }
                lastCause = cause;
            }
            throw new KustvaktException(StatusCodes.CLIENT_REGISTRATION_FAILED,
                    cause.getMessage(), OAuth2Error.INVALID_REQUEST);
        }

        return new OAuth2ClientDto(id, secret);
    }


    private boolean isNativeClient (String url, String redirectURI)
            throws KustvaktException {
        if (url == null || url.isEmpty() || redirectURI == null
                || redirectURI.isEmpty()) {
            return false;
        }

        String nativeHost = config.getNativeClientHost();
        String urlHost = null;
        try {
            urlHost = new URL(url).getHost();
        }
        catch (MalformedURLException e) {
            throw new KustvaktException(StatusCodes.INVALID_ARGUMENT,
                    "Invalid url :" + e.getMessage(),
                    OAuth2Error.INVALID_REQUEST);
        }

        if (!urlHost.equals(nativeHost)) {
            return false;
        }

        String uriHost = null;
        try {
            uriHost = new URI(redirectURI).getHost();
        }
        catch (URISyntaxException e) {
            throw new KustvaktException(StatusCodes.INVALID_ARGUMENT,
                    "Invalid redirectURI: " + e.getMessage(),
                    OAuth2Error.INVALID_REQUEST);
        }
        if (!uriHost.equals(nativeHost)) {
            return false;
        }

        return true;
    }


    public void deregisterClient (String clientId, String clientSecret,
            String username) throws KustvaktException {

        OAuth2Client client = clientDao.retrieveClientById(clientId);
        if (client.getType().equals(OAuth2ClientType.CONFIDENTIAL)) {
            authenticateClient(clientId, clientSecret);
        }

        if (adminDao.isAdmin(username)
                || client.getRegisteredBy().equals(username)) {
            clientDao.deregisterClient(client);
        }
        else {
            throw new KustvaktException(StatusCodes.AUTHORIZATION_FAILED,
                    "Unauthorized operation for user: " + username, username);
        }
    }

    public OAuth2Client authenticateClient (String clientId,
            String clientSecret) throws KustvaktException {

        if (clientId == null || clientId.isEmpty()) {
            throw new KustvaktException(
                    StatusCodes.CLIENT_AUTHENTICATION_FAILED,
                    "Missing parameters: client id",
                    OAuth2Error.INVALID_REQUEST);
        }

        OAuth2Client client = clientDao.retrieveClientById(clientId);
        authenticateClient(client, clientSecret);
        return client;
    }

    public void authenticateClient (OAuth2Client client, String clientSecret)
            throws KustvaktException {
        if (clientSecret == null) {
            if (client.getType().equals(OAuth2ClientType.CONFIDENTIAL)) {
                throw new KustvaktException(
                        StatusCodes.CLIENT_AUTHENTICATION_FAILED,
                        "Missing parameters: client_secret",
                        OAuth2Error.INVALID_REQUEST);
            }
        }
        else if (clientSecret.isEmpty()) {
            if (client.getType().equals(OAuth2ClientType.CONFIDENTIAL)) {
                throw new KustvaktException(
                        StatusCodes.CLIENT_AUTHENTICATION_FAILED,
                        "Missing parameters: client_secret",
                        OAuth2Error.INVALID_REQUEST);
            }
        }
        else if (!encryption.checkHash(clientSecret, client.getSecret(),
                config.getPasscodeSaltField())) {
            throw new KustvaktException(
                    StatusCodes.CLIENT_AUTHENTICATION_FAILED,
                    "Invalid client credentials", OAuth2Error.INVALID_CLIENT);
        }
    }


    public OAuth2Client authenticateClientId (String clientId)
            throws KustvaktException {
        if (clientId == null || clientId.isEmpty()) {
            throw new KustvaktException(
                    StatusCodes.CLIENT_AUTHENTICATION_FAILED,
                    "Missing parameters: client id",
                    OAuth2Error.INVALID_REQUEST);
        }

        return clientDao.retrieveClientById(clientId);
    }
}
