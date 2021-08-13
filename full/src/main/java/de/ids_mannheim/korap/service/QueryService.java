package de.ids_mannheim.korap.service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import javax.ws.rs.core.Response.Status;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import de.ids_mannheim.korap.KrillCollection;
import de.ids_mannheim.korap.config.FullConfiguration;
import de.ids_mannheim.korap.constant.GroupMemberStatus;
import de.ids_mannheim.korap.constant.QueryType;
import de.ids_mannheim.korap.constant.QueryAccessStatus;
import de.ids_mannheim.korap.constant.ResourceType;
import de.ids_mannheim.korap.dao.AdminDao;
import de.ids_mannheim.korap.dao.QueryAccessDao;
import de.ids_mannheim.korap.dao.QueryDao;
import de.ids_mannheim.korap.dto.QueryAccessDto;
import de.ids_mannheim.korap.dto.QueryDto;
import de.ids_mannheim.korap.dto.converter.QueryAccessConverter;
import de.ids_mannheim.korap.dto.converter.QueryConverter;
import de.ids_mannheim.korap.entity.UserGroup;
import de.ids_mannheim.korap.entity.UserGroupMember;
import de.ids_mannheim.korap.entity.QueryDO;
import de.ids_mannheim.korap.entity.QueryAccess;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.query.serialize.QuerySerializer;
import de.ids_mannheim.korap.user.User.CorpusAccess;
import de.ids_mannheim.korap.utils.JsonUtils;
import de.ids_mannheim.korap.utils.KoralCollectionQueryBuilder;
import de.ids_mannheim.korap.utils.ParameterChecker;
import de.ids_mannheim.korap.web.SearchKrill;
import de.ids_mannheim.korap.web.controller.QueryReferenceController;
import de.ids_mannheim.korap.web.controller.VirtualCorpusController;
import de.ids_mannheim.korap.web.input.QueryJson;

/**
 * QueryService handles the logic behind
 * {@link VirtualCorpusController} and
 * {@link QueryReferenceController}. Virtual corpora and
 * stored-queries are both treated as queries of different types.
 * Thus, they are handled logically similarly.
 * 
 * QueryService communicates with {@link QueryDao}, handles
 * {@link QueryDO} and
 * returns
 * {@link QueryDto} to {@link VirtualCorpusController} and
 * {@link QueryReferenceController}.
 * 
 * @author margaretha
 *
 */
@Service
public class QueryService {

    public static Logger jlog =
            LogManager.getLogger(QueryService.class);

    public static boolean DEBUG = false;

    public static Pattern queryNamePattern = Pattern.compile("[-\\w.]+");

    @Autowired
    private QueryDao queryDao;
    @Autowired
    private QueryAccessDao accessDao;
    @Autowired
    private AdminDao adminDao;
    @Autowired
    private UserGroupService userGroupService;
    @Autowired
    private SearchKrill krill;
    @Autowired
    private FullConfiguration config;
    @Autowired
    private QueryConverter converter;
    @Autowired
    private QueryAccessConverter accessConverter;

    private void verifyUsername (String contextUsername, String pathUsername)
            throws KustvaktException {
        if (!contextUsername.equals(pathUsername)
                && !adminDao.isAdmin(contextUsername)) {
            throw new KustvaktException(StatusCodes.AUTHORIZATION_FAILED,
                    "Unauthorized operation for user: " + contextUsername,
                    contextUsername);
        }
    }

    public List<QueryDto> listOwnerQuery (String username,
            String queryCreator, QueryType queryType) throws KustvaktException {
        verifyUsername(username, queryCreator);
        List<QueryDO> list = queryDao.retrieveOwnerQuery(username, queryType);
        return createQueryDtos(list, queryType);
    }

    public List<QueryDto> listAvailableQueryForUser (
            String authenticatedUsername, String username, QueryType queryType)
            throws KustvaktException {

        boolean isAdmin = adminDao.isAdmin(authenticatedUsername);

        if (username != null) {
            if (!username.equals(authenticatedUsername) && !isAdmin) {
                throw new KustvaktException(StatusCodes.AUTHORIZATION_FAILED,
                        "Unauthorized operation for user: "
                                + authenticatedUsername,
                        authenticatedUsername);
            }
        }
        else {
            username = authenticatedUsername;
        }
        List<QueryDO> list =
                queryDao.retrieveQueryByUser(username, queryType);
        return createQueryDtos(list, queryType);
    }

