package de.ids_mannheim.korap.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import de.ids_mannheim.korap.config.FullConfiguration;
import de.ids_mannheim.korap.constant.VirtualCorpusType;
import de.ids_mannheim.korap.dao.VirtualCorpusDao;
import de.ids_mannheim.korap.dto.VirtualCorpusDto;
import de.ids_mannheim.korap.dto.converter.VirtualCorpusConverter;
import de.ids_mannheim.korap.entity.VirtualCorpus;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.interfaces.AuthenticationManagerIface;
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
    private FullConfiguration config;
    @Autowired
    private AuthenticationManagerIface authManager;
    @Autowired
    private VirtualCorpusConverter converter;

    public void storeVC (VirtualCorpusFromJson vc, String username)
            throws KustvaktException {

        User user = authManager.getUser(username);
        // EM: how about VirtualCorpusType.PUBLISHED?
        if (vc.getType().equals(VirtualCorpusType.PREDEFINED)
                && !user.isAdmin()) {
            throw new KustvaktException(StatusCodes.AUTHORIZATION_FAILED,
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

    public List<VirtualCorpusDto> retrieveUserVC (String username)
            throws KustvaktException {

        Set<VirtualCorpus> vcs = dao.retrieveVCByUser(username);
        ArrayList<VirtualCorpusDto> dtos = new ArrayList<>(vcs.size());
        
        for (VirtualCorpus vc : vcs) {
            String json = vc.getCollectionQuery();
            String statistics = krill.getStatistics(json);
            VirtualCorpusDto vcDto = converter.createVirtualCorpusDto(vc, statistics);
            dtos.add(vcDto);
        }
        return dtos;
    }
}
