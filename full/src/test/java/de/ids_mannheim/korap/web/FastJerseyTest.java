package de.ids_mannheim.korap.web;

import java.net.URI;
import java.util.concurrent.ThreadLocalRandom;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.UriBuilder;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.springframework.web.context.ContextLoaderListener;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.glassfish.jersey.test.DeploymentContext;
import org.glassfish.jersey.test.ServletDeploymentContext;
import org.glassfish.jersey.test.spi.TestContainer;
import org.glassfish.jersey.test.spi.TestContainerFactory;
import org.glassfish.jersey.test.grizzly.GrizzlyTestContainerFactory;
import org.glassfish.jersey.test.grizzly.GrizzlyWebTestContainerFactory;

import de.ids_mannheim.korap.config.BeanConfigTest;

public abstract class FastJerseyTest extends BeanConfigTest{

    private static String[] classPackages =
            new String[] { "de.ids_mannheim.korap.web.service.full",
                    "de.ids_mannheim.korap.web.filter",
                    "de.ids_mannheim.korap.web.utils" };

    public final static String API_VERSION = "v0.1";

    private static ResourceConfig resourceConfig =
            new ResourceConfig();

    private static TestContainerFactory testContainerFactory;

    protected static TestContainer testContainer;

    protected static javax.ws.rs.client.Client client;

    protected static int PORT = 8089; // FB, was: 9000;
    protected static int PORT_IT = 1;
    protected static String containerURI = "http://localhost/";

    public String getAPIVersion () {
        return API_VERSION;
    }

    public static void setTestContainerFactory (
            TestContainerFactory newTestContainerFactory) {
        testContainerFactory = newTestContainerFactory;
    }


    @BeforeClass
    public static void cleanStaticVariables () {
        resourceConfig = new ResourceConfig();
    }


    protected static void initServer (int port, String[] classPackages) {
        DeploymentContext dc;
        if (classPackages == null)
            dc = DeploymentContext.builder(resourceConfig).build();
        else
            dc = ServletDeploymentContext
                    .forServlet(new ServletContainer(resourceConfig.packages(classPackages)))
                    .addListener(ContextLoaderListener.class)
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
                UriBuilder.fromUri(containerURI).port(port).build(), dc);
        client = testContainer.getClientConfig().getClient();
        if (client == null) {
            client = ClientBuilder.newClient(testContainer.getClientConfig());
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


    public WebTarget target () {
        return client.target(getBaseUri());
    }
    
//    protected TestHelper helper () {
//        try {
//            return TestHelper.newInstance(this.context);
//        }
//        catch (Exception e) {
//            return null;
//        }
//    }
//
//
//    @Override
//    protected ContextHolder getContext () {
//        return helper().getContext();
//    }


    public static void startServer () {
        try {
            if (testContainer != null) {
                testContainer.start();
            }
        }
        catch (Exception e) {
            initServer(PORT + PORT_IT++, classPackages);
            startServer();
        }
    }


    @Before
    public void startServerBeforeFirstTestRun () {
        if (testContainer == null) {
            int port = ThreadLocalRandom.current().nextInt(5000, 8000 + 1);
            initServer(port, classPackages);
            startServer();
        }
    }

}
