package de.ids_mannheim.korap.security.auth;

import de.ids_mannheim.korap.config.BeanConfiguration;
import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.config.TestHelper;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.interfaces.AuthenticationManagerIface;
import de.ids_mannheim.korap.interfaces.db.EntityHandlerIface;
import de.ids_mannheim.korap.user.*;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author hanl
 * @date 04/02/2016
 */
public class KustvaktAuthenticationManagerTest {

    @BeforeClass
    public static void create() {
        BeanConfiguration.loadClasspathContext("default-config.xml");
        assert TestHelper.setupAccount();
    }

    @AfterClass
    public static void close() {
        assert TestHelper.dropUser();
        BeanConfiguration.closeApplication();
    }

    @After
    public void after() throws KustvaktException {
        User user = BeanConfiguration.getBeans().getAuthenticationManager()
                .getUser((String) KustvaktConfiguration.KUSTVAKT_USER
                        .get(Attributes.USERNAME));
        BeanConfiguration.getBeans().getAuthenticationManager()
                .deleteAccount(user);
    }

    @Test
    public void testCreateUser() throws KustvaktException {
        User user = BeanConfiguration.getBeans().getAuthenticationManager()
                .createUserAccount(KustvaktConfiguration.KUSTVAKT_USER, false);

        EntityHandlerIface dao = BeanConfiguration.getBeans()
                .getUserDBHandler();

        assert dao.size() > 0;
        User check = dao.getAccount(user.getUsername());
        assert check != null;
    }

    @Test
    public void testUserdetailsGet() throws KustvaktException {
        testCreateUser();
        AuthenticationManagerIface manager = BeanConfiguration.getBeans()
                .getAuthenticationManager();

        User user = manager.getUser((String) KustvaktConfiguration.KUSTVAKT_USER
                .get(Attributes.USERNAME));

        Userdata data = manager.getUserData(user, UserDetails.class);

        assert data != null;
    }

    @Test
    public void testUsersettingsGet() throws KustvaktException {
        testCreateUser();
        AuthenticationManagerIface manager = BeanConfiguration.getBeans()
                .getAuthenticationManager();

        User user = manager.getUser((String) KustvaktConfiguration.KUSTVAKT_USER
                .get(Attributes.USERNAME));

        Userdata data = manager.getUserData(user, UserSettings.class);
        assert data != null;
    }

    @Test(expected = KustvaktException.class)
    public void testUserDetailsGetNonExistent() throws KustvaktException {
        testCreateUser();
        AuthenticationManagerIface manager = BeanConfiguration.getBeans()
                .getAuthenticationManager();

        User user = new KorAPUser(10, "random");

        Userdata data = manager.getUserData(user, UserDetails.class);
        assert data != null;
    }

    @Test(expected = KustvaktException.class)
    public void testUserSettingsGetNonExistent() throws KustvaktException {
        testCreateUser();
        AuthenticationManagerIface manager = BeanConfiguration.getBeans()
                .getAuthenticationManager();

        User user = new KorAPUser(10, "random");

        Userdata data = manager.getUserData(user, UserSettings.class);
        assert data != null;
    }

}
