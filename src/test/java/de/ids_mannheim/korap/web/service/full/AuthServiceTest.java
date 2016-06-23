package de.ids_mannheim.korap.web.service.full;

import de.ids_mannheim.korap.config.TestHelper;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.user.User;
import de.ids_mannheim.korap.web.service.FastJerseyTest;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author hanl
 * @date 24/09/2015
 */
public class AuthServiceTest extends FastJerseyTest {

    @BeforeClass
    public static void configure () throws Exception {
        FastJerseyTest.setPackages("de.ids_mannheim.korap.web.service",
                "de.ids_mannheim.korap.web.filter",
                "de.ids_mannheim.korap.web.utils");
    }


    @Override
    public void initMethod () throws KustvaktException {
        helper().setupAccount();
    }


    @Test
    public void testBasicHttp () {
        User user = helper().getUser();

    }

    @Test
    public void testBlockingFilterFail() {

    }


    @Test
    public void testBasicLogout () {

    }


    @Test
    public void testSessionTokenLogin () {

    }


    @Test
    public void testSessionTokenLogout () {

    }


    @Test
    public void testOpenIDLogin () {

    }


    @Test
    public void testOpenIDLogout () {

    }


    // -- are these even right? auth - authorization
    @Test
    public void testOAuth2Login () {

    }


    @Test
    public void testOAuth2Logout () {

    }

    //todo: test basicauth via secure connection

}
