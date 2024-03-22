package de.ids_mannheim.korap.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;
import java.util.Scanner;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import org.springframework.web.context.support.XmlWebApplicationContext;

import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.dao.AnnotationDao;
import de.ids_mannheim.korap.encryption.RandomCodeGenerator;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import lombok.Getter;
import lombok.Setter;

/**
 * @author hanl, margaretha
 * 
 */
public abstract class KustvaktBaseServer {

    private Logger log = LogManager.getLogger(KustvaktBaseServer.class);
    
    protected static KustvaktConfiguration config;
    protected static String springConfig = "default-config.xml";

    protected static String rootPackages;
    protected static KustvaktArgs kargs;

    public KustvaktBaseServer () {
        rootPackages = "de.ids_mannheim.korap.core.web;"
                + "de.ids_mannheim.korap.web;"
//                + "com.fasterxml.jackson.jaxrs.json;"
                ;

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

    protected void loadProperties (String path, String defaultPath) throws IOException {
        File f = new File(path);
        Properties properties = new Properties();

        InputStream in = null;
        if (!f.exists()) {
            log.info("Loading kustvakt configuration from "+defaultPath);
            in = KustvaktServer.class.getClassLoader()
                    .getResourceAsStream(defaultPath);
        }
        else {
            log.info("Loading kustvakt configuration from "+path);
            in = new FileInputStream(f);
        }

        properties.load(in);
        in.close();
        
        config = new KustvaktConfiguration();
        config.loadBasicProperties(properties);
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

        String configLocation = "classpath:" + springConfig;
        if (kargs.getSpringConfig() != null) {
            configLocation = "file:" + kargs.getSpringConfig();
        }
        XmlWebApplicationContext context = new XmlWebApplicationContext();
        context.setConfigLocation(configLocation);
        
        ServletContextHandler contextHandler = new ServletContextHandler(
                ServletContextHandler.NO_SESSIONS);
        contextHandler.setContextPath("/");
        contextHandler.addEventListener(new ContextLoaderListener(context));
        contextHandler.setInitParameter("adminToken", adminToken);

        ServletHolder servletHolder = new ServletHolder(
                new ServletContainer());
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
                false);

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
