package de.ids_mannheim.korap.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import de.ids_mannheim.korap.handlers.AdminDao;
import de.ids_mannheim.korap.handlers.DocumentDao;
import de.ids_mannheim.korap.handlers.EntityDao;
import de.ids_mannheim.korap.handlers.JDBCAuditing;
import de.ids_mannheim.korap.handlers.ResourceDao;
import de.ids_mannheim.korap.handlers.UserDetailsDao;
import de.ids_mannheim.korap.handlers.UserSettingsDao;
import de.ids_mannheim.korap.interfaces.AuthenticationIface;
import de.ids_mannheim.korap.interfaces.AuthenticationManagerIface;
import de.ids_mannheim.korap.interfaces.EncryptionIface;
import de.ids_mannheim.korap.interfaces.db.AdminHandlerIface;
import de.ids_mannheim.korap.interfaces.db.AuditingIface;
import de.ids_mannheim.korap.interfaces.db.EntityHandlerIface;
import de.ids_mannheim.korap.interfaces.db.PersistenceClient;
import de.ids_mannheim.korap.interfaces.db.PolicyHandlerIface;
import de.ids_mannheim.korap.interfaces.db.ResourceOperationIface;
import de.ids_mannheim.korap.interfaces.db.UserDataDbIface;
import de.ids_mannheim.korap.interfaces.defaults.KustvaktEncryption;
import de.ids_mannheim.korap.security.ac.PolicyDao;
import de.ids_mannheim.korap.security.auth.APIAuthentication;
import de.ids_mannheim.korap.security.auth.BasicHttpAuth;
import de.ids_mannheim.korap.security.auth.KustvaktAuthenticationManager;
import de.ids_mannheim.korap.security.auth.OpenIDconnectAuthentication;
import de.ids_mannheim.korap.security.auth.SessionAuthentication;
@Configuration
public class AppTestConfig extends AppTestConfigBase implements TestBeans {

    protected PersistenceClient dataSource;

    public AppTestConfig () throws InterruptedException, IOException {
        this.dataSource = TestHelper.sqlite_db(true);
        //this.dataSource = TestHelper.mysql_db();
    }


    @Bean(name = "kustvakt_db")
    public PersistenceClient getDataSource () {
        return this.dataSource;
    }


    @Bean(name = ContextHolder.KUSTVAKT_POLICIES)
    @Override
    public PolicyHandlerIface getPolicyDao () {
        return new PolicyDao(this.dataSource);
    }


    @Bean(name = ContextHolder.KUSTVAKT_USERDB)
    @Override
    public EntityHandlerIface getUserDao () {
        return new EntityDao(this.dataSource);
    }


    @Bean(name = ContextHolder.KUSTVAKT_ADMINDB)
    @Override
    public AdminHandlerIface getAdminDao () {
        return new AdminDao(this.dataSource);
    }



    @Bean(name = ContextHolder.KUSTVAKT_AUDITING)
    @Override
    public AuditingIface getAuditingDao () {
        return new JDBCAuditing(this.dataSource);
    }


    @Bean(name = ContextHolder.KUSTVAKT_RESOURCES)
    @Override
    public List<ResourceOperationIface> getResourceDaos () {
        List<ResourceOperationIface> res = new ArrayList<>();
        res.add(getDocumentDao());
        res.add(getResourceDao());
        return res;
    }
    
    @Bean(name = "document_dao")
    public DocumentDao getDocumentDao () {
        return new DocumentDao(getDataSource());
    }

    
    @Bean(name = "resource_dao")
    public ResourceDao getResourceDao () {
        return new ResourceDao(getDataSource());
    }



    @Bean(name = ContextHolder.KUSTVAKT_USERDATA)
    @Override
    public List<UserDataDbIface> getUserdataDaos () {
        List<UserDataDbIface> ud = new ArrayList<>();
        ud.add(new UserSettingsDao(getDataSource()));
        ud.add(new UserDetailsDao(getDataSource()));
        return ud;
    }


    @Bean(name = ContextHolder.KUSTVAKT_ENCRYPTION)
    @Override
    public EncryptionIface getCrypto () {
        return new KustvaktEncryption(getConfig());
    }


    @Bean(name = ContextHolder.KUSTVAKT_AUTHENTICATION_MANAGER)
    @Override
    public AuthenticationManagerIface getAuthManager () {
        AuthenticationManagerIface manager = new KustvaktAuthenticationManager(
                getUserDao(), getAdminDao(), getCrypto(), getConfig(),
                getAuditingDao(), getUserdataDaos());
        Set<AuthenticationIface> pro = new HashSet<>();
        pro.add(new BasicHttpAuth());
        pro.add(new APIAuthentication(getConfig()));
        pro.add(new SessionAuthentication(getConfig(), getCrypto()));
        pro.add(new OpenIDconnectAuthentication(getConfig(), getDataSource()));
        manager.setProviders(pro);
        return manager;
    }

}


