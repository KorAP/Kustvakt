package de.ids_mannheim.korap.user;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.fasterxml.jackson.databind.node.ArrayNode;

import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.utils.JsonUtils;
import de.ids_mannheim.korap.validator.ApacheValidator;
import edu.emory.mathcs.backport.java.util.Arrays;

/**
 * @author hanl, margaretha
 * @date 27/01/2016
 */
public class UserdataTest {

    // EM: added
    @Test
    public void testReadEmptyMap () throws KustvaktException {
        Userdata userData = new UserSettingProcessor();
        userData.read(new HashMap<>(), false);
        String jsonSettings = userData.serialize();
        assertEquals("{}", jsonSettings);
    }

    @Test
    public void testReadNullMap () throws KustvaktException {
        Userdata userData = new UserSettingProcessor();
        userData.read(null, false);
        String jsonSettings = userData.serialize();
        assertEquals("{}", jsonSettings);
    }

    // EM: based on MH code, supposedly to validate entries like email
    // and date. See ApacheValidator
    //
    // It has inconsistent behaviors:
    // throws exceptions when there are invalid entries in a list,
    // otherwise skips invalid entries and returns a valid map
    // Moreover, Userdata.validate(ValidatorIface) does not return a
    // valid map.
    //
    // At the moment, validation is not needed for default settings.
    @Test
    public void testValidateMap () throws IOException, KustvaktException {

        Map<String, Object> map = new HashMap<>();
        map.put("k1", Arrays.asList(new String[] { "a", "b", "c" }));
        map.put("k2", Arrays.asList(new Integer[] { 1, 2, 3 }));

        Userdata data = new UserSettingProcessor();
        data.read(map, false);
        data.validate(new ApacheValidator());
    }

    // EM: below are tests from MH
    @Test
    public void testDataValidation () {
        Userdata data = new UserDetails(1);
        data.setField(Attributes.COUNTRY, "Germany");

        String[] req = data.requiredFields();
        String[] r = data.findMissingFields();
        assertNotEquals(0, r.length);
        assertEquals(req.length, r.length);
        assertFalse(data.isValid());
    }


    @Test
    public void testSettingsValidation () {
        Userdata data = new UserSettingProcessor();
        data.setField(Attributes.FILE_FORMAT_FOR_EXPORT, "export");

        String[] req = data.requiredFields();
        String[] r = data.findMissingFields();
        assertEquals(0, r.length);
        assertEquals(req.length, r.length);
        assertTrue(data.isValid());
    }
    
    @Test
    public void testUserdataRequiredFields () throws KustvaktException {
        UserDetails details = new UserDetails(-1);
        Map<String, Object> m = new HashMap<>();
        m.put(Attributes.FIRSTNAME, "first");
        m.put(Attributes.LASTNAME, "last");
        m.put(Attributes.ADDRESS, "address");
        m.put(Attributes.EMAIL, "email");
        details.setData(JsonUtils.toJSON(m));

        details.setData(JsonUtils.toJSON(m));
        String[] missing = details.findMissingFields();
        assertEquals(0, missing.length);
    }

    @Test
    public void testUserdataDefaultFields () throws KustvaktException {
        UserSettingProcessor settings = new UserSettingProcessor();
        Map<String, Object> m = new HashMap<>();
        m.put(Attributes.DEFAULT_FOUNDRY_RELATION, "rel_1");
        m.put(Attributes.DEFAULT_FOUNDRY_CONSTITUENT, "const_1");
        m.put(Attributes.DEFAULT_FOUNDRY_POS, "pos_1");
        m.put(Attributes.DEFAULT_FOUNDRY_LEMMA, "lemma_1");
        m.put(Attributes.PAGE_LENGTH, 10);
        m.put(Attributes.QUERY_LANGUAGE, "poliqarp");
        m.put(Attributes.METADATA_QUERY_EXPERT_MODUS, false);

        settings.read(m, true);

        assertNotEquals(m.size(), settings.size());
        assertEquals(settings.defaultFields().length, settings.size());
        assertEquals("rel_1", settings.get(Attributes.DEFAULT_FOUNDRY_RELATION));
        assertEquals("pos_1", settings.get(Attributes.DEFAULT_FOUNDRY_POS));
        assertEquals("lemma_1", settings.get(Attributes.DEFAULT_FOUNDRY_LEMMA));
        assertEquals("const_1", settings.get(Attributes.DEFAULT_FOUNDRY_CONSTITUENT));
        assertEquals(10, settings.get(Attributes.PAGE_LENGTH));

    }

    @Test(expected = KustvaktException.class)
    public void testUserDataRequiredFieldsException ()
            throws KustvaktException {
        UserDetails details = new UserDetails(-1);
        Map<String, Object> m = new HashMap<>();
        m.put(Attributes.FIRSTNAME, "first");
        m.put(Attributes.LASTNAME, "last");
        m.put(Attributes.ADDRESS, "address");

        details.setData(JsonUtils.toJSON(m));
        String[] missing = details.findMissingFields();

        assertEquals(1, missing.length);
        assertEquals("email", missing[0]);
        details.checkRequired();
    }

    @Test
    public void testUserDataPointerFunction () throws KustvaktException {
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
}
