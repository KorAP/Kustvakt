package de.ids_mannheim.korap.web.controller.vc;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.http.entity.ContentType;
import org.glassfish.jersey.server.ContainerRequest;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.net.HttpHeaders;

import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.utils.JsonUtils;
import de.ids_mannheim.korap.web.controller.usergroup.UserGroupTestBase;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

public abstract class VirtualCorpusTestBase extends UserGroupTestBase {

    protected JsonNode retrieveVCInfo (String username, String vcCreator,
            String vcName) throws ProcessingException, KustvaktException {
        Response response = target().path(API_VERSION).path("vc")
                .path("~" + vcCreator).path(vcName).request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "pass"))
                .get();
        String entity = response.readEntity(String.class);
        // System.out.println(entity);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        return JsonUtils.readTree(entity);
    }

    protected void createVC (String authHeader, String username, String vcName,
            String vcJson) throws KustvaktException {
        Response response = target().path(API_VERSION).path("vc")
                .path("~" + username).path(vcName).request()
                .header(Attributes.AUTHORIZATION, authHeader)
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                .put(Entity.json(vcJson));

        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
    }

    protected void editVC (String username, String vcCreator, String vcName,
            String vcJson) throws KustvaktException {
        Response response = target().path(API_VERSION).path("vc")
                .path("~" + vcCreator).path(vcName).request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "pass"))
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                .put(Entity.json(vcJson));

        assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
    }

    protected JsonNode listVC (String username)
            throws ProcessingException, KustvaktException {
        Response response = target().path(API_VERSION).path("vc").request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "pass"))
                .get();

        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        // System.out.println(entity);
        return JsonUtils.readTree(entity);
    }

    protected JsonNode listVCWithAuthHeader (String authHeader)
            throws ProcessingException, KustvaktException {
        Response response = target().path(API_VERSION).path("vc").request()
                .header(Attributes.AUTHORIZATION, authHeader).get();

        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        return JsonUtils.readTree(entity);
    }

    protected JsonNode testListOwnerVC (String username)
            throws ProcessingException, KustvaktException {
        Response response = target().path(API_VERSION).path("vc")
                .queryParam("filter-by", "own").request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "pass"))
                .get();

        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        String entity = response.readEntity(String.class);
        return JsonUtils.readTree(entity);
    }

    protected JsonNode listSystemVC (String username) throws KustvaktException {
        Response response = target().path(API_VERSION).path("vc")
                .queryParam("filter-by", "system").request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue("pearl", "pass"))
                .get();
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        return node;
    }

    protected Response testShareVCByCreator (String vcCreator, String vcName,
            String groupName) throws ProcessingException, KustvaktException {

        return target().path(API_VERSION).path("vc").path("~" + vcCreator)
                .path(vcName).path("share").path("@" + groupName).request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(vcCreator, "pass"))
                .post(Entity.form(new Form()));
    }
    
    protected Response shareVC (String vcCreator, String vcName,
            String groupName, String username) throws ProcessingException, KustvaktException {

        return target().path(API_VERSION).path("vc").path("~" + vcCreator)
                .path(vcName).path("share").path("@" + groupName).request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "pass"))
                .post(Entity.form(new Form()));
    }

    protected JsonNode listAccessByGroup (String username, String groupName)
            throws KustvaktException {
        Response response = target().path(API_VERSION).path("vc").path("access")
                .queryParam("groupName", groupName).request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "pass"))
                .get();
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        return node;
    }

    protected void deleteVC (String vcName, String vcCreator, String username)
            throws KustvaktException {
        Response response = target().path(API_VERSION).path("vc")
                .path("~" + vcCreator).path(vcName).request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "pass"))
                .delete();

        assertEquals(Status.OK.getStatusCode(), response.getStatus());
    }

    protected void testResponseUnauthorized (Response response, String username)
            throws KustvaktException {
        assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());

        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);

        assertEquals(StatusCodes.AUTHORIZATION_FAILED,
                node.at("/errors/0/0").asInt());
        assertEquals("Unauthorized operation for user: " + username,
                node.at("/errors/0/1").asText());

        checkWWWAuthenticateHeader(response);
    }

    protected void checkWWWAuthenticateHeader (Response response) {
        Set<Entry<String, List<Object>>> headers = response.getHeaders()
                .entrySet();

        for (Entry<String, List<Object>> header : headers) {
            if (header.getKey().equals(ContainerRequest.WWW_AUTHENTICATE)) {
                assertThat(header.getValue(),
                        not(hasItem("Api realm=\"Kustvakt\"")));
                assertThat(header.getValue(),
                        hasItem("Bearer realm=\"Kustvakt\""));
                assertThat(header.getValue(),
                        hasItem("Basic realm=\"Kustvakt\""));
            }
        }
    }
    
    protected void createAccess (String vcCreator, String vcName,
            String groupName, String username)
            throws ProcessingException, KustvaktException {
        Response response = target().path(API_VERSION).path("vc")
                .path("~" + vcCreator).path(vcName).path("share")
                .path("@" + groupName).request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "pass"))
                .post(Entity.form(new Form()));
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
    }
    
    protected Response deleteAccess (String username, String accessId)
            throws ProcessingException, KustvaktException {
        Response response = target().path(API_VERSION).path("vc").path("access")
                .path(accessId).request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "pass"))
                .delete();
        return response;
    }
}
