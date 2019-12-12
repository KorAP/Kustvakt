package de.ids_mannheim.korap.service;

import java.util.List;

public class BasicService {

    protected String combineMultipleCorpusQuery (List<String> cqList) {
        String combinedCorpusQuery = null;
        if (cqList!=null && cqList.size() > 0) {
            combinedCorpusQuery = cqList.get(0);
            for (int i = 1; i < cqList.size(); i++) {
                combinedCorpusQuery += "&" + cqList.get(i);
            }
        }
        return combinedCorpusQuery;
    }
}
