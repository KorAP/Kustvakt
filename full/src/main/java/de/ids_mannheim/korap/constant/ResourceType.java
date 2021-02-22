package de.ids_mannheim.korap.constant;

import de.ids_mannheim.korap.entity.QueryDO;

/** Defines types of {@link QueryDO} 
 * 
 * @author margaretha
 *
 */
/*
 * TODO (nd):
 *   This should probably be renamed to something like RessourceType,
 *   as QueryReferences will use the same types.
 */
public enum ResourceType {
    /**
     * available for all
     */
    SYSTEM, 
    // 
    /** available to project group members
     * 
     */
    PROJECT, 
    /**
     * available only for the creator
     */
    PRIVATE, 
    /**
     * available for all, but not listed for all 
     */
    PUBLISHED;
    
    public String displayName () {
        return name().toLowerCase();

    }
}
