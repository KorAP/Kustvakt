package de.ids_mannheim.korap.dao;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import de.ids_mannheim.korap.constant.PrivilegeType;
import de.ids_mannheim.korap.entity.Privilege;
import de.ids_mannheim.korap.entity.Role;
import de.ids_mannheim.korap.entity.Role_;

@Transactional
@Repository
public class RoleDao {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    PrivilegeDao privilegeDao;

    public void createRole (String name, List<PrivilegeType> privilegeTypes) {
        Role r = new Role();
        r.setName(name);
        entityManager.persist(r);
        privilegeDao.addPrivilegesToRole(r, privilegeTypes);
    }

    public void deleteRole (int roleId) {
        Role r = retrieveRoleById(roleId);
        entityManager.remove(r);
    }

    public void updateRoleName (int roleId, String name) {
        Role r = retrieveRoleById(roleId);
        r.setName(name);
        entityManager.persist(r);
    }
    
    public Role retrieveRoleById (int roleId) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Role> query = criteriaBuilder.createQuery(Role.class);

        Root<Role> root = query.from(Role.class);
        root.fetch(Role_.privileges);
        query.select(root);
        query.where(criteriaBuilder.equal(root.get(Role_.id), roleId));
        Query q = entityManager.createQuery(query);
        return (Role) q.getSingleResult();
    }

    public Role retrieveRoleByName (String roleName) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Role> query = criteriaBuilder.createQuery(Role.class);

        Root<Role> root = query.from(Role.class);
        root.fetch(Role_.privileges);
        query.select(root);
        query.where(criteriaBuilder.equal(root.get(Role_.name), roleName));
        Query q = entityManager.createQuery(query);
        return (Role) q.getSingleResult();
    }

}
