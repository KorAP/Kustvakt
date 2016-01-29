package de.ids_mannheim.korap.config;

import de.ids_mannheim.korap.interfaces.db.PersistenceClient;
import de.ids_mannheim.korap.interfaces.db.ResourceOperationIface;
import org.reflections.Reflections;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Set;

/**
 * @author hanl
 * @date 10/06/2015
 */
public class KustvaktClassLoader {

    private static final Reflections reflections = new Reflections(
            "de.ids_mannheim.korap");

    private KustvaktClassLoader() {
    }

    /**
     * loads interface implementations in current classpath
     *
     * @param iface
     * @param <T>
     * @return
     */
    public static <T> Set<Class<? extends T>> loadSubTypes(Class<T> iface) {
        return reflections.getSubTypesOf(iface);
    }

    public static Set<Class<?>> loadFromAnnotation(
            Class<? extends Annotation> annotation) {
        return reflections.getTypesAnnotatedWith(annotation);
    }



    @Deprecated
    public static void registerResourceClasses() {
        PersistenceClient cl = BeanConfiguration.getBeans()
                .getPersistenceClient();
        Set<ResourceOperationIface> set = new HashSet<>();
        Set<Class<? extends ResourceOperationIface>> resource_prov = loadSubTypes(
                ResourceOperationIface.class);
        for (Class<? extends ResourceOperationIface> op : resource_prov) {
            Constructor c;
            try {
                c = op.getConstructor(PersistenceClient.class);
                set.add((ResourceOperationIface) c.newInstance(cl));
            }catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
                // do nothing;
            }
        }

        if (BeanConfiguration.hasContext()) {
            BeanConfiguration.BeanHolderHelper helper = BeanConfiguration
                    .getBeans();
            if (helper.getPolicyDbProvider() != null
                    && helper.getEncryption() != null
                    && helper.getResourceProvider() != null) {

                de.ids_mannheim.korap.security.ac.SecurityManager
                        .setProviders(helper.getPolicyDbProvider(),
                                helper.getEncryption(), set);

            }
        }
    }
}
