package de.ids_mannheim.korap.user;

import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.interfaces.ValidatorIface;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.util.*;

/**
 * @author hanl
 * @date 22/01/2016
 */
public abstract class Userdata {

    @Getter
    @Setter
    private Integer id;
    @Getter(AccessLevel.PRIVATE)
    @Setter(AccessLevel.PRIVATE)
    private Object data;
    @Getter
    @Setter
    private Integer userId;


    public Userdata () {
        this(-1);
    }

    public Userdata(Integer userid) {
        this.userId = userid;
        this.id = -1;
        this.data = DataFactory.getFactory().convertData(null);
    }


    public int size () {
        return DataFactory.getFactory().size(this.data);
    }


    public Object get (String key) {
        return DataFactory.getFactory().getValue(this.data, key);
    }


    /**
     * 
     * @return
     */
    public boolean isValid () {
        return missing().length == 0;
    }


    public String[] missing () {
        Set<String> missing = new HashSet<>();
        Set<String> keys = DataFactory.getFactory().keys(this.data);
        for (String key : requiredFields()) {
            if (!keys.contains(key))
                missing.add(key);
        }
        return missing.toArray(new String[0]);
    }


    public void checkRequired () throws KustvaktException {
        String[] fields = missing();
        if (missing().length != 0) {
            throw new KustvaktException(userId, StatusCodes.MISSING_ARGUMENTS,
                    "User data object not valid. Object has missing fields!",
                    Arrays.asList(fields).toString());
        }
    }


    //fixme: if data array, return empty?!
    public Set<String> keys () {
        return DataFactory.getFactory().keys(this.data);
    }


    public Collection<Object> values () {
        return DataFactory.getFactory().values(this.data);
    }


    public void setData (String data) {
        this.data = DataFactory.getFactory().convertData(data);
    }


    public void update (Userdata other) {
        if (other != null && this.getClass().equals(other.getClass()))
            this.data = DataFactory.getFactory().merge(this.data, other.data);
    }


    public String serialize () throws KustvaktException {
        // to have consistency with required fields --> updates/deletion may cause required fields to be missing.
        this.checkRequired();
        return DataFactory.getFactory().toStringValue(this.data);
    }


    public void setField (String key, Object value) {
        DataFactory.getFactory().addValue(this.data, key, value);
    }

    // todo: test
    public void validate (ValidatorIface validator) throws KustvaktException {
        DataFactory.getFactory().validate(this.data, validator);
    }


    public void read (Map<String, Object> map, boolean defaults_only)
            throws KustvaktException {
        this.readQuietly(map, defaults_only);
        this.checkRequired();
    }


    public void readQuietly (Map<String, Object> map, boolean defaults_only) {
        if (defaults_only) {
            for (String k : defaultFields()) {
                Object o = map.get(k);
                if (o != null) {
                    DataFactory.getFactory().addValue(this.data, k, o);
                }
            }
        }
        else {
            for (String key : map.keySet())
                DataFactory.getFactory().addValue(this.data, key, map.get(key));
        }
    }


    public abstract String[] requiredFields ();


    public abstract String[] defaultFields ();

}
