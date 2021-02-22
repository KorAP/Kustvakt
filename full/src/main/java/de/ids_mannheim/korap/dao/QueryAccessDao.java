package de.ids_mannheim.korap.dao;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import de.ids_mannheim.korap.constant.QueryAccessStatus;
import de.ids_mannheim.korap.entity.UserGroup;
import de.ids_mannheim.korap.entity.UserGroup_;
import de.ids_mannheim.korap.entity.QueryAccess;
import de.ids_mannheim.korap.entity.QueryAccess_;
import de.ids_mannheim.korap.entity.QueryDO;
import de.ids_mannheim.korap.entity.QueryDO_;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.utils.ParameterChecker;

/**
 * Manages database queries and transactions regarding
 * {@link QueryAccess} entity and its corresponding database
 * table.
 * 
 * @author margaretha
 *
 * @see QueryAccess
 * @see Query
 */
@Transactional
@Repository
public class QueryAccessDao {

    @PersistenceContext
    private EntityManager entityManager;

    public QueryAccess retrieveAccessById (int accessId)
            throws KustvaktException {
        ParameterChecker.checkIntegerValue(accessId, "accessId");

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<QueryAccess> query =
                builder.createQuery(QueryAccess.class);

        Root<QueryAccess> access =
                query.from(QueryAccess.class);
        query.select(access);
        query.where(
                builder.equal(access.get(QueryAccess_.id), accessId));
        Query q = entityManager.createQuery(query);
        try{
            return (QueryAccess) q.getSingleResult();
        }
        catch (NoResultException e) {
            throw new KustvaktException(StatusCodes.NO_RESOURCE_FOUND,
                    "Query access is not found",
                    String.valueOf(accessId));
        }
    }

    // for query-access admins
    public List<QueryAccess> retrieveActiveAccessByQuery (int queryId)
            throws KustvaktException {
        ParameterChecker.checkIntegerValue(queryId, "queryId");

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<QueryAccess> query =
                builder.createQuery(QueryAccess.class);

        Root<QueryAccess> access =
                query.from(QueryAccess.class);
        Join<QueryAccess, QueryDO> accessQuery =
                access.join(QueryAccess_.query);

        Predicate p = builder.and(
                builder.equal(accessQuery.get(QueryDO_.id), queryId),
                builder.equal(access.get(QueryAccess_.status),
                        QueryAccessStatus.ACTIVE));
        query.select(access);
        query.where(p);
        TypedQuery<QueryAccess> q = entityManager.createQuery(query);
        return q.getResultList();
    }

    public List<QueryAccess> retrieveActiveAccessByQuery (String queryCreator,
            String queryName) throws KustvaktException {
        ParameterChecker.checkStringValue(queryCreator, "queryCreator");
        ParameterChecker.checkStringValue(queryName, "queryName");

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<QueryAccess> query =
                builder.createQuery(QueryAccess.class);

        Root<QueryAccess> access =
                query.from(QueryAccess.class);
        Join<QueryAccess, QueryDO> accessQuery =
                access.join(QueryAccess_.query);

        Predicate p = builder.and(
                builder.equal(accessQuery.get(QueryDO_.name), queryName),
                builder.equal(accessQuery.get(QueryDO_.createdBy), queryCreator),
                builder.equal(access.get(QueryAccess_.status),
                        QueryAccessStatus.ACTIVE));
        query.select(access);
        query.where(p);
        TypedQuery<QueryAccess> q = entityManager.createQuery(query);
        return q.getResultList();
    }
    
    public List<QueryAccess> retrieveAllAccess ()
            throws KustvaktException {

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<QueryAccess> query =
                builder.createQuery(QueryAccess.class);
        Root<QueryAccess> access =
                query.from(QueryAccess.class);
        query.select(access);
        TypedQuery<QueryAccess> q = entityManager.createQuery(query);
        return q.getResultList();
    }

