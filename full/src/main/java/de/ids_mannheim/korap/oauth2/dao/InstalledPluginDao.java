package de.ids_mannheim.korap.oauth2.dao;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.entity.InstalledPlugin;
import de.ids_mannheim.korap.entity.InstalledPlugin_;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.oauth2.entity.OAuth2Client;
import de.ids_mannheim.korap.oauth2.entity.OAuth2Client_;
import de.ids_mannheim.korap.utils.ParameterChecker;

@Repository
@Transactional
public class InstalledPluginDao {

    @PersistenceContext
    private EntityManager entityManager;

    public InstalledPlugin storeUserPlugin (OAuth2Client superClient,
            OAuth2Client client, String installedBy) throws KustvaktException {
        ParameterChecker.checkStringValue(installedBy, "installed_by");

        InstalledPlugin p = new InstalledPlugin();
        p.setInstalledBy(installedBy);
        p.setInstalledDate(
                ZonedDateTime.now(ZoneId.of(Attributes.DEFAULT_TIME_ZONE)));
        p.setClient(client);
        p.setSuperClient(superClient);
        entityManager.persist(p);
        return p;
    }

    public InstalledPlugin retrieveInstalledPlugin (String superClientId,
            String clientId, String installedBy) throws KustvaktException {
        ParameterChecker.checkStringValue(superClientId, "super_client_id");
        ParameterChecker.checkStringValue(clientId, "client_id");
        ParameterChecker.checkStringValue(installedBy, "installedBy");

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<InstalledPlugin> query = builder
                .createQuery(InstalledPlugin.class);

        Root<InstalledPlugin> root = query.from(InstalledPlugin.class);
        query.select(root);
        query.where(builder.and(
                builder.equal(root.get(InstalledPlugin_.INSTALLED_BY),
                        installedBy),
                builder.equal(
                        root.get(InstalledPlugin_.client).get(OAuth2Client_.id),
                        clientId),
                builder.equal(root.get(InstalledPlugin_.superClient)
                        .get(OAuth2Client_.id), superClientId)));

        Query q = entityManager.createQuery(query);
        try {
            return (InstalledPlugin) q.getSingleResult();
        }
        catch (NoResultException e) {
            throw new KustvaktException(StatusCodes.NO_RESOURCE_FOUND);
        }
    }

    public List<InstalledPlugin> retrieveInstalledPlugins (String superClientId,
            String installedBy) throws KustvaktException {
        ParameterChecker.checkStringValue(superClientId, "super_client_id");
        ParameterChecker.checkStringValue(installedBy, "installedBy");

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<InstalledPlugin> query = builder
                .createQuery(InstalledPlugin.class);

        Root<InstalledPlugin> root = query.from(InstalledPlugin.class);
        query.select(root);
        query.where(builder.and(
                builder.equal(root.get(InstalledPlugin_.INSTALLED_BY),
                        installedBy),
                builder.equal(root.get(InstalledPlugin_.superClient)
                        .get(OAuth2Client_.id), superClientId)));

        TypedQuery<InstalledPlugin> q = entityManager.createQuery(query);
        try {
            return q.getResultList();
        }
        catch (NoResultException e) {
            throw new KustvaktException(StatusCodes.NO_RESOURCE_FOUND);
        }
    }

    public void uninstallPlugin (String superClientId, String clientId,
            String username) throws KustvaktException {
        InstalledPlugin plugin = retrieveInstalledPlugin(superClientId,
                clientId, username);
        entityManager.remove(plugin);
    }

}
