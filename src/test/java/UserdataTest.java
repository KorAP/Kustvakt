import de.ids_mannheim.korap.config.BeanConfiguration;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.dbException;
import de.ids_mannheim.korap.handlers.UserDetailsDao;
import de.ids_mannheim.korap.handlers.UserSettingsDao;
import de.ids_mannheim.korap.interfaces.db.UserDataDbIface;
import de.ids_mannheim.korap.user.*;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author hanl
 * @date 27/01/2016
 */
public class UserdataTest {

    @BeforeClass
    public static void init() {
        BeanConfiguration.loadClasspathContext("default-config.xml");
    }

    @AfterClass
    public static void drop() {
        BeanConfiguration.closeApplication();
    }

    @Before
    public void clear() {
        UserDetailsDao dao = new UserDetailsDao(
                BeanConfiguration.getBeans().getPersistenceClient());
        UserSettingsDao sdao = new UserSettingsDao(
                BeanConfiguration.getBeans().getPersistenceClient());
        assert dao.deleteAll() != -1;
        assert sdao.deleteAll() != -1;
    }

    @Test
    public void testDataStore() {
        User user = new KorAPUser();
        user.setId(1);
        UserDetailsDao dao = new UserDetailsDao(
                BeanConfiguration.getBeans().getPersistenceClient());
        UserDetails d = new UserDetails(1);
        d.addField("key_1", "value is a value");
        assert dao.store(d) != -1;
    }

    @Test
    public void testDataGet() throws dbException {
        User user = new KorAPUser();
        user.setId(1);
        UserDetailsDao dao = new UserDetailsDao(
                BeanConfiguration.getBeans().getPersistenceClient());
        UserDetails d = new UserDetails(1);
        d.addField("key_1", "value is a value");
        assert dao.store(d) != -1;

        d = dao.get(d.getId());
        assert d != null;
        assert "value is a value".equals(d.get("key_1"));

        d = dao.get(user);
        assert d != null;
        assert "value is a value".equals(d.get("key_1"));
    }

    @Test
    public void testDataValidation() {
        Userdata data = new UserDetails(1);
        data.addField(Attributes.COUNTRY, "Germany");

        String[] req = data.requiredFields();
        String[] r = data.missing();
        assert r.length > 0;
        assert r.length == req.length;
        assert !data.isValid();
    }

    @Test
    public void testSettingsValidation() {
        Userdata data = new UserSettings(1);
        data.addField(Attributes.FILE_FORMAT_FOR_EXPORT, "export");

        String[] req = data.requiredFields();
        String[] r = data.missing();
        assert r.length == 0;
        assert r.length == req.length;
        assert data.isValid();
    }

    @Test
    public void testUserdatafactory() throws KustvaktException {
        UserDataDbIface dao = UserdataFactory.getDaoInstance(UserDetails.class);
        assert UserDetailsDao.class.equals(dao.getClass());
    }

    @Test(expected = KustvaktException.class)
    public void testUserdatafactoryError() throws KustvaktException {
        UserdataFactory.getDaoInstance(new Userdata(1) {
            @Override
            public String[] requiredFields() {
                return new String[0];
            }

            @Override
            public String[] defaultFields() {
                return new String[0];
            }
        }.getClass());
    }

}
