package de.ids_mannheim.korap.web.controller;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
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
import de.ids_mannheim.korap.service.VirtualCorpusService;
import de.ids_mannheim.korap.user.TokenContext;
import de.ids_mannheim.korap.utils.JsonUtils;
import de.ids_mannheim.korap.web.FullResponseHandler;
import de.ids_mannheim.korap.web.filter.AuthenticationFilter;
import de.ids_mannheim.korap.web.filter.BlockingFilter;
import de.ids_mannheim.korap.web.filter.PiwikFilter;
import de.ids_mannheim.korap.web.input.VirtualCorpusJson;

/** VirtualCorpusController defines web APIs related to virtual corpus
 * such as creating, deleting and listing user virtual corpora.
 * 
 * These APIs are only available to logged-in users.
 * 
 * @author margaretha
 *
 */
@Controller
@Path("vc")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
@ResourceFilters({ AuthenticationFilter.class, BlockingFilter.class,
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
    @Consumes("application/json")
    public Response storeVC (@Context SecurityContext securityContext,
            VirtualCorpusJson vc) {
        try {
            jlog.debug(vc.toString());

            // get user info
            TokenContext context =
                    (TokenContext) securityContext.getUserPrincipal();

            service.storeVC(vc, context.getUsername());
        }
        catch (KustvaktException e) {
            throw responseHandler.throwit(e);
        }
        return Response.ok().build();
    }

    // EM: nicer URL with username?
    @GET
    @Path("user")
    public Response getUserVC (@Context SecurityContext securityContext) {
        String result;
        TokenContext context =
                (TokenContext) securityContext.getUserPrincipal();
        try {
            List<VirtualCorpusDto> dtos =
                    service.retrieveUserVC(context.getUsername());
            result = JsonUtils.toJSON(dtos);
        }
        catch (KustvaktException e) {
            throw responseHandler.throwit(e);
        }
        return Response.ok(result).build();
    }

    //    @POST
    //    @Path("edit")
    //    public Response editVC (@Context SecurityContext securityContext,
    //            String json) throws KustvaktException {
    //        TokenContext context =
    //                (TokenContext) securityContext.getUserPrincipal();
    //        if (context.isDemo()) {
    //            throw new KustvaktException(StatusCodes.AUTHORIZATION_FAILED,
    //                    "Operation is not permitted for user: "
    //                            + context.getUsername(),
    //                    context.getUsername());
    //        }
    //
    //        return Response.ok().build();
    //    }

    /** Only VC owner and system admin can delete VCs. VC-access admins 
     *  can delete VC-accesses e.g. of project VCs, but not the VCs 
     *  themselves. 
     * 
     * @param securityContext
     * @param vcId
     * @return HTTP status 200, if successful
     */
    @DELETE
    @Path("delete")
    public Response deleteVC (@Context SecurityContext securityContext,
            @QueryParam("vcId") int vcId) {
        TokenContext context =
                (TokenContext) securityContext.getUserPrincipal();
        try {
            service.deleteVC(context.getUsername(), vcId);
        }
        catch (KustvaktException e) {
            throw responseHandler.throwit(e);
        }
        return Response.ok().build();
    }
}
