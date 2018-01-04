package de.ids_mannheim.korap.dao;

import java.util.ArrayList;
import java.util.List;

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
import de.ids_mannheim.korap.constant.VirtualCorpusAccessStatus;
import de.ids_mannheim.korap.entity.Role;
import de.ids_mannheim.korap.entity.UserGroup;
import de.ids_mannheim.korap.entity.UserGroupMember;
import de.ids_mannheim.korap.entity.UserGroupMember_;
import de.ids_mannheim.korap.entity.UserGroup_;
import de.ids_mannheim.korap.entity.VirtualCorpus;
import de.ids_mannheim.korap.entity.VirtualCorpusAccess;
import de.ids_mannheim.korap.entity.VirtualCorpusAccess_;
import de.ids_mannheim.korap.entity.VirtualCorpus_;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.utils.ParameterChecker;

@Transactional
@Repository
public class UserGroupDao {

    public static final String USER_GROUP_ALL = "all";

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private RoleDao roleDao;

    public int createGroup (String name, String createdBy,
            UserGroupStatus status) throws KustvaktException {
        ParameterChecker.checkStringValue(name, "name");
        ParameterChecker.checkStringValue(createdBy, "createdBy");
        ParameterChecker.checkObjectValue(status, "UserGroupStatus");

        UserGroup group = new UserGroup();
        group.setName(name);
        group.setStatus(status);
        group.setCreatedBy(createdBy);
        entityManager.persist(group);

        List<Role> roles = new ArrayList<Role>(2);
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
            throw new KustvaktException(StatusCodes.NO_RESULT_FOUND,
                    "groupId: " + groupId);
        }

        if (isSoftDelete) {
            group.setStatus(UserGroupStatus.DELETED);
            group.setDeletedBy(deletedBy);
            entityManager.persist(group);
        }
        else {
            entityManager.remove(group);
        }
    }

    public void editGroupName (int groupId, String name)
            throws KustvaktException {
        UserGroup group = retrieveGroupById(groupId);
        group.setName(name);
        entityManager.persist(group);
    }

    /** Retrieves the UserGroup by the given group id. This methods does not 
     *  fetch group members because only group admin is allowed to see them. 
     *  Group members have to be retrieved separately.
     * 
     * @see UserGroupMember
     * @param groupId group id
     * @return UserGroup
     * @throws KustvaktException 
     */
    public UserGroup retrieveGroupById (int groupId) throws KustvaktException {
        ParameterChecker.checkIntegerValue(groupId, "groupId");

        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<UserGroup> query =
                criteriaBuilder.createQuery(UserGroup.class);

        Root<UserGroup> root = query.from(UserGroup.class);
        query.select(root);
        query.where(criteriaBuilder.equal(root.get(UserGroup_.id), groupId));
        Query q = entityManager.createQuery(query);

        UserGroup userGroup;
        try {
            userGroup = (UserGroup) q.getSingleResult();
        }
        catch (NoResultException e) {
            throw new KustvaktException(StatusCodes.NO_RESULT_FOUND,
                    "No result found for query: retrieve group by id "
                            + groupId,
                    String.valueOf(groupId), e);
        }
        return userGroup;
    }

    public UserGroup retrieveGroupWithMemberById (int groupId)
            throws KustvaktException {

        ParameterChecker.checkIntegerValue(groupId, "groupId");

        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<UserGroup> query =
                criteriaBuilder.createQuery(UserGroup.class);

        Root<UserGroup> root = query.from(UserGroup.class);
        root.fetch(UserGroup_.members);
        query.select(root);
        query.where(criteriaBuilder.equal(root.get(UserGroup_.id), groupId));
        Query q = entityManager.createQuery(query);

        UserGroup userGroup;
        try {
            userGroup = (UserGroup) q.getSingleResult();
        }
        catch (NoResultException e) {
            throw new KustvaktException(StatusCodes.NO_RESULT_FOUND,
                    "No result found for query: retrieve group by id "
                            + groupId,
                    String.valueOf(groupId), e);
        }
        return userGroup;
    }

    /** Retrieves only user-groups that are active (not hidden or deleted).
     * 
     * @param userId user id
     * @return a list of UserGroup
     * @throws KustvaktException
     */
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
                criteriaBuilder.equal(members.get(UserGroupMember_.status),
                        GroupMemberStatus.ACTIVE));


        query.select(root);
        query.where(restrictions);
        Query q = entityManager.createQuery(query);
        return q.getResultList();
    }

    public UserGroup retrieveGroupByName (String groupName)
            throws KustvaktException {
        ParameterChecker.checkStringValue(groupName, "groupName");

        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<UserGroup> query =
                criteriaBuilder.createQuery(UserGroup.class);

        Root<UserGroup> root = query.from(UserGroup.class);
        query.select(root);
        query.where(
                criteriaBuilder.equal(root.get(UserGroup_.name), groupName));
        Query q = entityManager.createQuery(query);

        UserGroup userGroup;
        try {
            userGroup = (UserGroup) q.getSingleResult();
        }
        catch (NoResultException e) {
            throw new KustvaktException(StatusCodes.NO_RESULT_FOUND,
                    "No result found for query: retrieve group by name "
                            + groupName,
                    groupName, e);
        }
        return userGroup;
    }

    //    public void retrieveGroupByVCId (String vcId) {
    //        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
    //        CriteriaQuery<VirtualCorpusAccess> query =
    //                criteriaBuilder.createQuery(VirtualCorpusAccess.class);
    //
    //        Root<VirtualCorpusAccess> root = query.from(VirtualCorpusAccess.class);
    //
    //
    //    }

    public void addVCToGroup (VirtualCorpus virtualCorpus, String createdBy,
            VirtualCorpusAccessStatus status, UserGroup group) {
        VirtualCorpusAccess accessGroup = new VirtualCorpusAccess();
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

    public void deleteVCFromGroup (int virtualCorpusId, int groupId)
            throws KustvaktException {
        ParameterChecker.checkIntegerValue(virtualCorpusId, "virtualCorpusId");
        ParameterChecker.checkIntegerValue(groupId, "groupId");

        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<VirtualCorpusAccess> query =
                criteriaBuilder.createQuery(VirtualCorpusAccess.class);

        Root<VirtualCorpusAccess> root = query.from(VirtualCorpusAccess.class);
        Join<VirtualCorpusAccess, VirtualCorpus> vc =
                root.join(VirtualCorpusAccess_.virtualCorpus);
        Join<VirtualCorpusAccess, UserGroup> group =
                root.join(VirtualCorpusAccess_.userGroup);

        Predicate virtualCorpus = criteriaBuilder
                .equal(vc.get(VirtualCorpus_.id), virtualCorpusId);
        Predicate userGroup =
                criteriaBuilder.equal(group.get(UserGroup_.id), groupId);

        query.select(root);
        query.where(criteriaBuilder.and(virtualCorpus, userGroup));
        Query q = entityManager.createQuery(query);
        VirtualCorpusAccess vcAccess =
                (VirtualCorpusAccess) q.getSingleResult();
        entityManager.remove(vcAccess);
    }

}
