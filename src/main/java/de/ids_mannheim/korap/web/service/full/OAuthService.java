package de.ids_mannheim.korap.web.service.full;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ResourceFilters;
import de.ids_mannheim.korap.config.*;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.handlers.OAuth2Handler;
import de.ids_mannheim.korap.interfaces.AuthenticationManagerIface;
import de.ids_mannheim.korap.interfaces.EncryptionIface;
import de.ids_mannheim.korap.user.*;
import de.ids_mannheim.korap.utils.JsonUtils;
import de.ids_mannheim.korap.utils.StringUtils;
import de.ids_mannheim.korap.web.KustvaktServer;
import de.ids_mannheim.korap.web.filter.AuthFilter;
import de.ids_mannheim.korap.web.filter.BlockingFilter;
import de.ids_mannheim.korap.web.filter.DefaultFilter;
import de.ids_mannheim.korap.web.filter.PiwikFilter;
import de.ids_mannheim.korap.web.utils.FormRequestWrapper;
import de.ids_mannheim.korap.web.utils.KustvaktResponseHandler;
import org.apache.oltu.oauth2.as.issuer.MD5Generator;
import org.apache.oltu.oauth2.as.issuer.OAuthIssuer;
import org.apache.oltu.oauth2.as.issuer.OAuthIssuerImpl;
import org.apache.oltu.oauth2.as.request.OAuthAuthzRequest;
import org.apache.oltu.oauth2.as.request.OAuthTokenRequest;
import org.apache.oltu.oauth2.as.response.OAuthASResponse;
import org.apache.oltu.oauth2.common.OAuth;
import org.apache.oltu.oauth2.common.error.OAuthError;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.OAuthResponse;
import org.apache.oltu.oauth2.common.message.types.GrantType;
import org.apache.oltu.oauth2.common.message.types.ResponseType;
import org.apache.oltu.oauth2.common.message.types.TokenType;
import org.apache.oltu.oauth2.common.utils.OAuthUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author hanl
 * @date 07/06/2014
 */
//todo: only allow oauth2 access_token requests GET methods?
//todo: allow refresh tokens
@Path(KustvaktServer.API_VERSION + "/oauth2")
//@ResourceFilters({ AccessLevelFilter.class, PiwikFilter.class })
public class OAuthService {

    private OAuth2Handler handler;
    private AuthenticationManagerIface controller;
    private EncryptionIface crypto;
    private KustvaktConfiguration config;

    public OAuthService() {
        this.handler = new OAuth2Handler(
                BeanConfiguration.getBeans().getPersistenceClient());
        this.controller = BeanConfiguration.getBeans()
                .getAuthenticationManager();
        this.crypto = BeanConfiguration.getBeans().getEncryption();
        this.config = BeanConfiguration.getBeans().getConfiguration();
    }

    @POST
    @Path("unregister")
    @ResourceFilters({ AuthFilter.class, BlockingFilter.class })
    public Response unregisterClient(@Context SecurityContext context,
            @HeaderParam("Host") String host,
            @QueryParam("client_secret") String secret,
            @QueryParam("client_id") String client_id) {
        ClientInfo info = new ClientInfo(client_id, secret);
        info.setUrl(host);
        TokenContext ctx = (TokenContext) context.getUserPrincipal();
        try {
            this.handler.removeClient(info,
                    this.controller.getUser(ctx.getUsername()));
        }catch (KustvaktException e) {
            throw KustvaktResponseHandler.throwit(e);
        }
        return Response.ok().build();
    }

