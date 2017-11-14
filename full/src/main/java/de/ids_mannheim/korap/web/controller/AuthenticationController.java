package de.ids_mannheim.korap.web.controller;

import java.util.HashMap;
import java.util.Iterator; // 07.02.17/FB
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ResourceFilters;

import de.ids_mannheim.korap.authentication.BasicHttpAuth;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.AuthenticationType;
import de.ids_mannheim.korap.config.BeansFactory;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.interfaces.AuthenticationManagerIface;
import de.ids_mannheim.korap.user.TokenContext;
import de.ids_mannheim.korap.user.User;
import de.ids_mannheim.korap.utils.JsonUtils;
import de.ids_mannheim.korap.utils.KustvaktLogger;
import de.ids_mannheim.korap.utils.ServiceInfo;
import de.ids_mannheim.korap.web.filter.AuthenticationFilter;
import de.ids_mannheim.korap.web.filter.BlockingFilter;
import de.ids_mannheim.korap.web.filter.DemoUserFilter;
import de.ids_mannheim.korap.web.filter.PiwikFilter;
import de.ids_mannheim.korap.web.utils.KustvaktResponseHandler;

// import com.sun.xml.internal.messaging.saaj.util.Base64;

/**
 * @author hanl
 * @date 24/01/2014
 */
@Controller
@Path("/auth")
@ResourceFilters({ PiwikFilter.class })
@Produces(MediaType.TEXT_HTML + ";charset=utf-8")
public class AuthenticationController {

    @Autowired
    KustvaktResponseHandler kustvaktResponseHandler;

    private static Boolean DEBUG_LOG = true;

    //todo: bootstrap function to transmit certain default configuration settings and examples (example user queries,
    // default usersettings, etc.)
    private static Logger jlog =
            KustvaktLogger.getLogger(AuthenticationController.class);

    @Autowired
    private AuthenticationManagerIface controller;

