package de.ids_mannheim.korap.oauth2.openid.service;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Map;
import java.util.List;
import java.util.Set;

import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.validator.routines.UrlValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.nimbusds.oauth2.sdk.AuthorizationCode;
import com.nimbusds.oauth2.sdk.AuthorizationRequest;
import com.nimbusds.oauth2.sdk.AuthorizationSuccessResponse;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.ResponseMode;
import com.nimbusds.oauth2.sdk.ResponseType;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.id.State;
import com.nimbusds.openid.connect.sdk.AuthenticationRequest;
import com.nimbusds.openid.connect.sdk.AuthenticationSuccessResponse;
import com.nimbusds.openid.connect.sdk.Nonce;

import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.oauth2.constant.OAuth2Error;
import de.ids_mannheim.korap.oauth2.entity.OAuth2Client;
import de.ids_mannheim.korap.oauth2.service.OAuth2AuthorizationService;

/** Authorization service implementation using open id. 
 * 
 * @author margaretha
 *
 */
@Service
public class OpenIdAuthorizationService extends OAuth2AuthorizationService {

    @Autowired
    private UrlValidator redirectURIValidator;

    public void checkRedirectUriParam (Map<String, String> map)
            throws KustvaktException {
        if (map.containsKey("redirect_uri")) {
            String redirect_uri = map.get("redirect_uri");
            if (redirect_uri != null && !redirect_uri.isEmpty()) {
                if (!redirectURIValidator.isValid(redirect_uri)) {
                    throw new KustvaktException(
                            StatusCodes.INVALID_REDIRECT_URI,
                            "Invalid redirect URI",
                            OAuth2Error.INVALID_REQUEST);
                }
                return;
            }
        }

        throw new KustvaktException(StatusCodes.MISSING_REDIRECT_URI,
                "redirect_uri is required", OAuth2Error.INVALID_REQUEST);
    }

    public URI requestAuthorizationCode (MultivaluedMap<String, String> map,
            String username, boolean isAuthentication,
            ZonedDateTime authenticationTime)
            throws KustvaktException, ParseException {

        AuthorizationCode code = new AuthorizationCode();
        URI redirectUri = null;

        if (isAuthentication) {
            AuthenticationRequest authRequest = null;
            authRequest = AuthenticationRequest.parse((Map<String,List<String>>) map);
            redirectUri = handleAuthenticationRequest(authRequest, code,
                    username, authenticationTime);
            return new AuthenticationSuccessResponse(redirectUri, code, null,
                    null, authRequest.getState(), null, null).toURI();
        }
        else {
            AuthorizationRequest authzRequest = AuthorizationRequest.parse((Map<String,List<String>>) map);
            redirectUri = handleAuthorizationRequest(authzRequest, code,
                    username, authenticationTime, null);
            return new AuthorizationSuccessResponse(redirectUri, code, null,
                    authzRequest.getState(), null).toURI();

        }
    }

    private URI handleAuthorizationRequest (AuthorizationRequest authzRequest,
            AuthorizationCode code, String username,
            ZonedDateTime authenticationTime, String nonce)
            throws KustvaktException {

        URI redirectUri = authzRequest.getRedirectionURI();
        String redirectUriStr =
                (redirectUri != null) ? redirectUri.toString() : null;

        String clientId = authzRequest.getClientID().getValue();
        OAuth2Client client = clientService.authenticateClientId(clientId);
        String verifiedRedirectUri = verifyRedirectUri(client, redirectUriStr);

        try {
            redirectUri = new URI(verifiedRedirectUri);
        }
        catch (URISyntaxException e) {
            throw new KustvaktException(StatusCodes.INVALID_REDIRECT_URI,
                    "Invalid redirect URI", OAuth2Error.INVALID_REQUEST);
        }

        try {
            ResponseType responseType = authzRequest.getResponseType();
            checkResponseType(responseType.toString());

            Scope scope = authzRequest.getScope();
            Set<String> scopeSet = null;
            if (scope != null) {
                scopeSet = new HashSet<>(scope.toStringList());
            }
            createAuthorization(username, clientId, redirectUriStr, scopeSet,
                    code.getValue(), authenticationTime, nonce);
        }
        catch (KustvaktException e) {
            e.setRedirectUri(redirectUri);
            throw e;
        }

        return redirectUri;
    }


