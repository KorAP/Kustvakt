package de.ids_mannheim.korap.web.service.full;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ResourceFilters;

import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.BeansFactory;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.filter.AuthFilter;
import de.ids_mannheim.korap.interfaces.AuthenticationManagerIface;
import de.ids_mannheim.korap.security.auth.BasicHttpAuth;
import de.ids_mannheim.korap.server.KustvaktServer;
import de.ids_mannheim.korap.user.*;
import de.ids_mannheim.korap.utils.JsonUtils;
import de.ids_mannheim.korap.utils.KustvaktLogger;
import de.ids_mannheim.korap.utils.ServiceInfo;
import de.ids_mannheim.korap.web.filter.*;
import de.ids_mannheim.korap.web.utils.KustvaktResponseHandler;

import org.eclipse.jetty.util.log.Log;
import org.slf4j.Logger;

import javax.servlet.http.HttpServletRequest; // FB
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.xml.ws.WebServiceContext; // FB
import javax.xml.ws.handler.MessageContext; // FB
import javax.annotation.Resource; // FB

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ArrayList;
import java.util.Iterator; // 07.02.17/FB

//import com.sun.xml.internal.messaging.saaj.util.Base64;

/**
 * @author hanl
 * @date 24/01/2014
 */
@Path("/auth")
@ResourceFilters({ PiwikFilter.class })
@Produces(MediaType.TEXT_HTML + ";charset=utf-8")
public class AuthService {

    private static Boolean DEBUG_LOG = true;

    //todo: bootstrap function to transmit certain default configuration settings and examples (example user queries,
    // default usersettings, etc.)
    private static Logger jlog = KustvaktLogger.getLogger(AuthService.class);

    private AuthenticationManagerIface controller;


    //    private SendMail mail;

    public AuthService () {
        this.controller =
                BeansFactory.getKustvaktContext().getAuthenticationManager();
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
    @ResourceFilters({ AuthFilter.class, DemoUserFilter.class,
            BlockingFilter.class })
    public Response getStatus (@Context SecurityContext context,
            @HeaderParam(ContainerRequest.USER_AGENT) String agent,
            @HeaderParam(ContainerRequest.HOST) String host,
            @Context Locale locale) {
        TokenContext ctx = (TokenContext) context.getUserPrincipal();
        return Response.ok(ctx.toJson()).build();
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
            throw KustvaktResponseHandler
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
            throw KustvaktResponseHandler.throwit(StatusCodes.ACCESS_DENIED);

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
            // User user = controller.authenticate(0, values[0], values[1], attr); Implementation by Hanl
            User user = controller.authenticate(2, values[0], values[1], attr); // Implementation with IdM/LDAP
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
                    Attributes.API_AUTHENTICATION);
        }
        catch (KustvaktException e) {
            throw KustvaktResponseHandler.throwit(e);
        }

        return Response.ok(context.toJson()).build();
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
            throw KustvaktResponseHandler.throwit(StatusCodes.BAD_CREDENTIALS);

        // Implementation Hanl mit '|'. 16.02.17/FB
        //if (values[0].equalsIgnoreCase("null")
        //        | values[1].equalsIgnoreCase("null"))
        if (values[0].equalsIgnoreCase("null")
                || values[1].equalsIgnoreCase("null"))
            throw KustvaktResponseHandler.throwit(StatusCodes.REQUEST_INVALID);

        Map<String, Object> attr = new HashMap<>();
        attr.put(Attributes.HOST, host);
        attr.put(Attributes.USER_AGENT, agent);
        TokenContext context;
        String contextJson;
        try {
            User user = controller.authenticate(0, values[0], values[1], attr);
            context = controller.createTokenContext(user, attr,
                    Attributes.SESSION_AUTHENTICATION);
            contextJson = context.toJson();
            jlog.debug(contextJson);
        }
        catch (KustvaktException e) {
            throw KustvaktResponseHandler.throwit(e);
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
            User user = controller.authenticate(1, null, null, attr);
            context = controller.createTokenContext(user, attr, null);
        }
        catch (KustvaktException e) {
            throw KustvaktResponseHandler.throwit(e);
        }
        return Response.ok().entity(context.toJson()).build();
    }


    //fixme: moved from userservice
    @GET
    @Path("logout")
    @ResourceFilters({ AuthFilter.class, DemoUserFilter.class,
            PiwikFilter.class })
    public Response logout (@Context SecurityContext ctx,
            @Context Locale locale) {
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
