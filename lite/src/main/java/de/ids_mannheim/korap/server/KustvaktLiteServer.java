package de.ids_mannheim.korap.server;

import de.ids_mannheim.korap.config.BeansFactory;
import de.ids_mannheim.korap.web.KustvaktBaseServer;

public class KustvaktLiteServer extends KustvaktBaseServer{

    
    public static void main (String[] args) throws Exception {
        KustvaktLiteServer server = new KustvaktLiteServer();
        kargs = server.readAttributes(args);

        if (kargs.getConfig() != null)
            BeansFactory.loadFileContext(kargs.getConfig());
        else{
            kargs.setConfig("light-config.xml");
            BeansFactory.loadClasspathContext();
        }
        kargs.setRootPackages(new String[] { "de.ids_mannheim.korap.web.service.light" });
        rootPackages = "de.ids_mannheim.korap.web.service.light";
        
        server.start();
    }
    
    @Override
    protected void setup () {
        // TODO Auto-generated method stub
        
    }

}
