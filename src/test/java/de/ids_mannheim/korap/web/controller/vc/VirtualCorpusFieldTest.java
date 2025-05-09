package de.ids_mannheim.korap.web.controller.vc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.net.HttpHeaders;

import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.dao.QueryDao;
import de.ids_mannheim.korap.entity.QueryDO;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.init.NamedVCLoader;
import de.ids_mannheim.korap.util.QueryException;
import de.ids_mannheim.korap.utils.JsonUtils;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

public class VirtualCorpusFieldTest extends VirtualCorpusTestBase {

    @Autowired
    private NamedVCLoader vcLoader;

    @Autowired
    private QueryDao dao;

    private JsonNode testRetrieveField (String username, String vcName,
            String field) throws ProcessingException, KustvaktException {
        Response response = target().path(API_VERSION).path("vc").path("field")
                .path("~" + username).path(vcName)
                .queryParam("fieldName", field).request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue("admin", "pass"))
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                .get();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        return node;
    }

    private void testRetrieveProhibitedField (String username, String vcName,
            String field) throws ProcessingException, KustvaktException {
        Response response = target().path(API_VERSION).path("vc").path("field")
                .path("~" + username).path(vcName)
                .queryParam("fieldName", field).request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue("admin", "pass"))
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                .get();
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.NOT_ALLOWED, node.at("/errors/0/0").asInt());
    }

    private void deleteVcFromDB (String vcName) throws KustvaktException {
        QueryDO vc = dao.retrieveQueryByName(vcName, "system");
        dao.deleteQuery(vc);
        vc = dao.retrieveQueryByName(vcName, "system");
        assertEquals(null, vc);
    }

    @Test
    public void testRetrieveFieldsNamedVC1 ()
            throws IOException, QueryException, KustvaktException {
        vcLoader.loadVCToCache("named-vc1", "/vc/named-vc1.jsonld");
        JsonNode n = testRetrieveField("system", "named-vc1", "textSigle");
        assertEquals(n.at("/@context").asText(),
                "http://korap.ids-mannheim.de/ns/KoralQuery/v0.3/context.jsonld");
        assertEquals(n.at("/corpus/key").asText(), "textSigle");
        assertEquals(2, n.at("/corpus/value").size());
        n = testRetrieveField("system", "named-vc1", "author");
        assertEquals(2, n.at("/corpus/value").size());
        assertEquals(n.at("/corpus/value/0").asText(),
                "Goethe, Johann Wolfgang von");
        testRetrieveUnknownTokens();
        testRetrieveProhibitedField("system", "named-vc1", "tokens");
        testRetrieveProhibitedField("system", "named-vc1", "base");
        testDeleteVC("named-vc1", "system", "admin");
    }

    private void testRetrieveUnknownTokens ()
            throws ProcessingException, KustvaktException {
        JsonNode n = testRetrieveField("system", "named-vc1", "unknown");
        assertEquals(n.at("/corpus/key").asText(), "unknown");
        assertEquals(0, n.at("/corpus/value").size());
    }

    @Test
    public void testRetrieveTextSigleNamedVC2 ()
            throws IOException, QueryException, KustvaktException {
        vcLoader.loadVCToCache("named-vc2", "/vc/named-vc2.jsonld");
        JsonNode n = testRetrieveField("system", "named-vc2", "textSigle");
        assertEquals(2, n.at("/corpus/value").size());
        testDeleteVC("named-vc2", "system", admin);
    }

    @Test
    public void testRetrieveTextSigleNamedVC3 ()
            throws IOException, QueryException, KustvaktException {
        vcLoader.loadVCToCache("named-vc3", "/vc/named-vc3.jsonld");
        JsonNode n = testRetrieveField("system", "named-vc3", "textSigle");
        n = n.at("/corpus/value");
        assertEquals(1, n.size());
        assertEquals(n.get(0).asText(), "GOE/AGI/00000");
        testDeleteVC("named-vc3", "system", admin);
    }

    @Test
    public void testRetrieveFieldUnauthorized ()
            throws KustvaktException, IOException, QueryException {
        vcLoader.loadVCToCache("named-vc3", "/vc/named-vc3.jsonld");
        Response response = target().path(API_VERSION).path("vc").path("field")
                .path("~system").path("named-vc3")
                .queryParam("fieldName", "textSigle").request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue("dory", "pass"))
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                .get();
        assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.AUTHORIZATION_FAILED,
                node.at("/errors/0/0").asInt());
        assertEquals(node.at("/errors/0/1").asText(),
                "Unauthorized operation for user: dory");
        testDeleteVC("named-vc3", "system", admin);
    }
}
