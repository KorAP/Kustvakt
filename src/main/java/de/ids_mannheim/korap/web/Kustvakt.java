package de.ids_mannheim.korap.web;

import com.sun.grizzly.http.embed.GrizzlyWebServer;
import com.sun.grizzly.http.servlet.ServletAdapter;
import com.sun.jersey.spi.container.servlet.ServletContainer;
import de.ids_mannheim.korap.config.BeanConfiguration;
import de.ids_mannheim.korap.utils.KorAPLogger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * @author hanl
 * @date 01/06/2015
 */
public class Kustvakt {

    private static Integer PORT = -1;
    private static String CONFIG = null;

    public static void main(String[] args) throws Exception {
        attributes(args);
        BeanConfiguration.loadClasspathContext();

        if (CONFIG != null) {
            BeanConfiguration.getConfiguration()
                    .setProperties(new FileInputStream(new File(CONFIG)));

        }
        grizzlyServer(PORT);
    }

    public static void grizzlyServer(int port) throws IOException {
        if (port == -1)
            port = BeanConfiguration.getConfiguration().getPort();
        System.out.println("Starting grizzly on port " + port + " ...");
        GrizzlyWebServer gws = new GrizzlyWebServer(port);
        ServletAdapter jerseyAdapter = new ServletAdapter();
        jerseyAdapter
                .addInitParameter("com.sun.jersey.config.property.packages",
                        "de.ids_mannheim.korap.web.service");
        jerseyAdapter.setContextPath("/api");
        jerseyAdapter.setServletInstance(new ServletContainer());

        gws.addGrizzlyAdapter(jerseyAdapter, new String[] { "/api" });
        gws.start();
    }

    private static void attributes(String[] args) {
        for (int i = 0; i < args.length; i++) {
            switch ((args[i])) {
                case "--debug":
                    KorAPLogger.DEBUG = true;
                    break;
                case "--config":
                    CONFIG = args[i + 1];
                    break;
                case "--port":
                    PORT = Integer.valueOf(args[i + 1]);
                    break;
            }
        }
    }

}
