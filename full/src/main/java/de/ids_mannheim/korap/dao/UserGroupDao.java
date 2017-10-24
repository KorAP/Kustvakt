package de.ids_mannheim.korap.dao;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import de.ids_mannheim.korap.constants.UserGroupStatus;
import de.ids_mannheim.korap.constants.VirtualCorpusAccessStatus;
import de.ids_mannheim.korap.entity.UserGroup;
import de.ids_mannheim.korap.entity.UserGroupMember;
import de.ids_mannheim.korap.entity.VirtualCorpus;
import de.ids_mannheim.korap.entity.VirtualCorpusAccessGroup;

@Transactional
@Component
public class UserGroupDao {

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
        query.where(criteriaBuilder.equal(root.get("id"), groupId));
        Query q = entityManager.createQuery(query);
        return (UserGroup) q.getSingleResult();
    }

    public void addVCToGroup (List<VirtualCorpus> virtualCorpora,
            String createdBy, UserGroup group) {
        // check role
        
        VirtualCorpusAccessGroup accessGroup;
        for (VirtualCorpus vc : virtualCorpora) {
            accessGroup = new VirtualCorpusAccessGroup();
            accessGroup.setCreatedBy(createdBy);
            accessGroup.setStatus(VirtualCorpusAccessStatus.ACTIVE);
            accessGroup.setUserGroup(group);
            accessGroup.setVirtualCorpus(vc);
            entityManager.persist(accessGroup);
        }
    }
}
