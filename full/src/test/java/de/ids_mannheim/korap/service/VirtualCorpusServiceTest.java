package de.ids_mannheim.korap.service;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import de.ids_mannheim.korap.constant.UserGroupStatus;
import de.ids_mannheim.korap.constant.VirtualCorpusAccessStatus;
import de.ids_mannheim.korap.constant.VirtualCorpusType;
import de.ids_mannheim.korap.dto.VirtualCorpusDto;
import de.ids_mannheim.korap.entity.UserGroup;
import de.ids_mannheim.korap.entity.VirtualCorpusAccess;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.web.input.VirtualCorpusJson;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:test-config.xml")
public class VirtualCorpusServiceTest {

    @Autowired
    private VirtualCorpusService vcService;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testCreateNonUniqueVC () throws KustvaktException {
        thrown.expect(KustvaktException.class);
        // EM: message differs depending on the database used
        // for testing. The message below is from sqlite.
//        thrown.expectMessage("A UNIQUE constraint failed "
//                + "(UNIQUE constraint failed: virtual_corpus.name, "
//                + "virtual_corpus.created_by)");

        VirtualCorpusJson vc = new VirtualCorpusJson();
        vc.setCorpusQuery("corpusSigle=GOE");
        vc.setName("dory VC");
        vc.setType(VirtualCorpusType.PRIVATE);
        vcService.storeVC(vc, vc.getName(), "dory");
    }

    @Test
    public void createDeletePublishVC () throws KustvaktException {
        String username = "VirtualCorpusServiceTest";

        VirtualCorpusJson vc = new VirtualCorpusJson();
        vc.setCorpusQuery("corpusSigle=GOE");
        vc.setName("new published vc");
        vc.setType(VirtualCorpusType.PUBLISHED);
        int vcId = vcService.storeVC(vc,vc.getName(), "VirtualCorpusServiceTest");

        List<VirtualCorpusAccess> accesses =
                vcService.retrieveAllVCAccess(vcId);
        assertEquals(1, accesses.size());

        VirtualCorpusAccess access = accesses.get(0);
        assertEquals(VirtualCorpusAccessStatus.HIDDEN, access.getStatus());

        vcService.deleteVC(username, vcId);
        accesses = vcService.retrieveAllVCAccess(vcId);
        assertEquals(0, accesses.size());
    }

    @Test
    public void testEditPublishVC () throws KustvaktException {
        String username = "dory";
        int vcId = 2;

        VirtualCorpusJson vcJson = new VirtualCorpusJson();
        vcJson.setId(vcId);
        vcJson.setName("group VC published");
        vcJson.setType(VirtualCorpusType.PUBLISHED);

        vcService.editVC(vcJson, username);

        // check VC
        VirtualCorpusDto vcDto = vcService.searchVCById("dory", vcId);
        assertEquals("group VC published", vcDto.getName());
        assertEquals(VirtualCorpusType.PUBLISHED.displayName(),
                vcDto.getType());

        // check access
        List<VirtualCorpusAccess> accesses =
                vcService.retrieveAllVCAccess(vcId);
        assertEquals(2, accesses.size());

        VirtualCorpusAccess access = accesses.get(1);
        assertEquals(VirtualCorpusAccessStatus.HIDDEN, access.getStatus());

        // check auto hidden group
        UserGroup autoHiddenGroup = access.getUserGroup();
        assertEquals(UserGroupStatus.HIDDEN, autoHiddenGroup.getStatus());

        // 2nd edit (withdraw from publication)
        vcJson = new VirtualCorpusJson();
        vcJson.setId(vcId);
        vcJson.setName("group VC");
        vcJson.setType(VirtualCorpusType.PROJECT);

        vcService.editVC(vcJson, username);

        // check VC
        vcDto = vcService.searchVCById("dory", vcId);
        assertEquals("group VC", vcDto.getName());
        assertEquals(VirtualCorpusType.PROJECT.displayName(), vcDto.getType());

        // check access
        accesses = vcService.retrieveAllVCAccess(vcId);
        assertEquals(1, accesses.size());
    }

}
