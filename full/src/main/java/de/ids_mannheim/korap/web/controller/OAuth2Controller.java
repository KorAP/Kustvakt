package de.ids_mannheim.korap.web.controller;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.ZonedDateTime;

import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.OAuthResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.nimbusds.oauth2.sdk.AuthorizationErrorResponse;
import com.nimbusds.oauth2.sdk.AuthorizationGrant;
import com.nimbusds.oauth2.sdk.ClientCredentialsGrant;
import com.nimbusds.oauth2.sdk.OAuth2Error;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.auth.ClientAuthenticationMethod;
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic;
import com.nimbusds.oauth2.sdk.auth.ClientSecretPost;
import com.nimbusds.oauth2.sdk.id.ClientID;

import de.ids_mannheim.korap.constant.OAuth2Scope;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.oauth2.service.OAuth2AuthorizationService;
import de.ids_mannheim.korap.oauth2.service.OAuth2ScopeService;
import de.ids_mannheim.korap.oauth2.service.OAuth2TokenService;
import de.ids_mannheim.korap.security.context.TokenContext;
import de.ids_mannheim.korap.web.OAuth2ResponseHandler;
import de.ids_mannheim.korap.web.filter.APIVersionFilter;
import de.ids_mannheim.korap.web.filter.AuthenticationFilter;
import de.ids_mannheim.korap.web.filter.BlockingFilter;
import de.ids_mannheim.korap.web.utils.ResourceFilters;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.UriBuilder;

/**
 * OAuth2Controller describes OAuth2 web API for authorization
 * for both internal (e.g Kalamar) and external (third party) clients.
 * 
 * Possible authorization scopes are listed in {@link OAuth2Scope} For
 * more information about OAuth2 authorization mechanisms, see RFC
 * 6749.
 * 
 * @author margaretha
 *
 */
