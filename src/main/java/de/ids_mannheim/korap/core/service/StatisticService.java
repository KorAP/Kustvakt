package de.ids_mannheim.korap.core.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.rewrite.RewriteHandler;
import de.ids_mannheim.korap.user.User;
import jakarta.ws.rs.core.HttpHeaders;

@Service
public class StatisticService extends BasicService {

	@Autowired
	private RewriteHandler statisticsRewriteHandler;
	
	public String retrieveStatisticsForCorpusQuery (List<String> cqList,
			String username, HttpHeaders headers) throws KustvaktException {

		String json = buildKoralQueryFromCorpusQuery(cqList);
		//System.out.println("Before:" + json + "\n");
		if (!cqList.isEmpty() && !combineMultipleCorpusQuery(cqList).isEmpty()) {
			User user = createUser(username, headers);
			json = statisticsRewriteHandler.processQuery(json, user);
		}
		//System.out.println("After:" + json);
		String stats = searchKrill.getStatistics(json);

		if (stats.contains("-1")) {
			throw new KustvaktException(StatusCodes.NO_RESULT_FOUND);
		}
		return stats;
	}

    public String retrieveStatisticsForKoralQuery (String koralQuery)
            throws KustvaktException {
        String stats = null;
        if (koralQuery != null && !koralQuery.isEmpty()) {
            checkVC(koralQuery);
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
