package de.ids_mannheim.korap.oauth2.service;

import de.ids_mannheim.korap.constant.OAuth2Scope;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.security.context.TokenContext;

public class DummyOAuth2ScopeServiceImpl implements OAuth2ScopeService {

      @Override
    public void verifyScope (TokenContext context, OAuth2Scope requiredScope)
            throws KustvaktException {
        // TODO Auto-generated method stub

    }
}
