package de.ids_mannheim.korap.oauth2.openid.service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.nimbusds.oauth2.sdk.AuthorizationCode;
import com.nimbusds.oauth2.sdk.AuthorizationRequest;
import com.nimbusds.oauth2.sdk.ResponseType;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.State;
import com.nimbusds.openid.connect.sdk.AuthenticationRequest;
import com.nimbusds.openid.connect.sdk.AuthenticationSuccessResponse;

import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.oauth2.entity.Authorization;
import de.ids_mannheim.korap.oauth2.service.OAuth2AuthorizationService;

@Service
public class OpenIdAuthorizationService
        extends OAuth2AuthorizationService {

    public URI requestAuthorizationCode (AuthenticationRequest authRequest,
            String username) throws KustvaktException {

        ClientID clientId = authRequest.getClientID();
        URI redirectUri = authRequest.getRedirectionURI();
        State state = authRequest.getState();
        Scope scope = authRequest.getScope();
        ResponseType responseType = authRequest.getResponseType();

        String redirectUriStr =
                (redirectUri != null) ? redirectUri.toString() : null;
        Set<String> scopeSet =
                (scope != null) ? new HashSet<>(scope.toStringList()) : null;
        AuthorizationCode code = new AuthorizationCode();

        Authorization authorization = createAuthorization(username,
                clientId.toString(), responseType.toString(), redirectUriStr,
                scopeSet, code.getValue());

        try {
            return new AuthenticationSuccessResponse(
                    new URI(authorization.getRedirectURI()), code, null, null,
                    state, null, null).toURI();
        }
        catch (URISyntaxException e) {
            throw new KustvaktException(StatusCodes.INVALID_REDIRECT_URI,
                    "Invalid redirect URI.");
        }
    }
}
