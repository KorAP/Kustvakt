package de.ids_mannheim.korap.dao;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import de.ids_mannheim.korap.constants.GroupMemberStatus;
import de.ids_mannheim.korap.entity.UserGroup;
import de.ids_mannheim.korap.entity.UserGroupMember;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.utils.ParameterChecker;

@Transactional
@Component
public class UserGroupMemberDao {

    @PersistenceContext
    private EntityManager entityManager;

    public void addMember (UserGroupMember member, UserGroup userGroup)
            throws KustvaktException {
        ParameterChecker.checkObjectValue(member, "userGroupMember");
        ParameterChecker.checkObjectValue(userGroup, "userGroup");

        List<UserGroupMember> members = userGroup.getMembers();
        members.add(member);
        entityManager.persist(userGroup);
    }
    
    public void addMembers (List<UserGroupMember> newMembers, UserGroup userGroup)
            throws KustvaktException {
        ParameterChecker.checkObjectValue(newMembers, "List<UserGroupMember>");
        ParameterChecker.checkObjectValue(userGroup, "userGroup");

        List<UserGroupMember> members = userGroup.getMembers();
        members.addAll(newMembers);
        entityManager.persist(userGroup);
    }
    
    public void approveMember (String userId, int groupId)
            throws KustvaktException {
        ParameterChecker.checkStringValue(userId, "userId");
        ParameterChecker.checkIntegerValue(groupId, "groupId");

        UserGroupMember member = retrieveMemberById(userId, groupId);
        member.setStatus(GroupMemberStatus.ACTIVE);
        entityManager.persist(member);
    }

    public void deleteMember (String userId, int groupId, boolean isSoftDelete)
            throws KustvaktException {
        ParameterChecker.checkStringValue(userId, "userId");
        ParameterChecker.checkIntegerValue(groupId, "groupId");

        UserGroupMember m = retrieveMemberById(userId, groupId);
        if (isSoftDelete) {
            m.setStatus(GroupMemberStatus.DELETED);
            entityManager.persist(m);
        }
        else {
            entityManager.remove(m);
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
                criteriaBuilder.equal(root.get("groupId"), groupId),
                criteriaBuilder.equal(root.get("userId"), userId));

        query.select(root);
        query.where(predicate);
        Query q = entityManager.createQuery(query);
        return (UserGroupMember) q.getSingleResult();
    }

}
