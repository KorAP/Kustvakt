package de.ids_mannheim.korap.web.controller;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.apache.oltu.oauth2.as.request.OAuthTokenRequest;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.OAuthResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.service.OAuth2Service;
import de.ids_mannheim.korap.web.OAuth2ResponseHandler;
import de.ids_mannheim.korap.web.utils.FormRequestWrapper;

@Controller
@Path("/oauth2")
public class OAuth2Controller {

    @Autowired
    private OAuth2ResponseHandler responseHandler;
    @Autowired
    private OAuth2Service oauth2Service;

    /** Grants a client an access token, namely a string used in authenticated 
     *  requests representing user authorization for the client to access user 
     *  resources. 
     * 
     *  EM: should we allow client_secret in the request body?
     * 
     * @param securityContext
     * @param authorization
     * @param grantType
     * @param authorizationCode
     * @param redirectURI
     * @param client_id a client id required for authorization_code grant, otherwise optional
     * @param username
     * @param password
     * @param scope
     * @return
     */
    @POST
    @Path("token")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response requestAccessToken (@Context HttpServletRequest request,
            @Context SecurityContext securityContext,
            @HeaderParam("Authorization") String authorization,
            MultivaluedMap<String, String> form) {

        try {
            OAuthTokenRequest oAuthRequest = null;
            try {
                oAuthRequest = new OAuthTokenRequest(
                        new FormRequestWrapper(request, form));
            }
            catch (OAuthProblemException e) {
                throw responseHandler.throwit(e);
            }

            OAuthResponse oAuthResponse = oauth2Service
                    .requestAccessToken(oAuthRequest, authorization);

            return responseHandler.createResponse(oAuthResponse);
        }
        catch (KustvaktException e) {
            throw responseHandler.throwit(e);
        }
        catch (OAuthSystemException e) {
            throw responseHandler.throwit(e);
        }
    }
}
