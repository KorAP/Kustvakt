package de.ids_mannheim.korap.web.controller;

import java.io.IOException;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.jersey.spi.container.ResourceFilters;

import de.ids_mannheim.korap.dto.FoundryDto;
import de.ids_mannheim.korap.dto.LayerDto;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.service.AnnotationService;
import de.ids_mannheim.korap.utils.JsonUtils;
import de.ids_mannheim.korap.web.KustvaktResponseHandler;
import de.ids_mannheim.korap.web.filter.APIVersionFilter;
import de.ids_mannheim.korap.web.filter.DemoUserFilter;
import de.ids_mannheim.korap.web.filter.PiwikFilter;

/**
 * Provides services regarding annotation related information.
 * 
 * @author margaretha
 *
 */
@Controller
@Path("/{version}/annotation/")
@ResourceFilters({APIVersionFilter.class, DemoUserFilter.class, PiwikFilter.class })
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class AnnotationController {

    @Autowired
    private KustvaktResponseHandler kustvaktResponseHandler;

    @Autowired
    private AnnotationService annotationService;

    /**
     * Returns information about all supported layers
     * 
     * @return a json serialization of all supported layers
     */
    @GET
    @Path("layers")
    public List<LayerDto> getLayers () {
        return annotationService.getLayerDtos();
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
    public List<FoundryDto> getFoundryDescriptions (String json) {
        if (json == null || json.isEmpty()) {
            throw kustvaktResponseHandler
                    .throwit(new KustvaktException(StatusCodes.MISSING_PARAMETER,
                            "Missing a json string.", ""));
        }

        JsonNode node;
        try {
            node = JsonUtils.readTree(json);
        }
        catch (KustvaktException e1) {
            throw kustvaktResponseHandler.throwit(e1);
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
                throw kustvaktResponseHandler.throwit(
                        new KustvaktException(StatusCodes.UNSUPPORTED_VALUE,
                                "Unsupported value:", language));
            }
        }

        List<String> codes;
        try {
            codes = JsonUtils.convert(node.get("codes"), List.class);
        }
        catch (IOException | NullPointerException e) {
            throw kustvaktResponseHandler.throwit(new KustvaktException(
                    StatusCodes.INVALID_ARGUMENT, "Bad argument:", json));
        }
        if (codes == null || codes.isEmpty()) {
            throw kustvaktResponseHandler.throwit(
                    new KustvaktException(StatusCodes.MISSING_ATTRIBUTE,
                            "codes is null or empty", "codes"));
        }

        try {
            return annotationService.getFoundryDtos(codes, language);
        }
        catch (KustvaktException e) {
            throw kustvaktResponseHandler.throwit(e);
        }
    }
}

