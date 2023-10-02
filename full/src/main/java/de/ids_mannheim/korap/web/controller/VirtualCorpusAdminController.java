package de.ids_mannheim.korap.web.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import de.ids_mannheim.korap.config.NamedVCLoader;
import de.ids_mannheim.korap.constant.QueryType;
import de.ids_mannheim.korap.constant.ResourceType;
import de.ids_mannheim.korap.dto.QueryDto;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.service.QueryService;
import de.ids_mannheim.korap.web.KustvaktResponseHandler;
import de.ids_mannheim.korap.web.filter.APIVersionFilter;
import de.ids_mannheim.korap.web.filter.AdminFilter;
import de.ids_mannheim.korap.web.utils.ResourceFilters;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

@Controller
@Path("{version}/admin/vc")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
@ResourceFilters({ APIVersionFilter.class, AdminFilter.class })
public class VirtualCorpusAdminController {

    @Autowired
    private KustvaktResponseHandler kustvaktResponseHandler;
    @Autowired
    private QueryService service;
    
    @Autowired
    private NamedVCLoader vcLoader;
    
    @POST
    @Path("load-cache")
    public Response loadAndCacheSystemVC () {
        Thread t = new Thread(vcLoader);
        t.start();
        
        return Response.status(Status.OK).build();
    }
    
    
    /**
     * Lists virtual corpora by creator and type. This is a controller
     * for system admin requiring valid system admin authentication.
     * 
     * If type is not specified, retrieves virtual corpora of all
     * types. If createdBy is not specified, retrieves virtual corpora
     * of all users.
     * 
     * @param securityContext
     * @param createdBy
     *            username of virtual corpus creator (optional)
     * @param type
     *            {@link ResourceType}
     * @return a list of virtual corpora
     */
    @POST
    @Path("list")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public List<QueryDto> listVCByType (
            @FormParam("createdBy") String createdBy,
            @FormParam("type") ResourceType type) {
        try {
            return service.listQueryByType(createdBy, type,
                    QueryType.VIRTUAL_CORPUS);
        }
        catch (KustvaktException e) {
            throw kustvaktResponseHandler.throwit(e);
        }
    }
}
