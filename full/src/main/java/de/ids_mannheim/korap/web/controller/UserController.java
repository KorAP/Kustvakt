package de.ids_mannheim.korap.web.controller;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ResourceFilters;

import de.ids_mannheim.korap.authentication.AuthenticationManager;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.Scopes;
import de.ids_mannheim.korap.config.URIParam;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.security.context.TokenContext;
import de.ids_mannheim.korap.user.KorAPUser;
import de.ids_mannheim.korap.user.User;
import de.ids_mannheim.korap.user.UserDetails;
import de.ids_mannheim.korap.user.UserQuery;
import de.ids_mannheim.korap.user.UserSettings;
import de.ids_mannheim.korap.user.Userdata;
import de.ids_mannheim.korap.utils.JsonUtils;
import de.ids_mannheim.korap.utils.StringUtils;
import de.ids_mannheim.korap.utils.TimeUtils;
import de.ids_mannheim.korap.web.KustvaktResponseHandler;
import de.ids_mannheim.korap.web.filter.APIVersionFilter;
import de.ids_mannheim.korap.web.filter.AuthenticationFilter;
import de.ids_mannheim.korap.web.filter.BlockingFilter;
import de.ids_mannheim.korap.web.filter.DemoUserFilter;
import de.ids_mannheim.korap.web.filter.PiwikFilter;

/** Some of the APIs are not applicable due to changes in DB, 
 * i.e. users are not saved in the DB.
 * 
 * @author hanl, margaretha
 * @lastUpdate 11/2017
 */
@Controller
@Path("v0.1/user")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
@ResourceFilters({APIVersionFilter.class, PiwikFilter.class })
public class UserController {

    @Autowired
    private KustvaktResponseHandler kustvaktResponseHandler;

    private static Logger jlog = LogManager.getLogger(UserController.class);
    @Autowired
    private AuthenticationManager controller;

    private @Context UriInfo info;


    // fixme: json contains password in clear text. Encrypt request?
    // EM: no encryption is needed for communications over https. 
    // It should not be necessary in IDS internal network. 

    @Deprecated
    // fixme: should also collect service exception, not just db exception!
    @POST
    @Path("register")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response signUp (
            @HeaderParam(ContainerRequest.USER_AGENT) String agent,
            @HeaderParam(ContainerRequest.HOST) String host,
            @Context Locale locale, Map values) {

        values.put(Attributes.HOST, host);
        values.put(Attributes.USER_AGENT, agent);
        UriBuilder uriBuilder;
        User user;
        try {
            uriBuilder = info.getBaseUriBuilder();
            uriBuilder.path("user").path("confirm");
            user = controller.createUserAccount(values, true);
        }
        catch (KustvaktException e) {
            throw kustvaktResponseHandler.throwit(e);
        }
        URIParam uri = user.getField(URIParam.class);
        if (uri.hasValues()) {
            uriBuilder
                    .queryParam(Attributes.QUERY_PARAM_URI,
                            uri.getUriFragment())
                    .queryParam(Attributes.QUERY_PARAM_USER,
                            user.getUsername());
            jlog.info("registration was successful for user "+
                    user.getUsername());
            Map<String, Object> object = new HashMap<String, Object>();
            object.put("confirm_uri", uriBuilder.build());
            object.put("uri_expiration",
                    TimeUtils.format(uri.getUriExpiration()));
            try {
                return Response.ok(JsonUtils.toJSON(object)).build();
            }
            catch (KustvaktException e) {
                throw kustvaktResponseHandler.throwit(e);
            }
        }
        else {
            jlog.error("Failed creating confirmation and expiry tokens.");
            // EM: why illegal argument when uri fragment/param is self-generated 
            throw kustvaktResponseHandler.throwit(StatusCodes.ILLEGAL_ARGUMENT,
                    "failed to validate uri parameter",
                    "confirmation fragment");
        }

    }

    @Deprecated
    //todo: password update in special function? --> password reset only!
    @POST
    @Path("update")
    @Consumes(MediaType.APPLICATION_JSON)
    @ResourceFilters({ AuthenticationFilter.class, PiwikFilter.class,
            BlockingFilter.class })
    public Response updateAccount (@Context SecurityContext ctx, String json) {
        TokenContext context = (TokenContext) ctx.getUserPrincipal();
        try {
            User user = controller.getUser(context.getUsername());

            JsonNode node = JsonUtils.readTree(json);
            KorAPUser ident = (KorAPUser) user;
            KorAPUser values = User.UserFactory.toUser(json);
            //            user = controller
            //                    .checkPasswordAllowance(ident, values.getPassword(),
            //                            node.path("new_password").asText());
            //            controller.updateAccount(user);
        }
        catch (KustvaktException e) {
            throw kustvaktResponseHandler.throwit(e);
        }
        return Response.ok().build();
    }

