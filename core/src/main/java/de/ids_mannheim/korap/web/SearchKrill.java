// Connector to the Lucene Backend
package de.ids_mannheim.korap.web;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.store.MMapDirectory;

import de.ids_mannheim.korap.Krill;
import de.ids_mannheim.korap.KrillCollection;
import de.ids_mannheim.korap.KrillIndex;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.response.Match;
import de.ids_mannheim.korap.response.MetaFields;
import de.ids_mannheim.korap.response.Result;
import de.ids_mannheim.korap.util.QueryException;

/**
 * The SearchKrill class allows for searching in the
 * Lucene based Krill backend by applying KoralQuery.
 * 
 * @author Nils Diewald
 */
public class SearchKrill {
    private final static Logger jlog = LogManager
            .getLogger(SearchKrill.class);

    // Temporary - shouldn't be here.
    String indexDir = "/data/prep_corpus/index/";
    String i = "/Users/hanl/Projects/prep_corpus";
    String klinux10 = "/vol/work/hanl/indices";
    private KrillIndex index;
    
    /**
     * Constructor
     */
    // todo: use korap.config to get index location
    public SearchKrill (String path) {
    	
        try {
            if (path.equals(":temp:")) {
                this.index = new KrillIndex();
            }
            else {
                File f = new File(path);
                jlog.info("Loading index from " + path);
                if (!f.exists()) {
                    jlog.error("Index not found: " + path + "!");
                    System.exit(-1);
                }
                this.index = new KrillIndex(new MMapDirectory(Paths.get(path)));
            };
        }
        catch (IOException e) {
            jlog.error("Unable to loadSubTypes index:"+ e.getMessage());
        };
    };

    public KrillIndex getIndex () {
        return this.index;
    };


    /**
     * Search in the Lucene index.
     * 
     * @param json
     *            JSON-LD string with search and potential meta
     *            filters.
     */
    public String search (String json) {
        jlog.trace(json);
        if (this.index != null)
            return new Krill(json).apply(this.index).toJsonString();
        Result kr = new Result();
        kr.addError(601, "Unable to find index");
        return kr.toJsonString();
    };


    /**
     * Search in the Lucene index and return matches as token lists.
     * 
     * @param json
     *            JSON-LD string with search and potential meta
     *            filters.
     */
    @Deprecated
    public String searchTokenList (String json) {
        jlog.trace(json);
        if (this.index != null)
            return new Krill(json).apply(this.index).toTokenListJsonString();
        Result kr = new Result();
        kr.addError(601, "Unable to find index");
        return kr.toJsonString();
    };


    /**
     * Get info on a match - by means of a richly annotated html
     * snippet.
     * 
     * @param id
     *            match id
     * @param availabilityList 
     * @throws KustvaktException 
     */
    public String getMatch (String id, Pattern licensePattern) {
    	Match km;
        if (this.index != null) {
            try {
            	km = this.index.getMatch(id);
            	String availability = km.getAvailability();
            	if (licensePattern!=null && availability != null){
            		Matcher m = licensePattern.matcher(availability);
            		if (!m.matches()){
            		    jlog.debug("availability: "+availability);
                        if (availability.isEmpty()){
                            km.addError(StatusCodes.MISSING_ATTRIBUTE, 
                                    "Availability for "+ id +"is empty.", id);
                        }
            			km = new Match();
            			km.addError(StatusCodes.ACCESS_DENIED, 
            				"Retrieving match info with ID "+id+" is not allowed.", id);
            		}
            	}
            }
            catch (QueryException qe) {
                km = new Match();
                km.addError(qe.getErrorCode(), qe.getMessage());
            }
        }
        else{
        	km = new Match();
        	km.addError(601, "Unable to find index");
        }
        return km.toJsonString();
    };


