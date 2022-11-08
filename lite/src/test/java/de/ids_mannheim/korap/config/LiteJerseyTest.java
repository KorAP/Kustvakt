package de.ids_mannheim.korap.config;

import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.context.support.GenericWebApplicationContext;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.glassfish.jersey.test.DeploymentContext;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.ServletDeploymentContext;
import org.glassfish.jersey.test.grizzly.GrizzlyWebTestContainerFactory;
import org.glassfish.jersey.test.spi.TestContainerException;
import org.glassfish.jersey.test.spi.TestContainerFactory;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:test-config.xml")
public abstract class LiteJerseyTest extends JerseyTest{
    
    public static final String API_VERSION = "v1.0";
    
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
    protected DeploymentContext configureDeployment() {
        return ServletDeploymentContext
                .forServlet(new ServletContainer(new ResourceConfig().packages(classPackages)))
                .addListener(StaticContextLoaderListener.class)
                .contextParam("adminToken", "secret")
                .build();
    }
}
