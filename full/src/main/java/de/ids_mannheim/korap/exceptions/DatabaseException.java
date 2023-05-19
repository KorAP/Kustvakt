package de.ids_mannheim.korap.exceptions;

import java.util.Arrays;

/**
 * @author hanl
 * @date 08/04/2015
 */
public class DatabaseException extends KustvaktException {

    private DatabaseException (Object userid, Integer status, String message,
                         String args, Exception e) {
        super(String.valueOf(userid), status, message, args, e);
    }

    public DatabaseException (Object userid, String target, Integer status, String message,
            String ... args) {
        this(null, userid, target, status, message);
    }

    public DatabaseException (Exception e, Object userid, String target, Integer status, String message,
                        String ... args) {
        this(userid, status, message, Arrays.asList(args).toString(), e);
    }


    public DatabaseException (KustvaktException e, Integer status, String ... args) {
        this(e.getUserid(), e.getStatusCode(), e.getMessage(), e.getEntity(), e);
    }


    @Override
    public String string () {
        return "DBExcpt{" + "status=" + getStatusCode() + ", message="
                + getMessage() + ", args=" + getEntity() + ", userid="
                + this.getUserid() + '}';
    }
}
