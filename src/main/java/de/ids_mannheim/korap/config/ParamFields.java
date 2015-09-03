package de.ids_mannheim.korap.config;

import lombok.Getter;

import java.util.Collection;
import java.util.HashMap;

/**
 * @author hanl
 * @date 21/07/2015
 */
// could also be an array or list!
public class ParamFields extends HashMap<String, ParamFields.Param> {

    public void add(Param param) {
        this.put(Param.class.getName(), param);
    }

    public <T extends Param> T get(Class<T> cl) {
        return (T) this.get(cl.getName());
    }

    public <T extends Param> T remove(Class<T> cl) {
        return (T) this.remove(cl.getName());
    }

    public void addAll(Collection<Param> params) {
        for (Param p : params)
            super.put(p.getClass().getName(), p);
    }

    @Getter
    public abstract static class Param {

        public boolean hasValues(){
            return false;
        }

    }

}
