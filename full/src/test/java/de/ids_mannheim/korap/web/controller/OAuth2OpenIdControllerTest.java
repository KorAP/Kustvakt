package de.ids_mannheim.korap.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.text.ParseException;
import java.util.Date;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.entity.ContentType;
import org.apache.oltu.oauth2.common.message.types.TokenType;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.net.HttpHeaders;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.SignedJWT;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.core.util.MultivaluedMapImpl;

import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.FullConfiguration;
import de.ids_mannheim.korap.config.SpringJerseyTest;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.oauth2.constant.OAuth2Error;
import de.ids_mannheim.korap.utils.JsonUtils;

public class OAuth2OpenIdControllerTest extends SpringJerseyTest {

    @Autowired
    private HttpAuthorizationHandler handler;
    @Autowired
    private FullConfiguration config;

    private String redirectUri =
            "https://korap.ids-mannheim.de/confidential/redirect";
    private String username = "dory";

    private static String publicKey;

    @BeforeClass
    public static void init () throws IOException {
        InputStream is = OAuth2OpenIdControllerTest.class.getClassLoader()
                .getResourceAsStream("kustvakt-public.key");

        try (BufferedReader reader =
                new BufferedReader(new InputStreamReader(is));) {
            publicKey = reader.readLine();
        }
    }

