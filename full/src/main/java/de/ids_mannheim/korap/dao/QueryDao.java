package de.ids_mannheim.korap.dao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import de.ids_mannheim.korap.constant.GroupMemberStatus;
import de.ids_mannheim.korap.constant.QueryType;
import de.ids_mannheim.korap.constant.ResourceType;
import de.ids_mannheim.korap.constant.UserGroupStatus;
import de.ids_mannheim.korap.constant.QueryAccessStatus;
import de.ids_mannheim.korap.entity.QueryDO;
import de.ids_mannheim.korap.entity.QueryDO_;
import de.ids_mannheim.korap.entity.UserGroup;
import de.ids_mannheim.korap.entity.UserGroupMember;
import de.ids_mannheim.korap.entity.UserGroupMember_;
import de.ids_mannheim.korap.entity.UserGroup_;
import de.ids_mannheim.korap.entity.QueryAccess;
import de.ids_mannheim.korap.entity.QueryAccess_;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.user.User.CorpusAccess;
import de.ids_mannheim.korap.utils.ParameterChecker;

/**
 * QueryDao manages database queries and transactions
 * regarding virtual corpus and KorAP queries.
 * 
 * @author margaretha
 *
 */
@Transactional
@Repository
public class QueryDao {

    @PersistenceContext
    private EntityManager entityManager;

    public int createQuery (String name, ResourceType type,
            QueryType queryType, CorpusAccess requiredAccess, String koralQuery,
            String definition, String description, String status,
            boolean isCached, String createdBy, String query,
            String queryLanguage) throws KustvaktException {

        QueryDO q = new QueryDO();
        q.setName(name);
        q.setType(type);
        q.setQueryType(queryType);
        q.setRequiredAccess(requiredAccess);
        q.setKoralQuery(koralQuery);
        q.setDefinition(definition);
        q.setDescription(description);
        q.setStatus(status);
        q.setCreatedBy(createdBy);
        q.setCached(isCached);
        q.setQuery(query);
        q.setQueryLanguage(queryLanguage);

        entityManager.persist(q);
        return q.getId();
    }

    public void editQuery (QueryDO query, String name,
            ResourceType type, CorpusAccess requiredAccess, String koralQuery,
            String definition, String description, String status,
            boolean isCached) throws KustvaktException {

        if (name != null && !name.isEmpty()) {
            query.setName(name);
        }
        if (type != null) {
            query.setType(type);
        }
        if (requiredAccess != null) {
            query.setRequiredAccess(requiredAccess);
        }
        if (koralQuery != null) {
            query.setKoralQuery(koralQuery);
        }
        if (definition != null && !definition.isEmpty()) {
            query.setDefinition(definition);
        }
        if (description != null && !description.isEmpty()) {
            query.setDescription(description);
        }
        if (status != null && !status.isEmpty()) {
            query.setStatus(status);
        }
        query.setCached(isCached);
        entityManager.merge(query);
    }

    public void deleteQuery (QueryDO query)
            throws KustvaktException {
        if (!entityManager.contains(query)) {
            query = entityManager.merge(query);
        }
        entityManager.remove(query);

    }

    /**
     * System admin function.
     * 
     * Retrieves queries by creator and type. If type is not
     * specified, retrieves queries of all types. If createdBy is not
     * specified, retrieves queries of all users.
     * 
     * @param type
     *            {@link ResourceType}
     * @param createdBy
     *            username of the query creator
     * @return a list of {@link Query}
     * @throws KustvaktException
     */
    @SuppressWarnings("unchecked")
    public List<QueryDO> retrieveQueryByType (ResourceType type,
            String createdBy, QueryType queryType) throws KustvaktException {

        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<QueryDO> criteriaQuery =
                criteriaBuilder.createQuery(QueryDO.class);
        Root<QueryDO> query = criteriaQuery.from(QueryDO.class);

        Predicate conditions = criteriaBuilder
                .equal(query.get(QueryDO_.queryType), queryType);
        if (createdBy != null && !createdBy.isEmpty()) {
            conditions = criteriaBuilder.and(conditions, criteriaBuilder.equal(
                    query.get(QueryDO_.createdBy), createdBy));
            if (type != null) {
                conditions = criteriaBuilder.and(conditions, criteriaBuilder
                        .equal(query.get(QueryDO_.type), type));
            }
        }
        else if (type != null) {
            conditions = criteriaBuilder.and(conditions, criteriaBuilder
                    .equal(query.get(QueryDO_.type), type));
        }

        criteriaQuery.select(query);
        criteriaQuery.where(conditions);
        Query q = entityManager.createQuery(criteriaQuery);
        return q.getResultList();
    }

