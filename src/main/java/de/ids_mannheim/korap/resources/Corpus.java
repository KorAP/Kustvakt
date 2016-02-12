package de.ids_mannheim.korap.resources;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * User: hanl
 * Date: 11/8/13
 * Time: 5:15 PM
 */
@Getter
@Setter
public class Corpus extends KustvaktResource {

    // todo: can be deprecated since resource offers data field here!
    @Deprecated
    private Map stats;

    public Corpus() {
        super();
    }

    public Corpus(String pers) {
        super(pers);
    }

    public Corpus(Integer id) {
        super(id);
    }

    @Override
    public Map toMap() {
        Map res = super.toMap();
        if (stats != null && !stats.isEmpty())
            res.put("statistics", stats);
        return res;
    }
}
