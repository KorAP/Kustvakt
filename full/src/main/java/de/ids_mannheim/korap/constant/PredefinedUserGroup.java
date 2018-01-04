package de.ids_mannheim.korap.constant;

public enum PredefinedUserGroup {
    ALL("all users");
    
    private String value;

    PredefinedUserGroup (String value) {
        this.value = value;
    }
    
    public String getValue () {
        return value;
    }
}
