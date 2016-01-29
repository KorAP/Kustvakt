package de.ids_mannheim.korap.user;

import de.ids_mannheim.korap.config.BeanConfiguration;
import de.ids_mannheim.korap.config.KustvaktClassLoader;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.interfaces.db.PersistenceClient;
import de.ids_mannheim.korap.interfaces.db.UserDataDbIface;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author hanl
 * @date 27/01/2016
 */
public class UserdataFactory {

    private static final Map<Class<? extends Userdata>, UserDataDbIface> instances = new HashMap<>();

    private UserdataFactory() {
    }

    public static Class<? extends UserDataDbIface> getClass(
            Class<? extends Userdata> data) {
        Set<Class<? extends UserDataDbIface>> c = KustvaktClassLoader
                .loadSubTypes(UserDataDbIface.class);
        for (Class<? extends UserDataDbIface> o : c) {
            Type type = o.getGenericInterfaces()[0];
            if (type instanceof ParameterizedType) {
                ParameterizedType ptype = (ParameterizedType) type;
                Class ctype = (Class) ptype.getActualTypeArguments()[0];
                if (ctype.equals(data))
                    return o;
            }
        }
        return null;
    }

    public static UserDataDbIface getDaoInstance(
            Class<? extends Userdata> data) throws KustvaktException {
        if (instances.get(data) == null) {
            Class<? extends UserDataDbIface> cl = getClass(data);
            if (BeanConfiguration.hasContext() && cl != null) {
                try {
                    Constructor c = cl.getConstructor(PersistenceClient.class);
                    UserDataDbIface iface = (UserDataDbIface) c.newInstance(
                            BeanConfiguration.getBeans()
                                    .getPersistenceClient());
                    instances.put(data, iface);
                    return iface;
                }catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
                    return null;
                }
            }
            throw new KustvaktException(StatusCodes.NOT_SUPPORTED,
                    "No database class found for type " + data.getSimpleName());
        }else
            return instances.get(data);
    }
}
