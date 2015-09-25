package de.ids_mannheim.korap.user;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.ids_mannheim.korap.utils.JsonUtils;
import lombok.Data;
import org.joda.time.DateTime;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author hanl
 * @date 27/01/2014
 */
@Data
public class TokenContext implements java.security.Principal {

    /**
     * session relevant data. Are never persisted into a database
     */
    private String username;
    private Date expirationTime;
    // either "session_token " / "api_token
    private String tokenType;
    private String token;
    private boolean secureRequired;

    private Map<String, Object> parameters;
    private String hostAddress;
    private String userAgent;

    public TokenContext() {
        this.parameters = new HashMap<>();
        this.setUsername("");
        this.setToken("");
        this.setSecureRequired(false);
    }

    private Map statusMap() {
        Map m = new HashMap();
        if (username != null && !username.isEmpty())
            m.put(Attributes.USERNAME, username);
        m.put(Attributes.TOKEN_EXPIRATION,
                new DateTime(expirationTime).toString());
        m.put(Attributes.TOKEN, this.token);
        return m;
    }

    public boolean match(TokenContext other) {
        if (other.getToken().equals(this.token))
            if (this.getHostAddress().equals(this.hostAddress))
                // user agent should be irrelvant -- what about os system version?
                //                if (other.getUserAgent().equals(this.userAgent))
                return true;
        return false;
    }

    public void addContextParameter(String key, String value) {
        this.parameters.put(key, value);
    }

    public void removeContextParameter(String key) {
        this.parameters.remove(key);
    }

    public void setExpirationTime(long date) {
        this.expirationTime = new Date(date);
    }

    //todo: complete
    public static TokenContext fromJSON(String s) {
        JsonNode node = JsonUtils.readTree(s);
        TokenContext c = new TokenContext();
        if (node != null) {
            c.setUsername(node.path(Attributes.USERNAME).asText());
            c.setToken(node.path(Attributes.TOKEN).asText());
        }
        return c;
    }

    public static TokenContext fromOAuth2(String s) {
        JsonNode node = JsonUtils.readTree(s);
        TokenContext c = new TokenContext();
        if (node != null) {
            c.setToken(node.path("token").asText());
            c.setTokenType(node.path("token_type").asText());
            c.setExpirationTime(node.path("expires_in").asLong());
            c.addContextParameter("refresh_token",
                    node.path("refresh_token").asText());

        }
        return c;
    }

    public String getToken() {
        return token;
    }

    public String toJSON() {
        return JsonUtils.toJSON(this.statusMap());
    }

    public String toResponse() {
        ObjectNode node = JsonUtils.createObjectNode();
        node.put("token", this.getToken());
        node.put("expires", this.getExpirationTime().getTime());
        node.put("token_type", this.getTokenType());
        return JsonUtils.toJSON(node);
    }

    @Override
    public String getName() {
        return this.getUsername();
    }

}
