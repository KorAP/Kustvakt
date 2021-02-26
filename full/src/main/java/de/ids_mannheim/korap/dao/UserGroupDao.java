package de.ids_mannheim.korap.dao;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.ListJoin;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import de.ids_mannheim.korap.constant.GroupMemberStatus;
import de.ids_mannheim.korap.constant.PredefinedRole;
import de.ids_mannheim.korap.constant.UserGroupStatus;
import de.ids_mannheim.korap.constant.QueryAccessStatus;
import de.ids_mannheim.korap.entity.Role;
import de.ids_mannheim.korap.entity.UserGroup;
import de.ids_mannheim.korap.entity.UserGroupMember;
import de.ids_mannheim.korap.entity.UserGroupMember_;
import de.ids_mannheim.korap.entity.UserGroup_;
import de.ids_mannheim.korap.entity.QueryDO;
import de.ids_mannheim.korap.entity.QueryAccess;
import de.ids_mannheim.korap.entity.QueryAccess_;
import de.ids_mannheim.korap.entity.QueryDO_;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.utils.ParameterChecker;

/**
 * Manages database queries and transactions regarding
 * {@link UserGroup} entity and database table.
 * 
 * @author margaretha
 * 
 * @see UserGroup
 *
 */
@Transactional
@Repository
public class UserGroupDao {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private RoleDao roleDao;

    public int createGroup (String name, String description,
            String createdBy, UserGroupStatus status) throws KustvaktException {
        ParameterChecker.checkStringValue(name, "name");
        ParameterChecker.checkStringValue(createdBy, "createdBy");
        ParameterChecker.checkObjectValue(status, "UserGroupStatus");

        UserGroup group = new UserGroup();
        group.setName(name);
        group.setDescription(description);
        group.setStatus(status);
        group.setCreatedBy(createdBy);
        entityManager.persist(group);

        Set<Role> roles = new HashSet<Role>();
        roles.add(roleDao
                .retrieveRoleById(PredefinedRole.USER_GROUP_ADMIN.getId()));
        roles.add(roleDao
                .retrieveRoleById(PredefinedRole.VC_ACCESS_ADMIN.getId()));

        UserGroupMember owner = new UserGroupMember();
        owner.setUserId(createdBy);
        owner.setCreatedBy(createdBy);
        owner.setStatus(GroupMemberStatus.ACTIVE);
        owner.setGroup(group);
        owner.setRoles(roles);
        entityManager.persist(owner);

        return group.getId();
    }

    public void deleteGroup (int groupId, String deletedBy,
            boolean isSoftDelete) throws KustvaktException {
        ParameterChecker.checkIntegerValue(groupId, "groupId");
        ParameterChecker.checkStringValue(deletedBy, "deletedBy");

        UserGroup group = null;
        try {
            group = retrieveGroupById(groupId);
        }
        catch (NoResultException e) {
            throw new KustvaktException(StatusCodes.NO_RESOURCE_FOUND,
                    "Group " + groupId + " is not found.",
                    "groupId: " + groupId);
        }

        if (isSoftDelete) {
            group.setStatus(UserGroupStatus.DELETED);
            group.setDeletedBy(deletedBy);
            entityManager.merge(group);
        }
        else {
            if (!entityManager.contains(group)) {
                group = entityManager.merge(group);
            }
            entityManager.remove(group);
        }
    }

    public void updateGroup (UserGroup group) throws KustvaktException {
        ParameterChecker.checkObjectValue(group, "user-group");
        entityManager.merge(group);
    }
    
    /**
     * Retrieves the UserGroup by the given group id. This methods
     * does not
     * fetch group members because only group admin is allowed to see
     * them.
     * Group members have to be retrieved separately.
     * 
     * @see UserGroupMember
     * @param groupId
     *            group id
     * @return UserGroup
     * @throws KustvaktException
     */
    public UserGroup retrieveGroupById (int groupId) throws KustvaktException {
        return retrieveGroupById(groupId, false);
    }

