package de.ids_mannheim.korap.web.controller;

import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.server.KustvaktServer;
import de.ids_mannheim.korap.utils.JsonUtils;
import de.ids_mannheim.korap.utils.ServiceInfo;
import de.ids_mannheim.korap.web.utils.KustvaktResponseHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by hanl on 29.04.16.
 */
@Path("kustvakt")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class KustvaktController {

    private static Logger jlog = LoggerFactory.getLogger(UserController.class);
    @Autowired
    private KustvaktResponseHandler kustvaktResponseHandler;


    @Path("info")
    public Response getInfo () {
        Map m = new HashMap();
        m.put("version", ServiceInfo.getInfo().getVersion());
        m.put("recent_api_version", KustvaktServer.API_VERSION);
        m.put("service_name", ServiceInfo.getInfo().getName());
        try {
            return Response.ok(JsonUtils.toJSON(m)).build();
        }
        catch (KustvaktException e) {
            throw kustvaktResponseHandler.throwit(e);
        }
    }

}
