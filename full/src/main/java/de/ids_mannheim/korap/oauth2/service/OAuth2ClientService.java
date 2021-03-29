package de.ids_mannheim.korap.oauth2.service;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.validator.routines.UrlValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import de.ids_mannheim.korap.config.FullConfiguration;
import de.ids_mannheim.korap.dao.AdminDao;
import de.ids_mannheim.korap.encryption.RandomCodeGenerator;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.interfaces.EncryptionIface;
import de.ids_mannheim.korap.oauth2.constant.OAuth2ClientType;
import de.ids_mannheim.korap.oauth2.constant.OAuth2Error;
import de.ids_mannheim.korap.oauth2.dao.AccessTokenDao;
import de.ids_mannheim.korap.oauth2.dao.AuthorizationDao;
import de.ids_mannheim.korap.oauth2.dao.OAuth2ClientDao;
import de.ids_mannheim.korap.oauth2.dao.RefreshTokenDao;
import de.ids_mannheim.korap.oauth2.dto.OAuth2ClientDto;
import de.ids_mannheim.korap.oauth2.dto.OAuth2ClientInfoDto;
import de.ids_mannheim.korap.oauth2.dto.OAuth2UserClientDto;
import de.ids_mannheim.korap.oauth2.entity.AccessToken;
import de.ids_mannheim.korap.oauth2.entity.Authorization;
import de.ids_mannheim.korap.oauth2.entity.OAuth2Client;
import de.ids_mannheim.korap.oauth2.entity.RefreshToken;
import de.ids_mannheim.korap.utils.ParameterChecker;
import de.ids_mannheim.korap.web.input.OAuth2ClientJson;

