package de.ids_mannheim.korap.user;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.BeanConfigTest;
import de.ids_mannheim.korap.config.BeansFactory;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.handlers.UserDetailsDao;
import de.ids_mannheim.korap.handlers.UserSettingsDao;
import de.ids_mannheim.korap.interfaces.db.UserDataDbIface;
import de.ids_mannheim.korap.utils.JsonUtils;

/**
 * @author hanl
 * @date 27/01/2016
 */
public class UserdataTest extends BeanConfigTest {

    @Before
    public void clear () {
        UserDetailsDao dao = new UserDetailsDao(helper().getContext()
                .getPersistenceClient());
        UserSettingsDao sdao = new UserSettingsDao(helper().getContext()
                .getPersistenceClient());
        assertNotEquals(-1, dao.deleteAll());
        assertNotEquals(-1, sdao.deleteAll());
    }


    @Test
    public void testDataStore () throws KustvaktException {
        String val = "value1;value_data";
        User user = new KorAPUser();
        user.setId(1);
        UserDetailsDao dao = new UserDetailsDao(helper().getContext()
                .getPersistenceClient());
        UserDetails d = new UserDetails(1);
        d.setField(Attributes.FIRSTNAME, "first");
        d.setField(Attributes.LASTNAME, "last");
        d.setField(Attributes.ADDRESS, "address");
        d.setField(Attributes.EMAIL, "email");
        d.setField("key_1", val);
        assertNotEquals(-1, dao.store(d));
    }


    @Test
    public void testDataGet () throws KustvaktException {
        String val = "value1;value_data";
        User user = new KorAPUser();
        user.setId(1);
        
        UserDetailsDao dao = new UserDetailsDao(helper().getContext()
                .getPersistenceClient());
        UserDetails d = new UserDetails(1);
        d.setField(Attributes.FIRSTNAME, "first");
        d.setField(Attributes.LASTNAME, "last");
        d.setField(Attributes.ADDRESS, "address");
        d.setField(Attributes.EMAIL, "email");
        d.setField("key_1", val);

        assertNotEquals(-1, dao.store(d));

        d = dao.get(d.getId());
        assertNotNull(d);
        assertEquals(val, d.get("key_1"));

        d = dao.get(user);
        assertNotNull(d);
        assertEquals(val, d.get("key_1"));
    }


    @Test
    public void testDataValidation () {
        Userdata data = new UserDetails(1);
        data.setField(Attributes.COUNTRY, "Germany");

        String[] req = data.requiredFields();
        String[] r = data.missing();
        assertNotEquals(0, r.length);
        assertEquals(req.length, r.length);
        assertFalse(data.isValid());
    }


    @Test
    public void testSettingsValidation () {
        Userdata data = new UserSettings(1);
        data.setField(Attributes.FILE_FORMAT_FOR_EXPORT, "export");

        String[] req = data.requiredFields();
        String[] r = data.missing();
        assertEquals(0, r.length);
        assertEquals(req.length, r.length);
        assertTrue(data.isValid());
    }


    @Test
    public void testUserdataDaoTypefactory () throws KustvaktException {
        UserDataDbIface dao = BeansFactory.getTypeFactory()
                .getTypeInterfaceBean(
                        helper().getContext().getUserDataProviders(),
                        UserDetails.class);
        assertNotNull(dao);
        assertEquals(UserDetailsDao.class, dao.getClass());

        dao = BeansFactory.getTypeFactory().getTypeInterfaceBean(
                helper().getContext().getUserDataProviders(),
                UserSettings.class);
        assertNotNull(dao);
        assertEquals(UserSettingsDao.class, dao.getClass());
    }


    @Test
    public void testDataFactoryAdd () {
        String data = "{}";
        Object node = JsonUtils.readTree(data);

        DataFactory factory = DataFactory.getFactory();
        assertTrue(factory.addValue(node, "field_1", "value_1"));
        assertTrue(factory.addValue(node, "field_2", 20));
        assertTrue(factory.addValue(node, "field_3", true));


        data = "[]";
        node = JsonUtils.readTree(data);

        factory = DataFactory.getFactory();
        assertTrue(factory.addValue(node, "field_1", "value_1"));
        assertTrue(factory.addValue(node, "field_2", 20));
        assertTrue(factory.addValue(node, "field_3", true));

    }


