package de.ids_mannheim.korap.dao;

import java.util.ArrayList;
import java.util.List;

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

import de.ids_mannheim.korap.constant.PredefinedUserGroup;
import de.ids_mannheim.korap.constant.VirtualCorpusAccessStatus;
import de.ids_mannheim.korap.entity.UserGroup;
import de.ids_mannheim.korap.entity.UserGroup_;
import de.ids_mannheim.korap.entity.VirtualCorpus;
import de.ids_mannheim.korap.entity.VirtualCorpusAccess;
import de.ids_mannheim.korap.entity.VirtualCorpusAccess_;
import de.ids_mannheim.korap.entity.VirtualCorpus_;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.utils.ParameterChecker;

@Transactional
@Repository
public class VirtualCorpusAccessDao {

    @PersistenceContext
    private EntityManager entityManager;

    public VirtualCorpusAccess retrieveAccessById (int accessId)
            throws KustvaktException {
        ParameterChecker.checkIntegerValue(accessId, "accessId");

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<VirtualCorpusAccess> query =
                builder.createQuery(VirtualCorpusAccess.class);

        Root<VirtualCorpusAccess> access =
                query.from(VirtualCorpusAccess.class);
        query.select(access);
        query.where(
                builder.equal(access.get(VirtualCorpusAccess_.id), accessId));
        Query q = entityManager.createQuery(query);
        return (VirtualCorpusAccess) q.getSingleResult();
    }

    // for vca admins
    public List<VirtualCorpusAccess> retrieveActiveAccessByVC (int vcId)
            throws KustvaktException {
        ParameterChecker.checkIntegerValue(vcId, "vcId");

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<VirtualCorpusAccess> query =
                builder.createQuery(VirtualCorpusAccess.class);

        Root<VirtualCorpusAccess> access =
                query.from(VirtualCorpusAccess.class);
        Join<VirtualCorpusAccess, VirtualCorpus> accessVC =
                access.join(VirtualCorpusAccess_.virtualCorpus);

        Predicate p = builder.and(
                builder.equal(accessVC.get(VirtualCorpus_.id), vcId),
                builder.equal(access.get(VirtualCorpusAccess_.status),
                        VirtualCorpusAccessStatus.ACTIVE));
        query.select(access);
        query.where(p);
        Query q = entityManager.createQuery(query);
        return q.getResultList();
    }

    // for system admins
    public List<VirtualCorpusAccess> retrieveAllAccessByVC (int vcId)
            throws KustvaktException {
        ParameterChecker.checkIntegerValue(vcId, "vcId");

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<VirtualCorpusAccess> query =
                builder.createQuery(VirtualCorpusAccess.class);

        Root<VirtualCorpusAccess> access =
                query.from(VirtualCorpusAccess.class);
        Join<VirtualCorpusAccess, VirtualCorpus> accessVC =
                access.join(VirtualCorpusAccess_.virtualCorpus);

        query.select(access);
        query.where(builder.equal(accessVC.get(VirtualCorpus_.id), vcId));
        Query q = entityManager.createQuery(query);
        return q.getResultList();
    }

    public List<VirtualCorpusAccess> retrieveAllAccessByGroup (int groupId)
            throws KustvaktException {
        ParameterChecker.checkIntegerValue(groupId, "groupId");

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<VirtualCorpusAccess> query =
                builder.createQuery(VirtualCorpusAccess.class);

        Root<VirtualCorpusAccess> access =
                query.from(VirtualCorpusAccess.class);
        Join<VirtualCorpusAccess, UserGroup> accessVC =
                access.join(VirtualCorpusAccess_.userGroup);

        query.select(access);
        query.where(builder.equal(accessVC.get(UserGroup_.id), groupId));
        Query q = entityManager.createQuery(query);
        return q.getResultList();
    }

