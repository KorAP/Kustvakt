package de.ids_mannheim.korap.oauth2.oltu.service;

import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.oltu.oauth2.as.issuer.OAuthIssuer;
import org.apache.oltu.oauth2.as.request.OAuthAuthzRequest;
import org.apache.oltu.oauth2.as.response.OAuthASResponse;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.OAuthResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sun.jersey.api.client.ClientResponse.Status;

import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.oauth2.entity.AccessScope;
import de.ids_mannheim.korap.oauth2.entity.Authorization;
import de.ids_mannheim.korap.oauth2.service.OAuth2AuthorizationService;

/**
 * OAuth2 authorization service using Apache Oltu
 * 
 * @author margaretha
 *
 */
@Service
public class OltuAuthorizationService extends OAuth2AuthorizationService {

    @Autowired
    private OAuthIssuer oauthIssuer;

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
    public String requestAuthorizationCode (HttpServletRequest request,
            OAuthAuthzRequest authzRequest, String username)
            throws KustvaktException, OAuthSystemException {

        String code = oauthIssuer.authorizationCode();
        Authorization authorization = createAuthorization(username,
                authzRequest.getClientId(), authzRequest.getResponseType(),
                authzRequest.getRedirectURI(), authzRequest.getScopes(), code);

        Set<AccessScope> scopes = authorization.getScopes();
        String scopeStr = scopeService.convertAccessScopesToString(scopes);

        OAuthResponse oAuthResponse = OAuthASResponse
                .authorizationResponse(request, Status.FOUND.getStatusCode())
                .setCode(code).setScope(scopeStr)
                .location(authorization.getRedirectURI()).buildQueryMessage();
        return oAuthResponse.getLocationUri();
    }
}