    public List<QueryDto> listQueryByType (String username,
            String createdBy, ResourceType type, QueryType queryType)
            throws KustvaktException {

        boolean isAdmin = adminDao.isAdmin(username);

        if (isAdmin) {
            List<QueryDO> virtualCorpora =
                    queryDao.retrieveQueryByType(type, createdBy, queryType);
            Collections.sort(virtualCorpora);
            return createQueryDtos(virtualCorpora, queryType);
        }
        else {
            throw new KustvaktException(StatusCodes.AUTHORIZATION_FAILED,
                    "Unauthorized operation for user: " + username, username);
        }
    }

    private ArrayList<QueryDto> createQueryDtos (
            List<QueryDO> queryList, QueryType queryType)
            throws KustvaktException {
        ArrayList<QueryDto> dtos = new ArrayList<>(queryList.size());
        QueryDO query;
        Iterator<QueryDO> i = queryList.iterator();
        while (i.hasNext()) {
            query = i.next();
            String json = query.getKoralQuery();
            String statistics = null;
            if (queryType.equals(QueryType.VIRTUAL_CORPUS)) {
                statistics = krill.getStatistics(json);
            }
            QueryDto dto =
                    converter.createQueryDto(query, statistics);
            dtos.add(dto);
        }
        return dtos;
    }

    public void deleteQueryByName (String username, String queryName,
            String createdBy, QueryType type) throws KustvaktException {

        QueryDO query = queryDao.retrieveQueryByName(queryName, createdBy);

        if (query == null) {
            String code = createdBy + "/" + queryName;
            throw new KustvaktException(StatusCodes.NO_RESOURCE_FOUND,
                    "Query " + code + " is not found.",
                    String.valueOf(code));
        }
        else if (query.getCreatedBy().equals(username)
                || adminDao.isAdmin(username)) {

            if (query.getType().equals(ResourceType.PUBLISHED)) {
                QueryAccess access =
                        accessDao.retrieveHiddenAccess(query.getId());
                accessDao.deleteAccess(access, "system");
                userGroupService.deleteAutoHiddenGroup(
                        access.getUserGroup().getId(), "system");
            }
            if (type.equals(QueryType.VIRTUAL_CORPUS)
                    && KrillCollection.cache.get(query.getName()) != null) {
                KrillCollection.cache.remove(query.getName());
            }
            queryDao.deleteQuery(query);
        }
        else {
            throw new KustvaktException(StatusCodes.AUTHORIZATION_FAILED,
                    "Unauthorized operation for user: " + username, username);
        }
    }

    public Status handlePutRequest (String username, String queryCreator,
            String queryName, QueryJson queryJson) throws KustvaktException {

        verifyUsername(username, queryCreator);
        QueryDO query = queryDao.retrieveQueryByName(queryName, queryCreator);
        
        if (query == null) {
            storeQuery(queryJson, queryName, username);
            return Status.CREATED;
        }
        else {
            editQuery(query, queryJson, queryName, username);
            return Status.NO_CONTENT;
        }
    }

    public void editQuery (QueryDO existingQuery, QueryJson newQuery,
            String queryName, String username) throws KustvaktException {

        if (!username.equals(existingQuery.getCreatedBy())
                && !adminDao.isAdmin(username)) {
            throw new KustvaktException(StatusCodes.AUTHORIZATION_FAILED,
                    "Unauthorized operation for user: " + username, username);
        }

        String koralQuery = null;
        CorpusAccess requiredAccess = null;
        String corpusQuery = newQuery.getCorpusQuery();
        if (corpusQuery != null && !corpusQuery.isEmpty()) {
            koralQuery = serializeCorpusQuery(corpusQuery);
            requiredAccess = determineRequiredAccess(newQuery.isCached(), queryName,
                    koralQuery);
        }

        ResourceType type = newQuery.getType();
        if (type != null) {
            if (existingQuery.getType().equals(ResourceType.PUBLISHED)) {
                // withdraw from publication
                if (!type.equals(ResourceType.PUBLISHED)) {
                    QueryAccess hiddenAccess =
                            accessDao.retrieveHiddenAccess(existingQuery.getId());
                    deleteQueryAccess(hiddenAccess.getId(), "system");
                    int groupId = hiddenAccess.getUserGroup().getId();
                    userGroupService.deleteAutoHiddenGroup(groupId, "system");
                    // EM: should the users within the hidden group
                    // receive
                    // notifications?
                }
                // else remains the same
            }
            else if (type.equals(ResourceType.PUBLISHED)) {
                publishQuery(existingQuery.getId());
            }
        }

        queryDao.editQuery(existingQuery, queryName, type, requiredAccess,
                koralQuery, newQuery.getDefinition(), newQuery.getDescription(),
                newQuery.getStatus(), newQuery.isCached());
    }

