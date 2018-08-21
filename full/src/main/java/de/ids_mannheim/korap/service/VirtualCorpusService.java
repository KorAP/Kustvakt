package de.ids_mannheim.korap.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import de.ids_mannheim.korap.config.FullConfiguration;
import de.ids_mannheim.korap.constant.GroupMemberStatus;
import de.ids_mannheim.korap.constant.VirtualCorpusAccessStatus;
import de.ids_mannheim.korap.constant.VirtualCorpusType;
import de.ids_mannheim.korap.dao.AdminDao;
import de.ids_mannheim.korap.dao.VirtualCorpusAccessDao;
import de.ids_mannheim.korap.dao.VirtualCorpusDao;
import de.ids_mannheim.korap.dto.VirtualCorpusAccessDto;
import de.ids_mannheim.korap.dto.VirtualCorpusDto;
import de.ids_mannheim.korap.dto.converter.VirtualCorpusAccessConverter;
import de.ids_mannheim.korap.dto.converter.VirtualCorpusConverter;
import de.ids_mannheim.korap.entity.UserGroup;
import de.ids_mannheim.korap.entity.UserGroupMember;
import de.ids_mannheim.korap.entity.VirtualCorpus;
import de.ids_mannheim.korap.entity.VirtualCorpusAccess;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.query.serialize.QuerySerializer;
import de.ids_mannheim.korap.user.User.CorpusAccess;
import de.ids_mannheim.korap.utils.JsonUtils;
import de.ids_mannheim.korap.utils.KoralCollectionQueryBuilder;
import de.ids_mannheim.korap.utils.ParameterChecker;
import de.ids_mannheim.korap.web.SearchKrill;
import de.ids_mannheim.korap.web.controller.VirtualCorpusController;
import de.ids_mannheim.korap.web.input.VirtualCorpusJson;

/**
 * VirtualCorpusService handles the logic behind
 * {@link VirtualCorpusController}.
 * It communicates with {@link VirtualCorpusDao} and returns
 * {@link VirtualCorpusDto} to {@link VirtualCorpusController}.
 * 
 * @author margaretha
 *
 */
@Service
public class VirtualCorpusService {

    private static Logger jlog =
            LogManager.getLogger(VirtualCorpusService.class);

    public static Pattern wordPattern = Pattern.compile("[\\w ]+");

    @Autowired
    private VirtualCorpusDao vcDao;
    @Autowired
    private VirtualCorpusAccessDao accessDao;
    @Autowired
    private AdminDao adminDao;
    @Autowired
    private UserGroupService userGroupService;
    @Autowired
    private SearchKrill krill;
    @Autowired
    private FullConfiguration config;
    @Autowired
    private VirtualCorpusConverter converter;
    @Autowired
    private VirtualCorpusAccessConverter accessConverter;

    public List<VirtualCorpusDto> listOwnerVC (String username)
            throws KustvaktException {
        List<VirtualCorpus> vcList = vcDao.retrieveOwnerVC(username);
        return createVCDtos(vcList);
    }

    public List<VirtualCorpusDto> listVCByUser (String contextUsername,
            String createdBy) throws KustvaktException {

        boolean isAdmin = adminDao.isAdmin(contextUsername);

        if (createdBy != null) {
            if (!createdBy.equals(contextUsername) && !isAdmin) {
                throw new KustvaktException(StatusCodes.AUTHORIZATION_FAILED,
                        "Unauthorized operation for user: " + contextUsername,
                        contextUsername);
            }
        }
        else {
            createdBy = contextUsername;
        }
        List<VirtualCorpus> vcList = vcDao.retrieveVCByUser(createdBy);
        return createVCDtos(vcList);
    }

    public List<VirtualCorpusDto> listVCByType (String username,
            String createdBy, VirtualCorpusType type) throws KustvaktException {

        boolean isAdmin = adminDao.isAdmin(username);

        if (isAdmin) {
            List<VirtualCorpus> virtualCorpora =
                    vcDao.retrieveVCByType(type, createdBy);
            Collections.sort(virtualCorpora);
            return createVCDtos(virtualCorpora);
        }
        else {
            throw new KustvaktException(StatusCodes.AUTHORIZATION_FAILED,
                    "Unauthorized operation for user: " + username, username);
        }
    }

