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
        String username = "VirtualCorpusServiceTest";
        
        VirtualCorpusJson vc = new VirtualCorpusJson();
        vc.setCorpusQuery("corpusSigle=GOE");
        vc.setCreatedBy(username);
        vc.setName("new published vc");
        vc.setType(VirtualCorpusType.PUBLISHED);
        int vcId = vcService.storeVC(vc, "VirtualCorpusServiceTest");

        List<VirtualCorpusAccess> accesses = vcService.retrieveAllVCAccess(vcId);
        assertEquals(1, accesses.size());
        
        VirtualCorpusAccess access = accesses.get(0);
        assertEquals(VirtualCorpusAccessStatus.HIDDEN, access.getStatus());
        
        // delete VC
        vcService.deleteVC(username, vcId);
        accesses = vcService.retrieveAllVCAccess(vcId);
        assertEquals(0, accesses.size());
    }

}
