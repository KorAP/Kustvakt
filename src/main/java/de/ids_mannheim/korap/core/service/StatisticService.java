package de.ids_mannheim.korap.core.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import de.ids_mannheim.korap.constant.QueryType;
import de.ids_mannheim.korap.entity.QueryDO;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.rewrite.RewriteHandler;
import de.ids_mannheim.korap.server.KustvaktBaseServer;
import de.ids_mannheim.korap.service.QueryServiceInterface;
import de.ids_mannheim.korap.user.User;
import de.ids_mannheim.korap.utils.ServiceInfo;
import jakarta.ws.rs.core.HttpHeaders;

@Service
public class StatisticService extends BasicService {

	@Autowired
	private RewriteHandler statisticsRewriteHandler;
	@Autowired
	private QueryServiceInterface queryService;
	
	public String retrieveStatisticsForCorpusQuery (List<String> cqList,
			String username, HttpHeaders headers, double apiVersion) 
					throws KustvaktException {
		
		// Check for single VC reference
		if (!KustvaktBaseServer.kargs.isLite() && cqList.size() == 1) {
			String cq = cqList.get(0);
			if(cq.startsWith("referTo") && !cq.contains("&") && !cq.contains("|")) {
				QueryDO vc = retrieveVCReference(cqList.get(0), username);
    	        if (vc != null) {
    	        	String vcStatistics = vc.getStatistics();
    	        	if(vcStatistics != null && !vcStatistics.isEmpty()) {
    	        		return vcStatistics;
    	        	}
    	        }
			}
		}
		
		String json = buildKoralQueryFromCorpusQuery(cqList, apiVersion);
//		System.out.println("Before:" + json + "\n");
		if (!cqList.isEmpty() && !combineMultipleCorpusQuery(cqList).isEmpty()) {
			User user = createUser(username, headers);
			json = statisticsRewriteHandler.processQuery(json, user, apiVersion);
		}
//		System.out.println("After:" + json);
		
		String stats = searchKrill.getStatistics(json);

		if (stats.contains("-1")) {
			throw new KustvaktException(StatusCodes.NO_RESULT_FOUND);
		}
		return stats;
	}
	
	private QueryDO retrieveVCReference (String ref, String username) 
			throws KustvaktException {
		// expect something like: referTo "owner/vcName" or referTo "vcName"
		String[] cqParts = ref.split(" ");
		String vcOwner = "system";
		String vcName = "";
		if (cqParts.length > 1) {
			vcName = cqParts[1];
			// remove surrounding quotes, if any
			if (vcName.startsWith("\"") && vcName.endsWith("\"")) {
				vcName = vcName.substring(1, vcName.length() - 1);
			}
			if (vcName.contains("/")) {
				String[] names = vcName.split("/");
				if (names.length == 2) {
					vcOwner = names[0]; // may not be "system"
					vcName = names[1];
				}
			}
		}
		return queryService.searchQueryByName(username, vcName, vcOwner,
				QueryType.VIRTUAL_CORPUS);
	}
	
    public String retrieveStatisticsForKoralQuery (String koralQuery, 
    		double apiVersion)
            throws KustvaktException {
        String stats = null;
        if (koralQuery != null && !koralQuery.isEmpty()) {
            checkVC(koralQuery, apiVersion);
            stats = searchKrill.getStatistics(koralQuery);
        }
        else {
            stats = searchKrill.getStatistics(null);
        }

        if (stats.contains("-1")) {
            throw new KustvaktException(StatusCodes.NO_RESULT_FOUND);
        }
        return stats;
    }

    /**
     * Return the fingerprint of the latest index revision.
     */
    public String getIndexFingerprint () {
        return searchKrill.getIndexFingerprint();
    }
}