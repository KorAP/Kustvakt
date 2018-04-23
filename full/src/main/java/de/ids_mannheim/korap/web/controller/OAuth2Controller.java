package de.ids_mannheim.korap.web.controller;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.oltu.oauth2.as.request.AbstractOAuthTokenRequest;
import org.apache.oltu.oauth2.as.request.OAuthAuthzRequest;
import org.apache.oltu.oauth2.as.request.OAuthTokenRequest;
import org.apache.oltu.oauth2.as.request.OAuthUnauthenticatedTokenRequest;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.OAuthResponse;
import org.apache.oltu.oauth2.common.message.types.GrantType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.service.OAuth2AuthorizationService;
import de.ids_mannheim.korap.service.OAuth2Service;
import de.ids_mannheim.korap.web.OAuth2ResponseHandler;
import de.ids_mannheim.korap.web.utils.FormRequestWrapper;

@Controller
@Path("/oauth2")
public class OAuth2Controller {

    @Autowired
    private OAuth2ResponseHandler responseHandler;
    @Autowired
    private OAuth2Service oAuth2Service;
    @Autowired
    private OAuth2AuthorizationService authorizationService;

    /** Kustvakt supports authorization only with Kalamar as the authorization 
     * web-frontend or user interface. Thus authorization code request requires
     * user credentials in the request body, similar to access token request in
     * resource owner password grant request. 
     * 
     * @param request
     * @param authorization
     * @param form
     * @return
     */
    @POST
    @Path("authorize")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response requestAuthorizationCode (
            @Context HttpServletRequest request,
            @HeaderParam("Authorization") String authorization,
            MultivaluedMap<String, String> form) {

        try {
            HttpServletRequest requestWithForm =
                    new FormRequestWrapper(request, form);
            OAuthAuthzRequest authzRequest =
                    new OAuthAuthzRequest(requestWithForm);
            OAuthResponse authResponse =
                    authorizationService.requestAuthorizationCode(
                            requestWithForm, authzRequest, authorization);
            return responseHandler.sendRedirect(authResponse.getLocationUri());
        }
        catch (OAuthSystemException e) {
            throw responseHandler.throwit(e);
        }
        catch (OAuthProblemException e) {
            throw responseHandler.throwit(e);
        }
        catch (KustvaktException e) {
            throw responseHandler.throwit(e);
        }
    }


    /** Grants a client an access token, namely a string used in authenticated 
     *  requests representing user authorization for the client to access user 
     *  resources. 
     * 
     * @param request the request
     * @param authorization authorization header
     * @param form form parameters in a map
     * @return a JSON object containing an access token, a refresh token, 
     *  a token type and token expiry/life time (in seconds) if successful, 
     *  an error code and an error description otherwise.
     */
    @POST
    @Path("token")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response requestAccessToken (@Context HttpServletRequest request,
            @FormParam("grant_type") String grantType,
            MultivaluedMap<String, String> form) {

        try {
            AbstractOAuthTokenRequest oAuthRequest = null;
            if (grantType != null && !grantType.isEmpty() && grantType
                    .equals(GrantType.CLIENT_CREDENTIALS.toString())) {
                oAuthRequest = new OAuthTokenRequest(
                        new FormRequestWrapper(request, form));
            }
            else {
                oAuthRequest = new OAuthUnauthenticatedTokenRequest(
                        new FormRequestWrapper(request, form));
            }

            OAuthResponse oAuthResponse =
                    oAuth2Service.requestAccessToken(oAuthRequest);

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
}
