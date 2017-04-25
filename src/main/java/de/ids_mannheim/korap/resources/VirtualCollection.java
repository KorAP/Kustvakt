package de.ids_mannheim.korap.resources;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class VirtualCollection extends KustvaktResource {

    //    private String query;
    // use ehcache instead and only save persisted values in the database
    //    @Deprecated
    //    private boolean cache = false;
    @Deprecated
    private Map stats;


    public VirtualCollection () {
        super();
    }


    public VirtualCollection (Integer id) {
        super(id);
    }


    public VirtualCollection (String persistentID) {
        super(persistentID);
    }


    @Override
    public void merge (KustvaktResource resource) {
        super.merge(resource);
        if (resource == null | !(resource instanceof VirtualCollection))
            return;
        VirtualCollection other = (VirtualCollection) resource;
        if (this.getFields() == null || this.getFields().isEmpty()){
        	setFields(other.getFields());
        }
    }


    @Override
    @SuppressWarnings("unchecked")
    public void checkNull () {
        this.setDescription(this.getDescription() == null ? "" : this
                .getDescription());
        super.checkNull();
    }


    @Override
    public String toString () {
        return "VirtualCollection{" + "id='" + this.getId() + '\''
                + ", persistentID='" + this.getPersistentID() + '\''
                + ", created=" + created + ", path=" + this.getPath()
                + ", name='" + this.getName() + '\'' + ", data='"
                + this.getData() + '\'' + '}';
    }

    //    @Override
    //    public Map toMap() {
    //        Map res = super.toMap();
    //        res.put("query", JsonUtils.readTree());
    //        if (stats != null && !stats.isEmpty())
    //            res.put("statistics", stats);
    //        return res;
    //    }

}
