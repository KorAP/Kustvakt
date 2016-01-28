package de.ids_mannheim.korap.user;

import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.utils.JsonUtils;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.*;

/**
 * @author hanl
 * @date 22/01/2016
 */
@Data
public abstract class Userdata {

    private Integer id;
    @Getter(AccessLevel.PRIVATE)
    @Setter(AccessLevel.PRIVATE)
    private Map<String, Object> fields;
    @Setter(AccessLevel.PRIVATE)
    private Integer userID;

    public Userdata(Integer userid) {
        this.fields = new HashMap<>();
        this.userID = userid;
        this.id = -1;
    }

    public void setData(Map<String, Object> map) throws KustvaktException {
        Set missing = missing(map);
        if (!missing.isEmpty())
            throw new KustvaktException(StatusCodes.MISSING_ARGUMENTS,
                    missing.toString());
        this.fields.clear();
        this.fields.putAll(map);
    }

    private Set<String> missing(Map<String, Object> map) {
        Set<String> missing = new HashSet<>();
        for (String key : requiredFields()) {
            if (!map.containsKey(key))
                missing.add(key);
        }
        return missing;
    }

    public int size() {
        return this.fields.size();
    }

    public Object get(String key) {
        return this.fields.get(key);
    }

    public boolean isValid() {
        //        return this.missing(this.fields).isEmpty() && this.userID != -1;
        return validationReturn().length == 0;
    }

    public String[] validationReturn() {
        StringBuilder b = new StringBuilder();
        Set<String> m = missing(this.fields);
        for (String k : m) {
            b.append(k).append(";");
        }
        return b.toString().split(";");
    }

    public Set<String> keys() {
        return this.fields.keySet();
    }

    public Collection<Object> values() {
        return this.fields.values();
    }

    public void setData(String data) {
        Map m = JsonUtils.readSimple(data, Map.class);
        if (m != null)
            this.fields.putAll(m);
    }

    public void update(Userdata other) {
        if (other != null && this.getClass().equals(other.getClass())) {
            if (!other.isValid()) {
                throw new RuntimeException(
                        "User data object not valid. Missing fields: "
                                + missing(this.fields));
            }
            this.fields.putAll(other.fields);
        }
    }

    public String data() {
        return JsonUtils.toJSON(this.fields);
    }

    public void addField(String key, Object value) {
        this.fields.put(key, value);
    }

    public abstract String[] requiredFields();

}