    @POST
    @Path("register")
    @ResourceFilters({ AuthFilter.class, BlockingFilter.class })
    public Response registerClient(@Context SecurityContext context,
            @HeaderParam("Host") String host,
            @QueryParam("redirect_url") String rurl) {
        ClientInfo info = new ClientInfo(crypto.createID(),
                crypto.createToken());
        info.setUrl(host);
        if (rurl == null)
            throw KustvaktResponseHandler
                    .throwit(StatusCodes.ILLEGAL_ARGUMENT, "Missing parameter!",
                            "redirect_url");
        info.setRedirect_uri(rurl);
        TokenContext ctx = (TokenContext) context.getUserPrincipal();
        try {
            this.handler.registerClient(info,
                    this.controller.getUser(ctx.getUsername()));
        }catch (KustvaktException e) {
            throw KustvaktResponseHandler.throwit(e);
        }
        return Response.ok(info.toJSON()).build();
    }

    @GET
    @Path("info")
    @ResourceFilters({ AuthFilter.class, DefaultFilter.class,
            PiwikFilter.class })
    public Response getStatus(@Context SecurityContext context,
            @QueryParam("scope") String scopes) {
        TokenContext ctx = (TokenContext) context.getUserPrincipal();
        User user;
        try {
            user = this.controller.getUser(ctx.getUsername());
            Userdata data = this.controller
                    .getUserData(user, Userdetails2.class);
            user.addUserData(data);
            Set<String> base_scope = StringUtils.toSet(scopes, " ");
            base_scope.retainAll(StringUtils.toSet(scopes));
            scopes = StringUtils.toString(base_scope);
        }catch (KustvaktException e) {
            throw KustvaktResponseHandler.throwit(e);
        }
        // json format with scope callback parameter
        // todo: add other scopes as well!
        return Response.ok(JsonUtils
                .toJSON(Scopes.mapScopes(scopes, user.getDetails()))).build();
    }

    @GET
    @Path("authorizations")
    @ResourceFilters({ AuthFilter.class, BlockingFilter.class })
    public Response getAuthorizations(@Context SecurityContext context,
            @HeaderParam(ContainerRequest.USER_AGENT) String agent,
            @HeaderParam(ContainerRequest.HOST) String host) {
        // works on all tokens, but access to native apps cannot be revoked!
        // check secret and id and retrieve access tokens
        TokenContext ctx = (TokenContext) context.getUserPrincipal();
        try {
            User user = this.controller.getUser(ctx.getUsername());
            Collection auths = this.handler.getAuthorizedClients(user.getId());
            if (auths.isEmpty())
                return Response.noContent().build();
            return Response.ok(JsonUtils.toJSON(auths)).build();
        }catch (KustvaktException e) {
            throw KustvaktResponseHandler.throwit(e);
        }
    }

