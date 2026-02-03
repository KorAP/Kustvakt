package de.ids_mannheim.korap.dao;

import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import de.ids_mannheim.korap.constant.PredefinedRole;
import de.ids_mannheim.korap.constant.PrivilegeType;
import de.ids_mannheim.korap.constant.UserGroupStatus;
import de.ids_mannheim.korap.entity.QueryDO;
import de.ids_mannheim.korap.entity.QueryDO_;
import de.ids_mannheim.korap.entity.Role;
import de.ids_mannheim.korap.entity.Role_;
import de.ids_mannheim.korap.entity.UserGroup;
import de.ids_mannheim.korap.entity.UserGroupMember;
import de.ids_mannheim.korap.entity.UserGroupMember_;
import de.ids_mannheim.korap.entity.UserGroup_;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.utils.ParameterChecker;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.ListJoin;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

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

    public int createGroup (String name, String description, String createdBy,
            UserGroupStatus status) throws KustvaktException {
        ParameterChecker.checkStringValue(name, "name");
        ParameterChecker.checkStringValue(createdBy, "createdBy");
        ParameterChecker.checkObjectValue(status, "UserGroupStatus");

        UserGroup group = new UserGroup();
        group.setName(name);
        group.setDescription(description);
        group.setStatus(status);
        group.setCreatedBy(createdBy);
        group.setCreatedDate(ZonedDateTime.now());
        entityManager.persist(group);
        entityManager.flush();
        
        if (createdBy != "system") {
            Set<Role> roles = createUserGroupAdminRoles(group);
            for (Role role : roles) {
                entityManager.persist(role);
            }
            entityManager.flush();
        
            UserGroupMember owner = new UserGroupMember();
            owner.setUserId(createdBy);
            owner.setGroup(group);
            owner.setRoles(roles);
            entityManager.persist(owner);
            entityManager.flush();
        };
        
        return group.getId();
    }
    
    private Set<Role> createUserGroupAdminRoles (UserGroup group) {
        Set<Role> roles = new HashSet<Role>();
        roles.add(new Role(PredefinedRole.GROUP_ADMIN,
                PrivilegeType.DELETE_MEMBER, group));
        roles.add(new Role(PredefinedRole.GROUP_ADMIN, PrivilegeType.READ_MEMBER,
                group));
        roles.add(new Role(PredefinedRole.GROUP_ADMIN, PrivilegeType.WRITE_MEMBER,
                group));
        roles.add(new Role(PredefinedRole.GROUP_ADMIN, PrivilegeType.SHARE_QUERY,
                group));
        roles.add(new Role(PredefinedRole.GROUP_ADMIN, PrivilegeType.DELETE_QUERY,
                group));
        return roles;
    }

    public void deleteGroup (int groupId, String deletedBy) throws KustvaktException {
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

        // Before deleting the group, detach role associations from members to
        // avoid transient role references during flush
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<UserGroupMember> cq = cb.createQuery(UserGroupMember.class);
        Root<UserGroupMember> memberRoot = cq.from(UserGroupMember.class);
        cq.select(memberRoot);
        cq.where(cb.equal(memberRoot.get(UserGroupMember_.group).get("id"), groupId));
        List<UserGroupMember> members = entityManager.createQuery(cq).getResultList();
        for (UserGroupMember m : members) {
            if (!entityManager.contains(m)) {
                m = entityManager.merge(m);
            }
            m.setRoles(new HashSet<>());
            entityManager.merge(m);
        }

        if (!entityManager.contains(group)) {
            group = entityManager.merge(group);
        }
        entityManager.remove(group);
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
        CriteriaQuery<UserGroup> query = criteriaBuilder
                .createQuery(UserGroup.class);

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
        CriteriaQuery<UserGroup> query = criteriaBuilder
                .createQuery(UserGroup.class);

        Root<UserGroup> root = query.from(UserGroup.class);

        ListJoin<UserGroup, UserGroupMember> members = root
                .join(UserGroup_.members);
        Predicate restrictions = criteriaBuilder.and(
                criteriaBuilder.equal(root.get(UserGroup_.status),
                        UserGroupStatus.ACTIVE),
                criteriaBuilder.equal(members.get(UserGroupMember_.userId),
                        userId));
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

    /** If fetchMembers=true, this method doesn't return groups with empty 
     *  members.
     * 
     * @param groupName
     * @param fetchMembers
     * @return
     * @throws KustvaktException
     */
    public UserGroup retrieveGroupByName (String groupName,
            boolean fetchMembers) throws KustvaktException {
        ParameterChecker.checkStringValue(groupName, "groupName");

        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<UserGroup> query = criteriaBuilder
                .createQuery(UserGroup.class);

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
    
    public UserGroup retrieveHiddenGroupByQueryName (String queryName)
            throws KustvaktException {
        ParameterChecker.checkNameValue(queryName, "queryName");

        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<UserGroup> criteriaQuery = criteriaBuilder
                .createQuery(UserGroup.class);

        Root<UserGroup> root = criteriaQuery.from(UserGroup.class);
        Join<UserGroup, Role> group_role = root.join(UserGroup_.roles);
        Join<Role, QueryDO> query_role = group_role.join(Role_.query);

        Predicate p = criteriaBuilder.and(
                criteriaBuilder.equal(root.get(UserGroup_.status),
                        UserGroupStatus.HIDDEN),
                criteriaBuilder.equal(criteriaBuilder.lower(query_role.get(QueryDO_.name)),
                        queryName.toLowerCase())
        );

        criteriaQuery.select(root);
        criteriaQuery.where(p);
        Query q = entityManager.createQuery(criteriaQuery);

        try {
            return (UserGroup) q.getSingleResult();
        }
        catch (NoResultException e) {
            throw new KustvaktException(StatusCodes.NO_RESOURCE_FOUND,
                    "No hidden group for query " + queryName
                            + " is found",
                    String.valueOf(queryName), e);
        }

    }

    public UserGroup retrieveHiddenGroupByQueryId (int queryId)
            throws KustvaktException {
        ParameterChecker.checkIntegerValue(queryId, "queryId");

        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<UserGroup> criteriaQuery = criteriaBuilder
                .createQuery(UserGroup.class);

        Root<UserGroup> root = criteriaQuery.from(UserGroup.class);
        Join<UserGroup, Role> group_role = root.join(UserGroup_.roles);
        Join<Role, QueryDO> query_role = group_role.join(Role_.query);

        Predicate p = criteriaBuilder.and(
                criteriaBuilder.equal(root.get(UserGroup_.status),
                        UserGroupStatus.HIDDEN),
                criteriaBuilder.equal(query_role.get(QueryDO_.id), queryId));

        criteriaQuery.select(root);
        criteriaQuery.where(p);
        Query q = entityManager.createQuery(criteriaQuery);

        try {
            return (UserGroup) q.getSingleResult();
        }
        catch (NoResultException e) {
            throw new KustvaktException(StatusCodes.NO_RESOURCE_FOUND,
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
        CriteriaQuery<UserGroup> query = criteriaBuilder
                .createQuery(UserGroup.class);

        Root<UserGroup> root = query.from(UserGroup.class);

        Predicate restrictions = null;

        if (userId != null && !userId.isEmpty()) {

            ListJoin<UserGroup, UserGroupMember> members = root
                    .join(UserGroup_.members);
            restrictions = criteriaBuilder.and(criteriaBuilder
                    .equal(members.get(UserGroupMember_.userId), userId));

            if (status != null) {
                restrictions = criteriaBuilder.and(restrictions, criteriaBuilder
                        .equal(root.get(UserGroup_.status), status));
            }
        }
        else if (status != null) {
            restrictions = criteriaBuilder.equal(root.get(UserGroup_.status),
                    status);

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
}