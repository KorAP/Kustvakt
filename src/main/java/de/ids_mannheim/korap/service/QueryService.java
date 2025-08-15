package de.ids_mannheim.korap.service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import de.ids_mannheim.korap.cache.VirtualCorpusCache;
import de.ids_mannheim.korap.config.FullConfiguration;
import de.ids_mannheim.korap.constant.PredefinedRole;
import de.ids_mannheim.korap.constant.PrivilegeType;
import de.ids_mannheim.korap.constant.QueryType;
import de.ids_mannheim.korap.constant.ResourceType;
import de.ids_mannheim.korap.core.service.BasicService;
import de.ids_mannheim.korap.dao.AdminDao;
import de.ids_mannheim.korap.dao.QueryDao;
import de.ids_mannheim.korap.dao.RoleDao;
import de.ids_mannheim.korap.dao.UserGroupDao;
import de.ids_mannheim.korap.dao.UserGroupMemberDao;
import de.ids_mannheim.korap.dto.QueryDto;
import de.ids_mannheim.korap.dto.RoleDto;
import de.ids_mannheim.korap.dto.converter.QueryConverter;
import de.ids_mannheim.korap.dto.converter.RoleConverter;
import de.ids_mannheim.korap.entity.QueryDO;
import de.ids_mannheim.korap.entity.Role;
import de.ids_mannheim.korap.entity.UserGroup;
import de.ids_mannheim.korap.entity.UserGroupMember;
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
import jakarta.persistence.NoResultException;
import jakarta.ws.rs.core.Response.Status;

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
public class QueryService extends BasicService {

    public static Logger jlog = LogManager.getLogger(QueryService.class);

    public static boolean DEBUG = false;

    public static Pattern queryNamePattern = Pattern
            .compile("[a-zA-Z0-9]+[a-zA-Z_0-9-.]+");

    @Autowired
    private QueryDao queryDao;
    @Autowired
    private RoleDao roleDao;
    @Autowired
    private UserGroupDao userGroupDao;
    @Autowired
    private UserGroupMemberDao memberDao;

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
    private RoleConverter roleConverter;

    private void verifyUsername (String contextUsername, String pathUsername)
            throws KustvaktException {
        if (!contextUsername.equals(pathUsername)
                && !adminDao.isAdmin(contextUsername)) {
            throw new KustvaktException(StatusCodes.AUTHORIZATION_FAILED,
                    "Unauthorized operation for user: " + contextUsername,
                    contextUsername);
        }
    }

	public List<QueryDto> listOwnerQuery (String username, QueryType queryType,
			double apiVersion) throws KustvaktException {
        List<QueryDO> list = queryDao.retrieveOwnerQuery(username, queryType);
        return createQueryDtos(list, queryType, apiVersion);
    }

	public List<QueryDto> listSystemQuery (QueryType queryType,
			double apiVersion) throws KustvaktException {
        List<QueryDO> list = queryDao.retrieveQueryByType(ResourceType.SYSTEM,
                null, queryType);
        return createQueryDtos(list, queryType, apiVersion);
    }

    public List<QueryDto> listAvailableQueryForUser (String username,
            QueryType queryType, double apiVersion) throws KustvaktException {
        List<QueryDO> list = queryDao.retrieveQueryByUser(username, queryType);
        return createQueryDtos(list, queryType, apiVersion);
    }

    public List<QueryDto> listQueryByType (String createdBy, ResourceType type,
            QueryType queryType, double apiVersion) throws KustvaktException {

        List<QueryDO> virtualCorpora = queryDao.retrieveQueryByType(type,
                createdBy, queryType);
        Collections.sort(virtualCorpora);
        return createQueryDtos(virtualCorpora, queryType, apiVersion);

    }

    private ArrayList<QueryDto> createQueryDtos (List<QueryDO> queryList,
            QueryType queryType, double apiVersion) throws KustvaktException {
        ArrayList<QueryDto> dtos = new ArrayList<>(queryList.size());
        QueryDO query;
        Iterator<QueryDO> i = queryList.iterator();
        while (i.hasNext()) {
            query = i.next();
            String statistics = computeStatisticsForVC(query, queryType);
			QueryDto dto = converter.createQueryDto(query, statistics);
			dtos.add(dto);
		}
		return dtos;
    }
    