    // todo: scopes for access_token are defined here
    // todo: if user already has an access token registered for client and application, then redirect to token endpoint to retrieve that token
    // todo: demo account should be disabled for this function --> if authentication failed, client must redirect to login url (little login window)
    @POST
    @Path("authorize")
    @Consumes("application/x-www-form-urlencoded")
    @Produces("application/json")
    @ResourceFilters({ BlockingFilter.class })
    public Response authorize(@Context HttpServletRequest request,
            @Context SecurityContext context,
            @HeaderParam(ContainerRequest.USER_AGENT) String agent,
            @HeaderParam(ContainerRequest.HOST) String host,
            MultivaluedMap<String, String> form)
            throws OAuthSystemException, URISyntaxException {
        // user needs to be authenticated to this service!
        TokenContext c = (TokenContext) context.getUserPrincipal();

        try {
            OAuthAuthzRequest oauthRequest = new OAuthAuthzRequest(
                    new FormRequestWrapper(request, form));
            OAuthIssuerImpl oauthIssuerImpl = new OAuthIssuerImpl(
                    new MD5Generator());
            User user;

            Map<String, String> attr = new HashMap<>();
            attr.put(Attributes.HOST, host);
            attr.put(Attributes.USER_AGENT, agent);
            attr.put(Attributes.USERNAME, c.getUsername());
            // also extractable via authorization header
            attr.put(Attributes.CLIENT_ID, oauthRequest.getClientId());
            attr.put(Attributes.CLIENT_SECRET, oauthRequest.getClientSecret());
            StringBuilder scopes = new StringBuilder();
            for (String scope : oauthRequest.getScopes())
                scopes.append(scope + " ");
            attr.put(Attributes.SCOPES, scopes.toString());

            try {
                user = controller.getUser(c.getUsername());
                Userdata data = controller
                        .getUserData(user, Userdetails2.class);
                user.addUserData(data);
            }catch (KustvaktException e) {
                throw KustvaktResponseHandler.throwit(e);
            }

            // register response according to response_type
            String responseType = oauthRequest
                    .getParam(OAuth.OAUTH_RESPONSE_TYPE);

            final String authorizationCode = oauthIssuerImpl
                    .authorizationCode();
            ClientInfo info = this.handler
                    .getClient(oauthRequest.getClientId());

            if (info == null || !info.getClient_secret()
                    .equals(oauthRequest.getClientSecret())) {
                OAuthResponse res = OAuthASResponse
                        .errorResponse(HttpServletResponse.SC_BAD_REQUEST)
                        .setError(OAuthError.CodeResponse.UNAUTHORIZED_CLIENT)
                        .setErrorDescription("Unauthorized client!\n")
                        .buildJSONMessage();
                return Response.status(res.getResponseStatus())
                        .entity(res.getBody()).build();
            }

            if (!info.getRedirect_uri()
                    .contains(oauthRequest.getRedirectURI())) {
                OAuthResponse res = OAuthASResponse
                        .errorResponse(HttpServletResponse.SC_BAD_REQUEST)
                        .setError(OAuthError.CodeResponse.INVALID_REQUEST)
                        .setErrorDescription("Unauthorized redirect!\n")
                        .buildJSONMessage();
                return Response.status(res.getResponseStatus())
                        .entity(res.getBody()).build();
            }

            String accessToken = this.handler
                    .getToken(oauthRequest.getClientId(), user.getId());

            //todo: test correct redirect and parameters
            if (accessToken != null) {
                // fixme: correct status code?
                OAuthASResponse.OAuthResponseBuilder builder = OAuthASResponse
                        .status(HttpServletResponse.SC_FOUND);
                final OAuthResponse response = builder.location("/oauth2/token")
                        .setParam(OAuth.OAUTH_CLIENT_ID,
                                oauthRequest.getClientId())
                        .setParam(OAuth.OAUTH_CLIENT_SECRET,
                                oauthRequest.getClientSecret())
                        .buildQueryMessage();
                return Response.status(response.getResponseStatus())
                        .location(new URI(response.getLocationUri())).build();
            }

            final OAuthResponse response;
            String redirectURI = oauthRequest.getRedirectURI();
            if (OAuthUtils.isEmpty(redirectURI)) {
                throw new WebApplicationException(
                        Response.status(HttpServletResponse.SC_BAD_REQUEST)
                                .entity("OAuth callback url needs to be provided by client!!!\n")
                                .build());
            }

            if (responseType.equals(ResponseType.CODE.toString())) {
                OAuthASResponse.OAuthAuthorizationResponseBuilder builder = OAuthASResponse
                        .authorizationResponse(request,
                                HttpServletResponse.SC_FOUND);
                builder.location(redirectURI);

                try {
                    AuthCodeInfo codeInfo = new AuthCodeInfo(
                            info.getClient_id(), authorizationCode);
                    codeInfo.setScopes(StringUtils
                            .toString(oauthRequest.getScopes(), " "));
                    this.handler.authorize(codeInfo, user);
                }catch (KustvaktException e) {
                    throw KustvaktResponseHandler.throwit(e);
                }
                builder.setParam(OAuth.OAUTH_RESPONSE_TYPE,
                        ResponseType.CODE.toString());
                builder.setCode(authorizationCode);
                response = builder.buildBodyMessage();

            }else if (responseType.contains(ResponseType.TOKEN.toString())) {
                OAuthASResponse.OAuthTokenResponseBuilder builder = OAuthASResponse
                        .tokenResponse(HttpServletResponse.SC_OK);
                builder.setParam(OAuth.OAUTH_RESPONSE_TYPE,
                        ResponseType.TOKEN.toString());
                builder.location(redirectURI);

                String token = oauthIssuerImpl.accessToken();
                String refresh = oauthIssuerImpl.refreshToken();

                this.handler.addToken(token, refresh, user.getId(),
                        oauthRequest.getClientId(),
                        StringUtils.toString(oauthRequest.getScopes(), " "),
                        config.getLongTokenTTL());
                builder.setAccessToken(token);
                builder.setRefreshToken(refresh);
                builder.setExpiresIn(String.valueOf(config.getLongTokenTTL()));

                // skips authorization code type and returns id_token and access token directly
                if (oauthRequest.getScopes().contains("openid")) {
                    try {
                        TokenContext new_context = this.controller
                                .createTokenContext(user, attr, null);
                        builder.setParam(new_context.getTokenType(),
                                new_context.getToken());
                    }catch (KustvaktException e) {
                        throw KustvaktResponseHandler.throwit(e);
                    }
                }
                response = builder.buildBodyMessage();
            }else {
                OAuthResponse res = OAuthASResponse
                        .errorResponse(HttpServletResponse.SC_BAD_REQUEST)
                        .setError(
                                OAuthError.CodeResponse.UNSUPPORTED_RESPONSE_TYPE)
                        .setErrorDescription("Unsupported Response type!\n")
                        .buildJSONMessage();
                return Response.status(res.getResponseStatus())
                        .entity(res.getBody()).build();
            }
            //
            //            String redirectURI = oauthRequest.getRedirectURI();
            //
            //            // enables state parameter to disable cross-site scripting attacks
            //            final OAuthResponse response = builder.location(redirectURI)
            //                    .buildQueryMessage();
            //            if (OAuthUtils.isEmpty(redirectURI)) {
            //                throw new WebApplicationException(
            //                        Response.status(HttpServletResponse.SC_BAD_REQUEST)
            //                                .entity("OAuth callback url needs to be provided by client!!!\n")
            //                                .build());
            //            }

            return Response.status(response.getResponseStatus())
                    .location(new URI(response.getLocationUri())).build();
        }catch (OAuthProblemException e) {
            final Response.ResponseBuilder responseBuilder = Response
                    .status(HttpServletResponse.SC_BAD_REQUEST);
            String redirectUri = e.getRedirectUri();

            if (OAuthUtils.isEmpty(redirectUri))
                throw new WebApplicationException(responseBuilder
                        .entity("OAuth callback url needs to be provided by client!!!\n")
                        .build());

            final OAuthResponse response = OAuthASResponse
                    .errorResponse(HttpServletResponse.SC_BAD_REQUEST).error(e)
                    .location(redirectUri).buildQueryMessage();
            final URI location = new URI(response.getLocationUri());
            return responseBuilder.location(location).build();
        }catch (OAuthSystemException | URISyntaxException | KustvaktException e) {
            e.printStackTrace();
        }
        return Response.noContent().build();
    }

