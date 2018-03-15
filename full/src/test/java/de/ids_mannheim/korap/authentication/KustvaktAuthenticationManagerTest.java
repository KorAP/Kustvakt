package de.ids_mannheim.korap.authentication;

import static org.junit.Assert.assertNotNull;

import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.BeanConfigTest;
import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.interfaces.AuthenticationManagerIface;
import de.ids_mannheim.korap.user.KorAPUser;
import de.ids_mannheim.korap.user.User;
import de.ids_mannheim.korap.user.UserDetails;
import de.ids_mannheim.korap.user.UserSettings;
import de.ids_mannheim.korap.user.Userdata;

/**
 * EM: fix tests. new DB does not save users.
 * @author hanl
 * @date 04/02/2016
 */
@Ignore
public class KustvaktAuthenticationManagerTest extends BeanConfigTest {

    @Autowired
    private AuthenticationManagerIface authManager;
    
    @After
    public void after () {
        try {
            User user = authManager
                    .getUser(
                            (String) KustvaktConfiguration.KUSTVAKT_USER
                                    .get(Attributes.USERNAME));
            authManager
                    .deleteAccount(user);
        }
        catch (KustvaktException e) {}
    }

    @Test
    public void testBatchStore () {
        int i = 6;

//        AuthenticationManagerIface manager = helper().getContext()
//                .getAuthenticationManager();
        for (int ix = 0; ix < i; ix++) {}

    }


    @Test
    @Ignore
    public void testUserdetailsGet () throws KustvaktException {
        User user = authManager
                .getUser((String) KustvaktConfiguration.KUSTVAKT_USER
                        .get(Attributes.USERNAME));

        Userdata data = authManager.getUserData(user, UserDetails.class);
        assertNotNull(data);
    }


    @Test
    @Ignore
    public void testUsersettingsGet () throws KustvaktException {
        User user = authManager
                .getUser((String) KustvaktConfiguration.KUSTVAKT_USER
                        .get(Attributes.USERNAME));

        Userdata data = authManager.getUserData(user, UserSettings.class);
        assertNotNull(data);
    }


    @Test(expected = KustvaktException.class)
    public void testUserDetailsGetNonExistent () throws KustvaktException {
        User user = new KorAPUser(10, "random");
        authManager.getUserData(user, UserDetails.class);
    }


    @Test(expected = KustvaktException.class)
    public void testUserSettingsGetNonExistent () throws KustvaktException {
        User user = new KorAPUser(10, "random");
        authManager.getUserData(user, UserSettings.class);
    }


    @Override
    public void initMethod () throws KustvaktException {
        helper().setupAccount();
    }
}
