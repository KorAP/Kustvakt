package de.ids_mannheim.korap.web.controller;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.utils.JsonUtils;
import de.ids_mannheim.korap.utils.ServiceInfo;
import de.ids_mannheim.korap.web.CoreResponseHandler;

/**
 * Created by hanl on 29.04.16.
 */
@Controller
@Path("kustvakt")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class KustvaktController {

    @Autowired
    private CoreResponseHandler kustvaktResponseHandler;

    @Autowired
    private KustvaktConfiguration config;

    @GET
    @Path("info")
    public Response getInfo () {
        
        Map m = new HashMap();
        m.put("version", ServiceInfo.getInfo().getVersion());
        m.put("supported_api_version(s)", config.getVersion());
        m.put("service_name", ServiceInfo.getInfo().getName());
        try {
            return Response.ok(JsonUtils.toJSON(m)).build();
        }
        catch (KustvaktException e) {
            throw kustvaktResponseHandler.throwit(e);
        }
    }

}
