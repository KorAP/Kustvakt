package de.ids_mannheim.korap.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author hanl
 * @date 17/06/2015
 */
public class DefaultHandler {

    private Map<String, Object> defaults;


    public DefaultHandler () {
        this.defaults = new HashMap<>();
        loadClasses();
    }


    private void loadClasses () {
        Set<Class<?>> cls = KustvaktClassLoader
                .loadFromAnnotation(Configurable.class);
        for (Class clazz : cls) {
            Configurable c = (Configurable) clazz
                    .getAnnotation(Configurable.class);
            try {
                this.defaults.put(c.value(), clazz.newInstance());
            }
            catch (InstantiationException | IllegalAccessException e) {
                throw new RuntimeException("Could not instantiate class");
            }
        }
    }


    public Object getDefault (String name) {
        return this.defaults.get(name);
    }


    public <T> T getDefault (Class<T> tClass) {
        for (Object o : this.defaults.values()) {
            if (o.getClass().equals(tClass))
                return (T) o;
        }
        return null;
    }


    public void remove (String name) {
        this.defaults.remove(name);
    }


    @Override
    public String toString () {
        return defaults.toString();
    }
}
