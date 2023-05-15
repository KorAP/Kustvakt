package de.ids_mannheim.korap.web;

import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import de.ids_mannheim.korap.auditing.AuditRecord;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.interfaces.db.AuditingIface;
import de.ids_mannheim.korap.response.Notifications;

/**
 * @author hanl, margaretha
 * @date 29/01/2014
 * @last 04/12/2017
 */
public class CoreResponseHandler {

    private AuditingIface auditing;

    public CoreResponseHandler (AuditingIface iface) {
        this.auditing = iface;
    }

    private void register (List<AuditRecord> records) {
        if (auditing != null && !records.isEmpty())
            auditing.audit(records);
        else if (auditing == null)
            throw new RuntimeException("Auditing handler must be set!");
    }


    public WebApplicationException throwit (KustvaktException e) {
        Response s;
        if (e.hasNotification()) {
            if (e.getStatusCode() != null) {
                s = Response.status(getStatus(e.getStatusCode()))
                        .entity(e.getNotification()).build();
            }
            // KustvaktException just wraps another exception 
            else {
                s=Response.status(Response.Status.BAD_REQUEST)
                        .entity(e.getNotification()).build();
            }
        }
        else {
            s = Response.status(getStatus(e.getStatusCode()))
                    .entity(buildNotification(e)).build();
        }
        return new WebApplicationException(s);
    }

    public WebApplicationException throwit (int code) {
        return new WebApplicationException(Response.status(getStatus(code))
                .entity(buildNotification(code, "", "")).build());
    }


    public WebApplicationException throwit (int code, String message,
            String entity) {
        return new WebApplicationException(Response.status(getStatus(code))
                .entity(buildNotification(code, message, entity)).build());
    }

    public WebApplicationException throwit (int code, String notification) {
        return new WebApplicationException(
                Response.status(getStatus(code)).entity(notification).build());
    }

    protected String buildNotification (KustvaktException e) {
        register(e.getRecords());
        return buildNotification(e.getStatusCode(), e.getMessage(),
                e.getEntity());
    }

    public static String buildNotification (int code, String message,
            String entity) {
        Notifications notif = new Notifications();
        notif.addError(code, message, entity);
        return notif.toJsonString() + "\n";
    }

    protected Response.Status getStatus (int code) {
        Response.Status status = Response.Status.BAD_REQUEST;
        switch (code) {
            // case StatusCodes.NO_VALUE_FOUND:
            // status = Response.Status.NO_CONTENT;
            // break;
            case StatusCodes.ILLEGAL_ARGUMENT:
                status = Response.Status.NOT_ACCEPTABLE;
                break;
            case StatusCodes.STATUS_OK:
                status = Response.Status.OK;
                break;
            // EM: Added 
            case StatusCodes.NO_RESOURCE_FOUND:
                status = Response.Status.NOT_FOUND;
                break;
            case StatusCodes.CACHING_VC:
                status = Response.Status.SERVICE_UNAVAILABLE;
                break;    
        }
        return status;
    }
}
