package de.ids_mannheim.korap.resources;

/**
 * @author hanl
 * @date 25/06/2014
 */
public class Foundry extends KustvaktResource {

    public Foundry() {
        super();
    }

    public Foundry(Integer id, int creator) {
        super(id, creator);
    }

    public Foundry(String persistentID, int creator) {
        super(persistentID, creator);
        this.setName(persistentID);
    }

}
