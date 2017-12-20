package de.ids_mannheim.korap.constant;

public enum PredefinedRole {
    USER_GROUP_ADMIN(1), USER_GROUP_MEMBER(2), VC_ACCESS_ADMIN(3), VC_ACCESS_MEMBER(4);
    
    private int id;
    private String name;

    PredefinedRole (int i) {
        this.id = i;
        this.name = name().toLowerCase().replace("_", " "); 
    }
    
    public int getId () {
        return id;
    }
    
    @Override
    public String toString () {
        return this.name;
    }
}
