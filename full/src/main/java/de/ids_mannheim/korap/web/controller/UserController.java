package de.ids_mannheim.korap.web.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ResourceFilters;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.BeansFactory;
import de.ids_mannheim.korap.config.Scopes;
import de.ids_mannheim.korap.config.URIParam;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.filter.AuthFilter;
import de.ids_mannheim.korap.interfaces.AuthenticationManagerIface;
import de.ids_mannheim.korap.server.KustvaktServer;
import de.ids_mannheim.korap.user.*;
import de.ids_mannheim.korap.utils.JsonUtils;
import de.ids_mannheim.korap.utils.StringUtils;
import de.ids_mannheim.korap.utils.TimeUtils;
import de.ids_mannheim.korap.web.filter.BlockingFilter;
import de.ids_mannheim.korap.web.filter.DemoUserFilter;
import de.ids_mannheim.korap.web.filter.PiwikFilter;
import de.ids_mannheim.korap.web.utils.KustvaktResponseHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.util.*;

/**
 * @author hanl, margaretha
 * @lastUpdate 04/2017
 */
@Path("/user")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
@ResourceFilters({ PiwikFilter.class })
public class UserController {

    private static Logger jlog = LoggerFactory.getLogger(UserController.class);
    private AuthenticationManagerIface controller;

    private @Context UriInfo info;


    public UserController () {
        this.controller = BeansFactory.getKustvaktContext()
                .getAuthenticationManager();
    }


