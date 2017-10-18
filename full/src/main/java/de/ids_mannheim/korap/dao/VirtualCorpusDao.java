package de.ids_mannheim.korap.dao;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.stereotype.Component;

import de.ids_mannheim.korap.constants.GroupMemberStatus;
import de.ids_mannheim.korap.constants.UserGroupStatus;
import de.ids_mannheim.korap.constants.VirtualCorpusAccessStatus;
import de.ids_mannheim.korap.entity.UserGroup;
import de.ids_mannheim.korap.entity.VirtualCorpus;
import de.ids_mannheim.korap.entity.VirtualCorpusAccessGroup;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;

/** VirtualCorpusDao manages SQL queries regarding virtual corpora, 
 *  e.g. retrieving and storing virtual corpora.
 *  
 * @author margaretha
 *
 */
@Component
public class VirtualCorpusDao {

    @PersistenceContext
    private EntityManager entityManager;


    public void storeVirtualCorpus (VirtualCorpus virtualCorpus) {
        entityManager.getTransaction().begin();
        entityManager.persist(virtualCorpus);
        entityManager.getTransaction().commit();
    }


    public VirtualCorpus retrieveVirtualCorpusById (int id)
            throws KustvaktException {
        if (id == 0) {
            throw new KustvaktException(StatusCodes.MISSING_ARGUMENT, "id",
                    String.valueOf(id));
        }
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<VirtualCorpus> query =
                criteriaBuilder.createQuery(VirtualCorpus.class);
        Root<VirtualCorpus> virtualCorpus = query.from(VirtualCorpus.class);
        query.select(virtualCorpus);
        query.where(criteriaBuilder.equal(virtualCorpus.get("id"), id));
        Query q = entityManager.createQuery(query);
        return (VirtualCorpus) q.getSingleResult();
    }


    public List<VirtualCorpus> retrieveVirtualCorpusByUser (String userId) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<VirtualCorpus> query =
                criteriaBuilder.createQuery(VirtualCorpus.class);

        Root<VirtualCorpus> virtualCorpus = query.from(VirtualCorpus.class);
        virtualCorpus.fetch("accessGroup");

        Join<VirtualCorpus, VirtualCorpusAccessGroup> accessGroup =
                virtualCorpus.join("accessGroup");

        Predicate corpusStatus = criteriaBuilder.notEqual(
                accessGroup.get("status"), VirtualCorpusAccessStatus.HIDDEN);
        Predicate userGroupStatus = criteriaBuilder.notEqual(
                accessGroup.get("userGroup").get("status"),
                UserGroupStatus.DELETED);

        Join<VirtualCorpusAccessGroup, UserGroup> userGroupMembers =
                accessGroup.join("userGroup").join("members");

        Predicate memberStatus = criteriaBuilder.equal(
                userGroupMembers.get("status"), GroupMemberStatus.ACTIVE);

        Predicate user =
                criteriaBuilder.equal(userGroupMembers.get("userId"), userId);

        query.select(virtualCorpus);
        query.where(criteriaBuilder.and(corpusStatus, userGroupStatus,
                memberStatus, user));
        Query q = entityManager.createQuery(query);
        return q.getResultList();
    }


    // EM: what is needed for admin?
    public List<VirtualCorpus> retrieveVirtualCorpusByAdmin () {
        return null;

    }
}
