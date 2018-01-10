package de.ids_mannheim.korap.service;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import de.ids_mannheim.korap.constant.VirtualCorpusAccessStatus;
import de.ids_mannheim.korap.constant.VirtualCorpusType;
import de.ids_mannheim.korap.dto.UserGroupDto;
import de.ids_mannheim.korap.entity.VirtualCorpusAccess;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.web.input.VirtualCorpusJson;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:test-config.xml")
public class VirtualCorpusServiceTest {

    @Autowired
    private VirtualCorpusService vcService;

    @Test
    public void createPublishVC () throws KustvaktException {

        VirtualCorpusJson vc = new VirtualCorpusJson();
        vc.setCorpusQuery("corpusSigle=GOE");
        vc.setCreatedBy("VirtualCorpusServiceTest");
        vc.setName("new published vc");
        vc.setType(VirtualCorpusType.PUBLISHED);
        int vcId = vcService.storeVC(vc, "VirtualCorpusServiceTest");

        List<VirtualCorpusAccess> accesses = vcService.retrieveVCAccess(vcId);
        assertEquals(2, accesses.size());
        for (VirtualCorpusAccess access : accesses) {
            assertEquals(VirtualCorpusAccessStatus.HIDDEN, access.getStatus());
        }
    }

}
