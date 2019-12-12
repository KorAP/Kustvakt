package de.ids_mannheim.korap.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.response.Notifications;
import de.ids_mannheim.korap.utils.JsonUtils;
import de.ids_mannheim.korap.utils.KoralCollectionQueryBuilder;
import de.ids_mannheim.korap.web.SearchKrill;

@Service
public class StatisticService extends BasicService {

    @Autowired
    private SearchKrill searchKrill;
    @Autowired
    private KustvaktConfiguration config;

    public String retrieveStatisticsForCorpusQuery (List<String> cqList,
            boolean isDeprecated) throws KustvaktException {

        KoralCollectionQueryBuilder builder = new KoralCollectionQueryBuilder();
        String cq = combineMultipleCorpusQuery(cqList);
        String json = null;
        if (cq != null && !cq.isEmpty()) {
            builder.with(cq);
            json = builder.toJSON();
        }

        if (json != null) {
            checkVC(json);
        }
        String stats = searchKrill.getStatistics(json);

        if (isDeprecated) {
            Notifications n = new Notifications();
            n.addWarning(StatusCodes.DEPRECATED_PARAMETER,
                    "Parameter corpusQuery is deprecated in favor of cq.");
            ObjectNode warning = (ObjectNode) n.toJsonNode();
            ObjectNode node = (ObjectNode) JsonUtils.readTree(stats);
            node.setAll(warning);
            stats = node.toString();
        }
        
        if (stats.contains("-1")) {
            throw new KustvaktException(StatusCodes.NO_RESULT_FOUND);
        }
        return stats;
    }

    private void checkVC (String json) throws KustvaktException {
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
}
