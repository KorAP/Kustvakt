package de.ids_mannheim.korap.config;

import org.reflections.Reflections;

import java.lang.annotation.Annotation;
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
}