	/*
	 * Retrieve the meta fields for a certain document
	 */
	public String getFields (String id) {
		MetaFields meta;

		// No index found
		if (this.index == null) {
        	meta = new MetaFields(id);
        	meta.addError(601, "Unable to find index");
		}

		// Index available
		else {

			//Get fields
			meta = this.index.getFields(id);
		};
		return meta.toJsonString();
	};


	
    public String getMatch (String id, List<String> foundries,
            List<String> layers, boolean includeSpans,
            boolean includeHighlights, boolean sentenceExpansion, 
            Pattern licensePattern) {
    	 Match km;
        if (this.index != null) {
            try {
            	km = this.index.getMatchInfo(id, "tokens", true, foundries,
                        layers, includeSpans, includeHighlights,
                        sentenceExpansion);
            	String availability = km.getAvailability();
            	
            	if (licensePattern !=null && availability != null){
            	    if (availability.isEmpty()){
            	        km.addError(StatusCodes.MISSING_ATTRIBUTE, 
                                "Availability for "+ id +"is empty.", id);
            	    }
            		Matcher m = licensePattern.matcher(availability);
            		if (!m.matches()){
            		    jlog.debug("pattern: "+ licensePattern.toString() + ", availability: "+availability);
            			km = new Match();
            			km.addError(StatusCodes.ACCESS_DENIED, 
            					"Retrieving match info with ID "+id+" is not allowed.", id);
            		}
            	}
            	
            }
            catch (QueryException qe) {
                km = new Match();
                km.addError(qe.getErrorCode(), qe.getMessage());
            }
        }
        else{
        	km = new Match();
        	km.addError(601, "Unable to find index");
        }
        return km.toJsonString();
    };


    /**
     * Get info on a match - by means of a richly annotated html
     * snippet.
     * 
     * @param id
     *            match id
     * @param foundry
     *            the foundry of interest - may be null
     * @param layer
     *            the layer of interest - may be null
     * @param includeSpans
     *            Should spans be included (or only token infos)?
     * @param includeHighlights
     *            Should highlight markup be included?
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
            }
            catch (QueryException qe) {
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
     * EM: might be changed later
     * 
     * @param json
     *            JSON-LD string with potential meta filters.
     */
    public String getStatistics (String json) {
        if (this.index == null) {
            return "{\"documents\" : -1, error\" : \"No index given\" }";
        };

		// Define a virtual corpus
		KrillCollection kc;
		if (json != null && !json.equals("")) {
			jlog.trace(json);

			// Create Virtual collection from json search
			kc = new KrillCollection(json);
		}

		// There is no json string defined
		else {

			// Create Virtual collection of everything
			kc = new KrillCollection();
		};

        // Set index
        kc.setIndex(this.index);
        long docs = 0, tokens = 0, sentences = 0, paragraphs = 0;
        // Get numbers from index (currently slow)
        try {
            docs = kc.numberOf("documents");
			if (docs > 0) {
				tokens = kc.numberOf("tokens");
				sentences = kc.numberOf("base/sentences");
				paragraphs = kc.numberOf("base/paragraphs");
			};
        }
        catch (IOException e) {
            e.printStackTrace();
        };
        // Build json response
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"documents\":").append(docs).append(",\"tokens\":")
                .append(tokens).append(",\"sentences\":").append(sentences)
                .append(",\"paragraphs\":").append(paragraphs).append("}");
        return sb.toString();
    };


    /**
     * Return the match identifier as a string.
     * This is a convenient method to deal with legacy instantiation
     * of the
     * code.
     */
    public String getMatchId (String corpusID, String docID, String textID,
            String matchID) {
        // Create a string representation of the match 
        StringBuilder sb = new StringBuilder();
        sb.append("match-").append(corpusID).append('/').append(docID)
                .append('/').append(textID).append('-').append(matchID);
        return sb.toString();
    };


    /**
     * Return the text sigle as a string.
     */
    public String getTextSigle (String corpusID, String docID, String textID) {
        // Create a string representation of the match 
        StringBuilder sb = new StringBuilder();
        sb.append(corpusID).append('/').append(docID)
                .append('/').append(textID);
        return sb.toString();
    };
};
