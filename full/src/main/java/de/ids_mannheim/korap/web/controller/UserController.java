package de.ids_mannheim.korap.web.controller;

import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
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
import de.ids_mannheim.korap.oauth2.service.OAuth2ScopeService;
import de.ids_mannheim.korap.security.context.TokenContext;
import de.ids_mannheim.korap.service.DefaultSettingService;
import de.ids_mannheim.korap.web.KustvaktResponseHandler;
import de.ids_mannheim.korap.web.filter.APIVersionFilter;
import de.ids_mannheim.korap.web.filter.AuthenticationFilter;
import de.ids_mannheim.korap.web.filter.BlockingFilter;
import de.ids_mannheim.korap.web.filter.PiwikFilter;

/**
 * @author margaretha
 *
 */
@Controller
@Path("{version}/{username: ~[a-zA-Z0-9_]+}")
@ResourceFilters({ AuthenticationFilter.class, APIVersionFilter.class,
        PiwikFilter.class })
public class UserController {

    @Autowired
    private DefaultSettingService settingService;
    @Autowired
    private KustvaktResponseHandler kustvaktResponseHandler;
    @Autowired
    private OAuth2ScopeService scopeService;

    @PUT
    @Path("setting")
    @Consumes(MediaType.APPLICATION_JSON)
    @ResourceFilters({ AuthenticationFilter.class, PiwikFilter.class,
            BlockingFilter.class })
    public Response createDefaultSetting (@Context SecurityContext context,
            @PathParam("username") String username, Map<String, Object> map) {

        TokenContext tokenContext = (TokenContext) context.getUserPrincipal();
        try {
            scopeService.verifyScope(tokenContext,
                    OAuth2Scope.CREATE_DEFAULT_SETTING);
            int statusCode = settingService.handlePutRequest(username, map,
                    tokenContext.getUsername());
            return Response.status(statusCode).build();
        }
        catch (KustvaktException e) {
            throw kustvaktResponseHandler.throwit(e);
        }

    }

    @GET
    @Path("setting")
    @ResourceFilters({ AuthenticationFilter.class, PiwikFilter.class,
            BlockingFilter.class })
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response retrieveDefaultSetting (@Context SecurityContext context,
            @PathParam("username") String username) {
        TokenContext tokenContext = (TokenContext) context.getUserPrincipal();

        try {
            scopeService.verifyScope(tokenContext,
                    OAuth2Scope.READ_DEFAULT_SETTING);
            String settings = settingService.retrieveDefaultSettings(username,
                    tokenContext.getUsername());
            return Response.ok(settings).build();
        }
        catch (KustvaktException e) {
            throw kustvaktResponseHandler.throwit(e);
        }
    }

    @DELETE
    @Path("setting/{key}")
    @Consumes(MediaType.APPLICATION_JSON)
    @ResourceFilters({ AuthenticationFilter.class, PiwikFilter.class,
            BlockingFilter.class })
    public Response createDefaultSetting (@Context SecurityContext context,
            @PathParam("username") String username,
            @PathParam("key") String key) {

        TokenContext tokenContext = (TokenContext) context.getUserPrincipal();
        try {
            scopeService.verifyScope(tokenContext,
                    OAuth2Scope.DELETE_DEFAULT_SETTING);
            settingService.deleteKey(username, tokenContext.getUsername(), key);
            return Response.ok().build();
        }
        catch (KustvaktException e) {
            throw kustvaktResponseHandler.throwit(e);
        }
    }
}
