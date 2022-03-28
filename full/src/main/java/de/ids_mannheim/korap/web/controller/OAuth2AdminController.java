package de.ids_mannheim.korap.web.controller;

import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.sun.jersey.spi.container.ResourceFilters;

import de.ids_mannheim.korap.constant.OAuth2Scope;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.oauth2.service.OAuth2AdminService;
import de.ids_mannheim.korap.oauth2.service.OAuth2ScopeService;
import de.ids_mannheim.korap.security.context.TokenContext;
import de.ids_mannheim.korap.web.OAuth2ResponseHandler;
import de.ids_mannheim.korap.web.filter.APIVersionFilter;
import de.ids_mannheim.korap.web.filter.AdminFilter;

@Controller
@Path("{version}/oauth2/admin")
@ResourceFilters({ APIVersionFilter.class, AdminFilter.class })
public class OAuth2AdminController {

    @Autowired
    private OAuth2AdminService adminService;
    @Autowired
    private OAuth2ScopeService scopeService;
    @Autowired
    private OAuth2ResponseHandler responseHandler;

    @Path("token/clean")
    public Response cleanExpiredInvalidToken (
            @Context SecurityContext securityContext) {

        TokenContext context =
                (TokenContext) securityContext.getUserPrincipal();

        try {
            scopeService.verifyScope(context, OAuth2Scope.ADMIN);
            adminService.cleanTokens();

        }
        catch (KustvaktException e) {
            throw responseHandler.throwit(e);
        }
        return Response.ok().build();
    }
}