	private String computeStatisticsForVC (QueryDO query, QueryType queryType)
			throws KustvaktException {
		if (config.includeStatisticsInVCList() && 
				queryType.equals(QueryType.VIRTUAL_CORPUS)) {		
    		String json = "";
    		if (query.isCached()) {
    			List<String> cqList = new ArrayList<>(1);
    			cqList.add("referTo " + query.getName());
    			json = buildKoralQueryFromCorpusQuery(cqList);
    		}
    		else {
    			json = query.getKoralQuery();
    		}
    		return krill.getStatistics(json);
		}
		else {
			return null;
		}
	}

    public void deleteQueryByName (String deletedBy, String queryName,
            String createdBy, QueryType type) throws KustvaktException {

        QueryDO query = queryDao.retrieveQueryByName(queryName, createdBy);

        if (query == null) {
            String code = createdBy + "/" + queryName;
            throw new KustvaktException(StatusCodes.NO_RESOURCE_FOUND,
                    "Query " + code + " is not found.", String.valueOf(code));
        }
        else if (query.getCreatedBy().equals(deletedBy)
                || adminDao.isAdmin(deletedBy)) {

            // If published, fetch the hidden group BEFORE deleting roles, so we can remove
            // it later
            UserGroup hiddenGroup = null;
            boolean isPublished = query.getType().equals(ResourceType.PUBLISHED);
            if (isPublished) {
                hiddenGroup = userGroupDao.retrieveHiddenGroupByQueryName(queryName);
            }

            // Detach member-role links and delete all roles linked to the query
            List<Role> queryRoles = roleDao.retrieveRolesByQueryIdWithMembers(query.getId());
            for (Role role : queryRoles) {
                if (role.getUserGroupMembers() != null) {
                    for (UserGroupMember m : role.getUserGroupMembers()) {
                        if (m.getRoles() != null && m.getRoles().remove(role)) {
                            memberDao.updateMember(m);
                        }
                    }
                }
            }
            roleDao.deleteRolesByQueryId(query.getId());

            if (isPublished && hiddenGroup != null) {
                userGroupDao.deleteGroup(hiddenGroup.getId(), deletedBy);
            }
            if (type.equals(QueryType.VIRTUAL_CORPUS)
                    && VirtualCorpusCache.contains(queryName)) {
                VirtualCorpusCache.delete(queryName);
            }
            queryDao.deleteQuery(query);
        }
        else {
            throw new KustvaktException(StatusCodes.AUTHORIZATION_FAILED,
                    "Unauthorized operation for user: " + deletedBy, deletedBy);
        }
    }

    public Status handlePutRequest (String username, String queryCreator,
            String queryName, QueryJson queryJson, double apiVersion) 
            		throws KustvaktException {

        verifyUsername(username, queryCreator);
        QueryDO query = queryDao.retrieveQueryByName(queryName, queryCreator);

        if (query == null) {
            storeQuery(queryJson, queryName, queryCreator, username, 
            		apiVersion);
            return Status.CREATED;
        }
        else {
            editQuery(query, queryJson, queryName, username, 
            		apiVersion);
            return Status.NO_CONTENT;
        }
    }

    public void editQuery (QueryDO existingQuery, QueryJson newQuery,
            String queryName, String username, double apiVersion) 
    		throws KustvaktException {

        if (!username.equals(existingQuery.getCreatedBy())
                && !adminDao.isAdmin(username)) {
            throw new KustvaktException(StatusCodes.AUTHORIZATION_FAILED,
                    "Unauthorized operation for user: " + username, username);
        }

        String koralQuery = null;
        CorpusAccess requiredAccess = null;
        String corpusQuery = newQuery.getCorpusQuery();
        String query = newQuery.getQuery();
        String queryLanguage = newQuery.getQueryLanguage();
        if (corpusQuery != null && !corpusQuery.isEmpty()) {
            koralQuery = serializeCorpusQuery(corpusQuery, apiVersion);
            requiredAccess = determineRequiredAccess(newQuery.isCached(),
                    queryName, koralQuery, apiVersion);
        }
        else if (query != null && !query.isEmpty() && queryLanguage != null
                && !queryLanguage.isEmpty()) {
            koralQuery = serializeQuery(query, queryLanguage, apiVersion);
        }

        ResourceType type = newQuery.getType();
        if (type != null) {
            if (existingQuery.getType().equals(ResourceType.PUBLISHED)) {
                // withdraw from publication
                if (!type.equals(ResourceType.PUBLISHED)) {
                    UserGroup group = userGroupDao
                            .retrieveHiddenGroupByQueryName(queryName);
                    int groupId = group.getId();
                    userGroupDao.deleteGroup(groupId, username);
                    // EM: should the users within the hidden group
                    // receive
                    // notifications?
                }
                // else remains the same
            }
            else if (type.equals(ResourceType.PUBLISHED)) {
                publishQuery(existingQuery.getId(), username, queryName);
            }
        }

        queryDao.editQuery(existingQuery, queryName, type, requiredAccess,
                koralQuery, newQuery.getDefinition(), newQuery.getDescription(),
                newQuery.getStatus(), newQuery.isCached(), query,
                queryLanguage);
    }

