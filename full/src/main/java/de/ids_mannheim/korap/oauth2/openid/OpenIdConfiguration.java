package de.ids_mannheim.korap.oauth2.openid;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Defines OpenID configuration.
 * 
 * Note: some configuration such as display_values_supported and
 * ui_locales_supported are more relevant to KorAP user interface
 * component Kalamar.
 * 
 * @see <a
 *      href="https://openid.net/specs/openid-connect-discovery-1_0.html#ProviderMetadata">OpenID
 *      Provider Metadata</a>
 * @author margaretha
 *
 */
@JsonInclude(Include.NON_EMPTY)
public class OpenIdConfiguration {

    public final static String JWKS_ENDPOINT = "/oauth2/openid/jwks";
    public static final String CLIENT_REGISTRATION_ENDPOINT =
            "/oauth2/client/register";
    public static final String AUTHORIZATION_ENDPOINT =
            "/oauth2/openid/authorize";
    public static final String TOKEN_ENDPOINT = "/oauth2/openid/token";

    private String issuer;
    private String jwks_uri;

    private String authorization_endpoint;
    private String token_endpoint;
    private String userinfo_endpoint;
    private String registration_endpoint;

    // Additional endpoints
    private String introspection_endpoint;
    private String revocation_endpoint;
    private String end_session_endpoint;

    private String[] scopes_supported;
    private String[] response_types_supported;
    private String[] response_modes_supported;
    private String[] grant_types_supported;

    private String[] token_endpoint_auth_methods_supported;
    private String[] token_endpoint_auth_signing_alg_values_supported;

    private String[] id_token_signing_alg_values_supported;
    private String[] id_token_encryption_alg_values_supported;
    private String[] id_token_encryption_enc_values_supported;

    private String[] userinfo_signing_alg_values_supported;
    private String[] userinfo_encryption_alg_values_supported;
    private String[] userinfo_encryption_enc_values_supported;

    private String[] request_object_signing_alg_values_supported;
    private String[] request_object_encryption_alg_values_supported;
    private String[] request_object_encryption_enc_values_supported;

    private String[] subject_types_supported;
    private String[] acr_values_supported;
    private String[] display_values_supported;
    private String[] claim_types_supported;
    private String[] claims_supported;
    private String[] claims_locales_supported;
    private String[] ui_locales_supported;

    private boolean claims_parameter_supported = false;
    private boolean request_parameter_supported = false;
    private boolean request_uri_parameter_supported = true;
    private boolean require_request_uri_registration = false;

    private String op_policy_uri;
    private String op_tos_uri;
    private String service_documentation;

    private boolean mutual_tls_sender_constrained_access_tokens = false;

    // OAuth2.0 Discovery
    // List of Proof Key for Code Exchange (PKCE) code challenge
    // methods supported on by the authorization server
    // private String[] code_challenge_methods_supported;

    public String getIssuer () {
        return issuer;
    }

    /**
     * REQUIRED
     * 
     * @param issuer
     *            The server identifier, typically base-URL
     */
    public void setIssuer (String issuer) {
        this.issuer = issuer;
    }

    public String getJwks_uri () {
        return jwks_uri;
    }

    /**
     * REQUIRED
     * 
     * @param jwks_uri
     *            The public JWK set URL
     */
    public void setJwks_uri (String jwks_uri) {
        this.jwks_uri = jwks_uri;
    }

    public String getAuthorization_endpoint () {
        return authorization_endpoint;
    }

    /**
     * REQUIRED
     * 
     * @param authorization_endpoint
     *            The authorisation endpoint URL.
     */
    public void setAuthorization_endpoint (String authorization_endpoint) {
        this.authorization_endpoint = authorization_endpoint;
    }

    public String getToken_endpoint () {
        return token_endpoint;
    }

    /**
     * REQUIRED unless only the Implicit Flow is used.
     * 
     * @param token_endpoint
     *            The token endpoint URL.
     */
    public void setToken_endpoint (String token_endpoint) {
        this.token_endpoint = token_endpoint;
    }

    public String getUserinfo_endpoint () {
        return userinfo_endpoint;
    }

    /**
     * RECOMMENDED. The URL MUST use the https scheme.
     * 
     * @param userinfo_endpoint
     *            The OpenID Connect UserInfo endpoint URL.
     */
    public void setUserinfo_endpoint (String userinfo_endpoint) {
        this.userinfo_endpoint = userinfo_endpoint;
    }

    public String getRegistration_endpoint () {
        return registration_endpoint;
    }

    /**
     * RECOMMENDED
     * 
     * @param registration_endpoint
     *            The OAuth 2.0 / OpenID Connect client registration
     *            endpoint
     *            URL.
     */
    public void setRegistration_endpoint (String registration_endpoint) {
        this.registration_endpoint = registration_endpoint;
    }