    public QueryDO retrieveQueryById (int id) throws KustvaktException {
        ParameterChecker.checkIntegerValue(id, "id");

        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<QueryDO> criteriaQuery =
                criteriaBuilder.createQuery(QueryDO.class);
        Root<QueryDO> query = criteriaQuery.from(QueryDO.class);
        criteriaQuery.select(query);
        criteriaQuery.where(criteriaBuilder.equal(query.get(QueryDO_.id),
                id));

        QueryDO qe = null;
        try {
            Query q = entityManager.createQuery(criteriaQuery);
            qe = (QueryDO) q.getSingleResult();
        }
        catch (NoResultException e) {
            throw new KustvaktException(StatusCodes.NO_RESOURCE_FOUND,
                    "Query with id: " + id + " is not found",
                    String.valueOf(id), e);
        }
        return qe;
    }

    public QueryDO retrieveQueryByName (String queryName, String createdBy)
            throws KustvaktException {
        ParameterChecker.checkStringValue(createdBy, "created_by");
        ParameterChecker.checkStringValue(queryName, "query_name");

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<QueryDO> criteriaQuery =
                builder.createQuery(QueryDO.class);

        Root<QueryDO> query = criteriaQuery.from(QueryDO.class);

        Predicate condition = builder.and(
                builder.equal(query.get(QueryDO_.createdBy),
                        createdBy),
                builder.equal(query.get(QueryDO_.name), queryName));

        criteriaQuery.select(query);
        criteriaQuery.where(condition);

        Query q = entityManager.createQuery(criteriaQuery);
        QueryDO qe = null;
        try {
            qe = (QueryDO) q.getSingleResult();
        }
        catch (NoResultException e) {
            return null;
        }
        catch (NonUniqueResultException e) {
            String code = createdBy + "/" + queryName;
            throw new KustvaktException(StatusCodes.NON_UNIQUE_RESULT_FOUND,
                    "Non unique result found for query: retrieve query by name "
                            + code,
                    String.valueOf(code), e);
        }
        return qe;
    }

