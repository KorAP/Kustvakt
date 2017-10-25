package de.ids_mannheim.korap.web.controller;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.sun.jersey.spi.container.ResourceFilters;

import de.ids_mannheim.korap.service.ResourceService;
import de.ids_mannheim.korap.utils.JsonUtils;
import de.ids_mannheim.korap.web.filter.PiwikFilter;

/**
 * Provides information about free resources.
 * 
 * @author margaretha
 *
 */
@Controller
@Path("resource/")
@ResourceFilters({ PiwikFilter.class })
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class ResourceController {

    @Autowired
    private ResourceService resourceService;


    /** Returns descriptions of all free resources stored in 
     * the database.
     * 
     * @return a json description of all free resources stored in 
     * the database. 
     */
    @GET
    @Path("info")
    public Response getAllResourceInfo () {
        String result = JsonUtils.toJSON(resourceService.getResourceDtos());
        return Response.ok(result).build();
    }
}
