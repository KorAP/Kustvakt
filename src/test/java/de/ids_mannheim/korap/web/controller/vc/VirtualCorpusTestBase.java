package de.ids_mannheim.korap.web.controller.vc;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.http.entity.ContentType;
import org.glassfish.jersey.server.ContainerRequest;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.net.HttpHeaders;

import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.cache.VirtualCorpusCache;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.constant.ResourceType;
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

    protected void createNemoVC() throws KustvaktException {
    	String authHeader = HttpAuthorizationHandler
                .createBasicAuthorizationHeaderValue("nemo", "pass");
    	
    	String vcJson = "{\"type\": \"PRIVATE\""
                + ",\"queryType\": \"VIRTUAL_CORPUS\""
    			+ ",\"status\":\"experimental\""
                + ",\"corpusQuery\": \"corpusSigle=GOE\"}";
    	createVC(authHeader, "nemo", "nemo-vc", vcJson);
    };
    
	protected void createDoryVC () throws KustvaktException {
		String authHeader = HttpAuthorizationHandler
				.createBasicAuthorizationHeaderValue("dory", "pass");

		String vcJson = "{\"type\": \"PRIVATE\""
				+ ",\"queryType\": \"VIRTUAL_CORPUS\""
				+ ",\"status\":\"experimental\""
				+ ",\"corpusQuery\": \"corpusSigle=GOE AND creationDate since "
				+ "1820\"}";
		createVC(authHeader, "dory", "dory-vc", vcJson);
	};
    
	protected void createDoryGroupVC () throws KustvaktException {
		String authHeader = HttpAuthorizationHandler
				.createBasicAuthorizationHeaderValue("dory", "pass");

		String vcJson = "{\"type\": \"PROJECT\""
				+ ",\"queryType\": \"VIRTUAL_CORPUS\""
				+ ",\"status\":\"experimental\""
				+ ",\"corpusQuery\": \"corpusSigle=GOE AND creationDate since "
				+ "1810\"}";
		createVC(authHeader, "dory", "group-vc", vcJson);
	}
	
	protected void createMarlinVC() throws KustvaktException {
    	createVC("marlin", "marlin-vc", ResourceType.PRIVATE);
    };
    
    protected void createMarlinPublishedVC() throws KustvaktException {
    	createVC("marlin", "published-vc", ResourceType.PUBLISHED);
    };
    
    
    protected void createVC (String authHeader, String username, String vcName,
            String vcJson) throws KustvaktException {
        Response response = target().path(API_VERSION).path("vc")
                .path("~" + username).path(vcName).request()
                .header(Attributes.AUTHORIZATION, authHeader)
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                .put(Entity.json(vcJson));

        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
    }
    
    protected void createVC (String username, String vcName,
            ResourceType vcType) throws KustvaktException {
        String vcJson = "{\"type\": \""+vcType+"\""
                + ",\"queryType\": \"VIRTUAL_CORPUS\""
                + ",\"corpusQuery\": \"corpusSigle=GOE\"}";

        String authHeader = HttpAuthorizationHandler
                .createBasicAuthorizationHeaderValue(username, "pass");
        
        Response response = target().path(API_VERSION).path("vc")
                .path("~" + username).path(vcName).request()
                .header(Attributes.AUTHORIZATION, authHeader)
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                .put(Entity.json(vcJson));

        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
    }
    
    protected void createPrivateVC (String username, String vcName)
            throws KustvaktException {
        createVC(username, vcName, ResourceType.PRIVATE);
    }
    
    protected void createProjectVC (String username, String vcName)
            throws KustvaktException {
        createVC(username, vcName, ResourceType.PROJECT);
    }

    protected void createPublishedVC (String username, String vcName)
            throws KustvaktException {
        createVC(username, vcName, ResourceType.PUBLISHED);
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

    protected Response shareVCByCreator (String vcCreator, String vcName,
            String groupName) throws KustvaktException {

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

    protected JsonNode listRolesByGroup (String username, String groupName)
            throws KustvaktException {
        return listRolesByGroup(username, groupName, true);
    }
    
    protected JsonNode listRolesByGroup (String username, String groupName,
            boolean hasQuery)
            throws KustvaktException {
        Response response = target().path(API_VERSION).path("vc").path("access")
                .queryParam("groupName", groupName)
                .queryParam("hasQuery", hasQuery)
                .request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "pass"))
                .get();
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        return node;
    }

    protected Response deleteVC (String vcName, String vcCreator, String username)
            throws KustvaktException {
        Response response = target().path(API_VERSION).path("vc")
                .path("~" + vcCreator).path(vcName).request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "pass"))
                .delete();

//        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        return response;
    }
    
	protected void testDeleteVC (String vcName, String vcCreator,
			String deletedBy) throws KustvaktException {
		deleteVC(vcName, vcCreator, deletedBy);
		
		Response response = target().path(API_VERSION).path("search")
                .queryParam("q", "[orth=der]").queryParam("ql", "poliqarp")
                .queryParam("cq", "referTo "+vcName).request().get();
        
        String ent = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
		assertEquals(StatusCodes.NO_RESOURCE_FOUND,
				node.at("/errors/0/0").asInt());
		assertEquals("Virtual corpus "+vcCreator+"/"+vcName+" is not found.",
				node.at("/errors/0/1").asText());
        assertFalse(VirtualCorpusCache.contains(vcName));
		
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
    
    protected Response deleteRoleByGroupAndQuery (String vcCreator, String vcName,
            String groupName, String deleteBy)
            throws ProcessingException, KustvaktException {
        Response response = target().path(API_VERSION).path("vc")
                .path("~" + vcCreator).path(vcName).path("delete")
                .path("@" + groupName).request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(deleteBy, "pass"))
                .delete();
        return response;
    }
    
    protected Response searchWithVCRef (String username, String vcCreator,
            String vcName) throws KustvaktException {
        Response response = target().path(API_VERSION).path("search")
                .queryParam("q", "[orth=der]").queryParam("ql", "poliqarp")
                .queryParam("cq",
                        "referTo \"" + vcCreator + "/" + vcName + "\"")
                .request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "pass"))
                .get();
        return response;
    }
}