    @POST
    @Path("revoke")
    public Response revokeToken(@Context HttpServletRequest request,
            @Context SecurityContext context,
            @HeaderParam(ContainerRequest.USER_AGENT) String agent,
            @HeaderParam(ContainerRequest.HOST) String host)
            throws OAuthSystemException, URISyntaxException {
        TokenContext ctx = (TokenContext) context.getUserPrincipal();
        try {

            if (!this.handler.revokeToken(ctx.getToken())) {
                OAuthResponse res = OAuthASResponse
                        .errorResponse(HttpServletResponse.SC_BAD_REQUEST)
                        .setError(OAuthError.TokenResponse.UNAUTHORIZED_CLIENT)
                        .setErrorDescription("Invalid access token!\n")
                        .buildJSONMessage();
                return Response.status(res.getResponseStatus())
                        .entity(res.getBody()).build();
            }

        }catch (KustvaktException e) {
            e.printStackTrace();
            // fixme: do something
            /**
             * final Response.ResponseBuilder responseBuilder = Response
             .status(HttpServletResponse.SC_FOUND);
             String redirectUri = e.getRedirectUri();

             final OAuthResponse response = OAuthASResponse
             .errorResponse(HttpServletResponse.SC_FOUND).error(e)
             .location(redirectUri).buildQueryMessage();
             final URI location = new URI(response.getLocationUri());
             return responseBuilder.location(location).build();
             */
        }

        return Response.ok().build();
    }

