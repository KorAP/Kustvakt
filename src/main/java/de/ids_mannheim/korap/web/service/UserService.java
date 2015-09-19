package de.ids_mannheim.korap.web.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ResourceFilters;
import de.ids_mannheim.korap.config.BeanConfiguration;
import de.ids_mannheim.korap.config.Scopes;
import de.ids_mannheim.korap.config.URIParam;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.interfaces.AuthenticationManagerIface;
import de.ids_mannheim.korap.security.ac.ResourceHandler;
import de.ids_mannheim.korap.user.*;
import de.ids_mannheim.korap.utils.JsonUtils;
import de.ids_mannheim.korap.utils.KustvaktLogger;
import de.ids_mannheim.korap.utils.StringUtils;
import de.ids_mannheim.korap.utils.TimeUtils;
import de.ids_mannheim.korap.web.KustvaktServer;
import de.ids_mannheim.korap.web.filter.AuthFilter;
import de.ids_mannheim.korap.web.filter.DefaultFilter;
import de.ids_mannheim.korap.web.filter.PiwikFilter;
import de.ids_mannheim.korap.web.utils.FormWrapper;
import org.slf4j.Logger;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.io.IOException;
import java.util.*;

/**
 * @author hanl
 * @date 29/01/2014
 */
@Path(KustvaktServer.API_VERSION + "/user")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
@ResourceFilters({ PiwikFilter.class })
public class UserService {

    private static Logger error = KustvaktLogger
            .initiate(KustvaktLogger.ERROR_LOG);
    private static Logger jlog = KustvaktLogger
            .initiate(KustvaktLogger.SECURITY_LOG);
    private AuthenticationManagerIface controller;
    private ResourceHandler resourceHandler;

    private
    @Context
    UriInfo info;

    public UserService() {
        this.controller = BeanConfiguration.getBeans()
                .getAuthenticationManager();
        //        this.resourceHandler = BeanConfiguration.getResourceHandler();
    }

    // fixme: json contains password in clear text. Encrypt request?
    // fixme: should also collect service exception, not just db exception!
    @POST
    @Path("register")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response signUp(
            @HeaderParam(ContainerRequest.USER_AGENT) String agent,
            @HeaderParam(ContainerRequest.HOST) String host,
            @Context Locale locale, MultivaluedMap form_values) {

        FormWrapper wrapper = new FormWrapper(form_values);

        wrapper.put(Attributes.HOST, host);
        wrapper.put(Attributes.USER_AGENT, agent);
        UriBuilder uriBuilder;
        User user;
        if (wrapper.get(Attributes.EMAIL) == null)
            throw BeanConfiguration.getResponseHandler()
                    .throwit(StatusCodes.ILLEGAL_ARGUMENT, "parameter missing",
                            "email");

        try {
            uriBuilder = info.getBaseUriBuilder();
            uriBuilder.path(KustvaktServer.API_VERSION).path("user")
                    .path("confirm");

            user = controller.createUserAccount(wrapper);

        }catch (KustvaktException e) {
            throw BeanConfiguration.getResponseHandler().throwit(e);
        }
        URIParam uri = user.getField(URIParam.class);
        if (uri.hasValues()) {
            uriBuilder.queryParam(Attributes.QUERY_PARAM_URI,
                    uri.getUriFragment())
                    .queryParam(Attributes.QUERY_PARAM_USER,
                            user.getUsername());
            jlog.info("registration was successful for user '{}'",
                    form_values.get(Attributes.USERNAME));
            Map object = new HashMap();
            object.put("confirm_uri", uriBuilder.build());
            object.put("uri_expiration",
                    TimeUtils.format(uri.getUriExpiration()));
            return Response.ok(JsonUtils.toJSON(object)).build();
        }else {
            // todo: return error or warning
            return null;
        }

    }

    //todo: password update in special function?
    @POST
    @Path("update")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @ResourceFilters({ AuthFilter.class, DefaultFilter.class,
            PiwikFilter.class })
    public Response updateAccount(@Context SecurityContext ctx, String json) {
        TokenContext context = (TokenContext) ctx.getUserPrincipal();
        try {
            User user = controller.getUser(context.getUsername());

            JsonNode node = JsonUtils.readTree(json);
            KorAPUser ident = (KorAPUser) user;
            KorAPUser values = User.UserFactory.toUser(json);
            //            user = controller
            //                    .checkPasswordAllowance(ident, values.getPassword(),
            //                            node.path("new_password").asText());
            controller.updateAccount(user);
        }catch (KustvaktException e) {
            throw BeanConfiguration.getResponseHandler().throwit(e);
        }
        return Response.ok().build();
    }

