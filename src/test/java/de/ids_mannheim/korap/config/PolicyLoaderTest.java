package de.ids_mannheim.korap.config;

import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.security.ac.PolicyDao;
import de.ids_mannheim.korap.web.service.CollectionLoader;
import de.ids_mannheim.korap.web.service.PolicyLoader;
import de.ids_mannheim.korap.web.service.UserLoader;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author hanl
 * @date 11/02/2016
 */
public class PolicyLoaderTest {

    @AfterClass
    public static void close() {
        BeanConfiguration.closeApplication();
    }

    @BeforeClass
    public static void create() {
        BeanConfiguration.loadClasspathContext("default-config.xml");
    }

    @Test
    public void testUserLoader() {
        boolean error = false;
        UserLoader u = new UserLoader();
        CollectionLoader c = new CollectionLoader();
        PolicyLoader l = new PolicyLoader();
        try {
            u.load();
            c.load();
            l.load();
        }catch (KustvaktException e) {
            error = true;
        }
        assert !error;

        PolicyDao dao = new PolicyDao(
                BeanConfiguration.getBeans().getPersistenceClient());
        Assert.assertNotEquals("Is not supposed to be zero", 0, dao.size());
    }
}