    @Deprecated
    @GET
    @Path("confirm")
    @Produces(MediaType.TEXT_HTML)
    public Response confirmRegistration (@QueryParam("uri") String uritoken,
            @Context Locale locale, @QueryParam("user") String username) {
        if (uritoken == null || uritoken.isEmpty())
            throw kustvaktResponseHandler.throwit(StatusCodes.ILLEGAL_ARGUMENT,
                    "parameter missing", "uri parameter");
        if (username == null || username.isEmpty())
            throw kustvaktResponseHandler.throwit(StatusCodes.ILLEGAL_ARGUMENT,
                    "parameter missing", "Username");

        try {
            controller.confirmRegistration(uritoken, username);
        }
        catch (KustvaktException e) {
            e.printStackTrace();
            throw kustvaktResponseHandler.throwit(e);
        }
        return Response.ok().build();
    }

    @Deprecated
    // todo: auditing!
    @POST
    @Path("requestReset")
    @Produces(MediaType.TEXT_HTML)
    @Consumes({ MediaType.APPLICATION_JSON,
            MediaType.APPLICATION_FORM_URLENCODED })
    public Response requestPasswordReset (@Context Locale locale, String json) {
        JsonNode node;
        try {
            node = JsonUtils.readTree(json);
        }
        catch (KustvaktException e1) {
            throw kustvaktResponseHandler.throwit(e1);
        }
        StringBuilder builder = new StringBuilder();
        String username, email;
        username = node.path(Attributes.USERNAME).asText();
        email = node.path(Attributes.EMAIL).asText();

        // deprecated --> depends on the client!
        //        String url = config.getMailProperties()
        //                .getProperty("korap.frontend.url", "");
        //        if (url.isEmpty())
        //            return Response.ok("URLException: Missing source URL").build();

        //        URIUtils utils = new URIUtils(info);
        // may inject the actual REST url in a redirect request?!
        //        UriBuilder uriBuilder = UriBuilder.fromUri(url).fragment("reset");
        Object[] objects;
        try {
            builder.append("?");
            // just append the endpint fragment plus the query parameter.
            // the address by which the data is handled depends on the frontend
            objects = controller.validateResetPasswordRequest(username, email);
            builder.append(Attributes.QUERY_PARAM_URI).append("=")
                    .append(objects[0]);
            builder.append(Attributes.QUERY_PARAM_USER).append("=")
                    .append(username);
        }
        catch (KustvaktException e) {
            jlog.error("Eoxception encountered! "+ e.string());
            throw kustvaktResponseHandler.throwit(e);
        }

        ObjectNode obj = JsonUtils.createObjectNode();
        obj.put(Attributes.URI, builder.toString());
        obj.put(Attributes.URI_EXPIRATION, objects[1].toString());
        try {
            return Response.ok(JsonUtils.toJSON(obj)).build();
        }
        catch (KustvaktException e) {
            throw kustvaktResponseHandler.throwit(e);
        }
    }

    @Deprecated
    @POST
    @Path("reset")
    @Produces(MediaType.TEXT_HTML)
    @Consumes({ MediaType.APPLICATION_JSON,
            MediaType.APPLICATION_FORM_URLENCODED })
    public Response resetPassword (
            @QueryParam(Attributes.QUERY_PARAM_URI) String uri,
            @QueryParam(Attributes.QUERY_PARAM_USER) String username,
            @Context HttpHeaders headers, String passphrase) {
        try {
            controller.resetPassword(uri, username, passphrase);
        }
        catch (KustvaktException e) {
            jlog.error("Exception encountered!", e);
            return Response.notModified().build();
        }
        return Response.ok().build();
    }


