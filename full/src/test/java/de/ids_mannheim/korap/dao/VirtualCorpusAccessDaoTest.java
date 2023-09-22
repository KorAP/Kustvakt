package de.ids_mannheim.korap.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import de.ids_mannheim.korap.constant.QueryAccessStatus;
import de.ids_mannheim.korap.entity.QueryAccess;
import de.ids_mannheim.korap.entity.UserGroup;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(SpringExtension.class)
@ContextConfiguration("classpath:test-config.xml")
@DisplayName("Virtual Corpus Access Dao Test")
class VirtualCorpusAccessDaoTest {

    @Autowired
    private QueryAccessDao dao;

    @Test
    @DisplayName("Get Access By VC")
    void getAccessByVC() throws KustvaktException {
        List<QueryAccess> vcaList = dao.retrieveActiveAccessByQuery(2);
        QueryAccess access = vcaList.get(0);
        assertEquals(QueryAccessStatus.ACTIVE, access.getStatus());
        assertEquals(access.getCreatedBy(), "dory");
        UserGroup group = access.getUserGroup();
        assertEquals(2, group.getId());
    }
}
