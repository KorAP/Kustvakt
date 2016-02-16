package de.ids_mannheim.korap.config;

import org.reflections.Reflections;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
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

    public static <T> Class<? extends T> getTypeClass(Class type, Class<T> iface) {
        Set<Class<? extends T>> c = KustvaktClassLoader.loadSubTypes(iface);
        for (Class<? extends T> o : c) {
            Type ctype = o.getGenericInterfaces()[0];
            if (ctype instanceof ParameterizedType) {
                ParameterizedType ptype = (ParameterizedType) ctype;
                Class tclass = (Class) ptype.getActualTypeArguments()[0];
                if (tclass.equals(type))
                    return o;
            }
        }
        return null;
    }
}
