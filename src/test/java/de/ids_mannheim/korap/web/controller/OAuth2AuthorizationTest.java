package de.ids_mannheim.korap.web.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.oauth2.constant.OAuth2Error;
import de.ids_mannheim.korap.utils.JsonUtils;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

public class OAuth2AuthorizationTest extends OAuth2TestBase {

    private String userAuthHeader;

    public OAuth2AuthorizationTest () throws KustvaktException {
        userAuthHeader = HttpAuthorizationHandler
                .createBasicAuthorizationHeaderValue("dory", "password");
    }

    @Test
    public void testAuthorizeUnauthenticated () throws KustvaktException {

        Response response = requestAuthorizationCode("code", publicClientId, "",
                "search match_info", "", "");
        assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.AUTHORIZATION_FAILED,
                node.at("/errors/0/0").asInt());
        assertEquals("Unauthorized operation for user: guest",
                node.at("/errors/0/1").asText());
    }

    @Test
    public void testAuthorizeConfidentialClient () throws KustvaktException {
        // with registered redirect URI
        Response response = requestAuthorizationCode("code",
                confidentialClientId, "", "match_info search client_info",
                state, userAuthHeader);

        assertEquals(Status.TEMPORARY_REDIRECT.getStatusCode(),
                response.getStatus());
        URI redirectUri = response.getLocation();
        MultivaluedMap<String, String> params = getQueryParamsFromURI(
                redirectUri);
        assertNotNull(params.getFirst("code"));
        assertEquals(state, params.getFirst("state"));
    }

    @Test
    public void testAuthorizePublicClient () throws KustvaktException {
        // with registered redirect URI
        String code = requestAuthorizationCode(publicClientId, userAuthHeader);
        assertNotNull(code);
    }

    @Test
    public void testAuthorizeWithRedirectUri () throws KustvaktException {
        Response response = requestAuthorizationCode("code", publicClientId2,
                "https://public.com/redirect", "search match_info", "",
                userAuthHeader);
        assertEquals(Status.TEMPORARY_REDIRECT.getStatusCode(),
                response.getStatus());

        URI redirectUri = response.getLocation();
        assertEquals("https", redirectUri.getScheme());
        assertEquals("public.com", redirectUri.getHost());
        assertEquals("/redirect", redirectUri.getPath());

        assertTrue(redirectUri.getQuery().startsWith("code="));
    }

    @Test
    public void testAuthorizeWithoutScope () throws KustvaktException {
        Response response = requestAuthorizationCode("code",
                confidentialClientId, "", "", "", userAuthHeader);
        assertEquals(Status.TEMPORARY_REDIRECT.getStatusCode(),
                response.getStatus());

        URI redirectUri = response.getLocation();
        assertEquals(redirectUri.getScheme(), "https");
        assertEquals(redirectUri.getHost(), "third.party.com");
        assertEquals(redirectUri.getPath(), "/confidential/redirect");

        String[] queryParts = redirectUri.getQuery().split("&");
        assertEquals("error_description=scope+is+required", queryParts[1]);
        assertEquals("error=invalid_scope", queryParts[0]);
    }

    @Test
    public void testAuthorizeMissingClientId () throws KustvaktException {
        Response response = requestAuthorizationCode("code", "", "", "search",
                "", userAuthHeader);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals("Missing parameter: client_id",
                node.at("/error_description").asText());
    }

    @Test
    public void testAuthorizeMissingRedirectUri () throws KustvaktException {
        Response response = requestAuthorizationCode("code", publicClientId2,
                "", "search", state, userAuthHeader);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());

        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(OAuth2Error.INVALID_REQUEST, node.at("/error").asText());
        assertEquals("Missing parameter: redirect URI",
                node.at("/error_description").asText());
        assertEquals(state, node.at("/state").asText());
    }

    @Test
    public void testAuthorizeMissingResponseType () throws KustvaktException {
        Response response = requestAuthorizationCode("", confidentialClientId,
                "", "search", "", userAuthHeader);
        assertEquals(Status.TEMPORARY_REDIRECT.getStatusCode(),
                response.getStatus());

        assertEquals(
                "https://third.party.com/confidential/redirect?"
                        + "error=invalid_request_uri&"
                        + "error_description=Missing+response_type+parameter",
                response.getLocation().toString());
    }

    @Test
    public void testAuthorizeMissingResponseTypeWithoutClientId ()
            throws KustvaktException {
        Response response = requestAuthorizationCode("", "", "", "search", "",
                userAuthHeader);

        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);

        assertEquals(OAuth2Error.INVALID_REQUEST, node.at("/error").asText());
        assertEquals("Missing parameter: client_id",
                node.at("/error_description").asText());
    }

    @Test
    public void testAuthorizeInvalidClientId () throws KustvaktException {
        Response response = requestAuthorizationCode("code",
                "unknown-client-id", "", "search", "", userAuthHeader);
        assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(OAuth2Error.INVALID_CLIENT, node.at("/error").asText());
        assertEquals("Unknown client: unknown-client-id",
                node.at("/error_description").asText());
    }

    @Test
    public void testAuthorizeDifferentRedirectUri () throws KustvaktException {
        String redirectUri = "https://different.uri/redirect";
        Response response = requestAuthorizationCode("code",
                confidentialClientId, redirectUri, "", state, userAuthHeader);

        testInvalidRedirectUri(response.readEntity(String.class),
                response.getHeaderString("Content-Type"), true,
                response.getStatus());
    }

    @Test
    public void testAuthorizeWithRedirectUriLocalhost ()
            throws KustvaktException {
        Response response = requestAuthorizationCode("code", publicClientId2,
                "http://localhost:1410", "search", state, userAuthHeader);
        assertEquals(Status.TEMPORARY_REDIRECT.getStatusCode(),
                response.getStatus());

        URI redirectUri = response.getLocation();
        MultivaluedMap<String, String> params = getQueryParamsFromURI(
                redirectUri);
        assertNotNull(params.getFirst("code"));
        assertEquals(state, params.getFirst("state"));
    }

    @Test
    public void testAuthorizeWithRedirectUriFragment ()
            throws KustvaktException {
        Response response = requestAuthorizationCode("code", publicClientId2,
                "http://public.com/index.html#redirect", "search", state,
                userAuthHeader);
        testInvalidRedirectUri(response.readEntity(String.class),
                response.getHeaderString("Content-Type"), true,
                response.getStatus());
    }

    @Test
    public void testAuthorizeInvalidRedirectUri () throws KustvaktException {
        // host not allowed by Apache URI Validator
        String redirectUri = "https://public.uri/redirect";
        Response response = requestAuthorizationCode("code", publicClientId2,
                redirectUri, "", state, userAuthHeader);
        testInvalidRedirectUri(response.readEntity(String.class),
                response.getHeaderString("Content-Type"), true,
                response.getStatus());
    }

    @Test
    public void testAuthorizeInvalidResponseType () throws KustvaktException {
        // without redirect URI in the request
        Response response = requestAuthorizationCode("string",
                confidentialClientId, "", "search", state, userAuthHeader);
        assertEquals(Status.TEMPORARY_REDIRECT.getStatusCode(),
                response.getStatus());

        assertEquals("https://third.party.com/confidential/redirect?"
                + "error=unsupported_response_type"
                + "&error_description=Unsupported+response+type.+Only+code+is+supported."
                + "&state=thisIsMyState", response.getLocation().toString());

        // with redirect URI, and no registered redirect URI
        response = requestAuthorizationCode("string", publicClientId2,
                "https://public.client.com/redirect", "search", state,
                userAuthHeader);
        assertEquals(Status.TEMPORARY_REDIRECT.getStatusCode(),
                response.getStatus());

        assertEquals("https://public.client.com/redirect?"
                + "error=unsupported_response_type"
                + "&error_description=Unsupported+response+type.+Only+code+is+supported."
                + "&state=thisIsMyState", response.getLocation().toString());

        // with different redirect URI
        String redirectUri = "https://different.uri/redirect";
        response = requestAuthorizationCode("string", confidentialClientId,
                redirectUri, "", state, userAuthHeader);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());

        JsonNode node = JsonUtils.readTree(response.readEntity(String.class));
        assertEquals(OAuth2Error.INVALID_REQUEST, node.at("/error").asText());
        assertEquals("Invalid redirect URI",
                node.at("/error_description").asText());
        assertEquals(state, node.at("/state").asText());

        // without redirect URI in the request and no registered
        // redirect URI
        response = requestAuthorizationCode("string", publicClientId2, "", "",
                state, userAuthHeader);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());

        node = JsonUtils.readTree(response.readEntity(String.class));
        assertEquals(OAuth2Error.INVALID_REQUEST, node.at("/error").asText());
        assertEquals("Missing parameter: redirect URI",
                node.at("/error_description").asText());
        assertEquals(state, node.at("/state").asText());
    }

    @Test
    public void testAuthorizeInvalidScope () throws KustvaktException {
        String scope = "read_address";
        Response response = requestAuthorizationCode("code",
                confidentialClientId, "", scope, state, userAuthHeader);
        assertEquals(Status.TEMPORARY_REDIRECT.getStatusCode(),
                response.getStatus());

        assertEquals(
                "https://third.party.com/confidential/redirect?"
                        + "error=invalid_scope&error_description=Invalid+"
                        + "scope&state=thisIsMyState",
                response.getLocation().toString());
    }

    @Test
    public void testAuthorizeUnsupportedTokenResponseType ()
            throws KustvaktException {
        Response response = requestAuthorizationCode("token",
                confidentialClientId, "", "search", state, userAuthHeader);
        assertEquals(Status.TEMPORARY_REDIRECT.getStatusCode(),
                response.getStatus());

        assertEquals("https://third.party.com/confidential/redirect?"
                + "error=unsupported_response_type"
                + "&error_description=Unsupported+response+type.+Only+code+is+supported."
                + "&state=thisIsMyState", response.getLocation().toString());
    }

}
