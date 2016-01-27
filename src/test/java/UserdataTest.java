import de.ids_mannheim.korap.config.BeanConfiguration;
import de.ids_mannheim.korap.user.KorAPUser;
import de.ids_mannheim.korap.user.User;
import de.ids_mannheim.korap.user.UserDetailsDao;
import de.ids_mannheim.korap.user.Userdetails2;
import org.junit.*;

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
        assert dao.deleteAll() != -1;
    }

    @Test
    public void testDataStore() {
        User user = new KorAPUser();
        user.setId(1);
        UserDetailsDao dao = new UserDetailsDao(
                BeanConfiguration.getBeans().getPersistenceClient());
        Userdetails2 d = new Userdetails2(1);
        d.addField("key_1", "value is a value");
        assert dao.store(d) != -1;
    }

    @Test
    public void testDataGet() {
        User user = new KorAPUser();
        user.setId(1);
        UserDetailsDao dao = new UserDetailsDao(
                BeanConfiguration.getBeans().getPersistenceClient());
        Userdetails2 d = new Userdetails2(1);
        d.addField("key_1", "value is a value");
        assert dao.store(d) != -1;

        d = dao.get(d.getId());
        assert d != null;
        assert "value is a value".equals(d.get("key_1"));

        d = dao.get(user);
        assert d != null;
        assert "value is a value".equals(d.get("key_1"));
    }

}
