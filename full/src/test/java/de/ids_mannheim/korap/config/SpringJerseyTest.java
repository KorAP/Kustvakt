package de.ids_mannheim.korap.config;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.context.support.GenericWebApplicationContext;

import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.spi.spring.container.servlet.SpringServlet;
import com.sun.jersey.test.framework.AppDescriptor;
import com.sun.jersey.test.framework.JerseyTest;
import com.sun.jersey.test.framework.WebAppDescriptor;
import com.sun.jersey.test.framework.spi.container.TestContainerException;
import com.sun.jersey.test.framework.spi.container.TestContainerFactory;
import com.sun.jersey.test.framework.spi.container.grizzly.web.GrizzlyWebTestContainerFactory;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:test-config.xml")
public abstract class SpringJerseyTest extends JerseyTest {

    public final static String API_VERSION = "v1.0";

    @Autowired
    protected GenericApplicationContext applicationContext;

    public static String[] classPackages =
            new String[] { "de.ids_mannheim.korap.web",
                    "de.ids_mannheim.korap.test",
                    "com.fasterxml.jackson.jaxrs.json"};

    @Override
    protected TestContainerFactory getTestContainerFactory ()
            throws TestContainerException {
        return new GrizzlyWebTestContainerFactory();
    }

    @Override
    public void setUp () throws Exception {

        GenericWebApplicationContext genericContext =
                new GenericWebApplicationContext();

        genericContext.setParent(this.applicationContext);
        genericContext.setClassLoader(this.applicationContext.getClassLoader());

        StaticContextLoaderListener.applicationContext = genericContext;
        super.setUp();
    }

    @Override
    protected AppDescriptor configure () {
        // Simulation of the production server 
        // Indicate to use codehaus jackson
        ClientConfig config = new DefaultClientConfig();
        config.getFeatures().put("com.sun.jersey.api.json.POJOMappingFeature", true);
        
        return new WebAppDescriptor.Builder(classPackages)
                .servletClass(SpringServlet.class)
                .contextListenerClass(StaticContextLoaderListener.class)
                .contextParam("adminToken", "secret")
                .build();
    }

    @Override
    protected int getPort (int defaultPort) {
        int port = ThreadLocalRandom.current().nextInt(5000, 8000 + 1);
        try {
            ServerSocket socket = new ServerSocket(port);
            socket.close();
        }
        catch (IOException e) {
//            e.printStackTrace();
            System.out.println("[WARNING] " + e.getMessage());
            port = getPort(port);
        }
        return port;
    }

}
