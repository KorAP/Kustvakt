package de.ids_mannheim.korap.oauth2.dao;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.oauth2.constant.OAuth2ClientType;
import de.ids_mannheim.korap.oauth2.entity.OAuth2Client;
import de.ids_mannheim.korap.oauth2.entity.OAuth2ClientUrl;
import de.ids_mannheim.korap.oauth2.entity.OAuth2Client_;
import de.ids_mannheim.korap.utils.ParameterChecker;

@Transactional
@Repository
public class OAuth2ClientDao {

    @PersistenceContext
    private EntityManager entityManager;

    public void registerClient (String id, String secretHashcode, String name,
            OAuth2ClientType type, boolean isNative, String url,
            int urlHashCode, String redirectURI, String registeredBy,
            String description) throws KustvaktException {
        ParameterChecker.checkStringValue(id, "client id");
        ParameterChecker.checkStringValue(name, "client name");
        ParameterChecker.checkObjectValue(type, "client type");
        ParameterChecker.checkStringValue(description, "client description");
        // ParameterChecker.checkStringValue(url, "client url");
        // ParameterChecker.checkStringValue(redirectURI, "client
        // redirect uri");
        ParameterChecker.checkStringValue(registeredBy, "registeredBy");

        OAuth2Client client = new OAuth2Client();
        client.setId(id);
        client.setName(name);
        client.setSecret(secretHashcode);
        client.setType(type);
        client.setNative(isNative);
        if (urlHashCode != 0) {
            OAuth2ClientUrl clientUrl = new OAuth2ClientUrl();
            clientUrl.setUrl(url);
            clientUrl.setUrlHashCode(urlHashCode);
            client.setClientUrl(clientUrl);
        }
        client.setRedirectURI(redirectURI);
        client.setRegisteredBy(registeredBy);
        client.setDescription(description);
        entityManager.persist(client);
    }

    public OAuth2Client retrieveClientById (String clientId)
            throws KustvaktException {

        ParameterChecker.checkStringValue(clientId, "client_id");

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<OAuth2Client> query =
                builder.createQuery(OAuth2Client.class);

        Root<OAuth2Client> root = query.from(OAuth2Client.class);
        query.select(root);
        query.where(builder.equal(root.get(OAuth2Client_.id), clientId));

        Query q = entityManager.createQuery(query);
        try {
            return (OAuth2Client) q.getSingleResult();
        }
        catch (NoResultException e) {
            throw new KustvaktException(StatusCodes.CLIENT_NOT_FOUND,
                    "Unknown client with " + clientId + ".", "invalid_client");
        }
        catch (Exception e) {
            throw new KustvaktException(StatusCodes.CLIENT_NOT_FOUND,
                    e.getMessage(), "invalid_client");
        }
    }

    public void deregisterClient (OAuth2Client client) throws KustvaktException {
        ParameterChecker.checkObjectValue(client, "client");
        if (!entityManager.contains(client)) {
            client = entityManager.merge(client);
        }
        entityManager.remove(client);
    }

    public void updateClient (OAuth2Client client) throws KustvaktException {
        ParameterChecker.checkObjectValue(client, "client");
        client = entityManager.merge(client);
    }

}
