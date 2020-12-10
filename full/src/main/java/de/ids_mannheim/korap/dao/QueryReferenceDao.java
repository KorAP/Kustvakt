package de.ids_mannheim.korap.dao;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;

import de.ids_mannheim.korap.constant.VirtualCorpusType;
import de.ids_mannheim.korap.utils.ParameterChecker;

import de.ids_mannheim.korap.entity.QueryReference;
import de.ids_mannheim.korap.entity.QueryReference_;

/**
 * QueryReferenceDao manages database queries and transactions
 * regarding query fragments, e.g. retrieving and storing queries
 * for embedding in more complex queries.
 * 
 * Based on VirtualCorpusDao.
 *
 * @author diewald
 *
 */

@Transactional
@Repository
public class QueryReferenceDao {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Create query reference and return ID.
     * Does not support any access management yet
     */
    public int createQuery(
        String qName,
        VirtualCorpusType type,
        // CorpusAccess requiredAccess,
        String koralQuery,
        String definition,
        String description,
        String status,
        String createdBy
        ) throws KustvaktException {
        
        QueryReference q = new QueryReference();
        q.setName(qName);
        q.setType(type);
        q.setKoralQuery(koralQuery);
        q.setDefinition(definition);
        q.setDescription(description);
        q.setStatus(status);
        q.setCreatedBy(createdBy);

        // Maybe unused
        q.setRequiredAccess("");

        entityManager.persist(q);
        return q.getId();
    };


    /**
     * Retrieve a single query reference based on its name.
     */
    public QueryReference retrieveQueryByName (String qName, String createdBy)
        throws KustvaktException {
        ParameterChecker.checkStringValue(createdBy, "createdBy");
        ParameterChecker.checkStringValue(qName, "q");

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<QueryReference> query =
            builder.createQuery(QueryReference.class);

        Root<QueryReference> qref = query.from(QueryReference.class);

        Predicate condition = builder.and(
            builder.equal(qref.get(QueryReference_.createdBy),
                          createdBy),
            builder.equal(qref.get(QueryReference_.name), qName));

        query.select(qref);
        query.where(condition);

        Query q = entityManager.createQuery(query);
        QueryReference qr = null;
        try {
            qr = (QueryReference) q.getSingleResult();
        }
        catch (NoResultException e) {
            return null;
        }
        catch (NonUniqueResultException e) {
            String refCode = createdBy + "/" + qName;
            throw new KustvaktException(StatusCodes.NON_UNIQUE_RESULT_FOUND,
                    "Non unique result found for query: retrieve virtual corpus by name "
                            + refCode,
                    String.valueOf(refCode), e);
        }
        return qr;
    };

    /**
     * Remove a query reference from the database.
     */
    public void deleteQueryReference (QueryReference q)
            throws KustvaktException {
        if (!entityManager.contains(q)) {
            q = entityManager.merge(q);
        }
        entityManager.remove(q);
    }
};
