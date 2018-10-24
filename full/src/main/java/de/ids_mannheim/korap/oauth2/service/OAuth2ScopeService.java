package de.ids_mannheim.korap.oauth2.service;

import java.util.Collection;
import java.util.Set;

import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.oauth2.constant.OAuth2Scope;
import de.ids_mannheim.korap.oauth2.entity.AccessScope;
import de.ids_mannheim.korap.security.context.TokenContext;

public interface OAuth2ScopeService {

    /**
     * Converts a set of scope strings to a set of {@link AccessScope}
     * 
     * @param scopes
     * @return
     * @throws KustvaktException
     */
    Set<AccessScope> convertToAccessScope (Collection<String> scopes)
            throws KustvaktException;

    String convertAccessScopesToString (Set<AccessScope> scopes);

    Set<String> convertAccessScopesToStringSet (Set<AccessScope> scopes);

    /**
     * Simple reduction of requested scopes, i.e. excluding any scopes
     * that are not default scopes for a specific authorization grant.
     * 
     * @param scopes
     * @param defaultScopes
     * @return accepted scopes
     */
    Set<String> filterScopes (Set<String> scopes, Set<String> defaultScopes);

    void verifyScope (TokenContext context, OAuth2Scope requiredScope)
            throws KustvaktException;

    /**
     * Verify scopes given in a refresh request. The scopes must not
     * include other scopes than those authorized in the original
     * access token issued together with the refresh token.
     * 
     * @param requestScopes
     *            requested scopes
     * @param originalScopes
     *            authorized scopes
     * @return a set of requested {@link AccessScope}
     * @throws KustvaktException
     */
    Set<AccessScope> verifyRefreshScope (Set<String> requestScopes,
            Set<AccessScope> originalScopes) throws KustvaktException;

}