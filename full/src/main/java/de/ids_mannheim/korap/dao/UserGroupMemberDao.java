package de.ids_mannheim.korap.dao;

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

@Transactional
@Repository
public class UserGroupMemberDao {

    @PersistenceContext
    private EntityManager entityManager;

    public void addMember (UserGroupMember member) throws KustvaktException {
        ParameterChecker.checkObjectValue(member, "userGroupMember");
        entityManager.persist(member);
    }

//    @Deprecated
//    public void addMembers (List<UserGroupMember> members)
//            throws KustvaktException {
//        ParameterChecker.checkObjectValue(members, "List<UserGroupMember>");
//
//        for (UserGroupMember member : members) {
//            addMember(member);
//        }
//    }

    public void updateMember (UserGroupMember member)
            throws KustvaktException {
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
            entityManager.persist(member);
        }
        else {
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
            throw new KustvaktException(StatusCodes.NO_RESULT_FOUND,
                    "Username " + userId + " is not found in group " + groupId,
                    userId);
        }

    }

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
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<UserGroupMember> query =
                criteriaBuilder.createQuery(UserGroupMember.class);

        Root<UserGroupMember> root = query.from(UserGroupMember.class);

        Predicate predicate = criteriaBuilder.and(
                criteriaBuilder.equal(root.get(UserGroupMember_.group),
                        groupId),
                criteriaBuilder.notEqual(root.get(UserGroupMember_.status),
                        GroupMemberStatus.DELETED));

        query.select(root);
        query.where(predicate);
        Query q = entityManager.createQuery(query);

        try {
            return q.getResultList();
        }
        catch (NoResultException e) {
            throw new KustvaktException(StatusCodes.NO_RESULT_FOUND,
                    "No member is found in group " + groupId,
                    String.valueOf(groupId));
        }
    }
}
