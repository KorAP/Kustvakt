package de.ids_mannheim.korap.dao;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.stereotype.Component;

import de.ids_mannheim.korap.entity.AnnotationPair;


/**
 * AnnotationDao manages SQL queries regarding annotations including
 * foundry and layer pairs.
 * 
 * @author margaretha
 *
 */
@Component
public class AnnotationDao {

    @PersistenceContext
    private EntityManager entityManager;


    /**
     * Retrieves all foundry-layer pairs.
     * 
     * @return a list of foundry-layer pairs.
     */
    public List<AnnotationPair> getAllFoundryLayerPairs () {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<AnnotationPair> query =
                criteriaBuilder.createQuery(AnnotationPair.class);
        Root<AnnotationPair> annotationPair = query.from(AnnotationPair.class);
        annotationPair.fetch("annotation1");
        annotationPair.fetch("annotation2");
        query.select(annotationPair);
        Query q = entityManager.createQuery(query);
        return q.getResultList();
    }


    /**
     * Retrieves foundry-layer pairs and their values for the given
     * foundry and layer. If layer is empty, retrieves data for all
     * layer in the given foundry. If foundry is empty, retrieves data
     * for all foundry and layer pairs.
     * 
     * @param foundry
     *            a foundry code
     * @param layer
     *            a layer code
     * @return a list of foundry-layer pairs.
     */
    public List<AnnotationPair> getAnnotationDescriptions (String foundry,
            String layer) {

        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object> query = criteriaBuilder.createQuery();
        Root<AnnotationPair> annotationPair = query.from(AnnotationPair.class);
        annotationPair.fetch("annotation1");
        annotationPair.fetch("annotation2");
        annotationPair.fetch("values");

        // EM: Hibernate bug in join n:m (see AnnotationPair.values). 
        // There should not be any redundant AnnotationPair. 
        // The redundancy can be also avoided with fetch=FetchType.EAGER 
        // because Hibernate does 2 selects.  
        query.distinct(true);
        query = query.select(annotationPair);

        if (!foundry.isEmpty()) {
            Predicate foundryPredicate = criteriaBuilder.equal(
                    annotationPair.get("annotation1").get("code"), foundry);
            if (layer.isEmpty() || layer.equals("*")) {
                query.where(foundryPredicate);
            }
            else {
                Predicate layerPredicate = criteriaBuilder.equal(
                        annotationPair.get("annotation2").get("code"), layer);
                Predicate andPredicate =
                        criteriaBuilder.and(foundryPredicate, layerPredicate);
                query.where(andPredicate);
            }
        }

        Query q = entityManager.createQuery(query);
        return q.getResultList();
    }
}
