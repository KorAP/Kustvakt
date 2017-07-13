package de.ids_mannheim.korap.web;

import com.sun.jersey.api.core.PackagesResourceConfig;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.spi.container.servlet.ServletContainer;
import com.sun.jersey.spi.spring.container.servlet.SpringServlet;

import de.ids_mannheim.korap.config.*;
import lombok.Getter;
import lombok.Setter;

import javax.servlet.ServletContextListener;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.bio.SocketConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.springframework.web.context.ContextLoaderListener;

/**
 * @author hanl
 * @date 01/06/2015
 */
public abstract class KustvaktBaseServer {
    
    protected static KustvaktConfiguration config;
    protected static String rootPackages;
    
    public KustvaktBaseServer () {
        KustvaktConfiguration.loadLogger();
    }


    public static void main (String[] args) throws Exception {
        KustvaktBaseServer server = new KustvaktBaseServer() {
            @Override
            protected void setup () {}
        };
        KustvaktArgs kargs = server.readAttributes(args);

        if (kargs.config != null)
            BeansFactory.loadFileContext(kargs.config);
        else
            BeansFactory.loadClasspathContext();

        kargs.setRootPackages(new String[] { "de.ids_mannheim.korap.web.service.light" });
        rootPackages = "de.ids_mannheim.korap.web.service.light";
        
        config = BeansFactory.getKustvaktContext().getConfiguration();
        server.startServer(kargs);
    }


    protected KustvaktArgs readAttributes (String[] args) {
        KustvaktArgs kargs = new KustvaktArgs();
        for (int i = 0; i < args.length; i++) {
            switch ((args[i])) {
                case "--config":
                    kargs.setConfig(args[i + 1]);
                    break;
                case "--port":
                    kargs.setPort(Integer.valueOf(args[i + 1]));
                    break;
                case "--help":
                    StringBuffer b = new StringBuffer();

                    b.append("Parameter description: \n")
                            .append("--config  <Path to spring configuration file> : Configuration file\n")
                            .append("--port  <Server port> : Port under which the server is accessible \n")
                            //                            .append("--props  <Path to kustvakt properties> : list of configuration properties\n")
                            .append("--help : This help menu\n");
                    System.out.println(b.toString());
                    System.out.println();
                    break;
                case "--init":
                    kargs.init = true;
                    break;
            }
        }
        return kargs;
    }


    protected abstract void setup ();


    protected void startServer(KustvaktArgs kargs){
        if (kargs.init){
            setup();
        }
        if (kargs.port == -1){
            kargs.setPort(BeansFactory.getKustvaktContext().getConfiguration().getPort());
        }
        
        Server server = new Server();
//        WebAppContext contextHandler = new WebAppContext();
//        contextHandler.setWar("src/main/webappkustvakt.war");
        ServletContextHandler contextHandler = new ServletContextHandler(
                ServletContextHandler.NO_SESSIONS);
        contextHandler.setContextPath("/");
        contextHandler.setInitParameter("contextConfigLocation", "classpath:default-config.xml");
        
        ServletContextListener listener = new ContextLoaderListener();
        contextHandler.addEventListener(listener);

        ServletHolder servletHolder = new ServletHolder(new SpringServlet());
        servletHolder.setInitParameter("com.sun.jersey.config.property.packages", 
                rootPackages);
        servletHolder.setInitOrder(1);
        contextHandler.addServlet(servletHolder, "/kustvakt/*");
        
        SocketConnector connector = new SocketConnector();
        connector.setPort(kargs.port);
        connector.setMaxIdleTime(60000);
        
        server.setHandler(contextHandler);
        server.setConnectors(new Connector[] { connector });
        try {
            server.start();
            server.join();
        }
        catch (Exception e) {
            System.out.println("Server could not be started!");
            System.out.println(e.getMessage());
            System.exit(-1);
//            e.printStackTrace();
        }
    }
    
    @Deprecated
    protected void startServerOld (KustvaktArgs kargs) {
        if (kargs.init)
            setup();

        if (kargs.port == -1)
            kargs.setPort(BeansFactory.getKustvaktContext().getConfiguration()
                    .getPort());

        System.out.println("Starting Kustvakt Service on port '" + kargs.port
                + "'");
        try {
            // from http://wiki.eclipse.org/Jetty/Tutorial/Embedding_Jetty
            Server server = new Server();
            ServletContextHandler contextHandler = new ServletContextHandler(
                    ServletContextHandler.NO_SESSIONS);
            contextHandler.setContextPath("/");
            
            SocketConnector connector = new SocketConnector();
            connector.setPort(kargs.port);
            connector.setMaxIdleTime(60000);

            // http://stackoverflow.com/questions/9670363/how-do-i-programmatically-configure-jersey-to-use-jackson-for-json-deserializa
            final ResourceConfig rc = new PackagesResourceConfig(
                    kargs.rootPackages);
            
            // from http://stackoverflow.com/questions/7421574/embedded-jetty-with-jersey-or-resteasy
            contextHandler.addServlet(new ServletHolder(new ServletContainer(rc)), "/api/*");

	        server.setHandler(contextHandler);
            server.setConnectors(new Connector[] { connector });
            server.start();
            server.join();
        }
        catch (Exception e) {
            System.out.println("Server could not be started!");
            System.out.println(e.getMessage());
            System.exit(-1);
        }

    }

    @Setter
    public static class KustvaktArgs {

        @Getter
        private String config;
        private int port;
        private SslContextFactory sslContext;
        private String[] rootPackages;
        private boolean init;


        public KustvaktArgs () {
            this.port = -1;
            this.sslContext = null;
            this.config = null;
            this.init = false;
        }

    }
}