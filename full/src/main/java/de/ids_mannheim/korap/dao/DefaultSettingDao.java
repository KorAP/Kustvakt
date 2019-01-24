package de.ids_mannheim.korap.dao;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import de.ids_mannheim.korap.entity.DefaultSetting;
import de.ids_mannheim.korap.entity.DefaultSetting_;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.utils.ParameterChecker;

/**
 * DefaultSettingDao manages database queries and transactions
 * regarding user default setting.
 * 
 * @author margaretha
 *
 */
@Transactional
@Repository
public class DefaultSettingDao {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Creates a new entry of default setting in the database. This
     * method allows storing settings of an empty json object, i.e.
     * {}.
     * 
     * @param username
     *            username
     * @param settings
     *            default settings in json
     * @throws KustvaktException
     */
    public void createDefaultSetting (String username, String settings)
            throws KustvaktException {
        ParameterChecker.checkStringValue(username, "username");
        ParameterChecker.checkStringValue(settings, "settings");
        DefaultSetting us = new DefaultSetting(username, settings);
        entityManager.persist(us);
    }

    public void updateDefaultSetting (DefaultSetting defaultSetting)
            throws KustvaktException {
        ParameterChecker.checkObjectValue(defaultSetting, "defaultSetting");
        entityManager.merge(defaultSetting);
    }

    public void deleteDefaultSetting (DefaultSetting defaultSetting)
            throws KustvaktException {
        ParameterChecker.checkObjectValue(defaultSetting, "defaultSetting");
        if (!entityManager.contains(defaultSetting)) {
            defaultSetting = entityManager.merge(defaultSetting);
        }
        entityManager.remove(defaultSetting);

    }

    public DefaultSetting retrieveDefautlSetting (String username)
            throws KustvaktException {
        ParameterChecker.checkStringValue(username, "username");

        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<DefaultSetting> query =
                criteriaBuilder.createQuery(DefaultSetting.class);
        Root<DefaultSetting> defaultSetting = query.from(DefaultSetting.class);

        query.select(defaultSetting);
        query.where(criteriaBuilder
                .equal(defaultSetting.get(DefaultSetting_.username), username));
        Query q = entityManager.createQuery(query);
        try {
            return (DefaultSetting) q.getSingleResult();
        }
        catch (NoResultException e) {
            return null;
        }
    }
}
