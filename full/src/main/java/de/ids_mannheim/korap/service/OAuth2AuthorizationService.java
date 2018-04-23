package de.ids_mannheim.korap.service;

import javax.servlet.http.HttpServletRequest;

import org.apache.oltu.oauth2.as.issuer.OAuthIssuer;
import org.apache.oltu.oauth2.as.request.OAuthAuthzRequest;
import org.apache.oltu.oauth2.as.response.OAuthASResponse;
import org.apache.oltu.oauth2.common.error.OAuthError;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.OAuthResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sun.jersey.api.client.ClientResponse.Status;

import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.entity.OAuth2Client;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;

@Service
public class OAuth2AuthorizationService {

    @Autowired
    private OAuth2ClientService clientService;
    @Autowired
    private OAuth2Service auth2Service;
    @Autowired
    private OAuthIssuer oauthIssuer;

    public OAuthResponse requestAuthorizationCode (HttpServletRequest request,
            OAuthAuthzRequest authzRequest, String authorization)
            throws KustvaktException, OAuthSystemException {

        String responseType = authzRequest.getResponseType();
        if (responseType == null || responseType.isEmpty()) {
            throw new KustvaktException(StatusCodes.MISSING_PARAMETER,
                    "response_type is missing.",
                    OAuthError.CodeResponse.INVALID_REQUEST);
        }

        OAuth2Client client;
        try {
            client = clientService.authenticateClient(
                    authzRequest.getClientId(), authzRequest.getClientSecret());
        }
        catch (KustvaktException e) {
            e.setEntity(OAuthError.CodeResponse.UNAUTHORIZED_CLIENT);
            throw e;
        }

        String redirectUri = authzRequest.getRedirectURI();
        boolean hasRedirectUri = hasRedirectUri(redirectUri);
        redirectUri = verifyRedirectUri(client, hasRedirectUri, redirectUri);

        auth2Service.authenticateUser(
                authzRequest.getParam(Attributes.USERNAME),
                authzRequest.getParam(Attributes.PASSWORD),
                authzRequest.getScopes());

        return OAuthASResponse
                .authorizationResponse(request, Status.FOUND.getStatusCode())
                .setCode(oauthIssuer.authorizationCode()).location(redirectUri)
                .buildQueryMessage();
    }


    private boolean hasRedirectUri (String redirectURI) {
        if (redirectURI != null && !redirectURI.isEmpty()) {
            return true;
        }
        return false;
    }

    /** If the request contains a redirect_uri parameter, the server must confirm 
     *  it is a valid redirect URI. 
     *  
     *  If there is no redirect_uri parameter in the request, and only one URI 
     *  was registered, the server uses the redirect URL that was previously 
     *  registered. 
     *  
     *  If no redirect URL has been registered, this is an error.
     *  
     * @param client an OAuth2Client
     * @param hasRedirectUri true if request contains redirect_uri, false otherwise
     * @param redirectUri the redirect_uri value
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
                        OAuthError.CodeResponse.INVALID_REQUEST);
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
                        OAuthError.CodeResponse.INVALID_REQUEST);
            }
        }

        return redirectUri;
    }
}
