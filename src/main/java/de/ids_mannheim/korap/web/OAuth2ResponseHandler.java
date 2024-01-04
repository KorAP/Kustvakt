package de.ids_mannheim.korap.web;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpHeaders;

import com.nimbusds.oauth2.sdk.AccessTokenResponse;
import com.nimbusds.oauth2.sdk.ErrorObject;
import com.nimbusds.oauth2.sdk.OAuth2Error;

import de.ids_mannheim.korap.exceptions.KustvaktException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import jakarta.ws.rs.core.Response.Status;

/**
 * OAuth2ResponseHandler builds {@link Response}s and handles
 * exceptions by building
 * OAuth error responses accordingly.
 * 
 * <br/><br/>
 * 
 * OAuth2 error response consists of error (required),
 * error_description (optional) and error_uri (optional).
 * 
 * @see OAuth2Error
 * 
 * @author margaretha
 *
 */
public class OAuth2ResponseHandler extends KustvaktResponseHandler {
    @Override
    public WebApplicationException throwit (KustvaktException e) {
        return throwit(e, null);
    }

    public WebApplicationException throwit (KustvaktException e, String state) {
        String errorCode = e.getEntity();
        int responseStatus = e.getResponseStatus();

        Response r = null;
        if (responseStatus > 0) {
            r = createResponse(e, Status.fromStatusCode(responseStatus), state);
        }
        else if (errorCode == null) {
            return super.throwit(e);
        }
        else if (errorCode.equals(OAuth2Error.INVALID_CLIENT.getCode())
                || errorCode.equals(OAuth2Error.UNAUTHORIZED_CLIENT.getCode())
                || errorCode.equals(
                        de.ids_mannheim.korap.oauth2.constant.OAuth2Error.INVALID_TOKEN)) {
            r = createResponse(e, Status.UNAUTHORIZED, state);
        }
        else if (errorCode.equals(OAuth2Error.INVALID_GRANT.getCode())
                || errorCode.equals(OAuth2Error.INVALID_REQUEST.getCode())
                || errorCode.equals(OAuth2Error.INVALID_SCOPE.getCode())
                || errorCode
                        .equals(OAuth2Error.UNSUPPORTED_GRANT_TYPE.getCode())
                || errorCode
                        .equals(OAuth2Error.UNSUPPORTED_RESPONSE_TYPE.getCode())
                || errorCode.equals(OAuth2Error.ACCESS_DENIED.getCode())) {
            r = createResponse(e, Status.BAD_REQUEST, state);
        }
        else if (errorCode.equals(
                de.ids_mannheim.korap.oauth2.constant.OAuth2Error.INSUFFICIENT_SCOPE)) {
            r = createResponse(e, Status.FORBIDDEN, state);
        }
        else if (errorCode.equals(OAuth2Error.SERVER_ERROR.getCode())) {
            r = createResponse(e, Status.INTERNAL_SERVER_ERROR, state);
        }
        else if (errorCode
                .equals(OAuth2Error.TEMPORARILY_UNAVAILABLE.getCode())) {
            r = createResponse(e, Status.SERVICE_UNAVAILABLE, state);
        }
        else {
            return super.throwit(e);
        }
        return new WebApplicationException(r);
    }

    public Response sendRedirect (URI locationUri) {
        ResponseBuilder builder = Response.temporaryRedirect(locationUri);
        return builder.build();
    }

    private Response createResponse (KustvaktException e, Status statusCode,
            String state) {
        ErrorObject eo = new ErrorObject(e.getEntity(), e.getMessage());
        if (state != null && !state.isEmpty()) {
            Map<String, String> map = new HashMap<String, String>();
            map.put("state", state);
            eo = eo.setCustomParams(map);
        }
        return createResponse(statusCode, eo.toJSONObject().toJSONString());
    }

    public Response createResponse (AccessTokenResponse tokenResponse) {
        String jsonString = tokenResponse.toJSONObject().toJSONString();
        return createResponse(Status.OK, jsonString);
    }

    private Response createResponse (Status status, String entity) {
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
}
