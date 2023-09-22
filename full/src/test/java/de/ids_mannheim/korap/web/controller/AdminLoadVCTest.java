package de.ids_mannheim.korap.web.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.junit.jupiter.api.Test;
import com.google.common.net.HttpHeaders;
import de.ids_mannheim.korap.cache.VirtualCorpusCache;
import de.ids_mannheim.korap.config.SpringJerseyTest;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;

@DisplayName("Admin Load VC Test")
class AdminLoadVCTest extends SpringJerseyTest {

    @Test
    @DisplayName("Test Load Cache VC")
    void testLoadCacheVC() throws KustvaktException, InterruptedException {
        assertFalse(VirtualCorpusCache.contains("named-vc1"));
        Form f = new Form();
        f.param("token", "secret");
        Response response = target().path(API_VERSION).path("admin").path("vc").path("load-cache").request().header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED).post(Entity.form(f));
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Thread.sleep(100);
        assertTrue(VirtualCorpusCache.contains("named-vc1"));
        VirtualCorpusCache.reset();
        assertFalse(VirtualCorpusCache.contains("named-vc1"));
    }
}
