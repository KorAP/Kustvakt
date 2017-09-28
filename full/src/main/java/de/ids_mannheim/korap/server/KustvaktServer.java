package de.ids_mannheim.korap.server;

import de.ids_mannheim.korap.config.BeansFactory;
import de.ids_mannheim.korap.web.KustvaktBaseServer;

/**
 * pu
 * 
 * @author hanl
 * @date 28/01/2014
 */
public class KustvaktServer extends KustvaktBaseServer {

    public static final String API_VERSION = "v0.1";
    
    public static void main (String[] args) throws Exception {
        KustvaktServer server = new KustvaktServer();
        kargs = server.readAttributes(args);

        if (kargs.getConfig() != null)
            BeansFactory.loadFileContext(kargs.getConfig());
        else{
            kargs.setConfig("default-config.xml");
            BeansFactory.loadClasspathContext("default-config.xml");
        }
        kargs.setRootPackages(new String[] { "de.ids_mannheim.korap.web.utils",
                "de.ids_mannheim.korap.web.service.full" });
        rootPackages = "de.ids_mannheim.korap.web.utils;"
                + "de.ids_mannheim.korap.web.service.full";
        server.start();
    }

    @Override
    protected void setup () {
//        Set<Class<? extends BootableBeanInterface>> set = KustvaktClassLoader
//                .loadSubTypes(BootableBeanInterface.class);
//
//        ContextHolder context = BeansFactory.getKustvaktContext();
//        if (context == null)
//            throw new RuntimeException("Beans could not be loaded!");
//
//        List<BootableBeanInterface> list = new ArrayList<>(set.size());
//        for (Class cl : set) {
//            BootableBeanInterface iface;
//            
//            try {
//                iface = (BootableBeanInterface) cl.newInstance();
//                if (iface instanceof CollectionLoader){
//                	continue;
//                }
//                list.add(iface);
//            }
//            catch (InstantiationException | IllegalAccessException e) {
//                continue;
//            }
//        }
//        System.out.println("Found boot loading interfaces: " + list);
//
//        while (!list.isEmpty()) {
//            loop: for (BootableBeanInterface iface : new ArrayList<>(list)) {
//                try {
//                    for (Class dep : iface.getDependencies()) {
//                        if (set.contains(dep))
//                            continue loop;
//                    }
//                    iface.load(context);
//                    list.remove(iface);
//                    set.remove(iface.getClass());
//                    System.out.println("Done with interface "
//                            + iface.getClass().getSimpleName());
//                }
//                catch (KustvaktException e) {
//                    // don't do anything!
//                    System.out.println("An error occurred in class "
//                            + iface.getClass().getSimpleName() + "!\n");
//                    e.printStackTrace();
//                    System.exit(-1);
//                }
//            }
//        }
    }
}