    public UserGroup retrieveGroupById (int groupId, boolean fetchMembers)
            throws KustvaktException {
        ParameterChecker.checkIntegerValue(groupId, "groupId");

        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<UserGroup> query =
                criteriaBuilder.createQuery(UserGroup.class);

        Root<UserGroup> root = query.from(UserGroup.class);
        if (fetchMembers) {
            root.fetch(UserGroup_.members);
        }
        query.select(root);
        query.where(criteriaBuilder.equal(root.get(UserGroup_.id), groupId));
        Query q = entityManager.createQuery(query);

        try {
            return (UserGroup) q.getSingleResult();
        }
        catch (NoResultException e) {
            throw new KustvaktException(StatusCodes.NO_RESOURCE_FOUND,
                    "Group with id " + groupId + " is not found",
                    String.valueOf(groupId), e);
        }
    }

    /**
     * Retrieves only user-groups that are active (not hidden or
     * deleted).
     * 
     * @param userId
     *            user id
     * @return a list of UserGroup
     * @throws KustvaktException
     */
    @SuppressWarnings("unchecked")
    public List<UserGroup> retrieveGroupByUserId (String userId)
            throws KustvaktException {
        ParameterChecker.checkStringValue(userId, "userId");

        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<UserGroup> query =
                criteriaBuilder.createQuery(UserGroup.class);

        Root<UserGroup> root = query.from(UserGroup.class);

        ListJoin<UserGroup, UserGroupMember> members =
                root.join(UserGroup_.members);
        Predicate restrictions = criteriaBuilder.and(
                criteriaBuilder.equal(root.get(UserGroup_.status),
                        UserGroupStatus.ACTIVE),
                criteriaBuilder.equal(members.get(UserGroupMember_.userId),
                        userId),
                criteriaBuilder.notEqual(members.get(UserGroupMember_.status),
                        GroupMemberStatus.DELETED));
        // criteriaBuilder.equal(members.get(UserGroupMember_.status),
        // GroupMemberStatus.ACTIVE));

        query.select(root);
        query.where(restrictions);
        Query q = entityManager.createQuery(query);

        try {
            return q.getResultList();
        }
        catch (NoResultException e) {
            throw new KustvaktException(StatusCodes.NO_RESULT_FOUND,
                    "No group for username: " + userId + " is found", userId,
                    e);
        }
    }

    public UserGroup retrieveGroupByName (String groupName, boolean fetchMembers)
            throws KustvaktException {
        ParameterChecker.checkStringValue(groupName, "groupName");

        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<UserGroup> query =
                criteriaBuilder.createQuery(UserGroup.class);

        Root<UserGroup> root = query.from(UserGroup.class);
        if (fetchMembers) {
            root.fetch(UserGroup_.members);
        }
        query.select(root);
        query.where(
                criteriaBuilder.equal(root.get(UserGroup_.name), groupName));
        Query q = entityManager.createQuery(query);

        try {
            return (UserGroup) q.getSingleResult();
        }
        catch (NoResultException e) {
            throw new KustvaktException(StatusCodes.NO_RESOURCE_FOUND,
                    "Group " + groupName + " is not found", groupName, e);
        }
    }

    public UserGroup retrieveHiddenGroupByQuery (int queryId)
            throws KustvaktException {
        ParameterChecker.checkIntegerValue(queryId, "queryId");

        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<UserGroup> criteriaQuery =
                criteriaBuilder.createQuery(UserGroup.class);

        Root<UserGroup> root = criteriaQuery.from(UserGroup.class);
        Join<UserGroup, QueryAccess> access =
                root.join(UserGroup_.queryAccess);
        Join<QueryAccess, QueryDO> query =
                access.join(QueryAccess_.query);

        Predicate p = criteriaBuilder.and(
                criteriaBuilder.equal(root.get(UserGroup_.status),
                        UserGroupStatus.HIDDEN),
                criteriaBuilder.equal(query.get(QueryDO_.id), queryId));

        criteriaQuery.select(root);
        criteriaQuery.where(p);
        Query q = entityManager.createQuery(criteriaQuery);

        try {
            return (UserGroup) q.getSingleResult();
        }
        catch (NoResultException e) {
            throw new KustvaktException(StatusCodes.NO_RESULT_FOUND,
                    "No hidden group for query with id " + queryId
                            + " is found",
                    String.valueOf(queryId), e);
        }

    }

