package de.ids_mannheim.korap.oauth2.dao;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.entity.InstalledPlugin;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.oauth2.entity.OAuth2Client;
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
}
