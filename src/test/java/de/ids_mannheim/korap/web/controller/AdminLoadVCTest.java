package de.ids_mannheim.korap.web.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.net.HttpHeaders;

import de.ids_mannheim.korap.cache.VirtualCorpusCache;
import de.ids_mannheim.korap.config.SpringJerseyTest;
import de.ids_mannheim.korap.dao.QueryDao;
import de.ids_mannheim.korap.entity.QueryDO;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

public class AdminLoadVCTest extends SpringJerseyTest {
    
    @Autowired
    private QueryDao dao;


    @Test
    public void testLoadCacheVC ()
            throws KustvaktException, InterruptedException {
        assertFalse(VirtualCorpusCache.contains("named-vc1"));
        Form f = new Form();
        f.param("token", "secret");
        Response response = target().path(API_VERSION).path("admin").path("vc")
                .path("load-cache").request()
                .header(HttpHeaders.CONTENT_TYPE,
                        MediaType.APPLICATION_FORM_URLENCODED)
                .post(Entity.form(f));
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Thread.sleep(200);
        
        assertTrue(VirtualCorpusCache.contains("named-vc1"));
        VirtualCorpusCache.reset();
        assertFalse(VirtualCorpusCache.contains("named-vc1"));

        QueryDO vc = dao.retrieveQueryByName("named-vc1", "system");
        dao.deleteQuery(vc);
        vc = dao.retrieveQueryByName("named-vc1", "system");
        assertNull(vc);
    }
}
