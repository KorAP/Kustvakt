package de.ids_mannheim.korap.web.utils;

import de.ids_mannheim.korap.auditing.AuditRecord;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.interfaces.db.AuditingIface;
import de.ids_mannheim.korap.response.Notifications;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * @author hanl
 * @date 29/01/2014
 */
public class KustvaktResponseHandler {

    private static AuditingIface auditing;

    public static void init(AuditingIface iface) {
        if (auditing == null)
            auditing = iface;
    }

    private static void register(List<AuditRecord> records) {
        if (auditing != null && !records.isEmpty())
            auditing.audit(records);
        else if (auditing == null)
            throw new RuntimeException("Auditing handler must be set!");
    }

    public static WebApplicationException throwit(KustvaktException e) {
        Response s = Response.status(getStatus(e.getStatusCode()))
                .entity(buildNotification(e)).build();
        return new WebApplicationException(s);
    }

    public static WebApplicationException throwit(int code) {
        return new WebApplicationException(Response.status(getStatus(code))
                .entity(buildNotification(code, "", "")).build());
    }

    public static WebApplicationException throwit(int code, String message,
            String entity) {
        return new WebApplicationException(Response.status(getStatus(code))
                .entity(buildNotification(code, message, entity)).build());
    }

    private static String buildNotification(KustvaktException e) {
        register(e.getRecords());
        return buildNotification(e.getStatusCode(), e.getMessage(),
                e.getEntity());
    }

    private static String buildNotification(int code, String message,
            String entity) {
        Notifications notif = new Notifications();
        notif.addError(code, message, entity);
        return notif.toJsonString() + "\n";
    }

    public static WebApplicationException throwAuthenticationException() {
        return new WebApplicationException(
                Response.status(Response.Status.UNAUTHORIZED)
                        .header(HttpHeaders.WWW_AUTHENTICATE,
                                "Basic realm=Kustvakt Authentication Service")
                        .entity(buildNotification(StatusCodes.BAD_CREDENTIALS,
                                "Unauthorized access", "")).build());
    }

    private static Response.Status getStatus(int code) {
        Response.Status status = Response.Status.BAD_REQUEST;
        switch (code) {
            case StatusCodes.EMPTY_RESULTS:
                status = Response.Status.NO_CONTENT;
                break;
            case StatusCodes.ILLEGAL_ARGUMENT:
                status = Response.Status.NOT_ACCEPTABLE;
                break;
        }
        return status;
    }
}
