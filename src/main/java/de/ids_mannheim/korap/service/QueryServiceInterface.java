package de.ids_mannheim.korap.service;

import de.ids_mannheim.korap.constant.QueryType;
import de.ids_mannheim.korap.entity.QueryDO;
import de.ids_mannheim.korap.exceptions.KustvaktException;

/**
 * Minimal interface exposing the query lookup used by other components.
 */
public interface QueryServiceInterface {

    /**
     * Search for a query (virtual corpus or stored query) by name and owner.
     *
     * @param username the user performing the lookup (for access checks)
     * @param vcName   the query name (virtual corpus name)
     * @param vcOwner  the owner/creator of the query (e.g. "system" or username)
     * @param queryType the type of query to look up
     * @return the matching QueryDO
     * @throws KustvaktException on not found or authorization problems
     */
    QueryDO searchQueryByName(String username, String vcName,
                              String vcOwner, QueryType queryType)
            throws KustvaktException;
}
