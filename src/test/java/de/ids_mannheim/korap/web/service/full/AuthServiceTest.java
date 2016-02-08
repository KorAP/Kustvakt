package de.ids_mannheim.korap.web.service.full;

import de.ids_mannheim.korap.config.BeanConfiguration;
import de.ids_mannheim.korap.config.TestHelper;
import de.ids_mannheim.korap.web.service.FastJerseyTest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author hanl
 * @date 24/09/2015
 */
public class AuthServiceTest extends FastJerseyTest {

    //todo: test basicauth via secure connection

    @BeforeClass
    public static void setup() {
        BeanConfiguration.loadClasspathContext("default-config.xml");
        FastJerseyTest.setPackages("de.ids_mannheim.korap.web.service.full",
                "de.ids_mannheim.korap.web.filter",
                "de.ids_mannheim.korap.web.utils");
        TestHelper.setupAccount();
    }

    @AfterClass
    public static void close() {
        TestHelper.dropUser();
        BeanConfiguration.closeApplication();
    }

    @Test
    public void testKustvaktAuth() {

    }

    @Test
    public void testDemoAuth() {

    }

    @Test
    public void testUnauthorizedAuth() {

    }



}
