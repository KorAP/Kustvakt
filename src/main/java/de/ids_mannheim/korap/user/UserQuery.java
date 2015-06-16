package de.ids_mannheim.korap.user;

import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: hanl
 * Date: 9/16/13
 * Time: 4:38 PM
 */
@Data
public class UserQuery {

    private Integer id;
    private String queryLanguage;
    private String query;
    private String name;
    private String description;
    private Integer creator;

    public UserQuery(Integer id, int creator) {
        setId(id);
        setCreator(creator);
        setName("");
        setDescription("");
        setQuery("");
        setQueryLanguage("");
    }

    public UserQuery(String ql, String query, String description) {
        setDescription(description);
        setQuery(query);
        setQueryLanguage(ql);
    }

    public UserQuery() {
        setDescription("");
        setQuery("");
        setQueryLanguage("");
        setName("");
    }

    public void setQuery(String query) {
        this.query = query;
        setName("Query: " + query
                .substring(0, query.length() > 20 ? 20 : query.length()));
    }

    // todo: use example queries or store in database
    public static List<UserQuery> demoUserQueries() {

        List<UserQuery> queries = new ArrayList<>();
        UserQuery q1 = new UserQuery();
        q1.setQueryLanguage("COSMAS2");
        q1.setQuery("$wegen #IN(L) <s>");
        q1.setDescription(
                "Findet 'wegen' an Satzanfängen. Berücksichtigt auch Groß- und Kleinschreibung");

        //todo: change query
        UserQuery q2 = new UserQuery();
        q2.setQueryLanguage("COSMAS2");
        q2.setQuery("base/cons:Buchstabe base/aggr:Buchstabe");

        UserQuery q3 = new UserQuery();
        q3.setQueryLanguage("COSMAS2");
        q3.setDescription("Regular Expression Search");
        q3.setQuery("id:/WPD_AAA.*/ AND textClass:sport");

        UserQuery q4 = new UserQuery();
        q4.setQueryLanguage("COSMAS2");
        q4.setQuery("mpt/syntax_pos:@CC\\|und");

        UserQuery q5 = new UserQuery();
        q5.setQueryLanguage("COSMAS2");
        q5.setQuery("VVINF\\|.*en");

        queries.add(q1);
        //        queries.add(q2);
        //        queries.add(q3);
        queries.add(q4);
        queries.add(q5);
        return queries;
    }

    //id is irrevelant, since data was coming
    // from frontend and thus this object does not contain a id that could be compared!
    // same with the userAccount. Not set yet!
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof UserQuery))
            return false;
        UserQuery userQuery = (UserQuery) o;
        if (!query.equals(userQuery.query))
            return false;
        if (!queryLanguage.equals(userQuery.queryLanguage))
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        int result = getId() != null ? getId().hashCode() : 0;
        result = 31 * result + (queryLanguage != null ?
                queryLanguage.hashCode() :
                0);
        result = 31 * result + (query != null ? query.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("UserQuery{");
        sb.append("id=").append(getId());
        //        sb.append(", owner=").append(getOwner());
        sb.append(", queryLanguage='").append(queryLanguage).append('\'');
        sb.append(", query='").append(query).append('\'');
        sb.append(", description='").append(getDescription()).append('\'');
        sb.append('}');
        return sb.toString();
    }

    public Map toMap() {
        Map map = new HashMap();
        map.put("name", this.name);
        map.put("description", this.description);
        map.put("query", this.query);
        map.put("queryLanguage", this.queryLanguage);
        return map;
    }
}
