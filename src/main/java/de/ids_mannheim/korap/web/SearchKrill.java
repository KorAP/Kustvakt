// Connector to the Lucene Backend
package de.ids_mannheim.korap.web;

import com.fasterxml.jackson.databind.JsonNode;
import de.ids_mannheim.korap.Krill;
import de.ids_mannheim.korap.KrillCollection;
import de.ids_mannheim.korap.KrillIndex;
import de.ids_mannheim.korap.response.Match;
import de.ids_mannheim.korap.response.Result;
import de.ids_mannheim.korap.util.QueryException;
import de.ids_mannheim.korap.utils.JsonUtils;
import de.ids_mannheim.korap.utils.KustvaktLogger;
import org.apache.lucene.store.MMapDirectory;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * The SearchKrill class allows for searching in the
 * Lucene based Krill backend by applying KoralQuery.
 *
 * @author Nils Diewald
 */
public class SearchKrill {
    private final static Logger qlog = KustvaktLogger.initiate("queryLogger");
    private final static Logger log = KustvaktLogger.initiate(SearchKrill.class);
    // Temporary
    String indexDir = "/data/prep_corpus/index/";
    String i = "/Users/hanl/Projects/prep_corpus";
    String klinux10 = "/vol/work/hanl/indices";

    private KrillIndex index;

    /**
     * Constructor
     */
    // todo: use korap.config to get index location
    public SearchKrill(String path) {
        try {
            File f = new File(path);
            log.info("Loading index from " + path);
            if (!f.exists()) {
                KustvaktLogger.ERROR_LOGGER.error("Index not found!");
                System.exit(-1);
            }
            this.index = new KrillIndex(new MMapDirectory(new File(path)));
        } catch (IOException e) {
            KustvaktLogger.ERROR_LOGGER
                    .error("Unable to loadSubTypes index: {}", e.getMessage());
        }
    }

    /**
     * Search in the Lucene index.
     *
     * @param json JSON-LD string with search and potential meta filters.
     */
    public String search(String json) {
        qlog.trace(json);
        if (this.index != null)
            return new Krill(json).apply(this.index).toJsonString();

        Result kr = new Result();
        kr.addError(601, "Unable to find index");
        return kr.toJsonString();
    };

    /**
     * Search in the Lucene index and return matches as token lists.
     *
     * @param json JSON-LD string with search and potential meta filters.
     */
    @Deprecated
    public String searchTokenList(String json) {
        qlog.trace(json);
        if (this.index != null)
            return new Krill(json).apply(this.index).toTokenListJsonString();

        Result kr = new Result();
        kr.addError(601, "Unable to find index");
        return kr.toJsonString();
    };

    /**
     * Get info on a match - by means of a richly annotated html snippet.
     *
     * @param id match id
     */
    public String getMatch(String id) {

        if (this.index != null) {
            try {
                return this.index.getMatch(id).toJsonString();
            } catch (QueryException qe) {
                Match km = new Match();
                km.addError(qe.getErrorCode(), qe.getMessage());
                return km.toJsonString();
            }
        };

        Match km = new Match();
        km.addError(601, "Unable to find index");
        return km.toJsonString();
    };

    public String getMatch(String id, List<String> foundries,
            List<String> layers, boolean includeSpans,
            boolean includeHighlights, boolean sentenceExpansion) {
        if (this.index != null) {
            try {

                return this.index
                        .getMatchInfo(id, "tokens", true, foundries, layers,
                                includeSpans, includeHighlights,
                                sentenceExpansion).toJsonString();
            } catch (QueryException qe) {
                Match km = new Match();
                km.addError(qe.getErrorCode(), qe.getMessage());
                return km.toJsonString();
            }
        };

        Match km = new Match();
        km.addError(601, "Unable to find index");
        return km.toJsonString();
    };


