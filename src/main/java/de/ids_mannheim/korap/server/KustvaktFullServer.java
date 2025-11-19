package de.ids_mannheim.korap.server;

public class KustvaktFullServer extends KustvaktBaseServer {

    public static void main (String[] args) throws Exception {
        System.setProperty("log4j.configurationFile", "data/log4j2.properties, log4j2.properties");
        
        KustvaktFullServer server = new KustvaktFullServer();
        kargs = server.readAttributes(args);

        if (kargs == null)
            System.exit(0);

        kargs.setLite(false);     
        springConfig = "default-config.xml";
        rootPackages = "de.ids_mannheim.korap.core.web;"
                + "de.ids_mannheim.korap.web;";
        
        server.loadProperties("data/kustvakt.conf", "kustvakt.conf");
        server.start();
    }

}
