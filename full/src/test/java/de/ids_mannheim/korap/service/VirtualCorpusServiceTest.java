package de.ids_mannheim.korap.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import de.ids_mannheim.korap.constant.QueryType;
import de.ids_mannheim.korap.constant.UserGroupStatus;
import de.ids_mannheim.korap.constant.ResourceType;
import de.ids_mannheim.korap.dto.VirtualCorpusAccessDto;
import de.ids_mannheim.korap.dto.VirtualCorpusDto;
import de.ids_mannheim.korap.entity.UserGroup;
import de.ids_mannheim.korap.entity.VirtualCorpus;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.web.input.QueryJson;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:test-config.xml")
public class VirtualCorpusServiceTest {

    @Autowired
    private VirtualCorpusService vcService;
    @Autowired
    private UserGroupService groupService;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testCreateNonUniqueVC () throws KustvaktException {
        thrown.expect(KustvaktException.class);
        // EM: message differs depending on the database used
        // for testing. The message below is from sqlite.
        // thrown.expectMessage("A UNIQUE constraint failed "
        // + "(UNIQUE constraint failed: virtual_corpus.name, "
        // + "virtual_corpus.created_by)");

        QueryJson vc = new QueryJson();
        vc.setCorpusQuery("corpusSigle=GOE");
        vc.setType(ResourceType.PRIVATE);
        vc.setQueryType(QueryType.VIRTUAL_CORPUS);
        vcService.storeVC(vc, "dory-vc", "dory");
    }

    @Test
    public void createDeletePublishVC () throws KustvaktException {
        String vcName = "new-published-vc";

        QueryJson vc = new QueryJson();
        vc.setCorpusQuery("corpusSigle=GOE");
        vc.setType(ResourceType.PUBLISHED);
        vc.setQueryType(QueryType.VIRTUAL_CORPUS);
        String username = "VirtualCorpusServiceTest";
        vcService.storeVC(vc, vcName, username );

        List<VirtualCorpusAccessDto> accesses =
                vcService.listVCAccessByUsername("admin");
        int size = accesses.size();

        VirtualCorpusAccessDto dto = accesses.get(accesses.size() - 1);
        assertEquals(vcName, dto.getVcName());
        assertEquals("system", dto.getCreatedBy());
        assertTrue(dto.getUserGroupName().startsWith("auto"));

        // check hidden group
        int groupId = dto.getUserGroupId();
        UserGroup group = groupService.retrieveUserGroupById(groupId);
        assertEquals(UserGroupStatus.HIDDEN, group.getStatus());

        //delete vc
        vcService.deleteVCByName(username, vcName, username);
        
        // check hidden access
        accesses = vcService.listVCAccessByUsername("admin");
        assertEquals(size-1, accesses.size());
        
        // check hidden group
        thrown.expect(KustvaktException.class);
        group = groupService.retrieveUserGroupById(groupId);
        thrown.expectMessage("Group with id "+groupId+" is not found");
    }

    @Test
    public void testEditPublishVC () throws KustvaktException {
        String username = "dory";
        int vcId = 2;

        String vcName = "group-vc";
        VirtualCorpus existingVC =
                vcService.searchVCByName(username, vcName, username);
        QueryJson vcJson = new QueryJson();
        vcJson.setType(ResourceType.PUBLISHED);

        vcService.editVC(existingVC, vcJson, vcName, username);

        // check VC
        VirtualCorpusDto vcDto = vcService.searchVCById("dory", vcId);
        assertEquals(vcName, vcDto.getName());
        assertEquals(ResourceType.PUBLISHED.displayName(),
                vcDto.getType());

        // check access
        List<VirtualCorpusAccessDto> accesses =
                vcService.listVCAccessByUsername("admin");
        int size = accesses.size();
        VirtualCorpusAccessDto dto = accesses.get(accesses.size() - 1);
        assertEquals(vcName, dto.getVcName());
        assertEquals("system", dto.getCreatedBy());
        assertTrue(dto.getUserGroupName().startsWith("auto"));

        // check auto hidden group
        int groupId = dto.getUserGroupId();
        UserGroup group = groupService.retrieveUserGroupById(groupId);
        assertEquals(UserGroupStatus.HIDDEN, group.getStatus());

        // 2nd edit (withdraw from publication)

        vcJson = new QueryJson();
        vcJson.setType(ResourceType.PROJECT);

        vcService.editVC(existingVC, vcJson, vcName, username);

        // check VC
        vcDto = vcService.searchVCById("dory", vcId);
        assertEquals("group-vc", vcDto.getName());
        assertEquals(ResourceType.PROJECT.displayName(), vcDto.getType());

        // check access
        accesses = vcService.listVCAccessByUsername("admin");
        assertEquals(size - 1, accesses.size());

        thrown.expect(KustvaktException.class);
        group = groupService.retrieveUserGroupById(groupId);
        thrown.expectMessage("Group with id 5 is not found");
    }

}
