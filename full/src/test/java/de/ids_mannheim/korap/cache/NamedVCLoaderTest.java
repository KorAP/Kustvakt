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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;

@DisplayName("Named VC Loader Test")
class NamedVCLoaderTest extends SpringJerseyTest {

    @Autowired
    private NamedVCLoader vcLoader;

    @Autowired
    private QueryDao dao;

    @Test
    @DisplayName("Test Named VC Loader")
    void testNamedVCLoader() throws IOException, QueryException, KustvaktException {
        String vcId = "named-vc1";
        vcLoader.loadVCToCache(vcId, "/vc/named-vc1.jsonld");
        assertTrue(VirtualCorpusCache.contains(vcId));
        Map<String, DocBits> cachedData = VirtualCorpusCache.retrieve(vcId);
        assertTrue(cachedData.size() > 0);
        VirtualCorpusCache.delete(vcId);
        assertFalse(VirtualCorpusCache.contains(vcId));
        QueryDO vc = dao.retrieveQueryByName(vcId, "system");
        assertNotNull(vc);
        dao.deleteQuery(vc);
        vc = dao.retrieveQueryByName(vcId, "system");
        assertNull(vc);
    }
}
