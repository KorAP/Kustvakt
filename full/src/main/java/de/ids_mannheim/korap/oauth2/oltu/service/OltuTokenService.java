package de.ids_mannheim.korap.oauth2.oltu.service;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.NoResultException;

import org.apache.oltu.oauth2.as.request.AbstractOAuthTokenRequest;
import org.apache.oltu.oauth2.as.response.OAuthASResponse;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.OAuthResponse;
import org.apache.oltu.oauth2.common.message.types.GrantType;
import org.apache.oltu.oauth2.common.message.types.TokenType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.unboundid.ldap.sdk.LDAPException;

import de.ids_mannheim.korap.authentication.LdapAuth3;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.constant.AuthenticationMethod;
import de.ids_mannheim.korap.encryption.RandomCodeGenerator;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.oauth2.constant.OAuth2Error;
import de.ids_mannheim.korap.oauth2.dao.AccessTokenDao;
import de.ids_mannheim.korap.oauth2.dao.RefreshTokenDao;
import de.ids_mannheim.korap.oauth2.dto.OAuth2TokenDto;
import de.ids_mannheim.korap.oauth2.entity.AccessScope;
import de.ids_mannheim.korap.oauth2.entity.AccessToken;
import de.ids_mannheim.korap.oauth2.entity.Authorization;
import de.ids_mannheim.korap.oauth2.entity.OAuth2Client;
import de.ids_mannheim.korap.oauth2.entity.RefreshToken;
import de.ids_mannheim.korap.oauth2.oltu.OAuth2RevokeAllTokenSuperRequest;
import de.ids_mannheim.korap.oauth2.oltu.OAuth2RevokeTokenRequest;
import de.ids_mannheim.korap.oauth2.oltu.OAuth2RevokeTokenSuperRequest;
import de.ids_mannheim.korap.oauth2.service.OAuth2ClientService;
import de.ids_mannheim.korap.oauth2.service.OAuth2TokenService;
import jakarta.ws.rs.core.Response.Status;

/** Implementation of token service using Apache Oltu.
 * 
 * @author margaretha
 *
 */
@Service
public class OltuTokenService extends OAuth2TokenService {

    @Autowired
    private RandomCodeGenerator randomGenerator;

    @Autowired
    private AccessTokenDao tokenDao;
    @Autowired
    private RefreshTokenDao refreshDao;
    
    @Autowired
    private OAuth2ClientService clientService;

    public OAuthResponse requestAccessToken (
            AbstractOAuthTokenRequest oAuthRequest)
            throws KustvaktException, OAuthSystemException {

        String grantType = oAuthRequest.getGrantType();

        if (grantType.equals(GrantType.AUTHORIZATION_CODE.toString())) {
            return requestAccessTokenWithAuthorizationCode(
                    oAuthRequest.getCode(), oAuthRequest.getRedirectURI(),
                    oAuthRequest.getClientId(), oAuthRequest.getClientSecret());
        }
        else if (grantType.equals(GrantType.PASSWORD.toString())) {
            return requestAccessTokenWithPassword(oAuthRequest.getClientId(),
                    oAuthRequest.getClientSecret(), oAuthRequest.getUsername(),
                    oAuthRequest.getPassword(), oAuthRequest.getScopes());
        }
        else if (grantType.equals(GrantType.CLIENT_CREDENTIALS.toString())) {
            return requestAccessTokenWithClientCredentials(
                    oAuthRequest.getClientId(), oAuthRequest.getClientSecret(),
                    oAuthRequest.getScopes());
        }
        else if (grantType.equals(GrantType.REFRESH_TOKEN.toString())) {
            return requestAccessTokenWithRefreshToken(
                    oAuthRequest.getRefreshToken(), oAuthRequest.getScopes(),
                    oAuthRequest.getClientId(), oAuthRequest.getClientSecret());
        }
        else {
            throw new KustvaktException(StatusCodes.UNSUPPORTED_GRANT_TYPE,
                    grantType + " is not supported.",
                    OAuth2Error.UNSUPPORTED_GRANT_TYPE);
        }

    }

