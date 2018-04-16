package de.ids_mannheim.korap.web;

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

/** OAuth2ResponseHandler builds {@link Response}s from 
 * {@link OAuthResponse}s and handles exceptions by building 
 * OAuth error responses accordingly. 
 * 
 * <br/><br/>
 * 
 * OAuth2 error response consists of error (required), 
 * error_description (optional) and error_uri (optional).
 * 
 * According to RFC 6749, error indicates error code 
 * categorized into:
 * <ul>
 * <li>invalid_request: The request is missing a required parameter, 
 * includes an unsupported parameter value (other than grant type),
 * repeats a parameter, includes multiple credentials, utilizes 
 * more than one mechanism for authenticating the client, or is 
 * otherwise malformed.</li>
 * 
 * <li>invalid_client: Client authentication failed (e.g., unknown 
 * client, no client authentication included, or unsupported 
 * authentication method).  The authorization sever MAY return 
 * an HTTP 401 (Unauthorized) status code to indicate which 
 * HTTP authentication schemes are supported. If the client 
 * attempted to authenticate via the "Authorization" request 
 * header field, the authorization server MUST respond with 
 * an HTTP 401 (Unauthorized) status code and include 
 * the "WWW-Authenticate" response header field matching 
 * the authentication scheme used by the client</li>
 * 
 * <li>invalid_grant: The provided authorization grant 
 * (e.g., authorization code, resource owner credentials) or 
 * refresh token is invalid, expired, revoked, does not match 
 * the redirection URI used in the authorization request, or 
 * was issued to another client.</li>
 * 
 * <li>unauthorized_client:The authenticated client is not 
 * authorized to use this authorization grant type.</li>
 * 
 * <li>unsupported_grant_type: The authorization grant type 
 * is not supported by the authorization server.</li>
 * 
 * <li>invalid_scope: The requested scope is invalid, unknown, 
 * malformed, or exceeds the scope granted by the resource owner.</li>
 * </ul>
 * 
 * 
 * @author margaretha
 *
 */
public class OAuth2ResponseHandler extends KustvaktExceptionHandler {

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
            if (errorCode.equals(OAuthError.TokenResponse.INVALID_CLIENT)) {
                oAuthResponse = createOAuthResponse(e,
                        Status.UNAUTHORIZED.getStatusCode());
            }
            else if (errorCode.equals(OAuthError.TokenResponse.INVALID_GRANT)
                    || errorCode
                            .equals(OAuthError.TokenResponse.INVALID_REQUEST)
                    || errorCode.equals(OAuthError.TokenResponse.INVALID_SCOPE)
                    || errorCode
                            .equals(OAuthError.TokenResponse.UNAUTHORIZED_CLIENT)
                    || errorCode.equals(
                            OAuthError.TokenResponse.UNSUPPORTED_GRANT_TYPE)) {
                oAuthResponse = createOAuthResponse(e,
                        Status.BAD_REQUEST.getStatusCode());

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
}
