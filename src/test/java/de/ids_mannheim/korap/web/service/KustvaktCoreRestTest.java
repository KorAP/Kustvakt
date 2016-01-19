package de.ids_mannheim.korap.web.service;

import com.sun.jersey.api.client.ClientResponse;
import de.ids_mannheim.korap.config.BeanConfiguration;
import de.ids_mannheim.korap.query.serialize.CollectionQueryProcessor;
import de.ids_mannheim.korap.query.serialize.QuerySerializer;
import de.ids_mannheim.korap.utils.JsonUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author hanl
 * @date 26/06/2015
 */
//todo: check tranferable index config path for test cases
public class KustvaktCoreRestTest extends FastJerseyTest {

    @BeforeClass
    public static void configure() {
        BeanConfiguration.loadClasspathContext();
        setPackages("de.ids_mannheim.korap.web.service.light",
                "de.ids_mannheim.korap.web.filter",
                "de.ids_mannheim.korap.web.utils");
    }

    @AfterClass
    public static void close() {
        BeanConfiguration.closeApplication();
    }

    //    @Test
    public void testFieldsInSearch() {
        ClientResponse response = resource().path(getAPIVersion())
                .path("search").queryParam("q", "[base=Wort]")
                .queryParam("ql", "poliqarp").get(ClientResponse.class);
        assert ClientResponse.Status.OK.getStatusCode() == response.getStatus();
    }

    @Test
    public void testQuery() {
        ClientResponse response = resource().path(getAPIVersion())
                .path("search").queryParam("q", "[base=Wort]")
                .queryParam("ql", "poliqarp").get(ClientResponse.class);
//        System.out.println("_______________________________________________");
//        System.out.println(response.getEntity(String.class));
        assert ClientResponse.Status.OK.getStatusCode() == response.getStatus();
    }

    @Test
    public void testQueryRaw() {
        QuerySerializer s = new QuerySerializer();
        s.setQuery("[base=Wort]", "poliqarp");

        ClientResponse response = resource().path(getAPIVersion())
                .path("search").post(ClientResponse.class, s.toJSON());
//        System.out.println("_______________________________________________ RAW");
//        System.out.println(response.getEntity(String.class));
        assert ClientResponse.Status.OK.getStatusCode() == response.getStatus();
    }

    // in case no index is there, this will throw an error
    @Ignore
    @Test
    public void testGetMatchInfoThrowsNoException() {
        ClientResponse response = resource().path(getAPIVersion())
                .get(ClientResponse.class);
    }

    //    @Test
    public void testGetStatsThrowsNoException() {
        CollectionQueryProcessor pr = new CollectionQueryProcessor();
        pr.process("corpusID=WPD & textClass=Sport");
        Map map = new LinkedHashMap();
        map.put("collection", pr.getRequestMap());
        ClientResponse response = resource().path(getAPIVersion()).path("stats")
                .post(ClientResponse.class, JsonUtils.toJSON(map));
        assert ClientResponse.Status.OK.getStatusCode() == response.getStatus();
    }

    @Test
    public void testGetStats2ThrowsNoException() {
        ClientResponse response = resource().path(getAPIVersion()).path("stats")
                .post(ClientResponse.class, "creationDate in 1787");
        String ent = response.getEntity(String.class);
        assert ClientResponse.Status.OK.getStatusCode() == response.getStatus();
//        System.out
//                .println("___________________________________________________");
//        System.out.println("STATS ENTITY " + ent);
    }

    //    @Test
    public void testBuildQueryThrowsNoException() {
        ClientResponse response = resource().path(getAPIVersion())
                .path("search").queryParam("q", "[base=Haus & surface=Hauses]")
                .queryParam("ql", "poliqarp").queryParam("cutOff", "true")
                .queryParam("page", "1").method("TRACE", ClientResponse.class);
        assert ClientResponse.Status.OK.getStatusCode() == response.getStatus();
    }

    //    @Test
    public void testQueryByNameThrowsNoException() {
        ClientResponse response = resource().path(getAPIVersion())
                .path("corpus").path("WPD").path("search")
                .queryParam("q", "[base=Haus & surface=Hauses]")
                .queryParam("ql", "poliqarp").queryParam("cutOff", "true")
                .queryParam("page", "1").get(ClientResponse.class);
        System.out.println("RESPONSE " + response.getEntity(String.class));
    }

}
