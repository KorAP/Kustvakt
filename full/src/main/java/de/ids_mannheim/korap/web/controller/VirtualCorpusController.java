package de.ids_mannheim.korap.web.controller;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
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

import de.ids_mannheim.korap.constant.VirtualCorpusAccessStatus;
import de.ids_mannheim.korap.dto.VirtualCorpusAccessDto;
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

/** VirtualCorpusController defines web APIs related to virtual corpus (VC)
 * such as creating, deleting and listing user virtual corpora.
 * 
 * This class also includes APIs related to virtual corpus access (VCA) 
 * such as sharing and publishing VC. When a VC is published, it is shared 
 * with all users, but not always listed like system VC. It is listed for 
 * a user, once when he/she have searched for the VC. A VC can be published 
 * by creating or editing the VC. 
 * 
 * All the APIs in this class are available to logged-in users.
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

    /** Creates a user VC, also for system admins
     * 
     * @param securityContext
     * @param vc a JSON object describing the virtual corpus
     * @return HTTP Response OK if successful
     */
    @POST
    @Path("create")
    @Consumes("application/json")
    public Response createVC (@Context SecurityContext securityContext,
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

    /** Only the VC owner and system admins can edit VC.
     * 
     * @param securityContext
     * @param vc a JSON object describing the virtual corpus
     * @return HTTP Response OK if successful
     * @throws KustvaktException
     */
    @POST
    @Path("edit")
    @Consumes("application/json")
    public Response editVC (@Context SecurityContext securityContext,
            VirtualCorpusJson vc) throws KustvaktException {
        TokenContext context =
                (TokenContext) securityContext.getUserPrincipal();

        try {
            service.editVC(vc, context.getUsername());
        }
        catch (KustvaktException e) {
            throw responseHandler.throwit(e);
        }
        return Response.ok().build();
    }

    /** Searches for a specific VC.
     * 
     * @param securityContext
     * @param vcId a virtual corpus id
     * @return a list of VC
     */
    @GET
    @Path("search/{vcId}")
    public Response searchVC (@Context SecurityContext securityContext,
            @PathParam("vcId") int vcId) {
        String result;
        TokenContext context =
                (TokenContext) securityContext.getUserPrincipal();
        try {
            VirtualCorpusDto dto =
                    service.searchVCById(context.getUsername(), vcId);
            result = JsonUtils.toJSON(dto);
        }
        catch (KustvaktException e) {
            throw responseHandler.throwit(e);
        }
        return Response.ok(result).build();
    }

    /** Lists not only private VC but all VC available to a user.
     * 
     * @param securityContext
     * @return a list of VC
     */
    @GET
    @Path("list")
    public Response listVCByUser (@Context SecurityContext securityContext) {
        String result;
        TokenContext context =
                (TokenContext) securityContext.getUserPrincipal();
        try {
            List<VirtualCorpusDto> dtos =
                    service.listVCByUser(context.getUsername());
            result = JsonUtils.toJSON(dtos);
        }
        catch (KustvaktException e) {
            throw responseHandler.throwit(e);
        }
        return Response.ok(result).build();
    }

    /** Lists all VC created by a user
     * 
     * @param securityContext
     * @return a list of VC created by the user in the security context.
     */
    @GET
    @Path("list/user")
    public Response listUserVC (@Context SecurityContext securityContext) {
        String result;
        TokenContext context =
                (TokenContext) securityContext.getUserPrincipal();
        try {
            List<VirtualCorpusDto> dtos =
                    service.listOwnerVC(context.getUsername());
            result = JsonUtils.toJSON(dtos);
        }
        catch (KustvaktException e) {
            throw responseHandler.throwit(e);
        }
        return Response.ok(result).build();
    }

    /** Only the VC owner and system admins can delete VC. VCA admins 
     *  can delete VC-accesses e.g. of project VC, but not the VC 
     *  themselves. 
     * 
     * @param securityContext
     * @param vcId the id of the virtual corpus
     * @return HTTP status 200, if successful
     */
    @DELETE
    @Path("delete/{vcId}")
    public Response deleteVC (@Context SecurityContext securityContext,
            @PathParam("vcId") int vcId) {
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

    //  @POST
    //  @Path("conceal")
    //  public Response concealPublishedVC (@Context SecurityContext securityContext,
    //          @QueryParam("vcId") int vcId) {
    //      TokenContext context =
    //              (TokenContext) securityContext.getUserPrincipal();
    //      try {
    //          service.concealVC(context.getUsername(), vcId);
    //      }
    //      catch (KustvaktException e) {
    //          throw responseHandler.throwit(e);
    //      }
    //      return Response.ok().build();
    //  }

    /** VC can only be shared with a group, not individuals. 
     *  Only VCA admins are allowed to share VC and 
     *  the VC must have been created by themselves.
     * 
     * @param securityContext
     * @param vcId a virtual corpus id
     * @param groupId a user group id
     * @return HTTP status 200, if successful
     */
    @POST
    @Path("access/share")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response shareVC (@Context SecurityContext securityContext,
            @FormParam("vcId") int vcId, @FormParam("groupId") int groupId) {
        TokenContext context =
                (TokenContext) securityContext.getUserPrincipal();
        try {
            service.shareVC(context.getUsername(), vcId, groupId);
        }
        catch (KustvaktException e) {
            throw responseHandler.throwit(e);
        }
        return Response.ok().build();
    }

    /** Only VCA Admins and system admins are allowed to delete a VC-access.
     * 
     * @param securityContext
     * @param accessId
     * @return
     */
    @DELETE
    @Path("access/delete/{accessId}")
    public Response deleteVCAccess (@Context SecurityContext securityContext,
            @PathParam("accessId") int accessId) {
        TokenContext context =
                (TokenContext) securityContext.getUserPrincipal();
        try {
            service.deleteVCAccess(context.getUsername(), accessId);
        }
        catch (KustvaktException e) {
            throw responseHandler.throwit(e);
        }
        return Response.ok().build();
    }


    /** Lists only active accesses to the specified virtual corpus.
     * Only available to VCA and system admins.
     * 
     * @see VirtualCorpusAccessStatus
     * 
     * @param securityContext
     * @param vcId virtual corpus id
     * @return a list of access to the specified virtual corpus
     */
    @GET
    @Path("access/list")
    public Response listVCAccess (@Context SecurityContext securityContext,
            @QueryParam("vcId") int vcId) {
        TokenContext context =
                (TokenContext) securityContext.getUserPrincipal();
        String result;
        try {
            List<VirtualCorpusAccessDto> dtos =
                    service.listVCAccessByVC(context.getUsername(), vcId);
            result = JsonUtils.toJSON(dtos);
        }
        catch (KustvaktException e) {
            throw responseHandler.throwit(e);
        }
        return Response.ok(result).build();
    }

    /** Lists all VC-accesses available for a group. 
     *  Only available to VCA and system admins.
     * 
     * @param securityContext
     * @param groupId a group id
     * @return a list of VC-access
     */
    @GET
    @Path("access/list/byGroup")
    public Response listVCAccessByGroup (
            @Context SecurityContext securityContext,
            @QueryParam("groupId") int groupId) {
        TokenContext context =
                (TokenContext) securityContext.getUserPrincipal();
        String result;
        try {
            List<VirtualCorpusAccessDto> dtos =
                    service.listVCAccessByGroup(context.getUsername(), groupId);
            result = JsonUtils.toJSON(dtos);
        }
        catch (KustvaktException e) {
            throw responseHandler.throwit(e);
        }
        return Response.ok(result).build();
    }
}
