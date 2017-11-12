package de.ids_mannheim.korap.web.service.full;

import static org.junit.Assert.*;

import org.eclipse.jetty.http.HttpHeaders;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.context.ContextLoaderListener;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.spi.spring.container.servlet.SpringServlet;
import com.sun.jersey.test.framework.AppDescriptor;
import com.sun.jersey.test.framework.JerseyTest;
import com.sun.jersey.test.framework.WebAppDescriptor;
import com.sun.jersey.test.framework.spi.container.TestContainerException;
import com.sun.jersey.test.framework.spi.container.TestContainerFactory;
import com.sun.jersey.test.framework.spi.container.grizzly.web.GrizzlyWebTestContainerFactory;

import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.security.auth.BasicHttpAuth;
import de.ids_mannheim.korap.utils.JsonUtils;

@Ignore
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:test-config.xml")
public class VirtualCorpusServiceTest extends JerseyTest {

    private static String[] classPackages =
            new String[] { "de.ids_mannheim.korap.web.service.full",
                    "de.ids_mannheim.korap.web.filter",
                    "de.ids_mannheim.korap.web.utils" };


    @Override
    protected TestContainerFactory getTestContainerFactory ()
            throws TestContainerException {
        return new GrizzlyWebTestContainerFactory();
    }

    @Override
    protected AppDescriptor configure () {
        return new WebAppDescriptor.Builder(classPackages)
                .servletClass(SpringServlet.class)
                .contextListenerClass(ContextLoaderListener.class)
                .contextParam("contextConfigLocation",
                        "classpath:test-config.xml")
                .build();
    }

    @Test
    public void testStoreVC () throws KustvaktException {
        String json =
                "{\"name\": \"new vc\",\"type\": \"PRIVATE\",\"createdBy\": "
                        + "\"test class\",\"collectionQuery\": \"corpusSigle=GOE\"}";

        ClientResponse response = resource().path("vc").path("store")
                .header(Attributes.AUTHORIZATION,
                        BasicHttpAuth.encode("kustvakt", "kustvakt2015"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .entity(json)
                .post(ClientResponse.class);
        String entity = response.getEntity(String.class);
        System.out.println(entity);
    }

    @Test
    public void testStoreVCUnauthorized () throws KustvaktException {
        String json =
                "{\"name\": \"new vc\",\"type\": \"PRIVATE\",\"createdBy\": "
                        + "\"test class\",\"collectionQuery\": \"pubDate eq 1982\"}";

        ClientResponse response = resource().path("vc").path("store")
                .entity(json).post(ClientResponse.class);
        String entity = response.getEntity(String.class);

        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.UNAUTHORIZED_OPERATION,
                node.at("/errors/0/0").asInt());
    }

    @Test
    public void testStoreVCWithWrongType () throws KustvaktException {
        String json =
                "{\"name\": \"new vc\",\"type\": \"PRIVAT\",\"createdBy\": "
                        + "\"test class\",\"collectionQuery\": \"pubDate eq 1982\"}";

        ClientResponse response = resource().path("vc").path("store")
                .entity(json).post(ClientResponse.class);
        String entity = response.getEntity(String.class);
        //        System.out.println(entity);

        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.DESERIALIZATION_FAILED,
                node.at("/errors/0/0").asInt());
        assertTrue(node.at("/errors/0/1").asText().startsWith(
                "Cannot deserialize value of type `de.ids_mannheim.korap.constant."
                        + "VirtualCorpusType` from String \"PRIVAT\": value not one of "
                        + "declared Enum instance names: [PROJECT, PRIVATE, PREDEFINED, "
                        + "PUBLISHED]"));
    }
}
