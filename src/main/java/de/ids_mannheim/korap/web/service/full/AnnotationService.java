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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.sun.jersey.spi.container.ResourceFilters;

import de.ids_mannheim.korap.dao.AnnotationDao;
import de.ids_mannheim.korap.entity.Annotation;
import de.ids_mannheim.korap.entity.AnnotationPair;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.utils.JsonUtils;
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
@Controller
@Path("annotation/")
@ResourceFilters({ AuthFilter.class, DemoUserFilter.class, PiwikFilter.class })
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class AnnotationService {

    private static Logger jlog = LoggerFactory
            .getLogger(AnnotationService.class);

    @Autowired
    private AnnotationDao annotationDao;
    
    /** Returns information about all supported layers
     * 
     * @return a json serialization of all supported layers 
     */
    @GET
    @Path("layers")
    public Response getLayers () {
        List<AnnotationPair> layers = annotationDao.getAllFoundryLayerPairs();
        String result = JsonUtils.toJSON(layers);
        jlog.debug("/layers "+layers.toString());
        return Response.ok(result).build();
    }


    @POST
    @Path("description")
    public Response getAnnotations (@QueryParam("symbol") List<String> symbols,
            String language) {
        List<AnnotationPair> annotationPairs = null;
        if (language == null || language.isEmpty()) {
            language = "en";
        }
        if (symbols == null){
            throw KustvaktResponseHandler.throwit(StatusCodes.MISSING_ARGUMENT);
        }
        if (symbols.isEmpty() || symbols.contains("*")){
            annotationPairs = annotationDao.getAllAnnotationDescriptions(); 
        }
        else {
            String[] annotationSymbols;
            String foundry, layer;
            
            for (String s : symbols){
                annotationSymbols = s.split("/");
                if (annotationSymbols.length != 2){
                    throw KustvaktResponseHandler.throwit(StatusCodes.PARAMETER_VALIDATION_ERROR);
                }
                foundry = annotationSymbols[0];
                layer = annotationSymbols[1];
                // select
            }
        }
        
        if (annotationPairs != null && !annotationPairs.isEmpty()){
            String result = JsonUtils.toJSON(annotationPairs);
            jlog.debug("/layers "+annotationPairs.toString());
            return Response.ok(result).build();
        }
        else{
            return Response.ok().build();
        }
    }

}

