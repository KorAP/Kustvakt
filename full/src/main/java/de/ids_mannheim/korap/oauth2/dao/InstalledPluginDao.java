package de.ids_mannheim.korap.oauth2.dao;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Root;

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

    public InstalledPlugin storeUserPlugin (OAuth2Client client,
            String installedBy) throws KustvaktException {
        ParameterChecker.checkStringValue(installedBy, "installed_by");

        InstalledPlugin p = new InstalledPlugin();
        p.setInstalledBy(installedBy);
        p.setInstalledDate(
                ZonedDateTime.now(ZoneId.of(Attributes.DEFAULT_TIME_ZONE)));
        p.setClient(client);
        entityManager.persist(p);
        return p;
    }

    public InstalledPlugin retrieveInstalledPlugin (String clientId,
            String installedBy) throws KustvaktException {
        ParameterChecker.checkStringValue(clientId, "client_id");
        ParameterChecker.checkStringValue(installedBy, "installedBy");

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<InstalledPlugin> query =
                builder.createQuery(InstalledPlugin.class);

        Root<InstalledPlugin> root = query.from(InstalledPlugin.class);
        Join<InstalledPlugin, OAuth2Client> client =
                root.join(InstalledPlugin_.client);
        query.select(root);
        query.where(builder.and(
                builder.equal(root.get(InstalledPlugin_.INSTALLED_BY),
                        installedBy),
                builder.equal(client.get(OAuth2Client_.id), clientId)));

        Query q = entityManager.createQuery(query);
        try {
            return (InstalledPlugin) q.getSingleResult();
        }
        catch (NoResultException e) {
            throw new KustvaktException(StatusCodes.NO_RESOURCE_FOUND);
        }
    }
}