    @POST
    @Consumes("application/x-www-form-urlencoded")
    @Produces("application/json")
    @Path("token")
    public Response requestToken(@Context HttpServletRequest request,
            @HeaderParam(ContainerRequest.USER_AGENT) String agent,
            @HeaderParam(ContainerRequest.HOST) String host,
            MultivaluedMap form) throws OAuthSystemException {
        boolean openid_valid = false;
        User user = null;
        OAuthTokenRequest oauthRequest;
        OAuthASResponse.OAuthTokenResponseBuilder builder = OAuthASResponse
                .tokenResponse(HttpServletResponse.SC_OK);

        OAuthIssuer oauthIssuerImpl = new OAuthIssuerImpl(new MD5Generator());
        ClientInfo info;
        try {
            oauthRequest = new OAuthTokenRequest(
                    new FormRequestWrapper(request, form));

            if ((info = this.handler.getClient(oauthRequest.getClientId()))
                    == null) {
                OAuthResponse res = OAuthASResponse
                        .errorResponse(HttpServletResponse.SC_BAD_REQUEST)
                        .setError(OAuthError.TokenResponse.INVALID_CLIENT)
                        .setErrorDescription("Invalid client id!\n")
                        .buildJSONMessage();
                return Response.status(res.getResponseStatus())
                        .entity(res.getBody()).build();
            }else if (!info.getClient_secret()
                    .equals(oauthRequest.getClientSecret())) {
                OAuthResponse res = OAuthASResponse
                        .errorResponse(HttpServletResponse.SC_UNAUTHORIZED)
                        .setError(OAuthError.TokenResponse.UNAUTHORIZED_CLIENT)
                        .setErrorDescription("Invalid client secret!\n")
                        .buildJSONMessage();
                return Response.status(res.getResponseStatus())
                        .entity(res.getBody()).build();
            }

            Map<String, String> attr = new HashMap<>();
            attr.put(Attributes.HOST, host);
            attr.put(Attributes.USER_AGENT, agent);
            attr.put(Attributes.SCOPES,
                    StringUtils.toString(oauthRequest.getScopes(), " "));

            // support code (for external clients only) and password grant type
            // password grant at this point is only allowed with trusted clients (korap frontend)
            if (oauthRequest.getGrantType().equalsIgnoreCase(
                    GrantType.AUTHORIZATION_CODE.toString())) {
                // validate auth code
                AuthCodeInfo codeInfo;
                try {
                    //can this be joined with the simple retrieval of access tokens?
                    // partially yes: auth code can be valid, even though no access token exists
                    // --> zero result set
                    codeInfo = this.handler
                            .getAuthorization(oauthRequest.getCode());
                    if (codeInfo == null) {
                        OAuthResponse res = OAuthASResponse.errorResponse(
                                HttpServletResponse.SC_UNAUTHORIZED).setError(
                                OAuthError.TokenResponse.INVALID_REQUEST)
                                .setErrorDescription(
                                        "Invalid authorization code\n")
                                .buildJSONMessage();
                        return Response.status(res.getResponseStatus())
                                .entity(res.getBody()).build();
                    }else {
                        openid_valid = codeInfo.getScopes().contains("openid");
                        String accessToken = oauthIssuerImpl.accessToken();
                        String refreshToken = oauthIssuerImpl.refreshToken();
                        // auth code posesses the user reference. native apps access_tokens are directly associated with the user
                        this.handler
                                .addToken(oauthRequest.getCode(), accessToken,
                                        refreshToken, config.getTokenTTL());

                        builder.setTokenType(TokenType.BEARER.toString());
                        builder.setExpiresIn(
                                String.valueOf(config.getLongTokenTTL()));
                        builder.setAccessToken(accessToken);
                        builder.setRefreshToken(refreshToken);
                    }
                }catch (KustvaktException e) {
                    throw KustvaktResponseHandler.throwit(e);
                }
                // todo: errors for invalid scopes or different scopes then during authorization request?
                //todo ??
                attr.put(Attributes.SCOPES, codeInfo.getScopes());

            }else if (oauthRequest.getGrantType()
                    .equalsIgnoreCase(GrantType.PASSWORD.toString())) {
                //fixme: via https; as basic auth header and only if client is native!
                if (!info.isConfidential()) {
                    OAuthResponse res = OAuthASResponse
                            .errorResponse(HttpServletResponse.SC_BAD_REQUEST)
                            .setError(
                                    OAuthError.TokenResponse.UNAUTHORIZED_CLIENT)
                            .setErrorDescription(
                                    "Grant type not supported for client!\n")
                            .buildJSONMessage();
                    return Response.status(res.getResponseStatus())
                            .entity(res.getBody()).build();
                }

                openid_valid = true;
                try {
                    user = controller
                            .authenticate(0, oauthRequest.getUsername(),
                                    oauthRequest.getPassword(), attr);
                }catch (KustvaktException e) {
                    throw KustvaktResponseHandler.throwit(e);
                }

                try {
                    String accessToken = this.handler
                            .getToken(oauthRequest.getClientId(), user.getId());
                    if (accessToken == null) {
                        String refresh = oauthIssuerImpl.refreshToken();
                        accessToken = oauthIssuerImpl.accessToken();
                        this.handler
                                .addToken(accessToken, refresh, user.getId(),
                                        oauthRequest.getClientId(), StringUtils
                                                .toString(oauthRequest
                                                        .getScopes(), " "),
                                        config.getLongTokenTTL());
                        builder.setRefreshToken(refresh);
                    }
                    builder.setTokenType(TokenType.BEARER.toString());
                    builder.setExpiresIn(
                            String.valueOf(config.getLongTokenTTL()));
                    builder.setAccessToken(accessToken);

                }catch (KustvaktException e) {
                    throw KustvaktResponseHandler.throwit(e);
                }
            }

            if (openid_valid && oauthRequest.getScopes()
                    .contains(Scopes.Scope.openid.toString())) {
                try {
                    if (user == null)
                        user = controller
                                .authenticate(0, oauthRequest.getUsername(),
                                        oauthRequest.getPassword(), attr);
                    Userdata data = controller
                            .getUserData(user, Userdetails2.class);
                    user.addUserData(data);

                    attr.put(Attributes.CLIENT_SECRET,
                            oauthRequest.getClientSecret());
                    TokenContext c = controller.createTokenContext(user, attr,
                            Attributes.OPENID_AUTHENTICATION);
                    builder.setParam(c.getTokenType(), c.getToken());
                }catch (KustvaktException e) {
                    throw KustvaktResponseHandler.throwit(e);
                }
            }

            OAuthResponse r = builder.buildJSONMessage();
            return Response.status(r.getResponseStatus()).entity(r.getBody())
                    .build();
        }catch (OAuthProblemException ex) {
            OAuthResponse r = OAuthResponse.errorResponse(401).error(ex)
                    .buildJSONMessage();
            return Response.status(r.getResponseStatus()).entity(r.getBody())
                    .build();
        }catch (OAuthSystemException e) {
            e.printStackTrace();
            // todo: throw error
        }
        return Response.noContent().build();
    }

}