    public String[] getScopes_supported () {
        return scopes_supported;
    }

    /**
     * RECOMMENDED
     * 
     * @param scopes_supported
     *            List of the supported scope values. Certain
     *            values may be omitted for privacy reasons.
     */
    public void setScopes_supported (String[] scopes_supported) {
        this.scopes_supported = scopes_supported;
    }

    public String[] getResponse_types_supported () {
        return response_types_supported;
    }

    /**
     * REQUIRED
     * 
     * @param response_types_supported
     *            List of the supported response_type
     *            values.
     */
    public void setResponse_types_supported (
            String[] response_types_supported) {
        this.response_types_supported = response_types_supported;
    }

    public String[] getResponse_modes_supported () {
        return response_modes_supported;
    }

    /**
     * OPTIONAL
     * 
     * @param response_modes_supported
     *            List of the supported response mode
     *            values.
     */
    public void setResponse_modes_supported (
            String[] response_modes_supported) {
        this.response_modes_supported = response_modes_supported;
    }

    public String[] getGrant_types_supported () {
        return grant_types_supported;
    }

    /**
     * OPTIONAL
     * 
     * @param grant_types_supported
     *            List of the supported grant types.
     */
    public void setGrant_types_supported (String[] grant_types_supported) {
        this.grant_types_supported = grant_types_supported;
    }

    public String[] getAcr_values_supported () {
        return acr_values_supported;
    }

    /**
     * OPTIONAL
     * 
     * @param acr_values_supported
     *            List of the supported Authentication Context Class
     *            References.
     */
    public void setAcr_values_supported (String[] acr_values_supported) {
        this.acr_values_supported = acr_values_supported;
    }

    public String[] getSubject_types_supported () {
        return subject_types_supported;
    }

    /**
     * REQUIRED
     * 
     * @param subject_types_supported
     *            List of the supported subject (end-user) identifier
     *            types.
     */
    public void setSubject_types_supported (String[] subject_types_supported) {
        this.subject_types_supported = subject_types_supported;
    }

    public String[] getId_token_signing_alg_values_supported () {
        return id_token_signing_alg_values_supported;
    }

    /**
     * REQUIRED
     * 
     * @param id_token_signing_alg_values_supported
     *            List of the supported JWS algorithms for
     *            the issued ID tokens to encode claims in a JWT.
     */
    public void setId_token_signing_alg_values_supported (
            String[] id_token_signing_alg_values_supported) {
        this.id_token_signing_alg_values_supported =
                id_token_signing_alg_values_supported;
    }

    public String[] getId_token_encryption_alg_values_supported () {
        return id_token_encryption_alg_values_supported;
    }

    /**
     * OPTIONAL
     * 
     * @param id_token_encryption_alg_values_supported
     *            List of the supported JWE algorithms for
     *            the issued ID tokens to encode claims in a JWT.
     */
    public void setId_token_encryption_alg_values_supported (
            String[] id_token_encryption_alg_values_supported) {
        this.id_token_encryption_alg_values_supported =
                id_token_encryption_alg_values_supported;
    }

    public String[] getId_token_encryption_enc_values_supported () {
        return id_token_encryption_enc_values_supported;
    }

    /**
     * OPTIONAL
     * 
     * @param id_token_encryption_enc_values_supported
     *            List of the supported JWE encryption methods for
     *            the issued ID tokens to encode claims in a JWT.
     */
    public void setId_token_encryption_enc_values_supported (
            String[] id_token_encryption_enc_values_supported) {
        this.id_token_encryption_enc_values_supported =
                id_token_encryption_enc_values_supported;
    }

    public String[] getUserinfo_signing_alg_values_supported () {
        return userinfo_signing_alg_values_supported;
    }

    /**
     * OPTIONAL
     * 
     * @param userinfo_signing_alg_values_supported
     *            List of the supported signing JWS algorithms for
     *            encoding the claims in a JWT returned at the
     *            UserInfo endpoint.
     */
    public void setUserinfo_signing_alg_values_supported (
            String[] userinfo_signing_alg_values_supported) {
        this.userinfo_signing_alg_values_supported =
                userinfo_signing_alg_values_supported;
    }

    public String[] getUserinfo_encryption_alg_values_supported () {
        return userinfo_encryption_alg_values_supported;
    }

    /**
     * OPTIONAL
     * 
     * @param userinfo_encryption_alg_values_supported
     *            List of the supported JWE encryption algorithms for
     *            encoding the claims in a JWT returned at the
     *            UserInfo endpoint.
     */
    public void setUserinfo_encryption_alg_values_supported (
            String[] userinfo_encryption_alg_values_supported) {
        this.userinfo_encryption_alg_values_supported =
                userinfo_encryption_alg_values_supported;
    }

