package de.ids_mannheim.korap.web.service.full;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ResourceFilters;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.BeansFactory;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.interfaces.AuthenticationManagerIface;
import de.ids_mannheim.korap.security.auth.BasicHttpAuth;
import de.ids_mannheim.korap.user.*;
import de.ids_mannheim.korap.utils.JsonUtils;
import de.ids_mannheim.korap.utils.KustvaktLogger;
import de.ids_mannheim.korap.utils.ServiceInfo;
import de.ids_mannheim.korap.web.KustvaktServer;
import de.ids_mannheim.korap.web.filter.*;
import de.ids_mannheim.korap.web.utils.KustvaktResponseHandler;
import org.slf4j.Logger;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

//import com.sun.xml.internal.messaging.saaj.util.Base64;

/**
 * @author hanl
 * @date 24/01/2014
 */
@Path(KustvaktServer.API_VERSION + "/auth")
@ResourceFilters({ PiwikFilter.class })
@Produces(MediaType.TEXT_HTML + ";charset=utf-8")
public class AuthService {

    //todo: bootstrap function to transmit certain default configuration settings and examples (example user queries,
    // default usersettings, etc.)
    private static Logger jlog = KustvaktLogger.getLogger(AuthService.class);

    private AuthenticationManagerIface controller;


    //    private SendMail mail;

    public AuthService () {
        this.controller = BeansFactory.getKustvaktContext()
                .getAuthenticationManager();
        //todo: replace with real property values
        //        this.mail = new SendMail(ExtConfiguration.getMailProperties());
    }


    /**
     * represents json string with data. All GUI clients can access
     * this method to get certain default values
     * --> security checks?
     * 
     * @return String
     */
    @Deprecated
    @GET
    @Path("bootstrap")
    @Produces(MediaType.APPLICATION_JSON)
    public Response bootstrap () {
        Map m = new HashMap();
        //        m.put("settings", new UserSettings().toObjectMap());
        m.put("ql", BeansFactory.getKustvaktContext().getConfiguration()
                .getQueryLanguages());
        m.put("SortTypes", null); // types of sorting that are supported!
        m.put("version", ServiceInfo.getInfo().getVersion());
        return Response.ok(JsonUtils.toJSON(m)).build();
    }


    // fixme: moved to user
    @GET
    @Path("status")
    @ResourceFilters({ AuthFilter.class, DemoUserFilter.class, BlockingFilter.class })
    public Response getStatus (@Context SecurityContext context,
            @HeaderParam(ContainerRequest.USER_AGENT) String agent,
            @HeaderParam(ContainerRequest.HOST) String host,
            @Context Locale locale) {
        TokenContext ctx = (TokenContext) context.getUserPrincipal();
        return Response.ok(ctx.toJSON()).build();
    }


    @GET
    @Path("apiToken")
    @ResourceFilters({HeaderFilter.class})
    public Response requestAPIToken (@Context HttpHeaders headers,
            @Context Locale locale,
            @HeaderParam(ContainerRequest.USER_AGENT) String agent,
            @HeaderParam(ContainerRequest.HOST) String host,
            @HeaderParam("referer-url") String referer,
            @QueryParam("scope") String scopes) {
        List<String> auth = headers
                .getRequestHeader(ContainerRequest.AUTHORIZATION);


       // if (auth == null)
        //    throw KustvaktResponseHandler
        //            .throwit(StatusCodes.ACCESS_DENIED);
        String[] values = BasicHttpAuth.decode(auth.get(0));

        // "Invalid syntax for username and password"
        if (values == null)
            throw KustvaktResponseHandler
                    .throwit(StatusCodes.ACCESS_DENIED);

        if (values[0].equalsIgnoreCase("null")
                | values[1].equalsIgnoreCase("null"))
            // is actual an invalid request
            throw KustvaktResponseHandler.throwit(StatusCodes.REQUEST_INVALID);

        Map<String, Object> attr = new HashMap<>();
        if (scopes != null && !scopes.isEmpty())
            attr.put(Attributes.SCOPES, scopes);
        attr.put(Attributes.HOST, host);
        attr.put(Attributes.USER_AGENT, agent);
        TokenContext context;
        try {
            User user = controller.authenticate(0, values[0], values[1], attr);
            Userdata data = this.controller
                    .getUserData(user, UserDetails.class);
            // todo: is this necessary?
            //            attr.putAll(data.fields());
            context = controller.createTokenContext(user, attr,
                    Attributes.API_AUTHENTICATION);
        }
        catch (KustvaktException e) {
            throw KustvaktResponseHandler.throwit(e);
        }

        return Response.ok(context.toResponse()).build();
    }


