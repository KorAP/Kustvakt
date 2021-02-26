package de.ids_mannheim.korap.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import de.ids_mannheim.korap.constant.QueryType;
import de.ids_mannheim.korap.constant.ResourceType;
import de.ids_mannheim.korap.constant.UserGroupStatus;
import de.ids_mannheim.korap.dto.QueryAccessDto;
import de.ids_mannheim.korap.dto.QueryDto;
import de.ids_mannheim.korap.entity.QueryDO;
import de.ids_mannheim.korap.entity.UserGroup;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.web.input.QueryJson;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:test-config.xml")
public class VirtualCorpusServiceTest {

    @Autowired
    private QueryService vcService;
    @Autowired
    private UserGroupService groupService;

    @Test
    public void testCreateNonUniqueVC () throws KustvaktException {
        
        // EM: message differs depending on the database used
        // for testing. The message below is from sqlite.
        // thrown.expectMessage("A UNIQUE constraint failed "
        // + "(UNIQUE constraint failed: virtual_corpus.name, "
        // + "virtual_corpus.created_by)");

        QueryJson vc = new QueryJson();
        vc.setCorpusQuery("corpusSigle=GOE");
        vc.setType(ResourceType.PRIVATE);
        vc.setQueryType(QueryType.VIRTUAL_CORPUS);
        
        Assert.assertThrows(KustvaktException.class,
                () -> vcService.storeQuery(vc, "dory-vc", "dory"));
    }

    @Test
    public void createDeletePublishVC () throws KustvaktException {
        String vcName = "new-published-vc";

        QueryJson vc = new QueryJson();
        vc.setCorpusQuery("corpusSigle=GOE");
        vc.setType(ResourceType.PUBLISHED);
        vc.setQueryType(QueryType.VIRTUAL_CORPUS);
        String username = "VirtualCorpusServiceTest";
        vcService.storeQuery(vc, vcName, username );

        List<QueryAccessDto> accesses =
                vcService.listQueryAccessByUsername("admin");
        int size = accesses.size();

        QueryAccessDto dto = accesses.get(accesses.size() - 1);
        assertEquals(vcName, dto.getQueryName());
        assertEquals("system", dto.getCreatedBy());
        assertTrue(dto.getUserGroupName().startsWith("auto"));

        // check hidden group
        int groupId = dto.getUserGroupId();
        UserGroup group = groupService.retrieveUserGroupById(groupId);
        assertEquals(UserGroupStatus.HIDDEN, group.getStatus());

        //delete vc
        vcService.deleteQueryByName(username, vcName, username,
                QueryType.VIRTUAL_CORPUS);
        
        // check hidden access
        accesses = vcService.listQueryAccessByUsername("admin");
        assertEquals(size-1, accesses.size());
        
        // check hidden group
        KustvaktException e = assertThrows(KustvaktException.class,
                () -> groupService.retrieveUserGroupById(groupId));
        assertEquals("Group with id " + groupId + " is not found",
                e.getMessage());
    }

    @Test
    public void testEditPublishVC () throws KustvaktException {
        String username = "dory";
        int vcId = 2;

        String vcName = "group-vc";
        QueryDO existingVC =
                vcService.searchQueryByName(username, vcName, username, QueryType.VIRTUAL_CORPUS);
        QueryJson vcJson = new QueryJson();
        vcJson.setType(ResourceType.PUBLISHED);

        vcService.editQuery(existingVC, vcJson, vcName, username);

        // check VC
        QueryDto vcDto = vcService.searchQueryById("dory", vcId);
        assertEquals(vcName, vcDto.getName());
        assertEquals(ResourceType.PUBLISHED.displayName(),
                vcDto.getType());

        // check access
        List<QueryAccessDto> accesses =
                vcService.listQueryAccessByUsername("admin");
        int size = accesses.size();
        QueryAccessDto dto = accesses.get(accesses.size() - 1);
        assertEquals(vcName, dto.getQueryName());
        assertEquals("system", dto.getCreatedBy());
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
        assertEquals("group-vc", vcDto.getName());
        assertEquals(ResourceType.PROJECT.displayName(), vcDto.getType());

        // check access
        accesses = vcService.listQueryAccessByUsername("admin");
        assertEquals(size - 1, accesses.size());

        KustvaktException e = assertThrows(KustvaktException.class,
                () -> groupService.retrieveUserGroupById(groupId));
        
        assertEquals("Group with id 5 is not found", e.getMessage());
    }

}
