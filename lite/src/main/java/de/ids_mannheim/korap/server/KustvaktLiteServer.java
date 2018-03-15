package de.ids_mannheim.korap.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.web.KustvaktBaseServer;

public class KustvaktLiteServer extends KustvaktBaseServer {


    public static void main (String[] args) throws Exception {
        KustvaktLiteServer server = new KustvaktLiteServer();
        kargs = server.readAttributes(args);

        if (kargs == null) System.exit(0);

        File f = new File("kustvakt-lite.conf");
        Properties properties = new Properties();
        InputStream in = null;
        
        if (!f.exists()){
            in = KustvaktLiteServer.class.getClassLoader().getResourceAsStream("kustvakt-lite.conf");
        }
        else{
            in = new FileInputStream(f);
        }
        
        properties.load(in);
        in.close();
        config = new KustvaktConfiguration(properties);
        
        kargs.setSpringConfig("lite-config.xml");
        kargs.setRootPackages(
                new String[] { "de.ids_mannheim.korap.web.service.lite" });
        rootPackages = "de.ids_mannheim.korap.web.service.lite";

        server.start();
    }

}
