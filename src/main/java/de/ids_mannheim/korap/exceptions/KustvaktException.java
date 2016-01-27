package de.ids_mannheim.korap.exceptions;

import de.ids_mannheim.korap.auditing.AuditRecord;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author hanl
 * @date 11/12/2013
 */
@Setter
@Getter
public class KustvaktException extends Exception {

    protected List<AuditRecord> records = new ArrayList<>();
    private String userid;
    private Integer statusCode;
    private String entity;

    public KustvaktException(int status) {
        this.statusCode = status;
    }

    public KustvaktException(int status, String... args) {
        this.statusCode = status;
        this.entity = Arrays.asList(args).toString();
    }

    public KustvaktException(Object userid, int status) {
        this(status);
        this.userid = String.valueOf(userid);
    }

    public KustvaktException(Object userid, int status, String message,
            String entity) {
        super(message);
        this.statusCode = status;
        this.entity = entity;
        this.userid = String.valueOf(userid);
    }

    public KustvaktException(int status, String message, String entity) {
        super(message);
        this.statusCode = status;
        this.entity = entity;
    }

    public KustvaktException(Throwable cause, int status) {
        super(cause);
        this.statusCode = status;
    }

    public KustvaktException(String message, Throwable cause, int status) {
        super(message, cause);
        this.statusCode = status;
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