    @SuppressWarnings("unchecked")
    public List<QueryDO> retrieveOwnerQuery (String userId,
            QueryType queryType) throws KustvaktException {
        ParameterChecker.checkStringValue(userId, "user_id");

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<QueryDO> cq =
                builder.createQuery(QueryDO.class);

        Root<QueryDO> query = cq.from(QueryDO.class);
        Predicate conditions = builder.and(
                builder.equal(query.get(QueryDO_.createdBy),
                        userId),
                builder.equal(query.get(QueryDO_.queryType),
                        queryType));

        cq.select(query);
        cq.where(conditions);

        Query q = entityManager.createQuery(cq);
        return q.getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<QueryDO> retrieveOwnerQueryByType (String userId,
            ResourceType type) throws KustvaktException {
        ParameterChecker.checkStringValue(userId, "user_id");

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<QueryDO> cq =
                builder.createQuery(QueryDO.class);

        Root<QueryDO> query = cq.from(QueryDO.class);
        cq.select(query);

        Predicate p = builder.and(
                builder.equal(query.get(QueryDO_.createdBy),
                        userId),
                builder.equal(query.get(QueryDO_.type), type));
        cq.where(p);

        Query q = entityManager.createQuery(cq);
        return q.getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<QueryDO> retrieveGroupQueryByUser (String userId, QueryType queryType)
            throws KustvaktException {
        ParameterChecker.checkStringValue(userId, "user_id");

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<QueryDO> cq =
                builder.createQuery(QueryDO.class);

        Root<QueryDO> query = cq.from(QueryDO.class);
        Join<QueryDO, QueryAccess> access =
                query.join(QueryDO_.queryAccess);

        // Predicate corpusStatus = builder.and(
        // builder.notEqual(access.get(QueryAccess_.status),
        // VirtualCorpusAccessStatus.HIDDEN),
        // builder.notEqual(access.get(QueryAccess_.status),
        // VirtualCorpusAccessStatus.DELETED));

        Predicate type = builder
                .equal(query.get(QueryDO_.queryType), queryType);
                
        Predicate accessStatus =
                builder.notEqual(access.get(QueryAccess_.status),
                        QueryAccessStatus.DELETED);

        Predicate userGroupStatus =
                builder.notEqual(access.get(QueryAccess_.userGroup)
                        .get(UserGroup_.status), UserGroupStatus.DELETED);
        Join<UserGroup, UserGroupMember> members = access
                .join(QueryAccess_.userGroup).join(UserGroup_.members);

        Predicate memberStatus = builder.equal(
                members.get(UserGroupMember_.status), GroupMemberStatus.ACTIVE);

        Predicate user =
                builder.equal(members.get(UserGroupMember_.userId), userId);

        cq.select(query);
        cq.where(
                builder.and(type, accessStatus, userGroupStatus, memberStatus, user));

        Query q = entityManager.createQuery(cq);
        return q.getResultList();
    }

    public List<QueryDO> retrieveQueryByUser (String userId,
            QueryType queryType) throws KustvaktException {
        ParameterChecker.checkStringValue(userId, "user_id");
        ParameterChecker.checkObjectValue(queryType, "query_type");

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<QueryDO> criteriaQuery =
                builder.createQuery(QueryDO.class);

        Root<QueryDO> query = criteriaQuery.from(QueryDO.class);
        Predicate predicate = builder.and(
                builder.equal(query.get(QueryDO_.queryType),
                        queryType),
                builder.or(builder.equal(
                        query.get(QueryDO_.createdBy), userId),
                        builder.equal(query.get(QueryDO_.type),
                                ResourceType.SYSTEM)));

        criteriaQuery.select(query);
        criteriaQuery.where(predicate);
        criteriaQuery.distinct(true);
        Query q = entityManager.createQuery(criteriaQuery);

        @SuppressWarnings("unchecked")
        List<QueryDO> queryList = q.getResultList();
        List<QueryDO> groupQuery = retrieveGroupQueryByUser(userId, queryType);
        Set<QueryDO> querySet = new HashSet<QueryDO>();
        querySet.addAll(queryList);
        querySet.addAll(groupQuery);

        List<QueryDO> merger = new ArrayList<QueryDO>(querySet.size());
        merger.addAll(querySet);
        Collections.sort(merger);
        return merger;
    }

    // for admins
    @SuppressWarnings("unchecked")
    public List<QueryDO> retrieveQueryByGroup (int groupId)
            throws KustvaktException {
        ParameterChecker.checkIntegerValue(groupId, "group_id");

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<QueryDO> criteriaQuery =
                builder.createQuery(QueryDO.class);

        Root<QueryDO> query = criteriaQuery.from(QueryDO.class);
        Join<QueryDO, QueryAccess> queryAccess =
                query.join(QueryDO_.queryAccess);
        Join<QueryAccess, UserGroup> accessGroup =
                queryAccess.join(QueryAccess_.userGroup);

        criteriaQuery.select(query);
        criteriaQuery.where(builder.equal(accessGroup.get(UserGroup_.id), groupId));
        Query q = entityManager.createQuery(criteriaQuery);
        return q.getResultList();
    }

}
