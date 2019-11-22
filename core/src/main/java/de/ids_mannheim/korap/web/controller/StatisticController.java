package de.ids_mannheim.korap.web.controller;

import java.util.Locale;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.jersey.spi.container.ResourceFilters;

import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.response.Notifications;
import de.ids_mannheim.korap.utils.JsonUtils;
import de.ids_mannheim.korap.utils.KoralCollectionQueryBuilder;
import de.ids_mannheim.korap.web.CoreResponseHandler;
import de.ids_mannheim.korap.web.SearchKrill;
import de.ids_mannheim.korap.web.filter.APIVersionFilter;
import de.ids_mannheim.korap.web.filter.PiwikFilter;

/**
 * Web services related to statistics
 * 
 * @author hanl
 * @author margaretha
 *
 * @date 08/11/2017
 * 
 */
@Controller
@Path("{version}/statistics/")
@ResourceFilters({ APIVersionFilter.class, PiwikFilter.class })
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class StatisticController {

    private static final boolean DEBUG = false;
    private static Logger jlog =
            LogManager.getLogger(StatisticController.class);
    @Autowired
    private CoreResponseHandler kustvaktResponseHandler;
    @Autowired
    private SearchKrill searchKrill;

    /**
     * Returns statistics of the virtual corpus defined by the given
     * corpusQuery parameter.
     * 
     * @param context
     *            SecurityContext
     * @param locale
     *            Locale
     * @param cq
     *            a collection query specifying a virtual corpus
     * @param corpusQuery
     *            (DEPRECATED) a collection query specifying a virtual corpus 
     * @return statistics of the virtual corpus defined by the given
     *         corpusQuery parameter.
     */
    @GET
    public Response getStatistics (@Context SecurityContext context,
            @Context Locale locale,
            @QueryParam("cq") String cq,
            @QueryParam("corpusQuery") String corpusQuery) {

        KoralCollectionQueryBuilder builder = new KoralCollectionQueryBuilder();

        String stats;
        String json = null;
        boolean isDeprecated = false;
        try {
            if (cq != null && !cq.isEmpty()) {
                builder.with(cq);
                json = builder.toJSON();
            }
            else if (corpusQuery != null && !corpusQuery.isEmpty()) {
                builder.with(corpusQuery);
                json = builder.toJSON();
                isDeprecated = true;
            }
            
            stats = searchKrill.getStatistics(json);
            
            if (isDeprecated){
                Notifications n = new Notifications();
                n.addWarning(StatusCodes.DEPRECATED_PARAMETER,
                        "Parameter corpusQuery is deprecated in favor of cq.");
                ObjectNode warning = (ObjectNode) n.toJsonNode();
                ObjectNode node = (ObjectNode) JsonUtils.readTree(stats);
                node.setAll(warning);
                stats = node.toString();
            }
        }
        catch (KustvaktException e) {
            throw kustvaktResponseHandler.throwit(e);
        }
        
        if (stats.contains("-1")) {
            throw kustvaktResponseHandler.throwit(StatusCodes.NO_RESULT_FOUND);
        }
        if (DEBUG) {
            jlog.debug("Stats: " + stats);
        }
        return Response.ok(stats).build();
    }
    
    @POST
    @Consumes(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response getStatisticsFromKoralQuery (
            @Context SecurityContext context, @Context Locale locale,
            String koralQuery) {
        String stats;
        try {
            if (koralQuery != null && !koralQuery.isEmpty()) {
                stats = searchKrill.getStatistics(koralQuery);
            }
            else {
                stats = searchKrill.getStatistics(null);
            }

            if (stats.contains("-1")) {
                throw kustvaktResponseHandler
                        .throwit(StatusCodes.NO_RESULT_FOUND);
            }
            return Response.ok(stats).build();
        }
        catch (KustvaktException e) {
            throw kustvaktResponseHandler.throwit(e);
        }
    }
}