    // fixme: json contains password in clear text. Encrypt request?
    // EM: no encryption is needed for communications over https. 
    // It should not be necessary in IDS internal network. 
    
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
            throw KustvaktResponseHandler.throwit(e);
        }
        URIParam uri = user.getField(URIParam.class);
        if (uri.hasValues()) {
            uriBuilder
                    .queryParam(Attributes.QUERY_PARAM_URI,
                            uri.getUriFragment())
                    .queryParam(Attributes.QUERY_PARAM_USER,
                            user.getUsername());
            jlog.info("registration was successful for user '{}'",
                    user.getUsername());
            Map<String, Object> object = new HashMap<String, Object>();
            object.put("confirm_uri", uriBuilder.build());
            object.put("uri_expiration",
                    TimeUtils.format(uri.getUriExpiration()));
            return Response.ok(JsonUtils.toJSON(object)).build();
        }
        else {
            jlog.error("Failed creating confirmation and expiry tokens.");
            // EM: why illegal argument when uri fragment/param is self-generated 
            throw KustvaktResponseHandler.throwit(StatusCodes.ILLEGAL_ARGUMENT,
                    "failed to validate uri parameter", "confirmation fragment");
        }

    }


    //todo: password update in special function? --> password reset only!
    @POST
    @Path("update")
    @Consumes(MediaType.APPLICATION_JSON)
    @ResourceFilters({ AuthFilter.class, PiwikFilter.class,
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
            throw KustvaktResponseHandler.throwit(e);
        }
        return Response.ok().build();
    }


    @GET
    @Path("confirm")
    @Produces(MediaType.TEXT_HTML)
    public Response confirmRegistration (@QueryParam("uri") String uritoken,
            @Context Locale locale, @QueryParam("user") String username) {
        if (uritoken == null || uritoken.isEmpty())
            throw KustvaktResponseHandler.throwit(StatusCodes.ILLEGAL_ARGUMENT,
                    "parameter missing", "uri parameter");
        if (username == null || username.isEmpty())
            throw KustvaktResponseHandler.throwit(StatusCodes.ILLEGAL_ARGUMENT,
                    "parameter missing", "Username");

        try {
            controller.confirmRegistration(uritoken, username);
        }
        catch (KustvaktException e) {
            e.printStackTrace();
            throw KustvaktResponseHandler.throwit(e);
        }
        return Response.ok().build();
    }


    // todo: auditing!
    @POST
    @Path("requestReset")
    @Produces(MediaType.TEXT_HTML)
    @Consumes({ MediaType.APPLICATION_JSON,
            MediaType.APPLICATION_FORM_URLENCODED })
    public Response requestPasswordReset (@Context Locale locale, String json) {
        JsonNode node = JsonUtils.readTree(json);
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
            jlog.error("Eoxception encountered!", e.string());
            throw KustvaktResponseHandler.throwit(e);
        }

        ObjectNode obj = JsonUtils.createObjectNode();
        obj.put(Attributes.URI, builder.toString());
        obj.put(Attributes.URI_EXPIRATION, objects[1].toString());
        return Response.ok(JsonUtils.toJSON(obj)).build();
    }


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
    @ResourceFilters({ AuthFilter.class, PiwikFilter.class,
            BlockingFilter.class })
    public Response getStatus (@Context SecurityContext context,
            @QueryParam("scopes") String scopes) {
        TokenContext ctx = (TokenContext) context.getUserPrincipal();
        Scopes m;
        try {
            User user = controller.getUser(ctx.getUsername());
            Userdata data = controller.getUserData(user, UserDetails.class);

            Set<String> base_scope = StringUtils.toSet(scopes, " ");
            if (scopes != null)
                base_scope.retainAll(StringUtils.toSet(scopes));
            scopes = StringUtils.toString(base_scope);
            m = Scopes.mapScopes(scopes, data);
        }
        catch (KustvaktException e) {
            throw KustvaktResponseHandler
                    .throwAuthenticationException(ctx.getUsername());
        }
        return Response.ok(m.toEntity()).build();
    }


    @GET
    @Path("settings")
    @ResourceFilters({ AuthFilter.class, DemoUserFilter.class,
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
            throw KustvaktResponseHandler.throwit(e);
        }
        return Response.ok(result).build();
    }


    @POST
    @Path("settings")
    @Consumes({ MediaType.APPLICATION_JSON })
    @ResourceFilters({ AuthFilter.class, PiwikFilter.class,
            BlockingFilter.class })
    public Response updateSettings (@Context SecurityContext context,
            @Context Locale locale, Map settings) {
        TokenContext ctx = (TokenContext) context.getUserPrincipal();

        if (settings == null)
            return Response.notModified().build();

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
            throw KustvaktResponseHandler.throwit(e);
        }

        return Response.ok().build();
    }


    @GET
    @Path("details")
    @ResourceFilters({ AuthFilter.class, DemoUserFilter.class,
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
            jlog.error("Exception encountered: {}", e.string());
            throw KustvaktResponseHandler.throwit(e);
        }
        return Response.ok(result).build();
    }


    @POST
    @Path("details")
    @Consumes({ MediaType.APPLICATION_JSON })
    @ResourceFilters({ AuthFilter.class, PiwikFilter.class,
            BlockingFilter.class })
    public Response updateDetails (@Context SecurityContext context,
            @Context Locale locale, Map details) {
        TokenContext ctx = (TokenContext) context.getUserPrincipal();

        if (details == null)
            return Response.notModified().build();

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
            throw KustvaktResponseHandler.throwit(e);
        }
        return Response.ok().build();
    }


    //fixme: if policy allows, foreign user might be allowed to change search!
    @POST
    @Path("queries")
    @Consumes(MediaType.APPLICATION_JSON)
    @ResourceFilters({ AuthFilter.class, PiwikFilter.class,
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
                UserQuery query = new UserQuery(cursor.path("id").asInt(),
                        user.getId());
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
            throw KustvaktResponseHandler.throwit(e);
        }
        return Response.ok(JsonUtils.toJSON(add)).build();
    }


    @DELETE
    @ResourceFilters({ AuthFilter.class, PiwikFilter.class,
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
            throw KustvaktResponseHandler.throwit(e);
        }
        return Response.ok().build();
    }


    @GET
    @Path("queries")
    @ResourceFilters({ AuthFilter.class, DemoUserFilter.class,
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
            throw KustvaktResponseHandler.throwit(e);
        }
        return Response.ok(queryStr).build();
    }
}
