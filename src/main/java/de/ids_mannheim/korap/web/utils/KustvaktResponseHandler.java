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
        return new WebApplicationException(
                Response.status(Response.Status.BAD_REQUEST)
                        .entity(buildNotification(e)).build());
    }

    public static WebApplicationException throwit(int code) {
        return new WebApplicationException(Response.status(Response.Status.OK)
                .entity(buildNotification(code, "", "")).build());
    }

    public static WebApplicationException throwit(int code, String message,
            String entity) {
        return new WebApplicationException(Response.status(Response.Status.OK)
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
        KustvaktException e = new KustvaktException(
                StatusCodes.BAD_CREDENTIALS);
        return new WebApplicationException(
                Response.status(Response.Status.UNAUTHORIZED)
                        .header(HttpHeaders.WWW_AUTHENTICATE,
                                "Basic realm=Kustvakt Authentication Service")
                        .entity(buildNotification(e)).build());
    }
}
