package de.ids_mannheim.korap.dao;

import java.util.List;
import java.util.Set;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import de.ids_mannheim.korap.entity.Resource_;
import de.ids_mannheim.korap.entity.AnnotationLayer;
import de.ids_mannheim.korap.entity.Resource;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.utils.ParameterChecker;

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

    /**
     * Select all from the resource table
     * 
     * @return a list of resources
     */
    public List<Resource> getAllResources () {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Resource> query = criteriaBuilder
                .createQuery(Resource.class);
        Root<Resource> resource = query.from(Resource.class);
        query.select(resource);

        TypedQuery<Resource> q = entityManager.createQuery(query);
        return q.getResultList();
    }

    public Resource retrieveResource (String id) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Resource> query = criteriaBuilder
                .createQuery(Resource.class);
        Root<Resource> resource = query.from(Resource.class);
        query.select(resource);
        query.where(criteriaBuilder.equal(resource.get(Resource_.id), id));

        Query q = entityManager.createQuery(query);
        try {
            return (Resource) q.getSingleResult();
        }
        catch (NoResultException e) {
            return null;
        }
    }

    @Transactional
    public void createResource (String id, String pid, String germanTitle,
            String englishTitle, String englishDescription,
            Set<AnnotationLayer> layers, String institution, 
            String corpusQuery, String requiredAccess) 
            		throws KustvaktException {
        ParameterChecker.checkStringValue(id, "id");
        ParameterChecker.checkStringValue(englishTitle, "en_title");
        ParameterChecker.checkStringValue(germanTitle, "de_title");

		Resource r = new Resource(id, pid, germanTitle, englishTitle,
				englishDescription, layers, institution, corpusQuery,
				requiredAccess);
		entityManager.persist(r);
    }
    
    @Transactional
    public void updateResource (Resource r, String pid, String germanTitle,
			String englishTitle, String englishDescription,
			Set<AnnotationLayer> layers, String institution, String corpusQuery,
			String requiredAccess) throws KustvaktException {
		ParameterChecker.checkObjectValue(layers, "layers");
        ParameterChecker.checkStringValue(englishTitle, "en_title");
        ParameterChecker.checkStringValue(germanTitle, "de_title");

        r.setCorpusQuery(corpusQuery);
        r.setEnglishDescription(englishDescription);
        r.setEnglishTitle(englishTitle);
        r.setGermanTitle(germanTitle);
        r.setInstitution(institution);
        r.setLayers(layers);
        r.setPid(pid);
        r.setRequiredAccess(requiredAccess);
        
        entityManager.merge(r);
    }
}
