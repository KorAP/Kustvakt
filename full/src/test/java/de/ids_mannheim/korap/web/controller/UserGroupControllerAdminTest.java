package de.ids_mannheim.korap.web.controller;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.net.HttpHeaders;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.ClientResponse.Status;
import com.sun.jersey.api.client.UniformInterfaceException;

import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.SpringJerseyTest;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.utils.JsonUtils;

public class UserGroupControllerAdminTest extends SpringJerseyTest {
    @Autowired
    private HttpAuthorizationHandler handler;

    @Test
    public void testListDoryGroups () throws KustvaktException {
        ClientResponse response = resource().path("group").path("list")
                .queryParam("username", "dory")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue("admin",
                                "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .get(ClientResponse.class);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String entity = response.getEntity(String.class);
        //        System.out.println(entity);
        JsonNode node = JsonUtils.readTree(entity);
        JsonNode group = node.get(1);
        assertEquals(2, group.at("/id").asInt());
        assertEquals("dory group", group.at("/name").asText());
        assertEquals("dory", group.at("/owner").asText());
        assertEquals(3, group.at("/members").size());
    }

    @Test
    public void testListWithoutUsername () throws UniformInterfaceException,
            ClientHandlerException, KustvaktException {
        ClientResponse response = resource().path("group").path("list")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue("admin",
                                "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .get(ClientResponse.class);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String entity = response.getEntity(String.class);
        assertEquals("[]", entity);
    }


}
