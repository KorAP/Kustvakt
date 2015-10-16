package de.ids_mannheim.korap.config;

import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.interfaces.db.EntityHandlerIface;
import de.ids_mannheim.korap.user.KorAPUser;
import de.ids_mannheim.korap.user.User;
import org.junit.Assert;

import java.util.Arrays;

/**
 * creates a test user that can be used to access protected functions
 *
 * @author hanl
 * @date 16/10/2015
 */
public class TestHelper {

    private static final String[] credentials = new String[] { "test1",
            "testPass#2015" };

    public static void setup() {
        if (BeanConfiguration.hasContext()) {
            EntityHandlerIface dao = BeanConfiguration.getBeans()
                    .getUserDBHandler();

            KorAPUser user = User.UserFactory
                    .getUser(credentials[0], credentials[1]);
            try {
                Assert.assertNotNull("userdatabase handler must not be null",
                        dao);
                dao.createAccount(user);
            }catch (KustvaktException e) {
                e.printStackTrace();
            }
        }
    }

    public static final String[] getCredentials() {
        return Arrays.copyOf(credentials, 2);
    }

    private TestHelper() {
    }

}
