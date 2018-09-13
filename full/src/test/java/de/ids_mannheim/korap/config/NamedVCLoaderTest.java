package de.ids_mannheim.korap.config;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import de.ids_mannheim.korap.KrillCollection;
import de.ids_mannheim.korap.collection.CachedVCData;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.util.QueryException;
import net.sf.ehcache.Element;

public class NamedVCLoaderTest extends SpringJerseyTest{

    @Autowired
    private NamedVCLoader vcLoader;
    
    
    @Test
    public void testNamedVCLoader () throws IOException, QueryException, KustvaktException {
        Element element = KrillCollection.cache.get("named-vc1");
        assertTrue(element==null);
        
        vcLoader.loadVCToCache();

        element = KrillCollection.cache.get("named-vc1");
        assertNotNull(element);
        CachedVCData cachedData = (CachedVCData) element.getObjectValue();
        assertTrue(cachedData.getDocIdMap().size() > 0);
    }
}
