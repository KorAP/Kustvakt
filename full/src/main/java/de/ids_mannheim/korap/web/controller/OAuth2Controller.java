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
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.SecurityContext;

import org.apache.http.HttpHeaders;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.OAuthResponse;
import org.apache.oltu.oauth2.common.message.types.GrantType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.service.OAuth2Service;
import de.ids_mannheim.korap.web.FullResponseHandler;

@Controller
@Path("/oauth2")
public class OAuth2Controller {

    @Autowired
    private FullResponseHandler responseHandler;
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
            // required for all grants
            @FormParam("grant_type") GrantType grantType,
            // required for Authorization Code Grant
            @FormParam("code") String authorizationCode,
            @FormParam("redirect_uri") String redirectURI,
            @FormParam("client_id") String client_id,
            // required for Resource Owner Password Grant
            @FormParam("username") String username,
            @FormParam("password") String password,
            // optional for Resource Owner Password and Client Credentials Grants
            @FormParam("scope") String scope) {

        try {
            OAuthResponse oauth2Response = oauth2Service.requestAccessToken(request,
                    authorization, grantType, authorizationCode, redirectURI,
                    client_id, username, password, scope);

            ResponseBuilder builder =
                    Response.status(oauth2Response.getResponseStatus());
            builder.entity(oauth2Response.getBody());
            builder.header(HttpHeaders.CACHE_CONTROL, "no-store");
            builder.header(HttpHeaders.PRAGMA, "no-store");
            return builder.build();
        }
        catch (KustvaktException e) {
            throw responseHandler.throwit(e);
        }
        catch (OAuthProblemException e) {
            throw responseHandler.throwit(e);
        }
    }
}