    // todo: refactor and make something out of if --> needs to give some sort of feedback!
    @GET
    @Path("info")
    @ResourceFilters({ AuthenticationFilter.class, PiwikFilter.class,
            BlockingFilter.class })
    public Response getStatus (@Context SecurityContext context,
            @QueryParam("scopes") String scopes) {
        TokenContext ctx = (TokenContext) context.getUserPrincipal();
        Scopes m;
        try {
            User user = controller.getUser(ctx.getUsername());
            Userdata data = controller.getUserData(user, UserDetails.class);

            Set<String> base_scope = StringUtils.toSet(scopes, " ");
            if (scopes != null) base_scope.retainAll(StringUtils.toSet(scopes));
            scopes = StringUtils.toString(base_scope);
            m = Scopes.mapScopes(scopes, data);
            return Response.ok(m.toEntity()).build();
        }
        catch (KustvaktException e) {
            throw kustvaktResponseHandler.throwit(e);
        }
    }


    @GET
    @Path("settings")
    @ResourceFilters({ AuthenticationFilter.class, DemoUserFilter.class,
            PiwikFilter.class, BlockingFilter.class })
    public Response getUserSettings (@Context SecurityContext context,
            @Context Locale locale) {
        TokenContext ctx = (TokenContext) context.getUserPrincipal();
        String result;
        try {
            User user = controller.getUser(ctx.getUsername());
            Userdata data = controller.getUserData(user, UserSettings.class);
            data.setField(Attributes.USERNAME, ctx.getUsername());
            result = data.serialize();
        }
        catch (KustvaktException e) {
            jlog.error("Exception encountered!", e);
            throw kustvaktResponseHandler.throwit(e);
        }
        return Response.ok(result).build();
    }


    @POST
    @Path("settings")
    @Consumes({ MediaType.APPLICATION_JSON })
    @ResourceFilters({ AuthenticationFilter.class, PiwikFilter.class,
            BlockingFilter.class })
    public Response updateSettings (@Context SecurityContext context,
            @Context Locale locale, Map settings) {
        TokenContext ctx = (TokenContext) context.getUserPrincipal();

        if (settings == null) return Response.notModified().build();

        try {
            User user = controller.getUser(ctx.getUsername());
            if (User.UserFactory.isDemo(ctx.getUsername()))
                return Response.notModified().build();

            Userdata data = controller.getUserData(user, UserSettings.class);
            // todo: check setting only within the scope of user settings permissions; not foundry range. Latter is part of
            // frontend which only displays available foundries and
            //            SecurityManager.findbyId(us.getDefaultConstfoundry(), user, Foundry.class);
            //            SecurityManager.findbyId(us.getDefaultLemmafoundry(), user, Foundry.class);
            //            SecurityManager.findbyId(us.getDefaultPOSfoundry(), user, Foundry.class);
            //            SecurityManager.findbyId(us.getDefaultRelfoundry(), user, Foundry.class);
            Userdata new_data = new UserSettings(user.getId());
            new_data.readQuietly((Map<String, Object>) settings, false);
            data.update(new_data);
            controller.updateUserData(data);
        }
        catch (KustvaktException e) {
            jlog.error("Exception encountered!", e);
            throw kustvaktResponseHandler.throwit(e);
        }

        return Response.ok().build();
    }


    @GET
    @Path("details")
    @ResourceFilters({ AuthenticationFilter.class, DemoUserFilter.class,
            PiwikFilter.class, BlockingFilter.class })
    public Response getDetails (@Context SecurityContext context,
            @Context Locale locale, @QueryParam("pointer") String pointer) {
        TokenContext ctx = (TokenContext) context.getUserPrincipal();
        String result;
        try {
            User user = controller.getUser(ctx.getUsername());
            Userdata data = controller.getUserData(user, UserDetails.class);
            data.setField(Attributes.USERNAME, ctx.getUsername());
            if (pointer != null)
                result = data.get(pointer).toString();
            else
                result = data.serialize();
        }
        catch (KustvaktException e) {
            jlog.error("Exception encountered: "+ e.string());
            throw kustvaktResponseHandler.throwit(e);
        }
        return Response.ok(result).build();
    }


