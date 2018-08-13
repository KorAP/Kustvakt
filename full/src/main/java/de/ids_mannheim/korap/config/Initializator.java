package de.ids_mannheim.korap.config;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;

import de.ids_mannheim.korap.oauth2.constant.OAuth2Scope;
import de.ids_mannheim.korap.oauth2.dao.AccessScopeDao;

/**
 * Initializes values in the database from kustvakt configuration and
 * performs named VC caching.
 * 
 * @author margaretha
 *
 */
public class Initializator {

    @Autowired
    private AccessScopeDao accessScopeDao;
    @Autowired
    private NamedVCLoader loader;

    public Initializator () {}

    public void init () throws IOException {
        setInitialAccessScope();
        loader.loadVCToCache();
    }

    public void initTest () {
        setInitialAccessScope();
    }

    private void setInitialAccessScope () {
        OAuth2Scope[] enums = OAuth2Scope.values();
        Set<String> scopes = new HashSet<>(enums.length);
        for (OAuth2Scope s : enums) {
            scopes.add(s.toString());
        }
        accessScopeDao.storeAccessScopes(scopes);
    }
}
