package de.ids_mannheim.korap.web.controller;

import java.net.URI;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.SecurityContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.openid.connect.sdk.AuthenticationRequest;
import com.sun.jersey.spi.container.ResourceFilters;

import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.oauth2.openid.service.OpenIdAuthorizationService;
import de.ids_mannheim.korap.security.context.TokenContext;
import de.ids_mannheim.korap.web.filter.AuthenticationFilter;
import de.ids_mannheim.korap.web.filter.BlockingFilter;
import de.ids_mannheim.korap.web.utils.MapUtils;

@Controller
@Path("/oauth2/openid")
public class OAuth2WithOpenIdController {

    @Autowired
    private OpenIdAuthorizationService authzService;

    /**
     * Required parameters for OpenID authentication requests:
     * 
     * <ul>
     * <li>scope: MUST contain "openid" for OpenID Connect
     * requests,</li>
     * <li>response_type,</li>
     * <li>client_id,</li>
     * <li>redirect_uri: MUST match a pre-registered redirect uri
     * during client registration.</li>
     * </ul>
     * 
     * Other parameters:
     * 
     * <ul>
     * <li>state (recommended): Opaque value used to maintain state between the request and the 
     * callback.</li>
     * <li>response_mode (optional) : mechanism to be used for returning parameters</li>
     * <li>nonce (optional): String value used to associate a Client session with an ID Token, 
     * and to mitigate replay attacks. </li>
     * <li>display (optional):  specifies how the Authorization Server displays the authentication 
     * and consent user interface pages</li>
     * <li>prompt (optional): specifies if the Authorization Server prompts the End-User 
     * for reauthentication and consent. Defined values: none, login, consent, select_account </li>
     * <li>max_age (optional): maximum Authentication Age.</li>
     * <li>ui_locales (optional): preferred languages and scripts for the user interface 
     * represented as a space-separated list of BCP47 [RFC5646] </li>
     * <li>id_token_hint (optional): ID Token previously issued by the Authorization Server 
     * being passed as a hint</li>
     * <li>login_hint (optional): hint to the Authorization Server about the login identifier 
     * the End-User might use to log in</li>
     * <li>acr_values (optional): requested Authentication Context Class Reference values. </li>
     * </ul>
     * 
     * @see OpenID Connect Core 1.0 specification
     * 
     * @param request
     * @param context
     * @param form
     * @return a redirect to client redirect uri
     */
    @POST
    @Path("authorize")
    @ResourceFilters({ AuthenticationFilter.class, BlockingFilter.class })
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response requestAuthorizationCode (
            @Context HttpServletRequest request,
            @Context SecurityContext context,
            MultivaluedMap<String, String> form) {

        Map<String, String> map = MapUtils.toMap(form);
        AuthenticationRequest authRequest = null;
        try {
            authRequest = AuthenticationRequest.parse(map);
        }
        catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }

        TokenContext tokenContext = (TokenContext) context.getUserPrincipal();
        String username = tokenContext.getUsername();

        URI uri = null;
        try {
            uri = authzService.requestAuthorizationCode(authRequest, username);
            // System.out.println(uri.toString());
        }
        catch (KustvaktException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        ResponseBuilder builder = Response.temporaryRedirect(uri);
        return builder.build();
    }
}
