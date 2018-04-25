package de.ids_mannheim.korap.oauth2.service;

import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.oltu.oauth2.as.issuer.OAuthIssuer;
import org.apache.oltu.oauth2.as.request.OAuthAuthzRequest;
import org.apache.oltu.oauth2.as.response.OAuthASResponse;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.OAuthResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sun.jersey.api.client.ClientResponse.Status;

import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.oauth2.constant.OAuth2Error;
import de.ids_mannheim.korap.oauth2.dao.AccessScopeDao;
import de.ids_mannheim.korap.oauth2.dao.AuthorizationDao;
import de.ids_mannheim.korap.oauth2.entity.AccessScope;
import de.ids_mannheim.korap.oauth2.entity.Authorization;
import de.ids_mannheim.korap.oauth2.entity.OAuth2Client;

@Service
public class OAuth2AuthorizationService {

    private static Logger jlog =
            LoggerFactory.getLogger(OAuth2AuthorizationService.class);

    public static int MAX_ATTEMPTS = 3;
    
    @Autowired
    private OAuth2ClientService clientService;
    @Autowired
    private OAuth2Service auth2Service;
    @Autowired
    private OAuthIssuer oauthIssuer;

    @Autowired
    private AuthorizationDao authorizationDao;
    @Autowired
    private AccessScopeDao accessScopeDao;

    public OAuthResponse requestAuthorizationCode (HttpServletRequest request,
            OAuthAuthzRequest authzRequest, String authorization)
            throws KustvaktException, OAuthSystemException {

        String responseType = authzRequest.getResponseType();
        if (responseType == null || responseType.isEmpty()) {
            throw new KustvaktException(StatusCodes.MISSING_PARAMETER,
                    "response_type is missing.", OAuth2Error.INVALID_REQUEST);
        }

        OAuth2Client client = clientService.authenticateClient(
                authzRequest.getClientId(), authzRequest.getClientSecret());

        String redirectUri = authzRequest.getRedirectURI();
        boolean hasRedirectUri = hasRedirectUri(redirectUri);
        redirectUri = verifyRedirectUri(client, hasRedirectUri, redirectUri);

        String username = authzRequest.getParam(Attributes.USERNAME);
        auth2Service.authenticateUser(username,
                authzRequest.getParam(Attributes.PASSWORD),
                authzRequest.getScopes());

        String code = oauthIssuer.authorizationCode();
        Set<AccessScope> scopes =
                convertToAccessScope(authzRequest.getScopes());

        authorizationDao.storeAuthorizationCode(authzRequest.getClientId(),
                username, code, scopes, authzRequest.getRedirectURI());

        return OAuthASResponse
                .authorizationResponse(request, Status.FOUND.getStatusCode())
                .setCode(code).location(redirectUri).buildQueryMessage();
    }

    private Set<AccessScope> convertToAccessScope (Set<String> scopes)
            throws KustvaktException {

        if (scopes.isEmpty()) {
            // return default scopes
            return null;
        }

        List<AccessScope> definedScopes = accessScopeDao.retrieveAccessScopes();
        Set<AccessScope> requestedScopes =
                new HashSet<AccessScope>(scopes.size());
        int index;
        for (String scope : scopes) {
            index = definedScopes.indexOf(scope);
            if (index == -1) {
                throw new KustvaktException(StatusCodes.INVALID_SCOPE,
                        scope + " is invalid.", OAuth2Error.INVALID_SCOPE);
            }
            else {
                requestedScopes.add(definedScopes.get(index));
            }
        }
        return requestedScopes;
    }

    private boolean hasRedirectUri (String redirectURI) {
        if (redirectURI != null && !redirectURI.isEmpty()) {
            return true;
        }
        return false;
    }

    /**
     * If the request contains a redirect_uri parameter, the server
     * must confirm it is a valid redirect URI.
     * 
     * If there is no redirect_uri parameter in the request, and only
     * one URI was registered, the server uses the redirect URL that
     * was previously registered.
     * 
     * If no redirect URL has been registered, this is an error.
     * 
     * @param client
     *            an OAuth2Client
     * @param hasRedirectUri
     *            true if request contains redirect_uri, false
     *            otherwise
     * @param redirectUri
     *            the redirect_uri value
     * @return a client's redirect URI
     * @throws KustvaktException
     */
    private String verifyRedirectUri (OAuth2Client client,
            boolean hasRedirectUri, String redirectUri)
            throws KustvaktException {

        String registeredUri = client.getRedirectURI();
        if (hasRedirectUri) {
            // check if the redirect URI the same as that in DB
            if (!redirectUri.equals(registeredUri)) {
                throw new KustvaktException(StatusCodes.INVALID_REDIRECT_URI,
                        redirectUri + " is unknown",
                        OAuth2Error.INVALID_REQUEST);
            }
        }
        else {
            // check if there is a redirect URI in the DB
            // This should not happened as it is required in client registration!
            if (registeredUri != null && !registeredUri.isEmpty()) {
                redirectUri = registeredUri;
            }
            else {
                throw new KustvaktException(StatusCodes.MISSING_PARAMETER,
                        "redirect_uri is required",
                        OAuth2Error.INVALID_REQUEST);
            }
        }

        return redirectUri;
    }


    public void verifyAuthorization (String code, String clientId,
            String redirectURI) throws KustvaktException {
        Authorization authorization =
                authorizationDao.retrieveAuthorizationCode(code, clientId);

        // EM: can Kustvakt be specific about the invalid request param?
        if (authorization.isRevoked()) {
            addTotalAttempts(authorization);
            throw new KustvaktException(StatusCodes.INVALID_AUTHORIZATION,
                    "Invalid authorization", OAuth2Error.INVALID_REQUEST);
        }

        if (isExpired(authorization.getCreatedDate())) {
            addTotalAttempts(authorization);
            throw new KustvaktException(StatusCodes.INVALID_AUTHORIZATION,
                    "Authorization expired", OAuth2Error.INVALID_REQUEST);
        }

        String authorizedUri = authorization.getRedirectURI();
        if (authorizedUri != null && !authorizedUri.isEmpty()
                && !authorizedUri.equals(redirectURI)) {
            addTotalAttempts(authorization);
            throw new KustvaktException(StatusCodes.INVALID_REDIRECT_URI,
                    "Invalid redirect URI", OAuth2Error.INVALID_REQUEST);
        }
        
        authorization.setRevoked(true);
        authorizationDao.updateAuthorization(authorization);
    }

    public void addTotalAttempts (Authorization authorization) {
        int totalAttempts = authorization.getTotalAttempts() + 1;
        if (totalAttempts > MAX_ATTEMPTS){
            authorization.setRevoked(true);
        }
        else{
            authorization.setTotalAttempts(totalAttempts);
        }
        authorizationDao.updateAuthorization(authorization);
    }

    private boolean isExpired (ZonedDateTime createdDate) {
        jlog.debug("createdDate: " + createdDate);
        ZonedDateTime expiration = createdDate.plusMinutes(10);
        ZonedDateTime now = ZonedDateTime.now();
        jlog.debug("expiration: " + expiration + ", now: " + now);

        if (expiration.isAfter(now)) {
            return false;
        }
        return true;
    }
}
