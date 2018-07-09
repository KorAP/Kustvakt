package de.ids_mannheim.korap.config;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import de.ids_mannheim.korap.oauth2.constant.OAuth2Scope;
import de.ids_mannheim.korap.oauth2.dao.AccessScopeDao;

/**
 * Initializes values in the database from kustvakt configuration.
 * 
 * @author margaretha
 *
 */
public class Initializator {

    private AccessScopeDao accessScopeDao;

    public Initializator (AccessScopeDao accessScopeDao) {
        this.accessScopeDao = accessScopeDao;
    }

    public void init () {
        setAccessScope();
    }

    private void setAccessScope () {
        OAuth2Scope[] enums = OAuth2Scope.values();
        Set<String> scopes = new HashSet<>(enums.length);
        for (OAuth2Scope s : enums) {
            scopes.add(s.toString());
        }
        accessScopeDao.storeAccessScopes(scopes);
    }
}