    /**
     * This is an admin function. It retrieves all groups given the
     * userId
     * and status.
     * 
     * @param userId
     * @param status
     * @return a list of {@link UserGroup}s
     * @throws KustvaktException
     */
    @SuppressWarnings("unchecked")
    public List<UserGroup> retrieveGroupByStatus (String userId,
            UserGroupStatus status) throws KustvaktException {

        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<UserGroup> query =
                criteriaBuilder.createQuery(UserGroup.class);

        Root<UserGroup> root = query.from(UserGroup.class);

        Predicate restrictions = null;

        if (userId != null && !userId.isEmpty()) {

            ListJoin<UserGroup, UserGroupMember> members =
                    root.join(UserGroup_.members);
            restrictions = criteriaBuilder.and(criteriaBuilder
                    .equal(members.get(UserGroupMember_.userId), userId));

            if (status != null) {
                restrictions = criteriaBuilder.and(restrictions, criteriaBuilder
                        .equal(root.get(UserGroup_.status), status));
            }
        }
        else if (status != null) {
            restrictions =
                    criteriaBuilder.equal(root.get(UserGroup_.status), status);

        }

        query.select(root);
        if (restrictions != null) {
            query.where(restrictions);
        }
        Query q = entityManager.createQuery(query);

        try {
            return q.getResultList();
        }
        catch (NoResultException e) {
            throw new KustvaktException(StatusCodes.NO_RESULT_FOUND,
                    "No group with status " + status + " is found",
                    status.toString());
        }

    }

    public void addQueryToGroup (QueryDO query, String createdBy,
            QueryAccessStatus status, UserGroup group) {
        QueryAccess accessGroup = new QueryAccess();
        accessGroup.setCreatedBy(createdBy);
        accessGroup.setStatus(status);
        accessGroup.setUserGroup(group);
        accessGroup.setQuery(query);;
        entityManager.persist(accessGroup);
    }

    public void addQueryToGroup (List<QueryDO> queries,
            String createdBy, UserGroup group,
            QueryAccessStatus status) {

        for (QueryDO q : queries) {
            addQueryToGroup(q, createdBy, status, group);
        }
    }

    public void deleteQueryFromGroup (int queryId, int groupId)
            throws KustvaktException {
        ParameterChecker.checkIntegerValue(queryId, "queryId");
        ParameterChecker.checkIntegerValue(groupId, "groupId");

        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<QueryAccess> criteriaQuery =
                criteriaBuilder.createQuery(QueryAccess.class);

        Root<QueryAccess> root = criteriaQuery.from(QueryAccess.class);
        Join<QueryAccess, QueryDO> queryAccess =
                root.join(QueryAccess_.query);
        Join<QueryAccess, UserGroup> group =
                root.join(QueryAccess_.userGroup);

        Predicate query = criteriaBuilder
                .equal(queryAccess.get(QueryDO_.id), queryId);
        Predicate userGroup =
                criteriaBuilder.equal(group.get(UserGroup_.id), groupId);

        criteriaQuery.select(root);
        criteriaQuery.where(criteriaBuilder.and(query, userGroup));
        Query q = entityManager.createQuery(criteriaQuery);
        QueryAccess access =
                (QueryAccess) q.getSingleResult();
        entityManager.remove(access);
    }

}