    /**
     * Kustvakt does not support the following parameters:
     * <em>claims</em>, <em>requestURI</em>, <em>requestObject</em>,
     * <em>id_token_hint</em>, and ignores them if they are included
     * in an authentication request. Kustvakt provides minimum support
     * for <em>acr_values</em> by not throwing an error when it is
     * included in an authentication request.
     * 
     * <p>Parameters related to user interface are also ignored,
     * namely <em>display</em>, <em>prompt</em>,
     * <em>ui_locales</em>, <em>login_hint</em>. However,
     * <em>display</em>, <em>prompt</em>, and <em>ui_locales</em>
     * must be supported by Kalamar. The minimum level of
     * support required for these parameters is simply that its use
     * must not result in an error.</p>
     * 
     * <p>Some Authentication request parameters in addition to
     * OAuth2.0 authorization parameters according to OpenID connect
     * core 1.0 Specification:</p>
     * 
     * <ul>
     * 
     * <li>nonce</li>
     * <p> OPTIONAL. The value is passed through unmodified from the
     * Authentication Request to the ID Token.</p>
     * 
     * <li>max_age</li>
     * <p>OPTIONAL. Maximum Authentication Age in seconds. If the
     * elapsed time is
     * greater than this value, the OpenID Provider MUST attempt
     * to actively re-authenticate the End-User. When max_age is used,
     * the ID Token returned MUST include an auth_time Claim
     * Value.</p>
     * 
     * <li>claims</li>
     * <p>Support for the claims parameter is OPTIONAL. Should an OP
     * (openid provider) not support this parameter and an RP (relying
     * party /client) uses it, the OP SHOULD return a set of Claims to
     * the RP that it believes would be useful to the RP and the
     * End-User using whatever heuristics it believes are
     * appropriate.</p>
     * 
     * </ul>
     * 
     * @see "OpenID Connect Core 1.0 specification"
     * 
     * @param authRequest
     * @param code
     * @param username
     * @param authenticationTime
     * @return
     * @throws KustvaktException
     */
    private URI handleAuthenticationRequest (AuthenticationRequest authRequest,
            AuthorizationCode code, String username,
            ZonedDateTime authenticationTime) throws KustvaktException {
        // TO DO: extra checking for authentication params?

        Nonce nonce = authRequest.getNonce();
        String nonceValue = null;
        if (nonce != null && !nonce.getValue().isEmpty()) {
            nonceValue = nonce.getValue();
        }

        checkMaxAge(authRequest.getMaxAge(), authenticationTime);

        AuthorizationRequest request = authRequest;
        return handleAuthorizationRequest(request, code, username,
                authenticationTime, nonceValue);
    }

    private void checkMaxAge (int maxAge, ZonedDateTime authenticationTime)
            throws KustvaktException {
        if (maxAge > 0) {
            ZonedDateTime now =
                    ZonedDateTime.now(ZoneId.of(Attributes.DEFAULT_TIME_ZONE));

            if (authenticationTime.plusSeconds(maxAge).isBefore(now)) {
                throw new KustvaktException(
                        StatusCodes.USER_REAUTHENTICATION_REQUIRED,
                        "User reauthentication is required because the authentication "
                                + "time is too old according to max_age");
            }
        }
    }

    @Override
    protected void checkResponseType (String responseType)
            throws KustvaktException {
        String[] types = responseType.split(" ");
        for (String type : types) {
            super.checkResponseType(type);
        }
    }

    public State retrieveState (Map<String, String> map) {
        String stateStr = map.get("state");
        if (stateStr != null && stateStr.isEmpty()) {
            return new State(stateStr);
        }
        return null;
    }

    public ResponseMode retrieveResponseMode (Map<String, String> map) {
        String str = map.get("response_mode");
        if (str != null && str.isEmpty()) {
            return new ResponseMode(str);
        }
        return null;
    }
}
