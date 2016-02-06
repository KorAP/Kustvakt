import de.ids_mannheim.korap.config.BeanConfiguration;
import de.ids_mannheim.korap.config.TestHelper;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.interfaces.db.EntityHandlerIface;
import de.ids_mannheim.korap.interfaces.db.UserDataDbIface;
import de.ids_mannheim.korap.user.*;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

;

/**
 * @author hanl
 * @date 13/02/2015
 */
public class UserDaoTest {

    @BeforeClass
    public static void create() {
        BeanConfiguration.loadClasspathContext("default-config.xml");
        TestHelper.setupAccount();
        TestHelper.runBootInterfaces();
    }

    @AfterClass
    public static void close() {
        assert TestHelper.dropUser();
        BeanConfiguration.closeApplication();
    }

    @Test
    public void userput() throws KustvaktException {
        User user1 = BeanConfiguration.getBeans().getUserDBHandler()
                .getAccount(TestHelper.getUserCredentials()[0]);
        Assert.assertEquals("user creation failed", true, user1.getId() != -1);
    }

    @Test
    public void userget() throws KustvaktException {
        User user1 = BeanConfiguration.getBeans().getUserDBHandler()
                .getAccount(TestHelper.getUserCredentials()[0]);
        Assert.assertEquals("User Retrieval does not work",
                TestHelper.getUserCredentials()[0], user1.getUsername());
    }

    //    @Test
    public void testUserdetailsGetNonExistent() throws KustvaktException {
        TestHelper.setupSimpleAccount("userdbtest", "userdbTest");
        User user = BeanConfiguration.getBeans().getUserDBHandler()
                .getAccount("userdbtest");
        UserDataDbIface dao = UserdataFactory.getDaoInstance(UserDetails.class);
        Userdata data = dao.get(user);
        assert data == null;
        TestHelper.dropUser("userdbtest");
    }

    //    @Test
    public void testUserSettingsGetNonExistent() throws KustvaktException {
        TestHelper.setupSimpleAccount("userdbtest", "userdbTest");
        User user = BeanConfiguration.getBeans().getUserDBHandler()
                .getAccount("userdbtest");
        UserDataDbIface dao = UserdataFactory
                .getDaoInstance(UserSettings.class);
        Userdata data = dao.get(user);
        assert data == null;
        TestHelper.dropUser("userdbtest");

    }

    // username cannot currently be changed
    //    @Test
    public void updateUsername() throws KustvaktException {
        User user1 = BeanConfiguration.getBeans().getUserDBHandler()
                .getAccount(TestHelper.getUserCredentials()[0]);
        user1.setUsername("new_user");
        BeanConfiguration.getBeans().getUserDBHandler().updateAccount(user1);
        User u2 = BeanConfiguration.getBeans().getUserDBHandler()
                .getAccount("new_user");
        Assert.assertEquals("not found", user1.getUsername(), u2.getUsername());
    }

    @Test
    public void userupdate() throws KustvaktException {
        EntityHandlerIface dao = BeanConfiguration.getBeans()
                .getUserDBHandler();
        User user1 = dao.getAccount(TestHelper.getUserCredentials()[0]);
        user1.setAccountLocked(true);
        dao.updateAccount(user1);
        Assert.assertEquals("not valid", true,
                dao.getAccount(user1.getUsername()).isAccountLocked());
    }

}
