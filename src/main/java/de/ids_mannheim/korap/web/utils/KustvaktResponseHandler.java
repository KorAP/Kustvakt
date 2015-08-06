package de.ids_mannheim.korap.web.utils;

import de.ids_mannheim.korap.auditing.AuditRecord;
import de.ids_mannheim.korap.config.BeanConfiguration;
import de.ids_mannheim.korap.exceptions.BaseException;
import de.ids_mannheim.korap.exceptions.KorAPException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.interfaces.AuditingIface;
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

    private static AuditingIface auditing = BeanConfiguration.getBeans()
            .getAuditingProvider();

    private static void register(List<AuditRecord> records) {
        if (auditing != null && !records.isEmpty())
            auditing.audit(records);
    }

    public static WebApplicationException throwit(BaseException e) {
        //fixme: ??!
        e.printStackTrace();
        return new WebApplicationException(
                Response.status(Response.Status.BAD_REQUEST)
                        .entity(buildNotification(e)).build());
    }

    @Deprecated
    public static WebApplicationException throwit(int code) {
        KorAPException e = new KorAPException(code);
        return new WebApplicationException(
                Response.status(Response.Status.OK).entity(buildNotification(e))
                        .build());
    }

    @Deprecated
    public static WebApplicationException throwit(int code, String message,
            String entity) {
        KorAPException e = new KorAPException(code, message, entity);
        return new WebApplicationException(
                Response.status(Response.Status.OK).entity(buildNotification(e))
                        .build());
    }

    private static String buildNotification(BaseException e) {
        KustvaktResponseHandler.register(e.getRecords());
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
        KorAPException e = new KorAPException(StatusCodes.BAD_CREDENTIALS);
        return new WebApplicationException(
                Response.status(Response.Status.UNAUTHORIZED)
                        .header(HttpHeaders.WWW_AUTHENTICATE,
                                "Basic realm=Kustvakt Authentication Service")
                        .entity(buildNotification(e)).build());
    }

}