    private ClientResponse sendAuthorizationRequest (
            MultivaluedMap<String, String> form) throws KustvaktException {
        return resource().path("oauth2").path("openid").path("authorize")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue(username,
                                "password"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(HttpHeaders.CONTENT_TYPE,
                        ContentType.APPLICATION_FORM_URLENCODED)
                .entity(form).post(ClientResponse.class);
    }

    private ClientResponse sendTokenRequest (
            MultivaluedMap<String, String> form) throws KustvaktException {
        return resource().path("oauth2").path("openid").path("token")
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(HttpHeaders.CONTENT_TYPE,
                        ContentType.APPLICATION_FORM_URLENCODED)
                .entity(form).post(ClientResponse.class);
    }

    @Test
    public void testRequestAuthorizationCode ()
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {

        MultivaluedMap<String, String> form = new MultivaluedMapImpl();
        form.add("response_type", "code");
        form.add("client_id", "fCBbQkAyYzI4NzUxMg");

        testRequestAuthorizationCodeWithoutOpenID(form, redirectUri);
        form.add("scope", "openid");

        testRequestAuthorizationCodeMissingRedirectUri(form);
        testRequestAuthorizationCodeInvalidRedirectUri(form);
        form.add("redirect_uri", redirectUri);

        form.add("state", "thisIsMyState");

        ClientResponse response = sendAuthorizationRequest(form);
        URI location = response.getLocation();
        assertEquals(redirectUri, location.getScheme() + "://"
                + location.getHost() + location.getPath());

        MultiValueMap<String, String> params =
                UriComponentsBuilder.fromUri(location).build().getQueryParams();
        assertNotNull(params.getFirst("code"));
        assertEquals("thisIsMyState", params.getFirst("state"));
    }

    private void testRequestAuthorizationCodeWithoutOpenID (
            MultivaluedMap<String, String> form, String redirectUri)
            throws KustvaktException {
        ClientResponse response = sendAuthorizationRequest(form);
        URI location = response.getLocation();
        // System.out.println(location.toString());
        assertEquals(redirectUri, location.getScheme() + "://"
                + location.getHost() + location.getPath());
    }

    private void testRequestAuthorizationCodeMissingRedirectUri (
            MultivaluedMap<String, String> form) throws KustvaktException {
        ClientResponse response = sendAuthorizationRequest(form);
        String entity = response.getEntity(String.class);

        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(OAuth2Error.INVALID_REQUEST, node.at("/error").asText());
        assertEquals("redirect_uri is required",
                node.at("/error_description").asText());
    }

    private void testRequestAuthorizationCodeInvalidRedirectUri (
            MultivaluedMap<String, String> form) throws KustvaktException {
        form.add("redirect_uri", "blah");
        ClientResponse response = sendAuthorizationRequest(form);
        String entity = response.getEntity(String.class);

        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(OAuth2Error.INVALID_REQUEST, node.at("/error").asText());
        assertEquals("Invalid redirect URI",
                node.at("/error_description").asText());

        form.remove("redirect_uri");
    }

    @Test
    public void testRequestAuthorizationCodeMissingClientID ()
            throws KustvaktException {
        MultivaluedMap<String, String> form = new MultivaluedMapImpl();
        form.add("scope", "openid");
        form.add("redirect_uri", redirectUri);

        // error response is represented in JSON because redirect URI
        // cannot be verified without client id
        // Besides client_id is a mandatory parameter in a normal
        // OAuth2 authorization request, thus it is checked first,
        // before redirect_uri. see
        // com.nimbusds.oauth2.sdk.AuthorizationRequest

        ClientResponse response = sendAuthorizationRequest(form);
        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(OAuth2Error.INVALID_REQUEST, node.at("/error").asText());
        assertEquals("Invalid request: Missing \"client_id\" parameter",
                node.at("/error_description").asText());

    }

    @Test
    public void testRequestAuthorizationCodeMissingResponseType ()
            throws KustvaktException {
        MultivaluedMap<String, String> form = new MultivaluedMapImpl();
        form.add("scope", "openid");
        form.add("redirect_uri", redirectUri);
        form.add("client_id", "blah");

        // client_id has not been verified yet
        // MUST NOT automatically redirect the user-agent to the
        // invalid redirection URI.

        ClientResponse response = sendAuthorizationRequest(form);
        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(OAuth2Error.INVALID_REQUEST, node.at("/error").asText());
        assertEquals("Invalid request: Missing \"response_type\" parameter",
                node.at("/error_description").asText());
    }

    private void testRequestAuthorizationCodeUnsupportedResponseType (
            MultivaluedMap<String, String> form, String type)
            throws KustvaktException {

        ClientResponse response = sendAuthorizationRequest(form);
        System.out.println(response.getEntity(String.class));
        URI location = response.getLocation();
        assertEquals(MediaType.APPLICATION_FORM_URLENCODED,
                response.getType().toString());

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
        MultivaluedMap<String, String> form = new MultivaluedMapImpl();
        form.add("scope", "openid");
        form.add("redirect_uri", redirectUri);
        form.add("response_type", "id_token");
        form.add("client_id", "fCBbQkAyYzI4NzUxMg");
        form.add("nonce", "nonce");

        testRequestAuthorizationCodeUnsupportedResponseType(form, "id_token");

        form.remove("response_type");
        form.add("response_type", "id_token token");
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
     * @throws KustvaktException
     */

    @Test
    public void testRequestAuthorizationCodeUnsupportedHybridFlow ()
            throws KustvaktException {
        MultivaluedMap<String, String> form = new MultivaluedMapImpl();
        form.add("scope", "openid");
        form.add("redirect_uri", redirectUri);
        form.add("response_type", "code id_token");
        form.add("client_id", "fCBbQkAyYzI4NzUxMg");
        form.add("nonce", "nonce");
        testRequestAuthorizationCodeUnsupportedResponseType(form, "id_token");

        form.remove("response_type");
        form.add("response_type", "code token");
        testRequestAuthorizationCodeUnsupportedResponseType(form, "token");
    }

    @Test
    public void testRequestAccessToken ()
            throws KustvaktException, ParseException, InvalidKeySpecException,
            NoSuchAlgorithmException, JOSEException {
        String client_id = "fCBbQkAyYzI4NzUxMg";
        MultivaluedMap<String, String> form = new MultivaluedMapImpl();
        form.add("response_type", "code");
        form.add("client_id", client_id);
        form.add("redirect_uri", redirectUri);
        form.add("scope", "openid");
        form.add("state", "thisIsMyState");

        ClientResponse response = sendAuthorizationRequest(form);
        URI location = response.getLocation();
        MultiValueMap<String, String> params =
                UriComponentsBuilder.fromUri(location).build().getQueryParams();
        String code = params.getFirst("code");

        MultivaluedMap<String, String> tokenForm = new MultivaluedMapImpl();
        tokenForm.add("grant_type", "authorization_code");
        tokenForm.add("redirect_uri", redirectUri);
        tokenForm.add("client_id", client_id);
        tokenForm.add("client_secret", "secret");
        tokenForm.add("code", code);

        ClientResponse tokenResponse = sendTokenRequest(tokenForm);
        String entity = tokenResponse.getEntity(String.class);
        // System.out.println(entity);

        JsonNode node = JsonUtils.readTree(entity);
        assertNotNull(node.at("/access_token").asText());
        assertNotNull(node.at("/refresh_token").asText());
        assertEquals(TokenType.BEARER.toString(),
                node.at("/token_type").asText());
        assertNotNull(node.at("/expires_in").asText());
        String id_token = node.at("/id_token").asText();
        assertNotNull(id_token);

        verifyingIdToken(id_token, username, client_id);
    }

    private void verifyingIdToken (String id_token, String username,
            String client_id) throws ParseException, InvalidKeySpecException,
            NoSuchAlgorithmException, JOSEException {
        byte[] decodedPuk = Base64.decodeBase64(publicKey);
        KeySpec keySpec = new X509EncodedKeySpec(decodedPuk);
        RSAPublicKey publicKey = (RSAPublicKey) KeyFactory.getInstance("RSA")
                .generatePublic(keySpec);

        SignedJWT signedJWT = SignedJWT.parse(id_token);
        JWSVerifier verifier = new RSASSAVerifier(publicKey);
        assertTrue(signedJWT.verify(verifier));

        assertEquals(client_id,
                signedJWT.getJWTClaimsSet().getAudience().get(0));
        assertEquals(username, signedJWT.getJWTClaimsSet().getSubject());
        assertEquals(config.getIssuerURI().toString(),
                signedJWT.getJWTClaimsSet().getIssuer());
        assertTrue(new Date()
                .before(signedJWT.getJWTClaimsSet().getExpirationTime()));
    }
}
