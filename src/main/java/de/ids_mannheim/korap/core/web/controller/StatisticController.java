package de.ids_mannheim.korap.core.web.controller;

import java.util.List;
import java.util.Locale;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import de.ids_mannheim.korap.core.service.StatisticService;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.security.context.TokenContext;
import de.ids_mannheim.korap.web.CoreResponseHandler;
import de.ids_mannheim.korap.web.filter.APIVersionFilter;
import de.ids_mannheim.korap.web.filter.AuthenticationFilter;
import de.ids_mannheim.korap.web.filter.DemoUserFilter;
import de.ids_mannheim.korap.web.utils.ResourceFilters;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

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
@ResourceFilters({ APIVersionFilter.class, AuthenticationFilter.class,
	DemoUserFilter.class })
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class StatisticController {

    private static final boolean DEBUG = false;
    private static Logger jlog = LogManager
            .getLogger(StatisticController.class);
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
	public Response getStatistics (@Context SecurityContext securityContext,
			@Context Locale locale, @Context HttpHeaders headers,
			@QueryParam("cq") List<String> cq) {

		TokenContext context = (TokenContext) securityContext
				.getUserPrincipal();

        String stats;
        try {
			stats = service.retrieveStatisticsForCorpusQuery(cq,
					context.getUsername(), headers);
            if (DEBUG) {
                jlog.debug("Stats: " + stats);
            }

            return Response.ok(stats)
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
            return Response.ok(stats)
                    .header("X-Index-Revision", service.getIndexFingerprint())
                    .build();
        }
        catch (KustvaktException e) {
            throw kustvaktResponseHandler.throwit(e);
        }
    }
}
