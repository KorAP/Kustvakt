package de.ids_mannheim.korap.oauth2.service;

import java.time.ZonedDateTime;
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

import de.ids_mannheim.korap.config.FullConfiguration;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.oauth2.constant.OAuth2Error;
import de.ids_mannheim.korap.oauth2.dao.AuthorizationDao;
import de.ids_mannheim.korap.oauth2.entity.AccessScope;
import de.ids_mannheim.korap.oauth2.entity.Authorization;
import de.ids_mannheim.korap.oauth2.entity.OAuth2Client;

@Service
public class OAuth2AuthorizationService {

    private static Logger jlog =
            LoggerFactory.getLogger(OAuth2AuthorizationService.class);

    @Autowired
    private OAuth2ClientService clientService;
    @Autowired
    private OAuth2ScopeService scopeService;
    @Autowired
    private OAuthIssuer oauthIssuer;

    @Autowired
    private AuthorizationDao authorizationDao;

    @Autowired
    private FullConfiguration config;

    /**
     * Authorization code request does not require client
     * authentication, but only checks if the client id exists.
     * 
     * @param request
     * @param authzRequest
     * @param username
     * @return
     * @throws KustvaktException
     * @throws OAuthSystemException
     */
    public OAuthResponse requestAuthorizationCode (HttpServletRequest request,
            OAuthAuthzRequest authzRequest, String username)
            throws KustvaktException, OAuthSystemException {

        checkResponseType(authzRequest.getResponseType());

        OAuth2Client client =
                clientService.authenticateClientId(authzRequest.getClientId());

        String redirectUri = authzRequest.getRedirectURI();
        boolean hasRedirectUri = hasRedirectUri(redirectUri);
        redirectUri = verifyRedirectUri(client, hasRedirectUri, redirectUri);

        String code = oauthIssuer.authorizationCode();
        Set<String> scopeSet = authzRequest.getScopes();
        if (scopeSet == null || scopeSet.isEmpty()) {
            scopeSet = config.getDefaultAccessScopes();
        }
        String scopeStr = String.join(" ", scopeSet);
        Set<AccessScope> scopes = scopeService.convertToAccessScope(scopeSet);

        authorizationDao.storeAuthorizationCode(authzRequest.getClientId(),
                username, code, scopes, authzRequest.getRedirectURI());

        return OAuthASResponse
                .authorizationResponse(request, Status.FOUND.getStatusCode())
                .setCode(code).setScope(scopeStr).location(redirectUri)
                .buildQueryMessage();
    }

    private void checkResponseType (String responseType)
            throws KustvaktException {
        if (responseType == null || responseType.isEmpty()) {
            throw new KustvaktException(StatusCodes.MISSING_PARAMETER,
                    "response_type is missing.", OAuth2Error.INVALID_REQUEST);
        }
        else if (responseType.equals("token")) {
            throw new KustvaktException(StatusCodes.NOT_SUPPORTED,
                    "response_type token is not supported.",
                    OAuth2Error.INVALID_REQUEST);
        }
        else if (!responseType.equals("code")) {
            throw new KustvaktException(StatusCodes.INVALID_ARGUMENT,
                    "unknown response_type", OAuth2Error.INVALID_REQUEST);
        }
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
            // This should not happened as it is required in client
            // registration!
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


    public Authorization retrieveAuthorization (String code)
            throws KustvaktException {
        return authorizationDao.retrieveAuthorizationCode(code);
    }

    public Authorization verifyAuthorization (Authorization authorization,
            String clientId, String redirectURI) throws KustvaktException {

        // EM: can Kustvakt be specific about the invalid grant error
        // description?
        if (!authorization.getClientId().equals(clientId)) {
            throw new KustvaktException(StatusCodes.INVALID_AUTHORIZATION,
                    "Invalid authorization", OAuth2Error.INVALID_GRANT);
        }
        if (authorization.isRevoked()) {
            throw new KustvaktException(StatusCodes.INVALID_AUTHORIZATION,
                    "Invalid authorization", OAuth2Error.INVALID_GRANT);
        }

        if (isExpired(authorization.getCreatedDate())) {
            throw new KustvaktException(StatusCodes.INVALID_AUTHORIZATION,
                    "Authorization expired", OAuth2Error.INVALID_GRANT);
        }

        String authorizedUri = authorization.getRedirectURI();
        if (authorizedUri != null && !authorizedUri.isEmpty()
                && !authorizedUri.equals(redirectURI)) {
            throw new KustvaktException(StatusCodes.INVALID_REDIRECT_URI,
                    "Invalid redirect URI", OAuth2Error.INVALID_GRANT);
        }

        authorization.setRevoked(true);
        authorization = authorizationDao.updateAuthorization(authorization);

        return authorization;
    }

    public void addTotalAttempts (Authorization authorization)
            throws KustvaktException {
        int totalAttempts = authorization.getTotalAttempts() + 1;
        if (totalAttempts == config.getMaxAuthenticationAttempts()) {
            authorization.setRevoked(true);
        }
        authorization.setTotalAttempts(totalAttempts);
        authorizationDao.updateAuthorization(authorization);
    }

    private boolean isExpired (ZonedDateTime createdDate) {
        jlog.debug("createdDate: " + createdDate);
        ZonedDateTime expiration = createdDate.plusSeconds(60);
        ZonedDateTime now = ZonedDateTime.now();
        jlog.debug("expiration: " + expiration + ", now: " + now);

        if (expiration.isAfter(now)) {
            return false;
        }
        return true;
    }
}
