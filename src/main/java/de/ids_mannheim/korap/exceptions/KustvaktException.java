package de.ids_mannheim.korap.exceptions;

import de.ids_mannheim.korap.auditing.AuditRecord;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * @author hanl
 * @date 11/12/2013
 */
//fixme: redundant with baseexception
@Deprecated
@Setter
@Getter
public class KustvaktException extends Exception {

    protected List<AuditRecord> records = new ArrayList<>();
    private String userid;
    private Integer statusCode;
    private String entity;

    public KustvaktException(Integer status) {
        this.statusCode = status;
    }

    public KustvaktException(Object userid, Integer status) {
        this(status);
        this.userid = String.valueOf(userid);
    }

    public KustvaktException(Object userid, Integer status, String message,
            String entity) {
        super(status, message, entity);
        this.userid = String.valueOf(userid);
    }

    public KustvaktException(Integer status, String message, String entity) {
        super(status, message, entity);
    }

    public KustvaktException(Throwable cause, Integer status) {
        super(cause, status);
    }

    public KustvaktException(String message, Throwable cause, Integer status) {
        super(message, cause, status);
    }

    @Override
    public String toString() {
        return "Excpt{" +
                "status=" + getStatusCode() +
                ", message=" + getMessage() +
                ", args=" + getEntity() +
                ", userid=" + userid +
                '}';
    }
}