    @Test
    public void testDataFactoryGet () {
        String data = "{}";
        Object node = JsonUtils.readTree(data);

        DataFactory factory = DataFactory.getFactory();
        assertTrue(factory.addValue(node, "field_1", "value_1"));
        assertTrue(factory.addValue(node, "field_2", 20));
        assertTrue(factory.addValue(node, "field_3", true));

        Object value = factory.getValue(node, "field_1");
        assertEquals("value_1", value);
        value = factory.getValue(node, "field_2");
        assertEquals(20, value);
        value = factory.getValue(node, "field_3");
        assertEquals(true, value);

        data = "[]";
        node = JsonUtils.readTree(data);

        assertTrue(factory.addValue(node, "", "value_2"));
        assertTrue(factory.addValue(node, "", 10));
        assertTrue(factory.addValue(node, "", false));

        value = factory.getValue(node, "/0");
        assertEquals("value_2", value);
        value = factory.getValue(node, "/1");
        assertEquals(10, value);
        value = factory.getValue(node, "/2");
        assertEquals(false, value);
    }


    @Test
    public void testUserDataUpdate () {
        UserDetails details = new UserDetails(-1);
        details.setField(Attributes.FIRSTNAME, "first");
        details.setField(Attributes.LASTNAME, "last");
        details.setField(Attributes.ADDRESS, "address");
        details.setField(Attributes.EMAIL, "email");

        UserDetails details2 = new UserDetails(-1);
        details2.setField(Attributes.COUNTRY, "Germany");
        details.update(details2);

        assertEquals("first", details.get(Attributes.FIRSTNAME));
        assertEquals("Germany", details.get(Attributes.COUNTRY));
    }


    @Test
    public void testDataFactoryEmbeddedProperty () {
        String data = "{}";
        JsonNode node = JsonUtils.readTree(data);

        DataFactory factory = DataFactory.getFactory();
        assertTrue(factory.addValue(node, "field_1", "value_1"));
        assertTrue(factory.addValue(node, "field_2", 20));
        assertTrue(factory.addValue(node, "field_3", true));

        ArrayNode array = JsonUtils.createArrayNode();
        array.add(10);
        array.add("v1");
        array.add("v2");
        factory.addValue(node, "field_3", array);

        assertNotNull(node);
        assertEquals(10, node.at("/field_3/0").asInt());
        assertEquals("v1", node.at("/field_3/1").asText());
        assertEquals("v2", node.at("/field_3/2").asText());

    }


    @Test
    public void testUserDataPointerFunction () {
        UserDetails details = new UserDetails(-1);
        Map<String, Object> m = new HashMap<>();
        m.put(Attributes.FIRSTNAME, "first");
        m.put(Attributes.LASTNAME, "last");
        m.put(Attributes.ADDRESS, "address");
        m.put(Attributes.EMAIL, "email");
        details.setData(JsonUtils.toJSON(m));

        ArrayNode array = JsonUtils.createArrayNode();
        array.add(100);
        array.add("message");
        details.setField("errors", array);

        assertEquals(100, details.get("/errors/0"));
        assertEquals("message", details.get("/errors/1"));
    }


    @Test
    public void testDataFactoryMerge () {
        String data = "{}";
        Object node = JsonUtils.readTree(data);

        DataFactory factory = DataFactory.getFactory();
        assertTrue(factory.addValue(node, "field_1", "value_1"));
        assertTrue(factory.addValue(node, "field_2", 20));
        assertTrue(factory.addValue(node, "field_3", true));


        data = "{}";
        Object node2 = JsonUtils.readTree(data);
        assertTrue(factory.addValue(node2, "field_1", "value_new"));
        assertTrue(factory.addValue(node2, "field_2", "value_next"));
        assertTrue(factory.addValue(node2, "field_4", "value_2"));
        assertTrue(factory.addValue(node2, "field_7", "value_3"));

        JsonNode node_new = (JsonNode) factory.merge(node, node2);

        assertEquals("value_new", node_new.path("field_1").asText());
        assertEquals("value_next", node_new.path("field_2").asText());
        assertEquals(true, node_new.path("field_3").asBoolean());
        assertEquals("value_2", node_new.path("field_4").asText());
        assertEquals("value_3", node_new.path("field_7").asText());

    }


