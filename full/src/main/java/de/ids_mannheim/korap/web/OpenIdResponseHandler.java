package de.ids_mannheim.korap.web;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.springframework.stereotype.Service;

import com.nimbusds.oauth2.sdk.AuthorizationErrorResponse;
import com.nimbusds.oauth2.sdk.ErrorObject;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.ResponseMode;
import com.nimbusds.oauth2.sdk.id.State;
import com.nimbusds.oauth2.sdk.token.BearerTokenError;
import com.nimbusds.openid.connect.sdk.AuthenticationErrorResponse;

import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.interfaces.db.AuditingIface;
import de.ids_mannheim.korap.oauth2.constant.OAuth2Error;
import net.minidev.json.JSONObject;

@Service
public class OpenIdResponseHandler extends KustvaktResponseHandler {

    final static Map<String, ErrorObject> errorObjectMap = new HashMap<>();
    {
        errorObjectMap.put(OAuth2Error.ACCESS_DENIED,
                com.nimbusds.oauth2.sdk.OAuth2Error.ACCESS_DENIED);
        errorObjectMap.put(OAuth2Error.INVALID_CLIENT,
                com.nimbusds.oauth2.sdk.OAuth2Error.INVALID_CLIENT);
        errorObjectMap.put(OAuth2Error.INVALID_GRANT,
                com.nimbusds.oauth2.sdk.OAuth2Error.INVALID_GRANT);
        errorObjectMap.put(OAuth2Error.INVALID_REQUEST,
                com.nimbusds.oauth2.sdk.OAuth2Error.INVALID_REQUEST);
        errorObjectMap.put(OAuth2Error.INVALID_SCOPE,
                com.nimbusds.oauth2.sdk.OAuth2Error.INVALID_SCOPE);
        errorObjectMap.put(OAuth2Error.SERVER_ERROR,
                com.nimbusds.oauth2.sdk.OAuth2Error.SERVER_ERROR);
        errorObjectMap.put(OAuth2Error.TEMPORARILY_UNAVAILABLE,
                com.nimbusds.oauth2.sdk.OAuth2Error.TEMPORARILY_UNAVAILABLE);
        errorObjectMap.put(OAuth2Error.UNAUTHORIZED_CLIENT,
                com.nimbusds.oauth2.sdk.OAuth2Error.UNAUTHORIZED_CLIENT);
        errorObjectMap.put(OAuth2Error.UNSUPPORTED_GRANT_TYPE,
                com.nimbusds.oauth2.sdk.OAuth2Error.UNSUPPORTED_GRANT_TYPE);
        errorObjectMap.put(OAuth2Error.UNSUPPORTED_RESPONSE_TYPE,
                com.nimbusds.oauth2.sdk.OAuth2Error.UNSUPPORTED_RESPONSE_TYPE);
    }

    final static Map<String, ErrorObject> tokenErrorObjectMap = new HashMap<>();
    {
        errorObjectMap.put(OAuth2Error.INSUFFICIENT_SCOPE,
                BearerTokenError.INSUFFICIENT_SCOPE);
        errorObjectMap.put(OAuth2Error.INVALID_TOKEN,
                BearerTokenError.INVALID_TOKEN);
    }

    public OpenIdResponseHandler (AuditingIface iface) {
        super(iface);
    }

    /**
     * According to OpenID connect core 1.0 specification, all
     * authentication errors must be represented through
     * AuthenticationErrorResponse. Moreover, for authorization code
     * flow, the error response parameters must be added to the
     * redirect URI as query parameters, unless a different response
     * mode was specified.
     * 
     * {@link AuthorizationErrorResponse} defines specific
     * {@link ErrorObject}s regarding OAUTH2 errors.
     * {@link AuthenticationErrorResponse} defines additional
     * ErrorObjects regarding OpenID connect authenticaition errors.
     * 
     * @param e
     *            a {@link KustvaktException}
     * @param isAuthentication
     * @param redirectURI
     * @param state
     * @param responseMode
     * @return a redirect uri with error response parameters as part
     *         of query parameters
     */
    public Response createAuthorizationErrorResponse (KustvaktException e,
            boolean isAuthentication, URI redirectURI, State state,
            ResponseMode responseMode) {

        ErrorObject errorObject = createErrorObject(e);
        errorObject = errorObject.setDescription(e.getMessage());
        if (redirectURI == null) {
            return Response.status(errorObject.getHTTPStatusCode())
                    .entity(errorObject.toJSONObject()).build();
        }

        URI uri = null;
        if (isAuthentication) {
            uri = new AuthenticationErrorResponse(redirectURI, errorObject,
                    state, responseMode).toURI();
        }
        else {
            uri = new AuthorizationErrorResponse(redirectURI, errorObject,
                    state, responseMode).toURI();
        }

        ResponseBuilder builder = Response.temporaryRedirect(uri)
                .type(MediaType.APPLICATION_FORM_URLENCODED);
        return builder.build();
    }

    private ErrorObject createErrorObject (KustvaktException e) {
        String errorCode = e.getEntity();

        ErrorObject errorObject = errorObjectMap.get(errorCode);
        if (errorObject == null) {
            errorObject = new ErrorObject(e.getEntity(), e.getMessage());
        }
        return errorObject;
    }

    public Response createAuthorizationErrorResponse (ParseException e,
            boolean isAuthentication, State state) {
        ErrorObject errorObject = e.getErrorObject();
        if (errorObject == null) {
            errorObject = com.nimbusds.oauth2.sdk.OAuth2Error.INVALID_REQUEST;
            if (e.getMessage() != null) {
                errorObject = errorObject.setDescription(e.getMessage());
            }
        }

        JSONObject json = errorObject.toJSONObject();
        if (state != null) {
            json.put("state", state.getValue());
        }

        return Response.status(errorObject.getHTTPStatusCode()).entity(json)
                .build();

    }

}
