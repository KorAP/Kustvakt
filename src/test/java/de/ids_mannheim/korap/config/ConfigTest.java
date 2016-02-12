package de.ids_mannheim.korap.config;

import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.user.Attributes;
import de.ids_mannheim.korap.utils.ServiceVersion;
import de.ids_mannheim.korap.utils.TimeUtils;
import de.ids_mannheim.korap.web.Arg;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Set;

/**
 * @author hanl
 * @date 02/09/2015
 */
public class ConfigTest {

    @After
    public void close() {
        BeanConfiguration.closeApplication();
    }

    @Before
    public void create() {
        BeanConfiguration.loadClasspathContext("default-config.xml");
    }

    @Test
    public void testServiceVersion() {
        String v = ServiceVersion.getAPIVersion();
        Assert.assertNotEquals("wrong version", "UNKNOWN", v);
    }

    @Test
    public void testProperties() {
        BeanConfiguration.loadClasspathContext();

        Assert.assertEquals("token layer does not match", "opennlp",
                BeanConfiguration.getBeans().getConfiguration()
                        .getDefault_token());
        Assert.assertEquals("token expiration does not match",
                TimeUtils.convertTimeToSeconds("1D"),
                BeanConfiguration.getBeans().getConfiguration()
                        .getLongTokenTTL());
    }

    @Test(expected = KustvaktException.class)
    public void testBeanOverrideInjection() throws KustvaktException {
        BeanConfiguration.loadClasspathContext("default-config.xml");

        BeanConfiguration.getBeans().getConfiguration().setPropertiesAsStream(
                ConfigTest.class.getClassLoader()
                        .getResourceAsStream("kustvakt.conf"));

        String v = "testmail_&234@ids-mannheim.de";
        BeanConfiguration.getBeans().getEncryption()
                .validateEntry(v, Attributes.EMAIL);
    }

    @Test
    public void testArgLoader() {
        String[] args = new String[] { "--port", "8080", "--config",
                "local.conf", "--init" };
        Set<Arg> s = Arg.loadArgs(args);
        assert s.size() == 3;

        for (Arg arg : s) {
            if (arg instanceof Arg.PortArg)
                assert ((Arg.PortArg) arg).getValue() == 8080;
            if (arg instanceof Arg.ConfigArg)
                assert ((Arg.ConfigArg) arg).getValue().equals("local.conf");
            if (arg instanceof Arg.InitArg)
                assert ((Arg.InitArg) arg).getValue();
        }
    }

}


