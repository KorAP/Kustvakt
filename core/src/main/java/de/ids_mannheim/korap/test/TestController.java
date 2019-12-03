package de.ids_mannheim.korap.test;

import java.io.InputStream;

import javax.ws.rs.Produces;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.springframework.stereotype.Controller;

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

    @POST
    @Path("glemm")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response dummyGlemm (String jsonld) {
        InputStream is = getClass().getClassLoader()
                .getResourceAsStream("test-pipes.jsonld");
        return Response.ok(is).build();
    }
}
