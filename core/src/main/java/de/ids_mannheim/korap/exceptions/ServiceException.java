package de.ids_mannheim.korap.exceptions;

import de.ids_mannheim.korap.auditing.AuditRecord;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author hanl
 * @date 08/04/2015
 */
// should be a http exception that responds to a service point
// is the extension of the notauthorized exception!
@Setter(AccessLevel.PROTECTED)
@Getter(AccessLevel.PROTECTED)
public class ServiceException extends Exception {

    protected List<AuditRecord> records = new ArrayList<>();
    private static final Logger jlog = LoggerFactory
            .getLogger(ServiceException.class);

    private int status;
    private String entity;
    private Object userid;


    protected ServiceException (Object userid, Integer status, String message,
                                String args) {
        super(message);
        this.userid = userid;
        this.status = status;
        this.entity = args;
        AuditRecord record = AuditRecord.serviceRecord(userid, status, args);
        this.records.add(record);
    }


    @Deprecated
    public ServiceException (Object userid, Integer status, String ... args) {
        this(userid, status, StatusCodes.getMessage(status), Arrays
                .asList(args).toString());
    }


    public ServiceException (Integer status, KustvaktException ex) {
        this(ex.getUserid(), ex.getStatusCode(), ex.getMessage(), ex
                .getEntity());
        AuditRecord record = AuditRecord.serviceRecord(ex.getUserid(), status,
                ex.getEntity());
        record.setField_1(ex.toString());
        this.records.add(record);
        jlog.error("Exception: " + ex.toString());
    }

}
