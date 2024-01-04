package de.ids_mannheim.korap.oauth2.dao;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;

import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.oauth2.entity.AccessScope;
import de.ids_mannheim.korap.oauth2.entity.Authorization;

/**
 * AuthorizationDao is an interface describing methods for managing
 * authorizations.
 * 
 * @author margaretha
 *
 */
public interface AuthorizationDao {

    public Authorization storeAuthorizationCode (String clientId, String userId,
            String code, Set<AccessScope> scopes, String redirectURI,
            ZonedDateTime authenticationTime, String nonce)
            throws KustvaktException;

    public Authorization retrieveAuthorizationCode (String code)
            throws KustvaktException;

    public Authorization updateAuthorization (Authorization authorization)
            throws KustvaktException;

    public List<Authorization> retrieveAuthorizationsByClientId (
            String clientId);
}
