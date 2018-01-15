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
import de.ids_mannheim.korap.constant.VirtualCorpusAccessStatus;
import de.ids_mannheim.korap.constant.VirtualCorpusType;
import de.ids_mannheim.korap.dao.VirtualCorpusAccessDao;
import de.ids_mannheim.korap.dao.VirtualCorpusDao;
import de.ids_mannheim.korap.dto.VirtualCorpusDto;
import de.ids_mannheim.korap.dto.converter.VirtualCorpusConverter;
import de.ids_mannheim.korap.entity.Role;
import de.ids_mannheim.korap.entity.UserGroup;
import de.ids_mannheim.korap.entity.UserGroupMember;
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

        if (vc.getCreatedBy().equals(username) || user.isAdmin()) {
            vcDao.deleteVirtualCorpus(vcId);
        }
        else {
            throw new KustvaktException(StatusCodes.AUTHORIZATION_FAILED,
                    "Unauthorized operation for user: " + username, username);
        }
    }

    public void editVC (VirtualCorpusJson vcJson, String username)
            throws KustvaktException {

        VirtualCorpus vc = vcDao.retrieveVCById(vcJson.getId());
        editVC(vc, vcJson, username);
    }

    public void editVC (VirtualCorpus vc, VirtualCorpusJson vcJson,
            String username) throws KustvaktException {
        ParameterChecker.checkIntegerValue(vcJson.getId(), "id");
        User user = authManager.getUser(username);

        if (!username.equals(vc.getCreatedBy()) || !user.isAdmin()) {
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

    private void publishVC (int vcId) throws KustvaktException {

        List<VirtualCorpusAccess> hiddenAccess =
                accessDao.retrieveHiddenAccess(vcId);

        // check if hidden access exists
        if (hiddenAccess.isEmpty()) {
            // assign hidden access for all users
            VirtualCorpus vc = vcDao.retrieveVCById(vcId);
            UserGroup all = userGroupService.retrieveAllUserGroup();
            accessDao.createAccessToVC(vc, all, "system",
                    VirtualCorpusAccessStatus.HIDDEN);

            // create and assign a hidden group
            int groupId = userGroupService.createAutoHiddenGroup(vcId);
            UserGroup autoHidden =
                    userGroupService.retrieveUserGroupById(groupId);
            accessDao.createAccessToVC(vc, autoHidden, "system",
                    VirtualCorpusAccessStatus.HIDDEN);
        }
        else {
            jlog.error("Cannot publish VC with id: " + vcId
                    + ". There have been hidden accesses for the VC already.");
        }
    }


    //    public void concealVC (String username, int vcId) throws KustvaktException {
    //
    //        VirtualCorpus vc = vcDao.retrieveVCById(vcId);
    //        if (vc.getType().equals(VirtualCorpusType.PUBLISHED)) {
    //            throw new KustvaktException(StatusCodes.NOTHING_CHANGED,
    //                    "Virtual corpus is not published.");
    //        }
    //
    //        VirtualCorpusJson vcJson = new VirtualCorpusJson();
    //        // EM: a published VC may originate from a project or a private VC. 
    //        // This origin is not saved in the DB. To be on the safe side, 
    //        // VirtualCorpusType is changed into PROJECT so that any groups 
    //        // associated with the VC can access it.
    //        vcJson.setType(VirtualCorpusType.PROJECT);
    //        editVC(vc, vcJson, username);
    //
    //        List<VirtualCorpusAccess> hiddenAccess =
    //                accessDao.retrieveHiddenAccess(vcId);
    //        for (VirtualCorpusAccess access : hiddenAccess){
    //            access.setDeletedBy(username);
    //            editVCAccess(access,username);
    //        }
    //
    //    }

    public List<VirtualCorpusAccess> retrieveVCAccess (int vcId)
            throws KustvaktException {
        return accessDao.retrieveAccessByVC(vcId);
    }

    public void shareVC (String username, int vcId, int groupId)
            throws KustvaktException {

        User user = authManager.getUser(username);

        VirtualCorpus vc = vcDao.retrieveVCById(vcId);
        if (!username.equals(vc.getCreatedBy()) || !user.isAdmin()) {
            throw new KustvaktException(StatusCodes.AUTHORIZATION_FAILED,
                    "Unauthorized operation for user: " + username, username);
        }

        UserGroup userGroup = userGroupService.retrieveUserGroupById(groupId);

        if (!user.isAdmin() && !isVCAccessAdmin(userGroup, username)) {
            throw new KustvaktException(StatusCodes.AUTHORIZATION_FAILED,
                    "Unauthorized operation for user: " + username, username);
        }
        else {
            accessDao.createAccessToVC(vc, userGroup, username,
                    VirtualCorpusAccessStatus.ACTIVE);
        }
    }

    private boolean isVCAccessAdmin (UserGroup userGroup, String username)
            throws KustvaktException {
        List<UserGroupMember> accessAdmins =
                userGroupService.retrieveVCAccessAdmins(userGroup);
        for (UserGroupMember m : accessAdmins) {
            if (username.equals(m.getUserId())) {
                return true;
            }
        }
        return false;
    }

    public void editVCAccess (VirtualCorpusAccess access, String username)
            throws KustvaktException {

        // get all the VCA admins
        UserGroup userGroup = access.getUserGroup();
        List<UserGroupMember> accessAdmins =
                userGroupService.retrieveVCAccessAdmins(userGroup);

        User user = authManager.getUser(username);
        if (!user.isAdmin()) {
            throw new KustvaktException(StatusCodes.AUTHORIZATION_FAILED,
                    "Unauthorized operation for user: " + username, username);
        }
    }

    public List<VirtualCorpusAccess> listVCAccessByVC (String username,
            int vcId) throws KustvaktException {

        List<VirtualCorpusAccess> accessList =
                accessDao.retrieveAccessByVC(vcId);
        User user = authManager.getUser(username);
        if (user.isAdmin()){
            return accessList;
        }

        List<VirtualCorpusAccess> filteredAccessList = new ArrayList<>();
        for (VirtualCorpusAccess access : accessList){
            UserGroup userGroup = access.getUserGroup();
            if (isVCAccessAdmin(userGroup, username)){
                filteredAccessList.add(access);
            }
        }
        return filteredAccessList;
    }

    public List<VirtualCorpusAccess> listVCAccessByGroup (String username,
            int groupId) throws KustvaktException {
        User user = authManager.getUser(username);
        UserGroup userGroup = userGroupService.retrieveUserGroupById(groupId);
        if (!user.isAdmin() && !isVCAccessAdmin(userGroup, username)) {
            throw new KustvaktException(StatusCodes.AUTHORIZATION_FAILED,
                    "Unauthorized operation for user: " + username, username);
        }
        return accessDao.retrieveAccessByGroup(groupId);
    }
}
