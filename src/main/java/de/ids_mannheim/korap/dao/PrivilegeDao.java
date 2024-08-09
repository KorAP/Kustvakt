package de.ids_mannheim.korap.dao;

import java.util.List;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import de.ids_mannheim.korap.constant.PrivilegeType;
import de.ids_mannheim.korap.entity.Privilege;
import de.ids_mannheim.korap.entity.Privilege_;
import de.ids_mannheim.korap.entity.Role;
import de.ids_mannheim.korap.entity.Role_;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

/**
 * Manages database queries and transactions regarding
 * {@link Privilege} entity or database table.
 * 
 * @see Privilege
 * @see PrivilegeType
 * @see Role
 * 
 * @author margaretha
 *
 */
@Deprecated
@Transactional
@Repository
public class PrivilegeDao {

    @PersistenceContext
    private EntityManager entityManager;

    public void addPrivilegesToRole (Role role,
            List<PrivilegeType> privilegeTypes) {
        for (PrivilegeType type : privilegeTypes) {
            Privilege privilege = new Privilege(type, role);
            entityManager.persist(privilege);
        }
    }

    public void deletePrivilegeFromRole (int roleId,
            PrivilegeType privilegeType) {
        List<Privilege> privilegeList = retrievePrivilegeByRoleId(roleId);
        for (Privilege p : privilegeList) {
            if (p.getName().equals(privilegeType)) {
                entityManager.remove(p);
                break;
            }
        }
    }

    @SuppressWarnings("unchecked")
    public List<Privilege> retrievePrivilegeByRoleId (int roleId) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Privilege> query = criteriaBuilder
                .createQuery(Privilege.class);

        Root<Privilege> root = query.from(Privilege.class);
        root.fetch(Privilege_.role);
        query.select(root);
        query.where(criteriaBuilder
                .equal(root.get(Privilege_.role).get(Role_.id), roleId));
        Query q = entityManager.createQuery(query);
        return q.getResultList();
    }
}
