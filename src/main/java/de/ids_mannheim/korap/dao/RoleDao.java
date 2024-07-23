package de.ids_mannheim.korap.dao;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import de.ids_mannheim.korap.constant.PredefinedRole;
import de.ids_mannheim.korap.entity.QueryAccess;
import de.ids_mannheim.korap.entity.QueryDO_;
import de.ids_mannheim.korap.entity.Role;
import de.ids_mannheim.korap.entity.Role_;
import de.ids_mannheim.korap.entity.UserGroupMember;
import de.ids_mannheim.korap.entity.UserGroupMember_;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaDelete;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.ListJoin;
import jakarta.persistence.criteria.Root;

/**
 * Manages database queries and transactions regarding {@link Role}
 * entity or database table.
 * 
 * @author margaretha
 * @see Role
 */
@Transactional
@Repository
public class RoleDao {

    @PersistenceContext
    private EntityManager entityManager;

    //    public void deleteRole (Role role) {
    //        entityManager.remove(role);
    //        entityManager.flush();
    //    }
    //
    //    public void editRoleName (int roleId, PredefinedRole name) {
    //        Role r = retrieveRoleById(roleId);
    //        r.setName(name);
    //        entityManager.persist(r);
    //    }

    public void addRole (Role newRole) {
        entityManager.persist(newRole);
        entityManager.flush();
    }

    public Role retrieveRoleById (int roleId) throws KustvaktException {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Role> query = criteriaBuilder.createQuery(Role.class);

        Root<Role> root = query.from(Role.class);
        root.fetch(Role_.userGroup);
        query.select(root);
        query.where(criteriaBuilder.equal(root.get(Role_.id), roleId));
        Query q = entityManager.createQuery(query);
        
        try {
            return (Role) q.getSingleResult();
        }
        catch (NoResultException e) {
            throw new KustvaktException(StatusCodes.NO_RESOURCE_FOUND,
                    "Role is not found", String.valueOf(roleId));
        }
    }

    public Role retrieveRoleByName (PredefinedRole role) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Role> query = criteriaBuilder.createQuery(Role.class);

        Root<Role> root = query.from(Role.class);
        //        root.fetch(Role_.privileges);
        query.select(root);
        query.where(criteriaBuilder.equal(root.get(Role_.name), role));
        Query q = entityManager.createQuery(query);
        return (Role) q.getSingleResult();
    }

    public Set<Role> retrieveRoleByGroupMemberId (int userId) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Role> query = criteriaBuilder.createQuery(Role.class);

        Root<Role> root = query.from(Role.class);
        ListJoin<Role, UserGroupMember> memberRole = root
                .join(Role_.userGroupMembers);

        query.select(root);
        query.where(criteriaBuilder.equal(memberRole.get(UserGroupMember_.id),
                userId));
        TypedQuery<Role> q = entityManager.createQuery(query);
        List<Role> resultList = q.getResultList();
        return new HashSet<Role>(resultList);
    }
    
    public Set<Role> retrieveRoleByGroupId (int groupId, boolean hasQuery) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Role> query = cb.createQuery(Role.class);

        Root<Role> role = query.from(Role.class);
        role.fetch("userGroup", JoinType.INNER);
        role.fetch("query", JoinType.INNER);
        
        query.select(role);
        if (hasQuery) {
            query.where(cb.equal(role.get("userGroup").get("id"), groupId),
                    cb.isNotNull(role.get("query").get("id")));
        }
        else {
            query.where(cb.equal(role.get("userGroup").get("id"), groupId));
        }

        TypedQuery<Role> q = entityManager.createQuery(query);
        List<Role> resultList = q.getResultList();
        return new HashSet<Role>(resultList);
    }

    public Set<Role> retrieveRoleByQueryIdAndUsername (int queryId,
            String username) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Role> query = cb.createQuery(Role.class);

        Root<Role> role = query.from(Role.class);
        role.fetch(Role_.query, JoinType.INNER);

        Join<Role, UserGroupMember> members = role.join("userGroupMembers",
                JoinType.INNER);
        
        query.select(role);
        query.where(cb.equal(role.get(Role_.query).get(QueryDO_.id), queryId),
                cb.equal(members.get(UserGroupMember_.userId), username));

        TypedQuery<Role> q = entityManager.createQuery(query);
        List<Role> resultList = q.getResultList();
        return new HashSet<Role>(resultList);
    }

    public void deleteRole (int roleId) throws KustvaktException {

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaDelete<Role> delete = cb.createCriteriaDelete(Role.class);
        Root<Role> role = delete.from(Role.class);

        delete.where(
                cb.equal(role.get("id"), roleId));
        
        try {
            entityManager.createQuery(delete).executeUpdate();
        }
        catch (NoResultException e) {
            throw new KustvaktException(StatusCodes.NO_RESOURCE_FOUND,
                    "Role is not found", String.valueOf(roleId));
        }
        
    }

}