    @POST
    @Path("details")
    @Consumes({ MediaType.APPLICATION_JSON })
    @ResourceFilters({ AuthenticationFilter.class, PiwikFilter.class,
            BlockingFilter.class })
    public Response updateDetails (@Context SecurityContext context,
            @Context Locale locale, Map details) {
        TokenContext ctx = (TokenContext) context.getUserPrincipal();

        if (details == null) return Response.notModified().build();

        try {
            User user = controller.getUser(ctx.getUsername());
            if (User.UserFactory.isDemo(ctx.getUsername()))
                return Response.notModified().build();

            UserDetails new_data = new UserDetails(user.getId());
            new_data.readQuietly((Map<String, Object>) details, false);

            UserDetails det = controller.getUserData(user, UserDetails.class);
            det.update(new_data);
            controller.updateUserData(det);
        }
        catch (KustvaktException e) {
            jlog.error("Exception encountered!", e);
            throw kustvaktResponseHandler.throwit(e);
        }
        return Response.ok().build();
    }


    //fixme: if policy allows, foreign user might be allowed to change search!
    @POST
    @Path("queries")
    @Consumes(MediaType.APPLICATION_JSON)
    @ResourceFilters({ AuthenticationFilter.class, PiwikFilter.class,
            BlockingFilter.class })
    public Response updateQueries (@Context SecurityContext context,
            String json) {
        TokenContext ctx = (TokenContext) context.getUserPrincipal();
        Collection<UserQuery> add = new HashSet<>();
        try {
            User user = controller.getUser(ctx.getUsername());
            List<UserQuery> userQuieres = new ArrayList<>();
            JsonNode nodes = JsonUtils.readTree(json);
            Iterator<JsonNode> node = nodes.elements();
            while (node.hasNext()) {
                JsonNode cursor = node.next();
                UserQuery query =
                        new UserQuery(cursor.path("id").asInt(), user.getId());
                query.setQueryLanguage(cursor.path("queryLanguage").asText());
                query.setQuery(cursor.path("query").asText());
                query.setDescription(cursor.path("description").asText());
                userQuieres.add(query);
            }

            //1: add all that are new, update all that are retained, delete the rest
            //            Set<UserQuery> resources = ResourceFinder
            //                    .search(user, UserQuery.class);
            //
            //            add.addAll(userQuieres);
            //            add.removeAll(resources);
            //            Collection<UserQuery> update = new HashSet<>(userQuieres);
            //            update.retainAll(resources);
            //            resources.removeAll(userQuieres);
            //
            //            if (!update.isEmpty()) {
            //                resourceHandler.updateResources(user,
            //                        update.toArray(new UserQuery[update.size()]));
            //            }
            //            if (!add.isEmpty()) {
            //                resourceHandler.storeResources(user,
            //                        add.toArray(new UserQuery[add.size()]));
            //            }
            //            if (!resources.isEmpty()) {
            //                resourceHandler.deleteResources(user,
            //                        resources.toArray(new UserQuery[resources.size()]));
            //            }
        }
        catch (KustvaktException e) {
            jlog.error("Exception encountered!", e);
            throw kustvaktResponseHandler.throwit(e);
        }
        try {
            return Response.ok(JsonUtils.toJSON(add)).build();
        }
        catch (KustvaktException e) {
            throw kustvaktResponseHandler.throwit(e);
        }
    }

    @Deprecated
    @DELETE
    @ResourceFilters({ AuthenticationFilter.class, PiwikFilter.class,
            BlockingFilter.class })
    public Response deleteUser (@Context SecurityContext context) {
        TokenContext ctx = (TokenContext) context.getUserPrincipal();
        try {
            User user = controller.getUser(ctx.getUsername());
            //todo: test that demo user cannot be deleted!
            controller.deleteAccount(user);
        }
        catch (KustvaktException e) {
            jlog.error("Exception encountered!", e);
            throw kustvaktResponseHandler.throwit(e);
        }
        return Response.ok().build();
    }


    @GET
    @Path("queries")
    @ResourceFilters({ AuthenticationFilter.class, DemoUserFilter.class,
            PiwikFilter.class, BlockingFilter.class })
    public Response getQueries (@Context SecurityContext context,
            @Context Locale locale) {
        TokenContext ctx = (TokenContext) context.getUserPrincipal();
        String queryStr;
        try {
            User user = controller.getUser(ctx.getUsername());
            //            Set<UserQuery> queries = ResourceFinder
            //                    .search(user, UserQuery.class);
            //            queryStr = JsonUtils.toJSON(queries);
            //todo:
            queryStr = "";
        }
        catch (KustvaktException e) {
            jlog.error("Exception encountered!", e);
            throw kustvaktResponseHandler.throwit(e);
        }
        return Response.ok(queryStr).build();
    }
}