    public List<VirtualCorpusAccess> retrieveActiveAccessByGroup (int groupId)
            throws KustvaktException {
        ParameterChecker.checkIntegerValue(groupId, "groupId");

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<VirtualCorpusAccess> query =
                builder.createQuery(VirtualCorpusAccess.class);

        Root<VirtualCorpusAccess> access =
                query.from(VirtualCorpusAccess.class);
        Join<VirtualCorpusAccess, UserGroup> accessVC =
                access.join(VirtualCorpusAccess_.userGroup);

        Predicate p =
                builder.and(builder.equal(accessVC.get(UserGroup_.id), groupId),
                        builder.equal(access.get(VirtualCorpusAccess_.status),
                                VirtualCorpusAccessStatus.ACTIVE));

        query.select(access);
        query.where(p);
        Query q = entityManager.createQuery(query);
        return q.getResultList();
    }

    /** Hidden accesses are only created for published or system VC. 
     * 
     * Warn: The actual hidden accesses are not checked.
     * 
     * @param vcId vcId 
     * @return true if there is a hidden access, false otherwise
     * @throws KustvaktException
     */
    public List<VirtualCorpusAccess> retrieveHiddenAccess (int vcId)
            throws KustvaktException {
        ParameterChecker.checkIntegerValue(vcId, "vcId");

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<VirtualCorpusAccess> query =
                builder.createQuery(VirtualCorpusAccess.class);

        Root<VirtualCorpusAccess> access =
                query.from(VirtualCorpusAccess.class);
        Join<VirtualCorpusAccess, VirtualCorpus> accessVC =
                access.join(VirtualCorpusAccess_.virtualCorpus);

        Predicate p = builder.and(
                builder.equal(accessVC.get(VirtualCorpus_.id), vcId),
                builder.equal(access.get(VirtualCorpusAccess_.status),
                        VirtualCorpusAccessStatus.HIDDEN),
                builder.notEqual(access.get(VirtualCorpusAccess_.deletedBy),
                        "NULL"));

        query.select(access);
        query.where(p);

        try {
            Query q = entityManager.createQuery(query);
            return q.getResultList();
        }
        catch (NoResultException e) {
            return new ArrayList<>();
        }
    }

    public VirtualCorpusAccess retrievePublishedGroupAccess (int vcId)
            throws KustvaktException {
        ParameterChecker.checkIntegerValue(vcId, "vcId");

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<VirtualCorpusAccess> query =
                builder.createQuery(VirtualCorpusAccess.class);

        Root<VirtualCorpusAccess> access =
                query.from(VirtualCorpusAccess.class);
        Join<VirtualCorpusAccess, VirtualCorpus> accessVC =
                access.join(VirtualCorpusAccess_.virtualCorpus);
        Join<VirtualCorpusAccess, UserGroup> accessGroup =
                access.join(VirtualCorpusAccess_.userGroup);

        Predicate p = builder.and(
                builder.equal(accessVC.get(VirtualCorpus_.id), vcId),
                builder.equal(accessGroup.get(UserGroup_.id),
                        PredefinedUserGroup.ALL.getId()),
                builder.equal(access.get(VirtualCorpusAccess_.status),
                        VirtualCorpusAccessStatus.HIDDEN),
                builder.notEqual(access.get(VirtualCorpusAccess_.deletedBy),
                        "NULL"));

        query.select(access);
        query.where(p);

        try {
            Query q = entityManager.createQuery(query);
            return (VirtualCorpusAccess) q.getSingleResult();
        }
        catch (NoResultException e) {
            throw new KustvaktException(StatusCodes.NO_RESULT_FOUND,
                    "Auto group access  is not found for virtual corpus with id "
                            + vcId);
        }
    }

    public void createAccessToVC (VirtualCorpus virtualCorpus,
            UserGroup userGroup, String createdBy,
            VirtualCorpusAccessStatus status) {
        VirtualCorpusAccess vca = new VirtualCorpusAccess();
        vca.setVirtualCorpus(virtualCorpus);
        vca.setUserGroup(userGroup);
        vca.setCreatedBy(createdBy);
        vca.setStatus(status);
        entityManager.persist(vca);
    }

    public void deleteAccess (VirtualCorpusAccess access) {
        entityManager.remove(access);
    }

}
