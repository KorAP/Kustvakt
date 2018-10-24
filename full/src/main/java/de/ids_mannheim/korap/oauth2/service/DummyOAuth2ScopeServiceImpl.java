package de.ids_mannheim.korap.oauth2.service;

import java.util.Collection;
import java.util.Set;

import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.oauth2.constant.OAuth2Scope;
import de.ids_mannheim.korap.oauth2.entity.AccessScope;
import de.ids_mannheim.korap.security.context.TokenContext;

public class DummyOAuth2ScopeServiceImpl implements OAuth2ScopeService {

    @Override
    public Set<AccessScope> convertToAccessScope (Collection<String> scopes)
            throws KustvaktException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String convertAccessScopesToString (Set<AccessScope> scopes) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Set<String> convertAccessScopesToStringSet (
            Set<AccessScope> scopes) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Set<String> filterScopes (Set<String> scopes,
            Set<String> defaultScopes) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void verifyScope (TokenContext context, OAuth2Scope requiredScope)
            throws KustvaktException {
        // TODO Auto-generated method stub

    }

    @Override
    public Set<AccessScope> verifyRefreshScope (Set<String> requestScopes,
            Set<AccessScope> originalScopes) throws KustvaktException {
        // TODO Auto-generated method stub
        return null;
    }

}
