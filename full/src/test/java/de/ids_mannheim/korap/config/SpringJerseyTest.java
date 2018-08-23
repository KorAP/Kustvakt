package de.ids_mannheim.korap.config;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.runner.RunWith;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.context.support.AbstractRefreshableWebApplicationContext;

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
    protected GenericApplicationContext applicationContext;

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
    public void setUp () throws Exception {

        StaticContextLoaderListener.applicationContext =
                new AbstractRefreshableWebApplicationContext() {

                    ConfigurableListableBeanFactory existingBeanFactory =
                            applicationContext.getBeanFactory();

                    @Override
                    protected void loadBeanDefinitions (
                            DefaultListableBeanFactory beanFactory)
                            throws BeansException, IOException {

                        String[] beanDefinitionNames =
                                existingBeanFactory.getBeanDefinitionNames();
                        for (String beanName : beanDefinitionNames) {
                            beanFactory.registerBeanDefinition(beanName,
                                    existingBeanFactory
                                            .getBeanDefinition(beanName));
                        }
                    }
                };

        super.setUp();
    }

    @Override
    protected AppDescriptor configure () {
        return new WebAppDescriptor.Builder(classPackages)
                .servletClass(SpringServlet.class)
                .contextListenerClass(StaticContextLoaderListener.class)
                // .contextListenerClass(ContextLoaderListener.class)
                // .contextParam("contextConfigLocation",
                // "classpath:test-config.xml")
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
            e.printStackTrace();
            port = getPort(port);
        }
        return port;
    }

}
