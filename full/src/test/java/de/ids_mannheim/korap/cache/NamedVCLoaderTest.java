package de.ids_mannheim.korap.cache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Map;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.net.HttpHeaders;

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
        
        VirtualCorpusCache.delete(vcId);
        assertFalse(VirtualCorpusCache.contains(vcId));
        
        QueryDO vc = dao.retrieveQueryByName(vcId, "system");
        assertNotNull(vc);
        dao.deleteQuery(vc);
        vc = dao.retrieveQueryByName(vcId, "system");
        assertNull(vc);
    }
    
    @Test
    public void testLoadCacheVC () throws KustvaktException, InterruptedException {
        assertFalse(VirtualCorpusCache.contains("named-vc1"));
        Form f = new Form();
        f.param("token", "secret");
        
        Response response = target().path(API_VERSION).path("admin").path("vc")
                .path("load-cache").request()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED)
                .post(Entity.form(f));

        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        
        Thread.sleep(100);
        assertTrue(VirtualCorpusCache.contains("named-vc1"));
        
        VirtualCorpusCache.reset();
        assertFalse(VirtualCorpusCache.contains("named-vc1"));
    }
}
