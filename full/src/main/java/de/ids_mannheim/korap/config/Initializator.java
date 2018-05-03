package de.ids_mannheim.korap.config;

import java.util.Set;

import de.ids_mannheim.korap.oauth2.dao.AccessScopeDao;

/**
 * Initializes values in the database from kustvakt configuration.
 * 
 * @author margaretha
 *
 */
public class Initializator {

    private FullConfiguration config;
    private AccessScopeDao accessScopeDao;


    public Initializator (FullConfiguration config,
                          AccessScopeDao accessScopeDao) {
        this.config = config;
        this.accessScopeDao = accessScopeDao;
    }

    public void init () {
        setAccessScope();
    }

    private void setAccessScope () {
        accessScopeDao.storeAccessScopes(config.getDefaultAccessScopes());
        accessScopeDao.storeAccessScopes(config.getClientCredentialsScopes());
    }
}
