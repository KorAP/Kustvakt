package de.ids_mannheim.korap.dao;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import de.ids_mannheim.korap.constant.ClientType;
import de.ids_mannheim.korap.entity.OAuth2Client;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.utils.ParameterChecker;

@Transactional
@Repository
public class OAuth2ClientDao {

    @PersistenceContext
    private EntityManager entityManager;

    public void registerClient (String id, String secret, String name,
            ClientType type, String url, String redirectURI)
            throws KustvaktException {
        ParameterChecker.checkStringValue(id, "client id");
        ParameterChecker.checkStringValue(name, "client name");
        ParameterChecker.checkObjectValue(type, "client type");
        ParameterChecker.checkStringValue(url, "client url");
        ParameterChecker.checkStringValue(redirectURI, "client redirect uri");

        OAuth2Client client = new OAuth2Client();
        client.setId(id);
        client.setName(name);
        client.setSecret(secret);
        client.setType(type);
        client.setUrl(url);
        client.setRedirectURI(redirectURI);

        entityManager.persist(client);
    }


}
