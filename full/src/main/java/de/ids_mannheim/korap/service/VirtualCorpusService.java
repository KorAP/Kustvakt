package de.ids_mannheim.korap.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import de.ids_mannheim.korap.config.FullConfiguration;
import de.ids_mannheim.korap.constant.PredefinedUserGroup;
import de.ids_mannheim.korap.constant.VirtualCorpusAccessStatus;
import de.ids_mannheim.korap.constant.VirtualCorpusType;
import de.ids_mannheim.korap.dao.VirtualCorpusAccessDao;
import de.ids_mannheim.korap.dao.VirtualCorpusDao;
import de.ids_mannheim.korap.dto.VirtualCorpusDto;
import de.ids_mannheim.korap.dto.converter.VirtualCorpusConverter;
import de.ids_mannheim.korap.entity.UserGroup;
import de.ids_mannheim.korap.entity.VirtualCorpus;
import de.ids_mannheim.korap.entity.VirtualCorpusAccess;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.interfaces.AuthenticationManagerIface;
import de.ids_mannheim.korap.query.serialize.QuerySerializer;
import de.ids_mannheim.korap.user.User;
import de.ids_mannheim.korap.user.User.CorpusAccess;
import de.ids_mannheim.korap.utils.JsonUtils;
import de.ids_mannheim.korap.utils.KoralCollectionQueryBuilder;
import de.ids_mannheim.korap.utils.ParameterChecker;
import de.ids_mannheim.korap.web.SearchKrill;
import de.ids_mannheim.korap.web.controller.VirtualCorpusController;
import de.ids_mannheim.korap.web.input.VirtualCorpusJson;

/** VirtualCorpusService handles the logic behind {@link VirtualCorpusController}. 
 *  It communicates with {@link VirtualCorpusDao} and returns 
 *  {@link VirtualCorpusDto} to {@link VirtualCorpusController}.
 * 
 * @author margaretha
 *
 */
@Service
public class VirtualCorpusService {

    private static Logger jlog =
            LoggerFactory.getLogger(VirtualCorpusService.class);

    @Autowired
    private VirtualCorpusDao vcDao;
    @Autowired
    private VirtualCorpusAccessDao accessDao;
    @Autowired
    private UserGroupService userGroupService;
    @Autowired
    private SearchKrill krill;
    @Autowired
    private FullConfiguration config;
    @Autowired
    private AuthenticationManagerIface authManager;
    @Autowired
    private VirtualCorpusConverter converter;

    public int storeVC (VirtualCorpusJson vc, String username)
            throws KustvaktException {

        ParameterChecker.checkStringValue(vc.getName(), "name");
        ParameterChecker.checkObjectValue(vc.getType(), "type");
        ParameterChecker.checkStringValue(vc.getCorpusQuery(), "corpusQuery");
        ParameterChecker.checkStringValue(vc.getCreatedBy(), "createdBy");

        User user = authManager.getUser(username);

        if (vc.getType().equals(VirtualCorpusType.PREDEFINED)
                && !user.isAdmin()) {
            throw new KustvaktException(StatusCodes.AUTHORIZATION_FAILED,
                    "Unauthorized operation for user: " + username, username);
        }

        String koralQuery = serializeCorpusQuery(vc.getCorpusQuery());
        CorpusAccess requiredAccess = determineRequiredAccess(koralQuery);

        int vcId = vcDao.createVirtualCorpus(vc.getName(), vc.getType(),
                requiredAccess, koralQuery, vc.getDefinition(),
                vc.getDescription(), vc.getStatus(), vc.getCreatedBy());

        if (vc.getType().equals(VirtualCorpusType.PUBLISHED)) {
            publishVC(vcId);
        }
        // EM: should this return anything?
        return vcId;
    }

