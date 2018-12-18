package de.ids_mannheim.korap.misc;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import de.ids_mannheim.korap.config.ContextHolder;
import de.ids_mannheim.korap.config.DefaultHandler;
import de.ids_mannheim.korap.config.SpringJerseyTest;
import de.ids_mannheim.korap.handlers.JDBCAuditing;
import de.ids_mannheim.korap.interfaces.db.AuditingIface;

/**
 * @author hanl
 * @date 27/07/2015
 */
public class ClassLoaderTest extends SpringJerseyTest {

    @Autowired
    AuditingIface audit;
    
    @Test
    public void testDefaultCreationThrowsNoException () {
        DefaultHandler pl = new DefaultHandler();
        Object o = pl.getDefault(ContextHolder.KUSTVAKT_AUDITING);
        assertNotNull(o);
        assertTrue(o instanceof AuditingIface);
    }

    @Test
    public void testDefaultInterfaceMatchThrowsNoException () {
        assertNotNull(audit);
        assertTrue(audit instanceof JDBCAuditing);
    }
}
