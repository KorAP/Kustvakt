package de.ids_mannheim.korap.dao;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import de.ids_mannheim.korap.constant.VirtualCorpusAccessStatus;
import de.ids_mannheim.korap.entity.UserGroup;
import de.ids_mannheim.korap.entity.VirtualCorpusAccess;
import de.ids_mannheim.korap.exceptions.KustvaktException;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:test-config.xml")
public class VirtualCorpusAccessDaoTest {

    @Autowired
    private VirtualCorpusAccessDao dao;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void getAccessByVC () throws KustvaktException {
        List<VirtualCorpusAccess> vcaList = dao.retrieveActiveAccessByVC(2);
        VirtualCorpusAccess access = vcaList.get(0);
        assertEquals(VirtualCorpusAccessStatus.ACTIVE, access.getStatus());
        assertEquals("dory", access.getCreatedBy());
        
        UserGroup group = access.getUserGroup();
        assertEquals(2, group.getId());
    }
    
    
}
