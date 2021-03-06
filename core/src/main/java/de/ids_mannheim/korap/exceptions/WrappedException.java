package de.ids_mannheim.korap.exceptions;

import de.ids_mannheim.korap.auditing.AuditRecord;

import java.util.Arrays;

/**
 * @author hanl
 * @date 08/04/2015
 */
// should be a http exception that responds to a service point
// is the extension of the notauthorized exception!
public class WrappedException extends KustvaktException {

    private WrappedException (Object userid, Integer status, String message,
                              String args, Exception rootCause) {
        super(String.valueOf(userid), status, message, args, rootCause);
    }


    public WrappedException (Object userid, Integer status, String ... args) {
        this(userid, status, "", Arrays.asList(args).toString(), null);
        AuditRecord record = AuditRecord.serviceRecord(userid, status, args);
        this.records.add(record);
    }


    public WrappedException (KustvaktException e, Integer status,
                             String ... args) {
        this(e.getUserid(), e.getStatusCode(), e.getMessage(), e.getEntity(), e);
        AuditRecord record = AuditRecord.serviceRecord(e.getUserid(), status,
                args);
        record.setField_1(e.string());
        this.records.addAll(e.getRecords());
        this.records.add(record);
    }

}