@Controller
@Path("{version}/oauth2")
@ResourceFilters({ APIVersionFilter.class, AuthenticationFilter.class,
        BlockingFilter.class })
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class OAuth2Controller {

    @Autowired
    private OAuth2ResponseHandler responseHandler;

    @Autowired
    private OAuth2TokenService tokenService;
    @Autowired
    private OAuth2AuthorizationService authorizationService;
    
    @Autowired
    private OAuth2ScopeService scopeService;

    /**
     * Requests an authorization code.
     * 
     * Kustvakt supports authorization only with Kalamar as the
     * authorization web-frontend or user interface. Thus
     * authorization code request requires user authentication
     * using authorization header.
     * 
     * <br /><br />
     * RFC 6749:
     * If the client omits the scope parameter when requesting
     * authorization, the authorization server MUST either process the
     * request using a pre-defined default value or fail the request
     * indicating an invalid scope.
     * 
     * @param request
     *            HttpServletRequest
     * @param form
     *            form parameters
     * @return a redirect URL
     */
    @Deprecated
    @POST
    @Path("authorize")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response requestAuthorizationCode (
            @Context HttpServletRequest request,
            @Context SecurityContext context, 
            @FormParam("scope") String scope,
            @FormParam("state") String state,
            @FormParam("client_id") String clientId,
            @FormParam("redirect_uri") String redirectUri,
            MultivaluedMap<String, String> form) {

        TokenContext tokenContext = (TokenContext) context.getUserPrincipal();
        String username = tokenContext.getUsername();
        ZonedDateTime authTime = tokenContext.getAuthenticationTime();

        URI requestURI;
        UriBuilder builder = UriBuilder.fromPath(request.getRequestURI());
        for (String key : form.keySet()) {
            builder.queryParam(key, form.get(key).toArray());
        }
        requestURI = builder.build();
       
        try {
            scopeService.verifyScope(tokenContext, OAuth2Scope.AUTHORIZE);
            URI uri = authorizationService.requestAuthorizationCode(
                    requestURI, clientId, redirectUri,
                    scope, state, username, authTime);
            return responseHandler.sendRedirect(uri);
        }
        catch (KustvaktException e) {
            e = authorizationService.checkRedirectUri(e, clientId, redirectUri);
            if (e.getRedirectUri() != null) {
                AuthorizationErrorResponse errorResponse =
                        authorizationService.createAuthorizationError(e, state);
                return responseHandler.sendRedirect(errorResponse.toURI());
            }
            else {
                throw responseHandler.throwit(e, state);
            } 
        }
    }
    
    @GET
    @Path("authorize")
    public Response requestAuthorizationCode (
            @Context HttpServletRequest request,
            @Context SecurityContext context,
            @QueryParam("response_type") String responseType,
            @QueryParam("client_id") String clientId,
            @QueryParam("redirect_uri") String redirectUri,
            @QueryParam("scope") String scope,
            @QueryParam("state") String state) {

        TokenContext tokenContext = (TokenContext) context.getUserPrincipal();
        String username = tokenContext.getUsername();
        ZonedDateTime authTime = tokenContext.getAuthenticationTime();

        URI requestURI;
        try {
            requestURI = new URI(request.getRequestURI()+"?"+request.getQueryString());
        }
        catch (URISyntaxException e) {
            KustvaktException ke = new KustvaktException(
                    StatusCodes.INVALID_REQUEST, "Failed parsing request URI.",
                    OAuth2Error.INVALID_REQUEST_URI);
            throw responseHandler.throwit(ke, state);
        }
        
        try {
            scopeService.verifyScope(tokenContext, OAuth2Scope.AUTHORIZE);
            URI uri = authorizationService.requestAuthorizationCode(
                    requestURI, clientId, redirectUri,
                    scope, state, username, authTime);
            return responseHandler.sendRedirect(uri);
        }
        catch (KustvaktException e) {
            e = authorizationService.checkRedirectUri(e, clientId, redirectUri);
            if (e.getRedirectUri() != null) {
                AuthorizationErrorResponse errorResponse =
                        authorizationService.createAuthorizationError(e, state);
                return responseHandler.sendRedirect(errorResponse.toURI());
            }
            else {
                throw responseHandler.throwit(e, state);
            } 
        }
    }

    /**
     * Grants a client an access token, namely a string used in
     * authenticated requests representing user authorization for
     * the client to access user resources. An additional refresh
     * token strictly associated to the access token is also granted
     * for confidential clients. Both public and confidential clients
     * may issue multiple access tokens.
     * 
     * <br /><br />
     * 
     * Confidential clients may request refresh access token using
     * this endpoint. This request will grant a new access token.
     * 
     * Usually the given refresh token is not changed and can be used
     * until it expires. However, currently there is a limitation of
     * one access token per one refresh token. Thus, the given refresh
     * token will be revoked, and a new access token and a new refresh
     * token will be returned.
     * 
     * <br /><br />
     * 
     * Client credentials for authentication can be provided either as
     * an authorization header with Basic authentication scheme or as
     * form parameters in the request body.
     * 
     * <br /><br />
     * 
     * OAuth2 specification describes various ways of requesting an
     * access token. Kustvakt supports:
     * <ul>
     * <li> Authorization code grant: obtains authorization from a
     * third party application. Required parameters: grant_type,
     * code, client_id, redirect_uri (if specified in the
     * authorization request), client_secret (if the client is
     * confidential or issued a secret).
     * </li>
     * <li> Resource owner password grant: strictly for clients that
     * are parts of KorAP. Clients use user credentials, e.g. Kalamar
     * (front-end) with login form. Required parameters: grant_type,
     * username, password, client_id, client_secret (if the client is
     * confidential or issued a secret). Optional parameters: scope.
     * </li>
     * <li> Client credentials grant: strictly for clients that are
     * parts of KorAP. Clients access their own resources, not on
     * behalf of a user. Required parameters: grant_type, client_id,
     * client_secret. Optional parameters: scope.
     * </li>
     * </ul>
     * 
     * RFC 6749: The value of the scope parameter is expressed as a
     * list of space-delimited, case-sensitive strings defined by the
     * authorization server.
     * 
     * @param request
     *            the request
     * @param form
     *            form parameters in a map
     * @return a JSON object containing an access token, a refresh
     *         token, a token type and the token expiration in seconds
     *         if successful, an error code and an error description
     *         otherwise.
     */
    @POST
    @Path("token")
    @ResourceFilters({APIVersionFilter.class})
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response requestAccessToken (@Context HttpServletRequest request,
            @FormParam("client_id") String clientId,
            @FormParam("client_secret") String clientSecret,
            MultivaluedMap<String, String> form) {

        OAuthResponse oAuthResponse = null;
        try {
            URI requestURI;
            UriBuilder builder = UriBuilder.fromPath(
                    request.getRequestURL().toString());
            for (String key : form.keySet()) {
                builder.queryParam(key, form.get(key).toArray());
            }
            requestURI = builder.build();
            
            try {
                AuthorizationGrant authGrant = AuthorizationGrant.parse(form);  
                
                ClientAuthentication clientAuth = null;
                String authorizationHeader = request.getHeader("Authorization");
                if (authorizationHeader!=null && !authorizationHeader.isEmpty() ) {
                    clientAuth = ClientSecretBasic.parse(authorizationHeader);
                }
                else if (authGrant instanceof ClientCredentialsGrant) {
                    // this doesn't allow public clients
                    clientAuth = ClientSecretPost.parse(form);
                }
                
                TokenRequest tokenRequest = null;
                if (clientAuth!=null) {
                    ClientAuthenticationMethod method = clientAuth.getMethod();
                    if (method.equals(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)) {
                        ClientSecretBasic basic = (ClientSecretBasic) clientAuth;
                        clientSecret = basic.getClientSecret().getValue();
                        clientId = basic.getClientID().getValue();
                    }
                    else if (method.equals(ClientAuthenticationMethod.CLIENT_SECRET_POST)) {
                        ClientSecretPost post = (ClientSecretPost) clientAuth;
                        clientSecret = post.getClientSecret().getValue();
                        clientId = post.getClientID().getValue();
                    }
                    
                    tokenRequest = new TokenRequest(requestURI,
                            clientAuth,
                            AuthorizationGrant.parse(form),
                            Scope.parse(form.getFirst("scope")));
                }
                else {
                    // requires ClientAuthentication for client_credentials grant
                    tokenRequest = new TokenRequest(requestURI,
                        new ClientID(clientId),
                        AuthorizationGrant.parse(form),
                        Scope.parse(form.getFirst("scope")));
                }
            
                oAuthResponse = tokenService.requestAccessToken(tokenRequest,
                        clientId, clientSecret);
            }
            catch (ParseException | IllegalArgumentException e) {
                throw new KustvaktException(StatusCodes.INVALID_REQUEST,
                        e.getMessage(), OAuth2Error.INVALID_REQUEST); 
            }
            
        }
        catch (KustvaktException e) {
            throw responseHandler.throwit(e);
        }
        catch (OAuthSystemException e) {
            throw responseHandler.throwit(e);
        }
        
        return responseHandler.createResponse(oAuthResponse);
    }

    /**
     * Revokes either an access token or a refresh token. Revoking a
     * refresh token also revokes all access token associated with the
     * refresh token.
     * 
     * RFC 7009
     * Client authentication for confidential client
     * 
     * @param request
     * @param form
     *            containing
     *            client_id,
     *            client_secret (required for confidential clients),
     *            token,
     *            token_type (optional)
     * @return 200 if token invalidation is successful or the given
     *         token is invalid
     */
//    @POST
//    @Path("revoke")
//    @ResourceFilters({APIVersionFilter.class})
//    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
//    public Response revokeAccessToken (@Context HttpServletRequest request,
//            MultivaluedMap<String, String> form) {
//
//        try {
//            OAuth2RevokeTokenRequest revokeTokenRequest =
//                    new OAuth2RevokeTokenRequest(
//                            new FormRequestWrapper(request, form));
//            tokenService.revokeToken(revokeTokenRequest);
//            return Response.ok("SUCCESS").build();
//        }
//        catch (OAuthProblemException e) {
//            throw responseHandler.throwit(e);
//        }
//        catch (OAuthSystemException e) {
//            throw responseHandler.throwit(e);
//        }
//        catch (KustvaktException e) {
//            throw responseHandler.throwit(e);
//        }
//    }
//
//    @POST
//    @Path("revoke/super")
//    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
//    public Response revokeTokenViaSuperClient (@Context SecurityContext context,
//            @Context HttpServletRequest request,
//            MultivaluedMap<String, String> form) {
//
//        TokenContext tokenContext = (TokenContext) context.getUserPrincipal();
//        String username = tokenContext.getUsername();
//
//        try {
//            OAuth2RevokeTokenSuperRequest revokeTokenRequest =
//                    new OAuth2RevokeTokenSuperRequest(
//                            new FormRequestWrapper(request, form));
//            tokenService.revokeTokensViaSuperClient(username,
//                    revokeTokenRequest);
//            return Response.ok("SUCCESS").build();
//        }
//        catch (OAuthSystemException e) {
//            throw responseHandler.throwit(e);
//        }
//        catch (OAuthProblemException e) {
//            throw responseHandler.throwit(e);
//        }
//        catch (KustvaktException e) {
//            throw responseHandler.throwit(e);
//        }
//    }

    /**
     * Revokes all tokens of a client for the authenticated user from
     * a super client. This service is not part of the OAUTH2
     * specification. It requires user authentication via
     * authorization header, and super client
     * via URL-encoded form parameters.
     * 
     * @param request
     * @param form
     *            containing client_id, super_client_id,
     *            super_client_secret
     * @return 200 if token invalidation is successful or the given
     *         token is invalid
     */
//    @POST
//    @Path("revoke/super/all")
//    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
//    public Response revokeAllClientTokensViaSuperClient (
//            @Context SecurityContext context,
//            @Context HttpServletRequest request,
//            MultivaluedMap<String, String> form) {
//
//        TokenContext tokenContext = (TokenContext) context.getUserPrincipal();
//        String username = tokenContext.getUsername();
//
//        try {
//            OAuth2RevokeAllTokenSuperRequest revokeTokenRequest =
//                    new OAuth2RevokeAllTokenSuperRequest(
//                            new FormRequestWrapper(request, form));
//            tokenService.revokeAllClientTokensViaSuperClient(username,
//                    revokeTokenRequest);
//            return Response.ok("SUCCESS").build();
//        }
//        catch (OAuthSystemException e) {
//            throw responseHandler.throwit(e);
//        }
//        catch (OAuthProblemException e) {
//            throw responseHandler.throwit(e);
//        }
//        catch (KustvaktException e) {
//            throw responseHandler.throwit(e);
//        }
//    }
//
//    @POST
//    @Path("token/list")
//    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
//    public List<OAuth2TokenDto> listUserToken (
//            @Context SecurityContext context,
//            @FormParam("super_client_id") String superClientId,
//            @FormParam("super_client_secret") String superClientSecret,
//            @FormParam("client_id") String clientId, // optional
//            @FormParam("token_type") String tokenType) {
//
//        TokenContext tokenContext = (TokenContext) context.getUserPrincipal();
//        String username = tokenContext.getUsername();
//
//        try {
//            if (tokenType.equals("access_token")) {
//                return tokenService.listUserAccessToken(username, superClientId,
//                        superClientSecret, clientId);
//            }
//            else if (tokenType.equals("refresh_token")) {
//                return tokenService.listUserRefreshToken(username,
//                        superClientId, superClientSecret, clientId);
//            }
//            else {
//                throw new KustvaktException(StatusCodes.MISSING_PARAMETER,
//                        "Missing token_type parameter value",
//                        OAuth2Error.INVALID_REQUEST);
//            }
//        }
//        catch (KustvaktException e) {
//            throw responseHandler.throwit(e);
//        }
//
//    }
}