    private void publishQuery (int queryId) throws KustvaktException {

        QueryAccess access = accessDao.retrieveHiddenAccess(queryId);
        // check if hidden access exists
        if (access == null) {
            QueryDO query = queryDao.retrieveQueryById(queryId);
            // create and assign a new hidden group
            int groupId = userGroupService.createAutoHiddenGroup();
            UserGroup autoHidden =
                    userGroupService.retrieveUserGroupById(groupId);
            accessDao.createAccessToQuery(query, autoHidden, "system",
                    QueryAccessStatus.HIDDEN);
        }
        else {
            // should not happened
            jlog.error("Cannot publish query with id: " + queryId
                    + ". Hidden access exists! Access id: " + access.getId());
        }
    }

    public void storeQuery (QueryJson query, String queryName, String createdBy)
            throws KustvaktException {
        String koralQuery = null;
        if (query.getQueryType().equals(QueryType.VIRTUAL_CORPUS)) {
            ParameterChecker.checkStringValue(query.getCorpusQuery(),
                    "corpusQuery");
            koralQuery = serializeCorpusQuery(query.getCorpusQuery());
        }
        else if (query.getQueryType().equals(QueryType.QUERY)) {
            ParameterChecker.checkStringValue(query.getQuery(), "query");
            ParameterChecker.checkStringValue(query.getQueryLanguage(),
                    "queryLanguage");
            koralQuery =
                    serializeQuery(query.getQuery(), query.getQueryLanguage());
        }

        storeQuery(queryName, query.getType(), query.getQueryType(), koralQuery,
                query.getDefinition(), query.getDescription(),
                query.getStatus(), query.isCached(), createdBy,
                query.getQuery(), query.getQueryLanguage());
    }

