package de.ids_mannheim.korap.cache;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import de.ids_mannheim.korap.KrillCollection;
import de.ids_mannheim.korap.collection.CachedVCData;
import de.ids_mannheim.korap.config.NamedVCLoader;
import de.ids_mannheim.korap.config.SpringJerseyTest;
import de.ids_mannheim.korap.dao.VirtualCorpusDao;
import de.ids_mannheim.korap.entity.VirtualCorpus;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.util.QueryException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

public class NamedVCLoaderTest extends SpringJerseyTest {

    @Autowired
    private NamedVCLoader vcLoader;
    @Autowired
    private VirtualCorpusDao dao;

    @Test
    public void testNamedVCLoader ()
            throws IOException, QueryException, KustvaktException {
        KrillCollection.cache = CacheManager.newInstance().getCache("named_vc");
        Element element = KrillCollection.cache.get("named-vc1");
        assertTrue(element == null);

        vcLoader.loadVCToCache("named-vc1", "/vc/named-vc1.jsonld");

        element = KrillCollection.cache.get("named-vc1");
        assertNotNull(element);
        CachedVCData cachedData = (CachedVCData) element.getObjectValue();
        assertTrue(cachedData.getDocIdMap().size() > 0);
        
        KrillCollection.cache.removeAll();
        VirtualCorpus vc = dao.retrieveVCByName("named-vc1", "system");
        dao.deleteVirtualCorpus(vc);
    }
}
