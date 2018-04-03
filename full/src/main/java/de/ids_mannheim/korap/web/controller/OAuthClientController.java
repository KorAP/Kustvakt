package de.ids_mannheim.korap.web.controller;

import javax.ws.rs.Consumes;
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

import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.service.OAuth2ClientService;
import de.ids_mannheim.korap.web.FullResponseHandler;
import de.ids_mannheim.korap.web.filter.AuthenticationFilter;
import de.ids_mannheim.korap.web.filter.BlockingFilter;
import de.ids_mannheim.korap.web.input.OAuth2ClientJson;


@Controller
@Path("/client")
//@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
//@ResourceFilters({ AuthenticationFilter.class, BlockingFilter.class })
public class OAuthClientController {

    @Autowired
    private OAuth2ClientService clientService;
    @Autowired
    private FullResponseHandler responseHandler;

    /** EM: who can register a client?
     * 
     * The authorization server SHOULD document the size of any identifier 
     * it issues.
     * 
     * @param context
     * @param clientJson
     * @return
     */
    @POST
    @Path("register")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response registerClient (@Context SecurityContext context,
            OAuth2ClientJson clientJson) {
        try {
            clientService.registerClient(clientJson);
        }
        catch (KustvaktException e) {
            responseHandler.throwit(e);
        }
        return Response.ok().build();
    }

}
