package de.ids_mannheim.korap.constant;

public enum PredefinedUserGroup {
    ALL("all users", 1);
    
    private String value;
    private int id;

    PredefinedUserGroup (String value, int id) {
        this.value = value;
        this.id = id;
    }
    
    public String getValue () {
        return value;
    }
    
    public int getId () {
        return id;
    }
}
