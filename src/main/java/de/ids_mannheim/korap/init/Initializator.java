package de.ids_mannheim.korap.init;

import java.io.IOException;
import java.util.EnumSet;

import org.springframework.beans.factory.annotation.Autowired;

import de.ids_mannheim.korap.annotation.ResourceParser;
import de.ids_mannheim.korap.cache.VirtualCorpusCache;
import de.ids_mannheim.korap.config.FullConfiguration;
import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.constant.OAuth2Scope;
import de.ids_mannheim.korap.constant.QueryType;
import de.ids_mannheim.korap.constant.ResourceType;
import de.ids_mannheim.korap.dao.AdminDao;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.oauth2.dao.AccessScopeDao;
import de.ids_mannheim.korap.oauth2.service.OAuth2InitClientService;
import de.ids_mannheim.korap.service.QueryService;
import de.ids_mannheim.korap.user.KorAPUser;
import de.ids_mannheim.korap.util.QueryException;
import de.ids_mannheim.korap.web.input.QueryJson;

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
    private AdminDao adminDao;
    @Autowired
    private NamedVCLoader vcLoader;
    @Autowired
    private ResourceParser resourceParser;
    @Autowired
    private FullConfiguration config;
    @Autowired
    private OAuth2InitClientService clientService;
    @Autowired
    private QueryService queryService;

    public Initializator () {}

    public void init () throws IOException, QueryException, KustvaktException {
        VirtualCorpusCache.CACHE_LOCATION = KustvaktConfiguration.DATA_FOLDER
                + "/vc-cache";
        setInitialAccessScope();
        resourceParser.run();

        if (config.createInitialSuperClient()) {
            clientService.createInitialSuperClient(
                    OAuth2InitClientService.OUTPUT_FILENAME);
        }

        Thread t = new Thread(vcLoader);
        t.start();
    }

	public void initTest ()
			throws IOException, KustvaktException, QueryException {
		VirtualCorpusCache.CACHE_LOCATION = KustvaktConfiguration.DATA_FOLDER
				+ "/vc-cache";
		setInitialAccessScope();
		if (config.createInitialSuperClient()) {
			clientService.createInitialTestSuperClient();
		}
		vcLoader.loadVCToCache("system-vc", "/vc/system-vc.jsonld");
		adminDao.addAccount(new KorAPUser("admin"));
		
		QueryJson q = new QueryJson();
		q.setType(ResourceType.SYSTEM);
		q.setQueryLanguage("poliqarp");
		q.setQuery("[]");
		q.setDescription("\"system\" query");
		q.setQueryType(QueryType.QUERY);
		queryService.handlePutRequest("system", "system", "system-q", q);
	}

    public void initResourceTest () throws IOException, KustvaktException {
        setInitialAccessScope();
        resourceParser.run();
    }

    private void setInitialAccessScope () {
        EnumSet<OAuth2Scope> scopes = EnumSet.allOf(OAuth2Scope.class);
        accessScopeDao.storeAccessScopes(scopes);
    }
}
