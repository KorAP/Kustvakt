package de.ids_mannheim.korap.server;

import java.io.File;
import java.io.FileInputStream;
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
        if (!f.exists()){
            URL url = KustvaktServer.class.getResource("kustvakt.conf");
            if (url!=null){
                f = new File(url.toURI());
            }
        }
        
        Properties properties = new Properties();
        FileInputStream in = new FileInputStream(f);
        properties.load(in);
        in.close();
        fullConfig = new FullConfiguration(properties);
        config = fullConfig;

        if (kargs.getConfig() == null){
//            BeansFactory.loadFileContext(kargs.getConfig());
//        }
//        else {
            kargs.setConfig("default-config.xml");
//            BeansFactory.loadClasspathContext("default-config.xml");
        }
        kargs.setRootPackages(new String[] { "de.ids_mannheim.korap.web.utils",
                "de.ids_mannheim.korap.web.service.full" });
        rootPackages = "de.ids_mannheim.korap.web.utils;"
                + "de.ids_mannheim.korap.web.service.full";
        server.start();
    }
}