    public void storeQuery (String queryName, ResourceType type, QueryType queryType,
            String koralQuery, String definition, String description,
            String status, boolean isCached, String username, String query,
            String queryLanguage) throws KustvaktException {
        ParameterChecker.checkNameValue(queryName, "queryName");
        ParameterChecker.checkObjectValue(type, "type");

        if (!queryNamePattern.matcher(queryName).matches()) {
            throw new KustvaktException(StatusCodes.INVALID_ARGUMENT,
                    queryType.displayName() + " name must only contain "
                            + "letters, numbers, underscores, hypens and spaces",
                    queryName);
        }

        if (type.equals(ResourceType.SYSTEM) && !username.equals("system")
                && !adminDao.isAdmin(username)) {
            throw new KustvaktException(StatusCodes.AUTHORIZATION_FAILED,
                    "Unauthorized operation for user: " + username, username);
        }

        CorpusAccess requiredAccess = CorpusAccess.PUB;
        if (queryType.equals(QueryType.VIRTUAL_CORPUS)) {
            requiredAccess =
                    determineRequiredAccess(isCached, queryName, koralQuery);
        }

        if (DEBUG){
            jlog.debug("Storing query: " + queryName + "in the database ");
        }
        
        int queryId = 0;
        try {
            queryId = queryDao.createQuery(queryName, type, queryType,
                    requiredAccess, koralQuery, definition, description, status,
                    isCached, username, query, queryLanguage);

        }
        catch (Exception e) {
            Throwable cause = e;
            Throwable lastCause = null;
            while ((cause = cause.getCause()) != null
                    && !cause.equals(lastCause)) {
                if (cause instanceof SQLException) {
                    break;
                }
                lastCause = cause;
            }
            throw new KustvaktException(StatusCodes.DB_INSERT_FAILED,
                    cause.getMessage());
        }
        if (type.equals(ResourceType.PUBLISHED)) {
            publishQuery(queryId);
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
        if (DEBUG) {
            jlog.debug(koralQuery);
        }
        return koralQuery;
    }

    private String serializeQuery (String query, String queryLanguage)
            throws KustvaktException {
        QuerySerializer serializer = new QuerySerializer();
        String koralQuery;
        koralQuery = serializer.setQuery(query, queryLanguage).toJSON();
        if (DEBUG) {
            jlog.debug(koralQuery);
        }
        return koralQuery;
    }

    public CorpusAccess determineRequiredAccess (boolean isCached, String name,
            String koralQuery) throws KustvaktException {

        if (isCached) {
            KoralCollectionQueryBuilder koral =
                    new KoralCollectionQueryBuilder();
            koral.with("referTo " + name);
            koralQuery = koral.toJSON();
            if (DEBUG) {
                jlog.debug("Determine vc access with vc ref: " + koralQuery);
            }

        }

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
        if (DEBUG) {
            jlog.debug(
                    "License: " + license + ", number of docs: " + numberOfDoc);
        }
        return (numberOfDoc > 0) ? true : false;
    }

    public void shareQuery (String username, String createdBy, String queryName,
            String groupName) throws KustvaktException {

        QueryDO query = queryDao.retrieveQueryByName(queryName, createdBy);
        if (query == null) {
            String code = createdBy + "/" + queryName;
            throw new KustvaktException(StatusCodes.NO_RESOURCE_FOUND,
                    "Query " + code + " is not found.",
                    String.valueOf(code));
        }
        if (!username.equals(query.getCreatedBy())
                && !adminDao.isAdmin(username)) {
            throw new KustvaktException(StatusCodes.AUTHORIZATION_FAILED,
                    "Unauthorized operation for user: " + username, username);
        }

        UserGroup userGroup =
                userGroupService.retrieveUserGroupByName(groupName);

        if (!isQueryAccessAdmin(userGroup, username)
                && !adminDao.isAdmin(username)) {
            throw new KustvaktException(StatusCodes.AUTHORIZATION_FAILED,
                    "Unauthorized operation for user: " + username, username);
        }
        else {
            try {
                accessDao.createAccessToQuery(query, userGroup, username,
                        QueryAccessStatus.ACTIVE);
            }
            catch (Exception e) {
                Throwable cause = e;
                Throwable lastCause = null;
                while ((cause = cause.getCause()) != null
                        && !cause.equals(lastCause)) {
                    if (cause instanceof SQLException) {
                        break;
                    }
                    lastCause = cause;
                }
                throw new KustvaktException(StatusCodes.DB_INSERT_FAILED,
                        cause.getMessage());
            }

            queryDao.editQuery(query, null, ResourceType.PROJECT, null, null,
                    null, null, null, query.isCached());
        }
    }

    private boolean isQueryAccessAdmin (UserGroup userGroup, String username)
            throws KustvaktException {
        List<UserGroupMember> accessAdmins =
                userGroupService.retrieveQueryAccessAdmins(userGroup);
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

    public List<QueryAccessDto> listQueryAccessByUsername (String username)
            throws KustvaktException {
        List<QueryAccess> accessList = new ArrayList<>();
        if (adminDao.isAdmin(username)) {
            accessList = accessDao.retrieveAllAccess();
        }
        else {
            List<UserGroup> groups =
                    userGroupService.retrieveUserGroup(username);
            for (UserGroup g : groups) {
                if (isQueryAccessAdmin(g, username)) {
                    accessList.addAll(
                            accessDao.retrieveActiveAccessByGroup(g.getId()));
                }
            }
        }
        return accessConverter.createQueryAccessDto(accessList);
    }

    public List<QueryAccessDto> listQueryAccessByQuery (String username,
            String queryCreator, String queryName) throws KustvaktException {

        List<QueryAccess> accessList;
        if (adminDao.isAdmin(username)) {
            accessList = accessDao.retrieveAllAccessByQuery(queryCreator, queryName);
        }
        else {
            accessList = accessDao.retrieveActiveAccessByQuery(queryCreator, queryName);
            List<QueryAccess> filteredAccessList = new ArrayList<>();
            for (QueryAccess access : accessList) {
                UserGroup userGroup = access.getUserGroup();
                if (isQueryAccessAdmin(userGroup, username)) {
                    filteredAccessList.add(access);
                }
            }
            accessList = filteredAccessList;
        }
        return accessConverter.createQueryAccessDto(accessList);
    }

    @Deprecated
    public List<QueryAccessDto> listVCAccessByGroup (String username,
            int groupId) throws KustvaktException {
        UserGroup userGroup = userGroupService.retrieveUserGroupById(groupId);

        List<QueryAccess> accessList;
        if (adminDao.isAdmin(username)) {
            accessList = accessDao.retrieveAllAccessByGroup(groupId);
        }
        else if (isQueryAccessAdmin(userGroup, username)) {
            accessList = accessDao.retrieveActiveAccessByGroup(groupId);
        }
        else {
            throw new KustvaktException(StatusCodes.AUTHORIZATION_FAILED,
                    "Unauthorized operation for user: " + username, username);
        }

        return accessConverter.createQueryAccessDto(accessList);
    }

    public List<QueryAccessDto> listQueryAccessByGroup (String username,
            String groupName) throws KustvaktException {
        UserGroup userGroup =
                userGroupService.retrieveUserGroupByName(groupName);

        List<QueryAccess> accessList;
        if (adminDao.isAdmin(username)) {
            accessList = accessDao.retrieveAllAccessByGroup(userGroup.getId());
        }
        else if (isQueryAccessAdmin(userGroup, username)) {
            accessList =
                    accessDao.retrieveActiveAccessByGroup(userGroup.getId());
        }
        else {
            throw new KustvaktException(StatusCodes.AUTHORIZATION_FAILED,
                    "Unauthorized operation for user: " + username, username);
        }
        return accessConverter.createQueryAccessDto(accessList);
    }

    public void deleteQueryAccess (int accessId, String username)
            throws KustvaktException {

        QueryAccess access = accessDao.retrieveAccessById(accessId);
        UserGroup userGroup = access.getUserGroup();
        if (isQueryAccessAdmin(userGroup, username)
                || adminDao.isAdmin(username)) {
            accessDao.deleteAccess(access, username);
        }
        else {
            throw new KustvaktException(StatusCodes.AUTHORIZATION_FAILED,
                    "Unauthorized operation for user: " + username, username);
        }

    }

    public QueryDO searchQueryByName (String username, String queryName,
            String createdBy, QueryType queryType) throws KustvaktException {
        QueryDO query = queryDao.retrieveQueryByName(queryName, createdBy);
        if (query == null) {
            String code = createdBy + "/" + queryName;
            throw new KustvaktException(StatusCodes.NO_RESOURCE_FOUND,
                    queryType.displayName()+ " " + code + " is not found.",
                    String.valueOf(code));
        }
        checkQueryAccess(query, username);
        return query;
    }

    public QueryDto retrieveQueryByName (String username, String queryName,
            String createdBy, QueryType queryType) throws KustvaktException {
        QueryDO query = searchQueryByName(username, queryName, createdBy, queryType);
        String json = query.getKoralQuery();
        String statistics = null;
        if (query.getQueryType().equals(QueryType.VIRTUAL_CORPUS)) {
            statistics = krill.getStatistics(json);
        }
        return converter.createQueryDto(query, statistics);
    }

    public QueryDto searchQueryById (String username, int queryId)
            throws KustvaktException {

        QueryDO query = queryDao.retrieveQueryById(queryId);
        checkQueryAccess(query, username);
        String json = query.getKoralQuery();
        String statistics = krill.getStatistics(json);
        return converter.createQueryDto(query, statistics);
    }

    private void checkQueryAccess (QueryDO query, String username)
            throws KustvaktException {
        ResourceType type = query.getType();

        if (!adminDao.isAdmin(username)
                && !username.equals(query.getCreatedBy())) {
            if (type.equals(ResourceType.PRIVATE)
                    || (type.equals(ResourceType.PROJECT)
                            && !hasAccess(username, query.getId()))) {
                throw new KustvaktException(StatusCodes.AUTHORIZATION_FAILED,
                        "Unauthorized operation for user: " + username,
                        username);
            }

            else if (ResourceType.PUBLISHED.equals(type)
                    && !username.equals("guest")) {
                // add user in the query's auto group
                UserGroup userGroup = userGroupService
                        .retrieveHiddenUserGroupByQuery(query.getId());
                try {
                    userGroupService.addGroupMember(username, userGroup,
                            "system", GroupMemberStatus.ACTIVE);
                    // member roles are not set (not necessary)
                }
                catch (KustvaktException e) {
                    // member exists
                    // skip adding user to hidden group
                }
            }
            // else VirtualCorpusType.SYSTEM
        }
    }

    private boolean hasAccess (String username, int queryId)
            throws KustvaktException {
        UserGroup userGroup;
        List<QueryAccess> accessList =
                accessDao.retrieveActiveAccessByQuery(queryId);
        for (QueryAccess access : accessList) {
            userGroup = access.getUserGroup();
            if (userGroupService.isMember(username, userGroup)) {
                return true;
            }
        }
        return false;
    }
}
