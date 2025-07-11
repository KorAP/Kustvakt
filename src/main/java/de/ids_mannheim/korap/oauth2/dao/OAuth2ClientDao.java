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
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;

import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.FullConfiguration;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.oauth2.constant.OAuth2ClientType;
import de.ids_mannheim.korap.oauth2.entity.AccessToken;
import de.ids_mannheim.korap.oauth2.entity.AccessToken_;
import de.ids_mannheim.korap.oauth2.entity.OAuth2Client;
import de.ids_mannheim.korap.oauth2.entity.OAuth2Client_;
import de.ids_mannheim.korap.oauth2.entity.RefreshToken;
import de.ids_mannheim.korap.oauth2.entity.RefreshToken_;
import de.ids_mannheim.korap.utils.ParameterChecker;

/**
 * Manages database queries and transactions regarding OAuth2 clients.
 * 
 * @author margaretha
 *
 */
@Transactional
@Repository
public class OAuth2ClientDao {

    @PersistenceContext
    private EntityManager entityManager;
    @Autowired
    private FullConfiguration config;

	public void registerClient (boolean isSuper, String id,
			String secretHashcode, String name, OAuth2ClientType type,
			String url, String redirectURI, String registeredBy,
			String description, int refreshTokenExpiry, JsonNode source,
			boolean isPermitted) throws KustvaktException {
		
		ParameterChecker.checkStringValue(id, "client_id");
        ParameterChecker.checkStringValue(name, "client_name");
        ParameterChecker.checkObjectValue(type, "client_type");
        ParameterChecker.checkStringValue(description, "client_description");
        // ParameterChecker.checkStringValue(url, "client url");
        // ParameterChecker.checkStringValue(redirectURI, "client
        // redirect uri");
        ParameterChecker.checkStringValue(registeredBy, "registered_by");

        OAuth2Client client = new OAuth2Client();
        client.setId(id);
        client.setName(name);
        client.setSecret(secretHashcode);
        client.setType(type);
        client.setUrl(url);
        client.setRedirectURI(redirectURI);
        client.setRegisteredBy(registeredBy);
        client.setRegistrationDate(ZonedDateTime.now());
        client.setDescription(description);
        
        if (isSuper) {
        	client.setSuper(true);
		}
        if (source != null && !source.isNull()) {
            if (type.equals(OAuth2ClientType.CONFIDENTIAL)) {
                client.setSource(source.toString());
                // setting permit for plugin, used in OAuth2TestBase
                // this should be an admin function
                if (isPermitted) { 
                	client.setPermitted(isPermitted);
                }
            }
            else {
                throw new KustvaktException(StatusCodes.NOT_SUPPORTED,
                        "Only confidential plugins are supported.");
            }
        }
        else {
            client.setPermitted(true);
        }
        if (refreshTokenExpiry <= 0) {
            if (type.equals(OAuth2ClientType.CONFIDENTIAL)) {
                refreshTokenExpiry = config.getRefreshTokenLongExpiry();
            }
        }
        else if (type.equals(OAuth2ClientType.PUBLIC)) {
            throw new KustvaktException(
                    StatusCodes.INVALID_REFRESH_TOKEN_EXPIRY,
                    "Custom refresh token expiry is only applicable for confidential clients");
        }
        else if (refreshTokenExpiry > 31536000) {
            throw new KustvaktException(
                    StatusCodes.INVALID_REFRESH_TOKEN_EXPIRY,
                    "Maximum refresh token expiry is 31536000 seconds (1 year)");
        }

        client.setRefreshTokenExpiry(refreshTokenExpiry);
        entityManager.persist(client);
	}
    
	public void registerClient (String id, String secretHashcode, String name,
			OAuth2ClientType type, String url, String redirectURI,
			String registeredBy, String description, int refreshTokenExpiry,
			JsonNode source) throws KustvaktException {
		registerClient(false, id, secretHashcode, name, type, url, redirectURI,
				registeredBy, description, refreshTokenExpiry, source, false);
	}

