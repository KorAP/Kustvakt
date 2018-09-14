package de.ids_mannheim.korap.dao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

/**
 * VirtualCorpusDao manages SQL queries regarding virtual corpora,
 * e.g. retrieving and storing virtual corpora.
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
            CorpusAccess requiredAccess, String koralQuery, String definition,
            String description, String status, boolean isCached,
            String createdBy) throws KustvaktException {

        VirtualCorpus vc = new VirtualCorpus();
        vc.setName(name);
        vc.setType(type);
        vc.setRequiredAccess(requiredAccess);
        vc.setKoralQuery(koralQuery);
        vc.setDefinition(definition);
        vc.setDescription(description);
        vc.setStatus(status);
        vc.setCreatedBy(createdBy);
        vc.setCached(isCached);

        entityManager.persist(vc);
        return vc.getId();
    }

    public void editVirtualCorpus (VirtualCorpus vc, String name,
            VirtualCorpusType type, CorpusAccess requiredAccess,
            String koralQuery, String definition, String description,
            String status) throws KustvaktException {

        if (name != null && !name.isEmpty()) {
            vc.setName(name);
        }
        if (type != null) {
            vc.setType(type);
        }
        if (requiredAccess != null) {
            vc.setRequiredAccess(requiredAccess);
        }
        if (koralQuery != null) {
            vc.setKoralQuery(koralQuery);
        }
        if (definition != null && !definition.isEmpty()) {
            vc.setDefinition(definition);
        }
        if (description != null && !description.isEmpty()) {
            vc.setDescription(description);
        }
        if (status != null && !status.isEmpty()) {
            vc.setStatus(status);
        }
        entityManager.merge(vc);
    }

    public void deleteVirtualCorpus (VirtualCorpus vc)
            throws KustvaktException {
        if (!entityManager.contains(vc)) {
            vc = entityManager.merge(vc);
        }
        entityManager.remove(vc);

    }

    /**
     * System admin function.
     * 
     * Retrieves virtual corpus by creator and type. If type is not
     * specified,
     * retrieves virtual corpora of all types. If createdBy is not
     * specified,
     * retrieves virtual corpora of all users.
     * 
     * @param type
     *            {@link VirtualCorpusType}
     * @param createdBy
     *            username of the virtual corpus creator
     * @return a list of {@link VirtualCorpus}
     * @throws KustvaktException
     */
    @SuppressWarnings("unchecked")
    public List<VirtualCorpus> retrieveVCByType (VirtualCorpusType type,
            String createdBy) throws KustvaktException {

        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<VirtualCorpus> query =
                criteriaBuilder.createQuery(VirtualCorpus.class);
        Root<VirtualCorpus> virtualCorpus = query.from(VirtualCorpus.class);

        Predicate conditions = null;
        if (createdBy != null && !createdBy.isEmpty()) {
            conditions = criteriaBuilder.equal(
                    virtualCorpus.get(VirtualCorpus_.createdBy), createdBy);
            if (type != null) {
                conditions = criteriaBuilder.and(conditions, criteriaBuilder
                        .equal(virtualCorpus.get(VirtualCorpus_.type), type));
            }
        }
        else if (type != null) {
            conditions = criteriaBuilder
                    .equal(virtualCorpus.get(VirtualCorpus_.type), type);
        }

        query.select(virtualCorpus);
        if (conditions != null) {
            query.where(conditions);
        }
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

    public VirtualCorpus retrieveVCByName (String vcName, String createdBy)
            throws KustvaktException {
        ParameterChecker.checkStringValue(createdBy, "createdBy");
        ParameterChecker.checkStringValue(vcName, "vcName");

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<VirtualCorpus> query =
                builder.createQuery(VirtualCorpus.class);

        Root<VirtualCorpus> virtualCorpus = query.from(VirtualCorpus.class);

        Predicate condition = builder.and(
                builder.equal(virtualCorpus.get(VirtualCorpus_.createdBy),
                        createdBy),
                builder.equal(virtualCorpus.get(VirtualCorpus_.name), vcName));

        query.select(virtualCorpus);
        query.where(condition);

        Query q = entityManager.createQuery(query);
        VirtualCorpus vc = null;
        try {
            vc = (VirtualCorpus) q.getSingleResult();
        }
        catch (NoResultException e) {
            String vcCode = createdBy + "/" + vcName;
            throw new KustvaktException(StatusCodes.NO_RESULT_FOUND,
                    "No result found for query: retrieve virtual corpus by name "
                            + vcCode,
                    String.valueOf(vcCode), e);
        }
        catch (NonUniqueResultException e) {
            String vcCode = createdBy + "/" + vcName;
            throw new KustvaktException(StatusCodes.NON_UNIQUE_RESULT_FOUND,
                    "Non unique result found for query: retrieve virtual corpus by name "
                            + vcCode,
                    String.valueOf(vcCode), e);
        }
        return vc;
    }

    @SuppressWarnings("unchecked")
    public List<VirtualCorpus> retrieveOwnerVC (String userId)
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

    @SuppressWarnings("unchecked")
    public List<VirtualCorpus> retrieveOwnerVCByType (String userId,
            VirtualCorpusType type) throws KustvaktException {
        ParameterChecker.checkStringValue(userId, "userId");

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<VirtualCorpus> query =
                builder.createQuery(VirtualCorpus.class);

        Root<VirtualCorpus> virtualCorpus = query.from(VirtualCorpus.class);
        query.select(virtualCorpus);

        Predicate p = builder.and(
                builder.equal(virtualCorpus.get(VirtualCorpus_.createdBy),
                        userId),
                builder.equal(virtualCorpus.get(VirtualCorpus_.type), type));
        query.where(p);

        Query q = entityManager.createQuery(query);
        return q.getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<VirtualCorpus> retrieveGroupVCByUser (String userId)
            throws KustvaktException {
        ParameterChecker.checkStringValue(userId, "userId");

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<VirtualCorpus> query =
                builder.createQuery(VirtualCorpus.class);

        Root<VirtualCorpus> virtualCorpus = query.from(VirtualCorpus.class);
        Join<VirtualCorpus, VirtualCorpusAccess> access =
                virtualCorpus.join(VirtualCorpus_.virtualCorpusAccess);

        // Predicate corpusStatus = builder.and(
        // builder.notEqual(access.get(VirtualCorpusAccess_.status),
        // VirtualCorpusAccessStatus.HIDDEN),
        // builder.notEqual(access.get(VirtualCorpusAccess_.status),
        // VirtualCorpusAccessStatus.DELETED));

        Predicate corpusStatus =
                builder.notEqual(access.get(VirtualCorpusAccess_.status),
                        VirtualCorpusAccessStatus.DELETED);

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

    public List<VirtualCorpus> retrieveVCByUser (String userId)
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
                        VirtualCorpusType.SYSTEM));

        query.select(virtualCorpus);
        query.where(predicate);
        query.distinct(true);
        Query q = entityManager.createQuery(query);

        @SuppressWarnings("unchecked")
        List<VirtualCorpus> vcList = q.getResultList();
        List<VirtualCorpus> groupVC = retrieveGroupVCByUser(userId);
        Set<VirtualCorpus> vcSet = new HashSet<VirtualCorpus>();
        vcSet.addAll(vcList);
        vcSet.addAll(groupVC);

        List<VirtualCorpus> merger = new ArrayList<VirtualCorpus>(vcSet.size());
        merger.addAll(vcSet);
        Collections.sort(merger);
        return merger;
    }

    // for admins
    @SuppressWarnings("unchecked")
    public List<VirtualCorpus> retrieveVCByGroup (int groupId)
            throws KustvaktException {
        ParameterChecker.checkIntegerValue(groupId, "groupId");

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<VirtualCorpus> query =
                builder.createQuery(VirtualCorpus.class);

        Root<VirtualCorpus> virtualCorpus = query.from(VirtualCorpus.class);
        Join<VirtualCorpus, VirtualCorpusAccess> corpusAccess =
                virtualCorpus.join(VirtualCorpus_.virtualCorpusAccess);
        Join<VirtualCorpusAccess, UserGroup> accessGroup =
                corpusAccess.join(VirtualCorpusAccess_.userGroup);

        query.select(virtualCorpus);
        query.where(builder.equal(accessGroup.get(UserGroup_.id), groupId));
        Query q = entityManager.createQuery(query);
        return q.getResultList();
    }

}
