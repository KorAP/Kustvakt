package de.ids_mannheim.korap.resources;

/**
 * @author hanl
 * @date 25/06/2014
 */
public class Layer extends KustvaktResource {

    public Layer () {
        super();
    }


    public Layer (Integer id, int creator) {
        super(id);
    }


    // layer name must not be unique!
    public Layer (Integer id, String name) {
        super(id);
        this.setName(name);
    }


    public Layer (String persistentID, String name) {
        super(persistentID);
        this.setPersistentID(persistentID);
        this.setName(name);
    }


    public Layer (String persistentID) {
        super(persistentID);
    }

}
