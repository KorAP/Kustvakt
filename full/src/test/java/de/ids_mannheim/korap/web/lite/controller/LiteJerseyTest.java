package de.ids_mannheim.korap.web.lite.controller;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.web.context.ContextLoaderListener;

import com.sun.jersey.spi.spring.container.servlet.SpringServlet;
import com.sun.jersey.test.framework.AppDescriptor;
import com.sun.jersey.test.framework.JerseyTest;
import com.sun.jersey.test.framework.WebAppDescriptor;
import com.sun.jersey.test.framework.spi.container.TestContainerException;
import com.sun.jersey.test.framework.spi.container.TestContainerFactory;
import com.sun.jersey.test.framework.spi.container.grizzly.web.GrizzlyWebTestContainerFactory;

public class LiteJerseyTest  extends JerseyTest{

    public static final String API_VERSION = "v1.0";
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
        return new WebAppDescriptor.Builder(classPackages)
                .servletClass(SpringServlet.class)
                .contextListenerClass(ContextLoaderListener.class)
                .contextParam("contextConfigLocation",
                        "classpath:lite-config.xml")
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
