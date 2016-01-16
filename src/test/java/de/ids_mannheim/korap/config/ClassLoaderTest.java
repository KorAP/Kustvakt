package de.ids_mannheim.korap.config;

import de.ids_mannheim.korap.interfaces.AuthenticationManagerIface;
import de.ids_mannheim.korap.interfaces.db.AuditingIface;
import de.ids_mannheim.korap.interfaces.defaults.DefaultAuditing;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author hanl
 * @date 27/07/2015
 */
public class ClassLoaderTest {

    @After
    public void close() {
        BeanConfiguration.closeApplication();
    }    

    @Test
    public void testBeanConfigurationLoaderThrowsNoException() {
        BeanConfiguration.loadClasspathContext("default-config.xml");
        assert BeanConfiguration.hasContext();
    }

    @Test
    public void testDefaultCreationThrowsNoException() {
        DefaultHandler pl = new DefaultHandler();
        Object o = pl.getDefault(BeanConfiguration.KUSTVAKT_AUDITING);
        assert o != null;
        assert o instanceof AuditingIface;
    }

    @Test
    public void testDefaultCreationThrowsException() {
        BeanConfiguration.loadClasspathContext();
        AuthenticationManagerIface iface = BeanConfiguration.getBeans()
                .getAuthenticationManager();
        Assert.assertNull("default should be null", iface);
    }

    @Test
    public void testDefaultInterfaceMatchThrowsNoException() {
        BeanConfiguration.loadClasspathContext();
        AuditingIface iface = BeanConfiguration.getBeans()
                .getAuditingProvider();
        assert iface != null;
        assert iface instanceof DefaultAuditing;
    }

}
