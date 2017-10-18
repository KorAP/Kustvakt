package de.ids_mannheim.de.korap.dao;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import de.ids_mannheim.korap.config.BeanConfigTest;
import de.ids_mannheim.korap.dao.VirtualCorpusDao;
import de.ids_mannheim.korap.entity.VirtualCorpus;
import de.ids_mannheim.korap.exceptions.KustvaktException;

public class VirtualCorpusDaoTest extends BeanConfigTest{

    @Autowired
    VirtualCorpusDao dao;


    @Test
    public void retrieveVirtualCorpusByUserDory () {
        List<VirtualCorpus> virtualCorpora =
                dao.retrieveVirtualCorpusByUser("dory");
        assertEquals(3,virtualCorpora.size());
    }


    @Test
    public void retrieveVirtualCorpusByUserNemo () {
        List<VirtualCorpus> virtualCorpora =
                dao.retrieveVirtualCorpusByUser("nemo");

    }


    @Test
    public void retrieveVirtualCorpusByUserMarlin () {
        List<VirtualCorpus> virtualCorpora =
                dao.retrieveVirtualCorpusByUser("marlin");

    }


    @Test
    public void retrieveVirtualCorpusByUserPearl () {
        List<VirtualCorpus> virtualCorpora =
                dao.retrieveVirtualCorpusByUser("pearl");
    }


    @Override
    public void initMethod () throws KustvaktException {
        // TODO Auto-generated method stub
        
    }

}
