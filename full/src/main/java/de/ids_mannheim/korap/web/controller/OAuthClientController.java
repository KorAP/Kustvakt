package de.ids_mannheim.korap.web.controller;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.sun.jersey.spi.container.ResourceFilters;

import de.ids_mannheim.korap.dto.OAuth2ClientDto;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.security.context.TokenContext;
import de.ids_mannheim.korap.service.OAuth2ClientService;
import de.ids_mannheim.korap.web.KustvaktExceptionHandler;
import de.ids_mannheim.korap.web.OAuth2ExceptionHandler;
import de.ids_mannheim.korap.web.filter.AuthenticationFilter;
import de.ids_mannheim.korap.web.filter.BlockingFilter;
import de.ids_mannheim.korap.web.input.OAuth2ClientJson;


/** Defines controllers for OAuth2 clients, namely applications attempting
 * to access users' resources. 
 * 
 * @author margaretha
 *
 */
@Controller
@Path("/oauth2/client")
public class OAuthClientController {

    @Autowired
    private OAuth2ClientService clientService;
    @Autowired
    private OAuth2ExceptionHandler responseHandler;

    /** Registers a client application. Before starting an OAuth process, 
     * client applications have to be registered first. Only registered
     * users are allowed to register client applications. After registration,
     * the client will receive a client_id and a client_secret, if the client 
     * is confidential (capable of storing the client_secret), that are needed 
     * in the authorization process.
     * 
     * From RFC 6749:
     * The authorization server SHOULD document the size of any identifier 
     * it issues.
     * 
     * @param context
     * @param clientJson a JSON object describing the client
     * @return client_id and client_secret if the client type is confidential
     * 
     * @see OAuth2ClientJson
     */
    @POST
    @Path("register")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @ResourceFilters({ AuthenticationFilter.class, BlockingFilter.class })
    public OAuth2ClientDto registerClient (
            @Context SecurityContext securityContext,
            OAuth2ClientJson clientJson) {
        TokenContext context =
                (TokenContext) securityContext.getUserPrincipal();
        try {
            return clientService.registerClient(clientJson,
                    context.getUsername());
        }
        catch (KustvaktException e) {
            throw responseHandler.throwit(e);
        }
    }


    /** Deregisters a public client via owner authentication.
     * 
     * 
     * @param securityContext
     * @param clientId the client id
     * @return HTTP Response OK if successful.
     */
    @DELETE
    @Path("deregister")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @ResourceFilters({ AuthenticationFilter.class, BlockingFilter.class })
    public Response deregisterPublicClient (
            @Context SecurityContext securityContext,
            @FormParam("client_id") String clientId) {
        TokenContext context =
                (TokenContext) securityContext.getUserPrincipal();
        try {
            clientService.deregisterPublicClient(clientId,
                    context.getUsername());
            return Response.ok().build();
        }
        catch (KustvaktException e) {
            throw responseHandler.throwit(e);
        }
    }


    @DELETE
    @Path("deregister/confidential")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response deregisterConfidentialClient (
            @Context SecurityContext securityContext,
            @HeaderParam("Authorization") String authorization,
            @FormParam("client_id") String clientId) {
        try {
            clientService.deregisterConfidentialClient(authorization, clientId);
            return Response.ok().build();
        }
        catch (KustvaktException e) {
            throw responseHandler.throwit(e);
        }
    }
}