    //    private SendMail mail;

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
        try {
            return Response.ok(JsonUtils.toJSON(m)).build();
        }
        catch (KustvaktException e) {
            throw kustvaktResponseHandler.throwit(e);
        }
    }


    // fixme: moved to user
    @GET
    @Path("status")
    @ResourceFilters({ AuthenticationFilter.class, DemoUserFilter.class,
            BlockingFilter.class })
    public Response getStatus (@Context SecurityContext context,
            @HeaderParam(ContainerRequest.USER_AGENT) String agent,
            @HeaderParam(ContainerRequest.HOST) String host,
            @Context Locale locale) {
        TokenContext ctx = (TokenContext) context.getUserPrincipal();
        try {
            return Response.ok(ctx.toJson()).build();
        }
        catch (KustvaktException e) {
            throw kustvaktResponseHandler.throwit(e);
        }
    }


    @GET
    @Path("apiToken")
    //@ResourceFilters({HeaderFilter.class})
    public Response requestAPIToken (@Context HttpHeaders headers,
            @Context Locale locale,
            @HeaderParam(ContainerRequest.USER_AGENT) String agent,
            @HeaderParam(ContainerRequest.HOST) String host,
            @HeaderParam("referer-url") String referer,
            @QueryParam("scope") String scopes,
            //   @Context WebServiceContext wsContext, // FB
            @Context SecurityContext secCtx) {

        List<String> auth =
                headers.getRequestHeader(ContainerRequest.AUTHORIZATION);
        if (auth == null || auth.isEmpty()) {
            throw kustvaktResponseHandler
                    .throwit(new KustvaktException(StatusCodes.MISSING_ARGUMENT,
                            "Authorization header is missing.",
                            "Authorization header"));
        }

        String[] values = BasicHttpAuth.decode(auth.get(0));

        if (DEBUG_LOG == true) {
            System.out.printf("Debug: AuthService.requestAPIToken...:\n");
            System.out.printf("Debug: auth.size=%d\n", auth.size());
            System.out.printf("auth.get(0)='%s'\n", auth.get(0));
            System.out.printf("Debug: values.length=%d\n", values.length);
            /* hide password etc. - FB
             if( auth.size() > 0 )
            	{
            	Iterator it = auth.iterator();
            	while( it.hasNext() )
            		System.out.printf(" header '%s'\n",  it.next());
            	}
            if( values.length > 0 )
            	{
            	for(int i=0; i< values.length; i++)
            		{
            		System.out.printf(" values[%d]='%s'\n",  i, values[i]);
            		}
            	}
             */
            MultivaluedMap<String, String> headerMap =
                    headers.getRequestHeaders();
            if (headerMap != null && headerMap.size() > 0) {
                Iterator<String> it = headerMap.keySet().iterator();
                while (it.hasNext()) {
                    String key = (String) it.next();
                    List<String> vals = headerMap.get(key);
                    System.out.printf("Debug: requestAPIToken: '%s' = '%s'\n",
                            key, vals);
                }

            }
            System.out.printf("Debug: requestAPIToken: isSecure = %s.\n",
                    secCtx.isSecure() ? "yes" : "no");
        } // DEBUG_LOG        

        // "Invalid syntax for username and password"
        if (values == null)
            throw kustvaktResponseHandler.throwit(StatusCodes.ACCESS_DENIED);

        if (values[0].equalsIgnoreCase("null")
                | values[1].equalsIgnoreCase("null"))
            // is actual an invalid request
            throw kustvaktResponseHandler.throwit(StatusCodes.REQUEST_INVALID);

        Map<String, Object> attr = new HashMap<>();
        if (scopes != null && !scopes.isEmpty())
            attr.put(Attributes.SCOPES, scopes);
        attr.put(Attributes.HOST, host);
        attr.put(Attributes.USER_AGENT, agent);

        TokenContext context;
        try {
            // User user = controller.authenticate(0, values[0], values[1], attr); Implementation by Hanl
            User user = controller.authenticate(AuthenticationType.LDAP,
                    values[0], values[1], attr); // Implementation with IdM/LDAP
            // Userdata data = this.controller.getUserData(user, UserDetails.class); // Implem. by Hanl
            // todo: is this necessary?
            //            attr.putAll(data.fields());
            controller.setAccessAndLocation(user, headers);
            if (DEBUG_LOG == true) System.out.printf(
                    "Debug: /apiToken/: location=%s, access='%s'.\n",
                    user.locationtoString(), user.accesstoString());
            attr.put(Attributes.LOCATION, user.getLocation());
            attr.put(Attributes.CORPUS_ACCESS, user.getCorpusAccess());
            context = controller.createTokenContext(user, attr,
                    AuthenticationType.LDAP.name());
                    //Attributes.API_AUTHENTICATION);
        }
        catch (KustvaktException e) {
            throw kustvaktResponseHandler.throwit(e);
        }

        try {
            return Response.ok(context.toJson()).build();
        }
        catch (KustvaktException e) {
            throw kustvaktResponseHandler.throwit(e);
        }
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
    //@ResourceFilters({HeaderFilter.class})
    public Response requestSession (@Context HttpHeaders headers,
            @Context Locale locale,
            @HeaderParam(ContainerRequest.USER_AGENT) String agent,
            @HeaderParam(ContainerRequest.HOST) String host) {
        List<String> auth =
                headers.getRequestHeader(ContainerRequest.AUTHORIZATION);

        String[] values = BasicHttpAuth.decode(auth.get(0));
        //        authentication = StringUtils.stripTokenType(authentication);
        //        String[] values = new String(
        //                DatatypeConverter.parseBase64Binary(authentication)).split(":");
        //        String[] values = Base64.base64Decode(authentication).split(":");

        // "Invalid syntax for username and password"
        if (values == null)
            throw kustvaktResponseHandler.throwit(StatusCodes.BAD_CREDENTIALS);

        // Implementation Hanl mit '|'. 16.02.17/FB
        //if (values[0].equalsIgnoreCase("null")
        //        | values[1].equalsIgnoreCase("null"))
        if (values[0].equalsIgnoreCase("null")
                || values[1].equalsIgnoreCase("null"))
            throw kustvaktResponseHandler.throwit(StatusCodes.REQUEST_INVALID);

        Map<String, Object> attr = new HashMap<>();
        attr.put(Attributes.HOST, host);
        attr.put(Attributes.USER_AGENT, agent);
        TokenContext context;
        String contextJson;
        try {
            User user = controller.authenticate(AuthenticationType.SESSION,
                    values[0], values[1], attr);
            context = controller.createTokenContext(user, attr,
                    Attributes.SESSION_AUTHENTICATION);
            contextJson = context.toJson();
            jlog.debug(contextJson);
        }
        catch (KustvaktException e) {
            throw kustvaktResponseHandler.throwit(e);
        }
        return Response.ok().entity(contextJson).build();
    }


    // fixme: security issues: setup shibboleth compatible authentication system
    // todo: will be purged with token authentication --> shib is client side
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
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
            User user = controller.authenticate(AuthenticationType.SHIBBOLETH,
                    null, null, attr);
            context = controller.createTokenContext(user, attr, null);
        }
        catch (KustvaktException e) {
            throw kustvaktResponseHandler.throwit(e);
        }
        try {
            return Response.ok().entity(context.toJson()).build();
        }
        catch (KustvaktException e) {
            throw kustvaktResponseHandler.throwit(e);
        }
    }


    //fixme: moved from userservice
    @GET
    @Path("logout")
    @ResourceFilters({ AuthenticationFilter.class, DemoUserFilter.class,
            PiwikFilter.class })
    public Response logout (@Context SecurityContext ctx,
            @Context Locale locale) {
        TokenContext context = (TokenContext) ctx.getUserPrincipal();
        try {
            controller.logout(context);
        }
        catch (KustvaktException e) {
            jlog.error("Logout Exception: {}", e.string());
            throw kustvaktResponseHandler.throwit(e);
        }
        return Response.ok().build();
    }

}
