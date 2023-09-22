package de.ids_mannheim.korap.service;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import de.ids_mannheim.korap.constant.QueryType;
import de.ids_mannheim.korap.constant.ResourceType;
import de.ids_mannheim.korap.constant.UserGroupStatus;
import de.ids_mannheim.korap.dto.QueryAccessDto;
import de.ids_mannheim.korap.dto.QueryDto;
import de.ids_mannheim.korap.entity.QueryDO;
import de.ids_mannheim.korap.entity.UserGroup;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.web.input.QueryJson;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration("classpath:test-config.xml")
@DisplayName("Virtual Corpus Service Test")
class VirtualCorpusServiceTest {

    @Autowired
    private QueryService vcService;

    @Autowired
    private UserGroupService groupService;

    @Test
    @DisplayName("Test Create Non Unique VC")
    void testCreateNonUniqueVC() throws KustvaktException {
        // EM: message differs depending on the database used
        // for testing. The message below is from sqlite.
        // thrown.expectMessage("A UNIQUE constraint failed "
        // + "(UNIQUE constraint failed: virtual_corpus.name, "
        // + "virtual_corpus.created_by)");
        QueryJson vc = new QueryJson();
        vc.setCorpusQuery("corpusSigle=GOE");
        vc.setType(ResourceType.PRIVATE);
        vc.setQueryType(QueryType.VIRTUAL_CORPUS);
        assertThrows(KustvaktException.class, () -> vcService.storeQuery(vc, "dory-vc", "dory", "dory"));
    }

    @Test
    @DisplayName("Create Delete Publish VC")
    void createDeletePublishVC() throws KustvaktException {
        String vcName = "new-published-vc";
        QueryJson vc = new QueryJson();
        vc.setCorpusQuery("corpusSigle=GOE");
        vc.setType(ResourceType.PUBLISHED);
        vc.setQueryType(QueryType.VIRTUAL_CORPUS);
        String username = "VirtualCorpusServiceTest";
        vcService.storeQuery(vc, vcName, username, username);
        List<QueryAccessDto> accesses = vcService.listQueryAccessByUsername("admin");
        int size = accesses.size();
        QueryAccessDto dto = accesses.get(accesses.size() - 1);
        assertEquals(vcName, dto.getQueryName());
        assertEquals(dto.getCreatedBy(), "system");
        assertTrue(dto.getUserGroupName().startsWith("auto"));
        // check hidden group
        int groupId = dto.getUserGroupId();
        UserGroup group = groupService.retrieveUserGroupById(groupId);
        assertEquals(UserGroupStatus.HIDDEN, group.getStatus());
        // delete vc
        vcService.deleteQueryByName(username, vcName, username, QueryType.VIRTUAL_CORPUS);
        // check hidden access
        accesses = vcService.listQueryAccessByUsername("admin");
        assertEquals(size - 1, accesses.size());
        // check hidden group
        KustvaktException e = assertThrows(KustvaktException.class, () -> groupService.retrieveUserGroupById(groupId));
        assertEquals("Group with id " + groupId + " is not found", e.getMessage());
    }

    @Test
    @DisplayName("Test Edit Publish VC")
    void testEditPublishVC() throws KustvaktException {
        String username = "dory";
        int vcId = 2;
        String vcName = "group-vc";
        QueryDO existingVC = vcService.searchQueryByName(username, vcName, username, QueryType.VIRTUAL_CORPUS);
        QueryJson vcJson = new QueryJson();
        vcJson.setType(ResourceType.PUBLISHED);
        vcService.editQuery(existingVC, vcJson, vcName, username);
        // check VC
        QueryDto vcDto = vcService.searchQueryById("dory", vcId);
        assertEquals(vcName, vcDto.getName());
        assertEquals(ResourceType.PUBLISHED.displayName(), vcDto.getType());
        // check access
        List<QueryAccessDto> accesses = vcService.listQueryAccessByUsername("admin");
        int size = accesses.size();
        QueryAccessDto dto = accesses.get(accesses.size() - 1);
        assertEquals(vcName, dto.getQueryName());
        assertEquals(dto.getCreatedBy(), "system");
        assertTrue(dto.getUserGroupName().startsWith("auto"));
        // check auto hidden group
        int groupId = dto.getUserGroupId();
        UserGroup group = groupService.retrieveUserGroupById(groupId);
        assertEquals(UserGroupStatus.HIDDEN, group.getStatus());
        // 2nd edit (withdraw from publication)
        vcJson = new QueryJson();
        vcJson.setType(ResourceType.PROJECT);
        vcService.editQuery(existingVC, vcJson, vcName, username);
        // check VC
        vcDto = vcService.searchQueryById("dory", vcId);
        assertEquals(vcDto.getName(), "group-vc");
        assertEquals(ResourceType.PROJECT.displayName(), vcDto.getType());
        // check access
        accesses = vcService.listQueryAccessByUsername("admin");
        assertEquals(size - 1, accesses.size());
        KustvaktException e = assertThrows(KustvaktException.class, () -> groupService.retrieveUserGroupById(groupId));
        assertEquals("Group with id " + groupId + " is not found", e.getMessage());
    }
}
