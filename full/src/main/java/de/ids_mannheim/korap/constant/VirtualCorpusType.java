package de.ids_mannheim.korap.constant;

public enum VirtualCorpusType {
    // available for all
    PREDEFINED, 
    // available to project group members
    PROJECT, 
    // available only for the creator
    PRIVATE, 
    // available for all, but not listed for all
    PUBLISHED;
    
    public String displayName () {
        return name().toLowerCase();

    }
}
