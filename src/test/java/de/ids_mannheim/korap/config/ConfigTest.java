package de.ids_mannheim.korap.config;

import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.handlers.ResourceDao;
import de.ids_mannheim.korap.resources.VirtualCollection;
import de.ids_mannheim.korap.security.ac.ResourceFinder;
import de.ids_mannheim.korap.user.Attributes;
import de.ids_mannheim.korap.user.User;
import de.ids_mannheim.korap.utils.ServiceVersion;
import de.ids_mannheim.korap.utils.TimeUtils;
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
    public void testCollectionLoader() throws KustvaktException {
        TestHelper.runBootInterfaces();
        ResourceDao dao = new ResourceDao(
                BeanConfiguration.getBeans().getPersistenceClient());
        int size = dao.size();
        Assert.assertNotEquals("Is not supposed to be zero", 0, size);

        Set<VirtualCollection> set = ResourceFinder.search(User.UserFactory
                        .toUser(KustvaktConfiguration.KUSTVAKT_USER),
                VirtualCollection.class);
        System.out.println("RESULTING SET: " + set);
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
}