    public void editVC (VirtualCorpusJson vcJson, String username)
            throws KustvaktException {

        ParameterChecker.checkIntegerValue(vcJson.getId(), "id");
        VirtualCorpus vc = vcDao.retrieveVCById(vcJson.getId());

        User user = authManager.getUser(username);

        if (!username.equals(vc.getCreatedBy()) && !user.isAdmin()) {
            throw new KustvaktException(StatusCodes.AUTHORIZATION_FAILED,
                    "Unauthorized operation for user: " + username, username);
        }

        String koralQuery = null;
        CorpusAccess requiredAccess = null;
        if (vcJson.getCorpusQuery() != null
                && vcJson.getCorpusQuery().isEmpty()) {
            koralQuery = serializeCorpusQuery(vcJson.getCorpusQuery());
            requiredAccess = determineRequiredAccess(koralQuery);
        }

        vcDao.editVirtualCorpus(vc, vcJson.getName(), vcJson.getType(),
                requiredAccess, koralQuery, vcJson.getDefinition(),
                vcJson.getDescription(), vcJson.getStatus());

        if (!vc.getType().equals(VirtualCorpusType.PUBLISHED)
                && vcJson.getType() != null
                && vcJson.getType().equals(VirtualCorpusType.PUBLISHED)) {
            publishVC(vcJson.getId());
        }
    }

    private void publishVC (int vcId) throws KustvaktException {

        // check if hidden access exists
        if (!accessDao.hasHiddenAccess(vcId)) {
            // assign hidden access for all users
            VirtualCorpus vc = vcDao.retrieveVCById(vcId);
            UserGroup all = userGroupService.retrieveAllUserGroup();
            accessDao.addAccessToVC(vc, all, "system",
                    VirtualCorpusAccessStatus.HIDDEN);

            // create and assign a hidden group
            int groupId = userGroupService.createAutoHiddenGroup(vcId);
            UserGroup autoHidden =
                    userGroupService.retrieveUserGroupById(groupId);
            accessDao.addAccessToVC(vc, autoHidden, "system",
                    VirtualCorpusAccessStatus.HIDDEN);
        }
        else {
            jlog.error("Cannot publish VC with id: " + vcId
                    + ". There have been hidden accesses for the VC already.");
        }
    }

    private String serializeCorpusQuery (String corpusQuery)
            throws KustvaktException {
        QuerySerializer serializer = new QuerySerializer();
        serializer.setCollection(corpusQuery);
        String koralQuery;
        try {
            koralQuery = serializer.convertCollectionToJson();
        }
        catch (JsonProcessingException e) {
            throw new KustvaktException(StatusCodes.INVALID_ARGUMENT,
                    "Invalid argument: " + corpusQuery, corpusQuery);
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

    public List<VirtualCorpusDto> listOwnerVC (String username)
            throws KustvaktException {
        List<VirtualCorpus> vcList = vcDao.retrieveOwnerVC(username);
        return createVCDtos(vcList);
    }

    public List<VirtualCorpusDto> listVCByUser (String username)
            throws KustvaktException {
        Set<VirtualCorpus> vcSet = vcDao.retrieveVCByUser(username);
        return createVCDtos(vcSet);
    }

    private ArrayList<VirtualCorpusDto> createVCDtos (
            Collection<VirtualCorpus> vcList) throws KustvaktException {
        ArrayList<VirtualCorpusDto> dtos = new ArrayList<>(vcList.size());
        VirtualCorpus vc;
        Iterator<VirtualCorpus> i = vcList.iterator();
        while (i.hasNext()) {
            vc = i.next();
            String json = vc.getCorpusQuery();
            String statistics = krill.getStatistics(json);
            VirtualCorpusDto vcDto =
                    converter.createVirtualCorpusDto(vc, statistics);
            dtos.add(vcDto);
        }
        return dtos;
    }

    /** Only admin and the owner of the virtual corpus are allowed to 
     *  delete a virtual corpus.
     *  
     * @param username username
     * @param vcId virtual corpus id
     * @throws KustvaktException
     */
    public void deleteVC (String username, int vcId) throws KustvaktException {

        User user = authManager.getUser(username);
        VirtualCorpus vc = vcDao.retrieveVCById(vcId);

        if (user.isAdmin() || vc.getCreatedBy().equals(username)) {
            vcDao.deleteVirtualCorpus(vcId);
        }
        else {
            throw new KustvaktException(StatusCodes.AUTHORIZATION_FAILED,
                    "Unauthorized operation for user: " + username, username);
        }
    }

    public List<VirtualCorpusAccess> retrieveVCAccess (int vcId)
            throws KustvaktException {
        return accessDao.retrieveAccessByVC(vcId);
    }
}
