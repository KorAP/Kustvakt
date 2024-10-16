package de.ids_mannheim.korap.dao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import de.ids_mannheim.korap.constant.QueryType;
import de.ids_mannheim.korap.constant.ResourceType;
import de.ids_mannheim.korap.entity.QueryDO;
import de.ids_mannheim.korap.entity.QueryDO_;
import de.ids_mannheim.korap.entity.Role;
import de.ids_mannheim.korap.entity.Role_;
import de.ids_mannheim.korap.entity.UserGroupMember;
import de.ids_mannheim.korap.entity.UserGroupMember_;
import de.ids_mannheim.korap.entity.UserGroup_;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.user.User.CorpusAccess;
import de.ids_mannheim.korap.utils.ParameterChecker;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.NonUniqueResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

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

    public int createQuery (String name, ResourceType type, QueryType queryType,
            CorpusAccess requiredAccess, String koralQuery, String definition,
            String description, String status, boolean isCached,
            String createdBy, String query, String queryLanguage)
            throws KustvaktException {

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

    public void editQuery (QueryDO queryDO, String name, ResourceType type,
            CorpusAccess requiredAccess, String koralQuery, String definition,
            String description, String status, boolean isCached,
            String queryStr, String queryLanguage) throws KustvaktException {

        if (name != null && !name.isEmpty()) {
            queryDO.setName(name);
        }
        if (type != null) {
            queryDO.setType(type);
        }
        if (requiredAccess != null) {
            queryDO.setRequiredAccess(requiredAccess);
        }
        if (koralQuery != null) {
            queryDO.setKoralQuery(koralQuery);
        }
        if (definition != null && !definition.isEmpty()) {
            queryDO.setDefinition(definition);
        }
        if (description != null && !description.isEmpty()) {
            queryDO.setDescription(description);
        }
        if (status != null && !status.isEmpty()) {
            queryDO.setStatus(status);
        }
        if (queryStr != null && !queryStr.isEmpty()) {
            queryDO.setQuery(queryStr);
        }
        if (queryLanguage != null && !queryLanguage.isEmpty()) {
            queryDO.setQueryLanguage(queryLanguage);
        }
        queryDO.setCached(isCached);
        entityManager.merge(queryDO);
    }

    public void deleteQuery (QueryDO query) throws KustvaktException {
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
        CriteriaQuery<QueryDO> criteriaQuery = criteriaBuilder
                .createQuery(QueryDO.class);
        Root<QueryDO> query = criteriaQuery.from(QueryDO.class);

        Predicate conditions = criteriaBuilder
                .equal(query.get(QueryDO_.queryType), queryType);
        if (createdBy != null && !createdBy.isEmpty()) {
            conditions = criteriaBuilder.and(conditions, criteriaBuilder
                    .equal(query.get(QueryDO_.createdBy), createdBy));
            if (type != null) {
                conditions = criteriaBuilder.and(conditions,
                        criteriaBuilder.equal(query.get(QueryDO_.type), type));
            }
        }
        else if (type != null) {
            conditions = criteriaBuilder.and(conditions,
                    criteriaBuilder.equal(query.get(QueryDO_.type), type));
        }

        criteriaQuery.select(query);
        criteriaQuery.where(conditions);
        Query q = entityManager.createQuery(criteriaQuery);
        return q.getResultList();
    }

    public QueryDO retrieveQueryById (int id) throws KustvaktException {
        ParameterChecker.checkIntegerValue(id, "id");

        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<QueryDO> criteriaQuery = criteriaBuilder
                .createQuery(QueryDO.class);
        Root<QueryDO> query = criteriaQuery.from(QueryDO.class);
        criteriaQuery.select(query);
        criteriaQuery.where(criteriaBuilder.equal(query.get(QueryDO_.id), id));

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
        ParameterChecker.checkStringValue(createdBy, "createdBy");
        ParameterChecker.checkStringValue(queryName, "queryName");

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<QueryDO> criteriaQuery = builder
                .createQuery(QueryDO.class);

        Root<QueryDO> query = criteriaQuery.from(QueryDO.class);

        Predicate condition = builder.and(
                builder.equal(query.get(QueryDO_.createdBy), createdBy),
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
    public List<QueryDO> retrieveOwnerQuery (String userId, QueryType queryType)
            throws KustvaktException {
        ParameterChecker.checkStringValue(userId, "userId");

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<QueryDO> cq = builder.createQuery(QueryDO.class);

        Root<QueryDO> query = cq.from(QueryDO.class);
        Predicate conditions = builder.and(
                builder.equal(query.get(QueryDO_.createdBy), userId),
                builder.equal(query.get(QueryDO_.queryType), queryType));

        cq.select(query);
        cq.where(conditions);

        Query q = entityManager.createQuery(cq);
        return q.getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<QueryDO> retrieveOwnerQueryByType (String userId,
            ResourceType type) throws KustvaktException {
        ParameterChecker.checkStringValue(userId, "userId");

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<QueryDO> cq = builder.createQuery(QueryDO.class);

        Root<QueryDO> query = cq.from(QueryDO.class);
        cq.select(query);

        Predicate p = builder.and(
                builder.equal(query.get(QueryDO_.createdBy), userId),
                builder.equal(query.get(QueryDO_.type), type));
        cq.where(p);

        Query q = entityManager.createQuery(cq);
        return q.getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<QueryDO> retrieveGroupQueryByUser (String userId,
            QueryType queryType) throws KustvaktException {
        ParameterChecker.checkStringValue(userId, "userId");

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<QueryDO> cq = builder.createQuery(QueryDO.class);

        Root<QueryDO> query = cq.from(QueryDO.class);
        Join<QueryDO, Role> roles = query.join(QueryDO_.roles);
        Join<Role, UserGroupMember> members = roles
                .join(Role_.userGroupMembers);
        
        Predicate type = builder.equal(query.get(QueryDO_.queryType),
                queryType);
        Predicate user = builder.equal(members.get(UserGroupMember_.userId),
                userId);

        cq.select(query);
        cq.where(builder.and(type, user));

        Query q = entityManager.createQuery(cq);
        return q.getResultList();
    }

    public List<QueryDO> retrieveQueryByUser (String userId,
            QueryType queryType) throws KustvaktException {
        ParameterChecker.checkStringValue(userId, "userId");
        ParameterChecker.checkObjectValue(queryType, "queryType");

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<QueryDO> criteriaQuery = builder
                .createQuery(QueryDO.class);

        Root<QueryDO> query = criteriaQuery.from(QueryDO.class);
        Predicate predicate = builder.and(
                builder.equal(query.get(QueryDO_.queryType), queryType),
                builder.or(builder.equal(query.get(QueryDO_.createdBy), userId),
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
        ParameterChecker.checkIntegerValue(groupId, "groupId");

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<QueryDO> criteriaQuery = builder
                .createQuery(QueryDO.class);

        Root<QueryDO> query = criteriaQuery.from(QueryDO.class);
        Join<QueryDO, Role> query_role = query
                .join(QueryDO_.roles);

        criteriaQuery.select(query);
        criteriaQuery.where(builder.equal(
                query_role.get(Role_.userGroup).get(UserGroup_.id), groupId));
        Query q = entityManager.createQuery(criteriaQuery);
        return q.getResultList();
    }

    public Long countNumberOfQuery (String userId, QueryType queryType)
            throws KustvaktException {
        ParameterChecker.checkStringValue(userId, "userId");
        ParameterChecker.checkObjectValue(queryType, "queryType");

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> cq = builder.createQuery(Long.class);

        Root<QueryDO> query = cq.from(QueryDO.class);
        Predicate conditions = builder.and(
                builder.equal(query.get(QueryDO_.createdBy), userId),
                builder.equal(query.get(QueryDO_.queryType), queryType));

        cq.select(builder.count(query));
        cq.where(conditions);

        TypedQuery<Long> q = entityManager.createQuery(cq);
        return q.getSingleResult();
    }

}
