package de.ids_mannheim.korap.web.controller;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.sun.jersey.spi.container.ResourceFilters;

import de.ids_mannheim.korap.dto.VirtualCorpusDto;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.service.VirtualCorpusService;
import de.ids_mannheim.korap.user.TokenContext;
import de.ids_mannheim.korap.utils.JsonUtils;
import de.ids_mannheim.korap.utils.ParameterChecker;
import de.ids_mannheim.korap.web.FullResponseHandler;
import de.ids_mannheim.korap.web.filter.AuthenticationFilter;
import de.ids_mannheim.korap.web.filter.DemoUserFilter;
import de.ids_mannheim.korap.web.filter.PiwikFilter;
import de.ids_mannheim.korap.web.input.VirtualCorpusFromJson;

@Controller
@Path("vc")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
@ResourceFilters({ AuthenticationFilter.class, DemoUserFilter.class,
        PiwikFilter.class })
public class VirtualCorpusController {

    private static Logger jlog =
            LoggerFactory.getLogger(VirtualCorpusController.class);

    @Autowired
    private FullResponseHandler responseHandler;
    @Autowired
    private VirtualCorpusService service;

    @POST
    @Path("store")
    public Response storeVC (@Context SecurityContext securityContext,
            String json) {
        try {
            ParameterChecker.checkStringValue(json, "json string");

            // create vc object from json
            VirtualCorpusFromJson vc =
                    JsonUtils.convertToClass(json, VirtualCorpusFromJson.class);
            jlog.debug(vc.toString());

            // get user info
            TokenContext context =
                    (TokenContext) securityContext.getUserPrincipal();
            if (context.isDemo()) {
                throw new KustvaktException(StatusCodes.AUTHORIZATION_FAILED,
                        "Operation is not permitted for user: "
                                + context.getUsername(),
                        context.getUsername());
            }

            service.storeVC(vc, context.getUsername());
        }
        catch (KustvaktException e) {
            throw responseHandler.throwit(e);
        }
        return Response.ok().build();
    }

    @GET
    @Path("user")
    public Response getUserVC (@Context SecurityContext securityContext,
            @QueryParam("userId") String userId) throws KustvaktException {

        TokenContext context =
                (TokenContext) securityContext.getUserPrincipal();
        if (context.isDemo()) {
            throw new KustvaktException(StatusCodes.AUTHORIZATION_FAILED,
                    "Operation is not permitted for user: "
                            + context.getUsername(),
                    context.getUsername());
        }

        List<VirtualCorpusDto> dtos = service.retrieveUserVC(context.getUsername());
        String result;
        try {
            result = JsonUtils.toJSON(dtos);
        }
        catch (KustvaktException e) {
            throw responseHandler.throwit(e);
        }
        return Response.ok(result).build();
    }

}
