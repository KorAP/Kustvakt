package de.ids_mannheim.korap.oauth2.service;

import de.ids_mannheim.korap.constant.OAuth2Scope;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.security.context.TokenContext;

/**
 * @author margaretha
 *
 */
public interface OAuth2ScopeService {

    /**
     * Verifies whether the given token context contains the required
     * scope
     * 
     * @param context a token context containing authorized scopes
     * @param requiredScope the required scope
     * @throws KustvaktException
     */
    void verifyScope (TokenContext context, OAuth2Scope requiredScope)
            throws KustvaktException;

}