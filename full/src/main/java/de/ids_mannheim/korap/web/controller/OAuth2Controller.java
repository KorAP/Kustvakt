package de.ids_mannheim.korap.web.controller;

import java.time.ZonedDateTime;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.apache.oltu.oauth2.as.request.AbstractOAuthTokenRequest;
import org.apache.oltu.oauth2.as.request.OAuthTokenRequest;
import org.apache.oltu.oauth2.as.request.OAuthUnauthenticatedTokenRequest;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.OAuthResponse;
import org.apache.oltu.oauth2.common.message.types.GrantType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.sun.jersey.spi.container.ResourceFilters;

import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.oauth2.constant.OAuth2Scope;
import de.ids_mannheim.korap.oauth2.oltu.OAuth2AuthorizationRequest;
import de.ids_mannheim.korap.oauth2.oltu.OAuth2RevokeTokenRequest;
import de.ids_mannheim.korap.oauth2.oltu.service.OltuAuthorizationService;
import de.ids_mannheim.korap.oauth2.oltu.service.OltuTokenService;
import de.ids_mannheim.korap.oauth2.service.OAuth2ScopeService;
import de.ids_mannheim.korap.security.context.TokenContext;
import de.ids_mannheim.korap.web.OAuth2ResponseHandler;
import de.ids_mannheim.korap.web.filter.AuthenticationFilter;
import de.ids_mannheim.korap.web.filter.BlockingFilter;
import de.ids_mannheim.korap.web.utils.FormRequestWrapper;

@Controller
@Path("oauth2")
public class OAuth2Controller {

    @Autowired
    private OAuth2ResponseHandler responseHandler;
    @Autowired
    private OltuTokenService tokenService;
    @Autowired
    private OltuAuthorizationService authorizationService;
    @Autowired
    private OAuth2ScopeService scopeService;

    /**
     * Requests an authorization code.
     * 
     * Kustvakt supports authorization only with Kalamar as the
     * authorization web-frontend or user interface. Thus
     * authorization code request requires user authentication
     * using authentication header.
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
    @POST
    @Path("authorize")
    @ResourceFilters({ AuthenticationFilter.class, BlockingFilter.class })
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response requestAuthorizationCode (
            @Context HttpServletRequest request,
            @Context SecurityContext context, @FormParam("state") String state,
            MultivaluedMap<String, String> form) {

        TokenContext tokenContext = (TokenContext) context.getUserPrincipal();
        String username = tokenContext.getUsername();
        ZonedDateTime authTime = tokenContext.getAuthenticationTime();

        try {
            scopeService.verifyScope(tokenContext, OAuth2Scope.AUTHORIZE);

            HttpServletRequest requestWithForm =
                    new FormRequestWrapper(request, form);
            OAuth2AuthorizationRequest authzRequest =
                    new OAuth2AuthorizationRequest(requestWithForm);
            String uri = authorizationService.requestAuthorizationCode(
                    requestWithForm, authzRequest, username, authTime);
            return responseHandler.sendRedirect(uri);
        }
        catch (OAuthSystemException e) {
            throw responseHandler.throwit(e, state);
        }
        catch (OAuthProblemException e) {
            throw responseHandler.throwit(e, state);
        }
        catch (KustvaktException e) {
            throw responseHandler.throwit(e, state);
        }
    }

    /**
     * Grants a client an access token, namely a string used in
     * authenticated requests representing user authorization for
     * the client to access user resources. An additional refresh
     * token strictly associated to the access token is also granted.
     * 
     * <br /><br />
     * 
     * Clients may request refresh access token using this endpoint.
     * This request will grants a new access token. The refresh token
     * is not changed and can be used until it expires.
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
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response requestAccessToken (@Context HttpServletRequest request,
            @FormParam("grant_type") String grantType,
            MultivaluedMap<String, String> form) {

        try {
            boolean grantTypeExist = grantType != null && !grantType.isEmpty();
            AbstractOAuthTokenRequest oAuthRequest = null;
            if (grantTypeExist && grantType
                    .equals(GrantType.CLIENT_CREDENTIALS.toString())) {
                oAuthRequest = new OAuthTokenRequest(
                        new FormRequestWrapper(request, form));
            }
            else {
                oAuthRequest = new OAuthUnauthenticatedTokenRequest(
                        new FormRequestWrapper(request, form));
            }

            OAuthResponse oAuthResponse =
                    tokenService.requestAccessToken(oAuthRequest);

            return responseHandler.createResponse(oAuthResponse);
        }
        catch (KustvaktException e) {
            throw responseHandler.throwit(e);
        }
        catch (OAuthProblemException e) {
            throw responseHandler.throwit(e);
        }
        catch (OAuthSystemException e) {
            throw responseHandler.throwit(e);
        }
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
    @POST
    @Path("revoke")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response revokeAccessToken (@Context HttpServletRequest request,
            MultivaluedMap<String, String> form) {

        try {
            OAuth2RevokeTokenRequest revokeTokenRequest =
                    new OAuth2RevokeTokenRequest(
                            new FormRequestWrapper(request, form));
            tokenService.revokeToken(revokeTokenRequest);
            return Response.ok().build();
        }
        catch (OAuthProblemException e) {
            throw responseHandler.throwit(e);
        }
        catch (OAuthSystemException e) {
            throw responseHandler.throwit(e);
        }
        catch (KustvaktException e) {
            throw responseHandler.throwit(e);
        }
    }
}
