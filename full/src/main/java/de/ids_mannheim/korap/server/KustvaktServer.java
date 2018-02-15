package de.ids_mannheim.korap.server;

import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.util.Properties;

import javax.naming.NamingException;

import org.eclipse.jetty.jndi.factories.MailSessionReference;
import org.eclipse.jetty.plus.jndi.Resource;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.webapp.Configuration.ClassList;
import org.eclipse.jetty.webapp.WebAppContext;

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
            URL url = KustvaktServer.class.getClassLoader().getResource("kustvakt.conf");
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

    @Override
    protected void setupJndi(Server server, WebAppContext webapp) {
        
        
//        ClassList classlist = ClassList.setServerDefault(server);
//        classlist.addAfter("org.eclipse.jetty.webapp.FragmentConfiguration",
//                "org.eclipse.jetty.plus.webapp.EnvConfiguration",
//                "org.eclipse.jetty.plus.webapp.PlusConfiguration");
        
        MailSessionReference mailref = new MailSessionReference();
        mailref.setUser(fullConfig.getMailUsername());
        mailref.setPassword(fullConfig.getMailPassword());
        
        Properties props = new Properties();
        props.put("mail.smtp.auth", "false");
        props.put("mail.smtp.host",fullConfig.getMailSmtp());
        props.put("mail.from",fullConfig.getMailUsername());
        props.put("mail.debug", "false");
        mailref.setProperties(props);
        try {
            new Resource(webapp, "mail/Session", mailref);
        }
        catch (NamingException e) {
            e.printStackTrace();
        }
    }
    
}
