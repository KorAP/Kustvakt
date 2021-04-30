package de.ids_mannheim.korap.oauth2.dto;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Describes OAuth2 refresh tokens
 * 
 * @author margaretha
 *
 */
public class OAuth2TokenDto {

    private String token;
    @JsonProperty("created_date")
    private String createdDate;
    @JsonProperty("expires_in")
    private long expiresIn;
    @JsonProperty("user_authentication_time")
    private String userAuthenticationTime;
    private Set<String> scope;

    @JsonProperty("client_id")
    private String clientId;
    @JsonProperty("client_name")
    private String clientName;
    @JsonProperty("client_description")
    private String clientDescription;
    @JsonProperty("client_url")
    private String clientUrl;

    public String getToken () {
        return token;
    }

    public void setToken (String token) {
        this.token = token;
    }

    public String getClientId () {
        return clientId;
    }

    public void setClientId (String clientId) {
        this.clientId = clientId;
    }

    public String getClientName () {
        return clientName;
    }

    public void setClientName (String clientName) {
        this.clientName = clientName;
    }

    public String getClientDescription () {
        return clientDescription;
    }

    public void setClientDescription (String clientDescription) {
        this.clientDescription = clientDescription;
    }

    public String getClientUrl () {
        return clientUrl;
    }

    public void setClientUrl (String clientUrl) {
        this.clientUrl = clientUrl;
    }

    public String getCreatedDate () {
        return createdDate;
    }

    public void setCreatedDate (String createdDate) {
        this.createdDate = createdDate;
    }

    public long getExpiresIn () {
        return expiresIn;
    }

    public void setExpiresIn (long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public String getUserAuthenticationTime () {
        return userAuthenticationTime;
    }

    public void setUserAuthenticationTime (
            String userAuthenticationTime) {
        this.userAuthenticationTime = userAuthenticationTime;
    }

    public Set<String> getScope () {
        return scope;
    }

    public void setScope (Set<String> scope) {
        this.scope = scope;
    }

}
