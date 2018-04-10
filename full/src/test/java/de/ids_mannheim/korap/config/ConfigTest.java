package de.ids_mannheim.korap.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

import org.junit.Ignore;
import org.junit.Test;

import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.interfaces.EncryptionIface;
import de.ids_mannheim.korap.utils.ServiceInfo;
import de.ids_mannheim.korap.utils.TimeUtils;

/** EM: To do: fix tests
 * @author hanl
 * @date 02/09/2015
 */
@Ignore
public class ConfigTest extends BeanConfigTest {


    @Test
    public void testConfigLoader () {
        InputStream stream = ConfigLoader.loadConfigStream("kustvakt.conf");
        assertNotNull(stream);
    }


    @Test
    public void testPropertyLoader () throws IOException {
        Properties p = ConfigLoader.loadProperties("kustvakt.conf");
        assertNotNull(p);
    }

    @Test
    public void testServiceInfo () {
        String version = ServiceInfo.getInfo().getVersion();
        String name = ServiceInfo.getInfo().getName();
        assertNotEquals("wrong version", "UNKNOWN", version);
        assertNotEquals("wrong name", "UNKNOWN", name);
    }


    @Test
    public void testProperties () {
        assertEquals("token layer does not match", "opennlp", helper()
                .getContext().getConfiguration().getDefault_token());
        assertEquals("token expiration does not match",
                TimeUtils.convertTimeToSeconds("1D"), helper().getContext()
                        .getConfiguration().getLongTokenTTL());
    }


    @Test(expected = KustvaktException.class)
    @Ignore
    public void testBeanOverrideInjection () throws KustvaktException {
        helper().getContext()
                .getConfiguration()
                .setPropertiesAsStream(
                        ConfigTest.class.getClassLoader().getResourceAsStream(
                                "kustvakt.conf"));
    }

    @Test
    public void testBootConfigDependencyOrder () {
        // todo:

    }

    @Override
    public void initMethod () throws KustvaktException {

    }
}