    public String[] getUserinfo_encryption_enc_values_supported () {
        return userinfo_encryption_enc_values_supported;
    }

    /**
     * OPTIONAL
     * 
     * @param userinfo_encryption_enc_values_supported
     *            List of the supported JWE encryption methods for
     *            encoding the claims in a JWT returned at the
     *            UserInfo endpoint.
     */
    public void setUserinfo_encryption_enc_values_supported (
            String[] userinfo_encryption_enc_values_supported) {
        this.userinfo_encryption_enc_values_supported =
                userinfo_encryption_enc_values_supported;
    }

    public String[] getRequest_object_signing_alg_values_supported () {
        return request_object_signing_alg_values_supported;
    }

    /**
     * OPTIONAL
     * 
     * @param request_object_signing_alg_values_supported
     *            JSON array containing a list of supported JWS
     *            signing algorithms (alg values) supported for
     *            Request Objects
     */
    public void setRequest_object_signing_alg_values_supported (
            String[] request_object_signing_alg_values_supported) {
        this.request_object_signing_alg_values_supported =
                request_object_signing_alg_values_supported;
    }

    public String[] getRequest_object_encryption_alg_values_supported () {
        return request_object_encryption_alg_values_supported;
    }

    /**
     * OPTIONAL
     * 
     * @param request_object_encryption_alg_values_supported
     *            List of the supported JWE encryption algorithms for
     *            OpenID Connect request objects
     */
    public void setRequest_object_encryption_alg_values_supported (
            String[] request_object_encryption_alg_values_supported) {
        this.request_object_encryption_alg_values_supported =
                request_object_encryption_alg_values_supported;
    }

    public String[] getRequest_object_encryption_enc_values_supported () {
        return request_object_encryption_enc_values_supported;
    }

    /**
     * OPTIONAL
     * 
     * @param request_object_encryption_enc_values_supported
     *            List of the supported JWE encryption methods for
     *            OpenID Connect request objects, omitted or empty if
     *            none.
     */
    public void setRequest_object_encryption_enc_values_supported (
            String[] request_object_encryption_enc_values_supported) {
        this.request_object_encryption_enc_values_supported =
                request_object_encryption_enc_values_supported;
    }

    public String[] getToken_endpoint_auth_methods_supported () {
        return token_endpoint_auth_methods_supported;
    }

    /**
     * OPTIONAL
     * 
     * @param token_endpoint_auth_methods_supported
     *            List of the supported client authentication methods
     *            at the token endpoint.
     */
    public void setToken_endpoint_auth_methods_supported (
            String[] token_endpoint_auth_methods_supported) {
        this.token_endpoint_auth_methods_supported =
                token_endpoint_auth_methods_supported;
    }

    public String[] getToken_endpoint_auth_signing_alg_values_supported () {
        return token_endpoint_auth_signing_alg_values_supported;
    }

    /**
     * OPTIONAL
     * 
     * @param token_endpoint_auth_signing_alg_values_supported
     *            List of the supported JWS algorithms for JWT-based
     *            client authentication at the token endpoint
     */
    public void setToken_endpoint_auth_signing_alg_values_supported (
            String[] token_endpoint_auth_signing_alg_values_supported) {
        this.token_endpoint_auth_signing_alg_values_supported =
                token_endpoint_auth_signing_alg_values_supported;
    }

    public String[] getDisplay_values_supported () {
        return display_values_supported;
    }

    /**
     * OPTIONAL
     * 
     * @param display_values_supported
     *            List of the supported display parameters.
     */
    public void setDisplay_values_supported (
            String[] display_values_supported) {
        this.display_values_supported = display_values_supported;
    }

    public String[] getClaim_types_supported () {
        return claim_types_supported;
    }

    /**
     * OPTIONAL
     * 
     * @param claim_types_supported
     *            List of the supported OpenID Connect claim types.
     */
    public void setClaim_types_supported (String[] claim_types_supported) {
        this.claim_types_supported = claim_types_supported;
    }

    public String[] getClaims_supported () {
        return claims_supported;
    }

    /**
     * RECOMMENDED
     * 
     * @param claims_supported
     *            List of the supported OpenID Connect claims.
     */
    public void setClaims_supported (String[] claims_supported) {
        this.claims_supported = claims_supported;
    }

    public String getService_documentation () {
        return service_documentation;
    }

    /**
     * OPTIONAL
     * 
     * @param service_documentation
     *            The service documentation URL
     */
    public void setService_documentation (String service_documentation) {
        this.service_documentation = service_documentation;
    }

    public String[] getClaims_locales_supported () {
        return claims_locales_supported;
    }

    /**
     * OPTIONAL
     * 
     * @param claims_locales_supported
     *            List of the supported OpenID Connect claims locales
     */
    public void setClaims_locales_supported (
            String[] claims_locales_supported) {
        this.claims_locales_supported = claims_locales_supported;
    }

