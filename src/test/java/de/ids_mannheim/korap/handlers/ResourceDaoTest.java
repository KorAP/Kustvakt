package de.ids_mannheim.korap.handlers;

import de.ids_mannheim.korap.config.BeanConfiguration;
import de.ids_mannheim.korap.config.TestHelper;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.resources.KustvaktResource;
import de.ids_mannheim.korap.resources.VirtualCollection;
import de.ids_mannheim.korap.user.User;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author hanl
 * @date 26/01/2016
 */
public class ResourceDaoTest {

    @BeforeClass
    public static void init() {
        BeanConfiguration.loadClasspathContext("default-config.xml");
        TestHelper.setupAccount();
    }

    @AfterClass
    public static void drop() {
        //        TestHelper.dropUser();
        BeanConfiguration.closeApplication();
    }

    @After
    public void clear() throws KustvaktException {
        new ResourceDao<>(BeanConfiguration.getBeans().getPersistenceClient())
                .deleteAll();
    }

    @Test
    public void createCollection() throws KustvaktException {
        ResourceDao dao = new ResourceDao(
                BeanConfiguration.getBeans().getPersistenceClient());
        VirtualCollection c = new VirtualCollection("testColl");
        c.addField("key_1", "this is a test");
        c.addField("key_2", 2);

        User user = User.UserFactory
                .getUser(TestHelper.getUserCredentials()[0]);

        int id = dao.storeResource(c, user);

        KustvaktResource r = dao.findbyId(id, user);
        assert dao.size() > 0;
        assert r != null;
        assert r.getField("key_1") instanceof String;
        assert r.getField("key_2") instanceof Integer;
    }

    @Test
    public void ResourceDataUpdate() throws KustvaktException {
        ResourceDao dao = new ResourceDao(
                BeanConfiguration.getBeans().getPersistenceClient());
        VirtualCollection c = new VirtualCollection("testColl");
        c.addField("key_1", "this is a test");
        c.addField("key_2", 2);

        User user = User.UserFactory
                .getUser(TestHelper.getUserCredentials()[0]);

        int id = dao.storeResource(c, user);

        c.setId(id);
        c.addField("key_3", -1);
        int row_update = dao.updateResource(c, user);
        assert row_update > 0;

        KustvaktResource r = dao.findbyId(id, user);
        assert dao.size() > 0;
        assert r != null;
        assert r.getField("key_1") instanceof String;
        assert r.getField("key_2") instanceof Integer;
        assert r.getField("key_3") instanceof Integer;

    }
}
