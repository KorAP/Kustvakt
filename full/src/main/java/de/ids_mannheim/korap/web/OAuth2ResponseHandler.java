package de.ids_mannheim.korap.web;

import java.net.URI;
import java.net.URISyntaxException;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.apache.http.HttpHeaders;
import org.apache.oltu.oauth2.common.error.OAuthError;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.OAuthResponse;

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

    public WebApplicationException throwit (OAuthProblemException e) {
        OAuthResponse oAuthResponse = null;
        try {
            oAuthResponse = OAuthResponse.errorResponse(e.getResponseStatus())
                    .error(e).buildJSONMessage();
        }
        catch (OAuthSystemException e1) {
            throwit(e1);
        }
        Response r = createResponse(oAuthResponse);
        return new WebApplicationException(r);
    }

    @Override
    public WebApplicationException throwit (KustvaktException e) {
        OAuthResponse oAuthResponse = null;
        String errorCode = e.getEntity();
        try {
            if (errorCode.equals(OAuth2Error.INVALID_CLIENT)
                    || errorCode.equals(OAuth2Error.UNAUTHORIZED_CLIENT)
                    || errorCode.equals(OAuth2Error.INVALID_TOKEN)) {
                oAuthResponse = createOAuthResponse(e,
                        Status.UNAUTHORIZED.getStatusCode());
            }
            else if (errorCode.equals(OAuth2Error.INVALID_GRANT)
                    || errorCode.equals(OAuth2Error.INVALID_REQUEST)
                    || errorCode.equals(OAuth2Error.INVALID_SCOPE)
                    || errorCode.equals(OAuth2Error.UNSUPPORTED_GRANT_TYPE)
                    || errorCode.equals(OAuth2Error.UNSUPPORTED_RESPONSE_TYPE)
                    || errorCode.equals(OAuth2Error.ACCESS_DENIED)) {
                oAuthResponse = createOAuthResponse(e,
                        Status.BAD_REQUEST.getStatusCode());
            }
            else if (errorCode.equals(OAuth2Error.INSUFFICIENT_SCOPE)) {
                oAuthResponse = createOAuthResponse(e,
                        Status.FORBIDDEN.getStatusCode());
            }
            else if (errorCode.equals(OAuth2Error.SERVER_ERROR)) {
                oAuthResponse = createOAuthResponse(e,
                        Status.INTERNAL_SERVER_ERROR.getStatusCode());
            }
            else if (errorCode.equals(OAuth2Error.TEMPORARILY_UNAVAILABLE)) {
                oAuthResponse = createOAuthResponse(e,
                        Status.SERVICE_UNAVAILABLE.getStatusCode());
            }
            else {
                return super.throwit(e);
            }
        }
        catch (OAuthSystemException e1) {
            return throwit(e1);
        }

        Response r = createResponse(oAuthResponse);
        return new WebApplicationException(r);
    }

    private OAuthResponse createOAuthResponse (KustvaktException e,
            int statusCode) throws OAuthSystemException {
        OAuthProblemException oAuthProblemException = OAuthProblemException
                .error(e.getEntity()).description(e.getMessage());
        return OAuthResponse.errorResponse(statusCode)
                .error(oAuthProblemException).buildJSONMessage();
    }

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
