package de.ids_mannheim.korap.misc;
import com.sun.jersey.api.client.ClientResponse;
import de.ids_mannheim.korap.config.BeansFactory;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.web.service.FastJerseyTest;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author hanl
 * @date 04/02/2016
 */
public class DemoUserTest extends FastJerseyTest {

    @Override
    public void initMethod () throws KustvaktException {
        helper().setupAccount();
        BeansFactory.setKustvaktContext(helper().getContext());
    }


    @BeforeClass
    public static void setup () throws Exception {
        FastJerseyTest.setPackages("de.ids_mannheim.korap.web.service.full",
                "de.ids_mannheim.korap.web.filter",
                "de.ids_mannheim.korap.web.utils");
    }


    @Test
    public void testDemoCollectionGet () {
        //        ClientResponse response = resource().path(getVersion())
        //                .path("collection").path("i");
    }


    @Test
    public void testDemoUserInfoGet () {

    }


    @Test
    public void testDemoUserDetailsGet () {

    }


    @Test
    public void testDemoUserSettingsGet () {

    }


    @Test
    public void testSearch () {
        ClientResponse response = resource().path(getAPIVersion())
                .path("search").queryParam("q", "[base=Wort]")
                .queryParam("ql", "poliqarp").get(ClientResponse.class);

        System.out.println("_____________________________");
        System.out.println(response);
        System.out.println("entity " + response.getEntity(String.class));
    }

}
