package de.ids_mannheim.korap.dao;

import static org.junit.Assert.assertEquals;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import de.ids_mannheim.korap.constant.VirtualCorpusType;
import de.ids_mannheim.korap.entity.VirtualCorpus;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.user.User;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:test-config.xml")
public class VirtualCorpusDaoTest {

    @Autowired
    private VirtualCorpusDao dao;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testListVCByType () throws KustvaktException {
        List<VirtualCorpus> vcList =
                dao.retrieveVCByType(VirtualCorpusType.PUBLISHED);
        assertEquals(1, vcList.size());

        VirtualCorpus vc = vcList.get(0);
        assertEquals(4, vc.getId());
        assertEquals("published VC", vc.getName());
        assertEquals("marlin", vc.getCreatedBy());
    }

    @Test
    public void testPredefinedVC () throws KustvaktException {
        // insert vc
        int id = dao.createVirtualCorpus("predefined VC",
                VirtualCorpusType.PREDEFINED, User.CorpusAccess.FREE,
                "corpusSigle=GOE", "definition", "description", "experimental",
                "test class");

        // select vc
        List<VirtualCorpus> vcList =
                dao.retrieveVCByType(VirtualCorpusType.PREDEFINED);
        assertEquals(2, vcList.size());

        // delete vc
        dao.deleteVirtualCorpus(id);

        // check if vc has been deleted
        thrown.expect(KustvaktException.class);
        dao.retrieveVCById(id);
    }


    @Test
    public void retrievePredefinedVC () throws KustvaktException {
        List<VirtualCorpus> vc =
                dao.retrieveVCByType(VirtualCorpusType.PREDEFINED);
        assertEquals(1, vc.size());
    }


    /** retrieve private and group VC
     * excludes hidden published VC (user has never used it)
     * @throws KustvaktException
     */
    @Test
    public void retrieveVCByUserDory () throws KustvaktException {
        Set<VirtualCorpus> virtualCorpora = dao.retrieveVCByUser("dory");
        assertEquals(3, virtualCorpora.size());
        // order is random
        //        Iterator<VirtualCorpus> i = virtualCorpora.iterator();
        //        assertEquals("dory VC", i.next().getName());
        //        assertEquals("system VC", i.next().getName());
        //        assertEquals("group VC", i.next().getName());
    }


    /** retrieves group VC and
     *  excludes hidden published VC (user has never used it)
     * @throws KustvaktException
     */
    @Test
    public void retrieveVCByUserNemo () throws KustvaktException {
        Set<VirtualCorpus> virtualCorpora = dao.retrieveVCByUser("nemo");
        assertEquals(2, virtualCorpora.size());
        Iterator<VirtualCorpus> i = virtualCorpora.iterator();
        assertEquals("system VC", i.next().getName());
        assertEquals("group VC", i.next().getName());
    }


    /** retrieves published VC by the owner and
     *  excludes group vc when a user is a pending member
     * @throws KustvaktException
     */
    @Test
    public void retrieveVCByUserMarlin () throws KustvaktException {
        Set<VirtualCorpus> virtualCorpora = dao.retrieveVCByUser("marlin");
        assertEquals(2, virtualCorpora.size());
        Iterator<VirtualCorpus> i = virtualCorpora.iterator();
        assertEquals("system VC", i.next().getName());
        assertEquals("published VC", i.next().getName());
    }



    /** retrieves published VC from an auto-generated hidden group and 
     *  excludes group vc when a user is a deleted member 
     * @throws KustvaktException
     */
    @Test
    public void retrieveVCByUserPearl () throws KustvaktException {
        Set<VirtualCorpus> virtualCorpora = dao.retrieveVCByUser("pearl");
        assertEquals(2, virtualCorpora.size());
        Iterator<VirtualCorpus> i = virtualCorpora.iterator();
        assertEquals("system VC", i.next().getName());
        assertEquals("published VC", i.next().getName());
    }

}
