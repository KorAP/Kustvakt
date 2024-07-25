package de.ids_mannheim.korap.web.controller.vc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

import de.ids_mannheim.korap.constant.GroupMemberStatus;
import de.ids_mannheim.korap.constant.PredefinedRole;
import de.ids_mannheim.korap.constant.UserGroupStatus;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import jakarta.ws.rs.ProcessingException;

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
        
        testAccessPublishedVC("gill", testUser, vcName);
        
        deleteVC(vcName, testUser, testUser);
        
        // EM: check if the hidden groups are deleted as well
        node = getHiddenGroup(vcName);
        assertEquals(StatusCodes.NO_RESOURCE_FOUND,
                node.at("/errors/0/0").asInt());
        assertEquals("No hidden group for query " + vcName + " is found",
                node.at("/errors/0/1").asText());
    }
    
    private void testAccessPublishedVC (String username, String vcCreator,
            String vcName) throws ProcessingException, KustvaktException {
        retrieveVCInfo(username, vcCreator, vcName);
        
        JsonNode node = getHiddenGroup(vcName);
        System.out.println(node.toPrettyString());
        assertEquals("system", node.at("/owner").asText());
        assertEquals(UserGroupStatus.HIDDEN.name(), 
                node.at("/status").asText());
        assertEquals(username, node.at("/members/0/userId").asText());
        assertEquals(GroupMemberStatus.ACTIVE.name(), 
                node.at("/members/0/status").asText());
        assertEquals(1, node.at("/members/0/roles").size());
        assertEquals(PredefinedRole.QUERY_ACCESS.name(), 
                node.at("/members/0/roles/0").asText());
        String groupName = node.at("/name").asText();

        node = listAccessByGroup("admin", groupName);
        assertEquals(1, node.size());
        assertEquals(vcName, node.at("/0/queryName").asText());
        assertEquals(groupName, node.at("/0/userGroupName").asText());
        assertEquals(1, node.at("/0/members").size());
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
        
        testSharePublishedVC(vcName);
        
        deleteVC(vcName, "marlin", "marlin");
        
        node = listAccessByGroup("admin", marlinGroupName);
        assertEquals(0, node.size());
        
        deleteGroupByName(marlinGroupName, "marlin");
    }
    
    private void testSharePublishedVC (String vcName) throws KustvaktException {
        createMarlinGroup();
        inviteMember(marlinGroupName, "marlin", "dory");
        subscribe(marlinGroupName, "dory");

        JsonNode node = listVC("dory");
        assertEquals(3, node.size());

        shareVC("marlin", vcName, marlinGroupName, "marlin");
        
        // check marlin-group access
        node = listAccessByGroup("admin", marlinGroupName);
        assertEquals(1, node.size());
        assertEquals(vcName, node.at("/0/queryName").asText());
        assertEquals(marlinGroupName, node.at("/0/userGroupName").asText());
        assertEquals(2, node.at("/0/members").size());

        // check hidden group access
        node = getHiddenGroup(vcName);
        String hiddenGroupName = node.at("/name").asText();
        node = listAccessByGroup("admin", hiddenGroupName);
        assertEquals(0, node.at("/0/members").size());
        
//        testAccessPublishedVC("dory", "marlin", vcName);
    }
    
}
