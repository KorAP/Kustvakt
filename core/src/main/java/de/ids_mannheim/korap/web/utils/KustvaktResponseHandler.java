package de.ids_mannheim.korap.web.utils;

import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import de.ids_mannheim.korap.auditing.AuditRecord;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.interfaces.db.AuditingIface;
import de.ids_mannheim.korap.response.Notifications;

/**
 * @author hanl, margaretha
 * @date 29/01/2014
 * @last 08/11/2017
 */
public class KustvaktResponseHandler {

    private AuditingIface auditing;

    public KustvaktResponseHandler (AuditingIface iface) {
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
        if (e.hasNotification()){
            s = Response.status(getStatus(e.getStatusCode()))
                    .entity(e.getNotification()).build();
        }
        else{
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
        return new WebApplicationException(Response.status(getStatus(code))
                .entity(notification).build());
    }
    
    private String buildNotification (KustvaktException e) {
        register(e.getRecords());
        return buildNotification(e.getStatusCode(), e.getMessage(),
                e.getEntity());
    }


    public String buildNotification (int code, String message,
            String entity) {
        Notifications notif = new Notifications();
        notif.addError(code, message, entity);
        return notif.toJsonString() + "\n";
    }


    //todo:  if exception, make exception message and error code available if not masked!
    public WebApplicationException throwAuthenticationException (String username) {
        return new WebApplicationException(Response
                .status(Response.Status.UNAUTHORIZED)
                .header(HttpHeaders.WWW_AUTHENTICATE,
                        "Basic realm=Kustvakt Authentication Service")
                .entity(buildNotification(StatusCodes.BAD_CREDENTIALS,
                        "Unauthorized access", username)).build());
    }



    private Response.Status getStatus (int code) {
        Response.Status status = Response.Status.BAD_REQUEST;
        switch (code) {
//            case StatusCodes.NO_VALUE_FOUND:
//                status = Response.Status.NO_CONTENT;
//                break;
            case StatusCodes.ILLEGAL_ARGUMENT:
                status = Response.Status.NOT_ACCEPTABLE;
                break;
            case StatusCodes.STATUS_OK:
                status = Response.Status.OK;
                break;
        }
        return status;
    }
}
