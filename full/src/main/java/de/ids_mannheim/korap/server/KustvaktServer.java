package de.ids_mannheim.korap.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

import de.ids_mannheim.korap.config.FullConfiguration;
import de.ids_mannheim.korap.web.KustvaktBaseServer;

/**
 * pu
 * 
 * @author hanl
 * @date 28/01/2014
 */
public class KustvaktServer extends KustvaktBaseServer {

    private static FullConfiguration fullConfig;
    
    public static final String API_VERSION = "v0.1";
    
    public static void main (String[] args) throws Exception {
        KustvaktServer server = new KustvaktServer();
        kargs = server.readAttributes(args);
        
        File f = new File("kustvakt.conf");
        Properties properties = new Properties();
        
        InputStream in = null;
        if (!f.exists()){
            in = KustvaktServer.class.getClassLoader().getResourceAsStream("kustvakt.conf");
        }
        else{
            in = new FileInputStream(f);
        }
        
        properties.load(in);
        in.close();
        fullConfig = new FullConfiguration(properties);
        config = fullConfig;

		if (kargs == null)
			System.exit(0);

        if (kargs.getSpringConfig() == null){
            kargs.setSpringConfig("default-config.xml");
        }
        kargs.setRootPackages(new String[] { "de.ids_mannheim.korap.web.utils",
                "de.ids_mannheim.korap.web.service.full" });
        rootPackages = "de.ids_mannheim.korap.web.utils;"
                + "de.ids_mannheim.korap.web.service.full";
        server.start();
    }
}
