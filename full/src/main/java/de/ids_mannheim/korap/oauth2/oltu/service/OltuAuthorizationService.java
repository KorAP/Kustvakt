package de.ids_mannheim.korap.oauth2.oltu.service;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.ZonedDateTime;

import javax.servlet.http.HttpServletRequest;

import org.apache.http.HttpStatus;
import org.apache.oltu.oauth2.as.request.OAuthAuthzRequest;
import org.apache.oltu.oauth2.as.response.OAuthASResponse;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.OAuthResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import de.ids_mannheim.korap.encryption.RandomCodeGenerator;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.oauth2.constant.OAuth2Error;
import de.ids_mannheim.korap.oauth2.entity.OAuth2Client;
import de.ids_mannheim.korap.oauth2.service.OAuth2AuthorizationService;
import de.ids_mannheim.korap.oauth2.service.OAuth2ClientService;
import jakarta.ws.rs.core.Response.Status;

/**
 * OAuth2 authorization service using Apache Oltu
 * 
 * @author margaretha
 *
 */
@Service
public class OltuAuthorizationService extends OAuth2AuthorizationService {

    @Autowired
    private RandomCodeGenerator codeGenerator;
    @Autowired
    private OAuth2ClientService clientService;

    /**
     * Authorization code request does not require client
     * authentication, but only checks if the client id exists.
     * 
     * @param request
     * @param authzRequest
     * @param username
     * @param authTime
     * @return redirect URI containing authorization code if
     *         successful.
     * 
     * @throws KustvaktException
     * @throws OAuthSystemException
     */
    public String requestAuthorizationCode (HttpServletRequest request,
            OAuthAuthzRequest authzRequest, String username,
            ZonedDateTime authenticationTime)
            throws OAuthSystemException, KustvaktException {

        String clientId = authzRequest.getClientId();
        OAuth2Client client = clientService.authenticateClientId(clientId);

        String redirectUriStr = authzRequest.getRedirectURI();
        URI redirectURI = verifyRedirectUri(client, redirectUriStr);

        String scope, code;
        try {
            //checkResponseType(authzRequest.getResponseType(), redirectURI);
            code = codeGenerator.createRandomCode();
            scope = createAuthorization(username, authzRequest.getClientId(),
                    redirectUriStr, authzRequest.getScopes(), code,
                    authenticationTime, null);
        }
        catch (KustvaktException e) {
            e.setRedirectUri(redirectURI);
            throw e;
        }

        OAuthResponse oAuthResponse;
        try {
            oAuthResponse = OAuthASResponse
                    .authorizationResponse(request,
                            Status.FOUND.getStatusCode())
                    .setCode(code).setScope(scope)
                    .location(redirectURI.toString())
                    .buildQueryMessage();
        }
        catch (OAuthSystemException e) {
            // Should not happen
            KustvaktException ke =
                    new KustvaktException(StatusCodes.OAUTH2_SYSTEM_ERROR,
                            e.getMessage(), OAuth2Error.SERVER_ERROR);
            ke.setRedirectUri(redirectURI);
            throw ke;
        }
        return oAuthResponse.getLocationUri();
    }

    public OAuthProblemException checkRedirectUri (OAuthProblemException e,
            String clientId, String redirectUri) {
        if (clientId !=null && !clientId.isEmpty()) {
            String registeredUri = null;
            try {
                OAuth2Client client = clientService.retrieveClient(clientId);
                registeredUri = client.getRedirectURI();
            }
            catch (KustvaktException e1) {}

            if (redirectUri != null && !redirectUri.isEmpty()) {
                if (registeredUri != null && !registeredUri.isEmpty()
                        && !redirectUri.equals(registeredUri)) {
                    e.description("Invalid redirect URI");
                }
                else {
                    e.setRedirectUri(redirectUri);
                    e.responseStatus(HttpStatus.SC_TEMPORARY_REDIRECT);
                }
            }
            else if (registeredUri != null && !registeredUri.isEmpty()) {
                e.setRedirectUri(registeredUri);
                e.responseStatus(HttpStatus.SC_TEMPORARY_REDIRECT);
            }
            else {
                e.description("Missing parameter: redirect URI");
            }
        }

        return e;
    }
    
}
