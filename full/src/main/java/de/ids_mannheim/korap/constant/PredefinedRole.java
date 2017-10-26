package de.ids_mannheim.korap.constant;

public enum PredefinedRole {
    GROUP_ADMIN(1), GROUP_MEMBER(2), VC_ADMIN(3), VC_MEMBER(4);
    
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
