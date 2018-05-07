package de.ids_mannheim.korap.oauth2.service;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.SQLException;

import org.apache.commons.validator.routines.UrlValidator;
import org.apache.log4j.Logger;
import org.apache.oltu.oauth2.as.request.OAuthRequest;
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

    private Logger jlog = Logger.getLogger(OAuth2ClientService.class);

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
    private FullConfiguration config;

    public OAuth2ClientDto registerClient (OAuth2ClientJson clientJson,
            String registeredBy) throws KustvaktException {
        if (!urlValidator.isValid(clientJson.getUrl())) {
            throw new KustvaktException(StatusCodes.INVALID_ARGUMENT,
                    clientJson.getUrl() + " is invalid.",
                    OAuth2Error.INVALID_REQUEST);
        }
        if (!httpsValidator.isValid(clientJson.getRedirectURI())) {
            throw new KustvaktException(StatusCodes.HTTPS_REQUIRED,
                    clientJson.getRedirectURI()
                            + " is invalid. RedirectURI requires https.",
                    OAuth2Error.INVALID_REQUEST);
        }

        boolean isNative = isNativeClient(clientJson.getUrl(),
                clientJson.getRedirectURI());

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
                    clientJson.getType(), isNative, clientJson.getUrl(),
                    clientJson.getUrl().hashCode(), clientJson.getRedirectURI(),
                    registeredBy, clientJson.getDescription());
        }
        catch (Exception e) {
            Throwable cause = e;
            Throwable lastCause = null;
            while ((cause = cause.getCause()) != null
                    && !cause.equals(lastCause)) {
                if (cause instanceof SQLException) {
                    throw new KustvaktException(
                            StatusCodes.CLIENT_REGISTRATION_FAILED,
                            cause.getMessage(), OAuth2Error.INVALID_REQUEST);
                }
                lastCause = cause;
            }
        }

        return new OAuth2ClientDto(id, secret);
    }


    private boolean isNativeClient (String url, String redirectURI)
            throws KustvaktException {
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
        String uriHost = null;
        try {
            uriHost = new URI(redirectURI).getHost();
        }
        catch (URISyntaxException e) {
            throw new KustvaktException(StatusCodes.INVALID_ARGUMENT,
                    "Invalid redirectURI: " + e.getMessage(),
                    OAuth2Error.INVALID_REQUEST);
        }
        boolean isNative =
                urlHost.equals(nativeHost) && uriHost.equals(nativeHost);
        jlog.debug(urlHost + " " + uriHost + " " + isNative);
        return isNative;
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
                            + "oauth2/client/deregister/confidential.",
                    OAuth2Error.INVALID_REQUEST);
        }
        else if (client.getRegisteredBy().equals(username)) {
            clientDao.deregisterClient(client);
        }
        else {
            throw new KustvaktException(StatusCodes.AUTHORIZATION_FAILED,
                    "Unauthorized operation for user: " + username, username);
        }
    }


    public void deregisterConfidentialClient (OAuthRequest oAuthRequest)
            throws KustvaktException {

        OAuth2Client client = authenticateClient(oAuthRequest.getClientId(),
                oAuthRequest.getClientSecret());
        clientDao.deregisterClient(client);
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
        if (clientSecret == null || clientSecret.isEmpty()) {
            if (client.getSecret() != null
                    || client.getType().equals(OAuth2ClientType.CONFIDENTIAL)) {
                throw new KustvaktException(
                        StatusCodes.CLIENT_AUTHENTICATION_FAILED,
                        "Missing parameters: client_secret",
                        OAuth2Error.INVALID_REQUEST);
            }
            else
                return client;
        }
        else {
            if (client.getSecret() != null) {
                if (encryption.checkHash(clientSecret, client.getSecret(),
                        config.getPasscodeSaltField())) {
                    return client;
                }
            }
        }

        throw new KustvaktException(StatusCodes.CLIENT_AUTHENTICATION_FAILED,
                "Invalid client credentials", OAuth2Error.INVALID_CLIENT);
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
