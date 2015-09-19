package de.ids_mannheim.korap.resources;

import de.ids_mannheim.korap.utils.JsonUtils;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.codec.digest.DigestUtils;

import java.util.Map;

@Getter
@Setter
public class VirtualCollection extends KustvaktResource {

    private String query;
    // use ehcache instead and only save persisted values in the database
    //    @Deprecated
    //    private boolean cache = false;
    private Map stats;

    protected VirtualCollection() {
        super();
        this.setPersistentID(this.createID());
    }

    protected VirtualCollection(Integer id, int creator, long created) {
        super(id, creator, created);
    }

    public VirtualCollection(Integer id, int creator) {
        super(id, creator);
    }

    public VirtualCollection(String persistentID, int creator) {
        super(persistentID, creator);
    }

    public VirtualCollection(String query) {
        this();
        this.setQuery(query);
        this.setPersistentID(this.createID());
    }

    @Override
    protected String createID() {
        if (this.query != null) {
            String s = this.getQuery();
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
        this.setStats(
                this.getStats() == null ? other.getStats() : this.getStats());
        this.setQuery(
                this.getQuery() == null ? other.getQuery() : this.getQuery());
    }

    @Override
    @SuppressWarnings("unchecked")
    public void checkNull() {
        super.checkNull();
        this.setDescription(
                this.getDescription() == null ? "" : this.getDescription());
        this.setQuery(this.query == null ? "" : this.getQuery());
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
                ", query='" + query + '\'' +
                ", stats='" + stats + '\'' +
                '}';
    }

    @Override
    public Map toMap() {
        Map res = super.toMap();
        res.put("query", JsonUtils.readTree(query));
        res.put("statistics", stats);
        return res;
    }

}
