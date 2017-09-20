package de.ids_mannheim.korap.web.service.full;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.sun.jersey.api.client.ClientResponse;

import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.query.serialize.QuerySerializer;
import de.ids_mannheim.korap.web.service.FastJerseyTest;

/**
 * @author hanl
 * @date 26/06/2015
 */
public class KustvaktCoreRestTest extends FastJerseyTest {

	@Override
    public void initMethod () throws KustvaktException {
        helper().setupAccount();
//        helper().runBootInterfaces();
    }
	
    @BeforeClass
    public static void configure () {
        
//    	FastJerseyTest.setPackages("de.ids_mannheim.korap.web.service.light", // version hanl
        FastJerseyTest.setPackages("de.ids_mannheim.korap.web.service.full", // volle Version FB
                "de.ids_mannheim.korap.web.filter",
                "de.ids_mannheim.korap.web.utils");
    }


    //    @Test
    public void testFieldsInSearch () {
        ClientResponse response = resource()
                .path("search").queryParam("q", "[base=Wort]")
                .queryParam("ql", "poliqarp").get(ClientResponse.class);
        assert ClientResponse.Status.OK.getStatusCode() == response.getStatus();
    }


    @Test
    public void testQuery () {
        ClientResponse response = resource()
                .path("search").queryParam("q", "[base=Wort]")
                .queryParam("ql", "poliqarp").get(ClientResponse.class);
        //        System.out.println("_______________________________________________");
                System.out.println(response.getEntity(String.class));
        assert ClientResponse.Status.OK.getStatusCode() == response.getStatus();
    }


    @Test
    public void testQueryRaw () {
        QuerySerializer s = new QuerySerializer();
        s.setQuery("[base=Wort]", "poliqarp");

        ClientResponse response = resource()
                .path("search").post(ClientResponse.class, s.toJSON());
        //        System.out.println("_______________________________________________ RAW");
        //        System.out.println(response.getEntity(String.class));
        assert ClientResponse.Status.OK.getStatusCode() == response.getStatus();
    }


    // in case no index is there, this will throw an error
    @Test
    @Ignore
    public void testGetMatchInfoThrowsNoException () {
        ClientResponse response = resource().path(getAPIVersion()).get(
                ClientResponse.class);
    }
   
    //    @Test
    public void testBuildQueryThrowsNoException () {
        ClientResponse response = resource()
                .path("search").queryParam("q", "[base=Haus & surface=Hauses]")
                .queryParam("ql", "poliqarp").queryParam("cutOff", "true")
                .queryParam("page", "1").method("TRACE", ClientResponse.class);
        assert ClientResponse.Status.OK.getStatusCode() == response.getStatus();
    }


    //    @Test
    public void testQueryByNameThrowsNoException () {
        ClientResponse response = resource()
                .path("corpus").path("WPD").path("search")
                .queryParam("q", "[base=Haus & surface=Hauses]")
                .queryParam("ql", "poliqarp").queryParam("cutOff", "true")
                .queryParam("page", "1").get(ClientResponse.class);
        System.out.println("RESPONSE " + response.getEntity(String.class));
    }
    
}
