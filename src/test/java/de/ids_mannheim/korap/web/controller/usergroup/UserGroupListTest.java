package de.ids_mannheim.korap.web.controller.usergroup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.net.HttpHeaders;

import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.utils.JsonUtils;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

public class UserGroupListTest extends UserGroupTestBase{

    @Test
    public void testListDoryGroups () throws KustvaktException {
        createDoryGroup();
        inviteMember(doryGroupName, "dory", "marlin");
        inviteMember(doryGroupName, "dory", "nemo");
        
        JsonNode node = listUserGroups("dory");
        JsonNode group = node.get(0);
        assertEquals(group.at("/name").asText(), "dory-group");
        assertEquals(group.at("/owner").asText(), "dory");
        assertEquals(3, group.at("/members").size());
        
        testListNemoGroups();
        testListMarlinGroups();
        
        deleteGroupByName(doryGroupName,"dory");
        deleteGroupByName(marlinGroupName, "marlin");
    }
    
    public void testListNemoGroups () throws KustvaktException {
        Response response = subscribe(doryGroupName, "nemo");
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = listUserGroups("nemo");
        assertEquals(node.at("/0/name").asText(), "dory-group");
        assertEquals(node.at("/0/owner").asText(), "dory");
        // group members are not allowed to see other members
        assertTrue(node.at("/0/members").isMissingNode());
        System.out.println(node.toPrettyString());
    }
    
    // marlin has 2 groups
    public void testListMarlinGroups () throws KustvaktException {
        createMarlinGroup();
        Response response = subscribe(doryGroupName, "marlin");
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = listUserGroups("marlin");
        assertEquals(2, node.size());
    }
    
    @Test
    public void testListGroupGuest () throws KustvaktException {
        Response response = target().path(API_VERSION).path("group").request()
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32").get();
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
        assertEquals(StatusCodes.AUTHORIZATION_FAILED,
                node.at("/errors/0/0").asInt());
        assertEquals(node.at("/errors/0/1").asText(),
                "Unauthorized operation for user: guest");
    }
}
