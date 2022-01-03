package de.ids_mannheim.korap.dao;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import de.ids_mannheim.korap.constant.QueryAccessStatus;
import de.ids_mannheim.korap.entity.QueryAccess;
import de.ids_mannheim.korap.entity.UserGroup;
import de.ids_mannheim.korap.exceptions.KustvaktException;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:test-config.xml")
public class VirtualCorpusAccessDaoTest {

    @Autowired
    private QueryAccessDao dao;

    @Test
    public void getAccessByVC () throws KustvaktException {
        List<QueryAccess> vcaList = dao.retrieveActiveAccessByQuery(2);
        QueryAccess access = vcaList.get(0);
        assertEquals(QueryAccessStatus.ACTIVE, access.getStatus());
        assertEquals("dory", access.getCreatedBy());
        
        UserGroup group = access.getUserGroup();
        assertEquals(2, group.getId());
    }
    
}
