package de.ids_mannheim.korap.web.controller;

import java.util.List;
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

import de.ids_mannheim.korap.web.utils.ResourceFilters;

import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.service.StatisticService;
import de.ids_mannheim.korap.web.CoreResponseHandler;
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
    private StatisticService service;

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
     *            (DEPRECATED) a collection query specifying a virtual
     *            corpus
     * @return statistics of the virtual corpus defined by the given
     *         corpusQuery parameter.
     */
    @GET
    public Response getStatistics (@Context SecurityContext context,
            @Context Locale locale, @QueryParam("cq") List<String> cq,
            @QueryParam("corpusQuery") List<String> corpusQuery) {

        String stats;
        boolean isDeprecated = false;
        try {
            if (cq.isEmpty() && corpusQuery != null && !corpusQuery.isEmpty()) {
                isDeprecated = true;
                cq = corpusQuery;
            }
            stats = service.retrieveStatisticsForCorpusQuery(cq, isDeprecated);
            if (DEBUG) {
                jlog.debug("Stats: " + stats);
            }

            return Response
                .ok(stats)
                .header("X-Index-Revision", service.getIndexFingerprint())
                .build();
        }
        catch (KustvaktException e) {
            throw kustvaktResponseHandler.throwit(e);
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response getStatisticsFromKoralQuery (
            @Context SecurityContext context, @Context Locale locale,
            String koralQuery) {
        try {
            String stats = service.retrieveStatisticsForKoralQuery(koralQuery);
            return Response
                .ok(stats)
                .header("X-Index-Revision", service.getIndexFingerprint())
                .build();
        }
        catch (KustvaktException e) {
            throw kustvaktResponseHandler.throwit(e);
        }
    }
}
