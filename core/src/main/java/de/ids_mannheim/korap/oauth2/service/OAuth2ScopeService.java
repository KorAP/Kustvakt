package de.ids_mannheim.korap.oauth2.service;

import de.ids_mannheim.korap.constant.OAuth2Scope;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.security.context.TokenContext;

public interface OAuth2ScopeService {

    
    void verifyScope (TokenContext context, OAuth2Scope requiredScope)
            throws KustvaktException;

}