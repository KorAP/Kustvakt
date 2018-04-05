package de.ids_mannheim.korap.web.filter;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.LoggingFilter;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import com.sun.jersey.spi.container.ContainerResponseFilter;
import com.sun.jersey.spi.container.ResourceFilter;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.BeansFactory;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.interfaces.AuthenticationManagerIface;
import de.ids_mannheim.korap.security.context.TokenContext;
import de.ids_mannheim.korap.user.*;
import net.minidev.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.ext.Provider;
import java.security.SecureRandom;
import java.util.*;

/**
 * @author hanl
 * @date 13/05/2014
 */
@Component
@Provider
public class PiwikFilter implements ContainerRequestFilter, ResourceFilter {

    private WebResource service;
    //    private static final String SERVICE = "http://localhost:8888";
    private static final String SERVICE = "http://10.0.10.13";
    private static Logger jlog = LoggerFactory.getLogger(PiwikFilter.class);
    public static boolean ENABLED = false;
    private Map<String, String> customVars;
    @Autowired
    private AuthenticationManagerIface controller;


    public PiwikFilter () {
//        controller = BeansFactory.getKustvaktContext()
//                .getAuthenticationManager();
        ClientConfig config = new DefaultClientConfig();
        Client client = Client.create(config);
        if (jlog.isDebugEnabled())
            client.addFilter(new LoggingFilter());
        UriBuilder b = UriBuilder.fromUri(SERVICE);
        service = client.resource(b.build());
        this.customVars = new HashMap<>();
    }


    private void send (ContainerRequest request) {
        Random random = new SecureRandom();
        MultivaluedMap<String, String> params = new MultivaluedMapImpl();
        params.add("idsite", "2");
        params.add("rec", "1");
        if (!customVars.isEmpty())
            params.add("_cvar", translateCustomData());
        params.add("cip", request.getHeaderValue("Host"));
        params.add("cookie", "false");
        params.add("r", String.valueOf(random.nextDouble()));
        params.add("action_name", request.getRequestUri().toASCIIString());

        Locale l = null;
        if (request.getAcceptableLanguages() != null)
            l = request.getAcceptableLanguages().get(0);
        try {
            service.path("piwik/piwik.php")
                    .queryParam("idsite", "2")
                    .queryParam("rec", "1")
                    //todo check for empty container
                    .queryParam("_cvar", translateCustomData())
                    .queryParam("cip", request.getHeaderValue("Host"))
                    .queryParam("cookie", "false")
                    .queryParam("r", String.valueOf(random.nextDouble()))
                    .queryParam("action_name",
                            request.getRequestUri().toASCIIString())
                    .queryParams(params).accept("text/html")
                    .header("Host", request.getHeaderValue("Host"))
                    .header("User-Agent", request.getHeaderValue("User-Agent"))
                    .acceptLanguage(l).method("GET");
        }
        catch (Exception e) {
            // do nothing if piwik not available!
        }
    }


    private String translateCustomData () {
        final Map<String, List<String>> customVariables = new HashMap<String, List<String>>();
        int i = 0;
        for (final Map.Entry<String, String> entry : this.customVars.entrySet()) {
            i++;
            final List<String> list = new ArrayList<String>();
            list.add(entry.getKey());
            list.add(entry.getValue());
            customVariables.put(Integer.toString(i), list);
        }

        final JSONArray json = new JSONArray();
        json.add(customVariables);

        // remove unnecessary parent square brackets from JSON-string
        String jsonString = json.toString().substring(1,
                json.toString().length() - 1);
        customVars.clear();
        return jsonString;
    }


    @Override
    public ContainerRequest filter (ContainerRequest request) {
        if (ENABLED) {
            try {
                TokenContext context = (TokenContext) request
                        .getUserPrincipal();
                // since this is cached, not very expensive!
                User user = controller.getUser(context.getUsername());
                Userdata data = controller
                        .getUserData(user, UserSettings.class);
                if ((Boolean) data.get(Attributes.COLLECT_AUDITING_DATA))
                    customVars.put("username", context.getUsername());
            }
            catch (KustvaktException | UnsupportedOperationException e) {
                //do nothing
            }
            send(request);
        }
        return request;
    }


    @Override
    public ContainerRequestFilter getRequestFilter () {
        return this;
    }


    @Override
    public ContainerResponseFilter getResponseFilter () {
        return null;
    }
}
