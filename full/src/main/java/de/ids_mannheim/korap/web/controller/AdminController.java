package de.ids_mannheim.korap.web.controller;

import java.util.Locale;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.sun.jersey.spi.container.ResourceFilters;

import de.ids_mannheim.korap.auditing.AuditRecord;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.interfaces.db.AuditingIface;
import de.ids_mannheim.korap.server.KustvaktServer;
import de.ids_mannheim.korap.utils.JsonUtils;
import de.ids_mannheim.korap.utils.TimeUtils;
import de.ids_mannheim.korap.web.KustvaktResponseHandler;
import de.ids_mannheim.korap.web.filter.AdminFilter;
import de.ids_mannheim.korap.web.filter.PiwikFilter;

/**
 * @author hanl, margaretha 
 * Created date 6/11/14. 
 * Last update: 08/11/2017
 * Last changes:
 *  removed DocumentDao (EM)
 */
@Deprecated
@Controller
@Path(KustvaktServer.API_VERSION + "/admin")
@ResourceFilters({ AdminFilter.class, PiwikFilter.class })
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class AdminController {

    private static Logger jlog = LoggerFactory.getLogger(AdminController.class);
    @Autowired
    private AuditingIface auditingController;

    @Autowired
    private KustvaktResponseHandler kustvaktResponseHandler;

    // EM: not documented and tested, not sure what the purpose of the service is
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
            throw kustvaktResponseHandler.throwit(StatusCodes.ILLEGAL_ARGUMENT);
        }
        String result="";
        try {
            result = JsonUtils.toJSON(auditingController.retrieveRecords(
                    AuditRecord.CATEGORY.valueOf(type.toUpperCase()), from_date,
                    until_date, dayOnly, integer_limit));
        }
        catch (KustvaktException e) {
            throw kustvaktResponseHandler.throwit(e);
        }
        // limit number of records to return
        return Response.ok(result).build();
    }


//    @Deprecated
//    @POST
//    @Path("createPolicies/{id}")
//    public Response addResourcePolicy (@PathParam("id") String persistentid,
//            @QueryParam("type") String type, @QueryParam("name") String name,
//            @QueryParam("description") String description,
//            @QueryParam("group") String group,
//            @QueryParam("perm") List<String> permissions,
//            @QueryParam("loc") String loc,
//            @QueryParam("expire") String duration, @Context HttpContext context)
//            throws KustvaktException {
//
//        if (type == null | type.isEmpty()) {
//            KustvaktException e = new KustvaktException(
//                    StatusCodes.MISSING_ARGUMENT,
//                    "The value of parameter type is missing.");
//            throw kustvaktResponseHandler.throwit(e);
//        }
//        else if (name == null | name.isEmpty()) {
//            KustvaktException e = new KustvaktException(
//                    StatusCodes.MISSING_ARGUMENT,
//                    "The value of parameter name is missing.");
//            throw kustvaktResponseHandler.throwit(e);
//        }
//        else if (description == null | description.isEmpty()) {
//            KustvaktException e = new KustvaktException(
//                    StatusCodes.MISSING_ARGUMENT,
//                    "The value of parameter description is missing.");
//            throw kustvaktResponseHandler.throwit(e);
//        }
//        else if (group == null | group.isEmpty()) {
//            KustvaktException e = new KustvaktException(
//                    StatusCodes.MISSING_ARGUMENT,
//                    "The value of parameter group is missing.");
//            throw kustvaktResponseHandler.throwit(e);
//        }
//        else if (permissions == null | permissions.isEmpty()) {
//            KustvaktException e = new KustvaktException(
//                    StatusCodes.MISSING_ARGUMENT,
//                    "The value of parameter permissions is missing.");
//            throw kustvaktResponseHandler.throwit(e);
//        }
//
//
//        try {
//            KustvaktResource resource = ResourceFactory.getResource(type);
//            resource.setPersistentID(persistentid);
//            resource.setDescription(description);
//            resource.setName(name);
//
//            Permissions.Permission[] p = Permissions
//                    .read(permissions.toArray(new String[0]));
//
//            User user = (User) context.getProperties().get("user");
//
//            PolicyBuilder pb = new PolicyBuilder(user)
//                    .setConditions(new PolicyCondition(group))
//                    .setResources(resource);
//
//            if (loc != null && !loc.isEmpty()){
//                pb.setLocation(loc);
//            }
//            if (duration != null && !duration.isEmpty()){
//                long now = TimeUtils.getNow().getMillis();
//                pb.setContext(now,
//                        now + TimeUtils.convertTimeToSeconds(duration));
//            }
//            pb.setPermissions(p);
//            pb.create();
//        }
//        catch (KustvaktException e) {
//            throw kustvaktResponseHandler.throwit(e);
//        }
//
//        return Response.ok().build();
//    }

}
