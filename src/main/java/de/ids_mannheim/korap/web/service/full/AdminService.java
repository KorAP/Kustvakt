package de.ids_mannheim.korap.web.service.full;

import com.sun.jersey.spi.container.ResourceFilters;
import de.ids_mannheim.korap.auditing.AuditRecord;
import de.ids_mannheim.korap.config.BeansFactory;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.handlers.DocumentDao;
import de.ids_mannheim.korap.interfaces.db.AuditingIface;
import de.ids_mannheim.korap.interfaces.AuthenticationManagerIface;
import de.ids_mannheim.korap.resources.Document;
import de.ids_mannheim.korap.resources.KustvaktResource;
import de.ids_mannheim.korap.resources.Permissions;
import de.ids_mannheim.korap.resources.ResourceFactory;
import de.ids_mannheim.korap.security.PolicyCondition;
import de.ids_mannheim.korap.security.ac.PolicyBuilder;
import de.ids_mannheim.korap.user.User;
import de.ids_mannheim.korap.utils.JsonUtils;
import de.ids_mannheim.korap.utils.KustvaktLogger;
import de.ids_mannheim.korap.utils.TimeUtils;
import de.ids_mannheim.korap.web.KustvaktServer;
import de.ids_mannheim.korap.web.filter.AdminFilter;
import de.ids_mannheim.korap.web.filter.PiwikFilter;
import de.ids_mannheim.korap.web.utils.KustvaktResponseHandler;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Locale;

/**
 * Created by hanl on 6/11/14.
 */
@Path(KustvaktServer.API_VERSION + "/admin")
@ResourceFilters({ AdminFilter.class, PiwikFilter.class })
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class AdminService {

    private static Logger jlog = LoggerFactory.getLogger(AdminService.class);

    private AuthenticationManagerIface controller;
    private AuditingIface auditingController;
    private DocumentDao documentDao;


    public AdminService () {
        this.auditingController = BeansFactory.getKustvaktContext()
                .getAuditingProvider();
        this.controller = BeansFactory.getKustvaktContext()
                .getAuthenticationManager();
        this.documentDao = new DocumentDao(BeansFactory.getKustvaktContext()
                .getPersistenceClient());
    }


    @GET
    @Path("audit/{type}")
    public Response getAudits (@PathParam("type") String type,
            @QueryParam("from") String from, @QueryParam("until") String until,
            @QueryParam("day") Boolean day, @QueryParam("limit") String limit,
            @Context Locale locale) {
        DateTime from_date, until_date;

        if (from == null)
            from_date = TimeUtils.getNow();
        else
            from_date = TimeUtils.getTime(from);
        if (until == null)
            until_date = TimeUtils.getNow();
        else
            until_date = TimeUtils.getTime(until);

        int integer_limit;
        boolean dayOnly = Boolean.valueOf(day);
        try {
            integer_limit = Integer.valueOf(limit);
        }
        catch (NumberFormatException | NullPointerException e) {
            throw KustvaktResponseHandler.throwit(StatusCodes.ILLEGAL_ARGUMENT);
        }
        String result = JsonUtils.toJSON(auditingController.retrieveRecords(
                AuditRecord.CATEGORY.valueOf(type.toUpperCase()), from_date,
                until_date, dayOnly, integer_limit));
        // limit number of records to return
        return Response.ok(result).build();
    }


    @POST
    @Path("createPolicies/{id}")
    public Response addResourcePolicy (@PathParam("id") String persistentid,
            @QueryParam("type") String type, @QueryParam("name") String name,
            @QueryParam("description") String description,
            @QueryParam("group") String group,
            @QueryParam("perm") List<String> permissions,
            @QueryParam("loc") String loc, @QueryParam("expire") String duration) {

        try {
            KustvaktResource resource = ResourceFactory.getResource(type);
            resource.setPersistentID(persistentid);
            resource.setDescription(description);
            resource.setName(name);

            Permissions.Permission[] p = Permissions.read(permissions
                    .toArray(new String[0]));

            PolicyBuilder cr = new PolicyBuilder(User.UserFactory.getAdmin())
                    .setConditions(new PolicyCondition(group)).setResources(
                            resource);
            if (loc != null && !loc.isEmpty())
                cr.setLocation(loc);

            if (duration != null && duration.isEmpty())
                cr.setContext(TimeUtils.getNow().getMillis(),
                        TimeUtils.convertTimeToSeconds(duration));

            cr.setPermissions(p).create();
        }
        catch (KustvaktException e) {
            throw KustvaktResponseHandler.throwit(e);
        }

        return Response.ok().build();
    }


    @POST
    @Path("doc/{id}/add")
    @Deprecated
    public Response addDocument (@PathParam("id") String id) {
        Document document = new Document(id);
        try {
            this.documentDao.storeResource(document, null);
        }
        catch (KustvaktException e) {
            throw KustvaktResponseHandler.throwit(e);
        }
        return Response.ok().build();
    }

}
