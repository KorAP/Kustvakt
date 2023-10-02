package de.ids_mannheim.korap.oauth2.service;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Set;

import org.apache.commons.validator.routines.UrlValidator;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.nimbusds.oauth2.sdk.AuthorizationCode;
import com.nimbusds.oauth2.sdk.AuthorizationErrorResponse;
import com.nimbusds.oauth2.sdk.AuthorizationRequest;
import com.nimbusds.oauth2.sdk.AuthorizationSuccessResponse;
import com.nimbusds.oauth2.sdk.ErrorObject;
import com.nimbusds.oauth2.sdk.OAuth2Error;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.ResponseType;
import com.nimbusds.oauth2.sdk.id.State;

import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.FullConfiguration;
import de.ids_mannheim.korap.encryption.RandomCodeGenerator;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.oauth2.dao.AuthorizationDao;
import de.ids_mannheim.korap.oauth2.entity.AccessScope;
import de.ids_mannheim.korap.oauth2.entity.Authorization;
import de.ids_mannheim.korap.oauth2.entity.OAuth2Client;

/** Describes business logic behind OAuth2 authorization requests.
 * 
 * @author margaretha
 *
 */
@Service(value = "authorizationService")
public class OAuth2AuthorizationService {

    public static Logger jlog =
            LogManager.getLogger(OAuth2AuthorizationService.class);

    public static boolean DEBUG = false;
    
    @Autowired
    private RandomCodeGenerator codeGenerator;
    @Autowired
    protected OAuth2ClientService clientService;
    @Autowired
    protected OAuth2ScopeServiceImpl scopeService;
    @Autowired
    private AuthorizationDao authorizationDao;
    @Autowired
    private UrlValidator redirectURIValidator;

    @Autowired
    protected FullConfiguration config;
    
    public State createAuthorizationState (String state) {
        State authState = null;
        if (state!=null && !state.isEmpty())
            authState = new State(state);
        return authState;
    }
    
    public AuthorizationErrorResponse createAuthorizationError (
            KustvaktException e, String state) {
        State authState = createAuthorizationState(state);
        ErrorObject error = e.getOauth2Error();
        error = error.setDescription(e.getMessage());
        AuthorizationErrorResponse errorResponse =
                new AuthorizationErrorResponse(e.getRedirectUri(),
                        error,authState, null);
        return errorResponse;
    }
    
    public URI requestAuthorizationCode (URI requestURI, String clientId,
            String redirectUri, String scope, String state, String username,
            ZonedDateTime authenticationTime) throws KustvaktException {

        URI redirectURI = null;
        String code;
        try {
            OAuth2Client client = clientService.authenticateClientId(clientId);
            redirectURI = verifyRedirectUri(client, redirectUri);
            //checkResponseType(authzRequest.getResponseType(), redirectURI);
            code = codeGenerator.createRandomCode();
            URI responseURI = createAuthorizationResponse(requestURI,
                    redirectURI, code, state);

            createAuthorization(username, clientId, redirectUri, scope,
                    code.toString(), authenticationTime, null);
            return responseURI;
            
        }
        catch (KustvaktException e) {
            e.setRedirectUri(redirectURI);
            throw e;
        }
    }

    private URI createAuthorizationResponse (URI requestURI, URI redirectURI,
            String code, String state)
            throws KustvaktException {
        AuthorizationRequest authRequest = null;
        try {
            authRequest = AuthorizationRequest.parse(requestURI);

            if (authRequest.getResponseType()
                    .equals(new ResponseType(ResponseType.Value.CODE))) {

                State authState = createAuthorizationState(state);
                AuthorizationSuccessResponse response =
                        new AuthorizationSuccessResponse(redirectURI,
                                new AuthorizationCode(code), null, authState,
                                null);
                return response.toURI();
            }
            else {
                KustvaktException ke = new KustvaktException(
                        StatusCodes.UNSUPPORTED_RESPONSE_TYPE,
                        "Unsupported response type. Only code is supported.",
                        OAuth2Error.UNSUPPORTED_RESPONSE_TYPE);
                throw ke;
            }
        }
        catch (ParseException e) {
            KustvaktException ke =
                    new KustvaktException(StatusCodes.INVALID_REQUEST,
                            e.getMessage(), OAuth2Error.INVALID_REQUEST_URI);
            throw ke;
        }

    }
    @Deprecated
    public String createAuthorization (String username, String clientId,
            String redirectUri, Set<String> scopeSet, String code,
            ZonedDateTime authenticationTime, String nonce)
            throws KustvaktException {

        if (scopeSet == null || scopeSet.isEmpty()) {
            throw new KustvaktException(StatusCodes.MISSING_PARAMETER,
                    "scope is required", OAuth2Error.INVALID_SCOPE);
        }
        Set<AccessScope> scopes = scopeService.convertToAccessScope(scopeSet);

        authorizationDao.storeAuthorizationCode(clientId, username, code,
                scopes, redirectUri, authenticationTime, nonce);
        return String.join(" ", scopeSet);
    }
    
    /**
     * Authorization code request does not require client
     * authentication, but only checks if the client id exists.
     * 
     * @param username
     * @param clientId
     * @param redirectUri
     * @param scope
     * @param code
     * @param authenticationTime
     *            user authentication time
     * @param nonce
     * @throws KustvaktException
     */
    public void createAuthorization (String username, String clientId,
            String redirectUri, String scope, String code,
            ZonedDateTime authenticationTime, String nonce)
            throws KustvaktException {

        if (scope == null || scope.isEmpty()) {
            throw new KustvaktException(StatusCodes.MISSING_PARAMETER,
                    "scope is required", OAuth2Error.INVALID_SCOPE);
        }
        Set<AccessScope> accessScopes = scopeService.convertToAccessScope(scope);

        authorizationDao.storeAuthorizationCode(clientId, username, code,
                accessScopes, redirectUri, authenticationTime, nonce);
    }

