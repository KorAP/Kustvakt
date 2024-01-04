package de.ids_mannheim.korap.config;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.glassfish.jersey.test.DeploymentContext;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.ServletDeploymentContext;
import org.glassfish.jersey.test.grizzly.GrizzlyWebTestContainerFactory;
import org.glassfish.jersey.test.spi.TestContainerException;
import org.glassfish.jersey.test.spi.TestContainerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.context.support.GenericWebApplicationContext;

@ExtendWith(SpringExtension.class)
@ContextConfiguration("classpath:test-config-lite.xml")
public abstract class LiteJerseyTest extends JerseyTest {

    public static final String API_VERSION = "v1.0";

    @Autowired
    protected GenericApplicationContext applicationContext;

    public static String[] classPackages = new String[] {
            "de.ids_mannheim.korap.core.web",
            "de.ids_mannheim.korap.web.filter",
            "de.ids_mannheim.korap.web.utils", "de.ids_mannheim.korap.test",
            "com.fasterxml.jackson.jaxrs.json" };

    @Override
    protected TestContainerFactory getTestContainerFactory ()
            throws TestContainerException {
        return new GrizzlyWebTestContainerFactory();
    }

    @BeforeEach
    @Override
    public void setUp () throws Exception {
        GenericWebApplicationContext genericContext = new GenericWebApplicationContext();
        genericContext.setParent(this.applicationContext);
        genericContext.setClassLoader(this.applicationContext.getClassLoader());
        StaticContextLoaderListener.applicationContext = genericContext;
        super.setUp();
    }

    @Override
    protected DeploymentContext configureDeployment () {
        return ServletDeploymentContext
                .forServlet(new ServletContainer(
                        new ResourceConfig().packages(classPackages)))
                .addListener(StaticContextLoaderListener.class)
                .contextParam("adminToken", "secret").build();
    }
}
