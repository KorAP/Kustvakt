package de.ids_mannheim.korap.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.text.ParseException;
import java.util.Date;

import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;

import org.apache.http.entity.ContentType;
import org.apache.oltu.oauth2.common.message.types.TokenType;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.net.HttpHeaders;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.GrantType;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.Response;
import javax.ws.rs.client.Entity;

import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.FullConfiguration;
import de.ids_mannheim.korap.config.SpringJerseyTest;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.oauth2.constant.OAuth2Error;
import de.ids_mannheim.korap.utils.JsonUtils;

public class OAuth2OpenIdControllerTest extends SpringJerseyTest {

    @Autowired
    private FullConfiguration config;

    private String redirectUri =
            "https://korap.ids-mannheim.de/confidential/redirect";
    private String username = "dory";

    private Response sendAuthorizationRequest (
            Form form) throws KustvaktException {
        return target().path(API_VERSION).path("oauth2").path("openid").path("authorize")
                .request()
                .header(Attributes.AUTHORIZATION,
                        HttpAuthorizationHandler
                                .createBasicAuthorizationHeaderValue(username,
                                        "password"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(HttpHeaders.CONTENT_TYPE,
                        ContentType.APPLICATION_FORM_URLENCODED)
                .post(Entity.form(form));
    }

    private Response sendTokenRequest (
            Form form) throws KustvaktException {
        return target().path(API_VERSION).path("oauth2").path("openid").path("token")
                .request()
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(HttpHeaders.CONTENT_TYPE,
                        ContentType.APPLICATION_FORM_URLENCODED)
                .post(Entity.form(form));
    }

    @Test
    public void testRequestAuthorizationCode ()
            throws ProcessingException,
            KustvaktException {

        Form form = new Form();
        form.param("response_type", "code");
        form.param("client_id", "fCBbQkAyYzI4NzUxMg");

        testRequestAuthorizationCodeWithoutOpenID(form, redirectUri);
        form.param("scope", "openid");

        testRequestAuthorizationCodeMissingRedirectUri(form);
        testRequestAuthorizationCodeInvalidRedirectUri(form);
        form.param("redirect_uri", redirectUri);

        form.param("state", "thisIsMyState");

        Response response = sendAuthorizationRequest(form);
        URI location = response.getLocation();
        assertEquals(redirectUri, location.getScheme() + "://"
                + location.getHost() + location.getPath());

        MultiValueMap<String, String> params =
                UriComponentsBuilder.fromUri(location).build().getQueryParams();
        assertNotNull(params.getFirst("code"));
        assertEquals("thisIsMyState", params.getFirst("state"));
    }

    private void testRequestAuthorizationCodeWithoutOpenID (
            Form form, String redirectUri)
            throws KustvaktException {
        Response response = sendAuthorizationRequest(form);
        URI location = response.getLocation();
        // System.out.println(location.toString());
        assertEquals(redirectUri, location.getScheme() + "://"
                + location.getHost() + location.getPath());
    }

    private void testRequestAuthorizationCodeMissingRedirectUri (
            Form form) throws KustvaktException {
        Response response = sendAuthorizationRequest(form);
        String entity = response.readEntity(String.class);

        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(OAuth2Error.INVALID_REQUEST, node.at("/error").asText());
        assertEquals("redirect_uri is required",
                node.at("/error_description").asText());
    }

    private void testRequestAuthorizationCodeInvalidRedirectUri (
            Form form) throws KustvaktException {
        form.param("redirect_uri", "blah");
        Response response = sendAuthorizationRequest(form);
        String entity = response.readEntity(String.class);

        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(OAuth2Error.INVALID_REQUEST, node.at("/error").asText());
        assertEquals("Invalid redirect URI",
                node.at("/error_description").asText());

        form.asMap().remove("redirect_uri");
    }

    @Test
    public void testRequestAuthorizationCodeMissingClientID ()
            throws KustvaktException {
        Form form = new Form();
        form.param("scope", "openid");
        form.param("redirect_uri", redirectUri);

        // error response is represented in JSON because redirect URI
        // cannot be verified without client id
        // Besides client_id is a mandatory parameter in a normal
        // OAuth2 authorization request, thus it is checked first,
        // before redirect_uri. see
        // com.nimbusds.oauth2.sdk.AuthorizationRequest

        Response response = sendAuthorizationRequest(form);
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(OAuth2Error.INVALID_REQUEST, node.at("/error").asText());
        assertEquals("Invalid request: Missing client_id parameter",
                node.at("/error_description").asText());

    }

    @Test
    public void testRequestAuthorizationCodeMissingResponseType ()
            throws KustvaktException {
        Form form = new Form();
        form.param("scope", "openid");
        form.param("redirect_uri", redirectUri);
        form.param("client_id", "blah");

        // client_id has not been verified yet
        // MUST NOT automatically redirect the user-agent to the
        // invalid redirection URI.

        Response response = sendAuthorizationRequest(form);
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(OAuth2Error.INVALID_REQUEST, node.at("/error").asText());
        assertEquals("Invalid request: Missing response_type parameter",
                node.at("/error_description").asText());
    }

    private void testRequestAuthorizationCodeUnsupportedResponseType (
            Form form, String type)
            throws KustvaktException {

        Response response = sendAuthorizationRequest(form);
        URI location = response.getLocation();
        assertEquals(MediaType.APPLICATION_FORM_URLENCODED,
                response.getMediaType().toString());

        MultiValueMap<String, String> params =
                UriComponentsBuilder.fromUri(location).build().getQueryParams();
        assertEquals("invalid_request", params.getFirst("error"));
        assertEquals("unsupported+response_type%3A+" + type,
                params.getFirst("error_description"));
    }

    /**
     * We don't support implicit grant. Implicit grant allows
     * response_type:
     * <ul>
     * <li>id_token</li>
     * <li>id_token token</li>
     * </ul>
     * 
     * @throws KustvaktException
     */
    @Test
    public void testRequestAuthorizationCodeUnsupportedImplicitFlow ()
            throws KustvaktException {
        Form form = new Form();
        form.param("scope", "openid");
        form.param("redirect_uri", redirectUri);
        form.param("response_type", "id_token");
        form.param("client_id", "fCBbQkAyYzI4NzUxMg");
        form.param("nonce", "nonce");

        testRequestAuthorizationCodeUnsupportedResponseType(form, "id_token");

        form.asMap().remove("response_type");
        form.param("response_type", "id_token token");
        testRequestAuthorizationCodeUnsupportedResponseType(form, "id_token");
    }

    /**
     * Hybrid flow is not supported. Hybrid flow allows
     * response_type:
     * <ul>
     * <li>code id_token</li>
     * <li>code token</li>
     * <li>code id_token token</li>
     * </ul>
     * 
     * @throws KustvaktExceptiony);
     *             assertTrue(signedJWT.verify(verifier));
     */

    @Test
    public void testRequestAuthorizationCodeUnsupportedHybridFlow ()
            throws KustvaktException {
        Form form = new Form();
        form.param("scope", "openid");
        form.param("redirect_uri", redirectUri);
        form.param("response_type", "code id_token");
        form.param("client_id", "fCBbQkAyYzI4NzUxMg");
        form.param("nonce", "nonce");
        testRequestAuthorizationCodeUnsupportedResponseType(form, "id_token");

        form.asMap().remove("response_type");
        form.param("response_type", "code token");
        testRequestAuthorizationCodeUnsupportedResponseType(form, "token");
    }

    @Test
    public void testRequestAccessTokenWithAuthorizationCode ()
            throws KustvaktException, ParseException, InvalidKeySpecException,
            NoSuchAlgorithmException, JOSEException {
        String client_id = "fCBbQkAyYzI4NzUxMg";
        String nonce = "thisIsMyNonce";
        Form form = new Form();
        form.param("response_type", "code");
        form.param("client_id", client_id);
        form.param("redirect_uri", redirectUri);
        form.param("scope", "openid");
        form.param("state", "thisIsMyState");
        form.param("nonce", nonce);

        Response response = sendAuthorizationRequest(form);
        URI location = response.getLocation();
        MultiValueMap<String, String> params =
                UriComponentsBuilder.fromUri(location).build().getQueryParams();
        assertEquals("thisIsMyState", params.getFirst("state"));
        String code = params.getFirst("code");

        Form tokenForm = new Form();
        testRequestAccessTokenMissingGrant(tokenForm);
        tokenForm.param("grant_type", "authorization_code");
        tokenForm.param("code", code);
        testRequestAccessTokenMissingClientId(tokenForm);
        tokenForm.param("client_id", client_id);
        testRequestAccessTokenMissingClientSecret(tokenForm);
        tokenForm.param("client_secret", "secret");
        tokenForm.param("redirect_uri", redirectUri);

        Response tokenResponse = sendTokenRequest(tokenForm);
        String entity = tokenResponse.readEntity(String.class);

        JsonNode node = JsonUtils.readTree(entity);
        assertNotNull(node.at("/access_token").asText());
        assertNotNull(node.at("/refresh_token").asText());
        assertEquals(TokenType.BEARER.toString(),
                node.at("/token_type").asText());
        assertNotNull(node.at("/expires_in").asText());
        String id_token = node.at("/id_token").asText();
        assertNotNull(id_token);

        verifyingIdToken(id_token, username, client_id, nonce);
    }

    private void testRequestAccessTokenMissingGrant (
            Form tokenForm) throws KustvaktException {
        Response response = sendTokenRequest(tokenForm);
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(OAuth2Error.INVALID_REQUEST, node.at("/error").asText());
        assertEquals("Invalid request: Missing grant_type parameter",
                node.at("/error_description").asText());
    }

    private void testRequestAccessTokenMissingClientId (
            Form tokenForm) throws KustvaktException {
        Response response = sendTokenRequest(tokenForm);
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(OAuth2Error.INVALID_REQUEST, node.at("/error").asText());
        assertEquals("Invalid request: Missing required client_id "
                + "parameter", node.at("/error_description").asText());
    }

    private void testRequestAccessTokenMissingClientSecret (
            Form tokenForm) throws KustvaktException {
        Response response = sendTokenRequest(tokenForm);
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(OAuth2Error.INVALID_REQUEST, node.at("/error").asText());
        assertEquals("Missing parameter: client_secret",
                node.at("/error_description").asText());
    }

    private void verifyingIdToken (String id_token, String username,
            String client_id, String nonce) throws ParseException,
            InvalidKeySpecException, NoSuchAlgorithmException, JOSEException {
        JWKSet keySet = config.getPublicKeySet();
        RSAKey publicKey = (RSAKey) keySet.getKeyByKeyId(config.getRsaKeyId());

        SignedJWT signedJWT = SignedJWT.parse(id_token);
        JWSVerifier verifier = new RSASSAVerifier(publicKey);
        assertTrue(signedJWT.verify(verifier));

        JWTClaimsSet claimsSet = signedJWT.getJWTClaimsSet();
        assertEquals(client_id, claimsSet.getAudience().get(0));
        assertEquals(username, claimsSet.getSubject());
        assertEquals(config.getIssuerURI().toString(), claimsSet.getIssuer());
        assertTrue(new Date().before(claimsSet.getExpirationTime()));
        assertNotNull(claimsSet.getClaim(Attributes.AUTHENTICATION_TIME));
        assertEquals(nonce, claimsSet.getClaim("nonce"));
    }

    // no openid
    @Test
    public void testRequestAccessTokenWithPassword ()
            throws KustvaktException, ParseException, InvalidKeySpecException,
            NoSuchAlgorithmException, JOSEException {
        // public client
        String client_id = "8bIDtZnH6NvRkW2Fq";
        Form tokenForm = new Form();
        testRequestAccessTokenMissingGrant(tokenForm);

        tokenForm.param("grant_type", GrantType.PASSWORD.toString());
        testRequestAccessTokenMissingUsername(tokenForm);

        tokenForm.param("username", username);
        testRequestAccessTokenMissingPassword(tokenForm);

        tokenForm.param("password", "pass");
        tokenForm.param("client_id", client_id);

        Response tokenResponse = sendTokenRequest(tokenForm);
        String entity = tokenResponse.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);

        assertEquals(OAuth2Error.UNAUTHORIZED_CLIENT,
                node.at("/error").asText());
        assertEquals("Password grant is not allowed for third party clients",
                node.at("/error_description").asText());
    }

    private void testRequestAccessTokenMissingUsername (
            Form tokenForm) throws KustvaktException {
        Response response = sendTokenRequest(tokenForm);
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(OAuth2Error.INVALID_REQUEST, node.at("/error").asText());
        assertEquals("Invalid request: Missing or empty username parameter",
                node.at("/error_description").asText());
    }

    private void testRequestAccessTokenMissingPassword (
            Form tokenForm) throws KustvaktException {
        Response response = sendTokenRequest(tokenForm);
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(OAuth2Error.INVALID_REQUEST, node.at("/error").asText());
        assertEquals("Invalid request: Missing or empty password parameter",
                node.at("/error_description").asText());
    }

    @Test
    public void testPublicKeyAPI () throws KustvaktException {
        Response response = target().path(API_VERSION).path("oauth2").path("openid")
                .path("jwks")
                .request()
                .get();
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(1, node.at("/keys").size());
        node = node.at("/keys/0");
        assertEquals("RSA", node.at("/kty").asText());
        assertEquals(config.getRsaKeyId(), node.at("/kid").asText());
        assertNotNull(node.at("/e").asText());
        assertNotNull(node.at("/n").asText());
    }

    @Test
    public void testOpenIDConfiguration () throws KustvaktException {
        Response response = target().path(API_VERSION).path("oauth2").path("openid")
                .path("config")
                .request()
                .get();
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertNotNull(node.at("/issuer"));
        assertNotNull(node.at("/authorization_endpoint"));
        assertNotNull(node.at("/token_endpoint"));
        assertNotNull(node.at("/response_types_supported"));
        assertNotNull(node.at("/subject_types_supported"));
        assertNotNull(node.at("/id_token_signing_alg_values_supported"));
    }
}
