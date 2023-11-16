package de.ids_mannheim.korap.server;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ShutdownHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.servlet.ServletContainer;
import org.springframework.web.context.ContextLoaderListener;

import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.encryption.RandomCodeGenerator;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import jakarta.servlet.ServletContextListener;
import lombok.Getter;
import lombok.Setter;

/**
 * @author hanl
 * @date 01/06/2015
 */
public abstract class KustvaktBaseServer {

    protected static KustvaktConfiguration config;
    protected static String springConfig = "default-config.xml";

    protected static String rootPackages;
    protected static KustvaktArgs kargs;

    public KustvaktBaseServer () {
        rootPackages = "de.ids_mannheim.korap.core.web; "
                + "de.ids_mannheim.korap.web; "
                + "com.fasterxml.jackson.jaxrs.json;";

        File d = new File(KustvaktConfiguration.DATA_FOLDER);
        if (!d.exists()) {
            d.mkdir();
        }
    }

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

                    b.append("Parameter description: \n").append(
                            "--spring-config  <Spring XML configuration>\n")
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

        String adminToken = "";
        File f = new File("adminToken");
        if (!f.exists()) {
            RandomCodeGenerator random = new RandomCodeGenerator();
            adminToken = random.createRandomCode(config);
            FileOutputStream fos = new FileOutputStream(new File("adminToken"));
            OutputStreamWriter writer = new OutputStreamWriter(fos,
                    StandardCharsets.UTF_8.name());
            writer.append("token=");
            writer.append(adminToken);
            writer.flush();
            writer.close();
        }
        else {
            Scanner scanner = new Scanner(f);
            adminToken = scanner.nextLine().substring(6);
            scanner.close();
        }

        Server server = new Server();

        ServletContextHandler contextHandler = new ServletContextHandler(
                ServletContextHandler.NO_SESSIONS);
        contextHandler.setContextPath("/");

        if (kargs.getSpringConfig() != null) {
            contextHandler.setInitParameter("contextConfigLocation",
                    "file:" + kargs.getSpringConfig());
        }
        else {
            contextHandler.setInitParameter("contextConfigLocation",
                    "classpath:" + this.springConfig);
        }

        ServletContextListener listener = new ContextLoaderListener();
        contextHandler.addEventListener(listener);
        contextHandler.setInitParameter("adminToken", adminToken);

        ServletHolder servletHolder = new ServletHolder(new ServletContainer());
        servletHolder.setInitParameter(ServerProperties.PROVIDER_PACKAGES,
                rootPackages);
        servletHolder.setInitOrder(1);
        contextHandler.addServlet(servletHolder, config.getBaseURL());

        ServerConnector connector = new ServerConnector(server);
        connector.setPort(kargs.port);
        connector.setIdleTimeout(60000);
        connector.getConnectionFactory(HttpConnectionFactory.class)
                .getHttpConfiguration().setRequestHeaderSize(64000);

        ShutdownHandler shutdownHandler = new ShutdownHandler(adminToken, true,
                true);

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
