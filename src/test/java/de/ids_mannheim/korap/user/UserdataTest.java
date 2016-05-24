package de.ids_mannheim.korap.user;

import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.BeanConfigTest;
import de.ids_mannheim.korap.config.BeansFactory;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.dbException;
import de.ids_mannheim.korap.handlers.UserDetailsDao;
import de.ids_mannheim.korap.handlers.UserSettingsDao;
import de.ids_mannheim.korap.interfaces.db.UserDataDbIface;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

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
    public void testDataStore () {
        String val = "value1;value_data";
        User user = new KorAPUser();
        user.setId(1);
        UserDetailsDao dao = new UserDetailsDao(helper().getContext()
                .getPersistenceClient());
        UserDetails d = new UserDetails(1);
        d.setField("key_1", val);
        assertNotEquals(-1, dao.store(d));
    }


    @Test
    public void testDataGet () throws dbException {
        String val = "value1;value_data";
        User user = new KorAPUser();
        user.setId(1);
        UserDetailsDao dao = new UserDetailsDao(helper().getContext()
                .getPersistenceClient());
        UserDetails d = new UserDetails(1);
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
    public void testUserdatafactory () throws KustvaktException {
        UserDataDbIface dao = BeansFactory.getTypeFactory().getTypedBean(
                helper().getContext().getUserDataDaos(), UserDetails.class);
        assertNotNull(dao);
        assertEquals(UserDetailsDao.class, dao.getClass());

        dao = BeansFactory.getTypeFactory().getTypedBean(
                helper().getContext().getUserDataDaos(), UserSettings.class);
        assertNotNull(dao);
        assertEquals(UserSettingsDao.class, dao.getClass());

    }


    @Test(expected = RuntimeException.class)
    public void testUserdatafactoryError () throws KustvaktException {
        BeansFactory.getTypeFactory().getTypedBean(
                helper().getContext().getUserDataDaos(), new Userdata(1) {
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
