package de.ids_mannheim.korap.oauth2.openid.service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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

import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.oauth2.constant.OAuth2Error;
import de.ids_mannheim.korap.oauth2.entity.OAuth2Client;
import de.ids_mannheim.korap.oauth2.service.OAuth2AuthorizationService;

@Service
public class OpenIdAuthorizationService extends OAuth2AuthorizationService {

    @Autowired
    private UrlValidator urlValidator;

    public void checkRedirectUriParam (Map<String, String> map)
            throws KustvaktException {
        if (map.containsKey("redirect_uri")) {
            String redirect_uri = map.get("redirect_uri");
            if (redirect_uri != null && !redirect_uri.isEmpty()) {
                if (!urlValidator.isValid(redirect_uri)) {
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

    public URI requestAuthorizationCode (Map<String, String> map,
            String username, boolean isAuthentication)
            throws KustvaktException, ParseException {

        AuthorizationCode code = new AuthorizationCode();
        URI redirectUri = null;
        if (isAuthentication) {
            AuthenticationRequest authRequest = null;
            authRequest = AuthenticationRequest.parse(map);
            redirectUri =
                    handleAuthenticationRequest(authRequest, code, username);
            return new AuthenticationSuccessResponse(redirectUri, code, null,
                    null, authRequest.getState(), null, null).toURI();
        }
        else {
            AuthorizationRequest authzRequest = AuthorizationRequest.parse(map);
            redirectUri =
                    handleAuthorizationRequest(authzRequest, code, username);
            return new AuthorizationSuccessResponse(redirectUri, code, null,
                    authzRequest.getState(), null).toURI();

        }
    }

    private URI handleAuthorizationRequest (AuthorizationRequest authzRequest,
            AuthorizationCode code, String username) throws KustvaktException {

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
            Set<String> scopeSet = (scope != null)
                    ? new HashSet<>(scope.toStringList()) : null;

            createAuthorization(username, clientId, redirectUriStr, scopeSet,
                    code.getValue());
        }
        catch (KustvaktException e) {
            e.setRedirectUri(redirectUri);
            throw e;
        }

        return redirectUri;
    }


    private URI handleAuthenticationRequest (AuthenticationRequest authRequest,
            AuthorizationCode code, String username) throws KustvaktException {
        // TO DO: extra checking for authentication params?

        AuthorizationRequest request = authRequest;
        return handleAuthorizationRequest(request, code, username);
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
