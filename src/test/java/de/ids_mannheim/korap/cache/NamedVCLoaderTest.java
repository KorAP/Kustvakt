package de.ids_mannheim.korap.cache;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import de.ids_mannheim.korap.collection.DocBits;
import de.ids_mannheim.korap.config.NamedVCLoader;
import de.ids_mannheim.korap.config.SpringJerseyTest;
import de.ids_mannheim.korap.dao.QueryDao;
import de.ids_mannheim.korap.entity.QueryDO;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.util.QueryException;

public class NamedVCLoaderTest extends SpringJerseyTest {

    @Autowired
    private NamedVCLoader vcLoader;

    @Autowired
    private QueryDao dao;

    @Test
    public void testNamedVCLoader ()
            throws IOException, QueryException, KustvaktException {
        String vcId = "named-vc1";
        vcLoader.loadVCToCache(vcId, "/vc/named-vc1.jsonld");
        assertTrue(VirtualCorpusCache.contains(vcId));
        Map<String, DocBits> cachedData = VirtualCorpusCache.retrieve(vcId);
        assertTrue(cachedData.size() > 0);
        //VirtualCorpusCache.delete(vcId);
        //assertFalse(VirtualCorpusCache.contains(vcId));
        QueryDO vc = dao.retrieveQueryByName(vcId, "system");
        assertNotNull(vc);
        
        String koralQuery = vc.getKoralQuery();
        testUpdateVC(vcId,koralQuery);
    }
    
    private void testUpdateVC (String vcId, String koralQuery)
            throws IOException, QueryException, KustvaktException {
        String json = """
            {"collection": {
                "@type": "koral:doc",
                "key": "textSigle",
                "match": "match:eq",
                "type" : "type:string",
                "value": [
                    "GOE/AGF/00000"
                ]
            }}""";
       
        vcLoader.loadVCToCache(vcId, "", json);
        
        Map<String, DocBits> cachedData = VirtualCorpusCache.retrieve(vcId);
        assertTrue(cachedData.size() > 0);
        
        QueryDO vc = dao.retrieveQueryByName(vcId, "system");
        String updatedKoralQuery = vc.getKoralQuery();
        
        assertTrue (koralQuery.hashCode() != updatedKoralQuery.hashCode());
        
        VirtualCorpusCache.delete(vcId);
        assertFalse(VirtualCorpusCache.contains(vcId));
        dao.deleteQuery(vc);
        vc = dao.retrieveQueryByName(vcId, "system");
        assertNull(vc);
    }
}
