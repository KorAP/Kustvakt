package de.ids_mannheim.korap.dao;

import java.util.HashSet;
import java.util.List;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import de.ids_mannheim.korap.constant.PredefinedRole;
import de.ids_mannheim.korap.entity.Role;
import de.ids_mannheim.korap.entity.Role_;
import de.ids_mannheim.korap.entity.UserGroupMember;
import de.ids_mannheim.korap.entity.UserGroupMember_;
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
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

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
        if (member.getRoles() != null) {
            java.util.Set<Role> normalized = new java.util.HashSet<Role>();
            for (Role role : member.getRoles()) {
                if (role.getId() == 0) {
                    entityManager.persist(role);
                    normalized.add(role);
                } else {
                    Role attached = entityManager.contains(role) ? role : entityManager.merge(role);
                    normalized.add(attached);
                }
            }
            member.setRoles(normalized);
        }
        entityManager.persist(member);
        entityManager.flush();
    }

    public void updateMember (UserGroupMember member) throws KustvaktException {
        ParameterChecker.checkObjectValue(member, "UserGroupMember");
        if (member.getRoles() != null) {
            java.util.Set<Role> normalized = new java.util.HashSet<Role>();
            for (Role role : member.getRoles()) {
                if (role.getId() == 0) {
                    entityManager.persist(role);
                    normalized.add(role);
                } else {
                    Role attached = entityManager.contains(role) ? role : entityManager.merge(role);
                    normalized.add(attached);
                }
            }
            member.setRoles(normalized);
        }
        entityManager.merge(member);
    }

    public void deleteMember (UserGroupMember member, String deletedBy)
            throws KustvaktException {
        ParameterChecker.checkObjectValue(member, "UserGroupMember");
        ParameterChecker.checkStringValue(deletedBy, "deletedBy");

        if (!entityManager.contains(member)) {
            member = entityManager.merge(member);
        }

        member.setRoles(new HashSet<>());
        entityManager.remove(member);
    }

    public UserGroupMember retrieveMemberById (String userId, int groupId)
            throws KustvaktException {
        ParameterChecker.checkStringValue(userId, "userId");
        ParameterChecker.checkIntegerValue(groupId, "groupId");

        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<UserGroupMember> query = criteriaBuilder
                .createQuery(UserGroupMember.class);

        Root<UserGroupMember> root = query.from(UserGroupMember.class);

        Predicate predicate = criteriaBuilder.and(
                criteriaBuilder.equal(root.get(UserGroupMember_.group).get("id"),
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
    public List<UserGroupMember> retrieveMemberByRole (int groupId,
            PredefinedRole role) throws KustvaktException {
        ParameterChecker.checkIntegerValue(groupId, "groupId");

        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<UserGroupMember> query = criteriaBuilder
                .createQuery(UserGroupMember.class);

        Root<UserGroupMember> root = query.from(UserGroupMember.class);
        Join<UserGroupMember, Role> memberRole = root.join("roles");

        Predicate predicate = criteriaBuilder.and(
                criteriaBuilder.equal(root.get(UserGroupMember_.group).get("id"),
                        groupId),
                criteriaBuilder.equal(memberRole.get(Role_.NAME), role));

        query.select(root);
        query.where(predicate);
        Query q = entityManager.createQuery(query);
        try {
            return q.getResultList();
        }
        catch (NoResultException e) {
            throw new KustvaktException(
                    StatusCodes.NO_RESULT_FOUND, "No member with role " + role.name()
                            + " is found in group " + groupId,
                    role.name());
        }
    }

    @SuppressWarnings("unchecked")
    public List<UserGroupMember> retrieveMemberByGroupId (int groupId)
            throws KustvaktException {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<UserGroupMember> query = criteriaBuilder
                .createQuery(UserGroupMember.class);

        Root<UserGroupMember> root = query.from(UserGroupMember.class);

        Predicate predicate = criteriaBuilder.and(criteriaBuilder
                .equal(root.get(UserGroupMember_.group).get("id"), groupId));

        query.select(root);
        query.where(predicate);
        Query q = entityManager.createQuery(query);

        try {
            return q.getResultList();
        }
        catch (NoResultException e) {
            throw new KustvaktException(StatusCodes.NO_RESULT_FOUND,
                    "No member in group " + groupId + " is found",
                    String.valueOf(groupId));
        }
    }
}