    private void publishQuery (int queryId, String queryCreator,
            String queryName) throws KustvaktException {

//        QueryAccess access = accessDao.retrieveHiddenAccess(queryId);
        // check if hidden access exists
//        if (access == null) {
            QueryDO query = queryDao.retrieveQueryById(queryId);
            // create and assign a new hidden group
            int groupId = userGroupService.createAutoHiddenGroup(queryCreator,
                    queryName);
            UserGroup autoHidden = userGroupService
                    .retrieveUserGroupById(groupId);
//            accessDao.createAccessToQuery(query, autoHidden);
            addRoleToQuery(query, autoHidden);
//        }
//        else {
//            // should not happened
//            jlog.error("Cannot publish query with id: " + queryId
//                    + ". Hidden access exists! Access id: " + access.getId());
//        }
    }

    public void storeQuery (QueryJson query, String queryName,
            String queryCreator, String username, double apiVersion) 
            		throws KustvaktException {
        QueryType queryType = query.getQueryType();
        if (!checkNumberOfQueryLimit(username, queryType)) {
            String type = queryType.displayName().toLowerCase();
            throw new KustvaktException(StatusCodes.NOT_ALLOWED,
                    "Cannot create " + type + ". The maximum number " + "of "
                            + type + " has been reached.");
        }

        String koralQuery = computeKoralQuery(query, apiVersion);
        storeQuery(username, queryName, query.getType(), query.getQueryType(),
                koralQuery, query.getDefinition(), query.getDescription(),
                query.getStatus(), query.isCached(), queryCreator,
                query.getQuery(), query.getQueryLanguage(), apiVersion);
    }

    private boolean checkNumberOfQueryLimit (String username,
            QueryType queryType) throws KustvaktException {
        Long num = queryDao.countNumberOfQuery(username, queryType);
        if (num < config.getMaxNumberOfUserQueries())
            return true;
        else
            return false;
    }

    private String computeKoralQuery (QueryJson query, double apiVersion)
            throws KustvaktException {
        if (query.getQueryType().equals(QueryType.VIRTUAL_CORPUS)) {
            ParameterChecker.checkStringValue(query.getCorpusQuery(),
                    "corpusQuery");
            return serializeCorpusQuery(query.getCorpusQuery(), apiVersion);
        }

        if (query.getQueryType().equals(QueryType.QUERY)) {
            ParameterChecker.checkStringValue(query.getQuery(), "query");
            ParameterChecker.checkStringValue(query.getQueryLanguage(),
                    "queryLanguage");
            return serializeQuery(query.getQuery(), query.getQueryLanguage(), 
            		apiVersion);
        }

        return null;
    }

    public void storeQuery (String username, String queryName,
            ResourceType type, QueryType queryType, String koralQuery,
            String definition, String description, String status,
            boolean isCached, String queryCreator, String query,
            String queryLanguage, double apiVersion) throws KustvaktException {
        storeQuery(null, username, queryName, type, queryType, koralQuery,
                definition, description, status, isCached, queryCreator, query,
                queryLanguage, apiVersion);
    }
    