    @Deprecated
    protected void checkResponseType (String responseType)
            throws KustvaktException {
        if (responseType == null || responseType.isEmpty()) {
            throw new KustvaktException(StatusCodes.MISSING_PARAMETER,
                    "response_type is missing.", OAuth2Error.INVALID_REQUEST);
        }
        else if (!responseType.equals("code")) {
            throw new KustvaktException(StatusCodes.NOT_SUPPORTED,
                    "unsupported response_type: " + responseType,
                    OAuth2Error.INVALID_REQUEST);
        }
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
     * @param redirectUri
     *            the redirect_uri value
     * @return a client's redirect URI
     * @throws KustvaktException
     */
    public URI verifyRedirectUri (OAuth2Client client, String redirectUri)
            throws KustvaktException {

        String registeredUri = client.getRedirectURI();
        
        if (redirectUri != null && !redirectUri.isEmpty()) {
            // check if the redirect URI the same as that in DB
            if (!redirectURIValidator.isValid(redirectUri) ||
                    (registeredUri != null && !registeredUri.isEmpty()
                    && !redirectUri.equals(registeredUri))) {
                throw new KustvaktException(StatusCodes.INVALID_REDIRECT_URI,
                        "Invalid redirect URI", OAuth2Error.INVALID_REQUEST);
            }
        }
        // redirect_uri is not required in client registration
        else if (registeredUri != null && !registeredUri.isEmpty()) {
                redirectUri = registeredUri;
        }
        else {
            throw new KustvaktException(StatusCodes.MISSING_REDIRECT_URI,
                    "Missing parameter: redirect URI",
                    OAuth2Error.INVALID_REQUEST);
        }
        URI redirectURI;
        try {
            redirectURI = new URI(redirectUri);
        }
        catch (URISyntaxException e) {
            throw new KustvaktException(StatusCodes.INVALID_REDIRECT_URI,
                    "Invalid redirect URI", OAuth2Error.INVALID_REQUEST);
        }
        
        return redirectURI;
    }
    
    public KustvaktException checkRedirectUri (KustvaktException e,
            String clientId, String redirectUri){
        int statusCode = e.getStatusCode();
        if (clientId!=null && !clientId.isEmpty()
                && statusCode != StatusCodes.CLIENT_NOT_FOUND
                && statusCode != StatusCodes.AUTHORIZATION_FAILED
                && statusCode != StatusCodes.INVALID_REDIRECT_URI) {
            String registeredUri = null;
            try {
                OAuth2Client client = clientService.retrieveClient(clientId);
                registeredUri = client.getRedirectURI();
            }
            catch (KustvaktException e1) {}

            if (redirectUri != null && !redirectUri.isEmpty()) {
                if (registeredUri != null && !registeredUri.isEmpty()
                        && !redirectUri.equals(registeredUri)) {
                    return new KustvaktException(StatusCodes.INVALID_REDIRECT_URI,
                            "Invalid redirect URI", OAuth2Error.INVALID_REQUEST);
                }
                else {
                    try {
                        e.setRedirectUri(new URI(redirectUri));
                    }
                    catch (URISyntaxException e1) {
                        return new KustvaktException(StatusCodes.INVALID_REDIRECT_URI,
                                "Invalid redirect URI", OAuth2Error.INVALID_REQUEST);
                    }
                    e.setResponseStatus(HttpStatus.SC_TEMPORARY_REDIRECT);
                }
            }
            else if (registeredUri != null && !registeredUri.isEmpty()) {
                try {
                    e.setRedirectUri(new URI(registeredUri));
                }
                catch (URISyntaxException e1) {
                    return new KustvaktException(StatusCodes.INVALID_REDIRECT_URI,
                            "Invalid redirect URI", OAuth2Error.INVALID_REQUEST);
                }
                e.setResponseStatus(HttpStatus.SC_TEMPORARY_REDIRECT);
            }
            else {
                return new KustvaktException(StatusCodes.MISSING_REDIRECT_URI,
                        "Missing parameter: redirect URI", OAuth2Error.INVALID_REQUEST);
            }
        }

        return e;
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

        if (isExpired(authorization.getExpiryDate())) {
            throw new KustvaktException(StatusCodes.INVALID_AUTHORIZATION,
                    "Authorization expired", OAuth2Error.INVALID_GRANT);
        }

        String authorizedUri = authorization.getRedirectURI();
        if (authorizedUri != null && !authorizedUri.isEmpty()) {
            if (redirectURI == null || redirectURI.isEmpty()) {
                throw new KustvaktException(StatusCodes.INVALID_REDIRECT_URI,
                        "Missing redirect URI", OAuth2Error.INVALID_GRANT);
            }    
            if (!authorizedUri.equals(redirectURI)) {
                throw new KustvaktException(StatusCodes.INVALID_REDIRECT_URI,
                        "Invalid redirect URI", OAuth2Error.INVALID_GRANT);
            }
        }
        else if (redirectURI != null && !redirectURI.isEmpty()) {
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

    private boolean isExpired (ZonedDateTime expiryDate) {
        ZonedDateTime now =
                ZonedDateTime.now(ZoneId.of(Attributes.DEFAULT_TIME_ZONE));
        if (DEBUG) {
            jlog.debug("createdDate: " + expiryDate);
            jlog.debug("expiration: " + expiryDate + ", now: " + now);
        }
        if (expiryDate.isAfter(now)) {
            return false;
        }
        return true;
    }

}