    private boolean isInternalClient (String clientIpAddress) {
        try {
            InetAddress ip = InetAddress.getByName(clientIpAddress);
            if (ip.isSiteLocalAddress()) {
                return true;
            }
        }
        catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Revokes all access token associated with the given refresh
     * token, and creates a new access token and a new refresh
     * token with the same scopes. Thus, at one point of time,
     * there is only one active access token associated with
     * a refresh token.
     * 
     * Client authentication is done using the given client
     * credentials.
     * 
     * TODO: should create a new refresh token when the old refresh
     * token is used (DONE)
     * 
     * @param refreshTokenStr
     * @param requestScopes
     * @param clientId
     * @param clientSecret
     * @return if successful, a new access token
     * @throws KustvaktException
     * @throws OAuthSystemException
     */
    private OAuthResponse requestAccessTokenWithRefreshToken (
            String refreshTokenStr, Set<String> requestScopes, String clientId,
            String clientSecret)
            throws KustvaktException, OAuthSystemException {

        if (refreshTokenStr == null || refreshTokenStr.isEmpty()) {
            throw new KustvaktException(StatusCodes.MISSING_PARAMETER,
                    "Missing parameter: refresh_token",
                    OAuth2Error.INVALID_REQUEST);
        }

        OAuth2Client oAuth2Client = clientService.authenticateClient(clientId, clientSecret);

        RefreshToken refreshToken;
        try {
            refreshToken = refreshDao.retrieveRefreshToken(refreshTokenStr);
        }
        catch (NoResultException e) {
            throw new KustvaktException(StatusCodes.INVALID_REFRESH_TOKEN,
                    "Refresh token is not found", OAuth2Error.INVALID_GRANT);
        }

        if (!clientId.equals(refreshToken.getClient().getId())) {
            throw new KustvaktException(StatusCodes.CLIENT_AUTHORIZATION_FAILED,
                    "Client " + clientId + " is not authorized",
                    OAuth2Error.INVALID_CLIENT);
        }
        else if (refreshToken.isRevoked()) {
            throw new KustvaktException(StatusCodes.INVALID_REFRESH_TOKEN,
                    "Refresh token has been revoked",
                    OAuth2Error.INVALID_GRANT);
        }
        else if (ZonedDateTime.now(ZoneId.of(Attributes.DEFAULT_TIME_ZONE))
                .isAfter(refreshToken.getExpiryDate())) {
            throw new KustvaktException(StatusCodes.INVALID_REFRESH_TOKEN,
                    "Refresh token is expired", OAuth2Error.INVALID_GRANT);
        }

        Set<AccessScope> tokenScopes =
                new HashSet<>(refreshToken.getScopes());
        if (requestScopes != null && !requestScopes.isEmpty()) {
            tokenScopes =
                    scopeService.verifyRefreshScope(requestScopes, tokenScopes);
            requestScopes = scopeService
                    .convertAccessScopesToStringSet(tokenScopes);
        }

        // revoke the refresh token and all access tokens associated to it
        revokeRefreshToken(refreshTokenStr);

        return createsAccessTokenResponse(requestScopes, tokenScopes, clientId,
                refreshToken.getUserId(),
                refreshToken.getUserAuthenticationTime(), oAuth2Client);

        // without new refresh token
        // return createsAccessTokenResponse(scopes, requestedScopes,
        // clientId,
        // refreshToken.getUserId(),
        // refreshToken.getUserAuthenticationTime(), refreshToken);
    }

    /**
     * Issues an access token for the specified client if the
     * authorization code is valid and client successfully
     * authenticates.
     * 
     * @param code
     *            authorization code, required
     * @param redirectUri
     *            client redirect uri, required if specified in the
     *            authorization request
     * @param clientId
     *            client id, required
     * @param clientSecret
     *            client secret, required
     * @return an {@link OAuthResponse}
     * @throws OAuthSystemException
     * @throws KustvaktException
     */
    private OAuthResponse requestAccessTokenWithAuthorizationCode (String code,
            String redirectUri, String clientId, String clientSecret)
            throws OAuthSystemException, KustvaktException {
        Authorization authorization = retrieveAuthorization(code, redirectUri,
                clientId, clientSecret);

        Set<String> scopes = scopeService
                .convertAccessScopesToStringSet(authorization.getScopes());
        OAuth2Client oAuth2Client = clientService.retrieveClient(clientId);
        return createsAccessTokenResponse(scopes, authorization.getScopes(),
                authorization.getClientId(), authorization.getUserId(),
                authorization.getUserAuthenticationTime(), oAuth2Client);
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
     * TODO: FORCE client secret
     * 
     * @param clientId
     *            client_id, required
     * @param clientSecret
     *            client_secret, required if client_secret was issued
     *            for the client in client registration.
     * @param username
     *            username, required
     * @param password
     *            password, required
     * @param scopes
     *            authorization scopes, optional
     * @return an {@link OAuthResponse}
     * @throws KustvaktException
     * @throws OAuthSystemException
     */
    private OAuthResponse requestAccessTokenWithPassword (String clientId,
            String clientSecret, String username, String password,
            Set<String> scopes) throws KustvaktException, OAuthSystemException {

        OAuth2Client client =
                clientService.authenticateClient(clientId, clientSecret);
        if (!client.isSuper()) {
            throw new KustvaktException(StatusCodes.CLIENT_AUTHORIZATION_FAILED,
                    "Password grant is not allowed for third party clients",
                    OAuth2Error.UNAUTHORIZED_CLIENT);
        }

        if (scopes == null || scopes.isEmpty()) {
            scopes = new HashSet<String>(1);
            scopes.add("all");
            // scopes = config.getDefaultAccessScopes();
        }

        ZonedDateTime authenticationTime =
                authenticateUser(username, password, scopes);

        Set<AccessScope> accessScopes =
                scopeService.convertToAccessScope(scopes);
        
        if (config.getOAuth2passwordAuthentication()
                .equals(AuthenticationMethod.LDAP)) {
            try {
                //username = LdapAuth3.getEmail(username, config.getLdapConfig());
                username = LdapAuth3.getUsername(username, config.getLdapConfig());
            }
            catch (LDAPException e) {
                throw new KustvaktException(StatusCodes.LDAP_BASE_ERRCODE,
                        e.getExceptionMessage());
            }
        }
        
        return createsAccessTokenResponse(scopes, accessScopes, clientId,
                username, authenticationTime, client);
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
     *            authorization scopes, optional
     * @return an {@link OAuthResponse}
     * @throws KustvaktException
     * @throws OAuthSystemException
     */
    protected OAuthResponse requestAccessTokenWithClientCredentials (
            String clientId, String clientSecret, Set<String> scopes)
            throws KustvaktException, OAuthSystemException {

        if (clientSecret == null || clientSecret.isEmpty()) {
            throw new KustvaktException(
                    StatusCodes.CLIENT_AUTHENTICATION_FAILED,
                    "Missing parameter: client_secret",
                    OAuth2Error.INVALID_REQUEST);
        }

        // OAuth2Client client =
        OAuth2Client oAuth2Client = clientService.authenticateClient(clientId, clientSecret);

        // if (!client.isNative()) {
        // throw new KustvaktException(
        // StatusCodes.CLIENT_AUTHENTICATION_FAILED,
        // "Client credentials grant is not allowed for third party
        // clients",
        // OAuth2Error.UNAUTHORIZED_CLIENT);
        // }

        ZonedDateTime authenticationTime =
                ZonedDateTime.now(ZoneId.of(Attributes.DEFAULT_TIME_ZONE));

        scopes = scopeService.filterScopes(scopes,
                config.getClientCredentialsScopes());
        Set<AccessScope> accessScopes =
                scopeService.convertToAccessScope(scopes);
        return createsAccessTokenResponse(scopes, accessScopes, clientId, null,
                authenticationTime,oAuth2Client);
    }

    /**
     * Creates an OAuthResponse containing an access token of type
     * Bearer. By default, MD generator is used to generates access
     * token of 128 bit values, represented in hexadecimal comprising
     * 32 bytes. The generated value is subsequently encoded in
     * Base64.
     * 
     * <br /><br />
     * Additionally, a refresh token is issued for confidential clients. 
     * It can be used to request a new access token without requiring user
     * re-authentication.
     * 
     * @param scopes
     *            a set of access token scopes in String
     * @param accessScopes
     *            a set of access token scopes in {@link AccessScope}
     * @param clientId
     *            a client id
     * @param userId
     *            a user id
     * @param authenticationTime
     *            the user authentication time
     * @return an {@link OAuthResponse}
     * @throws OAuthSystemException
     * @throws KustvaktException
     */
    private OAuthResponse createsAccessTokenResponse (Set<String> scopes,
            Set<AccessScope> accessScopes, String clientId, String userId,
            ZonedDateTime authenticationTime, OAuth2Client client)
            throws OAuthSystemException, KustvaktException {

        String random = randomGenerator.createRandomCode();
        random += randomGenerator.createRandomCode();
        
        if (clientService.isPublicClient(client)){
            // refresh token == null, getAccessTokenLongExpiry
            return createsAccessTokenResponse(scopes, accessScopes, clientId,
                    userId, authenticationTime);
            }
        else {
            // refresh token != null, getAccessTokenExpiry
            // default refresh token expiry: 365 days in seconds
            RefreshToken refreshToken = refreshDao.storeRefreshToken(random,
                    userId, authenticationTime, client, accessScopes);
            return createsAccessTokenResponse(scopes, accessScopes, clientId,
                    userId, authenticationTime, refreshToken);
        }
    }

    private OAuthResponse createsAccessTokenResponse (Set<String> scopes,
            Set<AccessScope> accessScopes, String clientId, String userId,
            ZonedDateTime authenticationTime, RefreshToken refreshToken)
            throws OAuthSystemException, KustvaktException {

        String accessToken = randomGenerator.createRandomCode();
        accessToken +=randomGenerator.createRandomCode();
        tokenDao.storeAccessToken(accessToken, refreshToken, accessScopes,
                userId, clientId, authenticationTime);

        return OAuthASResponse.tokenResponse(Status.OK.getStatusCode())
                .setAccessToken(accessToken)
                .setTokenType(TokenType.BEARER.toString())
                .setExpiresIn(String.valueOf(config.getAccessTokenExpiry()))
                .setRefreshToken(refreshToken.getToken())
                .setScope(String.join(" ", scopes)).buildJSONMessage();
    }
    
    private OAuthResponse createsAccessTokenResponse (Set<String> scopes,
            Set<AccessScope> accessScopes, String clientId, String userId,
            ZonedDateTime authenticationTime)
            throws OAuthSystemException, KustvaktException {

        String accessToken = randomGenerator.createRandomCode();
        accessToken +=randomGenerator.createRandomCode();
        tokenDao.storeAccessToken(accessToken, null, accessScopes,
                userId, clientId, authenticationTime);

        return OAuthASResponse.tokenResponse(Status.OK.getStatusCode())
                .setAccessToken(accessToken)
                .setTokenType(TokenType.BEARER.toString())
                .setExpiresIn(String.valueOf(config.getAccessTokenLongExpiry()))
                .setScope(String.join(" ", scopes)).buildJSONMessage();
    }

    public void revokeToken (OAuth2RevokeTokenRequest revokeTokenRequest)
            throws KustvaktException {
        String clientId = revokeTokenRequest.getClientId();
        String clientSecret = revokeTokenRequest.getClientSecret();
        String token = revokeTokenRequest.getToken();
        String tokenType = revokeTokenRequest.getTokenType();

        clientService.authenticateClient(clientId, clientSecret);
        if (tokenType != null && tokenType.equals("refresh_token")) {
            if (!revokeRefreshToken(token)) {
                revokeAccessToken(token);
            }
            return;
        }

        if (!revokeAccessToken(token)) {
            revokeRefreshToken(token);
        }
    }

    private boolean revokeAccessToken (String token) throws KustvaktException {
        try {
            AccessToken accessToken = tokenDao.retrieveAccessToken(token);
            revokeAccessToken(accessToken);
            return true;
        }
        catch (KustvaktException e) {
            if (!e.getStatusCode().equals(StatusCodes.INVALID_ACCESS_TOKEN)) {
                return false;
            }
            throw e;
        }
    }
    
    private void revokeAccessToken (AccessToken accessToken)
            throws KustvaktException {
        if (accessToken != null){
            accessToken.setRevoked(true);
            tokenDao.updateAccessToken(accessToken);
        }
    }

    private boolean revokeRefreshToken (String token) throws KustvaktException {
        RefreshToken refreshToken = null;
        try {
            refreshToken = refreshDao.retrieveRefreshToken(token);
        }
        catch (NoResultException e) {
            return false;
        }

        return revokeRefreshToken(refreshToken);
    }

    public boolean revokeRefreshToken (RefreshToken refreshToken)
            throws KustvaktException {
        if (refreshToken != null){
            refreshToken.setRevoked(true);
            refreshDao.updateRefreshToken(refreshToken);
    
            Set<AccessToken> accessTokenList = refreshToken.getAccessTokens();
            for (AccessToken accessToken : accessTokenList) {
                accessToken.setRevoked(true);
                tokenDao.updateAccessToken(accessToken);
            }
            return true;
        }
        return false;
    }

    public void revokeAllClientTokensViaSuperClient (String username,
            OAuth2RevokeAllTokenSuperRequest revokeTokenRequest)
            throws KustvaktException {
        String superClientId = revokeTokenRequest.getSuperClientId();
        String superClientSecret = revokeTokenRequest.getSuperClientSecret();

        OAuth2Client superClient = clientService
                .authenticateClient(superClientId, superClientSecret);
        if (!superClient.isSuper()) {
            throw new KustvaktException(
                    StatusCodes.CLIENT_AUTHENTICATION_FAILED);
        }

        String clientId = revokeTokenRequest.getClientId();
        revokeAllClientTokensForUser(clientId, username);
    }
    
    public void revokeAllClientTokensForUser (String clientId, String username)
            throws KustvaktException {
        OAuth2Client client = clientService.retrieveClient(clientId);
        if (clientService.isPublicClient(client)) {
            List<AccessToken> accessTokens =
                    tokenDao.retrieveAccessTokenByClientId(clientId, username);
            for (AccessToken t : accessTokens) {
                revokeAccessToken(t);
            }
        }
        else {
            List<RefreshToken> refreshTokens = refreshDao
                    .retrieveRefreshTokenByClientId(clientId, username);
            for (RefreshToken r : refreshTokens) {
                revokeRefreshToken(r);
            }
        }
    }
    
    public void revokeTokensViaSuperClient (String username,
            OAuth2RevokeTokenSuperRequest revokeTokenRequest) throws KustvaktException {
        String superClientId = revokeTokenRequest.getSuperClientId();
        String superClientSecret = revokeTokenRequest.getSuperClientSecret();

        OAuth2Client superClient = clientService
                .authenticateClient(superClientId, superClientSecret);
        if (!superClient.isSuper()) {
            throw new KustvaktException(
                    StatusCodes.CLIENT_AUTHENTICATION_FAILED);
        }
        
        String token = revokeTokenRequest.getToken();
        RefreshToken refreshToken = refreshDao.retrieveRefreshToken(token, username);
        if (!revokeRefreshToken(refreshToken)){
            AccessToken accessToken = tokenDao.retrieveAccessToken(token, username);
            revokeAccessToken(accessToken);
        }
    }
    
    public List<OAuth2TokenDto> listUserRefreshToken (String username, String superClientId,
            String superClientSecret, String clientId) throws KustvaktException {
        
        OAuth2Client client = clientService.authenticateClient(superClientId, superClientSecret);
        if (!client.isSuper()) {
            throw new KustvaktException(StatusCodes.CLIENT_AUTHORIZATION_FAILED,
                    "Only super client is allowed.",
                    OAuth2Error.UNAUTHORIZED_CLIENT);
        }

        List<RefreshToken> tokens = refreshDao.retrieveRefreshTokenByUser(username, clientId);
        List<OAuth2TokenDto> dtoList = new ArrayList<>(tokens.size());
        for (RefreshToken t : tokens){
            OAuth2Client tokenClient = t.getClient();
            if (tokenClient.getId().equals(client.getId())){
                continue;
            }
            OAuth2TokenDto dto = new OAuth2TokenDto();
            dto.setClientId(tokenClient.getId());
            dto.setClientName(tokenClient.getName());
            dto.setClientUrl(tokenClient.getUrl());
            dto.setClientDescription(tokenClient.getDescription());
            
            DateTimeFormatter f = DateTimeFormatter.ISO_DATE_TIME;
            dto.setCreatedDate(t.getCreatedDate().format(f));
            long difference = ChronoUnit.SECONDS.between(ZonedDateTime.now(), t.getExpiryDate());
            dto.setExpiresIn(difference);
            
            dto.setUserAuthenticationTime(
                    t.getUserAuthenticationTime().format(f));
            dto.setToken(t.getToken());
            
            Set<AccessScope> accessScopes = t.getScopes();
            Set<String> scopes = new HashSet<>(accessScopes.size());
            for (AccessScope s : accessScopes){
                scopes.add(s.getId().toString());
            }
            dto.setScope(scopes);
            dtoList.add(dto);
        }
        return dtoList;
    }
    
    public List<OAuth2TokenDto> listUserAccessToken (String username, String superClientId,
            String superClientSecret, String clientId) throws KustvaktException {
        
        OAuth2Client superClient = clientService.authenticateClient(superClientId, superClientSecret);
        if (!superClient.isSuper()) {
            throw new KustvaktException(StatusCodes.CLIENT_AUTHORIZATION_FAILED,
                    "Only super client is allowed.",
                    OAuth2Error.UNAUTHORIZED_CLIENT);
        }

        List<AccessToken> tokens =
                tokenDao.retrieveAccessTokenByUser(username, clientId);
        List<OAuth2TokenDto> dtoList = new ArrayList<>(tokens.size());
        for (AccessToken t : tokens){
            OAuth2Client tokenClient = t.getClient();
            if (tokenClient.getId().equals(superClient.getId())){
                continue;
            }
            OAuth2TokenDto dto = new OAuth2TokenDto();
            dto.setClientId(tokenClient.getId());
            dto.setClientName(tokenClient.getName());
            dto.setClientUrl(tokenClient.getUrl());
            dto.setClientDescription(tokenClient.getDescription());
            
            DateTimeFormatter f = DateTimeFormatter.ISO_DATE_TIME;
            dto.setCreatedDate(t.getCreatedDate().format(f));
            
            long difference = ChronoUnit.SECONDS.between(ZonedDateTime.now(), t.getExpiryDate());
            dto.setExpiresIn(difference);
                    
            dto.setUserAuthenticationTime(
                    t.getUserAuthenticationTime().format(f));
            dto.setToken(t.getToken());
            
            Set<AccessScope> accessScopes = t.getScopes();
            Set<String> scopes = new HashSet<>(accessScopes.size());
            for (AccessScope s : accessScopes){
                scopes.add(s.getId().toString());
            }
            dto.setScope(scopes);
            dtoList.add(dto);
        }
        return dtoList;
    }
   
}
