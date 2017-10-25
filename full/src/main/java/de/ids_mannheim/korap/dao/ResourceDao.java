package de.ids_mannheim.korap.dao;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.springframework.stereotype.Repository;

import de.ids_mannheim.korap.entity.Resource;
import de.ids_mannheim.korap.entity.Resource_;

/**
 * ResourceDao manages SQL queries regarding resource info and layers.
 * 
 * @author margaretha
 *
 */
@Repository
public class ResourceDao {

    @PersistenceContext
    private EntityManager entityManager;

    /** Select all from the resource table  
     *  
     * @return a list of resources
     */
    public List<Resource> getAllResources () {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Resource> query =
                criteriaBuilder.createQuery(Resource.class);
        Root<Resource> resource = query.from(Resource.class);
        resource.fetch(Resource_.layers);
        query.select(resource);
        Query q = entityManager.createQuery(query);
        return q.getResultList();
    }
}
