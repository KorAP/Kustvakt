package de.ids_mannheim.korap.web.controller;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

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

    @POST
    @Path("token")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response requestAccessToken (
            @Context SecurityContext securityContext,
            @HeaderParam("Authorization") String authorization,
            // required for all grants
            @FormParam("grant_type") GrantType grantType,
            // required for Authorization Code Grant
            @FormParam("code") String authorizationCode,
            @FormParam("redirect_uri") String redirectURI,
            @FormParam("client_id") String client_id,
            // required for Resource Owner Password Credentials Grant
            @FormParam("username") String username,
            @FormParam("password") String password,
            // optional for Resource Owner Password and Client Credentials Grants
            @FormParam("scope") String scope) {

        try {
            oauth2Service.requestAccessToken(authorization, grantType,
                    authorizationCode, redirectURI, client_id, username,
                    password, scope);

            return Response.ok().build();
        }
        catch (KustvaktException e) {
            throw responseHandler.throwit(e);
        }
    }
}