    private ArrayList<VirtualCorpusDto> createVCDtos (
            List<VirtualCorpus> vcList) throws KustvaktException {
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

    /**
     * Only admin and the owner of the virtual corpus are allowed to
     * delete a virtual corpus.
     * 
     * @param username
     *            username
     * @param vcId
     *            virtual corpus id
     * @throws KustvaktException
     */
    public void deleteVC (String username, int vcId) throws KustvaktException {

        VirtualCorpus vc = vcDao.retrieveVCById(vcId);

        if (vc.getCreatedBy().equals(username) || adminDao.isAdmin(username)) {

            if (vc.getType().equals(VirtualCorpusType.PUBLISHED)) {
                VirtualCorpusAccess access =
                        accessDao.retrieveHiddenAccess(vcId);
                accessDao.deleteAccess(access, "system");
                userGroupService.deleteAutoHiddenGroup(
                        access.getUserGroup().getId(), "system");
            }
            vcDao.deleteVirtualCorpus(vc);
        }
        else {
            throw new KustvaktException(StatusCodes.AUTHORIZATION_FAILED,
                    "Unauthorized operation for user: " + username, username);
        }
    }

    public void editVC (VirtualCorpusJson vcJson, String username)
            throws KustvaktException {
        ParameterChecker.checkIntegerValue(vcJson.getId(), "id");
        int vcId = vcJson.getId();
        VirtualCorpus vc = vcDao.retrieveVCById(vcId);

        if (!username.equals(vc.getCreatedBy())
                && !adminDao.isAdmin(username)) {
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

        VirtualCorpusType type = vcJson.getType();
        if (type != null) {
            if (vc.getType().equals(VirtualCorpusType.PUBLISHED)) {
                // withdraw from publication
                if (!type.equals(VirtualCorpusType.PUBLISHED)) {
                    VirtualCorpusAccess hiddenAccess =
                            accessDao.retrieveHiddenAccess(vcId);
                    deleteVCAccess(hiddenAccess.getId(), "system");
                    int groupId = hiddenAccess.getUserGroup().getId();
                    userGroupService.deleteAutoHiddenGroup(groupId, "system");
                }
                // else remains the same
            }
            else if (type.equals(VirtualCorpusType.PUBLISHED)) {
                publishVC(vcJson.getId());
            }
        }

        vcDao.editVirtualCorpus(vc, vcJson.getName(), vcJson.getType(),
                requiredAccess, koralQuery, vcJson.getDefinition(),
                vcJson.getDescription(), vcJson.getStatus());
    }

    private void publishVC (int vcId) throws KustvaktException {

        VirtualCorpusAccess access = accessDao.retrieveHiddenAccess(vcId);
        // check if hidden access exists
        if (access == null) {
            VirtualCorpus vc = vcDao.retrieveVCById(vcId);
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

    public int storeVC (VirtualCorpusJson vc, String username)
            throws KustvaktException {
        ParameterChecker.checkStringValue(vc.getName(), "name");
        ParameterChecker.checkObjectValue(vc.getType(), "type");
        ParameterChecker.checkStringValue(vc.getCorpusQuery(), "corpusQuery");

        String name = vc.getName();
        if (!wordPattern.matcher(name).matches()) {
            throw new KustvaktException(StatusCodes.INVALID_ARGUMENT,
                    "Virtual corpus name must only contains letters, numbers, underscores and spaces",
                    name);
        }

        if (vc.getType().equals(VirtualCorpusType.SYSTEM)
                && !adminDao.isAdmin(username)) {
            throw new KustvaktException(StatusCodes.AUTHORIZATION_FAILED,
                    "Unauthorized operation for user: " + username, username);
        }

        String koralQuery = serializeCorpusQuery(vc.getCorpusQuery());
        CorpusAccess requiredAccess = determineRequiredAccess(koralQuery);

        int vcId = vcDao.createVirtualCorpus(vc.getName(), vc.getType(),
                requiredAccess, koralQuery, vc.getDefinition(),
                vc.getDescription(), vc.getStatus(), username);

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

    public List<VirtualCorpusAccess> retrieveAllVCAccess (int vcId)
            throws KustvaktException {
        return accessDao.retrieveAllAccessByVC(vcId);
    }

    public void shareVC (String username, int vcId, int groupId)
            throws KustvaktException {

        VirtualCorpus vc = vcDao.retrieveVCById(vcId);
        if (!username.equals(vc.getCreatedBy())
                && !adminDao.isAdmin(username)) {
            throw new KustvaktException(StatusCodes.AUTHORIZATION_FAILED,
                    "Unauthorized operation for user: " + username, username);
        }

        UserGroup userGroup = userGroupService.retrieveUserGroupById(groupId);

        if (!isVCAccessAdmin(userGroup, username)
                && !adminDao.isAdmin(username)) {
            throw new KustvaktException(StatusCodes.AUTHORIZATION_FAILED,
                    "Unauthorized operation for user: " + username, username);
        }
        else {
            accessDao.createAccessToVC(vc, userGroup, username,
                    VirtualCorpusAccessStatus.ACTIVE);
            vcDao.editVirtualCorpus(vc, null, VirtualCorpusType.PUBLISHED, null,
                    null, null, null, null);
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

    // public void editVCAccess (VirtualCorpusAccess access, String
    // username)
    // throws KustvaktException {
    //
    // // get all the VCA admins
    // UserGroup userGroup = access.getUserGroup();
    // List<UserGroupMember> accessAdmins =
    // userGroupService.retrieveVCAccessAdmins(userGroup);
    //
    // User user = authManager.getUser(username);
    // if (!user.isSystemAdmin()) {
    // throw new KustvaktException(StatusCodes.AUTHORIZATION_FAILED,
    // "Unauthorized operation for user: " + username, username);
    // }
    // }

    public List<VirtualCorpusAccessDto> listVCAccessByVC (String username,
            int vcId) throws KustvaktException {

        List<VirtualCorpusAccess> accessList;
        if (adminDao.isAdmin(username)) {
            accessList = accessDao.retrieveAllAccessByVC(vcId);
        }
        else {
            accessList = accessDao.retrieveActiveAccessByVC(vcId);
            List<VirtualCorpusAccess> filteredAccessList = new ArrayList<>();
            for (VirtualCorpusAccess access : accessList) {
                UserGroup userGroup = access.getUserGroup();
                if (isVCAccessAdmin(userGroup, username)) {
                    filteredAccessList.add(access);
                }
            }
            accessList = filteredAccessList;
        }
        return accessConverter.createVCADto(accessList);
    }

    public List<VirtualCorpusAccessDto> listVCAccessByGroup (String username,
            int groupId) throws KustvaktException {
        UserGroup userGroup = userGroupService.retrieveUserGroupById(groupId);

        List<VirtualCorpusAccess> accessList;
        if (adminDao.isAdmin(username)) {
            accessList = accessDao.retrieveAllAccessByGroup(groupId);
        }
        else if (isVCAccessAdmin(userGroup, username)) {
            accessList = accessDao.retrieveActiveAccessByGroup(groupId);
        }
        else {
            throw new KustvaktException(StatusCodes.AUTHORIZATION_FAILED,
                    "Unauthorized operation for user: " + username, username);
        }

        return accessConverter.createVCADto(accessList);
    }

    public void deleteVCAccess (int accessId, String username)
            throws KustvaktException {

        VirtualCorpusAccess access = accessDao.retrieveAccessById(accessId);
        UserGroup userGroup = access.getUserGroup();
        if (isVCAccessAdmin(userGroup, username)
                || adminDao.isAdmin(username)) {
            accessDao.deleteAccess(access, username);
        }
        else {
            throw new KustvaktException(StatusCodes.AUTHORIZATION_FAILED,
                    "Unauthorized operation for user: " + username, username);
        }

    }

    public VirtualCorpusDto searchVCById (String username, int vcId)
            throws KustvaktException {

        VirtualCorpus vc = vcDao.retrieveVCById(vcId);
        VirtualCorpusType type = vc.getType();

        if (!adminDao.isAdmin(username)
                && !username.equals(vc.getCreatedBy())) {
            if (type.equals(VirtualCorpusType.PRIVATE)
                    || (type.equals(VirtualCorpusType.PROJECT)
                            && !hasAccess(username, vcId))) {
                throw new KustvaktException(StatusCodes.AUTHORIZATION_FAILED,
                        "Unauthorized operation for user: " + username,
                        username);
            }

            else if (VirtualCorpusType.PUBLISHED.equals(type)) {
                // add user in the VC's auto group
                UserGroup userGroup =
                        userGroupService.retrieveHiddenUserGroupByVC(vcId);
                try {
                    userGroupService.addGroupMember(username, userGroup,
                            "system", GroupMemberStatus.ACTIVE);
                    // member roles has not been set (not necessary)
                }
                catch (KustvaktException e) {
                    // member exists
                    // skip adding user to hidden group
                }
            }
            // else VirtualCorpusType.SYSTEM
        }

        String json = vc.getCorpusQuery();
        String statistics = krill.getStatistics(json);
        return converter.createVirtualCorpusDto(vc, statistics);
    }

    private boolean hasAccess (String username, int vcId)
            throws KustvaktException {
        UserGroup userGroup;
        List<VirtualCorpusAccess> accessList =
                accessDao.retrieveActiveAccessByVC(vcId);
        for (VirtualCorpusAccess access : accessList) {
            userGroup = access.getUserGroup();
            if (userGroupService.isMember(username, userGroup)) {
                return true;
            }
        }
        return false;
    }
}
