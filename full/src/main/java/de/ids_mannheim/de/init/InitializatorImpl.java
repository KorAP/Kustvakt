package de.ids_mannheim.de.init;

import java.io.IOException;
import java.util.EnumSet;

import org.springframework.beans.factory.annotation.Autowired;

import de.ids_mannheim.korap.annotation.AnnotationParser;
import de.ids_mannheim.korap.annotation.FreeResourceParser;
import de.ids_mannheim.korap.config.NamedVCLoader;
import de.ids_mannheim.korap.constant.OAuth2Scope;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.oauth2.dao.AccessScopeDao;
import de.ids_mannheim.korap.util.QueryException;

/**
 * Initializes values in the database from kustvakt configuration and
 * performs named VC caching.
 * 
 * @author margaretha
 *
 */
public class InitializatorImpl implements Initializator {

    @Autowired
    private AccessScopeDao accessScopeDao;
    @Autowired
    private NamedVCLoader loader;
    @Autowired
    private AnnotationParser annotationParser;
    @Autowired
    private FreeResourceParser resourceParser;
    
    public InitializatorImpl () {}

    /* (non-Javadoc)
     * @see de.ids_mannheim.de.init.Initializator#init()
     */
    @Override
    public void init () throws IOException, QueryException, KustvaktException {
        setInitialAccessScope();
        loader.loadVCToCache();
    }

    public void initAnnotation ()
            throws IOException, QueryException, KustvaktException {
        setInitialAccessScope();
        loader.loadVCToCache();
        annotationParser.run();
        resourceParser.run();
    }

    /* (non-Javadoc)
     * @see de.ids_mannheim.de.init.Initializator#initTest()
     */
    @Override
    public void initTest () {
        setInitialAccessScope();
    }
    
    public void initAnnotationTest () throws IOException, KustvaktException {
        setInitialAccessScope();
        annotationParser.run();
        resourceParser.run();
    }

    private void setInitialAccessScope () {
        EnumSet<OAuth2Scope> scopes = EnumSet.allOf(OAuth2Scope.class);
        accessScopeDao.storeAccessScopes(scopes);
    }
}
