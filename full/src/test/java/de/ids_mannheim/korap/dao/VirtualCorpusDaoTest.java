package de.ids_mannheim.korap.dao;

import static org.junit.Assert.assertEquals;

import java.util.Iterator;
import java.util.List;

import javax.persistence.PersistenceException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.beans.factory.annotation.Autowired;

import de.ids_mannheim.korap.config.SpringJerseyTest;
import de.ids_mannheim.korap.constant.VirtualCorpusType;
import de.ids_mannheim.korap.entity.VirtualCorpus;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.user.User;

public class VirtualCorpusDaoTest extends SpringJerseyTest{

    @Autowired
    private VirtualCorpusDao dao;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testListVCByType () throws KustvaktException {
        List<VirtualCorpus> vcList =
                dao.retrieveVCByType(VirtualCorpusType.PUBLISHED, null);
        assertEquals(1, vcList.size());

        VirtualCorpus vc = vcList.get(0);
        assertEquals(4, vc.getId());
        assertEquals("published-vc", vc.getName());
        assertEquals("marlin", vc.getCreatedBy());
    }

    @Test
    public void testSystemVC () throws KustvaktException {
        // insert vc
        int id = dao.createVirtualCorpus("system-vc", VirtualCorpusType.SYSTEM,
                User.CorpusAccess.FREE, "corpusSigle=GOE", "definition",
                "description", "experimental", false, "test class");

        // select vc
        List<VirtualCorpus> vcList =
                dao.retrieveVCByType(VirtualCorpusType.SYSTEM, null);
        assertEquals(2, vcList.size());

        VirtualCorpus vc = dao.retrieveVCById(id);
        // delete vc
        dao.deleteVirtualCorpus(vc);

        // check if vc has been deleted
        thrown.expect(KustvaktException.class);
        dao.retrieveVCById(id);
    }

    @Test
    public void testNonUniqueVC () throws KustvaktException {
        thrown.expect(PersistenceException.class);
        thrown.expectMessage("could not execute statement");
        
        dao.createVirtualCorpus("system-vc", VirtualCorpusType.SYSTEM,
                User.CorpusAccess.FREE, "corpusSigle=GOE", "definition",
                "description", "experimental", false, "system");
    }

    @Test
    public void retrieveSystemVC () throws KustvaktException {
        List<VirtualCorpus> vc =
                dao.retrieveVCByType(VirtualCorpusType.SYSTEM, null);
        assertEquals(1, vc.size());
    }

    /**
     * retrieve private and group VC
     * 
     * @throws KustvaktException
     */
    @Test
    public void retrieveVCByUserDory () throws KustvaktException {
        List<VirtualCorpus> virtualCorpora = dao.retrieveVCByUser("dory");
        // System.out.println(virtualCorpora);
        assertEquals(4, virtualCorpora.size());
        // ordered by id
        Iterator<VirtualCorpus> i = virtualCorpora.iterator();
        assertEquals("dory-vc", i.next().getName());
        assertEquals("group-vc", i.next().getName());
        assertEquals("system-vc", i.next().getName());
        assertEquals("published-vc", i.next().getName());
    }

    /**
     * retrieves group VC and
     * excludes hidden published VC (user has never used it)
     * 
     * @throws KustvaktException
     */
    @Test
    public void retrieveVCByUserNemo () throws KustvaktException {
        List<VirtualCorpus> virtualCorpora = dao.retrieveVCByUser("nemo");
        assertEquals(3, virtualCorpora.size());
        Iterator<VirtualCorpus> i = virtualCorpora.iterator();
        assertEquals("group-vc", i.next().getName());
        assertEquals("system-vc", i.next().getName());
        assertEquals("nemo-vc", i.next().getName());
    }

    /**
     * retrieves published VC by the owner and
     * excludes group vc when a user is a pending member
     * 
     * @throws KustvaktException
     */
    @Test
    public void retrieveVCByUserMarlin () throws KustvaktException {
        List<VirtualCorpus> virtualCorpora = dao.retrieveVCByUser("marlin");
        assertEquals(3, virtualCorpora.size());
        Iterator<VirtualCorpus> i = virtualCorpora.iterator();
        assertEquals("system-vc", i.next().getName());
        assertEquals("published-vc", i.next().getName());
        assertEquals("marlin-vc", i.next().getName());
    }

    /**
     * retrieves published VC from an auto-generated hidden group and
     * excludes group vc when a user is a deleted member
     * 
     * @throws KustvaktException
     */
    @Test
    public void retrieveVCByUserPearl () throws KustvaktException {
        List<VirtualCorpus> virtualCorpora = dao.retrieveVCByUser("pearl");
        assertEquals(2, virtualCorpora.size());
        Iterator<VirtualCorpus> i = virtualCorpora.iterator();
        assertEquals("system-vc", i.next().getName());
        assertEquals("published-vc", i.next().getName());
    }

}
