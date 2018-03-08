package de.ids_mannheim.korap.server;

import de.ids_mannheim.korap.web.KustvaktBaseServer;

public class KustvaktLiteServer extends KustvaktBaseServer {


    public static void main (String[] args) throws Exception {
        KustvaktLiteServer server = new KustvaktLiteServer();
        kargs = server.readAttributes(args);

        if (kargs == null) System.exit(0);

        kargs.setSpringConfig("lite-config.xml");
        kargs.setRootPackages(
                new String[] { "de.ids_mannheim.korap.web.service.lite" });
        rootPackages = "de.ids_mannheim.korap.web.service.lite";

        server.start();
    }

}
