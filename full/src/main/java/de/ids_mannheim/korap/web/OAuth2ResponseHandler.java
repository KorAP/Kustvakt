package de.ids_mannheim.korap.web;

import java.net.URI;
import java.net.URISyntaxException;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.oltu.oauth2.common.error.OAuthError;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.OAuthResponse;
import org.apache.oltu.oauth2.common.message.OAuthResponse.OAuthErrorResponseBuilder;

import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.interfaces.db.AuditingIface;
import de.ids_mannheim.korap.oauth2.constant.OAuth2Error;

/**
 * OAuth2ResponseHandler builds {@link Response}s from
 * {@link OAuthResponse}s and handles exceptions by building
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

    public OAuth2ResponseHandler (AuditingIface iface) {
        super(iface);
    }

    public WebApplicationException throwit (OAuthSystemException e) {
        return throwit(StatusCodes.OAUTH2_SYSTEM_ERROR, e.getMessage());
    }

    public WebApplicationException throwit (OAuthSystemException e,
            String state) {
        if (state != null && !state.isEmpty()) {
            return throwit(StatusCodes.OAUTH2_SYSTEM_ERROR, e.getMessage(),
                    "state=" + state);
        }
        return throwit(e);
    }

    public WebApplicationException throwit (OAuthProblemException e) {
        OAuthResponse oAuthResponse = null;
        String state = e.getState();
        try {
            OAuthErrorResponseBuilder builder =
                    OAuthResponse.errorResponse(e.getResponseStatus()).error(e);
             if (state != null && !state.isEmpty()) {
                 builder.setState(state);
             }
             if (e.getRedirectUri()!= null && !e.getRedirectUri().isEmpty()) {
                 builder.location(e.getRedirectUri());
                 oAuthResponse = builder.buildQueryMessage();
             }
             else {
                 oAuthResponse = builder.buildJSONMessage();
             }
        }
        catch (OAuthSystemException e1) {
            throwit(e1, state);
        }
        Response r = createResponse(oAuthResponse);
        return new WebApplicationException(r);
    }

    @Override
    public WebApplicationException throwit (KustvaktException e){
        return throwit(e, null);
    }
    
    public WebApplicationException throwit (KustvaktException e, String state) {
        String errorCode = e.getEntity();
        int responseStatus = e.getResponseStatus();
        try {
            if(responseStatus>0) {
                return throwit(createOAuthProblemException(e, responseStatus, state));
            }
            else if (errorCode == null){
                return super.throwit(e);
            }
            else if (errorCode.equals(OAuth2Error.INVALID_CLIENT)
                    || errorCode.equals(OAuth2Error.UNAUTHORIZED_CLIENT)
                    || errorCode.equals(OAuth2Error.INVALID_TOKEN)) {
                return throwit(createOAuthProblemException(e,
                        Status.UNAUTHORIZED.getStatusCode(), state));
            }
            else if (errorCode.equals(OAuth2Error.INVALID_GRANT)
                    || errorCode.equals(OAuth2Error.INVALID_REQUEST)
                    || errorCode.equals(OAuth2Error.INVALID_SCOPE)
                    || errorCode.equals(OAuth2Error.UNSUPPORTED_GRANT_TYPE)
                    || errorCode.equals(OAuth2Error.UNSUPPORTED_RESPONSE_TYPE)
                    || errorCode.equals(OAuth2Error.ACCESS_DENIED)) {
                return throwit(createOAuthProblemException(e,
                        Status.BAD_REQUEST.getStatusCode(), state));
            }
            else if (errorCode.equals(OAuth2Error.INSUFFICIENT_SCOPE)) {
                return throwit(createOAuthProblemException(e,
                        Status.FORBIDDEN.getStatusCode(), state));
            }
            else if (errorCode.equals(OAuth2Error.SERVER_ERROR)) {
                return throwit(createOAuthProblemException(e,
                        Status.INTERNAL_SERVER_ERROR.getStatusCode(), state));
            }
            else if (errorCode.equals(OAuth2Error.TEMPORARILY_UNAVAILABLE)) {
                return throwit(createOAuthProblemException(e,
                        Status.SERVICE_UNAVAILABLE.getStatusCode(), state));
            }
            else {
                return super.throwit(e);
            }
        }
        catch (OAuthSystemException e1) {
            return throwit(e1, state);
        }
    }

    private OAuthProblemException createOAuthProblemException (
            KustvaktException e, int statusCode, String state)
            throws OAuthSystemException {
        OAuthProblemException ex = OAuthProblemException.error(e.getEntity())
                .responseStatus(statusCode).state(state)
                .description(e.getMessage());
        if (e.getRedirectUri()!= null) {
            ex.setRedirectUri(e.getRedirectUri().toString());
        }
        return ex;
    }

    /**
     * RFC 6749 regarding authorization error response:
     * 
     * If the request fails due to a missing, invalid, or mismatching
     * redirection URI, or if the client identifier is missing or
     * invalid, the authorization server SHOULD inform the resource
     * owner of the error and MUST NOT automatically redirect the
     * user-agent to the invalid redirection URI.
     * 
     * If the resource owner denies the access request or if the
     * request fails for reasons other than a missing or invalid
     * redirection URI, the authorization server informs the client by
     * adding the following parameters to the query component of the
     * redirection URI using the "application/x-www-form-urlencoded"
     * format.
     * 
     * @param oAuthResponse
     * @return
     */
    public Response createResponse (OAuthResponse oAuthResponse) {
        ResponseBuilder builder =
                Response.status(oAuthResponse.getResponseStatus());
        builder.entity(oAuthResponse.getBody());
        builder.header(HttpHeaders.CACHE_CONTROL, "no-store");
        builder.header(HttpHeaders.PRAGMA, "no-store");

        if (oAuthResponse.getResponseStatus() == Status.UNAUTHORIZED
                .getStatusCode()) {
            builder.header(HttpHeaders.WWW_AUTHENTICATE,
                    "Basic realm=\"Kustvakt\"");
        }
        String uri = oAuthResponse.getLocationUri();
        if (uri != null && !uri.isEmpty()) {
            try {
                builder.location(new URI(uri));
                builder.type(MediaType.APPLICATION_FORM_URLENCODED_TYPE);
            }
            catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }

        return builder.build();
    }

    public Response sendRedirect (String locationUri) throws KustvaktException {
        try {
            ResponseBuilder builder =
                    Response.temporaryRedirect(new URI(locationUri));
            return builder.build();
        }
        catch (URISyntaxException e) {
            throw new KustvaktException(StatusCodes.INVALID_ARGUMENT,
                    e.getMessage(), OAuthError.CodeResponse.INVALID_REQUEST);
        }
    }
}
