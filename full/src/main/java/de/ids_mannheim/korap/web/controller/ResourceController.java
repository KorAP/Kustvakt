package de.ids_mannheim.korap.web.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import de.ids_mannheim.korap.dto.ResourceDto;
import de.ids_mannheim.korap.service.ResourceService;
import de.ids_mannheim.korap.web.filter.APIVersionFilter;
import de.ids_mannheim.korap.web.filter.PiwikFilter;
import de.ids_mannheim.korap.web.utils.ResourceFilters;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * Provides information about free resources.
 * 
 * @author margaretha
 *
 */
@Controller
@Path("{version}/resource")
@ResourceFilters({APIVersionFilter.class, PiwikFilter.class })
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
    public List<ResourceDto> getAllResourceInfo () {
        return resourceService.getResourceDtos();
    }
}
