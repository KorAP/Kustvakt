package de.ids_mannheim.korap.resources;

/**
 * @author hanl
 * @date 25/06/2014
 */
public class Foundry extends KustvaktResource {

    public Foundry() {
        super();
    }

    public Foundry(Integer id, int creator, long created) {
        super(id, creator, created);
    }

    public Foundry(Integer id, int creator) {
        super(id, creator);
    }

    public Foundry(Integer id, String persistentID, int creator) {
        super(id, creator);
        this.setName(persistentID);
        this.setPersistentID(persistentID);
    }

    public Foundry(String persistentID,int creator) {
        super(persistentID, creator);
        this.setName(persistentID);
    }

}
