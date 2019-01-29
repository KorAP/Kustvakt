package de.ids_mannheim.korap.dao;

import java.util.HashSet;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
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
import de.ids_mannheim.korap.entity.Role;
import de.ids_mannheim.korap.entity.Role_;
import de.ids_mannheim.korap.entity.UserGroupMember;
import de.ids_mannheim.korap.entity.UserGroupMember_;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.utils.ParameterChecker;

/**
 * Manages database queries and transactions regarding
 * {@link UserGroupMember} entity and
 * database table.
 * 
 * @author margaretha
 * @see UserGroupMember
 *
 */
@Transactional
@Repository
public class UserGroupMemberDao {

    @PersistenceContext
    private EntityManager entityManager;

    public void addMember (UserGroupMember member) throws KustvaktException {
        ParameterChecker.checkObjectValue(member, "userGroupMember");
        entityManager.persist(member);
    }

    public void updateMember (UserGroupMember member) throws KustvaktException {
        ParameterChecker.checkObjectValue(member, "UserGroupMember");
        entityManager.merge(member);
    }

    public void deleteMember (UserGroupMember member, String deletedBy,
            boolean isSoftDelete) throws KustvaktException {
        ParameterChecker.checkObjectValue(member, "UserGroupMember");
        ParameterChecker.checkStringValue(deletedBy, "deletedBy");

        if (!entityManager.contains(member)) {
            member = entityManager.merge(member);
        }

        if (isSoftDelete) {
            member.setStatus(GroupMemberStatus.DELETED);
            member.setDeletedBy(deletedBy);
            member.setRoles(new HashSet<>());
            entityManager.persist(member);
        }
        else {
            member.setRoles(new HashSet<>());
            entityManager.remove(member);
        }
    }

    public UserGroupMember retrieveMemberById (String userId, int groupId)
            throws KustvaktException {
        ParameterChecker.checkStringValue(userId, "userId");
        ParameterChecker.checkIntegerValue(groupId, "groupId");

        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<UserGroupMember> query =
                criteriaBuilder.createQuery(UserGroupMember.class);

        Root<UserGroupMember> root = query.from(UserGroupMember.class);

        Predicate predicate = criteriaBuilder.and(
                criteriaBuilder.equal(root.get(UserGroupMember_.group),
                        groupId),
                criteriaBuilder.equal(root.get(UserGroupMember_.userId),
                        userId));

        query.select(root);
        query.where(predicate);
        Query q = entityManager.createQuery(query);

        try {
            return (UserGroupMember) q.getSingleResult();
        }
        catch (NoResultException e) {
            throw new KustvaktException(StatusCodes.GROUP_MEMBER_NOT_FOUND,
                    userId + " is not found in the group", userId);
        }

    }

    @SuppressWarnings("unchecked")
    public List<UserGroupMember> retrieveMemberByRole (int groupId, int roleId)
            throws KustvaktException {
        ParameterChecker.checkIntegerValue(roleId, "roleId");
        ParameterChecker.checkIntegerValue(groupId, "groupId");

        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<UserGroupMember> query =
                criteriaBuilder.createQuery(UserGroupMember.class);

        Root<UserGroupMember> root = query.from(UserGroupMember.class);
        Join<UserGroupMember, Role> memberRole = root.join("roles");

        Predicate predicate = criteriaBuilder.and(
                criteriaBuilder.equal(root.get(UserGroupMember_.group),
                        groupId),
                criteriaBuilder.equal(root.get(UserGroupMember_.status),
                        GroupMemberStatus.ACTIVE),
                criteriaBuilder.equal(memberRole.get(Role_.id), roleId));

        query.select(root);
        query.where(predicate);
        Query q = entityManager.createQuery(query);
        try {
            return q.getResultList();
        }
        catch (NoResultException e) {
            throw new KustvaktException(
                    StatusCodes.NO_RESULT_FOUND, "No member with role " + roleId
                            + " is found in group " + groupId,
                    String.valueOf(roleId));
        }
    }

    public List<UserGroupMember> retrieveMemberByGroupId (int groupId)
            throws KustvaktException {
        return retrieveMemberByGroupId(groupId, false);
    }

    @SuppressWarnings("unchecked")
    public List<UserGroupMember> retrieveMemberByGroupId (int groupId,
            boolean isAdmin) throws KustvaktException {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<UserGroupMember> query =
                criteriaBuilder.createQuery(UserGroupMember.class);

        Root<UserGroupMember> root = query.from(UserGroupMember.class);

        Predicate predicate = criteriaBuilder.and(criteriaBuilder
                .equal(root.get(UserGroupMember_.group), groupId));

        if (!isAdmin) {
            predicate = criteriaBuilder.and(predicate,
                    criteriaBuilder.notEqual(root.get(UserGroupMember_.status),
                            GroupMemberStatus.DELETED));
        }

        query.select(root);
        query.where(predicate);
        Query q = entityManager.createQuery(query);

        try {
            return q.getResultList();
        }
        catch (NoResultException e) {
            throw new KustvaktException(StatusCodes.NO_RESULT_FOUND,
                    "No member in group " + groupId+" is found",
                    String.valueOf(groupId));
        }
    }
}
