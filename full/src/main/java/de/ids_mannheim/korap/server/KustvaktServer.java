package de.ids_mannheim.korap.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

import de.ids_mannheim.korap.config.FullConfiguration;

/**
 * pu
 * 
 * @author hanl
 * @date 28/01/2014
 */
public class KustvaktServer extends KustvaktBaseServer {

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
        
        config = new FullConfiguration();
        config.loadBasicProperties(properties);

		if (kargs == null)
			System.exit(0);

        if (kargs.getSpringConfig() == null){
            kargs.setSpringConfig("default-config.xml");
        }
        rootPackages = "de.ids_mannheim.korap.web;";
        server.start();
    }
}