/**
 * Defines business logic related to OAuth2 client including
 * client registration and client authentication.
 * 
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

    @Autowired
    private OAuth2ClientDao clientDao;
    @Autowired
    private AccessTokenDao tokenDao;
    @Autowired
    private RefreshTokenDao refreshDao;
    @Autowired
    private AuthorizationDao authorizationDao;
    @Autowired
    private AdminDao adminDao;
    @Autowired
    private UrlValidator redirectURIValidator;
    @Autowired
    private UrlValidator urlValidator;
    @Autowired
    private EncryptionIface encryption;
    @Autowired
    private RandomCodeGenerator codeGenerator;
    @Autowired
    private FullConfiguration config;

    public OAuth2ClientDto registerClient (OAuth2ClientJson clientJson,
            String registeredBy) throws KustvaktException {
        
        ParameterChecker.checkNameValue(clientJson.getName(), "clientName");
        
        String url = clientJson.getUrl();
        if (url != null && !url.isEmpty()) {
            if (!urlValidator.isValid(url)) {
                throw new KustvaktException(StatusCodes.INVALID_ARGUMENT,
                        url + " is invalid.", OAuth2Error.INVALID_REQUEST);
            }
        }

        String redirectURI = clientJson.getRedirectURI();
        if (redirectURI != null && !redirectURI.isEmpty()
                && !redirectURIValidator.isValid(redirectURI)) {
            throw new KustvaktException(StatusCodes.INVALID_ARGUMENT,
                    redirectURI + " is invalid.", OAuth2Error.INVALID_REQUEST);
        }

        // boolean isNative = isNativeClient(url, redirectURI);

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

            secret = codeGenerator.createRandomCode();
            secretHashcode = encryption.secureHash(secret);
        }

        String id = codeGenerator.createRandomCode();
        try {
            clientDao.registerClient(id, secretHashcode, clientJson.getName(),
                    clientJson.getType(), url, redirectURI, registeredBy,
                    clientJson.getDescription());
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

    @Deprecated
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

    public void deregisterClient (String clientId, String username)
            throws KustvaktException {

        OAuth2Client client = clientDao.retrieveClientById(clientId);
        
        if (adminDao.isAdmin(username)
                || client.getRegisteredBy().equals(username)) {

            revokeAllAuthorizationsByClientId(clientId);
            clientDao.deregisterClient(client);
        }
        else {
            throw new KustvaktException(StatusCodes.AUTHORIZATION_FAILED,
                    "Unauthorized operation for user: " + username, username);
        }
    }

    private void revokeAllAuthorizationsByClientId (String clientId)
            throws KustvaktException {

        // revoke all related authorization codes
        List<Authorization> authList =
                authorizationDao.retrieveAuthorizationsByClientId(clientId);
        for (Authorization authorization : authList) {
            authorization.setRevoked(true);
            authorizationDao.updateAuthorization(authorization);
        }

        // revoke all related access tokens
        List<AccessToken> tokens =
                tokenDao.retrieveAccessTokenByClientId(clientId,null);
        for (AccessToken token : tokens) {
            token.setRevoked(true);
            tokenDao.updateAccessToken(token);
        }

        List<RefreshToken> refreshTokens =
                refreshDao.retrieveRefreshTokenByClientId(clientId,null);
        for (RefreshToken token : refreshTokens) {
            token.setRevoked(true);
            refreshDao.updateRefreshToken(token);
        }
    }

    public OAuth2ClientDto resetSecret (String clientId, String username)
            throws KustvaktException {

        OAuth2Client client = clientDao.retrieveClientById(clientId);
        if (!client.getType().equals(OAuth2ClientType.CONFIDENTIAL)) {
            throw new KustvaktException(StatusCodes.NOT_ALLOWED,
                    "Operation is not allowed for public clients",
                    OAuth2Error.INVALID_REQUEST);
        }
        if (adminDao.isAdmin(username)
                || client.getRegisteredBy().equals(username)) {

            String secret = codeGenerator.createRandomCode();
            String secretHashcode = encryption.secureHash(secret);

            client.setSecret(secretHashcode);
            clientDao.updateClient(client);
            return new OAuth2ClientDto(clientId, secret);
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
        if (clientSecret == null || clientSecret.isEmpty()) {
            if (client.getType().equals(OAuth2ClientType.CONFIDENTIAL)) {
                throw new KustvaktException(
                        StatusCodes.CLIENT_AUTHENTICATION_FAILED,
                        "Missing parameters: client_secret",
                        OAuth2Error.INVALID_REQUEST);
            }
        }
        else if (client.getSecret() == null || client.getSecret().isEmpty()) {
            if (client.getType().equals(OAuth2ClientType.CONFIDENTIAL)) {
                throw new KustvaktException(
                        StatusCodes.CLIENT_AUTHENTICATION_FAILED,
                        "Client secret was not registered",
                        OAuth2Error.INVALID_CLIENT);
            }
        }
        else if (!encryption.checkHash(clientSecret, client.getSecret())) {
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

    public void updatePrivilege (String username, String clientId,
            boolean isSuper) throws KustvaktException {

        if (adminDao.isAdmin(username)) {
            OAuth2Client client = clientDao.retrieveClientById(clientId);
            if (isSuper) {
                if (!client.getType().equals(OAuth2ClientType.CONFIDENTIAL)) {
                    throw new KustvaktException(StatusCodes.NOT_ALLOWED,
                            "Only confidential clients are allowed to be super clients.");
                }
            }
            else {
                revokeAllAuthorizationsByClientId(clientId);
            }

            client.setSuper(isSuper);
            clientDao.updateClient(client);
        }
        else {
            throw new KustvaktException(StatusCodes.AUTHORIZATION_FAILED,
                    "Unauthorized operation for user: " + username, username);
        }
    }

    public OAuth2ClientInfoDto retrieveClientInfo (String username,
            String clientId) throws KustvaktException {
        OAuth2Client client = clientDao.retrieveClientById(clientId);
        if (adminDao.isAdmin(username)
                || username.equals(client.getRegisteredBy())) {
            return new OAuth2ClientInfoDto(client);
        }
        else {
            throw new KustvaktException(StatusCodes.AUTHORIZATION_FAILED,
                    "Unauthorized operation for user: " + username, username);
        }
    }

    public OAuth2Client retrieveClient (String clientId)
            throws KustvaktException {
        return clientDao.retrieveClientById(clientId);
    }

    public List<OAuth2UserClientDto> listUserAuthorizedClients (String username,
            String clientId, String clientSecret) throws KustvaktException {
        OAuth2Client client = authenticateClient(clientId, clientSecret);
        if (!client.isSuper()) {
            throw new KustvaktException(StatusCodes.CLIENT_AUTHORIZATION_FAILED,
                    "Only super client is allowed to list user authorized clients.",
                    OAuth2Error.UNAUTHORIZED_CLIENT);
        }
                
        List<OAuth2Client> userClients =
                clientDao.retrieveUserAuthorizedClients(username);
        userClients.addAll(clientDao.retrieveClientsByAccessTokens(username));
        
        List<String> clientIds = new ArrayList<>();
        List<OAuth2Client> uniqueClients = new ArrayList<>();
        for (OAuth2Client c : userClients){
            String id = c.getId();
            if (!clientIds.contains(id)){
                clientIds.add(id);
                uniqueClients.add(c);
            }        
        }
        
        Collections.sort(uniqueClients);
        return createClientDtos(uniqueClients);
    }
    
    public List<OAuth2UserClientDto> listUserRegisteredClients (String username,
            String clientId, String clientSecret) throws KustvaktException {
        OAuth2Client client = authenticateClient(clientId, clientSecret);
        if (!client.isSuper()) {
            throw new KustvaktException(StatusCodes.CLIENT_AUTHORIZATION_FAILED,
                    "Only super client is allowed to list user registered clients.",
                    OAuth2Error.UNAUTHORIZED_CLIENT);
        }
        List<OAuth2Client> userClients =
                clientDao.retrieveUserRegisteredClients(username);
        Collections.sort(userClients);
        return createClientDtos(userClients);
    }
    
    private List<OAuth2UserClientDto> createClientDtos (List<OAuth2Client> userClients) {
        List<OAuth2UserClientDto> dtoList = new ArrayList<>(userClients.size());
        for (OAuth2Client uc : userClients) {
            if (uc.isSuper()) continue;
            OAuth2UserClientDto dto = new OAuth2UserClientDto();
            dto.setClientId(uc.getId());
            dto.setClientName(uc.getName());
            dto.setDescription(uc.getDescription());
            dto.setUrl(uc.getUrl());
            dto.setClientType(uc.getType());
            dtoList.add(dto);
        }
        return dtoList;
    }

    public boolean isPublicClient (OAuth2Client oAuth2Client) {
        return oAuth2Client.getType().equals(OAuth2ClientType.PUBLIC);
    }
}
