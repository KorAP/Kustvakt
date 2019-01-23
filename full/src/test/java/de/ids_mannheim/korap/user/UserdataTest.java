package de.ids_mannheim.korap.user;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.BeanConfigTest;
import de.ids_mannheim.korap.config.BeansFactory;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.handlers.UserDetailsDao;
import de.ids_mannheim.korap.handlers.UserSettingsDao;
import de.ids_mannheim.korap.interfaces.db.UserDataDbIface;

/**
 * @author hanl
 * @date 27/01/2016
 */
@Deprecated
@Ignore
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
        String[] r = data.findMissingFields();
        assertNotEquals(0, r.length);
        assertEquals(req.length, r.length);
        assertFalse(data.isValid());
    }


    @Test
    public void testSettingsValidation () {
        Userdata data = new UserSettingProcessor(1);
        data.setField(Attributes.FILE_FORMAT_FOR_EXPORT, "export");

        String[] req = data.requiredFields();
        String[] r = data.findMissingFields();
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
        assertTrue(dao instanceof UserDetailsDao);

        dao = BeansFactory.getTypeFactory().getTypeInterfaceBean(
                helper().getContext().getUserDataProviders(),
                UserSettingProcessor.class);
        assertNotNull(dao);
        assertTrue(dao instanceof UserSettingsDao);
    }

    @Deprecated
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
