package de.ids_mannheim.korap.config;

import org.reflections.Reflections;

import java.util.Set;

/**
 * @author hanl
 * @date 10/06/2015
 */
public class KustvaktClassLoader {

    private static final Reflections reflections = new Reflections(
            "de.ids_mannheim.korap");

    /**
     * loads interface implementations in current classpath
     *
     * @param iface
     * @param <T>
     * @return
     */
    public static <T> Set<Class<? extends T>> load(Class<T> iface) {
        return reflections.getSubTypesOf(iface);
    }
}
