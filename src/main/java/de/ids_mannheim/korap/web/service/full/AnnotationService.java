package de.ids_mannheim.korap.web.service.full;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.spi.container.ResourceFilters;

import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.web.KustvaktServer;
import de.ids_mannheim.korap.web.filter.AuthFilter;
import de.ids_mannheim.korap.web.filter.DemoUserFilter;
import de.ids_mannheim.korap.web.filter.PiwikFilter;
import de.ids_mannheim.korap.web.utils.KustvaktResponseHandler;

/**
 * Provides services regarding annotation related information.
 * 
 * @author margaretha
 *
 */
@Path(KustvaktServer.API_VERSION + "/annotation/")
@ResourceFilters({ AuthFilter.class, DemoUserFilter.class, PiwikFilter.class })
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class AnnotationService {

    private static Logger jlog = LoggerFactory
            .getLogger(AnnotationService.class);


    @GET
    @Path("layers")
    public Response getLayers () {
        // TODO Auto-generated method stub
        return Response.status(200).build();
    }


    @POST
    @Path("description")
    public Response getAnnotations (@QueryParam("symbol") List<String> symbols,
            String language) {
        if (language == null || language.isEmpty()) {
            language = "en";
        }
        if (symbols == null){
            throw KustvaktResponseHandler.throwit(StatusCodes.MISSING_ARGUMENT);
        }
        if (symbols.isEmpty() || symbols.contains("*")){
            // select all 
        }
        else {
            String[] annotationPair;
            String foundry, layer;
            
            for (String s : symbols){
                annotationPair = s.split("/");
                if (annotationPair.length != 2){
                    throw KustvaktResponseHandler.throwit(StatusCodes.PARAMETER_VALIDATION_ERROR);
                }
                foundry = annotationPair[0];
                layer = annotationPair[1];
                // select
            }
        }
        
        return Response.status(200).build();
    }

}

