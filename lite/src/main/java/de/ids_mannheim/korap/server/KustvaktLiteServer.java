package de.ids_mannheim.korap.server;

import de.ids_mannheim.korap.config.BeansFactory;
import de.ids_mannheim.korap.web.KustvaktBaseServer;

public class KustvaktLiteServer extends KustvaktBaseServer {


    public static void main (String[] args) throws Exception {
        KustvaktLiteServer server = new KustvaktLiteServer();
        kargs = server.readAttributes(args);

        if (kargs.getConfig() == null) {
            kargs.setConfig("lite-config.xml");
        }
        
        BeansFactory.loadClasspathContext(kargs.getConfig());
        kargs.setRootPackages(
                new String[] { "de.ids_mannheim.korap.web.service.lite" });
        rootPackages = "de.ids_mannheim.korap.web.service.lite";

        server.start();
    }

}
