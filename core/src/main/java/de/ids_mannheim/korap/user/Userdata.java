package de.ids_mannheim.korap.user;

import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.interfaces.ValidatorIface;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.util.*;

/**
 * @author hanl, margaretha
 * @date 22/01/2016
 * 
 */
public abstract class Userdata {

    public DataFactory dataFactory = DataFactory.getFactory();
    
    @Deprecated
    @Getter
    @Setter
    private Integer id;
    @Getter(AccessLevel.PRIVATE)
    private Object data;
    @Deprecated
    @Getter
    @Setter
    private Integer userId;
    
    public Userdata () {
        this(-1);
    }

    // EM: replace with username
    @Deprecated
    public Userdata(Integer userid) {
        this.userId = userid;
        this.id = -1;
        this.data = dataFactory.convertData(null);
    }

    public Userdata (String data) {
        this.data = dataFactory.convertData(data);
    }

    public int size () {
        return dataFactory.size(this.data);
    }


    public Object get (String key) {
        return dataFactory.getValue(this.data, key);
    }

    public Object filter(String ... keys) {
        return dataFactory.filter(this.data, keys);
    }


    /**
     * 
     * @return
     */
    public boolean isValid () {
        return findMissingFields().length == 0;
    }


    public String[] findMissingFields () {
        Set<String> missing = new HashSet<>();
        Set<String> keys = dataFactory.keys(this.data);
        for (String key : requiredFields()) {
            if (!keys.contains(key))
                missing.add(key);
        }
        return missing.toArray(new String[0]);
    }


    public void checkRequired () throws KustvaktException {
        String[] fields = findMissingFields();
        if (findMissingFields().length != 0) {
            throw new KustvaktException(userId, StatusCodes.MISSING_PARAMETER,
                    "User data object not valid. Object has missing fields!",
                    Arrays.asList(fields).toString());
        }
    }


    //fixme: if data array, return empty?!
    public Set<String> keys () {
        return dataFactory.keys(this.data);
    }


    public Collection<Object> values () {
        return dataFactory.values(this.data);
    }


    public void setData (String data) {
        this.data = dataFactory.convertData(data);
    }


    public void update (Userdata other) {
        if (other != null && this.getClass().equals(other.getClass()))
            this.data = dataFactory.merge(this.data, other.data);
    }


    public String serialize () throws KustvaktException {
        // to have consistency with required fields --> updates/deletion may cause required fields to be missing.
        this.checkRequired();
        return dataFactory.toStringValue(this.data);
    }


    public void setField (String key, Object value) {
        dataFactory.addValue(this.data, key, value);
    }

    // EM: not reliable
    // todo: test
    public void validate (ValidatorIface validator) throws KustvaktException {
        dataFactory.validate(this.data, validator);
    }


    public void read (Map<String, Object> map, boolean defaults_only)
            throws KustvaktException {
        this.readQuietly(map, defaults_only);
        this.checkRequired();
    }


    public void readQuietly (Map<String, Object> map, boolean defaults_only) {
        if (map != null){
            if (defaults_only) {
                for (String k : defaultFields()) {
                    Object o = map.get(k);
                    if (o != null) {
                        dataFactory.addValue(this.data, k, o);
                    }
                }
            }
            else {
                for (String key : map.keySet())
                    dataFactory.addValue(this.data, key, map.get(key));
            }
        }
    }

    // EM: added
    public boolean removeField (String field) {
        return dataFactory.removeValue(this.data, field);
    }
    
    public abstract String[] requiredFields ();


    public abstract String[] defaultFields ();

}
