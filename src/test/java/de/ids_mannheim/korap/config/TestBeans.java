package de.ids_mannheim.korap.config;

import de.ids_mannheim.korap.interfaces.AuthenticationManagerIface;
import de.ids_mannheim.korap.interfaces.EncryptionIface;
import de.ids_mannheim.korap.interfaces.db.*;
import org.springframework.context.annotation.Bean;

import java.util.List;

/**
 * @author hanl
 * @date 20/02/2016
 */
public abstract class TestBeans {

    protected PersistenceClient dataSource;


    public abstract PolicyHandlerIface getPolicyDao ();


    public abstract KustvaktConfiguration getConfig ();


    public abstract EntityHandlerIface getUserDao ();


    public abstract AuditingIface getAuditingDao ();


    public abstract List<ResourceOperationIface> getResourceDaos ();


    public abstract List<UserDataDbIface> getUserdataDaos ();


    public abstract EncryptionIface getCrypto ();


    public abstract AuthenticationManagerIface getAuthManager ();


    @Bean(name = "kustvakt_db")
    public PersistenceClient getDataSource () {
        return this.dataSource;
    }
}
