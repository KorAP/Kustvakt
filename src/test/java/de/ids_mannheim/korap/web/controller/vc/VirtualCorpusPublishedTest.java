package de.ids_mannheim.korap.web.controller.vc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

import de.ids_mannheim.korap.constant.GroupMemberStatus;
import de.ids_mannheim.korap.constant.PredefinedRole;
import de.ids_mannheim.korap.constant.PrivilegeType;
import de.ids_mannheim.korap.constant.ResourceType;
import de.ids_mannheim.korap.constant.UserGroupStatus;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

public class VirtualCorpusPublishedTest extends VirtualCorpusTestBase{
    
    private String testUser = "vcPublishedTest";

    @Test
    public void testCreatePublishedVC () throws KustvaktException {
        String vcName = "new-published-vc";
        createPublishedVC(testUser, vcName);
        
        // test list owner vc
        JsonNode node = retrieveVCInfo(testUser, testUser, vcName);
        assertEquals(vcName, node.get("name").asText());
        
        node = getHiddenGroup(vcName);
        assertEquals("system", node.at("/owner").asText());
        assertEquals(UserGroupStatus.HIDDEN.name(), 
                node.at("/status").asText());
        
        testRetrievePublishedVC("gill", testUser, vcName);
        
        String groupName = node.at("/name").asText();
        testDeletePublishedVCUnauthorized(testUser, vcName, "gill");
        testDeletePublishedVC(testUser, vcName, testUser, groupName);
    }
    
    private void testRetrievePublishedVC (String username, String vcCreator,
            String vcName) throws ProcessingException, KustvaktException {
        retrieveVCInfo(username, vcCreator, vcName);
        
        JsonNode node = getHiddenGroup(vcName);
        assertEquals("system", node.at("/owner").asText());
        assertEquals(UserGroupStatus.HIDDEN.name(), 
                node.at("/status").asText());
        assertEquals(username, node.at("/members/0/userId").asText());
        assertEquals(GroupMemberStatus.ACTIVE.name(), 
                node.at("/members/0/status").asText());
        assertEquals(1, node.at("/members/0/privileges").size());
        assertEquals(PrivilegeType.READ_QUERY.name(), 
                node.at("/members/0/privileges/0").asText());
        String groupName = node.at("/name").asText();

        node = listRolesByGroup("admin", groupName);
        assertEquals(1, node.size());
        assertEquals(vcName, node.at("/0/queryName").asText());
        assertEquals(groupName, node.at("/0/userGroupName").asText());
        assertEquals(1, node.at("/0/members").size());
    }
    
    private void testDeletePublishedVC (String vcCreator, String vcName,
            String deletedBy, String hiddenGroupName) throws KustvaktException {
        deleteVC(vcName, vcCreator, deletedBy);

        // EM: check if the hidden groups are deleted as well
        JsonNode node = getHiddenGroup(vcName);
        assertEquals(StatusCodes.NO_RESOURCE_FOUND,
                node.at("/errors/0/0").asInt());
        assertEquals("No hidden group for query " + vcName + " is found",
                node.at("/errors/0/1").asText());
        
        testHiddenGroupNotFound(hiddenGroupName);
    }
    
    private void testHiddenGroupNotFound (String hiddenGroupName)
            throws KustvaktException {
        JsonNode node = listRolesByGroup("admin", hiddenGroupName);
        assertEquals(StatusCodes.NO_RESOURCE_FOUND,
                node.at("/errors/0/0").asInt());
        assertEquals("Group " + hiddenGroupName + " is not found",
                node.at("/errors/0/1").asText());

    }

    private void testDeletePublishedVCUnauthorized (String vcCreator,
            String vcName, String deletedBy)
            throws KustvaktException {
        Response response = deleteVC(vcName, vcCreator, deletedBy);
        assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
        testResponseUnauthorized(response, deletedBy);
    }
    
