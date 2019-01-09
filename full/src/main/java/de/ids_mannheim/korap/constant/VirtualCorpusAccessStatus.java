package de.ids_mannheim.korap.constant;

import de.ids_mannheim.korap.entity.VirtualCorpusAccess;

/** Defines possible statuses of {@link VirtualCorpusAccess}
 * 
 * @author margaretha
 * @see VirtualCorpusAccess
 *
 */
public enum VirtualCorpusAccessStatus {

    ACTIVE, DELETED,
    // has not been used yet
    PENDING,
    // access for hidden group
    // maybe not necessary?
    HIDDEN;
}
