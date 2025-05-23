package de.ids_mannheim.korap.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import de.ids_mannheim.korap.constant.QueryType;
import de.ids_mannheim.korap.constant.ResourceType;
import de.ids_mannheim.korap.entity.QueryDO;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.user.User;
import de.ids_mannheim.korap.web.controller.vc.VirtualCorpusTestBase;
import jakarta.persistence.PersistenceException;

public class VirtualCorpusDaoTest extends VirtualCorpusTestBase {

    @Autowired
    private QueryDao dao;

    @Test
    public void testListVCByType () throws KustvaktException {
    	createDoryVC();
    	createMarlinPublishedVC();
    	
        List<QueryDO> vcList = dao.retrieveQueryByType(ResourceType.PUBLISHED,
                null, QueryType.VIRTUAL_CORPUS);
        assertEquals(1, vcList.size());
        QueryDO vc = vcList.get(0);
        assertEquals("published-vc", vc.getName());
        assertEquals("marlin", vc.getCreatedBy());
        
        deleteVC("dory-vc", "dory", "dory");
        deleteVC("published-vc", "marlin", "marlin");
    }

    @Test
    public void testSystemVC () throws KustvaktException {
        // insert vc
        int id = dao.createQuery("system-vc-2", ResourceType.SYSTEM,
                QueryType.VIRTUAL_CORPUS, User.CorpusAccess.FREE,
                "corpusSigle=GOE", "definition", "description", "experimental",
                false, "test class", null, null);
        // select vc
        List<QueryDO> vcList = dao.retrieveQueryByType(ResourceType.SYSTEM,
                null, QueryType.VIRTUAL_CORPUS);
        assertEquals(2, vcList.size());
        QueryDO vc = dao.retrieveQueryById(id);
        // delete vc
        dao.deleteQuery(vc);
        // check if vc has been deleted
        KustvaktException exception = assertThrows(KustvaktException.class,
                () -> {
                    dao.retrieveQueryById(id);
                });
        assertEquals(StatusCodes.NO_RESOURCE_FOUND,
                exception.getStatusCode().intValue());
    }

    @Test
    public void testNonUniqueVC () throws KustvaktException {

        PersistenceException exception = assertThrows(
                PersistenceException.class, () -> {
                    dao.createQuery("system-vc", ResourceType.SYSTEM,
                            QueryType.VIRTUAL_CORPUS, User.CorpusAccess.FREE,
                            "corpusSigle=GOE", "definition", "description",
                            "experimental", false, "system", null, null);
                });

        assertEquals(exception.getMessage(),
                "Converting `org.hibernate.exception.GenericJDBCException` "
                        + "to JPA `PersistenceException` : could not execute statement");
    }

    @Test
    public void retrieveSystemVC () throws KustvaktException {
        List<QueryDO> vc = dao.retrieveQueryByType(ResourceType.SYSTEM, null,
                QueryType.VIRTUAL_CORPUS);
        assertEquals(1, vc.size());
    }

    /**
     * retrieve private and group VC
     *
     * @throws KustvaktException
     */
    @Test
    public void retrieveVCByUserDory () throws KustvaktException {
    	createDoryVC();
    	createDoryGroupVC();
        List<QueryDO> virtualCorpora = dao.retrieveQueryByUser("dory",
                QueryType.VIRTUAL_CORPUS);
        assertEquals(3, virtualCorpora.size());
        // ordered by id
        Iterator<QueryDO> i = virtualCorpora.iterator();
        assertEquals("system-vc", i.next().getName());
        assertEquals("dory-vc", i.next().getName());
        assertEquals("group-vc", i.next().getName());
        
        deleteVC("dory-vc", "dory", "dory");
        deleteVC("group-vc", "dory", "dory");
    }

    /**
     * retrieves group VC and
     * excludes hidden published VC (user has never used it)
     *
     * @throws KustvaktException
     */
    @Test
    public void retrieveVCByUserNemo () throws KustvaktException {
    	createNemoVC();
    	
        List<QueryDO> virtualCorpora = dao.retrieveQueryByUser("nemo",
                QueryType.VIRTUAL_CORPUS);
        assertEquals(2, virtualCorpora.size());
        Iterator<QueryDO> i = virtualCorpora.iterator();
        assertEquals("system-vc",i.next().getName());
        assertEquals("nemo-vc",i.next().getName());
        
        deleteVC("nemo-vc", "nemo", "nemo");
    }

    /**
     * retrieves published VC by the owner and
     * excludes group vc when a user is a pending member
     *
     * @throws KustvaktException
     */
    @Test
    public void retrieveVCByUserMarlin () throws KustvaktException {
    	createMarlinVC();
    	createMarlinPublishedVC();
    	
        List<QueryDO> virtualCorpora = dao.retrieveQueryByUser("marlin",
                QueryType.VIRTUAL_CORPUS);
        assertEquals(3, virtualCorpora.size());
        Iterator<QueryDO> i = virtualCorpora.iterator();
        assertEquals("system-vc",i.next().getName());
        assertEquals("marlin-vc",i.next().getName());
        assertEquals("published-vc",i.next().getName());
        
        deleteVC("marlin-vc", "marlin", "marlin");
        deleteVC("published-vc", "marlin", "marlin");
    }

    /**
     * retrieves published VC from an auto-generated hidden group and
     * excludes group vc when a user is a deleted member
     *
     * @throws KustvaktException
     */
    @Test
    public void retrieveVCByUserPearl () throws KustvaktException {
        List<QueryDO> virtualCorpora = dao.retrieveQueryByUser("pearl",
                QueryType.VIRTUAL_CORPUS);
        assertEquals(1, virtualCorpora.size());
        Iterator<QueryDO> i = virtualCorpora.iterator();
        assertEquals("system-vc",i.next().getName());
    }
}
