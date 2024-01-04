package de.ids_mannheim.korap.web.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import de.ids_mannheim.korap.constant.OAuth2Scope;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.oauth2.dto.OAuth2ClientDto;
import de.ids_mannheim.korap.oauth2.dto.OAuth2ClientInfoDto;
import de.ids_mannheim.korap.oauth2.service.OAuth2ClientService;
import de.ids_mannheim.korap.oauth2.service.OAuth2ScopeService;
import de.ids_mannheim.korap.security.context.TokenContext;
import de.ids_mannheim.korap.web.OAuth2ResponseHandler;
import de.ids_mannheim.korap.web.filter.APIVersionFilter;
import de.ids_mannheim.korap.web.filter.AuthenticationFilter;
import de.ids_mannheim.korap.web.filter.BlockingFilter;
import de.ids_mannheim.korap.web.input.OAuth2ClientJson;
import de.ids_mannheim.korap.web.utils.ResourceFilters;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

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
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
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
    public OAuth2ClientDto registerClient (
            @Context SecurityContext securityContext,
            OAuth2ClientJson clientJson) {
        TokenContext context = (TokenContext) securityContext
                .getUserPrincipal();
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
     * Deregisters a client requires client owner authentication.
     * 
     * 
     * @param securityContext
     * @param clientId
     *            the client id
     * @return HTTP Response OK if successful.
     */
    @DELETE
    @Path("deregister/{client_id}")
    public Response deregisterClient (@Context SecurityContext securityContext,
            @PathParam("client_id") String clientId) {
        TokenContext context = (TokenContext) securityContext
                .getUserPrincipal();
        try {
            scopeService.verifyScope(context, OAuth2Scope.DEREGISTER_CLIENT);
            clientService.deregisterClient(clientId, context.getUsername());
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
    public OAuth2ClientDto resetClientSecret (
            @Context SecurityContext securityContext,
            @FormParam("client_id") String clientId) {
        TokenContext context = (TokenContext) securityContext
                .getUserPrincipal();
        try {
            scopeService.verifyScope(context, OAuth2Scope.RESET_CLIENT_SECRET);
            return clientService.resetSecret(clientId, context.getUsername());
        }
        catch (KustvaktException e) {
            throw responseHandler.throwit(e);
        }
    }

    @POST
    @Path("{client_id}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @ResourceFilters({ APIVersionFilter.class })
    public OAuth2ClientInfoDto retrieveClientInfo (
            @PathParam("client_id") String clientId,
            @FormParam("super_client_id") String superClientId,
            @FormParam("super_client_secret") String superClientSecret) {
        try {
            clientService.verifySuperClient(superClientId, superClientSecret);
            return clientService.retrieveClientInfo(clientId);
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
     * @param superClientId
     *            the client id of the super client
     * @param superClientSecret
     *            the client secret of the super client
     * @return a list of clients having refresh tokens of the
     *         given user
     */
    @POST
    @Path("/list")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public List<OAuth2ClientInfoDto> listUserClients (
            @Context SecurityContext context,
            @FormParam("super_client_id") String superClientId,
            @FormParam("super_client_secret") String superClientSecret,
            @FormParam("authorized_only") boolean authorizedOnly) {

        TokenContext tokenContext = (TokenContext) context.getUserPrincipal();
        String username = tokenContext.getUsername();

        try {
            scopeService.verifyScope(tokenContext,
                    OAuth2Scope.LIST_USER_CLIENT);

            clientService.verifySuperClient(superClientId, superClientSecret);
            if (authorizedOnly) {
                return clientService.listUserAuthorizedClients(username);
            }
            else {
                return clientService.listUserRegisteredClients(username);
            }
        }
        catch (KustvaktException e) {
            throw responseHandler.throwit(e);
        }
    }
}
