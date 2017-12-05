package de.ids_mannheim.korap.web.controller;

import com.sun.jersey.spi.container.ResourceFilters;
import de.ids_mannheim.korap.config.BeansFactory;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.handlers.DocumentDao;
import de.ids_mannheim.korap.resources.Document;
import de.ids_mannheim.korap.server.KustvaktServer;
import de.ids_mannheim.korap.utils.JsonUtils;
import de.ids_mannheim.korap.utils.KustvaktLogger;
import de.ids_mannheim.korap.web.CoreResponseHandler;
import de.ids_mannheim.korap.web.filter.AdminFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

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
    CoreResponseHandler kustvaktResponseHandler;
    
    private static Logger jlog =
            LoggerFactory.getLogger(DocumentController.class);
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
