package de.ids_mannheim.korap.resources;

/**
 * @author hanl
 * @date 25/06/2014
 */
public class Layer extends KustvaktResource {

    public Layer() {
        super();
    }


    public Layer(Integer id, int creator) {
        super(id, creator);
    }

    // layer name must not be unique!
    public Layer(Integer id, String name, int creator) {
        super(id, creator);
        this.setName(name);
    }

    public Layer(String persistentID, String name, int creator) {
        super(persistentID, creator);
        this.setPersistentID(persistentID);
        this.setName(name);
    }

    public Layer(String persistentID, int creator) {
        super(persistentID, creator);
    }

}
