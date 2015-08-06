package de.ids_mannheim.korap.config;

import de.ids_mannheim.korap.interfaces.*;
import de.ids_mannheim.korap.web.utils.KustvaktResponseHandler;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import java.util.HashSet;
import java.util.Set;

/**
 * User: hanl
 * Date: 10/9/13
 * Time: 11:20 AM
 */
public class BeanConfiguration {

    private static final String config_file = "default-config.xml";
    public static final String KUSTVAKT_DB = "kustvakt_db";

    public static final String KUSTVAKT_ENCRYPTION = "kustvakt_encryption";
    public static final String KUSTVAKT_AUDITING = "kustvakt_auditing";
    public static final String KUSTVAKT_CONFIG = "kustvakt_config";

    private static BeanHolder beans;

    public static void setCustomBeansHolder(BeanHolder holder) {
        ApplicationContext context = beans.context;
        holder.context = context;
        BeanConfiguration.beans = holder;
    }

    public static BeanHolder getBeans() {
        return BeanConfiguration.beans;
    }

    public static void loadAuthenticationProviders() {
        Set<Class<? extends AuthenticationIface>> set = KustvaktClassLoader
                .loadSubTypes(AuthenticationIface.class);
        Set<AuthenticationIface> set2 = new HashSet<>();
        for (Class<? extends AuthenticationIface> i : set) {
            try {
                set2.add(i.newInstance());
            }catch (InstantiationException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        try {
            getBeans().getAuthenticationManager().setProviders(set2);
        }catch (RuntimeException e) {
            // do nothing
        }
    }

    public static void loadClasspathContext(String... files) {
        if (beans == null) {
            ApplicationContext context;
            if (files.length == 0)
                context = new ClassPathXmlApplicationContext(config_file);
            else
                context = new ClassPathXmlApplicationContext(files);
            BeanConfiguration.beans = new BeanHolder(context);
        }
    }

    public static void loadFileContext(String filepath) {
        if (beans == null) {
            ApplicationContext context = new FileSystemXmlApplicationContext(
                    "file:" + filepath);
            BeanConfiguration.beans = new BeanHolder(context);
        }
    }
    //todo: set response handler
    public static KustvaktResponseHandler getResponseHandler() {
        return null;
    }

    public static class BeanHolder {

        private ApplicationContext context = null;
        private PluginHandler handler;

        public BeanHolder() {
            this.handler = new PluginHandler();
        }

        private BeanHolder(ApplicationContext context) {
            this.context = context;
        }

        protected <T> T getBean(Class<T> clazz) {
            try {
                return context.getBean(clazz);
            }catch (NoSuchBeanDefinitionException e) {
                return this.handler.getDefault(clazz);
            }
        }

        protected <T> T getBean(String name) {
            try {
                return (T) context.getBean(name);
            }catch (NoSuchBeanDefinitionException e) {
                return (T) this.handler.getDefault(name);
            }
        }

        public AuditingIface getAuditingProvider() {
            return (AuditingIface) context.getBean(KUSTVAKT_AUDITING);
        }

        public <T extends KustvaktConfiguration> T getConfiguration() {
            return (T) getBean(KUSTVAKT_CONFIG);
        }

        public PersistenceClient getPersistenceClient() {
            return getBean(KUSTVAKT_DB);
        }

        public AuthenticationManagerIface getAuthenticationManager() {
            throw new RuntimeException("!Stub");
        }

        public EntityHandlerIface getUserDBHandler() {
            throw new RuntimeException("!Stub");
        }

        public EncryptionIface getEncryption() {
            return getBean(KUSTVAKT_ENCRYPTION);
        }

    }
}
