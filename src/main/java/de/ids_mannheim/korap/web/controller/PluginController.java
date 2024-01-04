package de.ids_mannheim.korap.web.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import de.ids_mannheim.korap.constant.OAuth2Scope;
import de.ids_mannheim.korap.dto.InstalledPluginDto;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.oauth2.dto.OAuth2ClientInfoDto;
import de.ids_mannheim.korap.oauth2.service.OAuth2ClientService;
import de.ids_mannheim.korap.oauth2.service.OAuth2ScopeService;
import de.ids_mannheim.korap.security.context.TokenContext;
import de.ids_mannheim.korap.web.OAuth2ResponseHandler;
import de.ids_mannheim.korap.web.filter.APIVersionFilter;
import de.ids_mannheim.korap.web.filter.AuthenticationFilter;
import de.ids_mannheim.korap.web.filter.BlockingFilter;
import de.ids_mannheim.korap.web.utils.ResourceFilters;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

@Controller
@Path("{version}/plugins")
@ResourceFilters({ APIVersionFilter.class, AuthenticationFilter.class,
        BlockingFilter.class })
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class PluginController {

    @Autowired
    private OAuth2ScopeService scopeService;
    @Autowired
    private OAuth2ClientService clientService;
    @Autowired
    private OAuth2ResponseHandler responseHandler;

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public List<OAuth2ClientInfoDto> listPlugins (
            @Context SecurityContext context,
            @FormParam("super_client_id") String superClientId,
            @FormParam("super_client_secret") String superClientSecret,
            @FormParam("permitted_only") boolean permittedOnly) {

        TokenContext tokenContext = (TokenContext) context.getUserPrincipal();

        try {
            scopeService.verifyScope(tokenContext,
                    OAuth2Scope.LIST_USER_CLIENT);

            clientService.verifySuperClient(superClientId, superClientSecret);
            return clientService.listPlugins(permittedOnly);
        }
        catch (KustvaktException e) {
            throw responseHandler.throwit(e);
        }
    }

    @POST
    @Path("/install")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public InstalledPluginDto installPlugin (@Context SecurityContext context,
            @FormParam("super_client_id") String superClientId,
            @FormParam("super_client_secret") String superClientSecret,
            @FormParam("client_id") String clientId) {

        TokenContext tokenContext = (TokenContext) context.getUserPrincipal();
        String username = tokenContext.getUsername();

        try {
            scopeService.verifyScope(tokenContext,
                    OAuth2Scope.INSTALL_USER_CLIENT);

            clientService.verifySuperClient(superClientId, superClientSecret);
            return clientService.installPlugin(superClientId, clientId,
                    username);
        }
        catch (KustvaktException e) {
            throw responseHandler.throwit(e);
        }
    }

    @POST
    @Path("/installed")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public List<InstalledPluginDto> listInstalledPlugins (
            @Context SecurityContext context,
            @FormParam("super_client_id") String superClientId,
            @FormParam("super_client_secret") String superClientSecret) {

        TokenContext tokenContext = (TokenContext) context.getUserPrincipal();

        try {
            scopeService.verifyScope(tokenContext,
                    OAuth2Scope.LIST_USER_CLIENT);

            clientService.verifySuperClient(superClientId, superClientSecret);
            return clientService.listInstalledPlugins(superClientId,
                    tokenContext.getUsername());
        }
        catch (KustvaktException e) {
            throw responseHandler.throwit(e);
        }
    }

    @POST
    @Path("/uninstall")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response uninstallPlugin (@Context SecurityContext context,
            @FormParam("super_client_id") String superClientId,
            @FormParam("super_client_secret") String superClientSecret,
            @FormParam("client_id") String clientId) {

        TokenContext tokenContext = (TokenContext) context.getUserPrincipal();
        String username = tokenContext.getUsername();

        try {
            scopeService.verifyScope(tokenContext,
                    OAuth2Scope.UNINSTALL_USER_CLIENT);

            clientService.verifySuperClient(superClientId, superClientSecret);
            clientService.uninstallPlugin(superClientId, clientId, username);
            return Response.ok().build();
        }
        catch (KustvaktException e) {
            throw responseHandler.throwit(e);
        }
    }
}
