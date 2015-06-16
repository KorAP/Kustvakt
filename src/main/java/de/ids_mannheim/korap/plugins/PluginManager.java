package de.ids_mannheim.korap.plugins;

import de.ids_mannheim.korap.interfaces.EntityHandlerIface;
import de.ids_mannheim.korap.interfaces.UserControllerIface;

import java.util.HashMap;
import java.util.Map;

/**
 * @author hanl
 * @date 15/06/2015
 */
// via spring a list of implementations is inserted, for which there will be default constructors loaded
public class PluginManager {

    private Map<String, Class> plugins;

    public PluginManager() {
        plugins = new HashMap<>();
    }

    public void loadPluginInterfaces() {
        plugins.put("userdb", EntityHandlerIface.class);
        plugins.put("usercontroller", UserControllerIface.class);
        plugins.put("encrytion", EntityHandlerIface.class);
    }

    public void register(String key, Class cl) {
        plugins.put(key, cl);
    }
}
