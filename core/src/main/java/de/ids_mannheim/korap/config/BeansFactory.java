package de.ids_mannheim.korap.config;

import de.ids_mannheim.korap.interfaces.db.UserDataDbIface;
import de.ids_mannheim.korap.user.Userdata;
import de.ids_mannheim.korap.web.CoreResponseHandler;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;

/**
 * User: hanl
 * Date: 10/9/13
 * Time: 11:20 AM
 */
public class BeansFactory {

    private static final String CONFIG_FILE = "light-config.xml";

    private static ContextHolder beanHolder;


    //todo: allow this for external plugin systems that are not kustvakt specific
    @Deprecated
    public static void setCustomBeansHolder (ContextHolder holder) {
        beanHolder = holder;
    }


    public static synchronized ContextHolder getKustvaktContext () {
        return beanHolder;
    }


    public static synchronized ContextHolder getKustvaktContext (int i) {
        return beanHolder;
    }


    public static synchronized TypeBeanFactory getTypeFactory () {
        return new TypeBeanFactory();
    }


    public static int loadClasspathContext (String ... files) {
        ApplicationContext context;
        if (files.length == 0)
            context = new ClassPathXmlApplicationContext(CONFIG_FILE);
        else
            context = new ClassPathXmlApplicationContext(files);
        ContextHolder h = new ContextHolder(context) {};
        BeansFactory.beanHolder = h;
        //        return BeansFactory.beanHolder.indexOf(h);
        return 0;
    }


    public static synchronized int addApplicationContext (
            ApplicationContext context) {
        ContextHolder h = new ContextHolder(context) {};
        BeansFactory.beanHolder = h;
        //        return BeansFactory.beanHolder.indexOf(h);
        return 0;
    }


    public static synchronized void setKustvaktContext (ContextHolder holder) {
        BeansFactory.beanHolder = holder;
    }


    public static synchronized int setApplicationContext (
            ApplicationContext context) {
        ContextHolder h = new ContextHolder(context) {};
        BeansFactory.beanHolder = h;
        return 0;
    }


    public static synchronized int loadFileContext (String filepath) {
        ApplicationContext context = new FileSystemXmlApplicationContext(
                "file:" + filepath);
        ContextHolder h = new ContextHolder(context) {};
        BeansFactory.beanHolder = h;
        return 0;
    }


    public static void closeApplication () {
        BeansFactory.beanHolder = null;
    }


    //todo: set response handler
    @Deprecated
    public static CoreResponseHandler getResponseHandler () {
        return null;
    }


    public BeansFactory () {}

    public static class TypeBeanFactory {

        public <T> T getTypeInterfaceBean (Collection objs, Class type) {
            for (Object o : objs) {
                if (o instanceof KustvaktTypeInterface) {
                    Class t = ((KustvaktTypeInterface) o).type();
                    if (type.equals(t))
                        return (T) o;
                }
            }
            throw new RuntimeException(
                    "Could not find typed bean in context for class '" + type
                            + "'");
        }


        @Deprecated
        public <T> T getTypedBean (Collection objs, Class type) {
            for (Object o : objs) {
                Type gtype = o.getClass().getGenericSuperclass();
                if (gtype instanceof ParameterizedType) {
                    ParameterizedType ptype = (ParameterizedType) gtype;
                    Object ctype = ptype.getActualTypeArguments()[0];
                    if (ctype.equals(type))
                        return (T) o;
                }
            }
            throw new RuntimeException(
                    "Could not find typed bean in context for class '" + type
                            + "'");
        }
    }
}