    public List<QueryAccess> retrieveAllAccessByQuery (String queryCreator,
            String queryName) throws KustvaktException {
        ParameterChecker.checkStringValue(queryCreator, "queryCreator");
        ParameterChecker.checkStringValue(queryName, "queryName");

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<QueryAccess> query =
                builder.createQuery(QueryAccess.class);

        Root<QueryAccess> access =
                query.from(QueryAccess.class);
        Join<QueryAccess, QueryDO> accessQuery =
                access.join(QueryAccess_.query);

        Predicate conditions = builder.and(
                builder.equal(accessQuery.get(QueryDO_.createdBy),
                        queryCreator),
                builder.equal(accessQuery.get(QueryDO_.name), queryName));
        query.select(access);
        query.where(conditions);
        TypedQuery<QueryAccess> q = entityManager.createQuery(query);
        return q.getResultList();
    }

    public List<QueryAccess> retrieveAllAccessByGroup (int groupId)
            throws KustvaktException {
        ParameterChecker.checkIntegerValue(groupId, "groupId");

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<QueryAccess> query =
                builder.createQuery(QueryAccess.class);

        Root<QueryAccess> access =
                query.from(QueryAccess.class);
        Join<QueryAccess, UserGroup> accessQuery =
                access.join(QueryAccess_.userGroup);

        query.select(access);
        query.where(builder.equal(accessQuery.get(UserGroup_.id), groupId));
        TypedQuery<QueryAccess> q = entityManager.createQuery(query);
        return q.getResultList();
    }

    public List<QueryAccess> retrieveActiveAccessByGroup (int groupId)
            throws KustvaktException {
        ParameterChecker.checkIntegerValue(groupId, "groupId");

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<QueryAccess> query =
                builder.createQuery(QueryAccess.class);

        Root<QueryAccess> access =
                query.from(QueryAccess.class);
        Join<QueryAccess, UserGroup> accessQuery =
                access.join(QueryAccess_.userGroup);

        Predicate p =
                builder.and(builder.equal(accessQuery.get(UserGroup_.id), groupId),
                        builder.equal(access.get(QueryAccess_.status),
                                QueryAccessStatus.ACTIVE));

        query.select(access);
        query.where(p);
        TypedQuery<QueryAccess> q = entityManager.createQuery(query);
        return q.getResultList();
    }

    /**
     * Hidden accesses are only created for published or system query.
     * 
     * Warn: The actual hidden accesses are not checked.
     * 
     * @param queryId
     *            queryId
     * @return true if there is a hidden access, false otherwise
     * @throws KustvaktException
     */
    public QueryAccess retrieveHiddenAccess (int queryId)
            throws KustvaktException {
        ParameterChecker.checkIntegerValue(queryId, "queryId");

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<QueryAccess> query =
                builder.createQuery(QueryAccess.class);

        Root<QueryAccess> access =
                query.from(QueryAccess.class);
        Join<QueryAccess, QueryDO> accessQuery =
                access.join(QueryAccess_.query);

        Predicate p = builder.and(
                builder.equal(accessQuery.get(QueryDO_.id), queryId),
                builder.equal(access.get(QueryAccess_.status),
                        QueryAccessStatus.HIDDEN)
        // ,
        // builder.notEqual(access.get(QueryAccess_.deletedBy),
        // "NULL")
        );

        query.select(access);
        query.where(p);

        try {
            Query q = entityManager.createQuery(query);
            return (QueryAccess) q.getSingleResult();
        }
        catch (NoResultException e) {
            return null;
        }
    }

    public void createAccessToQuery (QueryDO query,
            UserGroup userGroup, String createdBy,
            QueryAccessStatus status) {
        QueryAccess queryAccess = new QueryAccess();
        queryAccess.setQuery(query);
        queryAccess.setUserGroup(userGroup);
        queryAccess.setCreatedBy(createdBy);
        queryAccess.setStatus(status);
        entityManager.persist(queryAccess);
    }

    public void deleteAccess (QueryAccess access, String deletedBy) {
        // soft delete

        // hard delete
        if (!entityManager.contains(access)) {
            access = entityManager.merge(access);
        }
        entityManager.remove(access);
    }

}
