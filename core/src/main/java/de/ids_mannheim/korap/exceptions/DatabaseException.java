package de.ids_mannheim.korap.exceptions;

import de.ids_mannheim.korap.auditing.AuditRecord;

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
        AuditRecord record = new AuditRecord(AuditRecord.CATEGORY.DATABASE);
        record.setUserid(String.valueOf(userid));
        record.setStatus(status);
        record.setTarget(target);
        record.setArgs(this.getEntity());
        this.records.add(record);
    }


    public DatabaseException (KustvaktException e, Integer status, String ... args) {
        this(e.getUserid(), e.getStatusCode(), e.getMessage(), e.getEntity(), e);
        AuditRecord record = AuditRecord.dbRecord(e.getUserid(), status, args);
        record.setField_1(e.string());
        this.records.addAll(e.getRecords());
        this.records.add(record);
    }


    @Override
    public String string () {
        return "DBExcpt{" + "status=" + getStatusCode() + ", message="
                + getMessage() + ", args=" + getEntity() + ", userid="
                + this.getUserid() + '}';
    }
}
