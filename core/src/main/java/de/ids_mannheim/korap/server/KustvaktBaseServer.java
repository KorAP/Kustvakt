package de.ids_mannheim.korap.server;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;

import javax.servlet.ServletContextListener;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ShutdownHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.springframework.web.context.ContextLoaderListener;

import com.sun.jersey.spi.spring.container.servlet.SpringServlet;

import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.encryption.RandomCodeGenerator;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import lombok.Getter;
import lombok.Setter;

/**
 * @author hanl
 * @date 01/06/2015
 */
public abstract class KustvaktBaseServer {

    protected static KustvaktConfiguration config;

    protected static String rootPackages;
    protected static KustvaktArgs kargs;

    public KustvaktBaseServer () {}

    protected KustvaktArgs readAttributes (String[] args) {
        KustvaktArgs kargs = new KustvaktArgs();
        for (int i = 0; i < args.length; i++) {
            switch ((args[i])) {
                case "--spring-config":
                    kargs.setSpringConfig(args[i + 1]);
                    break;
                case "--port":
                    kargs.setPort(Integer.valueOf(args[i + 1]));
                    break;
                case "--help":
                    StringBuffer b = new StringBuffer();

                    b.append("Parameter description: \n")
                            .append("--spring-config  <Spring XML configuration filename in classpath>\n")
                            .append("--port  <Server port number>\n")
                            .append("--help : This help menu\n");
                    System.out.println(b.toString());
                    System.out.println();
                    return (KustvaktArgs) null;
            }
        }
        return kargs;
    }

    protected void start ()
            throws KustvaktException, IOException, NoSuchAlgorithmException {

        if (kargs.port == -1) {
            kargs.setPort(config.getPort());
        }

        Server server = new Server();

        ServletContextHandler contextHandler =
                new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        contextHandler.setContextPath("/");
        contextHandler.setInitParameter("contextConfigLocation",
                "classpath:" + kargs.getSpringConfig());

        ServletContextListener listener = new ContextLoaderListener();
        contextHandler.addEventListener(listener);

        ServletHolder servletHolder = new ServletHolder(new SpringServlet());
        servletHolder.setInitParameter(
                "com.sun.jersey.config.property.packages", rootPackages);
        servletHolder.setInitParameter(
                "com.sun.jersey.api.json.POJOMappingFeature", "true");
        servletHolder.setInitOrder(1);
        contextHandler.addServlet(servletHolder, config.getBaseURL());

        ServerConnector connector = new ServerConnector(server);
        connector.setPort(kargs.port);
        connector.setIdleTimeout(60000);

        RandomCodeGenerator random = new RandomCodeGenerator();
        String shutdownToken = random.createRandomCode(config);
        ShutdownHandler shutdownHandler = new ShutdownHandler(shutdownToken,true,false);

        FileOutputStream fos = new FileOutputStream(new File("shutdownToken"));
        OutputStreamWriter writer =
                new OutputStreamWriter(fos, StandardCharsets.UTF_8.name());
        writer.append("token=");
        writer.append(shutdownToken);
        writer.flush();
        writer.close();

        HandlerList handlers = new HandlerList();
        handlers.addHandler(shutdownHandler);
        handlers.addHandler(contextHandler);

        server.setHandler(handlers);

        server.setConnectors(new Connector[] { connector });
        try {
            server.start();
            server.join();
        }
        catch (Exception e) {
            System.out.println("Server could not be started!");
            System.out.println(e.getMessage());
            e.printStackTrace();
            System.exit(-1);
        }
    }

    @Setter
    public static class KustvaktArgs {

        @Getter
        private String springConfig;
        private int port;

        public KustvaktArgs () {
            this.port = -1;
            this.springConfig = null;
        }
    }
}
