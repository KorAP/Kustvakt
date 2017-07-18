package de.ids_mannheim.korap.dao;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.antlr.v4.parse.ANTLRParser.id_return;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import de.ids_mannheim.korap.entity.Annotation;
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

    private static Logger jlog = LoggerFactory.getLogger(AnnotationDao.class);
    private NamedParameterJdbcTemplate jdbcTemplate;

    @PersistenceContext
    private EntityManager entityManager;


    /**
     * Select all foundry and layer pairs.
     * 
     * @return a list of all foundry and layer pairs.
     */
    public List<AnnotationPair> getAllFoundryLayerPairs () {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<AnnotationPair> query = criteriaBuilder
                .createQuery(AnnotationPair.class);
        Root<AnnotationPair> annotationPair = query.from(AnnotationPair.class);
        annotationPair.fetch("annotation1");
        annotationPair.fetch("annotation2");
        query.select(annotationPair);
        Query q = entityManager.createQuery(query);
        return q.getResultList();
    }
    
    public List<AnnotationPair> getAllAnnotationDescriptions () {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<AnnotationPair> query = criteriaBuilder
                .createQuery(AnnotationPair.class);
        Root<AnnotationPair> annotationPair = query.from(AnnotationPair.class);
        annotationPair.fetch("annotation1");
        annotationPair.fetch("annotation2");
        annotationPair.fetch("values");
        query.select(annotationPair);
        Query q = entityManager.createQuery(query);
        return q.getResultList();
    }


    /**
     * Select foundry and layer pairs' information of the given foundries.
     * 
     * @return a list of foundry and layer pairs.
     */
    public List<AnnotationPair> getFoundryLayerPairs (List<String> foundries) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();

        CriteriaQuery<Object> query = criteriaBuilder.createQuery();
        Root<Annotation> annotation = query.from(Annotation.class);
        Root<AnnotationPair> annotationPair = query.from(AnnotationPair.class);
        Predicate foundryPredicate = criteriaBuilder.equal(annotation.get("symbol"),
                foundries);
        Predicate valuePredicate =  criteriaBuilder.equal(annotationPair.get("value").get("id"),
                annotation.get("id"));
        Predicate wherePredicate = criteriaBuilder.and(foundryPredicate,valuePredicate);
        query.multiselect(annotation, annotationPair).where(wherePredicate);
        Query q = entityManager.createQuery(query);
        return q.getResultList();
    }
}
