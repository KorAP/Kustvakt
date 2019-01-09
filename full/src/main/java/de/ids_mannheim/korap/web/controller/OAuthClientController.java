package de.ids_mannheim.korap.web.controller;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.sun.jersey.spi.container.ResourceFilters;

import de.ids_mannheim.korap.constant.OAuth2Scope;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.oauth2.dto.OAuth2ClientDto;
import de.ids_mannheim.korap.oauth2.dto.OAuth2ClientInfoDto;
import de.ids_mannheim.korap.oauth2.dto.OAuth2UserClientDto;
import de.ids_mannheim.korap.oauth2.service.OAuth2ClientService;
import de.ids_mannheim.korap.oauth2.service.OAuth2ScopeService;
import de.ids_mannheim.korap.security.context.TokenContext;
import de.ids_mannheim.korap.web.OAuth2ResponseHandler;
import de.ids_mannheim.korap.web.filter.APIVersionFilter;
import de.ids_mannheim.korap.web.filter.AuthenticationFilter;
import de.ids_mannheim.korap.web.filter.BlockingFilter;
import de.ids_mannheim.korap.web.input.OAuth2ClientJson;

/**
 * Defines controllers for OAuth2 clients, namely applications
 * performing actions such as searching and retrieving match
 * information on behalf of users.
 * 
 * <br /><br />
 * According to its privileges, clients are categorized into super and
 * normal clients. Super clients are intended only for clients that
 * are part of KorAP. They has special privileges to use controllers
 * that usually are not allowed for normal clients, for instance using
 * OAuth2 password grant to obtain access tokens.
 * 
 * <br /><br />
 * By default, clients are set as normal clients. Super clients has to
 * be set manually by an admin, e.g by using
 * {@link #updateClientPrivilege(SecurityContext, String, boolean)}
 * controller. Only confidential clients are allowed to be super
 * clients.
 * 
 * @author margaretha
 *
 */
@Controller
@Path("{version}/oauth2/client")
@ResourceFilters({ APIVersionFilter.class, AuthenticationFilter.class,
        BlockingFilter.class })
public class OAuthClientController {

    @Autowired
    private OAuth2ClientService clientService;
    @Autowired
    private OAuth2ScopeService scopeService;
    @Autowired
    private OAuth2ResponseHandler responseHandler;

    /**
     * Registers a client application. Before starting an OAuth
     * process, client applications have to be registered first. Only
     * registered users are allowed to register client applications.
     * 
     * After registration, the client receives a client_id and a
     * client_secret, if the client is confidential (capable of
     * storing the client_secret), that are needed in the
     * authorization process.
     * 
     * From RFC 6749:
     * The authorization server SHOULD document the size of any
     * identifier it issues.
     * 
     * @param context
     * @param clientJson
     *            a JSON object describing the client
     * @return client_id and client_secret if the client type is
     *         confidential
     * 
     * @see OAuth2ClientJson
     */
    @POST
    @Path("register")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public OAuth2ClientDto registerClient (
            @Context SecurityContext securityContext,
            OAuth2ClientJson clientJson) {
        TokenContext context =
                (TokenContext) securityContext.getUserPrincipal();
        try {
            scopeService.verifyScope(context, OAuth2Scope.REGISTER_CLIENT);
            return clientService.registerClient(clientJson,
                    context.getUsername());
        }
        catch (KustvaktException e) {
            throw responseHandler.throwit(e);
        }
    }

    /**
     * Deregisters a client requires client owner authentication. For
     * confidential clients, client authentication is also required.
     * 
     * 
     * @param securityContext
     * @param clientId
     *            the client id
     * @param clientSecret
     *            the client secret
     * @return HTTP Response OK if successful.
     */
    @DELETE
    @Path("deregister/{client_id}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response deregisterPublicClient (
            @Context SecurityContext securityContext,
            @PathParam("client_id") String clientId,
            @FormParam("client_secret") String clientSecret) {
        TokenContext context =
                (TokenContext) securityContext.getUserPrincipal();
        try {
            scopeService.verifyScope(context, OAuth2Scope.DEREGISTER_CLIENT);
            clientService.deregisterClient(clientId, clientSecret,
                    context.getUsername());
            return Response.ok().build();
        }
        catch (KustvaktException e) {
            throw responseHandler.throwit(e);
        }
    }

