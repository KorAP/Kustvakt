package de.ids_mannheim.korap.dao;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.ListJoin;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import de.ids_mannheim.korap.constant.GroupMemberStatus;
import de.ids_mannheim.korap.constant.UserGroupStatus;
import de.ids_mannheim.korap.constant.VirtualCorpusAccessStatus;
import de.ids_mannheim.korap.entity.UserGroup;
import de.ids_mannheim.korap.entity.UserGroupMember;
import de.ids_mannheim.korap.entity.UserGroupMember_;
import de.ids_mannheim.korap.entity.UserGroup_;
import de.ids_mannheim.korap.entity.VirtualCorpus;
import de.ids_mannheim.korap.entity.VirtualCorpusAccessGroup;

@Transactional
@Repository
public class UserGroupDao {

    public static final String USER_GROUP_ALL = "all";

    @PersistenceContext
    private EntityManager entityManager;

    public void createGroup (String name, String createdBy,
            List<UserGroupMember> members, UserGroupStatus status) {
        UserGroup group = new UserGroup();
        group.setName(name);
        group.setStatus(status);
        group.setCreatedBy(createdBy);
        group.setMembers(members);
        entityManager.persist(group);
    }

    public void deleteGroup (int groupId, String deletedBy,
            boolean isSoftDelete) {
        UserGroup group = retrieveGroupById(groupId);
        if (isSoftDelete) {
            group.setStatus(UserGroupStatus.DELETED);
            group.setDeletedBy(deletedBy);
            entityManager.persist(group);
        }
        else {
            entityManager.remove(group);
        }
    }

    public UserGroup retrieveGroupById (int groupId) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<UserGroup> query =
                criteriaBuilder.createQuery(UserGroup.class);

        Root<UserGroup> root = query.from(UserGroup.class);
        query.select(root);
        query.where(criteriaBuilder.equal(root.get(UserGroup_.id), groupId));
        Query q = entityManager.createQuery(query);
        return (UserGroup) q.getSingleResult();
    }

    public List<UserGroup> retrieveGroupByUserId (String userId) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<UserGroup> query =
                criteriaBuilder.createQuery(UserGroup.class);

        Root<UserGroup> root = query.from(UserGroup.class);

        Predicate allUserGroup = criteriaBuilder.and(
                criteriaBuilder.equal(root.get(UserGroup_.name),
                        USER_GROUP_ALL),
                criteriaBuilder.notEqual(root.get(UserGroup_.status),
                        UserGroupStatus.DELETED));


        ListJoin<UserGroup, UserGroupMember> members =
                root.join(UserGroup_.members);
        Predicate memberships = criteriaBuilder.and(
                criteriaBuilder.equal(members.get(UserGroupMember_.userId),
                        userId),
                criteriaBuilder.equal(members.get(UserGroupMember_.status),
                        GroupMemberStatus.ACTIVE));


        query.select(root);
        query.where(criteriaBuilder.and(allUserGroup, memberships));
        Query q = entityManager.createQuery(query);
        return q.getResultList();
    }

    public void addVCToGroup (VirtualCorpus virtualCorpus, String createdBy,
            VirtualCorpusAccessStatus status, UserGroup group) {
        VirtualCorpusAccessGroup accessGroup = new VirtualCorpusAccessGroup();
        accessGroup.setCreatedBy(createdBy);
        accessGroup.setStatus(status);
        accessGroup.setUserGroup(group);
        accessGroup.setVirtualCorpus(virtualCorpus);
        entityManager.persist(accessGroup);
    }

    public void addVCToGroup (List<VirtualCorpus> virtualCorpora,
            String createdBy, UserGroup group,
            VirtualCorpusAccessStatus status) {

        for (VirtualCorpus vc : virtualCorpora) {
            addVCToGroup(vc, createdBy, status, group);
        }
    }

}
