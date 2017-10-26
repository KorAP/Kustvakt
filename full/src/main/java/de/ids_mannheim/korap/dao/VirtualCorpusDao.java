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
import javax.persistence.criteria.ListJoin;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import de.ids_mannheim.korap.constant.GroupMemberStatus;
import de.ids_mannheim.korap.constant.UserGroupStatus;
import de.ids_mannheim.korap.constant.VirtualCorpusAccessStatus;
import de.ids_mannheim.korap.constant.VirtualCorpusType;
import de.ids_mannheim.korap.entity.UserGroup;
import de.ids_mannheim.korap.entity.UserGroupMember;
import de.ids_mannheim.korap.entity.UserGroupMember_;
import de.ids_mannheim.korap.entity.UserGroup_;
import de.ids_mannheim.korap.entity.VirtualCorpus;
import de.ids_mannheim.korap.entity.VirtualCorpusAccess;
import de.ids_mannheim.korap.entity.VirtualCorpusAccess_;
import de.ids_mannheim.korap.entity.VirtualCorpus_;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.user.User.CorpusAccess;
import de.ids_mannheim.korap.utils.ParameterChecker;

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

    public int createVirtualCorpus (String name, VirtualCorpusType type,
            CorpusAccess requiredAccess, String collectionQuery, String definition,
            String description, String status, String createdBy) {

        VirtualCorpus vc = new VirtualCorpus();
        vc.setName(name);
        vc.setType(type);
        vc.setRequiredAccess(requiredAccess);
        vc.setCollectionQuery(collectionQuery);
        vc.setDefinition(definition);
        vc.setDescription(description);
        vc.setStatus(status);
        vc.setCreatedBy(createdBy);
        
        entityManager.persist(vc);
        return vc.getId();
    }

    public void deleteVirtualCorpus (int id) throws KustvaktException {
        VirtualCorpus vc = retrieveVCById(id);
        entityManager.remove(vc);
    }

    public List<VirtualCorpus> retrieveVCByType (VirtualCorpusType type)
            throws KustvaktException {
        ParameterChecker.checkObjectValue(type, "type");

        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<VirtualCorpus> query =
                criteriaBuilder.createQuery(VirtualCorpus.class);
        Root<VirtualCorpus> virtualCorpus = query.from(VirtualCorpus.class);
        query.select(virtualCorpus);
        query.where(criteriaBuilder
                .equal(virtualCorpus.get(VirtualCorpus_.type), type));
        Query q = entityManager.createQuery(query);
        return q.getResultList();
    }


    public VirtualCorpus retrieveVCById (int id) throws KustvaktException {
        ParameterChecker.checkIntegerValue(id, "id");

        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<VirtualCorpus> query =
                criteriaBuilder.createQuery(VirtualCorpus.class);
        Root<VirtualCorpus> virtualCorpus = query.from(VirtualCorpus.class);
        query.select(virtualCorpus);
        query.where(criteriaBuilder.equal(virtualCorpus.get(VirtualCorpus_.id),
                id));

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
        ParameterChecker.checkStringValue(userId, "userId");

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<VirtualCorpus> query =
                builder.createQuery(VirtualCorpus.class);

        Root<VirtualCorpus> virtualCorpus = query.from(VirtualCorpus.class);
        query.select(virtualCorpus);
        query.where(builder.equal(virtualCorpus.get(VirtualCorpus_.createdBy),
                userId));

        Query q = entityManager.createQuery(query);
        return q.getResultList();
    }


    public List<VirtualCorpus> retrieveGroupVCByUser (String userId)
            throws KustvaktException {
        ParameterChecker.checkStringValue(userId, "userId");

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<VirtualCorpus> query =
                builder.createQuery(VirtualCorpus.class);

        Root<VirtualCorpus> virtualCorpus = query.from(VirtualCorpus.class);
        Join<VirtualCorpus, VirtualCorpusAccess> access =
                virtualCorpus.join(VirtualCorpus_.virtualCorpusAccess);

        Predicate corpusStatus = builder.and(
                builder.notEqual(access.get(VirtualCorpusAccess_.status),
                        VirtualCorpusAccessStatus.HIDDEN),
                builder.notEqual(access.get(VirtualCorpusAccess_.status),
                        VirtualCorpusAccessStatus.DELETED));

        Predicate userGroupStatus =
                builder.notEqual(access.get(VirtualCorpusAccess_.userGroup)
                        .get(UserGroup_.status), UserGroupStatus.DELETED);
        Join<UserGroup, UserGroupMember> members = access
                .join(VirtualCorpusAccess_.userGroup).join(UserGroup_.members);

        Predicate memberStatus = builder.equal(
                members.get(UserGroupMember_.status), GroupMemberStatus.ACTIVE);

        Predicate user =
                builder.equal(members.get(UserGroupMember_.userId), userId);

        query.select(virtualCorpus);
        query.where(
                builder.and(corpusStatus, userGroupStatus, memberStatus, user));

        Query q = entityManager.createQuery(query);
        return q.getResultList();
    }


    public Set<VirtualCorpus> retrieveVCByUser (String userId)
            throws KustvaktException {
        ParameterChecker.checkStringValue(userId, "userId");

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<VirtualCorpus> query =
                builder.createQuery(VirtualCorpus.class);

        Root<VirtualCorpus> virtualCorpus = query.from(VirtualCorpus.class);
        Predicate predicate = builder.or(
                builder.equal(virtualCorpus.get(VirtualCorpus_.createdBy),
                        userId),
                builder.equal(virtualCorpus.get(VirtualCorpus_.type),
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

    public List<VirtualCorpus> retrieveVCByGroup (int groupId)
            throws KustvaktException {
        ParameterChecker.checkIntegerValue(groupId, "groupId");

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<VirtualCorpus> query =
                builder.createQuery(VirtualCorpus.class);

        Root<VirtualCorpus> virtualCorpus = query.from(VirtualCorpus.class);
        ListJoin<VirtualCorpus, VirtualCorpusAccess> corpusAccess =
                virtualCorpus.join(VirtualCorpus_.virtualCorpusAccess);
        Join<VirtualCorpusAccess, UserGroup> accessGroup =
                corpusAccess.join(VirtualCorpusAccess_.userGroup);

        query.select(virtualCorpus);
        query.where(builder.equal(accessGroup.get(UserGroup_.id), groupId));
        Query q = entityManager.createQuery(query);
        return q.getResultList();
    }

    // EM: what is needed for admin?
    public List<VirtualCorpus> retrieveVirtualCorpusByAdmin () {
        return null;

    }


}
