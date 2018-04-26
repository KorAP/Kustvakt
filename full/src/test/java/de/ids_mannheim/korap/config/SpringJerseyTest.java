package de.ids_mannheim.korap.config;

import java.util.concurrent.ThreadLocalRandom;

import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.context.WebApplicationContext;

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

    @Autowired
    protected ApplicationContext applicationContext;

    private static String[] classPackages =
            new String[] { "de.ids_mannheim.korap.web.controller",
                    "de.ids_mannheim.korap.web.filter",
                    "de.ids_mannheim.korap.web.utils" };


    @Override
    protected TestContainerFactory getTestContainerFactory ()
            throws TestContainerException {
        return new GrizzlyWebTestContainerFactory();
    }

    @Override
    protected AppDescriptor configure () {

        StaticContextLoaderListener.applicationContext =
                (WebApplicationContext) applicationContext;

        return new WebAppDescriptor.Builder(classPackages)
                .servletClass(SpringServlet.class)
                .contextListenerClass(StaticContextLoaderListener.class)
                .contextParam("contextConfigLocation",
                        "classpath:test-config.xml")
                .build();
    }

    @Override
    protected int getPort (int defaultPort) {
        return ThreadLocalRandom.current().nextInt(5000, 8000 + 1);
    }

}
