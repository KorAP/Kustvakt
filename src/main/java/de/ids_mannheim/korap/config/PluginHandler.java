package de.ids_mannheim.korap.config;

import de.ids_mannheim.korap.interfaces.AuditingIface;
import de.ids_mannheim.korap.interfaces.AuthenticationManagerIface;
import de.ids_mannheim.korap.interfaces.EncryptionIface;
import de.ids_mannheim.korap.interfaces.EntityHandlerIface;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author hanl
 * @date 17/06/2015
 */
public class PluginHandler {

    private static Map<Class<? extends Annotation>, Class> interfaces;
    private Map<Class<? extends Annotation>, Object> plugins;

    // add resource handler annotation
    static {
        interfaces = new HashMap<>();
        interfaces.put(AuditingHandler.class, AuditingIface.class);
        interfaces.put(UserDbHandler.class, EntityHandlerIface.class);
        interfaces.put(AuthenticationHandler.class,
                AuthenticationManagerIface.class);
        interfaces.put(EncryptionHandler.class, EncryptionIface.class);
        //todo:
        interfaces.put(ResourceHandler.class, ResourceHandler.class);
    }

    public PluginHandler() {
        this.plugins = new HashMap<>();
        this.load();
    }

    public void load() {
        for (Map.Entry<Class<? extends Annotation>, Class> en : new HashSet<>(
                interfaces.entrySet())) {
            Set<Class<?>> set = KustvaktClassLoader
                    .loadFromAnnotation(en.getKey());
            if (set.size() > 1)
                throw new UnsupportedOperationException(
                        "handler declaration not unique!");
            else if (set.size() == 0)
                interfaces.remove(en.getKey());
        }
    }

    public void addInterface(Class<? extends Annotation> anno, Class<?> iface) {
        interfaces.put(anno, iface);
    }

    public void registerPlugin(Object ob) {
        for (Map.Entry<Class<? extends Annotation>, Class> en : interfaces
                .entrySet()) {
            if (en.getValue().isInstance(ob))
                this.plugins.put(en.getKey(), ob);
        }
    }

    public Object getPluginInstance(Class<? extends Annotation> anno) {
        Object o = this.plugins.get(anno);
        if (o == null)
            return new NullPointerException(
                    "no plugin defined for type " + anno.toString());
        return o;
    }

    @Override
    public String toString() {
        System.out.println("PRINT INTERFACES " + interfaces.toString());
        return plugins.toString();
    }
}