    @Test
    @Ignore
    public void testDataFactoryRemove () {
        String data = "{}";
        Object node = JsonUtils.readTree(data);

        DataFactory factory = DataFactory.getFactory();
        assertTrue(factory.addValue(node, "field_1", "value_1"));
        assertTrue(factory.addValue(node, "field_2", 20));
        assertTrue(factory.addValue(node, "field_3", true));

        Object value = factory.getValue(node, "field_1");
        assertEquals("value_1", value);
        value = factory.getValue(node, "field_2");
        assertEquals(20, value);
        value = factory.getValue(node, "field_3");
        assertEquals(true, value);

        assertTrue(factory.removeValue(node, "field_1"));
        assertTrue(factory.removeValue(node, "field_2"));
        assertTrue(factory.removeValue(node, "field_3"));
        assertNotNull(node);
        assertEquals("{}", node.toString());

        data = "[]";
        node = JsonUtils.readTree(data);

        assertTrue(factory.addValue(node, "", "value_2"));
        assertTrue(factory.addValue(node, "", 10));
        assertTrue(factory.addValue(node, "", false));

        value = factory.getValue(node, "/0");
        assertEquals("value_2", value);
        value = factory.getValue(node, "/1");
        assertEquals(10, value);
        value = factory.getValue(node, "/2");
        assertEquals(false, value);


        // fixme: cannot be removed
        assertTrue(factory.removeValue(node, "0"));
        assertTrue(factory.removeValue(node, "1"));
        assertTrue(factory.removeValue(node, "2"));
        assertNotNull(node);
        assertEquals("[]", node.toString());
    }


    @Test
    public void testUserdataRequiredFields () {
        UserDetails details = new UserDetails(-1);
        Map<String, Object> m = new HashMap<>();
        m.put(Attributes.FIRSTNAME, "first");
        m.put(Attributes.LASTNAME, "last");
        m.put(Attributes.ADDRESS, "address");
        m.put(Attributes.EMAIL, "email");
        details.setData(JsonUtils.toJSON(m));

        details.setData(JsonUtils.toJSON(m));
        String[] missing = details.missing();
        assertEquals(0, missing.length);
    }


    @Test
    public void testUserdataDefaultFields () throws KustvaktException {
        UserSettings settings = new UserSettings(-1);
        Map<String, Object> m = new HashMap<>();
        m.put(Attributes.DEFAULT_REL_FOUNDRY, "rel_1");
        m.put(Attributes.DEFAULT_CONST_FOUNDRY, "const_1");
        m.put(Attributes.DEFAULT_POS_FOUNDRY, "pos_1");
        m.put(Attributes.DEFAULT_LEMMA_FOUNDRY, "lemma_1");
        m.put(Attributes.PAGE_LENGTH, 10);
        m.put(Attributes.QUERY_LANGUAGE, "poliqarp");
        m.put(Attributes.METADATA_QUERY_EXPERT_MODUS, false);

        settings.read(m, true);

        assertNotEquals(m.size(), settings.size());
        assertEquals(settings.defaultFields().length, settings.size());
        assertEquals("rel_1", settings.get(Attributes.DEFAULT_REL_FOUNDRY));
        assertEquals("pos_1", settings.get(Attributes.DEFAULT_POS_FOUNDRY));
        assertEquals("lemma_1", settings.get(Attributes.DEFAULT_LEMMA_FOUNDRY));
        assertEquals("const_1", settings.get(Attributes.DEFAULT_CONST_FOUNDRY));
        assertEquals(10, settings.get(Attributes.PAGE_LENGTH));

    }


    @Test(expected = KustvaktException.class)
    public void testUserDataRequiredFieldsException () throws KustvaktException {
        UserDetails details = new UserDetails(-1);
        Map<String, Object> m = new HashMap<>();
        m.put(Attributes.FIRSTNAME, "first");
        m.put(Attributes.LASTNAME, "last");
        m.put(Attributes.ADDRESS, "address");

        details.setData(JsonUtils.toJSON(m));
        String[] missing = details.missing();

        assertEquals(1, missing.length);
        assertEquals("email", missing[0]);
        details.checkRequired();
    }


    @Test
    public void testDataFactoryKeys () {
        String data = "{}";
        Object node = JsonUtils.readTree(data);

        DataFactory factory = DataFactory.getFactory();
        assertTrue(factory.addValue(node, "field_1", "value_1"));
        assertTrue(factory.addValue(node, "field_2", 20));
        assertTrue(factory.addValue(node, "field_3", true));
        assertEquals(3, factory.size(node));
        assertEquals(3, factory.keys(node).size());
    }


    @Test(expected = RuntimeException.class)
    public void testUserdatafactoryError () throws KustvaktException {
        BeansFactory.getTypeFactory().getTypeInterfaceBean(
                helper().getContext().getUserDataProviders(), new Userdata(1) {
                    @Override
                    public String[] requiredFields () {
                        return new String[0];
                    }


                    @Override
                    public String[] defaultFields () {
                        return new String[0];
                    }
                }.getClass());
    }


    @Override
    public void initMethod () throws KustvaktException {}
}