    @Test
    public void testMarlinPublishedVC () throws KustvaktException {
        
        JsonNode node = testListOwnerVC("marlin");
        assertEquals(2, node.size());
        node = listVC("marlin");
        assertEquals(3, node.size());
        
        String vcName = "marlin-published-vc";
        createPublishedVC("marlin", vcName);
        
        node = testListOwnerVC("marlin");
        assertEquals(3, node.size());
        node = listVC("marlin");
        assertEquals(4, node.size());
        
        String groupName = testSharePublishedVC(vcName);
        
        // dory is a member
        testDeletePublishedVCUnauthorized("marlin", vcName, "dory");
        // add dory as group admin
        addAdminRole(marlinGroupName, "dory", "marlin");
        testDeletePublishedVCUnauthorized("marlin", vcName, "dory");
        
        testDeletePublishedVC("marlin",vcName,"marlin", groupName);
        
        node = listRolesByGroup("admin", marlinGroupName);
        assertEquals(0, node.size());
        
        deleteGroupByName(marlinGroupName, "marlin");
    }
    
    private String testSharePublishedVC (String vcName) throws KustvaktException {
        createMarlinGroup();
        inviteMember(marlinGroupName, "marlin", "dory");
        subscribe(marlinGroupName, "dory");

        JsonNode node = listVC("dory");
        assertEquals(3, node.size());

        shareVC("marlin", vcName, marlinGroupName, "marlin");
        
        node = listVC("dory");
        assertEquals(4, node.size());
        node = listVC("marlin");
        assertEquals(4, node.size());
        
        // check marlin-group access
        node = listRolesByGroup("admin", marlinGroupName);
        assertEquals(1, node.size());
        assertEquals(vcName, node.at("/0/queryName").asText());
        assertEquals(marlinGroupName, node.at("/0/userGroupName").asText());
        assertEquals(2, node.at("/0/members").size());

        // check hidden group access
        node = getHiddenGroup(vcName);
        String hiddenGroupName = node.at("/name").asText();
        node = listRolesByGroup("admin", hiddenGroupName);
        assertEquals(0, node.at("/0/members").size());
        
        testAddMemberAfterSharingPublishedVC(hiddenGroupName);
        testRetrievePublishedVC("dory", "marlin", vcName);
        return hiddenGroupName;
    }
    
    private void testAddMemberAfterSharingPublishedVC (String hiddenGroupName)
            throws KustvaktException {
        JsonNode node = listVC("nemo");
        assertEquals(2, node.size());

        inviteMember(marlinGroupName, "marlin", "nemo");
        subscribe(marlinGroupName, "nemo");

        node = listVC("nemo");
        assertEquals(3, node.size());

        node = listRolesByGroup("admin", marlinGroupName);
        assertEquals(3, node.at("/0/members").size());

        node = listRolesByGroup("admin", hiddenGroupName);
        assertEquals(0, node.at("/0/members").size());
    }
    
    @Test
    public void testPublishProjectVC () throws KustvaktException {
        String vcName = "group-vc";
        JsonNode node = retrieveVCInfo("dory", "dory", vcName);
        assertEquals(ResourceType.PROJECT.displayName(),
                node.get("type").asText());
        
        // edit PROJECT to PUBLISHED vc
        String json = "{\"type\": \"PUBLISHED\"}";
        editVC("dory", "dory", vcName, json);
        
        // check VC type
        node = testListOwnerVC("dory");
        JsonNode n = node.get(1);
        assertEquals(ResourceType.PUBLISHED.displayName(),
                n.get("type").asText());
        
        // check hidden group and roles
        node = getHiddenGroup(vcName);
        String hiddenGroupName = node.at("/name").asText();
        node = listRolesByGroup("admin", hiddenGroupName);
        assertEquals(1, node.size());
        node = node.get(0);
        assertEquals(vcName, node.at("/queryName").asText());
        assertEquals(hiddenGroupName, node.at("/userGroupName").asText());
        
        // change PUBLISHED to PROJECT
        json = "{\"type\": \"PROJECT\"}";
        editVC("dory", "dory", vcName, json);
        node = testListOwnerVC("dory");
        assertEquals(ResourceType.PROJECT.displayName(),
                node.get(1).get("type").asText());
        
        testHiddenGroupNotFound(hiddenGroupName);
    }
}