    // todo:
    @Deprecated
    @GET
    @Path("refresh")
    public Response refresh (@Context SecurityContext context,
            @Context Locale locale) {
        TokenContext ctx = (TokenContext) context.getUserPrincipal();
        TokenContext newContext;

        //        try {
        //            newContext = controller.refresh(ctx);
        //        }catch (KorAPException e) {
        //            KorAPLogger.ERROR_LOGGER.error("Exception encountered!", e);
        //            throw KustvaktResponseHandler.throwit(e);
        //        }
        //        return Response.ok().entity(newContext.getToken()).build();
        return null;
    }


    @GET
    @Path("sessionToken")
    public Response requestSession (@Context HttpHeaders headers,
            @Context Locale locale,
            @HeaderParam(ContainerRequest.USER_AGENT) String agent,
            @HeaderParam(ContainerRequest.HOST) String host) {
        List<String> auth = headers
                .getRequestHeader(ContainerRequest.AUTHORIZATION);

        if (auth == null)
            throw KustvaktResponseHandler
                    .throwit(StatusCodes.ACCESS_DENIED);

        String[] values = BasicHttpAuth.decode(auth.get(0));
        //        authentication = StringUtils.stripTokenType(authentication);
        //        String[] values = new String(
        //                DatatypeConverter.parseBase64Binary(authentication)).split(":");
        //        String[] values = Base64.base64Decode(authentication).split(":");

        // "Invalid syntax for username and password"
        if (values == null)
            throw KustvaktResponseHandler
                    .throwit(StatusCodes.BAD_CREDENTIALS);

        if (values[0].equalsIgnoreCase("null")
                | values[1].equalsIgnoreCase("null"))
            throw KustvaktResponseHandler.throwit(StatusCodes.REQUEST_INVALID);

        Map<String, Object> attr = new HashMap<>();
        attr.put(Attributes.HOST, host);
        attr.put(Attributes.USER_AGENT, agent);
        TokenContext context;
        try {
            User user = controller.authenticate(0, values[0], values[1], attr);
            context = controller.createTokenContext(user, attr,
                    Attributes.SESSION_AUTHENTICATION);
        }
        catch (KustvaktException e) {
            throw KustvaktResponseHandler.throwit(e);
        }
        return Response.ok().entity(context.toJSON()).build();
    }


    // fixme: security issues: setup shibboleth compatible authentication system
    // todo: will be purged with token authentication --> shib is client side
    @POST
    @Consumes("application/x-www-form-urlencoded")
    @Produces("application/json")
    @Path("shibboleth")
    public Response loginshib (@Context HttpHeaders headers,
            @Context Locale locale,
            @HeaderParam(ContainerRequest.USER_AGENT) String agent,
            @HeaderParam(ContainerRequest.HOST) String host,
            @QueryParam("client_id") String client_id) {

        // the shibfilter decrypted the values
        // define default provider for returned access token strategy?!

        Map<String, Object> attr = new HashMap<>();
        attr.put(Attributes.HOST, host);
        attr.put(Attributes.USER_AGENT, agent);

        TokenContext context;

        try {
            // todo: distinguish type KorAP/Shibusers
            User user = controller.authenticate(1, null, null, attr);
            context = controller.createTokenContext(user, attr, null);
        }
        catch (KustvaktException e) {
            throw KustvaktResponseHandler.throwit(e);
        }
        return Response.ok().entity(context.toJSON()).build();
    }


    //fixme: moved from userservice
    @GET
    @Path("logout")
    @ResourceFilters({ AuthFilter.class, DemoUserFilter.class, PiwikFilter.class })
    public Response logout (@Context SecurityContext ctx, @Context Locale locale) {
        TokenContext context = (TokenContext) ctx.getUserPrincipal();
        try {
            controller.logout(context);
        }
        catch (KustvaktException e) {
            jlog.error("Logout Exception: {}", e.string());
            throw KustvaktResponseHandler.throwit(e);
        }
        return Response.ok().build();
    }

}
