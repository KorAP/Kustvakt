package de.ids_mannheim.korap.oauth2.service;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;

import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.constant.TokenType;
import de.ids_mannheim.korap.dao.AdminDao;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.oauth2.constant.OAuth2Error;
import de.ids_mannheim.korap.oauth2.constant.OAuth2Scope;
import de.ids_mannheim.korap.oauth2.dao.AccessScopeDao;
import de.ids_mannheim.korap.oauth2.entity.AccessScope;
import de.ids_mannheim.korap.security.context.TokenContext;

public class OAuth2ScopeServiceImpl implements OAuth2ScopeService {

    @Autowired
    private AccessScopeDao accessScopeDao;

    @Autowired
    private AdminDao adminDao;

    /* (non-Javadoc)
     * @see de.ids_mannheim.korap.oauth2.service.OAuth2ScopeService#convertToAccessScope(java.util.Collection)
     */
    @Override
    public Set<AccessScope> convertToAccessScope (Collection<String> scopes)
            throws KustvaktException {

        List<AccessScope> definedScopes = accessScopeDao.retrieveAccessScopes();
        Set<AccessScope> requestedScopes =
                new HashSet<AccessScope>(scopes.size());
        int index;
        OAuth2Scope oauth2Scope = null;
        for (String scope : scopes) {
            try{
                oauth2Scope = Enum.valueOf(OAuth2Scope.class, scope.toUpperCase());
            }
            catch (IllegalArgumentException e) {
                throw new KustvaktException(StatusCodes.INVALID_SCOPE,
                        scope + " is an invalid scope",
                        OAuth2Error.INVALID_SCOPE);
            }
            
            index = definedScopes.indexOf(new AccessScope(oauth2Scope));
            if (index == -1) {
                throw new KustvaktException(StatusCodes.INVALID_SCOPE,
                        scope + " is an invalid scope",
                        OAuth2Error.INVALID_SCOPE);
            }
            else {
                requestedScopes.add(definedScopes.get(index));
            }
        }
        return requestedScopes;
    }

    /* (non-Javadoc)
     * @see de.ids_mannheim.korap.oauth2.service.OAuth2ScopeService#convertAccessScopesToString(java.util.Set)
     */
    @Override
    public String convertAccessScopesToString (Set<AccessScope> scopes) {
        Set<String> set = convertAccessScopesToStringSet(scopes);
        return String.join(" ", set);
    }

    /* (non-Javadoc)
     * @see de.ids_mannheim.korap.oauth2.service.OAuth2ScopeService#convertAccessScopesToStringSet(java.util.Set)
     */
    @Override
    public Set<String> convertAccessScopesToStringSet (
            Set<AccessScope> scopes) {
        Set<String> set = scopes.stream().map(scope -> scope.toString())
                .collect(Collectors.toSet());
        return set;
    }

    /* (non-Javadoc)
     * @see de.ids_mannheim.korap.oauth2.service.OAuth2ScopeService#filterScopes(java.util.Set, java.util.Set)
     */
    @Override
    public Set<String> filterScopes (Set<String> scopes,
            Set<String> defaultScopes) {
        Stream<String> stream = scopes.stream();
        Set<String> filteredScopes =
                stream.filter(scope -> defaultScopes.contains(scope))
                        .collect(Collectors.toSet());
        return filteredScopes;
    }

    /* (non-Javadoc)
     * @see de.ids_mannheim.korap.oauth2.service.OAuth2ScopeService#verifyScope(de.ids_mannheim.korap.security.context.TokenContext, de.ids_mannheim.korap.oauth2.constant.OAuth2Scope)
     */
    @Override
    public void verifyScope (TokenContext context, OAuth2Scope requiredScope)
            throws KustvaktException {
        if (!adminDao.isAdmin(context.getUsername())
                && context.getTokenType().equals(TokenType.BEARER)) {
            Map<String, Object> parameters = context.getParameters();
            String authorizedScope = (String) parameters.get(Attributes.SCOPE);
            if (!authorizedScope.contains(OAuth2Scope.ALL.toString())
                    && !authorizedScope.contains(requiredScope.toString())) {
                throw new KustvaktException(StatusCodes.AUTHORIZATION_FAILED,
                        "Scope " + requiredScope + " is not authorized");
            }
        }
    }

    /* (non-Javadoc)
     * @see de.ids_mannheim.korap.oauth2.service.OAuth2ScopeService#verifyRefreshScope(java.util.Set, java.util.Set)
     */
    @Override
    public Set<AccessScope> verifyRefreshScope (Set<String> requestScopes,
            Set<AccessScope> originalScopes) throws KustvaktException {
        Set<AccessScope> requestedScopes = convertToAccessScope(requestScopes);
        for (AccessScope scope : requestedScopes) {
            if (!originalScopes.contains(scope)) {
                throw new KustvaktException(StatusCodes.INVALID_SCOPE,
                        "Scope " + scope.getId() + " is not authorized.",
                        OAuth2Error.INVALID_SCOPE);
            }
        }
        return requestedScopes;
    }
}
