package de.ids_mannheim.korap.config;

import java.io.IOException;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import de.ids_mannheim.korap.interfaces.EncryptionIface;
import de.ids_mannheim.korap.interfaces.ValidatorIface;
import de.ids_mannheim.korap.interfaces.db.AuditingIface;
import de.ids_mannheim.korap.interfaces.db.PersistenceClient;
import de.ids_mannheim.korap.interfaces.db.ResourceOperationIface;
import de.ids_mannheim.korap.interfaces.db.UserDataDbIface;
import de.ids_mannheim.korap.interfaces.defaults.ApacheValidator;
import de.ids_mannheim.korap.web.CoreResponseHandler;

/**
 * User: hanl
 * Date: 10/9/13
 * Time: 11:20 AM
 */
@Deprecated
public class BeanConfiguration {

    private static final String CONFIG_FILE = "light-config.xml";
    public static final String KUSTVAKT_DB = "kustvakt_db";

    public static final String KUSTVAKT_ENCRYPTION = "kustvakt_encryption";
    public static final String KUSTVAKT_AUDITING = "kustvakt_auditing";
    public static final String KUSTVAKT_CONFIG = "kustvakt_config";
    public static final String KUSTVAKT_USERSETTINGS = "kustvakt_usersettings";
    public static final String KUSTVAKT_USERDETAILS = "kustvakt_userdetails";

    public static final String KUSTVAKT_AUTHENTICATION_MANAGER = "kustvakt_authenticationmanager";
    public static final String KUSTVAKT_USERDB = "kustvakt_userdb";

    private static BeanHolderHelper beans;


    //todo: allow this for external plugin systems that are not kustvakt specific
    @Deprecated
    public static void setCustomBeansHolder (BeanHolderHelper holder) {
        ApplicationContext context = beans.context;
        holder.context = context;
        BeanConfiguration.beans = holder;
    }


    public static BeanHolderHelper getBeans () {
        return BeanConfiguration.beans;
    }



    public static boolean hasContext () {
        return beans != null && beans.context != null;
    }


    public static void loadClasspathContext (String ... files) {
        if (hasContext())
            closeApplication();

        ApplicationContext context;
        if (files.length == 0)
            context = new ClassPathXmlApplicationContext(CONFIG_FILE);
        else
            context = new ClassPathXmlApplicationContext(files);

        BeanConfiguration.beans = new BeanHolderHelper(context);

    }


    public static void loadFileContext (String filepath) {
        if (!hasContext()) {
            ApplicationContext context = new FileSystemXmlApplicationContext(
                    "file:" + filepath);
            BeanConfiguration.beans = new BeanHolderHelper(context);
        }
    }


    public static void closeApplication () {
        if (hasContext())
            beans.finish();
        beans = null;
    }


    //todo: set response handler
    @Deprecated
    public static CoreResponseHandler getResponseHandler () {
        return null;
    }

    public static class BeanHolderHelper {

        private ApplicationContext context = null;
        private DefaultHandler handler;


        private BeanHolderHelper (ApplicationContext context) {
            this.handler = new DefaultHandler();
            this.context = context;
            // todo: better method?!
            new CoreResponseHandler(getAuditingProvider());
        }


        protected <T> T getBean (Class<T> clazz) {
            if (context != null) {
                try {
                    return context.getBean(clazz);
                }
                catch (NoSuchBeanDefinitionException e) {
                    // do nothing
                }
            }
            return this.handler.getDefault(clazz);
        }


        protected <T> T getBean (String name) {
            if (context != null) {
                try {
                    return (T) context.getBean(name);
                }
                catch (NoSuchBeanDefinitionException e) {
                    // do nothing
                }
            }
            return (T) this.handler.getDefault(name);
        }


        public AuditingIface getAuditingProvider () {
            return (AuditingIface) getBean(KUSTVAKT_AUDITING);
        }


        public <T extends KustvaktConfiguration> T getConfiguration () {
            return (T) getBean(KUSTVAKT_CONFIG);
        }


        public PersistenceClient getPersistenceClient () {
            return getBean(KUSTVAKT_DB);
        }


        public UserDataDbIface getUserDetailsDao () {
            return getBean(KUSTVAKT_USERDETAILS);
        }


        public UserDataDbIface getUserSettingsDao () {
            return getBean(KUSTVAKT_USERSETTINGS);
        }


        public EncryptionIface getEncryption () {
            return getBean(KUSTVAKT_ENCRYPTION);
        }


//        public AuthenticationManagerIface getAuthenticationManager () {
//            return getBean(KUSTVAKT_AUTHENTICATION_MANAGER);
//        }


//        public EntityHandlerIface getUserDBHandler () {
//            return getBean(KUSTVAKT_USERDB);
//        }

        public ValidatorIface getValidator()  {
            try {
                return new ApacheValidator();
            } catch (IOException e) {
                throw new RuntimeException("validator could not be loaded!");
            }
        }

        // todo: !!!!!!!!!!!!!!!!!!!!!!!!!!
        // todo: more specific --> collection provider, document provider, etc.
        public ResourceOperationIface getResourceProvider () {
            return getBean("resourceProvider");
        }


        private void finish () {
            this.getAuditingProvider().finish();
            context = null;
        }

    }
}
