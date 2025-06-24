package de.ids_mannheim.korap.dao;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import de.ids_mannheim.korap.entity.Admin;
import de.ids_mannheim.korap.entity.Admin_;
import de.ids_mannheim.korap.user.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

/**
 * Describes database queries and transactions regarding admin users.
 * 
 * @author margaretha
 *
 */
@Transactional
@Repository
public class AdminDaoImpl implements AdminDao {

    @PersistenceContext
    private EntityManager entityManager;

    /* (non-Javadoc)
     * @see de.ids_mannheim.korap.dao.AdminDao#addAccount(de.ids_mannheim.korap.user.User)
     */
    @Override
    public void addAccount (User user) {
		if (!isAdmin(user.getUsername())) {
			Admin admin = new Admin();
			admin.setUserId(user.getUsername());
			entityManager.persist(admin);
		}
    }

    /* (non-Javadoc)
     * @see de.ids_mannheim.korap.dao.AdminDao#isAdmin(java.lang.String)
     */
    @Override
    public boolean isAdmin (String userId) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Admin> query = criteriaBuilder.createQuery(Admin.class);

        Root<Admin> admin = query.from(Admin.class);
        Predicate p = criteriaBuilder.equal(admin.get(Admin_.userId), userId);

        query.select(admin);
        query.where(p);

        Query q = entityManager.createQuery(query);
        try {
            q.getSingleResult();
        }
        catch (NoResultException e) {
            return false;
        }

        return true;
    }
}
