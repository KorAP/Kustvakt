package de.ids_mannheim.korap.utils;

import de.ids_mannheim.korap.query.serialize.CollectionQueryProcessor;

import java.io.IOException;
import java.util.*;

/**
 * convenience builder class for collection query
 *
 * @author hanl
 * @date 16/09/2014
 */
public class CollectionQueryBuilder3 {

    private boolean verbose;
    private List<Map> rq;
    private StringBuilder builder;

    public CollectionQueryBuilder3() {
        this(false);
    }

    public CollectionQueryBuilder3(boolean verbose) {
        this.verbose = verbose;
        this.builder = new StringBuilder();
        this.rq = new LinkedList<>();
    }

    public CollectionQueryBuilder3 addSegment(String field, String value) {
        String f = field + "=" + value;
        this.builder.append(f);
        return this;
    }

    /**
     * element can be a more complex sub query like (textClass=freizeit & corpusID=WPD)
     *
     * @param query will be parenthised in order to make sub query element
     * @return
     */
    public CollectionQueryBuilder3 addSub(String query) {
        if (!query.startsWith("(") && !query.endsWith(")"))
            query = "(" + query + ")";
        this.builder.append(query);
        return this;
    }

    public CollectionQueryBuilder3 and() {
        this.builder.append(" & ");
        return this;
    }

    public CollectionQueryBuilder3 or() {
        this.builder.append(" | ");
        return this;
    }

    public CollectionQueryBuilder3 addRaw(String collection) {
        try {
            Map v = JsonUtils.read(collection, HashMap.class);
            v.get("collection");
        }catch (IOException e) {
            throw new IllegalArgumentException("Conversion went wrong!");
        }
        return this;
    }

    public Map getRequest() {
        //todo: adding another resource query doesnt work

        CollectionQueryProcessor tree = new CollectionQueryProcessor(
                this.verbose);
        tree.process(this.builder.toString());

        Map request = tree.getRequestMap();
        if (!this.rq.isEmpty()) {
            List coll = (List) request.get("collection");
            coll.addAll(this.rq);
        }
        return request;
    }

    public String toJSON() {
        return JsonUtils.toJSON(getRequest());
    }

}
