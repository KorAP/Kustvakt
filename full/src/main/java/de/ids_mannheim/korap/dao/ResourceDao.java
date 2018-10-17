package de.ids_mannheim.korap.dao;

import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import de.ids_mannheim.korap.entity.AnnotationLayer;
import de.ids_mannheim.korap.entity.Resource;
import de.ids_mannheim.korap.entity.Resource_;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.utils.ParameterChecker;

/**
 * ResourceDao manages SQL queries regarding resource info and layers.
 * 
 * @author margaretha
 *
 */
@Transactional
@Repository
public class ResourceDao {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Select all from the resource table
     * 
     * @return a list of resources
     */
    @SuppressWarnings("unchecked")
    public List<Resource> getAllResources () {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Resource> query =
                criteriaBuilder.createQuery(Resource.class);
        Root<Resource> resource = query.from(Resource.class);
        query.select(resource);
        
        Query q = entityManager.createQuery(query);
        return q.getResultList();
    }

    public Resource retrieveResource (String id) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Resource> query =
                criteriaBuilder.createQuery(Resource.class);
        Root<Resource> resource = query.from(Resource.class);
        query.select(resource);
        query.where(criteriaBuilder.equal(resource.get(Resource_.id), id));

        Query q = entityManager.createQuery(query);
        return (Resource) q.getSingleResult();
    }

    public void createResource (String id, String germanTitle,
            String englishTitle, String englishDescription, Set<AnnotationLayer> layers)
            throws KustvaktException {
        ParameterChecker.checkStringValue(id, "id");
        ParameterChecker.checkStringValue(englishTitle, "en_title");
        ParameterChecker.checkStringValue(germanTitle, "de_title");

        Resource r = new Resource(id, germanTitle, englishTitle,
                englishDescription, layers);
        entityManager.persist(r);

    }
}
