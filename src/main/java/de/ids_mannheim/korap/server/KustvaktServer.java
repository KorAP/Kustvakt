package de.ids_mannheim.korap.server;

/**
 * pu
 * 
 * @author hanl
 * @date 28/01/2014
 */
public class KustvaktServer extends KustvaktBaseServer {

    public static void main (String[] args) throws Exception {
        System.setProperty("log4j.configurationFile", "data/log4j2.properties");
        KustvaktServer server = new KustvaktServer();
        kargs = server.readAttributes(args);

        // EM: why is this necessary?
        if (kargs == null)
            System.exit(0);

        if (kargs.isLite()) {
        	server.loadProperties("data/kustvakt-lite.conf", "kustvakt-lite.conf");
        }
        else {
        	server.loadProperties("data/kustvakt.conf", "kustvakt.conf");
        }
        server.start();
    }
}