    public void storeQuery (QueryDO existingQuery, String username, String queryName,
            ResourceType type, QueryType queryType, String koralQuery,
            String definition, String description, String status,
            boolean isCached, String queryCreator, String query,
            String queryLanguage, double apiVersion) throws KustvaktException {
        ParameterChecker.checkNameValue(queryName, "queryName");
        ParameterChecker.checkObjectValue(type, "type");

        if (!queryNamePattern.matcher(queryName).matches()) {
            throw new KustvaktException(StatusCodes.INVALID_ARGUMENT, queryType
                    .displayName()
                    + " must consists of alphanumerical characters "
                    + "(limited to ASCII), underscores, dashes and periods. "
                    + "The name has to start with an alphanumerical character.",
                    queryName);
        }

        if (type.equals(ResourceType.SYSTEM)) {
            if (adminDao.isAdmin(username)) {
                queryCreator = "system";
            }
            else if (!username.equals("system")) {
                throw new KustvaktException(StatusCodes.AUTHORIZATION_FAILED,
                        "Unauthorized operation for user: " + username,
                        username);
            }
        }

        CorpusAccess requiredAccess = CorpusAccess.FREE;
        if (queryType.equals(QueryType.VIRTUAL_CORPUS)) {
            requiredAccess = determineRequiredAccess(isCached, queryName,
                    koralQuery, apiVersion);
        }

        if (DEBUG) {
            jlog.debug("Storing query: " + queryName + "in the database ");
        }

        int queryId = 0;
        try {
            if (existingQuery==null) {
                queryId = queryDao.createQuery(queryName, type, queryType,
                        requiredAccess, koralQuery, definition, description,
                        status, isCached, queryCreator, query, queryLanguage);
            }
            else {
                queryDao.editQuery(existingQuery, queryName, type,
                        requiredAccess, koralQuery, definition, description,
                        status, isCached, query, queryLanguage);
            }

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
            publishQuery(queryId, queryCreator, queryName);
        }
    }

    public String serializeCorpusQuery (String corpusQuery, double apiVersion)
            throws KustvaktException {
        QuerySerializer serializer = new QuerySerializer(apiVersion);
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

    private String serializeQuery (String query, String queryLanguage, double 
    		apiVersion)
            throws KustvaktException {
        QuerySerializer serializer = new QuerySerializer(apiVersion);
        String koralQuery;
        koralQuery = serializer.setQuery(query, queryLanguage).toJSON();
        if (DEBUG) {
            jlog.debug(koralQuery);
        }
        JsonNode n = JsonUtils.readTree(koralQuery).get("query");
        return n.toString();
    }

    public CorpusAccess determineRequiredAccess (boolean isCached, String name,
            String koralQuery, double apiVersion) throws KustvaktException {

        if (isCached) {
            KoralCollectionQueryBuilder koral = 
            		new KoralCollectionQueryBuilder(apiVersion);
            koral.with("referTo " + name);
            koralQuery = koral.toJSON();
            if (DEBUG) {
                jlog.debug("Determine vc access with vc ref: " + koralQuery);
            }

        }

		if (findDocWithLicense(koralQuery, config.getAllOnlyRegex(),
				apiVersion)) {
			return CorpusAccess.ALL;
		}
		else if (findDocWithLicense(koralQuery, config.getPublicOnlyRegex(),
				apiVersion)) {
			return CorpusAccess.PUB;
        }
        else {
            return CorpusAccess.FREE;
        }
    }

    private boolean findDocWithLicense (String koralQuery, String license, 
    		double apiVersion) throws KustvaktException {
        KoralCollectionQueryBuilder koral = 
        		new KoralCollectionQueryBuilder(apiVersion);
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
                    "Query " + code + " is not found.", String.valueOf(code));
        }
        if (!username.equals(query.getCreatedBy())
                && !adminDao.isAdmin(username)) {
            throw new KustvaktException(StatusCodes.AUTHORIZATION_FAILED,
                    "Unauthorized operation for user: " + username, username);
        }

        UserGroup userGroup = userGroupService
                .retrieveUserGroupByName(groupName);

        if (!userGroupService.isUserGroupAdmin(username,userGroup)
                && !adminDao.isAdmin(username)) {
            throw new KustvaktException(StatusCodes.AUTHORIZATION_FAILED,
                    "Unauthorized operation for user: " + username, username);
        }
        else {
            try {
                addRoleToQuery(query, userGroup);
            }
            catch (Exception e) {
                Throwable cause = e;
                Throwable lastCause = null;
                while ((cause = cause.getCause()) != null
                        && !cause.equals(lastCause)) {
//                    if (cause instanceof SQLException) {
//                        break;
//                    }
                    lastCause = cause;
                }
                throw new KustvaktException(
                        StatusCodes.DB_UNIQUE_CONSTRAINT_FAILED,
                        lastCause.getMessage());
            }

            ResourceType queryType = query.getType();
            if(queryType.equals(ResourceType.PRIVATE)) {
                queryType = ResourceType.PROJECT;
            }
                
            queryDao.editQuery(query, null, queryType, null, null,
                    null, null, null, query.isCached(), null, null);
        }
    }
    
    public void addRoleToQuery (QueryDO query, UserGroup userGroup)
            throws KustvaktException {
    
        List<UserGroupMember> members = memberDao
                .retrieveMemberByGroupId(userGroup.getId());

        Role r1 = new Role(PredefinedRole.QUERY_ACCESS,
                PrivilegeType.READ_QUERY, userGroup, query);
        roleDao.addRole(r1);
        
        for (UserGroupMember member : members) {
            member.getRoles().add(r1);
            memberDao.updateMember(member);
        }
    }

