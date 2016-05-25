package de.ids_mannheim.korap.web.service.full;

import de.ids_mannheim.korap.utils.JsonUtils;
import de.ids_mannheim.korap.utils.ServiceInfo;
import de.ids_mannheim.korap.web.KustvaktServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class KustvaktService {

    private static Logger jlog = LoggerFactory.getLogger(UserService.class);


    @Path("info")
    public Response getInfo () {
        Map m = new HashMap();
        m.put("version", ServiceInfo.getInfo().getVersion());
        m.put("recent_api_version", KustvaktServer.API_VERSION);
        m.put("service_name", ServiceInfo.getInfo().getName());
        return Response.ok(JsonUtils.toJSON(m)).build();
    }

}