    public OAuth2Client retrieveClientById (String clientId)
            throws KustvaktException {

        ParameterChecker.checkStringValue(clientId, "client_id");

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<OAuth2Client> query = builder
                .createQuery(OAuth2Client.class);

        Root<OAuth2Client> root = query.from(OAuth2Client.class);
        query.select(root);
        query.where(builder.equal(root.get(OAuth2Client_.id), clientId));

        Query q = entityManager.createQuery(query);
        try {
            return (OAuth2Client) q.getSingleResult();
        }
        catch (NoResultException e) {
            throw new KustvaktException(StatusCodes.CLIENT_NOT_FOUND,
                    "Unknown client: " + clientId, "invalid_client");
        }
        catch (Exception e) {
            throw new KustvaktException(StatusCodes.CLIENT_NOT_FOUND,
                    e.getMessage(), "invalid_client");
        }
    }

    public void deregisterClient (OAuth2Client client)
            throws KustvaktException {
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

    public List<OAuth2Client> retrieveUserAuthorizedClients (String username)
            throws KustvaktException {
        ParameterChecker.checkStringValue(username, "username");

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<OAuth2Client> query = builder
                .createQuery(OAuth2Client.class);

        Root<OAuth2Client> client = query.from(OAuth2Client.class);
        Join<OAuth2Client, RefreshToken> refreshToken = client
                .join(OAuth2Client_.refreshTokens);
        Predicate condition = builder.and(
                builder.equal(refreshToken.get(RefreshToken_.userId), username),
                builder.equal(refreshToken.get(RefreshToken_.isRevoked), false),
                builder.greaterThan(
                        refreshToken
                                .<ZonedDateTime> get(RefreshToken_.expiryDate),
                        ZonedDateTime
                                .now(ZoneId.of(Attributes.DEFAULT_TIME_ZONE))));
        query.select(client);
        query.where(condition);
        query.distinct(true);
        TypedQuery<OAuth2Client> q = entityManager.createQuery(query);
        return q.getResultList();
    }

    public List<OAuth2Client> retrieveClientsByAccessTokens (String username)
            throws KustvaktException {
        ParameterChecker.checkStringValue(username, "username");

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<OAuth2Client> query = builder
                .createQuery(OAuth2Client.class);

        Root<OAuth2Client> client = query.from(OAuth2Client.class);
        Join<OAuth2Client, AccessToken> accessToken = client
                .join(OAuth2Client_.accessTokens);
        Predicate condition = builder.and(
                builder.equal(accessToken.get(AccessToken_.userId), username),
                builder.equal(accessToken.get(AccessToken_.isRevoked), false),
                builder.greaterThan(
                        accessToken
                                .<ZonedDateTime> get(AccessToken_.expiryDate),
                        ZonedDateTime
                                .now(ZoneId.of(Attributes.DEFAULT_TIME_ZONE))));
        query.select(client);
        query.where(condition);
        query.distinct(true);
        TypedQuery<OAuth2Client> q = entityManager.createQuery(query);
        return q.getResultList();
    }

    public List<OAuth2Client> retrieveUserRegisteredClients (String username)
            throws KustvaktException {
        ParameterChecker.checkStringValue(username, "username");

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<OAuth2Client> query = builder
                .createQuery(OAuth2Client.class);

        Root<OAuth2Client> client = query.from(OAuth2Client.class);
        query.select(client);
        query.where(builder.equal(client.get(OAuth2Client_.registeredBy),
                username));
        query.distinct(true);
        TypedQuery<OAuth2Client> q = entityManager.createQuery(query);
        return q.getResultList();
    }

    public List<OAuth2Client> retrievePlugins (boolean isPermittedOnly)
            throws KustvaktException {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<OAuth2Client> query = builder
                .createQuery(OAuth2Client.class);

        Root<OAuth2Client> client = query.from(OAuth2Client.class);
        Predicate restrictions = builder
                .isNotNull(client.get(OAuth2Client_.SOURCE));
        if (isPermittedOnly) {
            restrictions = builder.and(restrictions,
                    builder.isTrue(client.get(OAuth2Client_.IS_PERMITTED)));
        }

        query.select(client);
        query.where(restrictions);
        query.distinct(true);
        TypedQuery<OAuth2Client> q = entityManager.createQuery(query);
        return q.getResultList();
    }

}
