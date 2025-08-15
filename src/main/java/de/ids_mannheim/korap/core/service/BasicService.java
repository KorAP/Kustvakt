package de.ids_mannheim.korap.core.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.JsonNode;

import de.ids_mannheim.korap.authentication.AuthenticationManager;
import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.user.User;
import de.ids_mannheim.korap.utils.JsonUtils;
import de.ids_mannheim.korap.utils.KoralCollectionQueryBuilder;
import de.ids_mannheim.korap.web.SearchKrill;
import jakarta.ws.rs.core.HttpHeaders;

public class BasicService {
	
	@Autowired
	protected SearchKrill searchKrill;
    @Autowired
    protected KustvaktConfiguration config;
    
	@Autowired
    protected AuthenticationManager authenticationManager;
	
	protected static final boolean DEBUG = false;
	
    protected String combineMultipleCorpusQuery (List<String> cqList) {
        String combinedCorpusQuery = null;
        if (cqList != null && cqList.size() > 0) {
            combinedCorpusQuery = cqList.get(0);
            for (int i = 1; i < cqList.size(); i++) {
                combinedCorpusQuery += "&" + cqList.get(i);
            }
        }
        return combinedCorpusQuery;
    }
    
    protected User createUser (String username, HttpHeaders headers)
            throws KustvaktException {
        User user = authenticationManager.getUser(username);
        authenticationManager.setAccessAndLocation(user, headers);
//        if (DEBUG) {
//            if (user != null) {
//                jlog.debug("Debug: user location=" + user.locationtoString()
//                        + ", access=" + user.getCorpusAccess());
//            }
//        }
        return user;
    }
    
	protected String buildKoralQueryFromCorpusQuery (List<String> cqList,
			double apiVersion) throws KustvaktException {
		KoralCollectionQueryBuilder builder = new KoralCollectionQueryBuilder(
				apiVersion);
		String cq = combineMultipleCorpusQuery(cqList);
		String json = null;
		if (cq != null && !cq.isEmpty()) {
			builder.with(cq);
			json = builder.toJSON();
		}

		if (json != null) {
			checkVC(json);
		}
		return json;
	}
	
    protected void checkVC (String json) throws KustvaktException {
        JsonNode node = JsonUtils.readTree(json);
        node = node.at("/collection");
        if (node.has("ref")) {
            String vcName = node.path("ref").asText();
            if (vcName.contains("/")) {
                String[] names = vcName.split("/");
                if (names.length == 2) {
                    vcName = names[1];
                }
            }

            String vcInCaching = config.getVcInCaching();
            if (vcName.equals(vcInCaching)) {
                throw new KustvaktException(
                        de.ids_mannheim.korap.exceptions.StatusCodes.CACHING_VC,
                        "VC is currently busy and unaccessible due to "
                                + "caching process",
                        node.get("ref").asText());
            }
        }
    }
}
