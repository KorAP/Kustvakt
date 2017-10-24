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
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import de.ids_mannheim.korap.constants.VirtualCorpusType;
import de.ids_mannheim.korap.dao.VirtualCorpusDao;
import de.ids_mannheim.korap.entity.VirtualCorpus;
import de.ids_mannheim.korap.exceptions.KustvaktException;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:test-config.xml")
public class VirtualCorpusDaoTest {

    @Autowired
    VirtualCorpusDao dao;
    @Autowired
    protected ApplicationContext context;

    @Rule
    public ExpectedException thrown = ExpectedException.none();


    @Test
    public void testPredefinedVC () throws KustvaktException {
        // insert vc
        VirtualCorpus vc = new VirtualCorpus();
        vc.setName("predefined VC");
        vc.setCreatedBy("test class");
        vc.setCollectionQuery("corpusSigle=GOE");
        vc.setRequiredAccess("free");
        vc.setType(VirtualCorpusType.PREDEFINED);
        dao.storeVirtualCorpus(vc);

        // select vc
        List<VirtualCorpus> vcList =
                dao.retrieveVCByType(VirtualCorpusType.PREDEFINED);
        assertEquals(2, vcList.size());

        // delete vc
        dao.deleteVirtualCorpus(vc.getId());

        // check if vc has been deleted
        thrown.expect(KustvaktException.class);
        vc = dao.retrieveVCById(vc.getId());
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
    public void retrieveVirtualCorpusByUserDory () throws KustvaktException {
        Set<VirtualCorpus> virtualCorpora = dao.retrieveVCByUser("dory");
        assertEquals(3, virtualCorpora.size());
        Iterator<VirtualCorpus> i = virtualCorpora.iterator();
        assertEquals("dory VC", i.next().getName());
        assertEquals("system VC", i.next().getName());
        assertEquals("group VC", i.next().getName());
    }


    /** retrieves group VC and
     *  excludes hidden published VC (user has never used it)
     * @throws KustvaktException
     */
    @Test
    public void retrieveVirtualCorpusByUserNemo () throws KustvaktException {
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
    public void retrieveVirtualCorpusByUserMarlin () throws KustvaktException {
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
    public void retrieveVirtualCorpusByUserPearl () throws KustvaktException {
        Set<VirtualCorpus> virtualCorpora = dao.retrieveVCByUser("pearl");
        assertEquals(2, virtualCorpora.size());
        Iterator<VirtualCorpus> i = virtualCorpora.iterator();
        assertEquals("system VC", i.next().getName());
        assertEquals("published VC", i.next().getName());
    }

}
