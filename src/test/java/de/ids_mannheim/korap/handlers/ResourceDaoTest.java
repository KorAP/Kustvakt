package de.ids_mannheim.korap.handlers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.BeanConfigTest;
import de.ids_mannheim.korap.config.KustvaktClassLoader;
import de.ids_mannheim.korap.config.TestHelper;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.resources.KustvaktResource;
import de.ids_mannheim.korap.resources.VirtualCollection;
import de.ids_mannheim.korap.user.User;

/**
 * @author hanl
 * @date 26/01/2016
 */
public class ResourceDaoTest extends BeanConfigTest {

    private static List<Integer> ids = new ArrayList<>();

//    @Autowired
//    private TransactionTemplate txTemplate;
//    
//    @Autowired
//    private ResourceDao<KustvaktResource> resourceDao;

    @Override
    public void initMethod () {
        helper().setupAccount();

        List<Class<? extends KustvaktResource>> classes = new ArrayList<>(
                KustvaktClassLoader.loadSubTypes(KustvaktResource.class));
        int size = classes.size();
        for (int i = 0; i < size; i++) {
            Class<? extends KustvaktResource> s = classes.get(i < classes.size() ? i : 0);
            try {
                KustvaktResource r = (KustvaktResource) s.newInstance();
                r.setName("resource_" + i);
                r.setPersistentID(r.getName());
                Map<String, Object> map = new HashMap<>();
                map.put("testVar", "testVal_" + i);
				r.setFields(map);
                int id = helper().setupResource(r);
                ids.add(id);
                assertNotEquals(0, new ResourceDao<>(helper().getContext()
                        .getPersistenceClient()).size());
            }
            catch (InstantiationException e) {
                if (i < classes.size())
                    classes.remove(i);
            }
            catch (KustvaktException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    public void testBatchGetResources () throws KustvaktException {
        ResourceDao dao = new ResourceDao(helper().getContext()
                .getPersistenceClient());
        assertNotEquals(0, dao.size());
        Collection res = dao.getResources(ids, User.UserFactory.getDemoUser());
        assertEquals(ids.size(), res.size());
    }


    @Test
    public void testGetResource () throws KustvaktException {
        ResourceDao<?> dao = new ResourceDao<>(helper().getContext()
                .getPersistenceClient());
        assertEquals("sqlite", helper().getContext().getPersistenceClient()
                .getDatabase());

        assertNotEquals(0, dao.size());
        KustvaktResource res = dao.findbyId(ids.get(0),
                User.UserFactory.getDemoUser());
        assertNotNull(res);
        Assert.assertEquals(true,res.getField("testVar").toString().startsWith("testVal_"));
    }


    @Test
    public void createCollection () throws KustvaktException {
        ResourceDao dao = new ResourceDao(helper().getContext()
                .getPersistenceClient());
        VirtualCollection c = new VirtualCollection("testColl1");
        c.addField("key_1", "this is a test");
        c.addField("key_2", 2);

        User user = User.UserFactory
                .getUser((String) TestHelper.getUserCredentials().get(Attributes.USERNAME));

        int id = dao.storeResource(c, user);

        KustvaktResource r = dao.findbyId(id, user);
        assertNotEquals(dao.size(), 0);
        assertNotNull(r);
        assertEquals(r.getField("key_1") instanceof String, true);
        assertEquals(r.getField("key_2") instanceof Integer, true);
    }


    @Test
    public void ResourceDataUpdate () throws KustvaktException {
        ResourceDao dao = new ResourceDao(helper().getContext()
                .getPersistenceClient());
        VirtualCollection c = new VirtualCollection("testColl2");
        c.addField("key_1", "this is a test");
        c.addField("key_2", 2);

        User user = User.UserFactory
                .getUser((String) TestHelper.getUserCredentials().get(Attributes.USERNAME));

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
