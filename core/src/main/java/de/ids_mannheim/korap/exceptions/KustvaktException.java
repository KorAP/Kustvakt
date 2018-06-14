package de.ids_mannheim.korap.exceptions;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.ids_mannheim.korap.auditing.AuditRecord;
//import de.ids_mannheim.korap.constant.TokenType;
import lombok.Getter;
import lombok.Setter;

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
    private String notification;
    private boolean isNotification;
//    private TokenType authType;
    private URI redirectUri;

    public KustvaktException (int status) {
        this.statusCode = status;
    }
    
    public KustvaktException (int status, String ... args) {
        super(args[0]);
        this.statusCode = status;
        String[] subarray = Arrays.copyOfRange(args, 1, args.length);
        this.entity = Arrays.asList(subarray).toString();
    }

    public KustvaktException (int status, String notification, boolean isNotification) {
        this.statusCode = status;
        this.notification = notification;
        this.isNotification = isNotification;
    }
    
    public boolean hasNotification () {
        return isNotification;
    }

    public KustvaktException (Object userid, int status) {
        this(status);
        this.userid = String.valueOf(userid);
    }

    // add throwable to parameters
    public KustvaktException (Object userid, int status, String message,
                              String entity) {
        this(userid, status, message, entity, null);
    }


    public KustvaktException (Object userid, int status, String message,
                              String entity, Exception e) {
        super(message, e);
        this.statusCode = status;
        this.entity = entity;
        this.userid = String.valueOf(userid);
    }


    public KustvaktException (Object userid, int status, String entity) {
        super(StatusCodes.getMessage(status));
        this.statusCode = status;
        this.entity = entity;
        this.userid = String.valueOf(userid);
    }

    public KustvaktException (int status, String message, String entity) {
        super(message);
        this.statusCode = status;
        this.entity = entity;
    }
    
    public KustvaktException (int status, String message, String entity, Throwable e) {
        super(message, e);
        this.statusCode = status;
        this.entity = entity;
    }
    
    public KustvaktException (int status, String message, Throwable e) {
        super(message, e);
        this.statusCode = status;
    }


    public KustvaktException (Throwable cause, int status) {
        super(cause);
        this.statusCode = status;
    }


    public KustvaktException (String message, Throwable cause, int status) {
        super(message, cause);
        this.statusCode = status;
    }



    public String string () {
        return "Excpt{" + "status=" + getStatusCode() + ", message="
                + getMessage() + ", args=" + getEntity() + ", userid=" + userid
                + '}';
    }
}
