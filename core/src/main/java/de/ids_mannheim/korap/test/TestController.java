package de.ids_mannheim.korap.test;

import java.io.IOException;
import java.io.InputStream;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.utils.JsonUtils;
import de.ids_mannheim.korap.web.KustvaktResponseHandler;

/**
 * Controllers used only for testing
 * 
 * @author margaretha
 *
 */
@Controller
@Path("/{version}/test")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class TestController {

    @Autowired
    private KustvaktResponseHandler kustvaktResponseHandler;

    public static ObjectMapper mapper = new ObjectMapper();

    @POST
    @Path("glemm")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response dummyGlemm (String jsonld,
            @QueryParam("param") String param) throws IOException {
        InputStream is;
        is = getClass().getClassLoader()
                .getResourceAsStream("test-pipes.jsonld");

        ObjectNode newJson = (ObjectNode) mapper.readTree(is);

        try {
            JsonNode node = JsonUtils.readTree(jsonld);
            if (node.has("warnings")) {
                node = node.get("warnings");
                newJson.set("warnings", node);
            }
            if (param != null && !param.isEmpty()) {
                ArrayNode keys = (ArrayNode) newJson.at("/query/wrap/key");
                keys.add("die");
                newJson.set("key", keys);
            }
        }
        catch (KustvaktException e) {
            throw kustvaktResponseHandler.throwit(e);
        }
        return Response.ok(newJson.toString()).build();
    }

    @POST
    @Path("invalid-json-pipe")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response dummyPipe1 (String jsonld) {
        String incorrectJson = "{blah:}";
        return Response.ok(incorrectJson).build();
    }

    @POST
    @Path("plain-response-pipe")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response dummyPipe2 (String jsonld) {
        String incorrectJson = "brumbrum";
        return Response.ok(incorrectJson).build();
    }

    @POST
    @Path("urlencoded-pipe")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response dummyPipe3 (String jsonld) {
        return Response.ok().build();
    }
}
