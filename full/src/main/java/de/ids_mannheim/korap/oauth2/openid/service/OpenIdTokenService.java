package de.ids_mannheim.korap.oauth2.openid.service;

import java.net.URI;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.nimbusds.oauth2.sdk.AccessTokenResponse;
import com.nimbusds.oauth2.sdk.AuthorizationCodeGrant;
import com.nimbusds.oauth2.sdk.AuthorizationGrant;
import com.nimbusds.oauth2.sdk.GrantType;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.auth.ClientAuthenticationMethod;
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic;
import com.nimbusds.oauth2.sdk.auth.ClientSecretPost;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import com.nimbusds.oauth2.sdk.token.RefreshToken;
import com.nimbusds.oauth2.sdk.token.Tokens;
import com.nimbusds.openid.connect.sdk.OIDCTokenResponse;
import com.nimbusds.openid.connect.sdk.token.OIDCTokens;

import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.oauth2.constant.OAuth2Error;
import de.ids_mannheim.korap.oauth2.entity.AccessScope;
import de.ids_mannheim.korap.oauth2.entity.Authorization;
import de.ids_mannheim.korap.oauth2.service.OAuth2TokenService;

@Service
public class OpenIdTokenService extends OAuth2TokenService {

    public AccessTokenResponse requestAccessToken (TokenRequest tokenRequest)
            throws KustvaktException {
        AuthorizationGrant grant = tokenRequest.getAuthorizationGrant();
        GrantType grantType = grant.getType();
        ClientAuthentication clientAuthentication =
                tokenRequest.getClientAuthentication();
        String[] clientCredentials =
                extractClientCredentials(clientAuthentication);

        if (grantType.equals(GrantType.AUTHORIZATION_CODE)) {
            AuthorizationCodeGrant codeGrant = (AuthorizationCodeGrant) grant;
            String authorizationCode =
                    codeGrant.getAuthorizationCode().getValue();
            URI redirectionURI = codeGrant.getRedirectionURI();
            String redirectURI = null;
            if (redirectionURI != null) {
                redirectURI = redirectionURI.toString();
            }
            Authorization authorization =
                    requestAccessTokenWithAuthorizationCode(authorizationCode,
                            redirectURI, clientCredentials[0],
                            clientCredentials[1]);
            return createsAccessTokenResponse(authorization);
        }
        else if (grantType.equals(GrantType.PASSWORD)) {

        }
        else if (grantType.equals(GrantType.CLIENT_CREDENTIALS)) {

        }
        else {
            throw new KustvaktException(StatusCodes.UNSUPPORTED_GRANT_TYPE,
                    grantType + " is not supported.",
                    OAuth2Error.UNSUPPORTED_GRANT_TYPE);
        }
        return null;
    }


    private AccessTokenResponse createsAccessTokenResponse (
            Authorization authorization) {
        Set<AccessScope> scopes = authorization.getScopes();
        String[] scopeArray = scopes.stream().map(scope -> scope.toString())
                .toArray(String[]::new);
        Scope scope = new Scope(scopeArray);
        AccessToken accessToken =
                new BearerAccessToken(config.getTokenTTL(), scope);
        RefreshToken refreshToken = new RefreshToken();

        if (scope.contains("openid")) {
            // id token should be encrypted according to keys and
            // algorithms the client specified during registration
            String idToken = "thisIsIdToken";
            OIDCTokens tokens =
                    new OIDCTokens(idToken, accessToken, refreshToken);
            return new OIDCTokenResponse(tokens);
        }
        else {
            Tokens tokens = new Tokens(accessToken, refreshToken);
            return new AccessTokenResponse(tokens);
        }
    }


    private String[] extractClientCredentials (
            ClientAuthentication clientAuthentication)
            throws KustvaktException {

        ClientAuthenticationMethod method = clientAuthentication.getMethod();
        String clientSecret;
        String clientId;
        if (method.equals(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)) {
            ClientSecretBasic basic = (ClientSecretBasic) clientAuthentication;
            clientSecret = basic.getClientSecret().getValue();
            clientId = basic.getClientID().getValue();
        }
        else if (method.equals(ClientAuthenticationMethod.CLIENT_SECRET_POST)) {
            ClientSecretPost post = (ClientSecretPost) clientAuthentication;
            clientSecret = post.getClientSecret().getValue();
            clientId = post.getClientID().getValue();
        }
        else {
            // client authentication method is not supported
            throw new KustvaktException(
                    StatusCodes.UNSUPPORTED_AUTHENTICATION_METHOD,
                    method.getValue() + " is not supported.",
                    OAuth2Error.INVALID_CLIENT);
        }
        return new String[] { clientId, clientSecret };
    }
}
