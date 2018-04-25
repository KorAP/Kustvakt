package de.ids_mannheim.korap.oauth2.constant;

public class OAuth2Error {

    public static final String ERROR = "error";
    public static final String DESCRIPTION = "error_description";
    public static final String URI = "error_uri";


    /**
     * The request is missing a required parameter, includes an
     * invalid parameter value, includes a parameter more than
     * once, or is otherwise malformed.
     */
    public static final String INVALID_REQUEST = "invalid_request";

    /**
     * Client authentication failed (e.g., unknown client, no
     * client authentication included, or unsupported
     * authentication method). The authorization server MAY
     * return an HTTP 401 (Unauthorized) status code to indicate
     * which HTTP authentication schemes are supported. If the
     * client attempted to authenticate via the "Authorization"
     * request header field, the authorization server MUST
     * respond with an HTTP 401 (Unauthorized) status code and
     * include the "WWW-Authenticate" response header field
     * matching the authentication scheme used by the client.
     */
    public static final String INVALID_CLIENT = "invalid_client";

    /**
     * The provided authorization grant (e.g., authorization
     * code, resource owner credentials) or refresh token is
     * invalid, expired, revoked, does not match the redirection
     * URI used in the authorization request, or was issued to
     * another client.
     * 
     */
    public static final String INVALID_GRANT = "invalid_grant";

    /**
     * The client is not authorized to request an authorization
     * code using this method.
     * 
     */
    public static final String UNAUTHORIZED_CLIENT = "unauthorized_client";

    /**
     * The authorization grant type is not supported by the
     * authorization server.
     */
    public static final String UNSUPPORTED_GRANT_TYPE =
            "unsupported_grant_type";

    /**
     * The requested scope is invalid, unknown, or malformed.
     * 
     */
    public static final String INVALID_SCOPE = "invalid_scope";

    /**
     * The resource owner or authorization server denied the
     * request.
     * 
     */
    public static final String ACCESS_DENIED = "access_denied";

    /**
     * The authorization server does not support obtaining an
     * authorization code using this method.
     */
    public static final String UNSUPPORTED_RESPONSE_TYPE =
            "unsupported_response_type";

    /**
     * The authorization server encountered an unexpected
     * condition that prevented it from fulfilling the request.
     * (This error code is needed because a 500 Internal Server
     * Error HTTP status code cannot be returned to the client
     * via an HTTP redirect.)
     * 
     */
    public static final String SERVER_ERROR = "server_error";

    /**
     * The authorization server is currently unable to handle
     * the request due to a temporary overloading or maintenance
     * of the server. (This error code is needed because a 503
     * Service Unavailable HTTP status code cannot be returned
     * to the client via an HTTP redirect.)
     */
    public static final String TEMPORARILY_UNAVAILABLE =
            "temporarily_unavailable";


    // extensions
    
    public static final String INSUFFICIENT_SCOPE = "insufficient_scope";

    public static final String INVALID_TOKEN = "invalid_token";

    public static final String EXPIRED_TOKEN = "expired_token";
}
