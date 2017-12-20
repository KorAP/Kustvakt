package de.ids_mannheim.korap.web.controller;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.sun.jersey.spi.container.ResourceFilters;

import de.ids_mannheim.korap.dto.UserGroupDto;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.service.UserGroupService;
import de.ids_mannheim.korap.user.TokenContext;
import de.ids_mannheim.korap.utils.JsonUtils;
import de.ids_mannheim.korap.web.FullResponseHandler;
import de.ids_mannheim.korap.web.filter.AuthenticationFilter;
import de.ids_mannheim.korap.web.filter.DemoUserFilter;
import de.ids_mannheim.korap.web.filter.PiwikFilter;

@Controller
@Path("group")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
@ResourceFilters({ AuthenticationFilter.class, DemoUserFilter.class,
        PiwikFilter.class })
public class UserGroupController {

    private static Logger jlog =
            LoggerFactory.getLogger(UserGroupController.class);

    @Autowired
    private FullResponseHandler responseHandler;
    @Autowired
    private UserGroupService service;
    
    @GET
    @Path("user")
    public Response getUserGroup (@Context SecurityContext securityContext){
        String result;
        TokenContext context =
                (TokenContext) securityContext.getUserPrincipal();
        try {
            if (context.isDemo()) {
                throw new KustvaktException(StatusCodes.AUTHORIZATION_FAILED,
                        "Operation is not permitted for user: "
                                + context.getUsername(),
                        context.getUsername());
            }

            List<UserGroupDto> dtos =
                    service.retrieveUserGroup(context.getUsername());
            result = JsonUtils.toJSON(dtos);
        }
        catch (KustvaktException e) {
            throw responseHandler.throwit(e);
        }
        return Response.ok(result).build();
    }
}
