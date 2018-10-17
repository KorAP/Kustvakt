package de.ids_mannheim.korap.dao;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import de.ids_mannheim.korap.constant.AnnotationType;
import de.ids_mannheim.korap.entity.Annotation;
import de.ids_mannheim.korap.entity.AnnotationKey;
import de.ids_mannheim.korap.entity.AnnotationKey_;
import de.ids_mannheim.korap.entity.AnnotationLayer;
import de.ids_mannheim.korap.entity.AnnotationLayer_;
import de.ids_mannheim.korap.entity.Annotation_;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.utils.ParameterChecker;

/**
 * AnnotationDao manages SQL queries regarding annotations including
 * foundry and layer pairs.
 * 
 * @author margaretha
 *
 */
@Repository
@Transactional
public class AnnotationDao {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Retrieves all foundry-layer pairs.
     * 
     * @return a list of foundry-layer pairs.
     */
    @SuppressWarnings("unchecked")
    public List<AnnotationLayer> getAllFoundryLayerPairs () {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<AnnotationLayer> query =
                criteriaBuilder.createQuery(AnnotationLayer.class);
        Root<AnnotationLayer> layer = query.from(AnnotationLayer.class);
        layer.fetch(AnnotationLayer_.foundry);
        layer.fetch(AnnotationLayer_.layer);
        query.select(layer);
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
    @SuppressWarnings("unchecked")
    public List<AnnotationLayer> getAnnotationDescriptions (String foundry,
            String layer) {

        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object> query = criteriaBuilder.createQuery();
        Root<AnnotationLayer> annotationPair =
                query.from(AnnotationLayer.class);
        annotationPair.fetch(AnnotationLayer_.foundry);
        annotationPair.fetch(AnnotationLayer_.layer);
        annotationPair.fetch(AnnotationLayer_.keys);

        // EM: Hibernate bug in join n:m (see AnnotationPair.values).
        // There should not be any redundant AnnotationPair.
        // The redundancy can be also avoided with
        // fetch=FetchType.EAGER
        // because Hibernate does 2 selects.
        query.distinct(true);
        query = query.select(annotationPair);

        if (!foundry.isEmpty()) {
            Predicate foundryPredicate = criteriaBuilder.equal(annotationPair
                    .get(AnnotationLayer_.foundry).get(Annotation_.code),
                    foundry);
            if (layer.isEmpty() || layer.equals("*")) {
                query.where(foundryPredicate);
            }
            else {
                Predicate layerPredicate = criteriaBuilder.equal(annotationPair
                        .get(AnnotationLayer_.layer).get(Annotation_.code),
                        layer);
                Predicate andPredicate =
                        criteriaBuilder.and(foundryPredicate, layerPredicate);
                query.where(andPredicate);
            }
        }

        Query q = entityManager.createQuery(query);
        return q.getResultList();
    }

    public Annotation retrieveAnnotation (String code, String type) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Annotation> query =
                criteriaBuilder.createQuery(Annotation.class);

        Root<Annotation> annotation = query.from(Annotation.class);
        Predicate predicates = criteriaBuilder.and(
                criteriaBuilder.equal(annotation.get(Annotation_.code), code),
                criteriaBuilder.equal(annotation.get(Annotation_.type), type));
        query.select(annotation).where(predicates);
        Query q = entityManager.createQuery(query);
        try {
            return (Annotation) q.getSingleResult();
        }
        catch (NoResultException e) {
            return null;
        }
    }

    public AnnotationLayer retrieveAnnotationLayer (String foundry,
            String layer) {
        Annotation ann1 = retrieveAnnotation(foundry, AnnotationType.FOUNDRY);
        Annotation ann2 = retrieveAnnotation(layer, AnnotationType.LAYER);

        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<AnnotationLayer> query =
                criteriaBuilder.createQuery(AnnotationLayer.class);

        Root<AnnotationLayer> annotation = query.from(AnnotationLayer.class);
        Predicate predicates = criteriaBuilder.and(
                criteriaBuilder.equal(annotation.get(AnnotationLayer_.foundry),
                        ann1),
                criteriaBuilder.equal(annotation.get(AnnotationLayer_.layer),
                        ann2));
        query.select(annotation).where(predicates);
        Query q = entityManager.createQuery(query);
        try {
            return (AnnotationLayer) q.getSingleResult();
        }
        catch (NoResultException e) {
            return null;
        }

    }

    public Annotation createAnnotation (String code, String type, String text,
            String description) {
        Annotation ann = new Annotation(code, type, text, description);
        entityManager.persist(ann);
        return ann;
    }

    public AnnotationLayer createAnnotationLayer (Annotation foundry,
            Annotation layer) throws KustvaktException {
        ParameterChecker.checkObjectValue(foundry, "foundry");
        ParameterChecker.checkObjectValue(layer, "layer");

        AnnotationLayer annotationLayer = new AnnotationLayer();
        annotationLayer.setFoundryId(foundry.getId());
        annotationLayer.setLayerId(layer.getId());
        annotationLayer.setDescription(
                foundry.getDescription() + " " + layer.getDescription());
        entityManager.persist(annotationLayer);
        return annotationLayer;
    }

    public void updateAnnotationLayer (AnnotationLayer layer) {
        entityManager.merge(layer);
    }

    public void updateAnnotationKey (AnnotationKey key) {
        entityManager.merge(key);
    }

    public AnnotationKey createAnnotationKey (AnnotationLayer layer,
            Annotation key) {
        AnnotationKey annotation =
                new AnnotationKey(layer.getId(), key.getId());
        entityManager.persist(annotation);
        return annotation;
    }

    public AnnotationKey retrieveAnnotationKey (AnnotationLayer layer,
            Annotation key) {

        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<AnnotationKey> query =
                criteriaBuilder.createQuery(AnnotationKey.class);

        Root<AnnotationKey> annotation = query.from(AnnotationKey.class);
        Predicate predicates = criteriaBuilder.and(
                criteriaBuilder.equal(annotation.get(AnnotationKey_.layer),
                        layer),
                criteriaBuilder.equal(annotation.get(AnnotationKey_.key), key));
        query.select(annotation).where(predicates);
        Query q = entityManager.createQuery(query);
        try {
            return (AnnotationKey) q.getSingleResult();
        }
        catch (NoResultException e) {
            return null;
        }
    }
}
