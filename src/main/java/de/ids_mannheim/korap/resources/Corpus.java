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

    private Map stats;

    public Corpus() {
        super();
    }

    public Corpus(Integer id, long created, int creator) {
        super(id, creator, created);
    }

    public Corpus(Integer id, int creator) {
        super(id, creator);
    }

    public Corpus(String pers_id, int creator) {
        super(pers_id, creator);
        // deprecated
        this.setName(pers_id);
    }

    @Override
    public Map toMap() {
        Map res = super.toMap();
        res.put("statistics", stats);
        return res;
    }
}
