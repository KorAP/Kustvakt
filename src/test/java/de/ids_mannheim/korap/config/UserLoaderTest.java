package de.ids_mannheim.korap.config;

import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.handlers.EntityDao;
import de.ids_mannheim.korap.web.service.UserLoader;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author hanl
 * @date 11/02/2016
 */
public class UserLoaderTest {

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
        EntityDao dao = new EntityDao(
                BeanConfiguration.getBeans().getPersistenceClient());

        boolean error = false;
        UserLoader l = new UserLoader();
        try {
            l.load();
        }catch (KustvaktException e) {
            e.printStackTrace();
            error = true;
        }
        assert !error;
        Assert.assertNotEquals("Is not supposed to be zero", 0, dao.size());
    }
}
