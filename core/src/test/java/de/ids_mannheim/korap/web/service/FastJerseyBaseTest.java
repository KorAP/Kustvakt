package de.ids_mannheim.korap.web.service;

import java.net.URI;

import javax.ws.rs.core.UriBuilder;

import org.junit.After;
import org.junit.BeforeClass;
import org.springframework.web.context.ContextLoaderListener;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.core.DefaultResourceConfig;
import com.sun.jersey.spi.spring.container.servlet.SpringServlet;
import com.sun.jersey.test.framework.AppDescriptor;
import com.sun.jersey.test.framework.LowLevelAppDescriptor;
import com.sun.jersey.test.framework.WebAppDescriptor;
import com.sun.jersey.test.framework.spi.container.TestContainer;
import com.sun.jersey.test.framework.spi.container.TestContainerFactory;
import com.sun.jersey.test.framework.spi.container.grizzly.GrizzlyTestContainerFactory;
import com.sun.jersey.test.framework.spi.container.grizzly.web.GrizzlyWebTestContainerFactory;

import de.ids_mannheim.korap.config.BeanConfigBaseTest;

/**
 * @author hanl
 * @date 29/07/2015
 */
public abstract class FastJerseyBaseTest extends BeanConfigBaseTest {

    private final static String API_VERSION = "v0.1";

    private static DefaultResourceConfig resourceConfig =
            new DefaultResourceConfig();

    private static TestContainerFactory testContainerFactory;

    protected static TestContainer testContainer;

    protected static Client client;

    protected static int PORT = 8089; // FB, was: 9000;
    protected static int PORT_IT = 1;
    protected static String containerURI = "http://localhost/";


//    public static void addClass (Class<?> resourceClass) {
//        resourceConfig.getClasses().add(resourceClass);
//    }
//
//    public static void addSingleton (Object resourceSingleton) {
//        resourceConfig.getSingletons().add(resourceSingleton);
//    }


    public String getAPIVersion () {
        return API_VERSION;
    }


//    public static <T> void addProviderForContext (Class<T> contextClass,
//            T contextObject) {
//        addSingleton(new SingletonTypeInjectableProvider<Context, T>(
//                contextClass, contextObject) {});
//    }


//    public static void addRequestFilter (Object filter) {
//        resourceConfig.getContainerRequestFilters().add(filter);
//    }
//
//
//    public static void addResponseFilter (Object filter) {
//        resourceConfig.getContainerResponseFilters().add(filter);
//    }


    public static void setTestContainerFactory (
            TestContainerFactory newTestContainerFactory) {
        testContainerFactory = newTestContainerFactory;
    }


    @BeforeClass
    public static void cleanStaticVariables () {
        resourceConfig = new DefaultResourceConfig();
    }


    protected static void initServer (int port, String[] classPackages) {
        AppDescriptor ad;
        if (classPackages == null)
            ad = new LowLevelAppDescriptor.Builder(resourceConfig).build();
        else
            ad = new WebAppDescriptor.Builder(classPackages)
                    .servletClass(SpringServlet.class)
                    .contextListenerClass(ContextLoaderListener.class)
                    .contextParam("contextConfigLocation", "classpath:test-config.xml")
                    .build();

        TestContainerFactory tcf = testContainerFactory;
        if (tcf == null) {
            if (classPackages == null)
                tcf = new GrizzlyTestContainerFactory();
            else
                tcf = new GrizzlyWebTestContainerFactory();
        }

        testContainer = tcf.create(
                UriBuilder.fromUri(containerURI).port(port).build(), ad);
        client = testContainer.getClient();
        if (client == null) {
            client = Client.create(ad.getClientConfig());
        }
    }

    @After
    public void stopServer () {
        
        testContainer.stop();
        testContainer = null;
        client = null;
    }


    public Client client () {
        return client;
    }


    public URI getBaseUri () {
        return testContainer.getBaseUri();
    }


    public WebResource resource () {
        return client.resource(getBaseUri());
    }


    
}
