package de.ids_mannheim.korap.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;

import com.fasterxml.jackson.databind.JsonNode;
import de.ids_mannheim.korap.utils.JsonUtils;

/**
 * @author diewald
 */
@Service
public class QueryReferenceService {


    public JsonNode searchQueryByName (String username,
                                       String refName,
                                       String createdBy) throws KustvaktException {

        String refCode = createdBy + "/" + refName;

        if (refCode.equals("system/emptyToken")) {
            return JsonUtils.readTree("{\"@type\":\"koral:token\"}");
        };

        throw new KustvaktException(
            StatusCodes.NO_RESOURCE_FOUND,
            "Query reference " + refCode + " is not found.",
            String.valueOf(refCode));
    }
};
