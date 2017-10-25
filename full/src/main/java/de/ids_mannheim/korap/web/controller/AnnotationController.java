package de.ids_mannheim.korap.web.controller;

import java.io.IOException;
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

import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.filter.AuthFilter;
import de.ids_mannheim.korap.service.AnnotationService;
import de.ids_mannheim.korap.utils.JsonUtils;
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
public class AnnotationController {

    private static Logger jlog =
            LoggerFactory.getLogger(AnnotationController.class);

    @Autowired
    private AnnotationService annotationService;

    /**
     * Returns information about all supported layers
     * 
     * @return a json serialization of all supported layers
     */
    @GET
    @Path("layers")
    public Response getLayers () {
        String result = JsonUtils.toJSON(annotationService.getLayerDtos());
        return Response.ok(result).build();
    }


    /**
     * Returns a list of foundry descriptions.
     * 
     * @param codes
     *            foundry-layer code or a Kleene-star
     * @param language
     *            2-letter language code (description language)
     * @return a list of foundry, layer, value information in json
     */
    @SuppressWarnings("unchecked")
    @POST
    @Path("description")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getFoundryDescriptions (String json) {
        JsonNode node = JsonUtils.readTree(json);
        if (node == null) {
            throw KustvaktResponseHandler
                    .throwit(new KustvaktException(StatusCodes.MISSING_ARGUMENT,
                            "Missing a json string.", ""));
        }

        String language;
        if (!node.has("language")) {
            language = "en";
        }
        else {
            language = node.get("language").asText();
            if (language == null || language.isEmpty()) {
                language = "en";
            }
            else if (!(language.equals("en") || language.equals("de"))) {
                throw KustvaktResponseHandler.throwit(
                        new KustvaktException(StatusCodes.UNSUPPORTED_VALUE,
                                "Unsupported value:", language));
            }
        }

        List<String> codes;
        try {
            codes = JsonUtils.convert(node.get("codes"), List.class);
        }
        catch (IOException | NullPointerException e) {
            throw KustvaktResponseHandler.throwit(new KustvaktException(
                    StatusCodes.INVALID_ARGUMENT, "Bad argument:", json));
        }
        if (codes == null) {
            throw KustvaktResponseHandler.throwit(
                    new KustvaktException(StatusCodes.MISSING_ATTRIBUTE,
                            "Missing attribute:", "codes"));
        }
        else if (codes.isEmpty()) {
            throw KustvaktResponseHandler
                    .throwit(new KustvaktException(StatusCodes.NO_RESULT_FOUND,
                            "No result found.", "codes:[]"));
        }

        String result;
        try {
            result = JsonUtils
                    .toJSON(annotationService.getFoundryDtos(codes, language));
        }
        catch (KustvaktException e) {
            throw KustvaktResponseHandler.throwit(e);
        }
        return Response.ok(result).build();
    }

}

