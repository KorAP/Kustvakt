package de.ids_mannheim.korap.config;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.handlers.JDBCAuditing;
import de.ids_mannheim.korap.interfaces.db.AuditingIface;

/**
 * @author hanl
 * @date 27/07/2015
 */
public class ClassLoaderTest extends BeanConfigTest {

    @Test
    public void testDefaultCreationThrowsNoException () {
        DefaultHandler pl = new DefaultHandler();
        Object o = pl.getDefault(ContextHolder.KUSTVAKT_AUDITING);
        assertNotNull(o);
        assertTrue(o instanceof AuditingIface);
    }


//    @Test
//    @Deprecated
//    public void testDefaultCreation2ThrowsNoException () {
//        AuthenticationManagerIface iface = helper().getContext()
//                .getAuthenticationManager();
//        assertNotNull(iface);
//        assertTrue(iface instanceof KustvaktAuthenticationManager);
//    }


    @Test
    public void testDefaultInterfaceMatchThrowsNoException () {
        AuditingIface iface = helper().getContext().getAuditingProvider();
        assertNotNull(iface);
        assertTrue(iface instanceof JDBCAuditing);
    }


    @Override
    public void initMethod () throws KustvaktException {}
}