    /**
     * Resets client secret of the given client. This controller
     * requires client owner and client authentication. Only
     * confidential clients are issued client secrets.
     * 
     * @param securityContext
     * @param clientId
     * @param clientSecret
     * @return a new client secret
     */
    @POST
    @Path("reset")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public OAuth2ClientDto resetClientSecret (
            @Context SecurityContext securityContext,
            @FormParam("client_id") String clientId,
            @FormParam("client_secret") String clientSecret) {
        TokenContext context =
                (TokenContext) securityContext.getUserPrincipal();
        try {
            scopeService.verifyScope(context, OAuth2Scope.RESET_CLIENT_SECRET);
            return clientService.resetSecret(clientId, clientSecret,
                    context.getUsername());
        }
        catch (KustvaktException e) {
            throw responseHandler.throwit(e);
        }
    }

    /**
     * Facilitates editing client privileges for admin purposes, e.g.
     * setting a specific client to be a super client.
     * Only confidential clients are allowed to be super clients.
     * 
     * When upgrading clients to super clients, existing access tokens
     * and authorization codes retain their scopes.
     * 
     * When degrading super clients, all existing tokens and
     * authorization codes are invalidated.
     * 
     * @param securityContext
     * @param clientId
     *            OAuth2 client id
     * @param super
     *            true indicating super client, false otherwise
     * @return Response status OK, if successful
     */
    @POST
    @Path("privilege")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response updateClientPrivilege (
            @Context SecurityContext securityContext,
            @FormParam("client_id") String clientId,
            @FormParam("super") String isSuper) {
        TokenContext context =
                (TokenContext) securityContext.getUserPrincipal();
        try {
            scopeService.verifyScope(context, OAuth2Scope.ADMIN);
            clientService.updatePrivilege(context.getUsername(), clientId,
                    Boolean.valueOf(isSuper));
            return Response.ok().build();
        }
        catch (KustvaktException e) {
            throw responseHandler.throwit(e);
        }
    }

    @GET
    @Path("info/{client_id}")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public OAuth2ClientInfoDto retrieveClientInfo (
            @Context SecurityContext securityContext,
            @PathParam("client_id") String clientId) {
        TokenContext context =
                (TokenContext) securityContext.getUserPrincipal();
        try {
            scopeService.verifyScope(context, OAuth2Scope.CLIENT_INFO);
            return clientService.retrieveClientInfo(context.getUsername(),
                    clientId);
        }
        catch (KustvaktException e) {
            throw responseHandler.throwit(e);
        }
    }

    /**
     * Lists user clients having active refresh tokens (not revoked,
     * not expired), except super clients.
     * 
     * This service is not part of the OAuth2 specification. It is
     * intended to facilitate users revoking any suspicious and
     * misused access or refresh tokens.
     * 
     * Only super clients are allowed to use this service. It requires
     * user and client authentications.
     * 
     * @param context
     * @return a list of clients having refresh tokens of the
     *         given user
     */
    @POST
    @Path("list")
    @ResourceFilters({ AuthenticationFilter.class, BlockingFilter.class })
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public List<OAuth2UserClientDto> listUserApp (
            @Context SecurityContext context,
            @FormParam("client_id") String clientId,
            @FormParam("client_secret") String clientSecret) {

        TokenContext tokenContext = (TokenContext) context.getUserPrincipal();
        String username = tokenContext.getUsername();

        try {
            scopeService.verifyScope(tokenContext,
                    OAuth2Scope.LIST_USER_CLIENT);

            return clientService.listUserClients(username, clientId,
                    clientSecret);
        }
        catch (KustvaktException e) {
            throw responseHandler.throwit(e);
        }
    }

}
