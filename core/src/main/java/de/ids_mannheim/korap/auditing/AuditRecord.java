package de.ids_mannheim.korap.auditing;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;

import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.utils.JsonUtils;
import de.ids_mannheim.korap.utils.TimeUtils;
import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;
import java.util.Date;

/**
 * @author hanl
 *         <p/>
 *         Record holder for auditing requests. Holds the data until
 *         it can be persisted to a database
 */
@Getter
@Setter
public class AuditRecord {

    // todo: handle via status codes
    @Deprecated
    public enum Operation {
        GET, INSERT, UPDATE, DELETE, CREATE
    }

    public enum CATEGORY {
        SECURITY, DATABASE, RESOURCE, QUERY, SERVICE
    }

    @JsonIgnore
    private Integer id;
    //security access describes changes in user authorities and access control permissions of resources
    private String userid;
    private String target;

    //fixme: replace with more specific error codes
    private CATEGORY category;
    private String loc;
    private Long timestamp;
    private Integer status = -1;
    private String args;
    private String field_1 = "None";


    private AuditRecord () {
        this.timestamp = TimeUtils.getNow().getMillis();
    }


    public AuditRecord (CATEGORY category) {
        this();
        this.category = category;
    }


    public AuditRecord (CATEGORY cat, Object userID, Integer status) {
        this(cat);
        this.status = status;
        if (userID != null) {
            //todo: client info!
            //            this.loc = clientInfoToString(user.getTokenContext().getHostAddress(),
            //                    user.getTokenContext().getUserAgent());
            this.loc = clientInfoToString("null", "null");
            userid = String.valueOf(userID);
        }
        else {
            this.loc = clientInfoToString("null", "null");
            userid = "-1";
        }
    }


    public static AuditRecord serviceRecord (Object user, Integer status,
            String ... args) {
        AuditRecord r = new AuditRecord(CATEGORY.SERVICE);
        r.setArgs(Arrays.asList(args).toString());
        r.setUserid(String.valueOf(user));
        r.setStatus(status);
        return r;
    }


    public static AuditRecord dbRecord (Object user, Integer status,
            String ... args) {
        AuditRecord r = new AuditRecord(CATEGORY.DATABASE);
        r.setArgs(Arrays.asList(args).toString());
        r.setUserid(String.valueOf(user));
        r.setStatus(status);
        return r;
    }


    public AuditRecord fromJson (String json) throws KustvaktException {
        JsonNode n = JsonUtils.readTree(json);
        AuditRecord r = new AuditRecord();
        r.setCategory(CATEGORY.valueOf(n.path("category").asText()));
        r.setTarget(n.path("target").asText());
        r.setField_1(n.path("field_1").asText());
        r.setUserid(n.path("account").asText());
        r.setStatus(n.path("status").asInt());
        r.setLoc(n.path("loc").asText());
        return r;
    }


    private String clientInfoToString (String IP, String userAgent) {
        return userAgent + "@" + IP;
    }


    // fixme: add id, useragent
    @Override
    public String toString () {
        StringBuilder b = new StringBuilder();
        b.append(category.toString().toLowerCase() + " audit : ")
                .append(userid + "@" + new Date(timestamp)).append("\n")
                .append("Status " + status).append("; ");

        if (this.args != null)
            b.append("Args " + field_1).append("; ");
        if (this.loc != null)
            b.append("Location " + loc).append("; ");
        return b.toString();
    }


    @Override
    public boolean equals (Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        AuditRecord that = (AuditRecord) o;

        if (userid != null ? !userid.equals(that.userid) : that.userid != null)
            return false;
        if (category != that.category)
            return false;
        if (status != null ? !status.equals(that.status) : that.status != null)
            return false;
        if (field_1 != null ? !field_1.equals(that.field_1)
                : that.field_1 != null)
            return false;
        if (loc != null ? !loc.equals(that.loc) : that.loc != null)
            return false;
        if (target != null ? !target.equals(that.target) : that.target != null)
            return false;
        if (timestamp != null ? !timestamp.equals(that.timestamp)
                : that.timestamp != null)
            return false;

        return true;
    }


    @Override
    public int hashCode () {
        int result = userid != null ? userid.hashCode() : 0;
        result = 31 * result + (target != null ? target.hashCode() : 0);
        result = 31 * result + category.hashCode();
        result = 31 * result + (loc != null ? loc.hashCode() : 0);
        result = 31 * result + (timestamp != null ? timestamp.hashCode() : 0);
        result = 31 * result + (status != null ? status.hashCode() : 0);
        result = 31 * result + (field_1 != null ? field_1.hashCode() : 0);
        return result;
    }
}
