package de.ids_mannheim.korap.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.constant.VirtualCorpusType;
import de.ids_mannheim.korap.dao.VirtualCorpusDao;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.query.serialize.QuerySerializer;
import de.ids_mannheim.korap.user.User;
import de.ids_mannheim.korap.user.User.CorpusAccess;
import de.ids_mannheim.korap.utils.JsonUtils;
import de.ids_mannheim.korap.utils.KoralCollectionQueryBuilder;
import de.ids_mannheim.korap.web.SearchKrill;
import de.ids_mannheim.korap.web.input.VirtualCorpusFromJson;

@Service
public class VirtualCorpusService {

    private static Logger jlog =
            LoggerFactory.getLogger(VirtualCorpusService.class);

    @Autowired
    private VirtualCorpusDao dao;

    @Autowired
    private SearchKrill krill;

    @Autowired
    private KustvaktConfiguration config;

    public void storeVC (VirtualCorpusFromJson vc, User user)
            throws KustvaktException {

        // EM: how about VirtualCorpusType.PUBLISHED?
        if (vc.getType().equals(VirtualCorpusType.PREDEFINED)
                && !user.isAdmin()) {
            throw new KustvaktException(StatusCodes.UNAUTHORIZED_OPERATION,
                    "Unauthorized operation for user: " + user.getUsername(),
                    user.getUsername());
        }

        String koralQuery = serializeCollectionQuery(vc.getCollectionQuery());
        CorpusAccess requiredAccess = determineRequiredAccess(koralQuery);

        dao.createVirtualCorpus(vc.getName(), vc.getType(), requiredAccess,
                koralQuery, vc.getDefinition(), vc.getDescription(),
                vc.getStatus(), vc.getCreatedBy());

        // EM: should this return anything?
    }

    private String serializeCollectionQuery (String collectionQuery)
            throws KustvaktException {
        QuerySerializer serializer = new QuerySerializer();
        serializer.setCollection(collectionQuery);
        String koralQuery;
        try {
            koralQuery = serializer.convertCollectionToJson();
        }
        catch (JsonProcessingException e) {
            throw new KustvaktException(StatusCodes.INVALID_ARGUMENT,
                    "Invalid argument: " + collectionQuery, collectionQuery);
        }
        jlog.debug(koralQuery);
        return koralQuery;
    }

    private CorpusAccess determineRequiredAccess (String koralQuery)
            throws KustvaktException {

        if (findDocWithLicense(koralQuery, config.getAllOnlyRegex())) {
            return CorpusAccess.ALL;
        }
        else if (findDocWithLicense(koralQuery, config.getPublicOnlyRegex())) {
            return CorpusAccess.PUB;
        }
        else {
            return CorpusAccess.FREE;
        }
    }

    private boolean findDocWithLicense (String koralQuery, String license)
            throws KustvaktException {
        KoralCollectionQueryBuilder koral = new KoralCollectionQueryBuilder();
        koral.setBaseQuery(koralQuery);
        koral.with("availability=/" + license + "/");
        String json = koral.toJSON();

        String statistics = krill.getStatistics(json);
        JsonNode node = JsonUtils.readTree(statistics);
        int numberOfDoc = node.at("/documents").asInt();
        jlog.debug("License: " + license + ", number of docs: " + numberOfDoc);
        return (numberOfDoc > 0) ? true : false;
    }
}
