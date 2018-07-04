package de.ids_mannheim.korap.web.controller;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.sun.jersey.spi.container.ResourceFilters;

import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.handlers.DocumentDao;
import de.ids_mannheim.korap.resources.Document;
import de.ids_mannheim.korap.server.KustvaktServer;
import de.ids_mannheim.korap.utils.JsonUtils;
import de.ids_mannheim.korap.web.KustvaktResponseHandler;
import de.ids_mannheim.korap.web.filter.AdminFilter;

/**
 * EM: To Do: restructure codes regarding service and controller layers
 * 
 * @author hanl
 * @date 19/11/2014
 */
@Deprecated
@Controller
@Path(KustvaktServer.API_VERSION + "/doc")
@ResourceFilters({ AdminFilter.class })
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class DocumentController {

    @Autowired
    private KustvaktResponseHandler kustvaktResponseHandler;

    private static Logger jlog =
            LogManager.getLogger(DocumentController.class);
    private DocumentDao documentDao;


    @POST
    @Path("{doc}")
    public Response store (@PathParam("doc") String docid,
            @QueryParam("disabled") Boolean disabled) {
        Document doc = new Document(docid);
        doc.setDisabled(disabled);
        try {
            this.documentDao.storeResource(doc, null);
        }
        catch (KustvaktException e) {
            throw kustvaktResponseHandler.throwit(e);
        }
        return Response.ok().build();
    }


    //todo: pipe add document to index endpoint

    @GET
    @Path("{corpus}")
    public Response get (@PathParam("corpus") String corpus,
            @QueryParam("index") Integer index,
            @QueryParam("offset") Integer length) {
        if (index == null) index = 1;
        if (length == null) length = 25;
        try {
            List docs = this.documentDao.findbyCorpus(corpus, length, index);
            //todo: serialize to document json
            return Response.ok(JsonUtils.toJSON(docs)).build();
        }
        catch (KustvaktException e) {
            throw kustvaktResponseHandler.throwit(e);
        }
    }

}