    public String[] getUi_locales_supported () {
        return ui_locales_supported;
    }

    /**
     * OPTIONAL
     * 
     * @param ui_locales_supported
     *            List of the supported UI locales
     */
    public void setUi_locales_supported (String[] ui_locales_supported) {
        this.ui_locales_supported = ui_locales_supported;
    }

    public boolean isClaims_parameter_supported () {
        return claims_parameter_supported;
    }

    /**
     * OPTIONAL. Default false.
     * 
     * @param claims_parameter_supported
     *            Specifies whether the claims request parameter is
     *            supported.
     */
    public void setClaims_parameter_supported (
            boolean claims_parameter_supported) {
        this.claims_parameter_supported = claims_parameter_supported;
    }

    public boolean isRequest_parameter_supported () {
        return request_parameter_supported;
    }

    /**
     * OPTIONAL. Default false.
     * 
     * @param request_parameter_supported
     *            Specifies whether the request parameter is
     *            supported.
     */
    public void setRequest_parameter_supported (
            boolean request_parameter_supported) {
        this.request_parameter_supported = request_parameter_supported;
    }

    public boolean isRequest_uri_parameter_supported () {
        return request_uri_parameter_supported;
    }

    /**
     * OPTIONAL. Default true.
     * 
     * @param request_uri_parameter_supported
     *            Specifies whether the request_uri parameter is
     *            supported.
     */
    public void setRequest_uri_parameter_supported (
            boolean request_uri_parameter_supported) {
        this.request_uri_parameter_supported = request_uri_parameter_supported;
    }

    public boolean isRequire_request_uri_registration () {
        return require_request_uri_registration;
    }

    /**
     * OPTIONAL. Default false.
     * 
     * @param require_request_uri_registration
     *            Specifies whether request URIs must be registered
     *            for a client.
     */
    public void setRequire_request_uri_registration (
            boolean require_request_uri_registration) {
        this.require_request_uri_registration =
                require_request_uri_registration;
    }

    public String getOp_policy_uri () {
        return op_policy_uri;
    }

    /**
     * OPTIONAL. URL that the OpenID Provider provides to the person
     * registering the Client to read about the requirements on
     * how the client can use the data provided by the OpenID
     * Provider. The registration process SHOULD display this URL to
     * the person registering the Client if it is given.
     * 
     * @param op_policy_uri
     *            The privacy policy document URL, omitted if none.
     */
    public void setOp_policy_uri (String op_policy_uri) {
        this.op_policy_uri = op_policy_uri;
    }

    public String getOp_tos_uri () {
        return op_tos_uri;
    }

    /**
     * @param op_tos_uri
     *            The terms of service document URL, omitted if none.
     */
    public void setOp_tos_uri (String op_tos_uri) {
        this.op_tos_uri = op_tos_uri;
    }

    public String getIntrospection_endpoint () {
        return introspection_endpoint;
    }

    /**
     * ADDITIONAL
     * 
     * @param introspection_endpoint
     *            The token introspection endpoint URL.
     */
    public void setIntrospection_endpoint (String introspection_endpoint) {
        this.introspection_endpoint = introspection_endpoint;
    }

    public String getRevocation_endpoint () {
        return revocation_endpoint;
    }

    /**
     * ADDITIONAL
     * 
     * @param revocation_endpoint
     *            The token revocation endpoint URL.
     */
    public void setRevocation_endpoint (String revocation_endpoint) {
        this.revocation_endpoint = revocation_endpoint;
    }

    public String getEnd_session_endpoint () {
        return end_session_endpoint;
    }

    /**
     * ADDITIONAL
     * 
     * @param end_session_endpoint
     *            The OpenID Connect logout endpoint URL, omitted if
     *            disabled.
     */
    public void setEnd_session_endpoint (String end_session_endpoint) {
        this.end_session_endpoint = end_session_endpoint;
    }

    public boolean isMutual_tls_sender_constrained_access_tokens () {
        return mutual_tls_sender_constrained_access_tokens;
    }

    /**
     * OPTIONAL. Default false.
     * 
     * @see <a
     *      href="https://tools.ietf.org/id/draft-ietf-oauth-mtls-03.html#server_metadata">Mutual
     *      TLS Profile for OAuth 2.0</a>
     * @param mutual_tls_sender_constrained_access_tokens
     *            specifies whether issue of client X.509 certificate
     *            bound access tokens is supported, omitted
     *            implies no support.
     */
    public void setMutual_tls_sender_constrained_access_tokens (
            boolean mutual_tls_sender_constrained_access_tokens) {
        this.mutual_tls_sender_constrained_access_tokens =
                mutual_tls_sender_constrained_access_tokens;
    }
}
