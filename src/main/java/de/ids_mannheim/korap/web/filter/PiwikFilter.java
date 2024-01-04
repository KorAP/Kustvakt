package de.ids_mannheim.korap.web.filter;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.logging.LoggingFeature;
import org.springframework.beans.factory.annotation.Autowired;

import de.ids_mannheim.korap.authentication.AuthenticationManager;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.UriBuilder;
import net.minidev.json.JSONArray;

/**
 * @author hanl
 * @date 13/05/2014
 */
@Deprecated
//@Component
//@Priority(Priorities.AUTHORIZATION)
public class PiwikFilter implements ContainerRequestFilter {

    private WebTarget service;
    //    private static final String SERVICE = "http://localhost:8888";
    private static final String SERVICE = "http://10.0.10.13";
    private static Logger jlog = LogManager.getLogger(PiwikFilter.class);
    public static boolean ENABLED = false;
    private Map<String, String> customVars;
    @Autowired
    private AuthenticationManager authenticationManager;

    public PiwikFilter () {
        //        controller = BeansFactory.getKustvaktContext()
        //                .getAuthenticationManager();
        ClientConfig clientConfig = new ClientConfig();
        if (jlog.isDebugEnabled())
            clientConfig.register(LoggingFeature.class);
        Client client = ClientBuilder.newClient(clientConfig);
        UriBuilder b = UriBuilder.fromUri(SERVICE);
        service = client.target(b.build());
        this.customVars = new HashMap<>();
    }

    private void send (ContainerRequestContext request) {
        Random random = new SecureRandom();
        Locale l = null;
        if (request.getAcceptableLanguages() != null)
            l = request.getAcceptableLanguages().get(0);
        try {
            service.path("piwik/piwik.php").queryParam("idsite", "2")
                    .queryParam("rec", "1")
                    //todo check for empty container
                    .queryParam("_cvar", translateCustomData())
                    .queryParam("cip", request.getHeaderString("Host"))
                    .queryParam("cookie", "false")
                    .queryParam("r", String.valueOf(random.nextDouble()))
                    .queryParam("action_name",
                            request.getUriInfo().getRequestUri()
                                    .toASCIIString())
                    .request().accept("text/html")
                    .header("Host", request.getHeaderString("Host"))
                    .header("User-Agent", request.getHeaderString("User-Agent"))
                    .acceptLanguage(l).method("GET");
        }
        catch (Exception e) {
            // do nothing if piwik not available!
        }
    }

    private String translateCustomData () {
        final Map<String, List<String>> customVariables = new HashMap<String, List<String>>();
        int i = 0;
        for (final Map.Entry<String, String> entry : this.customVars
                .entrySet()) {
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
    public void filter (ContainerRequestContext request) {
        if (ENABLED) {
            //            try {
            //                TokenContext context;
            //                SecurityContext securityContext = request.getSecurityContext();
            //                if (securityContext != null) {
            //                    context = (TokenContext) securityContext.getUserPrincipal();
            //
            //                    if (context.getUsername() != null){
            //                        // since this is cached, not very expensive!
            //                        User user = authenticationManager.getUser(context.getUsername());
            //                        Userdata data = authenticationManager
            //                                .getUserData(user, UserSettingProcessor.class);
            //                        if ((Boolean) data.get(Attributes.COLLECT_AUDITING_DATA))
            //                            customVars.put("username", context.getUsername());
            //                    }
            //                }
            //            }
            //            catch (KustvaktException e) {
            //                //do nothing
            //            }
            send(request);
        }
    }
}
