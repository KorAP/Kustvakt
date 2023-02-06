package de.ids_mannheim.korap.web.controller;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.oauth2.service.OAuth2AdminService;
import de.ids_mannheim.korap.web.OAuth2ResponseHandler;
import de.ids_mannheim.korap.web.filter.APIVersionFilter;
import de.ids_mannheim.korap.web.filter.AdminFilter;
import de.ids_mannheim.korap.web.utils.ResourceFilters;

@Controller
@Path("{version}/oauth2/admin")
@ResourceFilters({ APIVersionFilter.class, AdminFilter.class })
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class OAuth2AdminController {

    @Autowired
    private OAuth2AdminService adminService;
    @Autowired
    private OAuth2ResponseHandler responseHandler;

    /**
     * Removes expired or invalid access and refresh tokens from
     * database and cache
     * 
     * @return Response status OK, if successful
     */
    @POST
    @Path("token/clean")
    public Response cleanExpiredInvalidToken () {
        adminService.cleanTokens();
        return Response.ok().build();
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
     * @param clientId
     *            OAuth2 client id
     * @param super
     *            true indicating super client, false otherwise
     * @return Response status OK, if successful
     */
    @POST
    @Path("client/privilege")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response updateClientPrivilege (
            @FormParam("client_id") String clientId,
            @FormParam("super") String isSuper) {
        try {
            adminService.updatePrivilege(clientId, Boolean.valueOf(isSuper));
            return Response.ok("SUCCESS").build();
        }
        catch (KustvaktException e) {
            throw responseHandler.throwit(e);
        }
    }
}
