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
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.oauth2.service.OAuth2ScopeService;
import de.ids_mannheim.korap.security.context.TokenContext;
import de.ids_mannheim.korap.service.DefaultSettingService;
import de.ids_mannheim.korap.web.KustvaktResponseHandler;
import de.ids_mannheim.korap.web.filter.APIVersionFilter;
import de.ids_mannheim.korap.web.filter.AuthenticationFilter;
import de.ids_mannheim.korap.web.filter.BlockingFilter;
import de.ids_mannheim.korap.web.filter.PiwikFilter;

/**
 * UserSettingController defines web APIs related to user default
 * setting.
 * 
 * All the APIs in this class are only available to logged-in users.
 * 
 * @author margaretha
 *
 */
@Controller
@Path("{version}/{username: ~[a-zA-Z0-9_]+}/setting")
@ResourceFilters({ AuthenticationFilter.class, APIVersionFilter.class,
        PiwikFilter.class })
public class UserSettingController {

    @Autowired
    private DefaultSettingService settingService;
    @Autowired
    private KustvaktResponseHandler kustvaktResponseHandler;
    @Autowired
    private OAuth2ScopeService scopeService;

    /**
     * Creates a default setting of the given username.
     * The setting inputs should be represented as a pair of keys and
     * values (a map). The keys must only contains alphabets, numbers,
     * hypens or underscores.
     * 
     * 
     * @param context
     *            security context
     * @param username
     *            username
     * @param map
     *            the default setting
     * @return status code 201 if a new resource is created, or 200 if
     *         an existing resource is edited.
     */
    @PUT
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

    /**
     * Retrieves the default setting of the given username.
     * 
     * @param context
     *            a security context
     * @param username
     *            a username
     * @return the default setting of the given username
     */
    @GET
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
            if (settings == null) {
                username = tokenContext.getUsername();
                throw new KustvaktException(StatusCodes.NO_RESOURCE_FOUND,
                        "No default setting for username: " + username+" is found",
                        username);
            }
            return Response.ok(settings).build();
        }
        catch (KustvaktException e) {
            throw kustvaktResponseHandler.throwit(e);
        }
    }

    /**
     * Deletes an entry of a default setting of a user by the given
     * key.
     * 
     * @param context
     *            a security context
     * @param username
     *            a username
     * @param key
     *            the key of the default setting entry to be deleted
     * @return
     */
    @DELETE
    @Path("{key}")
    @Consumes(MediaType.APPLICATION_JSON)
    @ResourceFilters({ AuthenticationFilter.class, PiwikFilter.class,
            BlockingFilter.class })
    public Response deleteDefaultSettingEntry (@Context SecurityContext context,
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

    /**
     * Deletes the default setting of the given username. If such a
     * setting does not exists, no error will be thrown and response
     * status 200 will be returned since the purpose of the request
     * has been achieved.
     * 
     * @param context
     * @param username
     *            a username
     * @return 200 if the request is successful
     */
    @DELETE
    @ResourceFilters({ AuthenticationFilter.class, PiwikFilter.class,
            BlockingFilter.class })
    public Response deleteDefaultSetting (@Context SecurityContext context,
            @PathParam("username") String username) {

        TokenContext tokenContext = (TokenContext) context.getUserPrincipal();
        try {
            scopeService.verifyScope(tokenContext,
                    OAuth2Scope.DELETE_DEFAULT_SETTING);
            settingService.deleteSetting(username, tokenContext.getUsername());
            return Response.ok().build();
        }
        catch (KustvaktException e) {
            throw kustvaktResponseHandler.throwit(e);
        }
    }
}
