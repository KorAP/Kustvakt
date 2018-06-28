package de.ids_mannheim.korap.web;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.apache.http.HttpHeaders;
import org.springframework.stereotype.Service;

import com.nimbusds.oauth2.sdk.AccessTokenResponse;
import com.nimbusds.oauth2.sdk.AuthorizationErrorResponse;
import com.nimbusds.oauth2.sdk.ErrorObject;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.ResponseMode;
import com.nimbusds.oauth2.sdk.TokenErrorResponse;
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
        tokenErrorObjectMap.put(OAuth2Error.INSUFFICIENT_SCOPE,
                BearerTokenError.INSUFFICIENT_SCOPE);
        tokenErrorObjectMap.put(OAuth2Error.INVALID_TOKEN,
                BearerTokenError.INVALID_TOKEN);
        tokenErrorObjectMap.put(OAuth2Error.INVALID_REQUEST,
                BearerTokenError.INVALID_REQUEST);
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
            // if (e.getStatusCode()
            // .equals(StatusCodes.USER_REAUTHENTICATION_REQUIRED)) {
            // return Response.status(HttpStatus.SC_UNAUTHORIZED)
            // .entity(e.getMessage()).build();
            // }

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
            if (errorCode != null && !errorCode.isEmpty()
                    && !errorCode.equals("[]")) {
                errorObject = new ErrorObject(e.getEntity(), e.getMessage());
            }
            else {
                throw throwit(e);
            }
        }
        return errorObject;
    }

    public Response createErrorResponse (ParseException e, State state) {
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

    public Response createTokenErrorResponse (KustvaktException e) {

        String errorCode = e.getEntity();
        ErrorObject errorObject = tokenErrorObjectMap.get(errorCode);
        if (errorObject == null) {
            errorObject = errorObjectMap.get(errorCode);
            if (errorCode != null && !errorCode.isEmpty()
                    && !errorCode.equals("[]")) {
                errorObject = new ErrorObject(e.getEntity(), e.getMessage());
            }
            else {
                throw throwit(e);
            }
        }

        errorObject = errorObject.setDescription(e.getMessage());
        TokenErrorResponse errorResponse = new TokenErrorResponse(errorObject);
        Status status = determineErrorStatus(errorCode);
        return createResponse(errorResponse, status);
    }

    public Response createResponse (AccessTokenResponse tokenResponse,
            Status status) {
        String jsonString = tokenResponse.toJSONObject().toJSONString();
        return createResponse(status, jsonString);
    }

    public Response createResponse (TokenErrorResponse tokenResponse,
            Status status) {
        String jsonString = tokenResponse.toJSONObject().toJSONString();
        return createResponse(status, jsonString);
    }

    private Response createResponse (Status status, Object entity) {
        ResponseBuilder builder = Response.status(status);
        builder.entity(entity);
        builder.header(HttpHeaders.CACHE_CONTROL, "no-store");
        builder.header(HttpHeaders.PRAGMA, "no-store");

        if (status == Status.UNAUTHORIZED) {
            builder.header(HttpHeaders.WWW_AUTHENTICATE,
                    "Basic realm=\"Kustvakt\"");
        }
        return builder.build();
    }

    private Status determineErrorStatus (String errorCode) {
        Status status = Status.BAD_REQUEST;
        if (errorCode.equals(OAuth2Error.INVALID_CLIENT)
                || errorCode.equals(OAuth2Error.UNAUTHORIZED_CLIENT)
                || errorCode.equals(OAuth2Error.INVALID_TOKEN)) {
            status = Status.UNAUTHORIZED;
        }
        else if (errorCode.equals(OAuth2Error.INVALID_GRANT)
                || errorCode.equals(OAuth2Error.INVALID_REQUEST)
                || errorCode.equals(OAuth2Error.INVALID_SCOPE)
                || errorCode.equals(OAuth2Error.UNSUPPORTED_GRANT_TYPE)
                || errorCode.equals(OAuth2Error.UNSUPPORTED_RESPONSE_TYPE)
                || errorCode.equals(OAuth2Error.ACCESS_DENIED)) {
            status = Status.BAD_REQUEST;
        }
        else if (errorCode.equals(OAuth2Error.INSUFFICIENT_SCOPE)) {
            status = Status.FORBIDDEN;
        }
        else if (errorCode.equals(OAuth2Error.SERVER_ERROR)) {
            status = Status.INTERNAL_SERVER_ERROR;
        }
        else if (errorCode.equals(OAuth2Error.TEMPORARILY_UNAVAILABLE)) {
            status = Status.SERVICE_UNAVAILABLE;
        }
        return status;
    }
}