    /**
     * Get info on a match - by means of a richly annotated html snippet.
     *
     * @param id                match id
     * @param foundry           the foundry of interest - may be null
     * @param layer             the layer of interest - may be null
     * @param includeSpans      Should spans be included (or only token infos)?
     * @param includeHighlights Should highlight markup be included?
     */
    public String getMatch (String id, String foundry, String layer,
            boolean includeSpans, boolean includeHighlights,
            boolean sentenceExpansion) {

        if (this.index != null) {
            try {

		/*
          For multiple foundries/layers use
		  String idString,
		  "tokens",
		  true,
		  ArrayList<String> foundry,
		  ArrayList<String> layer,
		  boolean includeSpans,
		  boolean includeHighlights,
		  boolean extendToSentence
		 */

                return this.index.getMatchInfo(id, "tokens", foundry, layer,
                        includeSpans, includeHighlights, sentenceExpansion)
                        .toJsonString();
            } catch (QueryException qe) {
                Match km = new Match();
                km.addError(qe.getErrorCode(), qe.getMessage());
                return km.toJsonString();
            }
        };

        Match km = new Match();
        km.addError(601, "Unable to find index");
        return km.toJsonString();
    };

    /**
     * Get statistics on (virtual) collections.
     *
     * @param json JSON-LD string with potential meta filters.
     */
    @Deprecated
    public String getStatisticsLegacy (JsonNode json) throws QueryException {
        qlog.trace(JsonUtils.toJSON(json));
        System.out.println("THE NODE BEFORE GETTING STATISTICS " + json);

        if (this.index == null) {
            return "{\"documents\" : -1, error\" : \"No index given\" }";
        }

        // Create Virtula VCollection from json search
        KrillCollection kc = new KrillCollection();
        kc.fromJsonLegacy(json);

        // Set index
        kc.setIndex(this.index);

        long docs = 0,
            tokens = 0,
            sentences = 0,
            paragraphs = 0;

        // Get numbers from index (currently slow)
        try {
            docs = kc.numberOf("documents");
            tokens = kc.numberOf("tokens");
            sentences = kc.numberOf("sentences");
            paragraphs = kc.numberOf("paragraphs");
        } catch (IOException e) {
            e.printStackTrace();
        }


	/*
    KorAPLogger.ERROR_LOGGER.error("Unable to retrieve statistics: {}", e.getMessage());
	*/

        // Build json response
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"documents\":").append(docs).append(",\"tokens\":")
                .append(tokens).append(",\"sentences\":").append(sentences)
                .append(",\"paragraphs\":").append(paragraphs).append("}");
        return sb.toString();
    }

    /**
     * Get statistics on (virtual) collections.
     *
     * @param json JSON-LD string with potential meta filters.
     */
    @Deprecated
    public String getStatistics (String json) {
        qlog.trace(json);

        if (this.index == null) {
            return "{\"documents\" : -1, error\" : \"No index given\" }";
        }

        // Create Virtual collection from json search
        KrillCollection kc = new KrillCollection(json);

        // Set index
        kc.setIndex(this.index);

        long docs = 0,
                tokens = 0,
                sentences = 0,
                paragraphs = 0;

        // Get numbers from index (currently slow)
        try {
            docs = kc.numberOf("documents");
            tokens = kc.numberOf("tokens");
            sentences = kc.numberOf("sentences");
            paragraphs = kc.numberOf("paragraphs");
        } catch (IOException e) {
            e.printStackTrace();
        }


	/*
    KorAPLogger.ERROR_LOGGER.error("Unable to retrieve statistics: {}", e.getMessage());
	*/

        // Build json response
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"documents\":").append(docs).append(",\"tokens\":")
                .append(tokens).append(",\"sentences\":").append(sentences)
                .append(",\"paragraphs\":").append(paragraphs).append("}");
        return sb.toString();
    }

    /**
     * Get set relations on field terms of (virtual) collections.
     *
     * @param json JSON-LD string with potential meta filters.
     */
    @Deprecated
    public String getTermRelation (String json, String field) {
        qlog.trace(json);

        if (this.index == null) {
            return "{\"documents\" : -1, \"error\" : \"No index given\" }";
        }

        // Create Virtula VCollection from json search
        KrillCollection kc = new KrillCollection(json);

        // Set index
        kc.setIndex(this.index);
        long v = 0L;
        try {
            v = kc.numberOf("documents");
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            // Get term relations as a json string
            return kc.getTermRelationJSON(field);
        } catch (IOException e) {
            KustvaktLogger.ERROR_LOGGER
                    .error("Unable to retrieve term relations: {}",
                            e.getMessage());
            return "{\"documents\" : -1, \"error\" : \"IO error\" }";
        }
    }

    public String getMatchId (String type, String docid, String tofrom) {
        return new StringBuilder().append("match-").append(type).append("!")
                .append(type).append("_").append(docid).append("-")
                .append(tofrom).toString();
    }

}