    @GET
    @Path("confirm")
    @Produces(MediaType.TEXT_HTML)
    public Response confirmRegistration(@QueryParam("uri") String uritoken,
            @Context Locale locale, @QueryParam("user") String username) {
        if (uritoken == null)
            throw BeanConfiguration.getResponseHandler()
                    .throwit(StatusCodes.ILLEGAL_ARGUMENT, "parameter missing",
                            "Uri-Token");
        if (username == null)
            throw BeanConfiguration.getResponseHandler()
                    .throwit(StatusCodes.ILLEGAL_ARGUMENT, "parameter missing",
                            "Username");

        try {
            controller.confirmRegistration(uritoken, username);
        }catch (KustvaktException e) {
            throw BeanConfiguration.getResponseHandler().throwit(e);
        }
        return Response.ok("success").build();
    }

    // todo: auditing!
    @POST
    @Path("requestReset")
    @Produces(MediaType.TEXT_HTML)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response requestPasswordReset(@Context Locale locale, String json) {
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
        }catch (KustvaktException e) {
            error.error("Eoxception encountered!", e);
            throw BeanConfiguration.getResponseHandler().throwit(e);
        }

        ObjectNode obj = JsonUtils.createObjectNode();
        obj.put(Attributes.URI, builder.toString());
        obj.put(Attributes.URI_EXPIRATION, String.valueOf(objects[1]));
        return Response.ok(JsonUtils.toJSON(obj)).build();
    }

    @POST
    @Path("reset")
    @Produces(MediaType.TEXT_HTML)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response resetPassword(
            @QueryParam(Attributes.QUERY_PARAM_URI) String uri,
            @QueryParam(Attributes.QUERY_PARAM_USER) String username,
            @Context HttpHeaders headers, String passphrase) {
        try {
            controller.resetPassword(uri, username, passphrase);
        }catch (KustvaktException e) {
            error.error("Exception encountered!", e);
            return Response.notModified().build();
        }
        return Response.ok().build();
    }

    @GET
    @Path("info")
    @ResourceFilters({ AuthFilter.class, DefaultFilter.class,
            PiwikFilter.class })
    public Response getStatus(@Context SecurityContext context,
            @QueryParam("scope") String scope) {
        TokenContext ctx = (TokenContext) context.getUserPrincipal();
        User user;
        try {
            user = controller.getUser(ctx.getUsername());
            controller.getUserDetails(user);
            Set<String> base_scope = StringUtils
                    .toSet((String) ctx.getParameters().get(Attributes.SCOPES),
                            " ");
            base_scope.retainAll(StringUtils.toSet(scope));
            scope = StringUtils.toString(base_scope);
        }catch (KustvaktException e) {
            throw BeanConfiguration.getResponseHandler().throwit(e);
        }
        return Response.ok(JsonUtils.toJSON(Scopes
                .mapOpenIDConnectScopes(scope, user.getDetails()))).build();
    }

    @GET
    @Path("settings")
    @ResourceFilters({ AuthFilter.class, DefaultFilter.class,
            PiwikFilter.class })
    public Response getUserSettings(@Context SecurityContext context,
            @Context Locale locale) {
        TokenContext ctx = (TokenContext) context.getUserPrincipal();
        User user;
        try {
            user = controller.getUser(ctx.getUsername());
            controller.getUserSettings(user);

        }catch (KustvaktException e) {
            error.error("Exception encountered!", e);
            throw BeanConfiguration.getResponseHandler().throwit(e);
        }
        return Response.ok(JsonUtils.toJSON(user.getSettings().toObjectMap()))
                .build();
    }

    // todo: test
    @POST
    @Path("settings")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @ResourceFilters({ AuthFilter.class, DefaultFilter.class,
            PiwikFilter.class })
    public Response updateSettings(@Context SecurityContext context,
            @Context Locale locale, String values) {
        TokenContext ctx = (TokenContext) context.getUserPrincipal();
        Map<String, Object> settings;
        try {
            settings = JsonUtils.read(values, Map.class);
        }catch (IOException e) {
            throw BeanConfiguration.getResponseHandler()
                    .throwit(StatusCodes.REQUEST_INVALID,
                            "Could not read parameters", values);
        }

        try {
            User user = controller.getUser(ctx.getUsername());
            UserSettings us = controller.getUserSettings(user);
            // todo:
            //            SecurityManager.findbyId(us.getDefaultConstfoundry(), user, Foundry.class);
            //            SecurityManager.findbyId(us.getDefaultLemmafoundry(), user, Foundry.class);
            //            SecurityManager.findbyId(us.getDefaultPOSfoundry(), user, Foundry.class);
            //            SecurityManager.findbyId(us.getDefaultRelfoundry(), user, Foundry.class);
            us.updateObjectSettings(settings);
            controller.updateUserSettings(user, us);
            if (user.isDemo())
                return Response.notModified().build();
        }catch (KustvaktException e) {
            error.error("Exception encountered!", e);
            throw BeanConfiguration.getResponseHandler().throwit(e);
        }

        return Response.ok().build();
    }

    @GET
    @Path("details")
    @ResourceFilters({ AuthFilter.class, DefaultFilter.class,
            PiwikFilter.class })
    public Response getDetails(@Context SecurityContext context,
            @Context Locale locale) {
        TokenContext ctx = (TokenContext) context.getUserPrincipal();
        User user;
        try {
            user = controller.getUser(ctx.getUsername());
            controller.getUserDetails(user);
        }catch (KustvaktException e) {
            error.error("Exception encountered!", e);
            throw BeanConfiguration.getResponseHandler().throwit(e);
        }

        return Response.ok(JsonUtils.toJSON(user.getDetails().toMap())).build();
    }

    @POST
    @Path("details")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @ResourceFilters({ AuthFilter.class, DefaultFilter.class,
            PiwikFilter.class })
    public Response updateDetails(@Context SecurityContext context,
            @Context Locale locale, String values) {
        TokenContext ctx = (TokenContext) context.getUserPrincipal();
        System.out.println("TO BE UPDATED DATA " + values);
        System.out.println("USER CONTEXT " + ctx);
        Map<String, String> details;
        try {
            details = JsonUtils.read(values, Map.class);
        }catch (IOException e) {
            error.error("Exception encountered!", e);
            throw BeanConfiguration.getResponseHandler()
                    .throwit(StatusCodes.REQUEST_INVALID,
                            "Could not read parameters", values);
        }

        try {
            User user = controller.getUser(ctx.getUsername());
            UserDetails det = controller.getUserDetails(user);
            det.updateDetails(details);
            controller.updateUserDetails(user, det);
            if (user.isDemo())
                return Response.notModified().build();
        }catch (KustvaktException e) {
            error.error("Exception encountered!", e);
            throw BeanConfiguration.getResponseHandler().throwit(e);
        }

        return Response.ok().build();
    }

    //fixme: if policy allows, foreign user might be allowed to change search!
    @POST
    @Path("queries")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @ResourceFilters({ AuthFilter.class, DefaultFilter.class,
            PiwikFilter.class })
    public Response updateQueries(@Context SecurityContext context,
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
        }catch (KustvaktException e) {
            error.error("Exception encountered!", e);
            throw BeanConfiguration.getResponseHandler().throwit(e);
        }
        return Response.ok(JsonUtils.toJSON(add)).build();
    }

    @DELETE
    @ResourceFilters({ AuthFilter.class, DefaultFilter.class,
            PiwikFilter.class })
    public Response deleteUser(@Context SecurityContext context) {
        TokenContext ctx = (TokenContext) context.getUserPrincipal();
        try {
            User user = controller.getUser(ctx.getUsername());
            if (user.isDemo())
                return Response.notModified().build();
            controller.deleteAccount(user);
        }catch (KustvaktException e) {
            error.error("Exception encountered!", e);
            throw BeanConfiguration.getResponseHandler().throwit(e);
        }
        return Response.ok().build();
    }

    @GET
    @Path("queries")
    @ResourceFilters({ AuthFilter.class, DefaultFilter.class,
            PiwikFilter.class })
    public Response getQueries(@Context SecurityContext context,
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
        }catch (KustvaktException e) {
            error.error("Exception encountered!", e);
            throw BeanConfiguration.getResponseHandler().throwit(e);
        }
        return Response.ok(queryStr).build();
    }

    @GET
    @Path("logout")
    @ResourceFilters({ AuthFilter.class, DefaultFilter.class,
            PiwikFilter.class })
    public Response logout(@Context SecurityContext ctx,
            @Context Locale locale) {
        TokenContext context = (TokenContext) ctx.getUserPrincipal();
        try {
            controller.logout(context);
        }catch (KustvaktException e) {
            error.error("Logout Exception", e);
            throw BeanConfiguration.getResponseHandler().throwit(e);
        }
        return Response.ok().build();
    }
}
