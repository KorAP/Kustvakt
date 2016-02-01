package de.ids_mannheim.korap.user;

import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.interfaces.EncryptionIface;
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
        return missing().length == 0;
    }

    public String[] missing() {
        StringBuilder b = new StringBuilder();
        Set<String> m = missing(this.fields);

        if (m.isEmpty())
            return new String[0];

        for (String k : m) {
            b.append(k).append(";");
        }
        return b.toString().split(";");
    }

    public void checkRequired() throws KustvaktException {
        if (!isValid()) {
            String[] fields = missing();
            throw new KustvaktException(StatusCodes.MISSING_ARGUMENTS,
                    "User data object not valid. Missing fields: " + Arrays
                            .asList(fields));
        }
    }

    public Set<String> keys() {
        return this.fields.keySet();
    }

    public Collection<Object> values() {
        return this.fields.values();
    }

    public Map<String, Object> fields() {
        return new HashMap<>(this.fields);
    }

    public void setData(String data) {
        Map m = JsonUtils.readSimple(data, Map.class);
        if (m != null)
            this.fields.putAll(m);
    }

    public void update(Userdata other) {
        if (other != null && this.getClass().equals(other.getClass()))
            this.fields.putAll(other.fields);
    }

    public String data() {
        return JsonUtils.toJSON(this.fields);
    }

    public void addField(String key, Object value) {
        this.fields.put(key, value);
    }

    public void validate(EncryptionIface crypto) throws KustvaktException {
        this.fields = crypto.validateMap(this.fields);
    }

    public void readDefaults(Map<String, Object> map) throws KustvaktException {
        for (String k : defaultFields()) {
            Object o = map.get(k);
            if (o != null)
                this.fields.put(k, o);
        }
        this.checkRequired();
    }

    public abstract String[] requiredFields();

    public abstract String[] defaultFields();

}
