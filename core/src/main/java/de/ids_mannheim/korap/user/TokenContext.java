package de.ids_mannheim.korap.user;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.TokenType;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.utils.JsonUtils;
import de.ids_mannheim.korap.utils.TimeUtils;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

/**
 * @author hanl
 * @date 27/01/2014
 */
@Data
public class TokenContext implements java.security.Principal, Serializable {

    /**
     * session relevant data. Are never persisted into a database
     */
    private String username;
    private long expirationTime;
    // either "session_token " / "api_token
    private TokenType tokenType;
    private String token;
    private boolean secureRequired;

    @Getter(AccessLevel.PRIVATE)
    @Setter(AccessLevel.PRIVATE)
    private Map<String, Object> parameters;
    private String hostAddress;
    private String userAgent;


    public TokenContext () {
        this.parameters = new HashMap<>();
        this.setUsername("");
        this.setToken("");
        this.setSecureRequired(false);
        this.setExpirationTime(-1);
    }


    private Map statusMap () {
        Map m = new HashMap();
        if (username != null && !username.isEmpty())
            m.put(Attributes.USERNAME, username);
        m.put(Attributes.TOKEN_EXPIRATION,
                TimeUtils.format(this.expirationTime));
        m.put(Attributes.TOKEN, this.token);
        m.put(Attributes.TOKEN_TYPE, this.tokenType);
        return m;
    }


    public Map<String, Object> params () {
        return new HashMap<>(parameters);
    }


    public boolean match (TokenContext other) {
        if (other.getToken().equals(this.token))
            if (this.getHostAddress().equals(this.hostAddress))
                // user agent should be irrelvant -- what about os system version?
                //                if (other.getUserAgent().equals(this.userAgent))
                return true;
        return false;
    }


    public void addContextParameter (String key, String value) {
        this.parameters.put(key, value);
    }


    public void addParams (Map<String, Object> map) {
        for (Map.Entry<String, Object> e : map.entrySet())
            this.parameters.put(e.getKey(), String.valueOf(e.getValue()));
    }


    public void removeContextParameter (String key) {
        this.parameters.remove(key);
    }


    public void setExpirationTime (long date) {
        this.expirationTime = date;
    }


    //todo: complete
    public static TokenContext fromJSON (String s) throws KustvaktException {
        JsonNode node = JsonUtils.readTree(s);
        TokenContext c = new TokenContext();
        if (node != null) {
            c.setUsername(node.path(Attributes.USERNAME).asText());
            c.setToken(node.path(Attributes.TOKEN).asText());
        }
        return c;
    }


    public static TokenContext fromOAuth2 (String s) throws KustvaktException {
        JsonNode node = JsonUtils.readTree(s);
        TokenContext c = new TokenContext();
        if (node != null) {
            c.setToken(node.path("token").asText());
            c.setTokenType(TokenType.valueOf(
                    node.path("token_type").asText()));
            c.setExpirationTime(node.path("expires_in").asLong());
            c.addContextParameter("refresh_token", node.path("refresh_token")
                    .asText());

        }
        return c;
    }


    public boolean isValid () {
        return (this.username != null && !this.username.isEmpty())
                && (this.token != null && !this.token.isEmpty())
                && (this.tokenType != null);
    }


    public String getToken () {
        return token;
    }


    public String toJson() throws KustvaktException {
        return JsonUtils.toJSON(this.statusMap());
    }


    public boolean isDemo() {
        return User.UserFactory.isDemo(this.username);
    }



    @Override
    public String getName () {
        return this.getUsername();
    }

}
