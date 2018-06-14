package de.ids_mannheim.korap.oauth2.oltu.service;

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
import de.ids_mannheim.korap.oauth2.entity.OAuth2Client;
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
        checkResponseType(authzRequest.getResponseType());

        String clientId = authzRequest.getClientId();
        OAuth2Client client = clientService.authenticateClientId(clientId);
        
        String redirectUri = authzRequest.getRedirectURI();
        String verifiedRedirectUri = verifyRedirectUri(client, redirectUri);
        String scope = createAuthorization(username, authzRequest.getClientId(),
                redirectUri, authzRequest.getScopes(), code);

        OAuthResponse oAuthResponse = OAuthASResponse
                .authorizationResponse(request, Status.FOUND.getStatusCode())
                .setCode(code).setScope(scope)
                .location(verifiedRedirectUri).buildQueryMessage();
        return oAuthResponse.getLocationUri();
    }
}
