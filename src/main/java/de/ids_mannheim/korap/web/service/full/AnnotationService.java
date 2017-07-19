package de.ids_mannheim.korap.web.service.full;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.jersey.spi.container.ResourceFilters;

import de.ids_mannheim.korap.dao.AnnotationDao;
import de.ids_mannheim.korap.dto.LayerDto;
import de.ids_mannheim.korap.dto.converter.AnnotationConverter;
import de.ids_mannheim.korap.entity.AnnotationPair;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.utils.JsonUtils;
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

    private static Logger jlog =
            LoggerFactory.getLogger(AnnotationService.class);

    @Autowired
    private AnnotationDao annotationDao;

    @Autowired
    private AnnotationConverter annotationConverter;


    /**
     * Returns information about all supported layers
     * 
     * @return a json serialization of all supported layers
     */
    @GET
    @Path("layers")
    public Response getLayers () {
        List<AnnotationPair> layers = annotationDao.getAllFoundryLayerPairs();
        List<LayerDto> layerDto = annotationConverter.convertToLayerDto(layers);
        String result = JsonUtils.toJSON(layerDto);
        jlog.debug("/layers " + layers.toString());
        return Response.ok(result).build();
    }


    /**
     * Returns descriptions and values of the given foundry-layer
     * pairs.
     * 
     * @param codes
     *            foundry-layer code or a Kleene-star
     * @param language
     *            2-letter language code (description language)
     * @return a list of foundry, layer, value information in json
     */
    @POST
    @Path("description")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getAnnotations (String json) {
        JsonNode node = JsonUtils.readTree(json);
        if (node == null) { throw KustvaktResponseHandler.throwit(
                StatusCodes.MISSING_ARGUMENT, "Missing a json string."); }

        String language;
        if (!node.has("language")) {
            language = "en";
        }
        else {
            language = node.get("language").asText();
            if (language == null || language.isEmpty()) {
                language = "en";
            }
        }

        List<String> codes;
        try {
            codes = JsonUtils.convert(node.get("codes"), List.class);
        }
        catch (IOException | NullPointerException e) {
            throw KustvaktResponseHandler.throwit(StatusCodes.PARAMETER_INVALID,
                    "Bad parameter:", json);
        }
        if (codes == null) { throw KustvaktResponseHandler
                .throwit(StatusCodes.MISSING_ARGUMENT); }

        List<AnnotationPair> annotationPairs = null;
        String foundry = "", layer = "";
        if (codes.contains("*")) {
            annotationPairs =
                    annotationDao.getAnnotationDescriptions(foundry, layer);
        }
        else {
            String[] annotationCode;
            annotationPairs = new ArrayList<AnnotationPair>();
            for (String code : codes) {
                jlog.debug("code " + code);
                annotationCode = code.split("/");
                if (annotationCode.length == 1) {
                    foundry = annotationCode[0];
                }
                else if (annotationCode.length == 2) {
                    foundry = annotationCode[0];
                    layer = annotationCode[1];
                }
                else {
                    jlog.error("Annotation code is wrong: " + annotationCode);
                    throw KustvaktResponseHandler.throwit(
                            StatusCodes.PARAMETER_INVALID, "Bad parameter:",
                            code);
                }

                annotationPairs.addAll(annotationDao
                        .getAnnotationDescriptions(foundry, layer));
            }
        }

        if (annotationPairs != null && !annotationPairs.isEmpty()) {
            String result = JsonUtils.toJSON(annotationPairs);
            jlog.debug("/layers " + annotationPairs.toString());
            return Response.ok(result).build();
        }
        else {
            throw KustvaktResponseHandler.throwit(StatusCodes.NO_VALUE_FOUND,
                    "No result found.");
        }
    }

}

