package de.ids_mannheim.korap.constant;

public enum PredefinedRole {
    GROUP_ADMIN(1), GROUP_MEMBER(2), VC_ADMIN(3), VC_MEMBER(3);
    
    private int id;

    PredefinedRole (int i) {
        this.id = i;
    }
    
    public int getId () {
        return id;
    }
}
