package de.ids_mannheim.korap.web.controller;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.fasterxml.jackson.databind.JsonNode;

import de.ids_mannheim.korap.constant.OAuth2Scope;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.oauth2.service.OAuth2ScopeService;
import de.ids_mannheim.korap.security.context.TokenContext;
import de.ids_mannheim.korap.service.UserService;
import de.ids_mannheim.korap.web.KustvaktResponseHandler;
import de.ids_mannheim.korap.web.filter.APIVersionFilter;
import de.ids_mannheim.korap.web.filter.AuthenticationFilter;
import de.ids_mannheim.korap.web.utils.ResourceFilters;

@Controller
@Path("{version}/user")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
@ResourceFilters({ AuthenticationFilter.class, APIVersionFilter.class})
public class UserController {

    @Autowired
    private KustvaktResponseHandler kustvaktResponseHandler;
    @Autowired
    private OAuth2ScopeService scopeService;
    @Autowired
    private UserService userService;

    @GET
    @Path("/info")
    public JsonNode getUsername (
        @Context SecurityContext securityContext) {
            TokenContext context =
                    (TokenContext) securityContext.getUserPrincipal();
            try {
                scopeService.verifyScope(context, OAuth2Scope.USER_INFO);
                return userService.retrieveUserInfo(context.getUsername());
            }
            catch (KustvaktException e) {
                throw kustvaktResponseHandler.throwit(e);
            }
        }
}
