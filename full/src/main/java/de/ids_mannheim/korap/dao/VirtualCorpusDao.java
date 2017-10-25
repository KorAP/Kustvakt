package de.ids_mannheim.korap.dao;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import de.ids_mannheim.korap.constant.GroupMemberStatus;
import de.ids_mannheim.korap.constant.UserGroupStatus;
import de.ids_mannheim.korap.constant.VirtualCorpusAccessStatus;
import de.ids_mannheim.korap.constant.VirtualCorpusType;
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
@Transactional
@Repository
public class VirtualCorpusDao {

    @PersistenceContext
    private EntityManager entityManager;

    public void storeVirtualCorpus (VirtualCorpus virtualCorpus) {
        entityManager.persist(virtualCorpus);
    }

    public void deleteVirtualCorpus (int id) throws KustvaktException {
        VirtualCorpus vc = retrieveVCById(id);
        entityManager.remove(vc);
    }

    public List<VirtualCorpus> retrieveVCByType (VirtualCorpusType type)
            throws KustvaktException {
        if (type == null) {
            throw new KustvaktException(StatusCodes.MISSING_ARGUMENT, "type",
                    "null");
        }
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<VirtualCorpus> query =
                criteriaBuilder.createQuery(VirtualCorpus.class);
        Root<VirtualCorpus> virtualCorpus = query.from(VirtualCorpus.class);
        query.select(virtualCorpus);
        query.where(criteriaBuilder.equal(virtualCorpus.get("type"), type));
        Query q = entityManager.createQuery(query);
        return q.getResultList();
    }


    public VirtualCorpus retrieveVCById (int id) throws KustvaktException {
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

        VirtualCorpus vc = null;
        try {
            Query q = entityManager.createQuery(query);
            vc = (VirtualCorpus) q.getSingleResult();
        }
        catch (NoResultException e) {
            throw new KustvaktException(StatusCodes.NO_RESULT_FOUND,
                    "No result found for query: retrieve virtual corpus by id "
                            + id,
                    String.valueOf(id), e);
        }
        return vc;
    }


    public List<VirtualCorpus> retrievePrivateVC (String userId)
            throws KustvaktException {
        if (userId == null || userId.isEmpty()) {
            throw new KustvaktException(StatusCodes.MISSING_ARGUMENT, "userId",
                    userId);
        }
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<VirtualCorpus> query =
                builder.createQuery(VirtualCorpus.class);

        Root<VirtualCorpus> virtualCorpus = query.from(VirtualCorpus.class);
        query.select(virtualCorpus);
        query.where(builder.equal(virtualCorpus.get("createdBy"), userId));

        Query q = entityManager.createQuery(query);
        return q.getResultList();
    }


    public List<VirtualCorpus> retrieveGroupVCByUser (String userId)
            throws KustvaktException {
        if (userId == null || userId.isEmpty()) {
            throw new KustvaktException(StatusCodes.MISSING_ARGUMENT, "userId",
                    userId);
        }

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<VirtualCorpus> query =
                builder.createQuery(VirtualCorpus.class);

        Root<VirtualCorpus> virtualCorpus = query.from(VirtualCorpus.class);
        Join<VirtualCorpus, VirtualCorpusAccessGroup> accessGroup =
                virtualCorpus.join("accessGroup");

        Predicate corpusStatus = builder.and(
                builder.notEqual(accessGroup.get("status"),
                        VirtualCorpusAccessStatus.HIDDEN),
                builder.notEqual(accessGroup.get("status"),
                        VirtualCorpusAccessStatus.DELETED));
        
        Predicate userGroupStatus =
                builder.notEqual(accessGroup.get("userGroup").get("status"),
                        UserGroupStatus.DELETED);
        Join<VirtualCorpusAccessGroup, UserGroup> userGroupMembers =
                accessGroup.join("userGroup").join("members");
        
        Predicate memberStatus = builder.equal(userGroupMembers.get("status"),
                GroupMemberStatus.ACTIVE);
        
        Predicate user = builder.equal(userGroupMembers.get("userId"), userId);

        query.select(virtualCorpus);
        query.where(
                builder.and(corpusStatus, userGroupStatus, memberStatus, user));

        Query q = entityManager.createQuery(query);
        return q.getResultList();
    }


    public Set<VirtualCorpus> retrieveVCByUser (String userId)
            throws KustvaktException {
        if (userId == null || userId.isEmpty()) {
            throw new KustvaktException(StatusCodes.MISSING_ARGUMENT, "userId",
                    userId);
        }
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<VirtualCorpus> query =
                builder.createQuery(VirtualCorpus.class);

        Root<VirtualCorpus> virtualCorpus = query.from(VirtualCorpus.class);
        Predicate predicate = builder.or(
                builder.equal(virtualCorpus.get("createdBy"), userId),
                builder.equal(virtualCorpus.get("type"),
                        VirtualCorpusType.PREDEFINED));


        query.select(virtualCorpus);
        query.where(predicate);
        query.distinct(true);
        Query q = entityManager.createQuery(query);

        List<VirtualCorpus> vcList = q.getResultList();
        List<VirtualCorpus> groupVC = retrieveGroupVCByUser(userId);
        Set<VirtualCorpus> vcSet = new HashSet<VirtualCorpus>();
        vcSet.addAll(vcList);
        vcSet.addAll(groupVC);
        return vcSet;
    }


    // EM: what is needed for admin?
    public List<VirtualCorpus> retrieveVirtualCorpusByAdmin () {
        return null;

    }
}