//    public List<QueryAccessDto> listQueryAccessByUsername (String username)
//            throws KustvaktException {
//        List<QueryAccess> accessList = new ArrayList<>();
//        if (adminDao.isAdmin(username)) {
//            accessList = accessDao.retrieveAllAccess();
//        }
//        else {
//            List<UserGroup> groups = userGroupService
//                    .retrieveUserGroup(username);
//            for (UserGroup g : groups) {
//                if (userGroupService.isUserGroupAdmin(username, g)) {
//                    accessList.addAll(
//                            accessDao.retrieveActiveAccessByGroup(g.getId()));
//                }
//            }
//        }
//        return accessConverter.createQueryAccessDto(accessList);
//    }
//
//    public List<QueryAccessDto> listQueryAccessByQuery (String username,
//            String queryCreator, String queryName) throws KustvaktException {
//
//        List<QueryAccess> accessList;
//        if (adminDao.isAdmin(username)) {
//            accessList = accessDao.retrieveAllAccessByQuery(queryCreator,
//                    queryName);
//        }
//        else {
//            accessList = accessDao.retrieveActiveAccessByQuery(queryCreator,
//                    queryName);
//            List<QueryAccess> filteredAccessList = new ArrayList<>();
//            for (QueryAccess access : accessList) {
//                UserGroup userGroup = access.getUserGroup();
//                if (userGroupService.isUserGroupAdmin(username, userGroup)) {
//                    filteredAccessList.add(access);
//                }
//            }
//            accessList = filteredAccessList;
//        }
//        return accessConverter.createQueryAccessDto(accessList);
//    }

    public List<RoleDto> listRolesByGroup (String username,
            String groupName, boolean hasQuery) throws KustvaktException {
        UserGroup userGroup = userGroupService
                .retrieveUserGroupByName(groupName);

        Set<Role> roles;
        if (adminDao.isAdmin(username)
                || userGroupService.isUserGroupAdmin(username, userGroup)) {
            roles = roleDao.retrieveRoleByGroupId(userGroup.getId(), hasQuery);

        }
        else {
            throw new KustvaktException(StatusCodes.AUTHORIZATION_FAILED,
                    "Unauthorized operation for user: " + username, username);
        }
        return roleConverter.createRoleDto(roles);
    }

    @Deprecated
    public void deleteRoleById (int roleId, String username)
            throws KustvaktException {

        Role role = roleDao.retrieveRoleById(roleId);
        UserGroup userGroup = role.getUserGroup();
        if (userGroupService.isUserGroupAdmin(username, userGroup)
                || adminDao.isAdmin(username)) {
            roleDao.deleteRole(roleId);
        }
        else {
            throw new KustvaktException(StatusCodes.AUTHORIZATION_FAILED,
                    "Unauthorized operation for user: " + username, username);
        }

    }
    
    public void deleteRoleByGroupAndQuery (String groupName,
            String queryCreator, String queryName, String deleteBy)
            throws KustvaktException {
        UserGroup userGroup = userGroupDao.retrieveGroupByName(groupName,
                false);
        if (userGroupService.isUserGroupAdmin(deleteBy, userGroup)
                || adminDao.isAdmin(deleteBy)) {
            roleDao.deleteRoleByGroupAndQuery(groupName, queryCreator,
                    queryName);
        }
        else {
            throw new KustvaktException(StatusCodes.AUTHORIZATION_FAILED,
                    "Unauthorized operation for user: " + deleteBy, deleteBy);
        }

    }

    public JsonNode retrieveKoralQuery (String username, String queryName,
            String createdBy, QueryType queryType) throws KustvaktException {
        QueryDO query = searchQueryByName(username, queryName, createdBy,
                queryType);
        String koralQuery = query.getKoralQuery();
        JsonNode kq = JsonUtils.readTree(koralQuery);
        return kq;
    }

    public JsonNode retrieveFieldValues (String username, String queryName,
            String createdBy, QueryType queryType, String fieldName)
            throws KustvaktException {

        ParameterChecker.checkStringValue(fieldName, "fieldName");

        //        if (!adminDao.isAdmin(username)) {
        //            throw new KustvaktException(StatusCodes.AUTHORIZATION_FAILED,
        //                    "Unauthorized operation for user: " + username, username);
        //        }

        if (fieldName.equals("tokens") || fieldName.equals("base")) {
            throw new KustvaktException(StatusCodes.NOT_ALLOWED,
                    "Retrieving values of field " + fieldName
                            + " is not allowed.");
        }
        else {
            QueryDO query = searchQueryByName(username, queryName, createdBy,
                    queryType);
            String koralQuery = query.getKoralQuery();
            return krill.getFieldValuesForVC(koralQuery, fieldName);
        }
    }

    public QueryDO searchQueryByName (String username, String queryName,
            String createdBy, QueryType queryType) throws KustvaktException {
        QueryDO query = queryDao.retrieveQueryByName(queryName, createdBy);
        if (query == null) {
            String code = createdBy + "/" + queryName;
            throw new KustvaktException(StatusCodes.NO_RESOURCE_FOUND,
                    queryType.displayName() + " " + code + " is not found.",
                    String.valueOf(code));
        }
        checkQueryAccess(query, username);
        return query;
    }

	public QueryDto retrieveQueryByName (String username, String queryName,
            String createdBy, QueryType queryType, double apiVersion) 
            		throws KustvaktException {
        QueryDO query = searchQueryByName(username, queryName, createdBy,
                queryType);

        String statistics = null;
        String json = "";
		if (query.getQueryType().equals(QueryType.VIRTUAL_CORPUS)) {
			if (query.isCached()) {
				List<String> cqList = new ArrayList<>(1);
				cqList.add("referTo " + query.getName());
				json = buildKoralQueryFromCorpusQuery(cqList, apiVersion);
			}
			else { 
				json = query.getKoralQuery();
			}
			statistics = krill.getStatistics(json);
		}
        return converter.createQueryDto(query, statistics);
    }
	
    //EM: unused
	@Deprecated
    public QueryDto searchQueryById (String username, int queryId)
            throws KustvaktException {

        QueryDO query = queryDao.retrieveQueryById(queryId);
        checkQueryAccess(query, username);
        // String json = query.getKoralQuery();
        // String statistics = krill.getStatistics(json);
        return converter.createQueryDto(query, null);
    }

    private void checkQueryAccess (QueryDO query, String username)
            throws KustvaktException {
        ResourceType type = query.getType();

        if (!adminDao.isAdmin(username)
                && !username.equals(query.getCreatedBy())) {
            if (type.equals(ResourceType.PRIVATE)
                    || (type.equals(ResourceType.PROJECT)
                            && !hasReadAccess(username, query.getId()))) {
                throw new KustvaktException(StatusCodes.AUTHORIZATION_FAILED,
                        "Unauthorized operation for user: " + username,
                        username);
            }

            else if (ResourceType.PUBLISHED.equals(type)
                    && !username.equals("guest")) {
                // add user in the query's auto group
                UserGroup userGroup = userGroupService
                        .retrieveHiddenUserGroupByQueryId(query.getId());
                try {
                    
                    Role r1= roleDao.retrieveRoleByGroupIdQueryIdPrivilege(
                            userGroup.getId(),query.getId(),
                            PrivilegeType.READ_QUERY);
                    Set<Role> memberRoles = new HashSet<Role>();
                    memberRoles.add(r1);
                    
                    userGroupService.addGroupMember(username, userGroup,
                            "system", memberRoles);    
                    // member roles are not set (not necessary)
                }
                catch (NoResultException ne) {
                    Role r1 = new Role(PredefinedRole.QUERY_ACCESS,
                            PrivilegeType.READ_QUERY, userGroup);
                    roleDao.addRole(r1);
                    Set<Role> memberRoles = new HashSet<Role>();
                    memberRoles.add(r1);
                    
                    userGroupService.addGroupMember(username, userGroup,
                            "system", memberRoles);                
                }
                catch (KustvaktException e) {
                    // member exists
                    // skip adding user to hidden group
                }
            }
            // else VirtualCorpusType.SYSTEM
        }
    }

    private boolean hasReadAccess (String username, int queryId)
            throws KustvaktException {
        Set<Role> roles = roleDao.retrieveRoleByQueryIdAndUsername(queryId,
                username);
        for (Role r :roles) {
            if (r.getPrivilege().equals(PrivilegeType.READ_QUERY))
                return true;
        }
        return false;
    }
}
