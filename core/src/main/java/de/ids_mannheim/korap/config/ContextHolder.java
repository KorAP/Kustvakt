package de.ids_mannheim.korap.config;

import de.ids_mannheim.korap.interfaces.EncryptionIface;
import de.ids_mannheim.korap.interfaces.ValidatorIface;
import de.ids_mannheim.korap.interfaces.db.*;
import de.ids_mannheim.korap.interfaces.defaults.ApacheValidator;
import de.ids_mannheim.korap.web.CoreResponseHandler;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.Collection;

/**
 * @author hanl
 * @date 26/02/2016
 */
public abstract class ContextHolder {

    public static final String KUSTVAKT_DB = "kustvakt_db";
    public static final String KUSTVAKT_ENCRYPTION = "kustvakt_encryption";
    public static final String KUSTVAKT_AUDITING = "kustvakt_auditing";
    public static final String KUSTVAKT_CONFIG = "kustvakt_config";
    public static final String KUSTVAKT_USERDATA = "kustvakt_userdata";
    public static final String KUSTVAKT_RESOURCES = "kustvakt_resources";

    public static final String KUSTVAKT_AUTHENTICATION_MANAGER = "kustvakt_authenticationmanager";
    public static final String KUSTVAKT_AUTHPROVIDERS = "kustvakt_authproviders";
    public static final String KUSTVAKT_USERDB = "kustvakt_userdb";
    public static final String KUSTVAKT_ADMINDB = "kustvakt_admindb";
    public static final String KUSTVAKT_POLICIES = "kustvakt_policies";

    private ApplicationContext context = null;
    private DefaultHandler handler;


    public ContextHolder (ApplicationContext context) {
        this.handler = new DefaultHandler();
        this.context = context;
        // todo: better method?!
        new CoreResponseHandler(getAuditingProvider());
    }


    protected <T> T getBean (Class<T> clazz) {
        if (this.context != null) {
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
        T bean = null;
        if (this.context != null) {
            try {
                bean = (T) context.getBean(name);
            }
            catch (NoSuchBeanDefinitionException e) {
                // do nothing
                bean = (T) this.handler.getDefault(name);
            }
        }

        return bean;
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


    public Collection<UserDataDbIface> getUserDataProviders () {
        return getBean(KUSTVAKT_USERDATA);
    }


    public EncryptionIface getEncryption () {
        return getBean(KUSTVAKT_ENCRYPTION);
    }


//    public AuthenticationManagerIface getAuthenticationManager () {
//        return getBean(KUSTVAKT_AUTHENTICATION_MANAGER);
//    }


    public EntityHandlerIface getUserDBHandler () {
        return getBean(KUSTVAKT_USERDB);
    }
    
    public PolicyHandlerIface getPolicyDbProvider () {
        Object b = getBean(KUSTVAKT_POLICIES);
        return (PolicyHandlerIface) b;
    }


//    public Collection<AuthenticationIface> getAuthProviders () {
//        return getBean(KUSTVAKT_AUTHPROVIDERS);
//    }


    public Collection<ResourceOperationIface> getResourceProviders () {
        return getBean(KUSTVAKT_RESOURCES);
    }


    public ValidatorIface getValidator()  {
        try {
            return new ApacheValidator();
        } catch (IOException e) {
            throw new RuntimeException("validator could not be loaded!");
        }
    }

    private void close () {
        this.getAuditingProvider().finish();
        this.context = null;
    }
}
