package de.ids_mannheim.korap.resources;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.codec.digest.DigestUtils;

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

    protected VirtualCollection() {
        super();
        this.setPersistentID(this.createID());
    }

    public VirtualCollection(Integer id, int creator) {
        super(id, creator);
    }

    public VirtualCollection(String persistentID, int creator) {
        super(persistentID, creator);
    }

    public VirtualCollection(String query) {
        this();
        this.setFields(query);
        this.setPersistentID(this.createID());
    }

    // todo: redo!
    @Override
    protected String createID() {
        if (this.getData() != null) {
            String s = this.getData();
            return DigestUtils.sha1Hex(s);
        }
        return super.createID();
    }

    @Override
    public void merge(KustvaktResource resource) {
        super.merge(resource);
        if (resource == null | !(resource instanceof VirtualCollection))
            return;
        VirtualCollection other = (VirtualCollection) resource;
        this.setFields(this.getFields() == null ?
                other.getFields() :
                this.getFields());
    }

    @Override
    @SuppressWarnings("unchecked")
    public void checkNull() {
        super.checkNull();
        this.setDescription(
                this.getDescription() == null ? "" : this.getDescription());
    }

    @Override
    public String toString() {
        return "VirtualCollection{" +
                "id='" + this.getId() + '\'' +
                ", persistentID='" + this.getPersistentID() + '\'' +
                ", created=" + created +
                ", path=" + this.getPath() +
                ", owner=" + this.getOwner() +
                ", name='" + this.getName() + '\'' +
                ", data='" + this.getData() + '\'' +
                '}';
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
