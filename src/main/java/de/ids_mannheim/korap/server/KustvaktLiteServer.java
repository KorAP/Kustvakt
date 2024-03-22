package de.ids_mannheim.korap.server;

public class KustvaktLiteServer extends KustvaktBaseServer {

    public static void main (String[] args) throws Exception {
        KustvaktLiteServer server = new KustvaktLiteServer();
        kargs = server.readAttributes(args);

        if (kargs == null)
            System.exit(0);

        server.loadProperties("data/kustvakt-lite.conf", "kustvakt-lite.conf");

        springConfig = "default-lite-config.xml";

        rootPackages = "de.ids_mannheim.korap.core.web; "
                + "de.ids_mannheim.korap.web.filter; "
                + "de.ids_mannheim.korap.web.utils; "
                + "com.fasterxml.jackson.jaxrs.json;";

        server.start();
    }

}
